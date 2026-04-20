package pixlepix.auracascade.aura;

import org.junit.jupiter.api.Test;
import pixlepix.auracascade.parity.AuraColor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraStorageTest {
    @Test
    void preservesPerColorQuantitiesAndSharedMath() {
        AuraStorage storage = new AuraStorage();
        storage.add(AuraColor.WHITE, 100);
        storage.add(AuraColor.BLACK, 40);
        storage.add(AuraColor.ORANGE, 10);

        assertEquals(150, storage.total());
        assertEquals(100, storage.get(AuraColor.WHITE));
        assertEquals(40, storage.get(AuraColor.BLACK));
        assertEquals(10, storage.get(AuraColor.ORANGE));
        assertEquals(100.0D / 150.0D, storage.composition(AuraColor.WHITE), 1.0E-9D);

        AuraStorage scaled = storage.scaled(0.5D);
        assertEquals(50, scaled.get(AuraColor.WHITE));
        assertEquals(20, scaled.get(AuraColor.BLACK));
        assertEquals(5, scaled.get(AuraColor.ORANGE));

        AuraStorage cap = new AuraStorage();
        cap.set(AuraColor.WHITE, 70);
        cap.set(AuraColor.BLACK, 80);
        cap.set(AuraColor.ORANGE, 1);
        AuraStorage minimum = storage.min(cap);
        assertEquals(70, minimum.get(AuraColor.WHITE));
        assertEquals(40, minimum.get(AuraColor.BLACK));
        assertEquals(1, minimum.get(AuraColor.ORANGE));

        AuraStorage request = new AuraStorage();
        request.set(AuraColor.WHITE, 40);
        request.set(AuraColor.BLACK, 20);
        assertTrue(storage.covers(request));
        storage.subtractAll(request);
        assertEquals(60, storage.get(AuraColor.WHITE));
        assertEquals(20, storage.get(AuraColor.BLACK));
        assertFalse(storage.isEmpty());
    }
}
