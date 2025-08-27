package com.subdivision.subdivision_prj.controller;

import com.subdivision.subdivision_prj.dto.ChatMessageDto;
import com.subdivision.subdivision_prj.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

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
        // 메시지 타입이 'TALK'(일반 대화)일 경우에만 데이터베이스에 저장합니다.
        if (ChatMessageDto.MessageType.TALK.equals(message.getType())) {
            chatService.saveMessage(message);
        }

        //사용자가 채팅방에 처음 입장했을 때의 처리
        if(ChatMessageDto.MessageType.ENTER.equals(message.getType())) {
            //입장했다는 알림 메시지를 생성합니다.
            message.setMessage(message.getSender() + "님이 입장하셨습니다.");

            //메시지를 해당 팟(채팅방)을 구독하고 있는 모든 클라이언트에게 전송(브로드캐스팅)합니다.
            //클라이언트는 "/topic/pots/{potId}" 경로를 구독하고 있어야 이 메시지를 받을 수 있습니다.
            messagingTemplate.convertAndSend("/topic/pots/" + message.getPotId(), message);
        }
    }
}
