package pixlepix.auracascade.block.entity;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraInspectionState;
import pixlepix.auracascade.aura.AuraKernel;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.aura.AuraTickContext;
import pixlepix.auracascade.aura.AuraTransferContext;
import pixlepix.auracascade.aura.AuraTransferResult;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.parity.AuraColor;

public abstract class AuraNetworkBlockEntity extends BlockEntity implements AuraSignalSource {
    private static final String NODE_STATE_TAG = "node_state";

    protected AuraNodeState nodeState = new AuraNodeState();

    protected AuraNetworkBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public AuraInspectionState inspectionState() {
        return nodeState.inspectionState();
    }

    @Override
    public int auraSignal() {
        return AuraMonitorLogic.nodeSignal(inspectionState(), comparatorCapacity());
    }

    protected final void serverTickBase(Level level, BlockPos pos) {
        AuraKernel.applyPassiveTick(nodeState, new AuraTickContext(level.getGameTime(), environment(level)));

        if (!nodeState.hasScannedLinks() || level.getGameTime() % 200L == 0L) {
            refreshLinks(level, pos);
        }

        if (level.getGameTime() % 20L == 0L) {
            transferNaturalAura(level, pos);
        }
    }

    protected abstract boolean canSendAuraTo(BlockPos targetPos, AuraColor color);

    protected abstract boolean canReceiveAuraFrom(BlockPos sourcePos, AuraColor color);

    protected abstract int comparatorCapacity();

    protected final AuraEnvironment environment(Level level) {
        boolean daytime = Math.floorMod(level.getDayTime(), 24_000L) < 12_000L;
        return new AuraEnvironment(daytime, level.isRaining());
    }

    public int storedPower() {
        return nodeState.storedPower();
    }

    public int extractStoredPower(int maxAmount) {
        int extracted = Math.min(Math.max(0, maxAmount), nodeState.storedPower());
        if (extracted > 0) {
            nodeState.setStoredPower(nodeState.storedPower() - extracted);
            setChanged();
        }
        return extracted;
    }

