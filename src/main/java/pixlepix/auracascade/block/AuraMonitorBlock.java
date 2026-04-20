package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.entity.AuraMonitorLogic;
import pixlepix.auracascade.block.entity.AuraSignalSource;

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
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return signalFor(level, pos);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level instanceof Level castLevel ? signalFor(castLevel, pos) : 0;
    }

    private static int signalFor(Level level, BlockPos pos) {
        List<Integer> signals = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof AuraSignalSource signalSource) {
                signals.add(signalSource.auraSignal());
            }
        }
        return AuraMonitorLogic.aggregate(signals);
    }
}
