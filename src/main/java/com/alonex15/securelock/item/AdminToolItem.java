package com.alonex15.securelock.item;

import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.access.LockDescriber;
import com.alonex15.securelock.access.PermissionHelper;
import com.alonex15.securelock.api.SecureLockEvents;
import com.alonex15.securelock.block.OwnedBlocks;
import com.alonex15.securelock.lock.LockKind;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.security.AuditLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Herramienta de administrador (solo OP/permiso): clic = ver información, agachado + clic = forzar desbloqueo. */
public class AdminToolItem extends Item implements SecureTool {
    public AdminToolItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack stack,
                                        InteractionHand hand, LockManager.@Nullable Locked locked) {
        if (!PermissionHelper.has(player, PermissionHelper.ADMIN_INSPECT)) {
            player.sendOverlayMessage(Component.translatable("message.securelock.no_permission").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (locked == null) {
            player.sendOverlayMessage(Component.translatable("message.securelock.not_protected").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (!player.isShiftKeyDown()) {
            LockDescriber.describe(locked, true, player::sendSystemMessage);
            return InteractionResult.SUCCESS;
        }
        if (!PermissionHelper.has(player, PermissionHelper.ADMIN_REMOVE)) {
            player.sendOverlayMessage(Component.translatable("message.securelock.no_permission").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        forceUnlock(player, level, locked);
        return InteractionResult.SUCCESS;
    }

    /** Quita la protección. Los bloques propios se convierten en su equivalente vanilla conservando el contenido. */
    public static void forceUnlock(@Nullable ServerPlayer player, ServerLevel level, LockManager.Locked locked) {
        if (locked.lock().kind() == LockKind.BLOCK) {
            OwnedBlocks.convertToVanilla(level, locked.pos());
        }
        LockManager.remove(level, locked.pos());
        String actor = player == null ? "server" : player.getName().getString();
        AuditLogger.log(AuditLogger.Event.ADMIN_UNLOCK, actor, AccessManager.describe(level, locked.pos()), "owner=" + locked.lock().ownerName());
        SecureLockEvents.BLOCK_UNLOCKED.invoker().onBlockUnlocked(player, level, locked.pos());
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.securelock.force_unlocked", locked.lock().ownerName()).withStyle(ChatFormatting.GREEN));
        }
    }
}
