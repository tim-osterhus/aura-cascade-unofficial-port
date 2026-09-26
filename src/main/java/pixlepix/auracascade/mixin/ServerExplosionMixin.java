package pixlepix.auracascade.mixin;

import java.util.List;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import pixlepix.auracascade.item.AuraItems;

@Mixin(ServerExplosion.class)
abstract class ServerExplosionMixin {
    @Shadow
    public abstract ServerLevel level();

    @Shadow
    public abstract Vec3 center();

    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"))
    private List<BlockPos> aura$filterShatteredStoneBlocks(List<BlockPos> affectedBlocks) {
        return AuraItems.filterShatteredStoneExplosionBlocks(level(), center(), affectedBlocks);
    }
}
