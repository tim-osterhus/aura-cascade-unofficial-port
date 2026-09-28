package pixlepix.auracascade.qa.multiplayer;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import pixlepix.auracascade.block.AuraContent;
import pixlepix.auracascade.compat.AuraFluxBridgeRegistry;
import team.reborn.energy.api.EnergyStorage;

/** Opt-in interoperability check against the published Trash Cans energy receiver. */
public final class ExternalEnergyProbe implements ModInitializer {
    @Override
    public void onInitialize() {
        if (!Boolean.getBoolean("aura.qa.externalEnergy")) return;
        Path output = Path.of(System.getProperty("aura.qa.externalEnergy.output"));
        if (Files.exists(output)) throw new IllegalStateException("Use a fresh energy report path");
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            JsonObject report = new JsonObject();
            report.addProperty("success", false);
            var level = server.overworld();
            BlockPos source = new BlockPos(8, 100, 8);
            BlockPos receiver = source.east();
            level.getChunkAt(source);
            var oldSource = level.getBlockState(source);
            var oldReceiver = level.getBlockState(receiver);
            try {
                require(oldSource.isAir() && oldReceiver.isAir(), "Disposable fixture positions must be empty");
                require(FabricLoader.getInstance().isModLoaded("trashcans"), "External mod missing");
                var block = BuiltInRegistries.BLOCK.getValue(Identifier.parse("trashcans:energy_trash_can"));
                require(block != null && block != net.minecraft.world.level.block.Blocks.AIR, "Real receiver not registered");
                level.setBlockAndUpdate(source, AuraContent.AURA_NODE_FLUX.defaultBlockState());
                level.setBlockAndUpdate(receiver, block.defaultBlockState());
                var storage = EnergyStorage.SIDED.find(level, receiver, Direction.WEST);
                require(storage != null && storage.supportsInsertion(), "External sided storage unavailable");
                report.addProperty("receiverClass", storage.getClass().getName());
                require(storage.getClass().getName().startsWith("com.supermartijn642.trashcans."), "Not the external receiver");
                int accepted = AuraFluxBridgeRegistry.export(level, source, 100);
                report.addProperty("powerConsumedWithReceiver", accepted);
                require(accepted == 100, "External receiver did not accept the expected 1500 energy / 100 power");
                level.removeBlock(receiver, false);
                int disconnected = AuraFluxBridgeRegistry.export(level, source, 100);
                report.addProperty("powerConsumedDisconnected", disconnected);
                require(disconnected == 0, "Disconnected export consumed power");
                require(AuraFluxBridgeRegistry.export(level, source, 0) == 0, "Empty export generated power");
                report.addProperty("success", true);
                report.addProperty("scope", "Actual published external receiver through Aura bridge; not a fueled progression test");
            } catch (Exception | AssertionError failure) {
                report.addProperty("error", failure.toString());
            } finally {
                if (oldSource.isAir() && oldReceiver.isAir()) {
                    level.setBlockAndUpdate(source, oldSource);
                    level.setBlockAndUpdate(receiver, oldReceiver);
                }
            }
            try {
                Files.createDirectories(output.toAbsolutePath().getParent());
                Files.writeString(output, new GsonBuilder().setPrettyPrinting().create().toJson(report),
                    StandardOpenOption.CREATE_NEW);
            } catch (Exception failure) {
                throw new IllegalStateException("Cannot write energy evidence", failure);
            }
        });
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
