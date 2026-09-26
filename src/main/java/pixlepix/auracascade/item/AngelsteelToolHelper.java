package pixlepix.auracascade.item;

import java.util.Arrays;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.util.NbtCompat;

public final class AngelsteelToolHelper {
    public static final int MAX_DEGREE = 12;
    public static final String NBT_BUFF_ARRAY_NAME = "angelbuffs";
    public static final String NBT_AURA_NAME = "aura";
    public static final TagKey<net.minecraft.world.item.Item> REPAIR_ITEMS = TagKey.create(
        net.minecraft.core.registries.Registries.ITEM,
        Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, "angelsteel_ingots")
    );

    private static final ToolMaterial[] MATERIALS = new ToolMaterial[MAX_DEGREE];

    static {
        for (int degreeIndex = 0; degreeIndex < MAX_DEGREE; degreeIndex++) {
            MATERIALS[degreeIndex] = new ToolMaterial(
                BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
                10,
                (float) Math.floor(5.0D * Math.pow(1.15D, degreeIndex)),
                (float) Math.floor(3.0D * Math.pow(1.15D, degreeIndex)),
                10,
                REPAIR_ITEMS
            );
        }
    }

    private AngelsteelToolHelper() {
    }

    public static ToolMaterial material(int degreeIndex) {
        return MATERIALS[clampDegree(degreeIndex)];
    }

    public static int clampDegree(int degreeIndex) {
        return Math.max(0, Math.min(MAX_DEGREE - 1, degreeIndex));
    }

    public static int displayedDegree(int degreeIndex) {
        return clampDegree(degreeIndex) + 1;
    }

    public static int[] randomBuffSet(int degreeIndex, net.minecraft.util.RandomSource random) {
        int[] buffs = new int[4];
        int pointCount = clampDegree(degreeIndex) * 2;
        for (int point = 0; point < pointCount; point++) {
            buffs[random.nextInt(buffs.length)]++;
        }
        return buffs;
    }

    public static int[] getBuffs(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        int[] raw = NbtCompat.getIntArrayOr(tag, NBT_BUFF_ARRAY_NAME, new int[0]);
        if (raw.length == 4) {
            return raw;
        }
        int[] normalized = new int[4];
        System.arraycopy(raw, 0, normalized, 0, Math.min(raw.length, normalized.length));
        return normalized;
    }

    public static void ensureBuffs(ItemStack stack, int degreeIndex, net.minecraft.util.RandomSource random) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            int[] existing = NbtCompat.getIntArrayOr(tag, NBT_BUFF_ARRAY_NAME, new int[0]);
            if (existing.length != 4) {
                tag.putIntArray(NBT_BUFF_ARRAY_NAME, randomBuffSet(degreeIndex, random));
            }
        });
    }

    public static boolean isAngelsteelMiningTool(ItemStack stack) {
        return stack.getItem() instanceof AngelsteelToolItem item && item.supportsMiningBuffs();
    }

    public static float destroySpeedMultiplier(int[] buffs, float hardness, boolean correctTool) {
        if (!correctTool) {
            return 1.0F;
        }
        double multiplier = Math.pow(1.3D, Math.max(0, buffs[0]));
        if (hardness <= 1.0F) {
            multiplier *= Math.pow(3.0D, Math.max(0, buffs[3]));
        }
        if (hardness >= 2.0F) {
            multiplier *= Math.pow(3.0D, Math.max(0, buffs[2]));
        }
        return (float) multiplier;
    }

    public static float destroySpeedMultiplier(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        if (!isAngelsteelMiningTool(stack)) {
            return 1.0F;
        }
        int[] buffs = getBuffs(stack);
        float hardness = state.getDestroySpeed(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO);
        return destroySpeedMultiplier(buffs, hardness, stack.isCorrectToolForDrops(state));
    }

    public static int fortuneLevelToApply(int buffFortune, int vanillaFortune, boolean correctTool, boolean crop) {
        return correctTool && !crop && buffFortune > vanillaFortune ? buffFortune : 0;
    }

    public static int fortuneLevel(ItemStack stack) {
        return getBuffs(stack)[1];
    }

    public static ItemStack dropFortuneTool(ItemStack stack, BlockState state, Holder<Enchantment> fortune) {
        if (!isAngelsteelMiningTool(stack)) {
            return stack;
        }
        int level = fortuneLevelToApply(fortuneLevel(stack), stack.getEnchantments().getLevel(fortune),
            stack.isCorrectToolForDrops(state), state.getBlock() instanceof CropBlock);
        if (level <= 0) {
            return stack;
        }
        // Drop-local enchantment only: recursive breaks must never mutate the held tool.
        ItemStack lootTool = stack.copy();
        lootTool.enchant(fortune, level);
        return lootTool;
    }

    public static int totalBuffPoints(ItemStack stack) {
        return Arrays.stream(getBuffs(stack)).sum();
    }

}
