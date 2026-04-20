package pixlepix.auracascade.fairy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.compat.AuraAccessoryBridgeRegistry;
import pixlepix.auracascade.compat.AuraAccessorySlot;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.FairyCharmItem;
import pixlepix.auracascade.item.RingOfBindingItem;

public final class FairySystem {
    private static final String FAIRY_TAG = "aura_fairy";
    private static final String OWNER_TAG_PREFIX = "aura_fairy_owner:";
    private static final String SLOT_TAG_PREFIX = "aura_fairy_slot:";
    private static final String ROLE_TAG_PREFIX = "aura_fairy_role:";

    private FairySystem() {
    }

    public static void syncPlayerFairies(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }

        List<ItemStack> equippedRings = AuraAccessoryBridgeRegistry.equipped(player, AuraAccessorySlot.RING);
        ItemStack ring = equippedRings.stream().filter(stack -> stack.is(AuraItems.RING_OF_BINDING)).findFirst().orElse(ItemStack.EMPTY);
        List<FairyRole> boundRoles = ring.isEmpty() ? List.of() : RingOfBindingItem.boundFairies(ring);
        if (!player.isAlive() || boundRoles.isEmpty()) {
            clearPlayerFairies(server, player.getUUID());
            return;
        }

        ArrayList<Allay> existing = new ArrayList<>(allFairies(server, player.getUUID()));
        for (int slot = 0; slot < boundRoles.size(); slot++) {
            FairyRole role = boundRoles.get(slot);
            Allay fairy = takeMatching(existing, player, slot, role);
            if (fairy == null) {
                fairy = spawnFairy(player, slot, role);
                if (fairy == null) {
                    continue;
                }
            }

            configureFairy(fairy, player, slot, role);
            moveFairy(fairy, player, slot);
            tickRole((ServerLevel) player.level(), fairy, player, slot, role);
        }

