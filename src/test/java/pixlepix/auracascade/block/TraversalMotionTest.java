package pixlepix.auracascade.block;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class TraversalMotionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void roadBoostRequiresMoreThanQuarterBlockSpeedAndUsesAllAxes() {
        assertEquals(Vec3.ZERO, TravelersBricksBlock.movementBoost(Vec3.ZERO));
        assertEquals(Vec3.ZERO, TravelersBricksBlock.movementBoost(new Vec3(0.25, 0, 0)));
        Vec3 boost = TravelersBricksBlock.movementBoost(new Vec3(0.3, 0.4, 0));
        assertEquals(3.0D, boost.x, 1.0e-8);
        assertEquals(4.0D, boost.y, 1.0e-8);
        assertEquals(5.0D, boost.length(), 1.0e-8);
    }

    @Test
    void reboundSetsTenVerticalWithoutErasingHorizontalMomentum() {
        assertEquals(new Vec3(0.3, 10, -0.8), ReboundingEnigmaBlock.launchVelocity(new Vec3(0.3, -2, -0.8)));
    }
}
