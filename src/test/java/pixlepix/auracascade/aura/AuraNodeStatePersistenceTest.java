package pixlepix.auracascade.aura;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraNodeStatePersistenceTest {
    @Test
    void roundTripsPersistentAuraStateAndInspectionSurface() {
        AuraNodeState state = new AuraNodeState();
        state.storage().set(AuraColor.WHITE, 30);
        state.storage().set(AuraColor.VIOLET, 20);
        state.replaceLinkedNodes(List.of(new BlockPos(1, 2, 3), new BlockPos(4, 5, 6)));
        state.setHasScannedLinks(true);
        state.setStoredPower(17);

        CompoundTag tag = state.toTag();
        AuraNodeState restored = AuraNodeState.fromTag(tag);
        AuraInspectionState inspection = restored.inspectionState();

        assertEquals(30, restored.storage().get(AuraColor.WHITE));
        assertEquals(20, restored.storage().get(AuraColor.VIOLET));
        assertEquals(2, restored.linkedNodes().size());
        assertTrue(restored.linkedNodes().contains(new BlockPos(1, 2, 3)));
        assertTrue(restored.hasScannedLinks());
        assertEquals(17, restored.storedPower());
        assertEquals(50, inspection.totalAura());
        assertEquals(4, inspection.comparatorLevel(200));
    }
}
