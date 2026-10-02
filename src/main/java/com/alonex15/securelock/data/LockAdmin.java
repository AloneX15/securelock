package com.alonex15.securelock.data;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.block.OwnedBlocks;
import com.alonex15.securelock.lock.LockData;
import com.alonex15.securelock.lock.LockKind;
import com.alonex15.securelock.lock.LockManager;
import com.google.gson.GsonBuilder;
import com.alonex15.securelock.block.SecureLockBlock;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Collection;
import java.util.stream.Stream;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Operaciones de administración masivas (purga, unlockall, export). Son comandos de admin, así que aquí
 * sí se cargan los chunks necesarios (nunca para comprobar una protección durante el juego).
 */
public final class LockAdmin {
    private LockAdmin() {
    }

    /** Recorre todas las protecciones de un jugador. */
    private static void forEachLock(MinecraftServer server, UUID owner, BiConsumer<ServerLevel, LockManager.Locked> action) {
        ProtectedBlocksState state = ProtectedBlocksState.get(server);
        for (GlobalPos globalPos : List.copyOf(state.blocksOf(owner))) {
            ServerLevel level = server.getLevel(globalPos.dimension());
            if (level == null) {
                state.remove(owner, globalPos);
                continue;
            }
            level.getChunkAt(globalPos.pos());
            LockManager.Locked locked = LockManager.getAt(level, globalPos.pos());
            if (locked == null || !locked.lock().isOwner(owner)) {
                state.remove(owner, globalPos);
                continue;
            }
            action.accept(level, locked);
        }
    }

    /** Un candado junto a su nivel, para copias de seguridad. */
    private record Found(ServerLevel level, LockManager.Locked locked) {
    }

    private static List<Found> collect(MinecraftServer server, Collection<UUID> owners) {
        List<Found> found = new ArrayList<>();
        for (UUID owner : owners) {
            forEachLock(server, owner, (level, locked) -> found.add(new Found(level, locked)));
        }
        return found;
    }

    /**
     * Quita todas las protecciones de un jugador. Los bloques propios pasan a ser vanilla (con su contenido).
     * Antes se guarda una copia de seguridad completa para poder hacer rollback.
     */
    public static int purge(MinecraftServer server, UUID owner) {
        return removeAll(server, List.of(owner), "purge-" + ProtectedBlocksState.get(server).nameOf(owner));
    }

    /** Quita TODAS las protecciones del mundo (para desinstalar el mod sin perder nada). */
    public static int unlockAll(MinecraftServer server) {
        return removeAll(server, Set.copyOf(ProtectedBlocksState.get(server).owners()), "unlockall");
    }

    private static int removeAll(MinecraftServer server, Collection<UUID> owners, String reason) {
        List<Found> found = collect(server, owners);
        try {
            backup(found, reason);
        } catch (IOException | RuntimeException e) {
            SecureLock.LOGGER.warn("[SecureLock] No se pudo crear la copia de seguridad antes de {}: {}", reason, e.getMessage());
        }
        for (Found entry : found) {
            removeProtection(entry.level(), entry.locked());
        }
        ProtectedBlocksState state = ProtectedBlocksState.get(server);
        owners.forEach(state::forget);
        return found.size();
    }

    // ---------- Copias de seguridad y rollback ----------

    public static Path backupDir() {
        return FabricLoader.getInstance().getGameDir().resolve("securelock-backups");
    }

