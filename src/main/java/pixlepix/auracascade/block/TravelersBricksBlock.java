package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TravelersBricksBlock extends Block {
    public static final MapCodec<TravelersBricksBlock> CODEC = simpleCodec(TravelersBricksBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12.8, 16);

    public TravelersBricksBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion().lightLevel(state -> 15));
    }

    @Override
    public MapCodec<TravelersBricksBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                             net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean precise) {
        Vec3 boost = movementBoost(entity.getDeltaMovement());
        if (boost.lengthSqr() > 0.0D) {
            entity.push(boost.x, boost.y, boost.z);
        }
    }

    static Vec3 movementBoost(Vec3 motion) {
        return motion.lengthSqr() > 0.25D * 0.25D ? motion.normalize().scale(5.0D) : Vec3.ZERO;
    }
}
