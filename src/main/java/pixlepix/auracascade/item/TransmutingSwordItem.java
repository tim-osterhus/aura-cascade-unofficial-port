package pixlepix.auracascade.item;

import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import pixlepix.auracascade.util.ToolPropertiesCompat;

public final class TransmutingSwordItem extends Item {
    private static final Map<EntityType<? extends LivingEntity>, EntityType<? extends LivingEntity>> TRANSMUTATIONS = Map.ofEntries(
        Map.entry(EntityType.COW, EntityType.MOOSHROOM),
        Map.entry(EntityType.MOOSHROOM, EntityType.COW),
        Map.entry(EntityType.CREEPER, EntityType.ENDERMAN),
        Map.entry(EntityType.ENDERMAN, EntityType.CREEPER),
        Map.entry(EntityType.SHEEP, EntityType.PIG),
        Map.entry(EntityType.PIG, EntityType.SHEEP),
        Map.entry(EntityType.GHAST, EntityType.BLAZE),
        Map.entry(EntityType.BLAZE, EntityType.GHAST),
        Map.entry(EntityType.SLIME, EntityType.MAGMA_CUBE),
        Map.entry(EntityType.MAGMA_CUBE, EntityType.SLIME),
        Map.entry(EntityType.OCELOT, EntityType.WOLF),
        Map.entry(EntityType.WOLF, EntityType.OCELOT)
    );

    public TransmutingSwordItem() {
        this(ToolPropertiesCompat.sword(new Item.Properties().stacksTo(1), AuraUtilityToolMaterials.ARCANE_SWORD, 3, -2.4F));
    }

    public TransmutingSwordItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!shouldTransmute(target.isAlive(), target.getHealth()) || !(target.level() instanceof ServerLevel serverLevel)) {
            return true;
        }

        EntityType<? extends LivingEntity> mappedType = mappedType(target.getType());
        if (mappedType == null) {
            return true;
        }

        Entity replacement = mappedType.create(serverLevel);
        if (!(replacement instanceof LivingEntity replacementLiving)) {
            return true;
        }

        replacement.copyPosition(target);
        replacement.setCustomName(target.getCustomName());
        replacement.setCustomNameVisible(target.isCustomNameVisible());
        if (target instanceof Slime oldSlime && replacementLiving instanceof Slime newSlime) {
            newSlime.setSize(oldSlime.getSize(), true);
        }
        replacementLiving.setHealth(Math.min(replacementLiving.getMaxHealth(), target.getHealth()));
        if (serverLevel.addFreshEntity(replacement)) {
            target.discard();
        }
        return true;
    }

    static EntityType<? extends LivingEntity> mappedType(EntityType<?> type) {
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
        return TRANSMUTATIONS.get(livingType);
    }

    static boolean shouldTransmute(boolean alive, float health) {
        return alive && health > 0.0F;
    }
}
