package pixlepix.auracascade.compat.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import pixlepix.auracascade.compat.AuraAccessoryMenu;

public final class AuraAccessoryScreen extends AbstractContainerScreen<AuraAccessoryMenu> {
    public AuraAccessoryScreen(AuraAccessoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        titleLabelX = 8;
        titleLabelY = 6;
        inventoryLabelX = 8;
        inventoryLabelY = 71;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF292929);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + imageHeight - 1, 0xFFC6C6C6);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + 18, 0xFFE2E2E2);
        for (int index = 0; index < 4; index++) {
            drawSlot(graphics, leftPos + 34 + index * 35, topPos + 34);
            String slotType = index == 0 ? "amulet" : index == 3 ? "belt" : "ring";
            Component label = Component.translatable("screen.aura.accessory.slot." + slotType);
            graphics.drawString(font, label, leftPos + 43 + index * 35 - font.width(label) / 2,
                topPos + 55, 0xFF404040, false);
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawSlot(graphics, leftPos + 7 + column * 18, topPos + 83 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawSlot(graphics, leftPos + 7 + column * 18, topPos + 141);
        }
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF777777);
        graphics.fill(x + 1, y + 1, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF393939);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
