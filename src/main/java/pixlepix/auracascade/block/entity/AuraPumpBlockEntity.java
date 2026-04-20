package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.aura.AuraKernel;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.parity.AuraColor;

public class AuraPumpBlockEntity extends AuraNetworkBlockEntity {
    private static final String PUMP_POWER_TAG = "pump_power";
    private static final String PUMP_SPEED_TAG = "pump_speed";

    private AuraPumpLogic.PumpState pumpState = new AuraPumpLogic.PumpState(0, 0);

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

        AuraPumpVariant variant = variant();
        if (variant.isCreative()) {
            addFuel(AuraPumpLogic.creativeFuel());
        } else if (!pumpState.active()) {
            switch (variant) {
                case BURNING, BURNING_ALT -> tryConsumeBurningFuel(level, pos);
                case ILLUMINATION, ILLUMINATION_ALT -> tryConsumeLightSources(level, pos);
                case REDSTONE, REDSTONE_ALT -> tryConsumeRedstone(level, pos);
                default -> {
                }
            }
        }

        if (level.getGameTime() % 20L == 2L) {
            pumpAuraUpward(level, pos);
        }
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
                BlockState targetState = level.getBlockState(targetPos);
                if (targetState.getBlock() instanceof RedStoneWireBlock && targetState.getValue(RedStoneWireBlock.POWER) > 0) {
                    level.destroyBlock(targetPos, false);
                    addFuel(AuraPumpLogic.redstoneFuel(distance));
                    return;
                }
                if (!targetState.isAir() && targetState.canOcclude()) {
                    break;
                }
            }
        }
    }

    private void pumpAuraUpward(Level level, BlockPos pos) {
        if (!pumpState.active() || level.hasNeighborSignal(pos)) {
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

            var request = AuraPumpLogic.planTransfer(
                nodeState,
                pos,
                targetPos,
                environment(level),
                pumpState,
                variant(),
                level.getGameTime()
            );

            if (request.isEmpty()) {
                return;
            }

            var filteredRequest = new pixlepix.auracascade.aura.AuraStorage();
            for (AuraColor color : AuraColor.values()) {
                if (request.get(color) > 0 && target.canReceiveAuraFrom(pos, color)) {
                    filteredRequest.set(color, request.get(color));
                }
            }

            if (filteredRequest.isEmpty()) {
                return;
            }

            pumpState = new AuraPumpLogic.PumpState(Math.max(0, pumpState.power() - 1), pumpState.speed());
            AuraKernel.applyTransfer(pos, nodeState, targetPos, target.nodeState, filteredRequest, environment(level));
            target.setChanged();
            setChanged();
            return;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pumpState = new AuraPumpLogic.PumpState(
            input.getIntOr(PUMP_POWER_TAG, 0),
            input.getIntOr(PUMP_SPEED_TAG, 0)
        );
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(PUMP_POWER_TAG, pumpState.power());
        output.putInt(PUMP_SPEED_TAG, pumpState.speed());
    }

    private static int burningFuelValue(net.minecraft.world.item.ItemStack stack) {
        var item = stack.getItem();
        if (item == net.minecraft.world.item.Items.COAL) {
            return 1_600;
        }
        if (item == net.minecraft.world.item.Items.CHARCOAL) {
            return 1_600;
        }
        if (item == net.minecraft.world.item.Items.BLAZE_ROD) {
            return 2_400;
        }
        if (item == net.minecraft.world.item.Items.COAL_BLOCK) {
            return 16_000;
        }
        if (item == net.minecraft.world.item.Items.LAVA_BUCKET) {
            return 20_000;
        }
        return 0;
    }
}
