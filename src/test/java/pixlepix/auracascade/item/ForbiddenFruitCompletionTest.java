package pixlepix.auracascade.item;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;
import org.junit.jupiter.api.Test;
import pixlepix.auracascade.support.TestMinecraftBootstrap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ForbiddenFruitCompletionTest {
    @Test
    void onlyLivingNonspectatorWearersCompletingEatOrDrinkAreEligible() {
        TestMinecraftBootstrap.ensureBootstrapped();
        ItemStack apple = new ItemStack(Items.APPLE);
        assertTrue(apple.has(DataComponents.FOOD));
        assertEquals(ItemUseAnimation.EAT, apple.getUseAnimation());
        for (ItemUseAnimation animation : new ItemUseAnimation[] {apple.getUseAnimation(), ItemUseAnimation.DRINK}) {
            assertTrue(ForbiddenFruitEffects.acceptsCompletedUse(animation, true, false, true));
            assertFalse(ForbiddenFruitEffects.acceptsCompletedUse(animation, false, false, true));
            assertFalse(ForbiddenFruitEffects.acceptsCompletedUse(animation, true, true, true));
            assertFalse(ForbiddenFruitEffects.acceptsCompletedUse(animation, true, false, false));
        }
    }

    @Test
    void potionAndMilkDoNotNeedAFoodComponentOrStackCountDecrease() {
        TestMinecraftBootstrap.ensureBootstrapped();
        for (var item : new net.minecraft.world.item.Item[] {Items.POTION, Items.MILK_BUCKET}) {
            ItemStack used = new ItemStack(item);
            assertFalse(used.has(DataComponents.FOOD));
            ItemUseAnimation beforeFinish = used.getUseAnimation();
            assertEquals(ItemUseAnimation.DRINK, beforeFinish);
            assertTrue(ForbiddenFruitEffects.acceptsCompletedUse(beforeFinish, true, false, true));
            // Completion may empty the stack or replace it with a bottle/bucket.
            used.setCount(0);
            assertTrue(ForbiddenFruitEffects.acceptsCompletedUse(beforeFinish, true, false, true));
        }
        assertFalse(ForbiddenFruitEffects.acceptsCompletedUse(new ItemStack(Items.GLASS_BOTTLE).getUseAnimation(), true, false, true));
    }

    @Test
    void everyOtherUseAnimationIsRejected() {
        for (ItemUseAnimation animation : ItemUseAnimation.values()) {
            assertEquals(animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK,
                ForbiddenFruitEffects.acceptsCompletedUse(animation, true, false, true), animation.name());
        }
    }

    @Test
    void completionHookCapturesAnimationAndStackBeforeCallingOriginalAndAppliesOnlyAfter() throws Exception {
        String source = Files.readString(Path.of("src/main/java/pixlepix/auracascade/mixin/ForbiddenFruitFinishMixin.java"));
        int original = source.indexOf("original.call(stack, level, user)");
        int animation = source.indexOf("ItemUseAnimation useAnimation = stack.getUseAnimation()");
        assertTrue(animation >= 0 && original > animation);
        assertTrue(source.indexOf("stack.copy()") >= 0 && source.indexOf("stack.copy()") < original);
        assertTrue(source.indexOf("AuraItems.onFoodFinished(player, usedFood, useAnimation)") > original);
        assertTrue(source.contains("@WrapOperation(method = \"completeUsingItem\""));
        assertFalse(source.contains("DataComponents.FOOD"));
    }

    @Test
    void completionMixinIsRegistered() {
        var resource = getClass().getResourceAsStream("/aura.mixins.json");
        assertNotNull(resource);
        try (var reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            var mixins = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("mixins");
            boolean registered = false;
            for (var name : mixins) {
                registered |= name.getAsString().equals("ForbiddenFruitFinishMixin");
            }
            assertTrue(registered);
        } catch (java.io.IOException exception) {
            throw new AssertionError(exception);
        }
    }
}
