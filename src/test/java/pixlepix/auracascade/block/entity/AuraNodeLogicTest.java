package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraNodeLogicTest {
    @Test
    void manipulatorsOnlyAcceptTheirOwnColorAndStayActiveUnderRedstone() {
        assertTrue(AuraNodeLogic.canReceive(AuraNodeVariant.BLACK_MANIPULATOR, AuraColor.BLACK, 0));
        assertFalse(AuraNodeLogic.canReceive(AuraNodeVariant.BLACK_MANIPULATOR, AuraColor.WHITE, 0));
        assertTrue(AuraNodeLogic.canSend(
            AuraNodeVariant.BLACK_MANIPULATOR,
            new BlockPos(0, 0, 0),
            new BlockPos(0, -1, 0),
            500,
            1_000,
            true
        ));

        var orangeState = AuraNodeLogic.manipulatorState(AuraNodeVariant.ORANGE_MANIPULATOR);
        assertEquals(100_000, orangeState.storage().get(AuraColor.ORANGE));
        assertEquals(0, orangeState.storage().get(AuraColor.BLACK));
    }

    @Test
    void capacitorAndConservingNodeEnforceTheirTransferRules() {
        BlockPos origin = new BlockPos(0, 10, 0);

        assertFalse(AuraNodeLogic.canSend(
            AuraNodeVariant.AURA_CAPACITOR,
            origin,
            new BlockPos(0, 9, 0),
            999,
            1_000,
            false
        ));
        assertTrue(AuraNodeLogic.canSend(
            AuraNodeVariant.AURA_CAPACITOR,
            origin,
            new BlockPos(0, 9, 0),
            1_000,
            1_000,
            false
        ));
        assertFalse(AuraNodeLogic.canReceive(AuraNodeVariant.AURA_CAPACITOR, AuraColor.WHITE, 20));
        assertTrue(AuraNodeLogic.canReceive(AuraNodeVariant.AURA_CAPACITOR, AuraColor.WHITE, 0));

        assertTrue(AuraNodeLogic.canSend(
            AuraNodeVariant.CONSERVING_AURA_NODE,
            origin,
            new BlockPos(1, 10, 0),
            500,
            1_000,
            false
        ));
        assertFalse(AuraNodeLogic.canSend(
            AuraNodeVariant.CONSERVING_AURA_NODE,
            origin,
            new BlockPos(0, 9, 0),
            500,
            1_000,
            false
        ));
        assertEquals(2_000, AuraNodeLogic.comparatorCapacity(AuraNodeVariant.AURA_CAPACITOR, 2_000));
    }
}
