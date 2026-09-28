package pixlepix.auracascade.mixin;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pixlepix.auracascade.item.PortableRedHoleItem;

@Mixin(ItemEntity.class)
abstract class PortableRedHoleLifetimeMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void aura$tickRedHole(CallbackInfo ci) {
        ItemEntity item = (ItemEntity) (Object) this;
        // Set a floor, leaving NeoForge's later expiry-event extensions intact.
        item.lifespan = pixlepix.auracascade.item.ConsumerItemKeepAlive.effectiveLifetime(item,
            Math.max(item.lifespan, PortableRedHoleItem.lifetimeTicks(item.getItem(), item.lifespan)));
        if (PortableRedHoleItem.onEntityItemTick((ItemEntity) (Object) this)) {
            // Forge's onEntityItemUpdate(true) skips the rest of this item tick.
            ci.cancel();
        }
    }

}
