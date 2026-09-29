package pixlepix.auracascade.qa.neoforge.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;
import pixlepix.auracascade.aura.AuraInspectionState;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.item.books.StorageBookData;
import pixlepix.auracascade.item.books.StorageBookItem;
import pixlepix.auracascade.parity.AuraColor;

final class QaPersistenceSupport {
    static final String OUTPUT_PROPERTY = "aura.qa.persistence.output";
    static final String WHITE_RESULT = "white-full-result.json";
    static final String PERSISTENCE_EXPECTATION = "persistence-expectation.json";
    static final String PERSISTENCE_RESULT = "persistence-check-result.json";
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path outputDirectory;
    private final JsonObject auraJar;
    final MinecraftServer server;
    final ProcessIdentity process;

    private QaPersistenceSupport(MinecraftServer server, ProcessIdentity process, Path outputDirectory, JsonObject auraJar) {
        this.server = server;
        this.process = process;
        this.outputDirectory = outputDirectory;
        this.auraJar = auraJar;
    }

    static QaPersistenceSupport open(MinecraftServer server, ProcessIdentity process) throws Exception {
        Path outputDirectory = outputDirectory(server);
        requireOutsideWorld(server, outputDirectory);
        Files.createDirectories(outputDirectory);

        var modFile = ModList.get().getModFileById("aura");
        require(modFile != null, "Aura mod is not loaded");
        Path jar = modFile.getFile().getFilePath().toAbsolutePath().normalize();
        require(Files.isRegularFile(jar) && jar.getFileName() != null
            && jar.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"),
            "Aura did not load from a regular packaged JAR: " + jar);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream stream = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        JsonObject jarProof = new JsonObject();
        jarProof.addProperty("origin", jar.toString());
        jarProof.addProperty("sha256", hash);
        jarProof.addProperty("bytes", Files.size(jar));
        jarProof.addProperty("proof", "verified-regular-aura-jar");
        jarProof.addProperty("runtimeNamespace", "neoforge");
        PersistenceQaMod.LOGGER.info("[aura_qa_persistence] LOADED_AURA_JAR path={} sha256={} bytes={}",
            jar, hash, Files.size(jar));
        return new QaPersistenceSupport(server, process, outputDirectory, jarProof);
    }

    JsonObject report(String scenario, String status, String runId) {
        JsonObject report = new JsonObject();
        report.addProperty("schemaVersion", 1);
        report.addProperty("scenario", scenario);
        report.addProperty("status", status);
        report.addProperty("success", false);
        report.addProperty("runId", runId);
        report.addProperty("utc", Instant.now().toString());
        report.add("process", process.toJson());
        report.add("auraJar", auraJar.deepCopy());
        return report;
    }

    Path path(String fileName) {
        return outputDirectory.resolve(fileName);
    }

    String auraJarHash() {
        return auraJar.get("sha256").getAsString();
    }

    void write(String fileName, JsonObject report) throws Exception {
        writeJson(path(fileName), report);
    }

    static void writeStartupFailure(
        MinecraftServer server,
        ProcessIdentity process,
        String scenario,
        String runId,
        String fileName,
        Throwable failure
    ) {
        try {
            Path output = outputDirectory(server);
            requireOutsideWorld(server, output);
            Files.createDirectories(output);
            JsonObject report = new JsonObject();
            report.addProperty("schemaVersion", 1);
            report.addProperty("scenario", scenario);
            report.addProperty("status", "FAIL");
            report.addProperty("success", false);
            report.addProperty("runId", runId);
            report.addProperty("utc", Instant.now().toString());
            report.add("process", process.toJson());
            report.addProperty("failure", failure.toString());
            writeJson(output.resolve(fileName), report);
        } catch (Exception reportFailure) {
            PersistenceQaMod.LOGGER.error("[aura_qa_persistence] Could not write fail-closed result JSON", reportFailure);
        }
    }

