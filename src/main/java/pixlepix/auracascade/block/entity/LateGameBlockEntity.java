package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.FortifiedBlock;
import pixlepix.auracascade.block.LateGameVariant;

public class LateGameBlockEntity extends BlockEntity implements AuraSignalSource {
    private static final String STORED_POWER_TAG = "stored_power";
    private static final String PROGRESS_TAG = "progress";
    private static final String MINER_CHARGE_TAG = "miner_charge";
    private static final String PULSE_LATCH_TAG = "pulse_latch";

    private int storedPower;
    private int progress;
    private int minerCharge;
    private boolean minerPulseLatched;

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

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        storedPower = input.getIntOr(STORED_POWER_TAG, 0);
        progress = input.getIntOr(PROGRESS_TAG, 0);
        minerCharge = input.getIntOr(MINER_CHARGE_TAG, 0);
        minerPulseLatched = input.getBooleanOr(PULSE_LATCH_TAG, false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt(STORED_POWER_TAG, storedPower);
        output.putInt(PROGRESS_TAG, progress);
        output.putInt(MINER_CHARGE_TAG, minerCharge);
        output.putBoolean(PULSE_LATCH_TAG, minerPulseLatched);
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
        int previousPower = storedPower;
        int previousProgress = progress;
        int previousCharge = minerCharge;

        if (level.getGameTime() % 20L == 18L) {
            storedPower = AuraConsumerLogic.bleedStoredPower(storedPower);
        }
        storedPower += collectAdjacentPower(level, pos);

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

        if (storedPower != previousPower || progress != previousProgress || minerCharge != previousCharge) {
            setChanged();
            level.sendBlockUpdated(pos, getBlockState(), getBlockState(), 3);
        }
    }

    private void tickMiner(ServerLevel level, BlockPos pos) {
        if (storedPower >= variant().powerPerProgress()) {
            int cost = variant().powerPerProgress();
            storedPower -= cost;
            progress++;
            if (progress > variant().maxProgress()) {
                minerCharge++;
                progress = 0;
            }
        }

        if (minerCharge > 0 && level.getGameTime() % 10L == 0L) {
            level.sendParticles(ParticleTypes.PORTAL, pos.getX() + 0.5D, pos.getY() - 0.25D, pos.getZ() + 0.5D, 6, 0.25D, 0.15D, 0.25D, 0.0D);
            level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, 4, 0.2D, 0.05D, 0.2D, 0.0D);
        }

        if (minerCharge > 0 && level.getGameTime() % 20L == 0L && !stressContainment(level, pos)) {
            level.explode(null, pos.getX() + 0.5D, pos.getY() - 0.5D, pos.getZ() + 0.5D, Math.min(18.0F, 4.0F + minerCharge / 2.0F), Level.ExplosionInteraction.BLOCK);
            level.destroyBlock(pos, false);
            minerCharge = 0;
            progress = 0;
            return;
        }

        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !minerPulseLatched && minerCharge > 0) {
            releaseMinerYield(level, pos);
        }
        minerPulseLatched = powered;
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
        ItemEntity itemEntity = new ItemEntity(
            level,
            pos.getX() + 0.5D,
            pos.getY() + 1.2D,
            pos.getZ() + 0.5D,
            LateGameWorldLogic.chooseLoot(level.getRandom())
        );
        itemEntity.setUnlimitedLifetime();
        return level.addFreshEntity(itemEntity);
    }

    private boolean spawnMob(ServerLevel level, BlockPos pos) {
        if (level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(8.0D), Mob::isAlive).size() >= 6) {
            return false;
        }
        BlockPos spawnPos = pos.above();
        if (!level.getBlockState(spawnPos).isAir() || !level.getBlockState(spawnPos.above()).isAir()) {
            return false;
        }

        var type = LateGameWorldLogic.chooseSpawnType(level.dimension(), level.getRandom());
        Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (mob == null) {
            return false;
        }
        mob.teleportTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
        return level.addFreshEntity(mob);
    }

    private boolean performRitual(ServerLevel level, BlockPos pos) {
        boolean changed = false;
        RandomSource random = level.getRandom();
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos targetPos = pos.offset(random.nextInt(9) - 4, random.nextInt(5) - 2, random.nextInt(9) - 4);
            BlockState targetState = level.getBlockState(targetPos);
            var mappedBlock = LateGameWorldLogic.ritualMapping(variant(), targetState.getBlock(), random);
            if (mappedBlock == null) {
                continue;
            }
            level.setBlock(targetPos, mappedBlock.defaultBlockState(), 3);
            changed = true;
        }

        LateGameWorldLogic.RitualDanger danger = LateGameWorldLogic.ritualDanger(variant());
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(4.5D), LivingEntity::isAlive)) {
            if (danger.damage() > 0.0F) {
                entity.hurt(level.damageSources().magic(), danger.damage());
            }
            if (danger.fireSeconds() > 0) {
                entity.igniteForSeconds(danger.fireSeconds());
            }
            if (variant() == LateGameVariant.RITUAL_END) {
                entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 0), entity);
                entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 80, 0), entity);
            }
        }

        if (danger.blazeBurst() && level.getRandom().nextInt(5) == 0) {
            Mob blaze = net.minecraft.world.entity.EntityType.BLAZE.create(level, EntitySpawnReason.EVENT);
            if (blaze != null) {
                blaze.teleportTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
                level.addFreshEntity(blaze);
            }
        } else if (variant() == LateGameVariant.RITUAL_END && level.getRandom().nextInt(5) == 0) {
            Mob endermite = net.minecraft.world.entity.EntityType.ENDERMITE.create(level, EntitySpawnReason.EVENT);
            if (endermite != null) {
                endermite.teleportTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
                level.addFreshEntity(endermite);
            }
        }

        return changed;
    }

    private boolean stressContainment(ServerLevel level, BlockPos pos) {
        ArrayList<BlockPos> containedBlocks = new ArrayList<>();
        BlockPos center = pos.below();
        for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof pixlepix.auracascade.block.FortifiedBlock) {
                containedBlocks.add(candidate.immutable());
            }
        }

        if (LateGameWorldLogic.minerContainmentFails(containedBlocks.size())) {
            return false;
        }

        containedBlocks.sort(Comparator.comparingDouble(candidate -> candidate.distSqr(center)));
        for (BlockPos containedBlock : containedBlocks) {
            if (FortifiedBlock.stress(level.getBlockState(containedBlock), level, containedBlock, level.getRandom())) {
                break;
            }
        }
        return true;
    }

    private void releaseMinerYield(ServerLevel level, BlockPos pos) {
        int oreCount = LateGameWorldLogic.minerOreYield(minerCharge);
        for (int index = 0; index < oreCount; index++) {
            ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5D,
                pos.getY() + 1.2D,
                pos.getZ() + 0.5D,
                LateGameWorldLogic.chooseOre(level.getRandom())
            );
            itemEntity.setDeltaMovement(
                (level.getRandom().nextDouble() - 0.5D) * 0.1D,
                0.05D + level.getRandom().nextDouble() * 0.1D,
                (level.getRandom().nextDouble() - 0.5D) * 0.1D
            );
            level.addFreshEntity(itemEntity);
        }
        minerCharge = 0;
        progress = 0;
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
}
