package com.sgtx.global.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

// 발신자의 전자서명 생성
public class SignatureUtil {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static String createSignature(
            String itemHash,
            PrivateKey privateKey
    ) throws Exception {

        // Signature 객체 생성
        Signature signature =
                Signature.getInstance(SIGNATURE_ALGORITHM);

        // 개인키를 이용한 서명 모드
        signature.initSign(privateKey);
        signature.update(itemHash.getBytes(StandardCharsets.UTF_8));

        // 전자서명 생성
        byte[] signedData = signature.sign();

        // API 전송을 위해 Base64 변환
        return Base64.getEncoder().encodeToString(signedData);
    }
}