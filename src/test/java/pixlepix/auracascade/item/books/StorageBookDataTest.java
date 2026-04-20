package pixlepix.auracascade.item.books;

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
}
