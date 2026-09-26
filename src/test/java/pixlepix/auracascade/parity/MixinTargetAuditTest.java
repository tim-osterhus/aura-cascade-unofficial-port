package pixlepix.auracascade.parity;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MixinTargetAuditTest {
    @Test
    void allTargetHooksRemainRequiredAndRegistered() throws Exception {
        var config = JsonParser.parseString(Files.readString(Path.of("src/main/resources/aura.mixins.json")))
            .getAsJsonObject();
        assertTrue(config.get("required").getAsBoolean());
        assertEquals(1, config.getAsJsonObject("injectors").get("defaultRequire").getAsInt());

        Map<String, String[]> targets = Map.ofEntries(
            Map.entry("ArrowDamageAccessor", new String[] {"@Mixin(AbstractArrow.class)", "@Accessor(\"baseDamage\")"}),
            Map.entry("ServerExplosionMixin", new String[] {"@Mixin(ServerExplosion.class)",
                "calculateExplodedPositions()Ljava/util/List;"}),
            Map.entry("CreeperAccessor", new String[] {"@Mixin(Creeper.class)", "@Accessor(\"swell\")", "@Accessor(\"maxSwell\")"}),
            Map.entry("KaleidoscopicDamageMixin", new String[] {"method = \"hurtServer\"",
                "actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V",
                "DamageSource source = args.get(1)", "float amount = args.get(2)", "args.set(2,"}),
            Map.entry("KaleidoscopicMiningMixin", new String[] {"@ModifyReturnValue(method = \"getDestroySpeed\""}),
            Map.entry("KaleidoscopicLootMixin", new String[] {"@Inject(method = \"dropAllDeathLoot\""}),
            Map.entry("KaleidoscopicOreDropsMixin", new String[] {"@Redirect(method = \"playerDestroy\"",
                "Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;"}),
            Map.entry("FairyNaturalSpawnMixin", new String[] {"@Inject(method = \"isValidSpawnPostitionForType\""}),
            Map.entry("FairyMiningSpeedMixin", new String[] {"@ModifyReturnValue(method = \"getDestroySpeed\""}),
            Map.entry("FairyFallDistanceMixin", new String[] {"causeFallDamage(DFLnet/minecraft/world/damagesource/DamageSource;)Z",
                "private double aura$applyGliderFairies(double distance)"}),
            Map.entry("ForbiddenFruitFinishMixin", new String[] {"@WrapOperation(method = \"completeUsingItem\"",
                "ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"}),
            Map.entry("ConsumerItemLifetimeMixin", new String[] {"ValueOutput output", "ValueInput input",
                "@ModifyConstant(method = \"isMergable\"", "@Inject(method = \"merge(Lnet/minecraft/world/entity/item/ItemEntity;"}),
            Map.entry("PortableRedHoleLifetimeMixin", new String[] {"@Inject(method = \"tick\"",
                "@ModifyConstant(method = \"tick\""})
        );

        var mixins = config.getAsJsonArray("mixins");
        assertEquals(targets.size(), mixins.size());
        for (var entry : mixins) {
            String name = entry.getAsString();
            assertTrue(targets.containsKey(name), name);
            String source = Files.readString(Path.of("src/main/java/pixlepix/auracascade/mixin", name + ".java"));
            for (String target : targets.get(name)) {
                assertTrue(source.contains(target), name + ": " + target);
            }
            assertFalse(source.contains("require = 0"), name);
        }
    }
}
