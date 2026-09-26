package pixlepix.auracascade.item;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

final class ShatteredStoneExplosionIntegrationTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void noWearerReturnsTheLiveVanillaListWithoutLookupMutationOrAllocation() {
        BlockPos stone = new BlockPos(0, 0, 0);
        BlockPos ore = new BlockPos(1, 0, 0);
        List<BlockPos> original = List.of(stone, ore, stone);
        List<BlockPos> affected = new ArrayList<>(original);

        List<BlockPos> filtered = RingOfShatteredStoneRuntime.filterProtectedExplosionBlocks(
            List.of(), affected, pos -> {
                fail("No wearer must not trigger block-state lookup");
                return null;
            }
        );

        assertSame(affected, filtered, "The mixin must recognize this alias before clearing vanilla's list");
        assertEquals(original, affected, "Keep vanilla order and every entry, including duplicates");
    }

    @Test
    void wearerReturnsAnIndependentOrderedListSafeForVanillaShuffle() {
        BlockPos stone = new BlockPos(0, 0, 0);
        BlockPos iron = new BlockPos(1, 0, 0);
        BlockPos diamond = new BlockPos(2, 0, 0);
        BlockPos air = new BlockPos(3, 0, 0);
        List<BlockPos> original = List.of(stone, iron, diamond, air, stone);
        List<BlockPos> affected = new ArrayList<>(original);
        var states = Map.of(
            stone, Blocks.STONE.defaultBlockState(),
            iron, Blocks.IRON_ORE.defaultBlockState(),
            diamond, Blocks.DIAMOND_ORE.defaultBlockState(),
            air, Blocks.AIR.defaultBlockState()
        );

        List<BlockPos> filtered = RingOfShatteredStoneRuntime.filterProtectedExplosionBlocks(
            List.of(Vec3.ZERO), affected, states::get
        );

        assertNotSame(affected, filtered);
        assertEquals(original, affected, "Filtering must not mutate the source list");
        assertEquals(List.of(stone, air, stone), filtered);
        Collections.swap(filtered, 0, 1);
        assertEquals(original, affected, "Vanilla's shuffle must not mutate the source list");
        assertEquals(List.of(air, stone, stone), filtered,
            "Terrain remains destructible while both ores are omitted from the blast list");
    }

    @Test
    void allProtectedBlocksStillProduceAnIndependentEmptyReplacement() {
        List<BlockPos> affected = new ArrayList<>(List.of(BlockPos.ZERO));
        List<BlockPos> filtered = RingOfShatteredStoneRuntime.filterProtectedExplosionBlocks(
            List.of(Vec3.ZERO), affected, pos -> Blocks.DIAMOND_ORE.defaultBlockState()
        );

        assertNotSame(affected, filtered);
        assertTrue(filtered.isEmpty());
        filtered.add(BlockPos.ZERO);
        assertEquals(List.of(BlockPos.ZERO), affected, "The replacement must not mutate vanilla's original list");
    }

    @Test
    void registeredServerExplosionHookReplacesLocalBlockListBeforeInteraction() throws Exception {
        // Structural integration guard, not a claim that this unit loader applies mixins.
        String source = Files.readString(Path.of(
            "src/main/java/pixlepix/auracascade/mixin/ServerExplosionMixin.java"
        )).replaceAll("(?m)//[^\\r\\n]*", "").replaceAll("\\s+", " ");
        assertTrue(source.contains("@Mixin(ServerExplosion.class)"));
        assertTrue(source.contains("@ModifyExpressionValue(method = \"explode\", at = @At(value = \"INVOKE\", target ="));
        assertTrue(source.contains("ServerExplosion;calculateExplodedPositions()Ljava/util/List;"));
        assertTrue(source.contains(
            "return AuraItems.filterShatteredStoneExplosionBlocks(level(), center(), affectedBlocks);"
        ), "The replacement list must feed both block interaction and fire placement");
        assertTrue(!source.contains("clearToBlow"), "The 1.21.11 explosion has no mutable affected-block field");
        var mixins = JsonParser.parseString(Files.readString(Path.of(
            "src/main/resources/aura.mixins.json"
        ))).getAsJsonObject().getAsJsonArray("mixins");
        assertTrue(mixins.asList().stream().anyMatch(entry -> entry.getAsString().equals("ServerExplosionMixin")));
    }
}
