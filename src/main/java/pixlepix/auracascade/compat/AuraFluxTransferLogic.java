package pixlepix.auracascade.compat;

public final class AuraFluxTransferLogic {
    public static final int MAX_CONNECTED_RECEIVERS = 4;

    private static final float POWER_TO_ENERGY_PER_TICK = 0.75F;
    private static final int TICKS_PER_EXPORT = 20;
    private static final int ENERGY_PER_POWER_UNIT_PER_EXPORT = 15;

    private AuraFluxTransferLogic() {
    }

    public static boolean canExportToReceiverCount(int receiverCount) {
        return receiverCount > 0 && receiverCount <= MAX_CONNECTED_RECEIVERS;
    }

    public static long maximumEnergyPerReceiver(int availablePower, int acceptingReceiverCount) {
        if (availablePower <= 0 || !canExportToReceiverCount(acceptingReceiverCount)) {
            return 0L;
        }

        int energyPerTick = (int) (availablePower * POWER_TO_ENERGY_PER_TICK / acceptingReceiverCount);
        return energyPerTick * (long) TICKS_PER_EXPORT;
    }

    public static int auraConsumed(long insertedEnergy, int availablePower) {
        if (insertedEnergy <= 0L || availablePower <= 0) {
            return 0;
        }

        long consumed = insertedEnergy / ENERGY_PER_POWER_UNIT_PER_EXPORT;
        if (insertedEnergy % ENERGY_PER_POWER_UNIT_PER_EXPORT != 0L) {
            consumed++;
        }
        return (int) Math.min(consumed, availablePower);
    }
}
