package com.sgtx.domain.card.service;

import com.sgtx.domain.card.CardStatus;
import com.sgtx.domain.card.dto.CardRegisterRequest;
import com.sgtx.domain.card.dto.CardRegisterResponse;
import com.sgtx.domain.card.dto.CardResponse;
import com.sgtx.domain.card.entity.CardEntity;
import com.sgtx.domain.card.repository.CardRepository;
import com.sgtx.domain.user.entity.UserEntity;
import com.sgtx.domain.user.repository.UserRepository;
import com.sgtx.global.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    // 카드 등록
    @Transactional
    public CardRegisterResponse registerCard(CardRegisterRequest request) {

        // 사용자 존재 여부 확인
        UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(
                        "존재하지 않는 사용자입니다. userId: " + request.userId()));

        CardEntity card = new CardEntity();
        card.setUser(user);
        card.setCardCompany(request.cardCompany());
        // 원본 카드번호는 저장하지 않고 마스킹된 값만 저장
        card.setMaskedCardNumber(maskCardNumber(request.cardNumber()));
        card.setCardStatus(CardStatus.ACTIVE);

        CardEntity saved = cardRepository.save(card);

        return new CardRegisterResponse(
                saved.getCardId(),
                saved.getCardCompany(),
                saved.getMaskedCardNumber(),
                saved.getCreatedAt()
        );
    }

    // 특정 사용자의 등록 카드 목록 조회
    @Transactional(readOnly = true)
    public List<CardResponse> getCards(Long userId) {

        // 사용자 존재 여부 확인
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("존재하지 않는 사용자입니다. userId: " + userId);
        }

        return cardRepository.findByUser_UserId(userId).stream()
                .map(card -> new CardResponse(
                        card.getCardId(),
                        card.getCardCompany(),
                        card.getMaskedCardNumber(),
                        card.getCreatedAt()
                ))
                .toList();
    }

    // 카드번호를 1234-****-****-5678 형식으로 마스킹
    private String maskCardNumber(String cardNumber) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("\\D", "");
        if (digits.length() < 8) {
            throw new IllegalArgumentException("올바르지 않은 카드 번호입니다.");
        }
        String first4 = digits.substring(0, 4);
        String last4 = digits.substring(digits.length() - 4);
        return first4 + "-****-****-" + last4;
    }
}
