package pixlepix.auracascade.item;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.*;

public final class AngelsteelDropFortuneTest {
    @BeforeAll
    static void bootstrap() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void onlyTheLootCopyReceivesFortuneAndOtherComponentsSurvive() {
        RuntimeCases.copyOnly();
    }

    @Test
    void equalOrStrongerFortuneIsNeverLowered() {
        RuntimeCases.strongerFortune();
    }

    @Test
    void wrongToolCropsAndNonAngelsteelToolsDoNotReceiveFortune() {
        RuntimeCases.eligibilityGates();
    }

    @Test
    void uninitializedToolsAreNotRolledDuringDrops() {
        RuntimeCases.uninitialized();
    }

    @Test
    void runtimeUsesDropLocalToolWithoutBreakCallbackBookkeeping() throws Exception {
        String items = Files.readString(Path.of("src/main/java/pixlepix/auracascade/item/AuraItems.java"));
        assertFalse(items.contains("prepareAngelsteelFortune"));
        assertFalse(items.contains("restoreAngelsteelFortune"));
        assertFalse(items.contains("ANGELSTEEL_FORTUNE_OVERRIDES"));
        String effects = Files.readString(Path.of("src/main/java/pixlepix/auracascade/enchantment/KaleidoscopicOriginalEffects.java"));
        assertTrue(effects.contains("AngelsteelToolHelper.dropFortuneTool(tool, state, fortune)"));
        assertTrue(effects.contains("Block.dropResources(state, level, pos, blockEntity, breaker, lootTool)"));
    }

    public static final class RuntimeCases {
        private static final Holder<Enchantment> FORTUNE = enchantment("fortune");
        private static final Holder<Enchantment> SILK_TOUCH = enchantment("silk_touch");

        public static void copyOnly() {
            ItemStack held = tool();
            held.enchant(FORTUNE, 1);
            held.enchant(SILK_TOUCH, 1);
            held.set(DataComponents.CUSTOM_NAME, Component.literal("Keep this tool"));
            held.setDamageValue(3);
            ItemStack before = held.copy();

            ItemStack loot = AngelsteelToolHelper.dropFortuneTool(held, Blocks.STONE.defaultBlockState(), FORTUNE);
            assertNotSame(held, loot);
            assertEquals(4, loot.getEnchantments().getLevel(FORTUNE));
            assertEquals(1, loot.getEnchantments().getLevel(SILK_TOUCH));
            assertEquals(held.get(DataComponents.CUSTOM_DATA), loot.get(DataComponents.CUSTOM_DATA));
            assertEquals(held.get(DataComponents.CUSTOM_NAME), loot.get(DataComponents.CUSTOM_NAME));
            assertEquals(3, loot.getDamageValue());
            assertEquals(held.getCount(), loot.getCount());
            assertTrue(ItemStack.isSameItemSameComponents(before, held));

            ItemStack secondDrop = AngelsteelToolHelper.dropFortuneTool(held, Blocks.STONE.defaultBlockState(), FORTUNE);
            assertNotSame(loot, secondDrop);
            assertTrue(ItemStack.isSameItemSameComponents(loot, secondDrop));
            assertSame(loot, AngelsteelToolHelper.dropFortuneTool(loot, Blocks.STONE.defaultBlockState(), FORTUNE));
            assertTrue(ItemStack.isSameItemSameComponents(before, held));
        }

        public static void strongerFortune() {
            for (int level : new int[] {4, 6}) {
                ItemStack held = tool();
                held.enchant(FORTUNE, level);
                held.enchant(SILK_TOUCH, 1);
                ItemStack before = held.copy();
                assertSame(held, AngelsteelToolHelper.dropFortuneTool(held, Blocks.STONE.defaultBlockState(), FORTUNE));
                assertTrue(ItemStack.isSameItemSameComponents(before, held));
            }
        }

        public static void eligibilityGates() {
            ItemStack held = tool();
            assertFalse(held.isCorrectToolForDrops(Blocks.DIRT.defaultBlockState()));
            assertSame(held, AngelsteelToolHelper.dropFortuneTool(held, Blocks.DIRT.defaultBlockState(), FORTUNE));
            // Deliberately correct for the crop so this exercises the independent crop veto.
            assertTrue(held.isCorrectToolForDrops(Blocks.WHEAT.defaultBlockState()));
            assertSame(held, AngelsteelToolHelper.dropFortuneTool(held, Blocks.WHEAT.defaultBlockState(), FORTUNE));
            ItemStack ordinary = new ItemStack(Items.DIAMOND_PICKAXE);
            ordinary.set(DataComponents.CUSTOM_DATA, held.get(DataComponents.CUSTOM_DATA));
            ordinary.set(DataComponents.TOOL, held.get(DataComponents.TOOL));
            assertTrue(ordinary.isCorrectToolForDrops(Blocks.STONE.defaultBlockState()));
            assertSame(ordinary, AngelsteelToolHelper.dropFortuneTool(ordinary, Blocks.STONE.defaultBlockState(), FORTUNE));
        }

        public static void uninitialized() {
            ItemStack held = tool();
            held.remove(DataComponents.CUSTOM_DATA);
            ItemStack before = held.copy();
            assertSame(held, AngelsteelToolHelper.dropFortuneTool(held, Blocks.STONE.defaultBlockState(), FORTUNE));
            assertTrue(ItemStack.isSameItemSameComponents(before, held));
            assertNull(held.get(DataComponents.CUSTOM_DATA));
        }

        private static ItemStack tool() {
            ItemStack stack = new ItemStack(AuraItems.angelsteelTool(AngelsteelToolKind.PICKAXE, 3));
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putIntArray(AngelsteelToolHelper.NBT_BUFF_ARRAY_NAME, new int[] {1, 4, 0, 1});
                tag.putString("unrelated", "preserved");
            });
            // Explicit rules keep this stack-level test independent of unloaded datapack tags.
            stack.set(DataComponents.TOOL, new Tool(List.of(Tool.Rule.minesAndDrops(List.of(Blocks.STONE, Blocks.WHEAT), 5.0F)), 1.0F, 1));
            return stack;
        }

        private static Holder<Enchantment> enchantment(String name) {
            return Holder.direct(new Enchantment(Component.literal(name),
                Enchantment.definition(HolderSet.direct(), 1, 10, Enchantment.constantCost(1),
                    Enchantment.constantCost(20), 1, EquipmentSlotGroup.MAINHAND),
                HolderSet.direct(), DataComponentMap.EMPTY));
        }
    }
}
