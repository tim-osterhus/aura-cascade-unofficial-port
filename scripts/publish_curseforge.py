#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
import time
import urllib.error
import urllib.request
import uuid
from pathlib import Path
from typing import Any


DEFAULT_CONFIG_PATH = "curseforge_publish.json"
DEFAULT_API_BASE_URL = "https://www.curseforge.com"
HEADING_RE = re.compile(r"^##\s+")


class PublishError(RuntimeError):
    pass


def main() -> int:
    parser = argparse.ArgumentParser(description="Prepare and publish CurseForge release bundles.")
    parser.add_argument("--workspace", default=".", help="Workspace root. Defaults to the current directory.")
    parser.add_argument("--config", default=DEFAULT_CONFIG_PATH, help="Publish config path relative to the workspace.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    prepare_parser = subparsers.add_parser(
        "prepare-bundle",
        help="Create the release bundle for one closure target without uploading.",
    )
    prepare_parser.add_argument("--root-spec-id", required=True, help="Root spec id to prepare.")
    prepare_parser.add_argument(
        "--allow-incomplete-verdict",
        action="store_true",
        help="Allow bundle preparation even if the Arbiter verdict is absent or incomplete.",
    )

    publish_parser = subparsers.add_parser(
        "publish-once",
        help="Prepare and publish the configured closure target once.",
    )
    publish_parser.add_argument("--root-spec-id", help="Override the configured target root spec id.")
    publish_parser.add_argument("--dry-run", action="store_true", help="Skip the live API upload.")

    watch_parser = subparsers.add_parser(
        "watch",
        help="Poll for an Arbiter-complete verdict and publish when ready.",
    )
    watch_parser.add_argument("--root-spec-id", help="Override the configured target root spec id.")
    watch_parser.add_argument("--dry-run", action="store_true", help="Skip the live API upload.")
    watch_parser.add_argument(
        "--poll-seconds",
        type=int,
        help="Override the configured polling interval in seconds.",
    )

    args = parser.parse_args()
    workspace = Path(args.workspace).expanduser().resolve()
    config = load_config(workspace, args.config)

    try:
        if args.command == "prepare-bundle":
            bundle = prepare_bundle(
                workspace,
                config,
                root_spec_id=args.root_spec_id,
                require_complete_verdict=not args.allow_incomplete_verdict,
            )
            print(bundle["publish_ready_path"])
            return 0

        if args.command == "publish-once":
            root_spec_id = args.root_spec_id or configured_root_spec_id(config)
            if root_spec_id is None:
                raise PublishError("No target root spec id configured.")
            published = publish_for_root_spec(
                workspace,
                config,
                root_spec_id=root_spec_id,
                dry_run=args.dry_run,
            )
            return 0 if published else 1

        if args.command == "watch":
            root_spec_id = args.root_spec_id or configured_root_spec_id(config)
            if root_spec_id is None:
                raise PublishError("No target root spec id configured for watch mode.")
            poll_seconds = args.poll_seconds or int(config.get("publish_poll_seconds", 30))
            return watch_loop(
                workspace,
                config,
                root_spec_id=root_spec_id,
                dry_run=args.dry_run,
                poll_seconds=poll_seconds,
            )

    except PublishError as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 2

    raise AssertionError("unreachable")


def load_config(workspace: Path, config_path: str) -> dict[str, Any]:
    path = (workspace / config_path).resolve()
    if not path.is_file():
        raise PublishError(f"Missing publish config: {path}")
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise PublishError(f"Invalid JSON in publish config: {path}") from exc
    if not isinstance(payload, dict):
        raise PublishError(f"Publish config must be a JSON object: {path}")
    if not payload.get("enabled", False):
        raise PublishError("CurseForge publishing is disabled in the workspace config.")
    for key in ("project_id", "project_slug", "game_version_names", "artifact_globs"):
        if key not in payload:
            raise PublishError(f"Missing required publish config key: {key}")
    return payload


def configured_root_spec_id(config: dict[str, Any]) -> str | None:
    value = config.get("target_root_spec_id")
    return str(value) if value else None


