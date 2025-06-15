package nhatroxanh.com.Nhatroxanh.Config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {
    @PostConstruct
    public  void configureEnvironment(){
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")           // Tìm file .env ở thư mục gốc
                    .ignoreIfMissing()         // Không lỗi nếu không tìm thấy file
                    .load();
            //Đặt tất cả biến từ .env vào system properties
            dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
            );
            System.out.println("Đã load thành công file .env");
        } catch (Exception e) {
            System.err.println("❌ Không thể load file .env: " + e.getMessage());
            System.err.println("⚠️ Ứng dụng sẽ dùng giá trị mặc định");
        }
    }
}
