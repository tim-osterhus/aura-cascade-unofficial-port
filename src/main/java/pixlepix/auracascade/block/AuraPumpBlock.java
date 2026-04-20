package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpVariant;
import pixlepix.auracascade.block.entity.AuraSignalSource;

public class AuraPumpBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<AuraPumpBlock> CODEC = simpleCodec(AuraPumpBlock::new);

    public AuraPumpBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<AuraPumpBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AuraPumpBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
        Level level,
        BlockState state,
        BlockEntityType<T> blockEntityType
    ) {
        return createTickerHelper(blockEntityType, AuraContent.AURA_PUMP_BLOCK_ENTITY, AuraPumpBlockEntity::serverTick);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof AuraSignalSource signalSource ? signalSource.auraSignal() : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof AuraSignalSource signalSource ? signalSource.auraSignal() : 0;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (!level.isClientSide()) {
            AuraPumpVariant variant = AuraContent.pumpVariant(state.getBlock());
            if (variant == AuraPumpVariant.MOMENTUM || variant == AuraPumpVariant.MOMENTUM_ALT) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof AuraPumpBlockEntity pumpBlockEntity) {
                    pumpBlockEntity.feedFromFall((float) fallDistance);
                }
            }
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        if (!level.isClientSide()) {
            AuraPumpVariant variant = AuraContent.pumpVariant(state.getBlock());
            if (variant == AuraPumpVariant.PROJECTILE || variant == AuraPumpVariant.PROJECTILE_ALT) {
                BlockEntity blockEntity = level.getBlockEntity(hitResult.getBlockPos());
                if (blockEntity instanceof AuraPumpBlockEntity pumpBlockEntity) {
                    if (projectile instanceof AbstractArrow) {
                        pumpBlockEntity.feedFromArrow();
                        projectile.discard();
                    } else if (projectile instanceof ThrownEgg) {
                        pumpBlockEntity.feedFromEgg();
                        projectile.discard();
                    } else if (projectile instanceof Snowball) {
                        pumpBlockEntity.feedFromSnowball();
                        projectile.discard();
                    }
                }
            }
        }
        super.onProjectileHit(level, state, hitResult, projectile);
    }
}
