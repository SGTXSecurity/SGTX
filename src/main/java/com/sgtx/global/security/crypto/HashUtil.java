package com.sgtx.global.security.crypto;

import java.security.MessageDigest;

// 거래 문서의 무결성 검증을 위한 해시 유틸
public class HashUtil {

    public static String generateSHA1(String data) throws Exception {

    // SHA-256 해시 생성
    // 거래 데이터의 위조 여부를 확인하기 위해 사용
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // [보안 취약점: 문자 인코딩 명시 없음]
        // getBytes()는 실행 환경의 기본 인코딩을 사용한다.
        byte[] hashBytes = md.digest(data.getBytes());

        // byte 배열을 16진수 문자열로 변환
        StringBuilder sb = new StringBuilder();

        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }
}