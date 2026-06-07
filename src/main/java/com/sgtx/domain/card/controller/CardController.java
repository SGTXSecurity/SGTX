package com.sgtx.domain.card.controller;

import com.sgtx.domain.card.dto.CardRegisterRequest;
import com.sgtx.domain.card.dto.CardRegisterResponse;
import com.sgtx.domain.card.dto.CardResponse;
import com.sgtx.domain.card.service.CardService;
import com.sgtx.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

//카드 API 명세
@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    // 카드 등록 Psost
    @PostMapping
    public ApiResponse<CardRegisterResponse> registerCard(
            @RequestBody CardRegisterRequest request
    ) {
        CardRegisterResponse response = cardService.registerCard(request);

        return new ApiResponse<>(
                200,
                "카드가 성공적으로 등록되었습니다.",
                response
        );
    }

    // 사용자별 등록 카드 목록 조회
    @GetMapping("/{userId}")
    public ApiResponse<List<CardResponse>> getCards(
            @PathVariable Long userId
    ) {
        List<CardResponse> response = cardService.getCards(userId);

        return new ApiResponse<>(
                200,
                "등록된 카드 목록 조회 성공",
                response
        );
    }
}
