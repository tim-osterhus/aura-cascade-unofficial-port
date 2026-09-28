package pixlepix.auracascade.qa.neoforge;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.enchantment.AuraEnchantments;
import pixlepix.auracascade.parity.AuraColor;

/**
 * Call from a native GameTest or QA command, on the server thread in an isolated test world.
 * These assertions require real entity methods and Aura's registered listeners/mixins.
 * No JUnit, testframework, reflection, synthetic event posting, or production test hooks are used.
 */
public final class NeoForgeHookRuntimeFixture {
    private NeoForgeHookRuntimeFixture() {
    }

    public static List<String> runAll(ServerLevel level, BlockPos clearLoadedPosition) {
        requireWorld(level, clearLoadedPosition);
        require(level.getGameTime() % 100L != 0L, "Run on a non-eruption tick (gameTime % 100 != 0)");
        verifyDamage(level, clearLoadedPosition);
        verifyRedHoleLifetime(level, clearLoadedPosition);
        verifyThiefDrops(level, clearLoadedPosition);
        verifyBonusLootCancellation(level, clearLoadedPosition);
        verifyHarvestingIsolation(level, clearLoadedPosition);
        return List.of("damage: vanilla=4, modified=10", "red-hole: extension=100, reload, eventual expiry",
            "thief: uncanceled=1, canceled=0, reward present in both event collections",
            "bonus-loot: vanilla=3..5 iron, enchanted=6..10 iron, canceled=0 spawned",
            "harvesting: face-connected oak removed only with Green, diagonal birch retained");
    }

    public static void verifyDamage(ServerLevel level, BlockPos pos) {
        requireWorld(level, pos);
        Villager control = villager(level, pos);
        Villager modified = villager(level, pos);
        AtomicInteger reductions = new AtomicInteger();
        Consumer<LivingIncomingDamageEvent> listener = event -> {
            if (event.getEntity() == modified) {
                event.addReductionModifier(DamageContainer.Reduction.INVULNERABILITY, (container, amount) -> {
                    near(6.0F, amount, "Vanilla cooldown reduction reaching the modifier");
                    reductions.incrementAndGet();
                    return 0.0F;
                });
            }
        };
        NeoForge.EVENT_BUS.addListener(LivingIncomingDamageEvent.class, listener);
        try {
            require(control.hurt(level.damageSources().generic(), 6.0F), "Control first hit rejected");
            near(14.0F, control.getHealth(), "Control health after first hit");
            require(control.hurt(level.damageSources().generic(), 10.0F), "Control second hit rejected");
            near(10.0F, control.getHealth(), "Unmodified cooldown must apply only 10 - 6 damage");

            require(modified.hurt(level.damageSources().generic(), 6.0F), "Modified first hit rejected");
            near(14.0F, modified.getHealth(), "Modified health after first hit");
            require(modified.hurt(level.damageSources().generic(), 10.0F), "Modified second hit rejected");
            require(reductions.get() == 1, "Cooldown modifier must execute exactly once");
            near(4.0F, modified.getHealth(), "Aura must preserve the container's full 10 damage");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            control.discard();
            modified.discard();
        }
    }

