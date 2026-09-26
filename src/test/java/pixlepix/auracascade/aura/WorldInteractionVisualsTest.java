package pixlepix.auracascade.aura;

import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldInteractionVisualsTest {
    @Test
    void placementTraceFollowsOnlySuppliedLinkGeometryAndExpires() {
        BlockPos source = new BlockPos(2, 4, 6);
        BlockPos target = new BlockPos(2, 4, 12);
        assertTrue(WorldInteractionVisuals.placementSamples(source, List.of(), 0).isEmpty());
        for (int tick = 0; tick < WorldInteractionVisuals.PLACEMENT_TICKS; tick++) {
            List<Vec3> samples = WorldInteractionVisuals.placementSamples(source, List.of(target), tick);
            assertEquals(2, samples.size());
            for (Vec3 point : samples) {
                assertEquals(2.5D, point.x);
                assertEquals(4.5D, point.y);
                assertTrue(point.z > 6.5D && point.z <= 12.5D);
            }
        }
        assertTrue(WorldInteractionVisuals.placementSamples(source, List.of(target),
            WorldInteractionVisuals.PLACEMENT_TICKS).isEmpty());
    }

    @Test
    void placementAndCraftParticleBudgetsStayBounded() {
        List<BlockPos> links = List.of(
            new BlockPos(1, 0, 0), new BlockPos(2, 0, 0), new BlockPos(3, 0, 0),
            new BlockPos(4, 0, 0), new BlockPos(5, 0, 0), new BlockPos(6, 0, 0),
            new BlockPos(7, 0, 0)
        );
        assertEquals(12, WorldInteractionVisuals.placementSamples(BlockPos.ZERO, links, 0).size());
        Vec3 center = new Vec3(3.5D, 5.5D, 7.5D);
        for (int tick = 0; tick < WorldInteractionVisuals.CRAFT_TICKS; tick++) {
            List<Vec3> samples = WorldInteractionVisuals.craftSamples(center, tick);
            assertEquals(4, samples.size());
            for (Vec3 point : samples) {
                assertTrue(point.distanceTo(center) < 1.0D);
            }
        }
        assertTrue(WorldInteractionVisuals.craftSamples(center, WorldInteractionVisuals.CRAFT_TICKS).isEmpty());
    }

    @Test
    void blockedAndOutOfRangeNodesNeverProducePreviewSamples() {
        BlockPos source = BlockPos.ZERO;
        BlockPos visible = new BlockPos(0, 0, -3);
        BlockPos blocked = new BlockPos(0, 0, 4);
        BlockPos distant = new BlockPos(AuraKernel.DEFAULT_LINK_RANGE + 1, 0, 0);
        Set<BlockPos> links = AuraKernel.scanStraightLineLinks(source,
            Set.of(visible, blocked, distant), Set.of(new BlockPos(0, 0, 2)), AuraKernel.DEFAULT_LINK_RANGE);
        assertEquals(Set.of(visible), links);
        for (Vec3 point : WorldInteractionVisuals.placementSamples(source, links, 3)) {
            assertEquals(0.5D, point.x);
            assertTrue(point.z < 0.5D);
        }
    }

    @Test
    void groundCombinationBurstIsShortAndRadial() {
        Vec3 center = new Vec3(4.0D, 8.0D, 12.0D);
        var samples = WorldInteractionVisuals.groundCraftBurst(center);
        assertEquals(WorldInteractionVisuals.GROUND_CRAFT_PARTICLES, samples.size());
        for (var sample : samples) {
            assertTrue(sample.position().distanceTo(center) < 0.25D);
            assertTrue(Math.hypot(sample.velocity().x, sample.velocity().z) <= 0.045D + 1.0e-9D);
            assertTrue(sample.velocity().y > 0.0D && sample.velocity().y < 0.1D);
        }
    }
}
