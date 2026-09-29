package pixlepix.auracascade.qa.neoforge.energy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

final class EnergyInteropRuntimeFixture {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int SOURCE_DROP = 6;
    private static final int MAX_POWER_WAIT_TICKS = 100;
    private static final int MAX_EXPORT_WAIT_TICKS = 24;
    private static final int RECEIVER_CAPACITY = 100_000;
    private static final int PARTIAL_RECEIVE = 37;
    private static final int LEGACY_FE_PER_AURA_POWER = 15;
    private static final String ENERGY_METER_ID = "energymeter";
    private static final ResourceLocation ENERGY_METER_BLOCK_ID = ResourceLocation.fromNamespaceAndPath("energymeter", "meter");
    private static final String ENERGY_METER_BLOCK_ENTITY = "com.almostreliable.energymeter.block.entity.MeterBlockEntity";

    private static final Case[] CASES = {
        Case.EMPTY,
        Case.FULL,
        Case.REJECTING,
        Case.PARTIAL,
        Case.SIDED,
        Case.FOUR_RECEIVERS,
        Case.OVER_LIMIT,
        Case.ENERGY_METER
    };

    private static EnergyInteropRuntimeFixture active;

    private final ServerLevel level;
    private final Path output;
    private final String runId = UUID.randomUUID().toString();
    private final JsonObject report = new JsonObject();
    private final JsonObject checks = new JsonObject();
    private final JsonObject results = new JsonObject();
    private final List<BlockPos> receiverPositions = new ArrayList<>();
    private final boolean energyMeterLoaded = ModList.get().isLoaded(ENERGY_METER_ID);

    private BlockPos fluxPos;
    private BlockPos feederPos;
    private ItemEntity crystalEntity;
    private boolean crystalSpawned;
    private long crystalSpawnGameTime = -1L;
    private Block energyMeterBlock = Blocks.AIR;
    private Object energyMeterEntity;
    private Object energyMeterHandler;
    private Method energyMeterIntervalMethod;
    private boolean sidedCapabilityPass;
    private boolean externalCapabilityPass;
    private Case currentCase;
    private Stage stage;
    private int caseIndex;
    private int stageTicks;
    private boolean exportSnapshot;
    private int powerBefore;
    private long energyBefore;
    private long snapshotGameTime;

    private EnergyInteropRuntimeFixture(ServerLevel level, Path output) {
        this.level = level;
        this.output = output;
        initializeReport();
    }

    static boolean start(ServerLevel level, Path outputDirectory) {
        if (active != null) {
            EnergyInteropQaMod.LOGGER.warn("[aura_qa_energy] RETRY: another energy QA run is active");
            return false;
        }

        Path output = outputDirectory.toAbsolutePath().normalize().resolve("energy-result.json");
        EnergyInteropRuntimeFixture fixture = new EnergyInteropRuntimeFixture(level, output);
        try {
            Files.createDirectories(output.getParent());
            fixture.writeReport();
            BlockPos arena = findClearArena(level);
            if (arena == null) {
                fixture.report.addProperty("status", "RETRY");
                fixture.report.addProperty("reason", "No clear loaded fixture area was found near overworld spawn.");
                fixture.report.addProperty("finishedAt", Instant.now().toString());
                fixture.writeReport();
                EnergyInteropQaMod.LOGGER.warn("[aura_qa_energy] RETRY: no clear loaded fixture area; output={}", output);
                return false;
            }

            fixture.fluxPos = arena.immutable();
            fixture.feederPos = arena.above(SOURCE_DROP).immutable();
            active = fixture;
            fixture.beginCase(CASES[0]);
            EnergyInteropQaMod.LOGGER.info("[aura_qa_energy] START run={} output={} dimension={}",
                fixture.runId, output, level.dimension().location());
            return true;
        } catch (Exception | LinkageError failure) {
            fixture.report.addProperty("status", "FAIL");
            fixture.report.addProperty("failure", failure.toString());
            fixture.report.addProperty("finishedAt", Instant.now().toString());
            fixture.writeReportQuietly();
            fixture.cleanupCase();
            EnergyInteropQaMod.LOGGER.error("[aura_qa_energy] FAIL: could not initialize runtime fixture", failure);
            return false;
        }
    }

