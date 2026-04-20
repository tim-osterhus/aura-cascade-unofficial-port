package pixlepix.auracascade.item;

import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

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
        this(new Item.Properties().stacksTo(1).sword(AuraUtilityToolMaterials.ARCANE_SWORD, 3.0F, -2.4F));
    }

    public TransmutingSwordItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        EntityType<? extends LivingEntity> mappedType = mappedType(target.getType());
        if (mappedType == null) {
            return;
        }

        Entity replacement = mappedType.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (!(replacement instanceof LivingEntity replacementLiving)) {
            return;
        }

        replacement.copyPosition(target);
        replacement.setCustomName(target.getCustomName());
        replacement.setCustomNameVisible(target.isCustomNameVisible());
        replacementLiving.setHealth(Math.min(replacementLiving.getMaxHealth(), target.getHealth()));
        serverLevel.addFreshEntity(replacement);
        target.discard();
    }

    static EntityType<? extends LivingEntity> mappedType(EntityType<?> type) {
        @SuppressWarnings("unchecked")
        EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
        return TRANSMUTATIONS.get(livingType);
    }
}
