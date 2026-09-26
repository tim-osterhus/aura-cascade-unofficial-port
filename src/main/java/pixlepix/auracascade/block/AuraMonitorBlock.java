package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.entity.AuraConsumerBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.LateGameBlockEntity;

public class AuraMonitorBlock extends Block {
    public static final MapCodec<AuraMonitorBlock> CODEC = simpleCodec(AuraMonitorBlock::new);

    public AuraMonitorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AuraMonitorBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return signalFor(level, pos, null);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return signalFor(level, pos, direction.getOpposite());
    }

    private static int signalFor(BlockGetter level, BlockPos pos, Direction excluded) {
        for (Direction direction : Direction.values()) {
            if (direction == excluded) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof AuraPumpBlockEntity pump) {
                return pump.pumpState().power() > 0 ? 0 : 15;
            }
            if (blockEntity instanceof AuraConsumerBlockEntity consumer) {
                return consumer.hasValidWork() ? 0 : 15;
            }
            if (blockEntity instanceof LateGameBlockEntity consumer) {
                return consumer.hasValidWork() ? 0 : 15;
            }
        }
        return 0;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moving) {
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Item drops and fuel exhaustion do not necessarily issue block updates.
        level.updateNeighborsAt(pos, this);
        level.updateNeighbourForOutputSignal(pos, this);
        level.scheduleTick(pos, this, 20);
    }
}
