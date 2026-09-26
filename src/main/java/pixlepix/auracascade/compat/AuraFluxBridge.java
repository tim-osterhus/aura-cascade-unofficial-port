package pixlepix.auracascade.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

@FunctionalInterface
public interface AuraFluxBridge {
    int export(Level level, BlockPos sourcePos, int availablePower);
}
