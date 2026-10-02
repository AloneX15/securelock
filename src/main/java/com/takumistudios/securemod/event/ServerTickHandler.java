package com.takumistudios.securemod.event;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.network.Payloads;
import com.takumistudios.securemod.network.ServerNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Trabajo periódico ligero: cada 10 ticks envía al jugador quién es el propietario del bloque que mira
 * (solo cuando cambia), y cada minuto limpia sesiones y bloqueos caducados. Ningún bloque hace tick.
 */
public final class ServerTickHandler {
    private static final int HUD_INTERVAL = 10;
    private static final int CLEANUP_INTERVAL = 1200;
    private static final Map<UUID, Payloads.OwnerInfoS2C> LAST_SENT = new HashMap<>();
    private static int ticks;

    private ServerTickHandler() {
    }

    public static void onEndTick(MinecraftServer server) {
        ticks++;
        if (ticks % CLEANUP_INTERVAL == 0) {
            AccessManager.cleanup();
        }
        if (ticks % HUD_INTERVAL != 0 || !SecureModConfig.get().ownerHud) {
            return;
        }
        try {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                updateHud(player);
            }
        } catch (RuntimeException e) {
            SecureMod.warnOnce("hud", "Error al enviar la información del HUD", e);
        }
    }

    private static void updateHud(ServerPlayer player) {
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        Payloads.OwnerInfoS2C info;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            LockManager.Locked locked = LockManager.find(player.level(), pos);
            info = locked == null ? Payloads.OwnerInfoS2C.none(pos)
                    : new Payloads.OwnerInfoS2C(pos, true, locked.lock().ownerName(), locked.lock().mode().id(),
                    locked.lock().hasPasscode(), locked.lock().kind().id());
        } else {
            info = Payloads.OwnerInfoS2C.none(BlockPos.ZERO);
        }
        Payloads.OwnerInfoS2C last = LAST_SENT.get(player.getUUID());
        if (last != null && sameInfo(last, info)) {
            return;
        }
        LAST_SENT.put(player.getUUID(), info);
        ServerNetworking.send(player, info);
    }

    private static boolean sameInfo(Payloads.OwnerInfoS2C a, Payloads.OwnerInfoS2C b) {
        if (!a.isProtected() && !b.isProtected()) {
            return true;
        }
        return a.isProtected() == b.isProtected() && a.pos().equals(b.pos()) && Objects.equals(a.ownerName(), b.ownerName())
                && Objects.equals(a.mode(), b.mode()) && a.hasCode() == b.hasCode();
    }

    public static void forget(UUID player) {
        LAST_SENT.remove(player);
    }
}
