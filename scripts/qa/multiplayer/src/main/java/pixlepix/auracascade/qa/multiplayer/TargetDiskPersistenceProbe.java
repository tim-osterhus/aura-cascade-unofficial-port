package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.books.StorageBookData;
import pixlepix.auracascade.item.books.StorageBookVariant;
import pixlepix.auracascade.parity.AuraColor;

/** Opt-in disk persistence probe for a real dedicated-server world restart. */
public final class TargetDiskPersistenceProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.targetDisk";
    private static final String BOOK_NAME = "target disk persistence book";
    private static final String PAYLOAD_NAME = "target disk component payload";
    private static final String BOOK_MARKER = "disk-book-probe";
    private static final String PAYLOAD_MARKER = "disk-payload-probe";
    private static final int SEEDED_WHITE = 500;
    private static final BlockPos NODE_POS = new BlockPos(900, 180, 900);
    private static final BlockPos SHELF_POS = new BlockPos(920, 180, 900);

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)
            || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }

        String phase = System.getProperty(PREFIX + ".phase", "").trim();
        String configuredOutput = System.getProperty(PREFIX + ".output", "").trim();
        String expectedSha256 = System.getProperty(PREFIX + ".sha256", "").trim();
        if (!(phase.equals("write") || phase.equals("verify"))) {
            failToConsole("phase: property " + PREFIX + ".phase must be write or verify");
            return;
        }
        if (configuredOutput.isEmpty()) {
            failToConsole("reportPath: property " + PREFIX + ".output must name a fresh report file");
            return;
        }
        if (!expectedSha256.matches("(?i)[0-9a-f]{64}")) {
            failToConsole("sha256: property " + PREFIX + ".sha256 must be a 64-digit SHA-256");
            return;
        }

        Path output;
        try {
            output = Path.of(configuredOutput).toAbsolutePath().normalize();
        } catch (RuntimeException error) {
            failToConsole("reportPath: invalid " + PREFIX + ".output: " + error);
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(server -> run(server, phase, output, expectedSha256));
    }

    private static void run(MinecraftServer server, String phase, Path output, String expectedSha256) {
        if (Files.exists(output)) {
            failToConsole("freshReportPath: expected an absent report file");
            return;
        }

        JsonObject report = new JsonObject();
        report.addProperty("probe", "targetDiskPersistence");
        report.addProperty("phase", phase);
        report.addProperty("success", false);
        report.addProperty("scope", phase.equals("write")
            ? "Seeds only the two named block entities and reports immediate seeded state, not progression or persistence. Stop the server normally before verify."
            : "Loads and verifies the saved fixture after restart; performs no fixture seeding or world-state writes.");
        report.addProperty("nodePos", NODE_POS.toShortString());
        report.addProperty("shelfPos", SHELF_POS.toShortString());
        report.addProperty("distance", NODE_POS.distManhattan(SHELF_POS));

        try {
            require(server.isDedicatedServer(), "dedicatedServer: expected a dedicated-server callback");
            String actualSha256 = candidateSha256();
            report.addProperty("auraSha256", actualSha256);
            require(actualSha256.equalsIgnoreCase(expectedSha256),
                "auraSha256: loaded Aura JAR does not match the supplied hash");

            ServerLevel level = server.overworld();
            report.addProperty("worldName", server.getWorldData().getLevelName());
            report.addProperty("gameTime", level.getGameTime());
            require(level.isInWorldBounds(NODE_POS) && level.isInWorldBounds(SHELF_POS),
                "worldBounds: fixture positions are outside the overworld bounds");
            level.getChunkAt(NODE_POS);
            level.getChunkAt(SHELF_POS);

            JsonObject assertions = new JsonObject();
            if (phase.equals("write")) {
                seed(level, assertions, report);
            } else {
                verify(level, assertions, report);
            }
            report.add("assertions", assertions);
            report.addProperty("success", true);
        } catch (Exception | AssertionError error) {
            report.addProperty("failure", describe(error));
        }

        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (Exception error) {
            failToConsole("reportWrite: could not create the fresh report: " + describe(error));
        }
    }

    private static void seed(ServerLevel level, JsonObject assertions, JsonObject report) {
        requireEmptyWritePosition(level, NODE_POS);
        requireEmptyWritePosition(level, SHELF_POS);
        assertions.addProperty("writePositionsEmpty", "PASS");

        ItemStack book = createBook();
        require(level.setBlock(NODE_POS, AuraContent.AURA_NODE.defaultBlockState(), 3),
            "seedNode: setBlock rejected the fixture node");
        require(level.setBlock(SHELF_POS, AuraContent.STORAGE_BOOKSHELF.defaultBlockState(), 3),
            "seedShelf: setBlock rejected the fixture shelf");

        BlockEntity nodeEntity = level.getBlockEntity(NODE_POS);
        BlockEntity shelfEntity = level.getBlockEntity(SHELF_POS);
        require(nodeEntity instanceof AuraNodeBlockEntity, "seedNode: registered node entity is missing");
        require(shelfEntity instanceof StorageBookshelfBlockEntity, "seedShelf: registered shelf entity is missing");
        AuraNodeBlockEntity node = (AuraNodeBlockEntity) nodeEntity;
        StorageBookshelfBlockEntity shelf = (StorageBookshelfBlockEntity) shelfEntity;
        node.feedCrystal(AuraColor.WHITE, SEEDED_WHITE);
        require(shelf.setBook(book), "seedShelf: public setBook API rejected the storage book");

        require(node.inspectionState().storage().get(AuraColor.WHITE) == SEEDED_WHITE,
            "seedNode: immediate white aura did not equal the seeded amount");
        requireBook(shelf.storedBook());
        assertions.addProperty("seededState", "PASS");
        report.addProperty("stateMoment", "immediately after seed; no progression claim");
        report.addProperty("seededWhite", SEEDED_WHITE);
        report.addProperty("seededBookName", BOOK_NAME);
        report.addProperty("seededNestedItemCount", 1);
    }

    private static void verify(ServerLevel level, JsonObject assertions, JsonObject report) {
        require(level.getBlockState(NODE_POS).is(AuraContent.AURA_NODE),
            "verifyNode: persisted Aura Node block is absent");
        BlockEntity nodeEntity = level.getBlockEntity(NODE_POS);
        require(nodeEntity instanceof AuraNodeBlockEntity, "verifyNode: persisted Aura Node block entity is absent");
        AuraNodeBlockEntity node = (AuraNodeBlockEntity) nodeEntity;
        int white = node.inspectionState().storage().get(AuraColor.WHITE);
        require(white == SEEDED_WHITE, "verifyNode: expected White aura " + SEEDED_WHITE + ", got " + white);
        assertions.addProperty("nodeWhite500AfterRestart", "PASS");

        require(level.getBlockState(SHELF_POS).is(AuraContent.STORAGE_BOOKSHELF),
            "verifyShelf: persisted storage bookshelf block is absent");
        BlockEntity shelfEntity = level.getBlockEntity(SHELF_POS);
        require(shelfEntity instanceof StorageBookshelfBlockEntity,
            "verifyShelf: persisted storage bookshelf block entity is absent");
        ItemStack book = ((StorageBookshelfBlockEntity) shelfEntity).storedBook();
        requireBook(book);
        assertions.addProperty("namedBookAndNestedComponentsAfterRestart", "PASS");

        report.addProperty("observedWhite", white);
        report.addProperty("observedBookName", componentString(book, DataComponents.CUSTOM_NAME));
        report.addProperty("observedNestedItemCount", StorageBookData.storedItemCount(book));
        report.addProperty("observedNestedPayloadName", componentString(
            StorageBookData.detailedEntries(book).getFirst().stack(), DataComponents.CUSTOM_NAME));
    }

    private static void requireEmptyWritePosition(ServerLevel level, BlockPos pos) {
        require(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null,
            "writePositionsEmpty: refusing to replace existing content at " + pos.toShortString());
    }

    private static ItemStack createBook() {
        StorageBookVariant variant = StorageBookVariant.BASIC;
        ItemStack book = new ItemStack(AuraItems.storageBook(variant));
        book.set(DataComponents.CUSTOM_NAME, Component.literal(BOOK_NAME));
        CustomData.update(DataComponents.CUSTOM_DATA, book, tag -> tag.putString(BOOK_MARKER, "outer-state"));

        ItemStack payload = new ItemStack(Items.DIAMOND_SWORD);
        payload.set(DataComponents.CUSTOM_NAME, Component.literal(PAYLOAD_NAME));
        payload.set(DataComponents.DAMAGE, 13);
        CustomData.update(DataComponents.CUSTOM_DATA, payload, tag -> tag.putString(PAYLOAD_MARKER, "nested-state"));
        require(StorageBookData.insert(book, variant, payload) == 1,
            "seedBook: could not insert the single nested payload");
        return book;
    }

    private static void requireBook(ItemStack book) {
        require(book.getItem() == AuraItems.storageBook(StorageBookVariant.BASIC)
                && BOOK_NAME.equals(componentString(book, DataComponents.CUSTOM_NAME))
                && "outer-state".equals(customString(book, BOOK_MARKER)),
            "bookComponents: persisted named storage book or outer custom data differs");

        var entries = StorageBookData.detailedEntries(book);
        require(StorageBookData.storedItemCount(book) == 1 && entries.size() == 1
                && entries.getFirst().count() == 1
                && entries.getFirst().stack().is(Items.DIAMOND_SWORD)
                && PAYLOAD_NAME.equals(componentString(entries.getFirst().stack(), DataComponents.CUSTOM_NAME))
                && entries.getFirst().stack().getOrDefault(DataComponents.DAMAGE, 0) == 13
                && "nested-state".equals(customString(entries.getFirst().stack(), PAYLOAD_MARKER)),
            "bookComponents: expected one named, damaged sword with nested custom data");
    }

    private static String candidateSha256() throws Exception {
        var aura = FabricLoader.getInstance().getModContainer("aura").orElseThrow(
            () -> new AssertionError("candidateOrigin: Aura mod container is absent"));
        ModOrigin origin = aura.getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1,
            "candidateOrigin: expected exactly one path-origin Aura JAR");
        Path jar = origin.getPaths().getFirst();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"),
            "candidateOrigin: expected a packaged Aura JAR");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String componentString(ItemStack stack, net.minecraft.core.component.DataComponentType<Component> type) {
        Component value = stack.get(type);
        return value == null ? "" : value.getString();
    }

    private static String customString(ItemStack stack, String key) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return data.copyTag().getString(key).orElse("");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }

    private static void failToConsole(String message) {
        System.err.println("[Aura Target Disk Persistence QA] FAIL: " + message);
    }
}
