package pixlepix.auracascade.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import pixlepix.auracascade.item.AuraAccessoryItem;

public final class AuraAccessoryInventory {
    public static final int SLOT_COUNT = 4;
    public static final int AMULET = 0;
    public static final int FIRST_RING = 1;
    public static final int SECOND_RING = 2;
    public static final int BELT = 3;

    private static final Codec<Loadout> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC.fieldOf("amulet").forGetter(Loadout::amulet),
        ItemStack.OPTIONAL_CODEC.fieldOf("ring1").forGetter(Loadout::ring1),
        ItemStack.OPTIONAL_CODEC.fieldOf("ring2").forGetter(Loadout::ring2),
        ItemStack.OPTIONAL_CODEC.fieldOf("belt").forGetter(Loadout::belt)
    ).apply(instance, Loadout::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, Loadout> SYNC_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, Loadout::amulet,
        ItemStack.OPTIONAL_STREAM_CODEC, Loadout::ring1,
        ItemStack.OPTIONAL_STREAM_CODEC, Loadout::ring2,
        ItemStack.OPTIONAL_STREAM_CODEC, Loadout::belt,
        Loadout::new
    );
    private static final AttachmentType<Loadout> ATTACHMENT = AttachmentRegistry.create(
        ResourceLocation.fromNamespaceAndPath("aura", "accessories"),
        builder -> builder.persistent(CODEC).copyOnDeath().syncWith(SYNC_CODEC, AttachmentSyncPredicate.targetOnly())
    );

    private AuraAccessoryInventory() {
    }

    public static void bootstrap() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer player)
                || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                return;
            }
            Loadout old = ((AttachmentTarget) player).getAttached(ATTACHMENT);
            if (old == null) {
                return;
            }
            ((AttachmentTarget) player).setAttached(ATTACHMENT, Loadout.EMPTY);
            for (int index = 0; index < SLOT_COUNT; index++) {
                ItemStack stack = old.get(index);
                if (!stack.isEmpty()) {
                    player.drop(stack.copy(), false);
                }
            }
        });
    }

    public static AuraAccessorySlot slotType(int index) {
        return switch (index) {
            case AMULET -> AuraAccessorySlot.AMULET;
            case FIRST_RING, SECOND_RING -> AuraAccessorySlot.RING;
            case BELT -> AuraAccessorySlot.BELT;
            default -> throw new IndexOutOfBoundsException(index);
        };
    }

    public static boolean accepts(int index, ItemStack stack) {
        return stack.getItem() instanceof AuraAccessoryItem item && item.slot() == slotType(index);
    }

    public static int firstEmpty(List<ItemStack> stacks, AuraAccessorySlot type) {
        if (stacks.size() != SLOT_COUNT) {
            throw new IllegalArgumentException("Expected four accessory slots");
        }
        for (int index = 0; index < SLOT_COUNT; index++) {
            if (slotType(index) == type && stacks.get(index).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    public static ItemStack get(Player player, int index) {
        Loadout loadout = ((AttachmentTarget) player).getAttached(ATTACHMENT);
        return loadout == null ? ItemStack.EMPTY : loadout.get(index);
    }

    public static List<ItemStack> equipped(Player player, AuraAccessorySlot type) {
        ArrayList<ItemStack> result = new ArrayList<>(2);
        for (int index = 0; index < SLOT_COUNT; index++) {
            if (slotType(index) == type && !get(player, index).isEmpty()) {
                result.add(get(player, index));
            }
        }
        return List.copyOf(result);
    }

    public static int firstEmpty(Player player, AuraAccessorySlot type) {
        return firstEmpty(snapshot(player), type);
    }

    public static void set(Player player, int index, ItemStack stack) {
        if (!stack.isEmpty() && !accepts(index, stack)) {
            throw new IllegalArgumentException("Accessory does not fit slot " + index);
        }
        Loadout current = ((AttachmentTarget) player).getAttached(ATTACHMENT);
        ((AttachmentTarget) player).setAttached(ATTACHMENT, (current == null ? Loadout.EMPTY : current).with(index, stack));
    }

    // Call after changing components on an equipped stack, such as a binding ring.
    public static void touch(Player player) {
        Loadout current = ((AttachmentTarget) player).getAttached(ATTACHMENT);
        if (current != null) {
            ((AttachmentTarget) player).setAttached(ATTACHMENT, current.copy());
        }
    }

    public static List<ItemStack> snapshot(Player player) {
        Loadout current = ((AttachmentTarget) player).getAttached(ATTACHMENT);
        return (current == null ? Loadout.EMPTY : current).snapshot();
    }

    private record Loadout(ItemStack amulet, ItemStack ring1, ItemStack ring2, ItemStack belt) {
        private static final Loadout EMPTY = new Loadout(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

        private ItemStack get(int index) {
            return switch (index) {
                case AMULET -> amulet;
                case FIRST_RING -> ring1;
                case SECOND_RING -> ring2;
                case BELT -> belt;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }

        private List<ItemStack> snapshot() {
            return List.of(amulet.copy(), ring1.copy(), ring2.copy(), belt.copy());
        }

        private Loadout copy() {
            return new Loadout(amulet.copy(), ring1.copy(), ring2.copy(), belt.copy());
        }

        private Loadout with(int index, ItemStack stack) {
            ItemStack copy = stack.copy();
            return switch (index) {
                case AMULET -> new Loadout(copy, ring1.copy(), ring2.copy(), belt.copy());
                case FIRST_RING -> new Loadout(amulet.copy(), copy, ring2.copy(), belt.copy());
                case SECOND_RING -> new Loadout(amulet.copy(), ring1.copy(), copy, belt.copy());
                case BELT -> new Loadout(amulet.copy(), ring1.copy(), ring2.copy(), copy);
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    }
}
