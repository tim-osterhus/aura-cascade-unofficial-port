package pixlepix.auracascade.fairy;

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
        SCARE_HOSTILE,
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
            case SCARER -> observation.nearbyHostile() ? Action.SCARE_HOSTILE : Action.NONE;
            case EXTINGUISHER -> observation.ownerOnFire() ? Action.EXTINGUISH_FIRE : Action.NONE;
            case DIGGER -> Action.DIG_SOFT_BLOCK;
            case LIGHTER -> observation.lowLight() ? Action.CREATE_LIGHT : Action.NONE;
            case GLIDER -> observation.ownerFalling() ? Action.SLOW_FALL : Action.NONE;
            case TRAINER -> Action.GRANT_EXPERIENCE;
        };
    }
}
