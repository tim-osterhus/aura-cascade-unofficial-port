package pixlepix.auracascade.item;

import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.parity.AuraColor;

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
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        int duration = (degreeIndex * degreeIndex * 100) + 100;
        switch (swordAura(stack).orElse(AuraColor.RED)) {
            case RED -> target.igniteForSeconds(Math.max(4, duration / 20));
            case ORANGE -> target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, duration / 2, Math.max(0, degreeIndex / 3)));
            case YELLOW -> target.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0));
            case GREEN -> target.addEffect(new MobEffectInstance(MobEffects.POISON, duration, Math.max(0, degreeIndex / 4)));
            case BLUE -> {
                if (target.getHealth() <= (target.getMaxHealth() / 2.0F)) {
                    target.hurt(target.damageSources().magic(), Math.max(2.0F, degreeIndex + 1.0F));
                }
            }
            case VIOLET -> target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, duration / 2, 0));
            default -> {
            }
        }
    }

    @Override
    protected Optional<AuraColor> swordAura(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        String colorId = tag.getString(AngelsteelToolHelper.NBT_AURA_NAME).orElse("");
        if (colorId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AuraColor.byId(colorId));
    }
}
