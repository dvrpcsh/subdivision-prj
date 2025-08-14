package com.subdivision.subdivision_prj.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    //빌더 패턴을 사용하여 객체를 생성합니다.
    @Builder
    public Pot(User user, String title, String content, String productName, int maximumHeadcount, int currentHeadcount) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.productName = productName;
        this.maximumHeadcount = maximumHeadcount;
        this.currentHeadcount = 1; // 팟 생성 시, 작성자를 포함하여 현재 인원을 1로 초기화합니다.
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
