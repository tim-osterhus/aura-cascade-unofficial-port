package pixlepix.auracascade.block.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Blocks;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.block.FortifiedBlockVariant;
import pixlepix.auracascade.block.LateGameVariant;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LateGameWorldLogicTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void ritualMappingsMatchLegacyBlockConversions() {
        RandomSource random = RandomSource.create(123L);

        assertEquals(Blocks.NETHERRACK, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.STONE, random));
        assertEquals(Blocks.GLOWSTONE, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.OAK_LOG, random));
        assertEquals(Blocks.NETHER_WART, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.SHORT_GRASS, random));
        assertEquals(Blocks.SOUL_SAND, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.GRAVEL, random));
        assertEquals(Blocks.LAVA, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.WATER, random));
        assertEquals(Blocks.END_STONE, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_END, Blocks.DIRT, random));
        assertEquals(Blocks.OBSIDIAN, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_END, Blocks.OAK_LEAVES, random));
        assertEquals(Blocks.END_STONE, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_END, Blocks.SAND, random));
        assertNull(LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_END, Blocks.WATER, random));
    }

    @Test
    void minerContainmentAndYieldStayLateGameGated() {
        assertTrue(LateGameWorldLogic.minerContainmentFails(0));
        assertFalse(LateGameWorldLogic.minerContainmentFails(6));
        assertEquals(0, LateGameWorldLogic.minerOreYield(20));
        assertEquals(0, LateGameWorldLogic.minerOreYield(1));
        assertEquals(1, LateGameWorldLogic.minerOreYield(21));
        assertTrue(LateGameWorldLogic.minerOreYield(50) > LateGameWorldLogic.minerOreYield(30));
        assertEquals(100, LateGameWorldLogic.minerExplosionCooldown(1));
        assertEquals(80, LateGameWorldLogic.minerExplosionCooldown(10));
        assertEquals(0.25D, LateGameWorldLogic.minerBounceAmplitude(1));
        assertEquals(0.5D, LateGameWorldLogic.minerBounceAmplitude(10));
        assertTrue(FortifiedBlockVariant.OBSIDIAN.resistChance() > FortifiedBlockVariant.GLASS.resistChance());
    }

    @Test
    void spawnerUsesTheSuppliedNaturalSpawnListAndRitualHasBoundedRadius() {
        var naturalSpawns = WeightedRandomList.create(
            new MobSpawnSettings.SpawnerData(EntityType.HUSK, 10, 1, 1)
        );
        assertEquals(EntityType.HUSK, LateGameWorldLogic.chooseSpawnType(naturalSpawns, RandomSource.create(0L)));
        assertNull(LateGameWorldLogic.chooseSpawnType(
            WeightedRandomList.<MobSpawnSettings.SpawnerData>create(), RandomSource.create(0L)
        ));
        assertTrue(LateGameWorldLogic.withinRitualRadius(150, 0));
        assertFalse(LateGameWorldLogic.withinRitualRadius(151, 0));
        assertFalse(LateGameWorldLogic.withinRitualRadius(106, 107));
        assertFalse(LateGameWorldLogic.withinRitualRadius(Integer.MAX_VALUE, 0));
        assertEquals(new BlockPos(-4, 70, 4), LateGameWorldLogic.ritualCellOrigin(new BlockPos(-1, 70, 7)));
        assertTrue(LateGameWorldLogic.ritualCellIntersectsRadius(new BlockPos(148, 70, 0), BlockPos.ZERO));
        assertFalse(LateGameWorldLogic.ritualCellIntersectsRadius(new BlockPos(152, 70, 0), BlockPos.ZERO));
    }

    @Test
    void looterSelectsOneGeneratedChestStackWithoutInventingItems() {
        ItemStack first = new ItemStack(Items.NAME_TAG);
        ItemStack second = new ItemStack(Items.ENCHANTED_BOOK);
        ItemStack result = LateGameWorldLogic.chooseGeneratedLoot(
            List.of(ItemStack.EMPTY, first, second), RandomSource.create(3L)
        );
        assertTrue(result.is(Items.NAME_TAG) || result.is(Items.ENCHANTED_BOOK));
        assertEquals(1, result.getCount());
        assertTrue(LateGameWorldLogic.chooseGeneratedLoot(List.of(), RandomSource.create(0L)).isEmpty());
    }

    @Test
    void minerProducesOreBlocksRatherThanRawMaterialShortcuts() {
        List<net.minecraft.world.item.Item> vanillaOres = List.of(
            Blocks.COAL_ORE.asItem(), Blocks.IRON_ORE.asItem(), Blocks.COPPER_ORE.asItem(),
            Blocks.REDSTONE_ORE.asItem(), Blocks.GOLD_ORE.asItem(), Blocks.LAPIS_ORE.asItem(),
            Blocks.DIAMOND_ORE.asItem(), Blocks.EMERALD_ORE.asItem(), Blocks.NETHER_QUARTZ_ORE.asItem()
        );
        RandomSource random = RandomSource.create(592L);
        for (int index = 0; index < 100; index++) {
            ItemStack ore = LateGameWorldLogic.chooseOre(random);
            assertTrue(vanillaOres.contains(ore.getItem()));
            assertEquals(1, ore.getCount());
        }
    }

    @Test
    void minerAlsoSelectsExistingTaggedOreItemsWithoutDuplicatingVanillaEntries() {
        RandomSource random = RandomSource.create(592L);
        boolean foundModernOre = false;
        for (int index = 0; index < 10_000; index++) {
            ItemStack ore = LateGameWorldLogic.chooseOre(random,
                List.of(Blocks.DEEPSLATE_DIAMOND_ORE.asItem(), Blocks.COAL_ORE.asItem()));
            if (ore.is(Blocks.DEEPSLATE_DIAMOND_ORE.asItem())) {
                foundModernOre = true;
                break;
            }
        }
        assertTrue(foundModernOre);
        RandomSource base = RandomSource.create(7L);
        RandomSource duplicated = RandomSource.create(7L);
        for (int index = 0; index < 100; index++) {
            assertEquals(LateGameWorldLogic.chooseOre(base).getItem(),
                LateGameWorldLogic.chooseOre(duplicated, List.of(Blocks.COAL_ORE.asItem())).getItem());
        }
    }
}
