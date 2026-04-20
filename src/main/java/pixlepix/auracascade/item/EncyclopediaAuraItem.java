package pixlepix.auracascade.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.Level;
import pixlepix.auracascade.lexicon.EncyclopediaAuraContent;

public final class EncyclopediaAuraItem extends WrittenBookItem {
    public EncyclopediaAuraItem() {
        this(new Item.Properties());
    }

    public EncyclopediaAuraItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            stack.set(DataComponents.WRITTEN_BOOK_CONTENT, EncyclopediaAuraContent.createBook(player));
        }
        return super.use(level, player, hand);
    }
}
