package pixlepix.auracascade.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.client.screen.BookshelfCoordinatorScreen;

public final class BookshelfCoordinatorClientNetworking {
    private BookshelfCoordinatorClientNetworking() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BookshelfCoordinatorClientNetworking::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BookshelfCoordinatorMenu.registeredMenuType(), BookshelfCoordinatorScreen::new);
    }
}
