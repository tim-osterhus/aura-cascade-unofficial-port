package pixlepix.auracascade.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.block.entity.AuraNodeBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpBlockEntity;
import pixlepix.auracascade.block.entity.AuraPumpLogic;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AuraNodePumpGeometryTest {
    private static final double EPSILON = 1.0e-9;
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
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void nodeAndPumpSelectionAndCollisionShapesMatchTheirBlockModels() throws IOException {
        Block node = AuraContent.AURA_NODE;
        Block pump = AuraContent.AURA_NODE_PUMP;
        Bounds nodeSelection = selectionBounds(node);
        Bounds nodeCollision = collisionBounds(node);
        Bounds pumpSelection = selectionBounds(pump);
        Bounds pumpCollision = collisionBounds(pump);
        assertFalse(node.defaultBlockState().canOcclude(), "Aura Node should not occlude neighboring faces");
        assertFalse(pump.defaultBlockState().canOcclude(), "Aura Pump should not occlude neighboring faces");

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
    void vortexPedestalMatchesItsSmallModelWithoutOccludingNeighbors() throws IOException {
        Block pedestal = AuraContent.VORTEX_PEDESTAL;
        Bounds model = inheritedModelBounds("aura:block/aura_node_crafting_pedestal", new HashSet<>());
        assertShapeBounds(selectionBounds(pedestal), model, "pedestal selection");
        assertShapeBounds(collisionBounds(pedestal), model, "pedestal collision");
        assertFalse(pedestal.defaultBlockState().canOcclude());
    }

    private static Bounds shapeBounds(VoxelShape shape) {
        AABB bounds = shape.bounds();
        return new Bounds(
            bounds.minX, bounds.minY, bounds.minZ,
            bounds.maxX, bounds.maxY, bounds.maxZ,
            shape.toAabbs().size()
        );
    }

    private static Bounds selectionBounds(Block block) {
        return shapeBounds(block.defaultBlockState().getShape(
            EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()
        ));
    }

    private static Bounds collisionBounds(Block block) {
        return shapeBounds(block.defaultBlockState().getCollisionShape(
            EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()
        ));
    }

    @Test
    void traversalBlocksKeepLegacyContactHeightAndFullLight() {
        Bounds expected = new Bounds(0, 0, 0, 1, 0.8D, 1, 1);
        for (var entry : Map.<String, Block>of(
            "TRAVELERS_BRICKS", AuraContent.TRAVELERS_BRICKS,
            "REBOUNDING_ENIGMA", AuraContent.REBOUNDING_ENIGMA
        ).entrySet()) {
            Block block = entry.getValue();
            String field = entry.getKey();
            assertShapeBounds(selectionBounds(block), expected, field + " selection");
            assertShapeBounds(collisionBounds(block), expected, field + " collision");
            assertFalse(block.defaultBlockState().canOcclude(), field);
            assertEquals(15, block.defaultBlockState().getLightEmission(), field);
        }
    }

    @Test
    void nodesAndPumpsExposeComparatorReadoutWithoutPoweringNeighbors() {
        for (var entry : Map.<String, Block>of(
            "AURA_NODE", AuraContent.AURA_NODE,
            "AURA_NODE_PUMP", AuraContent.AURA_NODE_PUMP,
            "VORTEX_CONTROLLER", AuraContent.VORTEX_CONTROLLER,
            "VORTEX_PEDESTAL", AuraContent.VORTEX_PEDESTAL,
            "CONSUMER_BLOCK_ORE", AuraContent.CONSUMER_BLOCK_ORE
        ).entrySet()) {
            BlockState state = entry.getValue().defaultBlockState();
            assertFalse(state.isSignalSource(), entry.getKey());
            assertTrue(state.hasAnalogOutputSignal(), entry.getKey());
        }
    }

    @Test
    void capacitorThresholdControlCyclesAllFourRegisteredValues() {
        AuraNodeBlockEntity capacitor = (AuraNodeBlockEntity) AuraContent.AURA_NODE_CAPACITOR.newBlockEntity(
            BlockPos.ZERO, AuraContent.AURA_NODE_CAPACITOR.defaultBlockState()
        );
        assertNotNull(capacitor);
        assertEquals(1_000, capacitor.capacitorThreshold());
        for (int expected : new int[] {10_000, 100_000, 100, 1_000}) {
            assertEquals(expected, capacitor.cycleCapacitorThreshold());
            assertEquals(expected, capacitor.capacitorThreshold());
        }
    }

    @Test
    void emptyHandStorageInteractionsFallThroughToOpeningOrWithdrawal() throws ReflectiveOperationException {
        for (var entry : Map.<String, Block>of(
            "BOOKSHELF_COORDINATOR", AuraContent.BOOKSHELF_COORDINATOR,
            "STORAGE_BOOKSHELF", AuraContent.STORAGE_BOOKSHELF
        ).entrySet()) {
            Method use = entry.getValue().getClass().getDeclaredMethod("useItemOn",
                ItemStack.class, BlockState.class, Level.class, BlockPos.class,
                Player.class, InteractionHand.class, BlockHitResult.class);
            use.setAccessible(true);
            Object result = use.invoke(entry.getValue(), ItemStack.EMPTY, null, null, null, null, null, null);
            assertEquals(ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION, result, entry.getKey());
        }
    }

    @Test
    void monitorReportsFirstEligiblePumpStatusAndExcludesOutputNeighbor() throws ReflectiveOperationException {
        AuraPumpBlockEntity empty = (AuraPumpBlockEntity) AuraContent.AURA_NODE_PUMP.newBlockEntity(
            new BlockPos(0, -1, 0), AuraContent.AURA_NODE_PUMP.defaultBlockState()
        );
        AuraPumpBlockEntity fueled = (AuraPumpBlockEntity) AuraContent.AURA_NODE_PUMP.newBlockEntity(
            new BlockPos(0, 1, 0), AuraContent.AURA_NODE_PUMP.defaultBlockState()
        );
        assertNotNull(empty);
        assertNotNull(fueled);
        var fuelField = AuraPumpBlockEntity.class.getDeclaredField("pumpState");
        fuelField.setAccessible(true);
        fuelField.set(fueled, new AuraPumpLogic.PumpState(20, 300));
        BlockGetter getter = (BlockGetter) Proxy.newProxyInstance(
            BlockGetter.class.getClassLoader(), new Class<?>[] {BlockGetter.class}, (proxy, method, args) -> {
                if (!method.getName().equals("getBlockEntity")) {
                    throw new UnsupportedOperationException(method.getName());
                }
                int y = ((BlockPos) args[0]).getY();
                return y == -1 ? empty : y == 1 ? fueled : null;
            }
        );
        assertEquals(15, AuraContent.MONITOR.getSignal(null, getter, BlockPos.ZERO, Direction.NORTH),
            "empty DOWN pump wins over fueled UP pump");
        assertEquals(0, AuraContent.MONITOR.getSignal(null, getter, BlockPos.ZERO, Direction.UP),
            "output's opposite neighbor is excluded");
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
