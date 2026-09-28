package pixlepix.auracascade.support;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import pixlepix.auracascade.item.AuraDiscoverability;

public final class TestMinecraftBootstrap {
    private static final ResourceLocation AURA_NODE = ResourceLocation.fromNamespaceAndPath("aura", "aura_node");

    private TestMinecraftBootstrap() {
    }

    public static void ensureBootstrapped() {
        if (!BuiltInRegistries.BLOCK.containsKey(AURA_NODE)) {
            throw new IllegalStateException("Aura is not loaded in the NeoForge JUnit runtime. Enable neoForge.unitTest with testedMod = mods.aura.");
        }
    }

    public static AuraRegistrationSnapshot auraRegistrationSnapshot() {
        ensureBootstrapped();
        return new AuraRegistrationSnapshot(
            registryNamespacePaths(BuiltInRegistries.BLOCK),
            registryNamespacePaths(BuiltInRegistries.ITEM)
        );
    }

    public static AuraDiscoverabilitySnapshot auraDiscoverabilitySnapshot() {
        ensureBootstrapped();
        return new AuraDiscoverabilitySnapshot(
            registryNamespacePaths(BuiltInRegistries.CREATIVE_MODE_TAB),
            Set.copyOf(AuraDiscoverability.discoverableRegistryPaths())
        );
    }

    public static AuraItemDescriptionSnapshot auraItemDescriptionSnapshot() {
        ensureBootstrapped();
        Map<String, String> descriptionIds = new TreeMap<>();
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if ("aura".equals(id.getNamespace())) {
                descriptionIds.put(id.getPath(), BuiltInRegistries.ITEM.get(id).getDescriptionId());
            }
        }
        return new AuraItemDescriptionSnapshot(Map.copyOf(descriptionIds));
    }

    private static Set<String> registryNamespacePaths(Registry<?> registry) {
        return registry.keySet().stream()
            .filter(id -> "aura".equals(id.getNamespace()))
            .map(ResourceLocation::getPath)
            .collect(Collectors.toUnmodifiableSet());
    }

    public record AuraRegistrationSnapshot(Set<String> blockIds, Set<String> itemIds) {
    }

    public record AuraDiscoverabilitySnapshot(Set<String> creativeTabIds, Set<String> discoverableItemIds) {
    }

    public record AuraItemDescriptionSnapshot(Map<String, String> descriptionIds) {
    }
}
