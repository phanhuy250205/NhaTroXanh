package nhatroxanh.com.Nhatroxanh.Controller;

import lombok.RequiredArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.entity.ChatMessage;
import nhatroxanh.com.Nhatroxanh.Service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        // Lưu tin nhắn vào database
        ChatMessage savedMessage = chatService.saveMessage(chatMessage);

        // Gửi tin nhắn đến tất cả users trong hostel
        messagingTemplate.convertAndSend(
                "/topic/hostel/" + chatMessage.getHostelId(),
                savedMessage
        );
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        // Thêm username vào websocket session
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderName());
        headerAccessor.getSessionAttributes().put("hostelId", chatMessage.getHostelId());

        // Thông báo user join
        messagingTemplate.convertAndSend(
                "/topic/hostel/" + chatMessage.getHostelId(),
                chatMessage
        );
    }
}
