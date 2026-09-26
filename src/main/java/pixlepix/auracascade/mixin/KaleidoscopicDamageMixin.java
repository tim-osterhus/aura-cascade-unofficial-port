package pixlepix.auracascade.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;
import pixlepix.auracascade.item.AuraItems;

@Mixin(LivingEntity.class)
abstract class KaleidoscopicDamageMixin {
    @ModifyArgs(method = "hurt", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void aura$modifyKaleidoscopicDamage(Args args) {
        LivingEntity victim = (LivingEntity) (Object) this;
        DamageSource source = args.get(0);
        float amount = args.get(1);
        float enchantedDamage = KaleidoscopicOriginalEffects.modifyDamage(victim, source, amount);
        args.set(1, AuraItems.modifyPreMitigationDamage(victim, source, enchantedDamage));
    }
}
