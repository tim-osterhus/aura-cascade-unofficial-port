package pixlepix.auracascade.block.menu;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BookshelfCoordinatorMenuAccessTest {
    @Test
    void serverAccessRequiresLoadedCoordinatorAndViewerWithinEightBlocks() {
        BlockPos pos = new BlockPos(0, 64, 0);

        assertTrue(BookshelfCoordinatorMenu.hasServerAccess(pos, 0.5D, 64.5D, 0.5D, true, true));
        assertFalse(BookshelfCoordinatorMenu.hasServerAccess(pos, 8.5D, 64.5D, 0.5D, true, true));
        assertFalse(BookshelfCoordinatorMenu.hasServerAccess(pos, 0.5D, 64.5D, 0.5D, false, true));
        assertFalse(BookshelfCoordinatorMenu.hasServerAccess(pos, 0.5D, 64.5D, 0.5D, true, false));
    }
}
