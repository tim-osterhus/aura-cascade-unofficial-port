package pixlepix.auracascade.item;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

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
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            ring,
            tag -> tag.putInt("boundFairyCount", 3)
        );

        assertEquals(List.of(FairyRole.BASIC, FairyRole.BASIC, FairyRole.BASIC), RingOfBindingItem.boundFairies(ring));
    }
}
