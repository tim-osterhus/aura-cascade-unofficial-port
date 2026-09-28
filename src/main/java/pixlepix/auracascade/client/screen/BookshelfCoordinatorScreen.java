package pixlepix.auracascade.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import pixlepix.auracascade.block.entity.BookshelfCoordinatorBlockEntity;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.network.BookshelfCoordinatorNetworking;

public final class BookshelfCoordinatorScreen extends AbstractContainerScreen<BookshelfCoordinatorMenu> {
    private static final int COLUMNS = 9;
    private static final int VISIBLE_ROWS = 3;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 55;
    private static final int SCROLLBAR_X = GRID_X + COLUMNS * SLOT_SIZE + 4;
    private static final int SCROLLBAR_Y = GRID_Y;
    private static final int SCROLLBAR_HEIGHT = VISIBLE_ROWS * SLOT_SIZE;

    private EditBox searchBox;
    private EditBox amountBox;
    private Button minusButton;
    private Button plusButton;
    private Button maxButton;
    private Button retrieveButton;
    private ItemStack selectedStack = ItemStack.EMPTY;
    private int selectedCount;
    private int scrollRow;
    private boolean draggingScrollbar;
    private boolean filteredDirty = true;
    private int observedSnapshotRevision = -1;
    private List<BookshelfCoordinatorBlockEntity.BrowserEntry> filteredEntries = List.of();

    public BookshelfCoordinatorScreen(BookshelfCoordinatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 352;
        imageHeight = 220;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 119;
    }

    @Override
    protected void init() {
        super.init();
        searchBox = new EditBox(
            font,
            leftPos + 8,
            topPos + 31,
            imageWidth - 16,
            18,
            Component.translatable("screen.aura.bookshelf_coordinator.search")
        );
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.translatable("screen.aura.bookshelf_coordinator.search"));
        searchBox.setResponder(value -> {
            filteredDirty = true;
            clampScroll();
            retainVisibleSelection();
            updateControls();
        });
        addRenderableWidget(searchBox);

        amountBox = new EditBox(
            font,
            leftPos + 194,
            topPos + 107,
            54,
            18,
            Component.translatable("screen.aura.bookshelf_coordinator.amount")
        );
        amountBox.setMaxLength(10);
        amountBox.setFilter(value -> value.isEmpty() || value.chars().allMatch(Character::isDigit));
        amountBox.setValue("1");
        amountBox.setResponder(value -> updateControls());
        addRenderableWidget(amountBox);

