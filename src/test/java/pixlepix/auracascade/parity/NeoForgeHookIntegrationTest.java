package pixlepix.auracascade.parity;

import java.util.Arrays;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.item.AuraItems;
import pixlepix.auracascade.item.ConsumerItemLifetimeAccess;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Loaded-class guards only; entity behavior is exercised by NeoForgeHookRuntimeFixture. */
final class NeoForgeHookIntegrationTest {
    @BeforeAll
    static void requireNativeModBootstrap() {
        TestMinecraftBootstrap.ensureBootstrapped();
    }

    @Test
    void damageMixinIsPresentInTheLoadedLivingEntity() {
        assertMergedHandler(LivingEntity.class, "aura$modifyKaleidoscopicDamage");
    }

    @Test
    void lifetimeMixinsArePresentInTheLoadedItemEntity() {
        assertMergedHandler(ItemEntity.class, "aura$tickRedHole");
        assertTrue(ConsumerItemLifetimeAccess.class.isAssignableFrom(ItemEntity.class),
            "ItemEntity must have the consumer lifetime mixin, not only NeoForge's lifespan field");
    }

    @Test
    void runtimeFixturesUseTheActualRegisteredItems() {
        assertSame(AuraItems.PORTABLE_RED_HOLE, BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("aura", "portable_red_hole")));
        assertSame(AuraItems.SWORD_OF_THE_THIEF, BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("aura", "sword_of_the_thief")));
    }

    private static void assertMergedHandler(Class<?> target, String handler) {
        assertTrue(Arrays.stream(target.getDeclaredMethods()).anyMatch(method -> method.getName().contains(handler)),
            () -> target.getName() + " has no merged " + handler + "; this is not a transformed runtime");
    }
}
