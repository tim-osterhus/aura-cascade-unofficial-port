package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.item.books.StorageBookData;

public final class BookshelfCoordinatorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public BookshelfCoordinatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.BOOKSHELF_COORDINATOR_BLOCK_ENTITY, pos, blockState);
    }

    public NetworkSnapshot snapshot(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return new NetworkSnapshot(List.of(), List.of(), 0, 0, false);
        }

        ShelfScan scan = collectLoadedConnectedShelves(level, pos);
        List<BlockPos> connectedShelves = new ArrayList<>(scan.shelves());
        ArrayList<BlockPos> visibleStorageShelves = new ArrayList<>();
        for (BlockPos shelfPos : connectedShelves) {
            if (!chunksLoadedBetween(level, pos, shelfPos)) {
                scan = new ShelfScan(scan.shelves(), false);
                continue;
            }
            if (!BookshelfNetworkLogic.hasLineOfSight(level, pos, shelfPos)) {
                continue;
            }
            if (level.getBlockEntity(shelfPos) instanceof StorageBookshelfBlockEntity) {
                visibleStorageShelves.add(shelfPos.immutable());
            }
        }
        return new NetworkSnapshot(
            List.copyOf(connectedShelves),
            List.copyOf(visibleStorageShelves),
            BookshelfNetworkLogic.powerCost(connectedShelves.size()),
            availablePower(level, pos),
            scan.complete()
        );
    }

    public BrowserSnapshot browserSnapshot(Level level, BlockPos pos) {
        NetworkSnapshot network = snapshot(level, pos);
        Map<Integer, List<MutableBrowserEntry>> entriesByHash = new HashMap<>();
        ArrayList<MutableBrowserEntry> entries = new ArrayList<>();
        for (BlockPos shelfPos : network.storageShelfPositions()) {
            if (!level.isLoaded(shelfPos)
                || !(level.getBlockEntity(shelfPos) instanceof StorageBookshelfBlockEntity bookshelf)) {
                continue;
            }
            for (StorageBookData.Entry storedEntry : bookshelf.storedEntries()) {
                ItemStack stack = storedEntry.stack();
                int hash = ItemStack.hashItemAndComponents(stack);
                List<MutableBrowserEntry> bucket = entriesByHash.computeIfAbsent(hash, ignored -> new ArrayList<>());
                MutableBrowserEntry matching = null;
                for (MutableBrowserEntry candidate : bucket) {
                    if (ItemStack.isSameItemSameComponents(candidate.stack, stack)) {
                        matching = candidate;
                        break;
                    }
                }
                if (matching == null) {
                    matching = new MutableBrowserEntry(stack, storedEntry.count());
                    bucket.add(matching);
                    entries.add(matching);
                } else {
                    matching.count += storedEntry.count();
                }
            }
        }

        ArrayList<BrowserEntry> browserEntries = new ArrayList<>(entries.size());
        for (MutableBrowserEntry entry : entries) {
            browserEntries.add(new BrowserEntry(entry.stack, entry.count));
        }
        return new BrowserSnapshot(
            List.copyOf(browserEntries),
            network.connectedShelfPositions().size(),
            network.storageShelfPositions().size(),
            network.requiredPower(),
            network.availablePower(),
            network.complete()
        );
    }

    public int depositIntoNetwork(Level level, BlockPos pos, ItemStack source) {
        NetworkSnapshot snapshot = snapshot(level, pos);
        if (!snapshot.complete()
            || snapshot.storageShelfPositions().isEmpty()
            || snapshot.availablePower() < snapshot.requiredPower()) {
            return 0;
        }

        int remaining = source.getCount();
        int moved = 0;
        for (BlockPos shelfPos : snapshot.storageShelfPositions()) {
            if (remaining <= 0) {
                break;
            }
            if (level.getBlockEntity(shelfPos) instanceof StorageBookshelfBlockEntity bookshelf) {
                ItemStack pending = source.copyWithCount(remaining);
                int inserted = bookshelf.deposit(pending);
                if (inserted > 0) {
                    moved += inserted;
                    remaining -= inserted;
                }
            }
        }

        if (moved > 0) {
            consumePower(level, pos, snapshot.requiredPower());
            setChanged();
        }
        return moved;
    }

    public ItemStack extractFromNetwork(Level level, BlockPos pos) {
        NetworkSnapshot snapshot = snapshot(level, pos);
        if (!snapshot.complete()
            || snapshot.storageShelfPositions().isEmpty()
            || snapshot.availablePower() < snapshot.requiredPower()) {
            return ItemStack.EMPTY;
        }

        for (BlockPos shelfPos : snapshot.storageShelfPositions()) {
            if (level.getBlockEntity(shelfPos) instanceof StorageBookshelfBlockEntity bookshelf) {
                ItemStack extracted = bookshelf.extractFirst();
                if (!extracted.isEmpty()) {
                    consumePower(level, pos, snapshot.requiredPower());
                    setChanged();
                    return extracted;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    public List<ItemStack> extractFromNetwork(Level level, BlockPos pos, ItemStack target, int requestedCount) {
        if (level.isClientSide() || target.isEmpty() || requestedCount <= 0) {
            return List.of();
        }

        NetworkSnapshot snapshot = snapshot(level, pos);
        if (!snapshot.complete()
            || snapshot.storageShelfPositions().isEmpty()
            || snapshot.availablePower() < snapshot.requiredPower()) {
            return List.of();
        }

        int remaining = requestedCount;
        ArrayList<ItemStack> extractedStacks = new ArrayList<>();
        for (BlockPos shelfPos : snapshot.storageShelfPositions()) {
            if (remaining <= 0) {
                break;
            }
            if (!level.isLoaded(shelfPos)
                || !(level.getBlockEntity(shelfPos) instanceof StorageBookshelfBlockEntity bookshelf)) {
                continue;
            }

            int available = matchingCount(bookshelf, target);
            if (available <= 0) {
                continue;
            }

            ItemStack extracted = bookshelf.extract(target, Math.min(remaining, available));
            if (extracted.isEmpty()) {
                continue;
            }

            int moved = extracted.getCount();
            int stackLimit = extracted.getMaxStackSize();
            while (!extracted.isEmpty()) {
                extractedStacks.add(extracted.split(Math.min(stackLimit, extracted.getCount())));
            }
            remaining -= moved;
        }

        int extractedCount = requestedCount - remaining;
        if (extractedCount > 0) {
            consumePower(level, pos, snapshot.requiredPower());
            setChanged();
        }
        return List.copyOf(extractedStacks);
    }

    public Component statusMessage(Level level, BlockPos pos) {
        NetworkSnapshot snapshot = snapshot(level, pos);
        if (snapshot.connectedShelfPositions().isEmpty()) {
            return Component.literal("No connected shelves.");
        }
        return Component.literal(
            "Shelves: "
                + snapshot.connectedShelfPositions().size()
                + ", storage shelves: "
                + snapshot.storageShelfPositions().size()
                + ", power: "
                + snapshot.availablePower()
                + "/"
                + snapshot.requiredPower()
        );
    }

    private int availablePower(Level level, BlockPos pos) {
        int available = 0;
        for (Direction direction : Direction.values()) {
            BlockPos adjacent = pos.relative(direction);
            if (!level.isLoaded(adjacent)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(adjacent);
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                available += auraNetworkBlockEntity.storedPower();
            }
        }
        return available;
    }

    private void consumePower(Level level, BlockPos pos, int amount) {
        int remaining = amount;
        for (Direction direction : Direction.values()) {
            if (remaining <= 0) {
                return;
            }
            BlockPos adjacent = pos.relative(direction);
            if (!level.isLoaded(adjacent)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(adjacent);
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                remaining -= auraNetworkBlockEntity.extractStoredPower(remaining);
            }
        }
    }

    private static ShelfScan collectLoadedConnectedShelves(Level level, BlockPos coordinatorPos) {
        LinkedHashSet<BlockPos> visited = new LinkedHashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        boolean complete = true;

        for (Direction direction : Direction.values()) {
            BlockPos candidate = coordinatorPos.relative(direction);
            if (!level.isInWorldBounds(candidate)) {
                continue;
            }
            if (!level.isLoaded(candidate)) {
                complete = false;
                continue;
            }
            if (isBookshelf(level.getBlockState(candidate)) && visited.add(candidate.immutable())) {
                queue.add(candidate.immutable());
            }
        }

        while (!queue.isEmpty()) {
            BlockPos current = queue.remove();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (!level.isInWorldBounds(next) || visited.contains(next)) {
                    continue;
                }
                if (!level.isLoaded(next)) {
                    complete = false;
                    continue;
                }
                if (isBookshelf(level.getBlockState(next))) {
                    BlockPos immutable = next.immutable();
                    visited.add(immutable);
                    queue.add(immutable);
                }
            }
        }

        return new ShelfScan(visited, complete);
    }

    private static boolean chunksLoadedBetween(Level level, BlockPos from, BlockPos to) {
        int minChunkX = Math.min(from.getX(), to.getX()) >> 4;
        int maxChunkX = Math.max(from.getX(), to.getX()) >> 4;
        int minChunkZ = Math.min(from.getZ(), to.getZ()) >> 4;
        int maxChunkZ = Math.max(from.getZ(), to.getZ()) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                BlockPos sample = new BlockPos(chunkX << 4, from.getY(), chunkZ << 4);
                if (!level.isLoaded(sample)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isBookshelf(BlockState state) {
        return state.is(Blocks.BOOKSHELF)
            || state.is(Blocks.CHISELED_BOOKSHELF)
            || state.is(AuraContent.STORAGE_BOOKSHELF);
    }

    private static int matchingCount(StorageBookshelfBlockEntity bookshelf, ItemStack target) {
        int count = 0;
        for (StorageBookData.Entry entry : bookshelf.storedEntries()) {
            if (ItemStack.isSameItemSameComponents(entry.stack(), target)) {
                count += entry.count();
            }
        }
        return count;
    }

    public record NetworkSnapshot(
        List<BlockPos> connectedShelfPositions,
        List<BlockPos> storageShelfPositions,
        int requiredPower,
        int availablePower,
        boolean complete
    ) {
    }

    public record BrowserSnapshot(
        List<BrowserEntry> entries,
        int connectedShelves,
        int storageShelves,
        int requiredPower,
        int availablePower,
        boolean complete
    ) {
        public BrowserSnapshot {
            entries = List.copyOf(entries);
        }
    }

    public record BrowserEntry(ItemStack stack, long count) {
        public BrowserEntry {
            stack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            count = Math.max(0L, count);
        }

        @Override
        public ItemStack stack() {
            return stack.copy();
        }
    }

    private record ShelfScan(LinkedHashSet<BlockPos> shelves, boolean complete) {
    }

    private static final class MutableBrowserEntry {
        private final ItemStack stack;
        private long count;

        private MutableBrowserEntry(ItemStack stack, long count) {
            this.stack = stack.copyWithCount(1);
            this.count = count;
        }
    }
}
