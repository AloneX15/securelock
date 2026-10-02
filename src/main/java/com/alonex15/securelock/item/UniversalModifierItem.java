package com.alonex15.securelock.item;

import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.network.ServerNetworking;
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

/** Codificador universal: abre el panel del bloque (propietario, modo, permitidos/bloqueados, código, nivel de tarjeta). */
public class UniversalModifierItem extends Item implements SecureTool {
    public UniversalModifierItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state, ItemStack stack,
                                        InteractionHand hand, LockManager.@Nullable Locked locked) {
        if (locked == null) {
            player.sendOverlayMessage(Component.translatable("message.securelock.not_protected").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (!AccessManager.canConfigure(player, locked.lock())) {
            AccessManager.deny(player, locked, "message.securelock.not_owner");
            return InteractionResult.FAIL;
        }
        AccessManager.auditBypass(player, level, locked);
        ServerNetworking.openConfig(player, level, locked.pos());
        return InteractionResult.SUCCESS;
    }
}
