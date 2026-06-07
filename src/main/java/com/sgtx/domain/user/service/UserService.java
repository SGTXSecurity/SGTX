package com.sgtx.domain.user.service;

import com.sgtx.domain.user.dto.PublicKeyResponse;
import com.sgtx.domain.user.entity.UserEntity;
import com.sgtx.domain.user.repository.UserRepository;
import com.sgtx.global.security.crypto.keyPairUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.KeyPair;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public PublicKeyResponse getPublicKey(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        return new PublicKeyResponse(user.getPublicKey());
    }

    //키 생성(초ㅣ초 1회)
    @Transactional
    public void generateKeysIfAbsent(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 최초 검사
        boolean hasRealKey =
                user.getPublicKey() != null
                        && user.getPrivateKey() != null
                        && !user.getPublicKey().isBlank()
                        && !user.getPrivateKey().isBlank()
                        //더미 키 검사(테스트 용)
                        && !user.getPublicKey().startsWith("RSA_PUBLIC_KEY")
                        && !user.getPrivateKey().startsWith("RSA_PRIVATE_KEY");

        if (hasRealKey) {
            return;
        }

        try {
            KeyPair keyPair = keyPairUtil.generateKeyPair();

            String publicKey = Base64.getEncoder()
                    .encodeToString(keyPair.getPublic().getEncoded());

            String privateKey = Base64.getEncoder()
                    .encodeToString(keyPair.getPrivate().getEncoded());

            user.setPublicKey(publicKey);
            user.setPrivateKey(privateKey);

            userRepository.save(user);

        } catch (Exception e) {
            throw new IllegalStateException("RSA 키 생성 실패: " + e.getMessage(), e);
        }
    }
}