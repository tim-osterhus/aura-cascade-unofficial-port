package pixlepix.auracascade.item.books;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import pixlepix.auracascade.util.NbtCompat;

public final class StorageBookData {
    private static final String STORAGE_ENTRIES_TAG = "storageEntries";
    private static final Codec<StoredEntry> STORED_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.fieldOf("stack").forGetter(StoredEntry::stack),
        Codec.INT.fieldOf("count").forGetter(StoredEntry::count)
    ).apply(instance, StoredEntry::new));

    private StorageBookData() {
    }

    public static List<ItemStack> entries(ItemStack bookStack) {
        ArrayList<ItemStack> copies = new ArrayList<>();
        for (StoredEntry storedEntry : storedEntries(bookStack)) {
            if (!storedEntry.stack().isEmpty()) {
                copies.add(storedEntry.stack().copy());
            }
        }
        return copies;
    }

    public static List<Entry> detailedEntries(ItemStack bookStack) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (StoredEntry storedEntry : storedEntries(bookStack)) {
            if (!storedEntry.stack().isEmpty() && storedEntry.count() > 0) {
                entries.add(new Entry(storedEntry.stack(), storedEntry.count()));
            }
        }
        return List.copyOf(entries);
    }

    public static int storedTypes(ItemStack bookStack) {
        return storedEntries(bookStack).size();
    }

    public static int storedItemCount(ItemStack bookStack) {
        int total = 0;
        for (StoredEntry entry : storedEntries(bookStack)) {
            total += entry.count();
        }
        return total;
    }

    public static int insert(ItemStack bookStack, StorageBookVariant variant, ItemStack source) {
        if (bookStack.isEmpty() || source.isEmpty()) {
            return 0;
        }

        ArrayList<StoredEntry> storedEntries = new ArrayList<>(storedEntries(bookStack));
        if (!variant.accepts(source, representativeStacks(storedEntries))) {
            return 0;
        }

        int remaining = source.getCount();
        boolean hasMatchingType = false;
        for (int index = 0; index < storedEntries.size(); index++) {
            StoredEntry storedEntry = storedEntries.get(index);
            if (!ItemStack.isSameItemSameComponents(storedEntry.stack(), source)) {
                continue;
            }
            hasMatchingType = true;

            int freeSpace = variant.maxItemsPerType() - storedEntry.count();
            if (freeSpace <= 0) {
                continue;
            }

            int moved = Math.min(freeSpace, remaining);
            storedEntries.set(index, new StoredEntry(storedEntry.stack(), storedEntry.count() + moved));
            remaining -= moved;
            if (remaining == 0) {
                break;
            }
        }

        if (remaining > 0
            && !hasMatchingType
            && storedEntries.size() < variant.maxStoredTypes()
            && variant.accepts(source, representativeStacks(storedEntries))) {
            int moved = Math.min(variant.maxItemsPerType(), remaining);
            storedEntries.add(new StoredEntry(source, moved));
            remaining -= moved;
        }

        int inserted = source.getCount() - remaining;
        if (inserted > 0) {
            storeEntries(bookStack, storedEntries);
        }
        return inserted;
    }

    public static ItemStack extractFirst(ItemStack bookStack) {
        ArrayList<StoredEntry> storedEntries = new ArrayList<>(storedEntries(bookStack));
        for (int index = 0; index < storedEntries.size(); index++) {
            StoredEntry storedEntry = storedEntries.get(index);
            if (storedEntry.stack().isEmpty() || storedEntry.count() <= 0) {
                continue;
            }

            int extractedCount = Math.min(storedEntry.count(), storedEntry.stack().getMaxStackSize());
            ItemStack extracted = storedEntry.stack().copyWithCount(extractedCount);
            int remaining = storedEntry.count() - extractedCount;
            if (remaining <= 0) {
                storedEntries.remove(index);
            } else {
                storedEntries.set(index, new StoredEntry(storedEntry.stack(), remaining));
            }
            storeEntries(bookStack, storedEntries);
            return extracted;
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack extract(ItemStack bookStack, ItemStack target, int requestedCount) {
        if (bookStack.isEmpty() || target.isEmpty() || requestedCount <= 0) {
            return ItemStack.EMPTY;
        }

        ArrayList<StoredEntry> storedEntries = new ArrayList<>(storedEntries(bookStack));
        ItemStack extracted = ItemStack.EMPTY;
        int remainingRequested = requestedCount;
        for (int index = 0; index < storedEntries.size(); index++) {
            StoredEntry storedEntry = storedEntries.get(index);
            if (storedEntry.stack().isEmpty()
                || !ItemStack.isSameItemSameComponents(storedEntry.stack(), target)) {
                continue;
            }

            int extractedCount = Math.min(storedEntry.count(), remainingRequested);
            if (extractedCount <= 0) {
                continue;
            }

            if (extracted.isEmpty()) {
                extracted = storedEntry.stack().copyWithCount(extractedCount);
            } else {
                extracted.grow(extractedCount);
            }
            int remaining = storedEntry.count() - extractedCount;
            if (remaining == 0) {
                storedEntries.remove(index);
                index--;
            } else {
                storedEntries.set(index, new StoredEntry(storedEntry.stack(), remaining));
            }
            remainingRequested -= extractedCount;
            if (remainingRequested == 0) {
                break;
            }
        }
        if (extracted.isEmpty()) {
            return ItemStack.EMPTY;
        }

        storeEntries(bookStack, storedEntries);
        return extracted;
    }

    public static String summary(ItemStack bookStack) {
        return storedTypes(bookStack) + " types / " + storedItemCount(bookStack) + " items";
    }

    private static List<StoredEntry> storedEntries(ItemStack bookStack) {
        CustomData customData = bookStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = customData.copyTag();
        return NbtCompat.read(tag, STORAGE_ENTRIES_TAG, STORED_ENTRY_CODEC.listOf()).orElse(List.of());
    }

    private static List<ItemStack> representativeStacks(List<StoredEntry> storedEntries) {
        ArrayList<ItemStack> stacks = new ArrayList<>(storedEntries.size());
        for (StoredEntry storedEntry : storedEntries) {
            stacks.add(storedEntry.stack());
        }
        return stacks;
    }

    private static void storeEntries(ItemStack bookStack, List<StoredEntry> storedEntries) {
        CustomData.update(DataComponents.CUSTOM_DATA, bookStack, tag -> {
            if (storedEntries.isEmpty()) {
                tag.remove(STORAGE_ENTRIES_TAG);
            } else {
                NbtCompat.store(tag, STORAGE_ENTRIES_TAG, STORED_ENTRY_CODEC.listOf(), storedEntries);
            }
        });
    }

    private record StoredEntry(ItemStack stack, int count) {
        private StoredEntry {
            stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            count = Math.max(0, count);
        }
    }

    public record Entry(ItemStack stack, int count) {
        public Entry {
            stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            count = Math.max(0, count);
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }
}
