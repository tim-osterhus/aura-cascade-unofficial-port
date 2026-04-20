package pixlepix.auracascade.aura;

public record AuraEnvironment(boolean daytime, boolean raining) {
    public static final AuraEnvironment CLEAR_DAY = new AuraEnvironment(true, false);
    public static final AuraEnvironment CLEAR_NIGHT = new AuraEnvironment(false, false);
    public static final AuraEnvironment RAINING_DAY = new AuraEnvironment(true, true);
}
