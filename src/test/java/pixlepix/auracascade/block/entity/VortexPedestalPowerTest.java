package pixlepix.auracascade.block.entity;

import org.junit.jupiter.api.Test;
import net.minecraft.nbt.CompoundTag;
import pixlepix.auracascade.aura.AuraEnvironment;
import pixlepix.auracascade.aura.AuraStorage;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VortexPedestalPowerTest {
    @Test
    void onlyMatchingPowerIsReceivedButAllGeneratedPowerLeavesGenericCounter() {
        AuraStorage moved = new AuraStorage();
        moved.set(AuraColor.RED, 10);
        moved.set(AuraColor.BLUE, 5);
        moved.set(AuraColor.BLACK, 50);

        assertEquals(new VortexPedestalBlockEntity.PowerDelivery(30, 20),
            VortexPedestalBlockEntity.powerDelivery(moved, 2, AuraEnvironment.CLEAR_DAY, AuraColor.RED));
        assertEquals(new VortexPedestalBlockEntity.PowerDelivery(30, 30),
            VortexPedestalBlockEntity.powerDelivery(moved, 2, AuraEnvironment.CLEAR_DAY, AuraColor.WHITE));
        assertEquals(new VortexPedestalBlockEntity.PowerDelivery(30, 0),
            VortexPedestalBlockEntity.powerDelivery(moved, 2, AuraEnvironment.CLEAR_DAY, AuraColor.VIOLET));
        assertEquals(new VortexPedestalBlockEntity.PowerDelivery(0, 0),
            VortexPedestalBlockEntity.powerDelivery(moved, -1, AuraEnvironment.CLEAR_DAY, AuraColor.RED));
    }

    @Test
    void greenPowerUsesEnvironmentMassLikeTheTransferKernel() {
        AuraStorage moved = AuraStorage.of(AuraColor.GREEN, 10);
        assertEquals(new VortexPedestalBlockEntity.PowerDelivery(20, 20),
            VortexPedestalBlockEntity.powerDelivery(moved, 1, AuraEnvironment.CLEAR_DAY, AuraColor.GREEN));
        assertEquals(new VortexPedestalBlockEntity.PowerDelivery(5, 5),
            VortexPedestalBlockEntity.powerDelivery(moved, 1, AuraEnvironment.CLEAR_NIGHT, AuraColor.GREEN));
    }

    @Test
    void receiptRoundTripsWithColorAndClampsCorruptOverfill() {
        CompoundTag tag = new CompoundTag();
        var receipt = new VortexPedestalBlockEntity.Receipt(12_000, 20_000, AuraColor.WHITE);
        VortexPedestalBlockEntity.writeReceipt(tag, receipt);
        assertEquals(receipt, VortexPedestalBlockEntity.readReceipt(tag));

        tag.putInt("power_received", 90_000);
        assertEquals(new VortexPedestalBlockEntity.Receipt(20_000, 20_000, AuraColor.WHITE),
            VortexPedestalBlockEntity.readReceipt(tag));
    }
}
