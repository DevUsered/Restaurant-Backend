package AttizosBackend.config;

import AttizosBackend.websocket.SyncSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final SyncSocketHandler syncSocketHandler;

    public WebSocketConfig(SyncSocketHandler syncSocketHandler) {
        this.syncSocketHandler = syncSocketHandler;
    }
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry){
        registry.addHandler(syncSocketHandler, "/ws-sync").setAllowedOrigins("*");
    }
}