def prepare_bundle(
    workspace: Path,
    config: dict[str, Any],
    *,
    root_spec_id: str,
    require_complete_verdict: bool,
) -> dict[str, Any]:
    verdict = load_verdict(workspace, root_spec_id)
    if require_complete_verdict:
        if verdict is None:
            raise PublishError(f"No Arbiter verdict exists yet for {root_spec_id}.")
        if verdict.get("status") != "complete" or verdict.get("terminal_marker") != "### ARBITER_COMPLETE":
            raise PublishError(f"Arbiter verdict for {root_spec_id} is not complete.")

    target_state = load_target_state(workspace, root_spec_id)
    version = load_mod_version(workspace)
    artifact_path = discover_primary_artifact(workspace, config, version)
    sources_path = discover_sources_artifact(workspace, version)
    jar_sha = sha256_file(artifact_path)
    changelog_text = extract_changelog_text(workspace, config, version)

    next_dir = workspace / str(config.get("next_dir", "millrace-agents/releases/next"))
    next_dir.mkdir(parents=True, exist_ok=True)

    display_name = str(config.get("display_name_template", "{artifact_name}")).format(
        version=version,
        artifact_name=artifact_path.name,
        project_slug=str(config.get("project_slug", "")),
        project_name=str(config.get("project_name", "")),
    )

    manifest = {
        "schema_version": "1.0",
        "kind": "curseforge_release_bundle",
        "root_spec_id": root_spec_id,
        "root_idea_id": target_state.get("root_idea_id") if target_state else None,
        "project_id": int(config["project_id"]),
        "project_slug": str(config["project_slug"]),
        "project_name": str(config.get("project_name", config["project_slug"])),
        "artifact_path": relpath(workspace, artifact_path),
        "sources_artifact_path": relpath(workspace, sources_path) if sources_path else None,
        "display_name": display_name,
        "release_type": str(config.get("release_type", "release")),
        "mark_for_manual_release": bool(config.get("mark_for_manual_release", False)),
        "game_version_names": tuple(str(x) for x in config.get("game_version_names", [])),
        "jar_sha256": jar_sha,
        "version": version,
        "verdict_path": relpath(workspace, verdict_path_for(workspace, root_spec_id)) if verdict else None,
        "report_path": str(verdict.get("report_path")) if verdict else None,
        "prepared_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
    }

    manifest_path = next_dir / "manifest.json"
    changelog_path = next_dir / "changelog.md"
    sha_path = next_dir / "artifact_sha256.txt"
    ready_path = next_dir / "publish_ready.json"

    write_json(manifest_path, manifest)
    changelog_path.write_text(changelog_text.rstrip() + "\n", encoding="utf-8")
    sha_path.write_text(f"{jar_sha}  {artifact_path.name}\n", encoding="utf-8")
    publish_ready = {
        "schema_version": "1.0",
        "kind": "curseforge_publish_ready",
        "root_spec_id": root_spec_id,
        "status": "ready",
        "manifest_path": relpath(workspace, manifest_path),
        "changelog_path": relpath(workspace, changelog_path),
        "artifact_path": relpath(workspace, artifact_path),
        "jar_sha256": jar_sha,
        "prepared_at": manifest["prepared_at"],
    }
    write_json(ready_path, publish_ready)

    manifest["manifest_path"] = relpath(workspace, manifest_path)
    manifest["changelog_path"] = relpath(workspace, changelog_path)
    manifest["publish_ready_path"] = relpath(workspace, ready_path)
    return manifest


