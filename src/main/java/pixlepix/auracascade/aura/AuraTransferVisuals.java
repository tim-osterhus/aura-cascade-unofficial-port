package pixlepix.auracascade.aura;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import pixlepix.auracascade.parity.AuraColor;

public final class AuraTransferVisuals {
    public static final int MAX_SAMPLES = 16;

    private AuraTransferVisuals() {
    }

    public static List<Sample> samples(BlockPos source, BlockPos target, AuraStorage moved) {
        if (moved.isEmpty() || source.equals(target)) {
            return List.of();
        }
        List<AuraColor> colors = new ArrayList<>();
        for (AuraColor color : AuraColor.values()) {
            if (moved.get(color) > 0) {
                colors.add(color);
            }
        }
        Vec3 start = Vec3.atCenterOf(source);
        Vec3 displacement = Vec3.atCenterOf(target).subtract(start);
        Vec3 direction = displacement.normalize();
        // Share a fixed particle budget among the colors actually transferred.
        int count = Math.max(1, Math.min(MAX_SAMPLES / colors.size(), (int) Math.ceil(displacement.length() * 2)));
        List<Sample> samples = new ArrayList<>();
        for (AuraColor color : colors) {
            for (int index = 0; index < count; index++) {
                double fraction = (index + 1.0D) / (count + 1.0D);
                samples.add(new Sample(color, start.add(displacement.scale(fraction)), direction));
            }
        }
        return List.copyOf(samples);
    }

    public record Sample(AuraColor color, Vec3 position, Vec3 direction) {
    }
}
