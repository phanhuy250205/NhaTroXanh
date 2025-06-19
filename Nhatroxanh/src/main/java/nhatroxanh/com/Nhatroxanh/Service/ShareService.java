package nhatroxanh.com.Nhatroxanh.Service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ShareDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;

@Service
@Transactional(readOnly = true)
public class ShareService {
    private static final Logger log = LoggerFactory.getLogger(ShareService.class);

    @Autowired
    private PostRepository postRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public ShareDTO generateShareUrl(Integer postId) {
        log.info("=== SHARE SERVICE ===");
        log.info("Generating share URL for postId: {}", postId);
        log.info("Base URL configured: {}", baseUrl);

        try {
            // First try simple findById to check if post exists
            log.info("Checking if post exists with simple findById...");
            Optional<Post> simpleCheck = postRepository.findById(postId);
            
            if (simpleCheck.isEmpty()) {
                log.warn("Post with id {} not found in simple check", postId);
                return new ShareDTO(postId, "Bài đăng không tồn tại");
            }
            
            log.info("Post exists, now fetching with details...");
            
            // Now try with details
            Optional<Post> postOptional = postRepository.findByIdWithShareDetails(postId);
            
            if (postOptional.isEmpty()) {
                log.warn("Post with id {} not found with details", postId);
                return new ShareDTO(postId, "Không thể tải chi tiết bài đăng");
            }

            Post post = postOptional.get();
            log.info("Found post: id={}, title={}, status={}, approvalStatus={}", 
                    post.getPostId(), post.getTitle(), post.getStatus(), post.getApprovalStatus());
            
            // Check if post is active and approved
            if (post.getStatus() == null || !post.getStatus()) {
                log.warn("Post {} is not active (status={})", postId, post.getStatus());
                return new ShareDTO(postId, "Bài đăng không còn hoạt động");
            }

            if (post.getApprovalStatus() != ApprovalStatus.APPROVED) {
                log.warn("Post {} is not approved (approvalStatus={})", postId, post.getApprovalStatus());
                return new ShareDTO(postId, "Bài đăng chưa được duyệt");
            }

            // Generate share URL
            String shareUrl = baseUrl + "/chi-tiet/" + postId;
            log.info("Generated share URL: {}", shareUrl);
            
            // Get post details for sharing
            String title = post.getTitle() != null ? post.getTitle() : "Phòng trọ cho thuê";
            String description = generateShareDescription(post);
            String imageUrl = getPostImageUrl(post);
            String price = formatPrice(post.getPrice());
            
            log.info("Share details - title: {}, description: {}, imageUrl: {}, price: {}", 
                    title, description, imageUrl, price);
            
            ShareDTO result = new ShareDTO(postId, shareUrl, title, description, imageUrl, price);
            log.info("Successfully generated share data: {}", result);
            return result;

        } catch (Exception e) {
            log.error("Error generating share URL for postId {}: {}", postId, e.getMessage(), e);
            return new ShareDTO(postId, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private String generateShareDescription(Post post) {
        try {
            StringBuilder desc = new StringBuilder();
            
            if (post.getPrice() != null) {
                desc.append("Giá: ").append(formatPrice(post.getPrice())).append(" VNĐ/tháng");
            }
            
            if (post.getArea() != null) {
                if (desc.length() > 0) desc.append(" | ");
                desc.append("Diện tích: ").append(post.getArea()).append("m²");
            }
            
            if (post.getAddress() != null) {
                if (desc.length() > 0) desc.append(" | ");
                desc.append("Địa chỉ: ").append(getAddressString(post));
            }
            
            if (desc.length() == 0) {
                desc.append("Phòng trọ cho thuê giá tốt");
            }
            
            return desc.toString();
        } catch (Exception e) {
            log.error("Error generating description: ", e);
            return "Phòng trọ cho thuê";
        }
    }

    private String getPostImageUrl(Post post) {
        try {
            if (post.getImages() != null && !post.getImages().isEmpty()) {
                String imageUrl = post.getImages().get(0).getUrl();
                log.info("Found image URL: {}", imageUrl);
                // Convert relative URL to absolute URL
                if (imageUrl != null && imageUrl.startsWith("/")) {
                    return baseUrl + imageUrl;
                }
                return imageUrl;
            }
            log.info("No images found, using default");
            return baseUrl + "/images/no-image.jpg";
        } catch (Exception e) {
            log.error("Error getting image URL: ", e);
            return baseUrl + "/images/no-image.jpg";
        }
    }

    private String getAddressString(Post post) {
        try {
            if (post.getAddress() == null) return "Chưa cập nhật";
            
            StringBuilder address = new StringBuilder();
            if (post.getAddress().getStreet() != null) {
                address.append(post.getAddress().getStreet());
            }
            if (post.getAddress().getWard() != null) {
                if (address.length() > 0) address.append(", ");
                address.append(post.getAddress().getWard().getName());
                
                if (post.getAddress().getWard().getDistrict() != null) {
                    address.append(", ").append(post.getAddress().getWard().getDistrict().getName());
                    
                    if (post.getAddress().getWard().getDistrict().getProvince() != null) {
                        address.append(", ").append(post.getAddress().getWard().getDistrict().getProvince().getName());
                    }
                }
            }
            
            return address.length() > 0 ? address.toString() : "Chưa cập nhật";
        } catch (Exception e) {
            log.error("Error getting address: ", e);
            return "Chưa cập nhật";
        }
    }

    private String formatPrice(Float price) {
        try {
            if (price == null) return "0";
            return String.format("%,.0f", price);
        } catch (Exception e) {
            log.error("Error formatting price: ", e);
            return "0";
        }
    }
}
