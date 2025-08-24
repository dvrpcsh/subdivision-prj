package com.subdivision.subdivision_prj.dto;

import com.subdivision.subdivision_prj.domain.Pot;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class PotResponseDto {

    private final Long potId;
    private final String title;
    private final String content;
    private final String productName;
    private final int maximumHeadcount;
    private final int currentHeadcount;
    private final String authorNickname; //작성자의 전체 정보 대신 닉네임만 전달
    private Double latitude;
    private Double longitude;
    private List<MemberInfo> members;
    private String imageUrl;
    private String address;
    private String detailAddress;
    private boolean currentUserJoined; //현재 사용자가 이 팟에 참여했는지 여부

    // Pot 엔티티를 파라미터로 받아 DTO로 변환하는 생성자
    public PotResponseDto(Pot pot) {
        this.potId = pot.getId();
        this.title = pot.getTitle();
        this.content = pot.getContent();
        this.productName = pot.getProductName();
        this.maximumHeadcount = pot.getMaximumHeadcount();
        this.currentHeadcount = pot.getCurrentHeadcount();
        this.authorNickname = pot.getUser().getNickname(); // User 엔티티에서 닉네임 정보만 추출
        this.latitude = pot.getLatitude();
        this.longitude = pot.getLongitude();
        this.imageUrl = pot.getImageUrl();
        this.members = pot.getMembers().stream()
                .map(potMember -> new MemberInfo(potMember.getUser().getNickname()))
                .collect(Collectors.toList());
        this.address = pot.getAddress();
        this.detailAddress = pot.getDetailAddress();
    }

    //멤버 닉네임만 담는 간단한 내부 클래스
    @Getter
    private static class MemberInfo {
        private String nickname;

        public MemberInfo(String nickname) {
            this.nickname = nickname;
        }
    }

    //서비스 레이어에서 사전 서명된 URL을 설정하기 위한 Setter
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    //참여 여부를 설정하기 위한 setter
    public void setCurrentUserJoined(boolean currentUserJoined) {
        this.currentUserJoined = currentUserJoined;
    }
}
