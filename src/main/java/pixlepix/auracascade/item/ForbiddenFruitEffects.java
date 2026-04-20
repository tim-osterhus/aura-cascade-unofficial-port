package pixlepix.auracascade.item;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ForbiddenFruitEffects {
    private static final List<String> EFFECT_IDS = List.of(
        "invisibility",
        "movement_speed",
        "jump_boost",
        "water_breathing",
        "glowing",
        "poison",
        "weakness",
        "hunger"
    );

    private ForbiddenFruitEffects() {
    }

    public static EffectDescriptor descriptorFor(ItemStack foodStack) {
        return descriptorForItemId(BuiltInRegistries.ITEM.getKey(foodStack.getItem()).toString());
    }

    public static EffectDescriptor descriptorForItemId(String itemId) {
        if ("minecraft:apple".equals(itemId)) {
            return new EffectDescriptor("regeneration", 200, 0);
        }

        int baseHash = itemId.hashCode();
        String effectId = EFFECT_IDS.get(Math.floorMod(baseHash, EFFECT_IDS.size()));
        int duration = 200 + (Math.floorMod(baseHash >>> 3, 5) * 80);
        int amplifier = Math.floorMod(baseHash >>> 1, 2);
        return new EffectDescriptor(effectId, duration, amplifier);
    }

    public static void apply(Player player, ItemStack foodStack) {
        EffectDescriptor descriptor = descriptorFor(foodStack);
        player.addEffect(new MobEffectInstance(effectFor(descriptor.effectId()), descriptor.duration(), descriptor.amplifier()));
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectFor(String effectId) {
        return switch (effectId) {
            case "regeneration" -> MobEffects.REGENERATION;
            case "movement_speed" -> MobEffects.SPEED;
            case "jump_boost" -> MobEffects.JUMP_BOOST;
            case "water_breathing" -> MobEffects.WATER_BREATHING;
            case "glowing" -> MobEffects.GLOWING;
            case "poison" -> MobEffects.POISON;
            case "weakness" -> MobEffects.WEAKNESS;
            case "hunger" -> MobEffects.HUNGER;
            case "invisibility" -> MobEffects.INVISIBILITY;
            default -> MobEffects.NAUSEA;
        };
    }

    public record EffectDescriptor(String effectId, int duration, int amplifier) {
    }
}
