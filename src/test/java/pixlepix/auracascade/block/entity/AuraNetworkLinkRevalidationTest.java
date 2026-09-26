package pixlepix.auracascade.block.entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.aura.AuraKernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The source guard verifies the runtime wiring; geometry is exercised without a mocked Level.
final class AuraNetworkLinkRevalidationTest {
    private static final Path NETWORK = Path.of(
        "src/main/java/pixlepix/auracascade/block/entity/AuraNetworkBlockEntity.java");

    @Test
    void insertedNodeAndOccluderReplaceThePreviouslyVisibleTarget() {
        BlockPos origin = BlockPos.ZERO;
        BlockPos near = new BlockPos(0, 0, 2);
        BlockPos far = new BlockPos(0, 0, 5);
        assertEquals(Set.of(far), AuraKernel.scanStraightLineLinks(origin, Set.of(far), Set.of(),
            AuraKernel.DEFAULT_LINK_RANGE));
        assertEquals(Set.of(near), AuraKernel.scanStraightLineLinks(origin, Set.of(near, far), Set.of(),
            AuraKernel.DEFAULT_LINK_RANGE));
        assertTrue(AuraKernel.scanStraightLineLinks(origin, Set.of(near, far),
            Set.of(new BlockPos(0, 0, 1)), AuraKernel.DEFAULT_LINK_RANGE).isEmpty());
    }

    @Test
    void everyEnumerationRescansAndUnchangedLinksDoNotDirtyState() throws IOException {
        String source = Files.readString(NETWORK);
        int enumeration = source.indexOf("protected final Map<BlockPos, AuraNetworkBlockEntity> linkedNetworks(Level level)");
        int scan = source.indexOf("refreshLinks(level, worldPosition);", enumeration);
        int loop = source.indexOf("for (BlockPos linkedPos : nodeState.linkedNodes())", enumeration);
        assertTrue(enumeration >= 0 && scan > enumeration && scan < loop);

        int refresh = source.indexOf("protected final void refreshLinks(Level level, BlockPos pos)");
        int unchanged = source.indexOf("nodeState.hasScannedLinks() && nodeState.linkedNodes().equals(links)", refresh);
        int replace = source.indexOf("nodeState.replaceLinkedNodes(links);", refresh);
        int dirty = source.indexOf("setChanged();", replace);
        assertTrue(refresh >= 0 && unchanged > refresh && unchanged < replace && replace < dirty);

        int bursts = source.indexOf("private void applyOrangeBursts(Level level)");
        int burstScan = source.indexOf("refreshLinks(level, worldPosition);", bursts);
        int burstLoop = source.indexOf("for (BlockPos targetPos : nodeState.linkedNodes())", bursts);
        assertTrue(bursts >= 0 && burstScan > bursts && burstScan < burstLoop);
    }
}
