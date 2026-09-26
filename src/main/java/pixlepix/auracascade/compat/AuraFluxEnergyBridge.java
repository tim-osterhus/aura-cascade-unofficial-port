package pixlepix.auracascade.compat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import team.reborn.energy.api.EnergyStorage;

public final class AuraFluxEnergyBridge implements AuraFluxBridge {
    @Override
    public int export(Level level, BlockPos sourcePos, int availablePower) {
        if (level == null || sourcePos == null || level.isClientSide() || availablePower <= 0) {
            return 0;
        }

        ReceiverScan scan = findConnectedReceivers(
            sourcePos,
            level::hasChunkAt,
            (pos, side) -> EnergyStorage.SIDED.find(level, pos, side)
        );
        if (scan.machineCount() > AuraFluxTransferLogic.MAX_CONNECTED_RECEIVERS) {
            return 0;
        }
        return exportToReceivers(scan.receivers(), availablePower);
    }

    static int exportToReceivers(List<EnergyStorage> candidates, int availablePower) {
        if (availablePower <= 0) {
            return 0;
        }

        List<EnergyStorage> receivers = distinctReceivers(candidates);
        if (!AuraFluxTransferLogic.canExportToReceiverCount(receivers.size())) {
            return 0;
        }

        int acceptingReceivers = countAcceptingReceivers(receivers);
        long maximumPerReceiver = AuraFluxTransferLogic.maximumEnergyPerReceiver(
            availablePower,
            acceptingReceivers
        );
        if (maximumPerReceiver <= 0) {
            return 0;
        }

        long insertedEnergy = insertIntoReceivers(receivers, maximumPerReceiver);
        return AuraFluxTransferLogic.auraConsumed(insertedEnergy, availablePower);
    }

    static ReceiverScan findConnectedReceivers(
        BlockPos sourcePos,
        Predicate<BlockPos> hasChunkAt,
        ReceiverLookup lookup
    ) {
        List<EnergyStorage> receivers = new ArrayList<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> receiverMachines = new HashSet<>();
        Set<ReceiverFace> queriedFaces = new HashSet<>();
        Set<EnergyStorage> seenStorages = Collections.newSetFromMap(new IdentityHashMap<>());
        BlockPos origin = sourcePos.immutable();
        frontier.add(origin);

        // 592 floods face-adjacent receivers and disables the whole network above four.
        int machineCount = 0;
        while (!frontier.isEmpty()) {
            BlockPos current = frontier.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction).immutable();
                Direction face = direction.getOpposite();
                if (adjacent.equals(origin)
                    || !queriedFaces.add(new ReceiverFace(adjacent, face))
                    || !hasChunkAt.test(adjacent)) {
                    continue;
                }

                EnergyStorage storage = lookup.find(adjacent, face);
                if (storage == null || !storage.supportsInsertion()) {
                    continue;
                }

                if (receiverMachines.add(adjacent)) {
                    machineCount++;
                    if (seenStorages.add(storage)) {
                        receivers.add(storage);
                    }
                    if (machineCount > AuraFluxTransferLogic.MAX_CONNECTED_RECEIVERS) {
                        return new ReceiverScan(receivers, machineCount);
                    }
                    frontier.addLast(adjacent);
                }
            }
        }
        return new ReceiverScan(receivers, machineCount);
    }

    private static List<EnergyStorage> distinctReceivers(List<EnergyStorage> candidates) {
        ArrayList<EnergyStorage> receivers = new ArrayList<>();
        Set<EnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (EnergyStorage candidate : candidates) {
            if (candidate != null && candidate.supportsInsertion() && seen.add(candidate)) {
                receivers.add(candidate);
            }
        }
        return receivers;
    }

    private static int countAcceptingReceivers(List<EnergyStorage> receivers) {
        int accepting = 0;
        for (EnergyStorage receiver : receivers) {
            try (Transaction simulation = Transaction.openOuter()) {
                if (receiver.insert(1L, simulation) > 0L) {
                    accepting++;
                }
            }
        }
        return accepting;
    }

    private static long insertIntoReceivers(List<EnergyStorage> receivers, long maximumPerReceiver) {
        long inserted = 0L;
        try (Transaction transaction = Transaction.openOuter()) {
            for (EnergyStorage receiver : receivers) {
                long accepted = receiver.insert(maximumPerReceiver, transaction);
                inserted += Math.max(0L, Math.min(maximumPerReceiver, accepted));
            }
            if (inserted > 0L) {
                transaction.commit();
            }
        }
        return inserted;
    }

    @FunctionalInterface
    interface ReceiverLookup {
        EnergyStorage find(BlockPos pos, Direction side);
    }

    record ReceiverScan(List<EnergyStorage> receivers, int machineCount) {
    }

    private record ReceiverFace(BlockPos pos, Direction side) {
    }
}
