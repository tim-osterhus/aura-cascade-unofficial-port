package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import pixlepix.auracascade.block.AuraContent;

public final class BookshelfCoordinatorBlockEntity extends net.minecraft.world.level.block.entity.BlockEntity {
    public BookshelfCoordinatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(AuraContent.BOOKSHELF_COORDINATOR_BLOCK_ENTITY, pos, blockState);
    }

    public NetworkSnapshot snapshot(Level level, BlockPos pos) {
        List<BlockPos> connectedShelves = new ArrayList<>(BookshelfNetworkLogic.collectConnectedShelves(level, pos));
        ArrayList<BlockPos> visibleStorageShelves = new ArrayList<>();
        for (BlockPos shelfPos : connectedShelves) {
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
            availablePower(level, pos)
        );
    }

    public int depositIntoNetwork(Level level, BlockPos pos, ItemStack source) {
        NetworkSnapshot snapshot = snapshot(level, pos);
        if (snapshot.storageShelfPositions().isEmpty() || snapshot.availablePower() < snapshot.requiredPower()) {
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
        if (snapshot.storageShelfPositions().isEmpty() || snapshot.availablePower() < snapshot.requiredPower()) {
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
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
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
            BlockEntity blockEntity = level.getBlockEntity(pos.relative(direction));
            if (blockEntity instanceof AuraNetworkBlockEntity auraNetworkBlockEntity) {
                remaining -= auraNetworkBlockEntity.extractStoredPower(remaining);
            }
        }
    }

    public record NetworkSnapshot(
        List<BlockPos> connectedShelfPositions,
        List<BlockPos> storageShelfPositions,
        int requiredPower,
        int availablePower
    ) {
    }
}
