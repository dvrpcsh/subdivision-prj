package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.List;

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

    /**
     * 특정 팟을 ID로 조회하는 API
     * @param potId URL 경로에서 추출한 팟의 ID
     * @return 조호된 팟의 정보와 HTTP 상태 코드 200(OK)
     */
    @GetMapping("/{potId}")
    public ResponseEntity<PotResponseDto> getPotById(@PathVariable Long potId) {
        PotResponseDto responseDto = potService.getPotById(potId);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 모든 팟의 목록을 조회하는 API
     * @return 모든 팟의 정보 리스트와 HTTP 상태 코드 200(OK)
     */
    @GetMapping
    public ResponseEntity<List<PotResponseDto>> getAllPots() {
        List<PotResponseDto> responseDtoList = potService.getAllPots();

        return ResponseEntity.ok(responseDtoList);
    }

    /**
     * 특정 팟을 수정하는 API
     */
    @PutMapping("/{potId}")
    public ResponseEntity<PotResponseDto> updatePot(
            @PathVariable Long potId,
            @RequestBody PotUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        PotResponseDto responseDto = potService.updatePot(potId, requestDto, userDetails);

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 팟을 삭제하는 API
     */
    @DeleteMapping("/{potId}")
    public ResponseEntity<Void> deletePot(
            @PathVariable Long potId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        potService.deletePot(potId, userDetails);
        //성공적으로 삭제되었을 경우, 본문(body)없이 204 No Content 상태 코드를 반환하는 것이 표준적입니다.
        return ResponseEntity.noContent().build();
    }
}
