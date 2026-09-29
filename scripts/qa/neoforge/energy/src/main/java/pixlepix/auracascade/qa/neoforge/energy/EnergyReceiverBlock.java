package pixlepix.auracascade.qa.neoforge.energy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

final class EnergyReceiverBlock extends BaseEntityBlock {
    static final MapCodec<EnergyReceiverBlock> CODEC = simpleCodec(ignored -> new EnergyReceiverBlock());

    EnergyReceiverBlock() {
        super(BlockBehaviour.Properties.of().strength(1.0F).noOcclusion());
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyReceiverBlockEntity(pos, state);
    }
}
