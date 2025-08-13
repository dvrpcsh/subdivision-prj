package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import com.subdivision.subdivision_prj.service.PotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pots") //이 컨트롤러의 모든 API는 /api/pots 경로로 시작합니다.
@RequiredArgsConstructor
public class PotController {

    private final PotService potService;

    /**
     * 새로운 팟(Pot)을 생성하는 API
     * @param requestDto 팟 생성에 필요한 데이터
     * @param userDetails JWT 필터를 통해 인증된 사용자의 정보
     * @return 생성된 팟의 정보와 HTTP 상태 코드 201 (Created)
     */
    @PostMapping
    public ResponseEntity<PotResponseDto> createPot(
            @RequestBody PotCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PotResponseDto responseDto = potService.createPot(requestDto, userDetails);

        // ResponseEntity를 사용하여 HTTP 상태 코드 201(Created)과 함께 응답 본문을 반환합니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}
