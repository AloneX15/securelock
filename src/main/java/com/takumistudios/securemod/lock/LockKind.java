package com.takumistudios.securemod.lock;

import java.util.Locale;

/** Origen de la protección. */
public enum LockKind {
    /** Bloque propio de Secure Mod (puerta reforzada, cofre con contraseña, teclado...). */
    BLOCK,
    /** Candado sobre un bloque existente, vanilla o de otro mod. */
    PADLOCK;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static LockKind byId(String id) {
        return "block".equalsIgnoreCase(id) ? BLOCK : PADLOCK;
    }
}
