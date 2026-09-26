package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.FuelValues;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.parity.AuraColor;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraPumpLogicTest {
    @Test
    void strongerFuelWinsAndAlternatorsTripleSpeed() {
        AuraPumpLogic.PumpState current = new AuraPumpLogic.PumpState(10, 5);
        AuraPumpLogic.PumpState stronger = AuraPumpLogic.addFuel(
            AuraPumpVariant.BURNING,
            current,
            new AuraPumpLogic.FuelOffer(20, 10)
        );
        assertEquals(20, stronger.power());
        assertEquals(10, stronger.speed());

        AuraPumpLogic.PumpState weaker = AuraPumpLogic.addFuel(
            AuraPumpVariant.BURNING,
            stronger,
            new AuraPumpLogic.FuelOffer(5, 5)
        );
        assertEquals(stronger, weaker);

        AuraPumpLogic.PumpState alternating = AuraPumpLogic.addFuel(
            AuraPumpVariant.BURNING_ALT,
            new AuraPumpLogic.PumpState(0, 0),
            new AuraPumpLogic.FuelOffer(20, 10)
        );
        assertEquals(20, alternating.power());
        assertEquals(30, alternating.speed());

        assertEquals(
            new AuraPumpLogic.PumpState(10, 30),
            AuraPumpLogic.addFuel(
                AuraPumpVariant.BURNING_ALT,
                new AuraPumpLogic.PumpState(10, 30),
                new AuraPumpLogic.FuelOffer(20, 10)
            )
        );
    }

    @Test
    void triggerFamiliesKeepDistinctFuelSemantics() {
        assertEquals(new AuraPumpLogic.FuelOffer(320, 300), AuraPumpLogic.burningFuel(1_600));
        assertEquals(new AuraPumpLogic.FuelOffer(180, 750), AuraPumpLogic.glowstoneFuel());
        assertEquals(new AuraPumpLogic.FuelOffer(30, 750), AuraPumpLogic.torchFuel());
        assertEquals(new AuraPumpLogic.FuelOffer(13, 500), AuraPumpLogic.fallFuel(6.5F));
        assertEquals(new AuraPumpLogic.FuelOffer(20, 1_000), AuraPumpLogic.arrowFuel());
        assertEquals(new AuraPumpLogic.FuelOffer(90, 500), AuraPumpLogic.eggFuel());
        assertEquals(new AuraPumpLogic.FuelOffer(10, 500), AuraPumpLogic.snowballFuel());
        assertEquals(new AuraPumpLogic.FuelOffer(14, 1_500), AuraPumpLogic.redstoneFuel(1));
        assertEquals(new AuraPumpLogic.FuelOffer(19, 1_500), AuraPumpLogic.redstoneFuel(2));
        assertEquals(new AuraPumpLogic.FuelOffer(2, 10_000_000), AuraPumpLogic.creativeFuel());
    }

    @Test
    void pumpTransferRespectsCompositionMassAndAscent() {
        AuraNodeState source = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 120);
        source.storage().set(AuraColor.YELLOW, 120);
        source.storage().set(AuraColor.BLACK, 120);

        var requested = AuraPumpLogic.planTransfer(
            source,
            new BlockPos(0, 0, 0),
            new BlockPos(0, 3, 0),
            AuraEnvironment.CLEAR_DAY,
            new AuraPumpLogic.PumpState(20, 18),
            AuraPumpVariant.BURNING,
            0L
        );

        assertTrue(requested.get(AuraColor.YELLOW) > requested.get(AuraColor.WHITE));
        assertEquals(0, requested.get(AuraColor.BLACK));
    }

    @Test
    void targetConsumesOneSecondEvenWhenEveryColorRoundsToZero() {
        AuraNodeState source = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 1);
        source.storage().set(AuraColor.YELLOW, 1);
        AuraPumpLogic.PumpState fuel = new AuraPumpLogic.PumpState(1, 3);

        var requested = AuraPumpLogic.planTransfer(
            source,
            BlockPos.ZERO,
            new BlockPos(0, 2, 0),
            AuraEnvironment.CLEAR_DAY,
            fuel,
            AuraPumpVariant.BURNING,
            2L
        );

        assertTrue(requested.isEmpty());
        assertEquals(new AuraPumpLogic.PumpState(0, 3), AuraPumpLogic.spendForTarget(fuel));
        assertEquals(1, fuel.power());
    }

    @Test
    void lastEligibleSecondCanStillMoveAura() {
        AuraNodeState source = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 20);
        AuraPumpLogic.PumpState beforeSpend = new AuraPumpLogic.PumpState(1, 300);
        AuraPumpLogic.PumpState afterSpend = AuraPumpLogic.spendForTarget(beforeSpend);

        assertEquals(0, afterSpend.power());
        assertEquals(20, AuraPumpLogic.planTransfer(
            source, BlockPos.ZERO, new BlockPos(0, 1, 0), AuraEnvironment.CLEAR_DAY,
            beforeSpend, AuraPumpVariant.BURNING, 2L
        ).get(AuraColor.WHITE));
        assertTrue(AuraPumpLogic.planTransfer(
            source, BlockPos.ZERO, new BlockPos(0, 1, 0), AuraEnvironment.CLEAR_DAY,
            afterSpend, AuraPumpVariant.BURNING, 2L
        ).isEmpty());
    }

    @Test
    void burningPumpUsesFurnaceFuelMapBeyondOldFixedSubset() {
        TestMinecraftBootstrap.ensureBootstrapped();
        FuelValues fuelValues = FuelValues.vanillaBurnTimes(
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY), FeatureFlags.DEFAULT_FLAGS);

        assertEquals(1_600, AuraPumpBlockEntity.burningFuelValue(new ItemStack(Items.COAL), fuelValues));
        assertEquals(100, AuraPumpBlockEntity.burningFuelValue(new ItemStack(Items.STICK), fuelValues));
        assertEquals(20_000, AuraPumpBlockEntity.burningFuelValue(new ItemStack(Items.LAVA_BUCKET), fuelValues));
        assertEquals(0, AuraPumpBlockEntity.burningFuelValue(new ItemStack(Items.DIAMOND), fuelValues));
    }

    @Test
    void alternatorTruncatesBeforeComposition() {
        AuraNodeState source = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 10);
        var requested = AuraPumpLogic.planTransfer(
            source,
            BlockPos.ZERO,
            new BlockPos(0, 2, 0),
            AuraEnvironment.CLEAR_DAY,
            new AuraPumpLogic.PumpState(1, 3),
            AuraPumpVariant.BURNING_ALT,
            1_667L
        );
        assertTrue(requested.isEmpty());
    }

    @Test
    void compositionTruncatesBeforeYellowAscentBoost() {
        AuraNodeState source = new AuraNodeState();
        source.storage().set(AuraColor.WHITE, 10);
        source.storage().set(AuraColor.YELLOW, 10);
        var requested = AuraPumpLogic.planTransfer(
            source,
            BlockPos.ZERO,
            new BlockPos(0, 1, 0),
            AuraEnvironment.CLEAR_DAY,
            new AuraPumpLogic.PumpState(1, 3),
            AuraPumpVariant.BURNING,
            2L
        );
        assertEquals(1, requested.get(AuraColor.WHITE));
        assertEquals(2, requested.get(AuraColor.YELLOW));
    }

    @Test
    void pumpHudStateRoundTripsThroughPumpTags() {
        CompoundTag tag = new CompoundTag();
        AuraPumpLogic.PumpState state = new AuraPumpLogic.PumpState(179, 750);
        AuraPumpBlockEntity.writePumpState(tag, state, true);

        assertEquals(state, AuraPumpBlockEntity.readPumpState(tag));
        assertTrue(AuraPumpBlockEntity.readPumpInhibited(tag));
        assertEquals(new AuraPumpLogic.PumpState(0, 0), AuraPumpBlockEntity.readPumpState(new CompoundTag()));
        assertFalse(AuraPumpBlockEntity.readPumpInhibited(new CompoundTag()));
    }
}
