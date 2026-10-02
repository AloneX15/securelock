package com.alonex15.securelock.mixin;

import com.alonex15.securelock.SecureLockHooks;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** El fuego no quema ni reemplaza bloques protegidos (p. ej. una puerta de madera con candado). */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @WrapOperation(method = "checkBurnOut", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"), require = 0)
    private boolean securelock$keepProtected(Level level, BlockPos pos, boolean movedByPiston, Operation<Boolean> original) {
        return SecureLockHooks.canFireDestroy(level, pos) && original.call(level, pos, movedByPiston);
    }

    //? if >=26.3 {
    @WrapWithCondition(method = "checkBurnOut", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 0)
    private boolean securelock$noFireOnProtected(Level level, BlockPos pos, BlockState state) {
        return SecureLockHooks.canFireDestroy(level, pos);
    }
    //?} else {
    /*@WrapWithCondition(method = "checkBurnOut", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 0)
    private boolean securelock$noFireOnProtected(Level level, BlockPos pos, BlockState state, int flags) {
        return SecureLockHooks.canFireDestroy(level, pos);
    }
    *///?}
}
