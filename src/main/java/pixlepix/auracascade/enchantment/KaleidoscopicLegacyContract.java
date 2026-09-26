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
    private static final EnumMap<AuraColor, String> MODERN_RUNTIME_EFFECTS = new EnumMap<>(AuraColor.class);
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
    private static final List<Interaction> MODERN_RUNTIME_INTERACTIONS = List.of(
        new Interaction(AuraColor.YELLOW, AuraColor.GREEN, "breaks connected growable blocks, up to 25 per pair strength"),
        new Interaction(AuraColor.BLUE, AuraColor.VIOLET, "damages nearby living entities in a two-block cube on attack"),
        new Interaction(AuraColor.GREEN, AuraColor.BLUE, "heals the attacker by half the pair strength on attack"),
        new Interaction(AuraColor.YELLOW, AuraColor.BLUE, "ignites the target and splash victims"),
        new Interaction(AuraColor.RED, AuraColor.VIOLET, "multiplies incoming player damage by 0.9 per pair strength"),
        new Interaction(AuraColor.YELLOW, AuraColor.VIOLET, "rolls bonus loot from the killed entity's table"),
        new Interaction(AuraColor.YELLOW, AuraColor.RED, "can convert a compatible ore to two ingots"),
        new Interaction(AuraColor.RED, AuraColor.GREEN, "divides mining speed by 3 per pair strength"),
        new Interaction(AuraColor.RED, AuraColor.BLUE, "deals recoil damage to the attacker"),
        new Interaction(AuraColor.GREEN, AuraColor.VIOLET, "subtracts pair strength from outgoing damage"),
        new Interaction(AuraColor.RED, AuraColor.ORANGE, "multiplies ore mining speed by 1.25 per pair strength"),
        new Interaction(AuraColor.YELLOW, AuraColor.ORANGE, "multiplies stone mining speed by 1.25 per pair strength"),
        new Interaction(AuraColor.BLUE, AuraColor.ORANGE, "multiplies loose-block mining speed by 1.25 per pair strength"),
        new Interaction(AuraColor.GREEN, AuraColor.ORANGE, "multiplies log mining speed by 1.25 per pair strength"),
        new Interaction(AuraColor.VIOLET, AuraColor.ORANGE, "multiplies hard-block mining speed by 1.5 per pair strength")
    );
    private static final List<String> MODERN_RUNTIME_LIMITS = List.of(
        "Yellow + Violet uses a bonus roll of the killed entity's modern loot table; Forge dropFewItems and rare-drop reflection have no exact equivalent.",
        "Yellow + Red supports vanilla metal ores and present c:ores/<material> to c:ingots/<material> tags, without inventing ingots.",
        "Red + Violet modifies damage before hurt rather than healing after it.",
        "Violet + Orange checks modern block destroy speed >= 3.0 at the player's position."
    );

    static {
        LEGACY_BASIC_EFFECTS.put(AuraColor.RED, "Silk Touch");
        LEGACY_BASIC_EFFECTS.put(AuraColor.ORANGE, "Efficiency");
        LEGACY_BASIC_EFFECTS.put(AuraColor.YELLOW, "Fortune");
        LEGACY_BASIC_EFFECTS.put(AuraColor.GREEN, "tree-felling");
        LEGACY_BASIC_EFFECTS.put(AuraColor.BLUE, "Knockback");
        LEGACY_BASIC_EFFECTS.put(AuraColor.VIOLET, "outgoing damage");

        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.RED, "ignite");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.ORANGE, "knockback");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.YELLOW, "mining efficiency");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.GREEN, "poison");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.BLUE, "bonus damage");
        IMPLEMENTED_RUNTIME_EFFECTS.put(AuraColor.VIOLET, "nausea");

        MODERN_RUNTIME_EFFECTS.put(AuraColor.RED, "temporary Silk Touch mining override");
        MODERN_RUNTIME_EFFECTS.put(AuraColor.ORANGE, "mining speed multiplied by 1.15 per level");
        MODERN_RUNTIME_EFFECTS.put(AuraColor.YELLOW, "temporary Fortune mining override");
        MODERN_RUNTIME_EFFECTS.put(AuraColor.GREEN, "connected same-block log felling, up to 25 per level");
        MODERN_RUNTIME_EFFECTS.put(AuraColor.BLUE, "knockback of 0.5 per level");
        MODERN_RUNTIME_EFFECTS.put(AuraColor.VIOLET, "outgoing damage increased by 0.5 per level");
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

    public static Map<AuraColor, String> modernRuntimeEffects() {
        return Map.copyOf(MODERN_RUNTIME_EFFECTS);
    }

    public static List<Interaction> modernRuntimeInteractions() {
        return MODERN_RUNTIME_INTERACTIONS;
    }

    public static List<String> modernRuntimeLimits() {
        return MODERN_RUNTIME_LIMITS;
    }

    public static String legacyBasicEffectSummary() {
        return effectSummary(LEGACY_BASIC_EFFECTS);
    }

    public static String implementedRuntimeSummary() {
        return effectSummary(IMPLEMENTED_RUNTIME_EFFECTS);
    }

    public static String modernRuntimeSummary() {
        return effectSummary(MODERN_RUNTIME_EFFECTS);
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
