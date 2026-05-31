package com.example.demo.domain.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradeCreateRequest {

    private Long buyerId;
    private Long sellerId;
    private Long itemId;
    private String encryptedData;
    private String encryptedSessionKey;
    private String signature;
    private String itemHash;
}