def publish_for_root_spec(
    workspace: Path,
    config: dict[str, Any],
    *,
    root_spec_id: str,
    dry_run: bool,
) -> bool:
    bundle = prepare_bundle(
        workspace,
        config,
        root_spec_id=root_spec_id,
        require_complete_verdict=True,
    )
    if receipt_exists(workspace, config, root_spec_id, bundle["jar_sha256"]):
        print(f"already published: {root_spec_id} {bundle['jar_sha256'][:12]}")
        return True

    token = os.environ.get("CURSEFORGE_API_TOKEN", "").strip()
    if not dry_run and not token:
        raise PublishError("CURSEFORGE_API_TOKEN is not set.")

    if dry_run:
        print(f"dry-run would upload: {bundle['artifact_path']} -> project {bundle['project_id']}")
        return True

    game_version_ids = resolve_game_version_ids(
        api_base_url=str(config.get("api_base_url", DEFAULT_API_BASE_URL)),
        token=token,
        names=[str(name) for name in config.get("game_version_names", ())],
    )

    artifact_path = workspace / bundle["artifact_path"]
    changelog_path = workspace / bundle["changelog_path"]
    changelog = changelog_path.read_text(encoding="utf-8")

    metadata = {
        "changelog": changelog,
        "changelogType": "markdown",
        "displayName": bundle["display_name"],
        "gameVersions": game_version_ids,
        "releaseType": bundle["release_type"],
    }
    if bundle["mark_for_manual_release"]:
        metadata["isMarkedForManualRelease"] = True

    response = upload_artifact(
        api_base_url=str(config.get("api_base_url", DEFAULT_API_BASE_URL)),
        token=token,
        project_id=int(bundle["project_id"]),
        artifact_path=artifact_path,
        metadata=metadata,
    )

    receipt = {
        "schema_version": "1.0",
        "kind": "curseforge_publish_receipt",
        "root_spec_id": root_spec_id,
        "project_id": int(bundle["project_id"]),
        "project_slug": bundle["project_slug"],
        "artifact_path": bundle["artifact_path"],
        "jar_sha256": bundle["jar_sha256"],
        "display_name": bundle["display_name"],
        "release_type": bundle["release_type"],
        "game_version_names": bundle["game_version_names"],
        "uploaded_at": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "api_response": response,
    }
    write_json(receipt_path_for(workspace, config, root_spec_id, bundle["jar_sha256"]), receipt)
    print(f"uploaded file id: {response.get('id')}")
    return True


def watch_loop(
    workspace: Path,
    config: dict[str, Any],
    *,
    root_spec_id: str,
    dry_run: bool,
    poll_seconds: int,
) -> int:
    print(f"watching root spec: {root_spec_id}")
    while True:
        try:
            verdict = load_verdict(workspace, root_spec_id)
            if verdict and verdict.get("status") == "complete" and verdict.get("terminal_marker") == "### ARBITER_COMPLETE":
                if publish_for_root_spec(workspace, config, root_spec_id=root_spec_id, dry_run=dry_run):
                    return 0
        except PublishError as exc:
            print(f"watch pending: {exc}")
        time.sleep(max(1, poll_seconds))


def load_verdict(workspace: Path, root_spec_id: str) -> dict[str, Any] | None:
    path = verdict_path_for(workspace, root_spec_id)
    if not path.is_file():
        return None
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise PublishError(f"Invalid verdict payload: {path}")
    return data


def load_target_state(workspace: Path, root_spec_id: str) -> dict[str, Any] | None:
    path = workspace / "millrace-agents" / "arbiter" / "targets" / f"{root_spec_id}.json"
    if not path.is_file():
        return None
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise PublishError(f"Invalid closure target payload: {path}")
    return data


def verdict_path_for(workspace: Path, root_spec_id: str) -> Path:
    return workspace / "millrace-agents" / "arbiter" / "verdicts" / f"{root_spec_id}.json"


def load_mod_version(workspace: Path) -> str:
    props = parse_gradle_properties(workspace / "gradle.properties")
    version = props.get("mod_version")
    if not version:
        raise PublishError("gradle.properties does not define mod_version.")
    return version


def parse_gradle_properties(path: Path) -> dict[str, str]:
    if not path.is_file():
        raise PublishError(f"Missing gradle.properties: {path}")
    result: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip()
    return result


def discover_primary_artifact(workspace: Path, config: dict[str, Any], version: str) -> Path:
    globs = [str(item) for item in config.get("artifact_globs", ())]
    exclude_suffixes = tuple(str(item) for item in config.get("artifact_exclude_suffixes", ()))
    candidates: list[Path] = []
    for pattern in globs:
        candidates.extend(path for path in workspace.glob(pattern) if path.is_file())
    filtered = [
        path
        for path in candidates
        if not any(path.name.endswith(suffix) for suffix in exclude_suffixes)
    ]
    if not filtered:
        raise PublishError("No publishable jar matched the configured artifact globs.")
    filtered.sort(
        key=lambda path: (
            version not in path.name,
            -path.stat().st_mtime,
            path.name,
        )
    )
    return filtered[0]


def discover_sources_artifact(workspace: Path, version: str) -> Path | None:
    matches = sorted(workspace.glob(f"build/libs/*{version}*-sources.jar"))
    return matches[0] if matches else None


