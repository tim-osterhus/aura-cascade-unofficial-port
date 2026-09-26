package pixlepix.auracascade.item;

public enum ProtectionAmuletProfile {
    RED(DamageFamily.FIRE, 1.0F, true),
    ORANGE(DamageFamily.EXPLOSION, 1.0F, true),
    YELLOW(DamageFamily.PROJECTILE, 0.0F, false, 0.5F),
    GREEN(DamageFamily.FALL, 1.0F, true),
    BLUE(DamageFamily.DROWN, 1.0F, true),
    VIOLET(DamageFamily.WITHER, 0.0F, true);

    private final DamageFamily family;
    private final float healFraction;
    private final boolean blocksDamage;
    private final float incomingDamageMultiplier;

    ProtectionAmuletProfile(DamageFamily family, float healFraction, boolean blocksDamage) {
        this(family, healFraction, blocksDamage, 1.0F);
    }

    ProtectionAmuletProfile(
        DamageFamily family,
        float healFraction,
        boolean blocksDamage,
        float incomingDamageMultiplier
    ) {
        this.family = family;
        this.healFraction = healFraction;
        this.blocksDamage = blocksDamage;
        this.incomingDamageMultiplier = incomingDamageMultiplier;
    }

    public DamageFamily family() {
        return family;
    }

    public float healFraction() {
        return healFraction;
    }

    public boolean blocksDamage() {
        return blocksDamage;
    }

    public float incomingDamageMultiplier() {
        return incomingDamageMultiplier;
    }

    public boolean appliesTo(DamageFamily family) {
        return this.family == family;
    }

    public enum DamageFamily {
        FIRE,
        EXPLOSION,
        PROJECTILE,
        FALL,
        DROWN,
        WITHER,
        OTHER
    }
}
