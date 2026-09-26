# Manual Beta Publishing

`scripts/publish_beta.py` is a separate, one-shot uploader. It does not read the
legacy `curseforge_publish.json`, poll Millrace, inspect Arbiter verdicts, or
automatically evaluate the parent release gate. Parent authorization and human
review remain manual prerequisites.

Prepare and review a manifest for the exact jar to publish:

```json
{
  "reviewed": true,
  "projectId": 1519395,
  "releaseType": "beta",
  "artifactPath": "build/libs/aura-cascade-0.2.0+1.21.1.jar",
  "version": "0.2.0+1.21.1",
  "expectedJarSHA256": "<64-hex-sha256-of-the-reviewed-jar>",
  "displayName": "Aura Cascade 0.2.0+1.21.1 beta",
  "changelog": "Reviewed release notes in Markdown.",
  "gameVersionNames": ["1.21.1", "Fabric"],
  "relations": {
    "projects": [
      {"slug": "fabric-api", "type": "requiredDependency"},
      {"slug": "patchouli", "type": "requiredDependency"}
    ]
  }
}
```

Run the default offline validation first:

```powershell
python scripts/publish_beta.py --manifest reviewed-beta.json
```

This validates the manifest, checks the jar SHA-256, and verifies `id`, version,
Minecraft 1.21.1, Fabric API, and Patchouli in both source `fabric.mod.json`
and the root `fabric.mod.json` inside the exact jar. `gameVersionNames` must be
exactly `1.21.1` and `Fabric`, with `Java 21` as the only optional extra. It does not read
`CURSEFORGE_API_TOKEN` or make a network request. Upload is a separate explicit
action after review and parent authorization:

```powershell
python scripts/publish_beta.py --manifest reviewed-beta.json --upload
```

Only the explicit upload path reads `CURSEFORGE_API_TOKEN`; the CLI never opens
`.env.curseforge`. The manifest requires project `1519395`, release type `beta`,
Minecraft `1.21.1`, Fabric, and required relations for Fabric API and Patchouli.
The artifact path must stay inside the repository workspace. A successful upload
is reported only when the API response contains a positive integer file ID.

The multipart fields and relation shape follow the [CurseForge Upload API](https://support.curseforge.com/support/solutions/articles/9000197321).
