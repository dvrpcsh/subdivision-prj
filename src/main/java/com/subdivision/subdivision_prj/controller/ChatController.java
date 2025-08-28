package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.dto.ChatMessageDto;
import com.subdivision.subdivision_prj.dto.ChatHistoryResponseDto;
import com.subdivision.subdivision_prj.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Controller
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    /**
     * 클라이언트로부터 메시지를 받아 처리하는 메서드입니다.
     * @MessageMapping("/chat/message")은 "/app/chat/message" 경로로 들어온 메시지를 이 메서드가 처리하도록 매핑합니다.
     * (WebSockerConfig에서 setApplicationDestinationPrefixes("/app")으로 설정했기 때문)
     * @param message 클라이언트가 보낸 채팅 메시지 정보(ChatMessageDto)
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        // 메시지 타입이 'ENTER'(입장)일 경우
        if (ChatMessageDto.MessageType.ENTER.equals(message.getType())) {
            // 서비스를 호출하여 메시지를 저장하고, 최초 참여자인지 확인합니다.
            boolean isFirstJoin = chatService.saveMessageAndCheckFirstJoin(message);

            // 최초 참여자일 경우에만 입장 알림 메시지를 전송합니다.
            if(isFirstJoin) {
                // 새로운 메시지 객체를 생성하여 환영 메시지를 담습니다.
                ChatMessageDto welcomeMessage = new ChatMessageDto();
                welcomeMessage.setType(ChatMessageDto.MessageType.ENTER);
                welcomeMessage.setPotId(message.getPotId());
                welcomeMessage.setSender(message.getSender());
                welcomeMessage.setMessage(message.getSender() + "님이 팟에 처음으로 참여했습니다!");

                // 환영 메시지를 모든 구독자에게 브로드캐스트
                messagingTemplate.convertAndSend("/topic/pots/" + message.getPotId(), welcomeMessage);
            }
        }
        // 'TALK' 타입의 메시지 처리
        else if (ChatMessageDto.MessageType.TALK.equals(message.getType())) {
            // 채팅 메시지를 저장하고 모든 구독자에게 브로드캐스트
            chatService.saveMessageAndCheckFirstJoin(message);
            messagingTemplate.convertAndSend("/topic/pots/" + message.getPotId(), message);
        }
    }

    /**
     * 특정 팟의 이전 대화 기록을 조회하는 HTTP GET API 엔드포인트입니다.
     * @param potId 조회할 팟의 ID
     * @return 대화 기록 DTO 리스트
     */
    @GetMapping("/api/pots/{potId}/chat/history")
    @ResponseBody //이 메서드의 반환값이 HTTP 응답 본문(body)에 직접 쓰여지도록 합니다.
    public ResponseEntity<List<ChatHistoryResponseDto>> getChatHistory(@PathVariable Long potId) {
        List<ChatHistoryResponseDto> chatHistory = chatService.getChatHistory(potId);

        return ResponseEntity.ok(chatHistory);
    }
}