def extract_changelog_text(workspace: Path, config: dict[str, Any], version: str) -> str:
    source = workspace / str(config.get("changelog_source", "CHANGELOG.md"))
    if not source.is_file():
        raise PublishError(f"Missing changelog source: {source}")
    text = source.read_text(encoding="utf-8")
    lines = text.splitlines()
    start = None
    for index, line in enumerate(lines):
        if HEADING_RE.match(line) and version in line:
            start = index
            break
    if start is None:
        return text.strip()
    end = len(lines)
    for index in range(start + 1, len(lines)):
        if HEADING_RE.match(lines[index]):
            end = index
            break
    return "\n".join(lines[start:end]).strip()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def resolve_game_version_ids(*, api_base_url: str, token: str, names: list[str]) -> list[int]:
    url = api_base_url.rstrip("/") + "/api/game/versions"
    payload = fetch_json(url, token)
    rows: list[dict[str, Any]]
    if isinstance(payload, list):
        rows = [row for row in payload if isinstance(row, dict)]
    elif isinstance(payload, dict) and isinstance(payload.get("data"), list):
        rows = [row for row in payload["data"] if isinstance(row, dict)]
    else:
        raise PublishError("Unexpected game versions payload shape from CurseForge.")

    type_mapping = fetch_version_type_mapping(api_base_url=api_base_url, token=token)
    indexed: dict[str, list[dict[str, Any]]] = {}
    for row in rows:
        name = row.get("name")
        slug = row.get("slug")
        if isinstance(name, str):
            indexed.setdefault(name.strip().lower(), []).append(row)
        if isinstance(slug, str):
            indexed.setdefault(slug.strip().lower(), []).append(row)

    resolved: list[int] = []
    missing: list[str] = []
    for name in names:
        identifier = resolve_one_game_version_id(
            requested_name=name,
            indexed=indexed,
            type_mapping=type_mapping,
        )
        if identifier is None:
            missing.append(name)
            continue
        resolved.append(identifier)
    if missing:
        raise PublishError(
            "Could not resolve CurseForge game version ids for: " + ", ".join(sorted(missing))
        )
    return resolved


def fetch_version_type_mapping(*, api_base_url: str, token: str) -> dict[int, dict[str, str]]:
    url = api_base_url.rstrip("/") + "/api/game/version-types"
    payload = fetch_json(url, token)
    rows: list[dict[str, Any]]
    if isinstance(payload, list):
        rows = [row for row in payload if isinstance(row, dict)]
    elif isinstance(payload, dict) and isinstance(payload.get("data"), list):
        rows = [row for row in payload["data"] if isinstance(row, dict)]
    else:
        raise PublishError("Unexpected game version types payload shape from CurseForge.")

    mapping: dict[int, dict[str, str]] = {}
    for row in rows:
        identifier = row.get("id")
        if not isinstance(identifier, int):
            continue
        mapping[identifier] = {
            "name": str(row.get("name", "")).strip(),
            "slug": str(row.get("slug", "")).strip(),
        }
    return mapping


def resolve_one_game_version_id(
    *,
    requested_name: str,
    indexed: dict[str, list[dict[str, Any]]],
    type_mapping: dict[int, dict[str, str]],
) -> int | None:
    key = requested_name.strip().lower()
    candidates = indexed.get(key, [])
    if not candidates:
        return None

    valid = [row for row in candidates if isinstance(row.get("id"), int)]
    if not valid:
        return None
    if len(valid) == 1:
        return int(valid[0]["id"])

    exact_slug_match = [row for row in valid if str(row.get("slug", "")).strip().lower() == key]
    if len(exact_slug_match) == 1:
        return int(exact_slug_match[0]["id"])

    preferred_type_slugs = infer_preferred_type_slugs(requested_name)
    if preferred_type_slugs:
        filtered = [
            row
            for row in valid
            if type_mapping.get(int(row.get("gameVersionTypeID", -1)), {}).get("slug", "") in preferred_type_slugs
        ]
        if len(filtered) == 1:
            return int(filtered[0]["id"])
        valid = filtered or valid

    non_snapshot = [
        row
        for row in valid
        if "snapshot" not in str(row.get("name", "")).lower()
        and "snapshot" not in str(row.get("slug", "")).lower()
    ]
    if len(non_snapshot) == 1:
        return int(non_snapshot[0]["id"])

    if len(non_snapshot) > 1:
        preferred_by_api = [row for row in non_snapshot if not str(row.get("apiVersion", "")).strip()]
        if len(preferred_by_api) == 1:
            return int(preferred_by_api[0]["id"])

    unique_ids = sorted({int(row["id"]) for row in valid})
    if len(unique_ids) == 1:
        return unique_ids[0]
    return None


