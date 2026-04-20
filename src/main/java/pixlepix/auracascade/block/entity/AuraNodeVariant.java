package pixlepix.auracascade.block.entity;

import pixlepix.auracascade.parity.AuraColor;

public enum AuraNodeVariant {
    AURA_NODE("aura_node", "block.aura.aura_node", 1_000),
    AURA_CAPACITOR("aura_node_capacitor", "block.aura.aura_node_capacitor", 1_000),
    CONSERVING_AURA_NODE("aura_node_conserve", "block.aura.aura_node_conserve", 1_000),
    BLACK_MANIPULATOR("aura_node_black", "block.aura.aura_node_black", 100_000, AuraColor.BLACK),
    ORANGE_MANIPULATOR("aura_node_orange", "block.aura.aura_node_orange", 100_000, AuraColor.ORANGE),
    FLUXING_NODE("aura_node_flux", "block.aura.aura_node_flux", 1_000);

    private final String registryPath;
    private final String translationKey;
    private final int comparatorCapacity;
    private final AuraColor manipulatorColor;

    AuraNodeVariant(String registryPath, String translationKey, int comparatorCapacity) {
        this(registryPath, translationKey, comparatorCapacity, null);
    }

    AuraNodeVariant(String registryPath, String translationKey, int comparatorCapacity, AuraColor manipulatorColor) {
        this.registryPath = registryPath;
        this.translationKey = translationKey;
        this.comparatorCapacity = comparatorCapacity;
        this.manipulatorColor = manipulatorColor;
    }

    public String registryPath() {
        return registryPath;
    }

    public String translationKey() {
        return translationKey;
    }

    public int comparatorCapacity() {
        return comparatorCapacity;
    }

    public boolean isCapacitor() {
        return this == AURA_CAPACITOR;
    }

    public boolean isConserving() {
        return this == CONSERVING_AURA_NODE;
    }

    public boolean isManipulator() {
        return manipulatorColor != null;
    }

    public AuraColor manipulatorColor() {
        return manipulatorColor;
    }

    public boolean isFlux() {
        return this == FLUXING_NODE;
    }
}
