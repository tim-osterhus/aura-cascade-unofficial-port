package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import java.util.IdentityHashMap;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import pixlepix.auracascade.AuraCascadeMod;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.AuraNodeVariant;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraConsumerVariant;
import pixlepix.auracascade.block.entity.LateGameBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpVariant;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.block.entity.VortexControllerBlockEntity;
import pixlepix.auracascade.block.entity.VortexPedestalBlockEntity;
import pixlepix.auracascade.item.AuraItems;

public final class AuraContent {
    private static final Map<Block, AuraNodeVariant> NODE_VARIANTS = new IdentityHashMap<>();
    private static final Map<Block, AuraPumpVariant> PUMP_VARIANTS = new IdentityHashMap<>();
    private static final Map<Block, AuraConsumerVariant> CONSUMER_VARIANTS = new IdentityHashMap<>();
    private static final Map<Block, LateGameVariant> LATE_GAME_VARIANTS = new IdentityHashMap<>();
    private static final Map<Block, FortifiedBlockVariant> FORTIFIED_VARIANTS = new IdentityHashMap<>();
    private static final Map<Block, Boolean> EXTRA_NETWORK_BLOCKS = new IdentityHashMap<>();

    public static final AuraNodeBlock AURA_NODE = registerNode(AuraNodeVariant.AURA_NODE);
    public static final AuraNodeBlock AURA_NODE_CAPACITOR = registerNode(AuraNodeVariant.AURA_CAPACITOR);
    public static final AuraNodeBlock AURA_NODE_CONSERVE = registerNode(AuraNodeVariant.CONSERVING_AURA_NODE);
    public static final AuraNodeBlock AURA_NODE_BLACK = registerNode(AuraNodeVariant.BLACK_MANIPULATOR);
    public static final AuraNodeBlock AURA_NODE_ORANGE = registerNode(AuraNodeVariant.ORANGE_MANIPULATOR);
    public static final AuraNodeBlock AURA_NODE_FLUX = registerNode(AuraNodeVariant.FLUXING_NODE);

    public static final AuraPumpBlock AURA_NODE_PUMP = registerPump(AuraPumpVariant.BURNING);
    public static final AuraPumpBlock AURA_NODE_PUMP_ALT = registerPump(AuraPumpVariant.BURNING_ALT);
    public static final AuraPumpBlock AURA_NODE_PUMP_LIGHT = registerPump(AuraPumpVariant.ILLUMINATION);
    public static final AuraPumpBlock AURA_NODE_PUMP_LIGHT_ALT = registerPump(AuraPumpVariant.ILLUMINATION_ALT);
    public static final AuraPumpBlock AURA_NODE_PUMP_FALL = registerPump(AuraPumpVariant.MOMENTUM);
    public static final AuraPumpBlock AURA_NODE_PUMP_FALL_ALT = registerPump(AuraPumpVariant.MOMENTUM_ALT);
    public static final AuraPumpBlock AURA_NODE_PUMP_PROJECTILE = registerPump(AuraPumpVariant.PROJECTILE);
    public static final AuraPumpBlock AURA_NODE_PUMP_PROJECTILE_ALT = registerPump(AuraPumpVariant.PROJECTILE_ALT);
    public static final AuraPumpBlock AURA_NODE_PUMP_REDSTONE = registerPump(AuraPumpVariant.REDSTONE);
    public static final AuraPumpBlock AURA_NODE_PUMP_REDSTONE_ALT = registerPump(AuraPumpVariant.REDSTONE_ALT);
    public static final AuraPumpBlock AURA_NODE_PUMP_CREATIVE = registerPump(AuraPumpVariant.CREATIVE);

