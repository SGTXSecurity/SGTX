package com.sgtx.global.security.decrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.util.Base64;

public class AesDecryptoUtil {

    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    public static String decrypt(String encryptedText, SecretKey secretKey) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            // 앞의 16바이트에서 IV 추출
            if (combined.length < IV_SIZE) {
                throw new IllegalArgumentException("암호화된 데이터가 너무 짧습니다.");
            }
            byte[] iv = new byte[IV_SIZE];
            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 나머지에서 실제 암호문 추출
            byte[] encrypted = new byte[combined.length - IV_SIZE];
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(AES_CBC);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decryptedBytes = cipher.doFinal(encrypted);

            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
