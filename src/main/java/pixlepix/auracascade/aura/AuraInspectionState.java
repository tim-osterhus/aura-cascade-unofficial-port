package pixlepix.auracascade.aura;

public record AuraInspectionState(
    AuraStorage storage,
    int linkedNodeCount,
    int storedPower,
    boolean hasScannedLinks
) {
    public AuraInspectionState {
        storage = storage.copy();
        linkedNodeCount = Math.max(0, linkedNodeCount);
        storedPower = Math.max(0, storedPower);
    }

    public int totalAura() {
        return storage.total();
    }

    public int comparatorLevel(int capacity) {
        if (capacity <= 0) {
            return totalAura() > 0 ? 15 : 0;
        }
        if (totalAura() <= 0) {
            return 0;
        }
        return Math.min(15, (int) Math.ceil(totalAura() * 15.0D / capacity));
    }
}
