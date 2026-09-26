package pixlepix.auracascade.block.entity;

import com.mojang.serialization.Codec;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.LateGameVariant;
import pixlepix.auracascade.util.NbtCompat;

public class LateGameBlockEntity extends BlockEntity implements AuraSignalSource {
    private static final String STORED_POWER_TAG = "stored_power";
    private static final String PROGRESS_TAG = "progress";
    private static final String MINER_CHARGE_TAG = "miner_charge";
    private static final String PULSE_LATCH_TAG = "pulse_latch";
    private static final String LAST_CHARGED_TAG = "last_charged";
    private static final String LAST_EXPLOSION_TAG = "last_explosion";
    private static final String MINER_ENTITY_TAG = "miner_entity";
    private static final String LAST_POWER_TAG = "last_power";
    private static final String LAST_RESULT_TAG = "last_result";
    private static final String RITUAL_QUEUE_TAG = "ritual_queue";
    private static final String RITUAL_BIOME_TAG = "ritual_source_biome";
    private static final Codec<long[]> LONG_ARRAY_CODEC = Codec.LONG_STREAM.xmap(LongStream::toArray, Arrays::stream);

    private int storedPower;
    private int progress;
    private int minerCharge;
    private boolean minerPulseLatched;
    private long lastChargedTick;
    private long lastExplosionTick;
    private UUID minerEntityId;
    private int lastPower;
    private WorkResult lastResult = WorkResult.NONE;
    private final ArrayDeque<BlockPos> ritualQueue = new ArrayDeque<>();
    private ResourceKey<Biome> ritualSourceBiome;

