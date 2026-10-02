package com.takumistudios.securemod.event;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.data.LockAdmin;
import com.takumistudios.securemod.data.ProtectedBlocksState;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Purga automática opcional de propietarios inactivos al arrancar el servidor. */
public final class PurgeHandler {
    private PurgeHandler() {
    }

    public static void onServerStarted(MinecraftServer server) {
        SecureModConfig config = SecureModConfig.get();
        if (!config.autoPurgeInactiveOwners || config.inactiveOwnerDays <= 0) {
            return;
        }
        try {
            ProtectedBlocksState state = ProtectedBlocksState.get(server);
            for (UUID owner : LockAdmin.inactiveOwners(server, config.inactiveOwnerDays)) {
                String name = state.nameOf(owner);
                int removed = LockAdmin.purge(server, owner);
                AuditLogger.log(AuditLogger.Event.ADMIN_PURGE, "server", "-", name + " inactivo, " + removed + " bloques");
                SecureMod.LOGGER.info("[Secure Mod] Purga automática: {} ({} bloques)", name, removed);
            }
        } catch (RuntimeException e) {
            SecureMod.warnOnce("auto_purge", "Error en la purga automática", e);
        }
    }
}
