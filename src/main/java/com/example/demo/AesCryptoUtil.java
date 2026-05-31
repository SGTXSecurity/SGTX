package com.example.demo;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

//거래 문서를 AES로 암호화
public class AesCryptoUtil {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    public static SecretKey generateAesKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);
        keyGenerator.init(256);
        return keyGenerator.generateKey();
    }

    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }
}