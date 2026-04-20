package pixlepix.auracascade.fairy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FairyRoleLogicTest {
    @Test
    void legacyRoleOrderMatchesTheRecoveredArtifactSequence() {
        assertEquals(FairyRole.BASIC, FairyRole.byLegacyIndex(0));
        assertEquals(FairyRole.FIGHTER, FairyRole.byLegacyIndex(1));
        assertEquals(FairyRole.DEBUFFER, FairyRole.byLegacyIndex(2));
        assertEquals(FairyRole.FETCHER, FairyRole.byLegacyIndex(8));
        assertEquals(FairyRole.LIGHTER, FairyRole.byLegacyIndex(14));
        assertEquals(FairyRole.TRAINER, FairyRole.byLegacyIndex(16));
    }

    @Test
    void userFacingNamesResolveToTheRecoveredFairyLabels() {
        assertEquals(FairyRole.BASIC, FairyRole.defaultRole());
        assertEquals(FairyRole.BASIC, FairyRole.fromUserFacingName("Fairy"));
        assertEquals(FairyRole.FIGHTER, FairyRole.fromUserFacingName("Fighter Fairy"));
        assertEquals(FairyRole.TRAINER, FairyRole.fromUserFacingName("Training Fairy"));
    }

    @Test
    void representativeRoleProfilesStayDistinct() {
        FairyRoleLogic.Observation crowdedCombat = new FairyRoleLogic.Observation(true, true, true, true, true, true, true, true, true);

        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.BASIC, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.ATTACK_HOSTILE, FairyRoleLogic.primaryAction(FairyRole.FIGHTER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.DEBUFF_HOSTILE, FairyRoleLogic.primaryAction(FairyRole.DEBUFFER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.BUFF_OWNER, FairyRoleLogic.primaryAction(FairyRole.BUFFER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.FETCH_ITEMS, FairyRoleLogic.primaryAction(FairyRole.FETCHER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.EXTINGUISH_FIRE, FairyRoleLogic.primaryAction(FairyRole.EXTINGUISHER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.SLOW_FALL, FairyRoleLogic.primaryAction(FairyRole.GLIDER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.GRANT_EXPERIENCE, FairyRoleLogic.primaryAction(FairyRole.TRAINER, crowdedCombat));
    }

    @Test
    void rolesStayIdleWhenTheirTriggerConditionIsMissing() {
        FairyRoleLogic.Observation quietState = new FairyRoleLogic.Observation(false, false, false, false, false, false, false, false, false);

        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.BASIC, quietState));
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.FIGHTER, quietState));
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.STEALER, quietState));
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.FETCHER, quietState));
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.LIGHTER, quietState));
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.GLIDER, quietState));
    }
}
