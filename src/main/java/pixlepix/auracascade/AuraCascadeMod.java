package pixlepix.auracascade;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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

@Mod(AuraCascadeMod.MOD_ID)
public final class AuraCascadeMod {
    public static final String MOD_ID = "aura";
    public static final Logger LOGGER = LoggerFactory.getLogger("AuraCascade");

    public AuraCascadeMod(IEventBus modBus) {
        pixlepix.auracascade.config.AuraConfig.bootstrap();
        modBus.addListener(AuraCascadeMod::registerContent);
        modBus.addListener(AuraCascadeMod::commonSetup);
        pixlepix.auracascade.data.recipe.AuraIngredientTypes.register(modBus);
        AuraAccessoryNetworking.bootstrapCommon(modBus);
        BookshelfCoordinatorNetworking.register(modBus);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            pixlepix.auracascade.client.AuraCascadeClient.bootstrapClient(modBus);
        }
    }

    private static void registerContent(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            AuraContent.bootstrap();
            pixlepix.auracascade.fairy.FairyTorchRegistry.bootstrapCommon();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            AuraContent.registerBlockItems();
            AuraItems.registerItems();
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            AuraContent.registerBlockEntityTypes();
        } else if (event.getRegistryKey().equals(Registries.ENTITY_TYPE)) {
            pixlepix.auracascade.fairy.AuraFairyEntityRegistry.bootstrapCommon();
            pixlepix.auracascade.block.entity.MinerExplosionEntities.bootstrapCommon();
        } else if (event.getRegistryKey().equals(Registries.MOB_EFFECT)) {
            pixlepix.auracascade.item.AngelsteelCurseEffects.bootstrap();
        } else if (event.getRegistryKey().equals(Registries.MENU)) {
            BookshelfCoordinatorMenu.registerMenuType(Registry.register(
                BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(MOD_ID, "bookshelf_coordinator"),
                new MenuType<>(BookshelfCoordinatorMenu::new, FeatureFlags.VANILLA_SET)));
        } else if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            AuraDiscoverability.bootstrap();
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AuraItems.bootstrap();
            pixlepix.auracascade.fairy.FairySystem.bootstrapCommon();
            AuraAccessoryNetworking.registerWingAction(AuraItems::activateAngelWing);
            LOGGER.info("Aura Cascade node, pump, consumer, storage, guidebook, and late-game runtime initialized.");
        });
    }
}
