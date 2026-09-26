package pixlepix.auracascade.item;

import java.util.Arrays;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RingOfBindingItemTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void ringStoresFairyRolesUpToTheLegacyCapAndClearsThem() {
        ItemStack ring = new ItemStack(Items.GOLD_INGOT);
        List<FairyRole> storedRoles = List.of(
            FairyRole.BASIC,
            FairyRole.FIGHTER,
            FairyRole.DEBUFFER,
            FairyRole.BUFFER,
            FairyRole.STEALER,
            FairyRole.PUSHER,
            FairyRole.SHOOTER,
            FairyRole.SAVIOR,
            FairyRole.FETCHER,
            FairyRole.BAITER,
            FairyRole.BREEDER,
            FairyRole.SCARER,
            FairyRole.EXTINGUISHER,
            FairyRole.DIGGER,
            FairyRole.LIGHTER
        );

        for (FairyRole role : storedRoles) {
            assertTrue(RingOfBindingItem.bindCharm(ring, role));
        }

        assertEquals(storedRoles.size(), RingOfBindingItem.boundFairyCount(ring));
        assertFalse(RingOfBindingItem.bindCharm(ring, FairyRole.TRAINER));
        List<FairyRole> released = RingOfBindingItem.releaseBoundFairies(ring);
        assertEquals(storedRoles, released);
        assertEquals(0, RingOfBindingItem.boundFairyCount(ring));
    }

    @Test
    void legacyCountMigrationDefaultsToBaseFairies() {
        ItemStack ring = new ItemStack(Items.GOLD_INGOT);
        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> tag.putInt("boundFairyCount", 3));

        assertEquals(List.of(FairyRole.BASIC, FairyRole.BASIC, FairyRole.BASIC), RingOfBindingItem.boundFairies(ring));
    }

    @Test
    void legacyFairyListRolesMigrateWhenTheRingIsUpdated() {
        ItemStack ring = new ItemStack(Items.GOLD_INGOT);
        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> tag.putIntArray("fairyList", new int[] { 1, 16 }));

        assertEquals(List.of(FairyRole.FIGHTER, FairyRole.TRAINER), RingOfBindingItem.boundFairies(ring));
        assertTrue(RingOfBindingItem.bindCharm(ring, FairyRole.BUFFER));

        var migratedTag = ring.get(DataComponents.CUSTOM_DATA).copyTag();
        assertFalse(migratedTag.contains("fairyList"));
        assertFalse(migratedTag.contains("boundFairyCount"));
        assertArrayEquals(new int[] { 1, 16, 3 }, migratedTag.getIntArray("boundFairies"));
    }

    @Test
    void recoveredFairyListOrderAndExistingStoredOrderRemainDistinct() {
        ItemStack ring = new ItemStack(Items.GOLD_INGOT);
        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> tag.putIntArray("fairyList", new int[] { 14, 15 }));
        assertEquals(List.of(FairyRole.GLIDER, FairyRole.LIGHTER), RingOfBindingItem.boundFairies(ring));

        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> {
            tag.remove("fairyList");
            tag.putIntArray("boundFairies", new int[] { 14, 15 });
        });
        assertEquals(List.of(FairyRole.LIGHTER, FairyRole.GLIDER), RingOfBindingItem.boundFairies(ring));
    }

    @Test
    void bothRoleArrayFormatsAndLegacyCountAreClampedToTheOriginalLimit() {
        ItemStack ring = new ItemStack(Items.GOLD_INGOT);
        int[] oversizedRoles = new int[20];
        Arrays.fill(oversizedRoles, FairyRole.FIGHTER.legacyIndex());

        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> tag.putIntArray("fairyList", oversizedRoles));
        assertEquals(RingOfBindingItem.MAX_BOUND_FAIRIES, RingOfBindingItem.boundFairyCount(ring));

        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> {
            tag.remove("fairyList");
            tag.putIntArray("boundFairies", oversizedRoles);
        });
        assertEquals(RingOfBindingItem.MAX_BOUND_FAIRIES, RingOfBindingItem.boundFairyCount(ring));
        assertFalse(RingOfBindingItem.bindCharm(ring, FairyRole.TRAINER));

        CustomData.update(DataComponents.CUSTOM_DATA, ring, tag -> {
            tag.remove("boundFairies");
            tag.putInt("boundFairyCount", 100);
        });
        assertEquals(RingOfBindingItem.MAX_BOUND_FAIRIES, RingOfBindingItem.boundFairyCount(ring));
    }
}
