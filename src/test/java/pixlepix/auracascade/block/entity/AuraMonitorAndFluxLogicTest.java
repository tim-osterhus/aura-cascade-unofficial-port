package pixlepix.auracascade.block.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.aura.AuraInspectionState;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.compat.AuraFluxBridgeRegistry;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AuraMonitorAndFluxLogicTest {
    @AfterEach
    void resetBridge() {
        AuraFluxBridgeRegistry.reset();
    }

    @Test
    void nodeAndPumpComparatorReadoutsRemainIndependentOfMonitorStatus() {
        AuraStorage storage = new AuraStorage();
        storage.set(AuraColor.WHITE, 500);
        AuraInspectionState node = new AuraInspectionState(storage, 2, 0, true);

        int nodeSignal = AuraMonitorLogic.nodeSignal(node, 1_000);
        int pumpSignal = AuraMonitorLogic.pumpSignal(node, 1_000, true);

        assertEquals(8, nodeSignal);
        assertEquals(15, pumpSignal);
    }

    @Test
    void fluxBridgeExportIsClampedAndReplaceable() {
        AuraFluxBridgeRegistry.install((level, pos, availablePower) -> availablePower + 50);
        assertEquals(200, AuraFluxBridgeRegistry.export(null, null, 200));

        AuraFluxBridgeRegistry.install((level, pos, availablePower) -> availablePower / 2);
        assertEquals(75, AuraFluxBridgeRegistry.export(null, null, 150));
    }
}