def infer_preferred_type_slugs(requested_name: str) -> set[str]:
    normalized = requested_name.strip().lower()
    if normalized in {"fabric", "forge", "neoforge", "quilt", "rift", "liteloader"}:
        return {"modloader"}
    if re.fullmatch(r"\d+\.\d+(?:\.\d+)?", normalized):
        major_minor = ".".join(normalized.split(".")[:2]).replace(".", "-")
        return {f"minecraft-{major_minor}"}
    return set()


def fetch_json(url: str, token: str) -> Any:
    request = urllib.request.Request(
        url,
        headers={
            "X-Api-Token": token,
            "Accept": "application/json",
            "User-Agent": "millrace-curseforge-publisher/1.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise PublishError(f"CurseForge API error {exc.code} for {url}: {body}") from exc
    except urllib.error.URLError as exc:
        raise PublishError(f"CurseForge API request failed for {url}: {exc}") from exc


def upload_artifact(
    *,
    api_base_url: str,
    token: str,
    project_id: int,
    artifact_path: Path,
    metadata: dict[str, Any],
) -> dict[str, Any]:
    url = api_base_url.rstrip("/") + f"/api/projects/{project_id}/upload-file"
    file_bytes = artifact_path.read_bytes()
    metadata_json = json.dumps(metadata)
    boundary = "----MillraceCurseForge" + uuid.uuid4().hex
    body = build_multipart_body(
        boundary=boundary,
        metadata_json=metadata_json,
        artifact_name=artifact_path.name,
        artifact_bytes=file_bytes,
    )
    request = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "X-Api-Token": token,
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Accept": "application/json",
            "User-Agent": "millrace-curseforge-publisher/1.0",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=120) as response:
            payload = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        body_text = exc.read().decode("utf-8", errors="replace")
        raise PublishError(f"CurseForge upload failed with {exc.code}: {body_text}") from exc
    except urllib.error.URLError as exc:
        raise PublishError(f"CurseForge upload request failed: {exc}") from exc

    if not isinstance(payload, dict):
        raise PublishError("Unexpected upload response payload from CurseForge.")
    return payload


def build_multipart_body(
    *,
    boundary: str,
    metadata_json: str,
    artifact_name: str,
    artifact_bytes: bytes,
) -> bytes:
    parts: list[bytes] = []
    line = lambda text: text.encode("utf-8") + b"\r\n"

    parts.append(line(f"--{boundary}"))
    parts.append(line('Content-Disposition: form-data; name="metadata"'))
    parts.append(line("Content-Type: application/json"))
    parts.append(b"\r\n")
    parts.append(metadata_json.encode("utf-8") + b"\r\n")

    parts.append(line(f"--{boundary}"))
    parts.append(
        line(
            f'Content-Disposition: form-data; name="file"; filename="{artifact_name}"'
        )
    )
    parts.append(line("Content-Type: application/java-archive"))
    parts.append(b"\r\n")
    parts.append(artifact_bytes + b"\r\n")

    parts.append(line(f"--{boundary}--"))
    return b"".join(parts)


def receipt_exists(workspace: Path, config: dict[str, Any], root_spec_id: str, jar_sha256: str) -> bool:
    return receipt_path_for(workspace, config, root_spec_id, jar_sha256).is_file()


def receipt_path_for(workspace: Path, config: dict[str, Any], root_spec_id: str, jar_sha256: str) -> Path:
    published_dir = workspace / str(config.get("published_dir", "millrace-agents/releases/published"))
    published_dir.mkdir(parents=True, exist_ok=True)
    safe_root_spec = root_spec_id.replace("/", "_")
    return published_dir / f"{safe_root_spec}--{jar_sha256[:16]}.json"


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, indent=2, sort_keys=False) + "\n"
    path.write_text(text, encoding="utf-8")


def relpath(workspace: Path, path: Path) -> str:
    return str(path.resolve().relative_to(workspace))


if __name__ == "__main__":
    raise SystemExit(main())