    public static final AuraConsumerBlock CONSUMER_BLOCK_ORE = registerConsumer(AuraConsumerVariant.PROCESSOR);
    public static final AuraConsumerBlock CONSUMER_BLOCK_ORE_ADV = registerConsumer(AuraConsumerVariant.PRISMATIC_PROCESSOR);
    public static final AuraConsumerBlock CONSUMER_BLOCK_FURNACE = registerConsumer(AuraConsumerVariant.SMELTER);
    public static final AuraConsumerBlock CONSUMER_BLOCK_PLANT = registerConsumer(AuraConsumerVariant.GROWER);
    public static final AuraConsumerBlock CONSUMER_BLOCK_FISH = registerConsumer(AuraConsumerVariant.FISHER);
    public static final AuraConsumerBlock CONSUMER_BLOCK_POTION = registerConsumer(AuraConsumerVariant.BREWER);
    public static final AuraConsumerBlock CONSUMER_BLOCK_DYE = registerConsumer(AuraConsumerVariant.COLORER);
    public static final AuraConsumerBlock CONSUMER_BLOCK_ANGEL = registerConsumer(AuraConsumerVariant.SYNTHESIZER);
    public static final AuraConsumerBlock CONSUMER_BLOCK_ENCHANT = registerConsumer(AuraConsumerVariant.ENCHANTER);
    public static final LateGameWorldBlock CONSUMER_BLOCK_LOOT = registerLateGame(
        LateGameVariant.LOOTER,
        commonProperties("consumer_block_loot", MapColor.COLOR_PURPLE, SoundType.COPPER)
    );
    public static final LateGameWorldBlock CONSUMER_BLOCK_SPAWN = registerLateGame(
        LateGameVariant.SPAWNER,
        commonProperties("consumer_block_spawn", MapColor.COLOR_RED, SoundType.COPPER)
    );
    public static final LateGameWorldBlock CONSUMER_BLOCK_MINER = registerLateGame(
        LateGameVariant.MINER,
        commonProperties("consumer_block_miner", MapColor.COLOR_BLACK, SoundType.AMETHYST)
    );
    public static final LateGameWorldBlock RITUAL_NETHER = registerLateGame(
        LateGameVariant.RITUAL_NETHER,
        commonProperties("ritual_nether", MapColor.COLOR_RED, SoundType.NETHERITE_BLOCK)
    );
    public static final LateGameWorldBlock RITUAL_END = registerLateGame(
        LateGameVariant.RITUAL_END,
        commonProperties("ritual_end", MapColor.COLOR_YELLOW, SoundType.AMETHYST)
    );

    public static final TravelersBricksBlock TRAVELERS_BRICKS = registerBlock(
        "travelers_bricks",
        new TravelersBricksBlock(commonProperties("travelers_bricks", MapColor.TERRACOTTA_RED, SoundType.NETHER_BRICKS))
    );

    public static final ReboundingEnigmaBlock REBOUNDING_ENIGMA = registerBlock(
        "rebounding_enigma",
        new ReboundingEnigmaBlock(commonProperties("rebounding_enigma", MapColor.COLOR_MAGENTA, SoundType.AMETHYST))
    );

    public static final FortifiedBlock FORTIFIED_COBBLESTONE = registerFortified(FortifiedBlockVariant.COBBLESTONE);
    public static final FortifiedBlock FORTIFIED_STONE = registerFortified(FortifiedBlockVariant.STONE);
    public static final FortifiedBlock FORTIFIED_PLANKS = registerFortified(FortifiedBlockVariant.PLANKS);
    public static final FortifiedBlock FORTIFIED_GLASS = registerFortified(FortifiedBlockVariant.GLASS);
    public static final FortifiedBlock FORTIFIED_OBSIDIAN = registerFortified(FortifiedBlockVariant.OBSIDIAN);
    public static final FortifiedBlock FORTIFIED_DIRT = registerFortified(FortifiedBlockVariant.DIRT);

    public static final VortexControllerBlock VORTEX_CONTROLLER = registerBlock(
        "aura_node_crafting_center",
        new VortexControllerBlock(commonProperties("aura_node_crafting_center", MapColor.COLOR_PURPLE, SoundType.AMETHYST))
    );

    public static final VortexPedestalBlock VORTEX_PEDESTAL = registerBlock(
        "aura_node_crafting_pedestal",
        new VortexPedestalBlock(commonProperties("aura_node_crafting_pedestal", MapColor.COLOR_LIGHT_GRAY, SoundType.STONE).noOcclusion())
    );

