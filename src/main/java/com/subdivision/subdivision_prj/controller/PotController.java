package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.dto.PotCreateRequestDto;
import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
import com.subdivision.subdivision_prj.dto.PotResponseDto;
import com.subdivision.subdivision_prj.service.PotService;
import com.subdivision.subdivision_prj.domain.PotCategory;
import com.subdivision.subdivision_prj.domain.PotStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 특정 팟에 참여하는 API
     */
    @PostMapping("/{potId}/join")
    public ResponseEntity<Void> joinPot(
            @PathVariable Long potId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        potService.joinPot(potId, userDetails);

        return ResponseEntity.ok().build();
    }

    /**
     * 특정 팟에 참여를 취소하는 API
     */
    @DeleteMapping("/{potId}/leave")
    public ResponseEntity<Void> leavePot(
            @PathVariable Long potId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        potService.leavePot(potId, userDetails);

        return ResponseEntity.noContent().build();
    }

    /**
     * 2025-08-18 메서드 삭제(아래 searchPots API로 통합)
     * 위치 기반으로 주변의 Pot 목록을 조회하는 API 엔트포인트입니다.
     * 클라이언트는 Query Parameter를 통해 현재 위치와 검색 반경을 전달합니다.
     * 예시 호출: GET /api/v1/pots/nearby?lat=35.179554&lon=129.075642&dist=3
     *
     * @param lat 사용자의 현재 위도(latitude)
     * @param lon 사용자의 현재 경도(longitude)
     * @param dist 검색 반경(km) - 값이 없을 경우 기본값으로 1km를 사용합니다.
     * @return 성공 응답(200 OK)와 함께 검색된 Pot 목록을 Body에 담아 반환합니다.

    @GetMapping("/nearby")
    public ResponseEntity<List<PotResponseDto>> getPotsByLocation(
            @RequestParam("lat") Double lat,
            @RequestParam("lon") Double lon,
            @RequestParam(value = "dist", defaultValue = "1") Double dist) {

        //1.PotService에 정의된 위치 기반 검색 메서드를 호출합니다.
        List<PotResponseDto> pots = potService.findPotsByLocation(lon, lat, dist);

        //2.서비스로부터 받은 DTO 리스트를 ResponseEntity에 담아 클라이언트에 반환합니다.
        return ResponseEntity.ok(pots);
    }
     */

    /**
     * 팟(Pot)을 복합 조건으로 검색하는 API
     * 위치, 키워드, 카테고리, 상태 등 다양한 조건으로 필터링 및 검색하는 API
     * @param lat 사용자 현재 위도 (필수)
     * @param lon 사용자 현재 경도 (필수)
     * @param distance 검색 반경(km) (선택, 기본값 10)
     * @param keyword 검색 키워드 (선택)
     * @param category 카테고리 필터 (선택)
     * @param status 상태 필터 (선택, 기본값 RECRUITING)
     * @param pageable 페이징 정보 (자동 주입)
     * @return 검색 조건에 맞는 팟의 페이징된 목록
     */
    @GetMapping("/search")
    public ResponseEntity<Page<PotResponseDto>> searchPots(
        @RequestParam("lat") Double lat,
        @RequestParam("lon") Double lon,
        @RequestParam(value = "distance", defaultValue = "10") Double distance,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "category", required = false) PotCategory category,
        @RequestParam(value = "status", defaultValue = "RECRUITING") PotStatus status,
        Pageable pageable
    ) {
        Page<PotResponseDto> pots = potService.searchPots(lat, lon, distance, keyword, category, status, pageable);

        return ResponseEntity.ok(pots);
    }
}
