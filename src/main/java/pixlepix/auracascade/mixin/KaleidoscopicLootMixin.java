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
    // Generate while NeoForge is still capturing drops, before LivingDropsEvent can cancel them.
    @Inject(method = "dropAllDeathLoot", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;captureDrops(Ljava/util/Collection;)Ljava/util/Collection;",
        ordinal = 1))
    private void aura$dropKaleidoscopicBonusLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
        KaleidoscopicOriginalEffects.extraLoot(level, (LivingEntity) (Object) this, source);
    }
}
