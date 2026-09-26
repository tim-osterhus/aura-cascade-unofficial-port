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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraInspectionState;
import pixlepix.auracascade.aura.AuraKernel;
import pixlepix.auracascade.aura.AuraPalette;
import pixlepix.auracascade.aura.AuraTransferVisuals;
import pixlepix.auracascade.aura.WorldInteractionVisuals;
import org.joml.Vector3f;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.aura.AuraTickContext;
import pixlepix.auracascade.aura.AuraTransferContext;
import pixlepix.auracascade.aura.AuraTransferResult;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.util.NbtCompat;
import pixlepix.auracascade.mixin.CreeperAccessor;

public abstract class AuraNetworkBlockEntity extends BlockEntity implements AuraSignalSource {
    private static final String NODE_STATE_TAG = "node_state";
    private int placementPreviewTick;

    protected AuraNodeState nodeState = new AuraNodeState();
    private CompoundTag lastSyncedInspection;
    private Map<BlockPos, AuraStorage> pendingNaturalTransfers = Map.of();
    private final LinkedHashMap<BlockPos, Integer> inducedBurstMap = new LinkedHashMap<>();

    protected AuraNetworkBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public AuraInspectionState inspectionState() {
        return nodeState.inspectionState();
    }

    public void feedCrystal(AuraColor color, int amount) {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide() || amount <= 0) {
            return;
        }

        nodeState.storage().add(color, amount);
        setChanged();
        currentLevel.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public int auraSignal() {
        return AuraMonitorLogic.nodeSignal(inspectionState(), comparatorCapacity());
    }

    protected final void serverTickBase(Level level, BlockPos pos) {
        tickPlacementPreview(level, pos);
        AuraKernel.applyPassiveTick(nodeState, new AuraTickContext(level.getGameTime(), environment(level)));

        if (!nodeState.hasScannedLinks() || level.getGameTime() % 200L == 0L) {
            refreshLinks(level, pos);
        }

        long tickPhase = level.getGameTime() % 20L;
        if (tickPhase == 0L) {
            pendingNaturalTransfers = planNaturalAura(level, pos);
        } else if (tickPhase == 1L) {
            applyPendingNaturalTransfers(level, pos);
        } else if (tickPhase == 2L) {
            applyOrangeBursts(level);
        }
        absorbRedExplosives(level, pos);
    }

    private void absorbRedExplosives(Level level, BlockPos pos) {
        if (nodeState.storage().get(AuraColor.RED) <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        AABB bounds = new AABB(pos).inflate(3.0D);
        for (PrimedTnt tnt : level.getEntitiesOfClass(PrimedTnt.class, bounds, entity -> !entity.isRemoved() && entity.getFuse() <= 2)) {
            tnt.discard();
            pushRedAura(server, pos, 200_000);
        }
        for (Creeper creeper : level.getEntitiesOfClass(Creeper.class, bounds, entity -> !entity.isRemoved())) {
            CreeperAccessor fuse = (CreeperAccessor) creeper;
            if (fuse.aura$getSwell() + 2 >= fuse.aura$getMaxSwell()) {
                creeper.discard();
                pushRedAura(server, pos, 50_000);
            }
        }
    }

    private void pushRedAura(ServerLevel level, BlockPos pos, int liftBudget) {
        for (var entry : linkedNetworks(level).entrySet()) {
            AuraStorage requested = AuraKernel.planRedExplosionPushUp(pos, nodeState, entry.getKey(), liftBudget);
            AuraTransferResult result = AuraKernel.applyTransfer(pos, nodeState, entry.getKey(), entry.getValue().nodeState,
                requested, environment(level));
            emitTransferParticles(level, pos, entry.getKey(), result.moved());
            entry.getValue().setChanged();
        }
        // The original finishes explosion visuals without calculating damaging rays.
        level.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 4.0F, 1.0F);
        setChanged();
    }

