package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;

public final class BookshelfCoordinatorBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<BookshelfCoordinatorBlock> CODEC = simpleCodec(BookshelfCoordinatorBlock::new);

    public BookshelfCoordinatorBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<BookshelfCoordinatorBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BookshelfCoordinatorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof BookshelfCoordinatorBlockEntity coordinator)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BookshelfCoordinatorBlockEntity.NetworkSnapshot snapshot = coordinator.snapshot(level, pos);
            if (snapshot.connectedShelfPositions().isEmpty()) {
                player.displayClientMessage(Component.literal("No connected shelves."), true);
            } else if (snapshot.availablePower() < snapshot.requiredPower()) {
                player.displayClientMessage(Component.literal("Not enough power to activate the Coordinator."), true);
            } else {
                int inserted = coordinator.depositIntoNetwork(level, pos, stack);
                if (inserted > 0) {
                    stack.shrink(inserted);
                    player.displayClientMessage(Component.literal("Stored " + inserted + " item(s) in the shelf network."), true);
                } else {
                    player.displayClientMessage(Component.literal("The shelf network could not accept that item."), true);
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof BookshelfCoordinatorBlockEntity coordinator)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BookshelfCoordinatorBlockEntity.NetworkSnapshot snapshot = coordinator.snapshot(level, pos);
            if (player.isShiftKeyDown()) {
                if (snapshot.connectedShelfPositions().isEmpty()) {
                    player.displayClientMessage(Component.literal("No connected shelves."), true);
                } else if (snapshot.availablePower() < snapshot.requiredPower()) {
                    player.displayClientMessage(Component.literal("Not enough power to activate the Coordinator."), true);
                } else {
                    ItemStack extracted = coordinator.extractFromNetwork(level, pos);
                    if (extracted.isEmpty()) {
                        player.displayClientMessage(Component.literal("The shelf network is empty."), true);
                    } else {
                        giveOrDrop(player, extracted);
                        player.displayClientMessage(Component.literal("Withdrew ").append(extracted.getHoverName()).append(Component.literal(".")), true);
                    }
                }
            } else {
                player.displayClientMessage(coordinator.statusMessage(level, pos), false);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
