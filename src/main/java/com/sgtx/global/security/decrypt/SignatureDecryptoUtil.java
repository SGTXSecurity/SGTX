package com.sgtx.global.security.decrypt;

import java.security.PublicKey;
import java.security.Signature;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

// 발신자의 전자서명 검증
public class SignatureDecryptoUtil {

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    public static boolean verifySignature(
            String itemHash,
            String signatureStr,
            PublicKey publicKey
    ) {
        try {
            // Signature 객체 생성
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);

            // 공개키를 이용한 검증 모드 초기화
            signature.initVerify(publicKey);

            // 검증할 원본 데이터(해시값) 설정
            // [취약점 유지: 인코딩 명시 없음]
            signature.update(itemHash.getBytes());

            // Base64 디코딩 후 서명 일치 여부 확인
            byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);
            return signature.verify(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
