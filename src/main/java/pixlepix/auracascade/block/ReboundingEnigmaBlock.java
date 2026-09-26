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

public class ReboundingEnigmaBlock extends Block {
    public static final MapCodec<ReboundingEnigmaBlock> CODEC = simpleCodec(ReboundingEnigmaBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12.8, 16);

    public ReboundingEnigmaBlock(BlockBehaviour.Properties properties) {
        super(properties.noOcclusion().lightLevel(state -> 15));
    }

    @Override
    public MapCodec<ReboundingEnigmaBlock> codec() {
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
        entity.setDeltaMovement(launchVelocity(entity.getDeltaMovement()));
    }

    static Vec3 launchVelocity(Vec3 incoming) {
        return new Vec3(incoming.x, 10.0D, incoming.z);
    }
}
