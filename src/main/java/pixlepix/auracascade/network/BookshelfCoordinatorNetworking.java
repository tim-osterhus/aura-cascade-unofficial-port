package pixlepix.auracascade.network;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;

public final class BookshelfCoordinatorNetworking {
    public static final int RESULT_NONE = 0;
    public static final int RESULT_RETRIEVED = 1;
    public static final int RESULT_EMPTY = 2;
    public static final int RESULT_POWERLESS = 3;
    public static final int RESULT_DISCONNECTED = 4;
    public static final int RESULT_INCOMPLETE_NETWORK = 5;
    public static final int RESULT_INVENTORY_FULL = 6;
    public static final int RESULT_TYPE_MISSING = 7;
    public static final int RESULT_INVALID_AMOUNT = 8;

    public static final CustomPacketPayload.Type<SnapshotPayload> SNAPSHOT_TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath("aura", "bookshelf_browser_snapshot")
    );
    private static final CustomPacketPayload.Type<ExtractRequest> EXTRACT_TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath("aura", "bookshelf_browser_extract")
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, ExtractRequest> EXTRACT_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        ExtractRequest::containerId,
        ItemStack.STREAM_CODEC,
        ExtractRequest::target,
        ByteBufCodecs.VAR_INT,
        ExtractRequest::requestedCount,
        ExtractRequest::new
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, SnapshotPayload> SNAPSHOT_CODEC = StreamCodec.of(
        (buffer, payload) -> payload.write(buffer),
        SnapshotPayload::new
    );

    private BookshelfCoordinatorNetworking() {
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(EXTRACT_TYPE, EXTRACT_CODEC);
        PayloadTypeRegistry.playS2C().register(SNAPSHOT_TYPE, SNAPSHOT_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(EXTRACT_TYPE, BookshelfCoordinatorNetworking::receiveExtract);
    }

    public static void sendSnapshot(ServerPlayer player, BookshelfCoordinatorMenu menu) {
        ServerPlayNetworking.send(player, new SnapshotPayload(
            menu.containerId,
            menu.entries(),
            menu.connectedShelves(),
            menu.storageShelves(),
            menu.requiredPower(),
            menu.availablePower(),
            menu.networkComplete(),
            menu.resultCode(),
            menu.resultAmount()
        ));
    }

    private static void receiveExtract(ExtractRequest request, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!player.isAlive() || player.isSpectator()
            || !(player.containerMenu instanceof BookshelfCoordinatorMenu menu)
            || menu.containerId != request.containerId()
            || !menu.hasServerViewer(player)
            || !menu.stillValid(player)) {
            return;
        }

        Level level = player.level();
        if (!(level.getBlockEntity(menu.coordinatorPos()) instanceof BookshelfCoordinatorBlockEntity coordinator)) {
            return;
        }

        BookshelfCoordinatorBlockEntity.NetworkSnapshot network = coordinator.snapshot(level, menu.coordinatorPos());
        if (!network.complete()) {
            menu.setResultAndRefresh(RESULT_INCOMPLETE_NETWORK, 0);
            return;
        }
        if (network.connectedShelfPositions().isEmpty() || network.storageShelfPositions().isEmpty()) {
            menu.setResultAndRefresh(RESULT_DISCONNECTED, 0);
            return;
        }
        if (network.availablePower() < network.requiredPower()) {
            menu.setResultAndRefresh(RESULT_POWERLESS, 0);
            return;
        }
        if (request.requestedCount() <= 0 || request.target().isEmpty()) {
            menu.setResultAndRefresh(RESULT_INVALID_AMOUNT, 0);
            return;
        }

        ItemStack target = request.target().copyWithCount(1);
        long available = matchingCount(coordinator.browserSnapshot(level, menu.coordinatorPos()).entries(), target);
        if (available <= 0L) {
            menu.setResultAndRefresh(RESULT_TYPE_MISSING, 0);
            return;
        }

        Inventory inventory = player.getInventory();
        int inventoryCapacity = inventoryCapacity(inventory, target);
        if (inventoryCapacity <= 0) {
            menu.setResultAndRefresh(RESULT_INVENTORY_FULL, 0);
            return;
        }

        int requested = (int) Math.min(
            Math.min((long) request.requestedCount(), available),
            inventoryCapacity
        );
        List<ItemStack> extracted = coordinator.extractFromNetwork(level, menu.coordinatorPos(), target, requested);
        int extractedCount = 0;
        for (ItemStack stack : extracted) {
            extractedCount += stack.getCount();
            if (!inventory.add(stack)) {
                player.drop(stack, false);
            }
        }

        if (extractedCount == 0) {
            menu.setResultAndRefresh(RESULT_TYPE_MISSING, 0);
        } else {
            menu.setResultAndRefresh(RESULT_RETRIEVED, extractedCount);
        }
    }

    private static long matchingCount(List<BookshelfCoordinatorBlockEntity.BrowserEntry> entries, ItemStack target) {
        for (BookshelfCoordinatorBlockEntity.BrowserEntry entry : entries) {
            if (ItemStack.isSameItemSameComponents(entry.stack(), target)) {
                return entry.count();
            }
        }
        return 0L;
    }

    private static int inventoryCapacity(Inventory inventory, ItemStack target) {
        int capacity = 0;
        int stackLimit = target.getMaxStackSize();
        for (ItemStack slot : inventory.items) {
            if (slot.isEmpty()) {
                capacity += stackLimit;
            } else if (ItemStack.isSameItemSameComponents(slot, target)) {
                capacity += Math.max(0, stackLimit - slot.getCount());
            }
        }
        return capacity;
    }

    public record ExtractRequest(int containerId, ItemStack target, int requestedCount) implements CustomPacketPayload {
        public ExtractRequest {
            target = target.isEmpty() ? ItemStack.EMPTY : target.copyWithCount(1);
        }

        @Override
        public ItemStack target() {
            return target.copy();
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return EXTRACT_TYPE;
        }
    }

    public record SnapshotPayload(
        int containerId,
        List<BookshelfCoordinatorBlockEntity.BrowserEntry> entries,
        int connectedShelves,
        int storageShelves,
        int requiredPower,
        int availablePower,
        boolean networkComplete,
        int resultCode,
        int resultAmount
    ) implements CustomPacketPayload {
        private static final int MAX_ENTRY_COUNT = 100_000;

        public SnapshotPayload {
            entries = List.copyOf(entries);
        }

        private SnapshotPayload(RegistryFriendlyByteBuf buffer) {
            this(
                buffer.readVarInt(),
                readEntries(buffer),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readVarInt(),
                buffer.readVarInt()
            );
        }

        private void write(RegistryFriendlyByteBuf buffer) {
            buffer.writeVarInt(containerId);
            if (entries.size() > MAX_ENTRY_COUNT) {
                throw new IllegalStateException("Bookshelf Coordinator snapshot exceeds the supported entry count.");
            }
            buffer.writeVarInt(entries.size());
            for (BookshelfCoordinatorBlockEntity.BrowserEntry entry : entries) {
                ItemStack.STREAM_CODEC.encode(buffer, entry.stack());
                buffer.writeVarLong(entry.count());
            }
            buffer.writeVarInt(connectedShelves);
            buffer.writeVarInt(storageShelves);
            buffer.writeVarInt(requiredPower);
            buffer.writeVarInt(availablePower);
            buffer.writeBoolean(networkComplete);
            buffer.writeVarInt(resultCode);
            buffer.writeVarInt(resultAmount);
        }

        private static List<BookshelfCoordinatorBlockEntity.BrowserEntry> readEntries(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readVarInt();
            if (size < 0 || size > MAX_ENTRY_COUNT) {
                throw new IllegalArgumentException("Invalid Bookshelf Coordinator snapshot entry count: " + size);
            }
            ArrayList<BookshelfCoordinatorBlockEntity.BrowserEntry> entries = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                ItemStack stack = ItemStack.STREAM_CODEC.decode(buffer);
                long count = buffer.readVarLong();
                entries.add(new BookshelfCoordinatorBlockEntity.BrowserEntry(stack, count));
            }
            return entries;
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return SNAPSHOT_TYPE;
        }
    }
}
