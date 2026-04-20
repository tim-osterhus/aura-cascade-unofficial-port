package pixlepix.auracascade.aura;

import java.util.Objects;

public record AuraTickContext(long gameTime, AuraEnvironment environment) {
    public AuraTickContext {
        Objects.requireNonNull(environment, "environment");
    }

    public boolean matchesPhase(long interval, long offset) {
        return Math.floorMod(gameTime, interval) == offset;
    }
}
