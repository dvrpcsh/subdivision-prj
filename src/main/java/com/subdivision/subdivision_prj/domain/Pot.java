package com.subdivision.subdivision_prj.domain;

import com.subdivision.subdivision_prj.dto.PotUpdateRequestDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

/**
 * '팟(공동구매 모임)' 정보를 나타내는 JPA 엔티티 클래스입니다.
 * 이 클래스는 데이터베이스의 'pots' 테이블과 매핑됩니다.
 * @author subdivision
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA는 기본 생성자를 필요로 합니다. protected로 설정하여 무분별한 객체 생성을 방지합니다.
@Table(name = "pots")
public class Pot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 생성을 데이터베이스에 위임합니다 (AUTO_INCREMENT).
    @Column(name = "pot_id")
    private Long id;

    // '작성자' 정보를 담기 위해 User 엔티티와 N:1 관계를 맺습니다.
    @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩을 통해 성능을 최적화합니다.
    @JoinColumn(name = "user_id", nullable = false) // 'user_id'라는 외래 키를 생성하며, null을 허용하지 않습니다.
    private User user;

    @Column(nullable = false)
    private String title; // 제목

    @Column(nullable = false)
    private String content; // 내용

    @Column(nullable = false)
    private String productName; // 소분할 상품명

    @Column(nullable = false)
    private int maximumHeadcount; // 최대 참여 인원

    @Column(nullable = false)
    private int currentHeadcount; // 현재 참여 인원

    @Column(nullable = false)
    private Double latitude; // 게시물의 위치 정보 (위도)

    @Column(nullable = false)
    private Double longitude; // 게시물의 위치 정보 (경도)

    private String address; // 주소
    private String detailAddress; // 상세주소

    // S3에 업로드 된 이미지의 '파일 경로(Key)'를 저장하는 필드입니다. (예: images/uuid-asdf.png)
    // Presigned URL은 길이가 매우 길 수 있으므로, VARCHAR 대신 길이 제한이 거의 없는 TEXT 타입을 사용합니다.
    @Column(columnDefinition = "TEXT", nullable = true) // 이미지는 선택 사항이므로 nullable = true로 설정합니다.
    private String imageUrl;

    // 이 팟에 참여한 사용자 목록을 나타냅니다. Pot이 삭제되면 관련된 참여 정보도 함께 삭제됩니다(cascade = CascadeType.ALL).
    @OneToMany(mappedBy = "pot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PotMember> members = new ArrayList<>();

    // 이 팟에 달린 채팅 메시지 목록과의 관계입니다. Pot(부모)이 삭제될 때, 관련된 ChatMessage(자식)들도 함께 삭제됩니다.
    @OneToMany(mappedBy = "pot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> chatMessages = new ArrayList<>();

    // Enum 타입을 문자열로 저장합니다. (예: "CHICKEN", "PIZZA")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'ETC'")
    private PotCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(255) DEFAULT 'RECRUITING'")
    private PotStatus status;

    @Column(nullable = false)
    private Integer price; // 가격 정보

    /**
     * 빌더 패턴을 사용하여 Pot 객체를 생성하는 생성자입니다.
     * 객체의 일관성을 유지하며 가독성을 높여줍니다.
     */
    @Builder
    public Pot(User user, String title, String content, Integer price, String productName, int maximumHeadcount, Double latitude, Double longitude, String imageUrl, PotCategory category, String address, String detailAddress) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.productName = productName;
        this.price = price;
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

    /**
     * Pot 엔티티의 정보를 업데이트하는 메서드입니다.
     * @param requestDto 수정할 정보를 담은 DTO
     * @param isNewImageUploaded 이미지가 새로 업로드되었는지 여부
     */
    public void update(PotUpdateRequestDto requestDto, boolean isNewImageUploaded) {
        this.title = requestDto.getTitle();
        this.content = requestDto.getContent();
        this.productName = requestDto.getProductName();
        this.price = requestDto.getPrice();
        this.maximumHeadcount = requestDto.getMaximumHeadcount();
        this.latitude = requestDto.getLatitude();
        this.longitude = requestDto.getLongitude();
        this.category = requestDto.getCategory();
        this.address = requestDto.getAddress();
        this.detailAddress = requestDto.getDetailAddress();

        // 💡 [핵심] isNewImageUploaded가 true일 때만 imageUrl 필드를 업데이트합니다.
        // 이 로직 덕분에, 이미지를 수정하지 않은 경우(isNewImageUploaded == false)
        // 프론트엔드에서 보낸 기존의 긴 Presigned URL이 DB에 덮어써지는 것을 방지하고,
        // DB에 저장된 원본 파일 경로(Key)가 안전하게 유지됩니다.
        if (isNewImageUploaded) {
            this.imageUrl = requestDto.getImageUrl();
        }
    }

    /**
     * 팟에 새로운 참여자가 들어왔을 때, 현재 인원을 1 증가시키고 상태를 업데이트합니다.
     */
    public void addParticipant() {
        if(this.currentHeadcount >= this.maximumHeadcount) {
            throw new IllegalArgumentException("모집 인원이 마감되었습니다.");
        }
        this.currentHeadcount++;
        // 인원 수가 최대치에 도달하면 상태를 '모집완료'로 변경합니다.
        if(this.currentHeadcount == this.maximumHeadcount) {
            this.status = PotStatus.COMPLETED;
        }
    }

    /**
     * 팟에서 기존 참여자가 나갔을 때, 현재 인원을 1 감소시키고 상태를 업데이트합니다.
     */
    public void removeParticipant() {
        if(this.currentHeadcount <= 1) {
            // 최소 1명(작성자)은 있어야 하므로 1 이하로 내려갈 수 없습니다.
            throw new IllegalArgumentException("작성자는 팟을 나갈 수 없습니다.");
        }
        this.currentHeadcount--;
        // 인원 수가 최대치 미만으로 떨어지면 상태를 다시 '모집중'으로 변경합니다.
        if(this.currentHeadcount < this.maximumHeadcount) {
            this.status = PotStatus.RECRUITING;
        }
    }
}

