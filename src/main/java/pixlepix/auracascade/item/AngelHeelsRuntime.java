package pixlepix.auracascade.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import pixlepix.auracascade.AuraCascadeMod;

public final class AngelHeelsRuntime {
    public static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, "angel_heels_step_height");
    public static final double TARGET_STEP_HEIGHT = 2.0D;
    public static final double COLLISION_INCREMENT = 0.3D;

    private AngelHeelsRuntime() {
    }

    public static void update(AttributeInstance stepHeight, boolean equipped, boolean horizontalCollision) {
        if (stepHeight == null) {
            return;
        }
        AttributeModifier previous = stepHeight.getModifier(MODIFIER_ID);
        if (!equipped) {
            stepHeight.removeModifier(MODIFIER_ID);
            return;
        }

        if (previous != null) {
            stepHeight.removeModifier(MODIFIER_ID);
        }
        double withoutAura = stepHeight.getValue();
        double baselineBonus = Math.max(0.0D, TARGET_STEP_HEIGHT - withoutAura);
        double collisionRamp = previous == null ? 0.0D : Math.max(0.0D, previous.amount() - baselineBonus);
        double amount = baselineBonus + (horizontalCollision ? collisionRamp + COLLISION_INCREMENT : 0.0D);
        if (amount > 0.0D) {
            stepHeight.addTransientModifier(new AttributeModifier(
                MODIFIER_ID,
                amount,
                AttributeModifier.Operation.ADD_VALUE
            ));
        }
    }
}
