package com.sgtx.global.security.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

// RSA 공개키 / 개인키 생성 유틸
public class keyPairUtil {

    private static final String RSA = "RSA";

    public static KeyPair generateKeyPair() throws Exception {

        // RSA 키 쌍 생성기 생성
        KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance(RSA);

        // [보안 취약점: 짧은 키 길이 사용]
        // 1024비트 RSA는 현재 기준으로 안전성이 낮다.
        // 실제 서비스에서는 2048비트 이상을 사용하는 것이 권장된다.
        keyPairGenerator.initialize(1024);

        // 공개키 + 개인키 한 쌍 생성
        return keyPairGenerator.generateKeyPair();
    }
}