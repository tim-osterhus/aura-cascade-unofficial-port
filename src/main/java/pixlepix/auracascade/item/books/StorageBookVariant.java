package pixlepix.auracascade.item.books;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public enum StorageBookVariant {
    BASIC("basic_storage_book", "Basic Storage Book", 27, 64),
    DENSE("dense_storage_book", "Dense Storage Book", 8, 1_000),
    VERY_DENSE("very_dense_storage_book", "Very Dense Storage Book", 4, 5_000),
    SUPER_DENSE("super_dense_storage_book", "Super Dense Storage Book", 2, 25_000),
    EXTREMELY_DENSE("extremely_dense_storage_book", "Extremely Dense Storage Book", 1, 100_000),
    LIGHT("light_storage_book", "Light Storage Book", 100, 32),
    VERY_LIGHT("very_light_storage_book", "Very Light Storage Book", 150, 16),
    SUPER_LIGHT("super_light_storage_book", "Super Light Storage Book", 1_000, 4),
    EXTREMELY_LIGHT("extremely_light_storage_book", "Extremely Light Storage Book", 10_000, 1),
    MINERAL("mineral_storage_book", "Mineral Storage Book", 8, 100_000),
    MOB("mob_storage_book", "Mob Storage Book", 32, 100_000),
    FARMING("farming_storage_book", "Farming Storage Book", 32, 100_000),
    MOD("mod_storage_book", "Mod Storage Book", 64, 100);

    private static final List<TagKey<Item>> MINERAL_TAGS = List.of(
        commonTag("ores"),
        commonTag("ingots"),
        commonTag("nuggets"),
        commonTag("dusts"),
        commonTag("gems"),
        commonTag("raw_materials"),
        commonTag("storage_blocks")
    );
    private static final List<TagKey<Item>> FARMING_TAGS = List.of(
        commonTag("seeds"),
        commonTag("crops"),
        commonTag("flowers"),
        commonTag("fruits"),
        commonTag("vegetables"),
        commonTag("saplings"),
        commonTag("leaves"),
        commonTag("logs")
    );

    private final String registryPath;
    private final String title;
    private final int maxStoredTypes;
    private final int maxItemsPerType;

    StorageBookVariant(String registryPath, String title, int maxStoredTypes, int maxItemsPerType) {
        this.registryPath = registryPath;
        this.title = title;
        this.maxStoredTypes = maxStoredTypes;
        this.maxItemsPerType = maxItemsPerType;
    }

    public String registryPath() {
        return registryPath;
    }

    public String title() {
        return title;
    }

    public int maxStoredTypes() {
        return maxStoredTypes;
    }

    public int maxItemsPerType() {
        return maxItemsPerType;
    }

    public boolean accepts(ItemStack stack, List<ItemStack> storedEntries) {
        if (stack.isEmpty()) {
            return false;
        }

        return switch (this) {
            case BASIC, DENSE, VERY_DENSE, SUPER_DENSE, EXTREMELY_DENSE, LIGHT, VERY_LIGHT, SUPER_LIGHT, EXTREMELY_LIGHT -> true;
            case MINERAL -> isMineral(stack);
            case MOB -> isMobDrop(stack);
            case FARMING -> isFarming(stack);
            case MOD -> isModScoped(stack, storedEntries);
        };
    }

    private static boolean isMineral(ItemStack stack) {
        return hasAnyTag(stack, MINERAL_TAGS)
            || stack.is(Items.COAL)
            || stack.is(Items.CHARCOAL)
            || stack.is(Items.REDSTONE)
            || stack.is(Items.QUARTZ)
            || stack.is(Items.CLAY_BALL)
            || stack.is(Items.BRICK)
            || stack.is(Items.NETHER_BRICK)
            || stack.is(Items.LAPIS_LAZULI)
            || idPath(stack).contains("ore");
    }

    private static boolean isMobDrop(ItemStack stack) {
        return stack.is(Items.ROTTEN_FLESH)
            || stack.is(Items.STRING)
            || stack.is(Items.LEATHER)
            || stack.is(Items.BONE)
            || stack.is(Items.FEATHER)
            || stack.is(Items.GUNPOWDER)
            || stack.is(Items.BOW)
            || stack.is(Items.SPIDER_EYE)
            || stack.is(Items.SLIME_BALL)
            || stack.is(Items.ENDER_PEARL)
            || stack.is(Items.GHAST_TEAR)
            || stack.is(Items.BLAZE_ROD)
            || stack.is(Items.PRISMARINE_CRYSTALS)
            || stack.is(Items.PRISMARINE_SHARD)
            || stack.is(Items.NAUTILUS_SHELL)
            || stack.is(Items.PHANTOM_MEMBRANE)
            || idPath(stack).contains("spawn_egg");
    }

    private static boolean isFarming(ItemStack stack) {
        String path = idPath(stack);
        return hasAnyTag(stack, FARMING_TAGS)
            || stack.is(ItemTags.SAPLINGS)
            || stack.is(ItemTags.LEAVES)
            || stack.is(Items.WHEAT)
            || stack.is(Items.WHEAT_SEEDS)
            || stack.is(Items.BEETROOT_SEEDS)
            || stack.is(Items.MELON_SEEDS)
            || stack.is(Items.PUMPKIN_SEEDS)
            || stack.is(Items.APPLE)
            || stack.is(Items.BONE_MEAL)
            || stack.is(Items.SUGAR_CANE)
            || stack.is(Items.CACTUS)
            || stack.is(Items.BAMBOO)
            || stack.is(Items.RED_MUSHROOM)
            || stack.is(Items.BROWN_MUSHROOM)
            || stack.is(Items.PUMPKIN)
            || stack.is(Items.MELON_SLICE)
            || stack.is(Items.KELP)
            || stack.is(Items.DRIED_KELP)
            || path.contains("sapling")
            || path.contains("seed")
            || path.contains("crop")
            || path.contains("flower")
            || path.contains("log")
            || path.contains("leaves");
    }

    private static boolean isModScoped(ItemStack stack, List<ItemStack> storedEntries) {
        String namespace = namespace(stack);
        if ("minecraft".equals(namespace)) {
            return false;
        }
        for (ItemStack storedEntry : storedEntries) {
            if (!storedEntry.isEmpty() && !namespace.equals(namespace(storedEntry))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasAnyTag(ItemStack stack, List<TagKey<Item>> tags) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static String namespace(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
    }

    private static String idPath(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
    }

    private static TagKey<Item> commonTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", path));
    }
}
