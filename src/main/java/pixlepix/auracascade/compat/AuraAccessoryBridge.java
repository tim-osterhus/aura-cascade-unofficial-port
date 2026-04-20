package pixlepix.auracascade.compat;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface AuraAccessoryBridge {
    List<ItemStack> equipped(Player player, AuraAccessorySlot slot);

    boolean isEquipped(Player player, ItemStack stack, AuraAccessorySlot slot);

    void equip(Player player, ItemStack stack, AuraAccessorySlot slot);

    void unequip(Player player, ItemStack stack, AuraAccessorySlot slot);
}
