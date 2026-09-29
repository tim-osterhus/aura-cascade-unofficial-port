package pixlepix.auracascade.qa.neoforge.persistence;

import com.google.gson.JsonObject;
import java.time.Instant;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraNetworkBlockEntity;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

final class WhiteArcaneRun implements PersistenceQaMod.PersistenceTickRun {
    private static final int MAX_TICKS = 2_500;
    private static final int CONTROL_TICKS = 40;
    private static final int RAW_CRYSTALS = 8;
    // Keep wool inside the processor scan but outside the adjacent pump's fuel scan.
    private static final double WOOL_INPUT_OFFSET = 3.5D;

    private final CommandSourceStack source;
    private final ServerLevel level;
    private final QaPersistenceSupport evidence;
    private final String runId;
    private final JsonObject report;
    private final BlockPos origin;
    private final BlockPos sourceNode;
    private final BlockPos pumpPos;
    private final BlockPos processorPos;
    private final BlockPos receiverPos;
    private final BlockPos liftTargetPos;
    private final BlockPos fallingSourcePos;
    private final AABB itemBounds;
    private int ticks;
    private int progressionTicks;
    private int crystalsAbsorbedAt = -1;
    private int peakPumpFuel;
    private int peakPumpSpeed;
    private int peakLiftAura;
    private int peakFallPower;
    private int peakProcessorProgress;
    private String phase = "unpowered-control";

    private WhiteArcaneRun(
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
        this.report = report;
        this.origin = origin.immutable();
        this.sourceNode = origin.west(2);
        this.pumpPos = origin.west();
        this.processorPos = origin;
        this.receiverPos = origin.east();
        this.liftTargetPos = pumpPos.above(5);
        this.fallingSourcePos = receiverPos.above(5);
        this.itemBounds = new AABB(origin.getX() - 5.0D, origin.getY() - 2.0D, origin.getZ() - 3.0D,
            origin.getX() + 5.0D, origin.getY() + 8.0D, origin.getZ() + 3.0D);
    }

    static WhiteArcaneRun begin(CommandSourceStack source, BlockPos origin, QaPersistenceSupport evidence, String runId)
        throws Exception {
        JsonObject report = evidence.report("white-full-cycle", "RUNNING", runId);
        report.addProperty("stage", "unpowered-control");
        report.addProperty("tickLimit", MAX_TICKS);
        WhiteArcaneRun run = new WhiteArcaneRun(source, origin, evidence, runId, report);
        evidence.write(QaPersistenceSupport.WHITE_RESULT, report);
        run.createStation();
        report.add("positions", run.positionsJson());
        report.add("initialState", run.stateSnapshot());
        report.add("initialItemCounts", run.itemCountsJson());
        evidence.write(QaPersistenceSupport.WHITE_RESULT, report);
        return run;
    }

    @Override
    public MinecraftServer server() {
        return source.getServer();
    }

    @Override
    public boolean tick() throws Exception {
        ticks++;
        QaPersistenceSupport.require(ticks <= MAX_TICKS, "White progression exceeded the " + MAX_TICKS + " tick bound");
        if (phase.equals("unpowered-control")) {
            if (ticks % 20 == 0) publishProgress();
            if (ticks >= CONTROL_TICKS) finishControlAndSupply();
            return false;
        }

        progressionTicks++;
        sampleProgression();
        if (progressionTicks % 20 == 0) publishProgress();

        int outputs = count(AuraItems.arcaneIngot(AuraColor.WHITE));
        QaPersistenceSupport.require(outputs <= 1, "Expected at most one White Arcane Ingot; observed " + outputs);
        if (outputs == 1) {
            finishSuccess();
            return true;
        }
        return false;
    }

