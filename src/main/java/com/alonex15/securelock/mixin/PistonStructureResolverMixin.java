package com.alonex15.securelock.mixin;

import com.alonex15.securelock.SecureLockHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/** Un pistón no puede empujar, tirar ni romper un bloque protegido (p. ej. una puerta vanilla con candado). */
@Mixin(PistonStructureResolver.class)
public abstract class PistonStructureResolverMixin {
    @Shadow
    @Final
    private Level level;

    @Shadow
    @Final
    private List<BlockPos> toPush;

    @Shadow
    @Final
    private List<BlockPos> toDestroy;

    @ModifyReturnValue(method = "resolve", at = @At("RETURN"), require = 0)
    private boolean securelock$blockProtected(boolean original) {
        return original && !SecureLockHooks.pistonBlocked(this.level, this.toPush, this.toDestroy);
    }
}
