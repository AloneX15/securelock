package com.alonex15.securelock.item;

import com.alonex15.securelock.lock.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Herramientas de SecureLock. El manejador de clic derecho las procesa antes que el bloque,
 * así funcionan sobre cofres o puertas protegidas sin abrirlos. {@code PASS} = seguir con la lógica normal.
 */
public interface SecureTool {
    InteractionResult useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack stack,
                                 InteractionHand hand, LockManager.@Nullable Locked locked);
}
