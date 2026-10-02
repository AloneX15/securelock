package com.alonex15.securelock.lock;

import com.alonex15.securelock.security.AccessMode;
import com.alonex15.securelock.security.PasscodeHasher;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Datos de protección de un bloque. Inmutable: cada cambio crea una copia (copy-on-write),
 * así el adjunto del chunk siempre se marca como modificado y se guarda.
 *
 * <p>El hash y la salt nunca se sincronizan con el cliente: el adjunto no tiene {@code syncWith}.
 */
public record LockData(
        UUID owner,
        String ownerName,
        AccessMode mode,
        Map<UUID, String> allowed,
        Map<UUID, String> denied,
        Optional<String> passcodeSalt,
        Optional<String> passcodeHash,
        int cardLevel,
        LockKind kind,
        String blockId,
        long createdAt,
        int dataVersion
) {
    /** Versión actual del formato. Si un mundo trae una mayor, el bloque queda cerrado para todos excepto admins. */
    public static final int CURRENT_DATA_VERSION = 1;
    public static final UUID CORRUPT_OWNER = new UUID(0L, 0L);

    private static final Codec<Map<UUID, String>> MEMBERS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).xmap(
            raw -> {
                Map<UUID, String> result = new LinkedHashMap<>();
                raw.forEach((key, name) -> {
                    try {
                        result.put(UUID.fromString(key), name);
                    } catch (IllegalArgumentException ignored) {
                        // Entrada corrupta: se descarta sin romper el resto
                    }
                });
                return Map.copyOf(result);
            },
            members -> {
                Map<String, String> raw = new LinkedHashMap<>();
                members.forEach((uuid, name) -> raw.put(uuid.toString(), name));
                return raw;
            });

    public static final Codec<LockData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.optionalFieldOf("owner", CORRUPT_OWNER).forGetter(LockData::owner),
            Codec.STRING.optionalFieldOf("owner_name", "?").forGetter(LockData::ownerName),
            Codec.STRING.xmap(AccessMode::byId, AccessMode::id).optionalFieldOf("mode", AccessMode.PRIVATE).forGetter(LockData::mode),
            MEMBERS_CODEC.optionalFieldOf("allowed", Map.of()).forGetter(LockData::allowed),
            MEMBERS_CODEC.optionalFieldOf("denied", Map.of()).forGetter(LockData::denied),
            Codec.STRING.optionalFieldOf("salt").forGetter(LockData::passcodeSalt),
            Codec.STRING.optionalFieldOf("hash").forGetter(LockData::passcodeHash),
            Codec.INT.optionalFieldOf("card_level", 0).forGetter(LockData::cardLevel),
            Codec.STRING.xmap(LockKind::byId, LockKind::id).optionalFieldOf("kind", LockKind.PADLOCK).forGetter(LockData::kind),
            Codec.STRING.optionalFieldOf("block", "").forGetter(LockData::blockId),
            Codec.LONG.optionalFieldOf("created", 0L).forGetter(LockData::createdAt),
            Codec.INT.optionalFieldOf("data_version", CURRENT_DATA_VERSION).forGetter(LockData::dataVersion)
    ).apply(i, LockData::new));

    public LockData {
        allowed = Map.copyOf(allowed);
        denied = Map.copyOf(denied);
        ownerName = ownerName == null ? "?" : ownerName;
        cardLevel = Math.max(0, Math.min(5, cardLevel));
    }

    public static LockData create(UUID owner, String ownerName, LockKind kind, String blockId) {
        return new LockData(owner, ownerName, AccessMode.PRIVATE, Map.of(), Map.of(), Optional.empty(), Optional.empty(),
                0, kind, blockId, System.currentTimeMillis(), CURRENT_DATA_VERSION);
    }

    /** Datos corruptos o de una versión futura del mod: solo los admins pueden acceder. */
    public boolean isCorrupt() {
        return owner.equals(CORRUPT_OWNER) || dataVersion > CURRENT_DATA_VERSION;
    }

    public boolean isOwner(UUID uuid) {
        return owner.equals(uuid);
    }

    public boolean hasPasscode() {
        return passcodeSalt.isPresent() && passcodeHash.isPresent();
    }

    public boolean checkPasscode(String code) {
        return hasPasscode() && PasscodeHasher.verify(passcodeSalt.get(), passcodeHash.get(), code);
    }

    public LockData withPasscode(String code) {
        if (code == null || code.isEmpty()) {
            return new LockData(owner, ownerName, mode, allowed, denied, Optional.empty(), Optional.empty(), cardLevel, kind, blockId, createdAt, dataVersion);
        }
        PasscodeHasher.Hashed hashed = PasscodeHasher.hash(code);
        return new LockData(owner, ownerName, mode, allowed, denied, Optional.of(hashed.salt()), Optional.of(hashed.hash()), cardLevel, kind, blockId, createdAt, dataVersion);
    }

    public LockData withMode(AccessMode newMode) {
        return new LockData(owner, ownerName, newMode, allowed, denied, passcodeSalt, passcodeHash, cardLevel, kind, blockId, createdAt, dataVersion);
    }

    public LockData withCardLevel(int level) {
        return new LockData(owner, ownerName, mode, allowed, denied, passcodeSalt, passcodeHash, level, kind, blockId, createdAt, dataVersion);
    }

    public LockData withOwner(UUID newOwner, String newOwnerName) {
        return new LockData(newOwner, newOwnerName, mode, allowed, denied, passcodeSalt, passcodeHash, cardLevel, kind, blockId, createdAt, CURRENT_DATA_VERSION);
    }

    public LockData withBlockId(String newBlockId) {
        return new LockData(owner, ownerName, mode, allowed, denied, passcodeSalt, passcodeHash, cardLevel, kind, newBlockId, createdAt, dataVersion);
    }

    public LockData withAllowed(UUID uuid, String name, boolean add) {
        Map<UUID, String> newAllowed = new LinkedHashMap<>(allowed);
        Map<UUID, String> newDenied = new LinkedHashMap<>(denied);
        if (add) {
            newAllowed.put(uuid, name);
            newDenied.remove(uuid);
        } else {
            newAllowed.remove(uuid);
        }
        return new LockData(owner, ownerName, mode, newAllowed, newDenied, passcodeSalt, passcodeHash, cardLevel, kind, blockId, createdAt, dataVersion);
    }

    public LockData withDenied(UUID uuid, String name, boolean add) {
        Map<UUID, String> newAllowed = new LinkedHashMap<>(allowed);
        Map<UUID, String> newDenied = new LinkedHashMap<>(denied);
        if (add) {
            newDenied.put(uuid, name);
            newAllowed.remove(uuid);
        } else {
            newDenied.remove(uuid);
        }
        return new LockData(owner, ownerName, mode, newAllowed, newDenied, passcodeSalt, passcodeHash, cardLevel, kind, blockId, createdAt, dataVersion);
    }
}
