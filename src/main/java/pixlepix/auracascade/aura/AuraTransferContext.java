package pixlepix.auracascade.aura;

import java.util.Objects;

public record AuraTransferContext(
    AuraEnvironment environment,
    int equilibriumThreshold,
    int passiveRetentionWeight,
    boolean naturalFlowEnabled
) {
    public AuraTransferContext {
        Objects.requireNonNull(environment, "environment");
        if (equilibriumThreshold < 0) {
            throw new IllegalArgumentException("equilibriumThreshold must be non-negative");
        }
        if (passiveRetentionWeight < 0) {
            throw new IllegalArgumentException("passiveRetentionWeight must be non-negative");
        }
    }

    public static AuraTransferContext natural(AuraEnvironment environment) {
        return new AuraTransferContext(
            environment,
            AuraKernel.DEFAULT_EQUILIBRIUM_THRESHOLD,
            AuraKernel.DEFAULT_RETENTION_WEIGHT,
            true
        );
    }
}