    public static final AuraMonitorBlock MONITOR = registerBlock(
        "monitor",
        new AuraMonitorBlock(commonProperties("monitor", MapColor.COLOR_LIGHT_GRAY, SoundType.STONE))
    );

    public static final StorageBookshelfBlock STORAGE_BOOKSHELF = registerBlock(
        "storage_bookshelf",
        new StorageBookshelfBlock(commonProperties("storage_bookshelf", MapColor.WOOD, SoundType.WOOD)),
        false
    );

    public static final BookshelfCoordinatorBlock BOOKSHELF_COORDINATOR = registerBlock(
        "bookshelf_coordinator",
        new BookshelfCoordinatorBlock(commonProperties("bookshelf_coordinator", MapColor.WOOD, SoundType.WOOD))
    );

    public static BlockEntityType<AuraNodeBlockEntity> AURA_NODE_BLOCK_ENTITY;
    public static BlockEntityType<AuraPumpBlockEntity> AURA_PUMP_BLOCK_ENTITY;
    public static BlockEntityType<AuraConsumerBlockEntity> AURA_CONSUMER_BLOCK_ENTITY;
    public static BlockEntityType<VortexControllerBlockEntity> VORTEX_CONTROLLER_BLOCK_ENTITY;
    public static BlockEntityType<VortexPedestalBlockEntity> VORTEX_PEDESTAL_BLOCK_ENTITY;
    public static BlockEntityType<LateGameBlockEntity> LATE_GAME_BLOCK_ENTITY;
    public static BlockEntityType<StorageBookshelfBlockEntity> STORAGE_BOOKSHELF_BLOCK_ENTITY;
    public static BlockEntityType<BookshelfCoordinatorBlockEntity> BOOKSHELF_COORDINATOR_BLOCK_ENTITY;

    private AuraContent() {
    }

    public static void bootstrap() {
        registerBlockEntityTypes();
        AuraItems.bootstrap();
        EXTRA_NETWORK_BLOCKS.put(VORTEX_PEDESTAL, Boolean.TRUE);
        AuraCascadeMod.LOGGER.info("Registered aura node, pump, consumer, storage, guidebook, fairy, ritual, and late-game runtime content.");
    }

    public static AuraNodeVariant nodeVariant(Block block) {
        return NODE_VARIANTS.get(block);
    }

    public static AuraPumpVariant pumpVariant(Block block) {
        return PUMP_VARIANTS.get(block);
    }

    public static AuraConsumerVariant consumerVariant(Block block) {
        return CONSUMER_VARIANTS.get(block);
    }

    public static LateGameVariant lateGameVariant(Block block) {
        return LATE_GAME_VARIANTS.get(block);
    }

    public static FortifiedBlockVariant fortifiedVariant(Block block) {
        return FORTIFIED_VARIANTS.get(block);
    }

    public static boolean isAuraNetworkBlock(Block block) {
        return NODE_VARIANTS.containsKey(block)
            || PUMP_VARIANTS.containsKey(block)
            || block == VORTEX_PEDESTAL
            || EXTRA_NETWORK_BLOCKS.containsKey(block);
    }

