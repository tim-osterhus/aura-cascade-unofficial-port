package pixlepix.auracascade.compat;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraAccessoryStateTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void equipExclusiveLeavesOnlyOneAccessoryEquippedPerSlot() {
        ItemStack firstRing = new ItemStack(Items.GOLD_INGOT);
        ItemStack secondRing = new ItemStack(Items.IRON_INGOT);

        AuraAccessoryState.equipExclusive(AuraAccessorySlot.RING, firstRing, List.of(firstRing, secondRing));
        assertTrue(AuraAccessoryState.isEquipped(firstRing, AuraAccessorySlot.RING));
        assertFalse(AuraAccessoryState.isEquipped(secondRing, AuraAccessorySlot.RING));

        AuraAccessoryState.equipExclusive(AuraAccessorySlot.RING, secondRing, List.of(firstRing, secondRing));
        assertFalse(AuraAccessoryState.isEquipped(firstRing, AuraAccessorySlot.RING));
        assertTrue(AuraAccessoryState.isEquipped(secondRing, AuraAccessorySlot.RING));
    }

    @Test
    void equippedSlotRoundTripsThroughCustomData() {
        ItemStack belt = new ItemStack(Items.LEATHER);

        AuraAccessoryState.setEquipped(belt, AuraAccessorySlot.BELT, true);
        assertEquals(AuraAccessorySlot.BELT, AuraAccessoryState.equippedSlot(belt).orElseThrow());

        AuraAccessoryState.setEquipped(belt, AuraAccessorySlot.BELT, false);
        assertTrue(AuraAccessoryState.equippedSlot(belt).isEmpty());
    }
}
