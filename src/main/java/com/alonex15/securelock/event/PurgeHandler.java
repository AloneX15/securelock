package com.alonex15.securelock.event;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.data.LockAdmin;
import com.alonex15.securelock.data.ProtectedBlocksState;
import com.alonex15.securelock.security.AuditLogger;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/** Purga automática opcional de propietarios inactivos al arrancar el servidor. */
public final class PurgeHandler {
    private PurgeHandler() {
    }

    public static void onServerStarted(MinecraftServer server) {
        SecureLockConfig config = SecureLockConfig.get();
        if (!config.autoPurgeInactiveOwners || config.inactiveOwnerDays <= 0) {
            return;
        }
        try {
            ProtectedBlocksState state = ProtectedBlocksState.get(server);
            for (UUID owner : LockAdmin.inactiveOwners(server, config.inactiveOwnerDays)) {
                String name = state.nameOf(owner);
                int removed = LockAdmin.purge(server, owner);
                AuditLogger.log(AuditLogger.Event.ADMIN_PURGE, "server", "-", name + " inactivo, " + removed + " bloques");
                SecureLock.LOGGER.info("[SecureLock] Purga automática: {} ({} bloques)", name, removed);
            }
        } catch (RuntimeException e) {
            SecureLock.warnOnce("auto_purge", "Error en la purga automática", e);
        }
    }
}
