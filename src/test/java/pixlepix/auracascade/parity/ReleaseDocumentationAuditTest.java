package pixlepix.auracascade.parity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleaseDocumentationAuditTest {
    @Test
    void releaseDocsCarryAttributionLicenseGuidanceAndParityAuditMarkers() throws IOException {
        String readme = Files.readString(Path.of("README.md"), StandardCharsets.UTF_8);
        String license = Files.readString(Path.of("LICENSE"), StandardCharsets.UTF_8);
        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);
        String fabricMod = Files.readString(Path.of("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);

        assertAll(
            () -> assertTrue(readme.contains("unofficial modern Fabric port")),
            () -> assertTrue(readme.contains("pixlepix")),
            () -> assertTrue(readme.contains("williewillus")),
            () -> assertTrue(readme.contains("./gradlew test")),
            () -> assertTrue(readme.contains("./gradlew build")),
            () -> assertTrue(readme.contains("./gradlew runServer")),
            () -> assertTrue(readme.contains("LICENSE")),
            () -> assertTrue(readme.contains("## Final Client Audit Snapshot")),
            () -> assertTrue(readme.contains("### Implemented Parity")),
            () -> assertTrue(readme.contains("### Intentional Modern Substitutions")),
            () -> assertTrue(readme.contains("### Remaining Unresolved Gaps")),
            () -> assertTrue(readme.contains("Aura creative-tab discoverability now matches the shipped evidence")),
            () -> assertTrue(readme.contains("`storage_bookshelf` stays a conversion-only block")),
            () -> assertTrue(readme.contains("using a Storage Book on a vanilla bookshelf")),
            () -> assertTrue(readme.contains("intentionally creative-only item-form block")),
            () -> assertTrue(readme.contains("fresh archived live-client replay")),
            () -> assertTrue(readme.contains("sampled `Monitor` item resolves through the localized block name")),
            () -> assertTrue(readme.contains("stays free of Aura-side missing-texture, failed-model, missing-model, and texture-metadata-parse warnings")),
            () -> assertTrue(readme.contains("Ring of the Shattered Stone still lacks the legacy loose-block explosion filter.")),
            () -> assertTrue(readme.contains("Prismatic Wand copy and paste still skips block entities and fluids and consumes direct placement items rather than the full legacy drop-plus-metadata path.")),
            () -> assertTrue(readme.contains("The recovered kaleidoscopic single-color table and fifteen pairwise interactions are now documented exactly, but the live runtime still uses the bounded modern substitution set instead of those recovered legacy effects.")),
            () -> assertTrue(license.contains("MIT License")),
            () -> assertTrue(license.contains("pixlepix")),
            () -> assertTrue(license.contains("williewillus")),
            () -> assertTrue(license.contains("visual asset imports derived from `AuraCascade-592.jar!/assets/aura/textures/**`")),
            () -> assertTrue(portingNotes.contains("## Final Parity Audit")),
            () -> assertTrue(portingNotes.contains("### Representative Client-Facing Evidence")),
            () -> assertTrue(portingNotes.contains("Discoverability and conversion")),
            () -> assertTrue(portingNotes.contains("aura_node_pump_creative` remains intentionally creative-only content rather than a missing survival recipe")),
            () -> assertTrue(portingNotes.contains("visual parity pass now imports the shipped `AuraCascade-592.jar!/assets/aura/textures/**` payload")),
            () -> assertTrue(portingNotes.contains("remaining `minecraft:` references on that restored client surface are limited to structural parent and item-definition type ids")),
            () -> assertTrue(portingNotes.contains("fresh live-client evidence for this closure slice is archived under `millrace-agents/runs/run-76955964dbb9474f848a0dfb36ae5bd5/live_client_validation/`")),
            () -> assertTrue(portingNotes.contains("localized `Monitor` replacement text")),
            () -> assertTrue(portingNotes.contains("no Aura-side `Missing textures in model`, `Unable to parse metadata from aura:`, failed-model, missing-model, or `item.aura.monitor` log hits")),
            () -> assertTrue(portingNotes.contains("Ring of the Shattered Stone still does not limit nearby explosion block damage to dirt, stone, sand, and gravel.")),
            () -> assertTrue(portingNotes.contains("Prismatic Wand copy-paste still skips block entities and fluids and consumes direct placement items rather than the full legacy drop-plus-metadata path.")),
            () -> assertTrue(portingNotes.contains("The current runtime still uses the bounded substitution set `Red = ignite`, `Orange = knockback`, `Yellow = mining efficiency`, `Green = poison`, `Blue = bonus damage`, and `Violet = nausea` instead of those recovered effects.")),
            () -> assertTrue(portingNotes.contains("VisualAssetParityAuditTest")),
            () -> assertTrue(portingNotes.contains("## Dedicated Server Validation")),
            () -> assertTrue(portingNotes.contains("## Release Validation")),
            () -> assertFalse(readme.contains("repo-local audit evidence")),
            () -> assertFalse(portingNotes.contains("No deterministic headless Minecraft client harness is shipped")),
            () -> assertTrue(fabricMod.contains("\"license\": \"MIT\"")),
            () -> assertFalse(fabricMod.contains("LicenseRef-Provenance-Pending"))
        );
    }
}
