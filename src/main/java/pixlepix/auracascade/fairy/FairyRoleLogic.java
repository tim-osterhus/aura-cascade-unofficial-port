package pixlepix.auracascade.fairy;

import java.util.List;

public final class FairyRoleLogic {
    private FairyRoleLogic() {
    }

    public enum Action {
        NONE,
        ATTACK_HOSTILE,
        DEBUFF_HOSTILE,
        BUFF_OWNER,
        STEAL_FROM_NEARBY_PLAYER,
        PUSH_HOSTILE,
        EMPOWER_PROJECTILE,
        CLUTCH_ATTACK,
        FETCH_ITEMS,
        SUMMON_PASSIVE,
        BREED_ANIMALS,
        EXTINGUISH_FIRE,
        DIG_SOFT_BLOCK,
        CREATE_LIGHT,
        SLOW_FALL,
        GRANT_EXPERIENCE
    }

    public record Observation(
        boolean ownerLowHealth,
        boolean ownerOnFire,
        boolean ownerFalling,
        boolean nearbyHostile,
        boolean nearbyItems,
        boolean nearbyAnimals,
        boolean nearbyPlayerHoldingItem,
        boolean lowLight,
        boolean nearbyProjectile
    ) {
    }

    public static Action primaryAction(FairyRole role, Observation observation) {
        return switch (role) {
            case BASIC -> Action.NONE;
            case FIGHTER -> observation.nearbyHostile() ? Action.ATTACK_HOSTILE : Action.NONE;
            case DEBUFFER -> observation.nearbyHostile() ? Action.DEBUFF_HOSTILE : Action.NONE;
            case BUFFER -> Action.BUFF_OWNER;
            case STEALER -> observation.nearbyPlayerHoldingItem() ? Action.STEAL_FROM_NEARBY_PLAYER : Action.NONE;
            case PUSHER -> observation.nearbyHostile() ? Action.PUSH_HOSTILE : Action.NONE;
            case SHOOTER -> observation.nearbyProjectile() ? Action.EMPOWER_PROJECTILE : Action.NONE;
            case SAVIOR -> observation.ownerLowHealth() && observation.nearbyHostile() ? Action.CLUTCH_ATTACK : Action.NONE;
            case FETCHER -> observation.nearbyItems() ? Action.FETCH_ITEMS : Action.NONE;
            case BAITER -> Action.SUMMON_PASSIVE;
            case BREEDER -> observation.nearbyAnimals() ? Action.BREED_ANIMALS : Action.NONE;
            case SCARER -> Action.NONE;
            case EXTINGUISHER -> observation.ownerOnFire() ? Action.EXTINGUISH_FIRE : Action.NONE;
            case DIGGER -> Action.NONE;
            case LIGHTER -> observation.lowLight() ? Action.CREATE_LIGHT : Action.NONE;
            case GLIDER -> Action.NONE;
            case TRAINER -> Action.GRANT_EXPERIENCE;
        };
    }

    static boolean retainTrackedArrow(boolean alive, double motionX, double motionY) {
        return alive && (motionX != 0.0D || motionY != 0.0D);
    }

    static boolean isAuthorized(boolean canonicalEntity, List<FairyRole> equippedRoles, int slot, FairyRole entityRole) {
        return canonicalEntity
            && slot >= 0
            && slot < equippedRoles.size()
            && equippedRoles.get(slot) == entityRole;
    }

    static double diggerSpeedMultiplier(int ownedDiggerFairies) {
        return Math.pow(1.08D, Math.min(ownedDiggerFairies - 1, 15));
    }

    static double gliderFallMultiplier(int ownedGliderFairies) {
        return Math.pow(0.5D, Math.max(ownedGliderFairies, 0));
    }

    static boolean deniesNaturalSpawn(int nearbyScarers, int roll) {
        return nearbyScarers > 0 && roll >= 0 && roll < 25 && roll <= nearbyScarers;
    }
}
