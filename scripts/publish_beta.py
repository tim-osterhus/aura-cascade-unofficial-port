#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import tomllib
import zipfile
from pathlib import Path
from typing import Any

from publish_curseforge import DEFAULT_API_BASE_URL, PublishError, load_mod_version, sha256_file, upload_artifact


PROJECT_ID = 1519395
MINECRAFT_VERSION = "1.21.1"
REQUIRED_RELATIONS = ("fabric-api", "patchouli")
NEOFORGE_REQUIRED_RELATIONS = ("patchouli",)
NEOFORGE_ARCHIVE_NAME = "aura-cascade-neoforge"
NEOFORGE_METADATA_PATH = "META-INF/neoforge.mods.toml"
FORBIDDEN_NEOFORGE_DEPENDENCIES = {"fabric-api", "team-reborn-energy", "team-reborn-energy-api"}
QA_HELPER_ENTRY_PREFIXES = (
    "pixlepix/auracascade/qa/",
    "io/github/millrace/auracascade/qa/",
)
SHA256_RE = re.compile(r"^[0-9a-fA-F]{64}$")


class BetaPublishError(RuntimeError):
    pass


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Validate a reviewed Aura Cascade beta manifest; upload only with --upload."
    )
    parser.add_argument(
        "--workspace",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Repository root (defaults to this script's repository).",
    )
    parser.add_argument("--manifest", type=Path, required=True, help="Explicitly reviewed beta manifest JSON.")
    parser.add_argument(
        "--upload",
        action="store_true",
        help="Upload after local validation. Without this flag the command is offline dry-run only.",
    )
    args = parser.parse_args(argv)

    workspace = args.workspace.expanduser().resolve()
    manifest_path = args.manifest.expanduser()
    if not manifest_path.is_absolute():
        manifest_path = workspace / manifest_path

    try:
        manifest, artifact_path, actual_sha256 = validate_manifest(workspace, manifest_path.resolve())
        if not args.upload:
            print(
                "DRY RUN: reviewed beta manifest validated; no credentials read and no network request made."
            )
            print(f"project: {PROJECT_ID} | artifact: {artifact_path} | sha256: {actual_sha256}")
            return 0

        token = read_api_token()
        metadata = upload_metadata(manifest)
        try:
            response = upload_artifact(
                api_base_url=DEFAULT_API_BASE_URL,
                token=token,
                project_id=PROJECT_ID,
                artifact_path=artifact_path,
                metadata=metadata,
            )
        except PublishError as exc:
            safe_message = str(exc).replace(token, "[redacted]")
            raise BetaPublishError(safe_message) from exc
        file_id = response.get("id")
        if type(file_id) is not int or file_id <= 0:
            raise BetaPublishError("CurseForge upload response did not contain a positive integer file id.")
        print(f"uploaded file id: {file_id}")
        return 0
    except (BetaPublishError, PublishError, OSError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2


def validate_manifest(workspace: Path, manifest_path: Path) -> tuple[dict[str, Any], Path, str]:
    if not manifest_path.is_file():
        raise BetaPublishError(f"Missing reviewed manifest: {manifest_path}")
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise BetaPublishError(f"Invalid manifest JSON: {manifest_path}") from exc
    if not isinstance(manifest, dict):
        raise BetaPublishError("Manifest must be a JSON object.")
    if manifest.get("reviewed") is not True:
        raise BetaPublishError("Manifest must explicitly set reviewed to true.")
    if type(manifest.get("projectId")) is not int or manifest["projectId"] != PROJECT_ID:
        raise BetaPublishError(f"Manifest projectId must be {PROJECT_ID}.")
    if manifest.get("releaseType") != "beta":
        raise BetaPublishError('Manifest releaseType must be "beta".')
    loader = manifest.get("loader", "Fabric")
    if not isinstance(loader, str) or loader not in {"Fabric", "NeoForge"}:
        raise BetaPublishError('Manifest loader must be exactly "Fabric" or "NeoForge".')

    version = _required_string(manifest, "version")
    _required_string(manifest, "displayName")
    _required_string(manifest, "changelog")
    expected_sha256 = _required_string(manifest, "expectedJarSHA256")
    if not SHA256_RE.fullmatch(expected_sha256):
        raise BetaPublishError("expectedJarSHA256 must contain exactly 64 hexadecimal characters.")

    artifact_name = _required_string(manifest, "artifactPath")
    artifact_path = (workspace / artifact_name).resolve()
    try:
        artifact_path.relative_to(workspace)
    except ValueError as exc:
        raise BetaPublishError("artifactPath must resolve inside the workspace.") from exc
    if artifact_path.suffix.lower() != ".jar" or not artifact_path.is_file():
        raise BetaPublishError(f"artifactPath must name an existing jar: {artifact_name}")

    if loader == "Fabric":
        source_metadata_path = workspace / "src" / "main" / "resources" / "fabric.mod.json"
        other_source_metadata = workspace / "src" / "main" / "resources" / NEOFORGE_METADATA_PATH
        if other_source_metadata.exists():
            raise BetaPublishError("Fabric manifest cannot use NeoForge source metadata.")
        try:
            source_metadata = json.loads(source_metadata_path.read_text(encoding="utf-8"))
        except FileNotFoundError as exc:
            raise BetaPublishError(f"Missing source Fabric metadata: {source_metadata_path}") from exc
        except json.JSONDecodeError as exc:
            raise BetaPublishError(f"Invalid source Fabric metadata: {source_metadata_path}") from exc
        if not isinstance(source_metadata, dict):
            raise BetaPublishError("Source fabric.mod.json must be a JSON object.")
        validate_fabric_metadata(source_metadata, version, "source", workspace)
    else:
        source_metadata_path = workspace / "src" / "main" / "resources" / NEOFORGE_METADATA_PATH
        other_source_metadata = workspace / "src" / "main" / "resources" / "fabric.mod.json"
        if other_source_metadata.exists():
            raise BetaPublishError("NeoForge manifest cannot use Fabric source metadata.")
        gradle_version = load_mod_version(workspace)
        if version != gradle_version:
            raise BetaPublishError("Manifest version must match gradle.properties mod_version exactly.")
        source_metadata = read_neoforge_metadata(source_metadata_path, "source")
        validate_neoforge_metadata(source_metadata, version, "source", workspace)
        expected_artifact_name = f"{NEOFORGE_ARCHIVE_NAME}-{version}.jar"
        if artifact_path.name != expected_artifact_name:
            raise BetaPublishError(
                f'NeoForge artifact filename must be exactly "{expected_artifact_name}".'
            )

    game_versions = manifest.get("gameVersionNames")
    if not isinstance(game_versions, list) or not all(isinstance(item, str) for item in game_versions):
        raise BetaPublishError("gameVersionNames must be a string array.")
    game_version_set = set(game_versions)
    loader_version_name = "Fabric" if loader == "Fabric" else "NeoForge"
    allowed_game_version_sets = (
        {MINECRAFT_VERSION, loader_version_name, "Client", "Server"},
        {MINECRAFT_VERSION, loader_version_name, "Client", "Server", "Java 21"},
    )
    if len(game_version_set) != len(game_versions) or game_version_set not in allowed_game_version_sets:
        expected_versions = f'["{MINECRAFT_VERSION}", "{loader_version_name}", "Client", "Server"]'
        raise BetaPublishError(
            f"gameVersionNames must be exactly {expected_versions} with optional \"Java 21\"."
        )

    relations = manifest.get("relations")
    if not isinstance(relations, dict) or not isinstance(relations.get("projects"), list):
        raise BetaPublishError('relations must contain a "projects" array.')
    projects = relations["projects"]
    for relation in projects:
        if not isinstance(relation, dict) or not isinstance(relation.get("slug"), str):
            raise BetaPublishError("Each relations.projects entry must include a slug and type.")
        if not isinstance(relation.get("type"), str):
            raise BetaPublishError("Each relations.projects entry must include a slug and type.")
    relation_slugs = {relation["slug"].strip().lower().replace("_", "-") for relation in projects}
    required_slugs = {
        relation["slug"].strip().lower().replace("_", "-")
        for relation in projects
        if relation.get("type") == "requiredDependency"
    }
    if loader == "NeoForge":
        forbidden_relations = sorted(relation_slugs & FORBIDDEN_NEOFORGE_DEPENDENCIES)
        if forbidden_relations:
            raise BetaPublishError(
                "NeoForge relations cannot include Fabric API or Team Reborn Energy: "
                + ", ".join(forbidden_relations)
            )
        if required_slugs != set(NEOFORGE_REQUIRED_RELATIONS):
            raise BetaPublishError("NeoForge relations must mark only patchouli as requiredDependency.")
    else:
        missing_relations = sorted(set(REQUIRED_RELATIONS) - required_slugs)
        if missing_relations:
            raise BetaPublishError(
                "Manifest relations must mark these projects as requiredDependency: "
                + ", ".join(missing_relations)
            )

    actual_sha256 = sha256_file(artifact_path)
    if actual_sha256.lower() != expected_sha256.lower():
        raise BetaPublishError(
            f"Artifact SHA-256 mismatch: expected {expected_sha256.lower()}, got {actual_sha256}."
        )
    jar_metadata = read_jar_metadata(artifact_path, loader)
    if loader == "Fabric":
        validate_fabric_metadata(jar_metadata, version, "artifact", workspace)
    else:
        validate_neoforge_metadata(jar_metadata, version, "artifact", workspace)
    return manifest, artifact_path, actual_sha256


def read_jar_metadata(artifact_path: Path, loader: str = "Fabric") -> dict[str, Any]:
    try:
        with zipfile.ZipFile(artifact_path) as archive:
            entries = archive.namelist()
            qa_entries = [
                entry for entry in entries
                if entry.replace("\\", "/").lower().startswith(QA_HELPER_ENTRY_PREFIXES)
            ]
            if qa_entries:
                raise BetaPublishError("Artifact must not contain QA helper entries.")
            expected_path = "fabric.mod.json" if loader == "Fabric" else NEOFORGE_METADATA_PATH
            other_path = NEOFORGE_METADATA_PATH if loader == "Fabric" else "fabric.mod.json"
            if other_path in entries:
                raise BetaPublishError(f"Artifact contains metadata for the wrong loader ({other_path}).")
            raw_metadata = archive.read(expected_path)
        if loader == "Fabric":
            metadata = json.loads(raw_metadata.decode("utf-8"))
        else:
            metadata = tomllib.loads(raw_metadata.decode("utf-8"))
    except BetaPublishError:
        raise
    except (KeyError, OSError, UnicodeDecodeError, json.JSONDecodeError, tomllib.TOMLDecodeError, zipfile.BadZipFile) as exc:
        if loader == "Fabric":
            raise BetaPublishError(
                "Artifact must be a valid jar containing a valid root fabric.mod.json."
            ) from exc
        raise BetaPublishError(
            f"Artifact must be a valid jar containing valid {NEOFORGE_METADATA_PATH}."
        ) from exc
    if not isinstance(metadata, dict):
        if loader == "Fabric":
            raise BetaPublishError("Artifact fabric.mod.json must be a JSON object.")
        raise BetaPublishError(f"Artifact {NEOFORGE_METADATA_PATH} must be an object.")
    return metadata


def read_neoforge_metadata(path: Path, label: str) -> dict[str, Any]:
    try:
        metadata = tomllib.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise BetaPublishError(f"Missing {label} NeoForge metadata: {path}") from exc
    except (OSError, UnicodeDecodeError, tomllib.TOMLDecodeError) as exc:
        raise BetaPublishError(f"Invalid {label} NeoForge metadata: {path}") from exc
    if not isinstance(metadata, dict):
        raise BetaPublishError(f"The {label} {NEOFORGE_METADATA_PATH} must be a TOML object.")
    return metadata


def validate_fabric_metadata(
    metadata: dict[str, Any],
    expected_version: str,
    label: str,
    workspace: Path,
) -> None:
    if metadata.get("id") != "aura":
        raise BetaPublishError(f"The {label} fabric.mod.json id must be aura.")
    actual_version = metadata.get("version")
    if label == "source" and actual_version == "${version}":
        actual_version = load_mod_version(workspace)
    if not isinstance(actual_version, str) or actual_version != expected_version:
        raise BetaPublishError(f"Manifest version must match the {label} fabric.mod.json version.")

    dependencies = metadata.get("depends")
    if not isinstance(dependencies, dict) or dependencies.get("minecraft") != MINECRAFT_VERSION:
        raise BetaPublishError(f"The {label} fabric.mod.json must target Minecraft {MINECRAFT_VERSION} exactly.")
    for mod_id in REQUIRED_RELATIONS:
        if mod_id not in dependencies:
            raise BetaPublishError(f"The {label} fabric.mod.json is missing required dependency {mod_id}.")


def validate_neoforge_metadata(
    metadata: dict[str, Any],
    expected_version: str,
    label: str,
    workspace: Path,
) -> None:
    if metadata.get("modLoader") != "javafml":
        raise BetaPublishError(f"The {label} {NEOFORGE_METADATA_PATH} must declare modLoader=\"javafml\".")

    mods = metadata.get("mods")
    if not isinstance(mods, list) or len(mods) != 1 or not isinstance(mods[0], dict):
        raise BetaPublishError(f"The {label} {NEOFORGE_METADATA_PATH} must contain only the aura mod entry.")
    mod = mods[0]
    if mod.get("modId") != "aura":
        raise BetaPublishError(f"The {label} {NEOFORGE_METADATA_PATH} modId must be aura.")
    actual_version = mod.get("version")
    if label == "source" and actual_version == "${version}":
        actual_version = load_mod_version(workspace)
    if not isinstance(actual_version, str) or actual_version != expected_version:
        raise BetaPublishError(f"Manifest version must match the {label} NeoForge mod version.")

    dependencies_root = metadata.get("dependencies")
    dependencies = dependencies_root.get("aura") if isinstance(dependencies_root, dict) else None
    if not isinstance(dependencies, list):
        raise BetaPublishError(f"The {label} {NEOFORGE_METADATA_PATH} must declare aura dependencies.")
    dependency_by_id: dict[str, dict[str, Any]] = {}
    for dependency in dependencies:
        if not isinstance(dependency, dict) or not isinstance(dependency.get("modId"), str):
            raise BetaPublishError(f"Each {label} NeoForge dependency must include modId and type.")
        mod_id = dependency["modId"].strip().lower().replace("_", "-")
        if not mod_id or mod_id in dependency_by_id:
            raise BetaPublishError(f"The {label} NeoForge dependencies must have unique modId values.")
        dependency_by_id[mod_id] = dependency

    forbidden = sorted(set(dependency_by_id) & FORBIDDEN_NEOFORGE_DEPENDENCIES)
    if forbidden:
        raise BetaPublishError(
            f"The {label} NeoForge metadata cannot depend on Fabric API or Team Reborn Energy: "
            + ", ".join(forbidden)
        )
    for mod_id in ("neoforge", "minecraft", "patchouli"):
        dependency = dependency_by_id.get(mod_id)
        if dependency is None or dependency.get("type") != "required":
            raise BetaPublishError(f"The {label} NeoForge metadata is missing required dependency {mod_id}.")
    if dependency_by_id["minecraft"].get("versionRange") != f"[{MINECRAFT_VERSION}]":
        raise BetaPublishError(
            f"The {label} NeoForge metadata must target Minecraft {MINECRAFT_VERSION} exactly."
        )


def upload_metadata(manifest: dict[str, Any]) -> dict[str, Any]:
    return {
        "changelog": manifest["changelog"],
        "changelogType": "markdown",
        "displayName": manifest["displayName"],
        "gameVersionNames": manifest["gameVersionNames"],
        "releaseType": "beta",
        "relations": manifest["relations"],
    }


def read_api_token() -> str:
    token = os.environ.get("CURSEFORGE_API_TOKEN", "").strip()
    if not token:
        raise BetaPublishError("CURSEFORGE_API_TOKEN is not set; no upload was attempted.")
    return token


def _required_string(manifest: dict[str, Any], key: str) -> str:
    value = manifest.get(key)
    if not isinstance(value, str) or not value.strip():
        raise BetaPublishError(f"Manifest field {key} must be a non-empty string.")
    return value.strip()


if __name__ == "__main__":
    raise SystemExit(main())
