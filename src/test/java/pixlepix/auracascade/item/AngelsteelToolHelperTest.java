package pixlepix.auracascade.item;

import java.util.Arrays;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AngelsteelToolHelperTest {
    @Test
    void higherDegreesAllocateMoreBuffPointsAndMaterialStats() {
        assertEquals(0, Arrays.stream(AngelsteelToolHelper.randomBuffSet(0, RandomSource.create(1L))).sum());
        assertEquals(22, Arrays.stream(AngelsteelToolHelper.randomBuffSet(11, RandomSource.create(1L))).sum());
        assertTrue(AngelsteelToolHelper.material(11).speed() > AngelsteelToolHelper.material(0).speed());
        assertTrue(AngelsteelToolHelper.material(11).attackDamageBonus() > AngelsteelToolHelper.material(0).attackDamageBonus());
    }

    @Test
    void ensureBuffsBackfillsMissingCustomDataOncePerStack() {
        TestMinecraftBootstrap.ensureBootstrapped();

        ItemStack stack = new ItemStack(Items.NETHERITE_PICKAXE);
        AngelsteelToolHelper.ensureBuffs(stack, 5, RandomSource.create(42L));

        int[] buffs = AngelsteelToolHelper.getBuffs(stack);
        assertEquals(4, buffs.length);
        assertEquals(10, Arrays.stream(buffs).sum());
    }
}
