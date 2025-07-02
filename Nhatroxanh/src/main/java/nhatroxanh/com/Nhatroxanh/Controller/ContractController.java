package nhatroxanh.com.Nhatroxanh.Controller;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private UserCccdRepository userCccdRepository;

    @Autowired
    private UserService userService;

    private void initializeModelAttributes(Model model, ContractDto contract) {
        if (contract == null) {
            contract = new ContractDto();
            contract.setContractDate(LocalDate.now());
        }
        logger.info("Initializing model with contract: {}, contractDate: {}", contract, contract.getContractDate());
        model.addAttribute("contract", contract);
        model.addAttribute("contractDate", LocalDate.now());
        model.addAttribute("statusOptions", Arrays.stream(Contracts.Status.values())
                .map(Enum::name)
                .collect(Collectors.toList()));
    }

    @GetMapping("/form")
    @PreAuthorize("hasRole('OWNER')")
    public String initContractForm(Authentication authentication, Model model) {
        logger.info("Initializing contract form for user");

        ContractDto contract = new ContractDto();
        contract.setContractDate(LocalDate.now());

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String cccd = userDetails.getCccd();
            String phone = userDetails.getPhone();
            String cccdNumber = userDetails.getCccdNumber();
            Date issueDate = userDetails.getIssueDate();
            String issuePlace = userDetails.getIssuePlace();
            logger.info("User CCCD: {}, Phone: {}, CCCD Number: {}, Issue Date: {}, Issue Place: {}",
                    cccd, phone, cccdNumber, issueDate, issuePlace);

            Users user = userService.findOwnerByCccdOrPhone(authentication, cccd, phone);

            if (user == null) {
                logger.error("Owner not found for CCCD: {} or phone: {}", cccd, phone);
                model.addAttribute("error", "Không tìm thấy thông tin chủ trọ hoặc bạn không có vai trò OWNER");
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

            contract.getOwner().setFullName(user.getFullname());
            contract.getOwner().setPhone(user.getPhone());
            contract.getOwner().setCccdNumber(cccdNumber != null ? cccdNumber : cccd);
            contract.getOwner().setEmail(user.getEmail());
            if (user.getBirthday() != null) {
                contract.getOwner().setBirthday(new Date(user.getBirthday().getTime()));
            }
            contract.getOwner().setBankAccount(user.getBankAccount());
            contract.getOwner().setIssueDate(issueDate);
            contract.getOwner().setIssuePlace(issuePlace);

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
            }

            contract.setStatus("DRAFT");
            initializeModelAttributes(model, contract);
            logger.info("Contract form initialized successfully for owner: {}", user.getFullname());
            return "host/hop-dong-host";

        } catch (Exception e) {
            logger.error("Error initializing contract form: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi tải dữ liệu form: " + e.getMessage());
            initializeModelAttributes(model, contract);
            return "host/hop-dong-host";
        }
    }

    @PostMapping("/get-tenant-by-phone")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getTenantByPhone(@RequestParam String phone, Model model) {
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
                    tenantData.put("cccdNumber", tenantCccd.getCccdNumber());
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
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Không tìm thấy người thuê với số điện thoại: " + phone);
                logger.warn("No tenant found for phone: {}", phone);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error getting tenant by phone: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin người thuê: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public String createContract(
            @ModelAttribute("contract") ContractDto contract,
            @RequestParam(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestParam(value = "cccdBack", required = false) MultipartFile cccdBack,
            BindingResult result,
            Model model,
            Authentication authentication) {
        logger.info("Creating new contract with data: {}", contract);

        try {
            if (result.hasErrors()) {
                logger.error("Validation errors: {}", result.getAllErrors());
                model.addAttribute("error", "Dữ liệu không hợp lệ!");
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

            String frontImageUrl = cccdFront != null && !cccdFront.isEmpty() ? saveFile(cccdFront) : null;
            String backImageUrl = cccdBack != null && !cccdBack.isEmpty() ? saveFile(cccdBack) : null;

            if (contract.getTenant().getPhone() != null && !contract.getTenant().getPhone().trim().isEmpty()) {
                Optional<Users> tenantUser = userRepository.findByPhone(contract.getTenant().getPhone());
                if (tenantUser.isPresent()) {
                    Users user = tenantUser.get();
                    UserCccd tenantCccd = userService.findUserCccdByUserId(user.getUserId());
                    if (tenantCccd != null) {
                        contract.getTenant().setCccdNumber(tenantCccd.getCccdNumber());
                        contract.getTenant().setIssueDate(tenantCccd.getIssueDate());
                        contract.getTenant().setIssuePlace(tenantCccd.getIssuePlace());
                    } else if (contract.getTenant().getCccdNumber() != null && !contract.getTenant().getCccdNumber().trim().isEmpty()) {
                        UserCccd newTenantCccd = new UserCccd();
                        newTenantCccd.setUser(user);
                        newTenantCccd.setCccdNumber(contract.getTenant().getCccdNumber());
                        newTenantCccd.setIssueDate(contract.getTenant().getIssueDate());
                        newTenantCccd.setIssuePlace(contract.getTenant().getIssuePlace());
                        userCccdRepository.save(newTenantCccd);
                        contract.getTenant().setCccdFrontUrl(frontImageUrl);
                        contract.getTenant().setCccdBackUrl(backImageUrl);
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
                    logger.error("No tenant found with phone: {}", contract.getTenant().getPhone());
                    model.addAttribute("error", "Không tìm thấy người thuê với số điện thoại: " + contract.getTenant().getPhone());
                    initializeModelAttributes(model, contract);
                    return "host/hop-dong-host";
                }
            } else {
                logger.error("Tenant phone is empty");
                model.addAttribute("error", "Số điện thoại người thuê không được để trống!");
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

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
                    ownerCccdEntity.setCccdNumber(contract.getOwner().getCccdNumber());
                    ownerCccdEntity.setIssueDate(contract.getOwner().getIssueDate());
                    ownerCccdEntity.setIssuePlace(contract.getOwner().getIssuePlace());
                    userService.saveUserCccd(ownerCccdEntity);
                } else if (contract.getOwner().getCccdNumber() != null && !contract.getOwner().getCccdNumber().trim().isEmpty()) {
                    UserCccd newOwnerCccd = new UserCccd();
                    newOwnerCccd.setUser(owner);
                    newOwnerCccd.setCccdNumber(contract.getOwner().getCccdNumber());
                    newOwnerCccd.setIssueDate(contract.getOwner().getIssueDate());
                    newOwnerCccd.setIssuePlace(contract.getOwner().getIssuePlace());
                    userCccdRepository.save(newOwnerCccd);
                }
                Optional<Address> addressOptional = userService.findAddressByUserId(owner.getUserId());
                if (addressOptional.isPresent()) {
                    Address address = addressOptional.get();
                    address.setStreet(contract.getOwner().getStreet());
                    userService.saveAddress(address);
                }
                userService.saveUser(owner);
            } else {
                logger.error("Owner not found for CCCD: {}", ownerCccd);
                model.addAttribute("error", "Không tìm thấy chủ trọ với CCCD: " + ownerCccd);
                initializeModelAttributes(model, contract);
                return "host/hop-dong-host";
            }

            Contracts savedContract = contractService.createContract(
                    contract.getTenant().getPhone(),
                    Integer.valueOf(contract.getRoom().getRoomNumber()),
                    Date.valueOf(contract.getContractDate()),
                    contract.getTerms().getStartDate() != null ? Date.valueOf(contract.getTerms().getStartDate()) : null,
                    contract.getTerms().getEndDate() != null ? Date.valueOf(contract.getTerms().getEndDate()) : null,
                    contract.getTerms().getPrice() != null ? contract.getTerms().getPrice().floatValue() : 0.0f,
                    contract.getTerms().getDeposit() != null ? contract.getTerms().getDeposit().floatValue() : 0.0f,
                    contract.getTerms().getTerms(),
                    Contracts.Status.valueOf(contract.getStatus().toUpperCase()),
                    ownerCccd);

            logger.info("Contract created successfully with ID: {}", savedContract.getContractId());
            return "redirect:/api/contracts/list";
        } catch (Exception e) {
            logger.error("Error creating contract: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi tạo hợp đồng: " + e.getMessage());
            initializeModelAttributes(model, contract);
            return "host/hop-dong-host";
        }
    }

    @PostMapping("/debug-form")
    @PreAuthorize("hasRole('OWNER')")
    public String debugForm(
            @ModelAttribute("contract") ContractDto contract,
            BindingResult result,
            Model model) {
        logger.info("Debugging form data: {}", contract);
        logger.info("Contract ID: {}, Contract Date: {}, Status: {}",
                contract.getId(), contract.getContractDate(), contract.getStatus());
        logger.info("Owner: {}, Owner Phone: {}, Owner CCCD: {}",
                contract.getOwner().getFullName(), contract.getOwner().getPhone(), contract.getOwner().getCccdNumber());
        logger.info("Tenant: {}, Tenant Phone: {}, Tenant CCCD: {}",
                contract.getTenant().getFullName(), contract.getTenant().getPhone(), contract.getTenant().getCccdNumber());

        if (result.hasErrors()) {
            logger.error("Binding errors: {}", result.getAllErrors());
            result.getAllErrors().forEach(error -> logger.error("- {}", error.getDefaultMessage()));
        }

        model.addAttribute("debugInfo", "Check console for form data");
        initializeModelAttributes(model, contract);
        return "host/hop-dong-host";
    }

    @PostMapping("/test-params")
    public ResponseEntity<?> testParams(@RequestParam Map<String, String> params) {
        logger.info("Testing parameters: {}", params);
        Map<String, Object> result = new HashMap<>();
        result.put("parameters", params);
        result.put("parameterCount", params.size());

        params.forEach((key, value) -> logger.info("Parameter: {} = {}", key, value));

        if (params.containsKey("contractDate")) {
            logger.info("contractDate parameter found: {}", params.get("contractDate"));
            result.put("contractDateFound", true);
            result.put("contractDateValue", params.get("contractDate"));
        } else {
            logger.error("contractDate parameter NOT found!");
            result.put("contractDateFound", false);
        }

        return ResponseEntity.ok(result);
    }

    @PutMapping("/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateContract(
            @PathVariable Integer contractId,
            @RequestBody Contracts updatedContract) {
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
        try {
            Optional<Contracts> contract = contractService.findContractById(id);
            if (contract.isPresent()) {
                logger.info("Found contract with ID: {}", id);
                return ResponseEntity.ok(contract.get());
            } else {
                logger.warn("Contract with ID {} not found", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error finding contract ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body("Lỗi khi tìm hợp đồng: " + e.getMessage());
        }
    }

    private String saveFile(MultipartFile file) {
        logger.info("Saving file");
        if (file == null || file.isEmpty()) {
            logger.warn("File is null or empty, returning null");
            return null;
        }
        try {
            String originalFilename = file.getOriginalFilename();
            logger.info("Original filename: {}", originalFilename);

            if (originalFilename != null && !isValidFileType(originalFilename)) {
                logger.error("Invalid file type: {}", originalFilename);
                throw new IllegalArgumentException("Chỉ cho phép file ảnh (jpg, jpeg, png)!");
            }

            String safeFileName = System.currentTimeMillis() + "_" +
                    originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_");
            logger.info("Generated safe file name: {}", safeFileName);

            String uploadDir = "Uploads/";
            Path uploadPath = Paths.get(uploadDir);
            logger.info("Creating upload directory if not exists: {}", uploadDir);
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath);
            logger.info("File copied to: {}", filePath);

            String fileUrl = uploadDir + safeFileName;
            logger.info("File saved successfully: {}", fileUrl);
            return fileUrl;
        } catch (Exception e) {
            logger.error("Error saving file: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Lỗi khi lưu file: " + e.getMessage());
        }
    }

    private boolean isValidFileType(String fileName) {
        logger.info("Validating file type for: {}", fileName);
        String[] allowedExtensions = { ".jpg", ".jpeg", ".png" };
        boolean isValid = Arrays.stream(allowedExtensions)
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext));
        logger.info("File type validation result: {}", isValid);
        return isValid;
    }
}