    @Override
    public void fail(Throwable failure) {
        report.addProperty("status", "FAIL");
        report.addProperty("success", false);
        report.addProperty("stage", phase);
        report.addProperty("ticks", ticks);
        report.addProperty("progressionTicks", progressionTicks);
        report.addProperty("finishedAt", Instant.now().toString());
        report.addProperty("failure", failure.toString());
        try {
            report.add("lastState", stateSnapshot());
            report.add("lastItemCounts", itemCountsJson());
            evidence.write(QaPersistenceSupport.WHITE_RESULT, report);
        } catch (Exception writeFailure) {
            PersistenceQaMod.LOGGER.error("[aura_qa_persistence] Could not write white-cycle failure JSON", writeFailure);
        }
        PersistenceQaMod.LOGGER.error("[aura_qa_persistence] COMPLETE scenario=white-full-cycle status=FAIL run={} ticks={} stage={}",
            runId, ticks, phase, failure);
        source.sendFailure(Component.literal("White Arcane Ingot QA FAIL after " + ticks + " ticks; see result JSON and server log."));
    }

    private void createStation() {
        QaPersistenceSupport.require(origin.getY() - 1 >= level.getMinBuildHeight()
                && origin.getY() + 5 < level.getMaxBuildHeight(),
            "Fixture origin is too close to a world height limit");

        BlockPos[] machines = {sourceNode, pumpPos, processorPos, receiverPos, liftTargetPos, fallingSourcePos};
        for (BlockPos pos : machines) {
            level.getChunkAt(pos);
            QaPersistenceSupport.require(level.getBlockState(pos).isAir(), "Fixture block position is occupied: " + pos);
        }
        for (int x = origin.getX() - 2; x <= origin.getX() + 4; x++) {
            for (int z = origin.getZ() - 1; z <= origin.getZ() + 1; z++) {
                BlockPos floor = new BlockPos(x, origin.getY() - 1, z);
                level.getChunkAt(floor);
                QaPersistenceSupport.require(level.getBlockState(floor).isAir(), "Fixture floor position is occupied: " + floor);
            }
        }
        QaPersistenceSupport.require(level.getEntitiesOfClass(ItemEntity.class, itemBounds, entity -> !entity.isRemoved()).isEmpty(),
            "Fixture bounds already contain item entities");
        QaPersistenceSupport.require(level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, itemBounds,
            player -> !player.isSpectator()).isEmpty(), "A player is inside the disposable fixture bounds");

