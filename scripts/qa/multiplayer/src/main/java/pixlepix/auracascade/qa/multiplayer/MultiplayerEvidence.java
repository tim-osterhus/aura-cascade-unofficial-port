package pixlepix.auracascade.qa.multiplayer;

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
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import pixlepix.auracascade.compat.AuraAccessoryInventory;
import pixlepix.auracascade.fairy.AuraFairyEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.RingOfBindingItem;

final class MultiplayerEvidence {
    static final Gson JSON = new GsonBuilder().create();
    final Path output = Path.of(System.getProperty("aura.qa.multiplayer.output")).toAbsolutePath();
    final long started = System.nanoTime();
    private final JsonObject proof = new JsonObject();
    private final AttachmentType<?> accessoryType;
    private int maxFairies;

    MultiplayerEvidence() throws Exception {
        Files.createDirectories(output);
        String namespace = FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
        if (!"intermediary".equals(namespace)) throw new IllegalStateException("Not a packaged intermediary runtime");
        var aura = FabricLoader.getInstance().getModContainer("aura").orElseThrow();
        var origin = aura.getOrigin();
        if (origin.getKind() != ModOrigin.Kind.PATH || origin.getPaths().size() != 1) {
            throw new IllegalStateException("Aura origin is not one packaged JAR");
        }
        Path jar = origin.getPaths().get(0).toAbsolutePath().normalize();
        if (!Files.isRegularFile(jar) || !jar.toString().endsWith(".jar")) throw new IllegalStateException("Aura is not a JAR");
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
        proof.addProperty("runtimeNamespace", namespace);
        proof.addProperty("auraOrigin", jar.toString());
        proof.addProperty("auraSha256", hash);
        proof.addProperty("auraVersion", aura.getMetadata().getVersion().getFriendlyString());
        // Read-only access to the actual type is needed to distinguish absent from empty.
        var field = AuraAccessoryInventory.class.getDeclaredField("ATTACHMENT");
        field.setAccessible(true);
        accessoryType = (AttachmentType<?>) field.get(null);
    }

    JsonObject snapshot(Iterable<? extends Player> players, Iterable<? extends Entity> entities, long tick) {
        JsonObject report = proof.deepCopy();
        report.addProperty("utc", Instant.now().toString());
        report.addProperty("tick", tick);
        JsonArray playerReports = new JsonArray();
        for (Player player : players) {
            JsonObject p = new JsonObject();
            p.addProperty("name", player.getGameProfile().name());
            p.addProperty("uuid", player.getUUID().toString());
            p.addProperty("accessoryAttached", ((AttachmentTarget) player).hasAttached(accessoryType));
            p.addProperty("alive", player.isAlive());
            p.addProperty("spectator", player.isSpectator());
            p.addProperty("selectedHotbarSlot", player.getInventory().getSelectedSlot());
            p.addProperty("mainHandItem", BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString());
            p.addProperty("mainHandCount", player.getMainHandItem().getCount());
            JsonArray slots = new JsonArray();
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
            }
            p.add("slots", slots);
            p.addProperty("ringsInInventoryAndEquipment", rings);
            p.addProperty("charmsInInventory", charms);
            playerReports.add(p);
        }
        JsonArray fairies = new JsonArray();
        for (Entity entity : entities) {
            if (entity instanceof AuraFairyEntity fairy && !fairy.isRemoved()) {
                JsonObject f = new JsonObject();
                f.addProperty("uuid", fairy.getUUID().toString());
                f.addProperty("owner", String.valueOf(fairy.ownerId()));
                f.addProperty("slot", fairy.slot());
                f.addProperty("role", fairy.role().id());
                f.addProperty("x", fairy.getX());
                f.addProperty("y", fairy.getY());
                f.addProperty("z", fairy.getZ());
                fairies.add(f);
            }
        }
        maxFairies = Math.max(maxFairies, fairies.size());
        report.addProperty("maxFairiesObserved", maxFairies);
        report.add("players", playerReports);
        report.add("fairies", fairies);
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
            Path dir = Path.of(System.getProperty("aura.qa.multiplayer.output"));
            Files.createDirectories(dir);
            JsonObject report = new JsonObject();
            report.addProperty("failure", error.toString());
            report.addProperty("utc", Instant.now().toString());
            Files.writeString(dir.resolve("failure.json"), JSON.toJson(report));
        } catch (Exception writeError) {
            System.err.println("[Aura Multiplayer QA] " + error + "; report write: " + writeError);
        }
    }
}
