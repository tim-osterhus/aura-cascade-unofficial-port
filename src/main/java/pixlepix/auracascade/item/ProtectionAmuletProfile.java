package pixlepix.auracascade.item;

public enum ProtectionAmuletProfile {
    RED(DamageFamily.FIRE, 0.25F, true),
    ORANGE(DamageFamily.EXPLOSION, 0.25F, true),
    YELLOW(DamageFamily.PROJECTILE, 0.5F, false),
    GREEN(DamageFamily.FALL, 0.25F, true),
    BLUE(DamageFamily.DROWN, 0.25F, true),
    VIOLET(DamageFamily.WITHER, 0.0F, true);

    private final DamageFamily family;
    private final float healFraction;
    private final boolean blocksDamage;

    ProtectionAmuletProfile(DamageFamily family, float healFraction, boolean blocksDamage) {
        this.family = family;
        this.healFraction = healFraction;
        this.blocksDamage = blocksDamage;
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
