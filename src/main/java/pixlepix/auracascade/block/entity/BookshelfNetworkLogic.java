package pixlepix.auracascade.block.entity;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import pixlepix.auracascade.block.AuraContent;

public final class BookshelfNetworkLogic {
    private BookshelfNetworkLogic() {
    }

    public static int powerCost(int bookshelfCount) {
        if (bookshelfCount <= 0) {
            return 0;
        }
        return (int) (5.0D * bookshelfCount * Math.pow(1.05D, bookshelfCount));
    }

    public static LinkedHashSet<BlockPos> collectConnectedShelves(Level level, BlockPos coordinatorPos) {
        LinkedHashSet<BlockPos> visited = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();

        for (Direction direction : Direction.values()) {
            BlockPos candidate = coordinatorPos.relative(direction);
            if (isBookshelf(level.getBlockState(candidate)) && visited.add(candidate.immutable())) {
                queue.add(candidate.immutable());
            }
        }

        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }
                if (isBookshelf(level.getBlockState(next))) {
                    BlockPos immutable = next.immutable();
                    visited.add(immutable);
                    queue.add(immutable);
                }
            }
        }

        return visited;
    }

    public static boolean hasLineOfSight(Level level, BlockPos fromPos, BlockPos toPos) {
        Vec3 from = Vec3.atCenterOf(fromPos);
        Vec3 to = Vec3.atCenterOf(toPos);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(fromPos) || hit.getBlockPos().equals(toPos);
    }

    private static boolean isBookshelf(BlockState state) {
        return state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF) || state.is(AuraContent.STORAGE_BOOKSHELF);
    }
}
