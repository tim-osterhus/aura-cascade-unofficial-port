package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.nio.file.Files;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import pixlepix.auracascade.compat.AuraAccessoryMenu;
import pixlepix.auracascade.compat.AuraAccessoryNetworking;
import pixlepix.auracascade.item.AuraItems;

public final class MultiplayerClientProbe implements ClientModInitializer {
    private MultiplayerEvidence evidence;
    private int ticks;
    private int renderedFrames;
    private int lastCommand;
    private int acknowledged;
    private String capture;
    private String lastScreenshot;
    private JsonObject lastUse;
    private boolean stopped;

    @Override
    public void onInitializeClient() {
        if (System.getProperty("aura.qa.multiplayer.output") == null) return;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudRenderCallback.EVENT.register((graphics, delta) -> render(Minecraft.getInstance()));
    }

    private void tick(Minecraft client) {
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
                    lastCommand = sequence;
                    action(client, command.get("action").getAsString());
                    if (capture == null) acknowledged = sequence;
                }
            }
            JsonObject report = loaded
                ? evidence.snapshot(client.level.players(), client.level.entitiesForRendering(), ++ticks)
                : evidence.snapshot(List.of(), List.of(), ++ticks);
            report.addProperty("worldLoaded", loaded);
            report.addProperty("singleplayer", client.getSingleplayerServer() != null);
            report.addProperty("acknowledged", acknowledged);
            report.add("lastUse", lastUse);
            report.addProperty("renderedFrames", renderedFrames);
            report.addProperty("screenshot", lastScreenshot);
            report.addProperty("localPlayer", client.player == null ? "" : client.player.getGameProfile().getName());
            report.addProperty("accessoryMenu", loaded && client.player.containerMenu instanceof AuraAccessoryMenu);
            if (ticks % 20 == 0) evidence.publish(report);
        } catch (Exception error) {
            MultiplayerEvidence.failure(error);
            stopped = true;
        }
    }

    private void action(Minecraft client, String action) {
        if (action.equals("capture")) {
            client.player.closeContainer();
            client.setScreen(null);
            client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            capture = "capture-" + lastCommand + ".png";
            renderedFrames = 0;
            return;
        }
        if (!"AuraOwnerQA".equals(client.player.getGameProfile().getName())) {
            throw new IllegalStateException("Only the fixture owner may issue equip/use actions");
        }
        switch (action) {
            case "open" -> ClientPlayNetworking.send(new AuraAccessoryNetworking.OpenRequest());
            case "equip" -> {
                if (!(client.player.containerMenu instanceof AuraAccessoryMenu menu)) {
                    throw new IllegalStateException("Real accessory menu is not open");
                }
                int source = -1;
                for (int index = 4; index < menu.slots.size(); index++) {
                    if (menu.getSlot(index).getItem().is(AuraItems.RING_OF_BINDING)) {
                        source = index;
                        break;
                    }
                }
                if (source < 0) throw new IllegalStateException("No real ring in menu inventory");
                client.gameMode.handleInventoryMouseClick(menu.containerId, source, 0, ClickType.QUICK_MOVE, client.player);
            }
            case "bind" -> {
                client.player.closeContainer();
                client.setScreen(null);
                if (!client.player.getInventory().getItem(1).is(AuraItems.FAIRY_CHARM)) {
                    throw new IllegalStateException("Fixture charm missing from hotbar slot 1");
                }
                client.player.getInventory().selected = 1;
                lastUse = new JsonObject();
                lastUse.addProperty("sequence", lastCommand);
                lastUse.addProperty("hand", "MAIN_HAND");
                lastUse.addProperty("selectedHotbarSlot", client.player.getInventory().selected);
                lastUse.addProperty("item", BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem()).toString());
                lastUse.addProperty("count", client.player.getMainHandItem().getCount());
                // Fabric's successful UseItemCallback cancels before vanilla selected-slot sync.
                client.player.connection.send(new ServerboundSetCarriedItemPacket(1));
                lastUse.addProperty("result", client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND).name());
            }
            default -> throw new IllegalArgumentException("Unknown fixture action " + action);
        }
    }

    private void render(Minecraft client) {
        if (stopped || evidence == null || client.level == null || client.player == null || client.screen != null) return;
        renderedFrames++;
        if (capture == null || renderedFrames < 40) return;
        try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            if (image == null || image.getWidth() < 1 || image.getHeight() < 1) throw new IllegalStateException("Empty screenshot");
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
