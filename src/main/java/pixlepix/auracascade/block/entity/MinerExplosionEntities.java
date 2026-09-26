package pixlepix.auracascade.block.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class MinerExplosionEntities {
    private static EntityType<MinerExplosionEntity> type;

    private MinerExplosionEntities() {
    }

    public static void bootstrapCommon() {
        if (type != null) {
            return;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("aura", "miner_explosion");
        type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
            EntityType.Builder.<MinerExplosionEntity>of(MinerExplosionEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build()
        );
    }

    public static EntityType<MinerExplosionEntity> type() {
        if (type == null) {
            throw new IllegalStateException("Register the miner explosion entity before using the miner");
        }
        return type;
    }
}
