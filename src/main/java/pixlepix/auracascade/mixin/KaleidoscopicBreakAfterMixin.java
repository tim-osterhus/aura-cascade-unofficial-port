package pixlepix.auracascade.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;

@Mixin(ServerPlayerGameMode.class)
abstract class KaleidoscopicBreakAfterMixin {
    @Shadow
    @Final
    private ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean aura$afterSuccessfulBreak(BlockPos pos, Operation<Boolean> original) {
        var level = player.serverLevel();
        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        boolean successful = original.call(pos);
        if (successful) {
            KaleidoscopicOriginalEffects.afterSuccessfulBlockBreak(player, pos, state, blockEntity);
        }
        return successful;
    }
}