    static void onLevelTickPre(LevelTickEvent.Pre event) {
        if (active != null && event.getLevel() == active.level) {
            active.onTickPre();
        }
    }

    static void onLevelTickPost(LevelTickEvent.Post event) {
        if (active != null && event.getLevel() == active.level) {
            active.onTickPost();
        }
    }

    private void initializeReport() {
        report.addProperty("runId", runId);
        report.addProperty("startedAt", Instant.now().toString());
        report.addProperty("status", "RUNNING");
        report.addProperty("success", false);
        report.addProperty("target", "Minecraft 1.21.1 / NeoForge 21.1.252");
        report.addProperty("dimension", level.dimension().location().toString());
        report.addProperty("gameTimeAtStart", level.getGameTime());
        report.addProperty("worldScope", "Disposable QA world only; the fixture places and removes temporary blocks.");
        report.addProperty("outcomeInjection", false);
        report.addProperty("powerSeedMechanism",
            "A real dropped White Aura Crystal is spawned clear of the feeder collision shape immediately before its next 10-tick absorption pass. Production natural downward transfer generates power in aura:aura_node_flux. The feeder is removed before measured FE exports.");
        report.addProperty("accountingRule", "Aura debit = min(power before, ceil(actual accepted FE / 15))");

        for (Case testCase : CASES) {
            checks.addProperty(testCase.id, "NOT_RUN");
        }
        report.add("checks", checks);
        report.add("results", results);
        report.add("externalReceiverRelease", externalReleaseJson());
        report.addProperty("externalReceiverAcceptanceRequired", true);
        report.addProperty("externalReceiverLoaded", energyMeterLoaded);
    }

    private static JsonObject externalReleaseJson() {
        JsonObject release = new JsonObject();
        release.addProperty("modId", ENERGY_METER_ID);
        release.addProperty("name", "Energy Meter");
        release.addProperty("version", "1.21.1-0.5.2");
        release.addProperty("upstreamModVersion", "0.5.2");
        release.addProperty("minecraft", "1.21.1+");
        release.addProperty("loader", "NeoForge");
        release.addProperty("releaseType", "Beta");
        release.addProperty("artifact", "energymeter-neoforge-1.21.1-0.5.2.jar");
        release.addProperty("artifactSizeBytes", 397_310);
        release.addProperty("expectedArtifactSha256", "18b32ea67f4b53de52f6724dbd74a8517b10a20271a3d2bd3de75493d7b08433");
        release.addProperty("curseForgeFileId", 8622279);
        release.addProperty("downloadPage", "https://www.curseforge.com/minecraft/mc-mods/energymeter/files/8622279");
        release.addProperty("sourceTag", "v1.21.1-neoforge-0.5.2");
        release.addProperty("sourceCommit", "ebd9896555bd859bfbc477c5aa76fb3f6ec3ba5b");
        release.addProperty("minimumNeoForgeVersion", "21.1.209+");
        release.addProperty("blockId", ENERGY_METER_BLOCK_ID.toString());
        release.addProperty("receiverMode", "CONSUME");
        release.addProperty("configuredInputFace", "WEST (the Fluxing Node is west of the meter)");
        release.addProperty("capability", "Capabilities.EnergyStorage.BLOCK / IEnergyStorage");
        JsonArray requiredDependencies = new JsonArray();
        requiredDependencies.add("Minecraft 1.21.1+");
        requiredDependencies.add("NeoForge 21.1.209+");
        release.add("requiredDependencies", requiredDependencies);
        JsonArray optionalIntegrations = new JsonArray();
        optionalIntegrations.add("computercraft 1.113.1 or newer");
        optionalIntegrations.add("guideme 21.1.15 or newer");
        release.add("optionalIntegrations", optionalIntegrations);
        release.addProperty("sourceContract",
            "MeterBlockEntity registers its sided IEnergyStorage through Capabilities.EnergyStorage.BLOCK. In CONSUME mode receiveEnergy(simulate) reports acceptance without mutation; a real insertion increments its read-only EnergyHandler interval total.");
        return release;
    }

