package com.takumistudios.securemod;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.access.PermissionHelper;
import com.takumistudios.securemod.command.SecureModCommand;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.data.ProtectedBlocksState;
import com.takumistudios.securemod.event.BlockBreakHandler;
import com.takumistudios.securemod.event.PurgeHandler;
import com.takumistudios.securemod.event.ServerTickHandler;
import com.takumistudios.securemod.event.UseBlockHandler;
import com.takumistudios.securemod.network.Payloads;
import com.takumistudios.securemod.network.ServerNetworking;
import com.takumistudios.securemod.registry.ModAttachments;
import com.takumistudios.securemod.registry.ModBlocks;
import com.takumistudios.securemod.registry.ModComponents;
import com.takumistudios.securemod.registry.ModItems;
import com.takumistudios.securemod.security.AuditLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.PlayerPickItemEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SecureMod implements ModInitializer {
    public static final String MOD_ID = "securemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    @Override
    public void onInitialize() {
        SecureModConfig.load();
        AccessManager.reconfigure();

        ModAttachments.init();
        ModComponents.init();
        ModBlocks.init();
        ModItems.init();
        Payloads.register();
        ServerNetworking.register();

        UseBlockCallback.EVENT.register(UseBlockHandler::onUseBlock);
        PlayerBlockBreakEvents.BEFORE.register(BlockBreakHandler::beforeBreak);
        PlayerBlockBreakEvents.AFTER.register(BlockBreakHandler::afterBreak);
        PlayerPickItemEvents.BLOCK.register(BlockBreakHandler::onPickBlock);
        ServerTickEvents.END_SERVER_TICK.register(ServerTickHandler::onEndTick);
        CommandRegistrationCallback.EVENT.register(SecureModCommand::register);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                ProtectedBlocksState.get(server).markSeen(handler.getPlayer().getUUID(), handler.getPlayer().getName().getString(), System.currentTimeMillis());
            } catch (RuntimeException e) {
                warnOnce("join", "Error al registrar la conexión de un jugador", e);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            AccessManager.forgetPlayer(handler.getPlayer().getUUID());
            PermissionHelper.invalidate(handler.getPlayer().getUUID());
            ServerTickHandler.forget(handler.getPlayer().getUUID());
        });
        ServerLifecycleEvents.SERVER_STARTED.register(PurgeHandler::onServerStarted);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> AuditLogger.close());

        LOGGER.info("Secure Mod inicializado");
    }

    /** Registra un aviso una sola vez por clave (sin llenar el log). */
    public static void warnOnce(String key, String message, Throwable error) {
        if (WARNED.add(key)) {
            LOGGER.warn("[Secure Mod] {} (se aplica el comportamiento seguro; este aviso no se repetirá)", message, error);
        }
    }
}
