package pixlepix.auracascade.parity;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningSpeedHookAuditTest {
    @Test
    void bothModifiersChainInsteadOfCancellingTheOtherReturnHook() throws Exception {
        // Structural guard only: the independent client test exercises transformed getDestroySpeed.
        for (String name : new String[] {"FairyMiningSpeedMixin", "KaleidoscopicMiningMixin"}) {
            String source = Files.readString(Path.of("src/main/java/pixlepix/auracascade/mixin/" + name + ".java"));
            assertTrue(source.contains("@ModifyReturnValue(method = \"getDestroySpeed\", at = @At(\"RETURN\"))"), name);
            assertTrue(source.contains("float original, BlockState state"), name);
            assertFalse(source.contains("CallbackInfoReturnable"), name);
            assertFalse(source.contains("setReturnValue"), name);
            assertFalse(source.contains("cancellable = true"), name);
        }
    }

    @Test
    void bothModifiersRemainRegistered() throws Exception {
        var mixins = JsonParser.parseString(Files.readString(Path.of("src/main/resources/aura.mixins.json")))
            .getAsJsonObject().getAsJsonArray("mixins");
        for (String name : new String[] {"FairyMiningSpeedMixin", "KaleidoscopicMiningMixin"}) {
            boolean registered = false;
            for (var mixin : mixins) {
                registered |= name.equals(mixin.getAsString());
            }
            assertTrue(registered, name);
        }
    }
}
