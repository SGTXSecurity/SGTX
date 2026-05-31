package com.sgtx.global.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.PublicKey;
import java.util.Base64;

// AES 키를 RSA 공개키로 암호화
public class RsaCryptoUtil {

    private static final String RSA = "RSA";

    public static String encryptAesKey(
            SecretKey aesKey,
            PublicKey publicKey
    ) throws Exception {

        // RSA Cipher 생성
        Cipher cipher = Cipher.getInstance(RSA);

        // 공개키 암호화 모드
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        // AES 키를 byte 배열로 변환 후 암호화
        byte[] encryptedKey =
                cipher.doFinal(aesKey.getEncoded());

        // 문자열로 변환하여 저장
        return Base64.getEncoder()
                .encodeToString(encryptedKey);
    }
}