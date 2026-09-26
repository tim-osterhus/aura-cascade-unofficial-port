package pixlepix.auracascade.block.entity;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BookshelfNetworkLogicTest {
    @BeforeAll
    static void bootstrap() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void powerCurveMatchesRecoveredCoordinatorFormula() {
        assertEquals(0, BookshelfNetworkLogic.powerCost(0));
        assertEquals(5, BookshelfNetworkLogic.powerCost(1));
        assertEquals(31, BookshelfNetworkLogic.powerCost(5));
        assertEquals(81, BookshelfNetworkLogic.powerCost(10));
    }

    @Test
    void sourceInsideHitMustNotConcealAnIntermediateBlock() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = new BlockPos(6, 64, 0);
        TestBlocks level = endpoints(from, to);
        BlockPos obstruction = new BlockPos(3, 64, 0);
        level.blocks.put(obstruction, Blocks.STONE.defaultBlockState());

        BlockHitResult ordinary = level.clip(new ClipContext(Vec3.atCenterOf(from), Vec3.atCenterOf(to),
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        assertEquals(HitResult.Type.BLOCK, ordinary.getType());
        assertEquals(from, ordinary.getBlockPos());
        assertTrue(ordinary.isInside(), "Exercise the real vanilla self-hit that caused the bypass");
        assertFalse(BookshelfNetworkLogic.hasLineOfSight(level, from, to));
        level.blocks.remove(obstruction);
        assertTrue(BookshelfNetworkLogic.hasLineOfSight(level, from, to));
    }

    @Test
    void fullCubeEndpointsAllowAdjacentAndClearDistantSightLines() {
        BlockPos from = new BlockPos(0, 64, 0);
        for (BlockPos to : new BlockPos[] {from.east(), from.above(), from.east(6)}) {
            TestBlocks level = endpoints(from, to);
            assertTrue(BookshelfNetworkLogic.hasLineOfSight(level, from, to));
            assertTrue(BookshelfNetworkLogic.hasLineOfSight(level, to, from));
        }
        assertTrue(BookshelfNetworkLogic.hasLineOfSight(endpoints(from, from), from, from));
    }

    @Test
    void onlyEndpointsAreExemptNotTheirNeighborsOrOtherBookshelves() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = from.east(6);
        for (BlockPos obstruction : new BlockPos[] {from.east(), from.east(3), to.west()}) {
            for (BlockState state : new BlockState[] {Blocks.STONE.defaultBlockState(),
                Blocks.BOOKSHELF.defaultBlockState(), Blocks.CHISELED_BOOKSHELF.defaultBlockState()}) {
                TestBlocks level = endpoints(from, to);
                level.blocks.put(obstruction, state);
                assertFalse(BookshelfNetworkLogic.hasLineOfSight(level, from, to),
                    "Intermediate collider at " + obstruction + " must obstruct: " + state);
                assertFalse(BookshelfNetworkLogic.hasLineOfSight(level, to, from));
            }
        }
    }

    @Test
    void nonCollidingBlocksAndFluidsDoNotBecomeLegacyNonAirObstructions() {
        BlockPos from = new BlockPos(0, 64, 0);
        BlockPos to = from.east(6);
        TestBlocks level = endpoints(from, to);
        level.blocks.put(from.east(2), Blocks.TORCH.defaultBlockState());
        level.blocks.put(from.east(3), Blocks.WATER.defaultBlockState());
        assertTrue(BookshelfNetworkLogic.hasLineOfSight(level, from, to));
    }

    @Test
    void vanillaTraversalHandlesDiagonalNegativeCoordinatesWithoutSampleRounding() {
        BlockPos from = new BlockPos(-8, 64, -8);
        BlockPos to = new BlockPos(-2, 64, -2);
        TestBlocks level = endpoints(from, to);
        assertTrue(BookshelfNetworkLogic.hasLineOfSight(level, from, to));
        level.blocks.put(new BlockPos(-5, 64, -5), Blocks.STONE.defaultBlockState());
        assertFalse(BookshelfNetworkLogic.hasLineOfSight(level, from, to));
        assertFalse(BookshelfNetworkLogic.hasLineOfSight(level, to, from));
    }

    private static TestBlocks endpoints(BlockPos from, BlockPos to) {
        TestBlocks level = new TestBlocks();
        // The coordinator uses an ordinary full-cube collider; no Aura registration is needed.
        level.blocks.put(from, Blocks.STONE.defaultBlockState());
        level.blocks.put(to, Blocks.BOOKSHELF.defaultBlockState());
        return level;
    }

    // Only block storage is substituted. clip(), traversal and collision shapes are vanilla.
    private static final class TestBlocks implements BlockGetter {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        @Override
        public BlockEntity getBlockEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(BlockPos pos) {
            return getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}
