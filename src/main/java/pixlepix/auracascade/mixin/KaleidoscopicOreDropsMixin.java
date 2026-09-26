package pixlepix.auracascade.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;

@Mixin(Block.class)
public abstract class KaleidoscopicOreDropsMixin {
    @Redirect(method = "playerDestroy", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void aura$dropOrConvert(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                    Entity breaker, ItemStack tool) {
        KaleidoscopicOriginalEffects.dropOrConvert(state, level, pos, blockEntity, breaker, tool);
    }
}
