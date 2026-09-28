package pixlepix.auracascade.compat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class AuraFluxEnergyBridge implements AuraFluxBridge {
    @Override
    public int export(Level level, BlockPos sourcePos, int availablePower) {
        if (level == null || sourcePos == null || level.isClientSide() || availablePower <= 0) {
            return 0;
        }

        ReceiverScan scan = findConnectedReceivers(
            sourcePos,
            level::hasChunkAt,
            (pos, side) -> level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side)
        );
        if (scan.machineCount() > AuraFluxTransferLogic.MAX_CONNECTED_RECEIVERS) {
            return 0;
        }
        return exportToReceivers(scan.receivers(), availablePower);
    }

    static int exportToReceivers(List<? extends IEnergyStorage> candidates, int availablePower) {
        if (availablePower <= 0) {
            return 0;
        }

        List<IEnergyStorage> receivers = distinctReceivers(candidates);
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
        List<IEnergyStorage> receivers = new ArrayList<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> receiverMachines = new HashSet<>();
        Set<ReceiverFace> queriedFaces = new HashSet<>();
        Set<IEnergyStorage> seenStorages = Collections.newSetFromMap(new IdentityHashMap<>());
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

                IEnergyStorage storage = lookup.find(adjacent, face);
                if (storage == null || !storage.canReceive()) {
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

    private static List<IEnergyStorage> distinctReceivers(List<? extends IEnergyStorage> candidates) {
        ArrayList<IEnergyStorage> receivers = new ArrayList<>();
        Set<IEnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (IEnergyStorage candidate : candidates) {
            if (candidate != null && candidate.canReceive() && seen.add(candidate)) {
                receivers.add(candidate);
            }
        }
        return receivers;
    }

    private static int countAcceptingReceivers(List<IEnergyStorage> receivers) {
        int accepting = 0;
        for (IEnergyStorage receiver : receivers) {
            if (receiver.canReceive() && receiver.receiveEnergy(1, true) > 0) {
                accepting++;
            }
        }
        return accepting;
    }

    private static long insertIntoReceivers(List<IEnergyStorage> receivers, long maximumPerReceiver) {
        int offered = (int) Math.min(maximumPerReceiver, Integer.MAX_VALUE);
        if (offered <= 0) {
            return 0L;
        }

        long inserted = 0L;
        for (IEnergyStorage receiver : receivers) {
            if (receiver.canReceive()) {
                int accepted = receiver.receiveEnergy(offered, false);
                inserted += Math.max(0, Math.min(offered, accepted));
            }
        }
        return inserted;
    }

    @FunctionalInterface
    interface ReceiverLookup {
        IEnergyStorage find(BlockPos pos, Direction side);
    }

    record ReceiverScan(List<IEnergyStorage> receivers, int machineCount) {
    }

    private record ReceiverFace(BlockPos pos, Direction side) {
    }
}
