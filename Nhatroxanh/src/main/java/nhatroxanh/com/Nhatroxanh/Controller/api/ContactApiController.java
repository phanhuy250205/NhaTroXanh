package nhatroxanh.com.Nhatroxanh.Controller.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContactDTO;
import nhatroxanh.com.Nhatroxanh.Service.ContactService;

@RestController
@RequestMapping("/api")
public class ContactApiController {
    private static final Logger log = LoggerFactory.getLogger(ContactApiController.class);

    @Autowired
    private ContactService contactService;

    @GetMapping("/contact/{postId}")
    public ResponseEntity<ContactDTO> getContactInfo(@PathVariable Integer postId) {
        log.info("Received contact request for postId: {}", postId);
        
        if (postId == null || postId <= 0) {
            log.warn("Invalid postId: {}", postId);
            return ResponseEntity.badRequest()
                    .body(new ContactDTO(postId, "ID bài đăng không hợp lệ"));
        }

        try {
            ContactDTO contactInfo = contactService.getContactInfo(postId);
            
            if (contactInfo.isSuccess()) {
                log.info("Successfully returned contact info for postId: {}", postId);
                return ResponseEntity.ok(contactInfo);
            } else {
                log.warn("Failed to get contact info for postId {}: {}", postId, contactInfo.getMessage());
                return ResponseEntity.badRequest().body(contactInfo);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error getting contact info for postId {}: {}", postId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new ContactDTO(postId, "Lỗi hệ thống không mong muốn"));
        }
    }
}
