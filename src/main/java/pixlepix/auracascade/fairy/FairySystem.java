package pixlepix.auracascade.fairy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import pixlepix.auracascade.compat.AuraAccessoryBridgeRegistry;
import pixlepix.auracascade.compat.AuraAccessorySlot;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.RingOfBindingItem;

public final class FairySystem {
    private static final String LEGACY_FAIRY_TAG = "aura_fairy";
    private static final String LEGACY_OWNER_PREFIX = "aura_fairy_owner:";
    private static final double RECONCILE_RADIUS = 32.0D;
    private static final int RECONCILE_INTERVAL = 10;
    private static final Map<FairySlot, UUID> CANONICAL_FAIRIES = new HashMap<>();
    private static final Map<UUID, PlayerLocation> LAST_PLAYER_LOCATIONS = new HashMap<>();
    private static final Map<UUID, List<FairyRole>> LAST_ROLE_LISTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_RECONCILE_TICKS = new HashMap<>();
    private static final List<Holder<MobEffect>> DEBUFF_EFFECTS = List.of(
        MobEffects.POISON,
        MobEffects.CONFUSION,
        MobEffects.WEAKNESS,
        MobEffects.WITHER,
        MobEffects.MOVEMENT_SLOWDOWN,
        MobEffects.HUNGER
    );
    private static final List<Holder<MobEffect>> BUFF_EFFECTS = List.of(
        MobEffects.REGENERATION,
        MobEffects.DAMAGE_RESISTANCE,
        MobEffects.DAMAGE_BOOST,
        MobEffects.ABSORPTION,
        MobEffects.JUMP,
        MobEffects.MOVEMENT_SPEED
    );
    private static boolean bootstrapped;

    private FairySystem() {
    }

