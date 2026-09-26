package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import pixlepix.auracascade.block.entity.LateGameWorldLogic;

public class FortifiedBlock extends Block {
    public static final MapCodec<FortifiedBlock> CODEC = simpleCodec(FortifiedBlock::new);
    public static final IntegerProperty DAMAGE = BlockStateProperties.AGE_15;

    public FortifiedBlock(BlockBehaviour.Properties properties) {
        super(properties.randomTicks().sound(SoundType.STONE));
        registerDefaultState(stateDefinition.any().setValue(DAMAGE, 0));
    }

    @Override
    public MapCodec<FortifiedBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        FortifiedBlockVariant variant = AuraContent.fortifiedVariant(this);
        if (variant == null || state.getValue(DAMAGE) <= 0) {
            return;
        }
        if (random.nextDouble() < variant.repairChance()) {
            level.setBlock(pos, state.setValue(DAMAGE, state.getValue(DAMAGE) - 1), 3);
        }
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        FortifiedBlockVariant variant = AuraContent.fortifiedVariant(this);
        return variant != null && variant.translucent() && adjacentBlockState.is(this);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        FortifiedBlockVariant variant = AuraContent.fortifiedVariant(this);
        return variant != null && variant.translucent();
    }

    public static boolean stress(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(state.getBlock() instanceof FortifiedBlock block)) {
            return false;
        }

        FortifiedBlockVariant variant = AuraContent.fortifiedVariant(block);
        if (variant == null || !LateGameWorldLogic.shouldDamageContainment(variant, random)) {
            return false;
        }

        int nextDamage = state.getValue(DAMAGE) + 1;
        if (nextDamage > 15) {
            level.destroyBlock(pos, false);
        } else {
            level.setBlock(pos, state.setValue(DAMAGE, nextDamage), 3);
        }
        return true;
    }
}
