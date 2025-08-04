package nhatroxanh.com.Nhatroxanh.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.ChatMessage;
import nhatroxanh.com.Nhatroxanh.Service.ChatService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String hostelId = (String) headerAccessor.getSessionAttributes().get("hostelId");
        String userType = (String) headerAccessor.getSessionAttributes().get("userType");

        log.info("New WebSocket connection - SessionId: {}, UserId: {}, HostelId: {}, UserType: {}",
                sessionId, userId, hostelId, userType);

        // 🔥 Gửi welcome message cho tenant mới
        if ("tenant".equals(userType) && userId != null && hostelId != null) {
            try {
                // Có thể gửi welcome message hoặc thông báo online status
                String destination = "/topic/hostel/" + hostelId;
                ChatMessage onlineNotification = new ChatMessage();
                onlineNotification.setType(ChatMessage.MessageType.SYSTEM);
                onlineNotification.setContent("Người dùng đã kết nối");
                onlineNotification.setSenderId(Long.parseLong(userId));
                onlineNotification.setHostelId(Long.parseLong(hostelId));
                onlineNotification.setIsSupportMessage(true);

                messagingTemplate.convertAndSend(destination, onlineNotification);
            } catch (Exception e) {
                log.error("Error sending online notification: ", e);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String userId = (String) headerAccessor.getSessionAttributes().get("userId");
        String hostelId = (String) headerAccessor.getSessionAttributes().get("hostelId");
        String userType = (String) headerAccessor.getSessionAttributes().get("userType");

        log.info("WebSocket disconnection - SessionId: {}, UserId: {}, HostelId: {}, UserType: {}",
                sessionId, userId, hostelId, userType);

        // 🔥 Thông báo offline status
        if (userId != null && hostelId != null) {
            try {
                String destination = "/topic/hostel/" + hostelId;
                ChatMessage offlineNotification = new ChatMessage();
                offlineNotification.setType(ChatMessage.MessageType.SYSTEM);
                offlineNotification.setContent("Người dùng đã ngắt kết nối");
                offlineNotification.setSenderId(Long.parseLong(userId));
                offlineNotification.setHostelId(Long.parseLong(hostelId));
                offlineNotification.setIsSupportMessage(true);

                messagingTemplate.convertAndSend(destination, offlineNotification);
            } catch (Exception e) {
                log.error("Error sending offline notification: ", e);
            }
        }
    }
}