    public LateGameBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.LATE_GAME_BLOCK_ENTITY, pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, LateGameBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            blockEntity.serverTick(level, pos);
        }
    }

    @Override
    public int auraSignal() {
        int base = Math.max(1, variant().powerPerProgress());
        return Math.min(15, Math.max(minerCharge, storedPower) / base + (minerCharge > 0 ? 1 : 0));
    }

    public InspectionSnapshot inspectionSnapshot() {
        return new InspectionSnapshot(progress, variant().maxProgress(), storedPower, minerCharge,
            lastPower, lastResult, ritualQueue.size());
    }

    public boolean hasValidWork() {
        return true;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedPower = NbtCompat.getIntOr(input, STORED_POWER_TAG, 0);
        progress = NbtCompat.getIntOr(input, PROGRESS_TAG, 0);
        minerCharge = NbtCompat.getIntOr(input, MINER_CHARGE_TAG, 0);
        minerPulseLatched = NbtCompat.getBooleanOr(input, PULSE_LATCH_TAG, false);
        lastChargedTick = NbtCompat.getLongOr(input, LAST_CHARGED_TAG, 0L);
        lastExplosionTick = NbtCompat.getLongOr(input, LAST_EXPLOSION_TAG, 0L);
        String minerId = NbtCompat.getStringOr(input, MINER_ENTITY_TAG, "");
        try {
            minerEntityId = minerId.isEmpty() ? null : UUID.fromString(minerId);
        } catch (IllegalArgumentException ignored) {
            minerEntityId = null;
        }
        lastPower = NbtCompat.getIntOr(input, LAST_POWER_TAG, 0);
        try {
            lastResult = WorkResult.valueOf(NbtCompat.getStringOr(input, LAST_RESULT_TAG, "NONE"));
        } catch (IllegalArgumentException ignored) {
            lastResult = WorkResult.NONE;
        }
        ritualQueue.clear();
        for (long packed : NbtCompat.read(input, RITUAL_QUEUE_TAG, LONG_ARRAY_CODEC).orElseGet(() -> new long[0])) {
            ritualQueue.add(BlockPos.of(packed));
        }
        String biomeId = NbtCompat.getStringOr(input, RITUAL_BIOME_TAG, "");
        ritualSourceBiome = biomeId.isEmpty() ? null
            : ResourceKey.create(Registries.BIOME, Identifier.parse(biomeId));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(STORED_POWER_TAG, storedPower);
        output.putInt(PROGRESS_TAG, progress);
        output.putInt(MINER_CHARGE_TAG, minerCharge);
        output.putBoolean(PULSE_LATCH_TAG, minerPulseLatched);
        output.putLong(LAST_CHARGED_TAG, lastChargedTick);
        output.putLong(LAST_EXPLOSION_TAG, lastExplosionTick);
        if (minerEntityId != null) {
            output.putString(MINER_ENTITY_TAG, minerEntityId.toString());
        }
        output.putInt(LAST_POWER_TAG, lastPower);
        output.putString(LAST_RESULT_TAG, lastResult.name());
        NbtCompat.store(output, RITUAL_QUEUE_TAG, LONG_ARRAY_CODEC, ritualQueue.stream().mapToLong(BlockPos::asLong).toArray());
        if (ritualSourceBiome != null) {
            output.putString(RITUAL_BIOME_TAG, ritualSourceBiome.identifier().toString());
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void serverTick(Level level, BlockPos pos) {
        pixlepix.auracascade.item.ConsumerItemKeepAlive.tick(level, pos);
        int previousPower = storedPower;
        int previousLastPower = lastPower;
        int previousProgress = progress;
        int previousCharge = minerCharge;
        boolean previousPulse = minerPulseLatched;
        WorkResult previousResult = lastResult;
        boolean ritualAdvanced = false;

        if (level.getGameTime() % 20L == 18L) {
            storedPower = AuraConsumerLogic.bleedStoredPower(storedPower);
        }
        lastPower = collectAdjacentPower(level, pos);
        storedPower += lastPower;

        if (!ritualQueue.isEmpty()) {
            ritualAdvanced = tickRitual((ServerLevel) level, pos);
            if (ritualQueue.isEmpty() && lastResult == WorkResult.RITUAL_COMPLETE) {
                return;
            }
        }

        if (variant() == LateGameVariant.MINER) {
            tickMiner((ServerLevel) level, pos);
        } else if (storedPower >= variant().powerPerProgress()) {
            int steps = LateGameWorldLogic.progressStepsForTick(storedPower, variant().powerPerProgress());
            for (int step = 0; step < steps; step++) {
                int cost = LateGameWorldLogic.powerCostForStep(variant().powerPerProgress(), step);
                if (cost <= 0 || storedPower < cost) {
                    break;
                }
                storedPower -= cost;
                progress++;
                if (progress > variant().maxProgress()) {
                    progress = performWork((ServerLevel) level, pos) ? 0 : variant().maxProgress();
                    if (progress != 0) {
                        break;
                    }
                }
            }
        }

        if (storedPower != previousPower || lastPower != previousLastPower
            || progress != previousProgress || minerCharge != previousCharge
            || minerPulseLatched != previousPulse || lastResult != previousResult || ritualAdvanced) {
            setChanged();
            if (!ritualAdvanced || level.getGameTime() % 10L == 0L || lastResult != previousResult) {
                level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
            }
        }
    }

    private void tickMiner(ServerLevel level, BlockPos pos) {
        minerPulseLatched |= level.hasNeighborSignal(pos);
        MinerExplosionEntity entity = minerEntity(level, pos);
        if (minerEntityId != null && entity == null) {
            if (level.getGameTime() <= lastChargedTick + 100L) {
                lastResult = WorkResult.BLOCKED;
                return;
            }
            minerEntityId = null;
            minerCharge = 0;
            lastResult = WorkResult.MINER_EXPIRED;
        }
        if (entity == null && minerCharge > 0) {
            entity = spawnMinerEntity(level, pos, true);
        }
        if (storedPower >= variant().powerPerProgress()) {
            int cost = variant().powerPerProgress();
            storedPower -= cost;
            progress++;
            if (progress > variant().maxProgress()) {
                if (minerPulseLatched) {
                    if (entity != null) {
                        int charge = entity.charge();
                        entity.disarm();
                        releaseMinerYield(level, pos, charge);
                    }
                    lastResult = WorkResult.MINER_RELEASED;
                } else {
                    if (entity == null) {
                        entity = spawnMinerEntity(level, pos, false);
                    } else {
                        entity.addCharge();
                    }
                    if (entity != null) {
                        minerCharge = entity.charge();
                        lastChargedTick = level.getGameTime();
                        lastResult = WorkResult.MINER_CHARGING;
                    } else {
                        lastResult = WorkResult.BLOCKED;
                    }
                }
                minerPulseLatched = false;
                progress = 0;
            }
        }
    }

    private MinerExplosionEntity minerEntity(ServerLevel level, BlockPos pos) {
        if (minerEntityId == null) {
            return null;
        }
        return level.getEntity(minerEntityId) instanceof MinerExplosionEntity entity
            && !entity.isRemoved() && pos.equals(entity.sourcePos()) ? entity : null;
    }

    private MinerExplosionEntity spawnMinerEntity(ServerLevel level, BlockPos pos, boolean restoring) {
        MinerExplosionEntity entity = new MinerExplosionEntity(MinerExplosionEntities.type(), level);
        entity.setPos(pos.getX() + 0.5D, pos.getY() - 1.5D, pos.getZ() + 0.5D);
        if (restoring) {
            entity.restore(pos, minerCharge, lastChargedTick, lastExplosionTick);
        } else {
            entity.start(pos);
        }
        if (!level.addFreshEntity(entity)) {
            lastResult = WorkResult.BLOCKED;
            return null;
        }
        minerEntityId = entity.getUUID();
        minerCharge = entity.charge();
        lastChargedTick = level.getGameTime();
        return entity;
    }

    void onMinerExplosionRemoved(MinerExplosionEntity entity, WorkResult result) {
        if (!entity.getUUID().equals(minerEntityId)) {
            return;
        }
        minerEntityId = null;
        minerCharge = 0;
        lastResult = result;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    boolean ownsMinerExplosion(MinerExplosionEntity entity) {
        return entity.getUUID().equals(minerEntityId);
    }

    private boolean performWork(ServerLevel level, BlockPos pos) {
        return switch (variant()) {
            case LOOTER -> spawnLoot(level, pos);
            case SPAWNER -> spawnMob(level, pos);
            case RITUAL_NETHER, RITUAL_END -> performRitual(level, pos);
            case MINER -> false;
        };
    }

    private boolean spawnLoot(ServerLevel level, BlockPos pos) {
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
            .create(LootContextParamSets.CHEST);
        List<ItemStack> generated = level.getServer().reloadableRegistries()
            .getLootTable(BuiltInLootTables.SIMPLE_DUNGEON).getRandomItems(params, level.getRandom());
        ItemStack chosen = LateGameWorldLogic.chooseGeneratedLoot(generated, level.getRandom());
        if (chosen.isEmpty()) {
            lastResult = WorkResult.BLOCKED;
            return false;
        }
        ItemEntity itemEntity = new ItemEntity(
            level,
            pos.getX() + 0.5D,
            pos.getY() + 1.5D,
            pos.getZ() + 0.5D,
            chosen
        );
        itemEntity.setDeltaMovement(Vec3.ZERO);
        boolean spawned = level.addFreshEntity(itemEntity);
        lastResult = spawned ? WorkResult.LOOTED : WorkResult.BLOCKED;
        return spawned;
    }

    private boolean spawnMob(ServerLevel level, BlockPos pos) {
        var naturalSpawns = level.getChunkSource().getGenerator().getMobsAt(
            level.getBiome(pos), level.structureManager(), MobCategory.MONSTER, pos
        );
        var type = LateGameWorldLogic.chooseSpawnType(naturalSpawns, level.getRandom());
        if (type == null || !(type.create(level, EntitySpawnReason.TRIGGERED) instanceof Mob mob)) {
            lastResult = WorkResult.BLOCKED;
            return false;
        }
        mob.teleportTo(pos.getX() + 0.5D, pos.getY() + 2.0D, pos.getZ() + 0.5D);
        boolean spawned = level.addFreshEntity(mob);
        lastResult = spawned ? WorkResult.SPAWNED : WorkResult.BLOCKED;
        return spawned;
    }

    private boolean performRitual(ServerLevel level, BlockPos pos) {
        if (!ritualQueue.isEmpty()) {
            return true;
        }
        ResourceKey<Biome> target = targetBiome();
        var source = cellBiome(level, pos).unwrapKey();
        if (source.isEmpty()) {
            lastResult = WorkResult.BLOCKED;
            return false;
        }
        if (source.get().equals(target)) {
            lastResult = WorkResult.NONE;
            return true;
        }
        ritualSourceBiome = source.get();
        ritualQueue.add(LateGameWorldLogic.ritualCellOrigin(pos));
        lastResult = WorkResult.RITUAL_RUNNING;
        return true;
    }

    private boolean tickRitual(ServerLevel level, BlockPos origin) {
        if (ritualSourceBiome == null) {
            ritualQueue.clear();
            lastResult = WorkResult.BLOCKED;
            return true;
        }
        Holder<Biome> target = level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(targetBiome());
        Set<LevelChunk> changedChunks = new HashSet<>();
        int processed = 0;
        while (processed < 2 && !ritualQueue.isEmpty()) {
            BlockPos cell = ritualQueue.removeFirst();
            processed++;
            if (!LateGameWorldLogic.ritualCellIntersectsRadius(cell, origin)
                || !cellBiome(level, cell).is(ritualSourceBiome)) {
                continue;
            }
            LevelChunk chunk = level.getChunkAt(cell);
            setCellBiome(chunk, cell, target);
            changedChunks.add(chunk);
            for (int x = cell.getX(); x < cell.getX() + 4; x++) {
                for (int z = cell.getZ(); z < cell.getZ() + 4; z++) {
                    if (!LateGameWorldLogic.withinRitualRadius(x - origin.getX(), z - origin.getZ())) {
                        continue;
                    }
                    for (int y = level.getMinY(); y <= level.getMaxY(); y++) {
                        BlockPos blockPos = new BlockPos(x, y, z);
                        BlockState state = level.getBlockState(blockPos);
                        var mapped = LateGameWorldLogic.ritualMapping(variant(), state.getBlock(), level.getRandom());
                        if (mapped != null) {
                            level.setBlock(blockPos, mapped.defaultBlockState(), 2);
                        }
                    }
                }
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos next = cell.relative(direction, 4);
                if (LateGameWorldLogic.ritualCellIntersectsRadius(next, origin)
                    && cellBiome(level, next).is(ritualSourceBiome) && !ritualQueue.contains(next)) {
                    ritualQueue.addLast(next.immutable());
                }
            }
        }
        if (!changedChunks.isEmpty()) {
            List<ChunkAccess> changed = changedChunks.stream().map(chunk -> (ChunkAccess) chunk).toList();
            level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
        }
        if (ritualQueue.isEmpty()) {
            ritualSourceBiome = null;
            lastResult = WorkResult.RITUAL_COMPLETE;
            level.destroyBlock(origin, false);
        }
        return processed > 0;
    }

    private static Holder<Biome> cellBiome(ServerLevel level, BlockPos pos) {
        return level.getChunkAt(pos).getNoiseBiome(
            QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ())
        );
    }

    @SuppressWarnings("unchecked")
    private static void setCellBiome(LevelChunk chunk, BlockPos cell, Holder<Biome> target) {
        int x = (cell.getX() & 15) >> 2;
        int z = (cell.getZ() & 15) >> 2;
        for (int index = 0; index < chunk.getSections().length; index++) {
            LevelChunkSection section = chunk.getSections()[index];
            if (!(section.getBiomes() instanceof PalettedContainer<?> biomes)) {
                throw new IllegalStateException("Cannot mutate chunk biome palette for ritual");
            }
            PalettedContainer<Holder<Biome>> palette = (PalettedContainer<Holder<Biome>>) biomes;
            for (int y = 0; y < 4; y++) {
                palette.set(x, y, z, target);
            }
        }
        chunk.markUnsaved();
    }

    private ResourceKey<Biome> targetBiome() {
        return variant() == LateGameVariant.RITUAL_NETHER ? Biomes.NETHER_WASTES : Biomes.THE_END;
    }

    private void releaseMinerYield(ServerLevel level, BlockPos pos, int charge) {
        int oreCount = LateGameWorldLogic.minerOreYield(charge);
        List<net.minecraft.world.item.Item> taggedOres = LateGameWorldLogic.taggedOres();
        for (int index = 0; index < oreCount; index++) {
            ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 1.5D,
                pos.getZ() + 0.5D,
                LateGameWorldLogic.chooseOre(level.getRandom(), taggedOres)
            );
            level.addFreshEntity(itemEntity);
        }
    }

    private int collectAdjacentPower(Level level, BlockPos pos) {
        int collected = 0;
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                collected += auraNetworkBlockEntity.extractStoredPower(Integer.MAX_VALUE);
            }
        }
        return collected;
    }

    private LateGameVariant variant() {
        return AuraContent.lateGameVariant(getBlockState().getBlock());
    }

    public record InspectionSnapshot(
        int progress,
        int maxProgress,
        int storedPower,
        int minerCharge,
        int lastPower,
        WorkResult lastResult,
        int ritualCellsRemaining
    ) {
    }

    public enum WorkResult {
        NONE,
        BLOCKED,
        LOOTED,
        SPAWNED,
        MINER_CHARGING,
        MINER_RELEASED,
        MINER_EXPIRED,
        MINER_EXPLODED,
        RITUAL_RUNNING,
        RITUAL_COMPLETE
    }
}
