package pixlepix.auracascade.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.item.books.StorageBookData;
import pixlepix.auracascade.item.books.StorageBookItem;

public final class StorageBookshelfBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    private static final String STORED_BOOK_TAG = "stored_book";

    private ItemStack storedBook = ItemStack.EMPTY;

    public StorageBookshelfBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.STORAGE_BOOKSHELF_BLOCK_ENTITY, pos, blockState);
    }

    public boolean hasBook() {
        return !storedBook.isEmpty();
    }

    public ItemStack storedBook() {
        return storedBook.copy();
    }

    public boolean hasStoredItems() {
        return storedTypes() > 0;
    }

    public int storedTypes() {
        return hasBook() ? StorageBookData.storedTypes(storedBook) : 0;
    }

    public int storedItemCount() {
        return hasBook() ? StorageBookData.storedItemCount(storedBook) : 0;
    }

    public List<StorageBookData.Entry> storedEntries() {
        return hasBook() ? StorageBookData.detailedEntries(storedBook) : List.of();
    }

    public String bookLabel() {
        return hasBook() ? storedBook.getHoverName().getString() : "Empty shelf";
    }

    public boolean setBook(ItemStack bookStack) {
        if (hasBook() || !(bookStack.getItem() instanceof StorageBookItem)) {
            return false;
        }
        storedBook = bookStack.copyWithCount(1);
        markUpdated();
        return true;
    }

    public int deposit(ItemStack source) {
        if (!(storedBook.getItem() instanceof StorageBookItem storageBookItem)) {
            return 0;
        }
        int inserted = storageBookItem.insert(storedBook, source);
        if (inserted > 0) {
            markUpdated();
        }
        return inserted;
    }

    public ItemStack extractFirst() {
        if (!(storedBook.getItem() instanceof StorageBookItem storageBookItem)) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = storageBookItem.extractFirst(storedBook);
        if (!extracted.isEmpty()) {
            markUpdated();
        }
        return extracted;
    }

    public ItemStack extract(ItemStack target, int requestedCount) {
        if (!(storedBook.getItem() instanceof StorageBookItem) || target.isEmpty() || requestedCount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = StorageBookData.extract(storedBook, target, requestedCount);
        if (!extracted.isEmpty()) {
            markUpdated();
        }
        return extracted;
    }

    public ItemStack removeBook() {
        if (storedBook.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = storedBook.copy();
        storedBook = ItemStack.EMPTY;
        markUpdated();
        return removed;
    }

    public void dropStoredBook(Level level, BlockPos pos) {
        if (!storedBook.isEmpty() && !level.isClientSide()) {
            Block.popResource(level, pos.above(), storedBook.copy());
            storedBook = ItemStack.EMPTY;
            markUpdated();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedBook = tag.contains(STORED_BOOK_TAG, Tag.TAG_COMPOUND)
            ? ItemStack.parseOptional(registries, tag.getCompound(STORED_BOOK_TAG))
            : ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!storedBook.isEmpty()) {
            tag.put(STORED_BOOK_TAG, storedBook.saveOptional(registries));
        }
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void markUpdated() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
