package com.takumistudios.securemod.mixin;

import com.takumistudios.securemod.SecureModHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** El wither no destruye bloques protegidos al recibir daño. */
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"), require = 0)
    private boolean securemod$keepProtected(ServerLevel level, BlockPos pos, boolean drop, Entity entity, Operation<Boolean> original) {
        return SecureModHooks.canMobDestroy(level, pos) && original.call(level, pos, drop, entity);
    }
}
