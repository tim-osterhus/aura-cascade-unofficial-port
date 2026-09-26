package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraDiscoverability {
    private static final ResourceLocation TAB_ID = ResourceLocation.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, AuraCascadeMod.MOD_ID);

    public static final CreativeModeTab AURA_TAB = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        TAB_ID,
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.aura"))
            .icon(() -> new ItemStack(AuraItems.crystal(AuraColor.WHITE)))
            .displayItems((parameters, output) -> addCreativeTabEntries(output::accept))
            .build()
    );

    private AuraDiscoverability() {
    }

    public static void bootstrap() {
    }

    public static List<String> discoverableRegistryPaths() {
        ArrayList<String> paths = new ArrayList<>();
        addCreativeTabEntries(itemLike -> paths.add(BuiltInRegistries.ITEM.getKey(itemLike.asItem()).getPath()));
        return List.copyOf(paths);
    }

    private static void addCreativeTabEntries(Consumer<ItemLike> consumer) {
        AuraContent.addDiscoverableItems(consumer);
        AuraItems.addDiscoverableItems(consumer);
    }
}
