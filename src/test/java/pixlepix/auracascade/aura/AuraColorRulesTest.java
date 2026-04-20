package pixlepix.auracascade.aura;

import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraColorRulesTest {
    @Test
    void preservesLegacyWeightAndMovementRules() {
        assertEquals(0.0D, AuraColor.BLACK.relativeMass(AuraEnvironment.CLEAR_DAY));
        assertEquals(0.0D, AuraColor.ORANGE.relativeMass(AuraEnvironment.CLEAR_DAY));
        assertFalse(AuraColor.BLACK.canNaturallyFlowHorizontally());
        assertFalse(AuraColor.ORANGE.canNaturallyFlowVertically());
        assertFalse(AuraColor.BLACK.supportsControlledUpwardFlow());
        assertFalse(AuraColor.ORANGE.supportsControlledUpwardFlow());
        assertFalse(AuraColor.BLACK.generatesFallingPower());
        assertTrue(AuraColor.ORANGE.inducesCurrentOnTransfer());
    }

    @Test
    void appliesEnvironmentSensitiveMassAndAscentBoosts() {
        assertEquals(2.0D, AuraColor.GREEN.relativeMass(AuraEnvironment.CLEAR_DAY));
        assertEquals(0.5D, AuraColor.GREEN.relativeMass(AuraEnvironment.CLEAR_NIGHT));
        assertEquals(2.0D, AuraColor.YELLOW.ascentBoost(AuraEnvironment.CLEAR_DAY));
        assertEquals(0.5D, AuraColor.BLUE.ascentBoost(AuraEnvironment.CLEAR_DAY));
        assertEquals(4.0D, AuraColor.BLUE.ascentBoost(AuraEnvironment.RAINING_DAY));
    }

    @Test
    void appliesYellowDecayAndVioletGrowthCurves() {
        AuraTickContext phaseTick = new AuraTickContext(5L, AuraEnvironment.CLEAR_DAY);

        assertEquals(80, AuraColor.YELLOW.applyPassiveTick(100, phaseTick));
        assertEquals(0, AuraColor.VIOLET.applyPassiveTick(10, phaseTick));
        assertEquals(225, AuraColor.VIOLET.applyPassiveTick(100, phaseTick));
        assertEquals(0, AuraColor.VIOLET.applyPassiveTick(2601, phaseTick));
        assertEquals(100, AuraColor.VIOLET.applyPassiveTick(100, new AuraTickContext(6L, AuraEnvironment.CLEAR_DAY)));
    }
}
