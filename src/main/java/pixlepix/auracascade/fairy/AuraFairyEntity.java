package pixlepix.auracascade.fairy;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.item.RingOfBindingItem;
import pixlepix.auracascade.util.NbtCompat;

public final class AuraFairyEntity extends Entity {
    private static final EntityDataAccessor<String> OWNER_ID = SynchedEntityData.defineId(
        AuraFairyEntity.class,
        EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Integer> SLOT = SynchedEntityData.defineId(
        AuraFairyEntity.class,
        EntityDataSerializers.INT
    );
    private static final EntityDataAccessor<String> ROLE = SynchedEntityData.defineId(
        AuraFairyEntity.class,
        EntityDataSerializers.STRING
    );
    private UUID trackedArrowId;

    public AuraFairyEntity(EntityType<? extends AuraFairyEntity> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_ID, "");
        builder.define(SLOT, -1);
        builder.define(ROLE, FairyRole.defaultRole().id());
    }

    public void setFairyData(UUID ownerId, int slot, FairyRole role) {
        if (role != FairyRole.SHOOTER) {
            trackedArrowId = null;
        }
        entityData.set(OWNER_ID, ownerId.toString());
        entityData.set(SLOT, slot);
        entityData.set(ROLE, role.id());
    }

    public UUID ownerId() {
        String value = entityData.get(OWNER_ID);
        try {
            return value.isEmpty() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public int slot() {
        return entityData.get(SLOT);
    }

    public FairyRole role() {
        return FairyRole.byId(entityData.get(ROLE));
    }

    UUID trackedArrowId() {
        return trackedArrowId;
    }

    void setTrackedArrowId(UUID trackedArrowId) {
        this.trackedArrowId = trackedArrowId;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        new SavedState(ownerId(), slot(), role()).write(output);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        SavedState saved = SavedState.read(input);
        trackedArrowId = null;
        entityData.set(OWNER_ID, saved.ownerId() == null ? "" : saved.ownerId().toString());
        entityData.set(SLOT, saved.slot());
        entityData.set(ROLE, saved.role().id());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            discard();
            return;
        }

        UUID ownerUuid = ownerId();
        ServerPlayer owner = ownerUuid == null ? null : serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
        if (!canRemainLoaded(ownerUuid, slot(), owner == null ? null : owner.getUUID(), owner != null && owner.isAlive(),
            owner != null && owner.level() == level(), owner == null ? Double.POSITIVE_INFINITY : distanceToSqr(owner))
            || !FairySystem.isRoleEquipped(this, owner)) {
            FairySystem.forgetFairy(this);
            discard();
            return;
        }

        setPos(FairySystem.orbitPosition(serverLevel.getGameTime(), slot(), owner));
        setDeltaMovement(Vec3.ZERO);
        FairySystem.tickFairy(this, owner);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    static boolean canRemainLoaded(
        UUID savedOwner,
        int slot,
        UUID onlineOwner,
        boolean ownerAlive,
        boolean sameLevel,
        double distanceSquared
    ) {
        return savedOwner != null
            && savedOwner.equals(onlineOwner)
            && slot >= 0
            && slot < RingOfBindingItem.MAX_BOUND_FAIRIES
            && ownerAlive
            && sameLevel
            && distanceSquared <= 256.0D;
    }

    static record SavedState(UUID ownerId, int slot, FairyRole role) {
        CompoundTag write() {
            CompoundTag tag = new CompoundTag();
            if (ownerId != null) {
                NbtCompat.store(tag, "Owner", UUIDUtil.CODEC, ownerId);
            }
            tag.putInt("Slot", slot);
            tag.putString("Role", role.id());
            return tag;
        }

        void write(ValueOutput output) {
            if (ownerId != null) {
                NbtCompat.store(output, "Owner", UUIDUtil.CODEC, ownerId);
            }
            output.putInt("Slot", slot);
            output.putString("Role", role.id());
        }

        static SavedState read(CompoundTag tag) {
            UUID owner = NbtCompat.read(tag, "Owner", UUIDUtil.CODEC).orElse(null);
            return readFields(owner, NbtCompat.getIntOr(tag, "Slot", -1),
                NbtCompat.getStringOr(tag, "Role", FairyRole.defaultRole().id()));
        }

        static SavedState read(ValueInput input) {
            UUID owner = NbtCompat.read(input, "Owner", UUIDUtil.CODEC).orElse(null);
            return readFields(owner, NbtCompat.getIntOr(input, "Slot", -1),
                NbtCompat.getStringOr(input, "Role", FairyRole.defaultRole().id()));
        }

        private static SavedState readFields(UUID owner, int slot, String role) {
            return new SavedState(
                owner,
                slot,
                FairyRole.byId(role)
            );
        }
    }
}
