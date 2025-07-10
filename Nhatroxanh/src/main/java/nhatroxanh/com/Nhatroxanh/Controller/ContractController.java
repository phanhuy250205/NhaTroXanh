package nhatroxanh.com.Nhatroxanh.Controller;

import jakarta.validation.Valid;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UnregisteredTenantsRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.RoomsService;
import nhatroxanh.com.Nhatroxanh.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
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
    private ContractsRepository contractsRepository;

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
            @Valid @ModelAttribute ContractDto contract,  // ← SỬA: @RequestBody thay vì @ModelAttribute
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

        try {
            // Update fullname
            if (StringUtils.hasText(ownerDto.getFullName())) {
                owner.setFullname(ownerDto.getFullName());
                logger.info("Updated fullname: {}", ownerDto.getFullName());
            } else {
                logger.warn("New fullname is empty or null");
            }

            // Update phone
            if (StringUtils.hasText(ownerDto.getPhone())) {
                owner.setPhone(ownerDto.getPhone());
                logger.info("Updated phone: {}", ownerDto.getPhone());
            } else {
                logger.warn("New phone number is empty or null");
            }

            // Update birthday
            if (ownerDto.getBirthday() != null) {
                java.sql.Date sqlBirthday = new java.sql.Date(ownerDto.getBirthday().getTime());
                owner.setBirthday(sqlBirthday);
                logger.info("Updated birthday: {}", sqlBirthday);
            } else {
                logger.warn("Birthday is null, skipping update");
            }

            // Update owner CCCD
            logger.info("Processing CCCD Information");
            UserCccd ownerCccdEntity = userCccdRepository.findByUserId(owner.getUserId())
                    .orElseGet(() -> {
                        logger.info("No existing CCCD found. Creating new CCCD entity for owner");
                        UserCccd newCccd = new UserCccd();
                        newCccd.setUser(owner);
                        return newCccd;
                    });

            if (StringUtils.hasText(ownerDto.getCccdNumber())) {
                ownerCccdEntity.setCccdNumber(ownerDto.getCccdNumber());
                logger.info("Updated CCCD Number: {}", ownerDto.getCccdNumber());
            }
            if (ownerDto.getIssueDate() != null) {
                ownerCccdEntity.setIssueDate(ownerDto.getIssueDate());
                logger.info("Updated CCCD Issue Date: {}", ownerDto.getIssueDate());
            }
            if (StringUtils.hasText(ownerDto.getIssuePlace())) {
                ownerCccdEntity.setIssuePlace(ownerDto.getIssuePlace());
                logger.info("Updated CCCD Issue Place: {}", ownerDto.getIssuePlace());
            }

            // Save CCCD
            UserCccd savedCccd = userCccdRepository.save(ownerCccdEntity);
            logger.info("CCCD saved successfully. CCCD ID: {}", savedCccd.getId());

            // Chỉ cập nhật địa chỉ nếu có đầy đủ dữ liệu (tùy chọn)
            boolean hasNewAddressData = StringUtils.hasText(ownerDto.getStreet()) &&
                    StringUtils.hasText(ownerDto.getWard()) &&
                    StringUtils.hasText(ownerDto.getDistrict()) &&
                    StringUtils.hasText(ownerDto.getProvince());
            if (hasNewAddressData) {
                StringBuilder newAddress = new StringBuilder();
                newAddress.append(ownerDto.getStreet())
                        .append(", ").append(ownerDto.getWard())
                        .append(", ").append(ownerDto.getDistrict())
                        .append(", ").append(ownerDto.getProvince());
                owner.setAddress(newAddress.toString());
                logger.info("Updated Owner Address: {}", newAddress.toString());
            } else {
                logger.info("Skipping address update due to incomplete data");
            }

            // Save user
            Users savedUser = userService.saveUser(owner);
            logger.info("Owner information saved. User ID: {}", savedUser.getUserId());

            logger.info("=== COMPLETED OWNER INFORMATION UPDATE ===");
        } catch (Exception e) {
            logger.error("Error updating owner information", e);
            throw new RuntimeException("Failed to update owner information", e);
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
        logger.info("=== HANDLE REGISTERED TENANT ===");
        logger.info("Tenant data: {}", tenantDto);

        if (tenantDto.getPhone() == null || tenantDto.getPhone().trim().isEmpty()) {
            logger.error("Tenant phone is null or empty");
            throw new IllegalArgumentException("Số điện thoại người thuê không được để trống!");
        }
        logger.info("Looking for tenant with phone: {}", tenantDto.getPhone());

        Optional<Users> tenantUser = userRepository.findByPhone(tenantDto.getPhone());
        if (!tenantUser.isPresent()) {
            logger.error("Tenant not found with phone: {}", tenantDto.getPhone());
            throw new IllegalArgumentException("Không tìm thấy người thuê với số điện thoại: " + tenantDto.getPhone());
        }

        Users tenant = tenantUser.get();
        logger.info("Found tenant: ID={}, Name={}", tenant.getUserId(), tenant.getFullname());

        // Cập nhật thông tin cơ bản
        if (StringUtils.hasText(tenantDto.getFullName())) {
            tenant.setFullname(tenantDto.getFullName());
            logger.info("Updated tenant fullname: {}", tenantDto.getFullName());
        }
        if (tenantDto.getBirthday() != null) {
            tenant.setBirthday(new java.sql.Date(tenantDto.getBirthday().getTime()));
            logger.info("Updated tenant birthday: {}", tenantDto.getBirthday());
        }

        // Cập nhật CCCD
        logger.info("Updating tenant CCCD information");
        UserCccd tenantCccd = userCccdRepository.findByUserId(tenant.getUserId())
                .orElseGet(() -> {
                    logger.info("Creating new CCCD entity for tenant");
                    UserCccd newCccd = new UserCccd();
                    newCccd.setUser(tenant);
                    return newCccd;
                });

        if (StringUtils.hasText(tenantDto.getCccdNumber())) {
            tenantCccd.setCccdNumber(tenantDto.getCccdNumber());
            logger.info("Tenant CCCD number updated: {}", tenantDto.getCccdNumber());
        }
        if (tenantDto.getIssueDate() != null) {
            tenantCccd.setIssueDate(tenantDto.getIssueDate());
            logger.info("Tenant CCCD issue date updated: {}", tenantDto.getIssueDate());
        }
        if (StringUtils.hasText(tenantDto.getIssuePlace())) {
            tenantCccd.setIssuePlace(tenantDto.getIssuePlace());
            logger.info("Tenant CCCD issue place updated: {}", tenantDto.getIssuePlace());
        }
        userCccdRepository.save(tenantCccd);
        logger.info("Tenant CCCD saved successfully");

        // Cập nhật địa chỉ
        logger.info("Updating tenant address");
        StringBuilder newAddress = new StringBuilder();
        boolean hasAddressData = false;

        if (StringUtils.hasText(tenantDto.getStreet())) {
            newAddress.append(tenantDto.getStreet());
            hasAddressData = true;
        }
        if (StringUtils.hasText(tenantDto.getWard())) {
            newAddress.append(", ").append(tenantDto.getWard());
            hasAddressData = true;
        }
        if (StringUtils.hasText(tenantDto.getDistrict())) {
            newAddress.append(", ").append(tenantDto.getDistrict());
            hasAddressData = true;
        }
        if (StringUtils.hasText(tenantDto.getProvince())) {
            newAddress.append(", ").append(tenantDto.getProvince());
            hasAddressData = true;
        }

        if (hasAddressData) {
            String addressString = newAddress.toString();
            if (!addressString.equals(tenant.getAddress())) {
                tenant.setAddress(addressString);
                logger.info("Updated tenant address: {}", addressString);
            } else {
                logger.info("Tenant address unchanged: {}", addressString);
            }
        } else {
            logger.warn("No address data provided for tenant, skipping address update");
        }

        // Lưu tenant
        Users savedTenant = userService.saveUser(tenant);
        logger.info("Registered tenant updated successfully: ID={}", savedTenant.getUserId());
        return savedTenant;
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

            // Validation cho BigDecimal - ĐÚNG CÁCH
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
            return addressParts;
        }

        String[] parts = addressString.split(", ");

        switch (parts.length) {
            case 4:
                addressParts.put("province", parts[3]);
                addressParts.put("district", parts[2]);
                addressParts.put("ward", parts[1]);
                addressParts.put("street", parts[0]);
                break;
            case 3:
                addressParts.put("province", parts[2]);
                addressParts.put("district", parts[1]);
                addressParts.put("street", parts[0]);
                break;
            case 2:
                addressParts.put("province", parts[1]);
                addressParts.put("street", parts[0]);
                break;
            case 1:
                addressParts.put("street", parts[0]);
                break;
        }

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
        try {
            // Lấy thông tin chủ sở hữu
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            String ownerCccd = userDetails.getCccd();

            // Xác định người thuê
            Users tenant = null;
            UnregisteredTenants unregisteredTenant = null;

            if ("REGISTERED".equals(contractDto.getTenantType())) {
                tenant = handleRegisteredTenant(contractDto.getTenant());
            } else if ("UNREGISTERED".equals(contractDto.getTenantType())) {
                unregisteredTenant = handleUnregisteredTenant(contractDto.getUnregisteredTenant(),
                        userService.findOwnerByCccdOrPhone(authentication, ownerCccd, null));
            }

            // Gọi service để cập nhật
            Contracts updatedContract = contractService.updateContract(contractId, contractDto);

            return ResponseEntity.ok(updatedContract);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi cập nhật: " + e.getMessage());
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
                // IN RA CONSOLE ĐỂ KIỂM TRA
                System.out.println("Contract DTO: " + contractDto);

                // Log chi tiết DTO
                logger.info("Contract DTO Details:");
                logger.info("DTO ID: {}", contractDto.getId());
                logger.info("DTO Contract Date: {}", contractDto.getContractDate());
                logger.info("DTO Status: {}", contractDto.getStatus());

                // Log Tenant DTO
                if (contractDto.getTenant() != null) {
                    logger.info("DTO Tenant Name: {}", contractDto.getTenant().getFullName());
                    logger.info("DTO Tenant Phone: {}", contractDto.getTenant().getPhone());
                    logger.info("DTO Tenant CCCD: {}", contractDto.getTenant().getCccdNumber());
                    logger.info("DTO Tenant Birthday: {}", contractDto.getTenant().getBirthday());
                    logger.info("DTO Tenant Issue Date: {}", contractDto.getTenant().getIssueDate());
                    logger.info("DTO Tenant Issue Place: {}", contractDto.getTenant().getIssuePlace());
                    logger.info("DTO Tenant Province: {}", contractDto.getTenant().getProvince());
                    logger.info("DTO Tenant District: {}", contractDto.getTenant().getDistrict());
                    logger.info("DTO Tenant Ward: {}", contractDto.getTenant().getWard());
                    logger.info("DTO Tenant Street: {}", contractDto.getTenant().getStreet());
                }

                // Log Owner DTO
                if (contractDto.getOwner() != null) {
                    logger.info("DTO Owner Name: {}", contractDto.getOwner().getFullName());
                    logger.info("DTO Owner Phone: {}", contractDto.getOwner().getPhone());
                }

                // Log Room DTO
                if (contractDto.getRoom() != null) {
                    logger.info("DTO Room Address: {}", contractDto.getRoom().getAddress());
                }

                model.addAttribute("contract", contractDto);
                model.addAttribute("isEditMode", true);

                logger.info("Model attributes added: contract, isEditMode");

                return "host/hop-dong-host";
            } else {
                logger.error("Contract not found with ID: {}", contractId);
                model.addAttribute("error", "Không tìm thấy hợp đồng");
                return "host/hop-dong-host";
            }
        } catch (Exception e) {
            logger.error("Error in edit contract form", e);
            model.addAttribute("error", "Lỗi khi tải hợp đồng: " + e.getMessage());
            return "host/hop-dong-host";
        }
    }
    private ContractDto convertToContractDto(Contracts contract) {
        ContractDto dto = new ContractDto();
        dto.setId(contract.getContractId());
//        dto.setContractDate(contract.getContractDate()); // Giữ java.sql.Date
        if (contract.getContractDate() != null) {
            dto.setContractDate(contract.getContractDate().toLocalDate());
        }
        dto.setStatus(String.valueOf(contract.getStatus()));

        // Map tenant (là Users)
        if (contract.getTenant() != null) {
            ContractDto.Tenant tenant = new ContractDto.Tenant();
            Users user = contract.getTenant(); // Tenant là Users
            if (user != null) {
                tenant.setFullName(user.getFullname());
                tenant.setPhone(user.getPhone());
                tenant.setEmail(user.getEmail());
                tenant.setBirthday(user.getBirthday());
                tenant.setStreet(user.getAddress()); // Truy xuất trực tiếp từ cột address
                logger.info("Address for owner with userId {}: {}", user.getUserId(), user.getAddress());
//                if (user.getAddressEntity() != null) {
//                    tenant.setStreet(user.getAddressEntity().getStreet());
//                    if (user.getAddressEntity().getWard() != null) {
//                        tenant.setWard(user.getAddressEntity().getWard().getName());
//                        if (user.getAddressEntity().getWard().getDistrict() != null) {
//                            tenant.setDistrict(user.getAddressEntity().getWard().getDistrict().getName());
//                            if (user.getAddressEntity().getWard().getDistrict().getProvince() != null) {
//                                tenant.setProvince(user.getAddressEntity().getWard().getDistrict().getProvince().getName());
//                            }
//                        }
//                    }
//                }
            }
            UserCccd cccd = user != null ? user.getUserCccd() : null;
            logger.info("UserCccd for tenant with userId {}: {}", user != null ? user.getUserId() : "null", cccd);
            if (cccd != null) {
                logger.info("IssueDate from UserCccd: {}", cccd.getIssueDate());
                tenant.setCccdNumber(cccd.getCccdNumber());
                tenant.setIssueDate(cccd.getIssueDate());
                tenant.setIssuePlace(cccd.getIssuePlace());
            } else {
                logger.warn("No UserCccd found for tenant with user ID: {}", user != null ? user.getUserId() : "N/A");
            }
            dto.setTenant(tenant);
        }

        // Map owner (cũng là Users)
        if (contract.getOwner() != null) {
            ContractDto.Owner owner = new ContractDto.Owner();
            Users user = contract.getOwner(); // Owner là Users
            if (user != null) {
                owner.setFullName(user.getFullname());
                owner.setPhone(user.getPhone());
                owner.setEmail(user.getEmail());
                owner.setBirthday(user.getBirthday());
                // SỬA: Sử dụng trực tiếp cột address từ bảng user
                owner.setStreet(user.getAddress());
                logger.info("Address for owner with userId {}: {}", user.getUserId(), user.getAddress());
//                if (user.getAddressEntity() != null) {
//                    owner.setStreet(user.getAddressEntity().getStreet());
//                    if (user.getAddressEntity().getWard() != null) {
//                        owner.setWard(user.getAddressEntity().getWard().getName());
//                        if (user.getAddressEntity().getWard().getDistrict() != null) {
//                            owner.setDistrict(user.getAddressEntity().getWard().getDistrict().getName());
//                            if (user.getAddressEntity().getWard().getDistrict().getProvince() != null) {
//                                owner.setProvince(user.getAddressEntity().getWard().getDistrict().getProvince().getName());
//                            }
//                        }
//                    }
//                }
            }
            String address = user.getAddress();
            if (StringUtils.hasText(address)) {
                Map<String, String> addressParts = parseAddress(address);
                owner.setStreet(addressParts.getOrDefault("street", ""));
                owner.setWard(addressParts.getOrDefault("ward", ""));
                owner.setDistrict(addressParts.getOrDefault("district", ""));
                owner.setProvince(addressParts.getOrDefault("province", ""));
            }

            UserCccd cccd = user != null ? user.getUserCccd() : null;
            if (cccd != null) {
                owner.setCccdNumber(cccd.getCccdNumber());
                owner.setIssueDate(cccd.getIssueDate());
                owner.setIssuePlace(cccd.getIssuePlace());
            }
            dto.setOwner(owner);
        }

        // Map room
        if (contract.getRoom() != null) {
            ContractDto.Room room = new ContractDto.Room();
            room.setRoomId(contract.getRoom().getRoomId());
            room.setRoomName(contract.getRoom().getNamerooms());
            room.setArea(contract.getRoom().getAcreage());
            room.setPrice(contract.getRoom().getPrice());
//            room.setHostelId(contract.getRoom().getHostelId());
//            if (contract.getRoom().getAddress() != null) {
//                room.setAddress(contract.getRoom().getAddress().getStreet() != null ? contract.getRoom().getAddress().getStreet() : "");
//            } else {
//                room.setAddress("");
//            }
            dto.setRoom(room);
        }

        // Map terms
        if (contract.getTerms() != null) {
            ContractDto.Terms terms = new ContractDto.Terms();
            if (contract.getStartDate() != null) {
                terms.setStartDate(contract.getStartDate().toLocalDate());
            }
            if (contract.getEndDate() != null) {
                terms.setEndDate(contract.getEndDate().toLocalDate());
            }
            terms.setPrice(Double.valueOf(contract.getPrice()));
            terms.setDeposit(Double.valueOf(contract.getDeposit()));
//            terms.setDuration(contract.getDuration());
            terms.setTerms(contract.getTerms());
            dto.setTerms(terms);
        }

        return dto;
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