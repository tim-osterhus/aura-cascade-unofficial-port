package pixlepix.auracascade.data.recipe;

import java.util.List;
import pixlepix.auracascade.aura.AuraStorage;

public record AuraVortexRecipe(
    String id,
    List<Component> components,
    net.minecraft.world.item.ItemStack result,
    int maxProgress
) {
    public AuraVortexRecipe {
        components = List.copyOf(components);
        result = result.copy();
        maxProgress = Math.max(1, maxProgress);
    }

    public record Component(net.minecraft.world.item.Item item, int count, AuraStorage auraRequirement) {
        public Component {
            count = Math.max(1, count);
            auraRequirement = auraRequirement.copy();
        }

        public pixlepix.auracascade.parity.AuraColor requiredColor() {
            for (var color : pixlepix.auracascade.parity.AuraColor.values()) {
                if (auraRequirement.get(color) > 0) {
                    return color;
                }
            }
            throw new IllegalStateException("Vortex component has no power requirement");
        }

        public int requiredPower() {
            return auraRequirement.get(requiredColor());
        }
    }
}
