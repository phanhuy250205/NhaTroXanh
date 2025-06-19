package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Service
public class FileUploadService {
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/";

    /**
     * Kiểm tra file có phải là ảnh hợp lệ không
     *
     * @param fileName Tên file cần kiểm tra
     * @return true nếu là ảnh, false nếu không
     */
    private boolean isValidImageFile(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") ||
                lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".gif") || lowerCaseName.endsWith(".webp");
    }

    /**
     * Upload file ảnh vào thư mục `static/uploads/`
     *
     * @param file      Ảnh tải lên từ người dùng
     * @param subFolder Tên thư mục con (VD: "avatars", "products")
     * @return Đường dẫn tương đối để hiển thị trên web (VD:
     *         "/uploads/avatars/123456_avatar.jpg")
     * @throws IOException Nếu xảy ra lỗi khi lưu file
     */
    public String uploadFile(MultipartFile file, String subFolder) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("❌ File tải lên bị rỗng!");
        }

        // 🔹 Kiểm tra phần mở rộng file (Chỉ chấp nhận ảnh)
        String originalFileName = Objects.requireNonNull(file.getOriginalFilename()).toLowerCase();
        if (!isValidImageFile(originalFileName)) {
            throw new IllegalArgumentException("❌ Chỉ chấp nhận các định dạng ảnh JPG, JPEG, PNG, GIF!");
        }

        // 🔹 Tạo đường dẫn thư mục lưu ảnh
        String uploadPath = UPLOAD_DIR + subFolder + "/";
        File directory = new File(uploadPath);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("❌ Không thể tạo thư mục lưu ảnh!");
        }

        // 🔹 Tạo tên file duy nhất
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = Paths.get(uploadPath, fileName);

        // 🔹 Kiểm tra xem file có bị trùng tên không, nếu có thì tạo tên mới
        while (Files.exists(filePath)) {
            fileName = System.currentTimeMillis() + "_" + originalFileName;
            filePath = Paths.get(uploadPath, fileName);
        }

        // 🔹 Lưu file vào thư mục đã chỉ định
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("🟢 Ảnh đã lưu vào: " + filePath.toString());

        // 🔹 **CHỈ TRẢ VỀ `/uploads/avatars/[file_name]`**
        return "/uploads/" + subFolder + "/" + fileName;
    }

    /**
     * Xóa file ảnh từ thư mục
     *
     * @param filePath Đường dẫn của file cần xóa (VD:
     *                 "/uploads/avatars/123456_avatar.jpg")
     * @return true nếu xóa thành công, false nếu thất bại
     */
    public boolean deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            System.out.println("❌ Đường dẫn file không hợp lệ.");
            return false;
        }

        // 🔹 Chuyển đường dẫn từ `/uploads/avatars/xxx.jpg` thành
        // `src/main/resources/static/uploads/avatars/xxx.jpg`
        String absolutePath = "src/main/resources/static" + filePath;

        File file = new File(absolutePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                System.out.println("🟢 Đã xóa file: " + absolutePath);
            } else {
                System.out.println("❌ Không thể xóa file: " + absolutePath);
            }
            return deleted;
        } else {
            System.out.println("⚠ File không tồn tại: " + absolutePath);
        }

        return false;
    }
}
