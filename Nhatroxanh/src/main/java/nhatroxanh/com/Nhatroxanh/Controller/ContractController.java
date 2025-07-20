package nhatroxanh.com.Nhatroxanh.Controller;

import jakarta.validation.Valid;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.*;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
//@RestController
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
    private UnregisteredTenantsRepository unregisteredTenantsRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private RoomsService roomsService;
    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ImageService imageService; // Sử dụng ImageService thay vì ImageRepository
    @Autowired
    private ContractsRepository contractsRepository;
    private Integer hostelId;
    private Integer currentRoomId;
    private Authentication authentication;

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
    @PostMapping("/form")
    @PreAuthorize("hasRole('OWNER')")
    public String initContractForm(
            @RequestParam(value = "hostelId", required = false) Integer hostelId,
            Authentication authentication,
            Model model) {
        logger.info("Initializing contract form for user, hostelId: {}", hostelId);

        ContractDto contract = new ContractDto();
        contract.setContractDate(LocalDate.now());

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();
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

//            Optional<Address> addressOptional = userService.findAddressByUserId(user.getUserId());
//            if (addressOptional.isPresent()) {
//                Address address = addressOptional.get();
//                contract.getOwner().setStreet(address.getStreet());
//                if (address.getWard() != null) {
//                    contract.getOwner().setWard(address.getWard().getName());
//                    if (address.getWard().getDistrict() != null) {
//                        contract.getOwner().setDistrict(address.getWard().getDistrict().getName());
//                        if (address.getWard().getDistrict().getProvince() != null) {
//                            contract.getOwner().setProvince(address.getWard().getDistrict().getProvince().getName());
//                        }
//                    }
//                }
//            }

            // Lấy địa chỉ trực tiếp từ cột address của Users
            String address = user.getAddress();
            if (StringUtils.hasText(address)) {
                // Tách địa chỉ thành các thành phần nếu cần
                Map<String, String> addressParts = parseAddress(address);
                contract.getOwner().setStreet(addressParts.getOrDefault("street", ""));
                contract.getOwner().setWard(addressParts.getOrDefault("ward", ""));
                contract.getOwner().setDistrict(addressParts.getOrDefault("district", ""));
                contract.getOwner().setProvince(addressParts.getOrDefault("province", ""));
            } else {
                logger.warn("No address found for user ID: {}", user.getUserId());
            }

            // Lấy danh sách khu trọ
            List<Hostel> hostels = hostelService.getHostelsWithRoomsByOwnerId(ownerId);
            if (hostels.isEmpty()) {
                logger.warn("No hostels found for ownerId: {}. Check owner_id in hostels table.", ownerId);
                model.addAttribute("error", "Không tìm thấy khu trọ nào cho chủ trọ này.");
            } else {
                hostels.forEach(hostel -> logger.info("Hostel ID: {}, Name: {}, Rooms: {}", hostel.getHostelId(), hostel.getName(), hostel.getRooms().size()));
                model.addAttribute("hostels", hostels);
            }

            // Lấy danh sách phòng trọ nếu có hostelId
            List<ContractDto.Room> rooms = new ArrayList<>();
            if (hostelId != null) {
                rooms = roomsService.getRoomsByHostelId(hostelId);
                if (rooms.isEmpty()) {
                    logger.warn("No rooms found for hostelId: {}. Check hostel_id and rooms table.", hostelId);
                } else {
                    logger.info("Found {} rooms for hostelId: {} - Rooms: {}", rooms.size(), hostelId, rooms.stream().map(r -> r.getRoomName()).collect(Collectors.joining(", ")));
                }
            } else {
                logger.info("No hostelId provided, rooms list remains empty.");
            }
            model.addAttribute("rooms", rooms);

            contract.setStatus("DRAFT");
            initializeModelAttributes(model, contract);
            logger.info("Contract form initialized successfully for owner: {}", user.getFullname());
            return "host/hop-dong-host";

        } catch (Exception e) {
            logger.error("Error initializing contract form: {}", e.getMessage(), e);
            model.addAttribute("error", "Lỗi khi tải dữ liệu form: " + e.getMessage());
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            model.addAttribute("hostels", hostelService.getHostelsWithRoomsByOwnerId(userDetails.getUserId()));
            initializeModelAttributes(model, contract);
            return "host/hop-dong-host";
        }
    }

    // Thêm endpoint này vào ContractController của bạn

    @GetMapping("/get-rooms-by-hostel")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomsByHostel(
            @RequestParam Integer hostelId,
            Authentication authentication) {
        logger.info("Getting rooms for hostelId: {}", hostelId);

        Map<String, Object> response = new HashMap<>();
        try {
            // Kiểm tra quyền sở hữu khu trọ
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            // Lấy danh sách khu trọ của owner để verify ownership
            List<Hostel> hostels = hostelService.getHostelsWithRoomsByOwnerId(ownerId);
            boolean isOwner = hostels.stream()
                    .anyMatch(hostel -> hostel.getHostelId().equals(hostelId));

            if (!isOwner) {
                logger.error("User {} does not own hostel {}", ownerId, hostelId);
                response.put("success", false);
                response.put("message", "Bạn không có quyền truy cập khu trọ này!");
                return ResponseEntity.status(403).body(response);
            }

            // Lấy danh sách phòng
            List<ContractDto.Room> rooms = roomsService.getRoomsByHostelId(hostelId);

            if (rooms.isEmpty()) {
                logger.warn("No rooms found for hostelId: {}", hostelId);
                response.put("success", true);
                response.put("rooms", new ArrayList<>());
                response.put("message", "Không có phòng nào trong khu trọ này.");
            } else {
                logger.info("Found {} rooms for hostelId: {}", rooms.size(), hostelId);

                // Chỉ lấy những phòng trống (AVAILABLE)
                List<ContractDto.Room> availableRooms = rooms.stream()
                        .filter(room -> "unactive".equals(room.getStatus()))
                        .collect(Collectors.toList());

                response.put("success", true);
                response.put("rooms", availableRooms);
                response.put("totalRooms", rooms.size());
                response.put("availableRooms", availableRooms.size());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting rooms for hostelId {}: {}", hostelId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy danh sách phòng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Cũng có thể thêm endpoint để lấy thông tin chi tiết phòng
    @GetMapping("/get-room-details")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomDetails(
            @RequestParam Integer roomId,
            Authentication authentication) {
        logger.info("Getting room details for roomId: {}", roomId);

        Map<String, Object> response = new HashMap<>();
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            // Kiểm tra quyền sở hữu phòng thông qua khu trọ
            List<Hostel> hostels = hostelService.getHostelsWithRoomsByOwnerId(ownerId);
            Rooms room = hostels.stream()
                    .flatMap(hostel -> hostel.getRooms().stream())
                    .filter(r -> r.getRoomId().equals(roomId))
                    .findFirst()
                    .orElse(null);

            if (room == null) {
                logger.error("Room {} not found or not owned by user {}", roomId, ownerId);
                response.put("success", false);
                response.put("message", "Không tìm thấy phòng hoặc bạn không có quyền truy cập!");
                return ResponseEntity.status(404).body(response);
            }

            Map<String, Object> roomDetails = new HashMap<>();
            roomDetails.put("roomId", room.getRoomId());
            roomDetails.put("roomName", room.getNamerooms());
            roomDetails.put("price", room.getPrice());
            roomDetails.put("acreage", room.getAcreage());
            roomDetails.put("maxTenants", room.getMax_tenants());
            roomDetails.put("status", room.getStatus().name());
            roomDetails.put("description", room.getDescription());

            // Thêm thông tin khu trọ
            roomDetails.put("hostelId", room.getHostel().getHostelId());
            roomDetails.put("hostelName", room.getHostel().getName());

            response.put("success", true);
            response.put("room", roomDetails);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting room details for roomId {}: {}", roomId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin phòng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ResponseEntity<?> createContract(
            @Valid @RequestBody ContractDto contract,
            Authentication authentication) {

        logger.info("=== START CREATE CONTRACT ===");
        logger.info("Authentication: {}", authentication.getName());
        logger.info("Contract data received: {}", contract);
        logger.info("Binding result has errors: false"); // Không có BindingResult với @RequestBody

        Map<String, Object> response = new HashMap<>();

        try {
            // Bỏ validation check với BindingResult vì @RequestBody tự động validate

            // Contract null check
            logger.info("=== CONTRACT DATA CHECK ===");
            if (contract == null) {
                logger.error("Contract is null!");
                throw new IllegalArgumentException("Dữ liệu hợp đồng không được để trống!");
            }
            logger.info("Contract is not null");

            // Room data logging
            logger.info("=== ROOM DATA CHECK ===");
            logger.info("Contract.getRoom(): {}", contract.getRoom());
            if (contract.getRoom() != null) {
                logger.info("Room ID: {}", contract.getRoom().getRoomId());
                logger.info("Room Name: {}", contract.getRoom().getRoomName());
                logger.info("Room Price: {}", contract.getRoom().getPrice());
                logger.info("Room Status: {}", contract.getRoom().getStatus());
            } else {
                logger.error("Room data is null!");
            }

            // Validate room data
            if (contract.getRoom() == null) {
                logger.error("Room information is null - throwing exception");
                throw new IllegalArgumentException("Thông tin phòng không được để trống!");
            }

            Integer roomId = contract.getRoom().getRoomId();
            logger.info("Extracted Room ID: {}", roomId);
            if (roomId == null || roomId <= 0) {
                logger.error("Room ID is null or invalid: {}", roomId);
                throw new IllegalArgumentException("ID phòng không được để trống hoặc không hợp lệ!");
            }
            logger.info("Room ID validation passed: {}", roomId);

            // Contract data validation
            logger.info("=== CONTRACT DATA VALIDATION ===");
            validateContractData(contract);
            logger.info("Contract data validation passed");

            // Date calculation
            logger.info("=== DATE CALCULATION ===");
            if (contract.getTerms().getDuration() != null && contract.getTerms().getDuration() > 0) {
                LocalDate startDate = contract.getTerms().getStartDate();
                LocalDate calculatedEndDate = startDate.plusMonths(contract.getTerms().getDuration());
                logger.info("Start date: {}", startDate);
                logger.info("Duration: {} months", contract.getTerms().getDuration());
                logger.info("Calculated end date: {}", calculatedEndDate);
                contract.getTerms().setEndDate(calculatedEndDate);
            }

            // Owner information
            logger.info("=== OWNER INFORMATION ===");
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();
            logger.info("Owner CCCD from authentication: {}", ownerCccd);

            Users owner = userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null);
            if (owner == null) {
                logger.error("Owner not found with CCCD: {}", ownerCccd);
                throw new IllegalArgumentException("Không tìm thấy thông tin chủ trọ!");
            }
            logger.info("Owner found: ID={}, Name={}", owner.getUserId(), owner.getFullname());

            // Update owner information - SỬA: Không có file upload
            logger.info("=== UPDATE OWNER INFORMATION ===");
            updateOwnerInformation(owner, contract.getOwner());
            logger.info("Owner information updated successfully");

            // Handle tenant information - SỬA: Không có file upload
            logger.info("=== TENANT HANDLING ===");
            Users tenant = null;
            UnregisteredTenants unregisteredTenant = null;
            String tenantPhone;

            logger.info("Tenant type: {}", contract.getTenantType());

            if ("UNREGISTERED".equals(contract.getTenantType())) {
                logger.info("Processing unregistered tenant");
                logger.info("Unregistered tenant data: {}", contract.getUnregisteredTenant());
                unregisteredTenant = handleUnregisteredTenant(contract.getUnregisteredTenant(), owner);
                tenantPhone = contract.getUnregisteredTenant().getPhone();
                logger.info("Unregistered tenant processed: ID={}, Phone={}",
                        unregisteredTenant.getId(), tenantPhone);
            } else {
                logger.info("Processing registered tenant");
                logger.info("Registered tenant data: {}", contract.getTenant());
                tenant = handleRegisteredTenant(contract.getTenant());
                tenantPhone = contract.getTenant().getPhone();
                logger.info("Registered tenant processed: ID={}, Phone={}",
                        tenant.getUserId(), tenantPhone);
            }

            // Validate and get room
            logger.info("=== ROOM VALIDATION ===");
            logger.info("Validating room with ID: {}", roomId);
            Rooms room = validateAndGetRoom(roomId);
            logger.info("Room validation passed: ID={}, Name={}, Status={}",
                    room.getRoomId(), room.getNamerooms(), room.getStatus());

            // Create contract
            logger.info("=== CREATE CONTRACT ===");
            logger.info("Creating contract with parameters:");
            logger.info("- Tenant Phone: {}", tenantPhone);
            logger.info("- Room ID: {}", roomId);
            logger.info("- Contract Date: {}", contract.getContractDate());
            logger.info("- Start Date: {}", contract.getTerms().getStartDate());
            logger.info("- End Date: {}", contract.getTerms().getEndDate());
            logger.info("- Price: {}", contract.getTerms().getPrice());
            logger.info("- Deposit: {}", contract.getTerms().getDeposit());
            logger.info("- Status: {}", contract.getStatus());
            logger.info("- Owner CCCD: {}", ownerCccd);
            logger.info("- Duration: {}", contract.getTerms().getDuration());

            // Convert BigDecimal to Float safely
            Float priceFloat = null;
            Float depositFloat = null;

            if (contract.getTerms().getPrice() != null) {
                priceFloat = contract.getTerms().getPrice().floatValue();
                logger.info("Converted price from {} to {}", contract.getTerms().getPrice(), priceFloat);
            }

            if (contract.getTerms().getDeposit() != null) {
                depositFloat = contract.getTerms().getDeposit().floatValue();
                logger.info("Converted deposit from {} to {}", contract.getTerms().getDeposit(), depositFloat);
            }

            Contracts savedContract = contractService.createContract(
                    tenantPhone,
                    roomId,
                    Date.valueOf(contract.getContractDate()),
                    Date.valueOf(contract.getTerms().getStartDate()),
                    Date.valueOf(contract.getTerms().getEndDate()),
                    priceFloat,
                    depositFloat,
                    contract.getTerms().getTerms(),
                    Contracts.Status.valueOf(contract.getStatus().toUpperCase()),
                    ownerCccd,
                    tenant,
                    unregisteredTenant,
                    contract.getTerms().getDuration()
            );

            logger.info("Contract created successfully: ID={}", savedContract.getContractId());

            // Update room status
            logger.info("=== UPDATE ROOM STATUS ===");
            logger.info("Updating room status from {} to ACTIVE", room.getStatus());
            room.setStatus(RoomStatus.active);
            roomsService.save(room);
            logger.info("Room status updated successfully");

            // Success response
            logger.info("=== SUCCESS RESPONSE ===");
            response.put("success", true);
            response.put("message", "Hợp đồng đã được tạo thành công!");
            response.put("contractId", savedContract.getContractId());

            logger.info("Contract creation completed successfully. Contract ID: {}", savedContract.getContractId());
            logger.info("=== END CREATE CONTRACT SUCCESS ===");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("=== ILLEGAL ARGUMENT EXCEPTION ===");
            logger.error("Error message: {}", e.getMessage());
            logger.error("Stack trace: ", e);
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Dữ liệu không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            logger.error("=== GENERAL EXCEPTION ===");
            logger.error("Error message: {}", e.getMessage());
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Full stack trace: ", e);
            response.put("success", false);
            response.put("message", "Lỗi khi tạo hợp đồng: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body("Dữ liệu không hợp lệ: " + e.getMessage());
        }
    }

    @PostMapping(value = "/upload-cccd", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ResponseEntity<Map<String, Object>> uploadCccd(
            @RequestParam(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestParam(value = "cccdBack", required = false) MultipartFile cccdBack,
            @RequestParam(value = "cccdNumber") String cccdNumber, // Lấy từ tenant.cccdNumber
            Authentication authentication) {
        logger.info("=== BẮT ĐẦU UPLOAD CCCD ===");
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();
            logger.info("CCCD chủ trọ từ xác thực: {}", ownerCccd);

            Users owner = userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null);
            if (owner == null) {
                logger.error("Không tìm thấy chủ trọ với CCCD: {}", ownerCccd);
                throw new IllegalArgumentException("Không tìm thấy thông tin chủ trọ!");
            }

            if (!StringUtils.hasText(cccdNumber)) {
                logger.error("Số CCCD không được để trống");
                throw new IllegalArgumentException("Số CCCD không được để trống!");
            }

            // Kiểm tra độ dài và định dạng (12 chữ số)
            if (!cccdNumber.matches("[0-9]{12}")) {
                logger.error("Số CCCD không hợp lệ: {}", cccdNumber);
                throw new IllegalArgumentException("Số CCCD phải là 12 chữ số!");
            }

            // Tìm UserCccd hiện có, nếu không có thì tạo mới và lưu ngay
            UserCccd tenantCccd = userCccdRepository.findByCccdNumber(cccdNumber)
                    .orElseGet(() -> {
                        UserCccd newCccd = new UserCccd();
                        newCccd.setCccdNumber(cccdNumber);
                        newCccd.setUser(null);
                        return userCccdRepository.save(newCccd); // Lưu ngay để lấy ID
                    });

            // Đảm bảo cccdNumber khớp với bản ghi hiện tại
            if (!tenantCccd.getCccdNumber().equals(cccdNumber)) {
                logger.warn("cccdNumber không khớp với bản ghi hiện tại, cập nhật lại");
                tenantCccd.setCccdNumber(cccdNumber);
                tenantCccd = userCccdRepository.saveAndFlush(tenantCccd);
            }

            logger.info("Using UserCccd with ID: {} for cccdNumber: {}", tenantCccd.getId(), cccdNumber);

            String cccdFrontUrl = null;
            String cccdBackUrl = null;

            // Xóa ảnh cũ chỉ khi có ảnh mới được upload
            if (cccdFront != null && !cccdFront.isEmpty()) {
                imageService.deleteImagesByUserCccdAndType(Long.valueOf(tenantCccd.getId()), Image.ImageType.FRONT);
                Image cccdFrontImage = imageService.saveImage(cccdFront, "cccd", tenantCccd, Image.ImageType.FRONT);
                cccdFrontUrl = cccdFrontImage.getUrl();
                logger.info("Lưu ảnh CCCD mặt trước thành công, ID: {}, URL: {}", cccdFrontImage.getId(), cccdFrontUrl);
            }

            if (cccdBack != null && !cccdBack.isEmpty()) {
                imageService.deleteImagesByUserCccdAndType(Long.valueOf(tenantCccd.getId()), Image.ImageType.BACK);
                Image cccdBackImage = imageService.saveImage(cccdBack, "cccd", tenantCccd, Image.ImageType.BACK);
                cccdBackUrl = cccdBackImage.getUrl();
                logger.info("Lưu ảnh CCCD mặt sau thành công, ID: {}, URL: {}", cccdBackImage.getId(), cccdBackUrl);
            }

            // Cập nhật lại tenantCccd để đảm bảo dữ liệu đồng bộ
            tenantCccd = userCccdRepository.saveAndFlush(tenantCccd);

            response.put("success", true);
            response.put("cccdFrontUrl", cccdFrontUrl);
            response.put("cccdBackUrl", cccdBackUrl);
            response.put("cccdId", tenantCccd.getId());
            response.put("message", "Upload ảnh CCCD thành công!");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Lỗi dữ liệu không hợp lệ: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            logger.error("Lỗi khi tải lên ảnh: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi tải lên ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            logger.error("Lỗi khi upload CCCD: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi upload CCCD: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // THÊM METHOD NÀY VÀO CONTROLLER CLASS CỦA BẠN
    private Rooms validateAndGetRoom(Integer roomId) {
        logger.info("=== VALIDATE AND GET ROOM ===");
        logger.info("Searching for room with ID: {}", roomId);

        // Kiểm tra roomId null
        if (roomId == null || roomId <= 0) {
            logger.error("Room ID is null or invalid: {}", roomId);
            throw new IllegalArgumentException("ID phòng không hợp lệ!");
        }

        // Tìm phòng trong database
        Optional<Rooms> roomOptional = roomsService.findById(roomId);
        if (!roomOptional.isPresent()) {
            logger.error("Room not found with ID: {}", roomId);
            throw new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId);
        }

        Rooms room = roomOptional.get();
        logger.info("Room found: ID={}, Name={}, Status={}",
                room.getRoomId(), room.getNamerooms(), room.getStatus());

        // Kiểm tra trạng thái phòng
        if (!RoomStatus.unactive.equals(room.getStatus())) {
            logger.error("Room is not available. Current status: {}", room.getStatus());
            throw new IllegalArgumentException("Phòng đã được thuê hoặc không khả dụng! Trạng thái hiện tại: " + room.getStatus());
        }

        logger.info("Room validation passed - room is available");
        return room;
    }


    // SỬA các method helper - bỏ file upload parameters

    private void updateOwnerInformation(Users owner, ContractDto.Owner ownerDto) {
        logger.info("=== START UPDATE OWNER INFORMATION ===");
        logger.info("Owner Current Details - ID: {}, Name: {}, Phone: {}, Address: {}",
                owner.getUserId(), owner.getFullname(), owner.getPhone(), owner.getAddress());
        logger.info("Incoming Owner DTO: {}", ownerDto);

        boolean updated = false;  // Flag cho toàn bộ (info + CCCD)

        // Cập nhật tên nếu có dữ liệu
        if (StringUtils.hasText(ownerDto.getFullName())) {
            owner.setFullname(ownerDto.getFullName());
            updated = true;
            logger.info("Set fullname: {}", ownerDto.getFullName());
        }

        // Cập nhật số điện thoại nếu có dữ liệu
        if (StringUtils.hasText(ownerDto.getPhone())) {
            owner.setPhone(ownerDto.getPhone());
            updated = true;
            logger.info("Set phone: {}", ownerDto.getPhone());
        }

        // Cập nhật ngày sinh nếu có dữ liệu
        if (ownerDto.getBirthday() != null) {
            java.sql.Date sqlBirthday = new java.sql.Date(ownerDto.getBirthday().getTime());
            owner.setBirthday(sqlBirthday);
            updated = true;
            logger.info("Set birthday: {}", sqlBirthday);
        }

        // Cập nhật địa chỉ: Build nếu có ít nhất một trường
        StringBuilder addressBuilder = new StringBuilder();
        if (StringUtils.hasText(ownerDto.getStreet())) {
            addressBuilder.append(ownerDto.getStreet().trim());
            updated = true;
        }
        if (StringUtils.hasText(ownerDto.getWard())) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(ownerDto.getWard().trim());
            updated = true;
        }
        if (StringUtils.hasText(ownerDto.getDistrict())) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(ownerDto.getDistrict().trim());
            updated = true;
        }
        if (StringUtils.hasText(ownerDto.getProvince())) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(ownerDto.getProvince().trim());
            updated = true;
        }

        // Set địa chỉ nếu builder có nội dung
        if (addressBuilder.length() > 0) {
            String addressString = addressBuilder.toString().trim();
            owner.setAddress(addressString);
            logger.info("Set owner address: {}", addressString);
        }

        // Cập nhật CCCD: Load hoặc tạo mới (nhưng không set updated ở đây)
        UserCccd ownerCccdEntity = userCccdRepository.findByUserId(owner.getUserId())
                .orElseGet(() -> {
                    UserCccd newCccd = new UserCccd();
                    newCccd.setUser(owner);
                    return newCccd;  // Không set updated ở đây nữa
                });

        // Set CCCD fields nếu có dữ liệu, và set flag chỉ khi set
        boolean cccdDataPresent = false;  // Flag riêng để check có data CCCD không
        if (StringUtils.hasText(ownerDto.getCccdNumber())) {
            ownerCccdEntity.setCccdNumber(ownerDto.getCccdNumber());
            updated = true;
            cccdDataPresent = true;
            logger.info("Set CCCD Number: {}", ownerDto.getCccdNumber());
        }
        if (ownerDto.getIssueDate() != null) {
            ownerCccdEntity.setIssueDate(ownerDto.getIssueDate());
            updated = true;
            cccdDataPresent = true;
            logger.info("Set CCCD Issue Date: {}", ownerDto.getIssueDate());
        }
        if (StringUtils.hasText(ownerDto.getIssuePlace())) {
            ownerCccdEntity.setIssuePlace(ownerDto.getIssuePlace());
            updated = true;
            cccdDataPresent = true;
            logger.info("Set CCCD Issue Place: {}", ownerDto.getIssuePlace());
        }

        // Save nếu có thay đổi
        if (updated) {
            try {
                // Chỉ save CCCD nếu tồn tại data hoặc đang cập nhật fields khác (nhưng ưu tiên check cccdDataPresent nếu tạo mới)
                if (cccdDataPresent || userCccdRepository.findByUserId(owner.getUserId()).isPresent()) {
                    userCccdRepository.save(ownerCccdEntity);
                    logger.info("CCCD saved successfully");
                } else {
                    logger.info("Skipping save for new empty CCCD entity.");
                }
                Users savedUser = userService.saveUser(owner);
                logger.info("Owner information saved. User ID: {}", savedUser.getUserId());
            } catch (Exception e) {
                logger.error("Error saving owner information: {}", e.getMessage(), e);
            }
        } else {
            logger.info("No data to update for owner.");
        }
    }


    private UnregisteredTenants handleUnregisteredTenant(ContractDto.UnregisteredTenant tenantDto, Users owner) {
        logger.info("=== HANDLE UNREGISTERED TENANT ===");
        logger.info("Tenant data: {}", tenantDto);
        logger.info("Owner ID: {}", owner.getUserId());

        if (tenantDto.getPhone() == null || tenantDto.getPhone().trim().isEmpty()) {
            logger.error("Tenant phone is null or empty");
            throw new IllegalArgumentException("Số điện thoại người thuê không được để trống!");
        }
        logger.info("Tenant phone: {}", tenantDto.getPhone());

        UnregisteredTenants unregisteredTenant = new UnregisteredTenants();
        unregisteredTenant.setUser(owner);
        unregisteredTenant.setFullName(tenantDto.getFullName());
        unregisteredTenant.setPhone(tenantDto.getPhone());
        unregisteredTenant.setCccdNumber(tenantDto.getCccdNumber());
        unregisteredTenant.setIssueDate(tenantDto.getIssueDate());
        unregisteredTenant.setIssuePlace(tenantDto.getIssuePlace());
        unregisteredTenant.setBirthday(tenantDto.getBirthday());

        logger.info("Unregistered tenant basic info set: Name={}, Phone={}, CCCD={}",
                tenantDto.getFullName(), tenantDto.getPhone(), tenantDto.getCccdNumber());

        // Tạo chuỗi địa chỉ từ dữ liệu DTO
        StringBuilder newAddress = new StringBuilder();
        boolean hasNewAddressData = false;

        if (StringUtils.hasText(tenantDto.getStreet())) {
            newAddress.append(tenantDto.getStreet());
            hasNewAddressData = true;
        }
        if (StringUtils.hasText(tenantDto.getWard())) {
            newAddress.append(", ").append(tenantDto.getWard());
            hasNewAddressData = true;
        }
        if (StringUtils.hasText(tenantDto.getDistrict())) {
            newAddress.append(", ").append(tenantDto.getDistrict());
            hasNewAddressData = true;
        }
        if (StringUtils.hasText(tenantDto.getProvince())) {
            newAddress.append(", ").append(tenantDto.getProvince());
            hasNewAddressData = true;
        }

        if (hasNewAddressData) {
            unregisteredTenant.setAddress(newAddress.toString());
            logger.info("Tenant address set: {}", newAddress.toString());
        } else {
            logger.warn("No address data provided for unregistered tenant");
            unregisteredTenant.setAddress(null); // Hoặc gán giá trị mặc định nếu cần
        }

        unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);

        UnregisteredTenants saved = unregisteredTenantsRepository.save(unregisteredTenant);
        logger.info("Unregistered tenant saved with ID: {}", saved.getId());
        return saved;
    }

    private Users handleRegisteredTenant(ContractDto.Tenant tenantDto) {
        logger.info("=== XỬ LÝ KHÁCH THUÊ ĐÃ ĐĂNG KÝ ===");
        logger.info("Dữ liệu tenant: {}", tenantDto);

        // 1. Kiểm tra số điện thoại
        if (tenantDto.getPhone() == null || tenantDto.getPhone().trim().isEmpty()) {
            logger.error("Số điện thoại người thuê là null hoặc rỗng");
            throw new IllegalArgumentException("Số điện thoại người thuê không được để trống!");
        }
        logger.info("Tìm khách thuê với số điện thoại: {}", tenantDto.getPhone());

        // 2. Tìm tenant
        Optional<Users> tenantUser = userRepository.findByPhone(tenantDto.getPhone());
        if (!tenantUser.isPresent()) {
            logger.error("Không tìm thấy khách thuê với số điện thoại: {}", tenantDto.getPhone());
            throw new IllegalArgumentException("Không tìm thấy người thuê với số điện thoại: " + tenantDto.getPhone());
        }

        Users tenant = tenantUser.get();
        logger.info("Tìm thấy khách thuê: ID={}, Tên={}, Địa chỉ hiện tại={}",
                tenant.getUserId(), tenant.getFullname(), tenant.getAddress());

        boolean updated = false; // Flag để kiểm tra có cần lưu tenant không

        // 3. Cập nhật thông tin tenant
        if (StringUtils.hasText(tenantDto.getFullName())) {
            tenant.setFullname(tenantDto.getFullName());
            updated = true;
            logger.info("Cập nhật tên khách thuê: {}", tenantDto.getFullName());
        }

        if (tenantDto.getBirthday() != null) {
            tenant.setBirthday(new java.sql.Date(tenantDto.getBirthday().getTime()));
            updated = true;
            logger.info("Cập nhật ngày sinh khách thuê: {}", tenantDto.getBirthday());
        }

        // 4. Cập nhật địa chỉ
        StringBuilder newAddress = new StringBuilder();
        boolean hasAddressData = false;
        if (StringUtils.hasText(tenantDto.getStreet())) {
            newAddress.append(tenantDto.getStreet().trim());
            hasAddressData = true;
            updated = true;
        }
        if (StringUtils.hasText(tenantDto.getWard())) {
            if (newAddress.length() > 0) newAddress.append(", ");
            newAddress.append(tenantDto.getWard().trim());
            hasAddressData = true;
            updated = true;
        }
        if (StringUtils.hasText(tenantDto.getDistrict())) {
            if (newAddress.length() > 0) newAddress.append(", ");
            newAddress.append(tenantDto.getDistrict().trim());
            hasAddressData = true;
            updated = true;
        }
        if (StringUtils.hasText(tenantDto.getProvince())) {
            if (newAddress.length() > 0) newAddress.append(", ");
            newAddress.append(tenantDto.getProvince().trim());
            hasAddressData = true;
            updated = true;
        }

        if (hasAddressData) {
            String addressString = newAddress.toString().trim();
            logger.info("Địa chỉ cũ: {}, Địa chỉ mới: {}", tenant.getAddress(), addressString);
            tenant.setAddress(addressString);
        }

        // 5. Cập nhật thông tin CCCD
        UserCccd tenantCccd = null;
        if (StringUtils.hasText(tenantDto.getCccdNumber())) {
            // Kiểm tra xem CCCD đã tồn tại chưa
            Optional<UserCccd> existingCccd = userCccdRepository.findByCccdNumber(tenantDto.getCccdNumber());
            if (existingCccd.isPresent()) {
                tenantCccd = existingCccd.get();
                logger.info("Tìm thấy CCCD hiện có: {}", tenantDto.getCccdNumber());
            } else {
                tenantCccd = userCccdRepository.findByUserId(tenant.getUserId())
                        .orElseGet(() -> {
                            UserCccd newCccd = new UserCccd();
                            newCccd.setUser(tenant);
                            return newCccd;
                        });
            }

            boolean cccdDataPresent = false;
            if (StringUtils.hasText(tenantDto.getCccdNumber())) {
                tenantCccd.setCccdNumber(tenantDto.getCccdNumber());
                updated = true;
                cccdDataPresent = true;
                logger.info("Cập nhật số CCCD: {}", tenantDto.getCccdNumber());
            }
            if (tenantDto.getIssueDate() != null) {
                tenantCccd.setIssueDate(tenantDto.getIssueDate());
                updated = true;
                cccdDataPresent = true;
                logger.info("Cập nhật ngày cấp CCCD: {}", tenantDto.getIssueDate());
            }
            if (StringUtils.hasText(tenantDto.getIssuePlace())) {
                tenantCccd.setIssuePlace(tenantDto.getIssuePlace());
                updated = true;
                cccdDataPresent = true;
                logger.info("Cập nhật nơi cấp CCCD: {}", tenantDto.getIssuePlace());
            }

            // 6. Xử lý ảnh CCCD từ URL (vì endpoint /api/contracts dùng @RequestBody)
            if (StringUtils.hasText(tenantDto.getCccdFrontUrl())) {
                List<Image> existingImages = imageService.findByUserCccdId(Long.valueOf(tenantCccd.getId()));
                boolean frontExists = existingImages != null && existingImages.stream()
                        .anyMatch(img -> img.getType() == Image.ImageType.FRONT);
                if (!frontExists) {
                    Image cccdFrontImage = Image.builder()
                            .url(tenantDto.getCccdFrontUrl())
                            .userCccd(tenantCccd)
                            .type(Image.ImageType.FRONT)
                            .build();
                    imageService.saveImage(cccdFrontImage);
                    logger.info("Lưu ảnh CCCD mặt trước từ URL thành công: URL={}", tenantDto.getCccdFrontUrl());
                    updated = true;
                } else {
                    logger.info("Ảnh CCCD mặt trước đã tồn tại cho userCccdId: {}", tenantCccd.getId());
                }
            }

            if (StringUtils.hasText(tenantDto.getCccdBackUrl())) {
                List<Image> existingImages = imageService.findByUserCccdId(Long.valueOf(tenantCccd.getId()));
                boolean backExists = existingImages != null && existingImages.stream()
                        .anyMatch(img -> img.getType() == Image.ImageType.BACK);
                if (!backExists) {
                    Image cccdBackImage = Image.builder()
                            .url(tenantDto.getCccdBackUrl())
                            .userCccd(tenantCccd)
                            .type(Image.ImageType.BACK)
                            .build();
                    imageService.saveImage(cccdBackImage);
                    logger.info("Lưu ảnh CCCD mặt sau từ URL thành công: URL={}", tenantDto.getCccdBackUrl());
                    updated = true;
                } else {
                    logger.info("Ảnh CCCD mặt sau đã tồn tại cho userCccdId: {}", tenantCccd.getId());
                }
            }

            // Lưu tenantCccd nếu có dữ liệu CCCD
            if (cccdDataPresent) {
                try {
                    userCccdRepository.saveAndFlush(tenantCccd);
                    logger.info("Lưu thông tin CCCD thành công");
                } catch (DataIntegrityViolationException e) {
                    logger.error("Lỗi trùng lặp CCCD: {}", tenantDto.getCccdNumber(), e);
                    throw new IllegalArgumentException("Số CCCD đã tồn tại: " + tenantDto.getCccdNumber());
                }
            } else {
                logger.info("Không có dữ liệu CCCD mới để lưu.");
            }
        } else {
            logger.info("Không có số CCCD, bỏ qua lưu UserCccd.");
        }

        // 7. Lưu tenant nếu có thay đổi
        if (updated) {
            try {
                logger.info("Trước khi lưu: Địa chỉ tenant = {}", tenant.getAddress());
                Users savedTenant = userService.saveUser(tenant);
                logger.info("Lưu tenant thành công: ID={}", savedTenant.getUserId());
                logger.info("Sau khi lưu: Địa chỉ tenant trong DB = {}", savedTenant.getAddress());
            } catch (Exception e) {
                logger.error("Lỗi khi lưu thông tin tenant: {}", e.getMessage(), e);
                throw new IllegalStateException("Lỗi khi lưu thông tin tenant: " + e.getMessage());
            }
        } else {
            logger.info("Không có dữ liệu để cập nhật cho tenant. Bỏ qua lưu.");
        }

        return tenant;
    }


    @PostMapping(value = "/update-cccd-image", consumes = {"multipart/form-data"})
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateCccdImage(
            @RequestParam(value = "cccdNumber") String cccdNumber,
            @RequestParam(value = "cccdFront", required = false) MultipartFile cccdFront,
            @RequestParam(value = "cccdBack", required = false) MultipartFile cccdBack,
            Authentication authentication) {

        logger.info("=== BẮT ĐẦU CẬP NHẬT ẢNH CCCD ===");
        logger.info("CCCD Number: {}", cccdNumber);
        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra quyền chủ trọ
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();
            logger.info("CCCD chủ trọ từ xác thực: {}", ownerCccd);

            Users owner = userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null);
            if (owner == null) {
                logger.error("Không tìm thấy chủ trọ với CCCD: {}", ownerCccd);
                throw new IllegalArgumentException("Không tìm thấy thông tin chủ trọ!");
            }

            // Kiểm tra số CCCD
            if (!StringUtils.hasText(cccdNumber)) {
                logger.error("Số CCCD không được để trống");
                throw new IllegalArgumentException("Số CCCD không được để trống!");
            }

            // Tìm UserCccd
            UserCccd tenantCccd = userCccdRepository.findByCccdNumber(cccdNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CCCD: " + cccdNumber));
            logger.info("Tìm thấy UserCccd, ID: {}", tenantCccd.getId());

            String cccdFrontUrl = null;
            String cccdBackUrl = null;

            // Xử lý ảnh mặt trước
            if (cccdFront != null && !cccdFront.isEmpty()) {
                // Xóa ảnh mặt trước cũ
                imageService.deleteImagesByUserCccdAndType(Long.valueOf(tenantCccd.getId()), Image.ImageType.FRONT);
                // Lưu ảnh mặt trước mới
                Image cccdFrontImage = imageService.saveImage(cccdFront, "cccd", tenantCccd, Image.ImageType.FRONT);
                cccdFrontUrl = cccdFrontImage.getUrl();
                logger.info("Cập nhật ảnh CCCD mặt trước thành công, ID: {}, URL: {}", cccdFrontImage.getId(), cccdFrontUrl);
            }

            // Xử lý ảnh mặt sau
            if (cccdBack != null && !cccdBack.isEmpty()) {
                // Xóa ảnh mặt sau cũ
                imageService.deleteImagesByUserCccdAndType(Long.valueOf(tenantCccd.getId()), Image.ImageType.BACK);
                // Lưu ảnh mặt sau mới
                Image cccdBackImage = imageService.saveImage(cccdBack, "cccd", tenantCccd, Image.ImageType.BACK);
                cccdBackUrl = cccdBackImage.getUrl();
                logger.info("Cập nhật ảnh CCCD mặt sau thành công, ID: {}, URL: {}", cccdBackImage.getId(), cccdBackUrl);
            }

            response.put("success", true);
            response.put("cccdFrontUrl", cccdFrontUrl);
            response.put("cccdBackUrl", cccdBackUrl);
            response.put("cccdId", tenantCccd.getId());
            response.put("message", "Cập nhật ảnh CCCD thành công!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Lỗi dữ liệu không hợp lệ: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (IOException e) {
            logger.error("Lỗi khi tải lên ảnh: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi tải lên ảnh: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (Exception e) {
            logger.error("Lỗi khi cập nhật ảnh CCCD: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật ảnh CCCD: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    // THÊM METHOD NÀY VÀO CONTROLLER CLASS CỦA BẠN
    private void validateContractData(ContractDto contract) {
        logger.info("=== VALIDATE CONTRACT DATA ===");
        List<String> errors = new ArrayList<>();

        // Validate contract date
        logger.info("Validating contract date: {}", contract.getContractDate());
        if (contract.getContractDate() == null) {
            logger.error("Contract date is null");
            errors.add("Ngày lập hợp đồng không được để trống!");
        }

        // Validate terms
        logger.info("Validating contract terms: {}", contract.getTerms());
        if (contract.getTerms() == null) {
            logger.error("Contract terms is null");
            errors.add("Điều khoản hợp đồng không được để trống!");
        } else {
            logger.info("Terms start date: {}", contract.getTerms().getStartDate());
            logger.info("Terms duration: {}", contract.getTerms().getDuration());
            logger.info("Terms price: {}", contract.getTerms().getPrice());
            logger.info("Terms deposit: {}", contract.getTerms().getDeposit());

            if (contract.getTerms().getStartDate() == null) {
                logger.error("Start date is null");
                errors.add("Ngày bắt đầu hợp đồng không được để trống!");
            }
            if (contract.getTerms().getDuration() == null || contract.getTerms().getDuration() <= 0) {
                logger.error("Duration is invalid: {}", contract.getTerms().getDuration());
                errors.add("Thời hạn hợp đồng phải lớn hơn 0!");
            }

            // Validation cho Double - CÁCH CHUYỂN ĐỔI
            if (contract.getTerms().getPrice() == null ||
                    contract.getTerms().getPrice() <= 0.0) {
                logger.error("Price is invalid: {}", contract.getTerms().getPrice());
                errors.add("Giá thuê phải lớn hơn 0!");
            }

            if (contract.getTerms().getDeposit() == null ||
                    contract.getTerms().getDeposit() < 0.0) {
                logger.error("Deposit is invalid: {}", contract.getTerms().getDeposit());
                errors.add("Tiền cọc phải lớn hơn hoặc bằng 0!");
            }
            if (contract.getTerms().getEndDate() == null) {
                logger.error("End date is null");
                errors.add("Ngày kết thúc hợp đồng không được để trống!");
            } else if (contract.getTerms().getEndDate().isBefore(contract.getTerms().getStartDate())) {
                logger.error("End date {} is before start date {}", contract.getTerms().getEndDate(), contract.getTerms().getStartDate());
                errors.add("Ngày kết thúc hợp đồng phải sau ngày bắt đầu!");
            }
        }

        if (!errors.isEmpty()) {
            logger.error("Validation failed with {} errors: {}", errors.size(), errors);
            throw new IllegalArgumentException(String.join(" ", errors));
        }
        logger.info("Contract data validation completed successfully");
    }


    @GetMapping("/calculate-end-date")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> calculateEndDate(
            @RequestParam String startDate,
            @RequestParam Integer duration) {
        logger.info("Calculating end date for start date: {} and duration: {}", startDate, duration);

        Map<String, Object> response = new HashMap<>();
        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate endDate = start.plusMonths(duration);

            response.put("success", true);
            response.put("endDate", endDate.toString());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error calculating end date: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Lỗi khi tính ngày kết thúc: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/get-tenant-by-phone")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getTenantByPhone(@RequestParam String phone, Model model) {
        logger.info("🔍 === BẮT ĐẦU TÌM NGƯỜI THUÊ BẰNG SỐ ĐIỆN THOẠI ===");
        logger.info("📱 Số điện thoại tìm kiếm: {}", phone);

        Map<String, Object> response = new HashMap<>();
        try {
            logger.info("🔎 Đang tìm kiếm người dùng với số điện thoại: {}", phone);
            Optional<Users> tenantUser = userRepository.findByPhone(phone);

            if (tenantUser.isPresent()) {
                logger.info("✅ Tìm thấy người dùng với số điện thoại: {}", phone);

                Users user = tenantUser.get();
                logger.info("👤 Thông tin người dùng:");
                logger.info("   - ID: {}", user.getUserId());
                logger.info("   - Tên: {}", user.getFullname());
                logger.info("   - Email: {}", user.getEmail());

                logger.info("🆔 Đang tìm thông tin CCCD cho người dùng");
                UserCccd tenantCccd = userService.findUserCccdByUserId(user.getUserId());

                Map<String, Object> tenantData = new HashMap<>();
                tenantData.put("fullName", user.getFullname());
                tenantData.put("phone", user.getPhone());
                tenantData.put("email", user.getEmail() != null ? user.getEmail() : "");
                tenantData.put("birthday", user.getBirthday() != null ? user.getBirthday().toString() : null);

                // Xử lý thông tin CCCD
                if (tenantCccd != null) {
                    logger.info("🆔 Thông tin CCCD:");
                    logger.info("   - Số CCCD: {}", tenantCccd.getCccdNumber());
                    logger.info("   - Ngày cấp: {}", tenantCccd.getIssueDate());
                    logger.info("   - Nơi cấp: {}", tenantCccd.getIssuePlace());
                    tenantData.put("cccdNumber", tenantCccd.getCccdNumber());
                    tenantData.put("issueDate", tenantCccd.getIssueDate() != null ? tenantCccd.getIssueDate().toString() : null);
                    tenantData.put("issuePlace", tenantCccd.getIssuePlace() != null ? tenantCccd.getIssuePlace() : "");
                } else {
                    logger.warn("⚠️ Không tìm thấy thông tin CCCD cho người dùng");
                    tenantData.put("cccdNumber", "");
                    tenantData.put("issueDate", null);
                    tenantData.put("issuePlace", "");
                }

                // Xử lý địa chỉ
                logger.info("🏠 Địa chỉ người dùng: {}", user.getAddress());
                tenantData.put("street", "");
                tenantData.put("ward", "");
                tenantData.put("district", "");
                tenantData.put("province", "");
                tenantData.put("address", user.getAddress() != null ? user.getAddress() : "");

                if (StringUtils.hasText(user.getAddress())) {
                    String[] addressParts = user.getAddress().split(",\\s*");
                    logger.info("📍 Chi tiết địa chỉ:");
                    logger.info("   - Số thành phần địa chỉ: {}", addressParts.length);

                    if (addressParts.length >= 1) {
                        logger.info("   - Đường/Số nhà: {}", addressParts[0]);
                        tenantData.put("street", addressParts[0].trim());
                    }
                    if (addressParts.length >= 2) {
                        logger.info("   - Phường/Xã: {}", addressParts[1]);
                        tenantData.put("ward", addressParts[1].trim());
                    }
                    if (addressParts.length >= 3) {
                        logger.info("   - Quận/Huyện: {}", addressParts[2]);
                        tenantData.put("district", addressParts[2].trim());
                    }
                    if (addressParts.length >= 4) {
                        logger.info("   - Tỉnh/Thành phố: {}", addressParts[3]);
                        tenantData.put("province", addressParts[3].trim());
                    }
                } else {
                    logger.warn("⚠️ Không có thông tin địa chỉ cho người dùng");
                }

                response.put("success", true);
                response.put("tenant", tenantData);

                logger.info("✅ === KẾT QUẢ TÌM KIẾM NGƯỜI THUÊ ===");
                logger.info("📊 Chi tiết người thuê: {}", tenantData);

                return ResponseEntity.ok(response);
            } else {
                logger.warn("❌ KHÔNG TÌM THẤY NGƯỜI DÙNG");
                logger.warn("📱 Số điện thoại: {}", phone);

                response.put("success", false);
                response.put("message", "Không tìm thấy người thuê với số điện thoại: " + phone);

                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("❌ === LỖI KHI TÌM KIẾM NGƯỜI THUÊ ===");
            logger.error("📱 Số điện thoại: {}", phone);
            logger.error("🔥 Chi tiết lỗi: {}", e.getMessage(), e);

            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin người thuê: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    private Map<String, String> parseAddress(String addressString) {
        Map<String, String> addressParts = new HashMap<>();
        if (addressString == null || addressString.trim().isEmpty()) {
            addressParts.put("street", "Chưa cập nhật");
            addressParts.put("ward", "");
            addressParts.put("district", "");
            addressParts.put("province", "");
            return addressParts;
        }

        // Loại tiền tố thừa và normalize
        String cleaned = addressString.replaceAll("^(Phòng trọ|Phòng|Phường|Quận)\\s+", "").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");  // Loại khoảng trắng thừa
        String[] parts = cleaned.split("[,\\-]\\s*");  // Split bằng , hoặc -

        // Gán linh hoạt dựa trên số phần
        if (parts.length >= 4) {
            addressParts.put("street", parts[0].trim());
            addressParts.put("ward", parts[1].trim());
            addressParts.put("district", parts[2].trim());
            addressParts.put("province", parts[3].trim());
        } else if (parts.length == 3) {
            addressParts.put("street", "Chưa cập nhật");
            addressParts.put("ward", parts[0].trim());
            addressParts.put("district", parts[1].trim());
            addressParts.put("province", parts[2].trim());
        } else {
            addressParts.put("street", cleaned);
            addressParts.put("ward", "");
            addressParts.put("district", "");
            addressParts.put("province", "");
        }

        // Log để check
        logger.info("Parsed address: " + addressParts);
        return addressParts;
    }

    @PostMapping("/add-unregistered-tenant")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> addUnregisteredTenant(
            @RequestParam("name") String fullName,
            @RequestParam(value = "dob", required = false) String dob,
            @RequestParam(value = "id") String cccdNumber,
            @RequestParam(value = "id-date", required = false) String issueDate,
            @RequestParam(value = "id-place", required = false) String issuePlace,
            @RequestParam("phone") String phone,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "street", required = false) String street,
            @RequestParam(value = "ward", required = false) String ward,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "province", required = false) String province,
            @RequestParam(value = "cccd-front", required = false) MultipartFile cccdFront,
            @RequestParam(value = "cccd-back", required = false) MultipartFile cccdBack,
            Authentication authentication) {
        logger.info("Adding unregistered tenant with phone: {}", phone);

        Map<String, Object> response = new HashMap<>();
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();
            Users owner = userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null);
            if (owner == null) {
                logger.error("Owner not found for CCCD: {}", ownerCccd);
                response.put("success", false);
                response.put("message", "Không tìm thấy chủ trọ với CCCD: " + ownerCccd);
                return ResponseEntity.badRequest().body(response);
            }

            if (fullName == null || fullName.trim().isEmpty()) {
                logger.error("Full name is empty");
                response.put("success", false);
                response.put("message", "Họ và tên không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }
            if (phone == null || phone.trim().isEmpty()) {
                logger.error("Phone is empty");
                response.put("success", false);
                response.put("message", "Số điện thoại không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }
            if (cccdNumber == null || cccdNumber.trim().isEmpty()) {
                logger.error("CCCD number is empty");
                response.put("success", false);
                response.put("message", "Số CCCD không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            UnregisteredTenants unregisteredTenant = new UnregisteredTenants();
            unregisteredTenant.setUser(owner);
            unregisteredTenant.setFullName(fullName);
            unregisteredTenant.setPhone(phone);
            unregisteredTenant.setCccdNumber(cccdNumber);
            unregisteredTenant.setIssueDate(issueDate != null && !issueDate.isEmpty() ? Date.valueOf(issueDate) : null);
            unregisteredTenant.setIssuePlace(issuePlace);
            unregisteredTenant.setBirthday(dob != null && !dob.isEmpty() ? Date.valueOf(dob) : null);
            unregisteredTenant.setCccdFrontUrl(cccdFront != null && !cccdFront.isEmpty() ? saveFile(cccdFront) : null);
            unregisteredTenant.setCccdBackUrl(cccdBack != null && !cccdBack.isEmpty() ? saveFile(cccdBack) : null);
            unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);

            Address address = new Address();
            address.setStreet(street);
//            unregisteredTenant.setAddress(address);

            unregisteredTenantsRepository.save(unregisteredTenant);

            Map<String, Object> tenantData = new HashMap<>();
            tenantData.put("fullName", unregisteredTenant.getFullName());
            tenantData.put("phone", unregisteredTenant.getPhone());
            tenantData.put("cccdNumber", unregisteredTenant.getCccdNumber());
            tenantData.put("birthday", unregisteredTenant.getBirthday() != null ? unregisteredTenant.getBirthday().toString() : null);
            tenantData.put("issueDate", unregisteredTenant.getIssueDate() != null ? unregisteredTenant.getIssueDate().toString() : null);
            tenantData.put("issuePlace", unregisteredTenant.getIssuePlace());
            tenantData.put("cccdFrontUrl", unregisteredTenant.getCccdFrontUrl());
            tenantData.put("cccdBackUrl", unregisteredTenant.getCccdBackUrl());
//            tenantData.put("street", unregisteredTenant.getAddress() != null ? unregisteredTenant.getAddress().getStreet() : null);

            response.put("success", true);
            response.put("tenant", tenantData);
            logger.info("Unregistered tenant added successfully: {}", tenantData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error adding unregistered tenant: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi thêm người thuê chưa đăng ký: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/debug-form")
    @PreAuthorize("hasRole('OWNER')")
    public String debugForm(
            @ModelAttribute("contract") ContractDto contract,
            BindingResult result,
            Model model) {
        logger.info("Debugging form data: {}", contract);
        logger.info("Contract ID: {}, Contract Date: {}, Status: {}, Tenant Type: {}",
                contract.getId(), contract.getContractDate(), contract.getStatus(), contract.getTenantType());
        logger.info("Owner: {}, Owner Phone: {}, Owner CCCD: {}",
                contract.getOwner().getFullName(), contract.getOwner().getPhone(), contract.getOwner().getCccdNumber());
        if ("UNREGISTERED".equals(contract.getTenantType())) {
            logger.info("Unregistered Tenant: {}, Tenant Phone: {}, Tenant CCCD: {}",
                    contract.getUnregisteredTenant().getFullName(), contract.getUnregisteredTenant().getPhone(),
                    contract.getUnregisteredTenant().getCccdNumber());
        } else {
            logger.info("Tenant: {}, Tenant Phone: {}, Tenant CCCD: {}",
                    contract.getTenant().getFullName(), contract.getTenant().getPhone(), contract.getTenant().getCccdNumber());
        }

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

    @PutMapping("/update/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateContract(
            @PathVariable Integer contractId,
            @RequestBody ContractDto contractDto,
            Authentication authentication
    ) {
        logger.info("=== START UPDATE CONTRACT ===");
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();
            logger.info("Authenticated user ID: {}", ownerId);

            // Tìm contract hiện tại
            Optional<Contracts> contractOptional = contractService.findContractById(contractId);
            if (!contractOptional.isPresent()) {
                logger.error("Contract {} not found", contractId);
                response.put("success", false);
                response.put("message", "Hợp đồng không tồn tại!");
                return ResponseEntity.status(404).body(response);
            }

            Contracts contract = contractOptional.get();
            if (!contract.getOwner().getUserId().equals(ownerId)) {
                logger.error("User {} does not own contract {}", ownerId, contractId);
                response.put("success", false);
                response.put("message", "Bạn không có quyền cập nhật hợp đồng này!");
                return ResponseEntity.status(403).body(response);
            }

            // Validate room (nếu có trong DTO)
            if (contractDto.getRoom() != null && contractDto.getRoom().getRoomId() != null) {
                logger.info("=== VALIDATE AND GET ROOM ===");
                logger.info("Searching for room with ID: {}", contractDto.getRoom().getRoomId());
                Optional<Rooms> room = roomsService.findById(contractDto.getRoom().getRoomId());
                if (room.isEmpty()) {
                    logger.error("Room not found: {}", contractDto.getRoom().getRoomId());
                    response.put("success", false);
                    response.put("message", "Phòng không tồn tại!");
                    return ResponseEntity.status(404).body(response);
                }
                logger.info("Room found: ID={}, Name={}, Status={}", room.get().getRoomId(), room.get().getNamerooms(), room.get().getStatus());
                // Chỉ kiểm tra trạng thái nếu roomId thay đổi
                if (!room.get().getRoomId().equals(contract.getRoom().getRoomId()) && !room.get().getStatus().equals(RoomStatus.unactive)) {
                    logger.error("Room is not available. Current status: {}", room.get().getStatus());
                    response.put("success", false);
                    response.put("message", "Phòng đã được thuê hoặc không khả dụng! Trạng thái hiện tại: " + room.get().getStatus());
                    return ResponseEntity.badRequest().body(response);
                }
            }

            // Cập nhật hợp đồng
            Contracts updatedContract = contractService.updateContract(contractId, contractDto);
            response.put("success", true);
            response.put("message", "Cập nhật hợp đồng thành công!");
            response.put("contractId", updatedContract.getContractId());
            logger.info("Contract updated successfully: ID={}", updatedContract.getContractId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid data: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error updating contract: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật hợp đồng: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    @PutMapping("/update-owner")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<?> updateOwner(
            @RequestBody ContractDto.Owner ownerDto,
            Authentication authentication) {
        logger.info("=== START UPDATE OWNER ===");
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();
            String ownerCccd = userDetails.getCccd();

            Users owner = userRepository.findById(ownerId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chủ trọ!"));

            // Cập nhật thông tin owner sử dụng hàm updateOwnerInformation đã có
            updateOwnerInformation(owner, ownerDto);

            response.put("success", true);
            response.put("message", "Thông tin chủ trọ đã được cập nhật thành công!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid data: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error updating owner: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật thông tin chủ trọ: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
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

    @PutMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('OWNER')")
    @Transactional
    public ResponseEntity<?> updateRoom(
            @PathVariable Integer roomId,
            @RequestBody Rooms roomDto,
            Authentication authentication) {
        logger.info("=== START UPDATE ROOM ===");
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            Rooms room = roomsService.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phòng với ID: " + roomId));

            // Kiểm tra quyền sở hữu
            if (!room.getHostel().getOwner().getUserId().equals(ownerId)) {
                throw new IllegalArgumentException("Bạn không có quyền cập nhật phòng này!");
            }

            // Cập nhật fields từ DTO
            if (roomDto.getNamerooms() != null) {
                room.setNamerooms(roomDto.getNamerooms());
            }
            if (roomDto.getAcreage() != null) {
                room.setAcreage(roomDto.getAcreage());
            }
            if (roomDto.getPrice() != null) {
                room.setPrice(roomDto.getPrice());
            }
            if (roomDto.getMax_tenants() != null) {
                room.setMax_tenants(roomDto.getMax_tenants());
            }
            if (roomDto.getDescription() != null) {
                room.setDescription(roomDto.getDescription());
            }
            if (roomDto.getAddress() != null) {
                room.setAddress(roomDto.getAddress());
            }
            // Status chỉ update nếu cần, ví dụ từ DTO
            if (roomDto.getStatus() != null) {
                room.setStatus(roomDto.getStatus());
            }

            // Save room
            Rooms updatedRoom = roomsService.save(room);

            response.put("success", true);
            response.put("message", "Thông tin phòng trọ đã được cập nhật thành công!");
            response.put("room", updatedRoom);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Invalid data: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Error updating room: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi cập nhật thông tin phòng trọ: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
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



    /**
     * API endpoint để lấy danh sách hợp đồng cho owner
     */
    @GetMapping("/list")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getContractsListApi(Authentication authentication) {
        logger.info("Getting contracts list API for owner");

        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            List<ContractListDto> contractsList = contractService.getContractsListByOwnerId(ownerId);

            response.put("success", true);
            response.put("contracts", contractsList);
            response.put("totalContracts", contractsList.size());
            response.put("message", "Lấy danh sách hợp đồng thành công");

            logger.info("API: Found {} contracts for owner ID: {}", contractsList.size(), ownerId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting contracts list API: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("contracts", List.of());
            response.put("totalContracts", 0);
            response.put("message", "Lỗi khi lấy danh sách hợp đồng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    /**
     * API endpoint để lấy tất cả hợp đồng cho admin
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllContractsApi() {
        logger.info("Getting all contracts API for admin");

        Map<String, Object> response = new HashMap<>();

        try {
            List<ContractListDto> contractsList = contractService.getAllContractsForList();

            response.put("success", true);
            response.put("contracts", contractsList);
            response.put("totalContracts", contractsList.size());
            response.put("message", "Lấy tất cả hợp đồng thành công");

            logger.info("API: Found {} total contracts", contractsList.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting all contracts API: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("contracts", List.of());
            response.put("totalContracts", 0);
            response.put("message", "Lỗi khi lấy tất cả hợp đồng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @PostMapping("/hop-dong/update-status/{contractId}")
    public ResponseEntity<Map<String, Object>> updateContractStatus(
            @PathVariable Long contractId,
            @RequestBody Map<String, String> request) {

        logger.info("🔄 === BẮT ĐẦU UPDATE CONTRACT STATUS ===");
        logger.info("📝 Contract ID: {}", contractId);
        logger.info("📝 Request body: {}", request);

        Map<String, Object> response = new HashMap<>();

        try {
            String newStatus = request.get("status");
            logger.info("📊 Status từ request: '{}'", newStatus);

            // 🔍 VALIDATE STATUS
            if (newStatus == null || newStatus.trim().isEmpty()) {
                logger.error("❌ Status is null or empty!");
                response.put("success", false);
                response.put("message", "Trạng thái không được để trống");
                response.put("validStatuses", java.util.Arrays.asList("DRAFT", "ACTIVE", "TERMINATED", "EXPIRED"));
                return ResponseEntity.badRequest().body(response);
            }

            // 🔍 KIỂM TRA STATUS HỢP LỆ
            try {
                Contracts.Status.valueOf(newStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.error("❌ Status không hợp lệ: '{}'", newStatus);
                response.put("success", false);
                response.put("message", "Trạng thái không hợp lệ: " + newStatus);
                response.put("validStatuses", java.util.Arrays.asList("DRAFT", "ACTIVE", "TERMINATED", "EXPIRED"));
                return ResponseEntity.badRequest().body(response);
            }

            // 🔄 GỌI SERVICE
            logger.info("🔄 Gọi contractService.updateStatus({}, '{}')", contractId, newStatus);
            contractService.updateStatus(contractId, newStatus);

            // ✅ THÀNH CÔNG
            logger.info("✅ Cập nhật thành công!");
            response.put("success", true);
            response.put("message", "Cập nhật trạng thái hợp đồng thành công");
            response.put("contractId", contractId);
            response.put("newStatus", newStatus.toUpperCase());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("❌ IllegalArgumentException: {}", e.getMessage());
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("validStatuses", java.util.Arrays.asList("DRAFT", "ACTIVE", "TERMINATED", "EXPIRED"));
            return ResponseEntity.badRequest().body(response);

        } catch (Exception e) {
            logger.error("❌ Unexpected Exception: ", e);
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 🧪 DEBUG ENDPOINT
    @GetMapping("/debug/contract/{contractId}")
    public ResponseEntity<Map<String, Object>> debugContract(@PathVariable Long contractId) {
        logger.info("🧪 Debug contract: {}", contractId);

        Map<String, Object> response = new HashMap<>();

        try {
            // Tìm contract
            Optional<Contracts> contractOpt = contractsRepository.findById(Math.toIntExact(contractId));

            if (contractOpt.isPresent()) {
                Contracts contract = contractOpt.get();
                response.put("found", true);
                response.put("contractId", contract.getContractId());
                response.put("currentStatus", contract.getStatus());
                response.put("statusType", contract.getStatus().getClass().getSimpleName());
            } else {
                response.put("found", false);
                response.put("message", "Contract not found");
            }

            response.put("success", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ Debug error: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 🧪 TEST ENDPOINT ĐỂ KIỂM TRA CONTROLLER
    @GetMapping("/test-controller")
    public ResponseEntity<Map<String, Object>> testController() {
        logger.info("🧪 Test controller endpoint được gọi");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Controller hoạt động bình thường!");
        response.put("timestamp", System.currentTimeMillis());
        response.put("contractServiceAvailable", contractService != null);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/details/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getContractDetails(@PathVariable Integer contractId, Authentication authentication) {
        logger.info("Received request to get contract details for ID: {}", contractId);
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            Optional<Contracts> contractOptional = contractService.findContractById(contractId);
            if (!contractOptional.isPresent()) {
                logger.warn("Contract with ID {} not found", contractId);
                response.put("success", false);
                response.put("message", "Không tìm thấy hợp đồng với ID: " + contractId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            Contracts contract = contractOptional.get();
            if (!contract.getOwner().getUserId().equals(ownerId)) {
                logger.error("User {} does not own contract {}", ownerId, contractId);
                response.put("success", false);
                response.put("message", "Bạn không có quyền truy cập hợp đồng này!");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            ContractDto contractDto = convertToContractDto(contract);
            logger.info("Tenant address in DTO: street={}, ward={}, district={}, province={}",
                    contractDto.getTenant() != null ? contractDto.getTenant().getStreet() : "null",
                    contractDto.getTenant() != null ? contractDto.getTenant().getWard() : "null",
                    contractDto.getTenant() != null ? contractDto.getTenant().getDistrict() : "null",
                    contractDto.getTenant() != null ? contractDto.getTenant().getProvince() : "null");

            response.put("success", true);
            response.put("contract", contractDto);
            response.put("message", "Lấy thông tin hợp đồng thành công");
            logger.info("Contract details retrieved successfully for ID: {}", contractId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving contract details for ID {}: {}", contractId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin hợp đồng: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @GetMapping("/edit/{contractId}")
    public String editContractForm(
            @PathVariable Integer contractId,
            Model model,
            Authentication authentication
    ) {
        System.out.println("🔍 === EDIT CONTRACT FORM CALLED ===");
        System.out.println("📝 Contract ID: " + contractId);

        logger.info("Preparing edit form for Contract ID: {}", contractId);

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            Optional<Contracts> contractOptional = contractService.findContractById(contractId);

            if (contractOptional.isPresent()) {
                Contracts contract = contractOptional.get();

                // Kiểm tra quyền sở hữu
                if (!contract.getOwner().getUserId().equals(ownerId)) {
                    logger.error("User {} does not own contract {}", ownerId, contractId);
                    model.addAttribute("error", "Bạn không có quyền chỉnh sửa hợp đồng này!");
                    return "host/hop-dong-host";
                }

                // Chuyển đổi sang DTO
                ContractDto contractDto = convertToContractDto(contract);

                // ✅ QUAN TRỌNG: Load hostels
                List<Hostel> hostels = hostelService.getHostelsWithRoomsByOwnerId(ownerId);
                model.addAttribute("hostels", hostels);
                System.out.println("🏢 Hostels loaded: " + hostels.size());

                // ✅ QUAN TRỌNG: Xác định hostel và room hiện tại
                Integer currentHostelId = null;
                Integer currentRoomId;
                List<ContractDto.Room> allRoomsForEdit = new ArrayList<>();

                if (contractDto.getRoom() != null) {
                    currentRoomId = contractDto.getRoom().getRoomId();

                    // Tìm hostelId từ room
                    for (Hostel hostel : hostels) {
                        for (Rooms room : hostel.getRooms()) {
                            if (room.getRoomId().equals(currentRoomId)) {
                                currentHostelId = hostel.getHostelId();
                                break;
                            }
                        }
                        if (currentHostelId != null) break;
                    }

                    System.out.println("🏠 Current Room ID: " + currentRoomId);
                    System.out.println("🏢 Current Hostel ID: " + currentHostelId);

                    // ✅ Load TẤT CẢ phòng của hostel hiện tại (để edit)
                    if (currentHostelId != null) {
                        List<ContractDto.Room> hostelRooms = roomsService.getRoomsByHostelId(currentHostelId);

                        // Lọc: phòng trống + phòng hiện tại
                        allRoomsForEdit = hostelRooms.stream()
                                .filter(room ->
                                        "unactive".equals(room.getStatus()) ||
                                                room.getRoomId().equals(currentRoomId)
                                )
                                .collect(Collectors.toList());

                        System.out.println("🏠 Available rooms for edit: " + allRoomsForEdit.size());
                        allRoomsForEdit.forEach(room ->
                                System.out.println("  - Room: " + room.getRoomName() +
                                        " (ID: " + room.getRoomId() +
                                        ", Status: " + room.getStatus() + ")")
                        );
                    }
                } else {
                    currentRoomId = null;
                }

                // ✅ TRUYỀN DATA CHO TEMPLATE
                model.addAttribute("rooms", allRoomsForEdit);
                model.addAttribute("contract", contractDto);
                model.addAttribute("isEditMode", true);
                model.addAttribute("contractId", contractId);

                // ✅ QUAN TRỌNG: Truyền thông tin để JS xử lý
                model.addAttribute("currentHostelId", currentHostelId);
                model.addAttribute("currentRoomId", currentRoomId);

                System.out.println("✅ EDIT FORM DATA:");
                System.out.println("   - Contract ID: " + contractId);
                System.out.println("   - Current Hostel ID: " + currentHostelId);
                System.out.println("   - Current Room ID: " + currentRoomId);
                System.out.println("   - Available Rooms: " + allRoomsForEdit.size());
                System.out.println("   - Is Edit Mode: true");

                return "host/hop-dong-host";

            } else {
                System.out.println("❌ CONTRACT NOT FOUND!");
                logger.error("Contract not found with ID: {}", contractId);
                model.addAttribute("error", "Không tìm thấy hợp đồng");
                return "redirect:/chu-tro/DS-hop-dong-host";
            }
        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            logger.error("Error in edit contract form", e);
            model.addAttribute("error", "Lỗi khi tải hợp đồng: " + e.getMessage());
            return "redirect:/chu-tro/DS-hop-dong-host";
        }
    }

    @GetMapping("/edit-data/{contractId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ContractDto> getContractForEdit(@PathVariable Integer contractId, Authentication authentication) {
        logger.info("Preparing edit data for Contract ID: {}", contractId);

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            Optional<Contracts> contractOptional = contractService.findContractById(contractId);
            if (!contractOptional.isPresent()) {
                logger.error("Contract not found with ID: {}", contractId);
                return ResponseEntity.notFound().build();
            }

            Contracts contract = contractOptional.get();
            if (!contract.getOwner().getUserId().equals(ownerId)) {
                logger.error("User {} does not own contract {}", ownerId, contractId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            ContractDto contractDto = convertToContractDto(contract);
            logger.info("Edit data prepared successfully for Contract ID: {}", contractId);
            return ResponseEntity.ok(contractDto);
        } catch (Exception e) {
            logger.error("Error preparing edit data for Contract ID {}: {}", contractId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-hostel/{hostelId}")
    public ResponseEntity<List<ContractDto.Room>> getRoomsByHostel(@PathVariable Long hostelId) {
        try {
            System.out.println("🏢 API: Getting rooms for hostel ID: " + hostelId);

            List<ContractDto.Room> rooms = roomsService.getRoomsByHostelId(Math.toIntExact(hostelId));

            System.out.println("🏠 API: Found " + rooms.size() + " rooms");
            rooms.forEach(room -> {
                System.out.println("   - Room: " + room.getRoomName() +
                        " (ID: " + room.getRoomId() +
                        ", Status: " + room.getStatus() + ")");
            });

            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            System.out.println("❌ API Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/get-rooms-by-hostel-for-edit")
    @PreAuthorize("hasRole('OWNER')")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRoomsByHostelForEdit(
            @RequestParam Integer hostelId,
            @RequestParam(required = false) Integer currentRoomId,
            Authentication authentication) {

        System.out.println("🔍 === GET ROOMS FOR EDIT ===");
        System.out.println("📝 Hostel ID: " + hostelId);
        System.out.println("📝 Current Room ID: " + currentRoomId); // ✅ THÊM LOG

        Map<String, Object> response = new HashMap<>();
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Integer ownerId = userDetails.getUserId();

            // Kiểm tra quyền sở hữu
            List<Hostel> hostels = hostelService.getHostelsWithRoomsByOwnerId(ownerId);
            boolean isOwner = hostels.stream()
                    .anyMatch(hostel -> hostel.getHostelId().equals(hostelId));

            if (!isOwner) {
                response.put("success", false);
                response.put("message", "Bạn không có quyền truy cập khu trọ này!");
                return ResponseEntity.status(403).body(response);
            }

            // ✅ Lấy rooms từ entity
            List<Rooms> roomEntities = roomsService.findByHostelId(hostelId);

            // ✅ Convert sang DTO với địa chỉ đầy đủ
            List<ContractDto.Room> allRooms = roomEntities.stream()
                    .map(room -> convertRoomToDto(room))
                    .collect(Collectors.toList());

            // ✅ Filter phòng available + current room
            List<ContractDto.Room> availableRooms = allRooms.stream()
                    .filter(room -> {
                        String status = room.getStatus();
                        boolean isUnactive = "UNACTIVE".equalsIgnoreCase(status) || "unactive".equals(status);
                        boolean isCurrentRoom = currentRoomId != null && room.getRoomId().equals(currentRoomId);

                        System.out.println("🏠 Room " + room.getRoomName() +
                                " - Status: " + status +
                                " - IsUnactive: " + isUnactive +
                                " - IsCurrentRoom: " + isCurrentRoom);

                        return isUnactive || isCurrentRoom;
                    })
                    .collect(Collectors.toList());

            // ✅ THÊM: Đánh dấu current room
            availableRooms.forEach(room -> {
                if (currentRoomId != null && room.getRoomId().equals(currentRoomId)) {
                    room.setIsCurrent(true); // ✅ THÊM FIELD NÀY
                    System.out.println("✅ Marked current room: " + room.getRoomName());
                }
            });

            response.put("success", true);
            response.put("rooms", availableRooms);
            response.put("currentRoomId", currentRoomId); // ✅ THÊM
            response.put("totalRooms", allRooms.size());
            response.put("availableRooms", availableRooms.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("❌ ERROR: " + e.getMessage());
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private ContractDto.Room convertRoomToDto(Rooms room) {
        ContractDto.Room dto = new ContractDto.Room();
        dto.setRoomId(room.getRoomId());
        dto.setRoomName(room.getNamerooms());
        dto.setArea(room.getAcreage());
        dto.setPrice(room.getPrice());
        dto.setStatus(room.getStatus() != null ? room.getStatus().name() : "UNKNOWN");

        // ✅ SET HOSTEL INFO
        if (room.getHostel() != null) {
            dto.setHostelId(room.getHostel().getHostelId());
            dto.setHostelName(room.getHostel().getName());
        }

        // ✅ LẤY ĐỊA CHỈ TỪ ROOM.ADDRESS (ƯU TIÊN)
        String roomAddress = room.getAddress();

        System.out.println("🏠 Room address: " + roomAddress);

        if (roomAddress != null && !roomAddress.trim().isEmpty()) {
            // ✅ SỬ DỤNG ĐỊA CHỈ TỪ ROOM
            dto.setAddress(roomAddress);

            // ✅ TÁCH ĐỊA CHỈ THÀNH CÁC PHẦN
            String[] addressParts = roomAddress.split(",");

            if (addressParts.length >= 3) {
                dto.setStreet(addressParts[0].trim());
                dto.setWard(addressParts.length > 1 ? addressParts[1].trim() : "");
                dto.setDistrict(addressParts.length > 2 ? addressParts[2].trim() : "");
                dto.setProvince(addressParts.length > 3 ? addressParts[3].trim() : "");

                System.out.println("✅ Parsed address:");
                System.out.println("   - Street: " + dto.getStreet());
                System.out.println("   - Ward: " + dto.getWard());
                System.out.println("   - District: " + dto.getDistrict());
                System.out.println("   - Province: " + dto.getProvince());
            } else {
                // Nếu không tách được, để toàn bộ vào street
                dto.setStreet(roomAddress);
                dto.setWard("");
                dto.setDistrict("");
                dto.setProvince("");
            }
        } else {
            // ✅ NẾU ROOM KHÔNG CÓ ADDRESS, THỬ LẤY TỪ HOSTEL ADDRESS ENTITY
            if (room.getHostel() != null && room.getHostel().getAddress() != null) {
                try {
                    // ✅ LẤY ĐỊA CHỈ TỪ HOSTEL ADDRESS ENTITY
                    Address hostelAddress = room.getHostel().getAddress();

                    // ✅ TẠO FULL ADDRESS TỪ ADDRESS ENTITY
                    StringBuilder addressBuilder = new StringBuilder();

                    if (hostelAddress.getStreet() != null && !hostelAddress.getStreet().trim().isEmpty()) {
                        addressBuilder.append(hostelAddress.getStreet());
                    }

                    if (hostelAddress.getWard() != null && hostelAddress.getWard().getName() != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(hostelAddress.getWard().getName());
                    }

                    // Thêm district và province nếu có
                    // (cần kiểm tra Ward entity có district không)

                    String fullAddress = addressBuilder.toString();
                    if (!fullAddress.isEmpty()) {
                        dto.setAddress(fullAddress);
                        dto.setStreet(hostelAddress.getStreet() != null ? hostelAddress.getStreet() : "");
                        dto.setWard(hostelAddress.getWard() != null && hostelAddress.getWard().getName() != null
                                ? hostelAddress.getWard().getName() : "");
                        dto.setDistrict(""); // Cần kiểm tra Ward có district không
                        dto.setProvince(""); // Cần kiểm tra Ward có province không

                        System.out.println("⚠️ Using hostel address entity: " + fullAddress);
                    } else {
                        dto.setAddress("Địa chỉ chưa cập nhật");
                        dto.setStreet("");
                        dto.setWard("");
                        dto.setDistrict("");
                        dto.setProvince("");
                    }

                } catch (Exception e) {
                    System.out.println("❌ Error getting hostel address: " + e.getMessage());
                    dto.setAddress("Địa chỉ chưa cập nhật");
                    dto.setStreet("");
                    dto.setWard("");
                    dto.setDistrict("");
                    dto.setProvince("");
                }
            } else {
                dto.setAddress("Địa chỉ chưa cập nhật");
                dto.setStreet("");
                dto.setWard("");
                dto.setDistrict("");
                dto.setProvince("");
            }
        }

        dto.setIsCurrent(false);

        System.out.println("🏠 Converted room: " + dto.getRoomName() +
                " - ID: " + dto.getRoomId() +
                " - Address: " + dto.getAddress());

        return dto;
    }


    private ContractDto convertToContractDto(Contracts contract) {
        System.out.println("🔄 Converting contract to DTO - ID: " + contract.getContractId());
        ContractDto dto = new ContractDto();
        dto.setId(Long.valueOf(contract.getContractId()));

        // ✅ Contract basic info
        if (contract.getContractDate() != null) {
            dto.setContractDate(contract.getContractDate().toLocalDate());
        }
        dto.setStatus(String.valueOf(contract.getStatus()));

        // ✅ Map tenant (người thuê đã đăng ký - Users)
        if (contract.getTenant() != null) {
            ContractDto.Tenant tenant = new ContractDto.Tenant();
            Users user = contract.getTenant();

            if (user != null) {
                tenant.setUserId(Long.valueOf(user.getUserId()));
                tenant.setFullName(user.getFullname());
                tenant.setPhone(user.getPhone());
                tenant.setEmail(user.getEmail() != null ? user.getEmail() : "");
                tenant.setBirthday(user.getBirthday());

                // ✅ Lấy và tách địa chỉ từ cột address của Users
                String address = user.getAddress();
                logger.info("Tenant address for userId {}: {}", user.getUserId(), address);
                if (StringUtils.hasText(address)) {
                    Map<String, String> addressParts = parseAddress(address);
                    tenant.setStreet(addressParts.getOrDefault("street", ""));
                    tenant.setWard(addressParts.getOrDefault("ward", ""));
                    tenant.setDistrict(addressParts.getOrDefault("district", ""));
                    tenant.setProvince(addressParts.getOrDefault("province", ""));
                    logger.info("Parsed tenant address: street={}, ward={}, district={}, province={}",
                            addressParts.get("street"), addressParts.get("ward"),
                            addressParts.get("district"), addressParts.get("province"));
                } else {
                    logger.warn("No address found for tenant with userId: {}", user.getUserId());
                    tenant.setStreet("");
                    tenant.setWard("");
                    tenant.setDistrict("");
                    tenant.setProvince("");
                }

                // ✅ Map thông tin CCCD
                UserCccd cccd = user.getUserCccd();
                if (cccd != null) {
                    tenant.setCccdNumber(cccd.getCccdNumber());
                    tenant.setIssueDate(cccd.getIssueDate());
                    tenant.setIssuePlace(cccd.getIssuePlace() != null ? cccd.getIssuePlace() : "");

                    List<Image> images = imageService.findByUserCccdId(Long.valueOf(cccd.getId()));
                    if (images.isEmpty()) {
                        logger.warn("No images found for userCccdId: {}", cccd.getId());
                    } else {
                        logger.info("Found {} images for userCccdId: {}", images.size(), cccd.getId());
                        for (Image image : images) {
                            logger.info("Image ID: {}, Type: {}, URL: {}", image.getId(), image.getType(), image.getUrl());
                            String imageUrl = image.getUrl().startsWith("/uploads/cccd/") ? image.getUrl() : "/uploads/cccd" + image.getUrl().replace("/uploads", "");
                            if (image.getType() == Image.ImageType.FRONT) {
                                tenant.setCccdFrontUrl(imageUrl);
                            } else if (image.getType() == Image.ImageType.BACK) {
                                tenant.setCccdBackUrl(imageUrl);
                            }
                        }
                    }
                } else {
                    logger.warn("No UserCccd found for tenant userId: {}", user.getUserId());
                    tenant.setCccdNumber("");
                    tenant.setIssueDate(null);
                    tenant.setIssuePlace("");
                }

                dto.setTenant(tenant);
                dto.setTenantType("REGISTERED");
                System.out.println("✅ Mapped registered tenant: " + user.getFullname());
            }
        }

        // ✅ Map unregistered tenant
        if (contract.getUnregisteredTenant() != null) {
            ContractDto.UnregisteredTenant unregTenant = new ContractDto.UnregisteredTenant();
            UnregisteredTenants unregUser = contract.getUnregisteredTenant();

            unregTenant.setFullName(unregUser.getFullName());
            unregTenant.setPhone(unregUser.getPhone());
            unregTenant.setCccdNumber(unregUser.getCccdNumber());
            unregTenant.setIssueDate(unregUser.getIssueDate());
            unregTenant.setIssuePlace(unregUser.getIssuePlace());
            unregTenant.setBirthday(unregUser.getBirthday());

            // ✅ Lấy địa chỉ từ cột address của UnregisteredTenants
            String address = unregUser.getAddress();
            logger.info("Address for unregistered tenant with ID {}: {}", unregUser.getId(), address);
            if (StringUtils.hasText(address)) {
                Map<String, String> addressParts = parseAddress(address);
                unregTenant.setStreet(addressParts.getOrDefault("street", ""));
                unregTenant.setWard(addressParts.getOrDefault("ward", ""));
                unregTenant.setDistrict(addressParts.getOrDefault("district", ""));
                unregTenant.setProvince(addressParts.getOrDefault("province", ""));
            } else {
                logger.warn("No address found for unregistered tenant with ID: {}", unregUser.getId());
                unregTenant.setStreet("");
                unregTenant.setWard("");
                unregTenant.setDistrict("");
                unregTenant.setProvince("");
            }

            // ✅ Lấy URL ảnh CCCD
            List<Image> images = imageService.findByUserCccdId(Long.valueOf(unregUser.getId()));
            if (images.isEmpty()) {
                logger.warn("No images found for unregistered tenant userCccdId: {}", unregUser.getId());
            } else {
                logger.info("Found {} images for unregistered tenant userCccdId: {}", images.size(), unregUser.getId());
                for (Image image : images) {
                    logger.info("Image ID: {}, Type: {}, URL: {}", image.getId(), image.getType(), image.getUrl());
                    if (image.getType() == Image.ImageType.FRONT) {
                        unregTenant.setCccdFrontUrl(image.getUrl());
                    } else if (image.getType() == Image.ImageType.BACK) {
                        unregTenant.setCccdBackUrl(image.getUrl());
                    }
                }
            }

            dto.setUnregisteredTenant(unregTenant);
            dto.setTenantType("UNREGISTERED");
            System.out.println("✅ Mapped unregistered tenant: " + unregUser.getFullName());
        }

        // ✅ Map owner (cũng là Users)
        if (contract.getOwner() != null) {
            ContractDto.Owner owner = new ContractDto.Owner();
            Users user = contract.getOwner();

            if (user != null) {
                owner.setUserId(Long.valueOf(user.getUserId()));
                owner.setFullName(user.getFullname());
                owner.setPhone(user.getPhone());
                owner.setEmail(user.getEmail());
                owner.setBirthday(user.getBirthday());
                owner.setBankAccount(user.getBankAccount());

                // ✅ Lấy địa chỉ từ cột address của Users
                String address = user.getAddress();
                logger.info("Address for owner with userId {}: {}", user.getUserId(), address);
                if (StringUtils.hasText(address)) {
                    Map<String, String> addressParts = parseAddress(address);
                    owner.setStreet(addressParts.getOrDefault("street", ""));
                    owner.setWard(addressParts.getOrDefault("ward", ""));
                    owner.setDistrict(addressParts.getOrDefault("district", ""));
                    owner.setProvince(addressParts.getOrDefault("province", ""));
                } else {
                    logger.warn("No address found for owner with userId: {}", user.getUserId());
                    owner.setStreet("");
                    owner.setWard("");
                    owner.setDistrict("");
                    owner.setProvince("");
                }

                // ✅ Map CCCD information
                UserCccd cccd = user.getUserCccd();
                if (cccd != null) {
                    owner.setCccdNumber(cccd.getCccdNumber());
                    owner.setIssueDate(cccd.getIssueDate());
                    owner.setIssuePlace(cccd.getIssuePlace());
                } else {
                    owner.setCccdNumber("");
                    owner.setIssueDate(null);
                    owner.setIssuePlace("");
                }

                dto.setOwner(owner);
                System.out.println("✅ Mapped owner: " + user.getFullname());
            }
        }

        // ✅ Map room - QUAN TRỌNG NHẤT
        if (contract.getRoom() != null) {
            ContractDto.Room room = new ContractDto.Room();
            Rooms roomEntity = contract.getRoom();

            room.setRoomId(roomEntity.getRoomId());
            room.setRoomName(roomEntity.getNamerooms());
            room.setArea(roomEntity.getAcreage());
            room.setPrice(roomEntity.getPrice());
            room.setStatus(roomEntity.getStatus().name());

            // ✅ QUAN TRỌNG: Thêm thông tin khu trọ
            if (roomEntity.getHostel() != null) {
                room.setHostelId(roomEntity.getHostel().getHostelId());
                room.setHostelName(roomEntity.getHostel().getName());
                System.out.println("✅ Room hostel info - ID: " + roomEntity.getHostel().getHostelId() +
                        ", Name: " + roomEntity.getHostel().getName());
            } else {
                System.out.println("⚠️ WARNING: Room has no hostel information!");
            }

            // ✅ THÊM: Lấy địa chỉ phòng từ hostel
            if (roomEntity.getHostel() != null && StringUtils.hasText(String.valueOf(roomEntity.getHostel().getAddress()))) {
                String hostelAddress = String.valueOf(roomEntity.getHostel().getAddress());
                room.setAddress(hostelAddress);

                // Phân tách địa chỉ cho form
                Map<String, String> addressParts = parseAddress(hostelAddress);
                room.setStreet(addressParts.getOrDefault("street", ""));
                room.setWard(addressParts.getOrDefault("ward", ""));
                room.setDistrict(addressParts.getOrDefault("district", ""));
                room.setProvince(addressParts.getOrDefault("province", ""));
                System.out.println("✅ Room address from hostel: " + hostelAddress);
            } else if (StringUtils.hasText(roomEntity.getAddress())) {
                // Fallback: lấy từ room address nếu có
                String roomAddress = roomEntity.getAddress();
                room.setAddress(roomAddress);

                Map<String, String> addressParts = parseAddress(roomAddress);
                room.setStreet(addressParts.getOrDefault("street", ""));
                room.setWard(addressParts.getOrDefault("ward", ""));
                room.setDistrict(addressParts.getOrDefault("district", ""));
                room.setProvince(addressParts.getOrDefault("province", ""));
                System.out.println("✅ Room address from room: " + roomAddress);
            } else {
                System.out.println("⚠️ WARNING: No address found for room!");
                room.setAddress("");
                room.setStreet("");
                room.setWard("");
                room.setDistrict("");
                room.setProvince("");
            }

            dto.setRoom(room);
            System.out.println("✅ Mapped room: " + roomEntity.getNamerooms() +
                    " (ID: " + roomEntity.getRoomId() +
                    ", HostelID: " + (roomEntity.getHostel() != null ? roomEntity.getHostel().getHostelId() : "NULL") + ")");
        }

        // ✅ Map terms - SỬA LẠI ĐỂ ĐẦY ĐỦ THÔNG TIN
        ContractDto.Terms terms = new ContractDto.Terms();

        // Ngày tháng
        if (contract.getStartDate() != null) {
            terms.setStartDate(contract.getStartDate().toLocalDate());
        }
        if (contract.getEndDate() != null) {
            terms.setEndDate(contract.getEndDate().toLocalDate());
        }

        // Giá cả
        terms.setPrice(contract.getPrice() != null ? Double.valueOf(contract.getPrice()) : 0.0);
        terms.setDeposit(contract.getDeposit() != null ? Double.valueOf(contract.getDeposit()) : 0.0);

        // ✅ THÊM: Tính duration từ start và end date
        if (contract.getStartDate() != null && contract.getEndDate() != null) {
            LocalDate startDate = contract.getStartDate().toLocalDate();
            LocalDate endDate = contract.getEndDate().toLocalDate();
            long monthsBetween = ChronoUnit.MONTHS.between(startDate, endDate);
            terms.setDuration((int) monthsBetween);
            System.out.println("✅ Calculated duration: " + monthsBetween + " months");
        }

        // Điều khoản
        terms.setTerms(contract.getTerms());

        dto.setTerms(terms);
        System.out.println("✅ Mapped terms - Price: " + terms.getPrice() + ", Deposit: " + terms.getDeposit());

        System.out.println("✅ Contract DTO conversion completed successfully");
        return dto;
    }


    @PostMapping("/cccd-images")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Map<String, Object>> getCccdImages(@RequestParam String cccdNumber, Authentication authentication) {
        logger.info("=== BẮT ĐẦU LẤY ẢNH CCCD ===");
        logger.info("CCCD Number: {}", cccdNumber);
        Map<String, Object> response = new HashMap<>();

        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();
            logger.info("CCCD chủ trọ từ xác thực: {}", ownerCccd);

            Users owner = userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null);
            if (owner == null) {
                logger.error("Không tìm thấy chủ trọ với CCCD: {}", ownerCccd);
                throw new IllegalArgumentException("Không tìm thấy thông tin chủ trọ!");
            }

            if (!StringUtils.hasText(cccdNumber)) {
                logger.error("Số CCCD không được để trống");
                throw new IllegalArgumentException("Số CCCD không được để trống!");
            }

            UserCccd tenantCccd = userCccdRepository.findByCccdNumber(cccdNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy CCCD: " + cccdNumber));
            logger.info("Tìm thấy UserCccd, ID: {}", tenantCccd.getId());

            String cccdFrontUrl = null;
            String cccdBackUrl = null;

            List<Image> images = imageService.findByUserCccdId(Long.valueOf(tenantCccd.getId()));
            if (images.isEmpty()) {
                logger.warn("No images found for userCccdId: {}", tenantCccd.getId());
            } else {
                logger.info("Found {} images for userCccdId: {}", images.size(), tenantCccd.getId());
                for (Image image : images) {
                    logger.info("Image ID: {}, Type: {}, URL: {}", image.getId(), image.getType(), image.getUrl());
                    if (image.getType() == Image.ImageType.FRONT) {
                        cccdFrontUrl = image.getUrl();
                    } else if (image.getType() == Image.ImageType.BACK) {
                        cccdBackUrl = image.getUrl();
                    }
                }
            }

            response.put("success", true);
            response.put("cccdFrontUrl", cccdFrontUrl);
            response.put("cccdBackUrl", cccdBackUrl);
            response.put("cccdId", tenantCccd.getId());
            response.put("message", "Lấy ảnh CCCD thành công!");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.error("Lỗi dữ liệu không hợp lệ: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "Dữ liệu không hợp lệ: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            logger.error("Lỗi khi lấy ảnh CCCD: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Lỗi khi lấy ảnh CCCD: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
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
}