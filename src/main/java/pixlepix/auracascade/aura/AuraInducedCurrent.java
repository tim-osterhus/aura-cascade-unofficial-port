package pixlepix.auracascade.aura;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public record AuraInducedCurrent(BlockPos nodePos, BlockPos downstreamTarget, Direction direction, int amount) {
    public AuraInducedCurrent {
        Objects.requireNonNull(nodePos, "nodePos");
        Objects.requireNonNull(downstreamTarget, "downstreamTarget");
        Objects.requireNonNull(direction, "direction");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
