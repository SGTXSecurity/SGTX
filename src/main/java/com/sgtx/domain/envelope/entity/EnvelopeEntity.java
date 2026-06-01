package com.sgtx.domain.envelope.entity;

import com.sgtx.domain.trade.entity.TradeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "envelopes")
@Getter
@Setter
@NoArgsConstructor
public class EnvelopeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "envelope_id")
    private Long envelopeId;

    // trade_id 는 UNIQUE KEY 표기 / 한 거래에 하나의 전자봉투만 매핑
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = false, unique = true)
    private TradeEntity trade;

    @Column(name = "aes_data", columnDefinition = "TEXT", nullable = false)
    private String aesData;

    @Column(name = "rsa_encrypted_key", columnDefinition = "TEXT", nullable = false)
    private String rsaEncryptedKey;

    @Column(name = "signature", columnDefinition = "TEXT", nullable = false)
    private String signature;

    @Column(name = "item_hash", length = 255, nullable = false)
    private String itemHash;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
