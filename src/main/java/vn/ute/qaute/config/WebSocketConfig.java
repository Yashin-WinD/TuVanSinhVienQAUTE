package vn.ute.qaute.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kênh client subscribe để nhận tin nhắn
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix cho các tin nhắn từ client gửi lên controller (@MessageMapping)
        config.setApplicationDestinationPrefixes("/app");
        // Prefix cho kênh tin nhắn riêng tư gửi đến user cụ thể
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Đăng ký endpoint websocket STOMP (hỗ trợ cả kết nối WebSocket thuần và SockJS dự phòng)
        registry.addEndpoint("/ws-qaute")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-qaute")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
