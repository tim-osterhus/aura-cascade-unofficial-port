package pixlepix.auracascade.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import vazkii.patchouli.api.PatchouliAPI;

public final class EncyclopediaAuraItem extends Item {
    private static final Identifier BOOK_ID = Identifier.fromNamespaceAndPath("aura", "encyclopedia_aura");

    public EncyclopediaAuraItem() {
        this(new Item.Properties());
    }

    public EncyclopediaAuraItem(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            pixlepix.auracascade.lexicon.AuraQuestProgress.onBookOpened(serverPlayer);
            PatchouliAPI.get().openBookGUI(serverPlayer, BOOK_ID);
        }
        return InteractionResult.SUCCESS;
    }
}
