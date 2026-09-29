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
        self.default_loader = "Fabric"
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
        if self.default_loader == "NeoForge":
            artifact_path = "build/libs/aura-cascade-neoforge-unit-test-version.jar"
            game_versions = ["1.21.1", "NeoForge", "Client", "Server"]
            relations = {"projects": [{"slug": "patchouli", "type": "requiredDependency"}]}
        else:
            artifact_path = "build/libs/aura-beta.jar"
            game_versions = ["1.21.1", "Fabric", "Client", "Server"]
            relations = {
                "projects": [
                    {"slug": "fabric-api", "type": "requiredDependency"},
                    {"slug": "patchouli", "type": "requiredDependency"},
                ]
            }
        manifest: dict[str, object] = {
            "reviewed": True,
            "projectId": 1519395,
            "releaseType": "beta",
            "artifactPath": artifact_path,
            "version": "unit-test-version",
            "expectedJarSHA256": hashlib.sha256(self.artifact.read_bytes()).hexdigest(),
            "displayName": "Aura Cascade test beta",
            "changelog": "Focused uploader test.",
            "gameVersionNames": game_versions,
            "relations": relations,
        }
        if self.default_loader == "NeoForge":
            manifest["loader"] = "NeoForge"
        manifest.update(updates)
        self.manifest_path.write_text(json.dumps(manifest), encoding="utf-8")

    @staticmethod
    def valid_neoforge_metadata(version: str = "unit-test-version") -> str:
        return '''modLoader="javafml"
loaderVersion="[4,)"
license="MIT"

[[mods]]
modId="aura"
version="__VERSION__"
displayName="Aura Cascade"

[[dependencies.aura]]
modId="neoforge"
type="required"
versionRange="[21.1.252,21.2)"
ordering="NONE"
side="BOTH"

[[dependencies.aura]]
modId="minecraft"
type="required"
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"

[[dependencies.aura]]
modId="patchouli"
type="required"
versionRange="[1.21.1-93-NEOFORGE,)"
ordering="AFTER"
side="BOTH"
'''.replace("__VERSION__", version)

    def configure_neoforge(self) -> None:
        self.default_loader = "NeoForge"
        (self.workspace / "src" / "main" / "resources" / "fabric.mod.json").unlink(missing_ok=True)
        source_metadata = self.workspace / "src" / "main" / "resources" / "META-INF" / "neoforge.mods.toml"
        source_metadata.parent.mkdir(parents=True, exist_ok=True)
        source_metadata.write_text(self.valid_neoforge_metadata("${version}"), encoding="utf-8")

        self.artifact = self.workspace / "build" / "libs" / "aura-cascade-neoforge-unit-test-version.jar"
        self.write_neoforge_jar()
        self.write_manifest()

    def write_neoforge_jar(self, metadata: str | None = None, *, helper_entry: str | None = None) -> None:
        with zipfile.ZipFile(self.artifact, "w") as jar:
            jar.writestr(
                "META-INF/neoforge.mods.toml",
                metadata if metadata is not None else self.valid_neoforge_metadata(),
            )
            if helper_entry is not None:
                jar.writestr(helper_entry, b"test-only QA helper")

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

    def test_explicit_neoforge_manifest_uses_offline_validation(self) -> None:
        self.configure_neoforge()
        self.write_manifest(gameVersionNames=["1.21.1", "NeoForge", "Client", "Server", "Java 21"])
        with patch.object(publish_beta, "read_api_token", side_effect=AssertionError("token was read")) as token_reader:
            with patch.object(publish_beta, "upload_artifact", side_effect=AssertionError("network was used")) as upload:
                result, stdout, stderr = self.run_cli()
        self.assertEqual((0, ""), (result, stderr))
        self.assertIn("DRY RUN", stdout)
        token_reader.assert_not_called()
        upload.assert_not_called()

    def test_neoforge_upload_reuses_reviewed_metadata_and_transport(self) -> None:
        self.configure_neoforge()
        with patch.object(publish_beta, "read_api_token", return_value="test-token") as token_reader:
            with patch.object(publish_beta, "upload_artifact", return_value={"id": 8765}) as upload:
                result, stdout, _ = self.run_cli("--upload")
        self.assertEqual(0, result)
        self.assertIn("8765", stdout)
        token_reader.assert_called_once_with()
        upload.assert_called_once()
        request = upload.call_args.kwargs
        self.assertEqual(self.artifact, request["artifact_path"])
        self.assertEqual(["1.21.1", "NeoForge", "Client", "Server"], request["metadata"]["gameVersionNames"])
        self.assertEqual([{"slug": "patchouli", "type": "requiredDependency"}], request["metadata"]["relations"]["projects"])

    def test_neoforge_requires_explicit_supported_loader_and_neoforge_game_versions(self) -> None:
        self.configure_neoforge()
        for updates, expected_error in (
            ({"loader": "neoforge"}, 'loader must be exactly "Fabric" or "NeoForge"'),
            ({"gameVersionNames": ["1.21.1", "Fabric", "Client", "Server"]}, "gameVersionNames must be exactly"),
            ({"gameVersionNames": ["1.21.1", "NeoForge", "Client"]}, "gameVersionNames must be exactly"),
        ):
            with self.subTest(updates=updates):
                self.write_manifest(**updates)
                result, _, stderr = self.run_cli()
                self.assertEqual(2, result)
                self.assertIn(expected_error, stderr)

    def test_neoforge_manifest_version_must_match_gradle_source_and_jar(self) -> None:
        self.configure_neoforge()
        self.write_manifest(version="another-version")
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("gradle.properties mod_version exactly", stderr)

        self.write_manifest()
        source_metadata = self.workspace / "src" / "main" / "resources" / "META-INF" / "neoforge.mods.toml"
        source_metadata.write_text(self.valid_neoforge_metadata("wrong-source-version"), encoding="utf-8")
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("source NeoForge mod version", stderr)

        source_metadata.write_text(self.valid_neoforge_metadata("${version}"), encoding="utf-8")
        self.write_neoforge_jar(self.valid_neoforge_metadata("wrong-artifact-version"))
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("artifact NeoForge mod version", stderr)

    def test_neoforge_rejects_fabric_or_malformed_loader_metadata(self) -> None:
        self.configure_neoforge()
        with zipfile.ZipFile(self.artifact, "w") as jar:
            jar.writestr("fabric.mod.json", json.dumps(self.valid_jar_metadata()))
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("wrong loader", stderr)

        metadata = self.valid_neoforge_metadata().replace('modLoader="javafml"', 'modLoader="kotlinforforge"')
        self.write_neoforge_jar(metadata)
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn('modLoader="javafml"', stderr)

    def test_neoforge_requires_exact_minecraft_and_patchouli_dependencies(self) -> None:
        self.configure_neoforge()
        metadata = self.valid_neoforge_metadata().replace('versionRange="[1.21.1]"', 'versionRange="[1.21.4]"')
        source_metadata = self.workspace / "src" / "main" / "resources" / "META-INF" / "neoforge.mods.toml"
        source_metadata.write_text(metadata, encoding="utf-8")
        self.write_neoforge_jar(metadata)
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("Minecraft 1.21.1 exactly", stderr)

        self.configure_neoforge()
        metadata = self.valid_neoforge_metadata().replace('modId="patchouli"', 'modId="otherbook"')
        source_metadata.write_text(metadata.replace("unit-test-version", "${version}"), encoding="utf-8")
        self.write_neoforge_jar(metadata)
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("missing required dependency patchouli", stderr)

    def test_neoforge_requires_only_patchouli_relation_and_rejects_fabric_dependencies(self) -> None:
        self.configure_neoforge()
        self.write_manifest(relations={"projects": [
            {"slug": "patchouli", "type": "requiredDependency"},
            {"slug": "fabric-api", "type": "optionalDependency"},
        ]})
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("cannot include Fabric API or Team Reborn Energy", stderr)

        self.write_manifest(relations={"projects": [
            {"slug": "patchouli", "type": "optionalDependency"},
        ]})
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("only patchouli as requiredDependency", stderr)

        metadata = self.valid_neoforge_metadata() + '''\n[[dependencies.aura]]
modId="team_reborn_energy"
type="required"
versionRange="*"
ordering="NONE"
side="BOTH"
'''
        source_metadata = self.workspace / "src" / "main" / "resources" / "META-INF" / "neoforge.mods.toml"
        source_metadata.write_text(metadata.replace("unit-test-version", "${version}"), encoding="utf-8")
        self.write_neoforge_jar(metadata)
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("Team Reborn Energy", stderr)

    def test_neoforge_rejects_packaged_qa_helper_entries(self) -> None:
        self.configure_neoforge()
        self.write_neoforge_jar(helper_entry="pixlepix/auracascade/qa/neoforge/AuraQaObserverMod.class")
        self.write_manifest()
        result, _, stderr = self.run_cli()
        self.assertEqual(2, result)
        self.assertIn("must not contain QA helper entries", stderr)


if __name__ == "__main__":
    unittest.main()
