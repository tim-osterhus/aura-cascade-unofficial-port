package pixlepix.auracascade.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraNodePumpGeometryTest {
    private static final double EPSILON = 1.0e-9;
    private static ClassLoader auraTargetLoader;
    private static final String[] BLOCK_IDS = {
        "aura_node",
        "aura_node_black",
        "aura_node_capacitor",
        "aura_node_conserve",
        "aura_node_flux",
        "aura_node_orange",
        "aura_node_pump",
        "aura_node_pump_alt",
        "aura_node_pump_creative",
        "aura_node_pump_fall",
        "aura_node_pump_fall_alt",
        "aura_node_pump_light",
        "aura_node_pump_light_alt",
        "aura_node_pump_projectile",
        "aura_node_pump_projectile_alt",
        "aura_node_pump_redstone",
        "aura_node_pump_redstone_alt"
    };

    @BeforeAll
    static void bootstrapMinecraft() {
        auraTargetLoader = loadRegisteredAuraContent();
    }

    private static ClassLoader loadRegisteredAuraContent() {
        try {
            ClassLoader loader = fabricTargetLoader();
            Class.forName("net.minecraft.SharedConstants", true, loader).getMethod("tryDetectVersion").invoke(null);
            Class.forName("net.minecraft.server.Bootstrap", true, loader).getMethod("bootStrap").invoke(null);
            unfreezeTargetAuraRegistries(loader);
            Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", true, loader);
            Method registerTypes = content.getDeclaredMethod("registerBlockEntityTypes");
            registerTypes.setAccessible(true);
            registerTypes.invoke(null);
            Class.forName("pixlepix.auracascade.item.AuraItems", true, loader);
            return loader;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to load registered Aura blocks in the Fabric target loader.", exception);
        }
    }

    private static ClassLoader fabricTargetLoader() throws ReflectiveOperationException {
        Method method = TestMinecraftBootstrap.class.getDeclaredMethod("fabricTargetClassLoader");
        method.setAccessible(true);
        return (ClassLoader) method.invoke(null);
    }

    private static void unfreezeTargetAuraRegistries(ClassLoader loader) throws ReflectiveOperationException {
        Method method = TestMinecraftBootstrap.class.getDeclaredMethod("unfreezeAuraRegistries", ClassLoader.class);
        method.setAccessible(true);
        method.invoke(null, loader);
    }

    @Test
    void nodeAndPumpSelectionAndCollisionShapesMatchTheirBlockModels() throws IOException, ReflectiveOperationException {
        Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader);
        Object node = content.getField("AURA_NODE").get(null);
        Object pump = content.getField("AURA_NODE_PUMP").get(null);
        Bounds nodeSelection = registeredShapeBounds(node, "getShape");
        Bounds nodeCollision = registeredShapeBounds(node, "getCollisionShape");
        Bounds pumpSelection = registeredShapeBounds(pump, "getShape");
        Bounds pumpCollision = registeredShapeBounds(pump, "getCollisionShape");
        assertFalse(registeredBlockCanOcclude(node), "Aura Node should not occlude neighboring faces");
        assertFalse(registeredBlockCanOcclude(pump), "Aura Pump should not occlude neighboring faces");

        for (String blockId : BLOCK_IDS) {
            JsonObject blockstate = readJson("/assets/aura/blockstates/" + blockId + ".json");
            JsonObject variants = blockstate.getAsJsonObject("variants");
            assertNotNull(variants, blockId + " blockstate variants");
            assertEquals(1, variants.size(), blockId + " should have one geometry variant");

            String modelId = variants.getAsJsonObject("").get("model").getAsString();
            Bounds modelBounds = inheritedModelBounds(modelId, new HashSet<>());
            assertShapeBounds(nodeSelection, modelBounds, blockId + " node selection");
            assertShapeBounds(nodeCollision, modelBounds, blockId + " node collision");
            assertShapeBounds(pumpSelection, modelBounds, blockId + " pump selection");
            assertShapeBounds(pumpCollision, modelBounds, blockId + " pump collision");
        }
    }

    @Test
    void vortexPedestalMatchesItsSmallModelWithoutOccludingNeighbors() throws IOException, ReflectiveOperationException {
        Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader);
        Object pedestal = content.getField("VORTEX_PEDESTAL").get(null);
        Bounds model = inheritedModelBounds("aura:block/aura_node_crafting_pedestal", new HashSet<>());
        assertShapeBounds(registeredShapeBounds(pedestal, "getShape"), model, "pedestal selection");
        assertShapeBounds(registeredShapeBounds(pedestal, "getCollisionShape"), model, "pedestal collision");
        assertFalse(registeredBlockCanOcclude(pedestal));
    }

    private static Bounds registeredShapeBounds(Object block, String shapeMethod)
        throws ReflectiveOperationException {
        Class<?>[] parameterTypes = {
            Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader),
            Class.forName("net.minecraft.world.level.BlockGetter", false, auraTargetLoader),
            Class.forName("net.minecraft.core.BlockPos", false, auraTargetLoader),
            Class.forName("net.minecraft.world.phys.shapes.CollisionContext", false, auraTargetLoader)
        };
        Object shape = block.getClass().getMethod(shapeMethod, parameterTypes).invoke(block, null, null, null, null);
        Class<?> voxelShapeClass = Class.forName("net.minecraft.world.phys.shapes.VoxelShape", false, auraTargetLoader);
        Object bounds = voxelShapeClass.getMethod("bounds").invoke(shape);
        int cuboidCount = ((List<?>) voxelShapeClass.getMethod("toAabbs").invoke(shape)).size();
        Class<?> aabbClass = Class.forName("net.minecraft.world.phys.AABB", false, auraTargetLoader);
        return new Bounds(
            aabbClass.getField("minX").getDouble(bounds),
            aabbClass.getField("minY").getDouble(bounds),
            aabbClass.getField("minZ").getDouble(bounds),
            aabbClass.getField("maxX").getDouble(bounds),
            aabbClass.getField("maxY").getDouble(bounds),
            aabbClass.getField("maxZ").getDouble(bounds),
            cuboidCount
        );
    }

    @Test
    void traversalBlocksKeepLegacyContactHeightAndFullLight() throws ReflectiveOperationException {
        Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader);
        Class<?> stateClass = Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader);
        Bounds expected = new Bounds(0, 0, 0, 1, 0.8D, 1, 1);
        for (String field : List.of("TRAVELERS_BRICKS", "REBOUNDING_ENIGMA")) {
            Object block = content.getField(field).get(null);
            assertShapeBounds(registeredShapeBounds(block, "getShape"), expected, field + " selection");
            assertShapeBounds(registeredShapeBounds(block, "getCollisionShape"), expected, field + " collision");
            assertFalse(registeredBlockCanOcclude(block), field);
            Object state = block.getClass().getMethod("defaultBlockState").invoke(block);
            assertEquals(15, stateClass.getMethod("getLightEmission").invoke(state), field);
        }
    }

    private static boolean registeredBlockCanOcclude(Object block) throws ReflectiveOperationException {
        Object defaultState = block.getClass().getMethod("defaultBlockState").invoke(block);
        Class<?> blockStateClass = Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader);
        return (boolean) blockStateClass.getMethod("canOcclude").invoke(defaultState);
    }

    @Test
    void nodesAndPumpsExposeComparatorReadoutWithoutPoweringNeighbors() throws ReflectiveOperationException {
        Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader);
        Class<?> stateClass = Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader);
        for (String field : List.of("AURA_NODE", "AURA_NODE_PUMP", "VORTEX_CONTROLLER", "VORTEX_PEDESTAL", "CONSUMER_BLOCK_ORE")) {
            Object block = content.getField(field).get(null);
            Object state = block.getClass().getMethod("defaultBlockState").invoke(block);
            assertFalse((boolean) stateClass.getMethod("isSignalSource").invoke(state), field);
            assertTrue((boolean) stateClass.getMethod("hasAnalogOutputSignal").invoke(state), field);
        }
    }

    @Test
    void capacitorThresholdControlCyclesAllFourRegisteredValues() throws ReflectiveOperationException {
        Object capacitor = registeredBlockEntity("AURA_NODE_CAPACITOR", 0, 0, 0);
        Method threshold = capacitor.getClass().getMethod("capacitorThreshold");
        Method cycle = capacitor.getClass().getMethod("cycleCapacitorThreshold");
        assertEquals(1_000, threshold.invoke(capacitor));
        for (int expected : new int[] {10_000, 100_000, 100, 1_000}) {
            assertEquals(expected, cycle.invoke(capacitor));
            assertEquals(expected, threshold.invoke(capacitor));
        }
    }

    @Test
    void emptyHandStorageInteractionsFallThroughToOpeningOrWithdrawal() throws ReflectiveOperationException {
        Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader);
        Class<?> stackClass = Class.forName("net.minecraft.world.item.ItemStack", false, auraTargetLoader);
        Class<?> stateClass = Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader);
        Class<?> levelClass = Class.forName("net.minecraft.world.level.Level", false, auraTargetLoader);
        Class<?> posClass = Class.forName("net.minecraft.core.BlockPos", false, auraTargetLoader);
        Class<?> playerClass = Class.forName("net.minecraft.world.entity.player.Player", false, auraTargetLoader);
        Class<?> handClass = Class.forName("net.minecraft.world.InteractionHand", false, auraTargetLoader);
        Class<?> hitClass = Class.forName("net.minecraft.world.phys.BlockHitResult", false, auraTargetLoader);
        for (String field : List.of("BOOKSHELF_COORDINATOR", "STORAGE_BOOKSHELF")) {
            Object block = content.getField(field).get(null);
            Method use = block.getClass().getDeclaredMethod("useItemOn", stackClass, stateClass, levelClass, posClass, playerClass, handClass, hitClass);
            use.setAccessible(true);
            Object result = use.invoke(block, stackClass.getField("EMPTY").get(null), null, null, null, null, null, null);
            assertEquals("PASS_TO_DEFAULT_BLOCK_INTERACTION", result.toString(), field);
        }
    }

    @Test
    void monitorReportsFirstEligiblePumpStatusAndExcludesOutputNeighbor() throws ReflectiveOperationException {
        Class<?> posClass = Class.forName("net.minecraft.core.BlockPos", false, auraTargetLoader);
        Class<?> directionClass = Class.forName("net.minecraft.core.Direction", false, auraTargetLoader);
        Class<?> stateClass = Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader);
        Class<?> getterClass = Class.forName("net.minecraft.world.level.BlockGetter", false, auraTargetLoader);
        Object origin = posClass.getConstructor(int.class, int.class, int.class).newInstance(0, 0, 0);
        Object empty = registeredBlockEntity("AURA_NODE_PUMP", 0, -1, 0);
        Object fueled = registeredBlockEntity("AURA_NODE_PUMP", 0, 1, 0);
        Class<?> fuelClass = Class.forName("pixlepix.auracascade.block.entity.AuraPumpLogic$PumpState", false, auraTargetLoader);
        var fuelField = fueled.getClass().getDeclaredField("pumpState");
        fuelField.setAccessible(true);
        fuelField.set(fueled, fuelClass.getConstructor(int.class, int.class).newInstance(20, 300));
        Object getter = java.lang.reflect.Proxy.newProxyInstance(auraTargetLoader, new Class<?>[] {getterClass}, (proxy, method, args) -> {
            if (!method.getName().equals("getBlockEntity")) {
                throw new UnsupportedOperationException(method.getName());
            }
            int y = (int) posClass.getMethod("getY").invoke(args[0]);
            return y == -1 ? empty : y == 1 ? fueled : null;
        });
        Object monitor = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader).getField("MONITOR").get(null);
        Method signal = monitor.getClass().getMethod("getSignal", stateClass, getterClass, posClass, directionClass);
        assertEquals(15, signal.invoke(monitor, null, getter, origin, directionClass.getField("NORTH").get(null)), "empty DOWN pump wins over fueled UP pump");
        assertEquals(0, signal.invoke(monitor, null, getter, origin, directionClass.getField("UP").get(null)), "output's opposite neighbor is excluded");
    }

    private static Object registeredBlockEntity(String field, int x, int y, int z) throws ReflectiveOperationException {
        Class<?> content = Class.forName("pixlepix.auracascade.block.AuraContent", false, auraTargetLoader);
        Class<?> posClass = Class.forName("net.minecraft.core.BlockPos", false, auraTargetLoader);
        Class<?> stateClass = Class.forName("net.minecraft.world.level.block.state.BlockState", false, auraTargetLoader);
        Object block = content.getField(field).get(null);
        Object state = block.getClass().getMethod("defaultBlockState").invoke(block);
        Object pos = posClass.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
        return block.getClass().getMethod("newBlockEntity", posClass, stateClass).invoke(block, pos, state);
    }

    private static Bounds inheritedModelBounds(String modelId, Set<String> visited) throws IOException {
        assertTrue(visited.add(modelId), "model parent cycle at " + modelId);
        String[] identifier = modelId.split(":", 2);
        assertEquals(2, identifier.length, "namespaced model id");
        JsonObject model = readJson("/assets/" + identifier[0] + "/models/" + identifier[1] + ".json");

        if (model.has("elements")) {
            JsonArray elements = model.getAsJsonArray("elements");
            assertEquals(1, elements.size(), modelId + " element count");
            JsonObject element = elements.get(0).getAsJsonObject();
            assertFalse(element.has("rotation"), modelId + " element rotation");
            JsonArray from = element.getAsJsonArray("from");
            JsonArray to = element.getAsJsonArray("to");
            return new Bounds(
                coordinate(from, 0),
                coordinate(from, 1),
                coordinate(from, 2),
                coordinate(to, 0),
                coordinate(to, 1),
                coordinate(to, 2),
                1
            );
        }

        assertTrue(model.has("parent"), modelId + " must define elements or inherit them");
        return inheritedModelBounds(model.get("parent").getAsString(), visited);
    }

    private static double coordinate(JsonArray coordinates, int axis) {
        return coordinates.get(axis).getAsDouble() / 16.0;
    }

    private static JsonObject readJson(String resourcePath) throws IOException {
        try (InputStream stream = AuraNodePumpGeometryTest.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "missing resource " + resourcePath);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static void assertShapeBounds(Bounds actual, Bounds expected, String label) {
        assertEquals(1, actual.cuboidCount(), label + " cuboid count");
        assertEquals(expected.minX(), actual.minX(), EPSILON, label + " minX");
        assertEquals(expected.minY(), actual.minY(), EPSILON, label + " minY");
        assertEquals(expected.minZ(), actual.minZ(), EPSILON, label + " minZ");
        assertEquals(expected.maxX(), actual.maxX(), EPSILON, label + " maxX");
        assertEquals(expected.maxY(), actual.maxY(), EPSILON, label + " maxY");
        assertEquals(expected.maxZ(), actual.maxZ(), EPSILON, label + " maxZ");
    }

    private record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int cuboidCount) {
    }
}
