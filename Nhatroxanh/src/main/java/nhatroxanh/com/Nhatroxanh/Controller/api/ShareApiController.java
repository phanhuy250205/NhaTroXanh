package nhatroxanh.com.Nhatroxanh.Controller.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ShareDTO;
import nhatroxanh.com.Nhatroxanh.Service.ShareService;

@RestController
@RequestMapping("/api")
public class ShareApiController {
    private static final Logger log = LoggerFactory.getLogger(ShareApiController.class);

    @Autowired
    private ShareService shareService;

    @GetMapping("/share/{postId}")
    public ResponseEntity<ShareDTO> getShareUrl(@PathVariable Integer postId) {
        log.info("=== SHARE API REQUEST ===");
        log.info("Received share request for postId: {}", postId);
        
        if (postId == null || postId <= 0) {
            log.warn("Invalid postId: {}", postId);
            ShareDTO errorResponse = new ShareDTO(postId, "ID bài đăng không hợp lệ");
            log.info("Returning error response: {}", errorResponse);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            log.info("Calling ShareService.generateShareUrl for postId: {}", postId);
            ShareDTO shareInfo = shareService.generateShareUrl(postId);
            log.info("ShareService returned: {}", shareInfo);
            
            if (shareInfo.isSuccess()) {
                log.info("Successfully generated share URL for postId: {}", postId);
                log.info("Response data: {}", shareInfo);
                return ResponseEntity.ok(shareInfo);
            } else {
                log.warn("Failed to generate share URL for postId {}: {}", postId, shareInfo.getMessage());
                log.info("Returning bad request with message: {}", shareInfo.getMessage());
                return ResponseEntity.badRequest().body(shareInfo);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error generating share URL for postId {}: {}", postId, e.getMessage(), e);
            ShareDTO errorResponse = new ShareDTO(postId, "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    // Test endpoint to debug
    @GetMapping("/share/test/{postId}")
    public ResponseEntity<String> testShare(@PathVariable Integer postId) {
        log.info("=== SHARE TEST ENDPOINT ===");
        log.info("Testing share for postId: {}", postId);
        
        try {
            ShareDTO result = shareService.generateShareUrl(postId);
            return ResponseEntity.ok("Test result: " + result.toString());
        } catch (Exception e) {
            log.error("Test error: ", e);
            return ResponseEntity.internalServerError().body("Test error: " + e.getMessage());
        }
    }
}
