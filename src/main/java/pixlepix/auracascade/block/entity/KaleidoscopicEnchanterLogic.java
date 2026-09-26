package pixlepix.auracascade.block.entity;

import java.util.ArrayDeque;
import java.util.List;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import pixlepix.auracascade.enchantment.AuraEnchantments;
import pixlepix.auracascade.parity.AuraColor;

public final class KaleidoscopicEnchanterLogic {
    public static final int MAX_LEVEL = 8;
    private static final int TREE_FELL_BLOCK_LIMIT = 48;
    private static final int HARVEST_RADIUS = 1;
    private static final double SPLASH_RADIUS = 2.5D;
    private static final double DROP_DUPLICATION_RADIUS = 1.5D;
    private static final float HARD_DURABILITY_THRESHOLD = 8.0F;

    private static boolean runtimeHooksRegistered;
    private static boolean temporaryBreakActive;
    private static boolean treeFellActive;
    private static boolean cropHarvestActive;
    private static boolean splashDamageActive;
    private static boolean lootDuplicationActive;

    private KaleidoscopicEnchanterLogic() {
    }

    public static void bootstrap() {
        if (runtimeHooksRegistered) {
            return;
        }

        AttackBlockCallback.EVENT.register(KaleidoscopicEnchanterLogic::handleAttackBlock);
        AttackEntityCallback.EVENT.register(KaleidoscopicEnchanterLogic::handleAttackEntity);
        PlayerBlockBreakEvents.BEFORE.register(KaleidoscopicEnchanterLogic::handleBeforeBlockBreak);
        PlayerBlockBreakEvents.AFTER.register(KaleidoscopicEnchanterLogic::handleAfterBlockBreak);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(KaleidoscopicEnchanterLogic::handleAfterDamage);
        ServerLivingEntityEvents.AFTER_DEATH.register(KaleidoscopicEnchanterLogic::handleAfterDeath);
        runtimeHooksRegistered = true;
    }

    public static boolean isValidTarget(ItemStack stack) {
        return !stack.isEmpty() && stack.is(AuraEnchantments.KALEIDOSCOPIC_ENCHANTABLE);
    }

    public static double successRate(int totalLevel, int maxLevel) {
        return Math.pow(0.75D, Math.max(0, totalLevel)) * Math.pow(0.25D, Math.max(0, maxLevel - 4));
    }

    public static Map<AuraColor, Integer> levels(ItemStack stack, HolderLookup.Provider registries) {
        EnumMap<AuraColor, Integer> levels = new EnumMap<>(AuraColor.class);
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        for (Map.Entry<AuraColor, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment>> entry : AuraEnchantments.allKaleidoscopic().entrySet()) {
            levels.put(entry.getKey(), enchantments.getLevel(lookup.getOrThrow(entry.getValue())));
        }
        return levels;
    }

    public static int totalLevel(Map<AuraColor, Integer> levels) {
        return levels.values().stream().mapToInt(Integer::intValue).sum();
    }

    public static int maxLevel(Map<AuraColor, Integer> levels) {
        return levels.values().stream().mapToInt(Integer::intValue).max().orElse(0);
    }

    public static void syncPersistentRuntimeEnchantments(ItemStack stack, HolderLookup.Provider registries) {
        if (!isValidTarget(stack)) {
            return;
        }

        Map<AuraColor, Integer> levels = levels(stack, registries);
        if (totalLevel(levels) == 0) {
            return;
        }

        RuntimeCombatProfile combatProfile = runtimeCombatProfile(levels);
        applyIfHigher(stack, registries, Enchantments.EFFICIENCY, combatProfile.efficiencyLevel());
        applyIfHigher(stack, registries, Enchantments.KNOCKBACK, combatProfile.knockbackLevel());
        applyIfHigher(stack, registries, Enchantments.FIRE_ASPECT, combatProfile.fireAspectLevel());
        applyIfHigher(stack, registries, Enchantments.LOOTING, combatProfile.lootingLevel());
        applyIfHigher(stack, registries, Enchantments.SWEEPING_EDGE, combatProfile.sweepingLevel());
    }

