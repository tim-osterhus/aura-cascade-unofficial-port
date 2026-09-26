package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;
import pixlepix.auracascade.item.books.StorageBookItem;

public final class StorageBookshelfBlock extends BaseEntityBlock implements EntityBlock {
    public static final MapCodec<StorageBookshelfBlock> CODEC = simpleCodec(StorageBookshelfBlock::new);

    public StorageBookshelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<StorageBookshelfBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBookshelfBlockEntity(pos, state);
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
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof StorageBookshelfBlockEntity bookshelf)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (stack.getItem() instanceof StorageBookItem && !bookshelf.hasBook()) {
            if (!level.isClientSide()) {
                bookshelf.setBook(stack.copyWithCount(1));
                stack.shrink(1);
                player.displayClientMessage(Component.literal("Inserted the storage book."), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (bookshelf.hasBook() && !(stack.getItem() instanceof StorageBookItem)) {
            if (!level.isClientSide()) {
                int inserted = bookshelf.deposit(stack);
                if (inserted > 0) {
                    stack.shrink(inserted);
                    player.displayClientMessage(Component.literal("Stored " + inserted + " item(s)."), true);
                } else {
                    player.displayClientMessage(Component.literal("That book cannot accept this item."), true);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof StorageBookshelfBlockEntity bookshelf)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (player.isShiftKeyDown()) {
                if (bookshelf.hasStoredItems()) {
                    ItemStack extracted = bookshelf.extractFirst();
                    giveOrDrop(player, extracted);
                    player.displayClientMessage(Component.literal("Withdrew ").append(extracted.getHoverName()).append(Component.literal(".")), true);
                } else if (bookshelf.hasBook()) {
                    ItemStack removedBook = bookshelf.removeBook();
                    giveOrDrop(player, removedBook);
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.BOOKSHELF.defaultBlockState(), 3);
                    player.displayClientMessage(Component.literal("Removed the storage book."), true);
                } else {
                    player.displayClientMessage(Component.literal("The shelf is empty."), true);
                }
            } else {
                player.displayClientMessage(
                    Component.literal(bookshelf.bookLabel() + ": " + bookshelf.storedTypes() + " types / " + bookshelf.storedItemCount() + " items"),
                    false
                );
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(net.minecraft.world.level.block.Blocks.BOOKSHELF);
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
