package pixlepix.auracascade.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pixlepix.auracascade.item.AuraItems;

@Mixin(LivingEntity.class)
abstract class ForbiddenFruitFinishMixin {
    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack aura$afterFoodFinished(ItemStack stack, Level level, LivingEntity user,
                                              Operation<ItemStack> original) {
        UseAnim useAnimation = stack.getUseAnimation();
        ItemStack usedFood = user instanceof ServerPlayer && (useAnimation == UseAnim.EAT || useAnimation == UseAnim.DRINK)
            ? stack.copy() : ItemStack.EMPTY;
        ItemStack result = original.call(stack, level, user);
        if (user instanceof ServerPlayer player && !usedFood.isEmpty()) {
            AuraItems.onFoodFinished(player, usedFood, useAnimation);
        }
        return result;
    }
}
