package com.alonex15.securelock.client.compat;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.client.OwnerHud;
import com.alonex15.securelock.network.Payloads;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Integración opcional con Jade: muestra el propietario y el modo del bloque protegido en el tooltip.
 * Usa la información que el servidor ya envía para el HUD (OwnerInfoS2C), así que no necesita Jade en el servidor.
 * Solo se carga desde el entrypoint "jade", es decir, si Jade está instalado.
 */
public final class SecureLockJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(SecureLock.MOD_ID, "owner");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(OwnerProvider.INSTANCE, Block.class);
    }

    private enum OwnerProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Payloads.OwnerInfoS2C info = OwnerHud.current();
            if (info == null || !info.pos().equals(accessor.getPosition())) {
                return;
            }
            tooltip.add(Component.translatable("hud.securelock.owned_by", info.ownerName()).withStyle(ChatFormatting.GOLD));
            Component mode = Component.translatable("mode.securelock." + info.mode()).withStyle(ChatFormatting.GRAY);
            tooltip.add(info.hasCode() ? mode.copy().append(Component.literal(" · ").append(Component.translatable("hud.securelock.passcode"))) : mode);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
