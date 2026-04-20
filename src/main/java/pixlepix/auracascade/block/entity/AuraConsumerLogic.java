package pixlepix.auracascade.block.entity;

public final class AuraConsumerLogic {
    private AuraConsumerLogic() {
    }

    public static int bleedStoredPower(int storedPower) {
        return Math.max(0, (int) Math.floor(storedPower * 0.75D));
    }

    public static int powerCostForStep(int basePower, int stepIndex) {
        if (basePower <= 0 || stepIndex < 0) {
            return 0;
        }

        long cost = (long) basePower << Math.min(stepIndex, 30);
        return cost >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cost;
    }

    public static int progressStepsForTick(int storedPower, int basePower) {
        if (storedPower <= 0 || basePower <= 0) {
            return 0;
        }

        int remaining = storedPower;
        int steps = 0;
        while (remaining >= powerCostForStep(basePower, steps)) {
            remaining -= powerCostForStep(basePower, steps);
            steps++;
        }
        return steps;
    }

    public static int minimumCyclePower(AuraConsumerVariant variant) {
        return variant.powerPerProgress() * (variant.maxProgress() + 1);
    }
}
