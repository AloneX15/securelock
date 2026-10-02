package com.takumistudios.securemod.item;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.access.PermissionHelper;
import com.takumistudios.securemod.api.SecureModEvents;
import com.takumistudios.securemod.lock.LockKind;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.registry.ModItems;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Universal Block Remover: el propietario recupera su bloque protegido (cae como ítem) o quita su candado
 * (recupera el Padlock).
 */
public class UniversalBlockRemoverItem extends Item implements SecureTool {
    public UniversalBlockRemoverItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack stack,
                                        InteractionHand hand, LockManager.@Nullable Locked locked) {
        if (locked == null) {
            player.sendOverlayMessage(Component.translatable("message.securemod.not_protected").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        boolean owner = !locked.lock().isCorrupt() && locked.lock().isOwner(player.getUUID());
        if (!owner && !PermissionHelper.has(player, PermissionHelper.ADMIN_REMOVE)) {
            AccessManager.deny(player, locked, "message.securemod.not_owner");
            return InteractionResult.FAIL;
        }
        String location = AccessManager.describe(level, locked.pos());
        if (locked.lock().kind() == LockKind.PADLOCK) {
            LockManager.remove(level, locked.pos());
            if (!player.isCreative()) {
                ItemStack padlock = new ItemStack(ModItems.PADLOCK);
                if (!player.getInventory().add(padlock)) {
                    Block.popResource(level, player.blockPosition(), padlock);
                }
            }
            player.sendOverlayMessage(Component.translatable("message.securemod.unlocked").withStyle(ChatFormatting.GREEN));
            AuditLogger.log(AuditLogger.Event.UNLOCK, player.getName().getString(), location, "padlock");
        } else {
            LockManager.remove(level, locked.pos());
            level.destroyBlock(locked.pos(), !player.isCreative(), player);
            AuditLogger.log(AuditLogger.Event.BREAK, player.getName().getString(), location, "remover");
        }
        if (!owner) {
            AuditLogger.log(AuditLogger.Event.ADMIN_BYPASS, player.getName().getString(), location, "remover owner=" + locked.lock().ownerName());
        }
        SecureModEvents.BLOCK_UNLOCKED.invoker().onBlockUnlocked(player, level, locked.pos());
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.SUCCESS;
    }
}
