package com.kong.backend.config;

import com.kong.backend.websocket.VideoWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VideoWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/video").setAllowedOrigins("*");
        registry.addHandler(handler, "/ws/admin/monitor").setAllowedOrigins("*");
    }

    // 여기에 메시지 크기 제한 확장 설정 추가
    @Bean
    public WebSocketHandlerDecoratorFactory webSocketHandlerDecoratorFactory() {
        return handler -> new WebSocketHandlerDecorator(handler) {
            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                session.setTextMessageSizeLimit(5 * 1024 * 1024); // 5MB
                session.setBinaryMessageSizeLimit(5 * 1024 * 1024); // 5MB
                super.afterConnectionEstablished(session);
            }
        };
    }
}
