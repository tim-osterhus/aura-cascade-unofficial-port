package pixlepix.auracascade.qa.neoforge.multiplayer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.compat.AuraAccessoryInventory;
import pixlepix.auracascade.fairy.AuraFairyEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.RingOfBindingItem;

final class MultiplayerEvidence {
    static final String OUTPUT_PROPERTY = "aura.qa.multiplayer.output";
    private static final Gson JSON = new GsonBuilder().create();
    final Path output = Path.of(System.getProperty(OUTPUT_PROPERTY)).toAbsolutePath().normalize();
    private final long started = System.nanoTime();
    private final JsonObject proof = new JsonObject();
    private final AttachmentType<?> accessoryType;
    private int maxFairies;
    private JsonObject peakFairySnapshot;

    MultiplayerEvidence() throws Exception {
        Files.createDirectories(output);
        var modFile = ModList.get().getModFileById("aura");
        if (modFile == null) throw new IllegalStateException("Aura is not loaded");
        Path jar = modFile.getFile().getFilePath().toAbsolutePath().normalize();
        if (!Files.isRegularFile(jar) || jar.getFileName() == null
            || !jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
            throw new IllegalStateException("Aura did not load from one packaged JAR");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        if (!hash.equalsIgnoreCase(System.getProperty("aura.qa.multiplayer.sha256", ""))) {
            throw new IllegalStateException("Loaded Aura does not match the requested candidate SHA-256");
        }
        proof.addProperty("auraOrigin", jar.toString());
        proof.addProperty("auraSha256", hash);
        proof.addProperty("auraJarBytes", Files.size(jar));
        proof.addProperty("auraJarProof", "verified");
        proof.addProperty("runtimeNamespace", "neoforge");

        ResourceLocation key = ResourceLocation.fromNamespaceAndPath("aura", "accessories");
        accessoryType = NeoForgeRegistries.ATTACHMENT_TYPES.get(key);
        if (accessoryType == null) throw new IllegalStateException("Aura accessory attachment is not registered");
    }

    JsonObject snapshot(Iterable<? extends Player> players, Iterable<? extends Entity> entities, long tick) {
        JsonObject report = proof.deepCopy();
        report.addProperty("utc", Instant.now().toString());
        report.addProperty("tick", tick);
        JsonArray playerReports = new JsonArray();
        for (Player player : players) {
            JsonObject p = new JsonObject();
            p.addProperty("name", player.getGameProfile().getName());
            p.addProperty("uuid", player.getUUID().toString());
            p.addProperty("accessoryAttached", player.hasData(accessoryType));
            p.addProperty("alive", player.isAlive());
            p.addProperty("health", player.getHealth());
            p.addProperty("spectator", player.isSpectator());
            p.addProperty("dimension", player.level().dimension().location().toString());
            if (player instanceof ServerPlayer serverPlayer) {
                p.addProperty("keepInventory", serverPlayer.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY));
            }
            p.addProperty("selectedHotbarSlot", player.getInventory().selected);
            p.addProperty("mainHandItem", BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
            p.addProperty("mainHandCount", player.getMainHandItem().getCount());
            JsonArray slots = new JsonArray();
            JsonArray inventory = new JsonArray();
            int rings = 0;
            int charms = 0;
            for (int i = 0; i < AuraAccessoryInventory.SLOT_COUNT; i++) {
                ItemStack stack = AuraAccessoryInventory.get(player, i);
                JsonObject slot = new JsonObject();
                slot.addProperty("slot", i);
                slot.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                slot.addProperty("count", stack.getCount());
                slot.addProperty("bound", stack.is(AuraItems.RING_OF_BINDING) ? RingOfBindingItem.boundFairyCount(stack) : 0);
                slots.add(slot);
                if (stack.is(AuraItems.RING_OF_BINDING)) rings += stack.getCount();
            }
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(AuraItems.RING_OF_BINDING)) rings += stack.getCount();
                if (stack.is(AuraItems.FAIRY_CHARM)) charms += stack.getCount();
                if (!stack.isEmpty()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("slot", i);
                    item.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                    item.addProperty("count", stack.getCount());
                    item.addProperty("bound", stack.is(AuraItems.RING_OF_BINDING) ? RingOfBindingItem.boundFairyCount(stack) : 0);
                    inventory.add(item);
                }
            }
            p.add("slots", slots);
            p.add("inventory", inventory);
            p.addProperty("ringsInInventoryAndEquipment", rings);
            p.addProperty("charmsInInventory", charms);
            if (player.containerMenu instanceof BookshelfCoordinatorMenu menu) {
                JsonObject bookshelf = new JsonObject();
                bookshelf.addProperty("containerId", menu.containerId);
                bookshelf.addProperty("revision", menu.snapshotRevision());
                bookshelf.addProperty("connectedShelves", menu.connectedShelves());
                bookshelf.addProperty("storageShelves", menu.storageShelves());
                bookshelf.addProperty("requiredPower", menu.requiredPower());
                bookshelf.addProperty("availablePower", menu.availablePower());
                bookshelf.addProperty("networkComplete", menu.networkComplete());
                bookshelf.addProperty("canRetrieve", menu.canRetrieve());
                bookshelf.addProperty("resultCode", menu.resultCode());
                bookshelf.addProperty("resultAmount", menu.resultAmount());
                JsonArray entries = new JsonArray();
                for (var entry : menu.entries()) {
                    JsonObject item = new JsonObject();
                    item.addProperty("item", BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());
                    item.addProperty("count", entry.count());
                    entries.add(item);
                }
                bookshelf.add("entries", entries);
                p.add("bookshelf", bookshelf);
            }
            playerReports.add(p);
        }
        JsonArray fairies = new JsonArray();
        JsonArray droppedItems = new JsonArray();
        int ringsDropped = 0;
        for (Entity entity : entities) {
            if (entity instanceof AuraFairyEntity fairy && !fairy.isRemoved()) {
                JsonObject f = new JsonObject();
                f.addProperty("uuid", fairy.getUUID().toString());
                f.addProperty("owner", String.valueOf(fairy.ownerId()));
                f.addProperty("slot", fairy.slot());
                f.addProperty("role", fairy.role().id());
                f.addProperty("dimension", fairy.level().dimension().location().toString());
                f.addProperty("x", fairy.getX());
                f.addProperty("y", fairy.getY());
                f.addProperty("z", fairy.getZ());
                fairies.add(f);
            }
            if (entity instanceof ItemEntity item && !item.isRemoved() && !item.getItem().isEmpty()) {
                ItemStack stack = item.getItem();
                JsonObject drop = new JsonObject();
                drop.addProperty("uuid", item.getUUID().toString());
                drop.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
                drop.addProperty("count", stack.getCount());
                drop.addProperty("bound", stack.is(AuraItems.RING_OF_BINDING) ? RingOfBindingItem.boundFairyCount(stack) : 0);
                drop.addProperty("dimension", item.level().dimension().location().toString());
                droppedItems.add(drop);
                if (stack.is(AuraItems.RING_OF_BINDING)) ringsDropped += stack.getCount();
            }
        }
        boolean newPeak = fairies.size() > maxFairies;
        maxFairies = Math.max(maxFairies, fairies.size());
        report.addProperty("maxFairiesObserved", maxFairies);
        report.add("players", playerReports);
        report.add("fairies", fairies);
        report.add("droppedItems", droppedItems);
        report.addProperty("ringsDropped", ringsDropped);
        if (newPeak && maxFairies > 1) {
            peakFairySnapshot = report.deepCopy();
        }
        if (peakFairySnapshot != null) {
            report.add("peakFairySnapshot", peakFairySnapshot);
        }
        return report;
    }

    void publish(JsonObject report) throws Exception {
        String text = JSON.toJson(report);
        Files.writeString(output.resolve("latest.tmp"), text);
        Files.move(output.resolve("latest.tmp"), output.resolve("latest.json"), StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(output.resolve("observations.jsonl"), text + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    boolean stopRequested() {
        return Files.exists(output.resolve("stop.request"));
    }

    boolean timedOut() {
        return System.nanoTime() - started > 600_000_000_000L;
    }

    static void failure(Throwable error) {
        try {
            Path dir = Path.of(System.getProperty(OUTPUT_PROPERTY)).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            JsonObject report = new JsonObject();
            report.addProperty("failure", error.toString());
            report.addProperty("utc", Instant.now().toString());
            Files.writeString(dir.resolve("failure.json"), JSON.toJson(report));
        } catch (Exception writeError) {
            System.err.println("[Aura NeoForge Multiplayer QA] " + error + "; report write: " + writeError);
        }
    }
}
