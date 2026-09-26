package pixlepix.auracascade.aura;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraTransferVisualsTest {
    @Test
    void idleAndZeroLengthTransfersProduceNoParticles() {
        AuraStorage empty = new AuraStorage();
        assertTrue(AuraTransferVisuals.samples(BlockPos.ZERO, new BlockPos(0, 8, 0), empty).isEmpty());
        empty.set(AuraColor.WHITE, 1);
        assertTrue(AuraTransferVisuals.samples(BlockPos.ZERO, BlockPos.ZERO, empty).isEmpty());
    }

    @Test
    void actualTransferredColorsFollowTheSourceToTargetDirectionWithinABoundedBudget() {
        AuraStorage moved = new AuraStorage();
        moved.set(AuraColor.BLUE, 10);
        moved.set(AuraColor.WHITE, 20);
        var samples = AuraTransferVisuals.samples(new BlockPos(2, 15, 4), new BlockPos(2, 0, 4), moved);
        assertFalse(samples.isEmpty());
        assertTrue(samples.size() <= AuraTransferVisuals.MAX_SAMPLES);
        assertTrue(samples.stream().anyMatch(sample -> sample.color() == AuraColor.BLUE));
        assertTrue(samples.stream().anyMatch(sample -> sample.color() == AuraColor.WHITE));
        for (var sample : samples) {
            assertTrue(sample.color() == AuraColor.BLUE || sample.color() == AuraColor.WHITE);
            assertEquals(2.5D, sample.position().x);
            assertEquals(4.5D, sample.position().z);
            assertTrue(sample.position().y > 0.5D && sample.position().y < 15.5D);
            assertEquals(-1.0D, sample.direction().y);
        }
        assertEquals(10, moved.get(AuraColor.BLUE));
        assertEquals(20, moved.get(AuraColor.WHITE));
    }

    @Test
    void allEightColorsRemainBoundedAndVisible() {
        AuraStorage moved = new AuraStorage();
        for (AuraColor color : AuraColor.values()) {
            moved.set(color, 1);
        }
        var samples = AuraTransferVisuals.samples(BlockPos.ZERO, new BlockPos(15, 0, 0), moved);
        assertTrue(samples.size() <= AuraTransferVisuals.MAX_SAMPLES);
        assertEquals(8, samples.stream().map(AuraTransferVisuals.Sample::color).distinct().count());
    }
}
