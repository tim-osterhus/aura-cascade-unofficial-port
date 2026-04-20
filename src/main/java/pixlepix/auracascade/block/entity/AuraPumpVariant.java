package pixlepix.auracascade.block.entity;

public enum AuraPumpVariant {
    BURNING("aura_node_pump", "block.aura.aura_node_pump", false),
    BURNING_ALT("aura_node_pump_alt", "block.aura.aura_node_pump_alt", true),
    ILLUMINATION("aura_node_pump_light", "block.aura.aura_node_pump_light", false),
    ILLUMINATION_ALT("aura_node_pump_light_alt", "block.aura.aura_node_pump_light_alt", true),
    MOMENTUM("aura_node_pump_fall", "block.aura.aura_node_pump_fall", false),
    MOMENTUM_ALT("aura_node_pump_fall_alt", "block.aura.aura_node_pump_fall_alt", true),
    PROJECTILE("aura_node_pump_projectile", "block.aura.aura_node_pump_projectile", false),
    PROJECTILE_ALT("aura_node_pump_projectile_alt", "block.aura.aura_node_pump_projectile_alt", true),
    REDSTONE("aura_node_pump_redstone", "block.aura.aura_node_pump_redstone", false),
    REDSTONE_ALT("aura_node_pump_redstone_alt", "block.aura.aura_node_pump_redstone_alt", true),
    CREATIVE("aura_node_pump_creative", "block.aura.aura_node_pump_creative", false);

    private final String registryPath;
    private final String translationKey;
    private final boolean alternating;

    AuraPumpVariant(String registryPath, String translationKey, boolean alternating) {
        this.registryPath = registryPath;
        this.translationKey = translationKey;
        this.alternating = alternating;
    }

    public String registryPath() {
        return registryPath;
    }

    public String translationKey() {
        return translationKey;
    }

    public boolean isAlternating() {
        return alternating;
    }

    public boolean isCreative() {
        return this == CREATIVE;
    }
}
