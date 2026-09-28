package pixlepix.auracascade.compat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;
import pixlepix.auracascade.compat.AuraAccessoryNetworking;

public final class AuraAccessoryClient {
    private static final KeyMapping OPEN = new KeyMapping(
        "key.aura.open_accessories", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "key.categories.aura"
    );
    private static final KeyMapping WING = new KeyMapping(
        "key.aura.activate_wing", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.aura"
    );

    private AuraAccessoryClient() {
    }

    public static void bootstrapClient(IEventBus modBus) {
        modBus.addListener(AuraAccessoryClient::registerScreens);
        modBus.addListener(AuraAccessoryClient::registerKeys);
        NeoForge.EVENT_BUS.addListener(AuraAccessoryClient::onClientTick);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AuraAccessoryNetworking.menuType(), AuraAccessoryScreen::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
        event.register(WING);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) {
            return;
        }
        while (OPEN.consumeClick()) {
            PacketDistributor.sendToServer(new AuraAccessoryNetworking.OpenRequest());
        }
        while (WING.consumeClick()) {
            PacketDistributor.sendToServer(new AuraAccessoryNetworking.WingRequest());
        }
    }
}
