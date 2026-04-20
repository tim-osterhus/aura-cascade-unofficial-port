package pixlepix.auracascade.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraConsumerLogicTest {
    @Test
    void processorFamilyKeepsGeometricThroughput() {
        assertEquals(0, AuraConsumerLogic.progressStepsForTick(149, AuraConsumerVariant.PROCESSOR.powerPerProgress()));
        assertEquals(1, AuraConsumerLogic.progressStepsForTick(150, AuraConsumerVariant.PROCESSOR.powerPerProgress()));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(450, AuraConsumerVariant.PROCESSOR.powerPerProgress()));
        assertTrue(
            AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.PRISMATIC_PROCESSOR)
                > AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.PROCESSOR)
        );
    }

    @Test
    void smelterCycleFloorMatchesItsConfiguredPowerBudget() {
        assertEquals(760, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.SMELTER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(570, AuraConsumerVariant.SMELTER.powerPerProgress()));
    }

    @Test
    void growerRetainsLowMidgameCyclePower() {
        assertEquals(150, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.GROWER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(150, AuraConsumerVariant.GROWER.powerPerProgress()));
    }

    @Test
    void fisherNeedsSustainedPowerInsteadOfFlatTimerTicks() {
        assertEquals(1_800, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.FISHER));
        assertEquals(3, AuraConsumerLogic.progressStepsForTick(1_400, AuraConsumerVariant.FISHER.powerPerProgress()));
    }

    @Test
    void brewerUsesTheSteepestMidgamePowerCurve() {
        assertEquals(3_000, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.BREWER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(1_500, AuraConsumerVariant.BREWER.powerPerProgress()));
    }

    @Test
    void colorerStaysCheapButStillUsesBurstThroughput() {
        assertEquals(150, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.COLORER));
        assertEquals(3, AuraConsumerLogic.progressStepsForTick(350, AuraConsumerVariant.COLORER.powerPerProgress()));
    }

    @Test
    void synthesizerMatchesLegacyAngelsteelCycleBudget() {
        assertEquals(500_000, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.SYNTHESIZER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(30_000, AuraConsumerVariant.SYNTHESIZER.powerPerProgress()));
    }

    @Test
    void enchanterRetainsItsLongLateGameRamp() {
        assertEquals(500_000, AuraConsumerLogic.minimumCyclePower(AuraConsumerVariant.ENCHANTER));
        assertEquals(2, AuraConsumerLogic.progressStepsForTick(1_500, AuraConsumerVariant.ENCHANTER.powerPerProgress()));
    }
}
