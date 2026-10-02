package com.takumistudios.securemod.client.compat;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.client.OwnerHud;
import com.takumistudios.securemod.network.Payloads;
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
public final class SecureModJadePlugin implements IWailaPlugin {
    private static final Identifier UID = Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, "owner");

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
            tooltip.add(Component.translatable("hud.securemod.owned_by", info.ownerName()).withStyle(ChatFormatting.GOLD));
            Component mode = Component.translatable("mode.securemod." + info.mode()).withStyle(ChatFormatting.GRAY);
            tooltip.add(info.hasCode() ? mode.copy().append(Component.literal(" · ").append(Component.translatable("hud.securemod.passcode"))) : mode);
        }

        @Override
        public Identifier getUid() {
            return UID;
        }
    }
}
