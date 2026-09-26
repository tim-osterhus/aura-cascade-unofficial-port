package pixlepix.auracascade.compat;

import java.util.List;
import java.lang.reflect.Method;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraAccessoryInventoryTest {
    private static ClassLoader auraTargetLoader;

    @BeforeAll
    static void bootstrapMinecraft() throws ReflectiveOperationException {
        TestMinecraftBootstrap.ensureBootstrapped();
        TestMinecraftBootstrap.auraRegistrationSnapshot();
        Method loaderMethod = TestMinecraftBootstrap.class.getDeclaredMethod("fabricTargetClassLoader");
        loaderMethod.setAccessible(true);
        auraTargetLoader = (ClassLoader) loaderMethod.invoke(null);
    }

    @Test
    void physicalSlotsMatchOriginalBaublesOrder() {
        assertEquals(AuraAccessorySlot.AMULET, AuraAccessoryInventory.slotType(0));
        assertEquals(AuraAccessorySlot.RING, AuraAccessoryInventory.slotType(1));
        assertEquals(AuraAccessorySlot.RING, AuraAccessoryInventory.slotType(2));
        assertEquals(AuraAccessorySlot.BELT, AuraAccessoryInventory.slotType(3));
    }

    @Test
    void ringsUseBothSlotsThenRejectAnotherWithoutReplacement() {
        ItemStack ringOne = new ItemStack(Items.GOLD_INGOT);
        ItemStack ringTwo = new ItemStack(Items.IRON_INGOT);
        List<ItemStack> empty = List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        assertEquals(1, AuraAccessoryInventory.firstEmpty(empty, AuraAccessorySlot.RING));

        List<ItemStack> oneRing = List.of(ItemStack.EMPTY, ringOne, ItemStack.EMPTY, ItemStack.EMPTY);
        assertEquals(2, AuraAccessoryInventory.firstEmpty(oneRing, AuraAccessorySlot.RING));

        List<ItemStack> full = List.of(ItemStack.EMPTY, ringOne, ringTwo, ItemStack.EMPTY);
        assertEquals(-1, AuraAccessoryInventory.firstEmpty(full, AuraAccessorySlot.RING));
        assertEquals(ringOne, full.get(1));
        assertEquals(ringTwo, full.get(2));
    }

    @Test
    void accessoryTypesCannotEnterOtherPhysicalSlots() throws ReflectiveOperationException {
        Object ring = registeredStack("pixlepix.auracascade.item.AuraItems", "RING_OF_BINDING");
        Object amulet = registeredStack("pixlepix.auracascade.item.AuraItems", "AMULET_OF_THE_ANGELS_WING");
        Object belt = registeredStack("pixlepix.auracascade.item.AuraItems", "SASH_OF_THE_ANGELS_HEELS");
        Object ordinary = registeredStack("net.minecraft.world.item.Items", "GOLD_INGOT");
        Class<?> stackType = Class.forName("net.minecraft.world.item.ItemStack", false, auraTargetLoader);
        Method accepts = Class.forName("pixlepix.auracascade.compat.AuraAccessoryInventory", true, auraTargetLoader)
            .getMethod("accepts", int.class, stackType);

        assertTrue((boolean) accepts.invoke(null, 1, ring));
        assertTrue((boolean) accepts.invoke(null, 2, ring));
        assertFalse((boolean) accepts.invoke(null, 0, ring));
        assertTrue((boolean) accepts.invoke(null, 0, amulet));
        assertTrue((boolean) accepts.invoke(null, 3, belt));
        assertFalse((boolean) accepts.invoke(null, 1, belt));
        assertFalse((boolean) accepts.invoke(null, 1, ordinary));
    }

    private static Object registeredStack(String holderClassName, String fieldName) throws ReflectiveOperationException {
        Object item = Class.forName(holderClassName, true, auraTargetLoader).getField(fieldName).get(null);
        Class<?> itemLike = Class.forName("net.minecraft.world.level.ItemLike", false, auraTargetLoader);
        return Class.forName("net.minecraft.world.item.ItemStack", false, auraTargetLoader)
            .getConstructor(itemLike).newInstance(item);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ownerSyncCodecPreservesAllSlotsAndItemComponents() throws ReflectiveOperationException {
        Class<?> loadoutClass = Class.forName("pixlepix.auracascade.compat.AuraAccessoryInventory$Loadout");
        var constructor = loadoutClass.getDeclaredConstructor(ItemStack.class, ItemStack.class, ItemStack.class, ItemStack.class);
        constructor.setAccessible(true);
        ItemStack named = new ItemStack(Items.GOLD_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Bound role marker"));
        List<ItemStack> expected = List.of(ItemStack.EMPTY, named, new ItemStack(Items.DIAMOND), ItemStack.EMPTY);
        Object loadout = constructor.newInstance(expected.toArray());
        var codecField = AuraAccessoryInventory.class.getDeclaredField("SYNC_CODEC");
        codecField.setAccessible(true);
        StreamCodec<RegistryFriendlyByteBuf, Object> codec =
            (StreamCodec<RegistryFriendlyByteBuf, Object>) codecField.get(null);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        try {
            codec.encode(buffer, loadout);
            Object decoded = codec.decode(buffer);
            Method get = loadoutClass.getDeclaredMethod("get", int.class);
            get.setAccessible(true);
            for (int slot = 0; slot < expected.size(); slot++) {
                assertTrue(ItemStack.matches(expected.get(slot), (ItemStack) get.invoke(decoded, slot)), "slot " + slot);
            }
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }
}
