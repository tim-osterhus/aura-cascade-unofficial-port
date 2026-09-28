package pixlepix.auracascade.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UtilityGearLogicTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void protectionProfilesMatchTheirExpectedDamageFamilies() {
        assertTrue(ProtectionAmuletProfile.RED.appliesTo(ProtectionAmuletProfile.DamageFamily.FIRE));
        assertTrue(ProtectionAmuletProfile.ORANGE.appliesTo(ProtectionAmuletProfile.DamageFamily.EXPLOSION));
        assertTrue(ProtectionAmuletProfile.YELLOW.appliesTo(ProtectionAmuletProfile.DamageFamily.PROJECTILE));
        assertTrue(ProtectionAmuletProfile.GREEN.appliesTo(ProtectionAmuletProfile.DamageFamily.FALL));
        assertTrue(ProtectionAmuletProfile.BLUE.appliesTo(ProtectionAmuletProfile.DamageFamily.DROWN));
        assertTrue(ProtectionAmuletProfile.VIOLET.appliesTo(ProtectionAmuletProfile.DamageFamily.WITHER));
        assertEquals(1.0F, ProtectionAmuletProfile.RED.healFraction());
        assertEquals(1.0F, ProtectionAmuletProfile.ORANGE.healFraction());
        assertEquals(1.0F, ProtectionAmuletProfile.GREEN.healFraction());
        assertEquals(1.0F, ProtectionAmuletProfile.BLUE.healFraction());
        assertEquals(0.0F, ProtectionAmuletProfile.YELLOW.healFraction());
        assertEquals(0.5F, ProtectionAmuletProfile.YELLOW.incomingDamageMultiplier());
        assertTrue(!ProtectionAmuletProfile.YELLOW.blocksDamage());
    }

    @Test
    void everyAuraCrystalCarriesTheLegacyThousandAuraCharge() {
        for (AuraColor color : AuraColor.values()) {
            assertEquals(1_000, AuraItems.auraCrystalCharge(new ItemStack(AuraItems.crystal(color))), color.toString());
        }
        assertEquals(0, AuraItems.auraCrystalCharge(new ItemStack(Items.AMETHYST_SHARD)));
    }

    @Test
    void ringOfShatteredStoneMatchesLegacyTerrainAllowlistWithoutWearerDamageImmunity() {
        assertFalse(RingOfShatteredStoneRuntime.blocksExplosionDamage(ProtectionAmuletProfile.DamageFamily.EXPLOSION, true));
        assertFalse(RingOfShatteredStoneRuntime.blocksExplosionDamage(ProtectionAmuletProfile.DamageFamily.FIRE, true));
        assertFalse(RingOfShatteredStoneRuntime.blocksExplosionDamage(ProtectionAmuletProfile.DamageFamily.EXPLOSION, false));
        assertTrue(RingOfShatteredStoneItem.explosionSearchBounds(Vec3.ZERO).contains(new Vec3(2.9D, 2.9D, 2.9D)));
        assertFalse(RingOfShatteredStoneItem.explosionSearchBounds(Vec3.ZERO).contains(new Vec3(3.1D, 0.0D, 0.0D)));

        List<Block> legacyTerrain = List.of(
            Blocks.GRASS_BLOCK,
            Blocks.SANDSTONE,
            Blocks.CHISELED_SANDSTONE,
            Blocks.CUT_SANDSTONE,
            Blocks.STONE,
            Blocks.GRANITE,
            Blocks.POLISHED_GRANITE,
            Blocks.DIORITE,
            Blocks.POLISHED_DIORITE,
            Blocks.ANDESITE,
            Blocks.POLISHED_ANDESITE,
            Blocks.SAND,
            Blocks.RED_SAND,
            Blocks.DIRT,
            Blocks.COARSE_DIRT,
            Blocks.PODZOL,
            Blocks.COBBLESTONE,
            Blocks.GRAVEL
        );
        for (Block block : legacyTerrain) {
            BlockState state = block.defaultBlockState();
            assertTrue(RingOfShatteredStoneItem.isLegacyExplosionTerrain(state), block.toString());
            assertFalse(RingOfShatteredStoneItem.shouldPreserveExplosionBlock(state), block.toString());
        }

        List<Block> preservedBlocks = List.of(
            Blocks.SMOOTH_SANDSTONE,
            Blocks.RED_SANDSTONE,
            Blocks.ROOTED_DIRT,
            Blocks.MOSSY_COBBLESTONE,
            Blocks.DIAMOND_ORE,
            Blocks.OAK_PLANKS
        );
        for (Block block : preservedBlocks) {
            BlockState state = block.defaultBlockState();
            assertFalse(RingOfShatteredStoneItem.isLegacyExplosionTerrain(state), block.toString());
            assertTrue(RingOfShatteredStoneItem.shouldPreserveExplosionBlock(state), block.toString());
        }
        assertFalse(RingOfShatteredStoneItem.shouldPreserveExplosionBlock(Blocks.AIR.defaultBlockState()));
    }

    @Test
    void shatteredStoneExplosionSeamProtectsTheWholeAffectedListForARingInTheExplosionBounds() {
        Vec3 explosionCenter = new Vec3(0.5D, 0.5D, 0.5D);
        Vec3 wearerInSearchBoxCorner = new Vec3(3.4D, 3.4D, 3.4D);
        Vec3 wearerOutsideSearchBox = new Vec3(7.5D, 0.5D, 0.5D);
        BlockPos nearbyDirt = new BlockPos(0, 0, 0);
        BlockPos distantSandstoneVariant = new BlockPos(40, 20, -20);
        BlockPos distantGravel = new BlockPos(-35, 12, 30);
        BlockPos distantDiamondOre = new BlockPos(60, 30, 60);
        BlockPos distantBuildingBlock = new BlockPos(-70, 40, -80);
        BlockPos air = new BlockPos(2, 1, 2);
        List<BlockPos> affectedBlocks = new ArrayList<>(
            List.of(nearbyDirt, distantSandstoneVariant, distantGravel, distantDiamondOre, distantBuildingBlock, air)
        );
        Map<BlockPos, BlockState> states = Map.of(
            nearbyDirt, Blocks.DIRT.defaultBlockState(),
            distantSandstoneVariant, Blocks.CUT_SANDSTONE.defaultBlockState(),
            distantGravel, Blocks.GRAVEL.defaultBlockState(),
            distantDiamondOre, Blocks.DIAMOND_ORE.defaultBlockState(),
            distantBuildingBlock, Blocks.OAK_PLANKS.defaultBlockState(),
            air, Blocks.AIR.defaultBlockState()
        );

        TestShatteredStoneExplosionAccess ringWearerAccess = new TestShatteredStoneExplosionAccess(
            List.of(wearerInSearchBoxCorner, wearerOutsideSearchBox),
            states
        );
        List<BlockPos> filtered = ShatteredStoneExplosionSeam.filterBlocks(ringWearerAccess, explosionCenter, affectedBlocks);
        assertEquals(List.of(nearbyDirt, distantSandstoneVariant, distantGravel, air), filtered);
        assertEquals(RingOfShatteredStoneItem.explosionSearchBounds(explosionCenter), ringWearerAccess.lastSearchBounds());
        assertEquals(affectedBlocks.size(), ringWearerAccess.blockStateLookups());

        TestShatteredStoneExplosionAccess noRingWearerAccess = new TestShatteredStoneExplosionAccess(
            List.of(wearerOutsideSearchBox),
            states
        );
        List<BlockPos> unchanged = ShatteredStoneExplosionSeam.filterBlocks(noRingWearerAccess, explosionCenter, affectedBlocks);
        assertSame(affectedBlocks, unchanged);
        assertFalse(noRingWearerAccess.lastSearchBounds().contains(wearerOutsideSearchBox));
        assertEquals(0, noRingWearerAccess.blockStateLookups());
    }

    @Test
    void forbiddenFruitMatchesLegacyAppleAndSeededPositivePotionRules() {
        ForbiddenFruitEffects.EffectDescriptor apple = ForbiddenFruitEffects.descriptorForItemId("minecraft:apple");
        ForbiddenFruitEffects.EffectDescriptor carrotFirst = ForbiddenFruitEffects.descriptorForItemId("minecraft:carrot");
        ForbiddenFruitEffects.EffectDescriptor carrotSecond = ForbiddenFruitEffects.descriptorForItemId("minecraft:carrot");

        assertEquals("regeneration", apple.effectId());
        assertEquals(7_200, apple.duration());
        assertEquals(1, apple.amplifier());
        assertEquals(carrotFirst, carrotSecond);
        assertNotEquals(apple, carrotFirst);
        assertTrue(List.of(
            "speed", "haste", "strength", "instant_health", "jump_boost", "regeneration", "resistance",
            "fire_resistance", "water_breathing", "invisibility", "night_vision", "health_boost", "absorption", "saturation"
        ).contains(carrotFirst.effectId()));
        assertTrue(carrotFirst.duration() >= 0);
        assertTrue(carrotFirst.amplifier() >= 0 && carrotFirst.amplifier() <= 5);
    }

    @Test
    void barbarianComboMathResetsAfterTimeoutAndCapsAtHundredHits() {
        assertEquals(0, SwordOfBarbarianItem.nextComboCount(0L, 0L, 0));
        assertEquals(0, SwordOfBarbarianItem.nextComboCount(0L, 4L, 5));
        assertEquals(2, SwordOfBarbarianItem.nextComboCount(5L, 80L, 1));
        assertEquals(0, SwordOfBarbarianItem.nextComboCount(0L, 100L, 99));
        assertEquals(100, SwordOfBarbarianItem.nextComboCount(10L, 20L, 100));
        assertEquals(1.0D, SwordOfBarbarianItem.comboMultiplier(0), 0.0001D);
        assertEquals(1.05D, SwordOfBarbarianItem.comboMultiplier(1), 0.0001D);
        assertEquals(Math.pow(1.05D, 100), SwordOfBarbarianItem.comboMultiplier(100), 0.0001D);
    }

    @Test
    void mirrorRedirectVelocityPreservesTheLegacyUnnormalizedTargetVector() {
        Vec3 redirected = AuraItems.mirrorRedirectVelocity(Vec3.ZERO, new Vec3(15.0D, 30.0D, -15.0D));
        assertEquals(1.0D, redirected.x, 0.000001D);
        assertEquals(2.0D, redirected.y, 0.000001D);
        assertEquals(-1.0D, redirected.z, 0.000001D);
    }

    @Test
    void transmutingSwordMappingsStayBidirectionalForLegacyPairs() {
        assertEquals(EntityType.MOOSHROOM, TransmutingSwordItem.mappedType(EntityType.COW));
        assertEquals(EntityType.COW, TransmutingSwordItem.mappedType(EntityType.MOOSHROOM));
        assertEquals(EntityType.ENDERMAN, TransmutingSwordItem.mappedType(EntityType.CREEPER));
        assertEquals(EntityType.CREEPER, TransmutingSwordItem.mappedType(EntityType.ENDERMAN));
    }

    private static final class TestShatteredStoneExplosionAccess implements ShatteredStoneExplosionAccess {
        private final List<Vec3> wearerPositions;
        private final Map<BlockPos, BlockState> states;
        private int blockStateLookups;
        private AABB lastSearchBounds;

        private TestShatteredStoneExplosionAccess(List<Vec3> wearerPositions, Map<BlockPos, BlockState> states) {
            this.wearerPositions = wearerPositions;
            this.states = states;
        }

        @Override
        public BlockState getBlockState(BlockPos pos) {
            blockStateLookups++;
            return states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public List<Vec3> protectedWearerPositions(AABB searchBounds) {
            lastSearchBounds = searchBounds;
            return wearerPositions.stream().filter(searchBounds::contains).toList();
        }

        private int blockStateLookups() {
            return blockStateLookups;
        }

        private AABB lastSearchBounds() {
            return lastSearchBounds;
        }
    }
}
