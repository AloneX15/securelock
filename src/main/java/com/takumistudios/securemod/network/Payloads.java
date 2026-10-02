package com.takumistudios.securemod.network;

import com.takumistudios.securemod.SecureMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Paquetes de red. El cliente solo envía intenciones; el servidor valida todo (distancia, dimensión,
 * que el bloque exista, permiso, longitudes). Las cadenas se leen con un tope para descartar basura.
 */
public final class Payloads {
    /** Tope de lectura de cadenas; el contenido real se valida después con límites más estrictos. */
    public static final int MAX_STRING = 64;
    public static final int MAX_NAMES = 64;

    private Payloads() {
    }

    private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> id(String name) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, name));
    }

    // ---------- Cliente -> Servidor ----------

    public record SubmitCodeC2S(BlockPos pos, String code) implements CustomPacketPayload {
        public static final Type<SubmitCodeC2S> TYPE = id("submit_code");
        public static final StreamCodec<RegistryFriendlyByteBuf, SubmitCodeC2S> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeUtf(p.code, MAX_STRING);
                },
                buf -> new SubmitCodeC2S(buf.readBlockPos(), buf.readUtf(MAX_STRING)));

        @Override
        public Type<SubmitCodeC2S> type() {
            return TYPE;
        }
    }

    public record SetCodeC2S(BlockPos pos, String code) implements CustomPacketPayload {
        public static final Type<SetCodeC2S> TYPE = id("set_code");
        public static final StreamCodec<RegistryFriendlyByteBuf, SetCodeC2S> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeUtf(p.code, MAX_STRING);
                },
                buf -> new SetCodeC2S(buf.readBlockPos(), buf.readUtf(MAX_STRING)));

        @Override
        public Type<SetCodeC2S> type() {
            return TYPE;
        }
    }

    public enum ListAction {
        ALLOW_ADD, ALLOW_REMOVE, DENY_ADD, DENY_REMOVE;

        static ListAction byId(int id) {
            ListAction[] values = values();
            return id >= 0 && id < values.length ? values[id] : null;
        }
    }

    public record UpdateAllowListC2S(BlockPos pos, ListAction action, String name) implements CustomPacketPayload {
        public static final Type<UpdateAllowListC2S> TYPE = id("update_allow_list");
        public static final StreamCodec<RegistryFriendlyByteBuf, UpdateAllowListC2S> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeVarInt(p.action.ordinal());
                    buf.writeUtf(p.name, MAX_STRING);
                },
                buf -> new UpdateAllowListC2S(buf.readBlockPos(), ListAction.byId(buf.readVarInt()), buf.readUtf(MAX_STRING)));

        @Override
        public Type<UpdateAllowListC2S> type() {
            return TYPE;
        }
    }

    public record SetModeC2S(BlockPos pos, String mode, int cardLevel) implements CustomPacketPayload {
        public static final Type<SetModeC2S> TYPE = id("set_mode");
        public static final StreamCodec<RegistryFriendlyByteBuf, SetModeC2S> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeUtf(p.mode, MAX_STRING);
                    buf.writeVarInt(p.cardLevel);
                },
                buf -> new SetModeC2S(buf.readBlockPos(), buf.readUtf(MAX_STRING), buf.readVarInt()));

        @Override
        public Type<SetModeC2S> type() {
            return TYPE;
        }
    }

    // ---------- Servidor -> Cliente ----------

    public record OpenKeypadS2C(BlockPos pos, int minLength, int maxLength) implements CustomPacketPayload {
        public static final Type<OpenKeypadS2C> TYPE = id("open_keypad");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenKeypadS2C> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeVarInt(p.minLength);
                    buf.writeVarInt(p.maxLength);
                },
                buf -> new OpenKeypadS2C(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt()));

        @Override
        public Type<OpenKeypadS2C> type() {
            return TYPE;
        }
    }

    public enum CodeStatus {
        OK, WRONG, LOCKED_OUT, INVALID
    }

    public record CodeResultS2C(CodeStatus status, int secondsRemaining, int attemptsLeft) implements CustomPacketPayload {
        public static final Type<CodeResultS2C> TYPE = id("code_result");
        public static final StreamCodec<RegistryFriendlyByteBuf, CodeResultS2C> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeVarInt(p.status.ordinal());
                    buf.writeVarInt(p.secondsRemaining);
                    buf.writeVarInt(p.attemptsLeft);
                },
                buf -> {
                    int status = buf.readVarInt();
                    CodeStatus[] values = CodeStatus.values();
                    return new CodeResultS2C(status >= 0 && status < values.length ? values[status] : CodeStatus.INVALID, buf.readVarInt(), buf.readVarInt());
                });

        @Override
        public Type<CodeResultS2C> type() {
            return TYPE;
        }
    }

    /** Información para el HUD/tooltip: propietario y modo del bloque que mira el jugador. */
    public record OwnerInfoS2C(BlockPos pos, boolean isProtected, String ownerName, String mode, boolean hasCode, String kind) implements CustomPacketPayload {
        public static final Type<OwnerInfoS2C> TYPE = id("owner_info");
        public static final StreamCodec<RegistryFriendlyByteBuf, OwnerInfoS2C> CODEC = StreamCodec.ofMember(
                (p, buf) -> {
                    buf.writeBlockPos(p.pos);
                    buf.writeBoolean(p.isProtected);
                    buf.writeUtf(p.ownerName, MAX_STRING);
                    buf.writeUtf(p.mode, MAX_STRING);
                    buf.writeBoolean(p.hasCode);
                    buf.writeUtf(p.kind, MAX_STRING);
                },
                buf -> new OwnerInfoS2C(buf.readBlockPos(), buf.readBoolean(), buf.readUtf(MAX_STRING), buf.readUtf(MAX_STRING), buf.readBoolean(), buf.readUtf(MAX_STRING)));

        public static OwnerInfoS2C none(BlockPos pos) {
            return new OwnerInfoS2C(pos, false, "", "", false, "");
        }

        @Override
        public Type<OwnerInfoS2C> type() {
            return TYPE;
        }
    }

    /** Datos del panel de configuración. Nunca incluye el hash ni el código. */
    public record OpenConfigS2C(BlockPos pos, String blockKey, String ownerName, String mode, List<String> allowed, List<String> denied,
                                boolean hasCode, int cardLevel, boolean cardReader, boolean usesCode, int codeMin, int codeMax) implements CustomPacketPayload {
        public static final Type<OpenConfigS2C> TYPE = id("open_config");
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenConfigS2C> CODEC = StreamCodec.ofMember(OpenConfigS2C::write, OpenConfigS2C::read);

        private void write(FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeUtf(blockKey, 256);
            buf.writeUtf(ownerName, MAX_STRING);
            buf.writeUtf(mode, MAX_STRING);
            writeNames(buf, allowed);
            writeNames(buf, denied);
            buf.writeBoolean(hasCode);
            buf.writeVarInt(cardLevel);
            buf.writeBoolean(cardReader);
            buf.writeBoolean(usesCode);
            buf.writeVarInt(codeMin);
            buf.writeVarInt(codeMax);
        }

        private static OpenConfigS2C read(FriendlyByteBuf buf) {
            return new OpenConfigS2C(buf.readBlockPos(), buf.readUtf(256), buf.readUtf(MAX_STRING), buf.readUtf(MAX_STRING),
                    readNames(buf), readNames(buf), buf.readBoolean(), buf.readVarInt(), buf.readBoolean(), buf.readBoolean(),
                    buf.readVarInt(), buf.readVarInt());
        }

        private static void writeNames(FriendlyByteBuf buf, List<String> names) {
            List<String> limited = names.size() > MAX_NAMES ? names.subList(0, MAX_NAMES) : names;
            buf.writeVarInt(limited.size());
            limited.forEach(name -> buf.writeUtf(name, MAX_STRING));
        }

        private static List<String> readNames(FriendlyByteBuf buf) {
            int size = Math.min(buf.readVarInt(), MAX_NAMES);
            String[] names = new String[Math.max(0, size)];
            for (int i = 0; i < names.length; i++) {
                names[i] = buf.readUtf(MAX_STRING);
            }
            return List.of(names);
        }

        @Override
        public Type<OpenConfigS2C> type() {
            return TYPE;
        }
    }

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(SubmitCodeC2S.TYPE, SubmitCodeC2S.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetCodeC2S.TYPE, SetCodeC2S.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(UpdateAllowListC2S.TYPE, UpdateAllowListC2S.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetModeC2S.TYPE, SetModeC2S.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenKeypadS2C.TYPE, OpenKeypadS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CodeResultS2C.TYPE, CodeResultS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OwnerInfoS2C.TYPE, OwnerInfoS2C.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenConfigS2C.TYPE, OpenConfigS2C.CODEC);
    }
}
