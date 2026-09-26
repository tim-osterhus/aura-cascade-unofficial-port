package pixlepix.auracascade.aura;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AuraConsumerInspectionTextTest {
    @Test
    void formatsLegacyProgressPowerSnapshotAndCurrentStoredPowerSeparately() {
        AuraConsumerInspectionState inspection = new AuraConsumerInspectionState(51, 60, 150, 33_789, 12_345);

        List<AuraConsumerInspectionText.Line> lines = AuraConsumerInspectionText.format(inspection);

        assertEquals(List.of(
            "text.aura.hud.consumer.progress",
            "text.aura.hud.consumer.power_per_progress",
            "text.aura.hud.consumer.last_power",
            "text.aura.hud.consumer.stored_power"
        ), lines.stream().map(AuraConsumerInspectionText.Line::translationKey).toList());
        assertEquals(List.of(51, 60), lines.get(0).arguments());
        assertEquals(List.of(150), lines.get(1).arguments());
        assertEquals(List.of(33_789), lines.get(2).arguments());
        assertEquals(List.of(12_345), lines.get(3).arguments());
    }
}