    protected final Map<BlockPos, AuraNetworkBlockEntity> linkedNetworks(Level level) {
        LinkedHashMap<BlockPos, AuraNetworkBlockEntity> linkedNetworks = new LinkedHashMap<>();
        for (BlockPos linkedPos : nodeState.linkedNodes()) {
            BlockEntity blockEntity = level.getBlockEntity(linkedPos);
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                linkedNetworks.put(linkedPos, auraNetworkBlockEntity);
            }
        }
        return linkedNetworks;
    }

    protected final void refreshLinks(Level level, BlockPos pos) {
        nodeState.replaceLinkedNodes(scanStraightLinks(level, pos));
        nodeState.setHasScannedLinks(true);
        setChanged();
    }

    private Set<BlockPos> scanStraightLinks(Level level, BlockPos pos) {
        java.util.LinkedHashSet<BlockPos> links = new java.util.LinkedHashSet<>();
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= AuraKernel.DEFAULT_LINK_RANGE; distance++) {
                BlockPos targetPos = pos.relative(direction, distance);
                BlockState targetState = level.getBlockState(targetPos);
                Block targetBlock = targetState.getBlock();

                if (AuraContent.isAuraNetworkBlock(targetBlock)) {
                    BlockEntity blockEntity = level.getBlockEntity(targetPos);
                    if (blockEntity instanceof AuraNetworkBlockEntity && !targetPos.equals(pos)) {
                        links.add(targetPos.immutable());
                    }
                    break;
                }

                if (!targetState.isAir() && targetState.canOcclude()) {
                    break;
                }
            }
        }
        return links;
    }

    private void transferNaturalAura(Level level, BlockPos pos) {
        Map<BlockPos, AuraNetworkBlockEntity> connected = linkedNetworks(level);
        if (connected.isEmpty()) {
            return;
        }

        LinkedHashMap<BlockPos, AuraNodeState> connectedStates = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, AuraNetworkBlockEntity> entry : connected.entrySet()) {
            connectedStates.put(entry.getKey(), entry.getValue().nodeState);
        }

        Map<BlockPos, AuraStorage> plans = AuraKernel.planNaturalTransfers(
            pos,
            nodeState,
            connectedStates,
            AuraTransferContext.natural(environment(level))
        );

        for (Map.Entry<BlockPos, AuraStorage> entry : plans.entrySet()) {
            BlockPos targetPos = entry.getKey();
            AuraNetworkBlockEntity target = connected.get(targetPos);
            if (target == null) {
                continue;
            }

            AuraStorage filteredRequest = filterTransferRequest(targetPos, entry.getValue(), target);
            if (filteredRequest.isEmpty()) {
                continue;
            }

            AuraTransferResult transferResult = AuraKernel.applyTransfer(
                pos,
                nodeState,
                targetPos,
                target.nodeState,
                filteredRequest,
                environment(level)
            );

            applyOrangeCurrents(level, pos, targetPos, transferResult, connected);
            target.setChanged();
            setChanged();
        }
    }

    private AuraStorage filterTransferRequest(BlockPos targetPos, AuraStorage request, AuraNetworkBlockEntity target) {
        AuraStorage filtered = new AuraStorage();
        for (AuraColor color : AuraColor.values()) {
            int requestedAmount = request.get(color);
            if (requestedAmount <= 0) {
                continue;
            }
            if (!canSendAuraTo(targetPos, color)) {
                continue;
            }
            if (!target.canReceiveAuraFrom(getBlockPos(), color)) {
                continue;
            }
            filtered.set(color, requestedAmount);
        }
        return filtered;
    }

    private void applyOrangeCurrents(
        Level level,
        BlockPos sourcePos,
        BlockPos targetPos,
        AuraTransferResult transferResult,
        Map<BlockPos, AuraNetworkBlockEntity> connected
    ) {
        int orangeAmount = transferResult.moved().get(AuraColor.ORANGE);
        if (orangeAmount <= 0) {
            return;
        }

        Direction direction = AuraKernel.cardinalDirection(sourcePos, targetPos).orElse(null);
        if (direction == null || direction == Direction.UP || direction == Direction.DOWN) {
            return;
        }

        LinkedHashMap<BlockPos, Set<BlockPos>> networkLinks = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, AuraNetworkBlockEntity> entry : connected.entrySet()) {
            networkLinks.put(entry.getKey(), entry.getValue().nodeState.linkedNodes());
        }

        for (var inducedCurrent : AuraKernel.planOrangeInducedCurrents(sourcePos, direction, orangeAmount, networkLinks)) {
            AuraNetworkBlockEntity currentNode = connected.get(inducedCurrent.nodePos());
            AuraNetworkBlockEntity downstreamNode = connected.get(inducedCurrent.downstreamTarget());
            if (currentNode == null || downstreamNode == null) {
                continue;
            }

            AuraStorage nonOrange = currentNode.nodeState.storage().copy();
            nonOrange.set(AuraColor.ORANGE, 0);
            int nonOrangeTotal = nonOrange.total();
            if (nonOrangeTotal <= 0) {
                continue;
            }

            double factor = Math.min(1.0D, inducedCurrent.amount() / (double) nonOrangeTotal);
            AuraStorage requested = nonOrange.scaled(factor);
            if (requested.isEmpty()) {
                continue;
            }

            AuraKernel.applyTransfer(
                inducedCurrent.nodePos(),
                currentNode.nodeState,
                inducedCurrent.downstreamTarget(),
                downstreamNode.nodeState,
                requested,
                environment(level)
            );
            currentNode.setChanged();
            downstreamNode.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        nodeState = AuraNodeState.fromTag(input.read(NODE_STATE_TAG, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(NODE_STATE_TAG, CompoundTag.CODEC, nodeState.toTag());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
