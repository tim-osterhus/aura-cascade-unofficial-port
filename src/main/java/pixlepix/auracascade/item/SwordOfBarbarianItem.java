package pixlepix.auracascade.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.util.NbtCompat;

public final class SwordOfBarbarianItem extends Item {
    static final String NBT_TAG_LAST_COMBO_TIME = "lastComboTime";
    static final String NBT_TAG_COMBO_COUNT = "comboCount";
    private static final int COMBO_WINDOW_TICKS = 100;
    private static final int MAX_COMBO_COUNT = 100;

    public SwordOfBarbarianItem() {
        this(pixlepix.auracascade.util.ToolPropertiesCompat.sword(new Item.Properties().stacksTo(1), AuraUtilityToolMaterials.ARCANE_SWORD, 3, -2.4F));
    }

    public SwordOfBarbarianItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
    }

    static float modifyIncomingDamage(Player attacker, ItemStack stack, float original) {
        long currentTime = attacker.level().getGameTime();
        long previousTime = lastComboTime(stack);
        int previousCombo = comboCount(stack);
        boolean continuingCombo = isWithinComboWindow(previousTime, currentTime);
        int damageCombo = continuingCombo ? previousCombo : 0;
        setComboState(stack, currentTime, nextComboCount(previousTime, currentTime, previousCombo));
        return original * (float) comboMultiplier(damageCombo);
    }

    static int comboCount(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return Math.max(0, NbtCompat.getIntOr(tag, NBT_TAG_COMBO_COUNT, 0));
    }

    static long lastComboTime(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return NbtCompat.getLongOr(tag, NBT_TAG_LAST_COMBO_TIME, 0L);
    }

    static int nextComboCount(long lastHitTime, long currentTime, int previousComboCount) {
        if (isWithinComboWindow(lastHitTime, currentTime)) {
            return Math.min(MAX_COMBO_COUNT, Math.max(1, previousComboCount + 1));
        }
        return 0;
    }

    static double comboMultiplier(int comboCount) {
        int normalized = Math.max(0, Math.min(MAX_COMBO_COUNT, comboCount));
        return Math.pow(1.05D, normalized);
    }

    static double hitMultiplier(long lastHitTime, long currentTime, int previousComboCount) {
        return comboMultiplier(isWithinComboWindow(lastHitTime, currentTime) ? previousComboCount : 0);
    }

    private static boolean isWithinComboWindow(long lastHitTime, long currentTime) {
        long elapsed = Math.abs(currentTime - lastHitTime);
        return elapsed > 4L && elapsed < COMBO_WINDOW_TICKS;
    }

    private static void setComboState(ItemStack stack, long currentTime, int comboCount) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong(NBT_TAG_LAST_COMBO_TIME, currentTime);
            tag.putInt(NBT_TAG_COMBO_COUNT, comboCount);
        });
    }
}
