package com.alonex15.securelock.data;

import com.alonex15.securelock.SecureLock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Índice global del mundo: bloques protegidos por jugador (para {@code /securelock list}, límites y purga),
 * jugadores de confianza globales ({@code /securelock trust}) y última conexión (purga de inactivos).
 */
public final class ProtectedBlocksState extends SavedData {
    public static final int DATA_VERSION = 1;

    private final Map<UUID, Set<GlobalPos>> blocksByOwner = new HashMap<>();
    private final Map<UUID, Map<UUID, String>> trusted = new HashMap<>();
    private final Map<UUID, Long> lastSeen = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    private record OwnerEntry(String owner, String name, List<GlobalPos> blocks, Map<String, String> trusted, long lastSeen) {
        static final Codec<OwnerEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("owner").forGetter(OwnerEntry::owner),
                Codec.STRING.optionalFieldOf("name", "?").forGetter(OwnerEntry::name),
                GlobalPos.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(OwnerEntry::blocks),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("trusted", Map.of()).forGetter(OwnerEntry::trusted),
                Codec.LONG.optionalFieldOf("last_seen", 0L).forGetter(OwnerEntry::lastSeen)
        ).apply(i, OwnerEntry::new));
    }

    public static final Codec<ProtectedBlocksState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("data_version", DATA_VERSION).forGetter(s -> DATA_VERSION),
            OwnerEntry.CODEC.listOf().optionalFieldOf("owners", List.of()).forGetter(ProtectedBlocksState::toEntries)
    ).apply(i, ProtectedBlocksState::fromEntries));

    public static final SavedDataType<ProtectedBlocksState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SecureLock.MOD_ID, "protected_blocks"),
            ProtectedBlocksState::new,
            CODEC,
            null);

    public ProtectedBlocksState() {
    }

    public static ProtectedBlocksState get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private static ProtectedBlocksState fromEntries(int dataVersion, List<OwnerEntry> entries) {
        ProtectedBlocksState state = new ProtectedBlocksState();
        for (OwnerEntry entry : entries) {
            UUID owner;
            try {
                owner = UUID.fromString(entry.owner());
            } catch (IllegalArgumentException e) {
                continue;
            }
            state.blocksByOwner.put(owner, new HashSet<>(entry.blocks()));
            state.names.put(owner, entry.name());
            if (entry.lastSeen() > 0) {
                state.lastSeen.put(owner, entry.lastSeen());
            }
            Map<UUID, String> trustedMap = new LinkedHashMap<>();
            entry.trusted().forEach((key, name) -> {
                try {
                    trustedMap.put(UUID.fromString(key), name);
                } catch (IllegalArgumentException ignored) {
                }
            });
            if (!trustedMap.isEmpty()) {
                state.trusted.put(owner, trustedMap);
            }
        }
        return state;
    }

    private List<OwnerEntry> toEntries() {
        Set<UUID> owners = new HashSet<>();
        owners.addAll(blocksByOwner.keySet());
        owners.addAll(trusted.keySet());
        owners.addAll(lastSeen.keySet());
        return owners.stream().map(owner -> {
            Map<String, String> trustedRaw = new LinkedHashMap<>();
            trusted.getOrDefault(owner, Map.of()).forEach((uuid, name) -> trustedRaw.put(uuid.toString(), name));
            return new OwnerEntry(owner.toString(), names.getOrDefault(owner, "?"),
                    List.copyOf(blocksByOwner.getOrDefault(owner, Set.of())), trustedRaw, lastSeen.getOrDefault(owner, 0L));
        }).toList();
    }

    // --- Bloques ---

    public void add(UUID owner, String ownerName, GlobalPos pos) {
        blocksByOwner.computeIfAbsent(owner, k -> new HashSet<>()).add(pos);
        names.put(owner, ownerName);
        setDirty();
    }

    public void remove(UUID owner, GlobalPos pos) {
        Set<GlobalPos> set = blocksByOwner.get(owner);
        if (set != null && set.remove(pos)) {
            if (set.isEmpty()) {
                blocksByOwner.remove(owner);
            }
            setDirty();
        }
    }

    public Set<GlobalPos> blocksOf(UUID owner) {
        return Collections.unmodifiableSet(blocksByOwner.getOrDefault(owner, Set.of()));
    }

    public int countOf(UUID owner) {
        return blocksByOwner.getOrDefault(owner, Set.of()).size();
    }

    public Set<UUID> owners() {
        return Collections.unmodifiableSet(blocksByOwner.keySet());
    }

    public int totalBlocks() {
        return blocksByOwner.values().stream().mapToInt(Set::size).sum();
    }

    // --- Confianza global ---

    public boolean trust(UUID owner, UUID target, String targetName) {
        String previous = trusted.computeIfAbsent(owner, k -> new LinkedHashMap<>()).put(target, targetName);
        setDirty();
        return previous == null;
    }

    public boolean untrust(UUID owner, UUID target) {
        Map<UUID, String> map = trusted.get(owner);
        if (map != null && map.remove(target) != null) {
            if (map.isEmpty()) {
                trusted.remove(owner);
            }
            setDirty();
            return true;
        }
        return false;
    }

    public boolean isTrusted(UUID owner, UUID target) {
        Map<UUID, String> map = trusted.get(owner);
        return map != null && map.containsKey(target);
    }

    public Map<UUID, String> trustedBy(UUID owner) {
        return Collections.unmodifiableMap(trusted.getOrDefault(owner, Map.of()));
    }

    // --- Actividad ---

    public void markSeen(UUID player, String name, long now) {
        lastSeen.put(player, now);
        names.put(player, name);
        setDirty();
    }

    public long lastSeen(UUID player) {
        return lastSeen.getOrDefault(player, 0L);
    }

    public String nameOf(UUID player) {
        return names.getOrDefault(player, player.toString());
    }

    /** Busca un jugador ya visto en este mundo por su nombre (sin peticiones de red). */
    public Optional<NameAndId> findByName(String name) {
        return names.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(name))
                .max(Comparator.comparingLong(e -> lastSeen.getOrDefault(e.getKey(), 0L)))
                .map(e -> new NameAndId(e.getKey(), e.getValue()));
    }

    /** Elimina todos los datos de un jugador (purga). Devuelve sus bloques para quitar la protección. */
    public Set<GlobalPos> forget(UUID owner) {
        Set<GlobalPos> removed = blocksByOwner.remove(owner);
        trusted.remove(owner);
        setDirty();
        return removed == null ? Set.of() : removed;
    }
}
