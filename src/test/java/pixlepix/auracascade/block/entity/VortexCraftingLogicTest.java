package pixlepix.auracascade.block.entity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.data.recipe.AuraVortexRecipe;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VortexCraftingLogicTest {
    @Test
    void matchesPedestalItemsWithoutCaringAboutOrderAndChecksAuraReadiness() {
        TestMinecraftBootstrap.ensureBootstrapped();

        AuraVortexRecipe recipe = new AuraVortexRecipe(
            "test:vortex_recipe",
            List.of(
                new AuraVortexRecipe.Component(Items.REDSTONE, 1, AuraStorage.of(AuraColor.RED, 500)),
                new AuraVortexRecipe.Component(Items.GLOWSTONE_DUST, 1, AuraStorage.of(AuraColor.YELLOW, 600)),
                new AuraVortexRecipe.Component(Items.QUARTZ, 1, AuraStorage.of(AuraColor.WHITE, 700)),
                new AuraVortexRecipe.Component(Items.AMETHYST_SHARD, 1, AuraStorage.of(AuraColor.VIOLET, 800))
            ),
            new ItemStack(Items.DIAMOND),
            120
        );

        List<VortexCraftingLogic.PedestalInput> lowPowerInputs = List.of(
            pedestal(new BlockPos(0, 0, 0), Items.QUARTZ, AuraColor.WHITE, 700, 700),
            pedestal(new BlockPos(1, 0, 0), Items.AMETHYST_SHARD, AuraColor.VIOLET, 800, 800),
            pedestal(new BlockPos(2, 0, 0), Items.REDSTONE, AuraColor.RED, 500, 500),
            pedestal(new BlockPos(3, 0, 0), Items.GLOWSTONE_DUST, AuraColor.YELLOW, 599, 600)
        );

        var match = VortexCraftingLogic.findMatch(lowPowerInputs, List.of(recipe));
        assertTrue(match.isPresent());
        assertFalse(VortexCraftingLogic.ready(match.get(), byPos(lowPowerInputs)));

        List<VortexCraftingLogic.PedestalInput> chargedInputs = List.of(
            pedestal(new BlockPos(0, 0, 0), Items.QUARTZ, AuraColor.WHITE, 700, 700),
            pedestal(new BlockPos(1, 0, 0), Items.AMETHYST_SHARD, AuraColor.VIOLET, 800, 800),
            pedestal(new BlockPos(2, 0, 0), Items.REDSTONE, AuraColor.RED, 500, 500),
            pedestal(new BlockPos(3, 0, 0), Items.GLOWSTONE_DUST, AuraColor.YELLOW, 600, 600)
        );

        assertTrue(VortexCraftingLogic.ready(match.get(), byPos(chargedInputs)));
        assertFalse(VortexCraftingLogic.ready(match.get(), byPos(List.of(
            pedestal(new BlockPos(0, 0, 0), Items.QUARTZ, AuraColor.RED, 700, 700),
            chargedInputs.get(1), chargedInputs.get(2), chargedInputs.get(3)
        ))));
    }

    @Test
    void progressSignalCoversIdleMidpointAndCompletion() {
        assertEquals(0, VortexCraftingLogic.progressSignal(0, 100));
        assertEquals(8, VortexCraftingLogic.progressSignal(50, 100));
        assertEquals(15, VortexCraftingLogic.progressSignal(100, 100));
    }

    @Test
    void repeatedIngredientNeedsFourDistinctPedestals() {
        TestMinecraftBootstrap.ensureBootstrapped();
        var component = new AuraVortexRecipe.Component(Items.DIAMOND, 1, AuraStorage.of(AuraColor.RED, 100000));
        var recipe = new AuraVortexRecipe("test:four_diamonds", List.of(component, component, component, component),
            new ItemStack(Items.EMERALD), 100);
        List<VortexCraftingLogic.PedestalInput> inputs = List.of(
            pedestal(new BlockPos(0, 0, -1), Items.DIAMOND, AuraColor.RED, 100000, 100000),
            pedestal(new BlockPos(1, 0, 0), Items.DIAMOND, AuraColor.RED, 100000, 100000),
            pedestal(new BlockPos(0, 0, 1), Items.DIAMOND, AuraColor.RED, 100000, 100000),
            pedestal(new BlockPos(-1, 0, 0), Items.DIAMOND, AuraColor.RED, 100000, 100000)
        );
        var match = VortexCraftingLogic.match(recipe, inputs);
        assertTrue(match.isPresent());
        assertEquals(4, match.get().assignments().size());
        assertTrue(VortexCraftingLogic.ready(match.get(), byPos(inputs)));
        assertFalse(VortexCraftingLogic.match(recipe, inputs.subList(0, 3)).isPresent());
    }

    private static VortexCraftingLogic.PedestalInput pedestal(BlockPos pos, net.minecraft.world.item.Item item,
                                                              AuraColor color, int received, int required) {
        return new VortexCraftingLogic.PedestalInput(pos, new ItemStack(item), received, color, required);
    }

    private static Map<BlockPos, VortexCraftingLogic.PedestalInput> byPos(List<VortexCraftingLogic.PedestalInput> pedestals) {
        LinkedHashMap<BlockPos, VortexCraftingLogic.PedestalInput> byPos = new LinkedHashMap<>();
        for (VortexCraftingLogic.PedestalInput pedestal : pedestals) {
            byPos.put(pedestal.pos(), pedestal);
        }
        return byPos;
    }
}
