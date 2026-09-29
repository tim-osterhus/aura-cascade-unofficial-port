package pixlepix.auracascade.qa.neoforge.energy;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(EnergyInteropQaMod.MOD_ID)
public final class EnergyInteropQaMod {
    public static final String MOD_ID = "aura_qa_energy";
    static final Logger LOGGER = LoggerFactory.getLogger("Aura Energy QA");
    static EnergyReceiverBlock RECEIVER_BLOCK;
    static BlockEntityType<EnergyReceiverBlockEntity> RECEIVER_BLOCK_ENTITY;

    public EnergyInteropQaMod(IEventBus modBus) {
        modBus.addListener(EnergyInteropQaMod::registerContent);
        modBus.addListener(EnergyInteropQaMod::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(EnergyInteropQaCommand::register);
        NeoForge.EVENT_BUS.addListener(EnergyInteropRuntimeFixture::onLevelTickPre);
        NeoForge.EVENT_BUS.addListener(EnergyInteropRuntimeFixture::onLevelTickPost);
    }

    private static void registerContent(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            RECEIVER_BLOCK = new EnergyReceiverBlock();
            Registry.register(BuiltInRegistries.BLOCK, id("energy_receiver"), RECEIVER_BLOCK);
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            RECEIVER_BLOCK_ENTITY = Registry.register(
                BuiltInRegistries.BLOCK_ENTITY_TYPE,
                id("energy_receiver"),
                BlockEntityType.Builder.of(EnergyReceiverBlockEntity::new, RECEIVER_BLOCK).build(null)
            );
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            RECEIVER_BLOCK_ENTITY,
            EnergyReceiverBlockEntity::getEnergyCapability
        );
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
