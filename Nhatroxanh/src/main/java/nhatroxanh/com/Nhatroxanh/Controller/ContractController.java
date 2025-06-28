package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/contracts")
public class ContractController {

    private static final Logger logger = LoggerFactory.getLogger(ContractController.class);

    @Autowired
    private ContractService contractService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    // Helper method để khởi tạo model attributes
    private void initializeModelAttributes(Model model, ContractDto contract) {
        if (contract == null) {
            contract = new ContractDto();
        }

        model.addAttribute("contract", contract);
        model.addAttribute("contractDate", LocalDate.now());
        model.addAttribute("statusOptions", Arrays.stream(Contracts.Status.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
    }

    // Endpoint để hiển thị form tạo hợp đồng
    @GetMapping("/form")
    @PreAuthorize("hasRole('OWNER')")
    public String initContractForm(Authentication authentication, Model model) {
        System.out.println("=== START: Initializing contract form ===");
        logger.info("Initializing contract form for user");

        ContractDto contract = new ContractDto();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String cccd = userDetails.getCccd();
            String phone = userDetails.getPhone();
            System.out.println("User CCCD: " + cccd + ", Phone: " + phone);

            Users user = userService.findOwnerByCccdOrPhone(authentication, cccd, phone);
            if (user == null) {
                System.out.println("❌ Owner not found for CCCD: " + cccd + " or phone: " + phone);
                model.addAttribute("error", "Không tìm thấy thông tin chủ trọ hoặc bạn không có vai trò OWNER");
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

            // Gán thông tin chủ trọ
            contract.getOwner().setFullName(user.getFullname());
            contract.getOwner().setPhone(user.getPhone());
            contract.getOwner().setId(user.getCccd());
            contract.getOwner().setEmail(user.getEmail());
            if (user.getBirthday() != null) {
                contract.getOwner().setBirthday(new Date(user.getBirthday().getTime()));
            }
            contract.getOwner().setBankAccount(user.getBankAccount());

            UserCccd userCccd = userService.findUserCccdByUserId(user.getUserId());
            if (userCccd != null) {
                contract.getOwner().setIssueDate(userCccd.getIssueDate());
                contract.getOwner().setIssuePlace(userCccd.getIssuePlace());
            }

            Optional<Address> addressOptional = userService.findAddressByUserId(user.getUserId());
            if (addressOptional.isPresent()) {
                Address address = addressOptional.get();
                contract.getOwner().setStreet(address.getStreet());
                if (address.getWard() != null) {
                    contract.getOwner().setWard(address.getWard().getName());
                    if (address.getWard().getDistrict() != null) {
                        contract.getOwner().setDistrict(address.getWard().getDistrict().getName());
                        if (address.getWard().getDistrict().getProvince() != null) {
                            contract.getOwner().setProvince(address.getWard().getDistrict().getProvince().getName());
                        }
                    }
                }
                logger.info("Address for userId {}: Street={}, Ward={}, District={}, Province={}", user.getUserId(), address.getStreet(), address.getWard() != null ? address.getWard().getName() : null, address.getWard() != null && address.getWard().getDistrict() != null ? address.getWard().getDistrict().getName() : null, address.getWard() != null && address.getWard().getDistrict() != null && address.getWard().getDistrict().getProvince() != null ? address.getWard().getDistrict().getProvince().getName() : null);
            } else {
                logger.warn("No Address found for userId: {}", user.getUserId());
            }

            contract.setContractDate(LocalDate.now());
            contract.setStatus("DRAFT");

            logger.info("Contract form initialized successfully for owner: {}", user.getFullname());
            System.out.println("=== END: Contract form initialized successfully ===");

        } catch (Exception e) {
            System.out.println("❌ Error initializing contract form: " + e.getMessage());
            logger.error("Error initializing contract form: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi tải dữ liệu form: " + e.getMessage());
        }

        initializeModelAttributes(model, contract);
        return "host/hop-dong-host";
    }

    // Endpoint để lấy thông tin người thuê dựa trên số điện thoại
    @PostMapping("/get-tenant-by-phone")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getTenantByPhone(@RequestParam String phone, Model model) {
        System.out.println("=== START: Getting tenant by phone: " + phone + " ===");
        logger.info("Request to get tenant by phone: {}", phone);

        Map<String, Object> response = new HashMap<>();
        try {
            Optional<Users> tenantUser = userRepository.findByPhone(phone);
            if (tenantUser.isPresent()) {
                Users user = tenantUser.get();
                UserCccd tenantCccd = userService.findUserCccdByUserId(user.getUserId());
                Optional<Address> tenantAddress = userService.findAddressByUserId(user.getUserId());

                Map<String, Object> tenantData = new HashMap<>();
                tenantData.put("fullName", user.getFullname());
                tenantData.put("phone", user.getPhone());
                if (user.getBirthday() != null) {
                    tenantData.put("birthday", user.getBirthday().toString());
                }
                if (tenantCccd != null) {
                    tenantData.put("id", tenantCccd.getCccdNumber());
                    tenantData.put("issueDate", tenantCccd.getIssueDate() != null ? tenantCccd.getIssueDate().toString() : null);
                    tenantData.put("issuePlace", tenantCccd.getIssuePlace());
                }
                if (tenantAddress.isPresent()) {
                    Address address = tenantAddress.get();
                    tenantData.put("street", address.getStreet());
                    if (address.getWard() != null) {
                        tenantData.put("ward", address.getWard().getName());
                        if (address.getWard().getDistrict() != null) {
                            tenantData.put("district", address.getWard().getDistrict().getName());
                            if (address.getWard().getDistrict().getProvince() != null) {
                                tenantData.put("province", address.getWard().getDistrict().getProvince().getName());
                            }
                        }
                    }
                }

                response.put("success", true);
                response.put("tenant", tenantData);
                logger.info("Found tenant data: {}", tenantData);
                System.out.println("✅ Found tenant data: " + tenantData);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy người thuê với số điện thoại: " + phone);
                logger.warn("No tenant found for phone: {}", phone);
                System.out.println("❌ No tenant found for phone: " + phone);
            }
            System.out.println("=== END: Getting tenant by phone ===");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error getting tenant by phone: " + e.getMessage());
            logger.error("Error getting tenant by phone: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin người thuê: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Endpoint để tạo hợp đồng
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public String createContract(
            @ModelAttribute("contract") ContractDto contract,
            @RequestParam(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestParam(value = "cccdBack", required = false) MultipartFile cccdBack,
            BindingResult result,
            Model model,
            Authentication authentication) {
        System.out.println("=== START: Creating new contract ===");
        System.out.println("Contract data received: " + contract);
        logger.info("Creating new contract with data: {}", contract);

        try {
            if (result.hasErrors()) {
                System.out.println("❌ Validation errors: " + result.getAllErrors());
                logger.error("Validation errors: {}", result.getAllErrors());
                model.addAttribute("error", "Dữ liệu không hợp lệ!");
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

            // Kiểm tra và tự động điền thông tin người thuê nếu có số điện thoại
            if (contract.getTenant().getPhone() != null && !contract.getTenant().getPhone().trim().isEmpty()) {
                Optional<Users> tenantUser = userRepository.findByPhone(contract.getTenant().getPhone());
                if (tenantUser.isPresent()) {
                    Users user = tenantUser.get();
                    UserCccd tenantCccd = userService.findUserCccdByUserId(user.getUserId());
                    if (tenantCccd != null) {
                        contract.getTenant().setId(tenantCccd.getCccdNumber());
                        contract.getTenant().setIssueDate(tenantCccd.getIssueDate());
                        contract.getTenant().setIssuePlace(tenantCccd.getIssuePlace());
                    }
                    contract.getTenant().setFullName(user.getFullname());
                    contract.getTenant().setPhone(user.getPhone());
                    if (user.getBirthday() != null) {
                        contract.getTenant().setBirthday(new Date(user.getBirthday().getTime()));
                    }
                    Optional<Address> tenantAddress = userService.findAddressByUserId(user.getUserId());
                    if (tenantAddress.isPresent()) {
                        Address address = tenantAddress.get();
                        contract.getTenant().setStreet(address.getStreet());
                        if (address.getWard() != null) {
                            contract.getTenant().setWard(address.getWard().getName());
                            if (address.getWard().getDistrict() != null) {
                                contract.getTenant().setDistrict(address.getWard().getDistrict().getName());
                                if (address.getWard().getDistrict().getProvince() != null) {
                                    contract.getTenant().setProvince(address.getWard().getDistrict().getProvince().getName());
                                }
                            }
                        }
                    }
                } else {
                    model.addAttribute("error", "Không tìm thấy người thuê với số điện thoại: " + contract.getTenant().getPhone());
                    initializeModelAttributes(model, contract);
                    return "host/hop-dong-host";
                }
            } else {
                model.addAttribute("error", "Số điện thoại người thuê không được để trống!");
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

            // Cập nhật thông tin chủ trọ
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();
            Users owner = userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null);
            if (owner != null) {
                owner.setFullname(contract.getOwner().getFullName());
                owner.setPhone(contract.getOwner().getPhone());
                if (contract.getOwner().getBirthday() != null) {
                    owner.setBirthday(new java.sql.Date(contract.getOwner().getBirthday().getTime()));
                }
                UserCccd ownerCccdEntity = userService.findUserCccdByUserId(owner.getUserId());
                if (ownerCccdEntity != null) {
                    ownerCccdEntity.setCccdNumber(contract.getOwner().getId());
                    ownerCccdEntity.setIssueDate(contract.getOwner().getIssueDate());
                    ownerCccdEntity.setIssuePlace(contract.getOwner().getIssuePlace());
                    userService.saveUserCccd(ownerCccdEntity);
                }
                Optional<Address> addressOptional = userService.findAddressByUserId(owner.getUserId());
                if (addressOptional.isPresent()) {
                    Address address = addressOptional.get();
                    address.setStreet(contract.getOwner().getStreet());
                    userService.saveAddress(address);
                }
                userService.saveUser(owner);

            }

            String frontImageUrl = saveFile(cccdFront);
            String backImageUrl = saveFile(cccdBack);

            CustomUserDetails userDetails1 = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd1= userDetails.getCccd();

            // Tạm thời bỏ các trường Room và Terms, chỉ lưu thông tin cơ bản
            Contracts savedContract = contractService.createContract(
                    contract.getTenant().getPhone(),
                    null, // Room number tạm thời để null
                    Date.valueOf(contract.getContractDate()),
                    null, // Start date tạm thời để null
                    null, // End date tạm thời để null
                    0.0f, // Price tạm thời để 0
                    0.0f, // Deposit tạm thời để 0
                    null, // Terms tạm thời để null
                    Contracts.Status.valueOf(contract.getStatus().toUpperCase()),
                    ownerCccd);

            System.out.println("✅ Contract created successfully with ID: " + savedContract.getContractId());
            logger.info("Contract created successfully with ID: {}", savedContract.getContractId());
            return "redirect:/api/contracts/list";
        } catch (Exception e) {
            System.out.println("❌ Error creating contract: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error creating contract: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi tạo hợp đồng: " + e.getMessage());
            initializeModelAttributes(model, contract);
            return "host/hop-dong-host";
        }
    }

    // Endpoint debug form data
    @PostMapping("/debug-form")
    @PreAuthorize("hasRole('OWNER')")
    public String debugForm(
            @ModelAttribute("contract") ContractDto contract,
            BindingResult result,
            Model model) {
        System.out.println("=== DEBUG FORM DATA ===");
        System.out.println("Contract: " + contract);
        System.out.println("Contract ID: " + contract.getId());
        System.out.println("Contract Date: " + contract.getContractDate());
        System.out.println("Status: " + contract.getStatus());

        System.out.println("Owner: " + contract.getOwner().getFullName());
        System.out.println("Owner Phone: " + contract.getOwner().getPhone());

        System.out.println("Tenant: " + contract.getTenant().getFullName());
        System.out.println("Tenant Phone: " + contract.getTenant().getPhone());

        if (result.hasErrors()) {
            System.out.println("Binding errors:");
            result.getAllErrors().forEach(error -> {
                System.out.println("- " + error.getDefaultMessage());
            });
        }

        model.addAttribute("debugInfo", "Check console for form data");
        initializeModelAttributes(model, contract);
        return "host/hop-dong-host";
    }

    // Endpoint test parameters
    @PostMapping("/test-params")
    public ResponseEntity<?> testParams(@RequestParam Map<String, String> params) {
        System.out.println("=== START: Testing parameters ===");
        logger.info("=== TEST PARAMETERS ENDPOINT ===");
        Map<String, Object> result = new HashMap<>();
        result.put("parameters", params);
        result.put("parameterCount", params.size());

        System.out.println("Received parameters: ");
        params.forEach((key, value) -> {
            System.out.println("Parameter: " + key + " = " + value);
            logger.info("Parameter: {} = {}", key, value);
        });

        if (params.containsKey("contractDate")) {
            System.out.println("✅ contractDate parameter found: " + params.get("contractDate"));
            logger.info("✅ contractDate parameter found: {}", params.get("contractDate"));
            result.put("contractDateFound", true);
            result.put("contractDateValue", params.get("contractDate"));
        } else {
            System.out.println("❌ contractDate parameter NOT found!");
            logger.error("❌ contractDate parameter NOT found!");
            result.put("contractDateFound", false);
        }

        System.out.println("=== END: Parameter testing completed ===");
        return ResponseEntity.ok(result);
    }

    // Endpoint cập nhật hợp đồng
    @PutMapping("/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateContract(
            @PathVariable Integer contractId,
            @RequestBody Contracts updatedContract) {
        System.out.println("=== START: Updating contract with ID: " + contractId + " ===");
        logger.info("Received request to update contract with ID: {}", contractId);
        try {
            System.out.println("Calling contract service to update contract");
            Contracts contract = contractService.updateContract(contractId, updatedContract);
            System.out.println("✅ Contract updated successfully with ID: " + contractId);
            logger.info("Contract updated successfully with ID: {}", contractId);
            System.out.println("=== END: Contract updated successfully ===");
            return ResponseEntity.ok(contract);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid data for updating contract ID " + contractId + ": " + e.getMessage());
            logger.error("Invalid data for updating contract ID {}: {}", contractId, e.getMessage());
            System.out.println("=== END: Contract update failed ===");
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error updating contract ID " + contractId + ": " + e.getMessage());
            e.printStackTrace();
            logger.error("Error updating contract ID {}: {}", contractId, e.getMessage(), e);
            if (e.getMessage().contains("Hợp đồng không tồn tại")) {
                System.out.println("=== END: Contract not found ===");
                return ResponseEntity.status(404).body("Hợp đồng không tồn tại!");
            }
            System.out.println("=== END: Contract update failed ===");
            return ResponseEntity.badRequest().body("Lỗi khi cập nhật hợp đồng. Vui lòng thử lại.");
        }
    }

    // Endpoint xóa hợp đồng
    @DeleteMapping("/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> deleteContract(@PathVariable Integer contractId) {
        System.out.println("=== START: Deleting contract with ID: " + contractId + " ===");
        logger.info("Received request to delete contract with ID: {}", contractId);
        try {
            System.out.println("Calling contract service to delete contract");
            contractService.deleteContract(contractId);
            System.out.println("✅ Contract deleted successfully with ID: " + contractId);
            logger.info("Contract deleted successfully with ID: {}", contractId);
            System.out.println("=== END: Contract deleted successfully ===");
            return ResponseEntity.ok("Hợp đồng đã được xóa!");
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid data for deleting contract ID " + contractId + ": " + e.getMessage());
            logger.error("Invalid data for deleting contract ID {}: {}", contractId, e.getMessage());
            System.out.println("=== END: Contract deletion failed ===");
            return ResponseEntity.badRequest().body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Error deleting contract ID " + contractId + ": " + e.getMessage());
            logger.error("Error deleting contract ID {}: {}", contractId, e.getMessage(), e);
            if (e.getMessage().contains("Hợp đồng không tồn tại")) {
                System.out.println("=== END: Contract not found ===");
                return ResponseEntity.status(404).body("Hợp đồng không tồn tại!");
            }
            System.out.println("=== END: Contract deletion failed ===");
            return ResponseEntity.badRequest().body("Lỗi khi xóa hợp đồng. Vui lòng thử lại.");
        }
    }

    // Endpoint tìm hợp đồng theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> findContractById(@PathVariable Integer id) {
        System.out.println("=== START: Finding contract by ID: " + id + " ===");
        logger.info("Received request to find contract by ID: {}", id);
        System.out.println("Calling contract service to find contract");
        Optional<Contracts> contract = contractService.findContractById(id);
        if (contract.isPresent()) {
            System.out.println("✅ Found contract with ID: " + id);
            logger.info("Found contract with ID: {}", id);
            System.out.println("=== END: Contract found ===");
            return ResponseEntity.ok(contract.get());
        } else {
            System.out.println("❌ Contract with ID " + id + " not found");
            logger.warn("Contract with ID {} not found", id);
            System.out.println("=== END: Contract not found ===");
            return ResponseEntity.notFound().build();
        }
    }

    private String saveFile(MultipartFile file) {
        System.out.println("=== START: Saving file ===");
        if (file == null || file.isEmpty()) {
            System.out.println("❌ File is null or empty, returning null");
            logger.warn("File is null or empty, returning null");
            System.out.println("=== END: File saving skipped ===");
            return null;
        }
        try {
            String originalFilename = file.getOriginalFilename();
            System.out.println("Original filename: " + originalFilename);

            System.out.println("Validating file type");
            if (originalFilename != null && !isValidFileType(originalFilename)) {
                System.out.println("❌ Invalid file type: " + originalFilename);
                logger.error("Invalid file type: {}", originalFilename);
                throw new IllegalArgumentException("Chỉ cho phép file ảnh (jpg, jpeg, png)!");
            }

            System.out.println("Generating safe file name");
            String safeFileName = System.currentTimeMillis() + "_" +
                    originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
            System.out.println("Generated safe file name: " + safeFileName);

            String uploadDir = "uploads/";
            System.out.println("Upload directory: " + uploadDir);
            Path uploadPath = Paths.get(uploadDir);
            System.out.println("Creating upload directory if not exists");
            Files.createDirectories(uploadPath);
            System.out.println("Upload directory created: " + uploadDir);

            System.out.println("Saving file to path");
            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath);
            System.out.println("File copied to: " + filePath);

            String fileUrl = uploadDir + safeFileName;
            System.out.println("✅ File saved successfully: " + fileUrl);
            logger.info("File saved successfully: {}", fileUrl);
            System.out.println("=== END: File saved successfully ===");
            return fileUrl;
        } catch (Exception e) {
            System.out.println("❌ Error saving file: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error saving file: {}", e.getMessage(), e);
            System.out.println("=== END: File saving failed ===");
            throw new IllegalArgumentException("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String fileName) {
        System.out.println("=== START: Validating file type for: " + fileName + " ===");
        String[] allowedExtensions = { ".jpg", ".jpeg", ".png" };
        boolean isValid = Arrays.stream(allowedExtensions)
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext));
        System.out.println("File type validation result: " + isValid);
        System.out.println("=== END: File type validation ===");
        return isValid;
    }
}