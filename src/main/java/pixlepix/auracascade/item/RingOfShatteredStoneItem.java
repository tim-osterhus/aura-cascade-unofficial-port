package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.compat.AuraAccessorySlot;

public final class RingOfShatteredStoneItem extends AuraAccessoryItem {
    private static final double EXPLOSION_SEARCH_RADIUS = 3.0D;
    private static final Set<Block> LEGACY_EXPLOSION_TERRAIN = Set.of(
        Blocks.GRASS_BLOCK,
        Blocks.SANDSTONE,
        Blocks.CHISELED_SANDSTONE,
        Blocks.CUT_SANDSTONE,
        Blocks.STONE,
        Blocks.GRANITE,
        Blocks.POLISHED_GRANITE,
        Blocks.DIORITE,
        Blocks.POLISHED_DIORITE,
        Blocks.ANDESITE,
        Blocks.POLISHED_ANDESITE,
        Blocks.SAND,
        Blocks.RED_SAND,
        Blocks.DIRT,
        Blocks.COARSE_DIRT,
        Blocks.PODZOL,
        Blocks.COBBLESTONE,
        Blocks.GRAVEL
    );

    public RingOfShatteredStoneItem(Item.Properties properties) {
        super(AuraAccessorySlot.RING, properties);
    }

    static AABB explosionSearchBounds(Vec3 center) {
        return new AABB(center, center).inflate(EXPLOSION_SEARCH_RADIUS);
    }

    static boolean isLegacyExplosionTerrain(BlockState state) {
        return state != null && LEGACY_EXPLOSION_TERRAIN.contains(state.getBlock());
    }

    static boolean shouldPreserveExplosionBlock(BlockState state) {
        return state != null && !state.isAir() && !isLegacyExplosionTerrain(state);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext tooltipContext,
        TooltipDisplay tooltipDisplay,
        Consumer<Component> tooltip,
        TooltipFlag tooltipFlag
    ) {
        tooltip.accept(Component.translatable("tooltip.aura.ring_of_shattered_stone.residual").withStyle(ChatFormatting.DARK_GRAY));
    }
}

final class RingOfShatteredStoneRuntime {
    private RingOfShatteredStoneRuntime() {
    }

    static boolean blocksExplosionDamage(ProtectionAmuletProfile.DamageFamily family, boolean ringEquipped) {
        // The shipped ring has no wearer-damage hook; its effect is limited to explosion blocks.
        return false;
    }

    static List<BlockPos> filterProtectedExplosionBlocks(
        List<Vec3> ringWearerPositions,
        List<BlockPos> affectedBlocks,
        java.util.function.Function<BlockPos, BlockState> stateLookup
    ) {
        if (ringWearerPositions.isEmpty()) {
            return affectedBlocks;
        }

        ArrayList<BlockPos> blocksLeftToExplode = new ArrayList<>(affectedBlocks.size());
        for (BlockPos pos : affectedBlocks) {
            if (!RingOfShatteredStoneItem.shouldPreserveExplosionBlock(stateLookup.apply(pos))) {
                blocksLeftToExplode.add(pos);
            }
        }
        return blocksLeftToExplode;
    }
}
