package pixlepix.auracascade.compat;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class AuraFluxBridgeRegistry {
    private static final AuraFluxBridge DEFAULT_BRIDGE = new AuraFluxEnergyBridge();
    private static AuraFluxBridge bridge = DEFAULT_BRIDGE;

    private AuraFluxBridgeRegistry() {
    }

    public static synchronized void install(AuraFluxBridge bridge) {
        AuraFluxBridgeRegistry.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public static synchronized void reset() {
        bridge = DEFAULT_BRIDGE;
    }

    public static synchronized int export(Level level, BlockPos sourcePos, int availablePower) {
        if (availablePower <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(availablePower, bridge.export(level, sourcePos, availablePower)));
    }
}
