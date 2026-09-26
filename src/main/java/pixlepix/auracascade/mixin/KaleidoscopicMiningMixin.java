package pixlepix.auracascade.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;

@Mixin(Player.class)
abstract class KaleidoscopicMiningMixin {
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float aura$modifyKaleidoscopicBreakSpeed(float original, BlockState state) {
        return KaleidoscopicOriginalEffects.modifyBreakSpeed((Player) (Object) this, state, original);
    }
}