    public static boolean canApply(ItemStack stack, AuraColor color, HolderLookup.Provider registries) {
        if (!AuraEnchantments.kaleidoscopic(color).isPresent() || !isValidTarget(stack)) {
            return false;
        }
        return levels(stack, registries).getOrDefault(color, 0) < MAX_LEVEL;
    }

    static RuntimeCombatProfile runtimeCombatProfile(Map<AuraColor, Integer> levels) {
        return new RuntimeCombatProfile(
            clamp(level(levels, AuraColor.ORANGE), 5),
            clamp(level(levels, AuraColor.BLUE), 2),
            clamp(pairLevel(levels, AuraColor.YELLOW, AuraColor.BLUE), 2),
            clamp(pairLevel(levels, AuraColor.YELLOW, AuraColor.VIOLET), 3),
            clamp(pairLevel(levels, AuraColor.BLUE, AuraColor.VIOLET), 3)
        );
    }

    static BreakProfile breakProfile(Map<AuraColor, Integer> levels, BlockState state) {
        Item extraIngot = extraIngotFor(state);
        if (pairLevel(levels, AuraColor.RED, AuraColor.YELLOW) > 0 && extraIngot != null) {
            return new BreakProfile(BreakMode.SMELTED_DOUBLE_INGOT, 1, extraIngot);
        }
        if (level(levels, AuraColor.RED) > 0) {
            return new BreakProfile(BreakMode.SILK_TOUCH, 1, null);
        }
        int fortuneLevel = clamp(level(levels, AuraColor.YELLOW), 3);
        if (fortuneLevel > 0) {
            return new BreakProfile(BreakMode.FORTUNE, fortuneLevel, null);
        }
        return BreakProfile.NONE;
    }

    static MiningProfile runtimeMiningProfile(Map<AuraColor, Integer> levels, BlockState state) {
        int hasteLevel = 0;
        if (level(levels, AuraColor.VIOLET) > 0 && isHardDurabilityBlock(state)) {
            hasteLevel = Math.max(hasteLevel, boostLevel(levels, AuraColor.VIOLET));
        }
        if (pairLevel(levels, AuraColor.RED, AuraColor.ORANGE) > 0 && isOreBlock(state)) {
            hasteLevel = Math.max(hasteLevel, boostLevel(levels, AuraColor.RED, AuraColor.ORANGE));
        }
        if (pairLevel(levels, AuraColor.YELLOW, AuraColor.ORANGE) > 0 && isStoneBlock(state)) {
            hasteLevel = Math.max(hasteLevel, boostLevel(levels, AuraColor.YELLOW, AuraColor.ORANGE));
        }
        if (pairLevel(levels, AuraColor.BLUE, AuraColor.ORANGE) > 0 && isLooseBlock(state)) {
            hasteLevel = Math.max(hasteLevel, boostLevel(levels, AuraColor.BLUE, AuraColor.ORANGE));
        }
        if (pairLevel(levels, AuraColor.GREEN, AuraColor.ORANGE) > 0 && isWoodBlock(state)) {
            hasteLevel = Math.max(hasteLevel, boostLevel(levels, AuraColor.GREEN, AuraColor.ORANGE));
        }
        if (pairLevel(levels, AuraColor.VIOLET, AuraColor.ORANGE) > 0 && isHardDurabilityBlock(state)) {
            hasteLevel = Math.max(hasteLevel, boostLevel(levels, AuraColor.VIOLET, AuraColor.ORANGE));
        }

        int fatigueLevel = 0;
        if (pairLevel(levels, AuraColor.RED, AuraColor.GREEN) > 0) {
            fatigueLevel = boostLevel(levels, AuraColor.RED, AuraColor.GREEN);
        }

        return new MiningProfile(hasteLevel, fatigueLevel);
    }

    static boolean isOreBlock(BlockState state) {
        return blockPath(state).contains("ore");
    }

