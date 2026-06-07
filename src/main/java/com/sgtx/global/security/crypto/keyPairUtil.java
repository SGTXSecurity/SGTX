package com.sgtx.global.security.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

// RSA 공개키 / 개인키 생성 유틸
public class keyPairUtil {

    private static final String RSA = "RSA";
// 공개키 + 개인키 한 쌍 생성
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

// String(Base64) -> PublicKey 변환
public static java.security.PublicKey getPublicKeyFromString(String key) throws Exception {
    byte[] byteKey = java.util.Base64.getDecoder().decode(key);
    java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(byteKey);
    java.security.KeyFactory kf = java.security.KeyFactory.getInstance(RSA);
    return kf.generatePublic(spec);
}

// String(Base64) -> PrivateKey 변환
public static java.security.PrivateKey getPrivateKeyFromString(String key) throws Exception {
    byte[] byteKey = java.util.Base64.getDecoder().decode(key);
    java.security.spec.PKCS8EncodedKeySpec spec = new java.security.spec.PKCS8EncodedKeySpec(byteKey);
    java.security.KeyFactory kf = java.security.KeyFactory.getInstance(RSA);
    return kf.generatePrivate(spec);
}
}