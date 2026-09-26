package pixlepix.auracascade.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import pixlepix.auracascade.fairy.FairySystem;

@Mixin(LivingEntity.class)
public abstract class FairyFallDistanceMixin {
    @ModifyVariable(
        method = "causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private double aura$applyGliderFairies(double distance) {
        if ((Object) this instanceof ServerPlayer player) {
            float adjusted = FairySystem.applyGliderFairies(player, (float) distance);
            return adjusted == (float) distance ? distance : adjusted;
        }
        return distance;
    }
}
