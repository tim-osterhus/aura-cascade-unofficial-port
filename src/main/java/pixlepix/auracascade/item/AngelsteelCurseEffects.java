package pixlepix.auracascade.item;

import java.util.EnumMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.parity.AuraColor;

public final class AngelsteelCurseEffects {
    private static final EnumMap<AuraColor, Holder<MobEffect>> EFFECTS = new EnumMap<>(AuraColor.class);
    private static boolean bootstrapped;

    private AngelsteelCurseEffects() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        for (AuraColor color : AuraColor.values()) {
            if (!isAttunedColor(color)) {
                continue;
            }
            Identifier id = Identifier.fromNamespaceAndPath(
                AuraCascadeMod.MOD_ID,
                "angelsteel_curse_" + color.id()
            );
            EFFECTS.put(color, Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                id,
                new AngelsteelCurseMobEffect(color)
            ));
        }
        bootstrapped = true;
    }

    public static Holder<MobEffect> effect(AuraColor color) {
        if (!isAttunedColor(color)) {
            throw new IllegalArgumentException("No Angelsteel curse is defined for aura color " + color);
        }
        Holder<MobEffect> effect = EFFECTS.get(color);
        if (effect == null) {
            throw new IllegalStateException("Angelsteel curse effects have not been registered");
        }
        return effect;
    }

    static boolean isAttunedColor(AuraColor color) {
        if (color == null) {
            return false;
        }
        return switch (color) {
            case RED, ORANGE, YELLOW, GREEN, BLUE, VIOLET -> true;
            case WHITE, BLACK -> false;
        };
    }
}
