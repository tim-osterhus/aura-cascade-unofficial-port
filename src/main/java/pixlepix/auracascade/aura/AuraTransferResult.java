package pixlepix.auracascade.aura;

import java.util.Objects;

public record AuraTransferResult(AuraStorage moved, int generatedPower) {
    public AuraTransferResult {
        Objects.requireNonNull(moved, "moved");
        moved = moved.copy();
        generatedPower = Math.max(0, generatedPower);
    }
}
