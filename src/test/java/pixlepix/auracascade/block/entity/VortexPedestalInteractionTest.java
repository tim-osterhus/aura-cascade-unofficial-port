package pixlepix.auracascade.block.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VortexPedestalInteractionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void handSelectionCopiesExactlyOneWithoutMutatingTheHandStackOrItsData() {
        ItemStack hand = new ItemStack(Items.DIAMOND, 12);
        hand.set(DataComponents.CUSTOM_NAME, Component.literal("Ritual diamond"));

        ItemStack selected = VortexPedestalBlockEntity.selectedItem(hand);

        assertNotSame(hand, selected);
        assertEquals(12, hand.getCount());
        assertEquals(1, selected.getCount());
        assertEquals(hand.get(DataComponents.CUSTOM_NAME), selected.get(DataComponents.CUSTOM_NAME));
        assertTrue(VortexPedestalBlockEntity.selectedItem(ItemStack.EMPTY).isEmpty());
    }
}
