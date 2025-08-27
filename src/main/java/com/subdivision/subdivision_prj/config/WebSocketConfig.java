package com.subdivision.subdivision_prj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 웹소켓 통신을 위한 설정 클래스입니다.
 * STOMP 프로토콜을 사용하여 메시지를 처리합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    /**
     * 클라이언트가 웹소켓 서버에 연결할 때 사용할 엔드포인트를 등록합니다.
     * @param registry STOMP 엔드포인트를 등록하기 위한 레지스트리
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // "/ws-chat"경로로 웹소켓 연결을 설정합니다.
        // 프론트엔드(React)에서는 이 주소로 서버에 접속하게 됩니다.
        // .withSockJS()는 웹소켓을 지원하지 않는 브라우저에서도 통신이 가능하도록 도와줍니다.
        registry.addEndpoint("/ws-chat").setAllowedOriginPatterns("*").withSockJS();
    }

    /**
     * 메시지 브로커를 설정하여, 메시지 라우팅 규칙을 정의합니다.
     * @param registry 메시지 브로커를 설정하기 위한 레지스트리
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // "/topic"으로 시작하는 경로를 구독하는 클라이언트에게 메시지를 전달합니다.
        // 예를 들어, 클라이언트는 "/topic/pots/123" 주소를 구독하여 123번 팟의 채팅 메시지를 받게됩니다.
        registry.enableSimpleBroker("/topic");

        // "/app"으로 시작하는 경로로 들어온 메시지는 @MessageMapping이 붙은 메서드와 라우팅됩니다.
        // 예를 들어, 클라이언트가 메시지를 보낼 때는 "/app/chat.sendMessage"와 같은 주소로 보내게 됩니다.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
