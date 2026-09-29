package pixlepix.auracascade.qa.neoforge.multiplayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.block.menu.BookshelfCoordinatorMenu;
import pixlepix.auracascade.compat.AuraAccessoryMenu;
import pixlepix.auracascade.compat.AuraAccessoryNetworking;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.network.BookshelfCoordinatorNetworking;

final class MultiplayerClientProbe {
    private MultiplayerEvidence evidence;
    private int ticks;
    private int renderedFrames;
    private int lastCommand;
    private int acknowledged;
    private String capture;
    private String lastScreenshot;
    private JsonObject lastUse;
    private JsonObject lastAction;
    private boolean stopped;

    static void bootstrap(IEventBus modBus) {
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            MultiplayerClientProbe probe = new MultiplayerClientProbe();
            NeoForge.EVENT_BUS.addListener(probe::tick);
            NeoForge.EVENT_BUS.addListener(probe::render);
        }));
    }

    private void tick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (stopped) {
            client.stop();
            return;
        }
        try {
            if (evidence == null) evidence = new MultiplayerEvidence();
            if (evidence.timedOut()) throw new IllegalStateException("Client probe deadline exceeded");
            if (evidence.stopRequested()) {
                stopped = true;
                return;
            }
            boolean loaded = client.level != null && client.player != null && client.getSingleplayerServer() == null;
            if (loaded && Files.exists(evidence.output.resolve("command.json"))) {
                JsonObject command = JsonParser.parseString(Files.readString(evidence.output.resolve("command.json"))).getAsJsonObject();
                int sequence = command.get("sequence").getAsInt();
                if (sequence > lastCommand) {
                    action(client, command);
                    lastCommand = sequence;
                    if (capture == null) acknowledged = sequence;
                }
            }
            var players = loaded ? new ArrayList<>(client.level.players()) : new ArrayList<net.minecraft.client.player.AbstractClientPlayer>();
            // Dead local players leave the world list before their death screen closes.
            if (loaded && !players.contains(client.player)) players.add(client.player);
            JsonObject report = loaded
                ? evidence.snapshot(players, client.level.entitiesForRendering(), ++ticks)
                : evidence.snapshot(List.of(), List.of(), ++ticks);
            report.addProperty("worldLoaded", loaded);
            report.addProperty("singleplayer", client.getSingleplayerServer() != null);
            report.addProperty("acknowledged", acknowledged);
            report.add("lastAction", lastAction);
            report.add("lastUse", lastUse);
            report.addProperty("renderedFrames", renderedFrames);
            report.addProperty("screenshot", lastScreenshot);
            report.addProperty("localPlayer", client.player == null ? "" : client.player.getGameProfile().getName());
            report.addProperty("accessoryMenu", loaded && client.player.containerMenu instanceof AuraAccessoryMenu);
            report.addProperty("bookshelfMenu", loaded && client.player.containerMenu instanceof BookshelfCoordinatorMenu);
            if (ticks % 20 == 0) evidence.publish(report);
        } catch (Exception error) {
            MultiplayerEvidence.failure(error);
            stopped = true;
        }
    }

    private void action(Minecraft client, JsonObject command) {
        String action = command.get("action").getAsString();
        int sequence = command.get("sequence").getAsInt();
        lastAction = new JsonObject();
        lastAction.addProperty("sequence", sequence);
        lastAction.addProperty("action", action);
        if (action.equals("capture")) {
            client.player.closeContainer();
            client.setScreen(null);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            capture = "capture-" + sequence + ".png";
            renderedFrames = 0;
            return;
        }
        if (!"AuraOwnerQA".equals(client.player.getGameProfile().getName())) {
            throw new IllegalStateException("Only the fixture owner may issue gameplay actions");
        }
        switch (action) {
            case "open" -> PacketDistributor.sendToServer(new AuraAccessoryNetworking.OpenRequest());
            case "equip", "reequip" -> {
                AuraAccessoryMenu menu = accessoryMenu(client);
                int source = -1;
                for (int index = 4; index < menu.slots.size(); index++) {
                    if (menu.getSlot(index).getItem().is(AuraItems.RING_OF_BINDING)) {
                        source = index;
                        break;
                    }
                }
                if (source < 0) throw new IllegalStateException("No real ring in menu inventory");
                client.gameMode.handleInventoryMouseClick(menu.containerId, source, 0, ClickType.QUICK_MOVE, client.player);
                lastAction.addProperty("menuSlot", source);
            }
            case "unequip" -> {
                AuraAccessoryMenu menu = accessoryMenu(client);
                int source = -1;
                for (int index = 1; index <= 2; index++) {
                    if (menu.getSlot(index).getItem().is(AuraItems.RING_OF_BINDING)) {
                        source = index;
                        break;
                    }
                }
                if (source < 0) throw new IllegalStateException("No equipped ring in the real menu");
                client.gameMode.handleInventoryMouseClick(menu.containerId, source, 0, ClickType.QUICK_MOVE, client.player);
                lastAction.addProperty("menuSlot", source);
            }
            case "bind" -> {
                client.player.closeContainer();
                client.setScreen(null);
                if (!client.player.getInventory().getItem(1).is(AuraItems.FAIRY_CHARM)) {
                    throw new IllegalStateException("Fixture charm missing from hotbar slot 1");
                }
                client.player.getInventory().selected = 1;
                lastUse = new JsonObject();
                lastUse.addProperty("sequence", sequence);
                lastUse.addProperty("hand", "MAIN_HAND");
                lastUse.addProperty("selectedHotbarSlot", 1);
                lastUse.addProperty("item", BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem()).toString());
                lastUse.addProperty("count", client.player.getMainHandItem().getCount());
                client.player.connection.send(new ServerboundSetCarriedItemPacket(1));
                lastUse.addProperty("result", client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).name());
            }
            case "respawn" -> {
                if (client.player.isAlive()) throw new IllegalStateException("Respawn requires a dead owner");
                client.player.connection.send(new ServerboundClientCommandPacket(
                    ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            }
            case "bookshelfOpen" -> {
                client.player.closeContainer();
                client.setScreen(null);
                BlockPos pos = new BlockPos(command.get("x").getAsInt(), command.get("y").getAsInt(),
                    command.get("z").getAsInt());
                if (!client.level.getBlockState(pos).is(AuraContent.BOOKSHELF_COORDINATOR)) {
                    throw new IllegalStateException("Fixture coordinator is not present at " + pos);
                }
                if (client.player.distanceToSqr(Vec3.atCenterOf(pos)) >= 64.0D) {
                    throw new IllegalStateException("Fixture coordinator is out of menu range");
                }
                if (!client.player.getMainHandItem().isEmpty()) {
                    throw new IllegalStateException("Use an empty selected hand to open the real bookshelf menu");
                }
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
                lastAction.addProperty("interactionResult",
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit).name());
                lastAction.addProperty("blockPos", pos.toShortString());
            }
            case "bookshelfRetrieve" -> {
                if (!(client.player.containerMenu instanceof BookshelfCoordinatorMenu menu)) {
                    throw new IllegalStateException("Real bookshelf menu is not open");
                }
                int entryIndex = command.get("entryIndex").getAsInt();
                int count = command.get("count").getAsInt();
                if (entryIndex < 0 || entryIndex >= menu.entries().size() || count <= 0) {
                    throw new IllegalArgumentException("Invalid bookshelf entry or count");
                }
                var entry = menu.entries().get(entryIndex);
                PacketDistributor.sendToServer(new BookshelfCoordinatorNetworking.ExtractRequest(
                    menu.containerId, entry.stack(), count));
                lastAction.addProperty("containerId", menu.containerId);
                lastAction.addProperty("entryIndex", entryIndex);
                lastAction.addProperty("item", BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString());
                lastAction.addProperty("requestedCount", count);
            }
            default -> throw new IllegalArgumentException("Unknown fixture action " + action);
        }
    }

    private static AuraAccessoryMenu accessoryMenu(Minecraft client) {
        if (!(client.player.containerMenu instanceof AuraAccessoryMenu menu)) {
            throw new IllegalStateException("Real accessory menu is not open");
        }
        return menu;
    }

    private void render(RenderFrameEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (stopped || evidence == null || client.level == null || client.player == null || client.screen != null) return;
        renderedFrames++;
        if (capture == null || renderedFrames < 40) return;
        try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) {
                throw new IllegalStateException("Empty screenshot");
            }
            image.writeToFile(evidence.output.resolve(capture));
            lastScreenshot = capture;
            capture = null;
            acknowledged = lastCommand;
        } catch (Exception error) {
            MultiplayerEvidence.failure(error);
            stopped = true;
        }
    }
}
