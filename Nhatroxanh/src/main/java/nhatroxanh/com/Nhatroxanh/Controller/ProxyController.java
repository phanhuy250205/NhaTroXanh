package nhatroxanh.com.Nhatroxanh.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    @GetMapping("/provinces")
    public ResponseEntity<String> getAllProvinces() throws IOException {
        URL url = new URL("https://provinces.open-api.vn/api/p/");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return ResponseEntity.ok(response.toString());
    }

    @GetMapping("/provinces/{code}")
    public ResponseEntity<String> getProvincesByCode(@PathVariable String code) throws IOException {
        URL url = new URL("https://provinces.open-api.vn/api/p/" + code + "?depth=2");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return ResponseEntity.ok(response.toString());
    }
}