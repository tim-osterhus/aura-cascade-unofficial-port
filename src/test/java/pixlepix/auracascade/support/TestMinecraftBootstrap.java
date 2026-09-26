package pixlepix.auracascade.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.impl.launch.knot.Knot;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public final class TestMinecraftBootstrap {
    private static volatile boolean bootstrapped;
    private static volatile AuraRegistrationSnapshot auraRegistrationSnapshot;
    private static volatile AuraDiscoverabilitySnapshot auraDiscoverabilitySnapshot;
    private static volatile AuraItemDescriptionSnapshot auraItemDescriptionSnapshot;

    private TestMinecraftBootstrap() {
    }

    public static void ensureBootstrapped() {
        if (!bootstrapped) {
            synchronized (TestMinecraftBootstrap.class) {
                if (!bootstrapped) {
                    SharedConstants.tryDetectVersion();
                    Bootstrap.bootStrap();
                    bootstrapped = true;
                }
            }
        }
    }

    public static AuraRegistrationSnapshot auraRegistrationSnapshot() {
        AuraRegistrationSnapshot snapshot = auraRegistrationSnapshot;
        if (snapshot == null) {
            synchronized (TestMinecraftBootstrap.class) {
                snapshot = auraRegistrationSnapshot;
                if (snapshot == null) {
                    snapshot = loadAuraRegistrationSnapshot();
                    auraRegistrationSnapshot = snapshot;
                }
            }
        }
        return snapshot;
    }

    public static AuraDiscoverabilitySnapshot auraDiscoverabilitySnapshot() {
        AuraDiscoverabilitySnapshot snapshot = auraDiscoverabilitySnapshot;
        if (snapshot == null) {
            synchronized (TestMinecraftBootstrap.class) {
                snapshot = auraDiscoverabilitySnapshot;
                if (snapshot == null) {
                    snapshot = loadAuraDiscoverabilitySnapshot();
                    auraDiscoverabilitySnapshot = snapshot;
                }
            }
        }
        return snapshot;
    }

    public static AuraItemDescriptionSnapshot auraItemDescriptionSnapshot() {
        AuraItemDescriptionSnapshot snapshot = auraItemDescriptionSnapshot;
        if (snapshot == null) {
            synchronized (TestMinecraftBootstrap.class) {
                snapshot = auraItemDescriptionSnapshot;
                if (snapshot == null) {
                    snapshot = loadAuraItemDescriptionSnapshot();
                    auraItemDescriptionSnapshot = snapshot;
                }
            }
        }
        return snapshot;
    }

    private static AuraRegistrationSnapshot loadAuraRegistrationSnapshot() {
        try {
            ClassLoader loader = fabricTargetClassLoader();
            invokeStatic(loader, "net.minecraft.SharedConstants", "tryDetectVersion");
            invokeStatic(loader, "net.minecraft.server.Bootstrap", "bootStrap");
            unfreezeAuraRegistries(loader);
            Class.forName("pixlepix.auracascade.block.AuraContent", true, loader);
            Class.forName("pixlepix.auracascade.item.AuraItems", true, loader);
            return new AuraRegistrationSnapshot(
                registryNamespacePaths(loader, "BLOCK"),
                registryNamespacePaths(loader, "ITEM")
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load Aura registrations through the Fabric test runtime.", exception);
        }
    }

    private static AuraDiscoverabilitySnapshot loadAuraDiscoverabilitySnapshot() {
        try {
            ClassLoader loader = fabricTargetClassLoader();
            invokeStatic(loader, "net.minecraft.SharedConstants", "tryDetectVersion");
            invokeStatic(loader, "net.minecraft.server.Bootstrap", "bootStrap");
            unfreezeAuraRegistries(loader);
            Class.forName("pixlepix.auracascade.block.AuraContent", true, loader);
            Class.forName("pixlepix.auracascade.item.AuraItems", true, loader);
            Class<?> discoverability = Class.forName("pixlepix.auracascade.item.AuraDiscoverability", true, loader);
            Method discoverableRegistryPaths = discoverability.getMethod("discoverableRegistryPaths");
            @SuppressWarnings("unchecked")
            java.util.List<String> discoverablePaths = (java.util.List<String>) invoke(discoverableRegistryPaths, null);
            return new AuraDiscoverabilitySnapshot(
                registryNamespacePaths(loader, "CREATIVE_MODE_TAB"),
                Set.copyOf(discoverablePaths)
            );
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load Aura discoverability through the Fabric test runtime.", exception);
        }
    }

    private static AuraItemDescriptionSnapshot loadAuraItemDescriptionSnapshot() {
        try {
            ClassLoader loader = fabricTargetClassLoader();
            invokeStatic(loader, "net.minecraft.SharedConstants", "tryDetectVersion");
            invokeStatic(loader, "net.minecraft.server.Bootstrap", "bootStrap");
            unfreezeAuraRegistries(loader);
            Class.forName("pixlepix.auracascade.block.AuraContent", true, loader);
            Class.forName("pixlepix.auracascade.item.AuraItems", true, loader);

            Object itemRegistry = registryField(loader, "ITEM");
            Method keySetMethod = itemRegistry.getClass().getMethod("keySet");
            Class<?> resourceLocationClass = Class.forName("net.minecraft.resources.Identifier", true, loader);
            Method getValueMethod = itemRegistry.getClass().getMethod("getValue", resourceLocationClass);
            Map<String, String> descriptionIds = new java.util.TreeMap<>();
            Iterable<?> identifiers = (Iterable<?>) invoke(keySetMethod, itemRegistry);
            for (Object identifier : identifiers) {
                Method getNamespaceMethod = identifier.getClass().getMethod("getNamespace");
                String namespace = (String) invoke(getNamespaceMethod, identifier);
                if (!"aura".equals(namespace)) {
                    continue;
                }

                Method getPathMethod = identifier.getClass().getMethod("getPath");
                String path = (String) invoke(getPathMethod, identifier);
                Object item = invoke(getValueMethod, itemRegistry, identifier);
                Method getDescriptionIdMethod = item.getClass().getMethod("getDescriptionId");
                descriptionIds.put(path, (String) invoke(getDescriptionIdMethod, item));
            }

            return new AuraItemDescriptionSnapshot(Map.copyOf(descriptionIds));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load Aura item description ids through the Fabric test runtime.", exception);
        }
    }

    private static ClassLoader fabricTargetClassLoader() throws ReflectiveOperationException {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null) {
            try {
                Class<?> launcherBase = Class.forName("net.fabricmc.loader.impl.launch.FabricLauncherBase", true, contextLoader);
                Method getLauncherMethod = launcherBase.getMethod("getLauncher");
                Object launcher = invoke(getLauncherMethod, null);
                if (launcher != null) {
                    Method getTargetClassLoaderMethod = launcher.getClass().getMethod("getTargetClassLoader");
                    ClassLoader targetLoader = (ClassLoader) invoke(getTargetClassLoaderMethod, launcher);
                    if (targetLoader != null) {
                        return targetLoader;
                    }
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        Thread thread = Thread.currentThread();
        ClassLoader originalContextLoader = thread.getContextClassLoader();
        try {
            EnvType envType = EnvType.valueOf(System.getProperty("fabric.side", EnvType.CLIENT.name()).toUpperCase(Locale.ROOT));
            Knot knot = new Knot(envType);
            return knot.init(new String[0]);
        } finally {
            thread.setContextClassLoader(originalContextLoader);
        }
    }

    private static void invokeStatic(ClassLoader loader, String className, String methodName) throws ReflectiveOperationException {
        Class<?> clazz = Class.forName(className, true, loader);
        Method method = clazz.getMethod(methodName);
        invoke(method, null);
    }

    private static Set<String> registryNamespacePaths(ClassLoader loader, String registryFieldName) throws ReflectiveOperationException {
        Class<?> builtInRegistries = Class.forName("net.minecraft.core.registries.BuiltInRegistries", true, loader);
        Object registry = builtInRegistries.getField(registryFieldName).get(null);
        Method keySetMethod = registry.getClass().getMethod("keySet");
        Iterable<?> identifiers = (Iterable<?>) invoke(keySetMethod, registry);
        Set<String> paths = new TreeSet<>();

        for (Object identifier : identifiers) {
            Method getNamespaceMethod = identifier.getClass().getMethod("getNamespace");
            String namespace = (String) invoke(getNamespaceMethod, identifier);
            if (!"aura".equals(namespace)) {
                continue;
            }

            Method getPathMethod = identifier.getClass().getMethod("getPath");
            paths.add((String) invoke(getPathMethod, identifier));
        }

        return Set.copyOf(paths);
    }

    private static void unfreezeAuraRegistries(ClassLoader loader) throws ReflectiveOperationException {
        unfreezeRegistry(loader, registryField(loader, "BLOCK"), true);
        unfreezeRegistry(loader, registryField(loader, "ITEM"), true);
        unfreezeRegistry(loader, registryField(loader, "BLOCK_ENTITY_TYPE"), true);
        unfreezeRegistry(loader, registryField(loader, "CREATIVE_MODE_TAB"), false);
        unfreezeRegistry(loader, registryField(loader, "MOB_EFFECT"), false);
    }

    private static Object registryField(ClassLoader loader, String fieldName) throws ReflectiveOperationException {
        Class<?> builtInRegistries = Class.forName("net.minecraft.core.registries.BuiltInRegistries", true, loader);
        return builtInRegistries.getField(fieldName).get(null);
    }

    private static void unfreezeRegistry(ClassLoader loader, Object registry, boolean intrusive) throws ReflectiveOperationException {
        Class<?> mappedRegistry = Class.forName("net.minecraft.core.MappedRegistry", true, loader);
        Field frozenField = mappedRegistry.getDeclaredField("frozen");
        frozenField.setAccessible(true);
        frozenField.setBoolean(registry, false);

        if (intrusive) {
            Field intrusiveHoldersField = mappedRegistry.getDeclaredField("unregisteredIntrusiveHolders");
            intrusiveHoldersField.setAccessible(true);
            if (intrusiveHoldersField.get(registry) == null) {
                intrusiveHoldersField.set(registry, new IdentityHashMap<>());
            }
        }
    }

    private static Object invoke(Method method, Object target, Object... arguments) throws ReflectiveOperationException {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof ReflectiveOperationException reflectiveCause) {
                throw reflectiveCause;
            }
            throw new IllegalStateException("Reflective Fabric runtime invocation failed.", cause);
        }
    }

    public record AuraRegistrationSnapshot(Set<String> blockIds, Set<String> itemIds) {
    }

    public record AuraDiscoverabilitySnapshot(Set<String> creativeTabIds, Set<String> discoverableItemIds) {
    }

    public record AuraItemDescriptionSnapshot(Map<String, String> descriptionIds) {
    }
}