    protected abstract boolean canSendAuraTo(BlockPos targetPos, AuraColor color);

    protected abstract boolean canReceiveAuraFrom(BlockPos sourcePos, AuraColor color);

    protected abstract int comparatorCapacity();

    protected final void syncInspection(Level level, BlockPos pos) {
        if (level.isClientSide() || level.getGameTime() % 5L != 0L) {
            return;
        }
        CompoundTag current = getUpdateTag(level.registryAccess());
        if (!current.equals(lastSyncedInspection)) {
            lastSyncedInspection = current.copy();
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    protected final void emitTransferParticles(Level level, BlockPos source, BlockPos target, AuraStorage moved) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        for (AuraTransferVisuals.Sample sample : AuraTransferVisuals.samples(source, target, moved)) {
            int rgb = AuraPalette.rgb(sample.color());
            DustParticleOptions particle = new DustParticleOptions(new Vector3f(
                ((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F
            ), 0.8F);
            serverLevel.sendParticles(particle, sample.position().x, sample.position().y, sample.position().z,
                0, sample.direction().x, sample.direction().y, sample.direction().z, 0.12D);
        }
    }

    private void tickPlacementPreview(Level level, BlockPos pos) {
        if (placementPreviewTick >= WorldInteractionVisuals.PLACEMENT_TICKS) {
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            DustParticleOptions particle = new DustParticleOptions(new Vector3f(0.35F, 0.9F, 1.0F), 1.1F);
            for (var point : WorldInteractionVisuals.placementSamples(pos, scanStraightLinks(level, pos), placementPreviewTick)) {
                serverLevel.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        placementPreviewTick++;
    }

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
        refreshLinks(level, worldPosition);
        LinkedHashMap<BlockPos, AuraNetworkBlockEntity> linkedNetworks = new LinkedHashMap<>();
        for (BlockPos linkedPos : nodeState.linkedNodes()) {
            if (!level.hasChunkAt(linkedPos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(linkedPos);
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                linkedNetworks.put(linkedPos, auraNetworkBlockEntity);
            }
        }
        return linkedNetworks;
    }

    protected final void refreshLinks(Level level, BlockPos pos) {
        Set<BlockPos> links = scanStraightLinks(level, pos);
        if (nodeState.hasScannedLinks() && nodeState.linkedNodes().equals(links)) {
            return;
        }
        nodeState.replaceLinkedNodes(links);
        nodeState.setHasScannedLinks(true);
        setChanged();
    }

    protected final Set<BlockPos> scanStraightLinks(Level level, BlockPos pos) {
        java.util.LinkedHashSet<BlockPos> links = new java.util.LinkedHashSet<>();
        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= AuraKernel.DEFAULT_LINK_RANGE; distance++) {
                BlockPos targetPos = pos.relative(direction, distance);
                if (!level.hasChunkAt(targetPos)) {
                    break;
                }
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

    private Map<BlockPos, AuraStorage> planNaturalAura(Level level, BlockPos pos) {
        Map<BlockPos, AuraNetworkBlockEntity> connected = linkedNetworks(level);
        if (connected.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<BlockPos, AuraNodeState> connectedStates = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, AuraNetworkBlockEntity> entry : connected.entrySet()) {
            connectedStates.put(entry.getKey(), entry.getValue().nodeState);
        }

        Map<BlockPos, AuraStorage> plans = AuraKernel.planNaturalTransfers(
            pos,
            nodeState,
            connectedStates,
            new AuraTransferContext(environment(level), AuraKernel.DEFAULT_EQUILIBRIUM_THRESHOLD,
                this instanceof AuraNodeBlockEntity node && node.isCapacitor() ? 0 : AuraKernel.DEFAULT_RETENTION_WEIGHT, true),
            targetPos -> canSendAuraTo(targetPos, AuraColor.WHITE)
        );

        return plans;
    }

    private void applyPendingNaturalTransfers(Level level, BlockPos pos) {
        Map<BlockPos, AuraStorage> plans = pendingNaturalTransfers;
        pendingNaturalTransfers = Map.of();
        if (plans.isEmpty()) {
            return;
        }

        Map<BlockPos, AuraNetworkBlockEntity> connected = linkedNetworks(level);
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

            if (target instanceof VortexPedestalBlockEntity pedestal) {
                pedestal.receiveFallingPower(transferResult.moved(), pos.getY() - targetPos.getY(), environment(level));
            }
            queueOrangeCurrents(level, pos, targetPos, transferResult);
            emitTransferParticles(level, pos, targetPos, transferResult.moved());
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

    private void queueOrangeCurrents(
        Level level,
        BlockPos sourcePos,
        BlockPos targetPos,
        AuraTransferResult transferResult
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
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos nodePos = sourcePos.offset(dx, dy, dz);
                    if (nodePos.equals(sourcePos) || !level.hasChunkAt(nodePos)) {
                        continue;
                    }
                    BlockEntity blockEntity = level.getBlockEntity(nodePos);
                    if (blockEntity instanceof AuraNetworkBlockEntity networkNode) {
                        networkLinks.put(nodePos, networkNode.nodeState.linkedNodes());
                    }
                }
            }
        }

        for (var inducedCurrent : AuraKernel.planOrangeInducedCurrents(sourcePos, direction, orangeAmount, networkLinks)) {
            if (!level.hasChunkAt(inducedCurrent.nodePos())) {
                continue;
            }
            BlockEntity currentBlockEntity = level.getBlockEntity(inducedCurrent.nodePos());
            if (!(currentBlockEntity instanceof AuraNetworkBlockEntity currentNode)) {
                continue;
            }

            currentNode.inducedBurstMap.put(inducedCurrent.downstreamTarget().immutable(), inducedCurrent.amount());
        }
    }

    private void applyOrangeBursts(Level level) {
        refreshLinks(level, worldPosition);
        LinkedHashMap<BlockPos, Integer> deferred = new LinkedHashMap<>();
        for (BlockPos targetPos : nodeState.linkedNodes()) {
            Integer requestedAmount = inducedBurstMap.get(targetPos);
            if (requestedAmount == null) {
                continue;
            }
            if (!level.hasChunkAt(targetPos)) {
                deferred.put(targetPos, requestedAmount);
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            inducedBurstMap.put(targetPos, 0);
            if (!(blockEntity instanceof AuraNetworkBlockEntity downstreamNode)) {
                continue;
            }

            int opposingAmount = downstreamNode.inducedBurstMap.getOrDefault(worldPosition, 0);
            AuraKernel.OrangeBurstPlan plan = AuraKernel.planOrangeBurst(
                nodeState.storage(), requestedAmount, opposingAmount
            );
            downstreamNode.inducedBurstMap.put(worldPosition.immutable(), plan.opposingRemainder());
            AuraStorage requested = plan.transfer();
            if (requested.isEmpty()) {
                continue;
            }

            AuraTransferResult result = AuraKernel.applyTransfer(
                worldPosition,
                nodeState,
                targetPos,
                downstreamNode.nodeState,
                requested,
                environment(level)
            );
            if (downstreamNode instanceof VortexPedestalBlockEntity pedestal) {
                pedestal.receiveFallingPower(result.moved(),
                    worldPosition.getY() - targetPos.getY(), environment(level));
            }
            emitTransferParticles(level, worldPosition, targetPos, result.moved());
            setChanged();
            downstreamNode.setChanged();
        }

        inducedBurstMap.clear();
        inducedBurstMap.putAll(deferred);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        placementPreviewTick = WorldInteractionVisuals.PLACEMENT_TICKS;
        nodeState = AuraNodeState.fromTag(NbtCompat.getCompoundOrEmpty(tag, NODE_STATE_TAG));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(NODE_STATE_TAG, nodeState.toTag());
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
