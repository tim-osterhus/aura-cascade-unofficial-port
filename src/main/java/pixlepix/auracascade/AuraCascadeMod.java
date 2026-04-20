package pixlepix.auracascade;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.item.AuraDiscoverability;

public final class AuraCascadeMod implements ModInitializer {
    public static final String MOD_ID = "aura";
    public static final Logger LOGGER = LoggerFactory.getLogger("AuraCascade");

    @Override
    public void onInitialize() {
        AuraContent.bootstrap();
        AuraDiscoverability.bootstrap();
        LOGGER.info("Aura Cascade node, pump, consumer, storage, guidebook, and late-game runtime initialized.");
    }
}
