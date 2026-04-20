package pixlepix.auracascade.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class SwordOfBarbarianItem extends Item {
    static final String NBT_TAG_LAST_COMBO_TIME = "lastComboTime";
    static final String NBT_TAG_COMBO_COUNT = "comboCount";
    private static final int COMBO_WINDOW_TICKS = 100;
    private static final int MAX_COMBO_COUNT = 100;
    private static final float BASE_DAMAGE = 6.0F;

    public SwordOfBarbarianItem() {
        this(new Item.Properties().stacksTo(1).sword(AuraUtilityToolMaterials.ARCANE_SWORD, 3.0F, -2.4F));
    }

    public SwordOfBarbarianItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        long currentTime = attacker.level().getGameTime();
        int combo = nextComboCount(lastComboTime(stack), currentTime, comboCount(stack));
        setComboState(stack, currentTime, combo);

        float extraDamage = comboBonusDamage(combo);
        if (extraDamage <= 0.0F) {
            return;
        }

        if (attacker instanceof Player player) {
            target.hurt(attacker.damageSources().playerAttack(player), extraDamage);
        } else {
            target.hurt(attacker.damageSources().mobAttack(attacker), extraDamage);
        }
    }

    static int comboCount(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return Math.max(0, tag.getInt(NBT_TAG_COMBO_COUNT).orElse(0));
    }

    static long lastComboTime(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return tag.getLong(NBT_TAG_LAST_COMBO_TIME).orElse(0L);
    }

    static int nextComboCount(long lastHitTime, long currentTime, int previousComboCount) {
        if (currentTime - lastHitTime <= COMBO_WINDOW_TICKS) {
            return Math.min(MAX_COMBO_COUNT, Math.max(1, previousComboCount + 1));
        }
        return 1;
    }

    static double comboMultiplier(int comboCount) {
        int normalized = Math.max(1, Math.min(MAX_COMBO_COUNT, comboCount));
        return 1.0D + (0.07D * (normalized - 1));
    }

    private static float comboBonusDamage(int comboCount) {
        return (float) ((comboMultiplier(comboCount) - 1.0D) * BASE_DAMAGE);
    }

    private static void setComboState(ItemStack stack, long currentTime, int comboCount) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(NBT_TAG_LAST_COMBO_TIME, currentTime);
            tag.putInt(NBT_TAG_COMBO_COUNT, comboCount);
        });
    }
}
