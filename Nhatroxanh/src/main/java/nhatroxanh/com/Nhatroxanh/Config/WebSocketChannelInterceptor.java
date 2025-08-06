package nhatroxanh.com.Nhatroxanh.Config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            switch (command) {
                case CONNECT:
                    // 🔥 Handle connection - Authentication logic here
                    System.out.println("WebSocket CONNECT: " + accessor.getSessionId());
                    break;

                case DISCONNECT:
                    // 🔥 Handle disconnection - Cleanup logic here
                    System.out.println("WebSocket DISCONNECT: " + accessor.getSessionId());
                    break;

                case SUBSCRIBE:
                    // 🔥 Handle subscription - Authorization logic here
                    System.out.println("WebSocket SUBSCRIBE: " + accessor.getDestination());
                    break;

                default:
                    break;
            }
        }

        return message;
    }
}
