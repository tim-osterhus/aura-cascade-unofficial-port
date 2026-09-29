package pixlepix.auracascade.fairy;

import java.util.UUID;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FairySystemTest {
    @Test
    void orbitWaitsInOwnersChunkWhenDestinationCannotTickEntities() {
        Vec3 owner = new Vec3(0.5D, 64.0D, 0.5D);
        Vec3 netherOrbit = new Vec3(-0.9D, 65.4D, -0.4D);

        assertEquals(owner.add(0.0D, 1.4D, 0.0D), FairySystem.entityTickingOrbitPosition(netherOrbit, owner, false));
        assertEquals(netherOrbit, FairySystem.entityTickingOrbitPosition(netherOrbit, owner, true));
    }

    @Test
    void loadedDuplicateCannotKeepActingForACanonicalSlot() {
        UUID canonical = UUID.randomUUID();
        UUID duplicate = UUID.randomUUID();

        assertFalse(FairySystem.isSuperseded(null, duplicate));
        assertFalse(FairySystem.isSuperseded(canonical, canonical));
        assertTrue(FairySystem.isSuperseded(canonical, duplicate));
    }

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
