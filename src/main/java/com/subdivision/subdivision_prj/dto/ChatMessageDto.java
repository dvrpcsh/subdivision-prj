package com.subdivision.subdivision_prj.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageDto {
    
    public enum MessageType {
        ENTER, TALK
    }
    
    private MessageType type; //메시지 타입
    private Long potId;       //메시지를 보낼 팟(채팅방) ID
    private String sender;    //메시지를 보낸 사람의 닉네임
    private String message;   //메시지 내용
}
