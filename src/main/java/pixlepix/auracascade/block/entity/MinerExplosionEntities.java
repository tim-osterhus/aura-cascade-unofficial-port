package pixlepix.auracascade.block.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
        Identifier id = Identifier.fromNamespaceAndPath("aura", "miner_explosion");
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
            EntityType.Builder.<MinerExplosionEntity>of(MinerExplosionEntity::new, MobCategory.MISC)
                .sized(1.0F, 1.0F)
                .clientTrackingRange(8)
                .updateInterval(1)
                .build(key)
        );
    }

    public static EntityType<MinerExplosionEntity> type() {
        if (type == null) {
            throw new IllegalStateException("Register the miner explosion entity before using the miner");
        }
        return type;
    }
}
