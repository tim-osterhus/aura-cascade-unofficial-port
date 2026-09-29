package pixlepix.auracascade.qa.neoforge.multiplayer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod("aura_qa_multiplayer")
public final class MultiplayerProbeMod {
    private MultiplayerEvidence serverEvidence;
    private int serverTicks;

    public MultiplayerProbeMod(IEventBus modBus) {
        if (System.getProperty(MultiplayerEvidence.OUTPUT_PROPERTY) == null) return;
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            NeoForge.EVENT_BUS.addListener(this::serverTick);
        } else {
            MultiplayerClientProbe.bootstrap(modBus);
        }
    }

    private void serverTick(ServerTickEvent.Post event) {
        try {
            if (serverEvidence == null) serverEvidence = new MultiplayerEvidence();
            if (serverEvidence.timedOut()) throw new IllegalStateException("Server probe deadline exceeded");
            if (serverEvidence.stopRequested()) {
                event.getServer().halt(false);
                return;
            }
            var entities = new java.util.ArrayList<net.minecraft.world.entity.Entity>();
            for (var level : event.getServer().getAllLevels()) {
                level.getAllEntities().forEach(entities::add);
            }
            var report = serverEvidence.snapshot(event.getServer().getPlayerList().getPlayers(), entities, ++serverTicks);
            if (serverTicks % 20 == 0) serverEvidence.publish(report);
        } catch (Exception error) {
            MultiplayerEvidence.failure(error);
            event.getServer().halt(false);
        }
    }
}
