package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.FortifiedBlock;

public final class MinerExplosionEntity extends Entity {
    private static final String CHARGE_TAG = "charge";
    private static final String LAST_CHARGED_TAG = "last_charged";
    private static final String LAST_EXPLOSION_TAG = "last_explosion";
    private static final String SOURCE_POS_TAG = "source_pos";

    private int charge;
    private long lastCharged;
    private long lastExplosion;
    private BlockPos sourcePos;

    public MinerExplosionEntity(EntityType<? extends MinerExplosionEntity> type, Level level) {
        super(type, level);
    }

    public void start(BlockPos minerPos) {
        sourcePos = minerPos.immutable();
        charge = 1;
        lastCharged = level().getGameTime();
        bounce();
    }

    public void restore(BlockPos minerPos, int restoredCharge, long chargedAt, long explodedAt) {
        sourcePos = minerPos.immutable();
        charge = restoredCharge;
        lastCharged = chargedAt > 0L ? chargedAt : level().getGameTime();
        lastExplosion = explodedAt;
        bounce();
    }

    public int charge() {
        return charge;
    }

    public BlockPos sourcePos() {
        return sourcePos;
    }

    public void addCharge() {
        charge++;
        lastCharged = level().getGameTime();
    }

    public void disarm() {
        notifyMiner(LateGameBlockEntity.WorkResult.MINER_RELEASED);
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount % 2 == 0) {
                level().addParticle(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
            }
            return;
        }

        ServerLevel server = (ServerLevel) level();
        if (sourcePos != null && server.hasChunkAt(sourcePos)
            && server.getBlockEntity(sourcePos) instanceof LateGameBlockEntity miner
            && !miner.ownsMinerExplosion(this)) {
            discard();
            return;
        }
        if (server.getGameTime() > lastCharged + 100L) {
            notifyMiner(LateGameBlockEntity.WorkResult.MINER_EXPIRED);
            discard();
            return;
        }
        move(MoverType.SELF, getDeltaMovement());
        if (horizontalCollision || verticalCollision) {
            explodeOnCollision(server);
            if (!isRemoved()) {
                bounce();
            }
        }
    }

    private void bounce() {
        double amplitude = LateGameWorldLogic.minerBounceAmplitude(charge);
        var random = level().getRandom();
        setDeltaMovement(
            (random.nextDouble() - 0.5D) * amplitude,
            (random.nextDouble() - 0.5D) * amplitude,
            (random.nextDouble() - 0.5D) * amplitude
        );
        hasImpulse = true;
    }

    private void explodeOnCollision(ServerLevel level) {
        long now = level.getGameTime();
        if (now <= lastExplosion + LateGameWorldLogic.minerExplosionCooldown(charge)) {
            return;
        }
        lastExplosion = now;
        BlockPos center = blockPosition();
        int contained = 0;
        for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-2, -2, -2), center.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof FortifiedBlock) {
                contained++;
                FortifiedBlock.stress(state, level, candidate, level.getRandom());
            }
        }
        if (LateGameWorldLogic.minerContainmentFails(contained)) {
            notifyMiner(LateGameBlockEntity.WorkResult.MINER_EXPLODED);
            level.explode(this, getX(), getY(), getZ(), 50.0F, true, Level.ExplosionInteraction.BLOCK);
            discard();
        }
    }

    private void notifyMiner(LateGameBlockEntity.WorkResult result) {
        if (sourcePos != null && level() instanceof ServerLevel server && server.hasChunkAt(sourcePos)
            && server.getBlockEntity(sourcePos) instanceof LateGameBlockEntity miner) {
            miner.onMinerExplosionRemoved(this, result);
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        charge = tag.getInt(CHARGE_TAG);
        lastCharged = tag.getLong(LAST_CHARGED_TAG);
        lastExplosion = tag.getLong(LAST_EXPLOSION_TAG);
        sourcePos = tag.contains(SOURCE_POS_TAG) ? BlockPos.of(tag.getLong(SOURCE_POS_TAG)) : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt(CHARGE_TAG, charge);
        tag.putLong(LAST_CHARGED_TAG, lastCharged);
        tag.putLong(LAST_EXPLOSION_TAG, lastExplosion);
        if (sourcePos != null) {
            tag.putLong(SOURCE_POS_TAG, sourcePos.asLong());
        }
    }
}
