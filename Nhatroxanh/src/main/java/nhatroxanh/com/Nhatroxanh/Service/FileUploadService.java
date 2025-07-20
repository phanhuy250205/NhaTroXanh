package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Objects;

@Service
public class FileUploadService {
    // 🔧 Lấy thư mục gốc của project (tuyệt đối)
    private static final String ROOT_DIR = Paths.get("").toAbsolutePath().toString();

    // 🔧 Đường dẫn upload đầy đủ: [project]/src/main/resources/static/uploads/
    private static final String UPLOAD_ROOT_DIR = ROOT_DIR + "/src/main/resources/static/uploads/";

    /**
     * Kiểm tra file có phải là ảnh hợp lệ không
     */
    private boolean isValidImageFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg")
                || lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".gif")
                || lowerCaseName.endsWith(".webp");
    }

    /**
     * Upload file ảnh vào thư mục `static/uploads/[subFolder]/`
     *
     * @param file      ảnh người dùng upload
     * @param subFolder thư mục con (vd: avatars, products)
     * @return đường dẫn tương đối để hiển thị trên web (vd:
     *         /uploads/avatars/123.png)
     */
    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("❌ File tải lên bị rỗng!");
        }

        String originalFileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        if (!isValidImageFile(originalFileName)) {
            throw new IllegalArgumentException("❌ Chỉ chấp nhận ảnh JPG, JPEG, PNG, GIF, WEBP!");
        }

        // 🔧 Đường dẫn tuyệt đối tới thư mục con
        String uploadPath = UPLOAD_ROOT_DIR + subFolder ;
        File directory = new File(uploadPath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("❌ Không thể tạo thư mục lưu ảnh: " + uploadPath);
        }

        // 🔧 Tạo tên file duy nhất
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = Paths.get(uploadPath, fileName);

        while (Files.exists(filePath)) {
            fileName = System.currentTimeMillis() + "_" + originalFileName;
            filePath = Paths.get(uploadPath, fileName);
        }

        // 📁 Ghi file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("🟢 Ảnh đã lưu vào: " + filePath.toString());

        // 📤 Trả về đường dẫn web
        return "/uploads/"  + fileName;
    }

    /**
     * Xóa file từ thư mục uploads
     *
     * @param filePath đường dẫn từ `/uploads/...`
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            System.out.println("❌ Đường dẫn file không hợp lệ.");
            return false;
        }
        String absolutePath = ROOT_DIR + "/src/main/resources/static" + filePath;
        String backupPath = ROOT_DIR + "/src/main/resources/static/uploads/backup/" + filePath.substring(filePath.lastIndexOf("/") + 1);

        File file = new File(absolutePath);
        if (file.exists()) {
            // Di chuyển file vào thư mục backup
            File backupDir = new File(backupPath).getParentFile();
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            boolean moved = file.renameTo(new File(backupPath));
            if (moved) {
                System.out.println("🟢 Đã di chuyển file vào backup: " + backupPath);
                return true;
            } else {
                System.out.println("❌ Không thể di chuyển file: " + absolutePath);
                return false;
            }
        } else {
            System.out.println("⚠ File không tồn tại: " + absolutePath);
            return false;
        }
    }


}
