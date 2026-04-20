package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraPumpLogic {
    private static final int GLOWSTONE_POWER = 240;
    private static final int GLOWSTONE_SPEED = 18;
    private static final int TORCH_POWER = 80;
    private static final int TORCH_SPEED = 6;
    private static final int FALL_SPEED = 10;
    private static final int ARROW_POWER = 200;
    private static final int ARROW_SPEED = 16;
    private static final int EGG_POWER = 60;
    private static final int EGG_SPEED = 6;
    private static final int SNOWBALL_POWER = 40;
    private static final int SNOWBALL_SPEED = 4;
    private static final int REDSTONE_BASE_POWER = 80;
    private static final int REDSTONE_SPEED = 12;
    private static final int CREATIVE_POWER = 2;
    private static final int CREATIVE_SPEED = 10_000_000;

    private AuraPumpLogic() {
    }

    public static FuelOffer burningFuel(int furnaceBurnTime) {
        return new FuelOffer(Math.max(20, (furnaceBurnTime * 3) / 5), 18);
    }

    public static FuelOffer glowstoneFuel() {
        return new FuelOffer(GLOWSTONE_POWER, GLOWSTONE_SPEED);
    }

    public static FuelOffer torchFuel() {
        return new FuelOffer(TORCH_POWER, TORCH_SPEED);
    }

    public static FuelOffer fallFuel(float fallDistance) {
        return new FuelOffer(Math.max(0, (int) Math.floor(fallDistance * 20.0F)), FALL_SPEED);
    }

    public static FuelOffer arrowFuel() {
        return new FuelOffer(ARROW_POWER, ARROW_SPEED);
    }

    public static FuelOffer eggFuel() {
        return new FuelOffer(EGG_POWER, EGG_SPEED);
    }

    public static FuelOffer snowballFuel() {
        return new FuelOffer(SNOWBALL_POWER, SNOWBALL_SPEED);
    }

    public static FuelOffer redstoneFuel(int distance) {
        int scaledPower = (int) Math.ceil(REDSTONE_BASE_POWER * Math.pow(1.4D, Math.max(1, distance)));
        return new FuelOffer(scaledPower, REDSTONE_SPEED);
    }

    public static FuelOffer creativeFuel() {
        return new FuelOffer(CREATIVE_POWER, CREATIVE_SPEED);
    }

    public static PumpState addFuel(AuraPumpVariant variant, PumpState current, FuelOffer offer) {
        int offerPower = offer.power();
        int offerSpeed = offer.speed();
        if (variant.isAlternating()) {
            offerSpeed *= 3;
        }

        if ((offerPower * offerSpeed) > (current.power() * current.speed())) {
            return new PumpState(offerPower, offerSpeed);
        }
        return current;
    }

    public static float alternatingFactor(long gameTime) {
        return (float) ((1.0D + Math.sin(Math.PI * gameTime / 10_000.0D)) / 2.0D);
    }

    public static AuraStorage planTransfer(
        AuraNodeState source,
        BlockPos origin,
        BlockPos target,
        AuraEnvironment environment,
        PumpState pumpState,
        AuraPumpVariant variant,
        long gameTime
    ) {
        AuraStorage requested = new AuraStorage();
        int rise = target.getY() - origin.getY();
        if (rise <= 0 || pumpState.power() <= 0 || pumpState.speed() <= 0) {
            return requested;
        }

        double baseAmount = (double) pumpState.speed() / rise;
        if (variant.isAlternating()) {
            baseAmount *= alternatingFactor(gameTime);
        }

        for (AuraColor color : AuraColor.values()) {
            double relativeMass = color.relativeMass(environment);
            if (relativeMass <= 0.0D) {
                continue;
            }

            int sourceAmount = source.storage().get(color);
            if (sourceAmount <= 0) {
                continue;
            }

            double requestedAmount = baseAmount * source.storage().composition(color);
            requestedAmount /= relativeMass;
            requestedAmount *= color.ascentBoost(environment);

            int movedAmount = Math.min(sourceAmount, (int) Math.floor(requestedAmount));
            if (movedAmount > 0) {
                requested.set(color, movedAmount);
            }
        }

        return requested;
    }

    public record FuelOffer(int power, int speed) {
    }

    public record PumpState(int power, int speed) {
        public PumpState {
            power = Math.max(0, power);
            speed = Math.max(0, speed);
        }

        public boolean active() {
            return power > 0 && speed > 0;
        }
    }
}
