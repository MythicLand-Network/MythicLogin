package cn.mythicland.mythiclogin.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void shouldGenerateAuthMeCompatibleSha256Hash() {
        String hash = passwordService.hashWithSalt("password", "f7d5c7523b2510b6");
        assertEquals(
            "$SHA$f7d5c7523b2510b6$137d10a73c1609c8a9122fe121a217db2ede8e9871144598c2f18e8ee3b013bc",
            hash
        );
    }

    @Test
    void shouldVerifyHashGeneratedWithFixedSalt() {
        String storedHash = "$SHA$15b880cc41beb31c$7e59d3f42f3e915017df2a614955496a2aa281e69a7a43e601857e478241e869";
        assertTrue(passwordService.verify("PassWord1", storedHash));
        assertFalse(passwordService.verify("password", storedHash));
    }

    @Test
    void shouldHandleRealAuthMeSqlHashFormatSafely() {
        String sampleFromAuthMeSql =
            "$SHA$f7d5c7523b2510b6$b0d0842ca208de17ed92de73853f2d53f64c60738e283cc0e9bdcdf5129e8b87";
        assertFalse(passwordService.verify("definitely-not-the-right-password", sampleFromAuthMeSql));
        assertFalse(passwordService.verify("anything", "$SHA$broken"));
        assertFalse(passwordService.verify("anything", null));
    }
}
