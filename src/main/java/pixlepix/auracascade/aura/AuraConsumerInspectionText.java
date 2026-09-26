package pixlepix.auracascade.aura;

import java.util.List;
import java.util.Objects;

public final class AuraConsumerInspectionText {
    private static final int TEXT_COLOR = 0xF0F0F0;

    private AuraConsumerInspectionText() {
    }

    public static List<Line> format(AuraConsumerInspectionState inspection) {
        Objects.requireNonNull(inspection, "inspection");
        return List.of(
            new Line("text.aura.hud.consumer.progress", List.of(inspection.progress(), inspection.maxProgress()), TEXT_COLOR),
            new Line("text.aura.hud.consumer.power_per_progress", List.of(inspection.requiredPower()), TEXT_COLOR),
            new Line("text.aura.hud.consumer.last_power", List.of(inspection.lastReceivedPower()), TEXT_COLOR),
            new Line("text.aura.hud.consumer.stored_power", List.of(inspection.storedPower()), TEXT_COLOR)
        );
    }

    public record Line(String translationKey, List<Object> arguments, int color) {
        public Line {
            Objects.requireNonNull(translationKey, "translationKey");
            arguments = List.copyOf(arguments);
        }
    }
}
