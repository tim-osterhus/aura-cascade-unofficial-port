package pixlepix.auracascade.fairy;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import pixlepix.auracascade.block.FairyTorchBlock;

public final class FairyTorchRegistry {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("aura", "fairy_torch");
    private static Block block;

    private FairyTorchRegistry() {
    }

    public static void bootstrapCommon() {
        if (block == null) {
            block = Registry.register(BuiltInRegistries.BLOCK, ID, new FairyTorchBlock());
        }
    }

    public static Block block() {
        if (block == null) {
            throw new IllegalStateException("Fairy torch block has not been registered");
        }
        return block;
    }
}
