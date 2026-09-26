package pixlepix.auracascade.lexicon;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import pixlepix.auracascade.config.AuraConfig;
import pixlepix.auracascade.item.FairyCharmItem;
import pixlepix.auracascade.fairy.FairyRole;

public final class AuraQuestProgress {
    private static final String CRITERION = "book_open";
    private static final List<Definition> DEFINITIONS = loadDefinitions();

    private AuraQuestProgress() {
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    public static void onBookOpened(ServerPlayer player) {
        if (!AuraConfig.questlineEnabled()) {
            return;
        }

        var manager = player.level().getServer().getAdvancements();
        var progress = player.getAdvancements();
        var root = manager.get(Identifier.fromNamespaceAndPath("aura", "quest/root"));
        if (root != null) {
            progress.award(root, CRITERION);
        }
        for (Definition quest : DEFINITIONS) {
            var advancement = manager.get(Identifier.fromNamespaceAndPath("aura", "quest/" + quest.id()));
            if (advancement == null || progress.getOrStartProgress(advancement).isDone() || !hasGoal(player, quest)) {
                continue;
            }
            var rewardItem = BuiltInRegistries.ITEM.getOptional(Identifier.parse(quest.reward())).orElseThrow();
            ItemStack reward = new ItemStack(rewardItem, quest.rewardCount());
            if (rewardItem instanceof FairyCharmItem) {
                FairyCharmItem.withRole(reward, FairyRole.defaultRole());
            }
            if (!progress.award(advancement, CRITERION)) {
                continue;
            }
            ItemEntity drop = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), reward);
            drop.setPickUpDelay(0);
            if (!player.level().addFreshEntity(drop)) {
                progress.revoke(advancement, CRITERION);
                continue;
            }
            player.displayClientMessage(Component.translatable("message.aura.quest.complete", quest.title()), false);
        }
        // Send completion before Patchouli opens so its quest pages reflect this check.
        progress.flushDirty(player, false);
    }

    private static boolean hasGoal(ServerPlayer player, Definition quest) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(quest.goal())) {
                continue;
            }
            if (stack.getItem() instanceof FairyCharmItem && FairyCharmItem.role(stack) != FairyRole.defaultRole()) {
                continue;
            }
            return true;
        }
        return false;
    }

    private static List<Definition> loadDefinitions() {
        var stream = AuraQuestProgress.class.getClassLoader().getResourceAsStream("data/aura/quests.json");
        if (stream == null) {
            throw new IllegalStateException("Missing Aura quest definitions");
        }
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            List<Definition> quests = new ArrayList<>();
            for (var element : JsonParser.parseReader(reader).getAsJsonArray()) {
                var json = element.getAsJsonObject();
                quests.add(new Definition(json.get("id").getAsString(), json.get("title").getAsString(),
                    json.get("goal").getAsString(), json.get("reward").getAsString(), json.get("reward_count").getAsInt()));
            }
            return List.copyOf(quests);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid Aura quest definitions", exception);
        }
    }

    public record Definition(String id, String title, String goal, String reward, int rewardCount) {
    }
}
