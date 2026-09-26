package pixlepix.auracascade.fairy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.Registry;

public final class AuraFairyEntityRegistry {
    private static final Identifier ID = Identifier.fromNamespaceAndPath("aura", "fairy");
    private static EntityType<AuraFairyEntity> entityType;

    private AuraFairyEntityRegistry() {
    }

    public static void bootstrapCommon() {
        if (entityType != null) {
            return;
        }
        entityType = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ID,
            EntityType.Builder.of(AuraFairyEntity::new, MobCategory.MISC)
                .sized(0.1F, 0.1F)
                .clientTrackingRange(8)
                .updateInterval(2)
                .build(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, ID))
        );
    }

    public static EntityType<AuraFairyEntity> entityType() {
        if (entityType == null) {
            throw new IllegalStateException("Aura fairy entity has not been registered");
        }
        return entityType;
    }
}
