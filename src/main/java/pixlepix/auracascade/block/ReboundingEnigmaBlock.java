package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ReboundingEnigmaBlock extends Block {
    public static final MapCodec<ReboundingEnigmaBlock> CODEC = simpleCodec(ReboundingEnigmaBlock::new);

    public ReboundingEnigmaBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ReboundingEnigmaBlock> codec() {
        return CODEC;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.fallDistance = 0.0F;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity.onGround() && entity.getDeltaMovement().y < 1.5D) {
            entity.setDeltaMovement(entity.getDeltaMovement().x, 1.5D, entity.getDeltaMovement().z);
        }
        entity.fallDistance = 0.0F;
        super.stepOn(level, pos, state, entity);
    }
}
