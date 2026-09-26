package pixlepix.auracascade.compat;

import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
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
    private static MenuType<AuraAccessoryMenu> menuType;
    private static Consumer<ServerPlayer> wingAction = player -> {};

    private AuraAccessoryNetworking() {
    }

    public static void bootstrapCommon() {
        if (menuType != null) {
            return;
        }
        AuraAccessoryInventory.bootstrap();
        menuType = Registry.register(
            BuiltInRegistries.MENU,
            ResourceLocation.fromNamespaceAndPath("aura", "accessories"),
            new MenuType<>(AuraAccessoryMenu::new, FeatureFlags.VANILLA_SET)
        );
        PayloadTypeRegistry.playC2S().register(OPEN_TYPE, OPEN_CODEC);
        PayloadTypeRegistry.playC2S().register(WING_TYPE, WING_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(OPEN_TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (canUse(player)) {
                player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, viewer) -> new AuraAccessoryMenu(containerId, inventory),
                    Component.translatable("menu.aura.accessories")
                ));
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(WING_TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (canUse(player) && AuraAccessoryInventory.equipped(player, AuraAccessorySlot.AMULET).stream()
                .anyMatch(stack -> stack.is(AuraItems.AMULET_OF_THE_ANGELS_WING))) {
                wingAction.accept(player);
            }
        });
    }

    public static void registerWingAction(Consumer<ServerPlayer> action) {
        wingAction = Objects.requireNonNull(action);
    }

    public static MenuType<AuraAccessoryMenu> menuType() {
        if (menuType == null) {
            throw new IllegalStateException("Register accessory networking before opening its menu");
        }
        return menuType;
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
