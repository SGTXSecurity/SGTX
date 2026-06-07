package com.sgtx.global.security.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

//거래 문서를 AES로 암호화
public class AesCryptoUtil {

    // 사용할 암호화 알고리즘
    private static final String AES = "AES";

    // ECB 모드는 같은 평문 블록이 같은 암호문 블록으로 변환된다.
    // 따라서 데이터 패턴이 노출될 수 있다.
    // 실제 서비스에서는 AES/GCM 또는 AES/CBC + IV 사용이 권장된다.
    private static final String AES_ECB = "AES/ECB/PKCS5Padding";

     //거래 문서를 암호화할 때 사용할 대칭키를 만든다
    public static SecretKey generateAesKey() throws Exception {

        // AES 키 생성기 생성
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES);

        // 키 길이를 명확히 지정하지 않으면 환경에 따라 기본 키 길이가 사용될 수 있다.
        // 실제 서비스에서는 128 또는 256 비트처럼 명확히 지정하는 것이 좋다.
        return keyGenerator.generateKey();
    }

    public static String encrypt(String plainText, SecretKey secretKey) throws Exception {

        // AES 암호화 객체 생성
        Cipher cipher = Cipher.getInstance(AES_ECB);

        // 공개키/개인키 방식이 아니라 같은 secretKey로 암호화와 복호화를 수행한다.
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // getBytes()는 실행 환경의 기본 인코딩을 사용한다.
        // 환경이 달라지면 암호화 결과나 복호화 결과가 달라질 수 있다.
        byte[] encrypted = cipher.doFinal(plainText.getBytes());

        // [보안 취약점 4: 무결성 검증 부재]
        // 따라서 별도의 SHA-1/SHA-256 해시 검증이나 전자서명이 필요하다.
        //but 따로 만들어줄거임

        // byte 배열은 DB 저장이나 API 전송이 불편하므로 Base64 문자열로 변환
        return Base64.getEncoder().encodeToString(encrypted);
    }
}