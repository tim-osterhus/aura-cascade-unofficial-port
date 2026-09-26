package pixlepix.auracascade.aura;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class WorldInteractionVisuals {
    public static final int PLACEMENT_TICKS = 12;
    public static final int CRAFT_TICKS = 10;
    public static final int MAX_PLACEMENT_LINKS = 6;
    public static final int GROUND_CRAFT_PARTICLES = 12;

    private WorldInteractionVisuals() {
    }

    public static List<Vec3> placementSamples(BlockPos source, Collection<BlockPos> visibleLinks, int tick) {
        if (tick < 0 || tick >= PLACEMENT_TICKS) {
            return List.of();
        }
        Vec3 start = Vec3.atCenterOf(source);
        double fraction = (tick + 1.0D) / (PLACEMENT_TICKS + 1.0D);
        List<Vec3> samples = new ArrayList<>();
        for (BlockPos target : visibleLinks) {
            if (samples.size() >= MAX_PLACEMENT_LINKS * 2) {
                break;
            }
            Vec3 path = Vec3.atCenterOf(target).subtract(start);
            samples.add(start.add(path.scale(fraction)));
            samples.add(start.add(path.scale(Math.min(1.0D, fraction + 0.06D))));
        }
        return List.copyOf(samples);
    }

    public static List<Vec3> craftSamples(Vec3 center, int tick) {
        if (tick < 0 || tick >= CRAFT_TICKS) {
            return List.of();
        }
        List<Vec3> samples = new ArrayList<>(4);
        for (int index = 0; index < 4; index++) {
            double angle = tick * 0.65D + index * Math.PI / 2.0D;
            double radius = 0.35D * (1.0D - tick / (double) CRAFT_TICKS);
            samples.add(center.add(Math.cos(angle) * radius, 0.12D + tick * 0.045D,
                Math.sin(angle) * radius));
        }
        return List.copyOf(samples);
    }

    public static List<BurstSample> groundCraftBurst(Vec3 center) {
        List<BurstSample> samples = new ArrayList<>(GROUND_CRAFT_PARTICLES);
        for (int index = 0; index < GROUND_CRAFT_PARTICLES; index++) {
            double angle = index * Math.PI * 2.0D / GROUND_CRAFT_PARTICLES;
            Vec3 outward = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
            Vec3 position = center.add(outward.scale(0.16D)).add(0.0D, 0.12D, 0.0D);
            Vec3 velocity = outward.scale(0.045D).add(0.0D, 0.045D + (index % 3) * 0.012D, 0.0D);
            samples.add(new BurstSample(position, velocity));
        }
        return List.copyOf(samples);
    }

    public record BurstSample(Vec3 position, Vec3 velocity) {
    }
}
