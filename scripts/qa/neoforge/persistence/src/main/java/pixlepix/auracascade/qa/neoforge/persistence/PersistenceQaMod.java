package pixlepix.auracascade.qa.neoforge.persistence;

import java.time.Instant;
import java.util.UUID;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("aura_qa_persistence")
public final class PersistenceQaMod {
    static final Logger LOGGER = LoggerFactory.getLogger("Aura Cascade Persistence QA");

    private final QaPersistenceSupport.ProcessIdentity process = new QaPersistenceSupport.ProcessIdentity(
        ProcessHandle.current().pid(), UUID.randomUUID().toString(), Instant.now().toString()
    );
    private PersistenceTickRun activeRun;

    public PersistenceQaMod(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        LOGGER.info("[aura_qa_persistence] PROCESS_START pid={} marker={}", process.pid(), process.startMarker());
    }

    private void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("auraqa")
            .requires(source -> source.hasPermission(4))
            .then(Commands.literal("persistence")
                .then(Commands.literal("white-full")
                    .then(Commands.argument("origin", BlockPosArgument.blockPos())
                        .executes(context -> startWhite(context.getSource(), BlockPosArgument.getBlockPos(context, "origin")))))
                .then(Commands.literal("prepare")
                    .then(Commands.argument("origin", BlockPosArgument.blockPos())
                        .executes(context -> startPrepare(context.getSource(), BlockPosArgument.getBlockPos(context, "origin")))))
                .then(Commands.literal("check")
                    .executes(context -> checkPersistence(context.getSource())))));
    }

    private int startWhite(net.minecraft.commands.CommandSourceStack source, BlockPos origin) {
        if (activeRun != null) return busy(source);
        String runId = UUID.randomUUID().toString();
        try {
            QaPersistenceSupport evidence = QaPersistenceSupport.open(source.getServer(), process);
            activeRun = WhiteArcaneRun.begin(source, origin, evidence, runId);
            source.sendSuccess(() -> Component.literal("White Arcane Ingot QA started; awaiting bounded server ticks."), true);
            return 1;
        } catch (Exception | AssertionError failure) {
            QaPersistenceSupport.writeStartupFailure(source.getServer(), process, "white-full", runId,
                QaPersistenceSupport.WHITE_RESULT, failure);
            LOGGER.error("[aura_qa_persistence] COMPLETE scenario=white-full status=FAIL run={}", runId, failure);
            source.sendFailure(Component.literal("White progression QA FAIL during setup; see result JSON and server log."));
            return 0;
        }
    }

    private int startPrepare(net.minecraft.commands.CommandSourceStack source, BlockPos origin) {
        if (activeRun != null) return busy(source);
        String runId = UUID.randomUUID().toString();
        try {
            QaPersistenceSupport evidence = QaPersistenceSupport.open(source.getServer(), process);
            activeRun = DiskRestartRun.prepare(source, origin, evidence, runId);
            source.sendSuccess(() -> Component.literal("Disk persistence fixture preparation started; wait for PREPARED."), true);
            return 1;
        } catch (Exception | AssertionError failure) {
            QaPersistenceSupport.writeStartupFailure(source.getServer(), process, "disk-restart-prepare", runId,
                QaPersistenceSupport.PERSISTENCE_EXPECTATION, failure);
            QaPersistenceSupport.writeStartupFailure(source.getServer(), process, "disk-restart-prepare", runId,
                QaPersistenceSupport.PERSISTENCE_RESULT, failure);
            LOGGER.error("[aura_qa_persistence] COMPLETE scenario=disk-restart-prepare status=FAIL run={}", runId, failure);
            source.sendFailure(Component.literal("Disk persistence QA FAIL during setup; see result JSON and server log."));
            return 0;
        }
    }

    private int checkPersistence(net.minecraft.commands.CommandSourceStack source) {
        if (activeRun != null) return busy(source);
        String runId = UUID.randomUUID().toString();
        try {
            QaPersistenceSupport evidence = QaPersistenceSupport.open(source.getServer(), process);
            return DiskRestartRun.check(source, evidence, runId) ? 1 : 0;
        } catch (Exception | AssertionError failure) {
            QaPersistenceSupport.writeStartupFailure(source.getServer(), process, "disk-restart-check", runId,
                QaPersistenceSupport.PERSISTENCE_RESULT, failure);
            LOGGER.error("[aura_qa_persistence] COMPLETE scenario=disk-restart-check status=FAIL run={}", runId, failure);
            source.sendFailure(Component.literal("Disk persistence QA FAIL; see result JSON and server log."));
            return 0;
        }
    }

    private int busy(net.minecraft.commands.CommandSourceStack source) {
        source.sendFailure(Component.literal("An Aura persistence QA run is already active."));
        return 0;
    }

    private void serverTick(ServerTickEvent.Post event) {
        PersistenceTickRun run = activeRun;
        if (run == null) return;
        try {
            if (run.server() != event.getServer()) {
                throw new IllegalStateException("QA run ticked on a different server instance");
            }
            if (run.tick()) activeRun = null;
        } catch (Exception | AssertionError failure) {
            activeRun = null;
            run.fail(failure);
        }
    }

    interface PersistenceTickRun {
        MinecraftServer server();

        boolean tick() throws Exception;

        void fail(Throwable failure);
    }
}
