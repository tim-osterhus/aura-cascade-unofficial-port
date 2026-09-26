package pixlepix.auracascade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
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
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;

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
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof BookshelfCoordinatorBlockEntity coordinator)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        if (!level.isClientSide()) {
            BookshelfCoordinatorBlockEntity.NetworkSnapshot snapshot = coordinator.snapshot(level, pos);
            if (snapshot.connectedShelfPositions().isEmpty()) {
                player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.disconnected"), true);
            } else if (!snapshot.complete()) {
                player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.incomplete_network"), true);
            } else if (snapshot.availablePower() < snapshot.requiredPower()) {
                player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.powerless"), true);
            } else {
                int inserted = coordinator.depositIntoNetwork(level, pos, stack);
                if (inserted > 0) {
                    stack.shrink(inserted);
                    player.displayClientMessage(
                        Component.translatable("message.aura.bookshelf_coordinator.stored", inserted),
                        true
                    );
                } else {
                    player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.could_not_store"), true);
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
                    player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.disconnected"), true);
                } else if (!snapshot.complete()) {
                    player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.incomplete_network"), true);
                } else if (snapshot.availablePower() < snapshot.requiredPower()) {
                    player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.powerless"), true);
                } else {
                    ItemStack extracted = coordinator.extractFromNetwork(level, pos);
                    if (extracted.isEmpty()) {
                        player.displayClientMessage(Component.translatable("message.aura.bookshelf_coordinator.empty"), true);
                    } else {
                        giveOrDrop(player, extracted);
                        player.displayClientMessage(
                            Component.translatable("message.aura.bookshelf_coordinator.withdrawn", extracted.getHoverName()),
                            true
                        );
                    }
                }
            } else {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(menuProvider(pos));
                    if (serverPlayer.containerMenu instanceof BookshelfCoordinatorMenu menu) {
                        menu.refreshFromWorld(true);
                    }
                }
            }
        }

        return InteractionResult.SUCCESS;
    }

    private static MenuProvider menuProvider(BlockPos pos) {
        BlockPos coordinatorPos = pos.immutable();
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.aura.bookshelf_coordinator");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                return new BookshelfCoordinatorMenu(
                    BookshelfCoordinatorMenu.registeredMenuType(),
                    containerId,
                    inventory,
                    coordinatorPos,
                    player
                );
            }
        };
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
