package pixlepix.auracascade.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pixlepix.auracascade.fairy.FairySystem;

@Mixin(NaturalSpawner.class)
public abstract class FairyNaturalSpawnMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("RETURN"), cancellable = true)
    private static void aura$denySpawnNearScarers(
        ServerLevel level,
        MobCategory category,
        StructureManager structureManager,
        ChunkGenerator chunkGenerator,
        MobSpawnSettings.SpawnerData spawnData,
        BlockPos.MutableBlockPos position,
        double distance,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && FairySystem.shouldDenyNaturalSpawn(level, position)) {
            cir.setReturnValue(false);
        }
    }
}
