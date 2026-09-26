package pixlepix.auracascade.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class PortableRedHoleItem extends Item {
    static final int LIFETIME_TICKS = 30_000;

    public PortableRedHoleItem(Item.Properties properties) {
        super(properties);
    }

    public static boolean onEntityItemTick(ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
            || !shouldErupt(entity.getItem().getItem() instanceof PortableRedHoleItem, level.getGameTime())) {
            return false;
        }

        // Legacy createExplosion(..., true) destroys terrain but does not start fires.
        level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 12.0F, false, Level.ExplosionInteraction.BLOCK);
        return true;
    }

    static boolean shouldErupt(boolean redHole, long gameTime) {
        return redHole && gameTime % 100L == 0L;
    }

    public static int lifetimeTicks(ItemStack stack, int vanillaLifetime) {
        return lifetimeTicks(stack.getItem() instanceof PortableRedHoleItem, vanillaLifetime);
    }

    static int lifetimeTicks(boolean redHole, int vanillaLifetime) {
        return redHole ? LIFETIME_TICKS : vanillaLifetime;
    }
}
