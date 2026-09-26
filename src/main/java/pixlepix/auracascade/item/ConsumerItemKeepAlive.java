package pixlepix.auracascade.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public final class ConsumerItemKeepAlive {
    public static final String PROTECTED_TAG = "aura:consumer_keep_alive";
    public static final String AGE_TAG = "aura:consumer_item_age";

    private ConsumerItemKeepAlive() {
    }

    public static boolean shouldRefresh(boolean clientSide, long gameTime) {
        return !clientSide && gameTime % 500L == 0L;
    }

    public static AABB bounds(BlockPos pos) {
        return new AABB(pos.getX() - 3.0D, pos.getY() - 3.0D, pos.getZ() - 3.0D,
            pos.getX() + 3.0D, pos.getY() + 3.0D, pos.getZ() + 3.0D);
    }

    public static void tick(Level level, BlockPos pos) {
        if (!shouldRefresh(level.isClientSide(), level.getGameTime())) {
            return;
        }
        // Entity queries visit loaded sections only; never load surrounding chunks.
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, bounds(pos))) {
            ((ConsumerItemLifetimeAccess) item).aura$keepAlive();
        }
    }

    public static int effectiveLifetime(boolean keptAlive, int itemLifetime) {
        return keptAlive ? Integer.MAX_VALUE : itemLifetime;
    }

    public static int effectiveLifetime(ItemEntity item, int itemLifetime) {
        return effectiveLifetime(((ConsumerItemLifetimeAccess) item).aura$isConsumerKeptAlive(), itemLifetime);
    }
}
