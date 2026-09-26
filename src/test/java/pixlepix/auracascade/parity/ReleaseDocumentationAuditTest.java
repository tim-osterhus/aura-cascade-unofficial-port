package pixlepix.auracascade.parity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ReleaseDocumentationAuditTest {
    @Test
    void metadataTargetsOnlyTheBranchMinecraftVersion() throws IOException {
        var metadata = JsonParser.parseString(Files.readString(
            Path.of("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("1.21.11", metadata.getAsJsonObject("depends").get("minecraft").getAsString());
    }

    private static final String STALE_SERVER_RUN_ID = "run-3389d91e5f0a4f5e802d8023f25c209c";
    private static final String STALE_RELEASE_JAR_SIZE = "1314747";
    private static final String STALE_RELEASE_JAR_SHA256 = "c93549acb6a5c71eaa0c557d514fbe26fecfca845a0b65be40cfb522ff9efb71";
    private static final String STALE_SOURCES_JAR_SIZE = "1113027";
    private static final String STALE_SOURCES_JAR_SHA256 = "f080c1adfa3c9c88a0cd85e921ebc882b2f25dfa32b03ecfd6a22a52b21a23c2";
    private static final Path RELEASE_JAR = Path.of("build/libs/aura-cascade-0.1.1+1.21.1.jar");
    private static final Path SOURCES_JAR = Path.of("build/libs/aura-cascade-0.1.1+1.21.1-sources.jar");

    @Test
    void releaseDocsCarryAttributionLicenseGuidanceAndParityAuditMarkers() throws IOException {
        String readme = Files.readString(Path.of("README.md"), StandardCharsets.UTF_8);
        String changelog = Files.readString(Path.of("CHANGELOG.md"), StandardCharsets.UTF_8);
        String license = Files.readString(Path.of("LICENSE"), StandardCharsets.UTF_8);
        String portingNotes = Files.readString(Path.of("PORTING_NOTES.md"), StandardCharsets.UTF_8);
        String fabricMod = Files.readString(Path.of("src/main/resources/fabric.mod.json"), StandardCharsets.UTF_8);

        assertAll(
            () -> assertTrue(readme.contains("unofficial modern Fabric port")),
            () -> assertTrue(readme.contains("pixlepix")),
            () -> assertTrue(readme.contains("williewillus")),
            () -> assertTrue(readme.contains("./gradlew --console=plain test")),
            () -> assertTrue(readme.contains("./gradlew --console=plain build")),
            () -> assertTrue(readme.contains("./gradlew --console=plain runServer")),
            () -> assertTrue(readme.contains("LICENSE")),
            () -> assertTrue(readme.contains("## Final Client Audit Snapshot")),
            () -> assertTrue(readme.contains("### Implemented Parity")),
            () -> assertTrue(readme.contains("### Intentional Modern Substitutions")),
            () -> assertTrue(readme.contains("### Shipped Runtime Bounds")),
            () -> assertTrue(readme.contains("direct crafting covers the node/control, pump, travel, rebound, miner, Bookshelf Coordinator, and End-ritual ids")),
            () -> assertTrue(readme.contains("fortified, looter, spawner, and Nether-ritual content use the documented processor or four-pedestal vortex seams")),
            () -> assertTrue(readme.contains("Aura creative-tab discoverability now matches the shipped evidence")),
            () -> assertTrue(readme.contains("`storage_bookshelf` stays a conversion-only block")),
            () -> assertTrue(readme.contains("using a Storage Book on a vanilla bookshelf")),
            () -> assertTrue(readme.contains("intentionally creative-only item-form block")),
            () -> assertTrue(readme.contains("no Aura `Parsing error loading recipe` or Fabric dependency failures")),
            () -> assertTrue(readme.contains("current repo-owned client-facing evidence")),
            () -> assertTrue(readme.contains("localized `Monitor` item naming surface")),
            () -> assertTrue(readme.contains("VisualAssetParityAuditTest")),
            () -> assertTrue(readme.contains("that blast to spare non-terrain blocks; the ring does not prevent wearer damage")),
            () -> assertTrue(readme.contains("Prismatic Wand copy records a live source region and its offset from the player.")),
            () -> assertTrue(readme.contains("charges survival materials only after successful placement")),
            () -> assertTrue(readme.contains("The Kaleidoscopic Enchanter now maps the recovered single-color table and pairwise matrix through modern runtime seams instead of the old ignite/knockback/mining-efficiency/poison/damage/nausea substitute set.")),
            () -> assertTrue(readme.contains("2026-09-23-kaleidoscopic-e1-contract.md")),
            () -> assertTrue(readme.contains("incoming damage reduction before hurt")),
            () -> assertTrue(license.contains("MIT License")),
            () -> assertTrue(license.contains("pixlepix")),
            () -> assertTrue(license.contains("williewillus")),
            () -> assertTrue(license.contains("visual asset imports derived from `AuraCascade-592.jar!/assets/aura/textures/**`")),
            () -> assertTrue(changelog.contains("## Beta `0.1.1+1.21.1`")),
            () -> assertTrue(changelog.contains("Restored Ring of the Shattered Stone loose-block filtering")),
            () -> assertTrue(changelog.contains("Restored Ring of the Shattered Stone loose-block filtering, Prismatic Wand clipboard closure for representative block entities and source fluids, and the kaleidoscopic legacy matrix through modern runtime seams.")),
            () -> assertTrue(changelog.contains("build/libs/aura-cascade-0.1.1+1.21.1.jar")),
            () -> assertTrue(changelog.contains("./gradlew --console=plain runServer")),
            () -> assertTrue(changelog.contains("## Crafting And Acquisition Surface")),
            () -> assertTrue(changelog.contains("Direct crafting now loads on `1.21.1` for the node/control, pump, travel, rebound, miner, `bookshelf_coordinator`, and End-ritual block ids")),
            () -> assertTrue(changelog.contains("`fortified_planks` and the other fortified containment blocks stay on the documented processor seam")),
            () -> assertTrue(changelog.contains("`consumer_block_loot`, `consumer_block_spawn`, and `ritual_nether` stay on the documented four-pedestal vortex seam instead of silent bench recipes.")),
            () -> assertTrue(changelog.contains("`storage_bookshelf` remains a conversion-only block")),
            () -> assertTrue(changelog.contains("`aura_node_pump_creative` remains intentionally creative-only with no survival recipe.")),
            () -> assertTrue(changelog.contains("no longer logs Aura `Parsing error loading recipe` entries")),
            () -> assertTrue(changelog.contains("## Shipped Runtime Bounds")),
            () -> assertTrue(portingNotes.contains("## Final Parity Audit")),
            () -> assertTrue(portingNotes.contains("### Representative Client-Facing Evidence")),
            () -> assertTrue(portingNotes.contains("Discoverability and conversion")),
            () -> assertTrue(portingNotes.contains("aura_node_pump_creative` remains intentionally creative-only content rather than a missing survival recipe")),
            () -> assertTrue(portingNotes.contains("bookshelf_coordinator")),
            () -> assertTrue(portingNotes.contains("src/main/resources/data/aura/recipes/processor/")),
            () -> assertTrue(portingNotes.contains("visual parity pass now imports the shipped `AuraCascade-592.jar!/assets/aura/textures/**` payload")),
            () -> assertTrue(portingNotes.contains("remaining `minecraft:` references on that restored client surface are limited to structural parent and item-definition type ids")),
            () -> assertTrue(portingNotes.contains("workspace does not retain the earlier `live_client_validation/` replay archive")),
            () -> assertTrue(portingNotes.contains("localized `Monitor` item naming surface")),
            () -> assertTrue(portingNotes.contains("repo-owned asset and audit surface")),
            () -> assertTrue(portingNotes.contains("### Shipped Runtime Bounds")),
            () -> assertTrue(portingNotes.contains("filters nearby dirt, stone, sand, and gravel out of explosion block damage")),
            () -> assertTrue(portingNotes.contains("captures non-air block states, source fluids, and representative block-entity data")),
            () -> assertTrue(portingNotes.contains("Red = temporary Silk Touch mining override")),
            () -> assertTrue(portingNotes.contains("destroy speed >= 8.0")),
            () -> assertTrue(portingNotes.contains("VisualAssetParityAuditTest")),
            () -> assertTrue(portingNotes.contains("## Dedicated Server Validation")),
            () -> assertTrue(portingNotes.contains("## Release Validation")),
            () -> assertTrue(portingNotes.contains("`run/logs/latest.log` reached `Loading Minecraft 1.21.1`")),
            () -> assertTrue(portingNotes.contains("`Done (...)! For help, type \"help\"`")),
            () -> assertTrue(portingNotes.contains("no longer reports Aura `Parsing error loading recipe` entries")),
            () -> assertTrue(portingNotes.contains("interrupted intentionally with `SIGINT`")),
            () -> assertTrue(portingNotes.contains("repo-local command exits `130` after the intentional interrupt")),
            () -> assertTrue(portingNotes.contains("current dedicated-server validation evidence for the `0.1.1+1.21.1` release surface")),
            () -> assertTrue(portingNotes.contains("./gradlew --console=plain test --tests pixlepix.auracascade.parity.ReleaseDocumentationAuditTest")),
            () -> assertTrue(portingNotes.contains("./gradlew --console=plain test --tests pixlepix.auracascade.parity.FinalClientParityAuditTest")),
            () -> assertTrue(portingNotes.contains("./gradlew --console=plain test")),
            () -> assertTrue(portingNotes.contains("Historical artifact records")),
            () -> assertTrue(hasArtifactRecord(portingNotes, "Built release artifact", RELEASE_JAR)),
            () -> assertTrue(hasArtifactRecord(portingNotes, "Built sources artifact", SOURCES_JAR)),
            () -> assertTrue(readme.contains("fabric/1.21.11")),
            () -> assertTrue(readme.contains("historical 1.21.1 evidence, not target-version passes")),
            () -> assertTrue(changelog.contains("0.2.1+1.21.11")),
            () -> assertFalse(readme.contains("repo-local audit evidence")),
            () -> assertFalse(readme.contains("archived live-client replay")),
            () -> assertFalse(readme.contains("### Remaining Unresolved Gaps")),
            () -> assertFalse(readme.contains("Ring of the Shattered Stone still lacks the legacy loose-block explosion filter.")),
            () -> assertFalse(readme.contains("still skips block entities and fluids")),
            () -> assertFalse(readme.contains("still uses the bounded modern substitution set")),
            () -> assertFalse(portingNotes.contains("No deterministic headless Minecraft client harness is shipped")),
            () -> assertFalse(portingNotes.contains("run-76955964dbb9474f848a0dfb36ae5bd5/live_client_validation/")),
            () -> assertFalse(portingNotes.contains("### Unresolved Gaps")),
            () -> assertFalse(portingNotes.contains("still does not limit nearby explosion block damage")),
            () -> assertFalse(portingNotes.contains("still skips block entities and fluids")),
            () -> assertTrue(portingNotes.contains("1.21.11 Forward Port")),
            () -> assertFalse(portingNotes.contains("contains `180` `Parsing error loading recipe` entries")),
            () -> assertFalse(portingNotes.contains("Dedicated-server smoke was not re-run")),
            () -> assertFalse(portingNotes.contains("remains outstanding for the follow-up release-truthfulness pass")),
            () -> assertFalse(portingNotes.contains("compileJava compileTestJava")),
            () -> assertFalse(portingNotes.contains("Red = ignite")),
            () -> assertFalse(portingNotes.contains("run-4bed701ec1bd4bd48012512ba82d3e01")),
            () -> assertFalse(portingNotes.contains(STALE_SERVER_RUN_ID)),
            () -> assertFalse(portingNotes.contains(STALE_RELEASE_JAR_SIZE)),
            () -> assertFalse(portingNotes.contains(STALE_RELEASE_JAR_SHA256)),
            () -> assertFalse(portingNotes.contains(STALE_SOURCES_JAR_SIZE)),
            () -> assertFalse(portingNotes.contains(STALE_SOURCES_JAR_SHA256)),
            () -> assertTrue(fabricMod.contains("\"license\": \"MIT\"")),
            () -> assertFalse(fabricMod.contains("LicenseRef-Provenance-Pending"))
        );
    }

    private static boolean hasArtifactRecord(String notes, String label, Path path) {
        // Historical evidence must not depend on a previous local build's output.
        String prefix = label + ": `" + path.toString().replace('\\', '/') + "` (`";
        return Pattern.compile(Pattern.quote(prefix) + "[1-9][0-9]*` bytes, SHA-256 `[0-9a-f]{64}`\\)\\.")
            .matcher(notes).find();
    }
}
