package pixlepix.auracascade.lexicon;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.WrittenBookContent;
import org.jetbrains.annotations.Nullable;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.enchantment.KaleidoscopicLegacyContract;
import pixlepix.auracascade.fairy.FairyRole;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.parity.AuraColor;

public final class EncyclopediaAuraContent {
    private EncyclopediaAuraContent() {
    }

    public static WrittenBookContent createBook(@Nullable Player player) {
        OnboardingProgress progress = onboarding(player);
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(page(
            "Encyclopedia Aura\n\n"
                + "Learn the flow of the eight aura colors, build pumps and consumers, and carry that power into storage, fairies, and late-game work.\n\n"
                + checklist(progress)
        ));
        pages.add(page(
            "Aura Basics\n\n"
                + "Aura Nodes store colored aura and exchange it with linked machines. White is steady, black moves only vertically, orange drives current, red rises from blasts, "
                + "yellow surges upward, green shifts with time, blue favors rain, and violet grows dangerously fast."
        ));
        pages.add(page(
            "Pumps And Control\n\n"
                + "Burning, illumination, momentum, projectile, and redstone pumps feed nearby nodes. Alternating pumps swap behavior on redstone timing. "
                + "Manipulators force black or orange flow, capacitors burst at thresholds, conserving nodes hold charge, and the monitor reads nearby aura strength."
        ));
        pages.add(page(
            "Consumers And Vortex\n\n"
                + "Processors, smelters, growers, fishers, brewers, colorers, the synthesizer, and the enchanter consume stored aura directly from adjacent network blocks. "
                + "The Vortex Controller and Pedestals perform infusion recipes that unlock prism and late progression."
        ));
        pages.add(page(
            "Storage Network\n\n"
                + "Storage Books keep the original tradeoff between how many item types they hold and how deep each slot can stack. "
                + "Everything with an item form stays in the Aura creative tab, while the Storage Bookshelf itself remains a conversion-only block. "
                + "Use a Storage Book on a vanilla bookshelf to create a Storage Bookshelf. A Bookshelf Coordinator reads the connected shelf cluster, needs adjacent aura power, "
                + "and only reaches shelves with a clear line of sight."
        ));
        pages.add(page(
            "Gear And Utility\n\n"
                + "Arcane ingots, gems, prisms, and Angel's Steel tools drive the equipment line. The Prismatic Wand keeps Selection, Copy, and Paste, but copies non-air block "
                + "states only, skips block entities and fluids, and pastes by consuming placed block items directly."
        ));
        pages.add(page(
            "Fairies\n\n"
                + "The Ring of Binding stores up to fifteen Fairy Charms. An untuned charm binds a plain "
                + FairyRole.BASIC.displayName()
                + " with no active role. Attuned charms keep the shipped names: "
                + fairyRoleSummary()
                + "."
        ));
        pages.add(page(
            "Enchantments\n\n"
                + "The Kaleidoscopic Enchanter keeps the shipped 75%-per-step success curve. Legacy single-color effects were "
                + KaleidoscopicLegacyContract.legacyBasicEffectSummary()
                + ". The current runtime keeps a bounded substitution set: "
                + KaleidoscopicLegacyContract.implementedRuntimeSummary()
                + "."
        ));
        pages.add(page(
            "Enchant Matrix\n\n"
                + "Recovered pairings: "
                + KaleidoscopicLegacyContract.legacyInteractionSummary()
                + ". Those pairwise behaviors are documented from the jar but are not reproduced in the current runtime."
        ));
        pages.add(page(
            "Late Systems\n\n"
                + "Traveler's Bricks, Rebounding Enigma, fortified blocks, the Cascading Miner, looter, spawner, and the Nether and End rituals are all part of the current port. "
                + "Rituals and fairy runtime use modern substitutions where the old engine APIs no longer exist, but the destructive progression arc remains in place. The Ring of the "
                + "Shattered Stone still protects the wearer from nearby blasts, but it does not yet restrict explosion damage to dirt, stone, sand, and gravel."
        ));
        return new WrittenBookContent(
            Filterable.passThrough("Encyclopedia Aura"),
            "pixlepix / unofficial modern port",
            0,
            pages,
            true
        );
    }

    static OnboardingProgress onboarding(@Nullable Player player) {
        if (player == null) {
            return new OnboardingProgress(false, false, false, false, false, false);
        }

        Inventory inventory = player.getInventory();
        return new OnboardingProgress(
            hasItem(inventory, AuraItems.crystal(AuraColor.WHITE)),
            hasItem(inventory, AuraContent.AURA_NODE.asItem()),
            hasItem(inventory, AuraContent.AURA_NODE_PUMP.asItem()),
            hasItem(inventory, AuraContent.CONSUMER_BLOCK_ORE.asItem()),
            hasItem(inventory, AuraContent.VORTEX_CONTROLLER.asItem()) || hasItem(inventory, AuraItems.ARCANE_PRISM),
            hasItem(inventory, AuraItems.RING_OF_BINDING) || hasItem(inventory, AuraItems.FAIRY_CHARM)
        );
    }

    private static boolean hasItem(Inventory inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static String checklist(OnboardingProgress progress) {
        return line(progress.hasCrystal(), "Carry an Aura Crystal")
            + line(progress.hasNode(), "Place an Aura Node")
            + line(progress.hasPump(), "Run an Aura Pump")
            + line(progress.hasConsumer(), "Power a consumer")
            + line(progress.hasVortex(), "Reach vortex work")
            + line(progress.hasFairy(), "Bind a fairy");
    }

    private static String line(boolean complete, String text) {
        return (complete ? "[x] " : "[ ] ") + text + "\n";
    }

    private static String fairyRoleSummary() {
        return String.join(
            ", ",
            FairyRole.FIGHTER.displayName(),
            FairyRole.DEBUFFER.displayName(),
            FairyRole.BUFFER.displayName(),
            FairyRole.STEALER.displayName(),
            FairyRole.PUSHER.displayName(),
            FairyRole.SHOOTER.displayName(),
            FairyRole.SAVIOR.displayName(),
            FairyRole.FETCHER.displayName(),
            FairyRole.BAITER.displayName(),
            FairyRole.BREEDER.displayName(),
            FairyRole.SCARER.displayName(),
            FairyRole.EXTINGUISHER.displayName(),
            FairyRole.DIGGER.displayName(),
            FairyRole.LIGHTER.displayName(),
            FairyRole.GLIDER.displayName(),
            FairyRole.TRAINER.displayName()
        );
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }

    record OnboardingProgress(
        boolean hasCrystal,
        boolean hasNode,
        boolean hasPump,
        boolean hasConsumer,
        boolean hasVortex,
        boolean hasFairy
    ) {
    }
}
