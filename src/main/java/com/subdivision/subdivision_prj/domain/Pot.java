package com.subdivision.subdivision_prj.domain;

import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pots")
public class Pot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pot_id")
    private Long id;

    // '작성자' 정보를 담기 위해 User 엔티티와 관계를 맺습니다.
    // @ManyToOne: Pot(N) : User(1) 관계. 한 명의 유저는 여러 개의 팟을 만들 수 있습니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // 'user_id'라는 이름의 외래 키를 생성합니다.
    private User user;

    @Column(nullable = false)
    private String title; //제목

    @Column(nullable = false)
    private String content; //내용

    @Column(nullable = false)
    private String productName; //소분할 상품명

    @Column(nullable = false)
    private int maximumHeadcount; //최대 참여 인원

    @Column(nullable = false)
    private int currentHeadcount; //현재 참여 인원

    @Column(nullable = false)
    private Double latitude; //게시물의 위치 정보(위도)

    @Column(nullable = false)
    private Double longitude; //게시물의 위치 정보(경도)

    //주소와 상세주소 저장 필드
    private String address;
    private String detailAddress;

    //S3에 업로드 된 이미지의 URL을 저장할 필드
    //이미지는 선택 사항일 수 있으므로 nullable = true로 설정합니다.
    @Column(length = 512, nullable = true)
    private String imageUrl;

    //mappedBy는 PotMember 엔티티의 'pot' 필드를 가리킵니다.
    //이 팟에 속한 참여자 목록을 의미합니다.
    @OneToMany(mappedBy = "pot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PotMember> members = new ArrayList<>();

    //팟의 카테고리를 저장할 필드
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'ETC'")
    private PotCategory category;

    //팟의 모집 상태를 저장할 필드
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'RECRUITING'")
    private PotStatus status;

    //빌더 패턴을 사용하여 객체를 생성합니다.
    @Builder
    public Pot(User user, String title, String content, String productName, int maximumHeadcount, int currentHeadcount, Double latitude, Double longitude, String imageUrl, PotCategory category, String address, String detailAddress) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.productName = productName;
        this.maximumHeadcount = maximumHeadcount;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.category = category;
        this.status = PotStatus.RECRUITING; // 팟 생성 시, 기본 상태를 '모집중'으로 설정합니다.
        this.currentHeadcount = 1; // 팟 생성 시, 작성자를 포함하여 현재 인원을 1로 초기화합니다.
        this.address = address;
        this.detailAddress = detailAddress;
    }

    public void update(PotUpdateRequestDto requestDto) {
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.productName = requestDto.getProductName();
        this.maximumHeadcount = requestDto.getMaximumHeadcount();
        this.imageUrl = requestDto.getImageUrl();
        this.latitude = requestDto.getLatitude();
        this.longitude = requestDto.getLongitude();
        this.category = requestDto.getCategory();
        this.address = requestDto.getAddress();
        this.detailAddress = requestDto.getDetailAddress();
    }

    //현재 인원을 1 증가시키는 메서드
    public void addParticipant() {
        if(this.currentHeadcount >= this.maximumHeadcount) {
            throw new IllegalArgumentException("모집 인원이 마감되었습니다.");
        }

        this.currentHeadcount++;

        //인원 수가 최대치에 도달하면 상태를 '모집완료'로 변경합니다.
        if(this.currentHeadcount == this.maximumHeadcount) {
            this.status = PotStatus.COMPLETED;
        }
    }

    //현재 인원을 1 감소시키는 메서드
    public void removeParticipant() {
        if(this.currentHeadcount <= 1) {
            //최소 1명(작성자)은 있어야 하므로 1이하로 내려갈 수 없음
            throw new IllegalArgumentException("작성자는 팟을 나갈 수 없습니다.");
        }
        this.currentHeadcount--;

        //인원 수가 최대치 미만으로 떨어지면 상태를 다시 '모집중'으로 변경합니다.
        if(this.currentHeadcount < this.maximumHeadcount) {
            this.status = PotStatus.RECRUITING;
        }
    }
}
