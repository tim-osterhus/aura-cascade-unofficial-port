package pixlepix.auracascade.item;

import net.minecraft.world.item.Item;
import pixlepix.auracascade.compat.AuraAccessorySlot;

public class AuraAccessoryItem extends Item {
    private final AuraAccessorySlot slot;

    public AuraAccessoryItem(AuraAccessorySlot slot) {
        this(slot, 1, new Item.Properties());
    }

    public AuraAccessoryItem(AuraAccessorySlot slot, int stackSize) {
        this(slot, stackSize, new Item.Properties());
    }

    public AuraAccessoryItem(AuraAccessorySlot slot, Item.Properties properties) {
        this(slot, 1, properties);
    }

    public AuraAccessoryItem(AuraAccessorySlot slot, int stackSize, Item.Properties properties) {
        super(properties.stacksTo(stackSize));
        this.slot = slot;
    }

    public AuraAccessorySlot slot() {
        return slot;
    }
}
