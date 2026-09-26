package pixlepix.auracascade.aura;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraInspectionText {
    private static final int TEXT_COLOR = 0xF0F0F0;
    private static final int MUTED_COLOR = 0xB8B8B8;
    private static final int BLACK_AURA_HUD_COLOR = 0xD9D9D9;
    private static final int BLUE_AURA_HUD_COLOR = 0x80BFFF;

    private AuraInspectionText() {
    }

    public static List<Line> format(AuraInspectionState inspection, Optional<PumpReadout> pumpReadout) {
        return format(inspection, pumpReadout, Set.of());
    }

    public static List<Line> format(AuraInspectionState inspection, Optional<PumpReadout> pumpReadout,
                                  Set<AuraColor> visibleColors) {
        Objects.requireNonNull(inspection, "inspection");
        Objects.requireNonNull(pumpReadout, "pumpReadout");

        List<Line> lines = new ArrayList<>();
        if (inspection.totalAura() == 0 && visibleColors.isEmpty()) {
            lines.add(new Line("text.aura.hud.no_aura", List.of(), MUTED_COLOR));
        } else {
            for (AuraColor color : AuraColor.values()) {
                int amount = inspection.storage().get(color);
                if (amount > 0 || visibleColors.contains(color)) {
                    lines.add(new Line(
                        "text.aura.hud.color." + color.id(),
                        List.of(amount),
                        hudColor(color)
                    ));
                }
            }
        }

        lines.add(new Line("text.aura.hud.stored_power", List.of(inspection.storedPower()), TEXT_COLOR));
        pumpReadout.ifPresent(pump -> {
            lines.add(new Line("text.aura.hud.pump.run_time", List.of(pump.runTimeSeconds()), TEXT_COLOR));
            lines.add(new Line("text.aura.hud.pump.power", List.of(pump.powerPerSecond()), TEXT_COLOR));
            lines.add(new Line(
                pump.fueled() ? "text.aura.hud.pump.fueled" : "text.aura.hud.pump.no_fuel",
                List.of(),
                pump.fueled() ? 0x78D9A3 : MUTED_COLOR
            ));
            if (pump.redstoneInhibited()) {
                lines.add(new Line("text.aura.hud.pump.redstone_inhibited", List.of(), 0xFFB36B));
            }
        });
        return List.copyOf(lines);
    }

    private static int hudColor(AuraColor color) {
        return switch (color) {
            case BLACK -> BLACK_AURA_HUD_COLOR;
            case BLUE -> BLUE_AURA_HUD_COLOR;
            default -> AuraPalette.rgb(color);
        };
    }

    public record Line(String translationKey, List<Object> arguments, int color) {
        public Line {
            Objects.requireNonNull(translationKey, "translationKey");
            arguments = List.copyOf(arguments);
        }
    }

    public record PumpReadout(int runTimeSeconds, int powerPerSecond, boolean fueled, boolean redstoneInhibited) {
        public PumpReadout {
            runTimeSeconds = Math.max(0, runTimeSeconds);
            powerPerSecond = Math.max(0, powerPerSecond);
        }
    }
}
