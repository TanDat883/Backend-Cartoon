package flim.backendcartoon.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Cấu hình WebSocket cho tính năng xem phim chung
 *
 * @author Tran Tan Dat
 * @version 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Cấu hình simple broker cho topic (broadcast) và queue (unicast)
        config.enableSimpleBroker("/topic", "/queue");

        // Prefix cho các message từ client gửi lên
        config.setApplicationDestinationPrefixes("/app");

        // Prefix cho user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket cho watch together
        registry.addEndpoint("/ws/watch")
                .setAllowedOriginPatterns("http://localhost:3000", "http://localhost:*")
                .withSockJS();
    }
}