    /** Guarda los candados completos (incluido el hash del código) para poder restaurarlos. Archivo privado del servidor. */
    private static void backup(List<Found> found, String reason) throws IOException {
        if (found.isEmpty()) {
            return;
        }
        JsonArray entries = new JsonArray();
        for (Found entry : found) {
            JsonObject json = new JsonObject();
            json.addProperty("dimension", entry.level().dimension().identifier().toString());
            json.addProperty("x", entry.locked().pos().getX());
            json.addProperty("y", entry.locked().pos().getY());
            json.addProperty("z", entry.locked().pos().getZ());
            json.add("lock", LockData.CODEC.encodeStart(JsonOps.INSTANCE, entry.locked().lock()).getOrThrow());
            entries.add(json);
        }
        Files.createDirectories(backupDir());
        String safeReason = reason.replaceAll("[^A-Za-z0-9_-]", "_");
        Path file = backupDir().resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + safeReason + ".json");
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(entries), StandardCharsets.UTF_8);
    }

    public static List<String> backups() {
        if (!Files.isDirectory(backupDir())) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(backupDir())) {
            return files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    /** Resultado de un rollback. */
    public record RollbackResult(int restored, int skipped) {
    }

    /**
     * Restaura las protecciones de una copia. Si un bloque propio se convirtió en vanilla, la protección vuelve
     * como candado. Se omiten las posiciones donde el bloque ya no existe o ya tiene otra protección.
     */
    public static RollbackResult rollback(MinecraftServer server, String fileName) throws IOException {
        if (!fileName.matches("[A-Za-z0-9._-]+\\.json")) {
            throw new IOException("Nombre de archivo no válido");
        }
        Path dir = backupDir().normalize();
        Path file = dir.resolve(fileName).normalize();
        if (!file.startsWith(dir) || !Files.exists(file)) {
            throw new IOException("No existe la copia " + fileName);
        }
        JsonArray entries = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonArray();
        int restored = 0;
        int skipped = 0;
        for (JsonElement element : entries) {
            try {
                JsonObject json = element.getAsJsonObject();
                ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, Identifier.parse(json.get("dimension").getAsString()));
                ServerLevel level = server.getLevel(dimension);
                BlockPos pos = new BlockPos(json.get("x").getAsInt(), json.get("y").getAsInt(), json.get("z").getAsInt());
                LockData lock = LockData.CODEC.parse(JsonOps.INSTANCE, json.get("lock")).getOrThrow();
                if (level == null) {
                    skipped++;
                    continue;
                }
                level.getChunkAt(pos);
                BlockState state = level.getBlockState(pos);
                if (LockManager.getAt(level, pos) != null) {
                    skipped++;
                    continue;
                }
                if (state.getBlock() instanceof SecureLockBlock) {
                    LockManager.put(level, pos, lock);
                } else if (LockManager.isLockable(state)) {
                    LockManager.put(level, pos, new LockData(lock.owner(), lock.ownerName(), lock.mode(), lock.allowed(), lock.denied(),
                            lock.passcodeSalt(), lock.passcodeHash(), lock.cardLevel(), LockKind.PADLOCK, LockManager.blockId(state),
                            lock.createdAt(), lock.dataVersion()));
                } else {
                    skipped++;
                    continue;
                }
                restored++;
            } catch (RuntimeException e) {
                skipped++;
            }
        }
        return new RollbackResult(restored, skipped);
    }

    /** Cambia el propietario de un bloque protegido. */
    public static void transfer(ServerLevel level, LockManager.Locked locked, UUID newOwner, String newOwnerName) {
        LockManager.put(level, locked.pos(), locked.lock().withOwner(newOwner, newOwnerName));
    }

    private static void removeProtection(ServerLevel level, LockManager.Locked locked) {
        try {
            if (locked.lock().kind() == LockKind.BLOCK) {
                OwnedBlocks.convertToVanilla(level, locked.pos());
            }
            LockManager.remove(level, locked.pos());
        } catch (RuntimeException e) {
            SecureLock.warnOnce("purge", "Error al quitar una protección durante una purga", e);
        }
    }

    /** Propietarios inactivos más de {@code days} días. */
    public static List<UUID> inactiveOwners(MinecraftServer server, int days) {
        ProtectedBlocksState state = ProtectedBlocksState.get(server);
        long limit = System.currentTimeMillis() - days * 86_400_000L;
        List<UUID> result = new ArrayList<>();
        for (UUID owner : state.owners()) {
            long seen = state.lastSeen(owner);
            if (seen > 0 && seen < limit && server.getPlayerList().getPlayer(owner) == null) {
                result.add(owner);
            }
        }
        return result;
    }

    /** Exporta todas las protecciones a {@code securelock-export-<fecha>.json} (sin hashes de código). */
    public static Path export(MinecraftServer server) throws IOException {
        JsonArray locks = new JsonArray();
        ProtectedBlocksState state = ProtectedBlocksState.get(server);
        for (UUID owner : Set.copyOf(state.owners())) {
            forEachLock(server, owner, (level, locked) -> locks.add(toJson(level, locked)));
        }
        JsonObject root = new JsonObject();
        root.addProperty("mod", SecureLock.MOD_ID);
        root.addProperty("data_version", LockData.CURRENT_DATA_VERSION);
        root.addProperty("exported_at", LocalDateTime.now().toString());
        root.add("locks", locks);
        Path dir = FabricLoader.getInstance().getGameDir().resolve("securelock-exports");
        Files.createDirectories(dir);
        Path file = dir.resolve("securelock-export-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json");
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root), StandardCharsets.UTF_8);
        return file;
    }

    private static JsonObject toJson(ServerLevel level, LockManager.Locked locked) {
        LockData lock = locked.lock();
        JsonObject json = new JsonObject();
        json.addProperty("dimension", level.dimension().identifier().toString());
        json.addProperty("x", locked.pos().getX());
        json.addProperty("y", locked.pos().getY());
        json.addProperty("z", locked.pos().getZ());
        json.addProperty("block", lock.blockId());
        json.addProperty("kind", lock.kind().id());
        json.addProperty("owner", lock.owner().toString());
        json.addProperty("owner_name", lock.ownerName());
        json.addProperty("mode", lock.mode().id());
        json.addProperty("has_passcode", lock.hasPasscode());
        json.addProperty("card_level", lock.cardLevel());
        JsonArray allowed = new JsonArray();
        lock.allowed().values().forEach(allowed::add);
        json.add("allowed", allowed);
        JsonArray denied = new JsonArray();
        lock.denied().values().forEach(denied::add);
        json.add("denied", denied);
        json.addProperty("created", lock.createdAt());
        return json;
    }
}
