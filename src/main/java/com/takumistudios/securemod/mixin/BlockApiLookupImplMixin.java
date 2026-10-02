package com.takumistudios.securemod.mixin;

import com.takumistudios.securemod.SecureModHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.impl.lookup.block.BlockApiLookupImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mods de almacenamiento (Create, Tom's, tipo AE2...) acceden a los contenedores por la Transfer API.
 * Para un contenedor protegido se devuelve "sin almacenamiento", sin romper nada: el mod simplemente no lo ve.
 * Cubre también hoppers optimizados (p. ej. Lithium) que usan esta API.
 */
@Mixin(value = BlockApiLookupImpl.class, remap = false)
public abstract class BlockApiLookupImplMixin {
    @ModifyReturnValue(method = "find", at = @At("RETURN"), require = 0)
    private Object securemod$hideProtectedStorage(Object original, Level level, BlockPos pos, @Nullable BlockState state,
                                                   @Nullable BlockEntity blockEntity, Object context) {
        if (original != null && (Object) this == ItemStorage.SIDED && SecureModHooks.blockTransfer(level, pos)) {
            return null;
        }
        return original;
    }
}
