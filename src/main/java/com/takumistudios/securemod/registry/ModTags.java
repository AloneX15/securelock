package com.takumistudios.securemod.registry;

import com.takumistudios.securemod.SecureMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Tags de datapack: los dueños de servidores y modpacks ajustan qué se puede bloquear sin tocar código. */
public final class ModTags {
    /** Bloques que el Padlock puede bloquear. */
    public static final TagKey<Block> LOCKABLE = block("lockable");
    /** Bloques que nunca se pueden bloquear, aunque estén en {@link #LOCKABLE}. */
    public static final TagKey<Block> NEVER_LOCK = block("never_lock");

    private ModTags() {
    }

    private static TagKey<Block> block(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, path));
    }
}
