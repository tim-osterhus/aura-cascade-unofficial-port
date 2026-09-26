package pixlepix.auracascade.compat.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import pixlepix.auracascade.compat.AuraAccessoryNetworking;

public final class AuraAccessoryClient {
    private static final KeyMapping.Category AURA_CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("aura", "aura")
    );

    private AuraAccessoryClient() {
    }

    public static void bootstrapClient() {
        MenuScreens.register(AuraAccessoryNetworking.menuType(), AuraAccessoryScreen::new);
        KeyMapping open = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aura.open_accessories", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, AURA_CATEGORY
        ));
        KeyMapping wing = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.aura.activate_wing", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, AURA_CATEGORY
        ));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.screen != null) {
                return;
            }
            while (open.consumeClick()) {
                ClientPlayNetworking.send(new AuraAccessoryNetworking.OpenRequest());
            }
            while (wing.consumeClick()) {
                ClientPlayNetworking.send(new AuraAccessoryNetworking.WingRequest());
            }
        });
    }
}
