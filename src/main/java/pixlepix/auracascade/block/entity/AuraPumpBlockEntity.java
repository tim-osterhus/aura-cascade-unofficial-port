package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.aura.AuraKernel;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.NbtCompat;

public class AuraPumpBlockEntity extends AuraNetworkBlockEntity {
    private static final String PUMP_POWER_TAG = "pump_power";
    private static final String PUMP_SPEED_TAG = "pump_speed";
    private static final String PUMP_INHIBITED_TAG = "pump_inhibited";

    private AuraPumpLogic.PumpState pumpState = new AuraPumpLogic.PumpState(0, 0);
    private boolean pumpInhibited;

    public AuraPumpBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.AURA_PUMP_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AuraPumpBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        serverTickBase(level, pos);
        boolean inhibited = level.hasNeighborSignal(pos);
        if (pumpInhibited != inhibited) {
            pumpInhibited = inhibited;
            setChanged();
        }

        AuraPumpVariant variant = variant();
        if (variant.isCreative()) {
            addFuel(AuraPumpLogic.creativeFuel());
        } else if (pumpState.power() == 0) {
            switch (variant) {
                case BURNING, BURNING_ALT -> {
                    if (level.getGameTime() % 20L == 2L && !pumpInhibited) {
                        tryConsumeBurningFuel(level, pos);
                    }
                }
                case ILLUMINATION, ILLUMINATION_ALT -> tryConsumeLightSources(level, pos);
                case REDSTONE, REDSTONE_ALT -> tryConsumeRedstone(level, pos);
                default -> {
                }
            }
        }

        if (level.getGameTime() % 20L == 2L) {
            pumpAuraUpward(level, pos);
        }
        syncInspection(level, pos);
    }

    @Override
    protected boolean canSendAuraTo(BlockPos targetPos, AuraColor color) {
        return false;
    }

    @Override
    protected boolean canReceiveAuraFrom(BlockPos sourcePos, AuraColor color) {
        return sourcePos.getY() <= getBlockPos().getY();
    }

    @Override
    protected int comparatorCapacity() {
        return 1_000;
    }

    @Override
    public int auraSignal() {
        return AuraMonitorLogic.pumpSignal(inspectionState(), comparatorCapacity(), pumpState.active());
    }

    public AuraPumpLogic.PumpState pumpState() {
        return pumpState;
    }

    public boolean pumpInhibited() {
        return pumpInhibited;
    }

    public void feedFromFall(float fallDistance) {
        addFuel(AuraPumpLogic.fallFuel(fallDistance));
    }

    public void feedFromArrow() {
        addFuel(AuraPumpLogic.arrowFuel());
    }

    public void feedFromEgg() {
        addFuel(AuraPumpLogic.eggFuel());
    }

    public void feedFromSnowball() {
        addFuel(AuraPumpLogic.snowballFuel());
    }

    private void addFuel(AuraPumpLogic.FuelOffer offer) {
        pumpState = AuraPumpLogic.addFuel(variant(), pumpState, offer);
        setChanged();
    }

    private AuraPumpVariant variant() {
        AuraPumpVariant variant = AuraContent.pumpVariant(getBlockState().getBlock());
        return variant != null ? variant : AuraPumpVariant.BURNING;
    }

    private void tryConsumeBurningFuel(Level level, BlockPos pos) {
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3.0D))) {
            if (itemEntity.isRemoved()) {
                continue;
            }
            var stack = itemEntity.getItem();
            int burnTime = burningFuelValue(stack);
            if (burnTime <= 0) {
                continue;
            }
            addFuel(AuraPumpLogic.burningFuel(burnTime));
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            return;
        }
    }

    private void tryConsumeLightSources(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            if (!level.hasChunkAt(targetPos)) {
                continue;
            }
            BlockState targetState = level.getBlockState(targetPos);
            if (targetState.is(Blocks.GLOWSTONE)) {
                level.destroyBlock(targetPos, false);
                addFuel(AuraPumpLogic.glowstoneFuel());
                return;
            }
            if (targetState.is(Blocks.TORCH) || targetState.is(Blocks.WALL_TORCH)) {
                level.destroyBlock(targetPos, false);
                addFuel(AuraPumpLogic.torchFuel());
                return;
            }
        }
    }

    private void tryConsumeRedstone(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= AuraKernel.DEFAULT_LINK_RANGE; distance++) {
                BlockPos targetPos = pos.relative(direction, distance);
                if (!level.hasChunkAt(targetPos)) {
                    break;
                }
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.getBlock() instanceof RedStoneWireBlock && targetState.getValue(RedStoneWireBlock.POWER) > 0) {
                    level.destroyBlock(targetPos, false);
                    addFuel(AuraPumpLogic.redstoneFuel(distance));
                }
                if (!(targetState.getBlock() instanceof RedStoneWireBlock)) {
                    break;
                }
            }
        }
    }

    private void pumpAuraUpward(Level level, BlockPos pos) {
        if (pumpState.power() == 0 || pumpInhibited) {
            return;
        }

        for (int distance = 1; distance <= AuraKernel.DEFAULT_LINK_RANGE; distance++) {
            BlockPos targetPos = pos.above(distance);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (!(blockEntity instanceof AuraNetworkBlockEntity target)) {
                BlockState targetState = level.getBlockState(targetPos);
                if (!targetState.isAir() && targetState.canOcclude()) {
                    break;
                }
                continue;
            }

            if (!nodeState.hasScannedLinks()) {
                refreshLinks(level, pos);
            }

            AuraPumpLogic.PumpState transferState = pumpState;
            pumpState = AuraPumpLogic.spendForTarget(pumpState);
            setChanged();

            var request = AuraPumpLogic.planTransfer(
                nodeState,
                pos,
                targetPos,
                environment(level),
                transferState,
                variant(),
                level.getGameTime()
            );

            if (request.isEmpty()) {
                return;
            }

            var result = AuraKernel.applyTransfer(pos, nodeState, targetPos, target.nodeState, request, environment(level));
            if (!result.moved().isEmpty()) {
                emitTransferParticles(level, pos, targetPos, result.moved());
                target.setChanged();
            }
            return;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        pumpState = readPumpState(tag);
        pumpInhibited = readPumpInhibited(tag);
    }

    static AuraPumpLogic.PumpState readPumpState(CompoundTag tag) {
        return new AuraPumpLogic.PumpState(
            NbtCompat.getIntOr(tag, PUMP_POWER_TAG, 0),
            NbtCompat.getIntOr(tag, PUMP_SPEED_TAG, 0)
        );
    }

    static boolean readPumpInhibited(CompoundTag tag) {
        return NbtCompat.getBooleanOr(tag, PUMP_INHIBITED_TAG, false);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writePumpState(tag, pumpState, pumpInhibited);
    }

    static void writePumpState(CompoundTag tag, AuraPumpLogic.PumpState state, boolean inhibited) {
        tag.putInt(PUMP_POWER_TAG, state.power());
        tag.putInt(PUMP_SPEED_TAG, state.speed());
        tag.putBoolean(PUMP_INHIBITED_TAG, inhibited);
    }

    static int burningFuelValue(ItemStack stack) {
        return AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
    }
}
