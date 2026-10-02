package com.takumistudios.securemod.item;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.api.SecureModEvents;
import com.takumistudios.securemod.block.OwnedBlocks;
import com.takumistudios.securemod.block.SecureModBlock;
import com.takumistudios.securemod.lock.LockData;
import com.takumistudios.securemod.lock.LockKind;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Candado: bloquea cualquier cofre, puerta o contenedor existente (vanilla o de otro mod) sin reemplazar el bloque.
 * El candado se guarda como dato adjunto del chunk.
 */
public class PadlockItem extends Item implements SecureTool {
    public PadlockItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack stack,
                                        InteractionHand hand, LockManager.@Nullable Locked locked) {
        if (locked != null) {
            String key = locked.lock().isOwner(player.getUUID()) ? "message.securemod.already_locked" : "message.securemod.locked_by";
            player.sendOverlayMessage(Component.translatable(key, locked.lock().ownerName()).withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (state.getBlock() instanceof SecureModBlock || !LockManager.isLockable(state)) {
            player.sendOverlayMessage(Component.translatable("message.securemod.not_lockable").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        if (OwnedBlocks.isAtLimit(player)) {
            return InteractionResult.FAIL;
        }
        BlockPos canonical = LockManager.canonical(pos, state);
        LockData lock = LockData.create(player.getUUID(), player.getName().getString(), LockKind.PADLOCK, LockManager.blockId(state));
        LockManager.put(level, canonical, lock);
        if (!player.isCreative()) {
            stack.shrink(1);
        }
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
        player.sendOverlayMessage(Component.translatable("message.securemod.locked").withStyle(ChatFormatting.GREEN));
        AuditLogger.log(AuditLogger.Event.LOCK, player.getName().getString(), AccessManager.describe(level, canonical), LockManager.blockId(state));
        SecureModEvents.BLOCK_LOCKED.invoker().onBlockLocked(player, level, canonical);
        return InteractionResult.SUCCESS;
    }
}
