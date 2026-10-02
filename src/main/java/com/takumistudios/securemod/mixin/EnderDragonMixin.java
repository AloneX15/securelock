package com.takumistudios.securemod.mixin;

import com.takumistudios.securemod.SecureModHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** El ender dragon no destruye bloques protegidos al atravesarlos. */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @WrapOperation(method = "checkWalls", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"), require = 0)
    private boolean securemod$keepProtected(ServerLevel level, BlockPos pos, boolean movedByPiston, Operation<Boolean> original) {
        return SecureModHooks.canMobDestroy(level, pos) && original.call(level, pos, movedByPiston);
    }
}
