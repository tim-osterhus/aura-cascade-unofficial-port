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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.aura.AuraInspectionState;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraConsumerLogic;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.VortexControllerBlockEntity;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

/** Opt-in packaged-server progression and real partial Vortex-receipt persistence probe. */
public final class TargetProgressionProbe implements ModInitializer {
    private static final String PREFIX = "aura.qa.targetProgression";
    private static final int PRECONDITION_TICKS = 25;
    private static final int MAX_SERVER_TICKS = 1_800;
    private static final int CRYSTAL_CHARGE = 1_000;
    private static final int VORTEX_WHITE_REQUIREMENT = 50_000;
    private static final String RING_RECIPE = "data/aura/recipes/vortex/ring_of_binding.json";

    private static final BlockPos PROCESSOR = new BlockPos(359, 180, 240);
    private static final BlockPos PROCESSOR_SOURCE = new BlockPos(360, 180, 240);
    private static final BlockPos PROCESSOR_PUMP = new BlockPos(360, 180, 239);
    private static final BlockPos PROCESSOR_PUMP_TOP = new BlockPos(360, 185, 239);
    private static final BlockPos PROCESSOR_TOP = new BlockPos(360, 185, 240);

    private static final BlockPos VORTEX_CONTROLLER = new BlockPos(392, 180, 240);
    private static final BlockPos VORTEX_NORTH = new BlockPos(392, 180, 239);
    private static final BlockPos VORTEX_EAST = new BlockPos(393, 180, 240);
    private static final BlockPos VORTEX_SOUTH = new BlockPos(392, 180, 241);
    private static final BlockPos VORTEX_WEST = new BlockPos(391, 180, 240);
    private static final BlockPos VORTEX_PUMP = new BlockPos(394, 180, 240);
    private static final BlockPos VORTEX_SOURCE = new BlockPos(395, 180, 240);
    private static final BlockPos VORTEX_PUMP_TOP = new BlockPos(394, 185, 240);
    private static final BlockPos VORTEX_TOP = new BlockPos(393, 185, 240);

    private final List<BlockPos> placedBlocks = new ArrayList<>();
    private final List<int[]> forcedChunks = new ArrayList<>();
    private final JsonObject checks = new JsonObject();

