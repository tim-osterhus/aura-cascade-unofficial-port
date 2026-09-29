package pixlepix.auracascade.qa.neoforge.persistence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.books.StorageBookVariant;
import pixlepix.auracascade.parity.AuraColor;

final class DiskRestartRun implements PersistenceQaMod.PersistenceTickRun {
    private static final int MAX_PREPARE_TICKS = 250;
    private static final int RAW_CRYSTALS = 3;
    private static final String BOOK_NAME = "Aura QA MOD storage";

    private final CommandSourceStack source;
    private final ServerLevel level;
    private final QaPersistenceSupport evidence;
    private final String runId;
    private final BlockPos origin;
    private final BlockPos shelfPos;
    private final BlockPos coordinatorPos;
    private final AABB itemBounds;
    private final JsonObject report;
    private int ticks;

    private DiskRestartRun(
        CommandSourceStack source,
        BlockPos origin,
        QaPersistenceSupport evidence,
        String runId,
        JsonObject report
    ) {
        this.source = source;
        this.level = source.getLevel();
        this.evidence = evidence;
        this.runId = runId;
        this.origin = origin.immutable();
        this.shelfPos = origin.east(4);
        this.coordinatorPos = origin.east(3);
        this.itemBounds = new AABB(origin.getX() - 3.0D, origin.getY() - 2.0D, origin.getZ() - 3.0D,
            origin.getX() + 7.0D, origin.getY() + 5.0D, origin.getZ() + 3.0D);
        this.report = report;
    }

    static DiskRestartRun prepare(CommandSourceStack source, BlockPos origin, QaPersistenceSupport evidence, String runId)
        throws Exception {
        JsonObject report = evidence.report("disk-restart-prepare", "RUNNING", runId);
        report.addProperty("stage", "building-production-preconditions");
        report.addProperty("persistenceVerified", false);
        report.addProperty("tickLimit", MAX_PREPARE_TICKS);
        evidence.write(QaPersistenceSupport.PERSISTENCE_EXPECTATION, report.deepCopy());
        JsonObject checkNotRun = evidence.report("disk-restart-check", "NOT_RUN", runId);
        checkNotRun.addProperty("reason", "Awaiting normal server stop, a new process, and the check command.");
        evidence.write(QaPersistenceSupport.PERSISTENCE_RESULT, checkNotRun);

        DiskRestartRun run = new DiskRestartRun(source, origin, evidence, runId, report);
        run.createFixture();
        report.add("positions", run.positionsJson());
        report.add("bookConfig", expectedBookshelf());
        report.add("initialNode", QaPersistenceSupport.nodeSnapshot(run.level, origin));
        evidence.write(QaPersistenceSupport.PERSISTENCE_EXPECTATION, report.deepCopy());
        return run;
    }

    @Override
    public MinecraftServer server() {
        return source.getServer();
    }

    @Override
    public boolean tick() throws Exception {
        ticks++;
        QaPersistenceSupport.require(ticks <= MAX_PREPARE_TICKS,
            "Persistence preconditions were not ready within " + MAX_PREPARE_TICKS + " server ticks");
        if (ticks % 10 == 0) {
            report.addProperty("ticks", ticks);
            report.addProperty("crystalItemsRemaining", crystalCount());
            evidence.write(QaPersistenceSupport.PERSISTENCE_EXPECTATION, report);
        }

        var nodeState = ((AuraNodeBlockEntity) level.getBlockEntity(origin)).inspectionState();
        if (crystalCount() != 0 || !nodeState.hasScannedLinks()) return false;

        JsonObject expected = expectedState();
        JsonObject actual = actualState(level, origin, shelfPos, coordinatorPos);
        report.add("expected", expected.deepCopy());
        report.add("preparedActual", actual.deepCopy());
        QaPersistenceSupport.require(expected.getAsJsonObject("node").equals(actual.getAsJsonObject("node")),
            "Prepared Aura node state differs from fixed expectation: " + actual.getAsJsonObject("node"));
        QaPersistenceSupport.require(expected.getAsJsonObject("bookshelf").equals(actual.getAsJsonObject("bookshelf")),
            "Prepared storage-book state differs from fixed expectation: " + actual.getAsJsonObject("bookshelf"));
        QaPersistenceSupport.require(expected.getAsJsonObject("coordinator").equals(actual.getAsJsonObject("coordinator")),
            "Prepared bookshelf network differs from fixed expectation: " + actual.getAsJsonObject("coordinator"));
        QaPersistenceSupport.require(expected.getAsJsonObject("remainingWorldItems").equals(actual.getAsJsonObject("remainingWorldItems")),
            "Unexpected loose item entities remain around the fixture");

        report.addProperty("status", "PREPARED");
        report.addProperty("success", true);
        report.addProperty("stage", "preconditions-only; persistence-not-yet-verified");
        report.addProperty("persistenceVerified", false);
        report.addProperty("ticks", ticks);
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("dimension", level.dimension().location().toString());
        report.add("positions", positionsJson());
        report.add("expected", expected);
        report.add("preparedActual", actual);
        evidence.write(QaPersistenceSupport.PERSISTENCE_EXPECTATION, report);

        JsonObject prepared = evidence.report("disk-restart-prepare", "PREPARED", runId);
        prepared.addProperty("success", true);
        prepared.addProperty("persistenceVerified", false);
        prepared.addProperty("stage", "preconditions-only; persistence-not-yet-verified");
        prepared.addProperty("dimension", level.dimension().location().toString());
        prepared.addProperty("ticks", ticks);
        prepared.add("positions", positionsJson());
        prepared.add("expected", expected);
        prepared.add("preparedActual", actual);
        evidence.write(QaPersistenceSupport.PERSISTENCE_RESULT, prepared);
        PersistenceQaMod.LOGGER.info("[aura_qa_persistence] COMPLETE scenario=disk-restart-prepare status=PREPARED run={} ticks={} persistenceVerified=false",
            runId, ticks);
        source.sendSuccess(() -> Component.literal(
            "Disk persistence fixture PREPARED; normal-stop this server, start a new process, then run /auraqa persistence check."), true);
        return true;
    }

