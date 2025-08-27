package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.*;
import com.subdivision.subdivision_prj.domain.ChatMessageRepository;
import com.subdivision.subdivision_prj.dto.ChatMessageDto;
import com.subdivision.subdivision_prj.domain.ChatMessageRepository;
import com.subdivision.subdivision_prj.domain.PotRepository;
import com.subdivision.subdivision_prj.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 채팅 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final PotRepository potRepository;
    private final UserRepository userRepository;

    /**
     * 수신된 채팅 메시지를 데이터베이스에 저장합니다.
     * @param messageDto 클라이언트로부터 받은 채팅 메시지 DTO
     */
    @Transactional
    public void saveMessage(ChatMessageDto messageDto) {
        //1.DTO에서 팟 ID를 사용하여 Pot 엔티티를 조회합니다.
        Pot pot = potRepository.findById(messageDto.getPotId())
                .orElseThrow(() -> new IllegalArgumentException("해당 팟을 찾을 수 없습니다. id=" + messageDto.getPotId()));
        //2.DTO에서 보낸 사람의 닉네임을 사용하여 User 엔티티를 조회합니다.
        User sender = userRepository.findByNickname(messageDto.getSender())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. nickname=" + messageDto.getSender()));

        //3.조회한 엔티티와 메시지 내용을 바탕으로 ChatMessage 엔티티를 생성합니다.
        ChatMessage chatMessage = ChatMessage.builder()
                .pot(pot)
                .sender(sender)
                .message(messageDto.getMessage())
                .build();

        //4.생성된 ChatMessage 엔티티를 데이터베이스에 저장합니다.
        chatMessageRepository.save(chatMessage);
    }
}
