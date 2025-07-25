package nhatroxanh.com.Nhatroxanh.Service;


import org.springframework.web.multipart.MultipartFile;

import nhatroxanh.com.Nhatroxanh.Model.entity.Image;
import nhatroxanh.com.Nhatroxanh.Model.entity.UserCccd;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface ImageService {

    Image saveImage(MultipartFile file, String subFolder, UserCccd userCccd, Image.ImageType type) throws IOException;
    List<Image> findByUserCccdId(Long userCccdId);
    Optional<Image> findById(Integer id);
    void deleteImage(Integer id);
    Image saveImage(Image image); // Thêm phương thức để lưu đối tượng Image trực tiếp

    // Thêm phương thức mới để xóa ảnh theo userCccdId và ImageType
    void deleteImagesByUserCccdAndType(Long userCccdId, Image.ImageType type);
}
