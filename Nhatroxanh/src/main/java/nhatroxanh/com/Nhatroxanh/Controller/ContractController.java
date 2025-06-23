package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Autowired
    private ContractService contractService;

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> createContract(
            @RequestParam("tenantPhone") String tenantPhone,
            @RequestParam("roomId") Integer roomId,
            @RequestParam("contractDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate contractDate,
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam("price") Float price,
            @RequestParam(value = "deposit", required = false, defaultValue = "0") Float deposit,
            @RequestParam(value = "terms", required = false) String terms,
            @RequestParam("status") String status,
            @RequestParam("ownerId") Integer ownerId,
            @RequestParam(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestParam(value = "cccdBack", required = false) MultipartFile cccdBack
    ) {
        logger.info("=== DEBUG: CONTRACT CREATION REQUEST ===");
        logger.info("tenantPhone: {}", tenantPhone);
        logger.info("roomId: {}", roomId);
        logger.info("contractDate: {}", contractDate);
        logger.info("startDate: {}", startDate);
        logger.info("endDate: {}", endDate);
        logger.info("price: {}", price);
        logger.info("deposit: {}", deposit);
        logger.info("terms: {}", terms);
        logger.info("status: {}", status);
        logger.info("ownerId: {}", ownerId);
        logger.info("cccdFront: {}", cccdFront != null ? cccdFront.getOriginalFilename() : "null");
        logger.info("cccdBack: {}", cccdBack != null ? cccdBack.getOriginalFilename() : "null");

        try {
            // Validation
            if (contractDate == null) {
                logger.error("❌ CONTRACT DATE IS NULL!");
                return ResponseEntity.badRequest().body("Ngày hợp đồng không được để trống!");
            }
            if (roomId == null || roomId <= 0) {
                logger.error("❌ Invalid room ID: {}", roomId);
                return ResponseEntity.badRequest().body("ID phòng không hợp lệ!");
            }
            if (ownerId == null || ownerId <= 0) {
                logger.error("❌ Invalid owner ID: {}", ownerId);
                return ResponseEntity.badRequest().body("ID chủ trọ không hợp lệ!");
            }
            if (tenantPhone == null || tenantPhone.trim().isEmpty()) {
                logger.error("❌ Tenant phone is null or empty");
                return ResponseEntity.badRequest().body("Số điện thoại khách thuê không được để trống!");
            }

            // Convert dates
            Date sqlContractDate = Date.valueOf(contractDate);
            Date sqlStartDate = Date.valueOf(startDate);
            Date sqlEndDate = Date.valueOf(endDate);

            // Process file upload
            String frontImageUrl = saveFile(cccdFront);
            String backImageUrl = saveFile(cccdBack);

            // Validate status
            Contracts.Status contractStatus;
            try {
                contractStatus = Contracts.Status.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                String validStatuses = Arrays.stream(Contracts.Status.values())
                        .map(Enum::name)
                        .collect(Collectors.joining(", "));
                logger.error("❌ Invalid status: {}", status);
                return ResponseEntity.badRequest()
                        .body("Trạng thái không hợp lệ! Các trạng thái hợp lệ: " + validStatuses);
            }

            // Call service
            Contracts contract = contractService.createContract(
                    tenantPhone, roomId, sqlContractDate, sqlStartDate, sqlEndDate, price,
                    deposit, terms, contractStatus, ownerId
            );

            logger.info("✅ Contract created successfully with ID: {}", contract.getContractId());
            return ResponseEntity.ok(contract);

        } catch (Exception e) {
            logger.error("❌ ERROR CREATING CONTRACT: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Lỗi khi tạo hợp đồng");
            errorResponse.put("message", "Vui lòng kiểm tra dữ liệu và thử lại.");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/test-params")
    public ResponseEntity<?> testParams(@RequestParam Map<String, String> params) {
        logger.info("=== TEST PARAMETERS ENDPOINT ===");
        Map<String, Object> result = new HashMap<>();
        result.put("parameters", params);
        result.put("parameterCount", params.size());

        params.forEach((key, value) -> logger.info("Parameter: {} = {}", key, value));

        if (params.containsKey("contractDate")) {
            logger.info("✅ contractDate parameter found: {}", params.get("contractDate"));
            result.put("contractDateFound", true);
            result.put("contractDateValue", params.get("contractDate"));
        } else {
            logger.error("❌ contractDate parameter NOT found!");
            result.put("contractDateFound", false);
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateContract(
            @PathVariable Integer contractId,
            @RequestBody Contracts updatedContract
    ) {
        logger.info("Received request to update contract with ID: {}", contractId);
        try {
            Contracts contract = contractService.updateContract(contractId, updatedContract);
            logger.info("Contract updated successfully with ID: {}", contractId);
            return ResponseEntity.ok(contract);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data for updating contract ID {}: {}", contractId, e.getMessage());
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating contract ID {}: {}", contractId, e.getMessage(), e);
            if (e.getMessage().contains("Hợp đồng không tồn tại")) {
                return ResponseEntity.status(404).body("Hợp đồng không tồn tại!");
            }
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật hợp đồng. Vui lòng thử lại.");
        }
    }

    @DeleteMapping("/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteContract(@PathVariable Integer contractId) {
        logger.info("Received request to delete contract with ID: {}", contractId);
        try {
            contractService.deleteContract(contractId);
            logger.info("Contract deleted successfully with ID: {}", contractId);
            return ResponseEntity.ok("Hợp đồng đã được xóa!");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid data for deleting contract ID {}: {}", contractId, e.getMessage());
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting contract ID {}: {}", contractId, e.getMessage(), e);
            if (e.getMessage().contains("Hợp đồng không tồn tại")) {
                return ResponseEntity.status(404).body("Hợp đồng không tồn tại!");
            }
            return ResponseEntity.badRequest().body("Lỗi khi xóa hợp đồng. Vui lòng thử lại.");
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> findContractById(@PathVariable Integer id) {
        logger.info("Received request to find contract by ID: {}", id);
        Optional<Contracts> contract = contractService.findContractById(id);
        if (contract.isPresent()) {
            logger.info("Found contract with ID: {}", id);
            return ResponseEntity.ok(contract.get());
        } else {
            logger.warn("Contract with ID {} not found", id);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsByRoomId(@PathVariable Integer roomId) {
        logger.info("Received request to find contracts by roomId: {}", roomId);
        if (roomId == null || roomId <= 0) {
            logger.error("Invalid room ID: {}", roomId);
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByRoomId(roomId);
        logger.info("Found {} contracts for roomId: {}", contracts.size(), roomId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/tenant/{tenantUserId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsByTenantUserId(@PathVariable Integer tenantUserId) {
        logger.info("Received request to find contracts by tenantUserId: {}", tenantUserId);
        if (tenantUserId == null || tenantUserId <= 0) {
            logger.error("Invalid tenantUserId: {}", tenantUserId);
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByTenantUserId(tenantUserId);
        logger.info("Found {} contracts for tenantUserId: {}", contracts.size(), tenantUserId);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/tenant/name")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsByTenantName(@RequestParam String name) {
        logger.info("Received request to find contracts by tenant name: {}", name);
        if (name == null || name.trim().isEmpty()) {
            logger.error("Tenant name is null or empty");
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByTenantName(name);
        logger.info("Found {} contracts for tenant name: {}", contracts.size(), name);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/tenant/phone")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsByTenantPhone(@RequestParam String phone) {
        logger.info("Received request to find contracts by tenant phone: {}", phone);
        if (phone == null || phone.trim().isEmpty()) {
            logger.error("Tenant phone is null or empty");
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByTenantPhone(phone);
        logger.info("Found {} contracts for tenant phone: {}", contracts.size(), phone);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/tenant/cccd")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsByTenantCccd(@RequestParam String cccd) {
        logger.info("Received request to find contracts by tenant CCCD: {}", cccd);
        if (cccd == null || cccd.trim().isEmpty()) {
            logger.error("Tenant CCCD is null or empty");
            return ResponseEntity.badRequest().build();
        }
        List<Contracts> contracts = contractService.findContractsByTenantCccd(cccd);
        logger.info("Found {} contracts for tenant CCCD: {}", contracts.size(), cccd);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsByDateRange(
            @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        logger.info("Received request to find contracts by date range: {} to {}", startDate, endDate);
        if (startDate == null || endDate == null) {
            logger.error("Start date or end date is null");
            return ResponseEntity.badRequest().build();
        }
        Date sqlStartDate = Date.valueOf(startDate);
        Date sqlEndDate = Date.valueOf(endDate);
        List<Contracts> contracts = contractService.findContractsByDateRange(sqlStartDate, sqlEndDate);
        logger.info("Found {} contracts for date range: {} to {}", contracts.size(), startDate, endDate);
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<List<Contracts>> findContractsExpiringWithin30Days() {
        logger.info("Received request to find contracts expiring within 30 days");
        List<Contracts> contracts = contractService.findContractsExpiringWithin30Days();
        logger.info("Found {} contracts expiring within 30 days", contracts.size());
        return ResponseEntity.ok(contracts);
    }

    @GetMapping("/room/{roomId}/active")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> findActiveContractByRoomId(@PathVariable Integer roomId) {
        logger.info("Received request to find active contract by roomId: {}", roomId);
        if (roomId == null || roomId <= 0) {
            logger.error("Invalid room ID: {}", roomId);
            return ResponseEntity.badRequest().build();
        }
        Optional<Contracts> contract = contractService.findActiveContractByRoomId(roomId);
        if (contract.isPresent()) {
            logger.info("Found active contract for roomId: {}", roomId);
            return ResponseEntity.ok(contract.get());
        } else {
            logger.info("No active contract found for roomId: {}", roomId);
            return ResponseEntity.ok("Phòng không có hợp đồng active");
        }
    }

    private String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            logger.warn("File is null or empty, returning null");
            return null;
        }
        try {
            // Kiểm tra định dạng file
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && !isValidFileType(originalFilename)) {
                logger.error("Invalid file type: {}", originalFilename);
                throw new IllegalArgumentException("Chỉ cho phép file ảnh (jpg, jpeg, png)!");
            }

            // Làm sạch tên file
            String safeFileName = System.currentTimeMillis() + "_" +
                    originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");

            // Sử dụng đường dẫn cấu hình
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath);

            String fileUrl = uploadDir + safeFileName;
            logger.info("File saved successfully: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            logger.error("Error saving file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String fileName) {
        String[] allowedExtensions = {".jpg", ".jpeg", ".png"};
        return Arrays.stream(allowedExtensions)
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext));
    }
}