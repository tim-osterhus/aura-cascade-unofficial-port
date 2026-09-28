package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.compat.AuraAccessoryInventory;
import pixlepix.auracascade.enchantment.AuraEnchantments;
import pixlepix.auracascade.fairy.AuraFairyEntity;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.fairy.FairySystem;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.RingOfBindingItem;
import pixlepix.auracascade.parity.AuraColor;

/** Finite, opt-in checks against transformed hooks in a disposable packaged server. */
public final class TargetRemainingHookProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.remainingHooks";
    private static final BlockPos ORE = new BlockPos(800, 180, 640);
    private static final BlockPos LOOT = new BlockPos(832, 180, 640);
    private static final BlockPos SPAWN = new BlockPos(864, 180, 640);
    private static final BlockPos HOLE = new BlockPos(928, 180, 640);
    private static final List<BlockPos> SITES = List.of(ORE, LOOT, SPAWN, HOLE, HOLE.east(32));

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)) return;
        String configured = System.getProperty(PREFIX + ".output", "");
        if (configured.isBlank()) throw new IllegalStateException("Remaining-hooks report path is required");
        Path output = Path.of(configured).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalStateException("Use a fresh remaining-hooks report path");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> start(server, output));
    }

    private static JsonObject report() {
        JsonObject result = new JsonObject();
        result.addProperty("probe", "remainingHooks");
        result.addProperty("success", false);
        result.addProperty("complete", false);
        result.addProperty("scope", "Seeded blocks, chicken, tool components, synthetic loaded player/fairy, and dropped Red Hole; native transformed hooks on a disposable dedicated server");
        result.addProperty("scarerBoundary", "Calls transformed vanilla natural-spawn position eligibility; does not count naturally spawned mobs or establish T07 rate");
        result.addProperty("redHoleBoundary", "Two world-tick blasts and frozen-time negative; not full 30000-tick expiry, save/reload, or exact radius");
        return result;
    }

    private static void start(MinecraftServer server, Path output) {
        JsonObject result = report();
        write(output, result, true);
        List<BlockPos> forced = new ArrayList<>();
        List<BlockPos> shaft = new ArrayList<>();
        try {
            require(server.isDedicatedServer(), "Dedicated server required");
            for (BlockPos site : SITES) {
                server.overworld().setChunkForced(site.getX() >> 4, site.getZ() >> 4, true);
                forced.add(site);
                server.overworld().getChunkAt(site);
            }
            buildScarerShaft(server.overworld(), shaft);
            int[] ticks = {0};
            ServerTickEvents.END_SERVER_TICK.register(ticking -> {
                if (ticking == server && ++ticks[0] == 20) run(server, output, forced, shaft);
            });
        } catch (Exception | AssertionError error) {
            result.addProperty("error", describe(error));
            clearShaft(server.overworld(), shaft);
            unforce(server.overworld(), forced);
            result.addProperty("complete", true);
            write(output, result, false);
        }
    }

    private static void run(MinecraftServer server, Path output, List<BlockPos> forced, List<BlockPos> shaft) {
        JsonObject result = report();
        try {
            packagedOrigin(result);
            ServerLevel level = server.overworld();
            JsonObject checks = new JsonObject();
            boolean passed = check(checks, "oreDropsAndFinalDurability", () -> ore(level, server));
            passed &= check(checks, "deathLoot", () -> deathLoot(level, server));
            passed &= check(checks, "scarerNaturalSpawnEligibility", () -> scarer(level, server, shaft));
            clearShaft(level, shaft);
            result.add("assertions", checks);
            result.addProperty("preflightPassed", passed);
            new RedHoleRun(server, level, output, forced, result, checks, passed).begin();
            return;
        } catch (Exception | AssertionError error) {
            result.addProperty("error", describe(error));
        }
        clearShaft(server.overworld(), shaft);
        unforce(server.overworld(), forced);
        result.addProperty("complete", true);
        write(output, result, false);
    }

    private static void ore(ServerLevel level, MinecraftServer server) {
        ServerPlayer miner = player(server, level, "AuraRemainingOreQA");
        miner.setPos(ORE.getX() + 0.5D, ORE.getY() + 2.0D, ORE.getZ() + 0.5D);
        try {
            ItemStack ordinary = new ItemStack(Items.DIAMOND_PICKAXE);
            miner.setItemSlot(EquipmentSlot.MAINHAND, ordinary);
            breakOre(level, miner, ORE, false);
            require(itemCount(level, ORE, Items.RAW_IRON) == 1, "Unenchanted ore must produce one raw iron");
            clearItems(level, ORE);

            ItemStack enchanted = new ItemStack(Items.DIAMOND_PICKAXE);
            enchant(level, enchanted, AuraColor.RED, 4);
            enchant(level, enchanted, AuraColor.YELLOW, 4);
            miner.setItemSlot(EquipmentSlot.MAINHAND, enchanted);
            breakOre(level, miner, ORE, true);
            require(itemCount(level, ORE, Items.IRON_INGOT) == 2, "Guaranteed Red+Yellow conversion must produce two ingots");
            require(itemCount(level, ORE, Items.RAW_IRON) == 0, "Converted ore also dropped raw iron");
            clearItems(level, ORE);

            ItemStack finalPoint = new ItemStack(Items.DIAMOND_PICKAXE);
            enchant(level, finalPoint, AuraColor.RED, 4);
            enchant(level, finalPoint, AuraColor.YELLOW, 4);
            finalPoint.setDamageValue(finalPoint.getMaxDamage() - 1);
            miner.setItemSlot(EquipmentSlot.MAINHAND, finalPoint);
            breakOre(level, miner, ORE, true);
            require(miner.getMainHandItem().isEmpty(), "Final-durability pickaxe did not break");
            require(itemCount(level, ORE, Items.IRON_INGOT) == 2, "Final-durability break lost pre-wear conversion");
        } finally {
            level.setBlockAndUpdate(ORE, Blocks.AIR.defaultBlockState());
            clearItems(level, ORE);
        }
    }

    private static void breakOre(ServerLevel level, ServerPlayer miner, BlockPos pos, boolean converted) {
        require(level.getBlockState(pos).isAir(), "Ore fixture must start empty");
        require(level.setBlockAndUpdate(pos, Blocks.IRON_ORE.defaultBlockState()), "Could not seed iron ore");
        boolean outerBreak = miner.gameMode.destroyBlock(pos);
        require(converted ? !outerBreak : outerBreak,
            "Unexpected native break return for " + (converted ? "intercepted" : "plain") + " ore");
        require(level.getBlockState(pos).isAir(), "Ore remained after native break");
        require(itemCount(level, pos, converted ? Items.IRON_INGOT : Items.RAW_IRON) > 0,
            "Native break produced no expected drop");
    }

    private static void deathLoot(ServerLevel level, MinecraftServer server) {
        ServerPlayer attacker = player(server, level, "AuraRemainingLootQA");
        attacker.setPos(LOOT.getX() + 0.5D, LOOT.getY() + 2.0D, LOOT.getZ() + 0.5D);
        try {
            int plain = killChicken(level, attacker);
            require(plain == 1, "Plain chicken death must drop exactly one raw chicken, got " + plain);
            clearItems(level, LOOT);
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            enchant(level, sword, AuraColor.YELLOW, 2);
            enchant(level, sword, AuraColor.VIOLET, 2);
            attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);
            int bonus = killChicken(level, attacker);
            require(bonus >= 2 && bonus <= 4,
                "Yellow+Violet must add one Looting-II chicken-table roll (total 2..4), got " + bonus);
        } finally {
            clearItems(level, LOOT);
        }
    }

    private static int killChicken(ServerLevel level, ServerPlayer attacker) {
        var chicken = EntityType.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
        require(chicken != null, "Could not create chicken fixture");
        chicken.setPos(LOOT.getX() + 0.5D, LOOT.getY() + 1.0D, LOOT.getZ() + 0.5D);
        require(level.addFreshEntity(chicken), "Could not admit chicken to world");
        try {
            require(chicken.hurtServer(level, level.damageSources().playerAttack(attacker), 100.0F),
                "Native chicken damage was rejected");
            require(!chicken.isAlive(), "Chicken survived fatal native damage");
            return itemCount(level, LOOT, Items.CHICKEN);
        } finally {
            chicken.discard();
        }
    }

    private static void buildScarerShaft(ServerLevel level, List<BlockPos> built) {
        // Build before the 20-tick delay so the threaded light engine can settle.
        for (int y = -1; y <= 3; y++) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (y >= 0 && y < 3 && dx == 0 && dz == 0) continue;
                BlockPos pos = SPAWN.offset(dx, y, dz);
                require(level.getBlockState(pos).isAir(), "Scarer shaft not empty at " + pos);
                require(level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState()), "Could not build Scarer shaft");
                built.add(pos);
            }
        }
    }

    private static void clearShaft(ServerLevel level, List<BlockPos> built) {
        for (BlockPos pos : built) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        built.clear();
    }

    private static void scarer(ServerLevel level, MinecraftServer server, List<BlockPos> shaft) throws Exception {
        ServerPlayer owner = player(server, level, "AuraRemainingScarerQA");
        owner.setPos(SPAWN.getX() + 4.5D, SPAWN.getY(), SPAWN.getZ() + 0.5D);
        Map<UUID, ServerPlayer> playerMap = playerMap(server.getPlayerList());
        require(!playerMap.containsKey(owner.getUUID()), "Scarer fixture UUID already online");
        Difficulty previousDifficulty = server.getWorldData().getDifficulty();
        try {
            server.setDifficulty(Difficulty.NORMAL, true);
            require(level.getDifficulty() == Difficulty.NORMAL, "Scarer fixture did not leave Peaceful difficulty");
            MobSpawnSettings.SpawnerData zombie = level.getChunkSource().getGenerator()
                .getMobsAt(level.getBiome(SPAWN), level.structureManager(), MobCategory.MONSTER, SPAWN)
                .unwrap().stream().map(entry -> entry.value())
                .filter(data -> data.type() == EntityType.ZOMBIE).findFirst().orElseThrow();
            Method eligibility = naturalEligibility();
            require(validSpawn(eligibility, level, zombie), "Vanilla natural zombie position not valid in shaft"
                + " (sky=" + level.getBrightness(LightLayer.SKY, SPAWN)
                + ", block=" + level.getBrightness(LightLayer.BLOCK, SPAWN) + ")");
            playerMap.put(owner.getUUID(), owner);
            level.addNewPlayer(owner);
            ItemStack ring = new ItemStack(AuraItems.RING_OF_BINDING);
            for (int i = 0; i < 8; i++) require(RingOfBindingItem.bindCharm(ring, FairyRole.SCARER), "Scarer binding failed");
            AuraAccessoryInventory.set(owner, AuraAccessoryInventory.FIRST_RING, ring);
            FairySystem.syncPlayerFairies(owner);
            long count = level.getEntitiesOfClass(AuraFairyEntity.class, new AABB(SPAWN).inflate(20.0D),
                fairy -> fairy.role() == FairyRole.SCARER && FairySystem.isAuthorizedForRole(fairy, owner)).size();
            require(count == 8, "Expected eight authorized world-admitted Scarers, got " + count);
            int denied = 0;
            for (int i = 0; i < 100; i++) if (!validSpawn(eligibility, level, zombie)) denied++;
            require(denied >= 12 && denied <= 60, "Scarer transformed eligibility denial out of predeclared 12..60/100 range: " + denied);
            AuraAccessoryInventory.set(owner, AuraAccessoryInventory.FIRST_RING, ItemStack.EMPTY);
            FairySystem.syncPlayerFairies(owner);
            for (int i = 0; i < 20; i++) require(validSpawn(eligibility, level, zombie),
                "Unequipped natural position denied at control " + i);
        } finally {
            try {
                AuraAccessoryInventory.set(owner, AuraAccessoryInventory.FIRST_RING, ItemStack.EMPTY);
                if (playerMap.get(owner.getUUID()) == owner) {
                    FairySystem.syncPlayerFairies(owner);
                    playerMap.remove(owner.getUUID());
                }
                level.removePlayerImmediately(owner, Entity.RemovalReason.DISCARDED);
            } finally {
                server.setDifficulty(previousDifficulty, true);
                clearShaft(level, shaft);
            }
        }
    }

    private static boolean validSpawn(Method method, ServerLevel level, MobSpawnSettings.SpawnerData data) throws Exception {
        return Boolean.TRUE.equals(method.invoke(null, level, MobCategory.MONSTER, level.structureManager(),
            level.getChunkSource().getGenerator(), data, new BlockPos.MutableBlockPos(SPAWN.getX(), SPAWN.getY(), SPAWN.getZ()),
            900.0D));
    }

    private static Method naturalEligibility() throws Exception {
        String descriptor = "(Lnet/minecraft/class_3218;Lnet/minecraft/class_1311;Lnet/minecraft/class_5138;"
            + "Lnet/minecraft/class_2794;Lnet/minecraft/class_5483$class_1964;Lnet/minecraft/class_2338$class_2339;D)Z";
        String name = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary",
            "net.minecraft.class_1948", "method_24934", descriptor);
        Method method = NaturalSpawner.class.getDeclaredMethod(name, ServerLevel.class, MobCategory.class,
            net.minecraft.world.level.StructureManager.class, net.minecraft.world.level.chunk.ChunkGenerator.class,
            MobSpawnSettings.SpawnerData.class, BlockPos.MutableBlockPos.class, double.class);
        method.setAccessible(true);
        return method;
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, ServerPlayer> playerMap(PlayerList list) throws Exception {
        String name = FabricLoader.getInstance().getMappingResolver().mapFieldName("intermediary",
            "net.minecraft.class_3324", "field_14354", "Ljava/util/Map;");
        Field field = PlayerList.class.getDeclaredField(name);
        field.setAccessible(true);
        return (Map<UUID, ServerPlayer>) field.get(list);
    }

    private static ServerPlayer player(MinecraftServer server, ServerLevel level, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);
        ServerPlayer result = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        result.connection = new ServerGamePacketListenerImpl(server, new Connection(PacketFlow.SERVERBOUND), result,
            CommonListenerCookie.createInitial(profile, false));
        result.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        require(result.connection.hasClientLoaded(), "Synthetic player-loaded handshake failed");
        result.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return result;
    }

    private static void enchant(ServerLevel level, ItemStack stack, AuraColor color, int power) {
        var key = AuraEnchantments.kaleidoscopic(color).orElseThrow();
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), power);
    }

    private static int itemCount(ServerLevel level, BlockPos pos, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8.0D), entity -> entity.getItem().is(item))
            .stream().mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static void clearItems(ServerLevel level, BlockPos pos) {
        level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(8.0D)).forEach(Entity::discard);
    }

    private static final class RedHoleRun {
        private final MinecraftServer server;
        private final ServerLevel level;
        private final Path output;
        private final List<BlockPos> forced;
        private final JsonObject result;
        private final JsonObject checks;
        private final boolean preflightPassed;
        private final BlockPos near = HOLE.east(2);
        private final BlockPos far = HOLE.east(32);
        private ItemEntity marker;
        private UUID markerId;
        private int phase;
        private int frozenServerTicks;
        private int elapsedServerTicks;
        private long firstBlastTime;
        private int firstAge;
        private boolean finished;

        private RedHoleRun(MinecraftServer server, ServerLevel level, Path output, List<BlockPos> forced,
                           JsonObject result, JsonObject checks, boolean preflightPassed) {
            this.server = server;
            this.level = level;
            this.output = output;
            this.forced = forced;
            this.result = result;
            this.checks = checks;
            this.preflightPassed = preflightPassed;
        }

        private void begin() {
            try {
                require(level.getBlockState(HOLE).isAir() && level.getBlockState(near).isAir()
                    && level.getBlockState(far).isAir(), "Red Hole fixture not empty");
                require(level.setBlockAndUpdate(near, Blocks.STONE.defaultBlockState()), "Could not seed near witness");
                require(level.setBlockAndUpdate(far, Blocks.STONE.defaultBlockState()), "Could not seed far witness");
                ServerTickEvents.END_SERVER_TICK.register(ticking -> {
                    if (ticking != server || finished) return;
                    try {
                        step();
                    } catch (Exception | AssertionError error) {
                        checks.addProperty("redHoleRepeatedBlastAndFrozenTime", "FAIL: " + describe(error));
                        finish(false);
                    }
                });
            } catch (Exception | AssertionError error) {
                checks.addProperty("redHoleRepeatedBlastAndFrozenTime", "FAIL: " + describe(error));
                finish(false);
            }
        }

        private void step() {
            require(++elapsedServerTicks <= 350, "Red Hole cadence/freeze timed out after 350 server ticks");
            long time = level.getGameTime();
            if (phase == 0) {
                if (time % 100L != 99L) return;
                marker = new ItemEntity(level, HOLE.getX() + 0.5D, HOLE.getY() + 0.5D, HOLE.getZ() + 0.5D,
                    new ItemStack(AuraItems.PORTABLE_RED_HOLE));
                marker.setNoGravity(true);
                marker.setDeltaMovement(0.0D, 0.0D, 0.0D);
                marker.setPickUpDelay(32767);
                require(level.addFreshEntity(marker), "Could not admit dropped Red Hole");
                markerId = marker.getUUID();
                phase = 1;
            } else if (phase == 1) {
                require(time % 100L == 0L, "First blast did not reach cadence boundary");
                requireBlast("first");
                firstBlastTime = time;
                firstAge = marker.getAge();
                require(firstAge == 0, "Eruption did not skip item age increment");
                require(level.setBlockAndUpdate(near, Blocks.STONE.defaultBlockState()), "Could not reset near witness");
                server.tickRateManager().setFrozen(true);
                phase = 2;
            } else if (phase == 2) {
                require(time == firstBlastTime, "World time advanced while frozen");
                require(level.getBlockState(near).is(Blocks.STONE), "Red Hole erupted during frozen ticks");
                require(marker.getAge() == firstAge, "Dropped item aged during frozen ticks");
                requireMarker();
                if (++frozenServerTicks == 5) {
                    server.tickRateManager().setFrozen(false);
                    phase = 3;
                }
            } else if (phase == 3) {
                requireMarker();
                if (time < firstBlastTime + 100L) {
                    require(level.getBlockState(near).is(Blocks.STONE), "Red Hole erupted before next cadence");
                    return;
                }
                require(time == firstBlastTime + 100L, "Second cadence was skipped");
                requireBlast("second");
                require(marker.getAge() == firstAge + 99, "Expected 99 age ticks between eruptions");
                checks.addProperty("redHoleRepeatedBlastAndFrozenTime", "PASS");
                result.addProperty("firstBlastGameTime", firstBlastTime);
                result.addProperty("secondBlastGameTime", time);
                result.addProperty("markerUuid", markerId.toString());
                finish(true);
            }
        }

        private void requireBlast(String label) {
            requireMarker();
            require(level.getBlockState(near).isAir(), label + " blast did not destroy near stone");
            require(level.getBlockState(far).is(Blocks.STONE), label + " blast reached 32-block witness");
        }

        private void requireMarker() {
            require(marker != null && !marker.isRemoved() && marker.getUUID().equals(markerId)
                && marker.getItem().is(AuraItems.PORTABLE_RED_HOLE) && marker.getItem().getCount() == 1,
                "Dropped Red Hole marker did not persist");
        }

        private void finish(boolean passed) {
            if (finished) return;
            finished = true;
            server.tickRateManager().setFrozen(false);
            if (marker != null) marker.discard();
            level.setBlockAndUpdate(near, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(far, Blocks.AIR.defaultBlockState());
            unforce(level, forced);
            result.addProperty("success", preflightPassed && passed);
            result.addProperty("complete", true);
            write(output, result, false);
        }
    }

    private static void packagedOrigin(JsonObject result) throws Exception {
        var loader = FabricLoader.getInstance();
        String namespace = loader.getMappingResolver().getCurrentRuntimeNamespace();
        require("intermediary".equals(namespace), "Packaged intermediary runtime required");
        var origin = loader.getModContainer("aura").orElseThrow().getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1, "Expected one Aura origin JAR");
        Path jar = origin.getPaths().getFirst().toAbsolutePath().normalize();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"), "Aura origin is not a JAR");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        result.addProperty("runtimeNamespace", namespace);
        result.addProperty("auraOrigin", jar.toString());
        result.addProperty("auraSha256", hash);
        require(hash.equalsIgnoreCase(System.getProperty(PREFIX + ".sha256", "")), "Candidate hash mismatch");
    }

    private static void unforce(ServerLevel level, List<BlockPos> sites) {
        for (BlockPos site : sites) level.setChunkForced(site.getX() >> 4, site.getZ() >> 4, false);
    }

    private static void write(Path output, JsonObject result, boolean fresh) {
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(result),
                fresh ? StandardOpenOption.CREATE_NEW : StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot write remaining-hooks report", error);
        }
    }

    private static boolean check(JsonObject checks, String name, Check check) {
        try {
            check.run();
            checks.addProperty(name, "PASS");
            return true;
        } catch (Exception | AssertionError error) {
            checks.addProperty(name, "FAIL: " + describe(error));
            return false;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String describe(Throwable error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }

    @FunctionalInterface
    private interface Check {
        void run() throws Exception;
    }
}
