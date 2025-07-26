package nhatroxanh.com.Nhatroxanh.Controller.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import nhatroxanh.com.Nhatroxanh.Service.ChatGPTService;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatGPTService chatGPTService;

    @PostMapping
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String userMessage = request.get("message");
        String aiReply = chatGPTService.sendMessage(userMessage);

        return Map.of("reply", aiReply);
    }
}