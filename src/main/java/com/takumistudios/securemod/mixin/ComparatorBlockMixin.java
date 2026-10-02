package com.takumistudios.securemod.mixin;

import com.takumistudios.securemod.SecureModHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Opcional (config): un comparador lee 0 en un contenedor protegido para no filtrar su contenido. */
@Mixin(ComparatorBlock.class)
public abstract class ComparatorBlockMixin {
    @WrapOperation(method = "getInputSignal", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getAnalogOutputSignal(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)I"), require = 0)
    private int securemod$hideProtected(BlockState state, Level level, BlockPos pos, Direction direction, Operation<Integer> original) {
        return SecureModHooks.filterComparator(level, pos, original.call(state, level, pos, direction));
    }
}
