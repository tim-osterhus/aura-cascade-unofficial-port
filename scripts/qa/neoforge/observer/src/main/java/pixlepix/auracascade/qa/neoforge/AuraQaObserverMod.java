package pixlepix.auracascade.qa.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(AuraQaObserverMod.MOD_ID)
public final class AuraQaObserverMod {
    public static final String MOD_ID = "aura_qa_observer";
    static final Logger LOGGER = LoggerFactory.getLogger("Aura QA Observer");

    public AuraQaObserverMod(IEventBus modBus) {
        LOGGER.info("NeoForge QA observer loaded on {}.", FMLEnvironment.dist);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(AuraQaHookCommands::register);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            AuraQaObserverClient.bootstrapClient(modBus);
        }
    }
}
