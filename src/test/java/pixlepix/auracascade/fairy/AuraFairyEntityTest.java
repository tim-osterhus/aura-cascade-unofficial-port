package pixlepix.auracascade.fairy;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraFairyEntityTest {
    @Test
    void ownerRoleAndSlotSurviveEntityPersistence() {
        UUID owner = UUID.randomUUID();
        AuraFairyEntity.SavedState expected = new AuraFairyEntity.SavedState(owner, 12, FairyRole.SCARER);

        AuraFairyEntity.SavedState restored = AuraFairyEntity.SavedState.read(expected.write());

        assertEquals(expected, restored);
    }

    @Test
    void malformedOrOwnerlessSavedStateCannotClaimAnOnlineOwner() {
        AuraFairyEntity.SavedState restored = AuraFairyEntity.SavedState.read(new CompoundTag());

        assertEquals(null, restored.ownerId());
        assertFalse(AuraFairyEntity.canRemainLoaded(restored.ownerId(), restored.slot(), UUID.randomUUID(), true, true, 0.0D));
    }

    @Test
    void orphanDeathDimensionAndDistanceEachInvalidateAFairy() {
        UUID owner = UUID.randomUUID();
        assertTrue(AuraFairyEntity.canRemainLoaded(owner, 0, owner, true, true, 256.0D));
        assertFalse(AuraFairyEntity.canRemainLoaded(owner, 0, null, false, false, 0.0D));
        assertFalse(AuraFairyEntity.canRemainLoaded(owner, 0, owner, false, true, 0.0D));
        assertFalse(AuraFairyEntity.canRemainLoaded(owner, 0, owner, true, false, 0.0D));
        assertFalse(AuraFairyEntity.canRemainLoaded(owner, 0, owner, true, true, 256.01D));
        assertFalse(AuraFairyEntity.canRemainLoaded(owner, -1, owner, true, true, 0.0D));
    }

    @Test
    void authorizationUsesCanonicalFairyUuidAndCurrentOwnerRole() {
        UUID owner = UUID.randomUUID();
        UUID fairy = UUID.randomUUID();
        List<FairyRole> roles = List.of(FairyRole.DIGGER);

        assertTrue(FairySystem.isAuthorizedForRole(owner, owner, fairy, fairy, true, true, true,
            roles, 0, FairyRole.DIGGER));
        assertFalse(FairySystem.isAuthorizedForRole(owner, owner, fairy, owner, true, true, true,
            roles, 0, FairyRole.DIGGER));
        assertFalse(FairySystem.isAuthorizedForRole(owner, owner, fairy, fairy, true, true, true,
            roles, 0, FairyRole.GLIDER));
    }

    @Test
    void reloadedFairyIsRejectedAfterItsRingSlotIsUnequippedOrChanged() {
        UUID owner = UUID.randomUUID();
        AuraFairyEntity.SavedState loaded = AuraFairyEntity.SavedState.read(
            new AuraFairyEntity.SavedState(owner, 0, FairyRole.SCARER).write()
        );

        assertTrue(AuraFairyEntity.canRemainLoaded(owner, loaded.slot(), owner, true, true, 0.0D));
        assertTrue(FairySystem.isRoleEquipped(List.of(FairyRole.SCARER), loaded.slot(), loaded.role()));
        assertFalse(FairySystem.isRoleEquipped(List.of(), loaded.slot(), loaded.role()));
        assertFalse(FairySystem.isRoleEquipped(List.of(FairyRole.DIGGER), loaded.slot(), loaded.role()));
    }

    @Test
    void legacyAllayNeedsBothTheMarkerAndAParseableOwnerTag() {
        UUID owner = UUID.randomUUID();

        assertEquals(Optional.of(owner), FairySystem.provenLegacyOwner(true, Set.of("aura_fairy", "aura_fairy_owner:" + owner)));
        assertEquals(Optional.empty(), FairySystem.provenLegacyOwner(false, Set.of("aura_fairy", "aura_fairy_owner:" + owner)));
        assertEquals(Optional.empty(), FairySystem.provenLegacyOwner(true, Set.of("aura_fairy")));
        assertEquals(Optional.empty(), FairySystem.provenLegacyOwner(true, Set.of("aura_fairy", "aura_fairy_owner:invalid")));
        assertEquals(Optional.empty(), FairySystem.provenLegacyOwner(true, Set.of("aura_fairy_owner:" + owner)));
    }
}
