package com.alonex15.securelock.mixin;

import com.alonex15.securelock.SecureLockHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Tolvas, minecart-tolva y droppers no pueden extraer ni insertar en contenedores protegidos.
 * Todas las búsquedas de contenedor pasan por {@code getBlockContainer}; la tolva de origen se recuerda
 * para permitir (si la config lo dice) las tolvas con candado del mismo propietario.
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {
    @ModifyReturnValue(method = "getBlockContainer", at = @At("RETURN"), require = 0)
    private static @Nullable Container securelock$filterContainer(@Nullable Container original, Level level, BlockPos pos, BlockState state) {
        return SecureLockHooks.filterContainer(level, pos, original);
    }

    @WrapMethod(method = "getAttachedContainer", require = 0)
    private static @Nullable Container securelock$pushSource(Level level, BlockPos blockPos, HopperBlockEntity self, Operation<Container> original) {
        SecureLockHooks.setHopperSource(blockPos);
        try {
            return original.call(level, blockPos, self);
        } finally {
            SecureLockHooks.setHopperSource(null);
        }
    }

    @WrapMethod(method = "getSourceContainer", require = 0)
    private static @Nullable Container securelock$pullSource(Level level, Hopper hopper, BlockPos pos, BlockState state, Operation<Container> original) {
        SecureLockHooks.setHopperSource(hopper instanceof HopperBlockEntity be ? be.getBlockPos() : null);
        try {
            return original.call(level, hopper, pos, state);
        } finally {
            SecureLockHooks.setHopperSource(null);
        }
    }
}
