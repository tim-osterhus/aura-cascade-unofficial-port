#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import zipfile
from pathlib import Path
from typing import Any

from publish_curseforge import DEFAULT_API_BASE_URL, PublishError, load_mod_version, sha256_file, upload_artifact


PROJECT_ID = 1519395
MINECRAFT_VERSION = "1.21.11"
REQUIRED_RELATIONS = ("fabric-api", "patchouli-fabric-edition")
REQUIRED_MOD_IDS = ("fabric-api", "patchouli", "team_reborn_energy")
BUILD_PINS = {
    "minecraft_version": MINECRAFT_VERSION,
    "loader_version": "0.19.5",
    "fabric_api_version": "0.141.6+1.21.11",
    "patchouli_file": "8713841",
    "energy_api_version": "4.2.0",
}
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

    version = _required_string(manifest, "version")
    if not version.endswith(f"+{MINECRAFT_VERSION}"):
        raise BetaPublishError(f"Manifest version must end in +{MINECRAFT_VERSION}.")
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

    validate_build_pins(workspace)

    source_metadata_path = workspace / "src" / "main" / "resources" / "fabric.mod.json"
    try:
        source_metadata = json.loads(source_metadata_path.read_text(encoding="utf-8"))
    except FileNotFoundError as exc:
        raise BetaPublishError(f"Missing source Fabric metadata: {source_metadata_path}") from exc
    except json.JSONDecodeError as exc:
        raise BetaPublishError(f"Invalid source Fabric metadata: {source_metadata_path}") from exc
    if not isinstance(source_metadata, dict):
        raise BetaPublishError("Source fabric.mod.json must be a JSON object.")
    validate_fabric_metadata(source_metadata, version, "source", workspace)

    game_versions = manifest.get("gameVersionNames")
    if not isinstance(game_versions, list) or not all(isinstance(item, str) for item in game_versions):
        raise BetaPublishError("gameVersionNames must be a string array.")
    game_version_set = set(game_versions)
    allowed_game_version_sets = (
        {MINECRAFT_VERSION, "Fabric", "Client", "Server"},
        {MINECRAFT_VERSION, "Fabric", "Client", "Server", "Java 21"},
    )
    if len(game_version_set) != len(game_versions) or game_version_set not in allowed_game_version_sets:
        raise BetaPublishError(
            f'gameVersionNames must be exactly ["{MINECRAFT_VERSION}", "Fabric", "Client", "Server"] with optional "Java 21".'
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
    required_slugs = [
        relation["slug"].strip().lower()
        for relation in projects
        if relation.get("type") == "requiredDependency"
    ]
    if len(projects) != len(REQUIRED_RELATIONS) or sorted(required_slugs) != sorted(REQUIRED_RELATIONS):
        raise BetaPublishError(
            "Manifest relations must mark exactly these projects as requiredDependency: "
            + ", ".join(REQUIRED_RELATIONS)
        )

    actual_sha256 = sha256_file(artifact_path)
    if actual_sha256.lower() != expected_sha256.lower():
        raise BetaPublishError(
            f"Artifact SHA-256 mismatch: expected {expected_sha256.lower()}, got {actual_sha256}."
        )
    jar_metadata = read_jar_metadata(artifact_path)
    validate_fabric_metadata(jar_metadata, version, "artifact", workspace)
    return manifest, artifact_path, actual_sha256


def read_jar_metadata(artifact_path: Path) -> dict[str, Any]:
    try:
        with zipfile.ZipFile(artifact_path) as archive:
            raw_metadata = archive.read("fabric.mod.json")
        metadata = json.loads(raw_metadata.decode("utf-8"))
    except (KeyError, OSError, UnicodeDecodeError, json.JSONDecodeError, zipfile.BadZipFile) as exc:
        raise BetaPublishError("Artifact must be a valid jar containing a valid root fabric.mod.json.") from exc
    if not isinstance(metadata, dict):
        raise BetaPublishError("Artifact fabric.mod.json must be a JSON object.")
    return metadata


def validate_build_pins(workspace: Path) -> None:
    properties_path = workspace / "gradle.properties"
    try:
        properties = {}
        for raw in properties_path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line and not line.startswith(("#", "!")) and "=" in line:
                key, value = line.split("=", 1)
                properties[key.strip()] = value.strip()
    except OSError as exc:
        raise BetaPublishError(f"Missing build properties: {properties_path}") from exc
    for key, expected in BUILD_PINS.items():
        if properties.get(key, "").strip() != expected:
            raise BetaPublishError(f"gradle.properties {key} must be {expected} for this release.")


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
    if dependencies.get("fabricloader") != ">=0.19.5":
        raise BetaPublishError(f"The {label} fabric.mod.json must require Fabric Loader >=0.19.5.")
    if dependencies.get("patchouli") != ">=1.21.11-94.4-FABRIC":
        raise BetaPublishError(f"The {label} fabric.mod.json must require target Patchouli.")
    for mod_id in REQUIRED_MOD_IDS:
        if mod_id not in dependencies:
            raise BetaPublishError(f"The {label} fabric.mod.json is missing required dependency {mod_id}.")


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
