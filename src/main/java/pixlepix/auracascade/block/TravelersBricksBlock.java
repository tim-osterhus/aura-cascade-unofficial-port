package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class TravelersBricksBlock extends Block {
    public static final MapCodec<TravelersBricksBlock> CODEC = simpleCodec(TravelersBricksBlock::new);

    public TravelersBricksBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<TravelersBricksBlock> codec() {
        return CODEC;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        Vec3 horizontal = new Vec3(entity.getDeltaMovement().x, 0.0D, entity.getDeltaMovement().z);
        if (entity.onGround() && horizontal.lengthSqr() > 0.0025D) {
            Vec3 normalized = horizontal.normalize().scale(1.15D);
            entity.setDeltaMovement(normalized.x, Math.max(entity.getDeltaMovement().y, 0.0D), normalized.z);
        }
        super.stepOn(level, pos, state, entity);
    }
}
