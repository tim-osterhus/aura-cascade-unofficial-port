package pixlepix.auracascade.block.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import pixlepix.auracascade.data.recipe.AuraVortexRecipe;

public final class VortexCraftingLogic {
    private VortexCraftingLogic() {
    }

    public static Optional<RecipeMatch> findMatch(List<PedestalInput> pedestals, List<AuraVortexRecipe> recipes) {
        for (AuraVortexRecipe recipe : recipes) {
            Optional<RecipeMatch> match = match(recipe, pedestals);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    public static Optional<RecipeMatch> match(AuraVortexRecipe recipe, List<PedestalInput> pedestals) {
        if (pedestals.size() != recipe.components().size()) {
            return Optional.empty();
        }

        boolean[] used = new boolean[pedestals.size()];
        LinkedHashMap<net.minecraft.core.BlockPos, AuraVortexRecipe.Component> assignments = new LinkedHashMap<>();
        if (!assign(recipe.components(), pedestals, 0, used, assignments)) {
            return Optional.empty();
        }
        return Optional.of(new RecipeMatch(recipe, Map.copyOf(assignments)));
    }

    public static boolean ready(RecipeMatch match, Map<net.minecraft.core.BlockPos, PedestalInput> pedestalsByPos) {
        for (Map.Entry<net.minecraft.core.BlockPos, AuraVortexRecipe.Component> entry : match.assignments().entrySet()) {
            PedestalInput pedestal = pedestalsByPos.get(entry.getKey());
            if (pedestal == null || !pedestal.aura().covers(entry.getValue().auraRequirement())) {
                return false;
            }
        }
        return true;
    }

    private static boolean assign(
        List<AuraVortexRecipe.Component> components,
        List<PedestalInput> pedestals,
        int componentIndex,
        boolean[] used,
        LinkedHashMap<net.minecraft.core.BlockPos, AuraVortexRecipe.Component> assignments
    ) {
        if (componentIndex >= components.size()) {
            return true;
        }

        AuraVortexRecipe.Component component = components.get(componentIndex);
        for (int pedestalIndex = 0; pedestalIndex < pedestals.size(); pedestalIndex++) {
            if (used[pedestalIndex]) {
                continue;
            }

            PedestalInput pedestal = pedestals.get(pedestalIndex);
            if (!pedestal.item().is(component.item()) || pedestal.item().getCount() < component.count()) {
                continue;
            }

            used[pedestalIndex] = true;
            assignments.put(pedestal.pos(), component);
            if (assign(components, pedestals, componentIndex + 1, used, assignments)) {
                return true;
            }
            assignments.remove(pedestal.pos());
            used[pedestalIndex] = false;
        }

        return false;
    }

    public static int progressSignal(int progress, int maxProgress) {
        if (progress <= 0) {
            return 0;
        }
        return Math.min(15, (int) Math.ceil(progress * 15.0D / Math.max(1, maxProgress)));
    }

    public record PedestalInput(net.minecraft.core.BlockPos pos, net.minecraft.world.item.ItemStack item, pixlepix.auracascade.aura.AuraStorage aura) {
        public PedestalInput {
            item = item.copy();
            aura = aura.copy();
        }
    }

    public record RecipeMatch(AuraVortexRecipe recipe, Map<net.minecraft.core.BlockPos, AuraVortexRecipe.Component> assignments) {
        public RecipeMatch {
            assignments = Map.copyOf(assignments);
        }

        public List<net.minecraft.core.BlockPos> orderedPositions() {
            return new ArrayList<>(assignments.keySet());
        }
    }
}