        minusButton = addRenderableWidget(Button.builder(Component.literal("-"), button -> adjustAmount(-1))
            .bounds(leftPos + 251, topPos + 107, 18, 18)
            .build());
        plusButton = addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustAmount(1))
            .bounds(leftPos + 271, topPos + 107, 18, 18)
            .build());
        maxButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.aura.bookshelf_coordinator.max"),
                button -> setAmount(selectedCount)
            )
            .bounds(leftPos + 291, topPos + 107, 52, 18)
            .build());
        retrieveButton = addRenderableWidget(Button.builder(
                Component.translatable("screen.aura.bookshelf_coordinator.retrieve"),
                button -> retrieveSelected()
            )
            .bounds(leftPos + 194, topPos + 130, 149, 20)
            .build());
        updateControls();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (observedSnapshotRevision != menu.snapshotRevision()) {
            observedSnapshotRevision = menu.snapshotRevision();
            filteredDirty = true;
            clampScroll();
            refreshSelectionCount();
            retainVisibleSelection();
            updateControls();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF101010);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 27, 0xFF8B8B8B);
        graphics.fill(leftPos + 3, topPos + 3, leftPos + imageWidth - 3, topPos + 26, 0xFFC6C6C6);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int x = leftPos + GRID_X + column * SLOT_SIZE;
                int y = topPos + GRID_Y + row * SLOT_SIZE;
                drawBrowserSlot(graphics, x, y);
            }
        }

        for (var slot : menu.slots) {
            drawBrowserSlot(graphics, leftPos + slot.x - 1, topPos + slot.y - 1);
        }

        List<BookshelfCoordinatorBlockEntity.BrowserEntry> visible = visibleEntries();
        for (int index = 0; index < visible.size(); index++) {
            int row = index / COLUMNS;
            int column = index % COLUMNS;
            int x = leftPos + GRID_X + column * SLOT_SIZE;
            int y = topPos + GRID_Y + row * SLOT_SIZE;
            BookshelfCoordinatorBlockEntity.BrowserEntry entry = visible.get(index);
            ItemStack stack = entry.stack();
            if (!selectedStack.isEmpty() && ItemStack.isSameItemSameComponents(selectedStack, stack)) {
                graphics.renderOutline(x, y, SLOT_SIZE, SLOT_SIZE, 0xFFFFFFFF);
            }
            graphics.renderItem(stack, x + 1, y + 1);
            if (entry.count() > 1L) {
                String count = compactCount(entry.count());
                int width = font.width(count);
                float scale = Math.min(1.0F, 16.0F / Math.max(1, width));
                graphics.pose().pushMatrix();
                graphics.pose().translate(x + 17 - width * scale, y + 17 - font.lineHeight * scale);
                graphics.pose().scale(scale, scale);
                graphics.drawString(font, count, 0, 0, 0xFFFFFFFF, true);
                graphics.pose().popMatrix();
            }
        }

        drawScrollbar(graphics, filteredEntries().size());
        drawSelectionPanel(graphics);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component networkStatus = networkStatus();
        graphics.drawString(font, boundedText(networkStatus, 176), 8, 19, statusColor(), false);
        Component powerStatus = Component.translatable(
            "screen.aura.bookshelf_coordinator.power",
            menu.availablePower(),
            menu.requiredPower()
        );
        graphics.drawString(
            font,
            boundedText(powerStatus, 150),
            194,
            19,
            0xFF404040,
            false
        );

        if (menu.resultCode() != BookshelfCoordinatorNetworking.RESULT_NONE) {
            Component result = resultMessage();
            graphics.drawString(font, boundedText(result, 149), 194, 151, resultColor(), false);
        } else if (visibleEntries().isEmpty()) {
            Component empty = menu.entries().isEmpty()
                ? Component.translatable("screen.aura.bookshelf_coordinator.empty")
                : Component.translatable("screen.aura.bookshelf_coordinator.no_results");
            graphics.drawString(font, boundedText(empty, 149), 194, 151, 0xFF555555, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        BookshelfCoordinatorBlockEntity.BrowserEntry hovered = hoveredEntry(mouseX, mouseY);
        if (hovered != null) {
            graphics.setTooltipForNextFrame(font, List.of(
                hovered.stack().getHoverName(),
                Component.translatable("screen.aura.bookshelf_coordinator.item_count", hovered.count())
            ), Optional.empty(), mouseX, mouseY);
        } else if (isOverNetworkStatus(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, networkStatus(), mouseX, mouseY);
        } else if (isOverPowerStatus(mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(font, Component.translatable(
                "screen.aura.bookshelf_coordinator.power",
                menu.availablePower(),
                menu.requiredPower()
            ), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        if (button == 0 && isInScrollbar(mouseX, mouseY)) {
            draggingScrollbar = true;
            setScrollFromMouse(mouseY);
            return true;
        }
        if (button == 0 && isInBrowserGrid(mouseX, mouseY)) {
            int index = gridIndex(mouseX, mouseY);
            List<BookshelfCoordinatorBlockEntity.BrowserEntry> visible = visibleEntries();
            if (index >= 0 && index < visible.size()) {
                select(visible.get(index));
            } else {
                selectedStack = ItemStack.EMPTY;
                selectedCount = 0;
                updateControls();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingScrollbar && event.button() == 0) {
            setScrollFromMouse(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScrollbar = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isInBrowserGrid(mouseX, mouseY) && scrollY != 0.0D) {
            scrollRows(scrollY > 0.0D ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void drawSelectionPanel(GuiGraphics graphics) {
        graphics.fill(leftPos + 191, topPos + 53, leftPos + 344, topPos + 105, 0xFFB0B0B0);
        graphics.fill(leftPos + 192, topPos + 54, leftPos + 343, topPos + 104, 0xFFE2E2E2);
        if (selectedStack.isEmpty()) {
            Component choose = Component.translatable("screen.aura.bookshelf_coordinator.select_item");
            graphics.drawString(font, boundedText(choose, 142), leftPos + 197, topPos + 70, 0xFF555555, false);
            return;
        }

        graphics.renderItem(selectedStack, leftPos + 196, topPos + 59);
        graphics.drawString(
            font,
            boundedText(selectedStack.getHoverName(), 121),
            leftPos + 216,
            topPos + 59,
            0xFF202020,
            false
        );
        graphics.drawString(
            font,
            Component.translatable("screen.aura.bookshelf_coordinator.item_count", selectedCount),
            leftPos + 216,
            topPos + 73,
            0xFF555555,
            false
        );
    }

    private void drawBrowserSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFF373737);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
    }

    private void drawScrollbar(GuiGraphics graphics, int filteredCount) {
        int totalRows = (filteredCount + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        int x = leftPos + SCROLLBAR_X;
        int y = topPos + SCROLLBAR_Y;
        graphics.fill(x, y, x + 7, y + SCROLLBAR_HEIGHT, 0xFF5A5A5A);
        if (maxScroll == 0) {
            graphics.fill(x + 1, y + 1, x + 6, y + SCROLLBAR_HEIGHT - 1, 0xFFD0D0D0);
            return;
        }
        int thumbHeight = Math.max(10, SCROLLBAR_HEIGHT * VISIBLE_ROWS / totalRows);
        int thumbTravel = SCROLLBAR_HEIGHT - thumbHeight;
        int thumbY = y + thumbTravel * scrollRow / maxScroll;
        graphics.fill(x + 1, thumbY, x + 6, thumbY + thumbHeight, 0xFFD0D0D0);
        graphics.fill(x + 2, thumbY + 1, x + 5, thumbY + thumbHeight - 1, 0xFFE8E8E8);
    }

    private Component networkStatus() {
        if (!menu.networkComplete()) {
            return Component.translatable("screen.aura.bookshelf_coordinator.incomplete_network");
        }
        if (menu.connectedShelves() == 0) {
            return Component.translatable("screen.aura.bookshelf_coordinator.disconnected");
        }
        if (menu.availablePower() < menu.requiredPower()) {
            return Component.translatable("screen.aura.bookshelf_coordinator.powerless");
        }
        if (menu.storageShelves() == 0) {
            return Component.translatable("screen.aura.bookshelf_coordinator.no_storage_shelves");
        }
        return Component.translatable("screen.aura.bookshelf_coordinator.ready");
    }

    private String boundedText(Component text, int maxWidth) {
        String resolved = text.getString();
        if (font.width(resolved) <= maxWidth) {
            return resolved;
        }
        String ellipsis = "...";
        return font.plainSubstrByWidth(resolved, maxWidth - font.width(ellipsis)) + ellipsis;
    }

    private boolean isOverNetworkStatus(double mouseX, double mouseY) {
        return mouseX >= leftPos + 8 && mouseX < leftPos + 184
            && mouseY >= topPos + 15 && mouseY < topPos + 30;
    }

    private boolean isOverPowerStatus(double mouseX, double mouseY) {
        return mouseX >= leftPos + 194 && mouseX < leftPos + 344
            && mouseY >= topPos + 15 && mouseY < topPos + 30;
    }

    private Component resultMessage() {
        return switch (menu.resultCode()) {
            case BookshelfCoordinatorNetworking.RESULT_RETRIEVED -> Component.translatable(
                "screen.aura.bookshelf_coordinator.retrieved",
                menu.resultAmount()
            );
            case BookshelfCoordinatorNetworking.RESULT_EMPTY -> Component.translatable("screen.aura.bookshelf_coordinator.empty");
            case BookshelfCoordinatorNetworking.RESULT_POWERLESS -> Component.translatable("screen.aura.bookshelf_coordinator.powerless");
            case BookshelfCoordinatorNetworking.RESULT_DISCONNECTED -> Component.translatable("screen.aura.bookshelf_coordinator.disconnected");
            case BookshelfCoordinatorNetworking.RESULT_INCOMPLETE_NETWORK -> Component.translatable("screen.aura.bookshelf_coordinator.incomplete_network");
            case BookshelfCoordinatorNetworking.RESULT_INVENTORY_FULL -> Component.translatable("screen.aura.bookshelf_coordinator.inventory_full");
            case BookshelfCoordinatorNetworking.RESULT_TYPE_MISSING -> Component.translatable("screen.aura.bookshelf_coordinator.item_missing");
            case BookshelfCoordinatorNetworking.RESULT_INVALID_AMOUNT -> Component.translatable("screen.aura.bookshelf_coordinator.invalid_amount");
            default -> Component.empty();
        };
    }

    private int statusColor() {
        return menu.canRetrieve() ? 0xFF205020 : 0xFF7A2020;
    }

    private int resultColor() {
        return menu.resultCode() == BookshelfCoordinatorNetworking.RESULT_RETRIEVED ? 0xFF205020 : 0xFF7A2020;
    }

    private void select(BookshelfCoordinatorBlockEntity.BrowserEntry entry) {
        selectedStack = entry.stack();
        selectedCount = (int) Math.min(Integer.MAX_VALUE, entry.count());
        setAmount(1);
        updateControls();
    }

    private void refreshSelectionCount() {
        if (selectedStack.isEmpty()) {
            selectedCount = 0;
            return;
        }
        selectedCount = 0;
        for (BookshelfCoordinatorBlockEntity.BrowserEntry entry : menu.entries()) {
            if (ItemStack.isSameItemSameComponents(selectedStack, entry.stack())) {
                selectedCount = (int) Math.min(Integer.MAX_VALUE, entry.count());
                return;
            }
        }
        selectedStack = ItemStack.EMPTY;
    }

    private void retainVisibleSelection() {
        if (selectedStack.isEmpty()) {
            return;
        }
        for (BookshelfCoordinatorBlockEntity.BrowserEntry entry : filteredEntries()) {
            if (ItemStack.isSameItemSameComponents(selectedStack, entry.stack())) {
                return;
            }
        }
        selectedStack = ItemStack.EMPTY;
        selectedCount = 0;
    }

    private void retrieveSelected() {
        if (selectedStack.isEmpty() || !menu.canRetrieve()) {
            return;
        }
        int amount = parseAmount();
        if (amount <= 0) {
            return;
        }
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
            new BookshelfCoordinatorNetworking.ExtractRequest(menu.containerId, selectedStack, amount)
        );
    }

    private void adjustAmount(int delta) {
        int current = parseAmount();
        long adjusted = (long) current + delta;
        setAmount(Math.max(1L, Math.min(Integer.MAX_VALUE, adjusted)));
    }

    private void setAmount(long amount) {
        if (amountBox != null) {
            amountBox.setValue(Long.toString(Math.max(1L, Math.min(Integer.MAX_VALUE, amount))));
        }
        updateControls();
    }

    private int parseAmount() {
        try {
            return Integer.parseInt(amountBox.getValue());
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void updateControls() {
        if (retrieveButton == null) {
            return;
        }
        boolean hasSelection = !selectedStack.isEmpty() && selectedCount > 0;
        int amount = amountBox == null ? 0 : parseAmount();
        retrieveButton.active = hasSelection && menu.canRetrieve() && amount > 0;
        minusButton.active = hasSelection && amount > 1;
        plusButton.active = hasSelection && amount < Integer.MAX_VALUE;
        maxButton.active = hasSelection && selectedCount > 0;
    }

    private List<BookshelfCoordinatorBlockEntity.BrowserEntry> filteredEntries() {
        if (filteredDirty) {
            String filter = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
            ArrayList<BookshelfCoordinatorBlockEntity.BrowserEntry> result = new ArrayList<>();
            for (BookshelfCoordinatorBlockEntity.BrowserEntry entry : menu.entries()) {
                if (filter.isEmpty() || entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT).contains(filter)) {
                    result.add(entry);
                }
            }
            result.sort(Comparator
                .comparing((BookshelfCoordinatorBlockEntity.BrowserEntry entry) -> entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString()));
            filteredEntries = List.copyOf(result);
            filteredDirty = false;
        }
        return filteredEntries;
    }

    private List<BookshelfCoordinatorBlockEntity.BrowserEntry> visibleEntries() {
        List<BookshelfCoordinatorBlockEntity.BrowserEntry> filtered = filteredEntries();
        int start = scrollRow * COLUMNS;
        int end = Math.min(filtered.size(), start + COLUMNS * VISIBLE_ROWS);
        return start >= end ? List.of() : filtered.subList(start, end);
    }

    private BookshelfCoordinatorBlockEntity.BrowserEntry hoveredEntry(int mouseX, int mouseY) {
        if (!isInBrowserGrid(mouseX, mouseY)) {
            return null;
        }
        int index = gridIndex(mouseX, mouseY);
        List<BookshelfCoordinatorBlockEntity.BrowserEntry> visible = visibleEntries();
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    private boolean isInBrowserGrid(double mouseX, double mouseY) {
        return mouseX >= leftPos + GRID_X
            && mouseX < leftPos + GRID_X + COLUMNS * SLOT_SIZE
            && mouseY >= topPos + GRID_Y
            && mouseY < topPos + GRID_Y + VISIBLE_ROWS * SLOT_SIZE;
    }

    private int gridIndex(double mouseX, double mouseY) {
        int column = (int) (mouseX - leftPos - GRID_X) / SLOT_SIZE;
        int row = (int) (mouseY - topPos - GRID_Y) / SLOT_SIZE;
        return row * COLUMNS + column;
    }

    private boolean isInScrollbar(double mouseX, double mouseY) {
        return mouseX >= leftPos + SCROLLBAR_X
            && mouseX < leftPos + SCROLLBAR_X + 8
            && mouseY >= topPos + SCROLLBAR_Y
            && mouseY < topPos + SCROLLBAR_Y + SCROLLBAR_HEIGHT;
    }

    private void setScrollFromMouse(double mouseY) {
        int totalRows = (filteredEntries().size() + COLUMNS - 1) / COLUMNS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (maxScroll == 0) {
            scrollRow = 0;
            return;
        }
        double position = Math.max(0.0D, Math.min(SCROLLBAR_HEIGHT - 1.0D, mouseY - topPos - SCROLLBAR_Y));
        scrollRow = (int) Math.round(position * maxScroll / (SCROLLBAR_HEIGHT - 1.0D));
        clampScroll();
    }

    private void scrollRows(int amount) {
        scrollRow += amount;
        clampScroll();
    }

    private void clampScroll() {
        int totalRows = (filteredEntries().size() + COLUMNS - 1) / COLUMNS;
        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, totalRows - VISIBLE_ROWS)));
    }

    private String compactCount(long count) {
        if (count < 1_000L) {
            return Long.toString(count);
        }
        if (count < 1_000_000L) {
            return Long.toString(count / 1_000L) + "k";
        }
        return Long.toString(count / 1_000_000L) + "m";
    }
}
