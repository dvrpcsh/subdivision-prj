package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.Pot;
import lombok.Getter;

@Getter
public class PotResponseDto {

    private final Long potId;
    private final String title;
    private final String content;
    private final String productName;
    private final int maximumHeadcount;
    private final int currentHeadcount;
    private final String authorNickname; //작성자의 전체 정보 대신 닉네임만 전달

    // Pot 엔티티를 파라미터로 받아 DTO로 변환하는 생성자
    public PotResponseDto(Pot pot) {
        this.potId = pot.getId();
        this.title = pot.getTitle();
        this.content = pot.getContent();
        this.productName = pot.getProductName();
        this.maximumHeadcount = pot.getMaximumHeadcount();
        this.currentHeadcount = pot.getCurrentHeadcount();
        this.authorNickname = pot.getUser().getNickname(); // User 엔티티에서 닉네임 정보만 추출
    }
}
