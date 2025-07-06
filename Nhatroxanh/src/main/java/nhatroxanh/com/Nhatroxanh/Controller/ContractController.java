package nhatroxanh.com.Nhatroxanh.Controller;

import jakarta.validation.Valid;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.*;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
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
            @Valid @RequestBody ContractDto contract,  // ← SỬA: @RequestBody thay vì @ModelAttribute
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
        logger.info("=== UPDATE OWNER INFORMATION ===");
        logger.info("Owner ID: {}, Current name: {}", owner.getUserId(), owner.getFullname());
        logger.info("New owner data: {}", ownerDto);

        owner.setFullname(ownerDto.getFullName());
        owner.setPhone(ownerDto.getPhone());
        if (ownerDto.getBirthday() != null) {
            owner.setBirthday(new java.sql.Date(ownerDto.getBirthday().getTime()));
            logger.info("Updated owner birthday: {}", ownerDto.getBirthday());
        }

        // Update owner CCCD
        logger.info("Updating owner CCCD information");
        UserCccd ownerCccdEntity = userCccdRepository.findByUserId(owner.getUserId())
                .orElseGet(() -> {
                    logger.info("Creating new CCCD entity for owner");
                    UserCccd newCccd = new UserCccd();
                    newCccd.setUser(owner);
                    return newCccd;
                });

        ownerCccdEntity.setCccdNumber(ownerDto.getCccdNumber());
        ownerCccdEntity.setIssueDate(ownerDto.getIssueDate());
        ownerCccdEntity.setIssuePlace(ownerDto.getIssuePlace());
        logger.info("CCCD Number: {}, Issue Date: {}, Issue Place: {}",
                ownerDto.getCccdNumber(), ownerDto.getIssueDate(), ownerDto.getIssuePlace());

        // Bỏ phần xử lý file upload vì JSON không có file

        userCccdRepository.save(ownerCccdEntity);
        logger.info("Owner CCCD saved successfully");

        // Update owner address
        logger.info("Updating owner address");
        Optional<Address> addressOptional = userService.findAddressByUserId(owner.getUserId());
        Address address = addressOptional.orElseGet(() -> {
            logger.info("Creating new address for owner");
            Address newAddress = new Address();
            newAddress.setUser(owner);
            return newAddress;
        });
        address.setStreet(ownerDto.getStreet());
        logger.info("Owner address: {}", ownerDto.getStreet());
        userService.saveAddress(address);

        userService.saveUser(owner);
        logger.info("Owner information updated successfully");
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

        // Bỏ phần xử lý file upload

        unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);

        Address address = new Address();
        address.setStreet(tenantDto.getStreet());
        unregisteredTenant.setAddress(address);
        logger.info("Tenant address: {}", tenantDto.getStreet());

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

        tenant.setFullname(tenantDto.getFullName());
        if (tenantDto.getBirthday() != null) {
            tenant.setBirthday(new java.sql.Date(tenantDto.getBirthday().getTime()));
            logger.info("Updated tenant birthday: {}", tenantDto.getBirthday());
        }

        // Update tenant CCCD
        logger.info("Updating tenant CCCD information");
        UserCccd tenantCccd = userCccdRepository.findByUserId(tenant.getUserId())
                .orElseGet(() -> {
                    logger.info("Creating new CCCD entity for tenant");
                    UserCccd newCccd = new UserCccd();
                    newCccd.setUser(tenant);
                    return newCccd;
                });

        tenantCccd.setCccdNumber(tenantDto.getCccdNumber());
        tenantCccd.setIssueDate(tenantDto.getIssueDate());
        tenantCccd.setIssuePlace(tenantDto.getIssuePlace());
        logger.info("Tenant CCCD: Number={}, Issue Date={}, Issue Place={}",
                tenantDto.getCccdNumber(), tenantDto.getIssueDate(), tenantDto.getIssuePlace());

        // Bỏ phần xử lý file upload

        userCccdRepository.save(tenantCccd);
        logger.info("Tenant CCCD saved successfully");

        // Update tenant address
        logger.info("Updating tenant address");
        Optional<Address> tenantAddress = userService.findAddressByUserId(tenant.getUserId());
        Address address = tenantAddress.orElseGet(() -> {
            logger.info("Creating new address for tenant");
            Address newAddress = new Address();
            newAddress.setUser(tenant);
            return newAddress;
        });
        address.setStreet(tenantDto.getStreet());
        logger.info("Tenant address: {}", tenantDto.getStreet());
        userService.saveAddress(address);

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
            unregisteredTenant.setAddress(address);

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
            tenantData.put("street", unregisteredTenant.getAddress() != null ? unregisteredTenant.getAddress().getStreet() : null);

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