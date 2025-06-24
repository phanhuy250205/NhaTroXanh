package nhatroxanh.com.Nhatroxanh.Controller.web.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;

import java.util.Map;

@RestController
@RequestMapping("/api/utilities")
public class UtilityController {

    @Autowired
    private nhatroxanh.com.Nhatroxanh.Service.UtilityService utilityService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createUtility(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            Utility utility = utilityService.createUtility(name);
            return ResponseEntity.ok(utility);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteUtility(@PathVariable Integer id) {
        try {
            utilityService.deleteUtility(id);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}