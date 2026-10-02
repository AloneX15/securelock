package com.alonex15.securelock.item;

import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.api.SecureLockEvents;
import com.alonex15.securelock.block.OwnedBlocks;
import com.alonex15.securelock.block.SecureLockBlock;
import com.alonex15.securelock.lock.LockData;
import com.alonex15.securelock.lock.LockKind;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.security.AuditLogger;
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
            String key = locked.lock().isOwner(player.getUUID()) ? "message.securelock.already_locked" : "message.securelock.locked_by";
            player.sendOverlayMessage(Component.translatable(key, locked.lock().ownerName()).withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (state.getBlock() instanceof SecureLockBlock || !LockManager.isLockable(state)) {
            player.sendOverlayMessage(Component.translatable("message.securelock.not_lockable").withStyle(ChatFormatting.RED));
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
        player.sendOverlayMessage(Component.translatable("message.securelock.locked").withStyle(ChatFormatting.GREEN));
        AuditLogger.log(AuditLogger.Event.LOCK, player.getName().getString(), AccessManager.describe(level, canonical), LockManager.blockId(state));
        SecureLockEvents.BLOCK_LOCKED.invoker().onBlockLocked(player, level, canonical);
        return InteractionResult.SUCCESS;
    }
}
