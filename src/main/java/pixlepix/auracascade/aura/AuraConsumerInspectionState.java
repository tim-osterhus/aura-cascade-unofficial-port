package pixlepix.auracascade.aura;

public record AuraConsumerInspectionState(
    int progress,
    int maxProgress,
    int requiredPower,
    int lastReceivedPower,
    int storedPower
) {
}