    private void beginCase(Case testCase) {
        cleanupCase();
        currentCase = testCase;
        stage = Stage.WAITING_FOR_POWER;
        stageTicks = 0;
        exportSnapshot = false;
        crystalSpawned = false;
        crystalSpawnGameTime = -1L;
        energyMeterEntity = null;
        energyMeterHandler = null;
        energyMeterIntervalMethod = null;
        sidedCapabilityPass = false;
        externalCapabilityPass = false;

        if (testCase == Case.ENERGY_METER && !energyMeterLoaded) {
            checks.addProperty(testCase.id, "NOT_INSTALLED");
            JsonObject result = new JsonObject();
            result.addProperty("status", "NOT_INSTALLED");
            result.addProperty("requiredModId", ENERGY_METER_ID);
            result.addProperty("requiredArtifact", "energymeter-neoforge-1.21.1-0.5.2.jar");
            result.addProperty("requiredFileId", 8622279);
            results.add(testCase.id, result);
            finish(report.has("hasFailedChecks") ? "FAIL" : "BLOCKED",
                "Install the documented Energy Meter release to complete external interoperability acceptance.");
            return;
        }

        try {
            placeAuraPowerSource();
            writeReport();
            EnergyInteropQaMod.LOGGER.info("[aura_qa_energy] {}: waiting for naturally generated Aura power", testCase.id);
        } catch (Exception | LinkageError failure) {
            failCurrentCase("Fixture setup failed: " + failure);
        }
    }

    private void placeAuraPowerSource() {
        if (!level.getBlockState(fluxPos).isAir() || !level.getBlockState(feederPos).isAir()) {
            throw new IllegalStateException("Fixture node positions are no longer clear");
        }
        if (!level.setBlock(fluxPos, AuraContent.AURA_NODE_FLUX.defaultBlockState(), 3)
            || !level.setBlock(feederPos, AuraContent.AURA_NODE.defaultBlockState(), 3)) {
            throw new IllegalStateException("Could not place production Aura nodes");
        }

    }

    private void dropAuraCrystal() {
        ItemStack crystal = new ItemStack(AuraItems.crystal(AuraColor.WHITE));
        crystalEntity = new ItemEntity(level,
            feederPos.getX() + 0.5D,
            feederPos.getY() + 0.9D,
            feederPos.getZ() + 0.5D,
            crystal);
        if (!level.addFreshEntity(crystalEntity)) {
            throw new IllegalStateException("Could not spawn the real White Aura Crystal input");
        }
        crystalSpawned = true;
        crystalSpawnGameTime = level.getGameTime();
    }

    private void onTickPre() {
        if (stage != Stage.WAITING_FOR_EXPORT || exportSnapshot) {
            return;
        }
        AuraNodeBlockEntity flux = fluxNode();
        if (flux == null) {
            failCurrentCase("Fluxing Node disappeared before its measured server export tick");
            return;
        }
        powerBefore = flux.storedPower();
        if (powerBefore <= 0) {
            return;
        }
        try {
            energyBefore = acceptedEnergy();
            snapshotGameTime = level.getGameTime();
            exportSnapshot = true;
        } catch (Exception | LinkageError failure) {
            failCurrentCase("Could not read receiver acceptance before export: " + failure);
        }
    }