    private Path output;
    private Path temporaryOutput;
    private JsonObject report;
    private ServerLevel level;
    private ItemEntity processorWool;
    private ItemEntity processorIron;
    private ItemEntity processorCrystals;
    private ItemEntity processorCoal;
    private ItemEntity vortexCrystals;
    private ItemEntity vortexCoal;
    private boolean begun;
    private boolean finished;
    private boolean processorFuelObserved;
    private boolean vortexFuelObserved;
    private boolean processorComplete;
    private boolean vortexReloadComplete;
    private int elapsedServerTicks;
    private long startGameTime;
    private long previousCombinedPower;
    private int previousConsumerPower;
    private int previousProgress;
    private long processorGeneratedPower;
    private long processorSpentPower;
    private long processorBleedPower;
    private long processorProgressSteps;
    private int receiptAtReload;

    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean(PREFIX)
            || FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER) {
            return;
        }

        String configuredOutput = System.getProperty(PREFIX + ".output", "").trim();
        if (configuredOutput.isEmpty()) {
            failToConsole("reportPath: property " + PREFIX + ".output must name a fresh report file");
            return;
        }
        try {
            output = Path.of(configuredOutput).toAbsolutePath().normalize();
            temporaryOutput = output.resolveSibling(output.getFileName() + ".tmp");
        } catch (RuntimeException error) {
            failToConsole("reportPath: invalid output path: " + error);
            return;
        }
        if (Files.exists(output) || Files.exists(temporaryOutput)) {
            failToConsole("freshReportPath: refusing existing report or temporary path " + output);
            return;
        }

        ServerTickEvents.END_SERVER_TICK.register(this::serverTick);
    }

    private void serverTick(MinecraftServer server) {
        if (finished) {
            return;
        }
        try {
            if (!begun) {
                begin(server);
                return;
            }

            elapsedServerTicks++;
            observeProcessorPower();
            observeFuelAndConservation();

            if (elapsedServerTicks == PRECONDITION_TICKS) {
                verifyUnpoweredControl();
                seedFuelAndAura();
            }
            if (processorCrystals != null) {
                verifyVortexRecipeAndReceipt();
                observeProcessorOutput();
            }

            refreshReport();
            if (processorComplete && vortexReloadComplete) {
                verifyFinalState();
                finish(true, null, false);
                return;
            }
            if (elapsedServerTicks >= MAX_SERVER_TICKS) {
                finish(false, "timeout: progression did not complete within " + MAX_SERVER_TICKS
                    + " server ticks", true);
                return;
            }
            if (elapsedServerTicks % 20 == 0 || elapsedServerTicks == PRECONDITION_TICKS) {
                publishReport();
            }
        } catch (Exception | AssertionError error) {
            finish(false, describe(error), false);
        }
    }

    private void begin(MinecraftServer server) throws Exception {
        report = new JsonObject();
        report.addProperty("probe", "targetProgression");
        report.addProperty("reportPath", output.toString());
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("status", "RUNNING");
        report.addProperty("complete", false);
        report.addProperty("success", false);
        report.addProperty("serverKind", server.isDedicatedServer() ? "dedicated" : "integrated");
        report.addProperty("scope", "Full White Arcane Ingot progression plus a real partial ring-of-binding Vortex receipt round-trip; no Prism cycle.");
        report.add("checks", checks);
        publishReport();

        require(server.isDedicatedServer(), "dedicatedServer: expected packaged dedicated-server execution");
        recordCandidateOrigin();
        level = server.overworld();
        forceFixtureChunks();
        requireFixtureClear(new BlockPos(360, 180, 240));
        requireFixtureClear(new BlockPos(392, 180, 240));
        buildProcessorFixture();
        buildVortexFixture();
        seedProcessorInputs();
        seedVortexInputs();

        startGameTime = level.getGameTime();
        previousCombinedPower = processorCombinedPower();
        previousConsumerPower = processor().inspectionState().storedPower();
        previousProgress = processor().inspectionState().progress();
        require(previousCombinedPower == 0 && previousConsumerPower == 0 && previousProgress == 0,
            "freshProcessor: expected zero node power, zero consumer power, and zero progress");
        report.add("seededMaterials", seededMaterials());
        begun = true;
        refreshReport();
        publishReport();
    }

    private void recordCandidateOrigin() throws Exception {
        String expectedHash = System.getProperty(PREFIX + ".sha256", "").trim();
        require(!expectedHash.isEmpty(), "candidateSha256: property " + PREFIX + ".sha256 is required");
        var loader = FabricLoader.getInstance();
        String namespace = loader.getMappingResolver().getCurrentRuntimeNamespace();
        require("intermediary".equals(namespace), "runtimeNamespace: expected intermediary, got " + namespace);
        var aura = loader.getModContainer("aura").orElseThrow(
            () -> new AssertionError("candidateOrigin: Aura mod container is absent"));
        ModOrigin origin = aura.getOrigin();
        require(origin.getKind() == ModOrigin.Kind.PATH && origin.getPaths().size() == 1,
            "candidateOrigin: expected exactly one JAR path");
        Path jar = origin.getPaths().getFirst().toAbsolutePath().normalize();
        require(Files.isRegularFile(jar) && jar.toString().endsWith(".jar"),
            "candidateOrigin: expected a packaged Aura JAR, got " + jar);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(jar)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                digest.update(buffer, 0, length);
            }
        }
        String hash = HexFormat.of().formatHex(digest.digest());
        require(hash.equalsIgnoreCase(expectedHash), "candidateSha256: loaded Aura JAR does not match requested hash");
        report.addProperty("runtimeNamespace", namespace);
        report.addProperty("auraOrigin", jar.toString());
        report.addProperty("auraSha256", hash);
        report.addProperty("auraVersion", aura.getMetadata().getVersion().getFriendlyString());
        pass("packagedCandidate", "intermediary runtime loaded the requested Aura JAR hash");
    }

    private void forceFixtureChunks() {
        forceChunk(22, 14);
        forceChunk(22, 15);
        forceChunk(24, 14);
        forceChunk(24, 15);
    }

    private void forceChunk(int x, int z) {
        level.setChunkForced(x, z, true);
        forcedChunks.add(new int[] {x, z});
        level.getChunkAt(new BlockPos((x << 4) + 8, 180, (z << 4) + 8));
    }

    private void requireFixtureClear(BlockPos center) {
        AABB bounds = fixtureBounds(center);
        require(level.getEntitiesOfClass(ItemEntity.class, bounds).isEmpty(),
            "fixtureClear: item entities already occupy " + center);
        for (BlockPos pos : fixtureBlockPositions(center)) {
            require(level.getBlockState(pos).isAir(), "fixtureClear: machine position is not air at " + pos);
        }
    }

    private List<BlockPos> fixtureBlockPositions(BlockPos center) {
        if (center.equals(new BlockPos(360, 180, 240))) {
            return List.of(PROCESSOR, PROCESSOR_SOURCE, PROCESSOR_PUMP, PROCESSOR_PUMP_TOP, PROCESSOR_TOP);
        }
        return List.of(VORTEX_CONTROLLER, VORTEX_NORTH, VORTEX_EAST, VORTEX_SOUTH, VORTEX_WEST,
            VORTEX_PUMP, VORTEX_SOURCE, VORTEX_PUMP_TOP, VORTEX_TOP);
    }

    private void buildProcessorFixture() {
        place(PROCESSOR_SOURCE, AuraContent.AURA_NODE.defaultBlockState());
        place(PROCESSOR_PUMP, AuraContent.AURA_NODE_PUMP.defaultBlockState());
        place(PROCESSOR_PUMP_TOP, AuraContent.AURA_NODE.defaultBlockState());
        place(PROCESSOR_TOP, AuraContent.AURA_NODE.defaultBlockState());
        place(PROCESSOR, AuraContent.CONSUMER_BLOCK_ORE.defaultBlockState());
    }

    private void buildVortexFixture() {
        place(VORTEX_CONTROLLER, AuraContent.VORTEX_CONTROLLER.defaultBlockState());
        place(VORTEX_NORTH, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        place(VORTEX_EAST, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        place(VORTEX_SOUTH, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        place(VORTEX_WEST, AuraContent.VORTEX_PEDESTAL.defaultBlockState());
        place(VORTEX_SOURCE, AuraContent.AURA_NODE.defaultBlockState());
        place(VORTEX_PUMP, AuraContent.AURA_NODE_PUMP.defaultBlockState());
        place(VORTEX_PUMP_TOP, AuraContent.AURA_NODE.defaultBlockState());
        place(VORTEX_TOP, AuraContent.AURA_NODE.defaultBlockState());
    }

    private void place(BlockPos pos, BlockState state) {
        require(level.getBlockState(pos).isAir(), "fixturePlacement: refusing to replace non-air block at " + pos);
        require(level.setBlockAndUpdate(pos, state), "fixturePlacement: could not place " + state + " at " + pos);
        placedBlocks.add(pos.immutable());
    }

    private void seedProcessorInputs() {
        processorWool = seedItem(Items.WHITE_WOOL, 1, 356.5D, 181.2D, 240.5D);
        processorIron = seedItem(Items.IRON_INGOT, 1, 356.5D, 181.2D, 241.5D);
    }

    private void seedVortexInputs() {
        seedAtPedestal(Items.DIAMOND_BLOCK, VORTEX_NORTH);
        seedAtPedestal(Items.OBSIDIAN, VORTEX_EAST);
        seedAtPedestal(Items.GOLD_BLOCK, VORTEX_SOUTH);
        seedAtPedestal(Items.REDSTONE_BLOCK, VORTEX_WEST);
    }

    private void seedAtPedestal(Item item, BlockPos pedestal) {
        seedItem(item, 1, pedestal.getX() + 0.5D, pedestal.getY() + 1.1D, pedestal.getZ() + 0.5D);
    }

    private ItemEntity seedItem(Item item, int count, double x, double y, double z) {
        ItemEntity entity = new ItemEntity(level, x, y, z, new ItemStack(item, count));
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.setPickUpDelay(32_767);
        require(level.addFreshEntity(entity), "fixtureItem: could not spawn " + item + " x" + count);
        return entity;
    }

    private void verifyUnpoweredControl() {
        AuraConsumerBlockEntity consumer = processor();
        var state = consumer.inspectionState();
        require(consumer.hasValidWork(), "unpoweredControl: White ingot recipe was not recognized");
        require(state.progress() == 0 && state.storedPower() == 0,
            "unpoweredControl: processor advanced without generated power: " + state);
        require(itemCounts(fixtureBounds(new BlockPos(360, 180, 240))).equals(
                Map.of("minecraft:white_wool", 1, "minecraft:iron_ingot", 1)),
            "unpoweredControl: recipe inputs were not retained exactly");
        require(controllerSnapshot().recipeId().equals(RING_RECIPE)
                && controllerSnapshot().receivedPower() == 0
                && controllerSnapshot().requiredPower() == 200_000,
            "unpoweredControl: live controller did not match the four-color ring recipe without power");
        require(processorCombinedPower() == 0,
            "unpoweredControl: processor network contained power before its crystal/fuel feed");
        requireNodeLinks(PROCESSOR_SOURCE, 2);
        requireNodeLinks(PROCESSOR_PUMP, 2);
        requireNodeLinks(PROCESSOR_PUMP_TOP, 2);
        requireNodeLinks(PROCESSOR_TOP, 2);
        requireNodeLinks(VORTEX_SOURCE, 1);
        requireNodeLinks(VORTEX_PUMP, 3);
        requireNodeLinks(VORTEX_PUMP_TOP, 2);
        requireNodeLinks(VORTEX_TOP, 2);
        requireNodeLinks(VORTEX_EAST, 2);
        pass("unpoweredControls", "25 real ticks retained processor inputs and recognized an unpowered four-pedestal ring recipe");
        pass("actualNetworkLinks", "processor and Vortex nodes/pumps discovered the intended live fixture connections");
    }

    private void seedFuelAndAura() {
        processorCrystals = seedItem(AuraItems.crystal(AuraColor.WHITE), 8, 360.5D, 181.25D, 240.5D);
        processorCoal = seedItem(Items.COAL, 1, 360.5D, 181.25D, 238.5D);
        vortexCrystals = seedItem(AuraItems.crystal(AuraColor.WHITE), 1, 395.5D, 181.25D, 240.5D);
        vortexCoal = seedItem(Items.COAL, 1, 394.5D, 181.25D, 240.5D);
        report.add("seededMaterials", seededMaterials());
        pass("realFuelAndAuraSeeded", "8 White crystals + coal for the processor; 1 White crystal + coal for the partial Vortex");
    }

    private void observeProcessorPower() {
        AuraConsumerBlockEntity consumer = processor();
        var state = consumer.inspectionState();
        long nowPower = processorCombinedPower();
        int steps = progressSteps(previousProgress, state.progress(), state.maxProgress());
        long spent = stepCost(state.requiredPower(), steps);
        long bleed = level.getGameTime() % 20L == 18L
            ? previousConsumerPower - AuraConsumerLogic.bleedStoredPower(previousConsumerPower)
            : 0;

        // Sum live network/consumer balances so generated power is derived from real falling transfers,
        // observed work costs, and the consumer's documented periodic bleed, never seeded state.
        long generated = nowPower - previousCombinedPower + spent + bleed;
        require(generated >= 0, "processorPowerBalance: observed a negative generated-power delta " + generated);
        processorGeneratedPower += generated;
        processorSpentPower += spent;
        processorBleedPower += bleed;
        processorProgressSteps += steps;
        previousCombinedPower = nowPower;
        previousConsumerPower = state.storedPower();
        previousProgress = state.progress();
    }

    private void observeFuelAndConservation() {
        if (processorCrystals == null) {
            return;
        }
        AuraPumpBlockEntity processorPump = processorPump();
        AuraPumpBlockEntity vortexPump = vortexPump();
        processorFuelObserved |= processorPump.pumpState().power() > 0;
        vortexFuelObserved |= vortexPump.pumpState().power() > 0;
        require(processorWhiteAuraAndItems() == 8 * CRYSTAL_CHARGE,
            "processorAuraConservation: white aura plus unabsorbed crystals differs from 8000");
        require(vortexWhiteAuraAndItems() == CRYSTAL_CHARGE,
            "vortexAuraConservation: white aura plus unabsorbed crystal differs from 1000");
    }

    private void verifyVortexRecipeAndReceipt() {
        VortexControllerBlockEntity controller = controllerEntity();
        VortexPedestalBlockEntity pedestal = targetPedestal();
        var snapshot = controller.inspectionSnapshot(level, VORTEX_CONTROLLER);
        var receipt = pedestal.inspectionSnapshot();

        if (!vortexReloadComplete && receipt.received() > 0) {
            require(snapshot.recipeId().equals(RING_RECIPE),
                "vortexReceipt: actual controller stopped matching ring_of_binding");
            require(receipt.color() == AuraColor.WHITE && receipt.required() == VORTEX_WHITE_REQUIREMENT
                    && receipt.received() < receipt.required(),
                "vortexReceipt: expected a partial White receipt below 50000, got " + receipt);
            require(pedestal.heldItem().is(Items.OBSIDIAN) && pedestal.heldItem().getCount() == 1,
                "vortexReceipt: target pedestal did not retain its one obsidian");

            AuraInspectionState beforeStorage = pedestal.inspectionState();
            CompoundTag saved = pedestal.saveWithoutMetadata(level.registryAccess());
            VortexPedestalBlockEntity restored = new VortexPedestalBlockEntity(
                VORTEX_EAST, level.getBlockState(VORTEX_EAST));
            require(restored.heldItem().isEmpty() && restored.inspectionSnapshot().received() == 0,
                "vortexReceiptReload: fresh pedestal did not begin empty");
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
            var restoredReceipt = restored.inspectionSnapshot();
            require(receipt.equals(restoredReceipt),
                "vortexReceiptReload: Receipt changed across a fresh BlockEntity.loadWithComponents call");
            require(beforeStorage.storage().equals(restored.inspectionState().storage()),
                "vortexReceiptReload: pedestal aura storage changed during loadWithComponents");
            require(restored.heldItem().is(Items.OBSIDIAN) && restored.heldItem().getCount() == 1,
                "vortexReceiptReload: held obsidian changed during loadWithComponents");
            var restoredSnapshot = controllerSnapshot();
            require(restoredSnapshot.recipeId().equals(RING_RECIPE)
                    && restoredSnapshot.receivedPower() == receipt.received()
                    && restoredSnapshot.requiredPower() == 200_000,
                "vortexReceiptReload: controller snapshot did not expose the live partial receipt");
            receiptAtReload = receipt.received();
            vortexReloadComplete = true;
            pass("vortexPartialReceiptReload", "partial receipt and held item loaded into a fresh real pedestal via BlockEntity.loadWithComponents");
        }

        if (vortexReloadComplete) {
            require(receipt.received() >= receiptAtReload && receipt.received() < receipt.required(),
                "vortexReceipt: partial receipt changed unexpectedly or reached completion");
            require(snapshot.recipeId().equals(RING_RECIPE),
                "vortexReceipt: controller no longer recognizes ring_of_binding");
        }
    }

    private void observeProcessorOutput() {
        if (processorComplete) {
            return;
        }
        Map<String, Integer> items = itemCounts(fixtureBounds(new BlockPos(360, 180, 240)));
        int outputCount = items.getOrDefault("aura:arcane_ingot_white", 0);
        require(outputCount <= 1, "processorOutput: produced more than one White Arcane Ingot: " + items);
        if (outputCount == 1) {
            require(items.equals(Map.of("aura:arcane_ingot_white", 1)),
                "processorOutput: expected exactly one output and no remaining/drop materials, got " + items);
            require(processorWool.isRemoved() && processorWool.getItem().isEmpty()
                    && processorIron.isRemoved() && processorIron.getItem().isEmpty(),
                "processorOutput: White wool and iron ingot were not each consumed");
            require(processorCrystals.isRemoved() && processorCrystals.getItem().isEmpty()
                    && processorCoal.isRemoved() && processorCoal.getItem().isEmpty(),
                "processorOutput: all eight crystals and the one coal were not consumed");
            processorComplete = true;
            pass("whiteProcessorOutputAndConservation", "8 crystals, coal, wool, and iron consumed for exactly 1 White Arcane Ingot");
        }
    }

    private void verifyFinalState() {
        require(processorFuelObserved && vortexFuelObserved,
            "realFuel: both burning pumps were not observed consuming their fixture coal");
        require(vortexCrystals.isRemoved() && vortexCrystals.getItem().isEmpty()
                && vortexCoal.isRemoved() && vortexCoal.getItem().isEmpty(),
            "vortexMaterials: the one White crystal and one coal were not consumed");
        require(processorCrystals.isRemoved() && processorCrystals.getItem().isEmpty()
                && processorCoal.isRemoved() && processorCoal.getItem().isEmpty(),
            "processorMaterials: all eight White crystals and the one coal were not consumed");
        require(processorProgressSteps >= 61,
            "whiteProcessorProgress: expected all 61 real work steps, observed " + processorProgressSteps);
        require(processorGeneratedPower > 0 && processorSpentPower > 0,
            "processorPowerBalance: no real generated/consumed power was observed");
        require(processorGeneratedPower - processorSpentPower - processorBleedPower == processorCombinedPower(),
            "processorPowerBalance: generated, consumed, bleed, and stored totals do not reconcile");
        require(processorWhiteAuraAndItems() == 8 * CRYSTAL_CHARGE,
            "processorAuraConservation: final white aura total changed");
        require(vortexWhiteAuraAndItems() == CRYSTAL_CHARGE,
            "vortexAuraConservation: final white aura total changed");
        require(vortexPedestalItems().equals(Map.of(
                "minecraft:diamond_block", 1,
                "minecraft:obsidian", 1,
                "minecraft:gold_block", 1,
                "minecraft:redstone_block", 1)),
            "vortexMaterialConservation: ring recipe inputs changed across partial receipt");
        require(itemCounts(fixtureBounds(new BlockPos(392, 180, 240))).getOrDefault("aura:ring_of_binding", 0) == 0,
            "vortexOutput: partial receipt incorrectly produced a ring");

        report.addProperty("realGeneratedPowerWhiteProcessor", processorGeneratedPower);
        report.addProperty("realGeneratedPowerVortexPartialReceipt", targetPedestal().inspectionSnapshot().received());
        pass("processorPowerAccounting", "observed generated power reconciles with work cost, bleed, and live stored power");
        pass("vortexMaterialAndOutputConservation", "all four ring inputs remain held; partial receipt produced no output");
    }

    private int processorWhiteAuraAndItems() {
        return whiteAura(PROCESSOR_SOURCE, PROCESSOR_PUMP, PROCESSOR_PUMP_TOP, PROCESSOR_TOP)
            + remainingCrystalCharge(processorCrystals);
    }

    private int vortexWhiteAuraAndItems() {
        return whiteAura(VORTEX_SOURCE, VORTEX_PUMP, VORTEX_PUMP_TOP, VORTEX_TOP, VORTEX_EAST)
            + remainingCrystalCharge(vortexCrystals);
    }

    private int whiteAura(BlockPos... positions) {
        int total = 0;
        for (BlockPos pos : positions) {
            BlockEntity entity = level.getBlockEntity(pos);
            require(entity instanceof AuraNetworkBlockEntity,
                "auraConservation: expected an Aura network block entity at " + pos);
            total += ((AuraNetworkBlockEntity) entity).inspectionState().storage().get(AuraColor.WHITE);
        }
        return total;
    }

    private int remainingCrystalCharge(ItemEntity entity) {
        return entity == null || entity.isRemoved() ? 0 : entity.getItem().getCount() * CRYSTAL_CHARGE;
    }

    private long processorCombinedPower() {
        return networkPower(PROCESSOR_SOURCE) + networkPower(PROCESSOR_PUMP)
            + networkPower(PROCESSOR_PUMP_TOP) + networkPower(PROCESSOR_TOP)
            + processor().inspectionState().storedPower();
    }

    private int networkPower(BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof AuraNetworkBlockEntity,
            "processorPowerBalance: expected an Aura network block entity at " + pos);
        return ((AuraNetworkBlockEntity) entity).inspectionState().storedPower();
    }

    private void requireNodeLinks(BlockPos pos, int expectedCount) {
        BlockEntity entity = level.getBlockEntity(pos);
        require(entity instanceof AuraNetworkBlockEntity,
            "networkLinks: expected an Aura network block entity at " + pos);
        AuraInspectionState state = ((AuraNetworkBlockEntity) entity).inspectionState();
        require(state.hasScannedLinks() && state.linkedNodeCount() == expectedCount,
            "networkLinks: expected " + expectedCount + " discovered links at " + pos + ", got " + state);
    }

    private AuraConsumerBlockEntity processor() {
        BlockEntity entity = level.getBlockEntity(PROCESSOR);
        require(entity instanceof AuraConsumerBlockEntity, "processorFixture: consumer block entity is absent");
        return (AuraConsumerBlockEntity) entity;
    }

    private AuraPumpBlockEntity processorPump() {
        BlockEntity entity = level.getBlockEntity(PROCESSOR_PUMP);
        require(entity instanceof AuraPumpBlockEntity, "processorFixture: pump block entity is absent");
        return (AuraPumpBlockEntity) entity;
    }

    private AuraPumpBlockEntity vortexPump() {
        BlockEntity entity = level.getBlockEntity(VORTEX_PUMP);
        require(entity instanceof AuraPumpBlockEntity, "vortexFixture: pump block entity is absent");
        return (AuraPumpBlockEntity) entity;
    }

    private VortexControllerBlockEntity controllerEntity() {
        BlockEntity entity = level.getBlockEntity(VORTEX_CONTROLLER);
        require(entity instanceof VortexControllerBlockEntity, "vortexFixture: controller block entity is absent");
        return (VortexControllerBlockEntity) entity;
    }

    private VortexControllerBlockEntity.InspectionSnapshot controllerSnapshot() {
        return controllerEntity().inspectionSnapshot(level, VORTEX_CONTROLLER);
    }

    private VortexPedestalBlockEntity targetPedestal() {
        BlockEntity entity = level.getBlockEntity(VORTEX_EAST);
        require(entity instanceof VortexPedestalBlockEntity, "vortexFixture: target pedestal block entity is absent");
        return (VortexPedestalBlockEntity) entity;
    }

    private Map<String, Integer> vortexPedestalItems() {
        TreeMap<String, Integer> items = new TreeMap<>();
        for (BlockPos pos : List.of(VORTEX_NORTH, VORTEX_EAST, VORTEX_SOUTH, VORTEX_WEST)) {
            BlockEntity entity = level.getBlockEntity(pos);
            require(entity instanceof VortexPedestalBlockEntity,
                "vortexFixture: pedestal block entity is absent at " + pos);
            ItemStack stack = ((VortexPedestalBlockEntity) entity).heldItem();
            if (!stack.isEmpty()) {
                items.merge(itemId(stack.getItem()), stack.getCount(), Integer::sum);
            }
        }
        return items;
    }

    private Map<String, Integer> itemCounts(AABB bounds) {
        TreeMap<String, Integer> items = new TreeMap<>();
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, bounds)) {
            ItemStack stack = entity.getItem();
            if (!entity.isRemoved() && !stack.isEmpty()) {
                items.merge(itemId(stack.getItem()), stack.getCount(), Integer::sum);
            }
        }
        return items;
    }

    private JsonObject seededMaterials() {
        JsonObject processor = new JsonObject();
        processor.addProperty("minecraft:white_wool", 1);
        processor.addProperty("minecraft:iron_ingot", 1);
        processor.addProperty("aura:aura_crystal_white", 8);
        processor.addProperty("minecraft:coal", 1);

        JsonObject vortex = new JsonObject();
        vortex.addProperty("minecraft:diamond_block", 1);
        vortex.addProperty("minecraft:obsidian", 1);
        vortex.addProperty("minecraft:gold_block", 1);
        vortex.addProperty("minecraft:redstone_block", 1);
        vortex.addProperty("aura:aura_crystal_white", 1);
        vortex.addProperty("minecraft:coal", 1);

        JsonObject materials = new JsonObject();
        materials.add("whiteProcessor", processor);
        materials.add("ringOfBindingVortex", vortex);
        materials.addProperty("whiteAuraPerCrystal", CRYSTAL_CHARGE);
        return materials;
    }

    private void refreshReport() {
        report.addProperty("elapsedServerTicks", elapsedServerTicks);
        report.addProperty("elapsedWorldTicks", level.getGameTime() - startGameTime);
        report.addProperty("tickLimit", MAX_SERVER_TICKS);
        report.addProperty("timedOut", false);
        report.add("checks", checks);

        JsonObject processor = new JsonObject();
        var state = processor().inspectionState();
        processor.addProperty("progress", state.progress());
        processor.addProperty("maxProgress", state.maxProgress());
        processor.addProperty("storedPower", state.storedPower());
        processor.addProperty("whiteAuraIncludingUnabsorbedCrystals", processorWhiteAuraAndItems());
        processor.addProperty("realGeneratedPower", processorGeneratedPower);
        processor.addProperty("powerSpent", processorSpentPower);
        processor.addProperty("powerBleed", processorBleedPower);
        processor.addProperty("progressStepsObserved", processorProgressSteps);
        processor.addProperty("outputCount", itemCounts(fixtureBounds(new BlockPos(360, 180, 240)))
            .getOrDefault("aura:arcane_ingot_white", 0));
        report.add("whiteProcessor", processor);

        JsonObject vortex = new JsonObject();
        var receipt = targetPedestal().inspectionSnapshot();
        vortex.addProperty("recipeId", controllerSnapshot().recipeId());
        vortex.addProperty("receiptColor", receipt.color() == null ? "" : receipt.color().id());
        vortex.addProperty("receiptReceived", receipt.received());
        vortex.addProperty("receiptRequired", receipt.required());
        vortex.addProperty("receiptAtReload", receiptAtReload);
        vortex.addProperty("receiptReloadComplete", vortexReloadComplete);
        vortex.addProperty("whiteAuraIncludingUnabsorbedCrystal", vortexWhiteAuraAndItems());
        vortex.addProperty("realGeneratedPower", receipt.received());
        vortex.add("heldItems", intMap(vortexPedestalItems()));
        vortex.addProperty("ringOutputCount", itemCounts(fixtureBounds(new BlockPos(392, 180, 240)))
            .getOrDefault("aura:ring_of_binding", 0));
        report.add("ringOfBindingVortex", vortex);
        report.addProperty("processorPumpFuelObserved", processorFuelObserved);
        report.addProperty("vortexPumpFuelObserved", vortexFuelObserved);
    }

    private static JsonObject intMap(Map<String, Integer> values) {
        JsonObject object = new JsonObject();
        values.forEach(object::addProperty);
        return object;
    }

    private static int progressSteps(int previous, int current, int maxProgress) {
        if (current >= previous) {
            return current - previous;
        }
        return Math.max(0, maxProgress + 1 - previous) + current;
    }

    private static long stepCost(int basePower, int steps) {
        long total = 0;
        for (int step = 0; step < steps; step++) {
            total += AuraConsumerLogic.powerCostForStep(basePower, step);
        }
        return total;
    }

    private static AABB fixtureBounds(BlockPos center) {
        return new AABB(center.getX() - 8.0D, center.getY() - 256.0D, center.getZ() - 8.0D,
            center.getX() + 9.0D, center.getY() + 9.0D, center.getZ() + 9.0D);
    }

    private void pass(String name, String detail) {
        checks.addProperty(name, "PASS: " + detail);
        report.add("checks", checks);
    }

    private static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private void finish(boolean success, String failure, boolean timeout) {
        if (finished) {
            return;
        }
        finished = true;
        if (begun) {
            try {
                refreshReport();
            } catch (Exception | AssertionError snapshotError) {
                success = false;
                failure = (failure == null ? "" : failure + "; ") + "finalSnapshot: " + describe(snapshotError);
            }
        }
        report.addProperty("success", success);
        report.addProperty("complete", true);
        report.addProperty("status", success ? "PASS" : timeout ? "TIMEOUT" : "FAIL");
        report.addProperty("timedOut", timeout);
        report.addProperty("finishedAt", Instant.now().toString());
        if (failure != null) {
            report.addProperty("failure", failure);
        }

        try {
            cleanupFixture();
            report.addProperty("cleanup", "PASS");
        } catch (Exception | AssertionError cleanupError) {
            report.addProperty("cleanup", "FAIL: " + describe(cleanupError));
            report.addProperty("success", false);
            report.addProperty("status", "FAIL");
            report.addProperty("failure", (failure == null ? "" : failure + "; ")
                + "cleanup: " + describe(cleanupError));
        }
        try {
            publishReport();
        } catch (Exception writeError) {
            failToConsole("reportWrite: " + describe(writeError));
        }
    }

    private void cleanupFixture() {
        if (level != null) {
            discardFixtureItems(new BlockPos(360, 180, 240));
            discardFixtureItems(new BlockPos(392, 180, 240));
            for (int index = placedBlocks.size() - 1; index >= 0; index--) {
                BlockPos pos = placedBlocks.get(index);
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
            discardFixtureItems(new BlockPos(360, 180, 240));
            discardFixtureItems(new BlockPos(392, 180, 240));
            for (int[] chunk : forcedChunks) {
                level.setChunkForced(chunk[0], chunk[1], false);
            }
        }
        placedBlocks.clear();
    }

    private void discardFixtureItems(BlockPos center) {
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, fixtureBounds(center))) {
            entity.discard();
        }
    }

    private void publishReport() throws Exception {
        if (report == null) {
            return;
        }
        Files.createDirectories(output.getParent());
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(report);
        Files.writeString(temporaryOutput, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE);
        try {
            Files.move(temporaryOutput, output, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException error) {
            Files.move(temporaryOutput, output, StandardCopyOption.REPLACE_EXISTING);
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
        System.err.println("[Aura Target Progression QA] FAIL: " + message);
    }
}
