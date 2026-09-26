package pixlepix.auracascade.item;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UtilityRuntimeContractTest {
    @Test
    void angelHeelsAddsAndRemovesOnlyItsTransientStepHeightModifier() {
        TestMinecraftBootstrap.ensureBootstrapped();
        AttributeInstance stepHeight = new AttributeInstance(Attributes.STEP_HEIGHT, ignored -> { });
        stepHeight.setBaseValue(0.6D);
        AttributeModifier otherModifier = new AttributeModifier(
            ResourceLocation.fromNamespaceAndPath("test", "step_height"),
            0.25D,
            AttributeModifier.Operation.ADD_VALUE
        );
        stepHeight.addTransientModifier(otherModifier);

        AngelHeelsRuntime.update(stepHeight, true, false);
        AngelHeelsRuntime.update(stepHeight, true, false);
        assertEquals(2.0D, stepHeight.getValue(), 0.000001D);

        AngelHeelsRuntime.update(stepHeight, true, true);
        assertEquals(2.3D, stepHeight.getValue(), 0.000001D);
        AngelHeelsRuntime.update(stepHeight, true, true);
        assertEquals(2.6D, stepHeight.getValue(), 0.000001D);
        AngelHeelsRuntime.update(stepHeight, true, false);
        assertEquals(2.0D, stepHeight.getValue(), 0.000001D);

        AngelHeelsRuntime.update(stepHeight, false, false);
        assertEquals(0.85D, stepHeight.getValue(), 0.000001D);
        assertTrue(stepHeight.hasModifier(otherModifier.id()));
        assertFalse(stepHeight.hasModifier(AngelHeelsRuntime.MODIFIER_ID));
    }

    @Test
    void yellowAndBarbarianScaleTheRawDamageValue() {
        assertEquals(0.0F, ProtectionAmuletProfile.YELLOW.healFraction());
        assertEquals(0.5F, ProtectionAmuletProfile.YELLOW.incomingDamageMultiplier());
        assertEquals(10.0F, ProtectionAmuletProfile.YELLOW.incomingDamageMultiplier() * 20.0F, 0.00001F);
        assertEquals(1.0D, SwordOfBarbarianItem.hitMultiplier(100L, 105L, 0), 0.000001D);
        assertEquals(1.05D, SwordOfBarbarianItem.hitMultiplier(105L, 110L, 1), 0.000001D);
        assertEquals(Math.pow(1.05D, 99), SwordOfBarbarianItem.hitMultiplier(1L, 6L, 99), 0.000001D);
        assertEquals(1.0D, SwordOfBarbarianItem.hitMultiplier(1L, 101L, 99), 0.000001D);
    }

    @Test
    void angelsteelMiningBuffsUseLegacyMultipliersAndFortuneGate() {
        int[] buffs = {2, 4, 1, 3};
        assertEquals(Math.pow(1.3D, 2) * Math.pow(3.0D, 3),
            AngelsteelToolHelper.destroySpeedMultiplier(buffs, 1.0F, true), 0.0001D);
        assertEquals(Math.pow(1.3D, 2),
            AngelsteelToolHelper.destroySpeedMultiplier(buffs, 1.5F, true), 0.0001D);
        assertEquals(Math.pow(1.3D, 2) * 3.0D,
            AngelsteelToolHelper.destroySpeedMultiplier(buffs, 2.0F, true), 0.0001D);
        assertEquals(1.0F, AngelsteelToolHelper.destroySpeedMultiplier(buffs, 2.0F, false));

        assertEquals(4, AngelsteelToolHelper.fortuneLevelToApply(4, 2, true, false));
        assertEquals(0, AngelsteelToolHelper.fortuneLevelToApply(2, 2, true, false));
        assertEquals(0, AngelsteelToolHelper.fortuneLevelToApply(4, 2, false, false));
        assertEquals(0, AngelsteelToolHelper.fortuneLevelToApply(4, 2, true, true));
    }

    @Test
    void redHoleUsesPersistedItemAgeThresholdAndLeavesVanillaItemsAlone() {
        assertEquals(30_000, PortableRedHoleItem.lifetimeTicks(true, 6_000));
        assertEquals(6_000, PortableRedHoleItem.lifetimeTicks(false, 6_000));
    }

    @Test
    void transmutingRequiresALivePositiveHealthTarget() {
        assertTrue(TransmutingSwordItem.shouldTransmute(true, 1.0F));
        assertFalse(TransmutingSwordItem.shouldTransmute(false, 1.0F));
        assertFalse(TransmutingSwordItem.shouldTransmute(true, 0.0F));
        assertFalse(TransmutingSwordItem.shouldTransmute(true, -1.0F));
    }

    @Test
    void angelsteelCurseCadenceAndDegreeDurationMatchTheSource() {
        assertEquals(100, AngelsteelCurseMobEffect.tickInterval(AuraColor.RED));
        assertEquals(100, AngelsteelCurseMobEffect.tickInterval(AuraColor.ORANGE));
        assertEquals(250, AngelsteelCurseMobEffect.tickInterval(AuraColor.YELLOW));
        assertEquals(40, AngelsteelCurseMobEffect.tickInterval(AuraColor.GREEN));
        assertEquals(50, AngelsteelCurseMobEffect.tickInterval(AuraColor.BLUE));
        assertEquals(60, AngelsteelCurseMobEffect.tickInterval(AuraColor.VIOLET));
        for (AuraColor color : new AuraColor[] {
            AuraColor.RED, AuraColor.ORANGE, AuraColor.YELLOW,
            AuraColor.GREEN, AuraColor.BLUE, AuraColor.VIOLET
        }) {
            assertTrue(AngelsteelCurseEffects.isAttunedColor(color));
        }
        assertFalse(AngelsteelCurseEffects.isAttunedColor(AuraColor.WHITE));
        assertFalse(AngelsteelCurseEffects.isAttunedColor(AuraColor.BLACK));
        assertThrows(IllegalArgumentException.class, () -> AngelsteelCurseEffects.effect(AuraColor.WHITE));
        assertThrows(IllegalArgumentException.class, () -> AngelsteelCurseMobEffect.tickInterval(AuraColor.BLACK));
        assertEquals(100, AngelsteelCurseMobEffect.curseDuration(0));
        assertEquals(10_100, AngelsteelCurseMobEffect.curseDuration(10));
        assertEquals(10_100, AngelsteelCurseMobEffect.curseDuration(11));
    }

    @Test
    void invalidOrUnattunedSwordColorsStayUncoloredAndHitWithRedDefault() {
        TestMinecraftBootstrap.ensureBootstrapped();
        assertEquals(Optional.of(AuraColor.BLUE), AngelsteelSwordItem.parseSwordAura("blue"));
        for (String invalid : new String[] { "", "not-a-color", "white", "black" }) {
            Optional<AuraColor> parsed = AngelsteelSwordItem.parseSwordAura(invalid);
            assertTrue(parsed.isEmpty(), invalid);
            assertEquals(AuraColor.RED, AngelsteelSwordItem.auraForHit(parsed), invalid);
        }
    }
}
