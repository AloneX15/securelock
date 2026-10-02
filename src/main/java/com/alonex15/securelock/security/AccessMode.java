package com.alonex15.securelock.security;

import java.util.Locale;

/** Modo de acceso de un bloque protegido. */
public enum AccessMode {
    /** Solo propietario, permitidos y quien conozca el código o tenga tarjeta. */
    PRIVATE,
    /** Como el privado, más los miembros del equipo del propietario. */
    SHARED,
    /** Cualquiera puede usarlo, pero solo el propietario puede romperlo o configurarlo. */
    PUBLIC;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AccessMode byId(String id) {
        for (AccessMode mode : values()) {
            if (mode.id().equalsIgnoreCase(id)) {
                return mode;
            }
        }
        return PRIVATE;
    }

    public AccessMode next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
