package com.alonex15.securelock;

import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.access.PermissionHelper;
import com.alonex15.securelock.command.SecureLockCommand;
import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.data.ProtectedBlocksState;
import com.alonex15.securelock.event.BlockBreakHandler;
import com.alonex15.securelock.event.PurgeHandler;
import com.alonex15.securelock.event.ServerTickHandler;
import com.alonex15.securelock.event.UseBlockHandler;
import com.alonex15.securelock.network.Payloads;
import com.alonex15.securelock.network.ServerNetworking;
import com.alonex15.securelock.registry.ModAttachments;
import com.alonex15.securelock.registry.ModBlocks;
import com.alonex15.securelock.registry.ModComponents;
import com.alonex15.securelock.registry.ModItems;
import com.alonex15.securelock.security.AuditLogger;
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

public final class SecureLock implements ModInitializer {
    public static final String MOD_ID = "securelock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    @Override
    public void onInitialize() {
        SecureLockConfig.load();
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
        CommandRegistrationCallback.EVENT.register(SecureLockCommand::register);

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

        LOGGER.info("SecureLock inicializado");
    }

    /** Registra un aviso una sola vez por clave (sin llenar el log). */
    public static void warnOnce(String key, String message, Throwable error) {
        if (WARNED.add(key)) {
            LOGGER.warn("[SecureLock] {} (se aplica el comportamiento seguro; este aviso no se repetirá)", message, error);
        }
    }
}