    @Override
    public void fail(Throwable failure) {
        report.addProperty("status", "FAIL");
        report.addProperty("success", false);
        report.addProperty("persistenceVerified", false);
        report.addProperty("stage", "prepare");
        report.addProperty("ticks", ticks);
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("failure", failure.toString());
        try {
            evidence.write(QaPersistenceSupport.PERSISTENCE_EXPECTATION, report);
            JsonObject result = evidence.report("disk-restart-prepare", "FAIL", runId);
            result.addProperty("success", false);
            result.addProperty("persistenceVerified", false);
            result.addProperty("ticks", ticks);
            result.addProperty("failure", failure.toString());
            evidence.write(QaPersistenceSupport.PERSISTENCE_RESULT, result);
        } catch (Exception writeFailure) {
            PersistenceQaMod.LOGGER.error("[aura_qa_persistence] Could not write persistence preparation failure JSON", writeFailure);
        }
        PersistenceQaMod.LOGGER.error("[aura_qa_persistence] COMPLETE scenario=disk-restart-prepare status=FAIL run={} ticks={}",
            runId, ticks, failure);
        source.sendFailure(Component.literal("Disk persistence fixture preparation FAIL; see result JSON and server log."));
    }

    static boolean check(CommandSourceStack source, QaPersistenceSupport evidence, String runId) throws Exception {
        JsonObject result = evidence.report("disk-restart-check", "RUNNING", runId);
        result.addProperty("persistenceVerified", false);
        evidence.write(QaPersistenceSupport.PERSISTENCE_RESULT, result);
        try {
            JsonObject expectation;
            try (var reader = Files.newBufferedReader(evidence.path(QaPersistenceSupport.PERSISTENCE_EXPECTATION))) {
                expectation = JsonParser.parseReader(reader).getAsJsonObject();
            }
            QaPersistenceSupport.require(expectation.get("status").getAsString().equals("PREPARED"),
                "Expectation file is not a successful PREPARED fixture");
            QaPersistenceSupport.require(expectation.get("persistenceVerified").getAsBoolean() == false,
                "Expectation file is not a pre-restart fixture");
            QaPersistenceSupport.require(expectation.get("schemaVersion").getAsInt() == 1, "Unsupported expectation schema");

            JsonObject preparedProcess = expectation.getAsJsonObject("process");
            QaPersistenceSupport.require(preparedProcess.get("pid").getAsLong() != evidence.process.pid(),
                "Check is running in the preparation process; a new server process is required");
            QaPersistenceSupport.require(!preparedProcess.get("startMarker").getAsString().equals(evidence.process.startMarker()),
                "Check process start marker matches preparation; a new server process is required");
            String preparedHash = expectation.getAsJsonObject("auraJar").get("sha256").getAsString();
            String currentHash = evidence.auraJarHash();
            QaPersistenceSupport.require(preparedHash.equalsIgnoreCase(currentHash),
                "Loaded Aura regular-JAR hash differs from the preparation process");

            String dimension = expectation.get("dimension").getAsString();
            ServerLevel level = findLevel(source.getServer(), dimension);
            JsonObject positions = expectation.getAsJsonObject("positions");
            BlockPos nodePos = QaPersistenceSupport.readPosition(positions.getAsJsonObject("node"));
            BlockPos shelfPos = QaPersistenceSupport.readPosition(positions.getAsJsonObject("bookshelf"));
            BlockPos coordinatorPos = QaPersistenceSupport.readPosition(positions.getAsJsonObject("coordinator"));
            loadFixtureChunks(level, nodePos, shelfPos, coordinatorPos);

            JsonObject expected = expectation.getAsJsonObject("expected");
            JsonObject actual = actualState(level, nodePos, shelfPos, coordinatorPos);
            result.addProperty("preparedRunId", expectation.get("runId").getAsString());
            result.add("preparedProcess", preparedProcess.deepCopy());
            result.addProperty("dimension", dimension);
            result.add("positions", positions.deepCopy());
            result.add("expected", expected.deepCopy());
            result.add("actual", actual);

            for (String key : new String[]{"node", "bookshelf", "coordinator", "remainingWorldItems"}) {
                QaPersistenceSupport.require(expected.getAsJsonObject(key).equals(actual.getAsJsonObject(key)),
                    "Reloaded " + key + " differs from the persisted expectation: actual=" + actual.getAsJsonObject(key));
            }

            result.addProperty("status", "PASS");
            result.addProperty("success", true);
            result.addProperty("persistenceVerified", true);
            result.addProperty("finishedAt", Instant.now().toString());
            evidence.write(QaPersistenceSupport.PERSISTENCE_RESULT, result);
            PersistenceQaMod.LOGGER.info("[aura_qa_persistence] COMPLETE scenario=disk-restart-check status=PASS run={} preparedRun={} preparedPid={} checkedPid={}",
                runId, expectation.get("runId").getAsString(), preparedProcess.get("pid").getAsLong(), evidence.process.pid());
            source.sendSuccess(() -> Component.literal("Disk persistence QA PASS after a distinct process restart; result JSON recorded."), true);
            return true;
        } catch (Exception | AssertionError failure) {
            result.addProperty("status", "FAIL");
            result.addProperty("success", false);
            result.addProperty("persistenceVerified", false);
            result.addProperty("finishedAt", Instant.now().toString());
            result.addProperty("failure", failure.toString());
            evidence.write(QaPersistenceSupport.PERSISTENCE_RESULT, result);
            PersistenceQaMod.LOGGER.error("[aura_qa_persistence] COMPLETE scenario=disk-restart-check status=FAIL run={}", runId, failure);
            source.sendFailure(Component.literal("Disk persistence QA FAIL: " + failure.getMessage()));
            return false;
        }
    }