    private void onTickPost() {
        if (++stageTicks > (stage == Stage.WAITING_FOR_POWER ? MAX_POWER_WAIT_TICKS : MAX_EXPORT_WAIT_TICKS)) {
            failCurrentCase("Timed out while " + stage.name().toLowerCase().replace('_', ' '));
            return;
        }

        if (stage == Stage.WAITING_FOR_POWER) {
            AuraNodeBlockEntity flux = fluxNode();
            if (flux == null) {
                failCurrentCase("Production Fluxing Node block entity was not created");
                return;
            }
            if (!crystalSpawned && level.getGameTime() % 10L == 9L) {
                try {
                    dropAuraCrystal();
                } catch (RuntimeException failure) {
                    failCurrentCase("Could not spawn the real White Aura Crystal input: " + failure);
                    return;
                }
            }
            if (flux.storedPower() > 0 && flux.inspectionState().totalAura() > 0) {
                try {
                    removeFeeder();
                    installReceivers();
                    stage = Stage.WAITING_FOR_EXPORT;
                    stageTicks = 0;
                    writeReport();
                    EnergyInteropQaMod.LOGGER.info("[aura_qa_energy] {}: genuine Aura power={} held for measured export",
                        currentCase.id, flux.storedPower());
                } catch (Exception | LinkageError failure) {
                    failCurrentCase("Could not prepare receivers after natural power generation: " + failure);
                }
            }
            return;
        }

        if (stage == Stage.WAITING_FOR_EXPORT && exportSnapshot) {
            try {
                AuraNodeBlockEntity flux = fluxNode();
                if (flux == null) {
                    throw new IllegalStateException("Fluxing Node disappeared during measured export");
                }
                int powerAfter = flux.storedPower();
                long energyAfter = acceptedEnergy();
                long accepted = energyAfter - energyBefore;
                long observedTicks = level.getGameTime() - snapshotGameTime;
                if (accepted != 0L || powerAfter != powerBefore || observedTicks >= MAX_EXPORT_WAIT_TICKS) {
                    completeMeasuredExport(accepted, powerAfter, observedTicks);
                }
            } catch (Exception | LinkageError failure) {
                failCurrentCase("Could not read receiver acceptance after export: " + failure);
            }
        }
    }

