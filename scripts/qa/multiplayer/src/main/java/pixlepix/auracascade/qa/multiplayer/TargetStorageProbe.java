package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.books.StorageBookVariant;

/** Opt-in dedicated-server probe for real coordinator-backed storage search and retrieval. */
public final class TargetStorageProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.targetStorage";
    private static final String PAYLOAD_MARKER = "target-storage-payload";
    private static final BlockPos COORDINATOR_POS = new BlockPos(1208, 220, 1208);
    private static final BlockPos SHELF_ONE_POS = COORDINATOR_POS.north();
    private static final BlockPos SHELF_TWO_POS = COORDINATOR_POS.east();
    private static final BlockPos POWER_NODE_POS = COORDINATOR_POS.below();
    private static final BlockPos DISCONNECTED_POS = COORDINATOR_POS.west(6);
    private static final BlockPos DISCONNECTED_NODE_POS = DISCONNECTED_POS.below();
    private static final int FIRST_PAYLOAD_COUNT = 3;
    private static final int SECOND_PAYLOAD_COUNT = 4;
    private static final int DISTINCT_COMPONENT_COUNT = 2;
    private static final int REQUESTED_COUNT = 5;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)
            || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }

        String configuredOutput = System.getProperty(PREFIX + ".output", "").trim();
        String expectedSha256 = System.getProperty(PREFIX + ".sha256", "").trim();
        if (configuredOutput.isEmpty() || !expectedSha256.matches("(?i)[0-9a-f]{64}")) {
            failToConsole("output and a 64-digit " + PREFIX + ".sha256 are required");
            return;
        }

        Path output;
        try {
            output = Path.of(configuredOutput).toAbsolutePath().normalize();
        } catch (RuntimeException error) {
            failToConsole("invalid output path: " + error);
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(
            server -> run(server, output, expectedSha256));
    }

    private static void run(MinecraftServer server, Path output, String expectedSha256) {
        JsonObject report = new JsonObject();
        JsonObject assertions = new JsonObject();
        List<BlockPos> placedBlocks = new ArrayList<>();
        ServerLevel level = null;
        boolean chunkForced = false;
        report.addProperty("probe", "targetStorage");
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("complete", false);
        report.addProperty("success", false);
        report.addProperty("expectedAuraSha256", expectedSha256);
        report.addProperty("reportPath", output.toString());
        report.addProperty("serverKind", server.isDedicatedServer() ? "dedicated" : "integrated");
        report.addProperty("scope",
            "Real dedicated-server coordinator browserSnapshot and extractFromNetwork APIs with real storage books; no client text-input or packet-path claim.");
        report.addProperty("fixture",
            "Two fixture-seeded Basic Storage Books hold 3+4 identical rich-component diamond swords and 2 swords with distinct components. Adjacent Aura Node power is explicitly seeded through real node-state loading; it is not earned or generated.");

        try {
            require(server.isDedicatedServer(), "dedicatedServer: expected dedicated-server execution");
            require(!Files.exists(output), "freshReportPath: output already exists: " + output);
            String actualSha256 = candidateSha256();
            report.addProperty("auraSha256", actualSha256);
            require(actualSha256.equalsIgnoreCase(expectedSha256),
                "auraSha256: loaded Aura JAR differs from the requested hash");

            level = server.overworld();
            int chunkX = COORDINATOR_POS.getX() >> 4;
            int chunkZ = COORDINATOR_POS.getZ() >> 4;
            level.setChunkForced(chunkX, chunkZ, true);
            chunkForced = true;
            level.getChunkAt(COORDINATOR_POS);
            requireFixtureClear(level);

            place(level, placedBlocks, COORDINATOR_POS, AuraContent.BOOKSHELF_COORDINATOR.defaultBlockState());
            place(level, placedBlocks, SHELF_ONE_POS, AuraContent.STORAGE_BOOKSHELF.defaultBlockState());
            place(level, placedBlocks, SHELF_TWO_POS, AuraContent.STORAGE_BOOKSHELF.defaultBlockState());
            place(level, placedBlocks, POWER_NODE_POS, AuraContent.AURA_NODE.defaultBlockState());
            place(level, placedBlocks, DISCONNECTED_POS, AuraContent.BOOKSHELF_COORDINATOR.defaultBlockState());
            place(level, placedBlocks, DISCONNECTED_NODE_POS, AuraContent.AURA_NODE.defaultBlockState());

            BookshelfCoordinatorBlockEntity coordinator = coordinator(level, COORDINATOR_POS);
            StorageBookshelfBlockEntity firstShelf = shelf(level, SHELF_ONE_POS);
            StorageBookshelfBlockEntity secondShelf = shelf(level, SHELF_TWO_POS);
            ItemStack firstBook = basicBook();
            ItemStack secondBook = basicBook();
            require(firstShelf.setBook(firstBook) && secondShelf.setBook(secondBook),
                "fixtureBooks: real Storage Books were rejected by the shelves");
            ItemStack target = richPayload();
            ItemStack distinct = distinctPayload();
            require(firstShelf.deposit(target.copyWithCount(FIRST_PAYLOAD_COUNT)) == FIRST_PAYLOAD_COUNT,
                "fixtureDeposit: first shelf did not accept all target items");
            require(secondShelf.deposit(target.copyWithCount(SECOND_PAYLOAD_COUNT)) == SECOND_PAYLOAD_COUNT,
                "fixtureDeposit: second shelf did not accept all target items");
            require(secondShelf.deposit(distinct.copyWithCount(DISTINCT_COMPONENT_COUNT)) == DISTINCT_COMPONENT_COUNT,
                "fixtureDeposit: second shelf did not accept the distinct-component items");

            BookshelfCoordinatorBlockEntity.BrowserSnapshot browser = coordinator.browserSnapshot(level, COORDINATOR_POS);
            require(browser.complete() && browser.connectedShelves() == 2 && browser.storageShelves() == 2
                    && browser.requiredPower() > 0,
                "browserNetwork: expected a complete network with the two fixture shelves");
            BookshelfCoordinatorBlockEntity.BrowserEntry targetEntry = findEntry(browser, target);
            BookshelfCoordinatorBlockEntity.BrowserEntry distinctEntry = findEntry(browser, distinct);
            require(targetEntry != null && targetEntry.count() == FIRST_PAYLOAD_COUNT + SECOND_PAYLOAD_COUNT
                    && distinctEntry != null && distinctEntry.count() == DISTINCT_COMPONENT_COUNT
                    && browser.entries().size() == 2 && richComponentsMatch(targetEntry.stack()),
                "browserAggregation: expected component-aware aggregation (7 target, 2 distinct) with rich components preserved");
            assertions.addProperty("componentAwareBrowserAggregation", "PASS");
            report.addProperty("browserRequiredPower", browser.requiredPower());

            AuraNodeBlockEntity powerNode = node(level, POWER_NODE_POS);
            seedPower(level, powerNode, Math.max(0, browser.requiredPower() - 1));
            int insufficientPower = powerNode.storedPower();
            List<ItemStack> rejected = coordinator.extractFromNetwork(level, COORDINATOR_POS, target, REQUESTED_COUNT);
            require(rejected.isEmpty() && powerNode.storedPower() == insufficientPower
                    && storedMatchingCount(firstShelf, target) + storedMatchingCount(secondShelf, target)
                        == FIRST_PAYLOAD_COUNT + SECOND_PAYLOAD_COUNT,
                "insufficientPower: extraction must reject without changing power or stored items");
            assertions.addProperty("insufficientPowerRejectsWithoutMutation", "PASS");

            int sufficientPower = browser.requiredPower() + 5;
            seedPower(level, powerNode, sufficientPower);
            List<ItemStack> extracted = coordinator.extractFromNetwork(level, COORDINATOR_POS, target, REQUESTED_COUNT);
            int extractedCount = extracted.stream().mapToInt(ItemStack::getCount).sum();
            require(extractedCount == REQUESTED_COUNT && extracted.stream().allMatch(TargetStorageProbe::richComponentsMatch),
                "poweredRetrieval: expected five target stacks with the original item components");
            int targetRemaining = storedMatchingCount(firstShelf, target) + storedMatchingCount(secondShelf, target);
            int distinctRemaining = storedMatchingCount(firstShelf, distinct) + storedMatchingCount(secondShelf, distinct);
            require(targetRemaining == 2 && distinctRemaining == DISTINCT_COMPONENT_COUNT
                    && firstShelf.storedItemCount() + secondShelf.storedItemCount() + extractedCount
                        == FIRST_PAYLOAD_COUNT + SECOND_PAYLOAD_COUNT + DISTINCT_COMPONENT_COUNT
                    && powerNode.storedPower() == 5,
                "poweredRetrieval: item conservation, component isolation, and exact network power cost must hold");
            assertions.addProperty("poweredRetrievalConservesItemsComponentsAndPower", "PASS");
            report.addProperty("requestedCount", REQUESTED_COUNT);
            report.addProperty("extractedCount", extractedCount);
            report.addProperty("targetCountRemaining", targetRemaining);
            report.addProperty("distinctComponentCountRemaining", distinctRemaining);
            report.addProperty("seededSufficientPower", sufficientPower);
            report.addProperty("powerAfterRetrieval", powerNode.storedPower());

            BookshelfCoordinatorBlockEntity disconnected = coordinator(level, DISCONNECTED_POS);
            BookshelfCoordinatorBlockEntity.BrowserSnapshot disconnectedBrowser =
                disconnected.browserSnapshot(level, DISCONNECTED_POS);
            AuraNodeBlockEntity disconnectedNode = node(level, DISCONNECTED_NODE_POS);
            seedPower(level, disconnectedNode, 100);
            int disconnectedPower = disconnectedNode.storedPower();
            List<ItemStack> disconnectedResult = disconnected.extractFromNetwork(
                level, DISCONNECTED_POS, target, 1);
            require(disconnectedBrowser.complete() && disconnectedBrowser.connectedShelves() == 0
                    && disconnectedBrowser.storageShelves() == 0 && disconnectedResult.isEmpty()
                    && disconnectedNode.storedPower() == disconnectedPower,
                "disconnectedNetwork: retrieval must reject without shelves or power mutation");
            assertions.addProperty("disconnectedNetworkRejectsRetrieval", "PASS");

            report.addProperty("seededInsufficientPower", Math.max(0, browser.requiredPower() - 1));
            report.add("assertions", assertions);
            report.addProperty("success", true);
        } catch (Exception | AssertionError error) {
            report.add("assertions", assertions);
            report.addProperty("failure", describe(error));
        } finally {
            if (level != null) {
                try {
                    cleanup(level, chunkForced, placedBlocks);
                    report.addProperty("cleanup", "PASS");
                } catch (Exception | AssertionError error) {
                    report.addProperty("cleanup", "FAIL: " + describe(error));
                    report.addProperty("success", false);
                    report.addProperty("failure", "cleanup: " + describe(error));
                }
            }
        }

        report.addProperty("complete", true);
        report.addProperty("finishedAt", Instant.now().toString());
        try {
            Files.createDirectories(output.getParent());
            Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (Exception error) {
            failToConsole("reportWrite: " + describe(error));
        }
    }

    private static void requireFixtureClear(ServerLevel level) {
        List<BlockPos> positions = List.of(COORDINATOR_POS, SHELF_ONE_POS, SHELF_TWO_POS,
            POWER_NODE_POS, DISCONNECTED_POS, DISCONNECTED_NODE_POS);
        for (BlockPos pos : positions) {
            require(level.isInWorldBounds(pos) && level.getBlockState(pos).isAir()
                    && level.getBlockEntity(pos) == null,
                "fixtureClear: refusing to replace existing content at " + pos.toShortString());
        }

        for (BlockPos pos : List.of(COORDINATOR_POS, DISCONNECTED_POS)) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (neighbor.equals(SHELF_ONE_POS) || neighbor.equals(SHELF_TWO_POS)) {
                    continue;
                }
                require(!isBookshelf(level.getBlockState(neighbor)),
                    "fixtureClear: unexpected connected bookshelf at " + neighbor.toShortString());
            }
        }
        for (BlockPos pos : List.of(SHELF_ONE_POS, SHELF_TWO_POS)) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (neighbor.equals(COORDINATOR_POS)) {
                    continue;
                }
                require(!isBookshelf(level.getBlockState(neighbor)),
                    "fixtureClear: unexpected linked bookshelf at " + neighbor.toShortString());
            }
        }
    }

    private static boolean isBookshelf(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF)
            || state.is(AuraContent.STORAGE_BOOKSHELF);
    }

    private static void place(ServerLevel level, List<BlockPos> placedBlocks, BlockPos pos,
                               net.minecraft.world.level.block.state.BlockState state) {
        require(level.setBlockAndUpdate(pos, state), "fixturePlacement: could not place block at " + pos.toShortString());
        placedBlocks.add(pos);
    }

    private static BookshelfCoordinatorBlockEntity coordinator(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof BookshelfCoordinatorBlockEntity,
            "fixtureCoordinator: real coordinator block entity is absent at " + pos.toShortString());
        return (BookshelfCoordinatorBlockEntity) entity;
    }

    private static StorageBookshelfBlockEntity shelf(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof StorageBookshelfBlockEntity,
            "fixtureShelf: real storage shelf block entity is absent at " + pos.toShortString());
        return (StorageBookshelfBlockEntity) entity;
    }

    private static AuraNodeBlockEntity node(ServerLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof AuraNodeBlockEntity,
            "fixturePower: real Aura Node block entity is absent at " + pos.toShortString());
        return (AuraNodeBlockEntity) entity;
    }

    private static void seedPower(ServerLevel level, AuraNodeBlockEntity node, int amount) {
        CompoundTag saved = node.saveWithoutMetadata(level.registryAccess());
        AuraNodeState state = new AuraNodeState();
        state.receivePower(amount);
        saved.put("node_state", state.toTag());
        node.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
        node.setChanged();
        require(node.storedPower() == amount, "fixturePower: real Aura Node state load did not seed " + amount);
    }

    private static ItemStack basicBook() {
        return new ItemStack(AuraItems.storageBook(StorageBookVariant.BASIC));
    }

    private static ItemStack richPayload() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Target storage payload"));
        stack.set(DataComponents.DAMAGE, 13);
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putString(PAYLOAD_MARKER, "preserved"));
        return stack;
    }

    private static ItemStack distinctPayload() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Distinct storage payload"));
        stack.set(DataComponents.DAMAGE, 2);
        return stack;
    }

    private static BookshelfCoordinatorBlockEntity.BrowserEntry findEntry(
        BookshelfCoordinatorBlockEntity.BrowserSnapshot snapshot, ItemStack target) {
        return snapshot.entries().stream()
            .filter(entry -> ItemStack.isSameItemSameComponents(entry.stack(), target))
            .findFirst().orElse(null);
    }

    private static int storedMatchingCount(StorageBookshelfBlockEntity shelf, ItemStack target) {
        return shelf.storedEntries().stream()
            .filter(entry -> ItemStack.isSameItemSameComponents(entry.stack(), target))
            .mapToInt(entry -> entry.count()).sum();
    }

    private static boolean richComponentsMatch(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return stack.is(Items.DIAMOND_SWORD)
            && name != null && "Target storage payload".equals(name.getString())
            && stack.getOrDefault(DataComponents.DAMAGE, 0) == 13
            && "preserved".equals(customData.copyTag().getString(PAYLOAD_MARKER).orElse(""));
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

    private static void cleanup(ServerLevel level, boolean chunkForced, List<BlockPos> placedBlocks) {
        for (BlockPos pos : List.of(SHELF_ONE_POS, SHELF_TWO_POS)) {
            if (!placedBlocks.contains(pos)) {
                continue;
            }
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof StorageBookshelfBlockEntity shelf) {
                shelf.removeBook();
            }
        }
        for (int index = placedBlocks.size() - 1; index >= 0; index--) {
            BlockPos pos = placedBlocks.get(index);
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            require(level.getBlockState(pos).isAir() && level.getBlockEntity(pos) == null,
                "cleanup: fixture block remained at " + pos.toShortString());
        }
        if (chunkForced) {
            level.setChunkForced(COORDINATOR_POS.getX() >> 4, COORDINATOR_POS.getZ() >> 4, false);
        }
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
        System.err.println("[Aura Target Storage QA] FAIL: " + message);
    }
}
