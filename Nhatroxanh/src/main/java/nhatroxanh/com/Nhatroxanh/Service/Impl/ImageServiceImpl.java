package nhatroxanh.com.Nhatroxanh.Service.Impl;




import nhatroxanh.com.Nhatroxanh.Model.enity.Image;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import nhatroxanh.com.Nhatroxanh.Service.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Override
    @Transactional
    public Image saveImage(MultipartFile file, String subFolder, UserCccd userCccd, Image.ImageType type) throws IOException {
        logger.info("Bắt đầu lưu ảnh, subFolder: {}, userCccdId: {}, type: {}",
                subFolder, userCccd != null ? userCccd.getId() : "null", type);

        if (file == null || file.isEmpty()) {
            logger.error("Tệp ảnh rỗng hoặc null");
            throw new IllegalArgumentException("Tệp ảnh không được để trống!");
        }

        if (userCccd == null || userCccd.getId() == null) {
            logger.error("UserCccd null hoặc không có ID");
            throw new IllegalArgumentException("Thông tin UserCccd không hợp lệ!");
        }

        // Đảm bảo subFolder là 'cccd' cho ảnh CCCD
        String cccdSubFolder = "cccd";
        String imageUrl;
        try {
            imageUrl = fileUploadService.uploadFile(file, cccdSubFolder);
            logger.info("Tải lên tệp thành công, URL: {}", imageUrl);
        } catch (IOException e) {
            logger.error("Lỗi khi tải lên tệp: {}", e.getMessage(), e);
            throw new IOException("Không thể tải lên tệp ảnh: " + e.getMessage());
        }

        // Tạo thực thể Image
        Image image = Image.builder()
                .url(imageUrl)
                .userCccd(userCccd)
                .type(type)
                .build();

        // Lưu Image vào cơ sở dữ liệu
        Image savedImage = imageRepository.saveAndFlush(image);
        logger.info("Lưu ảnh thành công, ID: {}, user_cccd_id: {}", savedImage.getId(), userCccd.getId());

        return savedImage;
    }

    @Override
    public List<Image> findByUserCccdId(Long userCccdId) {
        logger.info("Tìm ảnh theo user_cccd_id: {}", userCccdId);
        if (userCccdId == null) {
            logger.error("user_cccd_id là null");
            throw new IllegalArgumentException("ID UserCccd không được để trống!");
        }

        List<Image> images = imageRepository.findByUserCccdId(userCccdId);
        logger.info("Tìm thấy {} ảnh cho user_cccd_id: {}", images.size(), userCccdId);
        return images;
    }

    @Override
    public Optional<Image> findById(Integer id) {
        logger.info("Tìm ảnh theo ID: {}", id);
        if (id == null) {
            logger.error("ID ảnh là null");
            throw new IllegalArgumentException("ID ảnh không được để trống!");
        }

        Optional<Image> image = imageRepository.findById(id);
        if (image.isPresent()) {
            logger.info("Tìm thấy ảnh, ID: {}, URL: {}", id, image.get().getUrl());
        } else {
            logger.warn("Không tìm thấy ảnh với ID: {}", id);
        }
        return image;
    }

    @Override
    @Transactional
    public void deleteImage(Integer id) {
        logger.info("Xóa ảnh với ID: {}", id);
        if (id == null) {
            logger.error("ID ảnh là null");
            throw new IllegalArgumentException("ID ảnh không được để trống!");
        }

        Optional<Image> image = imageRepository.findById(id);
        if (!image.isPresent()) {
            logger.warn("Không tìm thấy ảnh với ID: {}", id);
            throw new IllegalArgumentException("Ảnh không tồn tại!");
        }

        // Xóa tệp vật lý
        try {
            fileUploadService.deleteFile(image.get().getUrl());
            logger.info("Xóa tệp vật lý thành công: {}", image.get().getUrl());
        } catch (Exception e) {
            logger.error("Lỗi khi xóa tệp vật lý: {}", e.getMessage(), e);
            // Tiếp tục xóa bản ghi trong cơ sở dữ liệu ngay cả khi xóa tệp thất bại
        }

        imageRepository.deleteById(id);
        logger.info("Xóa bản ghi ảnh thành công, ID: {}", id);
    }



    @Override
    @Transactional
    public Image saveImage(Image image) {
        logger.info("Lưu ảnh với user_cccd_id: {}, type: {}",
                image.getUserCccd() != null ? image.getUserCccd().getId() : "null", image.getType());

        if (image.getUserCccd() == null || image.getUserCccd().getId() == null) {
            logger.error("UserCccd null hoặc không có ID");
            throw new IllegalArgumentException("Thông tin UserCccd không hợp lệ!");
        }
        if (!StringUtils.hasText(image.getUrl())) {
            logger.error("URL ảnh không hợp lệ");
            throw new IllegalArgumentException("URL ảnh không được để trống!");
        }

        Image savedImage = imageRepository.saveAndFlush(image);
        logger.info("Lưu ảnh thành công, ID: {}, URL: {}", savedImage.getId(), savedImage.getUrl());
        return savedImage;
    }

    @Override
    @Transactional
    public void deleteImagesByUserCccdAndType(Long userCccdId, Image.ImageType type) {
        logger.info("Deleting images for userCccdId: {} with type: {}", userCccdId, type);
        List<Image> images = imageRepository.findByUserCccdIdAndType(userCccdId, type);
        for (Image image : images) {
            try {
                // Xóa file vật lý
                fileUploadService.deleteFile(image.getUrl());
                // Xóa bản ghi trong database
                imageRepository.delete(image);
                logger.info("Deleted image ID: {} with URL: {}", image.getId(), image.getUrl());
            } catch (Exception e) {
                logger.error("Error deleting image ID: {} with URL: {}. Error: {}",
                        image.getId(), image.getUrl(), e.getMessage());
            }
        }
        logger.info("Completed deleting images for userCccdId: {} with type: {}", userCccdId, type);
    }

}