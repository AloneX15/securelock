package com.alonex15.securelock.mixin;

import com.alonex15.securelock.SecureLockHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Los mobs (zombis que rompen puertas, illagers que las abren) usan {@code DoorBlock.isWoodenDoor(Level, BlockPos)}
 * para decidir si pueden interactuar. Una puerta con candado deja de contar como "puerta de madera" para ellos.
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {
    @ModifyReturnValue(method = "isWoodenDoor(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z", at = @At("RETURN"), require = 0)
    private static boolean securelock$protectedNotForMobs(boolean original, Level level, BlockPos pos) {
        return original && SecureLockHooks.canMobDestroy(level, pos);
    }
}
