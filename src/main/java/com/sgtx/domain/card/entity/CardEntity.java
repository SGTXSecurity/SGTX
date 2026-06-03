package com.sgtx.domain.card.entity;

import com.sgtx.domain.card.CardStatus;
import com.sgtx.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Getter
@Setter
@NoArgsConstructor
public class CardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long cardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "card_company", length = 50)
    private String cardCompany;

    // 원본 카드번호는 저장하지 않고 마스킹된 값만 저장한다. (예: 1234-****-****-5678)
    @Column(name = "masked_card_number", length = 30)
    private String maskedCardNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_status")
    private CardStatus cardStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
