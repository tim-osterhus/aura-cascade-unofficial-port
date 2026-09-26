package pixlepix.auracascade.block.menu;

import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.network.BookshelfCoordinatorNetworking;

public final class BookshelfCoordinatorMenu extends AbstractContainerMenu {
    private static MenuType<BookshelfCoordinatorMenu> registeredType;

    private final BlockPos coordinatorPos;
    private final ServerPlayer serverViewer;
    private List<BookshelfCoordinatorBlockEntity.BrowserEntry> entries = List.of();
    private int connectedShelves;
    private int storageShelves;
    private int requiredPower;
    private int availablePower;
    private boolean networkComplete;
    private int resultCode;
    private int resultAmount;
    private int refreshTicks;
    private int snapshotRevision;

    public static void registerMenuType(MenuType<BookshelfCoordinatorMenu> menuType) {
        if (registeredType != null && registeredType != menuType) {
            throw new IllegalStateException("Bookshelf Coordinator menu type was registered twice.");
        }
        registeredType = Objects.requireNonNull(menuType);
    }

    public static MenuType<BookshelfCoordinatorMenu> registeredMenuType() {
        if (registeredType == null) {
            throw new IllegalStateException("Register the Bookshelf Coordinator menu type before opening it.");
        }
        return registeredType;
    }

    public BookshelfCoordinatorMenu(int containerId, Inventory inventory) {
        this(registeredMenuType(), containerId, inventory, BlockPos.ZERO, null);
    }

    public BookshelfCoordinatorMenu(
        MenuType<?> menuType,
        int containerId,
        Inventory inventory,
        BlockPos coordinatorPos,
        Player viewer
    ) {
        super(menuType, containerId);
        this.coordinatorPos = coordinatorPos.immutable();
        this.serverViewer = viewer instanceof ServerPlayer serverPlayer ? serverPlayer : null;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 134 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 192));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }

        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack original = slot.getItem();
        ItemStack moved = original.copy();
        int split = 27;
        if (slotIndex < split) {
            if (!moveItemStackTo(original, split, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(original, 0, split, false)) {
            return ItemStack.EMPTY;
        }

        if (original.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        if (serverViewer == null) {
            return true;
        }
        if (player != serverViewer) {
            return false;
        }

        Level level = serverViewer.level();
        boolean loaded = level.isLoaded(coordinatorPos);
        boolean coordinatorPresent = loaded
            && level.getBlockEntity(coordinatorPos) instanceof BookshelfCoordinatorBlockEntity;
        return hasServerAccess(
            coordinatorPos,
            player.getX(),
            player.getY(),
            player.getZ(),
            loaded,
            coordinatorPresent
        );
    }

    public static boolean hasServerAccess(
        BlockPos coordinatorPos,
        double playerX,
        double playerY,
        double playerZ,
        boolean chunkLoaded,
        boolean coordinatorPresent
    ) {
        if (!chunkLoaded || !coordinatorPresent) {
            return false;
        }
        double dx = coordinatorPos.getX() + 0.5D - playerX;
        double dy = coordinatorPos.getY() + 0.5D - playerY;
        double dz = coordinatorPos.getZ() + 0.5D - playerZ;
        return dx * dx + dy * dy + dz * dz < 64.0D;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (serverViewer != null && ++refreshTicks >= 20) {
            refreshTicks = 0;
            refreshFromWorld(false);
        }
    }

    public boolean hasServerViewer(Player player) {
        return serverViewer == player;
    }

    public BlockPos coordinatorPos() {
        return coordinatorPos;
    }

    public List<BookshelfCoordinatorBlockEntity.BrowserEntry> entries() {
        return entries;
    }

    public int connectedShelves() {
        return connectedShelves;
    }

    public int storageShelves() {
        return storageShelves;
    }

    public int requiredPower() {
        return requiredPower;
    }

    public int availablePower() {
        return availablePower;
    }

    public boolean networkComplete() {
        return networkComplete;
    }

    public int resultCode() {
        return resultCode;
    }

    public int resultAmount() {
        return resultAmount;
    }

    public int snapshotRevision() {
        return snapshotRevision;
    }

    public boolean canRetrieve() {
        return networkComplete
            && connectedShelves > 0
            && storageShelves > 0
            && availablePower >= requiredPower;
    }

    public void refreshFromWorld(boolean force) {
        if (serverViewer == null || serverViewer.containerMenu != this || !stillValid(serverViewer)) {
            return;
        }

        if (!(serverViewer.level().getBlockEntity(coordinatorPos) instanceof BookshelfCoordinatorBlockEntity coordinator)) {
            return;
        }
        BookshelfCoordinatorBlockEntity.BrowserSnapshot snapshot = coordinator.browserSnapshot(serverViewer.level(), coordinatorPos);
        boolean changed = force || !sameSnapshot(snapshot);
        if (!changed) {
            return;
        }

        entries = snapshot.entries();
        connectedShelves = snapshot.connectedShelves();
        storageShelves = snapshot.storageShelves();
        requiredPower = snapshot.requiredPower();
        availablePower = snapshot.availablePower();
        networkComplete = snapshot.complete();
        snapshotRevision++;
        BookshelfCoordinatorNetworking.sendSnapshot(serverViewer, this);
    }

    public void setResultAndRefresh(int resultCode, int resultAmount) {
        this.resultCode = resultCode;
        this.resultAmount = Math.max(0, resultAmount);
        refreshFromWorld(true);
    }

    public void applySnapshot(BookshelfCoordinatorNetworking.SnapshotPayload snapshot) {
        if (snapshot.containerId() != containerId) {
            return;
        }
        entries = snapshot.entries();
        connectedShelves = snapshot.connectedShelves();
        storageShelves = snapshot.storageShelves();
        requiredPower = snapshot.requiredPower();
        availablePower = snapshot.availablePower();
        networkComplete = snapshot.networkComplete();
        resultCode = snapshot.resultCode();
        resultAmount = snapshot.resultAmount();
        snapshotRevision++;
    }

    private boolean sameSnapshot(BookshelfCoordinatorBlockEntity.BrowserSnapshot snapshot) {
        if (connectedShelves != snapshot.connectedShelves()
            || storageShelves != snapshot.storageShelves()
            || requiredPower != snapshot.requiredPower()
            || availablePower != snapshot.availablePower()
            || networkComplete != snapshot.complete()
            || entries.size() != snapshot.entries().size()) {
            return false;
        }

        for (int index = 0; index < entries.size(); index++) {
            BookshelfCoordinatorBlockEntity.BrowserEntry current = entries.get(index);
            BookshelfCoordinatorBlockEntity.BrowserEntry next = snapshot.entries().get(index);
            if (current.count() != next.count()
                || !ItemStack.isSameItemSameComponents(current.stack(), next.stack())) {
                return false;
            }
        }
        return true;
    }
}