    public static void verifyRedHoleLifetime(ServerLevel level, BlockPos pos) {
        requireWorld(level, pos);
        require(level.getGameTime() % 100L != 0L, "Red Hole fixture must not run on an eruption tick");
        ItemEntity redHole = item(level, pos, new ItemStack(AuraItems.PORTABLE_RED_HOLE));
        ItemEntity reloaded = item(level, pos, new ItemStack(AuraItems.PORTABLE_RED_HOLE));
        ItemEntity ordinary = item(level, pos, new ItemStack(Items.COBBLESTONE));
        AtomicInteger expirations = new AtomicInteger();
        Consumer<ItemExpireEvent> listener = event -> {
            if (event.getEntity() == redHole || event.getEntity() == reloaded) {
                if (expirations.incrementAndGet() == 1) {
                    event.addExtraLife(100);
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(ItemExpireEvent.class, listener);
        try {
            setAge(redHole, 29_998);
            redHole.tick();
            require(redHole.lifespan == 30_000, "Red Hole must establish a 30,000-tick floor");
            require(!redHole.isRemoved() && expirations.get() == 0, "Red Hole expired before its floor");
            redHole.tick();
            require(expirations.get() == 1, "Real tick must fire exactly one expiry event at age 30,000");
            require(redHole.lifespan == 30_100 && !redHole.isRemoved(), "Expiry extension was discarded");

            CompoundTag saved = redHole.saveWithoutId(new CompoundTag());
            require(saved.getInt("Lifespan") == 30_100, "Extended lifespan must serialize");
            reloaded.load(saved);
            redHole.discard();
            reloaded.tick();
            require(reloaded.lifespan == 30_100 && !reloaded.isRemoved(), "Floor overwrote the reloaded extension");
            require(expirations.get() == 1, "Reload must not trigger premature expiry");
            setAge(reloaded, 30_099);
            reloaded.tick();
            require(expirations.get() == 2 && reloaded.isRemoved(), "No further extension must allow eventual expiry");

            setAge(ordinary, 5_999);
            ordinary.tick();
            require(ordinary.isRemoved(), "Ordinary items must retain their 6,000-tick lifetime");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
            redHole.discard();
            reloaded.discard();
            ordinary.discard();
        }
    }

    public static void verifyThiefDrops(ServerLevel level, BlockPos pos) {
        requireWorld(level, pos);
        verifyThiefDeath(level, pos, false);
        verifyThiefDeath(level, pos, true);
    }

    public static void verifyBonusLootCancellation(ServerLevel level, BlockPos pos) {
        requireWorld(level, pos);
        require(level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT), "Golem fixture requires doMobLoot=true");
        verifyGolemDeath(level, pos, false, false);
        verifyGolemDeath(level, pos, true, false);
        verifyGolemDeath(level, pos, true, true);
    }

    private static void verifyGolemDeath(ServerLevel level, BlockPos pos, boolean enchanted, boolean cancel) {
        IronGolem victim = new IronGolem(EntityType.IRON_GOLEM, level);
        victim.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        victim.setNoAi(true);
        victim.setNoGravity(true);
        FakePlayer attacker = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "AuraLootQA"));
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        if (enchanted) {
            enchant(level, sword, AuraColor.YELLOW, AuraColor.VIOLET);
        }
        attacker.setItemInHand(InteractionHand.MAIN_HAND, sword);
        List<ItemEntity> spawnedDrops = new ArrayList<>();
        AtomicInteger events = new AtomicInteger();
        AtomicInteger capturedIron = new AtomicInteger();
        AtomicInteger capturedIronStacks = new AtomicInteger();
        AtomicInteger prematureSpawns = new AtomicInteger();
        Consumer<EntityJoinLevelEvent> spawned = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item
                && victim.getBoundingBox().inflate(2.0D).contains(item.position())) {
                spawnedDrops.add(item);
            }
        };
        Consumer<LivingDropsEvent> observe = event -> {
            if (event.getEntity() == victim) {
                events.incrementAndGet();
                prematureSpawns.set(spawnedDrops.size());
                capturedIron.set(ironCount(event.getDrops()));
                capturedIronStacks.set((int) event.getDrops().stream().filter(item -> item.getItem().is(Items.IRON_INGOT)).count());
                if (cancel) {
                    event.setCanceled(true);
                }
            }
        };
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, spawned);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDropsEvent.class, observe);
        try {
            require(level.addFreshEntity(victim), "Fixture golem could not spawn");
            // The vanilla table guarantees 3..5 iron per roll; no loot or event is injected.
            level.getRandom().setSeed(0xA07A2026L);
            victim.getRandom().setSeed(0xA07A2026L);
            require(victim.hurt(level.damageSources().playerAttack(attacker), 1_000.0F), "Golem killing hit rejected");
            require(victim.isDeadOrDying(), "Fixture hit did not kill golem");
            require(events.get() == 1, "Real golem death must dispatch one LivingDropsEvent");
            require(prematureSpawns.get() == 0, "Golem loot spawned before drop-event processing");
            int minimum = enchanted ? 6 : 3;
            int maximum = enchanted ? 10 : 5;
            require(capturedIronStacks.get() == (enchanted ? 2 : 1),
                "Golem must expose exactly one vanilla iron stack plus one bonus stack when enchanted, got " + capturedIronStacks.get());
            require(capturedIron.get() >= minimum && capturedIron.get() <= maximum,
                "Golem event iron count: expected " + minimum + ".." + maximum + ", got " + capturedIron.get());
            if (cancel) {
                require(spawnedDrops.isEmpty(), "Canceled golem death leaked ordinary or kaleidoscopic bonus loot");
            } else {
                require(ironCount(spawnedDrops) == capturedIron.get(), "Spawned iron disagrees with event collection");
                for (ItemEntity item : spawnedDrops) {
                    require(level.getEntity(item.getUUID()) == item && !item.isRemoved(), "Golem drop did not enter world");
                }
            }
            AuraQaObserverMod.LOGGER.info("[aura_qa_hooks] golem enchanted={} canceled={} capturedIron={} spawnedIron={}",
                enchanted, cancel, capturedIron.get(), ironCount(spawnedDrops));
        } finally {
            NeoForge.EVENT_BUS.unregister(observe);
            NeoForge.EVENT_BUS.unregister(spawned);
            spawnedDrops.forEach(ItemEntity::discard);
            victim.discard();
            attacker.discard();
        }
    }

    public static void verifyHarvestingIsolation(ServerLevel level, BlockPos pos) {
        requireWorld(level, pos);
        // Reject occupied fixtures rather than replacing world blocks or reaching an existing tree.
        for (BlockPos cursor : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 2, 2))) {
            require(level.hasChunkAt(cursor) && !level.isOutsideBuildHeight(cursor) && level.getBlockState(cursor).isAir(),
                "Harvesting fixture requires clear loaded space at " + cursor);
        }
        verifyLogBreak(level, pos, false);
        verifyLogBreak(level, pos, true);
    }

    private static void verifyLogBreak(ServerLevel level, BlockPos oak, boolean enchanted) {
        BlockPos connectedOak = oak.above();
        BlockPos diagonalBirch = oak.offset(1, 0, 1);
        FakePlayer player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "AuraHarvestQA"));
        List<ItemEntity> drops = new ArrayList<>();
        Consumer<EntityJoinLevelEvent> spawned = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity item
                && (item.getItem().is(Items.OAK_LOG) || item.getItem().is(Items.BIRCH_LOG))
                && item.blockPosition().closerThan(oak, 4.0D)) {
                drops.add(item);
            }
        };
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, spawned);
        try {
            player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            player.setPos(oak.getX() + 0.5D, oak.getY(), oak.getZ() - 1.5D);
            ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
            require(axe.is(AuraEnchantments.KALEIDOSCOPIC_ENCHANTABLE), "Diamond axe must be a valid kaleidoscopic target");
            if (enchanted) {
                enchant(level, axe, AuraColor.GREEN);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            require(level.setBlock(oak, Blocks.OAK_LOG.defaultBlockState(), 3), "Cannot place fixture oak");
            require(level.setBlock(connectedOak, Blocks.OAK_LOG.defaultBlockState(), 3), "Cannot place connected oak");
            require(level.setBlock(diagonalBirch, Blocks.BIRCH_LOG.defaultBlockState(), 3), "Cannot place diagonal birch");

            require(player.gameMode.destroyBlock(oak), "Real ServerPlayerGameMode.destroyBlock rejected fixture oak");
            require(level.getBlockState(oak).isAir(), "Primary oak was not removed");
            require(level.getBlockState(diagonalBirch).is(Blocks.BIRCH_LOG), "Diagonal birch was incorrectly harvested");
            require(enchanted ? level.getBlockState(connectedOak).isAir() : level.getBlockState(connectedOak).is(Blocks.OAK_LOG),
                "Face-connected oak must be harvested only with Green; removing all after-break dispatch is not a fix");
            AuraQaObserverMod.LOGGER.info("[aura_qa_hooks] harvesting Green={} primaryOak=removed connectedOak={} diagonalBirch=retained",
                enchanted, enchanted ? "removed" : "retained");
        } finally {
            NeoForge.EVENT_BUS.unregister(spawned);
            for (BlockPos target : List.of(oak, connectedOak, diagonalBirch)) {
                level.setBlock(target, Blocks.AIR.defaultBlockState(), 3);
            }
            drops.forEach(ItemEntity::discard);
            player.discard();
        }
    }

    private static void enchant(ServerLevel level, ItemStack stack, AuraColor... colors) {
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (AuraColor color : colors) {
            stack.enchant(registry.getOrThrow(AuraEnchantments.kaleidoscopic(color).orElseThrow()), 1);
        }
    }

    private static int ironCount(Iterable<ItemEntity> items) {
        int count = 0;
        for (ItemEntity item : items) {
            if (item.getItem().is(Items.IRON_INGOT)) {
                count += item.getItem().getCount();
            }
        }
        return count;
    }

    private static void verifyThiefDeath(ServerLevel level, BlockPos pos, boolean cancel) {
        Villager victim = villager(level, pos);
        FakePlayer attacker = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "AuraHookQA"));
        attacker.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AuraItems.SWORD_OF_THE_THIEF));
        ItemStack reward = new ItemStack(Items.DIAMOND, 3);
        reward.set(DataComponents.CUSTOM_NAME, Component.literal("Aura hook QA " + UUID.randomUUID()));
        MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD), reward.copy(), 1, 0, 0.0F));
        victim.setOffers(offers);
        List<ItemEntity> spawnedRewards = new ArrayList<>();
        AtomicInteger observedDrops = new AtomicInteger();

        Consumer<EntityJoinLevelEvent> spawned = event -> {
            if (event.getLevel() == level && event.getEntity() instanceof ItemEntity entity
                && ItemStack.isSameItemSameComponents(entity.getItem(), reward)) {
                spawnedRewards.add(entity);
            }
        };
        Consumer<LivingDropsEvent> seedRoll = event -> {
            if (event.getEntity() == victim) {
                // Establish a winning RNG precondition; Aura still creates the reward itself.
                seedWinningThiefRoll(level);
            }
        };
        Consumer<LivingDropsEvent> observe = event -> {
            if (event.getEntity() != victim) {
                return;
            }
            observedDrops.incrementAndGet();
            require(spawnedRewards.isEmpty(), "Thief reward escaped the event collection before cancellation");
            List<ItemEntity> rewards = event.getDrops().stream()
                .filter(entity -> ItemStack.isSameItemSameComponents(entity.getItem(), reward)).toList();
            require(rewards.size() == 1, "Real death must place exactly one Thief reward in LivingDropsEvent");
            require(rewards.getFirst().getItem().getCount() == 3, "Trade result count was not preserved");
            require(rewards.getFirst().getItem() != victim.getOffers().getFirst().getResult(),
                "Dropped trade must be copied, not aliased");
            if (cancel) {
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, spawned);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LivingDropsEvent.class, seedRoll);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, LivingDropsEvent.class, observe);
        try {
            require(level.addFreshEntity(victim), "Fixture villager could not spawn in the test world");
            require(victim.hurt(level.damageSources().playerAttack(attacker), 100.0F), "Fixture killing hit rejected");
            require(victim.isDeadOrDying(), "Fixture hit did not kill villager");
            require(observedDrops.get() == 1, "Native death did not dispatch LivingDropsEvent exactly once");
            require(spawnedRewards.size() == (cancel ? 0 : 1), "Thief reward spawn count disagrees with cancellation");
            for (ItemEntity rewardEntity : spawnedRewards) {
                require(level.getEntity(rewardEntity.getUUID()) == rewardEntity && !rewardEntity.isRemoved(),
                    "Uncanceled reward must actually enter the world, not only fire EntityJoinLevelEvent");
            }
            require(victim.getOffers().getFirst().getResult().getCount() == 3, "Reward mutated the trade offer");
        } finally {
            NeoForge.EVENT_BUS.unregister(observe);
            NeoForge.EVENT_BUS.unregister(seedRoll);
            NeoForge.EVENT_BUS.unregister(spawned);
            spawnedRewards.forEach(ItemEntity::discard);
            victim.discard();
            attacker.discard();
        }
    }

    private static void seedWinningThiefRoll(ServerLevel level) {
        for (int attempt = 0; attempt < 1_024; attempt++) {
            long seed = attempt * 1_000_003L;
            level.getRandom().setSeed(seed);
            if (level.getRandom().nextInt(4) == 0) {
                level.getRandom().setSeed(seed);
                return;
            }
        }
        throw new AssertionError("Could not establish winning Thief RNG precondition");
    }

    private static Villager villager(ServerLevel level, BlockPos pos) {
        Villager entity = new Villager(EntityType.VILLAGER, level);
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.setNoAi(true);
        entity.setNoGravity(true);
        return entity;
    }

    private static ItemEntity item(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, stack);
        entity.setNoGravity(true);
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        return entity;
    }

    private static void setAge(ItemEntity entity, int age) {
        CompoundTag tag = entity.saveWithoutId(new CompoundTag());
        tag.putShort("Age", (short) age);
        entity.load(tag);
    }

    private static void requireWorld(ServerLevel level, BlockPos pos) {
        require(level.getServer().isSameThread(), "Runtime assertions must execute on the server thread");
        require(level.hasChunkAt(pos), "Fixture position must already be loaded");
    }

    private static void near(float expected, float actual, String message) {
        require(Float.isFinite(actual) && Math.abs(expected - actual) < 0.0001F,
            message + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
