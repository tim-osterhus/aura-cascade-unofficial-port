package pixlepix.auracascade.mixin;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pixlepix.auracascade.item.AuraItems;

@Mixin(Explosion.class)
abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    public abstract Vec3 center();

    @Shadow
    public abstract void clearToBlow();

    @Shadow
    public abstract List<BlockPos> getToBlow();

    @Inject(method = "explode", at = @At("RETURN"))
    private void aura$filterShatteredStoneBlocks(CallbackInfo ci) {
        if (this.level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            List<BlockPos> affectedBlocks = this.getToBlow();
            List<BlockPos> filteredBlocks = AuraItems.filterShatteredStoneExplosionBlocks(serverLevel, center(), affectedBlocks);
            // The no-wearer path returns the live vanilla list unchanged.
            if (filteredBlocks == affectedBlocks) {
                return;
            }
            this.clearToBlow();
            this.getToBlow().addAll(filteredBlocks);
        }
    }
}
