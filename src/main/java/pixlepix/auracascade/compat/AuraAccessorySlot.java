package pixlepix.auracascade.compat;

public enum AuraAccessorySlot {
    RING("ring"),
    AMULET("amulet"),
    BELT("belt");

    private final String id;

    AuraAccessorySlot(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static AuraAccessorySlot byId(String id) {
        for (AuraAccessorySlot slot : values()) {
            if (slot.id.equals(id)) {
                return slot;
            }
        }
        throw new IllegalArgumentException("Unknown accessory slot: " + id);
    }
}
