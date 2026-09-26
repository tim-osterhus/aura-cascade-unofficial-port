package pixlepix.auracascade.fairy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FairySystemTest {
    @Test
    void extinguisherTargetsLavaStatesButDoesNotClearFireBlocks() {
        assertTrue(FairySystem.shouldRemoveExtinguisherBlock(Blocks.LAVA.defaultBlockState()));
        assertTrue(FairySystem.shouldRemoveExtinguisherBlock(
            Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, 4)
        ));
        assertFalse(FairySystem.shouldRemoveExtinguisherBlock(Blocks.FIRE.defaultBlockState()));
        assertFalse(FairySystem.shouldRemoveExtinguisherBlock(Blocks.SOUL_FIRE.defaultBlockState()));
        assertFalse(FairySystem.shouldRemoveExtinguisherBlock(Blocks.WATER.defaultBlockState()));
    }
}
