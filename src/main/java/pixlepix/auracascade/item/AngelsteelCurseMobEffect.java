package pixlepix.auracascade.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.parity.AuraColor;

final class AngelsteelCurseMobEffect extends MobEffect {
    private final AuraColor color;

    AngelsteelCurseMobEffect(AuraColor color) {
        super(MobEffectCategory.HARMFUL, 0x7A3A9A);
        if (!AngelsteelCurseEffects.isAttunedColor(color)) {
            throw new IllegalArgumentException("No Angelsteel curse is defined for aura color " + color);
        }
        this.color = color;
    }

    static int tickInterval(AuraColor color) {
        return switch (color) {
            case RED, ORANGE -> 100;
            case YELLOW -> 250;
            case GREEN -> 40;
            case BLUE -> 50;
            case VIOLET -> 60;
            case WHITE, BLACK -> throw new IllegalArgumentException(
                "No Angelsteel curse cadence is defined for aura color " + color
            );
        };
    }

    static int curseDuration(int degreeIndex) {
        int degree = Math.max(0, Math.min(10, degreeIndex));
        return degree * degree * 100 + 100;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration > 0 && duration % tickInterval(color) == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        switch (color) {
            case RED -> placeFire(level, entity);
            case ORANGE -> {
                Vec3 motion = entity.getDeltaMovement();
                entity.setDeltaMovement(motion.x, motion.y + 1.0D, motion.z);
                entity.needsSync = true;
            }
            case YELLOW -> {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (lightning != null) {
                    lightning.snapTo(entity.getX(), entity.getY(), entity.getZ());
                    level.addFreshEntity(lightning);
                }
            }
            case GREEN -> spreadGreenCurse(level, entity, amplifier);
            case BLUE -> {
                if (entity.getHealth() < entity.getMaxHealth() / 2.0F) {
                    entity.hurt(level.damageSources().magic(), 4.0F);
                }
            }
            case VIOLET -> swapNearbyPositions(level, entity);
            case WHITE, BLACK -> throw new IllegalStateException("Unsupported Angelsteel curse color " + color);
        }
        return true;
    }

    private static void placeFire(ServerLevel level, LivingEntity entity) {
        BlockPos center = entity.blockPosition();
        for (int x = -5; x <= 6; x++) {
            for (int y = -2; y <= 3; y++) {
                for (int z = -5; z <= 6; z++) {
                    BlockPos pos = center.offset(x, y, z);
                    if (level.hasChunkAt(pos) && level.isEmptyBlock(pos)) {
                        level.setBlockAndUpdate(pos, Blocks.FIRE.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void spreadGreenCurse(ServerLevel level, LivingEntity entity, int amplifier) {
        entity.hurt(level.damageSources().magic(), 2.0F);
        MobEffectInstance current = entity.getEffect(AngelsteelCurseEffects.effect(AuraColor.GREEN));
        if (current == null) {
            return;
        }
        List<LivingEntity> candidates = level.getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(5.0D),
            candidate -> candidate != entity && candidate.isAlive()
                && !candidate.hasEffect(AngelsteelCurseEffects.effect(AuraColor.GREEN))
        );
        if (candidates.isEmpty()) {
            return;
        }
        LivingEntity selected = candidates.get(level.getRandom().nextInt(candidates.size()));
        selected.addEffect(new MobEffectInstance(
            AngelsteelCurseEffects.effect(AuraColor.GREEN),
            current.getDuration(),
            amplifier
        ));
    }

    private static void swapNearbyPositions(ServerLevel level, LivingEntity entity) {
        List<LivingEntity> candidates = level.getEntitiesOfClass(
            LivingEntity.class,
            entity.getBoundingBox().inflate(15.0D),
            candidate -> candidate != entity && candidate.isAlive()
        );
        if (candidates.isEmpty()) {
            return;
        }
        LivingEntity selected = candidates.get(level.getRandom().nextInt(candidates.size()));
        Vec3 entityPosition = entity.position();
        Vec3 selectedPosition = selected.position();
        moveTo(entity, selectedPosition);
        moveTo(selected, entityPosition);
    }

    private static void moveTo(LivingEntity entity, Vec3 position) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(position.x, position.y, position.z);
        } else {
            entity.setPos(position.x, position.y, position.z);
        }
    }
}
