package com.subdivision.subdivision_prj.domain;

import com.subdivision.subdivision_prj.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ChatMessage 엔티티에 대한 데이터베이스 연산을 처리하는 레파지토리입니다.
 * 기본적인 CRUD 메서드(save, findById, findAll, delete 등)는 JpaRepository가 자동으로 제공합니다.
 */
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
}