    private void createFixture() {
        QaPersistenceSupport.require(origin.getY() - 1 >= level.getMinBuildHeight()
                && origin.getY() < level.getMaxBuildHeight(),
            "Fixture origin is too close to a world height limit");
        loadFixtureChunks(level, origin, shelfPos, coordinatorPos);
        for (BlockPos pos : new BlockPos[]{origin, shelfPos, coordinatorPos}) {
            QaPersistenceSupport.require(level.getBlockState(pos).isAir(), "Fixture block position is occupied: " + pos);
        }
        for (int x = origin.getX() - 1; x <= origin.getX() + 4; x++) {
            for (int z = origin.getZ() - 1; z <= origin.getZ() + 1; z++) {
                BlockPos floor = new BlockPos(x, origin.getY() - 1, z);
                QaPersistenceSupport.require(level.getBlockState(floor).isAir(), "Fixture floor position is occupied: " + floor);
            }
        }
        QaPersistenceSupport.require(level.getEntitiesOfClass(ItemEntity.class, itemBounds, entity -> !entity.isRemoved()).isEmpty(),
            "Fixture bounds already contain item entities");
        QaPersistenceSupport.require(level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, itemBounds,
            player -> !player.isSpectator()).isEmpty(), "A player is inside the disposable fixture bounds");

