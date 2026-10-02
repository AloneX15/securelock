package com.takumistudios.securemod.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasscodeHasherTest {
    @Test
    void hashIsNeverThePlainCode() {
        PasscodeHasher.Hashed hashed = PasscodeHasher.hash("1234");
        assertNotEquals("1234", hashed.hash());
        assertEquals(64, hashed.hash().length(), "SHA-256 en hexadecimal");
        assertEquals(PasscodeHasher.SALT_BYTES * 2, hashed.salt().length());
    }

    @Test
    void verifiesCorrectCodeOnly() {
        PasscodeHasher.Hashed hashed = PasscodeHasher.hash("4821");
        assertTrue(PasscodeHasher.verify(hashed.salt(), hashed.hash(), "4821"));
        assertFalse(PasscodeHasher.verify(hashed.salt(), hashed.hash(), "4822"));
        assertFalse(PasscodeHasher.verify(hashed.salt(), hashed.hash(), ""));
        assertFalse(PasscodeHasher.verify(hashed.salt(), hashed.hash(), null));
        assertFalse(PasscodeHasher.verify(null, hashed.hash(), "4821"));
    }

    @Test
    void saltMakesEqualCodesProduceDifferentHashes() {
        PasscodeHasher.Hashed a = PasscodeHasher.hash("0000");
        PasscodeHasher.Hashed b = PasscodeHasher.hash("0000");
        assertNotEquals(a.salt(), b.salt());
        assertNotEquals(a.hash(), b.hash());
    }

    @Test
    void hashIsDeterministicForSameSalt() {
        assertEquals(PasscodeHasher.hash("abcd", "1234"), PasscodeHasher.hash("abcd", "1234"));
    }

    @Test
    void validatesCodeFormat() {
        assertTrue(PasscodeHasher.isValidCode("1234", 4, 16));
        assertTrue(PasscodeHasher.isValidCode("1234567890123456", 4, 16));
        assertFalse(PasscodeHasher.isValidCode("123", 4, 16), "demasiado corto");
        assertFalse(PasscodeHasher.isValidCode("12345678901234567", 4, 16), "demasiado largo");
        assertFalse(PasscodeHasher.isValidCode("12a4", 4, 16), "solo dígitos");
        assertFalse(PasscodeHasher.isValidCode("12 4", 4, 16));
        assertFalse(PasscodeHasher.isValidCode("１２３４", 4, 16), "dígitos no ASCII");
        assertFalse(PasscodeHasher.isValidCode(null, 4, 16));
        assertFalse(PasscodeHasher.isValidCode("9".repeat(10_000), 1, 16), "paquete manipulado de 10 000 caracteres");
    }
}
