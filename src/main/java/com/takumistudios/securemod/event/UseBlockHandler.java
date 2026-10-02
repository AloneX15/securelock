package com.takumistudios.securemod.event;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.block.SecureModBlock;
import com.takumistudios.securemod.item.SecureTool;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.network.ServerNetworking;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Clic derecho sobre bloques ({@code UseBlockCallback}). Cancela la apertura de bloques protegidos aunque el
 * jugador esté agachado, use otro ítem o sea espectador. Las herramientas de Secure Mod se procesan aquí.
 */
public final class UseBlockHandler {
    private UseBlockHandler() {
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            // En el cliente solo evitamos la predicción cuando se usa una herramienta propia; el servidor decide
            return stack.getItem() instanceof SecureTool ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        try {
            return handle(serverPlayer, serverLevel, hand, hit, stack);
        } catch (RuntimeException e) {
            // Fallo seguro: si algo falla, un bloque protegido sigue cerrado
            SecureMod.warnOnce("use_block", "Error en el manejador de clic derecho", e);
            return LockManager.isProtected(level, hit.getBlockPos()) ? InteractionResult.FAIL : InteractionResult.PASS;
        }
    }

    private static InteractionResult handle(ServerPlayer player, ServerLevel level, InteractionHand hand, BlockHitResult hit, ItemStack stack) {
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        LockManager.Locked locked = LockManager.find(level, pos);

        if (stack.getItem() instanceof SecureTool tool) {
            InteractionResult result = tool.useOnBlock(player, level, pos, state, stack, hand, locked);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        if (locked == null) {
            return InteractionResult.PASS;
        }
        if (state.getBlock() instanceof SecureModBlock secure && secure.isSelfGuarded()) {
            return InteractionResult.PASS;
        }
        if (AccessManager.canUse(player, level, locked)) {
            AccessManager.auditBypass(player, level, locked);
            return InteractionResult.PASS;
        }
        // Agachado con un bloque en la mano: vanilla coloca el bloque y no abre nada
        if (player.isShiftKeyDown() && stack.getItem() instanceof BlockItem && !player.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (locked.lock().hasPasscode() && !locked.lock().isCorrupt() && !locked.lock().denied().containsKey(player.getUUID())) {
            ServerNetworking.openKeypad(player, level, pos);
            return InteractionResult.FAIL;
        }
        AccessManager.deny(player, locked, "message.securemod.locked_by");
        AuditLogger.log(AuditLogger.Event.ACCESS_DENIED, player.getName().getString(), AccessManager.describe(level, locked.pos()), LockManager.blockId(state));
        return InteractionResult.FAIL;
    }
}
