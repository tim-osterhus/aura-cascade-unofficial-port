package pixlepix.auracascade.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.client.screen.BookshelfCoordinatorScreen;
import pixlepix.auracascade.network.BookshelfCoordinatorNetworking;

public final class BookshelfCoordinatorClientNetworking {
    private BookshelfCoordinatorClientNetworking() {
    }

    public static void register(MenuType<BookshelfCoordinatorMenu> menuType) {
        ClientPlayNetworking.registerGlobalReceiver(
            BookshelfCoordinatorNetworking.SNAPSHOT_TYPE,
            (payload, context) -> {
                if (context.player().containerMenu instanceof BookshelfCoordinatorMenu menu
                    && menu.containerId == payload.containerId()) {
                    menu.applySnapshot(payload);
                }
            }
        );
        MenuScreens.register(menuType, BookshelfCoordinatorScreen::new);
    }
}
