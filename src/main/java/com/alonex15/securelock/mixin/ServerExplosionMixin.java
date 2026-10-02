package com.alonex15.securelock.mixin;

import com.alonex15.securelock.SecureLockHooks;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Excluye los bloques protegidos (y el soporte de puertas protegidas) de cualquier explosión. */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @ModifyExpressionValue(method = "explode", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/ServerExplosion;calculateExplodedPositions()Ljava/util/List;"), require = 0)
    private List<BlockPos> securelock$filterProtected(List<BlockPos> original) {
        return SecureLockHooks.filterExplosion(this.level, original);
    }
}
