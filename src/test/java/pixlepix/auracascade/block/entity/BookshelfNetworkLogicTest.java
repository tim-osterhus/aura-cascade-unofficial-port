package pixlepix.auracascade.block.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class BookshelfNetworkLogicTest {
    @Test
    void powerCurveMatchesRecoveredCoordinatorFormula() {
        assertEquals(0, BookshelfNetworkLogic.powerCost(0));
        assertEquals(5, BookshelfNetworkLogic.powerCost(1));
        assertEquals(31, BookshelfNetworkLogic.powerCost(5));
        assertEquals(81, BookshelfNetworkLogic.powerCost(10));
    }
}