    static boolean isStoneBlock(BlockState state) {
        String path = blockPath(state);
        return path.contains("stone")
            || path.contains("deepslate")
            || path.contains("netherrack")
            || path.contains("blackstone")
            || path.contains("basalt")
            || path.contains("tuff")
            || path.contains("calcite")
            || path.contains("cobble");
    }

    static boolean isLooseBlock(BlockState state) {
        return state.is(Blocks.DIRT)
            || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.ROOTED_DIRT)
            || state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.PODZOL)
            || state.is(Blocks.MYCELIUM)
            || state.is(Blocks.GRAVEL)
            || state.is(Blocks.SAND)
            || state.is(Blocks.RED_SAND);
    }

    static boolean isWoodBlock(BlockState state) {
        String path = blockPath(state);
        return path.contains("log")
            || path.contains("wood")
            || path.contains("planks")
            || path.contains("stem")
            || path.contains("hyphae")
            || path.contains("bamboo");
    }

    static boolean isHardDurabilityBlock(BlockState state) {
        return state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) >= HARD_DURABILITY_THRESHOLD;
    }

    private static InteractionResult handleAttackBlock(Player player, Level level, InteractionHand hand, BlockPos pos, Direction direction) {
        if (level.isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getMainHandItem();
        if (!isValidTarget(stack)) {
            return InteractionResult.PASS;
        }

        syncPersistentRuntimeEnchantments(stack, level.registryAccess());
        MiningProfile miningProfile = runtimeMiningProfile(levels(stack, level.registryAccess()), level.getBlockState(pos));
        if (miningProfile.hasteLevel() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.HASTE, 12, miningProfile.hasteLevel() - 1, true, false, false));
        }
        if (miningProfile.fatigueLevel() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 12, miningProfile.fatigueLevel() - 1, true, false, false));
        }
        return InteractionResult.PASS;
    }

    private static InteractionResult handleAttackEntity(
        Player player,
        Level level,
        InteractionHand hand,
        Entity entity,
        EntityHitResult hitResult
    ) {
        if (!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
            syncPersistentRuntimeEnchantments(player.getMainHandItem(), level.registryAccess());
        }
        return InteractionResult.PASS;
    }

    private static boolean handleBeforeBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (temporaryBreakActive || !(player instanceof ServerPlayer serverPlayer) || player.getAbilities().instabuild) {
            return true;
        }

        ItemStack stack = player.getMainHandItem();
        if (!isValidTarget(stack)) {
            return true;
        }

        return applyBeforeBlockBreakRuntime(new LiveBreakRuntime(serverPlayer, stack), levels(stack, level.registryAccess()), pos, state);
    }

    private static void handleAfterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack stack = player.getMainHandItem();
        if (!isValidTarget(stack)) {
            return;
        }

        Map<AuraColor, Integer> levels = levels(stack, level.registryAccess());
        int greenLevel = level(levels, AuraColor.GREEN);
        if (greenLevel > 0 && isLogBlock(state)) {
            fellNearbyLogs(serverPlayer, pos, greenLevel);
        }
        if (pairLevel(levels, AuraColor.YELLOW, AuraColor.GREEN) > 0 && isHarvestableCrop(state)) {
            harvestNearbyCrops(serverPlayer, pos);
        }
    }

    private static void handleAfterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (!(entity.level() instanceof ServerLevel serverLevel) || damageTaken <= 0.0F) {
            return;
        }

        Map<AuraColor, Integer> defenderLevels = Map.of();
        ItemStack defenderStack = entity.getMainHandItem();
        if (isValidTarget(defenderStack)) {
            defenderLevels = levels(defenderStack, serverLevel.registryAccess());
        }

        LivingEntity attacker = null;
        Map<AuraColor, Integer> attackerLevels = Map.of();
        if (source.getEntity() instanceof LivingEntity livingAttacker && source.getDirectEntity() == livingAttacker) {
            ItemStack stack = livingAttacker.getMainHandItem();
            if (isValidTarget(stack)) {
                syncPersistentRuntimeEnchantments(stack, serverLevel.registryAccess());
                attacker = livingAttacker;
                attackerLevels = levels(stack, serverLevel.registryAccess());
            }
        }

        applyAfterDamageRuntime(new LiveAfterDamageRuntime(serverLevel, attacker, entity), defenderLevels, attackerLevels, damageTaken);
    }

    private static void handleAfterDeath(LivingEntity entity, DamageSource damageSource) {
        if (lootDuplicationActive || !(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(damageSource.getEntity() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (!isValidTarget(stack)) {
            return;
        }

        Map<AuraColor, Integer> levels = levels(stack, serverLevel.registryAccess());
        if (pairLevel(levels, AuraColor.YELLOW, AuraColor.VIOLET) <= 0) {
            return;
        }

        lootDuplicationActive = true;
        try {
            for (ItemEntity drop : serverLevel.getEntitiesOfClass(
                ItemEntity.class,
                entity.getBoundingBox().inflate(DROP_DUPLICATION_RADIUS),
                itemEntity -> !itemEntity.isRemoved()
            )) {
                if (drop.getItem().isEmpty()) {
                    continue;
                }
                ItemEntity extraDrop = new ItemEntity(serverLevel, drop.getX(), drop.getY(), drop.getZ(), drop.getItem().copy());
                extraDrop.setDeltaMovement(drop.getDeltaMovement());
                serverLevel.addFreshEntity(extraDrop);
            }
        } finally {
            lootDuplicationActive = false;
        }
    }

    static boolean applyBeforeBlockBreakRuntime(BreakRuntime runtime, Map<AuraColor, Integer> levels, BlockPos pos, BlockState state) {
        BreakProfile breakProfile = breakProfile(levels, state);
        if (breakProfile.mode() == BreakMode.NONE) {
            return true;
        }

        performTemporaryBreak(runtime, pos, state, breakProfile);
        return false;
    }

    private static void performTemporaryBreak(BreakRuntime runtime, BlockPos pos, BlockState state, BreakProfile breakProfile) {
        if (breakProfile.mode() == BreakMode.SMELTED_DOUBLE_INGOT) {
            performSmeltedDoubleIngotBreak(runtime, pos, state, breakProfile);
            return;
        }

        runtime.performStandardBreak(pos, breakProfile);
    }

    private static void performVanillaTemporaryBreak(ServerPlayer player, ItemStack stack, BlockPos pos, BreakProfile breakProfile) {

        ItemEnchantments originalEnchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutableEnchantments = new ItemEnchantments.Mutable(originalEnchantments);
        HolderLookup.RegistryLookup<Enchantment> enchantments = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        switch (breakProfile.mode()) {
            case SILK_TOUCH -> mutableEnchantments.set(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
            case FORTUNE -> mutableEnchantments.set(enchantments.getOrThrow(Enchantments.FORTUNE), breakProfile.temporaryLevel());
            case NONE -> {
                return;
            }
            case SMELTED_DOUBLE_INGOT -> {
                return;
            }
        }

        temporaryBreakActive = true;
        try {
            stack.set(DataComponents.ENCHANTMENTS, mutableEnchantments.toImmutable());
            player.gameMode.destroyBlock(pos);
        } finally {
            temporaryBreakActive = false;
            if (!stack.isEmpty()) {
                stack.set(DataComponents.ENCHANTMENTS, originalEnchantments);
            }
        }

        if (breakProfile.extraDrop() != null) {
            Block.popResource(player.level(), pos, new ItemStack(breakProfile.extraDrop()));
        }
    }

    private static void performSmeltedDoubleIngotBreak(BreakRuntime runtime, BlockPos pos, BlockState state, BreakProfile breakProfile) {
        if (state.isAir() || breakProfile.extraDrop() == null) {
            return;
        }

        Block block = state.getBlock();
        BlockState destroyState = runtime.prepareDestroyState(pos, state);
        boolean removed = runtime.removeBlock(pos);
        if (!removed) {
            return;
        }

        runtime.destroyBlock(block, pos, destroyState);
        boolean correctTool = runtime.hasCorrectToolForDrops(destroyState);
        runtime.mineBlock(destroyState, pos);
        if (!correctTool) {
            return;
        }

        runtime.awardStat(block);
        runtime.causeFoodExhaustion(0.005F);
        for (ItemStack drop : smeltedDoubleIngotDrops(state)) {
            runtime.popResource(pos, drop);
        }
    }

    private static void fellNearbyLogs(ServerPlayer player, BlockPos origin, int greenLevel) {
        if (treeFellActive) {
            return;
        }

        treeFellActive = true;
        try {
            int remaining = Math.min(TREE_FELL_BLOCK_LIMIT, 8 + (greenLevel * 4));
            ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            frontier.add(origin);
            visited.add(origin);

            while (!frontier.isEmpty() && remaining > 0) {
                BlockPos current = frontier.removeFirst();
                for (BlockPos cursor : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                    BlockPos next = cursor.immutable();
                    if (!visited.add(next) || next.equals(origin) || !isLogBlock(player.level().getBlockState(next))) {
                        continue;
                    }

                    frontier.addLast(next);
                    if (player.gameMode.destroyBlock(next)) {
                        remaining--;
                        if (remaining == 0) {
                            break;
                        }
                    }
                }
            }
        } finally {
            treeFellActive = false;
        }
    }

    private static void harvestNearbyCrops(ServerPlayer player, BlockPos origin) {
        if (cropHarvestActive) {
            return;
        }

        cropHarvestActive = true;
        try {
            for (int xOffset = -HARVEST_RADIUS; xOffset <= HARVEST_RADIUS; xOffset++) {
                for (int zOffset = -HARVEST_RADIUS; zOffset <= HARVEST_RADIUS; zOffset++) {
                    if (xOffset == 0 && zOffset == 0) {
                        continue;
                    }

                    BlockPos targetPos = origin.offset(xOffset, 0, zOffset);
                    if (isHarvestableCrop(player.level().getBlockState(targetPos))) {
                        player.gameMode.destroyBlock(targetPos);
                    }
                }
            }
        } finally {
            cropHarvestActive = false;
        }
    }

    static void applyAfterDamageRuntime(
        DamageRuntime runtime,
        Map<AuraColor, Integer> defenderLevels,
        Map<AuraColor, Integer> attackerLevels,
        float damageTaken
    ) {
        int reductionLevel = pairLevel(defenderLevels, AuraColor.RED, AuraColor.VIOLET);
        if (reductionLevel > 0) {
            runtime.healTarget(Math.max(0.5F, damageTaken * (0.10F + (reductionLevel * 0.05F))));
        }

        if (attackerLevels.isEmpty()) {
            return;
        }

        int splashLevel = pairLevel(attackerLevels, AuraColor.BLUE, AuraColor.VIOLET);
        if (splashLevel > 0) {
            runtime.applySplashDamage(damageTaken, splashLevel);
        }

        int lifeStealLevel = pairLevel(attackerLevels, AuraColor.GREEN, AuraColor.BLUE);
        if (lifeStealLevel > 0) {
            runtime.healAttacker(Math.max(0.5F, damageTaken * (0.10F + (lifeStealLevel * 0.05F))));
        }

        int recoilLevel = pairLevel(attackerLevels, AuraColor.RED, AuraColor.BLUE);
        if (recoilLevel > 0) {
            runtime.applyRecoil(recoilLevel);
        }

        MobEffectInstance attackReductionEffect = outgoingAttackReductionEffect(attackerLevels);
        if (attackReductionEffect != null && runtime.targetAlive()) {
            runtime.addTargetEffect(attackReductionEffect);
        }
    }

    private static void applySplashDamage(ServerLevel level, LivingEntity attacker, LivingEntity primaryTarget, float damageTaken, int pairLevel) {
        if (splashDamageActive) {
            return;
        }

        DamageSource splashSource = attacker instanceof ServerPlayer serverPlayer
            ? level.damageSources().playerAttack(serverPlayer)
            : level.damageSources().mobAttack(attacker);
        float splashDamage = Math.max(1.0F, damageTaken * (0.35F + (pairLevel * 0.05F)));

        splashDamageActive = true;
        try {
            for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class,
                primaryTarget.getBoundingBox().inflate(SPLASH_RADIUS),
                livingEntity -> livingEntity != attacker && livingEntity != primaryTarget && livingEntity.isAlive()
            )) {
                nearby.hurt(splashSource, splashDamage);
            }
        } finally {
            splashDamageActive = false;
        }
    }

    private static void applyRecoil(LivingEntity attacker, LivingEntity victim, int pairLevel) {
        double x = attacker.getX() - victim.getX();
        double z = attacker.getZ() - victim.getZ();
        double magnitude = Math.max(0.1D, Math.sqrt((x * x) + (z * z)));
        double strength = 0.25D + (0.1D * pairLevel);
        attacker.push((x / magnitude) * strength, 0.1D, (z / magnitude) * strength);
    }

    private static boolean isHarvestableCrop(BlockState state) {
        if (state.getBlock() instanceof CropBlock cropBlock) {
            return cropBlock.isMaxAge(state);
        }
        if (state.getBlock() instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (state.getBlock() instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 3;
        }
        return false;
    }

    private static Item extraIngotFor(BlockState state) {
        String path = blockPath(state);
        if (path.contains("iron_ore")) {
            return Items.IRON_INGOT;
        }
        if (path.contains("gold_ore")) {
            return Items.GOLD_INGOT;
        }
        if (path.contains("copper_ore")) {
            return Items.COPPER_INGOT;
        }
        return null;
    }

    static List<ItemStack> smeltedDoubleIngotDrops(BlockState state) {
        Item ingot = extraIngotFor(state);
        if (ingot == null) {
            return List.of();
        }
        return List.of(new ItemStack(ingot), new ItemStack(ingot));
    }

    private static boolean isLogBlock(BlockState state) {
        String path = blockPath(state);
        return path.contains("log")
            || path.endsWith("_wood")
            || path.contains("stem")
            || path.contains("hyphae");
    }

    private static String blockPath(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
    }

    private static int boostLevel(Map<AuraColor, Integer> levels, AuraColor first, AuraColor second) {
        return boostLevel(pairLevel(levels, first, second));
    }

    private static int boostLevel(Map<AuraColor, Integer> levels, AuraColor color) {
        return boostLevel(level(levels, color));
    }

    private static int boostLevel(int rawLevel) {
        if (rawLevel <= 0) {
            return 0;
        }
        return clamp(1 + ((rawLevel - 1) / 2), 4);
    }

    private static int pairLevel(Map<AuraColor, Integer> levels, AuraColor first, AuraColor second) {
        return Math.min(level(levels, first), level(levels, second));
    }

    private static int level(Map<AuraColor, Integer> levels, AuraColor color) {
        return levels.getOrDefault(color, 0);
    }

    private static void applyIfHigher(
        ItemStack stack,
        HolderLookup.Provider registries,
        net.minecraft.resources.ResourceKey<Enchantment> key,
        int level
    ) {
        if (level <= 0) {
            return;
        }

        Holder<Enchantment> enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        if (stack.getEnchantments().getLevel(enchantment) < level) {
            stack.enchant(enchantment, level);
        }
    }

    private static int clamp(int level, int maxLevel) {
        return Math.max(0, Math.min(level, maxLevel));
    }

    static MobEffectInstance outgoingAttackReductionEffect(Map<AuraColor, Integer> levels) {
        int attackReductionLevel = pairLevel(levels, AuraColor.GREEN, AuraColor.VIOLET);
        if (attackReductionLevel <= 0) {
            return null;
        }
        return new MobEffectInstance(
            MobEffects.WEAKNESS,
            80 + (attackReductionLevel * 20),
            Math.max(0, boostLevel(attackReductionLevel) - 1),
            true,
            true,
            true
        );
    }

    static BreakProfile modernRuntimeBreakProfile(Map<AuraColor, Integer> levels, BlockState state) {
        return breakProfile(levels, state);
    }

    interface BreakRuntime {
        void performStandardBreak(BlockPos pos, BreakProfile breakProfile);

        BlockState prepareDestroyState(BlockPos pos, BlockState state);

        boolean removeBlock(BlockPos pos);

        void destroyBlock(Block block, BlockPos pos, BlockState destroyState);

        boolean hasCorrectToolForDrops(BlockState state);

        void mineBlock(BlockState state, BlockPos pos);

        void awardStat(Block block);

        void causeFoodExhaustion(float exhaustion);

        void popResource(BlockPos pos, ItemStack stack);
    }

    interface DamageRuntime {
        void healTarget(float amount);

        void healAttacker(float amount);

        void applySplashDamage(float damageTaken, int pairLevel);

        void applyRecoil(int pairLevel);

        boolean targetAlive();

        void addTargetEffect(MobEffectInstance effect);
    }

    private record LiveBreakRuntime(ServerPlayer player, ItemStack stack) implements BreakRuntime {
        @Override
        public void performStandardBreak(BlockPos pos, BreakProfile breakProfile) {
            performVanillaTemporaryBreak(player, stack, pos, breakProfile);
        }

        @Override
        public BlockState prepareDestroyState(BlockPos pos, BlockState state) {
            return state.getBlock().playerWillDestroy((ServerLevel) player.level(), pos, state, player);
        }

        @Override
        public boolean removeBlock(BlockPos pos) {
            return ((ServerLevel) player.level()).removeBlock(pos, false);
        }

        @Override
        public void destroyBlock(Block block, BlockPos pos, BlockState destroyState) {
            block.destroy((ServerLevel) player.level(), pos, destroyState);
        }

        @Override
        public boolean hasCorrectToolForDrops(BlockState state) {
            return player.hasCorrectToolForDrops(state);
        }

        @Override
        public void mineBlock(BlockState state, BlockPos pos) {
            stack.mineBlock((ServerLevel) player.level(), state, pos, player);
        }

        @Override
        public void awardStat(Block block) {
            player.awardStat(Stats.BLOCK_MINED.get(block));
        }

        @Override
        public void causeFoodExhaustion(float exhaustion) {
            player.causeFoodExhaustion(exhaustion);
        }

        @Override
        public void popResource(BlockPos pos, ItemStack stack) {
            Block.popResource(player.level(), pos, stack);
        }
    }

    private record LiveAfterDamageRuntime(ServerLevel level, LivingEntity attacker, LivingEntity target) implements DamageRuntime {
        @Override
        public void healTarget(float amount) {
            target.heal(amount);
        }

        @Override
        public void healAttacker(float amount) {
            Objects.requireNonNull(attacker, "attacker").heal(amount);
        }

        @Override
        public void applySplashDamage(float damageTaken, int pairLevel) {
            KaleidoscopicEnchanterLogic.applySplashDamage(level, Objects.requireNonNull(attacker, "attacker"), target, damageTaken, pairLevel);
        }

        @Override
        public void applyRecoil(int pairLevel) {
            KaleidoscopicEnchanterLogic.applyRecoil(Objects.requireNonNull(attacker, "attacker"), target, pairLevel);
        }

        @Override
        public boolean targetAlive() {
            return target.isAlive();
        }

        @Override
        public void addTargetEffect(MobEffectInstance effect) {
            target.addEffect(effect);
        }
    }

    static record RuntimeCombatProfile(
        int efficiencyLevel,
        int knockbackLevel,
        int fireAspectLevel,
        int lootingLevel,
        int sweepingLevel
    ) {
    }

    static record MiningProfile(int hasteLevel, int fatigueLevel) {
    }

    static record BreakProfile(BreakMode mode, int temporaryLevel, Item extraDrop) {
        static final BreakProfile NONE = new BreakProfile(BreakMode.NONE, 0, null);
    }

    enum BreakMode {
        NONE,
        SILK_TOUCH,
        FORTUNE,
        SMELTED_DOUBLE_INGOT
    }
}
