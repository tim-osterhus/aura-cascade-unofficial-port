package pixlepix.auracascade.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import team.reborn.energy.api.EnergyStorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraFluxTransferLogicTest {
    @Test
    void legacyReceiverBubbleAllowsAtMostFourConnectedReceivers() {
        assertFalse(AuraFluxTransferLogic.canExportToReceiverCount(0));
        assertTrue(AuraFluxTransferLogic.canExportToReceiverCount(1));
        assertTrue(AuraFluxTransferLogic.canExportToReceiverCount(4));
        assertFalse(AuraFluxTransferLogic.canExportToReceiverCount(5));
    }

    @Test
    void receiverScanUsesLoadedChunksAndTheTargetFacingSide() {
        BlockPos origin = pos(0);
        BlockPos adjacent = pos(1);
        MockEnergyStorage receiver = new MockEnergyStorage(1_000L, 1_000L);
        List<LookupCall> calls = new ArrayList<>();

        AuraFluxEnergyBridge.ReceiverScan scan = AuraFluxEnergyBridge.findConnectedReceivers(
            origin,
            adjacent::equals,
            (target, side) -> {
                calls.add(new LookupCall(target, side));
                return receiver;
            }
        );

        assertEquals(List.of(receiver), scan.receivers());
        assertEquals(1, scan.machineCount());
        assertEquals(List.of(new LookupCall(adjacent, Direction.WEST)), calls);
    }

    @Test
    void receiverScanRetriesAnotherFaceWhenTheFirstFaceHasNoStorage() {
        BlockPos target = new BlockPos(1, 64, 1);
        MockEnergyStorage firstPath = new MockEnergyStorage(1_000L, 1_000L);
        MockEnergyStorage secondPath = new MockEnergyStorage(1_000L, 1_000L);
        MockEnergyStorage targetStorage = new MockEnergyStorage(1_000L, 1_000L);
        Map<BlockPos, EnergyStorage> graph = Map.of(
            pos(1), firstPath,
            new BlockPos(0, 64, 1), secondPath
        );
        List<Direction> queriedTargetFaces = new ArrayList<>();

        AuraFluxEnergyBridge.ReceiverScan scan = AuraFluxEnergyBridge.findConnectedReceivers(
            pos(0),
            ignored -> true,
            (candidate, side) -> {
                if (candidate.equals(target)) {
                    queriedTargetFaces.add(side);
                    return queriedTargetFaces.size() == 1 ? null : targetStorage;
                }
                return graph.get(candidate);
            }
        );

        assertEquals(3, scan.machineCount());
        assertTrue(scan.receivers().contains(targetStorage));
        assertEquals(2, queriedTargetFaces.size());
        assertTrue(queriedTargetFaces.get(0) != queriedTargetFaces.get(1));
    }

    @Test
    void receiverScanFollowsTheConnectedGraphAndDeduplicatesStorageIdentity() {
        MockEnergyStorage shared = new MockEnergyStorage(10_000L, 10_000L);
        MockEnergyStorage next = new MockEnergyStorage(10_000L, 10_000L);
        Map<BlockPos, EnergyStorage> graph = Map.of(
            pos(1), shared,
            pos(2), shared,
            pos(3), next
        );

        AuraFluxEnergyBridge.ReceiverScan scan = AuraFluxEnergyBridge.findConnectedReceivers(
            pos(0),
            ignored -> true,
            (target, side) -> graph.get(target)
        );

        assertEquals(List.of(shared, next), scan.receivers());
        assertEquals(3, scan.machineCount());
    }

    @Test
    void receiverScanStopsWhenTheConnectedGraphExceedsFourMachines() {
        Map<BlockPos, EnergyStorage> graph = new HashMap<>();
        for (int x = 1; x <= 6; x++) {
            graph.put(pos(x), new MockEnergyStorage(10_000L, 10_000L));
        }

        AuraFluxEnergyBridge.ReceiverScan scan = AuraFluxEnergyBridge.findConnectedReceivers(
            pos(0),
            ignored -> true,
            (target, side) -> graph.get(target)
        );

        assertEquals(5, scan.machineCount());
        assertEquals(5, scan.receivers().size());
        assertEquals(0, AuraFluxEnergyBridge.exportToReceivers(scan.receivers(), 1_000));
        assertTrue(scan.receivers().stream().allMatch(storage -> storage.getAmount() == 0L));
    }

    @Test
    void fourReceiversReceiveTheLegacyRateBatchedAcrossTwentyTicks() {
        List<MockEnergyStorage> receivers = List.of(
            new MockEnergyStorage(10_000L, 10_000L),
            new MockEnergyStorage(10_000L, 10_000L),
            new MockEnergyStorage(10_000L, 10_000L),
            new MockEnergyStorage(10_000L, 10_000L)
        );

        int consumed = AuraFluxEnergyBridge.exportToReceivers(new ArrayList<>(receivers), 1_000);

        assertEquals(998, consumed);
        assertTrue(receivers.stream().allMatch(receiver -> receiver.getAmount() == 3_740L));
        assertTrue(totalEnergy(receivers) <= consumed * 15L);
    }

    @Test
    void partialInsertionCommitsOnlyWhatWasAcceptedAndDebitsItsAuraEquivalent() {
        MockEnergyStorage receiver = new MockEnergyStorage(1_000L, 37L);

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(receiver), 100);

        assertEquals(37L, receiver.getAmount());
        assertEquals(3, consumed);
        assertTrue(receiver.getAmount() <= consumed * 15L);
    }

    @Test
    void fullReceiverSimulationDoesNotMutateAndOnlyAvailableEnergyIsDebited() {
        MockEnergyStorage full = new MockEnergyStorage(10L, 100L, 10L);
        MockEnergyStorage available = new MockEnergyStorage(1_000L, 100L);
        long initialEnergy = totalEnergy(List.of(full, available));

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(full, available), 100);

        assertEquals(10L, full.getAmount());
        assertEquals(100L, available.getAmount());
        assertEquals(7, consumed);
        long insertedEnergy = totalEnergy(List.of(full, available)) - initialEnergy;
        assertEquals(100L, insertedEnergy);
        assertTrue(insertedEnergy <= consumed * 15L);
    }

    @Test
    void theSameStorageObjectCannotMultiplyReceiverCapacity() {
        MockEnergyStorage shared = new MockEnergyStorage(2_000L, 2_000L);

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(shared, shared), 100);

        assertEquals(1_500L, shared.getAmount());
        assertEquals(100, consumed);
    }

    @Test
    void sourceRateUsesTheLegacyPowerFactor() {
        assertEquals(15_000L, AuraFluxTransferLogic.maximumEnergyPerReceiver(1_000, 1));
        assertEquals(7_500L, AuraFluxTransferLogic.maximumEnergyPerReceiver(1_000, 2));
        assertEquals(3_740L, AuraFluxTransferLogic.maximumEnergyPerReceiver(1_000, 4));
        assertEquals(0L, AuraFluxTransferLogic.maximumEnergyPerReceiver(1, 2));
        assertEquals(0, AuraFluxTransferLogic.auraConsumed(0L, 1_000));
        assertEquals(100, AuraFluxTransferLogic.auraConsumed(1_500L, 1_000));
        assertEquals(1, AuraFluxTransferLogic.auraConsumed(1L, 1_000));
        assertEquals(1_000, AuraFluxTransferLogic.auraConsumed(Long.MAX_VALUE, 1_000));
    }

    private static BlockPos pos(int x) {
        return new BlockPos(x, 64, 0);
    }

    private static long totalEnergy(List<? extends EnergyStorage> storages) {
        return storages.stream().mapToLong(EnergyStorage::getAmount).sum();
    }

    private record LookupCall(BlockPos pos, Direction side) {
    }

    private static final class MockEnergyStorage extends SnapshotParticipant<Long> implements EnergyStorage {
        private final long capacity;
        private final long insertLimit;
        private long amount;

        private MockEnergyStorage(long capacity, long insertLimit) {
            this(capacity, insertLimit, 0L);
        }

        private MockEnergyStorage(long capacity, long insertLimit, long initialAmount) {
            this.capacity = capacity;
            this.insertLimit = insertLimit;
            this.amount = initialAmount;
        }

        @Override
        public boolean supportsInsertion() {
            return true;
        }

        @Override
        public long insert(long maximum, TransactionContext transaction) {
            if (transaction == null || maximum <= 0L) {
                return 0L;
            }
            long accepted = Math.min(maximum, Math.min(insertLimit, capacity - amount));
            if (accepted > 0L) {
                updateSnapshots(transaction);
                amount += accepted;
            }
            return accepted;
        }

        @Override
        public boolean supportsExtraction() {
            return amount > 0L;
        }

        @Override
        public long extract(long maximum, TransactionContext transaction) {
            if (transaction == null || maximum <= 0L) {
                return 0L;
            }
            long extracted = Math.min(maximum, amount);
            if (extracted > 0L) {
                updateSnapshots(transaction);
                amount -= extracted;
            }
            return extracted;
        }

        @Override
        public long getAmount() {
            return amount;
        }

        @Override
        public long getCapacity() {
            return capacity;
        }

        @Override
        protected Long createSnapshot() {
            return amount;
        }

        @Override
        protected void readSnapshot(Long snapshot) {
            amount = snapshot;
        }
    }
}
