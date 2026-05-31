package com.example.demo.domain.trade.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TradeCreateResponse {

    private Long tradeId;
    private String status;
    private String message;
}
