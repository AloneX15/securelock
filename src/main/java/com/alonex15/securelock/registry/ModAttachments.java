package com.alonex15.securelock.registry;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.lock.ChunkLocks;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Adjuntos de datos (Fabric Data Attachment API). Las protecciones se guardan en el chunk como
 * {@code BlockPos -> LockData}, así el bloque protegido no se reemplaza ni se modifica.
 * Es persistente y NO se sincroniza con el cliente (el hash del código nunca sale del servidor).
 */
public final class ModAttachments {
    public static final AttachmentType<ChunkLocks> CHUNK_LOCKS = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(SecureLock.MOD_ID, "chunk_locks"),
            builder -> builder.persistent(ChunkLocks.CODEC));

    private ModAttachments() {
    }

    public static void init() {
        // Carga la clase para registrar los adjuntos
    }
}
