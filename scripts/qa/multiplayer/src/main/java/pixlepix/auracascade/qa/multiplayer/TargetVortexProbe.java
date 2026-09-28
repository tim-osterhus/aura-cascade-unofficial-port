package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.VortexControllerBlockEntity;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

/** Opt-in real crystal-feed and full White Arcane Gem Vortex probe. */
public final class TargetVortexProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.targetVortex";
    private static final String RECIPE_ID = "data/aura/recipes/vortex/arcane_gem_white.json";
    private static final int PRECONDITION_TICKS = 25, MAX_TICKS = 900, CRYSTALS_PER_NODE = 20;
    private static final int CRYSTAL_CHARGE = 1_000, FALL_DISTANCE = 5;
    private static final int[] REQUIRED_POWER = {60_000, 20_000, 20_000, 20_000};
    private static final List<String> SIDES = List.of("north", "east", "south", "west");
    private static final BlockPos CENTER = new BlockPos(488, 180, 488);
    private static final List<BlockPos> PEDESTALS = List.of(CENTER.north(), CENTER.east(), CENTER.south(), CENTER.west());
    private static final List<BlockPos> NODES = PEDESTALS.stream().map(pos -> pos.above(FALL_DISTANCE)).toList();

    private final List<BlockPos> placed = new ArrayList<>();
    private final List<ItemEntity> crystals = new ArrayList<>();
    private final int[] maxReceipts = new int[4];
    private Path output, temporaryOutput;
    private JsonObject report;
    private ServerLevel level;
    private boolean begun, chunkForced, crystalsSeeded, craftObserved, finished;
    private int elapsedTicks, pedestalAuraAtLastSample;
    private long generatedPowerObserved;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX) || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) return;
        try {
            String configured = System.getProperty(PREFIX + ".output", "").trim();
            if (configured.isEmpty()) throw new IllegalArgumentException("property " + PREFIX + ".output is required");
            output = Path.of(configured).toAbsolutePath().normalize();
            if (output.getFileName() == null) throw new IllegalArgumentException("output must name a file");
            temporaryOutput = output.resolveSibling(output.getFileName() + ".tmp");
            if (Files.exists(output) || Files.exists(temporaryOutput)) throw new IllegalArgumentException("report path is not fresh");
            ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
        } catch (RuntimeException error) {
            failToConsole("reportPath: " + error.getMessage());
        }
    }

    private void serverTick(MinecraftServer server) {
        if (finished) return;
        try {
            if (!begun) { begin(server); return; }
            elapsedTicks++;
            if (elapsedTicks == PRECONDITION_TICKS) { verifyUnpoweredControl(); seedCrystals(); }
            if (crystalsSeeded) {
                observeNaturalGeneration();
                require(whiteAuraBalance() == 80_000, "auraConservation: White aura plus unabsorbed crystals differs from 80000");
                int gems = itemCounts().getOrDefault("aura:arcane_gem_white", 0);
                require(gems <= 1, "outputConservation: produced multiple White Arcane Gems");
                if (gems == 1) {
                    if (!craftObserved) require(pedestalItems().isEmpty(), "outputConservation: recipe inputs remain on pedestals");
                    craftObserved = true;
                }
                if (craftObserved && allCrystalsAbsorbed() && receiptsCleared()) {
                    verifyFinalState();
                    finish(true, null, false);
                    return;
                }
            }
            if (elapsedTicks >= MAX_TICKS) {
                finish(false, "timeout: full White gem and crystal absorption did not complete within " + MAX_TICKS + " ticks", true);
                return;
            }
            refreshReport();
            if (elapsedTicks % 20 == 0 || elapsedTicks == PRECONDITION_TICKS) publishReport();
        } catch (Exception | AssertionError error) {
            finish(false, describe(error), false);
        }
    }

    private void begin(MinecraftServer server) throws Exception {
        report = new JsonObject();
        report.addProperty("probe", "targetVortex");
        report.addProperty("reportPath", output.toString());
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("status", "RUNNING");
        report.addProperty("complete", false);
        report.addProperty("success", false);
        report.addProperty("recipe", RECIPE_ID);
        report.addProperty("seededMaterials", "1 diamond, 3 White Arcane Ingots, 20 White crystals per upper node");
        publishReport();
        require(server.isDedicatedServer(), "dedicatedServer: expected packaged dedicated-server execution");
        recordCandidateOrigin();
        level = server.overworld();
        level.setChunkForced(CENTER.getX() >> 4, CENTER.getZ() >> 4, true);
        chunkForced = true;
        level.getChunkAt(CENTER);
        requireFixtureClear();
        place(CENTER, AuraContent.VORTEX_CONTROLLER.defaultBlockState());
        for (BlockPos pos : PEDESTALS) place(pos, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        for (BlockPos pos : NODES) place(pos, AuraContent.AURA_NODE.defaultBlockState());
        Item[] materials = {Items.DIAMOND, AuraItems.arcaneIngot(AuraColor.WHITE),
            AuraItems.arcaneIngot(AuraColor.WHITE), AuraItems.arcaneIngot(AuraColor.WHITE)};
        for (int i = 0; i < PEDESTALS.size(); i++) {
            BlockPos pos = PEDESTALS.get(i);
            seedItem(materials[i], pos.getX() + .5D, pos.getY() + 1.1D, pos.getZ() + .5D, 1);
        }
        begun = true;
        report.addProperty("fixture", "controller=" + CENTER + "; ordinary nodes=" + NODES + "; fallDistance=5");
        refreshReport();
        publishReport();
    }

    private void recordCandidateOrigin() throws Exception {
        String expected = System.getProperty(PREFIX + ".sha256", "").trim();
        require(!expected.isEmpty(), "candidateSha256: property " + PREFIX + ".sha256 is required");
        String namespace = FabricLoader.getInstance().getMappingResolver().getCurrentRuntimeNamespace();
        require("intermediary".equals(namespace), "runtimeNamespace: expected intermediary, got " + namespace);
        var aura = FabricLoader.getInstance().getModContainer("aura").orElseThrow(
            () -> new AssertionError("candidateOrigin: Aura mod container is absent"));
        ModOrigin origin = aura.getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1,
            "candidateOrigin: expected one packaged Aura JAR path");
        Path jar = origin.getPaths().getFirst().toAbsolutePath().normalize();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"), "candidateOrigin: not a JAR: " + jar);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        require(hash.equalsIgnoreCase(expected), "candidateSha256: loaded Aura JAR hash did not match");
        report.addProperty("runtimeNamespace", namespace);
        report.addProperty("auraOrigin", jar.toString());
        report.addProperty("auraSha256", hash);
    }

    private void requireFixtureClear() {
        require(level.getEntitiesOfClass(ItemEntity.class, fixtureBounds()).isEmpty(), "fixtureClear: items already occupy " + CENTER);
        for (BlockPos pos : fixtureBlocks()) require(level.getBlockState(pos).isAir(), "fixtureClear: block at " + pos);
    }

    private void place(BlockPos pos, BlockState state) {
        require(level.getBlockState(pos).isAir() && level.setBlockAndUpdate(pos, state), "fixturePlacement: failed at " + pos);
        placed.add(pos.immutable());
    }

    private void verifyUnpoweredControl() {
        var snapshot = controller().inspectionSnapshot(level, CENTER);
        require(snapshot.recipeId().equals(RECIPE_ID) && snapshot.requiredPower() == 120_000
                && snapshot.receivedPower() == 0 && snapshot.pedestals().size() == 4,
            "unpoweredControl: live arcane_gem_white recipe mismatch: " + snapshot);
        require(pedestalItems().equals(Map.of("minecraft:diamond", 1, "aura:arcane_ingot_white", 3)),
            "unpoweredControl: expected exactly one diamond and three ingots on pedestals");
        for (int i = 0; i < PEDESTALS.size(); i++) {
            var receipt = pedestal(PEDESTALS.get(i)).inspectionSnapshot();
            require(receipt.color() == AuraColor.WHITE && receipt.required() == REQUIRED_POWER[i] && receipt.received() == 0,
                "unpoweredControl: incorrect " + SIDES.get(i) + " pedestal requirement: " + receipt);
            requireLinks(PEDESTALS.get(i), 1);
            requireLinks(NODES.get(i), 2);
        }
        require(whiteAuraBalance() == 0, "unpoweredControl: aura existed before crystal feed");
    }

    private void seedCrystals() {
        for (BlockPos pos : NODES) crystals.add(seedItem(AuraItems.crystal(AuraColor.WHITE),
            pos.getX() + .5D, pos.getY() + 1.1D, pos.getZ() + .5D, CRYSTALS_PER_NODE));
        crystalsSeeded = true;
    }

    private void observeNaturalGeneration() {
        int pedestalAura = 0;
        for (int i = 0; i < PEDESTALS.size(); i++) {
            VortexPedestalBlockEntity pedestal = pedestal(PEDESTALS.get(i));
            pedestalAura += pedestal.inspectionState().storage().get(AuraColor.WHITE);
            maxReceipts[i] = Math.max(maxReceipts[i], pedestal.inspectionSnapshot().received());
        }
        int moved = pedestalAura - pedestalAuraAtLastSample;
        require(moved >= 0, "naturalTransfer: pedestal aura decreased");
        // White has unit relative mass; each observed pedestal storage increase fell exactly five blocks.
        generatedPowerObserved += (long) moved * FALL_DISTANCE;
        pedestalAuraAtLastSample = pedestalAura;
    }

    private void verifyFinalState() {
        require(itemCounts().equals(Map.of("aura:arcane_gem_white", 1)), "outputConservation: expected one gem and no loose items");
        require(pedestalItems().isEmpty(), "outputConservation: pedestal inputs remain");
        require(allCrystalsAbsorbed(), "crystalFeed: White crystals remain unabsorbed");
        for (int i = 0; i < PEDESTALS.size(); i++) {
            require(maxReceipts[i] == REQUIRED_POWER[i], "naturalTransfer: " + SIDES.get(i) + " receipt did not meet requirement");
            var receipt = pedestal(PEDESTALS.get(i)).inspectionSnapshot();
            require(receipt.received() == 0 && receipt.required() == 0 && receipt.color() == null,
                "receiptClear: " + SIDES.get(i) + " receipt was not cleared: " + receipt);
        }
        require(generatedPowerObserved >= 120_000, "naturalTransfer: generated power observed below recipe total");
        require(storedWhiteAura() == 80_000 && whiteAuraBalance() == 80_000, "auraConservation: final stored total was not 80000");
    }

    private void requireLinks(BlockPos pos, int count) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof AuraNetworkBlockEntity, "networkLinks: missing node at " + pos);
        var state = ((AuraNetworkBlockEntity) entity).inspectionState();
        require(state.hasScannedLinks() && state.linkedNodeCount() == count, "networkLinks: unexpected links at " + pos + ": " + state);
    }

    private int storedWhiteAura() {
        int total = 0;
        for (BlockPos pos : networkBlocks()) {
            BlockEntity entity = level.getBlockEntity(pos);
            require(entity instanceof AuraNetworkBlockEntity, "auraConservation: missing node at " + pos);
            var storage = ((AuraNetworkBlockEntity) entity).inspectionState().storage();
            require(storage.total() == storage.get(AuraColor.WHITE), "auraConservation: non-White aura at " + pos);
            total += storage.get(AuraColor.WHITE);
        }
        return total;
    }

    private int whiteAuraBalance() {
        int total = storedWhiteAura();
        for (ItemEntity crystal : crystals) if (!crystal.isRemoved()) total += crystal.getItem().getCount() * CRYSTAL_CHARGE;
        return total;
    }

    private boolean allCrystalsAbsorbed() {
        return crystals.stream().allMatch(crystal -> crystal.isRemoved() && crystal.getItem().isEmpty());
    }

    private boolean receiptsCleared() {
        for (BlockPos pos : PEDESTALS) {
            var receipt = pedestal(pos).inspectionSnapshot();
            if (receipt.received() != 0 || receipt.required() != 0 || receipt.color() != null) return false;
        }
        return true;
    }

    private VortexControllerBlockEntity controller() {
        BlockEntity entity = level.getBlockEntity(CENTER);
        require(entity instanceof VortexControllerBlockEntity, "vortexFixture: controller is absent");
        return (VortexControllerBlockEntity) entity;
    }

    private VortexPedestalBlockEntity pedestal(BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof VortexPedestalBlockEntity, "vortexFixture: pedestal absent at " + pos);
        return (VortexPedestalBlockEntity) entity;
    }

    private Map<String, Integer> pedestalItems() {
        TreeMap<String, Integer> items = new TreeMap<>();
        for (BlockPos pos : PEDESTALS) {
            ItemStack stack = pedestal(pos).heldItem();
            if (!stack.isEmpty()) items.merge(itemId(stack.getItem()), stack.getCount(), Integer::sum);
        }
        return items;
    }

    private Map<String, Integer> itemCounts() {
        TreeMap<String, Integer> items = new TreeMap<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, fixtureBounds())) {
            if (!entity.isRemoved() && !entity.getItem().isEmpty()) {
                items.merge(itemId(entity.getItem().getItem()), entity.getItem().getCount(), Integer::sum);
            }
        }
        return items;
    }

    private ItemEntity seedItem(Item item, double x, double y, double z, int count) {
        ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(item, count));
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(32_767);
        require(level.addFreshEntity(entity), "fixtureItem: could not spawn " + item);
        return entity;
    }

    private void refreshReport() {
        report.addProperty("elapsedServerTicks", elapsedTicks);
        report.addProperty("tickLimit", MAX_TICKS);
        report.addProperty("crystalsSeeded", crystalsSeeded);
        report.addProperty("crystalsRemaining", crystals.stream().filter(entity -> !entity.isRemoved())
            .mapToInt(entity -> entity.getItem().getCount()).sum());
        report.addProperty("whiteAuraStoredInNodesAndPedestals", begun ? storedWhiteAura() : 0);
        report.addProperty("whiteAuraBalanceIncludingCrystalItems", begun ? whiteAuraBalance() : 0);
        report.addProperty("realGeneratedPower", generatedPowerObserved);
        report.addProperty("craftObserved", craftObserved);
        report.addProperty("gemOutputCount", begun ? itemCounts().getOrDefault("aura:arcane_gem_white", 0) : 0);
        JsonObject receipts = new JsonObject();
        for (int i = 0; i < SIDES.size(); i++) receipts.addProperty(SIDES.get(i), maxReceipts[i]);
        report.add("maxReceipts", receipts);
    }

    private void finish(boolean success, String failure, boolean timeout) {
        if (finished) return;
        finished = true;
        if (report == null) { report = new JsonObject(); report.addProperty("probe", "targetVortex"); }
        else if (begun) {
            try { refreshReport(); }
            catch (Exception | AssertionError error) {
                success = false;
                failure = (failure == null ? "" : failure + "; ") + "finalSnapshot: " + describe(error);
            }
        }
        report.addProperty("success", success);
        report.addProperty("complete", true);
        report.addProperty("status", success ? "PASS" : timeout ? "TIMEOUT" : "FAIL");
        report.addProperty("timedOut", timeout);
        report.addProperty("finishedAt", Instant.now().toString());
        if (failure != null) report.addProperty("failure", failure);
        try { cleanupFixture(); report.addProperty("cleanup", "PASS"); }
        catch (Exception | AssertionError error) {
            report.addProperty("cleanup", "FAIL: " + describe(error));
            report.addProperty("success", false);
            report.addProperty("status", "FAIL");
            report.addProperty("failure", (failure == null ? "" : failure + "; ") + "cleanup: " + describe(error));
        }
        try { publishReport(); }
        catch (Exception error) { failToConsole("reportWrite: " + describe(error)); }
    }

    private void cleanupFixture() {
        if (level == null) return;
        for (int i = placed.size() - 1; i >= 0; i--) level.setBlockAndUpdate(placed.get(i), Blocks.AIR.defaultBlockState());
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, fixtureBounds())) entity.discard();
        if (chunkForced) level.setChunkForced(CENTER.getX() >> 4, CENTER.getZ() >> 4, false);
        placed.clear();
    }

    private static List<BlockPos> fixtureBlocks() {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        blocks.add(CENTER); blocks.addAll(PEDESTALS); blocks.addAll(NODES);
        return blocks;
    }

    private static List<BlockPos> networkBlocks() {
        ArrayList<BlockPos> blocks = new ArrayList<>(NODES);
        blocks.addAll(PEDESTALS);
        return blocks;
    }

    private static AABB fixtureBounds() { return new AABB(CENTER).inflate(4.0D, 256.0D, 4.0D); }

    private void publishReport() throws Exception {
        Files.createDirectories(output.getParent());
        Files.writeString(temporaryOutput, new GsonBuilder().setPrettyPrinting().create().toJson(report),
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try { Files.move(temporaryOutput, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException error) { Files.move(temporaryOutput, output, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static String itemId(Item item) { return BuiltInRegistries.ITEM.getKey(item).toString(); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static String describe(Throwable error) {
        return error.getClass().getSimpleName() + (error.getMessage() == null ? "" : ": " + error.getMessage());
    }
    private static void failToConsole(String message) { System.err.println("[Aura Target Vortex QA] FAIL: " + message); }
}
