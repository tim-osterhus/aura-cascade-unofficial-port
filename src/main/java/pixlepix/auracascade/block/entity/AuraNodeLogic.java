package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraNodeLogic {
    private AuraNodeLogic() {
    }

    public static boolean canSend(
        AuraNodeVariant variant,
        BlockPos origin,
        BlockPos target,
        int totalAura,
        int capacitorThreshold,
        boolean powered
    ) {
        if (variant.isCapacitor() && totalAura < capacitorThreshold) {
            return false;
        }
        if (variant.isConserving() && target.getY() != origin.getY()) {
            return false;
        }
        return !powered || variant.isManipulator();
    }

    public static boolean canReceive(
        AuraNodeVariant variant,
        AuraColor color,
        int capacitorCooldown
    ) {
        if (variant.isCapacitor() && capacitorCooldown > 0) {
            return false;
        }
        return !variant.isManipulator() || variant.manipulatorColor() == color;
    }

    public static int comparatorCapacity(AuraNodeVariant variant, int capacitorThreshold) {
        return variant.isCapacitor() ? capacitorThreshold : variant.comparatorCapacity();
    }

    public static AuraNodeState manipulatorState(AuraNodeVariant variant) {
        AuraNodeState nodeState = new AuraNodeState();
        refreshManipulator(nodeState, variant, true);
        return nodeState;
    }

    public static void refreshManipulator(AuraNodeState nodeState, AuraNodeVariant variant, boolean powered) {
        if (variant.isManipulator()) {
            for (AuraColor color : AuraColor.values()) {
                nodeState.storage().set(color, powered && color == variant.manipulatorColor() ? 100_000 : 0);
            }
        }
    }
}
