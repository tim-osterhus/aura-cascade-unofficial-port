package pixlepix.auracascade.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PrismaticWandLegacyContractTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void selectionSlidesAndCopyPersistsOnlyCoordinatesAndPlayerOffset() {
        ItemStack wand = new ItemStack(Items.STICK);
        CustomData.update(DataComponents.CUSTOM_DATA, wand, tag -> {
            tag.putString("clipboard", "old raw state ids");
            tag.putString("unrelated", "preserved");
        });
        assertTrue(PrismaticWandState.setSelectionPoint(wand, new BlockPos(2, 3, 4)));
        assertFalse(PrismaticWandState.setSelectionPoint(wand, new BlockPos(5, 6, 7)));
        assertEquals(new BlockPos(5, 6, 7), PrismaticWandState.selection(wand).first());
        assertEquals(new BlockPos(2, 3, 4), PrismaticWandState.selection(wand).second());

        assertTrue(PrismaticWandState.copySelection(wand, new BlockPos(10, 20, 30)));
        PrismaticWandState.CopiedRegion copied = PrismaticWandState.copiedRegion(wand);
        assertEquals(new BlockPos(2, 3, 4), copied.min());
        assertEquals(new BlockPos(5, 6, 7), copied.max());
        assertEquals(new BlockPos(-8, -17, -26), copied.playerOffset());
        var data = wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        assertFalse(data.contains("clipboard"));
        assertEquals("preserved", data.getString("unrelated"));
        assertFalse(data.contains("state_id"));

        PrismaticWandState.setSelectionPoint(wand, new BlockPos(8, 9, 10));
        assertEquals(new BlockPos(5, 6, 7), PrismaticWandState.selection(wand).second());
        assertEquals(copied, PrismaticWandState.copiedRegion(wand));
    }

    @Test
    void staleSnapshotIsInertAndClearedOnModeChange() {
        ItemStack wand = new ItemStack(Items.STICK);
        CustomData.update(DataComponents.CUSTOM_DATA, wand, tag -> tag.putString("clipboard", "0|0|0|123|minecraft:stone"));

        assertNull(PrismaticWandState.copiedRegion(wand));
        assertEquals(PrismaticWandState.Mode.COPY, PrismaticWandState.cycleMode(wand));
        assertFalse(wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("clipboard"));
    }

    @Test
    void fullGeometryAndUnsafeChunksAreRejectedBeforeAnyPlacement() {
        assertEquals(512, PrismaticWandItem.regionVolume(BlockPos.ZERO, new BlockPos(7, 7, 7)));
        assertEquals(-1, PrismaticWandItem.regionVolume(BlockPos.ZERO, new BlockPos(512, 0, 0)));
        FakeWorld world = new FakeWorld();
        world.blocked = new BlockPos(21, 0, 0);
        world.blocks.put(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, new ItemStack(Items.STONE, 2));
        var copied = new PrismaticWandState.CopiedRegion(BlockPos.ZERO, new BlockPos(1, 0, 0), BlockPos.ZERO);

        PrismaticWandItem.PasteResult result = PrismaticWandItem.paste(world, copied, new BlockPos(20, 0, 0), inventory, 1, false);

        assertFalse(result.safe());
        assertEquals(0, result.placed());
        assertEquals(0, world.writes);
        assertEquals(2, inventory.getItem(0).getCount());
    }

    @Test
    void adventurePasteCannotMutateWorldOrConsumeMaterials() {
        FakeWorld world = new FakeWorld();
        world.mayBuild = false;
        world.blocks.put(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, new ItemStack(Items.STONE));
        var copied = new PrismaticWandState.CopiedRegion(BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO);

        var result = PrismaticWandItem.paste(world, copied, new BlockPos(20, 0, 0), inventory, 1, false);

        assertFalse(result.safe());
        assertEquals(0, world.writes);
        assertEquals(1, inventory.getItem(0).getCount());
    }

    @Test
    void remoteProtectedDestinationRejectsWholePasteBeforeMutation() {
        FakeWorld world = new FakeWorld();
        world.protectedPos = new BlockPos(21, 0, 0);
        world.blocks.put(BlockPos.ZERO, Blocks.STONE.defaultBlockState());
        world.blocks.put(new BlockPos(1, 0, 0), Blocks.STONE.defaultBlockState());
        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, new ItemStack(Items.STONE, 2));
        var copied = new PrismaticWandState.CopiedRegion(BlockPos.ZERO, new BlockPos(1, 0, 0), BlockPos.ZERO);

        var result = PrismaticWandItem.paste(world, copied, new BlockPos(20, 0, 0), inventory, 1, false);

        assertFalse(result.safe());
        assertEquals(0, world.writes);
        assertEquals(2, inventory.getItem(0).getCount());
    }

    @Test
    void pasteUsesCurrentSourceAndSkipsOccupiedTargetsWithoutCharging() {
        FakeWorld world = new FakeWorld();
        world.blocks.put(new BlockPos(0, 0, 0), Blocks.STONE.defaultBlockState());
        world.blocks.put(new BlockPos(1, 0, 0), Blocks.DIRT.defaultBlockState());
        world.blocks.put(new BlockPos(2, 0, 0), Blocks.STONE.defaultBlockState());
        world.blocks.put(new BlockPos(21, 0, 0), Blocks.BEDROCK.defaultBlockState());
        world.failed = new BlockPos(22, 0, 0);
        SimpleContainer inventory = new SimpleContainer(2);
        inventory.setItem(0, new ItemStack(Items.STONE, 2));
        inventory.setItem(1, new ItemStack(Items.DIRT));
        var copied = new PrismaticWandState.CopiedRegion(BlockPos.ZERO, new BlockPos(2, 0, 0), BlockPos.ZERO);

        PrismaticWandItem.PasteResult result = PrismaticWandItem.paste(world, copied, new BlockPos(20, 0, 0), inventory, 2, false);

        assertTrue(result.safe());
        assertEquals(1, result.placed());
        assertEquals(Blocks.STONE.defaultBlockState(), world.getBlockState(new BlockPos(20, 0, 0)));
        assertEquals(Blocks.BEDROCK.defaultBlockState(), world.getBlockState(new BlockPos(21, 0, 0)));
        assertTrue(world.getBlockState(new BlockPos(22, 0, 0)).isAir());
        assertEquals(1, inventory.getItem(0).getCount());
        assertEquals(1, inventory.getItem(1).getCount());

        world.blocks.put(BlockPos.ZERO, Blocks.DIRT.defaultBlockState());
        world.failed = null;
        var oneBlock = new PrismaticWandState.CopiedRegion(BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO);
        PrismaticWandItem.PasteResult again = PrismaticWandItem.paste(world, oneBlock, new BlockPos(30, 0, 0), inventory, 2, false);
        assertEquals(1, again.placed());
        assertEquals(Blocks.DIRT.defaultBlockState(), world.getBlockState(new BlockPos(30, 0, 0)));
    }

    @Test
    void namedOrFilledShulkerIsNeverConsumedAsPlainBuildingMaterial() {
        FakeWorld world = new FakeWorld();
        world.blocks.put(BlockPos.ZERO, Blocks.SHULKER_BOX.defaultBlockState());
        SimpleContainer inventory = new SimpleContainer(3);
        ItemStack named = new ItemStack(Items.SHULKER_BOX);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Keepsake"));
        ItemStack filled = new ItemStack(Items.SHULKER_BOX);
        filled.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND))));
        ItemStack namedBefore = named.copy();
        ItemStack filledBefore = filled.copy();
        inventory.setItem(0, named);
        inventory.setItem(1, filled);
        var copied = new PrismaticWandState.CopiedRegion(BlockPos.ZERO, BlockPos.ZERO, BlockPos.ZERO);

        PrismaticWandItem.PasteResult withoutPlain = PrismaticWandItem.paste(
            world, copied, new BlockPos(20, 0, 0), inventory, 3, false);
        assertEquals(0, withoutPlain.placed());
        assertTrue(withoutPlain.missingMaterial());
        assertTrue(world.getBlockState(new BlockPos(20, 0, 0)).isAir());
        assertTrue(ItemStack.matches(namedBefore, inventory.getItem(0)));
        assertTrue(ItemStack.matches(filledBefore, inventory.getItem(1)));

        inventory.setItem(2, new ItemStack(Items.SHULKER_BOX));
        PrismaticWandItem.PasteResult withPlain = PrismaticWandItem.paste(
            world, copied, new BlockPos(30, 0, 0), inventory, 3, false);
        assertEquals(1, withPlain.placed());
        assertEquals(Blocks.SHULKER_BOX.defaultBlockState(), world.getBlockState(new BlockPos(30, 0, 0)));
        assertTrue(ItemStack.matches(namedBefore, inventory.getItem(0)));
        assertTrue(ItemStack.matches(filledBefore, inventory.getItem(1)));
        assertTrue(inventory.getItem(2).isEmpty());
    }

    private static final class FakeWorld implements PrismaticWandItem.WorldAccess {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private BlockPos blocked;
        private BlockPos protectedPos;
        private BlockPos failed;
        private boolean mayBuild = true;
        private int writes;

        @Override
        public boolean mayBuild() {
            return mayBuild;
        }

        @Override
        public boolean mayInteract(BlockPos pos) {
            return !pos.equals(protectedPos);
        }

        @Override
        public boolean isSafe(BlockPos pos) {
            return !pos.equals(blocked) && pos.getY() >= 0 && pos.getY() < 256;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public boolean setBlockState(BlockPos pos, BlockState state) {
            writes++;
            if (pos.equals(failed)) {
                return false;
            }
            blocks.put(pos, state);
            return true;
        }
    }
}
