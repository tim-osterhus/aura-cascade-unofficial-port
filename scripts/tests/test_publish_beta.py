from __future__ import annotations

import contextlib
import hashlib
import io
import json
import sys
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch

SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

import publish_beta


class PublishBetaTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.workspace = Path(self.temp_dir.name)
        self.artifact = self.workspace / "build" / "libs" / "aura-beta.jar"
        self.artifact.parent.mkdir(parents=True)
        self.write_jar_metadata(self.valid_jar_metadata())
        metadata_path = self.workspace / "src" / "main" / "resources" / "fabric.mod.json"
        metadata_path.parent.mkdir(parents=True)
        metadata_path.write_text(
            json.dumps({
                "id": "aura",
                "version": "${version}",
                "depends": {
                    "minecraft": "1.21.1",
                    "fabric-api": "*",
                    "patchouli": "*",
                },
            }),
            encoding="utf-8",
        )
        (self.workspace / "gradle.properties").write_text("mod_version=unit-test-version\n", encoding="utf-8")
        self.manifest_path = self.workspace / "reviewed-beta.json"
        self.write_manifest()

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    @staticmethod
    def valid_jar_metadata() -> dict[str, object]:
        return {
            "id": "aura",
            "version": "unit-test-version",
            "depends": {
                "minecraft": "1.21.1",
                "fabric-api": "*",
                "patchouli": "*",
            },
        }

    def write_jar_metadata(self, metadata: dict[str, object]) -> None:
        with zipfile.ZipFile(self.artifact, "w") as jar:
            jar.writestr("fabric.mod.json", json.dumps(metadata))

    def write_manifest(self, **updates: object) -> None:
        manifest: dict[str, object] = {
            "reviewed": True,
            "projectId": 1519395,
            "releaseType": "beta",
            "artifactPath": "build/libs/aura-beta.jar",
            "version": "unit-test-version",
            "expectedJarSHA256": hashlib.sha256(self.artifact.read_bytes()).hexdigest(),
            "displayName": "Aura Cascade test beta",
            "changelog": "Focused uploader test.",
            "gameVersionNames": ["1.21.1", "Fabric", "Client", "Server"],
            "relations": {
                "projects": [
                    {"slug": "fabric-api", "type": "requiredDependency"},
                    {"slug": "patchouli", "type": "requiredDependency"},
                ]
            },
        }
        manifest.update(updates)
        self.manifest_path.write_text(json.dumps(manifest), encoding="utf-8")

    def run_cli(self, *arguments: str) -> tuple[int, str, str]:
        stdout = io.StringIO()
        stderr = io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            result = publish_beta.main([
                "--workspace", str(self.workspace),
                "--manifest", str(self.manifest_path),
                *arguments,
            ])
        return result, stdout.getvalue(), stderr.getvalue()

    def test_default_is_offline_dry_run_without_reading_token(self) -> None:
        with patch.object(publish_beta, "read_api_token", side_effect=AssertionError("token was read")) as token_reader:
            with patch.object(publish_beta, "upload_artifact", side_effect=AssertionError("network was used")) as upload:
                result, stdout, _ = self.run_cli()
        self.assertEqual(0, result)
        self.assertIn("DRY RUN", stdout)
        token_reader.assert_not_called()
        upload.assert_not_called()

    def test_upload_requires_explicit_flag_and_sends_reviewed_beta_metadata(self) -> None:
        response = {"id": 9876}
        with patch.object(publish_beta, "read_api_token", return_value="test-token") as token_reader:
            with patch.object(publish_beta, "upload_artifact", return_value=response) as upload:
                result, stdout, _ = self.run_cli("--upload")
        self.assertEqual(0, result)
        self.assertIn("9876", stdout)
        token_reader.assert_called_once_with()
        upload.assert_called_once()
        request = upload.call_args.kwargs
        self.assertEqual(1519395, request["project_id"])
        self.assertEqual("test-token", request["token"])
        self.assertEqual("beta", request["metadata"]["releaseType"])
        self.assertEqual(["1.21.1", "Fabric", "Client", "Server"], request["metadata"]["gameVersionNames"])
        self.assertEqual(2, len(request["metadata"]["relations"]["projects"]))

    def test_rejects_artifact_hash_mismatch_before_token_or_network(self) -> None:
        self.write_manifest(expectedJarSHA256="0" * 64)
        with patch.object(publish_beta, "read_api_token", side_effect=AssertionError("token was read")) as token_reader:
            with patch.object(publish_beta, "upload_artifact", side_effect=AssertionError("network was used")) as upload:
                result, _, stderr = self.run_cli("--upload")
        self.assertEqual(2, result)
        self.assertIn("SHA-256 mismatch", stderr)
        token_reader.assert_not_called()
        upload.assert_not_called()

    def test_rejects_actual_jar_metadata_mismatches_with_matching_hash(self) -> None:
        variants = (
            ({**self.valid_jar_metadata(), "id": "other"}, "id must be aura"),
            ({**self.valid_jar_metadata(), "version": "older-test-version"}, "artifact fabric.mod.json version"),
            ({**self.valid_jar_metadata(), "depends": {"minecraft": "1.21.11", "fabric-api": "*", "patchouli": "*"}}, "Minecraft 1.21.1 exactly"),
            ({**self.valid_jar_metadata(), "depends": {"minecraft": "1.21.1", "patchouli": "*"}}, "missing required dependency fabric-api"),
            ({**self.valid_jar_metadata(), "depends": {"minecraft": "1.21.1", "fabric-api": "*"}}, "missing required dependency patchouli"),
        )
        for metadata, expected_error in variants:
            with self.subTest(expected_error=expected_error):
                self.write_jar_metadata(metadata)
                self.write_manifest()
                result, _, stderr = self.run_cli()
                self.assertEqual(2, result)
                self.assertIn(expected_error, stderr)

    def test_game_version_names_are_exact_with_optional_java_21(self) -> None:
        self.write_manifest(gameVersionNames=["1.21.1", "Fabric", "Client", "Server", "Java 21"])
        result, _, _ = self.run_cli()
        self.assertEqual(0, result)

        self.write_manifest(gameVersionNames=["1.21.1", "1.21.11", "Fabric"])
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("gameVersionNames must be exactly", stderr)

    def test_rejects_source_version_or_minecraft_mismatch(self) -> None:
        self.write_manifest(version="wrong-test-version")
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("source fabric.mod.json version", stderr)

        metadata_path = self.workspace / "src" / "main" / "resources" / "fabric.mod.json"
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        metadata["depends"]["minecraft"] = "1.21.4"
        metadata_path.write_text(json.dumps(metadata), encoding="utf-8")
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("Minecraft 1.21.1 exactly", stderr)

    def test_requires_both_validated_environments_before_upload(self) -> None:
        for versions in (
            ["1.21.1", "Fabric"],
            ["1.21.1", "Fabric", "Client"],
            ["1.21.1", "Fabric", "Server"],
            ["1.21.1", "Fabric", "Client", "Server", "Server"],
        ):
            with self.subTest(versions=versions):
                self.write_manifest(gameVersionNames=versions)
                with patch.object(publish_beta, "read_api_token", side_effect=AssertionError("token was read")):
                    with patch.object(publish_beta, "upload_artifact", side_effect=AssertionError("network was used")):
                        result, _, stderr = self.run_cli("--upload")
                self.assertEqual(2, result)
                self.assertIn("gameVersionNames must be exactly", stderr)

    def test_requires_fabric_api_and_patchouli_required_relations(self) -> None:
        self.write_manifest(relations={"projects": [{"slug": "fabric-api", "type": "optionalDependency"}]})
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("patchouli", stderr)

    def test_upload_response_must_contain_a_positive_integer_id(self) -> None:
        for response in ({}, {"id": 0}, {"id": True}, {"id": "9876"}):
            with self.subTest(response=response):
                with patch.object(publish_beta, "read_api_token", return_value="fake-token"):
                    with patch.object(publish_beta, "upload_artifact", return_value=response):
                        result, _, stderr = self.run_cli("--upload")
                self.assertEqual(2, result)
                self.assertIn("positive integer file id", stderr)

    def test_upload_errors_redact_the_token_before_reporting(self) -> None:
        token = "fake-token-that-must-not-leak"
        with patch.object(publish_beta, "read_api_token", return_value=token):
            with patch.object(
                publish_beta,
                "upload_artifact",
                side_effect=publish_beta.PublishError(f"simulated response echo: {token}"),
            ):
                result, _, stderr = self.run_cli("--upload")
        self.assertEqual(2, result)
        self.assertNotIn(token, stderr)
        self.assertIn("[redacted]", stderr)


if __name__ == "__main__":
    unittest.main()