    static void writeJson(Path output, JsonObject report) throws Exception {
        Files.createDirectories(output.getParent());
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp");
        Files.writeString(temporary, JSON.toJson(report));
        try {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException unsupported) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    static Path outputDirectory(MinecraftServer server) {
        String configured = System.getProperty(OUTPUT_PROPERTY);
        return (configured == null || configured.isBlank()
            ? server.getServerDirectory().resolve("qa-persistence")
            : Path.of(configured)).toAbsolutePath().normalize();
    }

    static void requireOutsideWorld(MinecraftServer server, Path output) {
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        require(!output.toAbsolutePath().normalize().startsWith(world),
            "QA JSON directory must be outside the world directory: " + output);
    }

    static int itemCount(ServerLevel level, AABB bounds, Item item) {
        int count = 0;
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds, e -> !e.isRemoved())) {
            if (entity.getItem().is(item)) count += entity.getItem().getCount();
        }
        return count;
    }

    static Map<String, Integer> itemCounts(ServerLevel level, AABB bounds) {
        TreeMap<String, Integer> counts = new TreeMap<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds, e -> !e.isRemoved())) {
            ItemStack stack = entity.getItem();
            if (!stack.isEmpty()) counts.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
        }
        return Map.copyOf(counts);
    }

    static ItemEntity drop(ServerLevel level, BlockPos supportPos, ItemStack stack, double xOffset) {
        ItemEntity entity = new ItemEntity(level,
            supportPos.getX() + xOffset,
            supportPos.getY() + 1.2D,
            supportPos.getZ() + 0.5D,
            stack.copy());
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.setPickUpDelay(32767);
        require(level.addFreshEntity(entity), "Could not spawn test input " + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        return entity;
    }

    static JsonObject nodeSnapshot(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof AuraNodeBlockEntity, "Expected Aura node block entity at " + pos);
        AuraNodeBlockEntity node = (AuraNodeBlockEntity) level.getBlockEntity(pos);
        AuraInspectionState state = node.inspectionState();
        JsonObject result = new JsonObject();
        result.addProperty("block", blockId(level, pos));
        JsonObject aura = new JsonObject();
        for (AuraColor color : AuraColor.values()) aura.addProperty(color.id(), state.storage().get(color));
        result.add("aura", aura);
        result.addProperty("totalAura", state.totalAura());
        result.addProperty("storedPower", state.storedPower());
        result.addProperty("linkedNodes", state.linkedNodeCount());
        result.addProperty("hasScannedLinks", state.hasScannedLinks());
        return result;
    }

    static JsonObject pumpSnapshot(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof AuraPumpBlockEntity, "Expected Aura pump block entity at " + pos);
        AuraPumpBlockEntity pump = (AuraPumpBlockEntity) level.getBlockEntity(pos);
        JsonObject result = networkSnapshot(level, pos, pump);
        result.addProperty("fuelAttempts", pump.pumpState().power());
        result.addProperty("fuelSpeed", pump.pumpState().speed());
        return result;
    }

    static JsonObject networkSnapshot(ServerLevel level, BlockPos pos, AuraNetworkBlockEntity network) {
        AuraInspectionState state = network.inspectionState();
        JsonObject result = new JsonObject();
        result.addProperty("block", blockId(level, pos));
        JsonObject aura = new JsonObject();
        for (AuraColor color : AuraColor.values()) aura.addProperty(color.id(), state.storage().get(color));
        result.add("aura", aura);
        result.addProperty("totalAura", state.totalAura());
        result.addProperty("storedPower", state.storedPower());
        result.addProperty("linkedNodes", state.linkedNodeCount());
        result.addProperty("hasScannedLinks", state.hasScannedLinks());
        return result;
    }

    static JsonObject consumerSnapshot(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof AuraConsumerBlockEntity, "Expected Aura processor block entity at " + pos);
        var state = ((AuraConsumerBlockEntity) level.getBlockEntity(pos)).inspectionState();
        JsonObject result = new JsonObject();
        result.addProperty("block", blockId(level, pos));
        result.addProperty("progress", state.progress());
        result.addProperty("maxProgress", state.maxProgress());
        result.addProperty("storedPower", state.storedPower());
        result.addProperty("lastReceivedPower", state.lastReceivedPower());
        return result;
    }

    static JsonObject bookShelfSnapshot(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof StorageBookshelfBlockEntity,
            "Expected storage bookshelf block entity at " + pos);
        StorageBookshelfBlockEntity shelf = (StorageBookshelfBlockEntity) level.getBlockEntity(pos);
        ItemStack book = shelf.storedBook();
        JsonObject result = new JsonObject();
        result.addProperty("block", blockId(level, pos));
        result.addProperty("bookItem", BuiltInRegistries.ITEM.getKey(book.getItem()).toString());
        result.addProperty("bookVariant", book.getItem() instanceof StorageBookItem item ? item.variant().registryPath() : "unknown");
        result.addProperty("bookName", book.isEmpty() ? "" : book.getHoverName().getString());
        result.addProperty("bookCount", book.getCount());
        result.add("entryCounts", countsJson(bookCounts(shelf)));
        result.addProperty("storedTypes", shelf.storedTypes());
        result.addProperty("storedItemCount", shelf.storedItemCount());
        return result;
    }

    static JsonObject coordinatorSnapshot(ServerLevel level, BlockPos pos) {
        require(level.getBlockEntity(pos) instanceof BookshelfCoordinatorBlockEntity,
            "Expected bookshelf coordinator block entity at " + pos);
        BookshelfCoordinatorBlockEntity coordinator = (BookshelfCoordinatorBlockEntity) level.getBlockEntity(pos);
        var network = coordinator.snapshot(level, pos);
        var browser = coordinator.browserSnapshot(level, pos);
        TreeMap<String, Integer> entries = new TreeMap<>();
        for (var entry : browser.entries()) {
            entries.merge(BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString(), Math.toIntExact(entry.count()), Integer::sum);
        }
        JsonObject result = new JsonObject();
        result.addProperty("block", blockId(level, pos));
        result.addProperty("connectedShelves", network.connectedShelfPositions().size());
        result.addProperty("storageShelves", network.storageShelfPositions().size());
        result.addProperty("requiredPower", network.requiredPower());
        result.addProperty("availablePower", network.availablePower());
        result.addProperty("complete", network.complete());
        result.add("browserEntryCounts", countsJson(entries));
        return result;
    }

    static Map<String, Integer> bookCounts(StorageBookshelfBlockEntity shelf) {
        TreeMap<String, Integer> counts = new TreeMap<>();
        for (StorageBookData.Entry entry : shelf.storedEntries()) {
            counts.merge(BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString(), entry.count(), Integer::sum);
        }
        return Map.copyOf(counts);
    }

    static JsonObject countsJson(Map<String, Integer> counts) {
        JsonObject result = new JsonObject();
        new TreeMap<>(counts).forEach(result::addProperty);
        return result;
    }

    static JsonObject position(BlockPos pos) {
        JsonObject result = new JsonObject();
        result.addProperty("x", pos.getX());
        result.addProperty("y", pos.getY());
        result.addProperty("z", pos.getZ());
        return result;
    }

    static BlockPos readPosition(JsonObject value) {
        return new BlockPos(value.get("x").getAsInt(), value.get("y").getAsInt(), value.get("z").getAsInt());
    }

    static String blockId(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    record ProcessIdentity(long pid, String startMarker, String startedAt) {
        JsonObject toJson() {
            JsonObject value = new JsonObject();
            value.addProperty("pid", pid);
            value.addProperty("startMarker", startMarker);
            value.addProperty("startedAt", startedAt);
            return value;
        }
    }
}
