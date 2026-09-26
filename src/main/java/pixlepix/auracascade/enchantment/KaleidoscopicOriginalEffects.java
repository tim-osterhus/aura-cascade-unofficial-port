package pixlepix.auracascade.enchantment;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import pixlepix.auracascade.block.entity.KaleidoscopicEnchanterLogic;
import pixlepix.auracascade.parity.AuraColor;

public final class KaleidoscopicOriginalEffects {
    private static final TagKey<Block> COMMON_ORES = TagKey.create(Registries.BLOCK,
        Identifier.fromNamespaceAndPath("c", "ores"));
    private static boolean bootstrapped;
    private static boolean temporaryBreak;
    private static boolean chainBreak;
    private static PendingChain pendingChain;

    private KaleidoscopicOriginalEffects() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        AttackEntityCallback.EVENT.register(KaleidoscopicOriginalEffects::onAttack);
        PlayerBlockBreakEvents.BEFORE.register(KaleidoscopicOriginalEffects::beforeBreak);
        PlayerBlockBreakEvents.AFTER.register(KaleidoscopicOriginalEffects::afterBreak);
        bootstrapped = true;
    }

    public static int pair(Map<AuraColor, Integer> levels, AuraColor first, AuraColor second) {
        return (int) Math.ceil(Math.sqrt((double) level(levels, first) * level(levels, second)));
    }

    public static float damageAmount(float original, Map<AuraColor, Integer> attacker,
                                     Map<AuraColor, Integer> defender, boolean playerAttacker, boolean playerDefender) {
        float amount = original;
        if (playerAttacker) {
            amount += 0.5F * level(attacker, AuraColor.VIOLET);
            amount = Math.max(0.0F, amount - pair(attacker, AuraColor.GREEN, AuraColor.VIOLET));
        }
        if (playerDefender) {
            amount *= (float) Math.pow(0.9D, pair(defender, AuraColor.RED, AuraColor.VIOLET));
        }
        return amount;
    }

    public static float modifyDamage(LivingEntity victim, DamageSource source, float original) {
        if (victim.level().isClientSide()) {
            return original;
        }
        Entity attacker = source.getEntity();
        Map<AuraColor, Integer> attackerLevels = attacker instanceof Player player ? levels(player) : Map.of();
        Map<AuraColor, Integer> defenderLevels = victim instanceof Player player ? levels(player) : Map.of();
        return damageAmount(original, attackerLevels, defenderLevels,
            attacker instanceof Player, victim instanceof Player);
    }

    public static float breakSpeed(float original, Map<AuraColor, Integer> levels, boolean correctTool,
                                   boolean ore, boolean stone, boolean log, boolean loose, float hardness) {
        double speed = original / Math.pow(3.0D, pair(levels, AuraColor.RED, AuraColor.GREEN));
        if (!correctTool) {
            return (float) speed;
        }
        speed *= Math.pow(1.15D, level(levels, AuraColor.ORANGE));
        if (hardness >= 3.0F) {
            speed *= Math.pow(1.5D, pair(levels, AuraColor.ORANGE, AuraColor.VIOLET));
        }
        if (ore) {
            speed *= Math.pow(1.25D, pair(levels, AuraColor.RED, AuraColor.ORANGE));
        }
        if (stone) {
            speed *= Math.pow(1.25D, pair(levels, AuraColor.YELLOW, AuraColor.ORANGE));
        }
        if (log) {
            speed *= Math.pow(1.25D, pair(levels, AuraColor.GREEN, AuraColor.ORANGE));
        }
        if (loose) {
            speed *= Math.pow(1.25D, pair(levels, AuraColor.BLUE, AuraColor.ORANGE));
        }
        return (float) speed;
    }

    public static float modifyBreakSpeed(Player player, BlockState state, float original) {
        Map<AuraColor, Integer> levels = levels(player);
        if (levels.isEmpty()) {
            return original;
        }
        Block block = state.getBlock();
        String path = BuiltInRegistries.BLOCK.getKey(block).getPath();
        boolean ore = state.is(COMMON_ORES) || path.endsWith("_ore");
        boolean loose = block == Blocks.GRASS_BLOCK || block == Blocks.DIRT || block == Blocks.GRAVEL || block == Blocks.SAND;
        return breakSpeed(original, levels, player.hasCorrectToolForDrops(state), ore, block == Blocks.STONE,
            state.is(BlockTags.LOGS), loose, state.getDestroySpeed(player.level(), player.blockPosition()));
    }

    public static boolean convertsOre(int pairStrength, int roll) {
        return pairStrength > 0 && roll < pairStrength;
    }

    public static int additionalConnectedBlocks(int strength) {
        return Math.max(0, 25 * strength - 1);
    }

    public static Item conversionIngot(BlockState state, Map<AuraColor, Integer> powers,
                                       boolean correctTool, IntSupplier roll) {
        int strength = pair(powers, AuraColor.RED, AuraColor.YELLOW);
        if (!correctTool || strength == 0) {
            return null;
        }
        Item ingot = ingotForOre(state);
        return ingot != null && convertsOre(strength, roll.getAsInt()) ? ingot : null;
    }

    public static void dropOrConvert(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                                     BlockEntity blockEntity, Entity breaker, ItemStack tool) {
        if (level instanceof ServerLevel server && breaker instanceof ServerPlayer player
            && !player.isSpectator() && KaleidoscopicEnchanterLogic.isValidTarget(tool)) {
            Map<AuraColor, Integer> powers = KaleidoscopicEnchanterLogic.levels(tool, server.registryAccess());
            Item ingot = conversionIngot(state, powers, tool.isCorrectToolForDrops(state),
                () -> server.getRandom().nextInt(4));
            if (ingot != null) {
                Block.popResource(level, pos, new ItemStack(ingot, 2));
                return;
            }
        }
        ItemStack lootTool = tool;
        if (level instanceof ServerLevel server && breaker instanceof ServerPlayer player && !player.isSpectator()
            && pixlepix.auracascade.item.AngelsteelToolHelper.isAngelsteelMiningTool(tool)) {
            var fortune = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
            lootTool = pixlepix.auracascade.item.AngelsteelToolHelper.dropFortuneTool(tool, state, fortune);
        }
        Block.dropResources(state, level, pos, blockEntity, breaker, lootTool);
    }

    private static InteractionResult onAttack(Player player, net.minecraft.world.level.Level level, InteractionHand hand,
                                              Entity target, EntityHitResult hit) {
        if (!(player instanceof ServerPlayer attacker) || attacker.isSpectator() || hand != InteractionHand.MAIN_HAND
            || !(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        Map<AuraColor, Integer> powers = levels(attacker);
        if (powers.isEmpty()) {
            return InteractionResult.PASS;
        }
        int splash = pair(powers, AuraColor.BLUE, AuraColor.VIOLET);
        int fire = pair(powers, AuraColor.YELLOW, AuraColor.BLUE);
        if (splash > 0) {
            AABB area = new AABB(target.getX() - 2.0D, target.getY() - 2.0D, target.getZ() - 2.0D,
                target.getX() + 2.0D, target.getY() + 2.0D, target.getZ() + 2.0D);
            for (LivingEntity nearby : server.getEntitiesOfClass(LivingEntity.class, area,
                entity -> entity != attacker && entity != target)) {
                nearby.hurt(server.damageSources().indirectMagic(attacker, target), splash);
                if (fire > 0) {
                    nearby.igniteForSeconds(fire * 0.5F);
                }
            }
        }
        int knockback = level(powers, AuraColor.BLUE);
        boolean invulnerable = target instanceof LivingEntity living
            ? living.isInvulnerableTo(server, server.damageSources().playerAttack(attacker))
            : target.isInvulnerable();
        if (knockback > 0 && !invulnerable) {
            double angle = Math.toRadians(attacker.getYRot());
            target.push(-Math.sin(angle) * knockback * 0.5D, 0.1D, Math.cos(angle) * knockback * 0.5D);
        }
        int recoil = pair(powers, AuraColor.RED, AuraColor.BLUE);
        if (recoil > 0) {
            attacker.hurt(server.damageSources().indirectMagic(attacker, attacker), recoil);
        }
        int lifesteal = pair(powers, AuraColor.GREEN, AuraColor.BLUE);
        if (lifesteal > 1) {
            attacker.heal(lifesteal / 2);
        }
        if (fire > 0) {
            target.igniteForSeconds(fire);
        }
        return InteractionResult.PASS;
    }

    private static boolean beforeBreak(net.minecraft.world.level.Level level, Player player, BlockPos pos,
                                       BlockState state, BlockEntity blockEntity) {
        if (temporaryBreak || !(player instanceof ServerPlayer serverPlayer) || player.getAbilities().instabuild) {
            return true;
        }
        ItemStack stack = player.getMainHandItem();
        Map<AuraColor, Integer> powers = levels(player);
        if (powers.isEmpty()) {
            return true;
        }
        int red = level(powers, AuraColor.RED);
        int yellow = level(powers, AuraColor.YELLOW);
        if (red <= 0 && yellow <= 0) {
            return true;
        }
        if (red <= 0 && state.getBlock() instanceof CropBlock) {
            return true;
        }
        ItemEnchantments original = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable temporary = new ItemEnchantments.Mutable(original);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> enchantment = enchantments.getOrThrow(red > 0 ? Enchantments.SILK_TOUCH : Enchantments.FORTUNE);
        int desired = red > 0 ? 1 : yellow;
        if (original.getLevel(enchantment) >= desired) {
            return true;
        }
        temporary.set(enchantment, desired);
        temporaryBreak = true;
        try {
            stack.set(DataComponents.ENCHANTMENTS, temporary.toImmutable());
            serverPlayer.gameMode.destroyBlock(pos);
        } finally {
            temporaryBreak = false;
            if (!stack.isEmpty()) {
                stack.set(DataComponents.ENCHANTMENTS, original);
            }
            PendingChain pending = pendingChain;
            pendingChain = null;
            if (pending != null) {
                afterBreak(pending.level(), pending.player(), pending.pos(), pending.state(), pending.blockEntity());
            }
        }
        return false;
    }

    private static void afterBreak(net.minecraft.world.level.Level level, Player player, BlockPos pos,
                                   BlockState state, BlockEntity blockEntity) {
        if (chainBreak || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (temporaryBreak) {
            pendingChain = new PendingChain(level, serverPlayer, pos, state, blockEntity);
            return;
        }
        Map<AuraColor, Integer> powers = levels(player);
        if (powers.isEmpty()) {
            return;
        }
        int strength = 0;
        Block block = state.getBlock();
        if (state.is(BlockTags.LOGS)) {
            strength = level(powers, AuraColor.GREEN);
        } else if (block instanceof BonemealableBlock && block != Blocks.GRASS_BLOCK) {
            strength = pair(powers, AuraColor.GREEN, AuraColor.YELLOW);
        }
        int remaining = additionalConnectedBlocks(strength);
        if (remaining <= 0) {
            return;
        }
        chainBreak = true;
        try {
            ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            frontier.add(pos);
            visited.add(pos);
            while (!frontier.isEmpty() && remaining > 0) {
                BlockPos current = frontier.removeFirst();
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!visited.add(next) || level.getBlockState(next).getBlock() != block) {
                        continue;
                    }
                    boolean broken = serverPlayer.gameMode.destroyBlock(next)
                        || level.getBlockState(next).getBlock() != block;
                    if (broken) {
                        frontier.addLast(next);
                        remaining--;
                        if (remaining == 0) {
                            break;
                        }
                    }
                }
            }
        } finally {
            chainBreak = false;
        }
    }

    public static void extraLoot(ServerLevel level, LivingEntity victim, DamageSource source) {
        if (!(victim instanceof Mob) || !(source.getEntity() instanceof ServerPlayer attacker) || !level.getGameRules().get(
            net.minecraft.world.level.gamerules.GameRules.MOB_DROPS) || victim.getLootTable().isEmpty()) {
            return;
        }
        int strength = pair(levels(attacker), AuraColor.YELLOW, AuraColor.VIOLET);
        if (strength <= 0) {
            return;
        }
        ItemStack stack = attacker.getMainHandItem();
        ItemEnchantments original = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        var looting = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);
        ItemEnchantments.Mutable temporary = new ItemEnchantments.Mutable(original);
        temporary.set(looting, Math.max(strength, original.getLevel(looting)));
        LootParams params = new LootParams.Builder(level)
            .withParameter(LootContextParams.THIS_ENTITY, victim)
            .withParameter(LootContextParams.ORIGIN, victim.position())
            .withParameter(LootContextParams.DAMAGE_SOURCE, source)
            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, attacker)
            .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
            .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, attacker)
            .withLuck(attacker.getLuck())
            .create(LootContextParamSets.ENTITY);
        try {
            stack.set(DataComponents.ENCHANTMENTS, temporary.toImmutable());
            for (ItemStack extra : level.getServer().reloadableRegistries().getLootTable(victim.getLootTable().orElseThrow())
                .getRandomItems(params, level.getRandom())) {
                victim.spawnAtLocation(level, extra);
            }
        } finally {
            if (!stack.isEmpty()) {
                stack.set(DataComponents.ENCHANTMENTS, original);
            }
        }
    }

    private static Item ingotForOre(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) {
            return Items.IRON_INGOT;
        }
        if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE) {
            return Items.GOLD_INGOT;
        }
        if (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE) {
            return Items.COPPER_INGOT;
        }
        return BuiltInRegistries.BLOCK.getTags().map(net.minecraft.core.HolderSet.Named::key)
            .filter(tag -> tag.location().getNamespace().equals("c")
                && tag.location().getPath().startsWith("ores/") && state.is(tag))
            .map(tag -> tag.location().getPath().substring("ores/".length()))
            .map(material -> TagKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath("c", "ingots/" + material)))
            .map(BuiltInRegistries.ITEM::get)
            .flatMap(java.util.Optional::stream)
            .flatMap(holders -> holders.stream().map(Holder::value))
            .findFirst().orElse(null);
    }

    private static Map<AuraColor, Integer> levels(Player player) {
        ItemStack stack = player.getMainHandItem();
        if (!KaleidoscopicEnchanterLogic.isValidTarget(stack)) {
            return Map.of();
        }
        Map<AuraColor, Integer> powers = KaleidoscopicEnchanterLogic.levels(stack, player.level().registryAccess());
        return KaleidoscopicEnchanterLogic.totalLevel(powers) > 0 ? powers : Map.of();
    }

    private static int level(Map<AuraColor, Integer> powers, AuraColor color) {
        return powers.getOrDefault(color, 0);
    }

    private record PendingChain(net.minecraft.world.level.Level level, ServerPlayer player, BlockPos pos,
                                BlockState state, BlockEntity blockEntity) {
    }
}
