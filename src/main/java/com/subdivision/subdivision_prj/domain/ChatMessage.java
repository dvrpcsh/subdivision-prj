package com.subdivision.subdivision_prj.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.operation.linemerge.LineMergeGraph;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

/**
 * 채팅 메시지를 데이터베이스에 저장하기 위한 엔티티 클래스입니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long id;

    //메시지가 속한 팟(채팅창) 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pot_id", nullable = false)
    private Pot pot;

    //메시지를 보낸 사용자 정보
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String message;

    @CreatedDate //엔티티가 생성될 때의 시간을 자동으로 저장
    @Column(updatable = false, nullable = false)
    private LocalDateTime sendAt;

    @Builder
    public ChatMessage(Pot pot, User sender, String message) {
        this.pot = pot;
        this.sender = sender;
        this.message = message;
    }

}
