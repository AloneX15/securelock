package com.alonex15.securelock.lock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapa {@code BlockPos -> LockData} adjunto a cada chunk con la Fabric Data Attachment API.
 * Inmutable: se reemplaza entero en cada cambio para que el chunk se marque como modificado.
 */
public record ChunkLocks(Map<BlockPos, LockData> locks) {
    public static final ChunkLocks EMPTY = new ChunkLocks(Map.of());

    private record Entry(BlockPos pos, LockData lock) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
                LockData.CODEC.fieldOf("lock").forGetter(Entry::lock)
        ).apply(i, Entry::new));
    }

    public static final Codec<ChunkLocks> CODEC = Entry.CODEC.listOf().xmap(
            entries -> {
                Map<BlockPos, LockData> map = new HashMap<>();
                for (Entry entry : entries) {
                    map.put(entry.pos().immutable(), entry.lock());
                }
                return new ChunkLocks(map);
            },
            chunkLocks -> chunkLocks.locks().entrySet().stream().map(e -> new Entry(e.getKey(), e.getValue())).toList()
    );

    public ChunkLocks {
        locks = Map.copyOf(locks);
    }

    public LockData get(BlockPos pos) {
        return locks.get(pos);
    }

    public ChunkLocks with(BlockPos pos, LockData lock) {
        Map<BlockPos, LockData> copy = new HashMap<>(locks);
        copy.put(pos.immutable(), lock);
        return new ChunkLocks(copy);
    }

    public ChunkLocks without(BlockPos pos) {
        if (!locks.containsKey(pos)) {
            return this;
        }
        Map<BlockPos, LockData> copy = new HashMap<>(locks);
        copy.remove(pos);
        return new ChunkLocks(copy);
    }

    public boolean isEmpty() {
        return locks.isEmpty();
    }

    public List<BlockPos> positions() {
        return List.copyOf(locks.keySet());
    }
}
