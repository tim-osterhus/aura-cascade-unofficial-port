package pixlepix.auracascade.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pixlepix.auracascade.item.ConsumerItemKeepAlive;
import pixlepix.auracascade.item.ConsumerItemLifetimeAccess;

@Mixin(ItemEntity.class)
abstract class ConsumerItemLifetimeMixin implements ConsumerItemLifetimeAccess {
    @Shadow private int age;
    @Unique private boolean aura$consumerKeptAlive;

    @Override
    public boolean aura$isConsumerKeptAlive() {
        return aura$consumerKeptAlive;
    }

    @Override
    public void aura$extendConsumerLifetime() {
        aura$consumerKeptAlive = true;
    }

    @Override
    public void aura$keepAlive() {
        aura$extendConsumerLifetime();
        age = 0;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aura$saveConsumerLifetime(ValueOutput output, CallbackInfo ci) {
        if (aura$consumerKeptAlive) {
            output.putBoolean(ConsumerItemKeepAlive.PROTECTED_TAG, true);
            // Vanilla Age is a short, insufficient for the extended lifetime.
            output.putInt(ConsumerItemKeepAlive.AGE_TAG, age);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aura$loadConsumerLifetime(ValueInput input, CallbackInfo ci) {
        aura$consumerKeptAlive = input.getBooleanOr(ConsumerItemKeepAlive.PROTECTED_TAG, false);
        if (aura$consumerKeptAlive) {
            age = input.getIntOr(ConsumerItemKeepAlive.AGE_TAG, age);
        }
    }

    @ModifyConstant(method = "isMergable", constant = @Constant(intValue = 6_000))
    private int aura$consumerMergeLifetime(int vanillaLifetime) {
        return ConsumerItemKeepAlive.effectiveLifetime(aura$consumerKeptAlive, vanillaLifetime);
    }

    @Inject(method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("HEAD"))
    private static void aura$preserveMergedLifetime(ItemEntity target, ItemStack targetStack,
                                                  ItemEntity source, ItemStack sourceStack, CallbackInfo ci) {
        if (((ConsumerItemLifetimeAccess) source).aura$isConsumerKeptAlive()) {
            ((ConsumerItemLifetimeAccess) target).aura$extendConsumerLifetime();
        }
    }
}
