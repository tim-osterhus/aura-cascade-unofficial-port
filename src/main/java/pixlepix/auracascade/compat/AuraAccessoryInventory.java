package pixlepix.auracascade.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
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
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, "aura");
    private static final Supplier<AttachmentType<Loadout>> ATTACHMENT = ATTACHMENTS.register(
        "accessories", () -> AttachmentType.builder(() -> Loadout.EMPTY).serialize(CODEC).copyOnDeath().build()
    );

    private AuraAccessoryInventory() {
    }

    public static void bootstrap(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(AuraAccessoryInventory::onDrops);
        NeoForge.EVENT_BUS.addListener(AuraAccessoryInventory::onLogin);
        NeoForge.EVENT_BUS.addListener(AuraAccessoryInventory::onRespawn);
        NeoForge.EVENT_BUS.addListener(AuraAccessoryInventory::onDimensionChange);
    }

    private static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
            || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)
            || !player.hasData(ATTACHMENT)) {
            return;
        }
        Loadout old = player.getData(ATTACHMENT);
        player.setData(ATTACHMENT, Loadout.EMPTY);
        for (ItemStack stack : old.snapshot()) {
            if (!stack.isEmpty()) {
                event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack));
            }
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        sync(event.getEntity());
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        sync(event.getEntity());
    }

    private static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        sync(event.getEntity());
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncPayload(serverPlayer.getData(ATTACHMENT).copy()));
        }
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
        return player.hasData(ATTACHMENT) ? player.getData(ATTACHMENT).get(index) : ItemStack.EMPTY;
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
        player.setData(ATTACHMENT, player.getData(ATTACHMENT).with(index, stack));
        sync(player);
    }

    // Call after changing components on an equipped stack, such as a binding ring.
    public static void touch(Player player) {
        if (player.hasData(ATTACHMENT)) {
            player.setData(ATTACHMENT, player.getData(ATTACHMENT).copy());
            sync(player);
        }
    }

    public static List<ItemStack> snapshot(Player player) {
        return player.hasData(ATTACHMENT) ? player.getData(ATTACHMENT).snapshot() : Loadout.EMPTY.snapshot();
    }

    static void applySync(Player player, SyncPayload payload) {
        player.setData(ATTACHMENT, payload.loadout().copy());
    }

    public record SyncPayload(Loadout loadout) implements CustomPacketPayload {
        public static final Type<SyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("aura", "accessories_sync")
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncPayload> STREAM_CODEC =
            SYNC_CODEC.map(SyncPayload::new, SyncPayload::loadout);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
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
