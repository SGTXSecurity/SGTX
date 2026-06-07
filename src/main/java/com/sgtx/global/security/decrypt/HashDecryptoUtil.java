package com.sgtx.global.security.decrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 거래 문서의 무결성을 최종 확인하는 해시 검증 유틸
public class HashDecryptoUtil {

    public static boolean verifyHash(String plainData, String originalHash) {
        try {
            // SHA-256 해시 생성기
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = md.digest(plainData.getBytes(StandardCharsets.UTF_8));

            // byte 배열을 16진수 문자열로 변환
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            String calculatedHash = sb.toString();

            // 원본 해시와 계산된 해시 비교
            return calculatedHash.equals(originalHash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
