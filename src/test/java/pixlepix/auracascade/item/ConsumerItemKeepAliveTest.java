package pixlepix.auracascade.item;

import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.*;

public final class ConsumerItemKeepAliveTest {
    @BeforeAll
    static void bootstrap() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void exactServerCadenceDoesNotDependOnPowerOrWork() {
        assertTrue(ConsumerItemKeepAlive.shouldRefresh(false, 0));
        assertTrue(ConsumerItemKeepAlive.shouldRefresh(false, 500));
        assertTrue(ConsumerItemKeepAlive.shouldRefresh(false, 1_000));
        assertFalse(ConsumerItemKeepAlive.shouldRefresh(false, 499));
        assertFalse(ConsumerItemKeepAlive.shouldRefresh(false, 501));
        assertFalse(ConsumerItemKeepAlive.shouldRefresh(true, 500));
    }

    @Test
    void queryUsesTheExactLegacySixBlockWideBox() {
        AABB bounds = ConsumerItemKeepAlive.bounds(new BlockPos(12, 40, -8));
        assertEquals(new AABB(9, 37, -11, 15, 43, -5), bounds);
        assertTrue(bounds.contains(14.99, 42.99, -5.01));
        assertFalse(bounds.contains(15.01, 40, -8));
        assertFalse(bounds.contains(12, 43.01, -8));
        assertFalse(bounds.contains(12, 40, -4.99));
    }

    @Test
    void ordinaryAndRedHoleLifetimesAreUnchangedUnlessProtected() {
        assertEquals(6_000, ConsumerItemKeepAlive.effectiveLifetime(false, 6_000));
        int redHoleLifetime = PortableRedHoleItem.lifetimeTicks(true, 6_000);
        assertEquals(30_000, ConsumerItemKeepAlive.effectiveLifetime(false, redHoleLifetime));
        assertEquals(Integer.MAX_VALUE, ConsumerItemKeepAlive.effectiveLifetime(true, 6_000));
        assertEquals(Integer.MAX_VALUE, ConsumerItemKeepAlive.effectiveLifetime(true, redHoleLifetime));
    }

    @Test
    void bothConsumerTicksRefreshBeforeWorkAndPowerBranches() throws Exception {
        for (String file : new String[] {"AuraConsumerBlockEntity.java", "LateGameBlockEntity.java"}) {
            String source = Files.readString(Path.of("src/main/java/pixlepix/auracascade/block/entity", file));
            assertTrue(source.contains("private void serverTick(Level level, BlockPos pos) {\n"
                + "        pixlepix.auracascade.item.ConsumerItemKeepAlive.tick(level, pos);")
                || source.contains("private void serverTick(Level level, BlockPos pos) {\r\n"
                + "        pixlepix.auracascade.item.ConsumerItemKeepAlive.tick(level, pos);"), file);
        }
        String lifetimeHook = Files.readString(Path.of("src/main/java/pixlepix/auracascade/mixin/PortableRedHoleLifetimeMixin.java"));
        assertTrue(lifetimeHook.contains("ConsumerItemKeepAlive.effectiveLifetime(item,"));
    }

    // This unit bootstrap does not load Aura mixins. Actual ItemEntity persistence,
    // merging and old-item eligibility are mandatory ConsumerLifetimeProbe runtime gates.
}
