package pixlepix.auracascade.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public final class FairyTorchBlock extends Block {
    public FairyTorchBlock() {
        super(Properties.of()
            .setId(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("aura", "fairy_torch")))
            .air()
            .replaceable()
            .noCollision()
            .noLootTable()
            .lightLevel(state -> 15)
            .randomTicks());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.removeBlock(pos, false);
    }
}
