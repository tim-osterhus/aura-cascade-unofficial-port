package pixlepix.auracascade.compat;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import pixlepix.auracascade.item.AuraItems;

public final class AuraAccessoryNetworking {
    public static final CustomPacketPayload.Type<OpenRequest> OPEN_TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath("aura", "open_accessories")
    );
    public static final CustomPacketPayload.Type<WingRequest> WING_TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath("aura", "activate_wing")
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, OpenRequest> OPEN_CODEC = StreamCodec.of(
        (buffer, payload) -> {}, buffer -> new OpenRequest()
    );
    private static final StreamCodec<RegistryFriendlyByteBuf, WingRequest> WING_CODEC = StreamCodec.of(
        (buffer, payload) -> {}, buffer -> new WingRequest()
    );
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, "aura");
    private static final Supplier<MenuType<AuraAccessoryMenu>> MENU_TYPE = MENUS.register(
        "accessories", () -> new MenuType<>(AuraAccessoryMenu::new, FeatureFlags.VANILLA_SET)
    );
    private static Consumer<ServerPlayer> wingAction = player -> {};

    private AuraAccessoryNetworking() {
    }

    public static void bootstrapCommon(IEventBus modBus) {
        AuraAccessoryInventory.bootstrap(modBus);
        MENUS.register(modBus);
        modBus.addListener(AuraAccessoryNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(OPEN_TYPE, OPEN_CODEC, AuraAccessoryNetworking::receiveOpen);
        registrar.playToServer(WING_TYPE, WING_CODEC, AuraAccessoryNetworking::receiveWing);
        registrar.playToClient(AuraAccessoryInventory.SyncPayload.TYPE,
            AuraAccessoryInventory.SyncPayload.STREAM_CODEC,
            (payload, context) -> AuraAccessoryInventory.applySync(context.player(), payload));
    }

    private static void receiveOpen(OpenRequest payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && canUse(player)) {
            player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, viewer) -> new AuraAccessoryMenu(containerId, inventory),
                Component.translatable("menu.aura.accessories")
            ));
        }
    }

    private static void receiveWing(WingRequest payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player && canUse(player)
            && AuraAccessoryInventory.equipped(player, AuraAccessorySlot.AMULET).stream()
                .anyMatch(stack -> stack.is(AuraItems.AMULET_OF_THE_ANGELS_WING))) {
            wingAction.accept(player);
        }
    }

    public static void registerWingAction(Consumer<ServerPlayer> action) {
        wingAction = Objects.requireNonNull(action);
    }

    public static MenuType<AuraAccessoryMenu> menuType() {
        return MENU_TYPE.get();
    }

    private static boolean canUse(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator();
    }

    public record OpenRequest() implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return OPEN_TYPE;
        }
    }

    public record WingRequest() implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return WING_TYPE;
        }
    }
}
