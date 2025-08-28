package com.subdivision.subdivision_prj.service;

import com.subdivision.subdivision_prj.domain.*;
import com.subdivision.subdivision_prj.domain.ChatMessageRepository;
import com.subdivision.subdivision_prj.dto.ChatMessageDto;
import com.subdivision.subdivision_prj.dto.ChatHistoryResponseDto;
import com.subdivision.subdivision_prj.domain.PotRepository;
import com.subdivision.subdivision_prj.domain.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 채팅 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final PotRepository potRepository;
    private final UserRepository userRepository;
    private final PotMemberRepository potMemberRepository;

    /**
     * 메시지를 저장하고, 최초 참여자인지 여부를 반환하는 메서드
     * @param messageDto 클라이언트로부터 받은 채팅 메시지 DTO
     * @return 최초 참여자일 경우 true, 아닐 경우 false
     */
    @Transactional
    public boolean saveMessageAndCheckFirstJoin(ChatMessageDto messageDto) {
        Pot pot = potRepository.findById(messageDto.getPotId())
                .orElseThrow(() -> new IllegalArgumentException("햇당 팟을 찾을 수 없습니다. id=" + messageDto.getPotId()));
        User sender = userRepository.findByNickname(messageDto.getSender())
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. nickname=" + messageDto.getSender()));

        // ENTER 메시지인 경우 최초 참여 여부를 확인
        if (ChatMessageDto.MessageType.ENTER.equals(messageDto.getType())) {
            // 이 사용자가 이 팟에서 보낸 채팅 메시지가 있는지 확인
            boolean hasChattedBefore = chatMessageRepository.existsByPotAndSender(pot, sender);

            // 최초 참여자인 경우에만 환영 메시지를 저장하고 true 반환
            if (!hasChattedBefore) {
                String welcomeMessage = messageDto.getSender() + "님이 팟에 처음으로 참여했습니다!";
                ChatMessage welcomeChatMessage = ChatMessage.builder()
                        .pot(pot)
                        .sender(sender)
                        .message(welcomeMessage)
                        .build();
                chatMessageRepository.save(welcomeChatMessage);
                return true; // 최초 참여자임을 반환
            }
            return false; // 재참여자임을 반환
        }

        // TALK 메시지인 경우 일반 채팅 메시지로 저장
        if (ChatMessageDto.MessageType.TALK.equals(messageDto.getType())) {
            ChatMessage chatMessage = ChatMessage.builder()
                    .pot(pot)
                    .sender(sender)
                    .message(messageDto.getMessage())
                    .build();
            chatMessageRepository.save(chatMessage);
        }

        return false;
    }

    /**
     * 특정 팟의 이전 대화 기록을 조회하는 메서드입니다.
     * @param potId 조회할 팟의 ID
     * @return 대화 기록 DTO 리스트
     */
    @Transactional(readOnly = true)
    public List<ChatHistoryResponseDto> getChatHistory(Long potId) {
        //1.레파지토리를 사용하여 특정 팟의 모든 메시지를 시간 순으로 가져옵니다.
        List<ChatMessage> messages = chatMessageRepository.findAllByPotIdOrderBySentAtAsc(potId);

        //2.가져온 ChatMessage 엔티티 리스트를 ChatHistoryResponseDto 리스트로 변환하여 반환합니다.
        return messages.stream()
                .map(ChatHistoryResponseDto::new)
                .collect(Collectors.toList());
    }
}
