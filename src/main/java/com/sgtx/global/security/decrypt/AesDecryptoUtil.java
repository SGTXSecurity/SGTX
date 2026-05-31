package com.sgtx.global.security.decrypt;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import javax.crypto.NoSuchPaddingException;
import java.util.Base64;

// AES 대칭키를 사용하여 암호화된 거래 문서를 복호화
public class AesDecryptoUtil {

    private static final String AES_ECB = "AES/ECB/PKCS5Padding";

    public static String decrypt(String encryptedText, SecretKey secretKey) {
        try {
            // AES 복호화 객체 생성
            Cipher cipher = Cipher.getInstance(AES_ECB);

            // 복호화 모드 초기화
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            // Base64 문자열을 byte 배열로 변환 후 복호화
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            // [취약점 유지: 인코딩 명시 없음]
            // 실행 환경의 기본 인코딩을 사용하여 문자열로 변환
            return new String(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
