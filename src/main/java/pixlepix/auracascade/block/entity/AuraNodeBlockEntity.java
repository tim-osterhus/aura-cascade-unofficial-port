package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.compat.AuraFluxBridgeRegistry;
import pixlepix.auracascade.parity.AuraColor;

public class AuraNodeBlockEntity extends AuraNetworkBlockEntity {
    private static final String CAPACITOR_THRESHOLD_INDEX_TAG = "capacitor_threshold_index";
    private static final String CAPACITOR_COOLDOWN_TAG = "capacitor_cooldown";
    private static final String CAPACITOR_BURST_TICKS_TAG = "capacitor_burst_ticks";
    private static final int[] CAPACITOR_THRESHOLDS = {100, 1_000, 10_000, 100_000};

    private int capacitorThresholdIndex = 1;
    private int capacitorCooldown;
    private int capacitorBurstTicks;

    public AuraNodeBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.AURA_NODE_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraNodeBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        AuraNodeVariant variant = variant();
        boolean powered = level.hasNeighborSignal(pos);

        if (variant.isCapacitor()) {
            tickCapacitor(level);
        }

        if (variant.isManipulator() && powered && level.getGameTime() % 20L == 2L) {
            nodeState = AuraNodeLogic.manipulatorState(variant);
            setChanged();
        }

        if (level.getGameTime() % 10L == 0L) {
            absorbNearbyAuraCrystals(level, pos);
        }

        serverTickBase(level, pos);

        if (variant.isFlux() && level.getGameTime() % 20L == 1L) {
            int acceptedPower = AuraFluxBridgeRegistry.export(level, pos, nodeState.storedPower());
            if (acceptedPower > 0) {
                nodeState.setStoredPower(nodeState.storedPower() - acceptedPower);
                setChanged();
            }
        }
    }

    @Override
    protected boolean canSendAuraTo(BlockPos targetPos, AuraColor color) {
        return AuraNodeLogic.canSend(
            variant(),
            getBlockPos(),
            targetPos,
            nodeState.storage().total(),
            capacitorThreshold(),
            level != null && level.hasNeighborSignal(getBlockPos())
        );
    }

    @Override
    protected boolean canReceiveAuraFrom(BlockPos sourcePos, AuraColor color) {
        return AuraNodeLogic.canReceive(variant(), color, capacitorCooldown);
    }

    @Override
    protected int comparatorCapacity() {
        return AuraNodeLogic.comparatorCapacity(variant(), capacitorThreshold());
    }

    @Override
    public int auraSignal() {
        int signal = super.auraSignal();
        if (variant().isFlux() && nodeState.storedPower() > 0) {
            signal = Math.max(signal, Math.min(15, (int) Math.ceil(nodeState.storedPower() / 100.0D)));
        }
        if (variant().isCapacitor() && capacitorBurstTicks > 0) {
            signal = 15;
        }
        return signal;
    }

    public int capacitorThreshold() {
        return CAPACITOR_THRESHOLDS[Math.max(0, Math.min(CAPACITOR_THRESHOLDS.length - 1, capacitorThresholdIndex))];
    }

    private AuraNodeVariant variant() {
        AuraNodeVariant variant = AuraContent.nodeVariant(getBlockState().getBlock());
        return variant != null ? variant : AuraNodeVariant.AURA_NODE;
    }

    private void tickCapacitor(Level level) {
        if (capacitorCooldown > 0) {
            capacitorCooldown--;
        }

        if (capacitorBurstTicks > 0) {
            capacitorBurstTicks--;
            if (capacitorBurstTicks == 0) {
                capacitorCooldown = 110;
            }
        } else if (level.getGameTime() % 19L == 0L
            && AuraNodeLogic.shouldStartCapacitorBurst(
            nodeState.storage().total(),
            capacitorThreshold(),
            capacitorCooldown,
            capacitorBurstTicks
        )) {
            capacitorBurstTicks = 5;
        }
    }

    private void absorbNearbyAuraCrystals(Level level, BlockPos pos) {
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(0.75D))) {
            var stack = itemEntity.getItem();
            var crystalColor = AuraItems.auraCrystalColor(stack);
            if (crystalColor.isEmpty()) {
                continue;
            }

            nodeState.storage().add(crystalColor.get(), AuraItems.auraCrystalCharge(stack));
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            setChanged();
            return;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        capacitorThresholdIndex = input.getIntOr(CAPACITOR_THRESHOLD_INDEX_TAG, 1);
        capacitorCooldown = input.getIntOr(CAPACITOR_COOLDOWN_TAG, 0);
        capacitorBurstTicks = input.getIntOr(CAPACITOR_BURST_TICKS_TAG, 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(CAPACITOR_THRESHOLD_INDEX_TAG, capacitorThresholdIndex);
        output.putInt(CAPACITOR_COOLDOWN_TAG, capacitorCooldown);
        output.putInt(CAPACITOR_BURST_TICKS_TAG, capacitorBurstTicks);
    }
}
