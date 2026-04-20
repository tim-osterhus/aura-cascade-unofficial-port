package pixlepix.auracascade.enchantment;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import pixlepix.auracascade.parity.AuraColor;

public final class KaleidoscopicLegacyContract {
    private static final List<AuraColor> COLOR_ORDER = List.of(
        AuraColor.RED,
        AuraColor.ORANGE,
        AuraColor.YELLOW,
        AuraColor.GREEN,
        AuraColor.BLUE,
        AuraColor.VIOLET
    );

    private static final EnumMap<AuraColor, String> LEGACY_BASIC_EFFECTS = new EnumMap<>(AuraColor.class);
    private static final EnumMap<AuraColor, String> IMPLEMENTED_RUNTIME_EFFECTS = new EnumMap<>(AuraColor.class);
    private static final List<Interaction> LEGACY_INTERACTIONS = List.of(
        new Interaction(AuraColor.YELLOW, AuraColor.GREEN, "harvests crops in an area"),
        new Interaction(AuraColor.BLUE, AuraColor.VIOLET, "damages nearby enemies"),
        new Interaction(AuraColor.GREEN, AuraColor.BLUE, "steals life"),
        new Interaction(AuraColor.YELLOW, AuraColor.BLUE, "ignites targets"),
        new Interaction(AuraColor.RED, AuraColor.VIOLET, "reduces incoming damage"),
        new Interaction(AuraColor.YELLOW, AuraColor.VIOLET, "multiplies mob drops"),
        new Interaction(AuraColor.YELLOW, AuraColor.RED, "drops two ingots from ores"),
        new Interaction(AuraColor.RED, AuraColor.GREEN, "slows mining"),
        new Interaction(AuraColor.RED, AuraColor.BLUE, "causes recoil"),
        new Interaction(AuraColor.GREEN, AuraColor.VIOLET, "lowers attack damage"),
        new Interaction(AuraColor.RED, AuraColor.ORANGE, "speeds ore mining"),
        new Interaction(AuraColor.YELLOW, AuraColor.ORANGE, "speeds stone mining"),
        new Interaction(AuraColor.BLUE, AuraColor.ORANGE, "speeds dirt, sand, grass, and gravel mining"),
        new Interaction(AuraColor.GREEN, AuraColor.ORANGE, "speeds wood mining"),
        new Interaction(AuraColor.VIOLET, AuraColor.ORANGE, "speeds high-durability block mining")
    );

    static {
        LEGACY_BASIC_EFFECTS.put(AuraColor.RED, "Silk Touch");
        LEGACY_BASIC_EFFECTS.put(AuraColor.ORANGE, "Efficiency");
        LEGACY_BASIC_EFFECTS.put(AuraColor.YELLOW, "Fortune");
        LEGACY_BASIC_EFFECTS.put(AuraColor.GREEN, "tree-felling");
        LEGACY_BASIC_EFFECTS.put(AuraColor.BLUE, "Knockback");
        LEGACY_BASIC_EFFECTS.put(AuraColor.VIOLET, "hard-material mining speed");

        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.RED, "ignite");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.ORANGE, "knockback");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.YELLOW, "mining efficiency");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.GREEN, "poison");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.BLUE, "bonus damage");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.VIOLET, "nausea");
    }

    private KaleidoscopicLegacyContract() {
    }

    public static Map<AuraColor, String> legacyBasicEffects() {
        return Map.copyOf(LEGACY_BASIC_EFFECTS);
    }

    public static Map<AuraColor, String> implementedRuntimeEffects() {
        return Map.copyOf(IMPLEMENTED_RUNTIME_EFFECTS);
    }

    public static List<Interaction> legacyInteractions() {
        return LEGACY_INTERACTIONS;
    }

    public static String legacyBasicEffectSummary() {
        return effectSummary(LEGACY_BASIC_EFFECTS);
    }

    public static String implementedRuntimeSummary() {
        return effectSummary(IMPLEMENTED_RUNTIME_EFFECTS);
    }

    public static String legacyInteractionSummary() {
        return LEGACY_INTERACTIONS.stream()
            .map(Interaction::summary)
            .reduce((left, right) -> left + "; " + right)
            .orElse("");
    }

    private static String effectSummary(Map<AuraColor, String> effects) {
        return COLOR_ORDER.stream()
            .map(color -> color.displayName() + " = " + effects.get(color))
            .reduce((left, right) -> left + ", " + right)
            .orElse("");
    }

    public record Interaction(AuraColor first, AuraColor second, String effect) {
        public String summary() {
            return first.displayName() + " + " + second.displayName() + " " + effect;
        }
    }
}
