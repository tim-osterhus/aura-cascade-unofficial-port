package pixlepix.auracascade.block.entity;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.block.FortifiedBlockVariant;
import pixlepix.auracascade.block.LateGameVariant;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LateGameWorldLogicTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void ritualMappingsPushTerrainTowardTheirTargetDimension() {
        RandomSource random = RandomSource.create(123L);

        assertEquals(Blocks.NETHERRACK, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.STONE, random));
        assertEquals(Blocks.LAVA, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_NETHER, Blocks.WATER, random));
        assertEquals(Blocks.END_STONE, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_END, Blocks.DIRT, random));
        assertEquals(Blocks.OBSIDIAN, LateGameWorldLogic.ritualMapping(LateGameVariant.RITUAL_END, Blocks.WATER, random));
    }

    @Test
    void minerContainmentAndYieldStayLateGameGated() {
        assertTrue(LateGameWorldLogic.minerContainmentFails(0));
        assertFalse(LateGameWorldLogic.minerContainmentFails(6));
        assertEquals(0, LateGameWorldLogic.minerOreYield(20));
        assertTrue(LateGameWorldLogic.minerOreYield(50) > LateGameWorldLogic.minerOreYield(30));
        assertTrue(FortifiedBlockVariant.OBSIDIAN.resistChance() > FortifiedBlockVariant.GLASS.resistChance());
    }

    @Test
    void ritualDangerAndSpawnerPoolsReflectTheirDimensions() {
        assertTrue(LateGameWorldLogic.ritualDanger(LateGameVariant.RITUAL_NETHER).blazeBurst());
        assertTrue(LateGameWorldLogic.ritualDanger(LateGameVariant.RITUAL_END).damage() > 0.0F);
        assertTrue(
            List.of(
                net.minecraft.world.entity.EntityType.BLAZE,
                net.minecraft.world.entity.EntityType.WITHER_SKELETON,
                net.minecraft.world.entity.EntityType.MAGMA_CUBE,
                net.minecraft.world.entity.EntityType.ZOMBIFIED_PIGLIN
            ).contains(LateGameWorldLogic.chooseSpawnType(Level.NETHER, RandomSource.create(0L)))
        );
        assertTrue(
            List.of(
                net.minecraft.world.entity.EntityType.ENDERMAN,
                net.minecraft.world.entity.EntityType.ENDERMITE,
                net.minecraft.world.entity.EntityType.SHULKER
            ).contains(LateGameWorldLogic.chooseSpawnType(Level.END, RandomSource.create(1L)))
        );
    }
}