    private void installReceivers() throws ReflectiveOperationException {
        receiverPositions.clear();
        switch (currentCase) {
            case EMPTY -> addFixtureReceiver(fluxPos.relative(Direction.EAST), EnergyReceiverBlockEntity.Mode.ACCEPT,
                RECEIVER_CAPACITY, 0, Integer.MAX_VALUE, null);
            case FULL -> addFixtureReceiver(fluxPos.relative(Direction.EAST), EnergyReceiverBlockEntity.Mode.ACCEPT,
                1_000, 1_000, Integer.MAX_VALUE, null);
            case REJECTING -> addFixtureReceiver(fluxPos.relative(Direction.EAST), EnergyReceiverBlockEntity.Mode.REJECT,
                RECEIVER_CAPACITY, 0, Integer.MAX_VALUE, null);
            case PARTIAL -> addFixtureReceiver(fluxPos.relative(Direction.EAST), EnergyReceiverBlockEntity.Mode.ACCEPT,
                RECEIVER_CAPACITY, 0, PARTIAL_RECEIVE, null);
            case SIDED -> {
                BlockPos receiver = fluxPos.relative(Direction.EAST);
                addFixtureReceiver(receiver, EnergyReceiverBlockEntity.Mode.ACCEPT,
                    RECEIVER_CAPACITY, 0, Integer.MAX_VALUE, Direction.WEST);
                IEnergyStorage expectedSide = level.getCapability(Capabilities.EnergyStorage.BLOCK, receiver, Direction.WEST);
                IEnergyStorage wrongSide = level.getCapability(Capabilities.EnergyStorage.BLOCK, receiver, Direction.NORTH);
                sidedCapabilityPass = expectedSide != null && wrongSide == null && expectedSide.canReceive();
                if (!sidedCapabilityPass) {
                    throw new IllegalStateException("Sided receiver did not expose only its configured WEST face");
                }
            }
            case FOUR_RECEIVERS -> {
                for (Direction direction : List.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)) {
                    addFixtureReceiver(fluxPos.relative(direction), EnergyReceiverBlockEntity.Mode.ACCEPT,
                        RECEIVER_CAPACITY, 0, Integer.MAX_VALUE, null);
                }
            }
            case OVER_LIMIT -> {
                for (int distance = 1; distance <= 5; distance++) {
                    addFixtureReceiver(fluxPos.relative(Direction.EAST, distance), EnergyReceiverBlockEntity.Mode.ACCEPT,
                        RECEIVER_CAPACITY, 0, Integer.MAX_VALUE, null);
                }
            }
            case ENERGY_METER -> configureExternalEnergyMeter();
        }
        report.add("receiverPositions", positionsJson(receiverPositions));
        report.addProperty("fixtureReceiverBlockId", EnergyInteropQaMod.id("energy_receiver").toString());
    }

    private void addFixtureReceiver(
        BlockPos pos,
        EnergyReceiverBlockEntity.Mode mode,
        int capacity,
        int initialEnergy,
        int maxReceive,
        Direction inputSide
    ) {
        if (!level.getBlockState(pos).isAir()
            || !level.setBlock(pos, EnergyInteropQaMod.RECEIVER_BLOCK.defaultBlockState(), 3)) {
            throw new IllegalStateException("Receiver position is not clear: " + pos);
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof EnergyReceiverBlockEntity receiver)) {
            throw new IllegalStateException("QA receiver block entity was not created at " + pos);
        }
        receiver.configure(mode, capacity, initialEnergy, maxReceive, inputSide);
        receiverPositions.add(pos.immutable());
    }

    private void configureExternalEnergyMeter() throws ReflectiveOperationException {
        if (!energyMeterLoaded) {
            throw new IllegalStateException("Energy Meter was not loaded");
        }
        energyMeterBlock = BuiltInRegistries.BLOCK.get(ENERGY_METER_BLOCK_ID);
        if (energyMeterBlock == Blocks.AIR || !level.getBlockState(fluxPos.relative(Direction.EAST)).isAir()) {
            throw new IllegalStateException("Energy Meter block energymeter:meter is unavailable or obstructed");
        }
        BlockPos meterPos = fluxPos.relative(Direction.EAST);
        if (!level.setBlock(meterPos, energyMeterBlock.defaultBlockState(), 3)) {
            throw new IllegalStateException("Could not place Energy Meter block");
        }
        receiverPositions.add(meterPos.immutable());
        energyMeterEntity = level.getBlockEntity(meterPos);
        if (energyMeterEntity == null || !Class.forName(ENERGY_METER_BLOCK_ENTITY).isInstance(energyMeterEntity)) {
            throw new IllegalStateException("energymeter:meter did not create the expected MeterBlockEntity");
        }

        Class<?> transferMode = Class.forName(ENERGY_METER_BLOCK_ENTITY + "$TransferMode");
        Object consume = enumValue(transferMode, "CONSUME");
        invoke(energyMeterEntity, "setTransferMode", new Class<?>[]{transferMode}, consume);
        Object ioConfig = invoke(energyMeterEntity, "getIoConfig", new Class<?>[0]);
        Class<?> ioSetting = Class.forName("com.almostreliable.energymeter.block.component.IoConfig$IoSettingWithPriority");
        Field inputSetting = ioSetting.getField("IN");
        invoke(ioConfig, "setSetting", new Class<?>[]{Direction.class, ioSetting}, Direction.WEST, inputSetting.get(null));
        invoke(energyMeterEntity, "setMeasureInterval", new Class<?>[]{int.class}, 5);
        invoke(energyMeterEntity, "setZeroTolerance", new Class<?>[]{int.class}, 1_200);

        energyMeterHandler = invoke(energyMeterEntity, "getEnergyHandler", new Class<?>[0]);
        energyMeterIntervalMethod = energyMeterHandler.getClass().getMethod("getEnergyPerInterval");
        IEnergyStorage capability = level.getCapability(
            Capabilities.EnergyStorage.BLOCK, meterPos, Direction.WEST);
        IEnergyStorage wrongSide = level.getCapability(
            Capabilities.EnergyStorage.BLOCK, meterPos, Direction.EAST);
        externalCapabilityPass = capability != null && capability.canReceive()
            && capability.receiveEnergy(1, true) == 1 && wrongSide == null;
        if (!externalCapabilityPass) {
            throw new IllegalStateException("Energy Meter did not expose a simulated receive only on its WEST input face");
        }
    }

    private void completeMeasuredExport(long accepted, int powerAfter, long observedTicks) {
        try {
            int actualDebit = powerBefore - powerAfter;
            long expectedDebit = Math.min(powerBefore, ceilDiv(Math.max(0L, accepted), LEGACY_FE_PER_AURA_POWER));
            int fixtureReceiverCount = fixtureReceiverCount();
            int actualInsertions = actualInsertions();

            JsonObject result = new JsonObject();
            result.addProperty("status", "RUNNING");
            result.addProperty("measuredGameTime", level.getGameTime());
            result.addProperty("observedTicks", observedTicks);
            result.addProperty("powerBefore", powerBefore);
            result.addProperty("powerAfter", powerAfter);
            result.addProperty("crystalSpawnGameTime", crystalSpawnGameTime);
            result.addProperty("acceptedFe", accepted);
            result.addProperty("auraPowerDebit", actualDebit);
            result.addProperty("expectedAuraPowerDebit", expectedDebit);
            result.addProperty("receiverCount", fixtureReceiverCount);
            result.addProperty("receiverEnergyBefore", energyBefore);
            result.addProperty("actualInsertCalls", actualInsertions);
            if (currentCase != Case.ENERGY_METER) {
                result.addProperty("simulatedInsertCalls", simulationCalls());
            }
            result.addProperty("feederRemovedBeforeMeasurement", level.getBlockState(feederPos).isAir());
            if (currentCase == Case.SIDED) {
                result.addProperty("sidedCapabilityPass", sidedCapabilityPass);
            }
            if (currentCase == Case.FOUR_RECEIVERS) {
                boolean allFourAccepted = receiverPositions.size() == 4;
                for (BlockPos pos : receiverPositions) {
                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    allFourAccepted &= blockEntity instanceof EnergyReceiverBlockEntity receiver
                        && receiver.getEnergyStored() > 0;
                }
                result.addProperty("allFourAccepted", allFourAccepted);
            }
            if (currentCase == Case.ENERGY_METER) {
                result.addProperty("externalCapabilityPass", externalCapabilityPass);
            }

            boolean accounting = accepted >= 0 && actualDebit == expectedDebit;
            int simulations = currentCase == Case.ENERGY_METER ? 0 : simulationCalls();
            boolean behavior = caseBehaviorPasses(accepted, fixtureReceiverCount, actualInsertions, simulations, result);
            boolean passed = accounting && behavior && result.get("feederRemovedBeforeMeasurement").getAsBoolean();
            result.addProperty("accountingPass", accounting);
            result.addProperty("behaviorPass", behavior);
            result.addProperty("success", passed);
            result.addProperty("status", passed ? "PASS" : "FAIL");
            checks.addProperty(currentCase.id, passed ? "PASS" : "FAIL");
            results.add(currentCase.id, result);
            if (!passed) {
                report.addProperty("failure", currentCase.id + " did not satisfy receiver behavior and Aura debit accounting");
            }
            writeReport();
            EnergyInteropQaMod.LOGGER.info(
                "[aura_qa_energy] {} {}: acceptedFe={} auraDebit={} expectedDebit={} power={}->{}",
                currentCase.id, passed ? "PASS" : "FAIL", accepted, actualDebit, expectedDebit, powerBefore, powerAfter);
            advanceCase();
        } catch (Exception | LinkageError failure) {
            failCurrentCase("Measured export assertion failed: " + failure);
        }
    }

    private boolean caseBehaviorPasses(
        long accepted,
        int receiverCount,
        int actualInsertions,
        int simulations,
        JsonObject result
    ) {
        return switch (currentCase) {
            case EMPTY -> accepted > 0 && energyBefore == 0 && receiverCount == 1 && actualInsertions > 0;
            case FULL -> accepted == 0 && receiverCount == 1 && actualInsertions == 0
                && energyBefore == 1_000 && simulations > 0;
            case REJECTING -> accepted == 0 && receiverCount == 1 && actualInsertions == 0 && simulations > 0;
            case PARTIAL -> accepted == PARTIAL_RECEIVE && receiverCount == 1 && actualInsertions > 0;
            case SIDED -> accepted > 0 && receiverCount == 1 && actualInsertions > 0
                && result.get("sidedCapabilityPass").getAsBoolean();
            case FOUR_RECEIVERS -> accepted > 0 && receiverCount == 4 && actualInsertions == 4 && simulations == 4
                && result.get("allFourAccepted").getAsBoolean();
            case OVER_LIMIT -> accepted == 0 && receiverCount == 5 && actualInsertions == 0 && simulations == 0;
            case ENERGY_METER -> accepted > 0 && receiverCount == 1 && actualInsertions == 0
                && result.get("externalCapabilityPass").getAsBoolean();
        };
    }

    private void advanceCase() {
        boolean passed = checks.get(currentCase.id).getAsString().equals("PASS");
        if (!passed) {
            report.addProperty("hasFailedChecks", true);
        }
        caseIndex++;
        if (caseIndex >= CASES.length) {
            finish(report.has("hasFailedChecks") ? "FAIL" : "PASS", null);
            return;
        }
        beginCase(CASES[caseIndex]);
    }

    private void failCurrentCase(String reason) {
        if (currentCase != null) {
            checks.addProperty(currentCase.id, "FAIL");
            JsonObject result = results.has(currentCase.id) ? results.getAsJsonObject(currentCase.id) : new JsonObject();
            result.addProperty("status", "FAIL");
            result.addProperty("success", false);
            result.addProperty("failure", reason);
            if (stage == Stage.WAITING_FOR_POWER) {
                result.addProperty("crystalSpawned", crystalSpawned);
                result.addProperty("crystalSpawnGameTime", crystalSpawnGameTime);
                result.addProperty("crystalEntityStillPresent", crystalEntity != null && !crystalEntity.isRemoved());
                BlockEntity feederEntity = level.getBlockEntity(feederPos);
                if (feederEntity instanceof AuraNodeBlockEntity feeder) {
                    result.addProperty("feederAuraAtFailure", feeder.inspectionState().totalAura());
                    result.addProperty("feederLinkedNodesAtFailure", feeder.inspectionState().linkedNodeCount());
                }
                AuraNodeBlockEntity flux = fluxNode();
                if (flux != null) {
                    result.addProperty("fluxAuraAtFailure", flux.inspectionState().totalAura());
                    result.addProperty("fluxPowerAtFailure", flux.storedPower());
                    result.addProperty("fluxLinkedNodesAtFailure", flux.inspectionState().linkedNodeCount());
                }
            }
            results.add(currentCase.id, result);
        }
        report.addProperty("hasFailedChecks", true);
        report.addProperty("failure", reason);
        EnergyInteropQaMod.LOGGER.error("[aura_qa_energy] {} FAIL: {}", currentCase == null ? "setup" : currentCase.id, reason);
        advanceCase();
    }

    private void finish(String status, String reason) {
        cleanupCase();
        report.addProperty("status", status);
        report.addProperty("success", "PASS".equals(status));
        if (reason != null) {
            report.addProperty("reason", reason);
        }
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("gameTimeAtFinish", level.getGameTime());
        boolean persisted = writeReportQuietly();
        if (!persisted) {
            status = "FAIL";
            reason = "Could not persist the energy interoperability result JSON.";
            report.addProperty("status", status);
            report.addProperty("success", false);
            report.addProperty("reason", reason);
        }
        if (active == this) {
            active = null;
        }
        if ("PASS".equals(status)) {
            EnergyInteropQaMod.LOGGER.info("[aura_qa_energy] PASS run={} output={}", runId, output);
        } else {
            EnergyInteropQaMod.LOGGER.warn("[aura_qa_energy] {} run={} output={} reason={}", status, runId, output, reason);
        }
    }

    private long acceptedEnergy() throws ReflectiveOperationException {
        if (currentCase == Case.ENERGY_METER) {
            return ((Number) energyMeterIntervalMethod.invoke(energyMeterHandler)).longValue();
        }
        long total = 0L;
        for (BlockPos pos : receiverPositions) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof EnergyReceiverBlockEntity receiver)) {
                throw new IllegalStateException("Fixture receiver disappeared at " + pos);
            }
            total += receiver.getEnergyStored();
        }
        return total;
    }

    private int fixtureReceiverCount() {
        return currentCase == Case.ENERGY_METER ? 1 : receiverPositions.size();
    }

    private int actualInsertions() {
        if (currentCase == Case.ENERGY_METER) {
            return 0;
        }
        int total = 0;
        for (BlockPos pos : receiverPositions) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyReceiverBlockEntity receiver) {
                total += receiver.actualInsertCalls();
            }
        }
        return total;
    }

    private int simulationCalls() {
        int total = 0;
        for (BlockPos pos : receiverPositions) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EnergyReceiverBlockEntity receiver) {
                total += receiver.simulationCalls();
            }
        }
        return total;
    }

    private AuraNodeBlockEntity fluxNode() {
        BlockEntity blockEntity = level.getBlockEntity(fluxPos);
        return blockEntity instanceof AuraNodeBlockEntity node ? node : null;
    }

    private void removeFeeder() {
        if (level.getBlockState(feederPos).is(AuraContent.AURA_NODE)) {
            level.removeBlock(feederPos, false);
        }
    }

    private void cleanupCase() {
        removeFeeder();
        if (fluxPos != null && level.getBlockState(fluxPos).is(AuraContent.AURA_NODE_FLUX)) {
            level.removeBlock(fluxPos, false);
        }
        for (BlockPos pos : receiverPositions) {
            if (level.getBlockState(pos).is(EnergyInteropQaMod.RECEIVER_BLOCK)
                || (energyMeterBlock != Blocks.AIR && level.getBlockState(pos).is(energyMeterBlock))) {
                level.removeBlock(pos, false);
            }
        }
        receiverPositions.clear();
        if (crystalEntity != null && !crystalEntity.isRemoved()) {
            crystalEntity.discard();
        }
        crystalEntity = null;
    }

    private void writeReport() throws IOException {
        Files.createDirectories(output.getParent());
        Files.writeString(output, JSON.toJson(report), StandardCharsets.UTF_8);
    }

    private boolean writeReportQuietly() {
        try {
            writeReport();
            return true;
        } catch (IOException failure) {
            EnergyInteropQaMod.LOGGER.error("[aura_qa_energy] Could not write {}", output, failure);
            return false;
        }
    }

    private static BlockPos findClearArena(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        for (int radius = 0; radius <= 24; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    int x = spawn.getX() + dx;
                    int z = spawn.getZ() + dz;
                    if (!level.hasChunkAt(new BlockPos(x, level.getMinBuildHeight(), z))) {
                        continue;
                    }
                    int floor = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos center = new BlockPos(x, floor + 2, z);
                    if (center.getY() + SOURCE_DROP >= level.getMaxBuildHeight() - 1) {
                        continue;
                    }
                    if (isArenaClear(level, center)) {
                        return center;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isArenaClear(ServerLevel level, BlockPos center) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(center);
        positions.add(center.above(SOURCE_DROP));
        positions.add(center.relative(Direction.NORTH));
        positions.add(center.relative(Direction.SOUTH));
        positions.add(center.relative(Direction.WEST));
        for (int distance = 1; distance <= 5; distance++) {
            positions.add(center.relative(Direction.EAST, distance));
        }
        for (BlockPos pos : positions) {
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()) {
                return false;
            }
        }
        for (int offset = 1; offset < SOURCE_DROP; offset++) {
            BlockPos pos = center.above(offset);
            if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir()) {
                return false;
            }
        }
        return true;
    }

    private static JsonArray positionsJson(List<BlockPos> positions) {
        JsonArray array = new JsonArray();
        for (BlockPos pos : positions) {
            JsonObject entry = new JsonObject();
            entry.addProperty("x", pos.getX());
            entry.addProperty("y", pos.getY());
            entry.addProperty("z", pos.getZ());
            array.add(entry);
        }
        return array;
    }

    private static long ceilDiv(long value, long divisor) {
        return value / divisor + (value % divisor == 0L ? 0L : 1L);
    }

    private static Object enumValue(Class<?> type, String name) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        Object value = Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), name);
        return value;
    }

    private static Object invoke(Object receiver, String methodName, Class<?>[] parameterTypes, Object... arguments)
        throws ReflectiveOperationException {
        Method method = receiver.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(receiver, arguments);
    }

    private enum Stage {
        WAITING_FOR_POWER,
        WAITING_FOR_EXPORT
    }

    private enum Case {
        EMPTY("emptyReceiver"),
        FULL("fullReceiver"),
        REJECTING("rejectingReceiver"),
        PARTIAL("partialReceiver"),
        SIDED("sidedReceiver"),
        FOUR_RECEIVERS("fourReceiverLimit"),
        OVER_LIMIT("overFourReceiverLimit"),
        ENERGY_METER("externalEnergyMeter");

        private final String id;

        Case(String id) {
            this.id = id;
        }
    }
}
