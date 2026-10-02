package com.takumistudios.securemod.access;

import com.takumistudios.securemod.SecureMod;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Nodos de permiso con la Fabric Permission API (compatible con LuckPerms y otros proveedores).
 * Si ningún proveedor responde, se usa el nivel de OP 2 (game master).
 * Los resultados por jugador se guardan 5 segundos en caché: las comprobaciones de acceso son frecuentes.
 */
public final class PermissionHelper {
    public static final String ADMIN_BYPASS = "admin.bypass";
    public static final String ADMIN_INSPECT = "admin.inspect";
    public static final String ADMIN_REMOVE = "admin.remove";
    /** Nodo entero: límite de bloques protegidos de ese jugador o rango (sobrescribe la config). */
    public static final String LIMIT = "limit";

    private static final long CACHE_MILLIS = 5_000;
    private static final Map<CacheKey, CachedValue> CACHE = new ConcurrentHashMap<>();

    private record CacheKey(UUID player, String node) {
    }

    private record CachedValue(Object value, long expiresAt) {
    }

    private PermissionHelper() {
    }

    public static Identifier node(String path) {
        return Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, path);
    }

    public static boolean has(Player player, String path) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Object cached = cached(player.getUUID(), path);
        if (cached instanceof Boolean value) {
            return value;
        }
        boolean result;
        try {
            result = serverPlayer.checkPermission(node(path), PermissionLevel.GAMEMASTERS);
        } catch (RuntimeException | LinkageError e) {
            SecureMod.warnOnce("permission_api", "Fallo en la Permission API; se usa el nivel de OP", e);
            result = serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
        store(player.getUUID(), path, result);
        return result;
    }

    /** Valor entero de un nodo (p. ej. {@code securemod:limit}), o {@code null} si ningún proveedor lo define. */
    public static @Nullable Integer intValue(Player player, String path) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        Object cached = cached(player.getUUID(), "int:" + path);
        if (cached != null) {
            return cached instanceof Integer value ? value : null;
        }
        Integer result;
        try {
            result = serverPlayer.checkPermission(PermissionNode.ofInteger(node(path)));
        } catch (RuntimeException | LinkageError e) {
            SecureMod.warnOnce("permission_api_int", "Fallo en la Permission API al leer un valor entero", e);
            result = null;
        }
        store(player.getUUID(), "int:" + path, result == null ? Boolean.FALSE : result);
        return result;
    }

    public static boolean has(CommandSourceStack source, String path, PermissionLevel fallback) {
        try {
            return source.checkPermission(node(path), fallback);
        } catch (RuntimeException | LinkageError e) {
            SecureMod.warnOnce("permission_api_cmd", "Fallo en la Permission API; se usa el nivel de OP", e);
            return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        }
    }

    /** Predicado para {@code requires(...)} de los comandos: {@code securemod:command.<name>}. */
    public static Predicate<CommandSourceStack> command(String name, PermissionLevel fallback) {
        return source -> has(source, "command." + name, fallback);
    }

    public static boolean canBypass(Player player) {
        return has(player, ADMIN_BYPASS);
    }

    private static @Nullable Object cached(UUID player, String node) {
        CachedValue value = CACHE.get(new CacheKey(player, node));
        if (value == null || value.expiresAt() < System.currentTimeMillis()) {
            return null;
        }
        return value.value();
    }

    private static void store(UUID player, String node, Object value) {
        CACHE.put(new CacheKey(player, node), new CachedValue(value, System.currentTimeMillis() + CACHE_MILLIS));
    }

    /** Al desconectarse, al recargar la config o al cambiar permisos. */
    public static void invalidate(@Nullable UUID player) {
        if (player == null) {
            CACHE.clear();
        } else {
            CACHE.keySet().removeIf(key -> key.player().equals(player));
        }
    }
}
