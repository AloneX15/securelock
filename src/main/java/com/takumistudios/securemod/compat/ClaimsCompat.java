package com.takumistudios.securemod.compat;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.config.SecureModConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Integración opcional con mods de claims (Open Parties and Claims, Flan). Secure Mod no duplica su función:
 * si {@code claimsGrantAccess} está activado, un jugador con permiso en el claim donde está el bloque también
 * puede usarlo (no romperlo ni configurarlo). Si el mod no está o su API cambia, la integración se desactiva sola.
 */
public final class ClaimsCompat {
    private static final boolean OPAC = FabricLoader.getInstance().isModLoaded("openpartiesandclaims");
    private static final boolean FLAN = FabricLoader.getInstance().isModLoaded("flan");
    private static boolean opacBroken;
    private static boolean flanBroken;

    private ClaimsCompat() {
    }

    public static boolean hasClaimAccess(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!SecureModConfig.get().claimsGrantAccess) {
            return false;
        }
        if (OPAC && !opacBroken) {
            try {
                if (OpacCompat.hasAccess(player, level, pos)) {
                    return true;
                }
            } catch (Throwable t) {
                opacBroken = true;
                SecureMod.warnOnce("opac", "La integración con Open Parties and Claims falló y se desactiva", t);
            }
        }
        if (FLAN && !flanBroken) {
            try {
                if (FlanCompat.hasAccess(player, level, pos)) {
                    return true;
                }
            } catch (Throwable t) {
                flanBroken = true;
                SecureMod.warnOnce("flan", "La integración con Flan falló y se desactiva", t);
            }
        }
        return false;
    }

    /** Solo se carga si Open Parties and Claims está instalado. */
    private static final class OpacCompat {
        static boolean hasAccess(ServerPlayer player, ServerLevel level, BlockPos pos) {
            xaero.pac.common.server.api.OpenPACServerAPI api = xaero.pac.common.server.api.OpenPACServerAPI.get(level.getServer());
            net.minecraft.resources.Identifier dimension = level.dimension().identifier();
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            // Solo dentro de un claim: en terreno sin reclamar no se concede nada
            if (api.getServerClaimsManager().get(dimension, chunkX, chunkZ) == null) {
                return false;
            }
            return api.getChunkProtection().hasChunkAccess(player, dimension, chunkX, chunkZ);
        }
    }

    /** Solo se carga si Flan está instalado. */
    private static final class FlanCompat {
        static boolean hasAccess(ServerPlayer player, ServerLevel level, BlockPos pos) {
            io.github.flemmli97.flan.api.data.IPermissionContainer container =
                    io.github.flemmli97.flan.api.ClaimHandler.getPermissionStorage(level).getForPermissionCheck(pos);
            // GlobalClaim = terreno sin reclamar: no concede acceso
            if (!(container instanceof io.github.flemmli97.flan.claim.Claim)) {
                return false;
            }
            return container.canInteract(player, io.github.flemmli97.flan.api.permission.BuiltinPermission.OPENCONTAINER, pos);
        }
    }
}
