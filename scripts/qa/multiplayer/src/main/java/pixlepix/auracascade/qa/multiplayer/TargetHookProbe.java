package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
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

/** Opt-in transformed-hook checks on a disposable packaged dedicated server. */
public final class TargetHookProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.targetHooks";
    private static final List<BlockPos> BLAST_SITES = List.of(
        new BlockPos(640, 180, 640), new BlockPos(680, 180, 640), new BlockPos(720, 180, 640));

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)) return;
        String configuredOutput = System.getProperty(PREFIX + ".output", "");
        if (configuredOutput.isBlank()) throw new IllegalStateException("Target hook report path is required");
        Path output = Path.of(configuredOutput).toAbsolutePath().normalize();
        if (Files.exists(output)) throw new IllegalStateException("Use a fresh target hook report path");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> start(server, output));
    }

    private static JsonObject report() {
        JsonObject report = new JsonObject();
        report.addProperty("probe", "targetHooks");
        report.addProperty("success", false);
        report.addProperty("complete", false);
        report.addProperty("scope", "Live damage, food, explosion, mining, and fall hooks with temporary world-admitted players and fairies");
        report.add("notRun", new GsonBuilder().create().toJsonTree(List.of(
            "ore drops", "final durability", "Scarer natural spawning", "death loot",
            "Red Hole repeated blasts and freeze"
        )));
        return report;
    }

    private static void start(MinecraftServer server, Path output) {
        JsonObject pending = report();
        write(output, pending, true);
        List<BlockPos> forced = new ArrayList<>();
        try {
            require(server.isDedicatedServer(), "Dedicated server required");
            ServerLevel level = server.overworld();
            for (BlockPos site : BLAST_SITES) {
                if (level.setChunkForced(site.getX() >> 4, site.getZ() >> 4, true)) forced.add(site);
                level.getChunkAt(site);
            }
            int[] ticks = {0};
            ServerTickEvents.END_SERVER_TICK.register(tickingServer -> {
                if (tickingServer != server || ++ticks[0] != 20) return;
                run(server, output, forced);
            });
        } catch (Exception | AssertionError error) {
            pending.addProperty("error", describe(error));
            try {
                unforce(server.overworld(), forced);
            } catch (Exception | AssertionError cleanupError) {
                pending.addProperty("cleanupError", describe(cleanupError));
            }
            pending.addProperty("complete", true);
            write(output, pending, false);
        }
    }

    private static void unforce(ServerLevel level, List<BlockPos> sites) {
        for (BlockPos site : sites) level.setChunkForced(site.getX() >> 4, site.getZ() >> 4, false);
    }

    private static void run(MinecraftServer server, Path output, List<BlockPos> forced) {
        JsonObject report = report();
        try {
            require(server.isDedicatedServer(), "Dedicated server required");
            packagedOrigin(report);
            ServerLevel level = server.overworld();
            JsonObject assertions = new JsonObject();
            boolean passed = check(assertions, "damageAcceptedRejectedAndColorCombo", () -> damage(server, level));
            passed &= check(assertions, "shatteredStoneTerrainAndOre", () -> explosion(server, level));
            passed &= check(assertions, "forbiddenFruitFinishAndCancel", () -> food(server, level));
            passed &= check(assertions, "diggerAndEnchantmentMiningComposition", () -> mining(server, level));
            passed &= check(assertions, "gliderFallDistance", () -> fall(server, level));
            report.add("assertions", assertions);
            report.addProperty("success", passed);
        } catch (Exception | AssertionError error) {
            report.addProperty("error", describe(error));
        } finally {
            try {
                unforce(server.overworld(), forced);
            } catch (Exception | AssertionError cleanupError) {
                report.addProperty("cleanupError", describe(cleanupError));
                report.addProperty("success", false);
            }
        }
        report.addProperty("complete", true);
        write(output, report, false);
    }

    private static void write(Path output, JsonObject report, boolean fresh) {
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report),
                fresh ? StandardOpenOption.CREATE_NEW : StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        } catch (Exception error) {
            throw new IllegalStateException("Cannot write target hook evidence", error);
        }
    }

    private static void packagedOrigin(JsonObject report) throws Exception {
        var loader = FabricLoader.getInstance();
        String namespace = loader.getMappingResolver().getCurrentRuntimeNamespace();
        require("intermediary".equals(namespace), "Packaged intermediary runtime required");
        var origin = loader.getModContainer("aura").orElseThrow().getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1,
            "Expected one Aura origin JAR");
        Path jar = origin.getPaths().getFirst().toAbsolutePath().normalize();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"), "Aura origin is not a JAR");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        report.addProperty("runtimeNamespace", namespace);
        report.addProperty("auraOrigin", jar.toString());
        report.addProperty("auraSha256", hash);
        require(hash.equalsIgnoreCase(System.getProperty(PREFIX + ".sha256", "")), "Candidate hash mismatch");
    }

    private static void damage(MinecraftServer server, ServerLevel level) {
        ServerPlayer attacker = player(server, level, "AuraDamageQA");
        float plain = damageTaken(level, attacker, 6.0F, false);
        requireNear(6.0F, plain, "plain accepted player damage");

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        enchant(level, sword, AuraColor.VIOLET, 2);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);
        float violet = damageTaken(level, attacker, 6.0F, false);
        requireNear(7.0F, violet, "Violet outgoing damage through hurtServer");

        enchant(level, sword, AuraColor.GREEN, 2);
        attacker.setItemSlot(EquipmentSlot.MAINHAND, sword);
        float combo = damageTaken(level, attacker, 6.0F, false);
        requireNear(5.0F, combo, "Green+Violet combo through hurtServer");
        float rejected = damageTaken(level, attacker, 6.0F, true);
        requireNear(0.0F, rejected, "invulnerable victim rejection");
    }

    private static float damageTaken(ServerLevel level, ServerPlayer attacker, float amount, boolean invulnerable) {
        LivingEntity victim = EntityType.PIG.create(level, EntitySpawnReason.LOAD);
        require(victim != null, "Pig damage control could not be created");
        victim.setInvulnerable(invulnerable);
        float before = victim.getHealth();
        boolean accepted = victim.hurtServer(level, level.damageSources().playerAttack(attacker), amount);
        require(accepted != invulnerable, "Unexpected hurtServer acceptance for invulnerable=" + invulnerable);
        return before - victim.getHealth();
    }

    private static void enchant(ServerLevel level, ItemStack stack, AuraColor color, int power) {
        var key = AuraEnchantments.kaleidoscopic(color).orElseThrow();
        var holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        stack.enchant(holder, power);
    }

    private static void mining(MinecraftServer server, ServerLevel level) {
        ServerPlayer miner = player(server, level, "AuraDiggerQA");
        miner.setPos(BLAST_SITES.get(1).getX() + 0.5D, 182.0D, BLAST_SITES.get(1).getZ() + 0.5D);
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        miner.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
        float plain = miner.getDestroySpeed(Blocks.STONE.defaultBlockState());
        require(plain > 0.0F, "Plain mining control must have positive speed");
        enchant(level, pickaxe, AuraColor.ORANGE, 2);
        miner.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
        float enchanted = miner.getDestroySpeed(Blocks.STONE.defaultBlockState());
        requireNear(plain * 1.15F * 1.15F, enchanted, "Orange II mining speed through getDestroySpeed");

        try {
            equipBoundRing(miner, FairyRole.DIGGER, FairyRole.DIGGER);
            level.addNewPlayer(miner);
            FairySystem.syncPlayerFairies(miner);
            requireFairies(level, miner, FairyRole.DIGGER, 2);
            float combined = miner.getDestroySpeed(Blocks.STONE.defaultBlockState());
            requireNear(enchanted * 1.08F, combined, "Digger and Orange composition through getDestroySpeed");
        } finally {
            removeRolePlayer(level, miner);
        }
    }

    private static void fall(MinecraftServer server, ServerLevel level) {
        float fullDistance = fallDamage(level, player(server, level, "AuraFallFullQA"), 10.0D);
        float halfDistance = fallDamage(level, player(server, level, "AuraFallHalfQA"), 5.0D);
        require(fullDistance > halfDistance && halfDistance > 0.0F,
            "Unbound fall controls must have distinct positive damage");

        ServerPlayer glider = player(server, level, "AuraGliderQA");
        glider.setPos(BLAST_SITES.get(1).getX() + 0.5D, 182.0D, BLAST_SITES.get(1).getZ() + 0.5D);
        try {
            equipBoundRing(glider, FairyRole.GLIDER);
            level.addNewPlayer(glider);
            FairySystem.syncPlayerFairies(glider);
            requireFairies(level, glider, FairyRole.GLIDER, 1);
            requireNear(halfDistance, fallDamage(level, glider, 10.0D),
                "One Glider halves distance through causeFallDamage");
        } finally {
            removeRolePlayer(level, glider);
        }
    }

    private static float fallDamage(ServerLevel level, ServerPlayer player, double distance) {
        float before = player.getHealth();
        require(player.causeFallDamage(distance, 1.0F, level.damageSources().fall()),
            "Fall damage control was rejected at distance " + distance);
        return before - player.getHealth();
    }

    private static void equipBoundRing(ServerPlayer player, FairyRole... roles) {
        ItemStack ring = new ItemStack(AuraItems.RING_OF_BINDING);
        for (FairyRole role : roles) require(RingOfBindingItem.bindCharm(ring, role), "Could not bind " + role);
        AuraAccessoryInventory.set(player, AuraAccessoryInventory.FIRST_RING, ring);
    }

    private static void requireFairies(ServerLevel level, ServerPlayer player, FairyRole role, int expected) {
        long count = level.getEntitiesOfClass(AuraFairyEntity.class, player.getBoundingBox().inflate(20.0D),
            fairy -> fairy.role() == role && FairySystem.isAuthorizedForRole(fairy, player)).size();
        require(count == expected, "Expected " + expected + " authorized " + role + " fairies, found " + count);
    }

    private static void removeRolePlayer(ServerLevel level, ServerPlayer player) {
        try {
            AuraAccessoryInventory.set(player, AuraAccessoryInventory.FIRST_RING, ItemStack.EMPTY);
            FairySystem.syncPlayerFairies(player);
        } finally {
            level.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void explosion(MinecraftServer server, ServerLevel level) {
        BlockPos control = BLAST_SITES.get(0);
        BlockPos protectedSite = BLAST_SITES.get(1);
        BlockPos unequippedSite = BLAST_SITES.get(2);
        List<BlockPos> placed = new ArrayList<>();
        ServerPlayer wearer = player(server, level, "AuraBlastQA");
        try {
            blast(level, control, placed);
            require(level.getBlockState(control).isAir() && level.getBlockState(control.east()).isAir(),
                "Unprotected blast must destroy adjacent terrain and iron ore");

            wearer.setPos(protectedSite.getX() + 0.5D, protectedSite.getY() + 2.0D, protectedSite.getZ() + 0.5D);
            wearer.setInvulnerable(true);
            AuraAccessoryInventory.set(wearer, AuraAccessoryInventory.FIRST_RING,
                new ItemStack(AuraItems.RING_OF_SHATTERED_STONE));
            level.addNewPlayer(wearer);
            require(level.getEntitiesOfClass(Player.class,
                bounds(protectedSite, 3.0D), candidate -> candidate == wearer).size() == 1,
                "Synthetic ring wearer was not admitted to the live world entity query");
            blast(level, protectedSite, placed);
            require(level.getBlockState(protectedSite).isAir(), "Ring must not spare stone terrain");
            require(level.getBlockState(protectedSite.east()).is(Blocks.IRON_ORE), "Ring must spare iron ore");

            AuraAccessoryInventory.set(wearer, AuraAccessoryInventory.FIRST_RING, ItemStack.EMPTY);
            wearer.setPos(unequippedSite.getX() + 0.5D, unequippedSite.getY() + 2.0D, unequippedSite.getZ() + 0.5D);
            blast(level, unequippedSite, placed);
            require(level.getBlockState(unequippedSite).isAir()
                && level.getBlockState(unequippedSite.east()).isAir(),
                "Physically unequipped control must lose ore protection");
        } finally {
            level.removePlayerImmediately(wearer, Entity.RemovalReason.DISCARDED);
            for (BlockPos pos : placed) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            for (BlockPos origin : List.of(control, protectedSite, unequippedSite)) {
                level.getEntitiesOfClass(ItemEntity.class, bounds(origin, 10.0D)).forEach(Entity::discard);
            }
        }
    }

    private static AABB bounds(BlockPos origin, double radius) {
        return new AABB(origin.getX(), origin.getY(), origin.getZ(),
            origin.getX() + 1, origin.getY() + 1, origin.getZ() + 1).inflate(radius);
    }

    private static void blast(ServerLevel level, BlockPos origin, List<BlockPos> placed) {
        BlockPos ore = origin.east();
        require(level.getBlockState(origin).isAir() && level.getBlockState(ore).isAir(),
            "Explosion fixture must start with empty target blocks at " + origin);
        require(level.setBlockAndUpdate(origin, Blocks.STONE.defaultBlockState()), "Could not place stone control");
        placed.add(origin);
        require(level.setBlockAndUpdate(ore, Blocks.IRON_ORE.defaultBlockState()), "Could not place ore control");
        placed.add(ore);
        level.explode(null, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D,
            8.0F, false, Level.ExplosionInteraction.BLOCK);
    }

    private static void food(MinecraftServer server, ServerLevel level) throws Exception {
        String name = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary",
            "net.minecraft.class_1309", "method_6040", "()V");
        Method complete = LivingEntity.class.getDeclaredMethod(name);
        complete.setAccessible(true);

        ServerPlayer completed = foodPlayer(server, level, "AuraEatQA");
        completed.startUsingItem(InteractionHand.MAIN_HAND);
        require(completed.isUsingItem(), "Apple completion control did not start using");
        complete.invoke(completed);
        require(completed.hasEffect(MobEffects.REGENERATION), "Completed apple did not apply Forbidden Fruit effect");

        ServerPlayer cancelled = foodPlayer(server, level, "AuraCancelQA");
        cancelled.startUsingItem(InteractionHand.MAIN_HAND);
        require(cancelled.isUsingItem(), "Apple cancellation control did not start using");
        cancelled.releaseUsingItem();
        require(!cancelled.isUsingItem(), "Apple cancellation did not stop use");
        complete.invoke(cancelled);
        require(!cancelled.hasEffect(MobEffects.REGENERATION), "Cancelled apple incorrectly applied Forbidden Fruit effect");
    }

    private static ServerPlayer foodPlayer(MinecraftServer server, ServerLevel level, String name) {
        ServerPlayer player = player(server, level, name);
        player.getFoodData().setFoodLevel(10);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.APPLE));
        AuraAccessoryInventory.set(player, AuraAccessoryInventory.AMULET,
            new ItemStack(AuraItems.AMULET_OF_THE_FORBIDDEN_FRUIT));
        return player;
    }

    private static ServerPlayer player(MinecraftServer server, ServerLevel level, String name) {
        GameProfile profile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)), name);
        ServerPlayer player = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        player.connection = new ServerGamePacketListenerImpl(server, connection, player,
            CommonListenerCookie.createInitial(profile, false));
        player.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        player.setGameMode(GameType.SURVIVAL);
        require(!player.getAbilities().instabuild, "Synthetic player must be in survival mode");
        return player;
    }

    private static boolean check(JsonObject assertions, String name, Check check) {
        try {
            check.run();
            assertions.addProperty(name, "PASS");
            return true;
        } catch (Exception | AssertionError error) {
            assertions.addProperty(name, "FAIL: " + describe(error));
            return false;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireNear(float expected, float actual, String label) {
        require(Math.abs(expected - actual) < 0.01F, label + ": expected " + expected + ", got " + actual);
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
