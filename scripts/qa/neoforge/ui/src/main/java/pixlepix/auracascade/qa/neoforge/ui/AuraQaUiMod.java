package pixlepix.auracascade.qa.neoforge.ui;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(AuraQaUiMod.MOD_ID)
public final class AuraQaUiMod {
    public static final String MOD_ID = "aura_qa_ui";

    public AuraQaUiMod(IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            AuraQaUiClient.bootstrapClient(modBus);
        }
    }
}
