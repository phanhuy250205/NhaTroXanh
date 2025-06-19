package nhatroxanh.com.Nhatroxanh.Service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContactDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;

@Service
@Transactional(readOnly = true)
public class ContactService {
    private static final Logger log = LoggerFactory.getLogger(ContactService.class);

    @Autowired
    private PostRepository postRepository;

    public ContactDTO getContactInfo(Integer postId) {
        try {
            log.info("Fetching contact info for postId: {}", postId);

            // Use custom query to fetch post with user in one query
            Optional<Post> postOptional = postRepository.findPostWithUserById(postId);
            
            if (postOptional.isEmpty()) {
                log.warn("Post with id {} not found", postId);
                return new ContactDTO(postId, "Bài đăng không tồn tại");
            }

            Post post = postOptional.get();
            
            // Check if post is active and approved
            if (post.getStatus() == null || !post.getStatus()) {
                log.warn("Post {} is not active", postId);
                return new ContactDTO(postId, "Bài đăng không còn hoạt động");
            }

            if (post.getApprovalStatus() != ApprovalStatus.APPROVED) {
                log.warn("Post {} is not approved", postId);
                return new ContactDTO(postId, "Bài đăng chưa được duyệt");
            }

            Users owner = post.getUser();
            if (owner == null) {
                log.warn("Owner not found for postId {}", postId);
                return new ContactDTO(postId, "Không tìm thấy thông tin chủ sở hữu");
            }

            String phoneNumber = owner.getPhone();
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                log.warn("Phone number not available for postId {}", postId);
                return new ContactDTO(postId, "Số điện thoại không khả dụng");
            }

            // Clean and validate phone number
            phoneNumber = cleanPhoneNumber(phoneNumber);
            if (!isValidPhoneNumber(phoneNumber)) {
                log.warn("Invalid phone number format for postId {}", postId);
                return new ContactDTO(postId, "Số điện thoại không hợp lệ");
            }

            String ownerName = owner.getFullname() != null ? owner.getFullname() : "Chủ trọ";
            
            log.info("Successfully retrieved contact info for postId {}", postId);
            return new ContactDTO(postId, phoneNumber, ownerName);

        } catch (Exception e) {
            log.error("Error fetching contact info for postId {}: {}", postId, e.getMessage(), e);
            return new ContactDTO(postId, "Lỗi hệ thống. Vui lòng thử lại sau");
        }
    }

    private String cleanPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) return null;
        
        // Remove all non-digit characters except +
        String cleaned = phoneNumber.replaceAll("[^0-9+]", "");
        
        // Convert Vietnamese phone format
        if (cleaned.startsWith("84")) {
            cleaned = "0" + cleaned.substring(2);
        } else if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3);
        }
        
        return cleaned;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 10) {
            return false;
        }
        
        // Vietnamese phone number patterns
        return phoneNumber.matches("^(0[3|5|7|8|9])[0-9]{8}$") || 
               phoneNumber.matches("^(\\+84[3|5|7|8|9])[0-9]{8}$");
    }
}
