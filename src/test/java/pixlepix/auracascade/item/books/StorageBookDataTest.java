package pixlepix.auracascade.item.books;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class StorageBookDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void denseBooksPreservePerTypeCapacityAndExtractPlayableStacks() {
        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack ingots = new ItemStack(Items.IRON_INGOT);
        ingots.setCount(1_000);

        assertEquals(1_000, StorageBookData.insert(book, StorageBookVariant.DENSE, ingots));
        assertEquals(0, StorageBookData.insert(book, StorageBookVariant.DENSE, new ItemStack(Items.IRON_INGOT, 1)));
        assertEquals(1, StorageBookData.storedTypes(book));
        assertEquals(1_000, StorageBookData.storedItemCount(book));

        ItemStack extracted = StorageBookData.extractFirst(book);
        assertTrue(extracted.is(Items.IRON_INGOT));
        assertEquals(64, extracted.getCount());
        assertEquals(936, StorageBookData.storedItemCount(book));
    }

    @Test
    void specializedBooksKeepTheirLegacyItemFilters() {
        ItemStack mineralBook = new ItemStack(Items.BOOK);
        assertEquals(12, StorageBookData.insert(mineralBook, StorageBookVariant.MINERAL, new ItemStack(Items.COAL, 12)));
        assertEquals(0, StorageBookData.insert(mineralBook, StorageBookVariant.MINERAL, new ItemStack(Items.BREAD, 1)));

        ItemStack modBook = new ItemStack(Items.BOOK);
        assertEquals(0, StorageBookData.insert(modBook, StorageBookVariant.MOD, new ItemStack(Items.DIRT, 1)));
        assertTrue(StorageBookData.entries(modBook).isEmpty());
    }

    @Test
    void browserEntriesKeepLargeCountsAndExtractionMatchesComponents() {
        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack namedIron = new ItemStack(Items.IRON_INGOT);
        namedIron.set(DataComponents.CUSTOM_NAME, Component.literal("First batch"));
        namedIron.setCount(128);
        ItemStack otherNamedIron = new ItemStack(Items.IRON_INGOT);
        otherNamedIron.set(DataComponents.CUSTOM_NAME, Component.literal("Second batch"));
        otherNamedIron.setCount(64);

        assertEquals(128, StorageBookData.insert(book, StorageBookVariant.DENSE, namedIron));
        assertEquals(64, StorageBookData.insert(book, StorageBookVariant.DENSE, otherNamedIron));
        assertEquals(2, StorageBookData.detailedEntries(book).size());
        assertEquals(128, StorageBookData.detailedEntries(book).getFirst().count());

        ItemStack persistedCopy = book.copy();
        ItemStack extracted = StorageBookData.extract(persistedCopy, otherNamedIron, 20);
        assertEquals(20, extracted.getCount());
        assertTrue(ItemStack.isSameItemSameComponents(otherNamedIron, extracted));
        assertEquals(128, StorageBookData.detailedEntries(persistedCopy).getFirst().count());
        assertEquals(44, StorageBookData.detailedEntries(persistedCopy).get(1).count());
        assertEquals(64, StorageBookData.detailedEntries(book).get(1).count());
    }
}
