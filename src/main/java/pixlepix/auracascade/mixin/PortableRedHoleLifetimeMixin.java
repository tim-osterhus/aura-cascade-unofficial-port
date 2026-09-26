package pixlepix.auracascade.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pixlepix.auracascade.item.PortableRedHoleItem;

@Mixin(ItemEntity.class)
abstract class PortableRedHoleLifetimeMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aura$tickRedHole(CallbackInfo ci) {
        if (PortableRedHoleItem.onEntityItemTick((ItemEntity) (Object) this)) {
            // Forge's onEntityItemUpdate(true) skips the rest of this item tick.
            ci.cancel();
        }
    }

    @ModifyConstant(method = "tick", constant = @Constant(intValue = 6_000))
    private int aura$extendRedHoleLifetime(int vanillaLifetime) {
        ItemEntity item = (ItemEntity) (Object) this;
        return pixlepix.auracascade.item.ConsumerItemKeepAlive.effectiveLifetime(item,
            PortableRedHoleItem.lifetimeTicks(item.getItem(), vanillaLifetime));
    }
}
