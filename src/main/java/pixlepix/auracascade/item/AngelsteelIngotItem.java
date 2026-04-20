package pixlepix.auracascade.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AngelsteelIngotItem extends Item {
    private final int degreeIndex;

    public AngelsteelIngotItem(int degreeIndex) {
        this(degreeIndex, new Item.Properties());
    }

    public AngelsteelIngotItem(int degreeIndex, Item.Properties properties) {
        super(properties);
        this.degreeIndex = AngelsteelToolHelper.clampDegree(degreeIndex);
    }

    public int degreeIndex() {
        return degreeIndex;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.literal("Angel's Steel of the " + ordinalName() + " degree");
    }

    private String ordinalName() {
        return switch (degreeIndex) {
            case 0 -> "first";
            case 1 -> "second";
            case 2 -> "third";
            case 3 -> "fourth";
            case 4 -> "fifth";
            case 5 -> "sixth";
            case 6 -> "seventh";
            case 7 -> "eighth";
            case 8 -> "ninth";
            case 9 -> "tenth";
            case 10 -> "eleventh";
            default -> "twelfth";
        };
    }
}
