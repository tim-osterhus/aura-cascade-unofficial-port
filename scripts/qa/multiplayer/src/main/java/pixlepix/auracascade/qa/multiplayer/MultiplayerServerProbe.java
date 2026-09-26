package pixlepix.auracascade.qa.multiplayer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class MultiplayerServerProbe implements ModInitializer {
    private MultiplayerEvidence evidence;
    private int ticks;

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER
            || System.getProperty("aura.qa.multiplayer.output") == null) return;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            try {
                if (evidence == null) evidence = new MultiplayerEvidence();
                if (evidence.timedOut()) throw new IllegalStateException("Server probe deadline exceeded");
                if (evidence.stopRequested()) {
                    server.halt(false);
                    return;
                }
                var snapshot = evidence.snapshot(server.getPlayerList().getPlayers(), server.overworld().getAllEntities(), ++ticks);
                if (ticks % 20 == 0) evidence.publish(snapshot);
            } catch (Exception error) {
                MultiplayerEvidence.failure(error);
                server.halt(false);
            }
        });
    }
}
