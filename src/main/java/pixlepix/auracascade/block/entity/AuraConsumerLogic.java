package pixlepix.auracascade.block.entity;

import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class AuraConsumerLogic {
    static final int GROWER_RANDOM_TICKS = 50;

    private AuraConsumerLogic() {
    }

    public static int bleedStoredPower(int storedPower) {
        return Math.max(0, (int) (storedPower * 0.25D));
    }

    public static boolean shouldAdvanceProgress(long gameTime) {
        long tickInSecond = gameTime % 20L;
        return tickInSecond == 1L || tickInSecond == 2L;
    }

    public static int powerCostForStep(int basePower, int stepIndex) {
        if (basePower <= 0 || stepIndex < 0) {
            return 0;
        }

        long cost = (long) basePower << Math.min(stepIndex, 30);
        return cost >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cost;
    }

    public static int progressStepsForTick(int storedPower, int basePower) {
        if (storedPower <= 0 || basePower <= 0) {
            return 0;
        }

        int remaining = storedPower;
        int steps = 0;
        while (remaining >= powerCostForStep(basePower, steps)) {
            remaining -= powerCostForStep(basePower, steps);
            steps++;
        }
        return steps;
    }

    static ProgressState advanceProgress(
        int progress,
        int storedPower,
        AuraConsumerVariant variant,
        Runnable onCompletion
    ) {
        int step = 0;
        while (true) {
            if (progress > variant.maxProgress()) {
                progress = 0;
                onCompletion.run();
            }

            int cost = powerCostForStep(variant.powerPerProgress(), step);
            if (cost <= 0 || storedPower < cost) {
                break;
            }

            progress++;
            storedPower -= cost;
            step++;
        }
        return new ProgressState(progress, storedPower);
    }

    static ProcessorRoute processorRoute(boolean hasDustConversion, boolean hasRecipe) {
        if (hasDustConversion) {
            return ProcessorRoute.DUST;
        }
        return hasRecipe ? ProcessorRoute.RECIPE : ProcessorRoute.NONE;
    }

    static boolean shouldContinueGrowerBatch(Block initialBlock, BlockState currentState) {
        return currentState.is(initialBlock);
    }

    public static int minimumCyclePower(AuraConsumerVariant variant) {
        return variant.powerPerProgress() * (variant.maxProgress() + 1);
    }

    static boolean isFisherWater(BlockState state) {
        return state.is(Blocks.WATER);
    }

    static Optional<TagKey<Item>> commonDustTagForOre(TagKey<Item> oreTag) {
        Identifier location = oreTag.location();
        String path = location.getPath();
        if (!location.getNamespace().equals("c") || !path.startsWith("ores/") || path.length() == "ores/".length()) {
            return Optional.empty();
        }
        return Optional.of(TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath("c", "dusts/" + path.substring("ores/".length()))
        ));
    }

    record ProgressState(int progress, int storedPower) {
    }

    enum ProcessorRoute {
        DUST,
        RECIPE,
        NONE
    }
}
