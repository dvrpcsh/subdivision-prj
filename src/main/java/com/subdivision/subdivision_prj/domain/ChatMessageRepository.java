package com.subdivision.subdivision_prj.domain;

import com.subdivision.subdivision_prj.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * ChatMessage 엔티티에 대한 데이터베이스 연산을 처리하는 레파지토리입니다.
 * 기본적인 CRUD 메서드(save, findById, findAll, delete 등)는 JpaRepository가 자동으로 제공합니다.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 특정 팟 ID에 해당하는 모든 메시지를 보낸 시간(sendAt) 오름차순으로 조회합니다.
     * @param potId 조회할 팟 ID
     * @return ChatMessage 엔티티 리스트
     */
    List<ChatMessage> findAllByPotIdOrderBySentAtAsc(Long potId);

    //최초 참여자인지 확인하는 메서드
    boolean existsByPotAndSender(Pot pot, User sender);
}
