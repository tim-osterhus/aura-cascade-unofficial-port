package pixlepix.auracascade.parity;

import java.util.Locale;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraTickContext;

public enum AuraColor {
    WHITE("white", "White"),
    BLACK("black", "Black") {
        @Override
        public double relativeMass(AuraEnvironment environment) {
            return 0.0D;
        }

        @Override
        public boolean canNaturallyFlowHorizontally() {
            return false;
        }

        @Override
        public boolean supportsControlledUpwardFlow() {
            return false;
        }

        @Override
        public boolean generatesFallingPower() {
            return false;
        }
    },
    ORANGE("orange", "Orange") {
        @Override
        public double relativeMass(AuraEnvironment environment) {
            return 0.0D;
        }

        @Override
        public boolean canNaturallyFlowVertically() {
            return false;
        }

        @Override
        public boolean supportsControlledUpwardFlow() {
            return false;
        }

        @Override
        public boolean inducesCurrentOnTransfer() {
            return true;
        }
    },
    RED("red", "Red"),
    YELLOW("yellow", "Yellow") {
        @Override
        public double ascentBoost(AuraEnvironment environment) {
            return 2.0D;
        }

        @Override
        public int applyPassiveTick(int storedAmount, AuraTickContext context) {
            if (context.matchesPhase(1200L, 5L)) {
                return (int) Math.floor(storedAmount * 0.8D);
            }
            return storedAmount;
        }
    },
    GREEN("green", "Green") {
        @Override
        public double relativeMass(AuraEnvironment environment) {
            return environment.daytime() ? 2.0D : 0.5D;
        }
    },
    BLUE("blue", "Blue") {
        @Override
        public double ascentBoost(AuraEnvironment environment) {
            return environment.raining() ? 4.0D : 0.5D;
        }
    },
    VIOLET("violet", "Violet") {
        @Override
        public int applyPassiveTick(int storedAmount, AuraTickContext context) {
            if (storedAmount > 2600) {
                return 0;
            }
            if (!context.matchesPhase(300L, 5L)) {
                return storedAmount;
            }
            if (storedAmount <= 25) {
                return 0;
            }

            int growth = 5 * Math.min(100, (int) Math.floor(2500.0D / storedAmount));
            return storedAmount + growth;
        }
    };

    private final String id;
    private final String displayName;

    AuraColor(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public double relativeMass(AuraEnvironment environment) {
        return 1.0D;
    }

    public double ascentBoost(AuraEnvironment environment) {
        return 1.0D;
    }

    public boolean canNaturallyFlowHorizontally() {
        return true;
    }

    public boolean canNaturallyFlowVertically() {
        return true;
    }

    public boolean supportsControlledUpwardFlow() {
        return true;
    }

    public boolean generatesFallingPower() {
        return true;
    }

    public boolean inducesCurrentOnTransfer() {
        return false;
    }

    public int applyPassiveTick(int storedAmount, AuraTickContext context) {
        return storedAmount;
    }

    public static AuraColor byId(String id) {
        String normalized = id.toLowerCase(Locale.ROOT);
        for (AuraColor color : values()) {
            if (color.id.equals(normalized)) {
                return color;
            }
        }
        throw new IllegalArgumentException("Unknown aura color id: " + id);
    }
}
