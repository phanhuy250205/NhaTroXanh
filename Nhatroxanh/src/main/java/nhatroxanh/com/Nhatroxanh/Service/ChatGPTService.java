package nhatroxanh.com.Nhatroxanh.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Service
public class ChatGPTService {

    private static final String API_KEY = ""; // 🔑 Thay bằng API key thật
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public String sendMessage(String userMessage) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Format chuẩn ChatGPT API
            Map<String, Object> jsonBody = new HashMap<>();
            jsonBody.put("model", "gpt-3.5-turbo"); // hoặc gpt-4
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", userMessage));
            jsonBody.put("messages", messages);

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(jsonBody);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Nhận response
            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNext()) {
                response.append(scanner.nextLine());
            }

            scanner.close();

            // Parse JSON kết quả
            Map<String, Object> responseMap = mapper.readValue(response.toString(), Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi khi gọi ChatGPT: " + e.getMessage();
        }
    }
}