        for (int x = origin.getX() - 2; x <= origin.getX() + 4; x++) {
            for (int z = origin.getZ() - 1; z <= origin.getZ() + 1; z++) {
                level.setBlock(new BlockPos(x, origin.getY() - 1, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        place(sourceNode, AuraContent.AURA_NODE);
        place(pumpPos, AuraContent.AURA_NODE_PUMP);
        place(processorPos, AuraContent.CONSUMER_BLOCK_ORE);
        place(receiverPos, AuraContent.AURA_NODE);
        place(liftTargetPos, AuraContent.AURA_NODE);
        place(fallingSourcePos, AuraContent.AURA_NODE);

        QaPersistenceSupport.drop(level, processorPos, new ItemStack(Items.IRON_INGOT), 0.3D);
        QaPersistenceSupport.drop(level, processorPos, new ItemStack(Items.WHITE_WOOL), WOOL_INPUT_OFFSET);
        assertInitiallyUnseeded();
    }

    private void finishControlAndSupply() throws Exception {
        JsonObject control = new JsonObject();
        control.addProperty("status", "PASS");
        control.addProperty("ticks", ticks);
        control.add("state", stateSnapshot());
        control.add("itemCounts", itemCountsJson());
        assertUnpoweredControl();
        report.add("unpoweredControl", control);

        for (int i = 0; i < RAW_CRYSTALS; i++) {
            QaPersistenceSupport.drop(level, sourceNode, new ItemStack(AuraItems.crystal(AuraColor.WHITE)), 0.5D);
        }
        QaPersistenceSupport.drop(level, pumpPos, new ItemStack(Items.COAL), 0.5D);
        phase = "full-progression";
        report.addProperty("stage", phase);
        report.addProperty("rawWhiteCrystalsSupplied", RAW_CRYSTALS);
        report.addProperty("coalSupplied", 1);
        report.addProperty("progressionStartedGameTime", level.getGameTime());
        report.add("postSupplyItems", itemCountsJson());
        publishProgress();
    }

    private void sampleProgression() {
        AuraPumpBlockEntity pump = (AuraPumpBlockEntity) level.getBlockEntity(pumpPos);
        peakPumpFuel = Math.max(peakPumpFuel, pump.pumpState().power());
        peakPumpSpeed = Math.max(peakPumpSpeed, pump.pumpState().speed());

        AuraNodeBlockEntity liftTarget = (AuraNodeBlockEntity) level.getBlockEntity(liftTargetPos);
        peakLiftAura = Math.max(peakLiftAura, liftTarget.inspectionState().storage().get(AuraColor.WHITE));

        AuraNetworkBlockEntity receiver = (AuraNetworkBlockEntity) level.getBlockEntity(receiverPos);
        AuraConsumerBlockEntity processor = (AuraConsumerBlockEntity) level.getBlockEntity(processorPos);
        var processorState = processor.inspectionState();
        peakFallPower = Math.max(peakFallPower, Math.max(receiver.storedPower(),
            Math.max(processorState.storedPower(), processorState.lastReceivedPower())));
        peakProcessorProgress = Math.max(peakProcessorProgress, processorState.progress());

        if (crystalsAbsorbedAt < 0 && count(AuraItems.crystal(AuraColor.WHITE)) == 0) {
            crystalsAbsorbedAt = progressionTicks;
        }
    }

    private void finishSuccess() throws Exception {
        assertExpectedTopology();
        QaPersistenceSupport.require(count(Items.IRON_INGOT) == 0 && count(Items.WHITE_WOOL) == 0,
            "Recipe completed without consuming exactly one iron ingot and white wool");
        QaPersistenceSupport.require(count(AuraItems.crystal(AuraColor.WHITE)) == 0,
            "Some supplied White crystals remain as item entities");
        QaPersistenceSupport.require(count(Items.COAL) == 0 && peakPumpFuel > 0 && peakPumpFuel > ((AuraPumpBlockEntity) level
            .getBlockEntity(pumpPos)).pumpState().power(), "The real pump did not consume fuel over multiple attempts");
        QaPersistenceSupport.require(peakPumpSpeed > 0 && peakLiftAura > 0,
            "No fueled five-block aura lift was observed");
        QaPersistenceSupport.require(peakFallPower > 0 && peakProcessorProgress > 0,
            "No falling-aura power reached the processor");
        QaPersistenceSupport.require(totalStoredAura() == RAW_CRYSTALS * 1_000,
            "Stored White aura did not conserve all eight absorbed crystals");
        QaPersistenceSupport.require(count(AuraItems.arcaneIngot(AuraColor.WHITE)) == 1,
            "Expected exactly one White Arcane Ingot output");

        report.addProperty("status", "PASS");
        report.addProperty("success", true);
        report.addProperty("stage", "complete");
        report.addProperty("ticks", ticks);
        report.addProperty("progressionTicks", progressionTicks);
        report.addProperty("progressionCompletedGameTime", level.getGameTime());
        report.addProperty("crystalsAbsorbedAtProgressionTick", crystalsAbsorbedAt);
        report.addProperty("peakPumpFuelAttempts", peakPumpFuel);
        report.addProperty("peakPumpSpeed", peakPumpSpeed);
        report.addProperty("peakLiftTargetWhiteAura", peakLiftAura);
        report.addProperty("peakObservedFallingPower", peakFallPower);
        report.addProperty("peakProcessorProgress", peakProcessorProgress);
        report.addProperty("totalStoredWhiteAura", totalStoredAura());
        report.addProperty("outputItem", "aura:arcane_ingot_white");
        report.addProperty("outputCount", count(AuraItems.arcaneIngot(AuraColor.WHITE)));
        report.addProperty("finishedAt", Instant.now().toString());
        report.add("finalState", stateSnapshot());
        report.add("finalItemCounts", itemCountsJson());
        evidence.write(QaPersistenceSupport.WHITE_RESULT, report);
        PersistenceQaMod.LOGGER.info("[aura_qa_persistence] COMPLETE scenario=white-full-cycle status=PASS run={} ticks={} progressionTicks={} output=1",
            runId, ticks, progressionTicks);
        source.sendSuccess(() -> Component.literal("White Arcane Ingot QA PASS in " + ticks + " ticks; result JSON recorded."), true);
    }

    private void assertInitiallyUnseeded() {
        for (BlockPos pos : new BlockPos[]{sourceNode, pumpPos, receiverPos, liftTargetPos, fallingSourcePos}) {
            var state = ((AuraNetworkBlockEntity) level.getBlockEntity(pos)).inspectionState();
            QaPersistenceSupport.require(state.totalAura() == 0 && state.storedPower() == 0
                    && state.linkedNodeCount() == 0 && !state.hasScannedLinks(),
                "Fresh node state was not zero/unscanned at " + pos);
        }
        var pump = (AuraPumpBlockEntity) level.getBlockEntity(pumpPos);
        QaPersistenceSupport.require(pump.pumpState().power() == 0 && pump.pumpState().speed() == 0,
            "Fresh pump fuel state was not zero");
        var processor = ((AuraConsumerBlockEntity) level.getBlockEntity(processorPos)).inspectionState();
        QaPersistenceSupport.require(processor.progress() == 0 && processor.storedPower() == 0,
            "Fresh processor progress/power was not zero");
        QaPersistenceSupport.require(count(AuraItems.arcaneIngot(AuraColor.WHITE)) == 0
                && count(AuraItems.crystal(AuraColor.WHITE)) == 0 && count(Items.COAL) == 0,
            "Fresh station unexpectedly contains output, crystals, or coal");
        QaPersistenceSupport.require(count(Items.IRON_INGOT) == 1 && count(Items.WHITE_WOOL) == 1,
            "Fresh control inputs were not supplied exactly once");
        QaPersistenceSupport.require(((AuraConsumerBlockEntity) level.getBlockEntity(processorPos)).hasValidWork(),
            "Supplied iron and white wool are not a valid in-range processor recipe");
    }

    private void assertUnpoweredControl() {
        QaPersistenceSupport.require(ticks >= CONTROL_TICKS, "Unpowered control ended early");
        QaPersistenceSupport.require(count(Items.IRON_INGOT) == 1 && count(Items.WHITE_WOOL) == 1,
            "Unpowered control consumed recipe inputs");
        QaPersistenceSupport.require(count(AuraItems.arcaneIngot(AuraColor.WHITE)) == 0,
            "Unpowered control produced an Arcane Ingot");
        QaPersistenceSupport.require(((AuraConsumerBlockEntity) level.getBlockEntity(processorPos)).hasValidWork(),
            "Unpowered control no longer has the real processor recipe in range");
        QaPersistenceSupport.require(totalStoredAura() == 0, "Unpowered control unexpectedly stored aura");
        for (BlockPos pos : new BlockPos[]{sourceNode, pumpPos, receiverPos, liftTargetPos, fallingSourcePos}) {
            var state = ((AuraNetworkBlockEntity) level.getBlockEntity(pos)).inspectionState();
            QaPersistenceSupport.require(state.storedPower() == 0, "Unpowered control generated stored power");
        }
        var pump = (AuraPumpBlockEntity) level.getBlockEntity(pumpPos);
        var processor = ((AuraConsumerBlockEntity) level.getBlockEntity(processorPos)).inspectionState();
        QaPersistenceSupport.require(pump.pumpState().power() == 0 && pump.pumpState().speed() == 0,
            "Unpowered control acquired pump fuel");
        QaPersistenceSupport.require(processor.progress() == 0 && processor.storedPower() == 0,
            "Unpowered control advanced processor progress");
        assertExpectedTopology();
    }

    private void assertExpectedTopology() {
        int[] expectedLinks = {1, 2, 1, 2, 2};
        BlockPos[] nodes = {sourceNode, pumpPos, receiverPos, liftTargetPos, fallingSourcePos};
        for (int index = 0; index < nodes.length; index++) {
            var state = ((AuraNetworkBlockEntity) level.getBlockEntity(nodes[index])).inspectionState();
            QaPersistenceSupport.require(state.hasScannedLinks() && state.linkedNodeCount() == expectedLinks[index],
                "Unexpected naturally scanned link count at " + nodes[index] + ": " + state.linkedNodeCount());
        }
    }

    private int totalStoredAura() {
        int total = 0;
        for (BlockPos pos : new BlockPos[]{sourceNode, pumpPos, receiverPos, liftTargetPos, fallingSourcePos}) {
            total += ((AuraNetworkBlockEntity) level.getBlockEntity(pos)).inspectionState().totalAura();
        }
        return total;
    }

    private int count(net.minecraft.world.item.Item item) {
        return QaPersistenceSupport.itemCount(level, itemBounds, item);
    }

    private void place(BlockPos pos, net.minecraft.world.level.block.Block block) {
        QaPersistenceSupport.require(level.setBlock(pos, block.defaultBlockState(), 3), "Could not place fixture block at " + pos);
        QaPersistenceSupport.require(level.getBlockEntity(pos) != null, "Fixture block did not create a block entity at " + pos);
    }

    private JsonObject positionsJson() {
        JsonObject positions = new JsonObject();
        positions.add("processor", QaPersistenceSupport.position(processorPos));
        positions.add("sourceNode", QaPersistenceSupport.position(sourceNode));
        positions.add("burningPump", QaPersistenceSupport.position(pumpPos));
        positions.add("liftTarget", QaPersistenceSupport.position(liftTargetPos));
        positions.add("fallingAuraSource", QaPersistenceSupport.position(fallingSourcePos));
        positions.add("fallingPowerReceiver", QaPersistenceSupport.position(receiverPos));
        return positions;
    }

    private JsonObject stateSnapshot() {
        JsonObject state = new JsonObject();
        state.add("sourceNode", QaPersistenceSupport.nodeSnapshot(level, sourceNode));
        state.add("pump", QaPersistenceSupport.pumpSnapshot(level, pumpPos));
        state.add("processor", QaPersistenceSupport.consumerSnapshot(level, processorPos));
        state.add("powerReceiver", QaPersistenceSupport.nodeSnapshot(level, receiverPos));
        state.add("liftTarget", QaPersistenceSupport.nodeSnapshot(level, liftTargetPos));
        state.add("fallingSource", QaPersistenceSupport.nodeSnapshot(level, fallingSourcePos));
        return state;
    }

    private JsonObject itemCountsJson() {
        JsonObject counts = new JsonObject();
        counts.add("nearby", QaPersistenceSupport.countsJson(QaPersistenceSupport.itemCounts(level, itemBounds)));
        return counts;
    }

    private void publishProgress() throws Exception {
        report.addProperty("status", "RUNNING");
        report.addProperty("stage", phase);
        report.addProperty("ticks", ticks);
        report.addProperty("progressionTicks", progressionTicks);
        report.addProperty("gameTime", level.getGameTime());
        if (phase.equals("full-progression")) {
            report.addProperty("crystalsAbsorbedAtProgressionTick", crystalsAbsorbedAt);
            report.addProperty("peakPumpFuelAttempts", peakPumpFuel);
            report.addProperty("peakLiftTargetWhiteAura", peakLiftAura);
            report.addProperty("peakObservedFallingPower", peakFallPower);
            report.addProperty("peakProcessorProgress", peakProcessorProgress);
        }
        evidence.write(QaPersistenceSupport.WHITE_RESULT, report);
    }
}
