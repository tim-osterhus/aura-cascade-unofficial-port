package pixlepix.auracascade.aura;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class AuraInspectionTextTest {
    private static final Path LANGUAGE_PATH = Path.of("src/main/resources/assets/aura/lang/en_us.json");
    private static final Pattern STRING_ARGUMENT = Pattern.compile("%s");

    @Test
    void formatsPresentAuraColorsAndKeepsStoredPowerSeparate() {
        AuraStorage storage = new AuraStorage();
        storage.set(AuraColor.WHITE, 12);
        storage.set(AuraColor.BLACK, 0);
        storage.set(AuraColor.VIOLET, 4);
        AuraInspectionState inspection = new AuraInspectionState(storage, 2, 31, true);

        List<AuraInspectionText.Line> lines = AuraInspectionText.format(inspection, Optional.empty());

        assertEquals(List.of(
            "text.aura.hud.color.white",
            "text.aura.hud.color.violet",
            "text.aura.hud.stored_power"
        ), lines.stream().map(AuraInspectionText.Line::translationKey).toList());
        assertEquals(List.of(12), lines.get(0).arguments());
        assertEquals(List.of(4), lines.get(1).arguments());
        assertEquals(List.of(31), lines.get(2).arguments());
        assertEquals(0xD9D9D9, AuraInspectionText.format(
            new AuraInspectionState(AuraStorage.of(AuraColor.BLACK, 1), 0, 0, true), Optional.empty()
        ).get(0).color());
        assertEquals(0x80BFFF, AuraInspectionText.format(
            new AuraInspectionState(AuraStorage.of(AuraColor.BLUE, 1), 0, 0, true), Optional.empty()
        ).get(0).color());
    }

    @Test
    void reportsEmptyAuraFuelReserveAndRedstoneInhibitionWithoutClaimingTransfer() {
        AuraInspectionState inspection = new AuraInspectionState(new AuraStorage(), 0, 9, true);
        AuraInspectionText.PumpReadout pump = new AuraInspectionText.PumpReadout(42, 18, true, true);

        List<AuraInspectionText.Line> lines = AuraInspectionText.format(inspection, Optional.of(pump));

        assertEquals(List.of(
            "text.aura.hud.no_aura",
            "text.aura.hud.stored_power",
            "text.aura.hud.pump.run_time",
            "text.aura.hud.pump.power",
            "text.aura.hud.pump.fueled",
            "text.aura.hud.pump.redstone_inhibited"
        ), lines.stream().map(AuraInspectionText.Line::translationKey).toList());
        assertEquals(List.of(9), lines.get(1).arguments());
        assertEquals(List.of(42), lines.get(2).arguments());
        assertEquals(List.of(18), lines.get(3).arguments());

        List<AuraInspectionText.Line> emptyPumpLines = AuraInspectionText.format(
            inspection,
            Optional.of(new AuraInspectionText.PumpReadout(0, 0, false, false))
        );
        assertEquals("text.aura.hud.pump.no_fuel", emptyPumpLines.get(4).translationKey());
    }

    @Test
    void observedColorRowsRemainVisibleAtZeroWithoutShowingStaleAmounts() {
        Set<AuraColor> visibleColors = Set.of(AuraColor.WHITE, AuraColor.BLUE);
        for (int amount : new int[] {12, 0, 7, 0}) {
            var inspection = new AuraInspectionState(AuraStorage.of(AuraColor.WHITE, amount), 1, 0, true);
            var lines = AuraInspectionText.format(inspection, Optional.empty(), visibleColors);
            assertEquals(List.of("text.aura.hud.color.white", "text.aura.hud.color.blue",
                "text.aura.hud.stored_power"), lines.stream().map(AuraInspectionText.Line::translationKey).toList());
            assertEquals(List.of(amount), lines.get(0).arguments());
            assertEquals(List.of(0), lines.get(1).arguments());
        }
        var empty = new AuraInspectionState(new AuraStorage(), 0, 0, true);
        assertEquals("text.aura.hud.no_aura", AuraInspectionText.format(empty, Optional.empty(), Set.of())
            .get(0).translationKey());
    }

    @Test
    void everyProducedHudLineHasAnEnglishTranslationAndMatchingArguments() throws IOException {
        AuraStorage storage = new AuraStorage();
        for (AuraColor color : AuraColor.values()) {
            storage.set(color, color.ordinal() + 1);
        }
        AuraInspectionState inspection = new AuraInspectionState(storage, 0, 7, true);
        List<AuraInspectionText.Line> lines = AuraInspectionText.format(
            inspection,
            Optional.of(new AuraInspectionText.PumpReadout(3, 10, true, true))
        );
        JsonObject language = JsonParser.parseString(Files.readString(LANGUAGE_PATH, StandardCharsets.UTF_8))
            .getAsJsonObject();

        for (AuraInspectionText.Line line : lines) {
            assertNotNull(language.get(line.translationKey()), "Missing translation: " + line.translationKey());
            String translated = language.get(line.translationKey()).getAsString();
            Matcher arguments = STRING_ARGUMENT.matcher(translated);
            assertEquals(line.arguments().size(), arguments.results().count(), line.translationKey());
        }
    }
}
