package pixlepix.auracascade.qa.neoforge.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

final class EnergyReceiverBlockEntity extends BlockEntity implements IEnergyStorage {
    private Mode mode = Mode.ACCEPT;
    private int capacity = 100_000;
    private int maxReceive = Integer.MAX_VALUE;
    private int energy;
    private Direction inputSide;
    private int simulationCalls;
    private int actualInsertCalls;

    EnergyReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(EnergyInteropQaMod.RECEIVER_BLOCK_ENTITY, pos, state);
    }

    void configure(Mode mode, int capacity, int energy, int maxReceive, @Nullable Direction inputSide) {
        if (capacity < 1 || energy < 0 || energy > capacity || maxReceive < 0) {
            throw new IllegalArgumentException("Invalid energy receiver configuration");
        }
        this.mode = mode;
        this.capacity = capacity;
        this.energy = energy;
        this.maxReceive = maxReceive;
        this.inputSide = inputSide;
    }

    @Nullable
    IEnergyStorage getEnergyCapability(@Nullable Direction side) {
        return inputSide == null || inputSide == side ? this : null;
    }

    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        if (simulate) {
            simulationCalls++;
        } else {
            actualInsertCalls++;
        }
        if (mode == Mode.REJECT || amount <= 0) {
            return 0;
        }

        int accepted = Math.min(amount, Math.min(maxReceive, capacity - energy));
        if (!simulate && accepted > 0) {
            energy += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    int simulationCalls() {
        return simulationCalls;
    }

    int actualInsertCalls() {
        return actualInsertCalls;
    }

    enum Mode {
        ACCEPT,
        REJECT
    }
}
