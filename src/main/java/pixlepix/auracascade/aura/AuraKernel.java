package pixlepix.auracascade.aura;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraKernel {
    public static final int DEFAULT_LINK_RANGE = 15;
    public static final int DEFAULT_EQUILIBRIUM_THRESHOLD = 25;
    public static final int DEFAULT_RETENTION_WEIGHT = 400;

    private AuraKernel() {
    }

    public static LinkedHashSet<BlockPos> scanStraightLineLinks(
        BlockPos origin,
        Set<BlockPos> nodePositions,
        Set<BlockPos> opaqueBlocks,
        int maxRange
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(nodePositions, "nodePositions");
        Objects.requireNonNull(opaqueBlocks, "opaqueBlocks");

        LinkedHashSet<BlockPos> links = new LinkedHashSet<>();
        if (maxRange <= 0) {
            return links;
        }

        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= maxRange; distance++) {
                BlockPos candidate = origin.relative(direction, distance);
                if (opaqueBlocks.contains(candidate)) {
                    break;
                }
                if (nodePositions.contains(candidate)) {
                    links.add(candidate);
                    break;
                }
            }
        }
        return links;
    }

    public static Map<BlockPos, AuraStorage> planNaturalTransfers(
        BlockPos origin,
        AuraNodeState source,
        Map<BlockPos, AuraNodeState> connectedNodes,
        AuraTransferContext context
    ) {
        return planNaturalTransfers(origin, source, connectedNodes, context, targetPos -> true);
    }

    public static Map<BlockPos, AuraStorage> planNaturalTransfers(
        BlockPos origin,
        AuraNodeState source,
        Map<BlockPos, AuraNodeState> connectedNodes,
        AuraTransferContext context,
        Predicate<BlockPos> canTransferPosition
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(connectedNodes, "connectedNodes");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(canTransferPosition, "canTransferPosition");

        if (!context.naturalFlowEnabled() || connectedNodes.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<BlockPos, Double> weights = new LinkedHashMap<>();
        double totalWeight = context.passiveRetentionWeight();
        for (BlockPos targetPos : connectedNodes.keySet()) {
            if (targetPos.getY() > origin.getY() || !canTransferPosition.test(targetPos)) {
                continue;
            }
            double weight = connectionWeight(origin, targetPos);
            weights.put(targetPos, weight);
            totalWeight += weight;
        }

        LinkedHashMap<BlockPos, AuraStorage> plans = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, Double> entry : weights.entrySet()) {
            BlockPos targetPos = entry.getKey();
            AuraNodeState target = connectedNodes.get(targetPos);
            double share = entry.getValue() / totalWeight;

            AuraStorage plan = new AuraStorage();
            for (AuraColor color : AuraColor.values()) {
                if (!canNaturallyTransfer(origin, targetPos, color)) {
                    continue;
                }
                int sourceAmount = source.storage().get(color);
                int targetAmount = target.storage().get(color);
                if (Math.abs(sourceAmount - targetAmount) <= context.equilibriumThreshold()) {
                    continue;
                }
                int plannedAmount = (int) Math.floor(sourceAmount * share);
                if (plannedAmount > 0) {
                    plan.set(color, plannedAmount);
                }
            }

            if (!plan.isEmpty()) {
                plans.put(targetPos, plan);
            }
        }

        return plans;
    }

    public static AuraStorage planControlledUpwardTransfer(
        BlockPos origin,
        AuraNodeState source,
        BlockPos target,
        AuraNodeState targetState,
        int liftBudget,
        AuraEnvironment environment,
        int equilibriumThreshold
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(targetState, "targetState");
        Objects.requireNonNull(environment, "environment");

        AuraStorage planned = new AuraStorage();
        if (liftBudget <= 0 || target.getY() <= origin.getY()) {
            return planned;
        }

        int rise = target.getY() - origin.getY();
        int remainingLift = liftBudget;
        for (AuraColor color : AuraColor.values()) {
            if (!color.supportsControlledUpwardFlow()) {
                continue;
            }

            int sourceAmount = source.storage().get(color);
            int targetAmount = targetState.storage().get(color);
            int diff = sourceAmount - targetAmount;
            if (diff <= equilibriumThreshold) {
                continue;
            }

            int desiredAmount = Math.max(1, (diff - equilibriumThreshold) / 2);
            int maxByLift = maxControlledUpwardAmount(color, remainingLift, rise, environment);
            int movedAmount = Math.min(sourceAmount, Math.min(desiredAmount, maxByLift));
            if (movedAmount <= 0) {
                continue;
            }

            planned.set(color, movedAmount);
            remainingLift -= requiredLift(color, movedAmount, rise, environment);
            if (remainingLift <= 0) {
                break;
            }
        }

        return planned;
    }

    public static int maxControlledUpwardAmount(
        AuraColor color,
        int liftBudget,
        int rise,
        AuraEnvironment environment
    ) {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(environment, "environment");

        if (liftBudget <= 0 || rise <= 0 || !color.supportsControlledUpwardFlow()) {
            return 0;
        }
        return (int) Math.floor((liftBudget * color.ascentBoost(environment)) / rise);
    }

    public static int requiredLift(
        AuraColor color,
        int amount,
        int rise,
        AuraEnvironment environment
    ) {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(environment, "environment");

        if (amount <= 0 || rise <= 0) {
            return 0;
        }
        return (int) Math.ceil((amount * rise) / color.ascentBoost(environment));
    }

    public static AuraStorage planRedExplosionPushUp(
        BlockPos origin,
        AuraNodeState source,
        BlockPos target,
        int explosionStrength
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");

        AuraStorage planned = new AuraStorage();
        if (explosionStrength <= 0 || target.getY() <= origin.getY()) {
            return planned;
        }

        int rise = target.getY() - origin.getY();
        int movedAmount = Math.min(source.storage().get(AuraColor.RED), explosionStrength / rise);
        if (movedAmount > 0) {
            planned.set(AuraColor.RED, movedAmount);
        }
        return planned;
    }

    public static AuraTransferResult applyTransfer(
        BlockPos origin,
        AuraNodeState source,
        BlockPos targetPos,
        AuraNodeState target,
        AuraStorage requested,
        AuraEnvironment environment
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(targetPos, "targetPos");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(environment, "environment");

        AuraStorage moved = requested.min(source.storage());
        if (moved.isEmpty()) {
            return new AuraTransferResult(moved, 0);
        }

        source.storage().subtractAll(moved);
        target.storage().addAll(moved);

        int fallDistance = origin.getY() - targetPos.getY();
        int generatedPower = 0;
        if (fallDistance > 0) {
            for (AuraColor color : AuraColor.values()) {
                int amount = moved.get(color);
                if (amount <= 0 || !color.generatesFallingPower()) {
                    continue;
                }
                generatedPower += (int) Math.floor(fallDistance * amount * color.relativeMass(environment));
            }
            target.receivePower(generatedPower);
        }

        return new AuraTransferResult(moved, generatedPower);
    }

    public static List<AuraInducedCurrent> planOrangeInducedCurrents(
        BlockPos sourcePos,
        Direction transferDirection,
        int orangeAmount,
        Map<BlockPos, ? extends Collection<BlockPos>> networkLinks
    ) {
        Objects.requireNonNull(sourcePos, "sourcePos");
        Objects.requireNonNull(transferDirection, "transferDirection");
        Objects.requireNonNull(networkLinks, "networkLinks");

        if (orangeAmount <= 0 || transferDirection == Direction.UP || transferDirection == Direction.DOWN) {
            return List.of();
        }

        List<AuraInducedCurrent> currents = new ArrayList<>();
        for (Map.Entry<BlockPos, ? extends Collection<BlockPos>> entry : networkLinks.entrySet()) {
            BlockPos nodePos = entry.getKey();
            if (nodePos.equals(sourcePos) || !isWithinCube(sourcePos, nodePos, 2)) {
                continue;
            }

            Optional<Direction> relativeDirection = cardinalDirection(sourcePos, nodePos);
            if (relativeDirection.isPresent()
                && (relativeDirection.get() == transferDirection
                || relativeDirection.get() == transferDirection.getOpposite())) {
                continue;
            }

            for (BlockPos downstreamTarget : entry.getValue()) {
                if (cardinalDirection(nodePos, downstreamTarget).orElse(null) == transferDirection) {
                    currents.add(new AuraInducedCurrent(nodePos, downstreamTarget, transferDirection, orangeAmount));
                }
            }
        }

        return currents;
    }

    public static OrangeBurstPlan planOrangeBurst(AuraStorage source, int requestedAmount, int opposingAmount) {
        Objects.requireNonNull(source, "source");

        int netAmount = requestedAmount - opposingAmount;
        if (netAmount <= 0) {
            return new OrangeBurstPlan(new AuraStorage(), opposingAmount - requestedAmount);
        }

        AuraStorage nonOrange = source.copy();
        int storedOrange = nonOrange.get(AuraColor.ORANGE);
        nonOrange.set(AuraColor.ORANGE, 0);
        int totalAura = source.total();
        if (totalAura <= 0) {
            return new OrangeBurstPlan(new AuraStorage(), 0);
        }

        double legacyFactor = Math.min(1.0D, netAmount / (double) totalAura) - storedOrange;
        return new OrangeBurstPlan(nonOrange.scaled(Math.max(0.0D, legacyFactor)), 0);
    }

    public static void applyPassiveTick(AuraNodeState state, AuraTickContext context) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(context, "context");

        for (AuraColor color : AuraColor.values()) {
            int storedAmount = state.storage().get(color);
            if (storedAmount <= 0) {
                continue;
            }
            state.storage().set(color, color.applyPassiveTick(storedAmount, context));
        }
    }

    public static Optional<Direction> cardinalDirection(BlockPos origin, BlockPos target) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(target, "target");

        int dx = target.getX() - origin.getX();
        int dy = target.getY() - origin.getY();
        int dz = target.getZ() - origin.getZ();
        int nonZeroAxes = countNonZero(dx, dy, dz);
        if (nonZeroAxes != 1) {
            return Optional.empty();
        }

        if (dx > 0) {
            return Optional.of(Direction.EAST);
        }
        if (dx < 0) {
            return Optional.of(Direction.WEST);
        }
        if (dy > 0) {
            return Optional.of(Direction.UP);
        }
        if (dy < 0) {
            return Optional.of(Direction.DOWN);
        }
        if (dz > 0) {
            return Optional.of(Direction.SOUTH);
        }
        if (dz < 0) {
            return Optional.of(Direction.NORTH);
        }
        return Optional.empty();
    }

    public static double connectionWeight(BlockPos origin, BlockPos target) {
        double distance = Math.sqrt(distanceSquared(origin, target));
        return Math.pow(20.0D - distance, 2.0D);
    }

    private static boolean canNaturallyTransfer(BlockPos origin, BlockPos target, AuraColor color) {
        int deltaY = target.getY() - origin.getY();
        if (deltaY > 0) {
            return false;
        }
        if (deltaY == 0) {
            return color.canNaturallyFlowHorizontally();
        }
        return color.canNaturallyFlowVertically();
    }

    private static boolean isWithinCube(BlockPos a, BlockPos b, int range) {
        return Math.abs(a.getX() - b.getX()) <= range
            && Math.abs(a.getY() - b.getY()) <= range
            && Math.abs(a.getZ() - b.getZ()) <= range;
    }

    private static double distanceSquared(BlockPos origin, BlockPos target) {
        long dx = target.getX() - origin.getX();
        long dy = target.getY() - origin.getY();
        long dz = target.getZ() - origin.getZ();
        return (dx * dx) + (dy * dy) + (dz * dz);
    }

    private static int countNonZero(int... values) {
        int count = 0;
        for (int value : values) {
            if (value != 0) {
                count++;
            }
        }
        return count;
    }

    public record OrangeBurstPlan(AuraStorage transfer, int opposingRemainder) {
        public OrangeBurstPlan {
            Objects.requireNonNull(transfer, "transfer");
        }
    }
}