    public static void addDiscoverableItems(Consumer<ItemLike> consumer) {
        acceptRegisteredBlockItem(consumer, AURA_NODE);
        acceptRegisteredBlockItem(consumer, AURA_NODE_CAPACITOR);
        acceptRegisteredBlockItem(consumer, AURA_NODE_CONSERVE);
        acceptRegisteredBlockItem(consumer, AURA_NODE_BLACK);
        acceptRegisteredBlockItem(consumer, AURA_NODE_ORANGE);
        acceptRegisteredBlockItem(consumer, AURA_NODE_FLUX);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_ALT);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_LIGHT);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_LIGHT_ALT);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_FALL);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_FALL_ALT);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_PROJECTILE);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_PROJECTILE_ALT);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_REDSTONE);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_REDSTONE_ALT);
        acceptRegisteredBlockItem(consumer, AURA_NODE_PUMP_CREATIVE);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_ORE);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_ORE_ADV);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_FURNACE);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_PLANT);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_FISH);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_POTION);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_DYE);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_ANGEL);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_ENCHANT);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_LOOT);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_SPAWN);
        acceptRegisteredBlockItem(consumer, CONSUMER_BLOCK_MINER);
        acceptRegisteredBlockItem(consumer, RITUAL_NETHER);
        acceptRegisteredBlockItem(consumer, RITUAL_END);
        acceptRegisteredBlockItem(consumer, VORTEX_CONTROLLER);
        acceptRegisteredBlockItem(consumer, VORTEX_PEDESTAL);
        acceptRegisteredBlockItem(consumer, MONITOR);
        acceptRegisteredBlockItem(consumer, BOOKSHELF_COORDINATOR);
        acceptRegisteredBlockItem(consumer, TRAVELERS_BRICKS);
        acceptRegisteredBlockItem(consumer, REBOUNDING_ENIGMA);
        acceptRegisteredBlockItem(consumer, FORTIFIED_COBBLESTONE);
        acceptRegisteredBlockItem(consumer, FORTIFIED_STONE);
        acceptRegisteredBlockItem(consumer, FORTIFIED_PLANKS);
        acceptRegisteredBlockItem(consumer, FORTIFIED_GLASS);
        acceptRegisteredBlockItem(consumer, FORTIFIED_OBSIDIAN);
        acceptRegisteredBlockItem(consumer, FORTIFIED_DIRT);
    }

    private static void acceptRegisteredBlockItem(Consumer<ItemLike> consumer, Block block) {
        Item item = BuiltInRegistries.ITEM.getValue(BuiltInRegistries.BLOCK.getKey(block));
        if (item == null || item == Items.AIR) {
            throw new IllegalStateException("Missing registered Aura block item for " + BuiltInRegistries.BLOCK.getKey(block));
        }
        consumer.accept(item);
    }

    private static AuraNodeBlock registerNode(AuraNodeVariant variant) {
        AuraNodeBlock block = registerBlock(
            variant.registryPath(),
            new AuraNodeBlock(commonProperties(variant.registryPath(), MapColor.COLOR_PURPLE, SoundType.AMETHYST).noOcclusion())
        );
        NODE_VARIANTS.put(block, variant);
        return block;
    }

    private static AuraPumpBlock registerPump(AuraPumpVariant variant) {
        AuraPumpBlock block = registerBlock(
            variant.registryPath(),
            new AuraPumpBlock(commonProperties(variant.registryPath(), MapColor.COLOR_ORANGE, SoundType.COPPER).noOcclusion())
        );
        PUMP_VARIANTS.put(block, variant);
        return block;
    }

    private static AuraConsumerBlock registerConsumer(AuraConsumerVariant variant) {
        AuraConsumerBlock block = registerBlock(
            variant.registryPath(),
            new AuraConsumerBlock(commonProperties(variant.registryPath(), MapColor.COLOR_CYAN, SoundType.COPPER))
        );
        CONSUMER_VARIANTS.put(block, variant);
        return block;
    }

    private static LateGameWorldBlock registerLateGame(LateGameVariant variant, BlockBehaviour.Properties properties) {
        LateGameWorldBlock block = registerBlock(variant.registryPath(), new LateGameWorldBlock(properties));
        LATE_GAME_VARIANTS.put(block, variant);
        return block;
    }

    private static FortifiedBlock registerFortified(FortifiedBlockVariant variant) {
        FortifiedBlock block = registerBlock(
            variant.registryPath(),
            new FortifiedBlock(
                BlockBehaviour.Properties.of()
                    .setId(ResourceKey.create(Registries.BLOCK, id(variant.registryPath())))
                    .mapColor(MapColor.STONE)
                    .strength(variant.destroyTime(), variant.explosionResistance())
                    .sound(variant.translucent() ? SoundType.GLASS : SoundType.STONE)
                    .requiresCorrectToolForDrops()
            )
        );
        FORTIFIED_VARIANTS.put(block, variant);
        return block;
    }

    private static BlockBehaviour.Properties commonProperties(String path, MapColor color, SoundType soundType) {
        return BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id(path)))
            .mapColor(color)
            .strength(3.0F, 6.0F)
            .sound(soundType)
            .requiresCorrectToolForDrops();
    }

    private static <T extends Block> T registerBlock(String path, T block) {
        return registerBlock(path, block, true);
    }

    private static <T extends Block> T registerBlock(String path, T block, boolean withItem) {
        Registry.register(BuiltInRegistries.BLOCK, id(path), block);
        if (withItem) {
            Registry.register(
                BuiltInRegistries.ITEM,
                id(path),
                new BlockItem(
                    block,
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id(path))).useBlockDescriptionPrefix()
                )
            );
        }
        return block;
    }

    private static void registerBlockEntityTypes() {
        if (AURA_NODE_BLOCK_ENTITY != null) {
            return;
        }

        AURA_NODE_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("aura_node"),
            FabricBlockEntityTypeBuilder.create(
                AuraNodeBlockEntity::new,
                AURA_NODE,
                AURA_NODE_CAPACITOR,
                AURA_NODE_CONSERVE,
                AURA_NODE_BLACK,
                AURA_NODE_ORANGE,
                AURA_NODE_FLUX
            ).build(null)
        );

        AURA_PUMP_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("aura_node_pump"),
            FabricBlockEntityTypeBuilder.create(
                AuraPumpBlockEntity::new,
                AURA_NODE_PUMP,
                AURA_NODE_PUMP_ALT,
                AURA_NODE_PUMP_LIGHT,
                AURA_NODE_PUMP_LIGHT_ALT,
                AURA_NODE_PUMP_FALL,
                AURA_NODE_PUMP_FALL_ALT,
                AURA_NODE_PUMP_PROJECTILE,
                AURA_NODE_PUMP_PROJECTILE_ALT,
                AURA_NODE_PUMP_REDSTONE,
                AURA_NODE_PUMP_REDSTONE_ALT,
                AURA_NODE_PUMP_CREATIVE
            ).build(null)
        );

        AURA_CONSUMER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("aura_consumer"),
            FabricBlockEntityTypeBuilder.create(
                AuraConsumerBlockEntity::new,
                CONSUMER_BLOCK_ORE,
                CONSUMER_BLOCK_ORE_ADV,
                CONSUMER_BLOCK_FURNACE,
                CONSUMER_BLOCK_PLANT,
                CONSUMER_BLOCK_FISH,
                CONSUMER_BLOCK_POTION,
                CONSUMER_BLOCK_DYE,
                CONSUMER_BLOCK_ANGEL,
                CONSUMER_BLOCK_ENCHANT
            ).build(null)
        );

        VORTEX_CONTROLLER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("aura_node_crafting_center"),
            FabricBlockEntityTypeBuilder.create(VortexControllerBlockEntity::new, VORTEX_CONTROLLER).build(null)
        );

        VORTEX_PEDESTAL_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("aura_node_crafting_pedestal"),
            FabricBlockEntityTypeBuilder.create(VortexPedestalBlockEntity::new, VORTEX_PEDESTAL).build(null)
        );

        LATE_GAME_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("late_game_world"),
            FabricBlockEntityTypeBuilder.create(
                LateGameBlockEntity::new,
                CONSUMER_BLOCK_LOOT,
                CONSUMER_BLOCK_SPAWN,
                CONSUMER_BLOCK_MINER,
                RITUAL_NETHER,
                RITUAL_END
            ).build(null)
        );

        STORAGE_BOOKSHELF_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("storage_bookshelf"),
            FabricBlockEntityTypeBuilder.create(StorageBookshelfBlockEntity::new, STORAGE_BOOKSHELF).build(null)
        );

        BOOKSHELF_COORDINATOR_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            id("bookshelf_coordinator"),
            FabricBlockEntityTypeBuilder.create(BookshelfCoordinatorBlockEntity::new, BOOKSHELF_COORDINATOR).build(null)
        );
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(AuraCascadeMod.MOD_ID, path);
    }
}
