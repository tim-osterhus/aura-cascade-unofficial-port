package pixlepix.auracascade.fairy;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FairyRoleLogicTest {
    @Test
    void shooterReleasesDeadOrStoppedTrackedArrows() {
        assertTrue(FairyRoleLogic.retainTrackedArrow(true, 0.5D, 0.0D));
        assertTrue(FairyRoleLogic.retainTrackedArrow(true, 0.0D, -0.1D));
        assertFalse(FairyRoleLogic.retainTrackedArrow(true, 0.0D, 0.0D));
        assertFalse(FairyRoleLogic.retainTrackedArrow(false, 0.5D, 0.0D));
    }

    @Test
    void legacyRoleOrderMatchesTheRecoveredArtifactSequence() {
        assertEquals(FairyRole.BASIC, FairyRole.byLegacyIndex(0));
        assertEquals(FairyRole.FIGHTER, FairyRole.byLegacyIndex(1));
        assertEquals(FairyRole.DEBUFFER, FairyRole.byLegacyIndex(2));
        assertEquals(FairyRole.FETCHER, FairyRole.byLegacyIndex(8));
        assertEquals(FairyRole.GLIDER, FairyRole.byLegacyIndex(14));
        assertEquals(FairyRole.LIGHTER, FairyRole.byLegacyIndex(15));
        assertEquals(FairyRole.TRAINER, FairyRole.byLegacyIndex(16));
        for (FairyRole role : FairyRole.values()) {
            assertEquals(role, FairyRole.byLegacyIndex(role.legacyIndex()));
            assertEquals(role, FairyRole.byStoredIndex(role.storedIndex()));
        }
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
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.DIGGER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.CREATE_LIGHT, FairyRoleLogic.primaryAction(FairyRole.LIGHTER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.NONE, FairyRoleLogic.primaryAction(FairyRole.GLIDER, crowdedCombat));
        assertEquals(FairyRoleLogic.Action.GRANT_EXPERIENCE, FairyRoleLogic.primaryAction(FairyRole.TRAINER, crowdedCombat));
    }

    @Test
    void everyRecoveredRoleHasAnExplicitDistinctPrimaryContract() {
        FairyRoleLogic.Observation active = new FairyRoleLogic.Observation(true, true, true, true, true, true, true, true, true);
        FairyRoleLogic.Action[] expected = {
            FairyRoleLogic.Action.NONE,
            FairyRoleLogic.Action.ATTACK_HOSTILE,
            FairyRoleLogic.Action.DEBUFF_HOSTILE,
            FairyRoleLogic.Action.BUFF_OWNER,
            FairyRoleLogic.Action.STEAL_FROM_NEARBY_PLAYER,
            FairyRoleLogic.Action.PUSH_HOSTILE,
            FairyRoleLogic.Action.EMPOWER_PROJECTILE,
            FairyRoleLogic.Action.CLUTCH_ATTACK,
            FairyRoleLogic.Action.FETCH_ITEMS,
            FairyRoleLogic.Action.SUMMON_PASSIVE,
            FairyRoleLogic.Action.BREED_ANIMALS,
            FairyRoleLogic.Action.NONE,
            FairyRoleLogic.Action.EXTINGUISH_FIRE,
            FairyRoleLogic.Action.NONE,
            FairyRoleLogic.Action.NONE,
            FairyRoleLogic.Action.CREATE_LIGHT,
            FairyRoleLogic.Action.GRANT_EXPERIENCE
        };

        FairyRole[] roles = FairyRole.values();
        assertEquals(expected.length, roles.length);
        for (int index = 0; index < roles.length; index++) {
            assertEquals(expected[index], FairyRoleLogic.primaryAction(roles[index], active), roles[index].id());
        }
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

    @Test
    void roleActionsRequireTheCanonicalEntityAndItsLiveRingSlot() {
        List<FairyRole> roles = List.of(FairyRole.FIGHTER, FairyRole.GLIDER);

        assertTrue(FairyRoleLogic.isAuthorized(true, roles, 1, FairyRole.GLIDER));
        assertFalse(FairyRoleLogic.isAuthorized(false, roles, 1, FairyRole.GLIDER));
        assertFalse(FairyRoleLogic.isAuthorized(true, roles, 1, FairyRole.DIGGER));
        assertFalse(FairyRoleLogic.isAuthorized(true, roles, 2, FairyRole.GLIDER));
    }

    @Test
    void legacyDiggerAndGliderMultipliersUseDistinctEntityCounts() {
        assertEquals(1.0D / 1.08D, FairyRoleLogic.diggerSpeedMultiplier(0), 0.000001D);
        assertEquals(1.0D, FairyRoleLogic.diggerSpeedMultiplier(1), 0.000001D);
        assertEquals(1.08D, FairyRoleLogic.diggerSpeedMultiplier(2), 0.000001D);
        assertEquals(Math.pow(1.08D, 15), FairyRoleLogic.diggerSpeedMultiplier(40), 0.000001D);
        assertEquals(1.0D, FairyRoleLogic.gliderFallMultiplier(0), 0.000001D);
        assertEquals(0.25D, FairyRoleLogic.gliderFallMultiplier(2), 0.000001D);
    }

    @Test
    void scarerOnlyRollsWhenAtLeastOneFairyIsInRange() {
        assertFalse(FairyRoleLogic.deniesNaturalSpawn(0, 0));
        assertTrue(FairyRoleLogic.deniesNaturalSpawn(1, 1));
        assertFalse(FairyRoleLogic.deniesNaturalSpawn(1, 2));
        assertTrue(FairyRoleLogic.deniesNaturalSpawn(25, 24));
    }
}
