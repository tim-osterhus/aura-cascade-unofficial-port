package pixlepix.auracascade.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pixlepix.auracascade.fairy.FairySystem;

@Mixin(Player.class)
public abstract class FairyMiningSpeedMixin {
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float aura$applyDiggerFairies(float original, BlockState state) {
        return FairySystem.applyDiggerFairies((Player) (Object) this, original);
    }
}
