package pixlepix.auracascade.item.books;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.entity.StorageBookshelfBlockEntity;

public final class StorageBookItem extends Item {
    private final StorageBookVariant variant;

    public StorageBookItem(StorageBookVariant variant) {
        this(variant, new Item.Properties());
    }

    public StorageBookItem(StorageBookVariant variant, Item.Properties properties) {
        super(properties.stacksTo(1));
        this.variant = variant;
    }

    public StorageBookVariant variant() {
        return variant;
    }

    public int insert(ItemStack bookStack, ItemStack source) {
        return StorageBookData.insert(bookStack, variant, source);
    }

    public ItemStack extractFirst(ItemStack bookStack) {
        return StorageBookData.extractFirst(bookStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!level.getBlockState(context.getClickedPos()).is(Blocks.BOOKSHELF)) {
            return super.useOn(context);
        }

        if (!level.isClientSide()) {
            ItemStack heldStack = context.getItemInHand();
            Component placedName = heldStack.getHoverName().copy();
            level.setBlock(context.getClickedPos(), AuraContent.STORAGE_BOOKSHELF.defaultBlockState(), 3);
            if (level.getBlockEntity(context.getClickedPos()) instanceof StorageBookshelfBlockEntity bookshelf) {
                bookshelf.setBook(heldStack.copyWithCount(1));
            }
            heldStack.shrink(1);
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                    Component.translatable("message.aura.storage_bookshelf_created", placedName),
                    true
                );
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        TooltipContext context,
        java.util.List<Component> tooltip,
        TooltipFlag tooltipFlag
    ) {
        tooltip.add(
            Component.literal(variant.maxStoredTypes() + " types, " + variant.maxItemsPerType() + " per type").withStyle(ChatFormatting.GRAY)
        );
        tooltip.add(Component.literal(StorageBookData.summary(stack)).withStyle(ChatFormatting.DARK_GREEN));
    }
}