        for (int x = origin.getX() - 1; x <= origin.getX() + 4; x++) {
            for (int z = origin.getZ() - 1; z <= origin.getZ() + 1; z++) {
                level.setBlock(new BlockPos(x, origin.getY() - 1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        place(origin, AuraContent.AURA_NODE);
        place(shelfPos, AuraContent.STORAGE_BOOKSHELF);
        place(coordinatorPos, AuraContent.BOOKSHELF_COORDINATOR);

        StorageBookshelfBlockEntity shelf = (StorageBookshelfBlockEntity) level.getBlockEntity(shelfPos);
        ItemStack book = new ItemStack(AuraItems.storageBook(StorageBookVariant.MOD));
        book.set(DataComponents.CUSTOM_NAME, Component.literal(BOOK_NAME));
        QaPersistenceSupport.require(shelf.setBook(book), "Production bookshelf helper rejected the storage book");
        QaPersistenceSupport.require(shelf.deposit(new ItemStack(AuraItems.crystal(AuraColor.WHITE), 2)) == 2,
            "Production storage-book helper did not accept the first Aura item stack");
        QaPersistenceSupport.require(shelf.deposit(new ItemStack(AuraItems.arcaneIngot(AuraColor.WHITE), 3)) == 3,
            "Production storage-book helper did not accept the second Aura item stack");
        QaPersistenceSupport.drop(level, origin, new ItemStack(AuraItems.crystal(AuraColor.WHITE), RAW_CRYSTALS), 0.5D);
    }

    private void place(BlockPos pos, Block block) {
        QaPersistenceSupport.require(level.setBlock(pos, block.defaultBlockState(), 3), "Could not place fixture block at " + pos);
        QaPersistenceSupport.require(level.getBlockEntity(pos) != null, "Fixture block did not create a block entity at " + pos);
    }

    private int crystalCount() {
        return QaPersistenceSupport.itemCount(level, itemBounds, AuraItems.crystal(AuraColor.WHITE));
    }

    private JsonObject expectedState() {
        JsonObject expected = new JsonObject();
        JsonObject node = new JsonObject();
        node.addProperty("block", "aura:aura_node");
        JsonObject aura = new JsonObject();
        for (AuraColor color : AuraColor.values()) aura.addProperty(color.id(), color == AuraColor.WHITE ? RAW_CRYSTALS * 1_000 : 0);
        node.add("aura", aura);
        node.addProperty("totalAura", RAW_CRYSTALS * 1_000);
        node.addProperty("storedPower", 0);
        node.addProperty("linkedNodes", 0);
        node.addProperty("hasScannedLinks", true);
        expected.add("node", node);
        expected.add("bookshelf", expectedBookshelf());
        expected.add("coordinator", expectedCoordinator());
        expected.add("remainingWorldItems", new JsonObject());
        return expected;
    }

    private static JsonObject expectedBookshelf() {
        JsonObject shelf = new JsonObject();
        shelf.addProperty("block", "aura:storage_bookshelf");
        shelf.addProperty("bookItem", "aura:mod_storage_book");
        shelf.addProperty("bookVariant", StorageBookVariant.MOD.registryPath());
        shelf.addProperty("bookName", BOOK_NAME);
        shelf.addProperty("bookCount", 1);
        shelf.add("entryCounts", QaPersistenceSupport.countsJson(Map.of(
            "aura:aura_crystal_white", 2,
            "aura:arcane_ingot_white", 3
        )));
        shelf.addProperty("storedTypes", 2);
        shelf.addProperty("storedItemCount", 5);
        return shelf;
    }

    private static JsonObject expectedCoordinator() {
        JsonObject coordinator = new JsonObject();
        coordinator.addProperty("block", "aura:bookshelf_coordinator");
        coordinator.addProperty("connectedShelves", 1);
        coordinator.addProperty("storageShelves", 1);
        coordinator.addProperty("requiredPower", 5);
        coordinator.addProperty("availablePower", 0);
        coordinator.addProperty("complete", true);
        coordinator.add("browserEntryCounts", QaPersistenceSupport.countsJson(Map.of(
            "aura:aura_crystal_white", 2,
            "aura:arcane_ingot_white", 3
        )));
        return coordinator;
    }

    private static JsonObject actualState(ServerLevel level, BlockPos nodePos, BlockPos shelfPos, BlockPos coordinatorPos) {
        JsonObject actual = new JsonObject();
        actual.add("node", QaPersistenceSupport.nodeSnapshot(level, nodePos));
        actual.add("bookshelf", QaPersistenceSupport.bookShelfSnapshot(level, shelfPos));
        actual.add("coordinator", QaPersistenceSupport.coordinatorSnapshot(level, coordinatorPos));
        AABB bounds = new AABB(nodePos.getX() - 3.0D, nodePos.getY() - 2.0D, nodePos.getZ() - 3.0D,
            nodePos.getX() + 7.0D, nodePos.getY() + 5.0D, nodePos.getZ() + 3.0D);
        actual.add("remainingWorldItems", QaPersistenceSupport.countsJson(QaPersistenceSupport.itemCounts(level, bounds)));
        return actual;
    }

    private JsonObject positionsJson() {
        JsonObject positions = new JsonObject();
        positions.add("node", QaPersistenceSupport.position(origin));
        positions.add("bookshelf", QaPersistenceSupport.position(shelfPos));
        positions.add("coordinator", QaPersistenceSupport.position(coordinatorPos));
        return positions;
    }

    private static void loadFixtureChunks(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) {
            level.getChunkAt(pos);
            for (Direction direction : Direction.values()) {
                level.getChunkAt(pos.relative(direction));
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                level.getChunkAt(pos.relative(direction, 15));
            }
        }
    }

    private static ServerLevel findLevel(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.dimension().location().toString().equals(dimension)) return level;
        }
        throw new IllegalStateException("Dimension is not present after restart: " + dimension);
    }
}
