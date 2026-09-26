package pixlepix.auracascade.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;

@Mixin(LivingEntity.class)
abstract class KaleidoscopicLootMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("TAIL"))
    private void aura$dropKaleidoscopicBonusLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        KaleidoscopicOriginalEffects.extraLoot(level, (LivingEntity) (Object) this, source);
    }
}
