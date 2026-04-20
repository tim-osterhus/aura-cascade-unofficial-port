package pixlepix.auracascade.item;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        assertTrue(!ProtectionAmuletProfile.YELLOW.blocksDamage());
    }

    @Test
    void forbiddenFruitMappingIsDeterministicAndApplesStayHardcoded() {
        ForbiddenFruitEffects.EffectDescriptor apple = ForbiddenFruitEffects.descriptorForItemId("minecraft:apple");
        ForbiddenFruitEffects.EffectDescriptor carrotFirst = ForbiddenFruitEffects.descriptorForItemId("minecraft:carrot");
        ForbiddenFruitEffects.EffectDescriptor carrotSecond = ForbiddenFruitEffects.descriptorForItemId("minecraft:carrot");

        assertEquals("regeneration", apple.effectId());
        assertEquals(carrotFirst, carrotSecond);
        assertNotEquals(apple, carrotFirst);
    }

    @Test
    void barbarianComboMathResetsAfterTimeoutAndCapsAtHundredHits() {
        assertEquals(1, SwordOfBarbarianItem.nextComboCount(0L, 0L, 0));
        assertEquals(2, SwordOfBarbarianItem.nextComboCount(5L, 80L, 1));
        assertEquals(1, SwordOfBarbarianItem.nextComboCount(0L, 101L, 99));
        assertEquals(100, SwordOfBarbarianItem.nextComboCount(10L, 20L, 100));
        assertEquals(1.0D, SwordOfBarbarianItem.comboMultiplier(1), 0.0001D);
        assertEquals(7.93D, SwordOfBarbarianItem.comboMultiplier(100), 0.0001D);
    }

    @Test
    void prismaticWandStateCyclesModesAndRoundTripsClipboard() {
        ItemStack wand = new ItemStack(Items.STICK);
        assertEquals(PrismaticWandState.Mode.SELECTION, PrismaticWandState.mode(wand));
        assertEquals("Selection", PrismaticWandState.Mode.SELECTION.displayName());
        assertEquals("Copy", PrismaticWandState.Mode.COPY.displayName());
        assertEquals("Paste", PrismaticWandState.Mode.PASTE.displayName());
        assertEquals(PrismaticWandState.Mode.COPY, PrismaticWandState.cycleMode(wand));
        assertEquals(PrismaticWandState.Mode.PASTE, PrismaticWandState.cycleMode(wand));
        assertEquals(PrismaticWandState.Mode.SELECTION, PrismaticWandState.cycleMode(wand));

        assertTrue(PrismaticWandState.setSelectionPoint(wand, new BlockPos(1, 2, 3)));
        assertTrue(!PrismaticWandState.setSelectionPoint(wand, new BlockPos(4, 5, 6)));
        assertEquals(new BlockPos(1, 2, 3), PrismaticWandState.selection(wand).first());
        assertEquals(new BlockPos(4, 5, 6), PrismaticWandState.selection(wand).second());

        List<PrismaticWandState.ClipboardBlock> clipboard = List.of(new PrismaticWandState.ClipboardBlock(0, 1, 2, 17, "minecraft:stone"));
        PrismaticWandState.storeClipboard(wand, clipboard);
        assertEquals(clipboard, PrismaticWandState.clipboard(wand));
    }

    @Test
    void prismaticWandClipboardRulesSkipBlockEntitiesAndFluids() {
        assertTrue(PrismaticWandItem.supportsClipboardCopy(Blocks.STONE.defaultBlockState()));
        assertTrue(!PrismaticWandItem.supportsClipboardCopy(Blocks.CHEST.defaultBlockState()));
        assertTrue(!PrismaticWandItem.supportsClipboardCopy(Blocks.WATER.defaultBlockState()));
    }

    @Test
    void transmutingSwordMappingsStayBidirectionalForLegacyPairs() {
        assertEquals(EntityType.MOOSHROOM, TransmutingSwordItem.mappedType(EntityType.COW));
        assertEquals(EntityType.COW, TransmutingSwordItem.mappedType(EntityType.MOOSHROOM));
        assertEquals(EntityType.ENDERMAN, TransmutingSwordItem.mappedType(EntityType.CREEPER));
        assertEquals(EntityType.CREEPER, TransmutingSwordItem.mappedType(EntityType.ENDERMAN));
    }
}
