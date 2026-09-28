package pixlepix.auracascade.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.junit.jupiter.api.Test;

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
        MockEnergyStorage receiver = new MockEnergyStorage(1_000, 1_000);
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
        MockEnergyStorage firstPath = new MockEnergyStorage(1_000, 1_000);
        MockEnergyStorage secondPath = new MockEnergyStorage(1_000, 1_000);
        MockEnergyStorage targetStorage = new MockEnergyStorage(1_000, 1_000);
        Map<BlockPos, IEnergyStorage> graph = Map.of(
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
        MockEnergyStorage shared = new MockEnergyStorage(10_000, 10_000);
        MockEnergyStorage next = new MockEnergyStorage(10_000, 10_000);
        Map<BlockPos, IEnergyStorage> graph = Map.of(
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
        Map<BlockPos, IEnergyStorage> graph = new HashMap<>();
        for (int x = 1; x <= 6; x++) {
            graph.put(pos(x), new MockEnergyStorage(10_000, 10_000));
        }

        AuraFluxEnergyBridge.ReceiverScan scan = AuraFluxEnergyBridge.findConnectedReceivers(
            pos(0),
            ignored -> true,
            (target, side) -> graph.get(target)
        );

        assertEquals(5, scan.machineCount());
        assertEquals(5, scan.receivers().size());
        assertEquals(0, AuraFluxEnergyBridge.exportToReceivers(scan.receivers(), 1_000));
        assertTrue(scan.receivers().stream().allMatch(storage -> storage.getEnergyStored() == 0));
    }

    @Test
    void fourReceiversReceiveTheLegacyRateBatchedAcrossTwentyTicks() {
        List<MockEnergyStorage> receivers = List.of(
            new MockEnergyStorage(10_000, 10_000),
            new MockEnergyStorage(10_000, 10_000),
            new MockEnergyStorage(10_000, 10_000),
            new MockEnergyStorage(10_000, 10_000)
        );

        int consumed = AuraFluxEnergyBridge.exportToReceivers(new ArrayList<>(receivers), 1_000);

        assertEquals(998, consumed);
        assertTrue(receivers.stream().allMatch(receiver -> receiver.getEnergyStored() == 3_740));
        assertTrue(totalEnergy(receivers) <= consumed * 15L);
    }

    @Test
    void partialInsertionDebitsOnlyWhatWasAccepted() {
        MockEnergyStorage receiver = new MockEnergyStorage(1_000, 37);

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(receiver), 100);

        assertEquals(37, receiver.getEnergyStored());
        assertEquals(3, consumed);
        assertTrue(receiver.getEnergyStored() <= consumed * 15L);
    }

    @Test
    void fullReceiverSimulationDoesNotMutateAndOnlyAcceptedEnergyIsDebited() {
        MockEnergyStorage full = new MockEnergyStorage(10, 100, 10);
        MockEnergyStorage available = new MockEnergyStorage(1_000, 100);
        long initialEnergy = totalEnergy(List.of(full, available));

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(full, available), 100);

        assertEquals(10, full.getEnergyStored());
        assertEquals(100, available.getEnergyStored());
        assertEquals(7, consumed);
        long insertedEnergy = totalEnergy(List.of(full, available)) - initialEnergy;
        assertEquals(100L, insertedEnergy);
        assertTrue(insertedEnergy <= consumed * 15L);
    }

    @Test
    void theSameStorageObjectCannotMultiplyReceiverCapacity() {
        MockEnergyStorage shared = new MockEnergyStorage(2_000, 2_000);

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(shared, shared), 100);

        assertEquals(1_500, shared.getEnergyStored());
        assertEquals(100, consumed);
    }

    @Test
    void receiverThatRejectsSimulationIsNotDebited() {
        MockEnergyStorage rejecting = new MockEnergyStorage(1_000, 0);

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(rejecting), 100);

        assertEquals(0, rejecting.getEnergyStored());
        assertEquals(0, rejecting.actualInsertCalls());
        assertEquals(0, consumed);
    }

    @Test
    void receiverThatCannotReceiveIsNotCountedAsAMachine() {
        MockEnergyStorage disabled = new MockEnergyStorage(1_000, 1_000, false);

        AuraFluxEnergyBridge.ReceiverScan scan = AuraFluxEnergyBridge.findConnectedReceivers(
            pos(0),
            ignored -> true,
            (target, side) -> disabled
        );

        assertTrue(scan.receivers().isEmpty());
        assertEquals(0, scan.machineCount());
    }

    @Test
    void longRateAllowanceIsBoundedToTheNativeIntTransfer() {
        MockEnergyStorage receiver = new MockEnergyStorage(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertTrue(AuraFluxTransferLogic.maximumEnergyPerReceiver(Integer.MAX_VALUE, 1) > Integer.MAX_VALUE);

        int consumed = AuraFluxEnergyBridge.exportToReceivers(List.of(receiver), Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, receiver.getEnergyStored());
        assertEquals(Integer.MAX_VALUE, receiver.lastActualRequest());
        assertEquals(AuraFluxTransferLogic.auraConsumed(Integer.MAX_VALUE, Integer.MAX_VALUE), consumed);
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

    private static long totalEnergy(List<? extends IEnergyStorage> storages) {
        return storages.stream().mapToLong(IEnergyStorage::getEnergyStored).sum();
    }

    private record LookupCall(BlockPos pos, Direction side) {
    }

    private static final class MockEnergyStorage implements IEnergyStorage {
        private final int capacity;
        private final int insertLimit;
        private final boolean receiveEnabled;
        private int amount;
        private int actualInsertCalls;
        private int lastActualRequest;

        private MockEnergyStorage(int capacity, int insertLimit) {
            this(capacity, insertLimit, 0, true);
        }

        private MockEnergyStorage(int capacity, int insertLimit, int initialAmount) {
            this(capacity, insertLimit, initialAmount, true);
        }

        private MockEnergyStorage(int capacity, int insertLimit, boolean receiveEnabled) {
            this(capacity, insertLimit, 0, receiveEnabled);
        }

        private MockEnergyStorage(int capacity, int insertLimit, int initialAmount, boolean receiveEnabled) {
            this.capacity = capacity;
            this.insertLimit = insertLimit;
            this.receiveEnabled = receiveEnabled;
            this.amount = initialAmount;
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (!receiveEnabled || toReceive <= 0) {
                return 0;
            }
            int accepted = Math.min(toReceive, Math.min(insertLimit, capacity - amount));
            if (!simulate) {
                actualInsertCalls++;
                lastActualRequest = toReceive;
                amount += accepted;
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (toExtract <= 0) {
                return 0;
            }
            int extracted = Math.min(toExtract, amount);
            if (!simulate) {
                amount -= extracted;
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return amount;
        }

        @Override
        public int getMaxEnergyStored() {
            return capacity;
        }

        @Override
        public boolean canExtract() {
            return amount > 0;
        }

        @Override
        public boolean canReceive() {
            return receiveEnabled;
        }

        int actualInsertCalls() {
            return actualInsertCalls;
        }

        int lastActualRequest() {
            return lastActualRequest;
        }
    }
}
