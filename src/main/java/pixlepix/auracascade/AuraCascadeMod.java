package pixlepix.auracascade;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.network.BookshelfCoordinatorNetworking;
import pixlepix.auracascade.compat.AuraAccessoryNetworking;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.AuraDiscoverability;

public final class AuraCascadeMod implements ModInitializer {
    public static final String MOD_ID = "aura";
    public static final Logger LOGGER = LoggerFactory.getLogger("AuraCascade");

    @Override
    public void onInitialize() {
        pixlepix.auracascade.config.AuraConfig.bootstrap();
        AuraContent.bootstrap();
        pixlepix.auracascade.fairy.FairySystem.bootstrapCommon();
        pixlepix.auracascade.block.entity.MinerExplosionEntities.bootstrapCommon();
        AuraAccessoryNetworking.bootstrapCommon();
        AuraAccessoryNetworking.registerWingAction(AuraItems::activateAngelWing);
        BookshelfCoordinatorMenu.registerMenuType(Registry.register(
            BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(MOD_ID, "bookshelf_coordinator"),
            new MenuType<>(BookshelfCoordinatorMenu::new, FeatureFlags.VANILLA_SET)));
        BookshelfCoordinatorNetworking.register();
        AuraDiscoverability.bootstrap();
        LOGGER.info("Aura Cascade node, pump, consumer, storage, guidebook, and late-game runtime initialized.");
    }
}
