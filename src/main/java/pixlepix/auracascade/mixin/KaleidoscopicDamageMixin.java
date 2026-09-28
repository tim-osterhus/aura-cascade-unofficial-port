package pixlepix.auracascade.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.Stack;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import pixlepix.auracascade.enchantment.KaleidoscopicOriginalEffects;
import pixlepix.auracascade.item.AuraItems;

@Mixin(LivingEntity.class)
abstract class KaleidoscopicDamageMixin {
    @Shadow protected Stack<DamageContainer> damageContainers;

    @ModifyArgs(method = "hurt", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void aura$modifyKaleidoscopicDamage(Args args) {
        LivingEntity victim = (LivingEntity) (Object) this;
        DamageSource source = args.get(0);
        float amount = damageContainers.peek().getNewDamage();
        float enchantedDamage = KaleidoscopicOriginalEffects.modifyDamage(victim, source, amount);
        float modifiedDamage = AuraItems.modifyPreMitigationDamage(victim, source, enchantedDamage);
        // NeoForge's actuallyHurt reads its container, not just the method argument.
        damageContainers.peek().setNewDamage(modifiedDamage);
        args.set(1, modifiedDamage);
    }
}
