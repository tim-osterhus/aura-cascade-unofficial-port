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
        method = "causeFallDamage(FFLnet/minecraft/world/damagesource/DamageSource;)Z",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private float aura$applyGliderFairies(float distance) {
        if ((Object) this instanceof ServerPlayer player) {
            return FairySystem.applyGliderFairies(player, distance);
        }
        return distance;
    }
}
