package cn.mythicland.mythiclogin.service;

import cn.mythicland.framework.component.InjectComponent;
import cn.mythicland.framework.security.PasswordHashes;

@InjectComponent
public final class PasswordService {
    private final PasswordHashes hashes = new PasswordHashes();

    public String hash(String plainText) {
        return hashes.hash(plainText);
    }

    String hashWithSalt(String plainText, String salt) {
        return hashes.hashWithSalt(plainText, salt);
    }

    public boolean verify(String plainText, String storedHash) {
        return hashes.verify(plainText, storedHash);
    }
}
