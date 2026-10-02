package com.alonex15.securelock.mixin;

import com.alonex15.securelock.SecureLockHooks;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/** Los gólems de cobre no sacan ni meten ítems en contenedores protegidos. */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
    @Inject(method = "isTargetValidToPick", at = @At("RETURN"), cancellable = true, require = 0)
    private void securelock$skipProtected(PathfinderMob body, Level level, BlockEntity blockEntity, Set<GlobalPos> visitedPositions,
                                          Set<GlobalPos> unreachablePositions, AABB searchArea, CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() != null && !SecureLockHooks.canMobUseContainer(level, blockEntity.getBlockPos())) {
            cir.setReturnValue(null);
        }
    }
}
