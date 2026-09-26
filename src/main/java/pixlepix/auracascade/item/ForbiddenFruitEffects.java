package pixlepix.auracascade.item;

import java.util.List;
import java.util.Random;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class ForbiddenFruitEffects {
    private static final List<LegacyPotion> LEGACY_POTIONS = List.of(
        new LegacyPotion("speed", true),
        new LegacyPotion("slowness", false),
        new LegacyPotion("haste", true),
        new LegacyPotion("mining_fatigue", false),
        new LegacyPotion("strength", true),
        new LegacyPotion("instant_health", true),
        new LegacyPotion("instant_damage", false),
        new LegacyPotion("jump_boost", true),
        new LegacyPotion("nausea", false),
        new LegacyPotion("regeneration", true),
        new LegacyPotion("resistance", true),
        new LegacyPotion("fire_resistance", true),
        new LegacyPotion("water_breathing", true),
        new LegacyPotion("invisibility", true),
        new LegacyPotion("blindness", false),
        new LegacyPotion("night_vision", true),
        new LegacyPotion("hunger", false),
        new LegacyPotion("weakness", false),
        new LegacyPotion("poison", false),
        new LegacyPotion("wither", false),
        new LegacyPotion("health_boost", true),
        new LegacyPotion("absorption", true),
        new LegacyPotion("saturation", true)
    );

    private ForbiddenFruitEffects() {
    }

    static boolean acceptsCompletedUse(UseAnim useAnimation, boolean alive, boolean spectator, boolean equipped) {
        return alive && !spectator && equipped && (useAnimation == UseAnim.EAT || useAnimation == UseAnim.DRINK);
    }

    public static EffectDescriptor descriptorFor(ItemStack foodStack) {
        return descriptorForItemId(BuiltInRegistries.ITEM.getKey(foodStack.getItem()).toString());
    }

    public static EffectDescriptor descriptorForItemId(String itemId) {
        if ("minecraft:apple".equals(itemId)) {
            return new EffectDescriptor("regeneration", 7_200, 1);
        }

        Random random = new Random(legacyUnlocalizedName(itemId).hashCode());
        LegacyPotion potion;
        do {
            potion = LEGACY_POTIONS.get(random.nextInt(LEGACY_POTIONS.size()));
        } while (!potion.beneficial());

        int duration = Math.max(0, (int) (4_800.0D + random.nextGaussian() * 2_400.0D));
        return new EffectDescriptor(potion.effectId(), duration, random.nextInt(6));
    }

    private static String legacyUnlocalizedName(String itemId) {
        int namespaceSeparator = itemId.indexOf(':');
        String path = namespaceSeparator < 0 ? itemId : itemId.substring(namespaceSeparator + 1);
        return "item." + path;
    }

    public static void apply(Player player, ItemStack foodStack) {
        EffectDescriptor descriptor = descriptorFor(foodStack);
        player.addEffect(new MobEffectInstance(effectFor(descriptor.effectId()), descriptor.duration(), descriptor.amplifier()));
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effectFor(String effectId) {
        return switch (effectId) {
            case "regeneration" -> MobEffects.REGENERATION;
            case "speed" -> MobEffects.MOVEMENT_SPEED;
            case "haste" -> MobEffects.DIG_SPEED;
            case "strength" -> MobEffects.DAMAGE_BOOST;
            case "instant_health" -> MobEffects.HEAL;
            case "jump_boost" -> MobEffects.JUMP;
            case "resistance" -> MobEffects.DAMAGE_RESISTANCE;
            case "fire_resistance" -> MobEffects.FIRE_RESISTANCE;
            case "water_breathing" -> MobEffects.WATER_BREATHING;
            case "invisibility" -> MobEffects.INVISIBILITY;
            case "night_vision" -> MobEffects.NIGHT_VISION;
            case "health_boost" -> MobEffects.HEALTH_BOOST;
            case "absorption" -> MobEffects.ABSORPTION;
            case "saturation" -> MobEffects.SATURATION;
            default -> MobEffects.CONFUSION;
        };
    }

    private record LegacyPotion(String effectId, boolean beneficial) {
    }

    public record EffectDescriptor(String effectId, int duration, int amplifier) {
    }
}
