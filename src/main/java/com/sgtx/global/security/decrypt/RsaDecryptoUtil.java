package com.sgtx.global.security.decrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.PrivateKey;
import java.util.Base64;

// RSA 개인키를 사용하여 암호화된 AES 키를 복호화
public class RsaDecryptoUtil {

    private static final String RSA = "RSA";

    public static SecretKey decryptAesKey(
            String encryptedAesKey,
            PrivateKey privateKey
    ) throws Exception {

        // RSA Cipher 생성
        Cipher cipher = Cipher.getInstance(RSA);

        // 개인키 복호화 모드 초기화
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        // Base64 문자열을 byte 배열로 변환 후 복호화
        byte[] decodedKey = Base64.getDecoder().decode(encryptedAesKey);
        byte[] decryptedKeyBytes = cipher.doFinal(decodedKey);

        // 복호화된 byte 배열을 다시 SecretKey 객체로 변환 (AES)
        return new SecretKeySpec(decryptedKeyBytes, "AES");
    }
}
