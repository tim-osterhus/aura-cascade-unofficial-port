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

        List<VortexCraftingLogic.PedestalInput> lowAuraInputs = List.of(
            pedestal(new BlockPos(0, 0, 0), Items.QUARTZ, AuraColor.WHITE, 700),
            pedestal(new BlockPos(1, 0, 0), Items.AMETHYST_SHARD, AuraColor.VIOLET, 800),
            pedestal(new BlockPos(2, 0, 0), Items.REDSTONE, AuraColor.RED, 500),
            pedestal(new BlockPos(3, 0, 0), Items.GLOWSTONE_DUST, AuraColor.YELLOW, 599)
        );

        var match = VortexCraftingLogic.findMatch(lowAuraInputs, List.of(recipe));
        assertTrue(match.isPresent());
        assertFalse(VortexCraftingLogic.ready(match.get(), byPos(lowAuraInputs)));

        List<VortexCraftingLogic.PedestalInput> chargedInputs = List.of(
            pedestal(new BlockPos(0, 0, 0), Items.QUARTZ, AuraColor.WHITE, 700),
            pedestal(new BlockPos(1, 0, 0), Items.AMETHYST_SHARD, AuraColor.VIOLET, 800),
            pedestal(new BlockPos(2, 0, 0), Items.REDSTONE, AuraColor.RED, 500),
            pedestal(new BlockPos(3, 0, 0), Items.GLOWSTONE_DUST, AuraColor.YELLOW, 600)
        );

        assertTrue(VortexCraftingLogic.ready(match.get(), byPos(chargedInputs)));
    }

    @Test
    void progressSignalCoversIdleMidpointAndCompletion() {
        assertEquals(0, VortexCraftingLogic.progressSignal(0, 100));
        assertEquals(8, VortexCraftingLogic.progressSignal(50, 100));
        assertEquals(15, VortexCraftingLogic.progressSignal(100, 100));
    }

    private static VortexCraftingLogic.PedestalInput pedestal(BlockPos pos, net.minecraft.world.item.Item item, AuraColor color, int amount) {
        return new VortexCraftingLogic.PedestalInput(pos, new ItemStack(item), AuraStorage.of(color, amount));
    }

    private static Map<BlockPos, VortexCraftingLogic.PedestalInput> byPos(List<VortexCraftingLogic.PedestalInput> pedestals) {
        LinkedHashMap<BlockPos, VortexCraftingLogic.PedestalInput> byPos = new LinkedHashMap<>();
        for (VortexCraftingLogic.PedestalInput pedestal : pedestals) {
            byPos.put(pedestal.pos(), pedestal);
        }
        return byPos;
    }
}
