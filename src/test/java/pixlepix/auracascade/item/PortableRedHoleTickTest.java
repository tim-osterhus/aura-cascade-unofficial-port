package pixlepix.auracascade.item;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PortableRedHoleTickTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void redHoleUsesWorldTimeBoundariesNotItsOwnAge() {
        for (long time : new long[] {0L, 100L, 200L, 123_600L, 123_700L, 3_000_000_000L}) {
            assertTrue(PortableRedHoleItem.shouldErupt(true, time), "World tick " + time);
        }
        for (long time : new long[] {1L, 99L, 101L, 199L, 123_599L, 123_601L}) {
            assertFalse(PortableRedHoleItem.shouldErupt(true, time), "World tick " + time);
        }
    }

    @Test
    void ordinaryItemsNeverEruptEvenAtTheBoundary() {
        for (long time : new long[] {0L, 1L, 99L, 100L, 123_600L}) {
            assertFalse(PortableRedHoleItem.shouldErupt(false, time));
        }
        assertEquals(30_000, PortableRedHoleItem.lifetimeTicks(true, 6_000));
        assertEquals(6_000, PortableRedHoleItem.lifetimeTicks(false, 6_000));
    }

    @Test
    void aFullCadenceContainsNinetyNineNormalItemUpdatesAndOneEruption() {
        int normalUpdates = 0;
        int eruptions = 0;
        for (long time = 123_601L; time <= 123_700L; time++) {
            if (PortableRedHoleItem.shouldErupt(true, time)) {
                eruptions++;
            } else {
                normalUpdates++;
            }
        }
        assertEquals(99, normalUpdates);
        assertEquals(1, eruptions);
    }

    @Test
    void registeredItemTickHookSkipsTheVanillaRemainderOnlyOnEruption() throws Exception {
        // Source integration coverage: this test loader does not apply Aura mixins.
        String mixin = source("mixin/PortableRedHoleLifetimeMixin.java");
        assertTrue(mixin.contains("@Mixin(ItemEntity.class)"));
        assertTrue(mixin.contains("@Inject(method = \"tick\", at = @At(\"HEAD\"), cancellable = true)"));
        assertTrue(mixin.contains("if (PortableRedHoleItem.onEntityItemTick((ItemEntity) (Object) this)) { ci.cancel(); }"));
        assertTrue(mixin.contains("ConsumerItemKeepAlive.effectiveLifetime(item, PortableRedHoleItem.lifetimeTicks(item.getItem(), vanillaLifetime))"));

        var mixins = JsonParser.parseString(Files.readString(Path.of("src/main/resources/aura.mixins.json")))
            .getAsJsonObject().getAsJsonArray("mixins");
        assertTrue(mixins.asList().stream().anyMatch(value -> value.getAsString().equals("PortableRedHoleLifetimeMixin")));
    }

    @Test
    void serverItemHookUsesLegacyNonFierySourceEntityBlastWithoutWorldTickFallback() throws Exception {
        String item = source("item/PortableRedHoleItem.java");
        assertTrue(item.contains("if (!(entity.level() instanceof ServerLevel level) || !shouldErupt(entity.getItem().getItem() instanceof PortableRedHoleItem, level.getGameTime())) { return false; }"));
        assertTrue(item.contains("level.explode(entity, entity.getX(), entity.getY(), entity.getZ(), 12.0F, false, Level.ExplosionInteraction.BLOCK); return true;"));
        assertFalse(item.contains("setInvulnerable"));
        assertFalse(item.contains("fireResistant"));
    }

    @Test
    void frozenWorldCallbacksHaveNoIndependentRedHoleEffectPath() throws Exception {
        // Vanilla skips ItemEntity.tick while frozen, but Fabric world-tail callbacks
        // still run. Keep the only dispatch inside the registered entity-tick hook.
        assertFalse(source("item/AuraItems.java").contains("tickWorldUtilityItems"),
            "An END_WORLD_TICK fallback can erupt repeatedly at the same frozen gameTime");
        assertFalse(source("item/AuraItems.java").contains("PortableRedHoleItem.onEntityItemTick"));
        String item = source("item/PortableRedHoleItem.java");
        assertFalse(item.contains("ServerTickEvents"));
        assertEquals(1, item.split("level\\.explode\\(", -1).length - 1);
        String mixin = source("mixin/PortableRedHoleLifetimeMixin.java");
        assertTrue(mixin.contains("@Inject(method = \"tick\", at = @At(\"HEAD\"), cancellable = true)"));
        assertEquals(1, mixin.split("PortableRedHoleItem\\.onEntityItemTick\\(", -1).length - 1);
    }

    private static String source(String relativePath) throws Exception {
        return Files.readString(Path.of("src/main/java/pixlepix/auracascade", relativePath))
            .replaceAll("(?m)//[^\\r\\n]*", "").replaceAll("\\s+", " ");
    }
}