        for (Allay staleFairy : existing) {
            staleFairy.discard();
        }
    }

    public static void clearPlayerFairies(MinecraftServer server, UUID ownerId) {
        for (Allay fairy : allFairies(server, ownerId)) {
            fairy.discard();
        }
    }

    private static List<Allay> allFairies(MinecraftServer server, UUID ownerId) {
        ArrayList<Allay> fairies = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            @SuppressWarnings("unchecked")
            List<Allay> levelFairies = (List<Allay>) level.getEntities(
                EntityTypeTest.forClass(Allay.class),
                entity -> entity.isAlive() && entity.getTags().contains(FAIRY_TAG) && hasOwner(entity, ownerId)
            );
            fairies.addAll(levelFairies);
        }
        return fairies;
    }

    private static Allay takeMatching(List<Allay> existing, ServerPlayer player, int slot, FairyRole role) {
        for (int index = 0; index < existing.size(); index++) {
            Allay fairy = existing.get(index);
            if (fairy.level() != player.level()) {
                continue;
            }
            if (slotOf(fairy) != slot) {
                continue;
            }
            if (roleOf(fairy) != role) {
                continue;
            }
            existing.remove(index);
            return fairy;
        }
        return null;
    }

    private static Allay spawnFairy(ServerPlayer player, int slot, FairyRole role) {
        ServerLevel level = (ServerLevel) player.level();
        Allay fairy = net.minecraft.world.entity.EntityType.ALLAY.create(level, EntitySpawnReason.EVENT);
        if (fairy == null) {
            return null;
        }

        configureFairy(fairy, player, slot, role);
        moveFairy(fairy, player, slot);
        level.addFreshEntity(fairy);
        return fairy;
    }

    private static void configureFairy(Allay fairy, ServerPlayer player, int slot, FairyRole role) {
        fairy.setNoAi(true);
        fairy.setNoGravity(true);
        fairy.setSilent(true);
        fairy.setInvulnerable(true);
        fairy.setGlowingTag(true);
        fairy.setCustomName(role.displayComponent());
        fairy.setCustomNameVisible(true);
        fairy.setItemInHand(InteractionHand.MAIN_HAND, FairyCharmItem.withRole(new ItemStack(AuraItems.FAIRY_CHARM), role));
        setTagValue(fairy, OWNER_TAG_PREFIX, player.getStringUUID());
        setTagValue(fairy, SLOT_TAG_PREFIX, Integer.toString(slot));
        setTagValue(fairy, ROLE_TAG_PREFIX, role.id());
        fairy.addTag(FAIRY_TAG);
    }

    private static void moveFairy(Allay fairy, ServerPlayer player, int slot) {
        ServerLevel level = (ServerLevel) player.level();
        double orbitAngle = (level.getGameTime() * 0.14D) + slot * 0.9D;
        double radius = 1.4D + (slot / 4) * 0.35D;
        double height = player.getY() + 1.4D + Math.sin((level.getGameTime() + slot * 13L) * 0.12D) * 0.35D;
        double x = player.getX() + Math.cos(orbitAngle) * radius;
        double z = player.getZ() + Math.sin(orbitAngle) * radius;
        fairy.teleportTo(x, height, z);
        fairy.setDeltaMovement(Vec3.ZERO);
        fairy.lookAt(player, 180.0F, 180.0F);
    }

    private static void tickRole(ServerLevel level, Allay fairy, ServerPlayer owner, int slot, FairyRole role) {
        long gameTime = level.getGameTime();
        FairyRoleLogic.Observation observation = new FairyRoleLogic.Observation(
            owner.getHealth() <= 6.0F,
            owner.isOnFire(),
            owner.fallDistance > 2.5F,
            findNearestHostile(owner, 6.0D) != null,
            !findNearbyItems(owner, 5.0D).isEmpty(),
            !findNearbyAnimals(owner, 4.0D).isEmpty(),
            findNearbyPlayerWithItem(owner, 4.0D) != null,
            level.getMaxLocalRawBrightness(BlockPos.containing(owner.position())) < 10,
            findTrackedArrow(owner, 5.0D) != null
        );

        if ((gameTime + slot) % role.tickInterval() != 0L) {
            if (role == FairyRole.GLIDER && FairyRoleLogic.primaryAction(role, observation) == FairyRoleLogic.Action.SLOW_FALL) {
                owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, true, false, false));
            }
            return;
        }

        switch (FairyRoleLogic.primaryAction(role, observation)) {
            case ATTACK_HOSTILE -> attackNearestHostile(owner, 2.0F);
            case DEBUFF_HOSTILE -> debuffNearestHostile(owner);
            case BUFF_OWNER -> owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, false));
            case STEAL_FROM_NEARBY_PLAYER -> stealFromNearbyPlayer(owner);
            case PUSH_HOSTILE -> pushNearestHostile(owner);
            case EMPOWER_PROJECTILE -> empowerArrow(owner);
            case CLUTCH_ATTACK -> saviorStrike(owner);
            case FETCH_ITEMS -> fetchItems(owner);
            case SUMMON_PASSIVE -> summonPassive(owner);
            case BREED_ANIMALS -> breedAnimals(owner);
            case SCARE_HOSTILE -> scareHostile(owner);
            case EXTINGUISH_FIRE -> extinguish(owner);
            case DIG_SOFT_BLOCK -> digSoftBlock(owner);
            case CREATE_LIGHT -> createLight(owner);
            case SLOW_FALL -> owner.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, true, false, false));
            case GRANT_EXPERIENCE -> owner.giveExperiencePoints(1);
            case NONE -> {
            }
        }

        level.sendParticles(ParticleTypes.END_ROD, fairy.getX(), fairy.getY(), fairy.getZ(), 1, 0.02D, 0.02D, 0.02D, 0.0D);
    }

    private static void attackNearestHostile(ServerPlayer owner, float damage) {
        Mob hostile = findNearestHostile(owner, 6.0D);
        if (hostile != null) {
            hostile.hurt(owner.damageSources().playerAttack(owner), damage);
        }
    }

    private static void debuffNearestHostile(ServerPlayer owner) {
        Mob hostile = findNearestHostile(owner, 6.0D);
        if (hostile == null) {
            return;
        }
        hostile.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 1), owner);
        hostile.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), owner);
    }

    private static void stealFromNearbyPlayer(ServerPlayer owner) {
        Player target = findNearbyPlayerWithItem(owner, 4.0D);
        if (target == null) {
            return;
        }

        ItemStack held = target.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }

        ItemStack stolen = held.copy();
        stolen.setCount(1);
        held.shrink(1);
        if (!owner.addItem(stolen)) {
            owner.drop(stolen, false);
        }
    }

    private static void pushNearestHostile(ServerPlayer owner) {
        Mob hostile = findNearestHostile(owner, 6.0D);
        if (hostile != null) {
            hostile.knockback(1.2D, owner.getX() - hostile.getX(), owner.getZ() - hostile.getZ());
        }
    }

    private static void empowerArrow(ServerPlayer owner) {
        AbstractArrow arrow = findTrackedArrow(owner, 5.0D);
        if (arrow == null) {
            return;
        }
        arrow.setBaseDamage(4.0D);
        arrow.setCritArrow(true);
    }

    private static void saviorStrike(ServerPlayer owner) {
        Mob hostile = findNearestHostile(owner, 6.0D);
        if (hostile == null) {
            return;
        }
        hostile.hurt(owner.damageSources().playerAttack(owner), 8.0F);
        owner.heal(2.0F);
    }

    private static void fetchItems(ServerPlayer owner) {
        for (ItemEntity itemEntity : findNearbyItems(owner, 5.0D)) {
            itemEntity.teleportTo(owner.getX(), owner.getY() + 0.5D, owner.getZ());
            itemEntity.setPickUpDelay(0);
        }
    }

    private static void summonPassive(ServerPlayer owner) {
        if (!findNearbyAnimals(owner, 12.0D).isEmpty()) {
            return;
        }

        List<net.minecraft.world.entity.EntityType<? extends Animal>> options = List.of(
            net.minecraft.world.entity.EntityType.COW,
            net.minecraft.world.entity.EntityType.CHICKEN,
            net.minecraft.world.entity.EntityType.PIG,
            net.minecraft.world.entity.EntityType.SHEEP
        );
        ServerLevel level = (ServerLevel) owner.level();
        net.minecraft.world.entity.EntityType<? extends Animal> type = options.get(level.getRandom().nextInt(options.size()));
        Animal animal = type.create(level, EntitySpawnReason.EVENT);
        if (animal == null) {
            return;
        }
        animal.teleportTo(owner.getX(), owner.getY(), owner.getZ());
        level.addFreshEntity(animal);
    }

    private static void breedAnimals(ServerPlayer owner) {
        for (Animal animal : findNearbyAnimals(owner, 4.0D)) {
            if (animal.canFallInLove()) {
                animal.setInLove(owner);
                return;
            }
        }
    }

    private static void scareHostile(ServerPlayer owner) {
        Mob hostile = findNearestHostile(owner, 8.0D);
        if (hostile == null) {
            return;
        }
        hostile.setTarget(null);
        hostile.knockback(1.5D, owner.getX() - hostile.getX(), owner.getZ() - hostile.getZ());
    }

    private static void extinguish(ServerPlayer owner) {
        ServerLevel level = (ServerLevel) owner.level();
        owner.clearFire();
        BlockPos center = BlockPos.containing(owner.position());
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 2, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static void digSoftBlock(ServerPlayer owner) {
        ServerLevel level = (ServerLevel) owner.level();
        BlockPos digPos = BlockPos.containing(owner.getEyePosition().add(owner.getLookAngle().scale(2.0D)));
        BlockState state = level.getBlockState(digPos);
        if (state.isAir() || level.getBlockEntity(digPos) != null) {
            return;
        }

        Block block = state.getBlock();
        if (block == Blocks.STONE
            || block == Blocks.COBBLESTONE
            || block == Blocks.DIRT
            || block == Blocks.GRASS_BLOCK
            || block == Blocks.SAND
            || block == Blocks.GRAVEL
            || block == Blocks.NETHERRACK
            || block == Blocks.END_STONE) {
            level.destroyBlock(digPos, true, owner, 64);
        }
    }

    private static void createLight(ServerPlayer owner) {
        ServerLevel level = (ServerLevel) owner.level();
        BlockPos lightPos = BlockPos.containing(owner.getX(), owner.getY() + 1.0D, owner.getZ());
        if (!level.getBlockState(lightPos).isAir()) {
            return;
        }
        level.setBlock(lightPos, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
    }

    private static Mob findNearestHostile(ServerPlayer owner, double radius) {
        ServerLevel level = (ServerLevel) owner.level();
        return level.getEntitiesOfClass(
            Mob.class,
            owner.getBoundingBox().inflate(radius),
            entity -> entity instanceof Enemy && entity.isAlive()
        ).stream().min(Comparator.comparingDouble(entity -> owner.distanceToSqr(entity))).orElse(null);
    }

    private static List<ItemEntity> findNearbyItems(ServerPlayer owner, double radius) {
        return ((ServerLevel) owner.level()).getEntitiesOfClass(
            ItemEntity.class,
            owner.getBoundingBox().inflate(radius),
            entity -> entity.isAlive() && !entity.getItem().isEmpty()
        );
    }

    private static List<Animal> findNearbyAnimals(ServerPlayer owner, double radius) {
        return ((ServerLevel) owner.level()).getEntitiesOfClass(
            Animal.class,
            owner.getBoundingBox().inflate(radius),
            LivingEntity::isAlive
        );
    }

    private static Player findNearbyPlayerWithItem(ServerPlayer owner, double radius) {
        return ((ServerLevel) owner.level()).getEntitiesOfClass(
            Player.class,
            owner.getBoundingBox().inflate(radius),
            player -> player != owner && player.isAlive() && !player.getMainHandItem().isEmpty()
        ).stream().findFirst().orElse(null);
    }

    private static AbstractArrow findTrackedArrow(ServerPlayer owner, double radius) {
        return ((ServerLevel) owner.level()).getEntitiesOfClass(
            AbstractArrow.class,
            owner.getBoundingBox().inflate(radius),
            arrow -> arrow.isAlive() && arrow.getOwner() == owner
        ).stream().findFirst().orElse(null);
    }

    private static boolean hasOwner(Entity entity, UUID ownerId) {
        return entity.getTags().contains(OWNER_TAG_PREFIX + ownerId);
    }

    private static FairyRole roleOf(Entity entity) {
        return FairyRole.byId(tagValue(entity, ROLE_TAG_PREFIX, FairyRole.defaultRole().id()));
    }

    private static int slotOf(Entity entity) {
        return Integer.parseInt(tagValue(entity, SLOT_TAG_PREFIX, "-1"));
    }

    private static String tagValue(Entity entity, String prefix, String fallback) {
        for (String tag : entity.getTags()) {
            if (tag.startsWith(prefix)) {
                return tag.substring(prefix.length());
            }
        }
        return fallback;
    }

    private static void setTagValue(Entity entity, String prefix, String value) {
        Set<String> tags = Set.copyOf(entity.getTags());
        for (String tag : tags) {
            if (tag.startsWith(prefix)) {
                entity.removeTag(tag);
            }
        }
        entity.addTag(prefix + value);
    }
}
