package com.takumistudios.securemod.registry;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.lock.ChunkLocks;
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
            Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, "chunk_locks"),
            builder -> builder.persistent(ChunkLocks.CODEC));

    private ModAttachments() {
    }

    public static void init() {
        // Carga la clase para registrar los adjuntos
    }
}
