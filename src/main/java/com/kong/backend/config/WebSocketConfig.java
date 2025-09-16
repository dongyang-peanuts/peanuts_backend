package com.kong.backend.config;

import com.kong.backend.websocket.VideoWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VideoWebSocketHandler handler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 일반 사용자(앱) 영상 미러링
        registry.addHandler(handler, "/ws/video")
                .setAllowedOrigins("*");

        // 관리자 영상 모니터
        registry.addHandler(handler, "/ws/admin/monitor")
                .setAllowedOrigins("*");

        // 관리자 알림 구독
        registry.addHandler(handler, "/ws/alert")
                .setAllowedOrigins("*");

        // 라즈베리파이(낙상 감지) 이벤트 푸시
        registry.addHandler(handler, "/ws/fall")
                .setAllowedOrigins("*");

        // (운영 권장) 정확한 도메인만 화이트리스트하세요.
        // 예) .setAllowedOrigins("https://admin.example.com", "https://app.example.com");
        // 또는 Spring 6+ 에서 패턴 사용할 땐 setAllowedOriginPatterns 사용
    }

    // 메시지 크기 제한 확장 설정
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);   // 1MB (이벤트 JSON은 훨씬 작음)
        container.setMaxBinaryMessageBufferSize(1024 * 1024); // 1MB (영상 바이너리 사용 시 조정)
        // 필요 시 idle timeout, async send timeout 등도 설정 가능:
        // container.setMaxSessionIdleTimeout(60_000L); // 60s
        // container.setAsyncSendTimeout(10_000L);     // 10s
        return container;
    }
}
