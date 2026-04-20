package pixlepix.auracascade.block.entity;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraNodeState;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    }

    @Test
    void triggerFamiliesKeepDistinctFuelSemantics() {
        assertTrue(AuraPumpLogic.burningFuel(1_600).power() > AuraPumpLogic.torchFuel().power());
        assertTrue(AuraPumpLogic.glowstoneFuel().power() > AuraPumpLogic.torchFuel().power());
        assertTrue(AuraPumpLogic.fallFuel(6.5F).power() > AuraPumpLogic.fallFuel(2.0F).power());
        assertTrue(AuraPumpLogic.arrowFuel().power() > AuraPumpLogic.eggFuel().power());
        assertTrue(AuraPumpLogic.eggFuel().power() > AuraPumpLogic.snowballFuel().power());
        assertTrue(AuraPumpLogic.redstoneFuel(6).power() > AuraPumpLogic.redstoneFuel(2).power());
        assertTrue(AuraPumpLogic.creativeFuel().speed() > AuraPumpLogic.arrowFuel().speed());
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
}
