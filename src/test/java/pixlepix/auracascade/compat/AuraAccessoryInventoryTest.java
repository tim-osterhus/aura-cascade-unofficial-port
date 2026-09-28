package pixlepix.auracascade.compat;

import com.mojang.serialization.Codec;
import java.lang.reflect.Method;
import java.util.List;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraAccessoryInventoryTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
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
    void accessoryTypesCannotEnterOtherPhysicalSlots() {
        ItemStack ring = new ItemStack(AuraItems.RING_OF_BINDING);
        ItemStack amulet = new ItemStack(AuraItems.AMULET_OF_THE_ANGELS_WING);
        ItemStack belt = new ItemStack(AuraItems.SASH_OF_THE_ANGELS_HEELS);
        ItemStack ordinary = new ItemStack(Items.GOLD_INGOT);

        assertTrue(AuraAccessoryInventory.accepts(1, ring));
        assertTrue(AuraAccessoryInventory.accepts(2, ring));
        assertFalse(AuraAccessoryInventory.accepts(0, ring));
        assertTrue(AuraAccessoryInventory.accepts(0, amulet));
        assertTrue(AuraAccessoryInventory.accepts(3, belt));
        assertFalse(AuraAccessoryInventory.accepts(1, belt));
        assertFalse(AuraAccessoryInventory.accepts(1, ordinary));
    }

    @Test
    @SuppressWarnings("unchecked")
    void ownerSyncCodecPreservesAllSlotsAndItemComponents() throws ReflectiveOperationException {
        Class<?> loadoutClass = loadoutClass();
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

    @Test
    @SuppressWarnings("unchecked")
    void attachmentPersistsSlotsAndRequestsDeathCopy() throws ReflectiveOperationException {
        var attachment = NeoForgeRegistries.ATTACHMENT_TYPES.get(
            ResourceLocation.fromNamespaceAndPath("aura", "accessories")
        );
        assertNotNull(attachment);
        var copyOnDeath = attachment.getClass().getDeclaredField("copyOnDeath");
        copyOnDeath.setAccessible(true);
        assertTrue(copyOnDeath.getBoolean(attachment));

        Class<?> loadoutClass = loadoutClass();
        var constructor = loadoutClass.getDeclaredConstructor(ItemStack.class, ItemStack.class, ItemStack.class, ItemStack.class);
        constructor.setAccessible(true);
        ItemStack named = new ItemStack(Items.GOLD_INGOT);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Persistent ring"));
        Object loadout = constructor.newInstance(ItemStack.EMPTY, named, ItemStack.EMPTY, ItemStack.EMPTY);
        var codecField = AuraAccessoryInventory.class.getDeclaredField("CODEC");
        codecField.setAccessible(true);
        Codec<Object> codec = (Codec<Object>) codecField.get(null);
        var ops = RegistryOps.create(NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
        var encoded = codec.encodeStart(ops, loadout).result().orElseThrow();
        Object decoded = codec.parse(ops, encoded).result().orElseThrow();
        Method get = loadoutClass.getDeclaredMethod("get", int.class);
        get.setAccessible(true);
        assertTrue(ItemStack.matches(named, (ItemStack) get.invoke(decoded, AuraAccessoryInventory.FIRST_RING)));
        assertTrue(((ItemStack) get.invoke(decoded, AuraAccessoryInventory.AMULET)).isEmpty());
    }

    private static Class<?> loadoutClass() throws ClassNotFoundException {
        return Class.forName("pixlepix.auracascade.compat.AuraAccessoryInventory$Loadout");
    }
}
