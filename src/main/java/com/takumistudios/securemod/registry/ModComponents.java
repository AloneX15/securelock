package com.takumistudios.securemod.registry;

import com.takumistudios.securemod.SecureMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Componentes de datos de los ítems. */
public final class ModComponents {
    /** Propietario al que está vinculada una Keycard (lo escribe el Card Writer). */
    public record CardLink(UUID owner, String ownerName) {
        public static final Codec<CardLink> CODEC = RecordCodecBuilder.create(i -> i.group(
                UUIDUtil.CODEC.fieldOf("owner").forGetter(CardLink::owner),
                Codec.STRING.optionalFieldOf("owner_name", "?").forGetter(CardLink::ownerName)
        ).apply(i, CardLink::new));
        public static final StreamCodec<ByteBuf, CardLink> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, CardLink::owner,
                ByteBufCodecs.stringUtf8(64), CardLink::ownerName,
                CardLink::new);
    }

    public static final DataComponentType<CardLink> CARD_LINK = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, "card_link"),
            DataComponentType.<CardLink>builder().persistent(CardLink.CODEC).networkSynchronized(CardLink.STREAM_CODEC).build());

    private ModComponents() {
    }

    public static void init() {
    }
}