    public static void bootstrapCommon() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        NeoForge.EVENT_BUS.addListener(FairySystem::onEntityJoinLevel);
        NeoForge.EVENT_BUS.addListener(FairySystem::onEntityLeaveLevel);
        NeoForge.EVENT_BUS.addListener(FairySystem::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(FairySystem::onPlayerChangedDimension);
        NeoForge.EVENT_BUS.addListener(FairySystem::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(FairySystem::onPlayerRespawn);
        NeoForge.EVENT_BUS.addListener(FairySystem::onServerStopped);
    }

    private static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide() && entity instanceof AuraFairyEntity fairy && isSuperseded(fairy)) {
            event.setCanceled(true);
            return;
        }
        if (!event.getLevel().isClientSide() && entity instanceof Allay
            && provenLegacyOwner(true, entity.getTags()).isPresent()) {
            event.setCanceled(true);
        }
    }

    private static void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof AuraFairyEntity fairy) {
            forgetFairy(fairy);
        }
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerLocation last = LAST_PLAYER_LOCATIONS.remove(player.getUUID());
        forgetOwnerState(player.getUUID());
        clearNearby(last == null ? player.serverLevel() : last.level(), player.getUUID(),
            last == null ? player.getBoundingBox() : last.bounds());
    }

    private static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerLocation last = LAST_PLAYER_LOCATIONS.remove(player.getUUID());
        forgetOwnerState(player.getUUID());
        ServerLevel origin = player.getServer().getLevel(event.getFrom());
        if (origin == null && last != null) {
            origin = last.level();
        }
        if (origin != null) {
            clearNearby(origin, player.getUUID(), last != null && last.level() == origin
                ? last.bounds() : player.getBoundingBox());
        }
    }

    private static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer original) {
            LAST_PLAYER_LOCATIONS.put(original.getUUID(),
                new PlayerLocation(original.serverLevel(), original.getBoundingBox()));
        }
    }

    private static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        PlayerLocation last = LAST_PLAYER_LOCATIONS.remove(player.getUUID());
        forgetOwnerState(player.getUUID());
        clearNearby(last == null ? player.serverLevel() : last.level(), player.getUUID(),
            last == null ? player.getBoundingBox() : last.bounds());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        CANONICAL_FAIRIES.clear();
        LAST_PLAYER_LOCATIONS.clear();
        LAST_ROLE_LISTS.clear();
        LAST_RECONCILE_TICKS.clear();
    }

    public static void syncPlayerFairies(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID ownerId = player.getUUID();
        PlayerLocation previousLocation = LAST_PLAYER_LOCATIONS.put(ownerId, new PlayerLocation(level, player.getBoundingBox()));
        if (previousLocation != null && previousLocation.level() != level) {
            forgetOwnerState(ownerId);
        }
        List<FairyRole> roles = equippedRoles(player);
        List<FairyRole> previousRoles = LAST_ROLE_LISTS.put(ownerId, roles);
        boolean rolesChanged = previousRoles == null || !previousRoles.equals(roles);
        long gameTime = level.getGameTime();
        long lastReconcile = LAST_RECONCILE_TICKS.getOrDefault(ownerId, Long.MIN_VALUE);
        boolean reconcileDue = rolesChanged
            || lastReconcile == Long.MIN_VALUE
            || gameTime - lastReconcile >= RECONCILE_INTERVAL;

        if (!player.isAlive() || roles.isEmpty()) {
            if (rolesChanged || previousRoles == null || !player.isAlive()) {
                reconcilePlayerFairies(player, roles);
                LAST_RECONCILE_TICKS.put(ownerId, gameTime);
            }
            return;
        }
        if (reconcileDue) {
            reconcilePlayerFairies(player, roles);
            LAST_RECONCILE_TICKS.put(ownerId, gameTime);
        }
    }

    private static List<FairyRole> equippedRoles(Player player) {
        ItemStack ring = AuraAccessoryBridgeRegistry.equipped(player, AuraAccessorySlot.RING).stream()
            .filter(stack -> stack.is(AuraItems.RING_OF_BINDING))
            .findFirst()
            .orElse(ItemStack.EMPTY);
        return ring.isEmpty() ? List.of() : RingOfBindingItem.boundFairies(ring);
    }

    private static void reconcilePlayerFairies(ServerPlayer player, List<FairyRole> roles) {
        ServerLevel level = player.serverLevel();
        if (player.isAlive() && !roles.isEmpty() && !level.isPositionEntityTicking(player.blockPosition())) {
            return;
        }
        UUID ownerId = player.getUUID();
        List<AuraFairyEntity> existing = level.getEntitiesOfClass(
            AuraFairyEntity.class,
            player.getBoundingBox().inflate(RECONCILE_RADIUS),
            fairy -> ownerId.equals(fairy.ownerId())
        );
        if (existing.size() != roles.size() || existing.stream().anyMatch(fairy -> isSuperseded(fairy))) {
            existing = loadedOwnerFairies(level, ownerId);
        }
        existing.sort(Comparator.comparingInt(Entity::getId));
        forgetOwnerFairies(ownerId);

        if (!player.isAlive() || roles.isEmpty()) {
            existing.forEach(Entity::discard);
            return;
        }

        ArrayList<AuraFairyEntity> remaining = new ArrayList<>(existing);
        for (int slot = 0; slot < roles.size(); slot++) {
            FairyRole role = roles.get(slot);
            AuraFairyEntity fairy = takeSlot(remaining, slot);
            if (fairy == null) {
                fairy = spawnFairy(level, player, slot, role);
            }
            if (fairy != null) {
                fairy.setFairyData(ownerId, slot, role);
                fairy.setPos(orbitPosition(level, slot, player));
                CANONICAL_FAIRIES.put(new FairySlot(ownerId, slot), fairy.getUUID());
            }
        }
        remaining.forEach(Entity::discard);
    }

    private static AuraFairyEntity takeSlot(List<AuraFairyEntity> fairies, int slot) {
        for (int index = 0; index < fairies.size(); index++) {
            if (fairies.get(index).slot() == slot) {
                return fairies.remove(index);
            }
        }
        return null;
    }

    private static AuraFairyEntity spawnFairy(ServerLevel level, ServerPlayer owner, int slot, FairyRole role) {
        AuraFairyEntity fairy = AuraFairyEntityRegistry.entityType().create(level);
        if (fairy == null) {
            return null;
        }
        fairy.setFairyData(owner.getUUID(), slot, role);
        fairy.setPos(owner.getX(), owner.getY() + 1.4D, owner.getZ());
        if (!level.addFreshEntity(fairy)) {
            fairy.discard();
            return null;
        }
        return fairy;
    }

    static Vec3 orbitPosition(long gameTime, int slot, ServerPlayer owner) {
        double angle = gameTime * 0.14D + slot * 0.9D;
        double radius = 1.4D + (slot / 4) * 0.35D;
        double y = owner.getY() + 1.4D + Math.sin((gameTime + slot * 13L) * 0.12D) * 0.35D;
        return new Vec3(owner.getX() + Math.cos(angle) * radius, y, owner.getZ() + Math.sin(angle) * radius);
    }

    static Vec3 orbitPosition(ServerLevel level, int slot, ServerPlayer owner) {
        Vec3 desired = orbitPosition(level.getGameTime(), slot, owner);
        BlockPos target = BlockPos.containing(desired.x, desired.y, desired.z);
        return entityTickingOrbitPosition(desired, owner.position(), level.isPositionEntityTicking(target));
    }

    static Vec3 entityTickingOrbitPosition(Vec3 desired, Vec3 ownerPosition, boolean targetEntityTicking) {
        return targetEntityTicking ? desired : ownerPosition.add(0.0D, 1.4D, 0.0D);
    }

    static boolean isSuperseded(AuraFairyEntity fairy) {
        UUID ownerId = fairy.ownerId();
        return ownerId != null && isSuperseded(
            CANONICAL_FAIRIES.get(new FairySlot(ownerId, fairy.slot())), fairy.getUUID()
        );
    }

    static boolean isSuperseded(UUID canonicalId, UUID fairyId) {
        return canonicalId != null && !canonicalId.equals(fairyId);
    }

    private static List<AuraFairyEntity> loadedOwnerFairies(ServerLevel level, UUID ownerId) {
        ArrayList<AuraFairyEntity> fairies = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof AuraFairyEntity fairy && !fairy.isRemoved() && ownerId.equals(fairy.ownerId())) {
                fairies.add(fairy);
            }
        }
        return fairies;
    }

    private static void clearNearby(ServerLevel level, UUID ownerId, AABB area) {
        forgetOwnerFairies(ownerId);
        loadedOwnerFairies(level, ownerId).forEach(Entity::discard);
        removeLegacyFairiesNear(level, ownerId, area);
    }

    private static void forgetOwnerState(UUID ownerId) {
        LAST_ROLE_LISTS.remove(ownerId);
        LAST_RECONCILE_TICKS.remove(ownerId);
        forgetOwnerFairies(ownerId);
    }

    private static void forgetOwnerFairies(UUID ownerId) {
        CANONICAL_FAIRIES.keySet().removeIf(key -> key.ownerId().equals(ownerId));
    }

    static void forgetFairy(AuraFairyEntity fairy) {
        UUID ownerId = fairy.ownerId();
        if (ownerId != null) {
            CANONICAL_FAIRIES.remove(new FairySlot(ownerId, fairy.slot()), fairy.getUUID());
        }
    }

    private static void removeLegacyFairiesNear(ServerLevel level, UUID ownerId, AABB area) {
        level.getEntitiesOfClass(Allay.class, area.inflate(RECONCILE_RADIUS), allay ->
            provenLegacyOwner(true, allay.getTags()).filter(ownerId::equals).isPresent()
        ).forEach(Entity::discard);
    }

    static Optional<UUID> provenLegacyOwner(boolean allay, Set<String> tags) {
        if (!allay || !tags.contains(LEGACY_FAIRY_TAG)) {
            return Optional.empty();
        }
        for (String tag : tags) {
            if (!tag.startsWith(LEGACY_OWNER_PREFIX)) {
                continue;
            }
            try {
                return Optional.of(UUID.fromString(tag.substring(LEGACY_OWNER_PREFIX.length())));
            } catch (IllegalArgumentException ignored) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public static boolean isAuthorizedForRole(AuraFairyEntity fairy, ServerPlayer owner) {
        return owner != null && isAuthorizedForRole(fairy, owner, equippedRoles(owner));
    }

    private static boolean isAuthorizedForRole(AuraFairyEntity fairy, ServerPlayer owner, List<FairyRole> roles) {
        UUID fairyOwnerId = fairy.ownerId();
        UUID canonicalEntityId = fairyOwnerId == null
            ? null
            : CANONICAL_FAIRIES.get(new FairySlot(fairyOwnerId, fairy.slot()));
        return isAuthorizedForRole(
            fairyOwnerId,
            owner.getUUID(),
            fairy.getUUID(),
            canonicalEntityId,
            fairy.isAlive(),
            owner.isAlive(),
            fairy.level() == owner.level(),
            roles,
            fairy.slot(),
            fairy.role()
        );
    }

    static boolean isAuthorizedForRole(
        UUID fairyOwnerId,
        UUID ownerId,
        UUID fairyEntityId,
        UUID canonicalEntityId,
        boolean fairyAlive,
        boolean ownerAlive,
        boolean sameLevel,
        List<FairyRole> roles,
        int slot,
        FairyRole role
    ) {
        if (fairyOwnerId == null || !fairyOwnerId.equals(ownerId) || !fairyAlive || !ownerAlive || !sameLevel) {
            return false;
        }
        return FairyRoleLogic.isAuthorized(
            fairyEntityId != null && fairyEntityId.equals(canonicalEntityId), roles, slot, role
        );
    }

    static boolean isRoleEquipped(AuraFairyEntity fairy, ServerPlayer owner) {
        return owner != null
            && owner.getUUID().equals(fairy.ownerId())
            && isRoleEquipped(equippedRoles(owner), fairy.slot(), fairy.role());
    }

    static boolean isRoleEquipped(List<FairyRole> roles, int slot, FairyRole role) {
        return FairyRoleLogic.isAuthorized(true, roles, slot, role);
    }

    public static float applyDiggerFairies(Player player, float baseSpeed) {
        if (!hasBindingRing(player)) {
            return baseSpeed;
        }
        List<FairyRole> roles = equippedRoles(player);
        int count;
        if (player instanceof ServerPlayer serverPlayer) {
            count = countOwnedRoleFairies(serverPlayer, FairyRole.DIGGER, 20.0D);
        } else if (player.level().isClientSide) {
            count = countVisibleClientFairies(player, FairyRole.DIGGER, roles, 20.0D);
        } else {
            return baseSpeed;
        }
        return (float) (baseSpeed * FairyRoleLogic.diggerSpeedMultiplier(count));
    }

    public static float applyGliderFairies(ServerPlayer player, float fallDistance) {
        if (!hasBindingRing(player)) {
            return fallDistance;
        }
        int count = countOwnedRoleFairies(player, FairyRole.GLIDER, 20.0D);
        return (float) (fallDistance * FairyRoleLogic.gliderFallMultiplier(count));
    }

    public static boolean shouldDenyNaturalSpawn(ServerLevel level, BlockPos spawnPosition) {
        double x = spawnPosition.getX() + 0.5D;
        double y = spawnPosition.getY();
        double z = spawnPosition.getZ() + 0.5D;
        int scarerCount = 0;
        Set<FairySlot> counted = new HashSet<>();
        for (AuraFairyEntity fairy : level.getEntitiesOfClass(
            AuraFairyEntity.class,
            new AABB(spawnPosition).inflate(50.0D),
            candidate -> candidate.role() == FairyRole.SCARER
        )) {
            UUID ownerId = fairy.ownerId();
            ServerPlayer owner = ownerFor(level, ownerId);
            if (ownerId == null || owner == null) {
                continue;
            }
            FairySlot key = new FairySlot(ownerId, fairy.slot());
            if (fairy.distanceToSqr(x, y, z) < 2500.0D
                && counted.add(key)
                && isAuthorizedForRole(fairy, owner, equippedRoles(owner))) {
                scarerCount++;
            }
        }
        return scarerCount > 0 && FairyRoleLogic.deniesNaturalSpawn(scarerCount, level.getRandom().nextInt(25));
    }

    private static ServerPlayer ownerFor(ServerLevel level, UUID ownerId) {
        return ownerId == null ? null : level.getServer().getPlayerList().getPlayer(ownerId);
    }

    private static boolean hasBindingRing(Player player) {
        return AuraAccessoryBridgeRegistry.equipped(player, AuraAccessorySlot.RING).stream()
            .anyMatch(stack -> stack.is(AuraItems.RING_OF_BINDING));
    }

    private static int countOwnedRoleFairies(ServerPlayer owner, FairyRole role, double radius) {
        UUID ownerId = owner.getUUID();
        List<FairyRole> roles = equippedRoles(owner);
        int count = 0;
        for (AuraFairyEntity fairy : owner.serverLevel().getEntitiesOfClass(
            AuraFairyEntity.class,
            owner.getBoundingBox().inflate(radius),
            candidate -> ownerId.equals(candidate.ownerId()) && candidate.role() == role
        )) {
            if (isAuthorizedForRole(fairy, owner, roles)) {
                count++;
            }
        }
        return count;
    }

    private static int countVisibleClientFairies(Player owner, FairyRole role, List<FairyRole> roles, double radius) {
        UUID ownerId = owner.getUUID();
        Set<Integer> countedSlots = new HashSet<>();
        for (AuraFairyEntity fairy : owner.level().getEntitiesOfClass(
            AuraFairyEntity.class,
            owner.getBoundingBox().inflate(radius),
            candidate -> ownerId.equals(candidate.ownerId()) && candidate.role() == role
        )) {
            int slot = fairy.slot();
            if (slot >= 0 && slot < roles.size() && roles.get(slot) == role && fairy.isAlive()) {
                countedSlots.add(slot);
            }
        }
        return countedSlots.size();
    }

    static void tickFairy(AuraFairyEntity fairy, ServerPlayer owner) {
        if (!isAuthorizedForRole(fairy, owner)) {
            return;
        }
        ServerLevel level = (ServerLevel) fairy.level();
        FairyRole role = fairy.role();
        long gameTime = level.getGameTime();

        if (role == FairyRole.BAITER) {
            if (level.getRandom().nextInt(3600) == 0) {
                summonPassive(fairy);
            }
            return;
        }
        if (role == FairyRole.TRAINER) {
            if (level.getRandom().nextInt(1200) == 0) {
                ExperienceOrb.award(level, fairy.position(), 5);
            }
            return;
        }
        if (gameTime % role.tickInterval() != 0L) {
            return;
        }

        switch (role) {
            case BASIC, DIGGER, GLIDER, SCARER -> {
            }
            case FIGHTER -> attackNearestHostile(fairy, owner, 1.5F);
            case DEBUFFER -> debuffNearestHostile(fairy, owner);
            case BUFFER -> owner.addEffect(new MobEffectInstance(
                BUFF_EFFECTS.get(level.getRandom().nextInt(BUFF_EFFECTS.size())), 2400, 0
            ));
            case STEALER -> stealFromNearbyPlayer(fairy, owner);
            case PUSHER -> pushNearestHostile(fairy, owner);
            case SHOOTER -> empowerArrow(fairy, owner);
            case SAVIOR -> {
                if (owner.getHealth() < 5.0F) {
                    attackNearestHostile(fairy, owner, 10.0F);
                }
            }
            case FETCHER -> fetchItems(fairy, owner);
            case BAITER, TRAINER -> {
            }
            case BREEDER -> breedAnimals(fairy, owner);
            case EXTINGUISHER -> extinguish(fairy, owner);
            case LIGHTER -> placeFairyTorch(fairy);
        }
    }

    private static void attackNearestHostile(AuraFairyEntity fairy, ServerPlayer owner, float damage) {
        Mob hostile = findNearestHostile(fairy, 2.0D);
        if (hostile != null) {
            hostile.hurt(owner.damageSources().playerAttack(owner), damage);
        }
    }

    private static void debuffNearestHostile(AuraFairyEntity fairy, ServerPlayer owner) {
        Mob hostile = findNearestHostile(fairy, 2.0D);
        if (hostile == null) {
            return;
        }
        for (Holder<MobEffect> effect : DEBUFF_EFFECTS) {
            hostile.addEffect(new MobEffectInstance(effect, 200, 0), owner);
        }
    }

    private static void stealFromNearbyPlayer(AuraFairyEntity fairy, ServerPlayer owner) {
        for (Player target : ((ServerLevel) fairy.level()).getEntitiesOfClass(
            Player.class,
            fairy.getBoundingBox().inflate(2.0D),
            player -> player != owner && player.isAlive() && !player.getMainHandItem().isEmpty()
        )) {
            ItemEntity stolen = new ItemEntity(owner.serverLevel(), owner.getX(), owner.getY(), owner.getZ(), target.getMainHandItem().copy());
            stolen.setPickUpDelay(0);
            stolen.setDeltaMovement(Vec3.ZERO);
            if (owner.serverLevel().addFreshEntity(stolen)) {
                target.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }
        }
    }

    private static void pushNearestHostile(AuraFairyEntity fairy, ServerPlayer owner) {
        Mob hostile = findNearestHostile(fairy, 2.0D);
        if (hostile != null) {
            hostile.knockback(0.4D, owner.getX() - hostile.getX(), owner.getZ() - hostile.getZ());
        }
    }

    private static void empowerArrow(AuraFairyEntity fairy, ServerPlayer owner) {
        ServerLevel level = (ServerLevel) fairy.level();
        UUID trackedId = fairy.trackedArrowId();
        AbstractArrow arrow = trackedId == null ? null : level.getEntity(trackedId) instanceof AbstractArrow tracked ? tracked : null;
        if (arrow != null && !FairyRoleLogic.retainTrackedArrow(
            arrow.isAlive(), arrow.getDeltaMovement().x, arrow.getDeltaMovement().y)) {
            arrow = null;
            fairy.setTrackedArrowId(null);
        }
        if (trackedId == null || arrow == null) {
            List<AbstractArrow> nearby = level.getEntitiesOfClass(
                AbstractArrow.class,
                fairy.getBoundingBox().inflate(2.0D),
                Entity::isAlive
            );
            AbstractArrow firstArrow = nearby.isEmpty() ? null : nearby.getFirst();
            if (firstArrow == null || firstArrow.getOwner() != owner) {
                return;
            }
            arrow = firstArrow;
            fairy.setTrackedArrowId(arrow.getUUID());
        }
        if (arrow.getBaseDamage() < 10.0D) {
            arrow.setBaseDamage(arrow.getBaseDamage() + 10.0D);
            arrow.setCritArrow(true);
        }
    }

    private static void fetchItems(AuraFairyEntity fairy, ServerPlayer owner) {
        for (ItemEntity item : ((ServerLevel) fairy.level()).getEntitiesOfClass(
            ItemEntity.class,
            fairy.getBoundingBox().inflate(4.0D),
            candidate -> candidate.isAlive() && !candidate.getItem().isEmpty() && !candidate.hasPickUpDelay()
        )) {
            item.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        }
    }

    private static void summonPassive(AuraFairyEntity fairy) {
        List<EntityType<? extends Animal>> choices = List.of(
            EntityType.COW,
            EntityType.CHICKEN,
            EntityType.PIG,
            EntityType.SHEEP
        );
        ServerLevel level = (ServerLevel) fairy.level();
        Animal animal = choices.get(level.getRandom().nextInt(choices.size())).create(level);
        if (animal == null) {
            return;
        }
        animal.moveTo(fairy.getX(), fairy.getY(), fairy.getZ(), level.getRandom().nextFloat() * 360.0F, 0.0F);
        level.addFreshEntity(animal);
    }

    private static void breedAnimals(AuraFairyEntity fairy, ServerPlayer owner) {
        for (Animal animal : ((ServerLevel) fairy.level()).getEntitiesOfClass(
            Animal.class,
            fairy.getBoundingBox().inflate(1.0D),
            candidate -> candidate.isAlive() && candidate.canFallInLove()
        )) {
            animal.setInLove(owner);
            return;
        }
    }

    static boolean shouldRemoveExtinguisherBlock(BlockState state) {
        return state.is(Blocks.LAVA);
    }

    private static void extinguish(AuraFairyEntity fairy, ServerPlayer owner) {
        owner.clearFire();
        ServerLevel level = (ServerLevel) fairy.level();
        BlockPos pos = fairy.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (shouldRemoveExtinguisherBlock(state)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void placeFairyTorch(AuraFairyEntity fairy) {
        ServerLevel level = (ServerLevel) fairy.level();
        BlockPos pos = fairy.blockPosition();
        if (level.hasChunkAt(pos)
            && level.getMaxLocalRawBrightness(pos) < 10
            && level.getBlockState(pos).isAir()) {
            level.setBlock(pos, FairyTorchRegistry.block().defaultBlockState(), 3);
        }
    }

    private static Mob findNearestHostile(AuraFairyEntity fairy, double radius) {
        return ((ServerLevel) fairy.level()).getEntitiesOfClass(
            Mob.class,
            fairy.getBoundingBox().inflate(radius),
            entity -> entity instanceof Enemy && entity.isAlive()
        ).stream().min(Comparator.comparingDouble(fairy::distanceToSqr)).orElse(null);
    }

    private record PlayerLocation(ServerLevel level, AABB bounds) {
    }

    private record FairySlot(UUID ownerId, int slot) {
    }
}
