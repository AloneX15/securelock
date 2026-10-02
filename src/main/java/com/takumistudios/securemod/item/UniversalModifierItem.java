package com.takumistudios.securemod.item;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.network.ServerNetworking;
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
            player.sendOverlayMessage(Component.translatable("message.securemod.not_protected").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (!AccessManager.canConfigure(player, locked.lock())) {
            AccessManager.deny(player, locked, "message.securemod.not_owner");
            return InteractionResult.FAIL;
        }
        AccessManager.auditBypass(player, level, locked);
        ServerNetworking.openConfig(player, level, locked.pos());
        return InteractionResult.SUCCESS;
    }
}
