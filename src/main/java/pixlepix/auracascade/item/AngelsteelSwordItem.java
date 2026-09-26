package pixlepix.auracascade.item;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.NbtCompat;

public final class AngelsteelSwordItem extends AngelsteelToolItem {
    public AngelsteelSwordItem(int degreeIndex) {
        super(AngelsteelToolKind.SWORD, degreeIndex);
    }

    public AngelsteelSwordItem(int degreeIndex, net.minecraft.world.item.Item.Properties properties) {
        super(AngelsteelToolKind.SWORD, degreeIndex, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Optional<AuraColor> aura = swordAura(stack);
        if (aura.isEmpty()) {
            return super.getName(stack);
        }
        return Component.literal(aura.get().displayName() + " ")
            .withStyle(ChatFormatting.RESET)
            .append(super.getName(stack));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.level().isClientSide()) {
            AuraColor color = auraForHit(swordAura(stack));
            int degree = Math.max(0, Math.min(10, degreeIndex));
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                AngelsteelCurseEffects.effect(color),
                AngelsteelCurseMobEffect.curseDuration(degree),
                degree
            ));
        }
        return true;
    }

    @Override
    public Optional<AuraColor> swordAura(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        String colorId = NbtCompat.getStringOr(tag, AngelsteelToolHelper.NBT_AURA_NAME, "");
        return parseSwordAura(colorId);
    }

    static Optional<AuraColor> parseSwordAura(String colorId) {
        if (colorId == null || colorId.isBlank()) {
            return Optional.empty();
        }
        try {
            AuraColor color = AuraColor.byId(colorId);
            return AngelsteelCurseEffects.isAttunedColor(color) ? Optional.of(color) : Optional.empty();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    static AuraColor auraForHit(Optional<AuraColor> aura) {
        return aura.orElse(AuraColor.RED);
    }
}
