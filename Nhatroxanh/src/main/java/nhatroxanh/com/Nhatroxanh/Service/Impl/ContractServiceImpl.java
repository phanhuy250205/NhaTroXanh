package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Model.entity.*;
import nhatroxanh.com.Nhatroxanh.Repository.ContractRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UtilityRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UnregisteredTenantsRepository;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import nhatroxanh.com.Nhatroxanh.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContractServiceImpl implements ContractService {

    private static final Logger logger = LoggerFactory.getLogger(ContractServiceImpl.class);

    @Autowired
    private UtilityRepository utilityRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomsRepository roomRepository;

    @Autowired
    private UserCccdRepository userCccdRepository;

    @Autowired
    private UnregisteredTenantsRepository unregisteredTenantsRepository;

    @Override
    @Transactional
    public Contracts createContract(
            String tenantPhone, Integer roomId, Date contractDate, Date startDate,
            Date endDate, Float price, Float deposit, String terms,
            Contracts.Status status, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant,
            Integer duration) throws Exception {
        logger.info("=== START CREATE CONTRACT (Detailed Parameters) ===");
        logger.info("Creating new contract for tenant phone: {}", tenantPhone);
        logger.info(
                "Input parameters - roomId: {}, contractDate: {}, startDate: {}, endDate: {}, price: {}, deposit: {}, duration: {}, status: {}",
                roomId, contractDate, startDate, endDate, price, deposit, duration, status);

        // Validate inputs
        if (tenantPhone == null || tenantPhone.trim().isEmpty()) {
            logger.error("Tenant phone is null or empty");
            throw new IllegalArgumentException("Số điện thoại khách thuê không được để trống!");
        }
        logger.info("Tenant phone validated: {}", tenantPhone);

        if (roomId == null || roomId <= 0) {
            logger.error("Invalid room ID: {}", roomId);
            throw new IllegalArgumentException("ID phòng không hợp lệ!");
        }
        logger.info("Room ID validated: {}", roomId);

        if (contractDate == null) {
            logger.error("Contract date is null");
            throw new IllegalArgumentException("Ngày lập hợp đồng không được null!");
        }
        logger.info("Contract date validated: {}", contractDate);

        if (startDate == null || endDate == null) {
            logger.error("Start date or end date is null, startDate: {}, endDate: {}", startDate, endDate);
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được null!");
        }
        logger.info("Start and end dates validated: {}, {}", startDate, endDate);

        if (price == null || price <= 0) {
            logger.error("Invalid price: {}", price);
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0!");
        }
        logger.info("Price validated: {}", price);

        if (deposit == null || deposit < 0) {
            logger.error("Invalid deposit: {}", deposit);
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn hoặc bằng 0!");
        }
        logger.info("Deposit validated: {}", deposit);

        if (terms != null && terms.length() > 255) {
            logger.error("Terms too long: {}", terms.length());
            throw new IllegalArgumentException("Điều khoản không được vượt quá 255 ký tự!");
        }
        logger.info("Terms validated: {}", terms);

        if (tenant == null && unregisteredTenant == null) {
            logger.error("No tenant or unregistered tenant provided");
            throw new IllegalArgumentException("Phải cung cấp thông tin người thuê (đã đăng ký hoặc chưa đăng ký)!");
        }
        logger.info("Tenant/unregisteredTenant provided: tenant={}, unregisteredTenant={}", tenant != null,
                unregisteredTenant != null);

        if (duration != null && duration <= 0) {
            logger.error("Invalid duration: {}", duration);
            throw new IllegalArgumentException("Thời hạn hợp đồng phải lớn hơn 0!");
        }
        logger.info("Duration validated: {}", duration);

        // Find owner
        logger.info("Searching for owner with CCCD: {}", ownerCccd);
        Optional<UserCccd> ownerCccdOpt = userCccdRepository.findByCccdNumber(ownerCccd);
        Users owner = ownerCccdOpt.map(UserCccd::getUser)
                .orElseThrow(() -> {
                    logger.error("Owner not found with CCCD: {}", ownerCccd);
                    return new IllegalArgumentException("Chủ trọ không tồn tại!");
                });
        logger.info("Owner found: {}", owner.getFullname());
        if (owner.getRole() != Users.Role.OWNER) {
            logger.error("User with CCCD {} is not an owner", ownerCccd);
            throw new IllegalArgumentException("Người dùng không phải là chủ trọ!");
        }

        // Find room
        logger.info("Searching for room with ID: {}", roomId);
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.error("Room not found: {}", roomId);
                    return new IllegalArgumentException("Phòng không tồn tại!");
                });
        logger.info("Room found: {}", room.getNamerooms());

        // Check for active contract on room
        logger.info("Checking for active contract on room ID: {}", roomId);
        Optional<Contracts> activeContract = contractRepository.findActiveContractByRoomId(roomId,
                Contracts.Status.ACTIVE);
        if (activeContract.isPresent()) {
            logger.error("Room {} has an active contract", roomId);
            throw new Exception("Phòng hiện đang có hợp đồng active!");
        }

        // Create contract
        logger.info("Building new contract entity");
        System.out.println("STATUS: " + status);
        Contracts contract = Contracts.builder()
                .tenantPhone(tenantPhone)
                .contractDate(contractDate)
                .createdAt(new Date(System.currentTimeMillis()))
                .startDate(startDate)
                .endDate(endDate)
                .price(price)
                .deposit(deposit)
                .terms(terms)
                .status(status)
                .owner(owner)
                .room(room)
                .duration(duration != null ? Float.valueOf(duration) : null)
                .build();
        logger.info("Contract entity built: {}", contract);

        // Handle tenant
        if (unregisteredTenant != null) {
            logger.info("Handling unregistered tenant: {}", unregisteredTenant.getFullName());
            if (unregisteredTenant.getStatus() == null) {
                unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);
            }
            unregisteredTenantsRepository.save(unregisteredTenant);
            contract.setUnregisteredTenant(unregisteredTenant);
            contract.setTenant(null); // Đảm bảo chỉ 1 loại tenant được set
            logger.info("Unregistered tenant saved and set to contract");
        } else if (tenant != null) {
            logger.info("Handling registered tenant: {}", tenant.getFullname());
            if (tenant.getRole() != Users.Role.CUSTOMER) {
                logger.error("User with ID {} is not a customer", tenant.getUserId());
                throw new IllegalArgumentException("Người dùng không phải là khách thuê!");
            }
            contract.setTenant(tenant);
            contract.setUnregisteredTenant(null); // Đảm bảo chỉ 1 loại tenant được set
            logger.info("Registered tenant set to contract");
        }

        logger.info("Saving contract to repository");
        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract created successfully: {}", savedContract.getContractId());
        logger.info("=== END CREATE CONTRACT (Detailed Parameters) ===");
        return savedContract;
    }

    @Override
    @Transactional
    public Contracts createContractFromDto(ContractDto contractDto, Integer ownerId, MultipartFile cccdFrontFile,
                                           MultipartFile cccdBackFile) {
        logger.info("SERVICE: Bắt đầu tạo hợp đồng từ DTO cho owner ID: {}", ownerId);

        // 1. Lấy và xác thực Owner (Chủ trọ) từ ownerId
        Users owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin chủ trọ!"));

        // 2. Lấy và xác thực Phòng trọ
        Rooms room = roomRepository.findById(contractDto.getRoom().getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Phòng trọ không tồn tại!"));

        if (room.getStatus() != RoomStatus.unactive) {
            throw new IllegalStateException("Phòng này đã được thuê hoặc không khả dụng.");
        }

        // Xử lý tiện ích phòng
        if (contractDto.getRoom().getUtilityIds() != null && !contractDto.getRoom().getUtilityIds().isEmpty()) {
            Set<Utility> utilities = utilityRepository.findByUtilityIdIn(contractDto.getRoom().getUtilityIds());
            room.setUtilities(utilities);
            logger.info("SERVICE: Đã gán {} tiện ích cho phòng ID {}.", utilities.size(), room.getRoomId());
        } else {
            room.getUtilities().clear();
            logger.info("SERVICE: Đã xóa hết tiện ích cho phòng ID {}.", room.getRoomId());
        }

        Users tenant = null;
        UnregisteredTenants guardian = null;
        String finalTenantPhone = null;

        // 3. LOGIC QUAN TRỌNG: KIỂM TRA XEM ĐANG TẠO CHO AI
        if (contractDto.getTenantType() != null && "UNREGISTERED".equals(contractDto.getTenantType()) &&
                contractDto.getUnregisteredTenant() != null &&
                StringUtils.hasText(contractDto.getUnregisteredTenant().getFullName())) {
            logger.info("SERVICE: Phát hiện thông tin Người bảo hộ/Thuê mới. Đang xử lý...");
            guardian = handleUnregisteredTenant(contractDto.getUnregisteredTenant(), owner, cccdFrontFile, cccdBackFile);
            finalTenantPhone = guardian.getPhone();
        } else if (contractDto.getTenantType() != null && "REGISTERED".equals(contractDto.getTenantType()) &&
                contractDto.getTenant() != null) {
            logger.info("SERVICE: Xử lý Người thuê đã đăng ký...");
            tenant = handleRegisteredTenant(contractDto.getTenant());
            finalTenantPhone = tenant.getPhone();
        } else {
            logger.error("Không có thông tin người thuê hợp lệ được cung cấp trong DTO.");
            throw new IllegalArgumentException("Phải cung cấp thông tin người thuê hợp lệ!");
        }

        // 4. Tạo và lưu đối tượng Hợp đồng
        Contracts contract = new Contracts();
        contract.setOwner(owner);
        contract.setRoom(room);
        contract.setTenant(tenant);
        contract.setUnregisteredTenant(guardian);
        contract.setTenantPhone(finalTenantPhone);

        // Điền các thông tin còn lại từ DTO
        contract.setContractDate(Date.valueOf(contractDto.getContractDate()));
        contract.setStartDate(Date.valueOf(contractDto.getTerms().getStartDate()));
        contract.setEndDate(Date.valueOf(contractDto.getTerms().getEndDate()));
        contract.setPrice(contractDto.getTerms().getPrice().floatValue());
        contract.setDeposit(contractDto.getTerms().getDeposit().floatValue());
        contract.setDuration(Float.valueOf(contractDto.getTerms().getDuration()));
        contract.setStatus(Contracts.Status.valueOf(contractDto.getStatus().toUpperCase()));
        contract.setTerms(contractDto.getTerms().getTerms());
        contract.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));

        if (contractDto.getPaymentMethod() != null) {
            contract.setPaymentMethod(Contracts.PaymentMethod.valueOf(contractDto.getPaymentMethod().name()));
        }
        if (contractDto.getResidents() != null && !contractDto.getResidents().isEmpty()) {
            logger.info("SERVICE: Tìm thấy {} người ở. Đang xử lý...", contractDto.getResidents().size());
            for (ContractDto.ResidentDto residentDto : contractDto.getResidents()) {
                Resident resident = new Resident();
                resident.setFullName(residentDto.getFullName());
                resident.setBirthYear(residentDto.getBirthYear());
                resident.setPhone(residentDto.getPhone());
                resident.setCccdNumber(residentDto.getCccdNumber());
                resident.setContract(contract); // **QUAN TRỌNG**: Liên kết người ở với hợp đồng này

                contract.getResidents().add(resident); // Thêm vào danh sách của hợp đồng
            }
        }

        Contracts savedContract = contractRepository.save(contract);

        // 5. Cập nhật trạng thái phòng
        room.setStatus(RoomStatus.active);
        roomRepository.save(room);

        logger.info("SERVICE: Đã tạo hợp đồng ID {} thành công.", savedContract.getContractId());
        return savedContract;
    }

    @Override
    @Transactional
    public Contracts updateContract(Integer contractId, Contracts updatedContract) throws Exception {
        logger.info("=== START UPDATE CONTRACT (Full Contract Object) ===");
        logger.info("Updating contract with ID: {}", contractId);
        logger.info("Updated contract data: {}", updatedContract);

        if (contractId == null || contractId <= 0) {
            logger.error("Invalid contract ID: {}", contractId);
            throw new IllegalArgumentException("ID hợp đồng không hợp lệ!");
        }

        Optional<Contracts> existingContract = contractRepository.findById(contractId);
        if (!existingContract.isPresent()) {
            logger.error("Contract not found: {}", contractId);
            throw new Exception("Hợp đồng không tồn tại!");
        }

        Contracts contract = existingContract.get();
        logger.info("Current contract: {}", contract);

        if (updatedContract.getTenantPhone() != null && !updatedContract.getTenantPhone().isEmpty()) {
            contract.setTenantPhone(updatedContract.getTenantPhone());
            logger.info("Updated tenant phone: {}", updatedContract.getTenantPhone());
        }
        if (updatedContract.getContractDate() != null) {
            contract.setContractDate(updatedContract.getContractDate());
            logger.info("Updated contract date: {}", updatedContract.getContractDate());
        } else {
            logger.error("Contract date is null for contract ID: {}", contractId);
            throw new IllegalArgumentException("Ngày lập hợp đồng không được null!");
        }
        if (updatedContract.getStartDate() != null) {
            contract.setStartDate(updatedContract.getStartDate());
            logger.info("Updated start date: {}", updatedContract.getStartDate());
        } else {
            logger.error("Start date is null for contract ID: {}", contractId);
            throw new IllegalArgumentException("Ngày bắt đầu không được null!");
        }
        if (updatedContract.getEndDate() != null) {
            contract.setEndDate(updatedContract.getEndDate());
            logger.info("Updated end date: {}", updatedContract.getEndDate());
        } else {
            logger.error("End date is null for contract ID: {}", contractId);
            throw new IllegalArgumentException("Ngày kết thúc không được null!");
        }
        if (updatedContract.getPrice() != null && updatedContract.getPrice() > 0) {
            contract.setPrice(updatedContract.getPrice());
            logger.info("Updated price: {}", updatedContract.getPrice());
        } else {
            logger.error("Invalid price: {} for contract ID: {}", updatedContract.getPrice(), contractId);
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0!");
        }
        if (updatedContract.getDeposit() != null && updatedContract.getDeposit() >= 0) {
            contract.setDeposit(updatedContract.getDeposit());
            logger.info("Updated deposit: {}", updatedContract.getDeposit());
        } else {
            logger.error("Invalid deposit: {} for contract ID: {}", updatedContract.getDeposit(), contractId);
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn hoặc bằng 0!");
        }
        if (updatedContract.getTerms() != null) {
            if (updatedContract.getTerms().length() > 255) {
                logger.error("Terms too long for contract ID: {}", contractId);
                throw new IllegalArgumentException("Điều khoản không được vượt quá 255 ký tự!");
            }
            contract.setTerms(updatedContract.getTerms());
            logger.info("Updated terms: {}", updatedContract.getTerms());
        }
        if (updatedContract.getStatus() != null) {
            contract.setStatus(updatedContract.getStatus());
            logger.info("Updated status: {}", updatedContract.getStatus());
        }
        if (updatedContract.getDuration() != null && updatedContract.getDuration() > 0) {
            contract.setDuration(updatedContract.getDuration());
            logger.info("Updated duration: {}", updatedContract.getDuration());
        }

        if (updatedContract.getRoom() != null && updatedContract.getRoom().getRoomId() != null) {
            logger.info("Updating room with ID: {}", updatedContract.getRoom().getRoomId());
            Rooms room = roomRepository.findById(updatedContract.getRoom().getRoomId())
                    .orElseThrow(() -> {
                        logger.error("Room not found: {}", updatedContract.getRoom().getRoomId());
                        return new IllegalArgumentException("Phòng không tồn tại!");
                    });
            contract.setRoom(room);
            logger.info("Room updated: {}", room.getNamerooms());
        }
        if (updatedContract.getTenant() != null && updatedContract.getTenant().getUserId() != null) {
            logger.info("Updating tenant with ID: {}", updatedContract.getTenant().getUserId());
            Users tenant = userRepository.findById(updatedContract.getTenant().getUserId())
                    .orElseThrow(() -> {
                        logger.error("Tenant not found: {}", updatedContract.getTenant().getUserId());
                        return new Exception("Người thuê không tồn tại!");
                    });
            contract.setTenant(tenant);
            contract.setUnregisteredTenant(null); // Đảm bảo set null cho unregistered tenant
            logger.info("Tenant updated: {}", tenant.getFullname());
        }
        if (updatedContract.getUnregisteredTenant() != null
                && updatedContract.getUnregisteredTenant().getId() != null) {
            logger.info("Updating unregistered tenant with ID: {}", updatedContract.getUnregisteredTenant().getId());
            UnregisteredTenants unregisteredTenantUpdate = unregisteredTenantsRepository
                    .findById(updatedContract.getUnregisteredTenant().getId())
                    .orElseThrow(() -> {
                        logger.error("Unregistered tenant not found: {}",
                                updatedContract.getUnregisteredTenant().getId());
                        return new Exception("Người thuê chưa đăng ký không tồn tại!");
                    });
            if (unregisteredTenantUpdate.getStatus() == null) {
                unregisteredTenantUpdate.setStatus(UnregisteredTenants.Status.ACTIVE);
                unregisteredTenantsRepository.save(unregisteredTenantUpdate);
            }
            contract.setUnregisteredTenant(unregisteredTenantUpdate);
            contract.setTenant(null); // Đảm bảo set null cho registered tenant
            logger.info("Unregistered tenant updated: {}", unregisteredTenantUpdate.getFullName());
        }
        if (updatedContract.getOwner() != null && updatedContract.getOwner().getUserId() != null) {
            logger.info("Updating owner with ID: {}", updatedContract.getOwner().getUserId());
            Users owner = userRepository.findById(updatedContract.getOwner().getUserId())
                    .orElseThrow(() -> {
                        logger.error("Owner not found: {}", updatedContract.getOwner().getUserId());
                        return new Exception("Chủ trọ không tồn tại!");
                    });
            contract.setOwner(owner);
            logger.info("Owner updated: {}", owner.getFullname());
        }

        logger.info("Saving updated contract");
        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract updated successfully: {}", savedContract.getContractId());
        logger.info("=== END UPDATE CONTRACT ===");
        return savedContract;
    }

    @Override
    @Transactional
    public Contracts updateContract(Integer contractId, ContractDto contractDto) throws Exception {
        logger.info("=== START UPDATE CONTRACT FROM DTO ===");
        logger.info("Updating contract with ID: {} from DTO with tenant type: {}", contractId,
                contractDto.getTenantType());

        // Validate contractId
        if (contractId == null || contractId <= 0) {
            logger.error("Invalid contract ID: {}", contractId);
            throw new IllegalArgumentException("ID hợp đồng không hợp lệ!");
        }

        // Tìm contract hiện tại
        Optional<Contracts> existingContract = contractRepository.findById(contractId);
        if (!existingContract.isPresent()) {
            logger.error("Contract not found: {}", contractId);
            throw new ResourceNotFoundException("Hợp đồng không tồn tại!");
        }

        Contracts contract = existingContract.get();
        logger.info("Current contract: {}", contract);

        // Cập nhật tenant phone (nếu có)
        if (contractDto.getTenantType() != null) {
            if ("REGISTERED".equals(contractDto.getTenantType()) && contractDto.getTenant() != null
                    && contractDto.getTenant().getPhone() != null) {
                contract.setTenantPhone(contractDto.getTenant().getPhone());
                logger.info("Updated tenant phone: {}", contract.getTenantPhone());
            } else if ("UNREGISTERED".equals(contractDto.getTenantType()) && contractDto.getUnregisteredTenant() != null
                    && contractDto.getUnregisteredTenant().getPhone() != null) {
                contract.setTenantPhone(contractDto.getUnregisteredTenant().getPhone());
                logger.info("Updated tenant phone: {}", contract.getTenantPhone());
            }
        }

        // Cập nhật contract date (nếu có)
        if (contractDto.getContractDate() != null) {
            contract.setContractDate(Date.valueOf(contractDto.getContractDate()));
            logger.info("Updated contract date: {}", contract.getContractDate());
        }

        // Cập nhật terms (nếu có)
        if (contractDto.getTerms() != null) {
            if (contractDto.getTerms().getStartDate() != null) {
                contract.setStartDate(Date.valueOf(contractDto.getTerms().getStartDate()));
                logger.info("Updated start date: {}", contract.getStartDate());
            }
            if (contractDto.getTerms().getEndDate() != null) {
                contract.setEndDate(Date.valueOf(contractDto.getTerms().getEndDate()));
                logger.info("Updated end date: {}", contract.getEndDate());
            } else if (contractDto.getTerms().getDuration() != null && contractDto.getTerms().getDuration() > 0
                    && contract.getStartDate() != null) {
                contract.setDuration(Float.valueOf(contractDto.getTerms().getDuration()));
                LocalDate startDate = contract.getStartDate().toLocalDate();
                contract.setEndDate(Date.valueOf(startDate.plusMonths(contractDto.getTerms().getDuration())));
                logger.info("Updated duration: {}, endDate: {}", contract.getDuration(), contract.getEndDate());
            }
            if (contractDto.getTerms().getPrice() != null) {
                contract.setPrice(contractDto.getTerms().getPrice().floatValue());
                logger.info("Updated price: {}", contract.getPrice());
            }
            if (contractDto.getTerms().getDeposit() != null) {
                contract.setDeposit(contractDto.getTerms().getDeposit().floatValue());
                logger.info("Updated deposit: {}", contract.getDeposit());
            }
            if (contractDto.getTerms().getTerms() != null) {
                if (contractDto.getTerms().getTerms().length() > 255) {
                    logger.error("Terms too long: {}", contractDto.getTerms().getTerms().length());
                    throw new IllegalArgumentException("Điều khoản không được vượt quá 255 ký tự!");
                }
                contract.setTerms(contractDto.getTerms().getTerms());
                logger.info("Updated terms: {}", contract.getTerms());
            }
        }

        // Cập nhật status (nếu có)
        if (contractDto.getStatus() != null) {
            try {
                Contracts.Status newStatus = parseStatusFromString(contractDto.getStatus());
                if (canChangeStatus(contract.getStatus(), newStatus)) {
                    contract.setStatus(newStatus);
                    logger.info("Updated status: {}", contract.getStatus());
                } else {
                    logger.error("Invalid status transition from {} to {}", contract.getStatus(), newStatus);
                    throw new IllegalArgumentException("Không thể chuyển trạng thái từ "
                            + getStatusLabel(contract.getStatus()) + " sang " + getStatusLabel(newStatus));
                }
            } catch (IllegalArgumentException e) {
                logger.error("Invalid status: {}", contractDto.getStatus());
                throw new IllegalArgumentException("Trạng thái hợp đồng không hợp lệ: " + contractDto.getStatus());
            }
        }

        // Cập nhật owner (nếu có)
        if (contractDto.getOwner() != null && contractDto.getOwner().getCccdNumber() != null) {
            logger.info("Searching for owner with CCCD: {}", contractDto.getOwner().getCccdNumber());
            Optional<UserCccd> ownerCccdOpt = userCccdRepository
                    .findByCccdNumber(contractDto.getOwner().getCccdNumber());
            if (ownerCccdOpt.isPresent()) {
                Users owner = ownerCccdOpt.get().getUser();
                if (owner.getRole() == Users.Role.OWNER) {
                    contract.setOwner(owner);
                    logger.info("Owner updated: {}", owner.getFullname());
                } else {
                    logger.error("User with CCCD {} is not an owner", contractDto.getOwner().getCccdNumber());
                    throw new IllegalArgumentException("Người dùng không phải là chủ trọ!");
                }
            } else {
                logger.warn("Owner not found with CCCD: {}, skipping update", contractDto.getOwner().getCccdNumber());
            }
        }

        // Cập nhật tenant (nếu có)
        if (contractDto.getTenantType() != null) {
            if ("REGISTERED".equals(contractDto.getTenantType()) && contractDto.getTenant() != null
                    && contractDto.getTenant().getPhone() != null) {
                logger.info("Finding tenant with phone: {}", contractDto.getTenant().getPhone());
                Optional<Users> tenantOpt = userRepository.findByPhone(contractDto.getTenant().getPhone());
                if (tenantOpt.isPresent()) {
                    Users tenant = tenantOpt.get();
                    if (tenant.getRole() != Users.Role.CUSTOMER) {
                        logger.error("User with phone {} is not a customer", contractDto.getTenant().getPhone());
                        throw new IllegalArgumentException("Người dùng không phải là khách thuê!");
                    }
                    if (contractDto.getTenant().getFullName() != null) {
                        tenant.setFullname(contractDto.getTenant().getFullName());
                    }
                    if (contractDto.getTenant().getBirthday() != null) {
                        tenant.setBirthday(new java.sql.Date(contractDto.getTenant().getBirthday().getTime()));
                    }
                    if (contractDto.getTenant().getStreet() != null || contractDto.getTenant().getWard() != null ||
                            contractDto.getTenant().getDistrict() != null
                            || contractDto.getTenant().getProvince() != null) {
                        StringBuilder addressBuilder = new StringBuilder();
                        if (contractDto.getTenant().getStreet() != null)
                            addressBuilder.append(contractDto.getTenant().getStreet());
                        if (contractDto.getTenant().getWard() != null)
                            addressBuilder.append(", ").append(contractDto.getTenant().getWard());
                        if (contractDto.getTenant().getDistrict() != null)
                            addressBuilder.append(", ").append(contractDto.getTenant().getDistrict());
                        if (contractDto.getTenant().getProvince() != null)
                            addressBuilder.append(", ").append(contractDto.getTenant().getProvince());
                        if (addressBuilder.length() > 0) {
                            tenant.setAddress(addressBuilder.toString());
                        }
                    }
                    userRepository.save(tenant);
                    contract.setTenant(tenant);
                    contract.setUnregisteredTenant(null); // Đảm bảo clear unregistered tenant nếu chuyển sang registered
                    logger.info("Updated registered tenant: {}", tenant.getFullname());
                } else {
                    logger.warn("Tenant not found with phone: {}, skipping update", contractDto.getTenant().getPhone());
                }
            } else if ("UNREGISTERED".equals(contractDto.getTenantType()) && contractDto.getUnregisteredTenant() != null
                    && contractDto.getUnregisteredTenant().getPhone() != null) {
                logger.info("Updating/creating unregistered tenant with phone: {}",
                        contractDto.getUnregisteredTenant().getPhone());
                UnregisteredTenants unregisteredTenant = unregisteredTenantsRepository
                        .findByPhone(contractDto.getUnregisteredTenant().getPhone())
                        .orElse(new UnregisteredTenants());
                unregisteredTenant.setUser(contract.getOwner()); // Associate with owner
                if (contractDto.getUnregisteredTenant().getFullName() != null) {
                    unregisteredTenant.setFullName(contractDto.getUnregisteredTenant().getFullName());
                }
                if (contractDto.getUnregisteredTenant().getPhone() != null) {
                    unregisteredTenant.setPhone(contractDto.getUnregisteredTenant().getPhone());
                }
                if (contractDto.getUnregisteredTenant().getCccdNumber() != null) {
                    unregisteredTenant.setCccdNumber(contractDto.getUnregisteredTenant().getCccdNumber());
                }
                if (contractDto.getUnregisteredTenant().getIssueDate() != null) {
                    unregisteredTenant.setIssueDate(contractDto.getUnregisteredTenant().getIssueDate());
                }
                if (contractDto.getUnregisteredTenant().getIssuePlace() != null) {
                    unregisteredTenant.setIssuePlace(contractDto.getUnregisteredTenant().getIssuePlace());
                }
                if (contractDto.getUnregisteredTenant().getBirthday() != null) {
                    unregisteredTenant.setBirthday(
                            new java.sql.Date(contractDto.getUnregisteredTenant().getBirthday().getTime()));
                }
                if (contractDto.getUnregisteredTenant().getCccdFrontUrl() != null) {
                    unregisteredTenant.setCccdFrontUrl(contractDto.getUnregisteredTenant().getCccdFrontUrl());
                }
                if (contractDto.getUnregisteredTenant().getCccdBackUrl() != null) {
                    unregisteredTenant.setCccdBackUrl(contractDto.getUnregisteredTenant().getCccdBackUrl());
                }
                if (contractDto.getUnregisteredTenant().getStreet() != null
                        || contractDto.getUnregisteredTenant().getWard() != null ||
                        contractDto.getUnregisteredTenant().getDistrict() != null
                        || contractDto.getUnregisteredTenant().getProvince() != null) {
                    StringBuilder addressBuilder = new StringBuilder();
                    if (contractDto.getUnregisteredTenant().getStreet() != null)
                        addressBuilder.append(contractDto.getUnregisteredTenant().getStreet());
                    if (contractDto.getUnregisteredTenant().getWard() != null)
                        addressBuilder.append(", ").append(contractDto.getUnregisteredTenant().getWard());
                    if (contractDto.getUnregisteredTenant().getDistrict() != null)
                        addressBuilder.append(", ").append(contractDto.getUnregisteredTenant().getDistrict());
                    if (contractDto.getUnregisteredTenant().getProvince() != null)
                        addressBuilder.append(", ").append(contractDto.getUnregisteredTenant().getProvince());
                    if (addressBuilder.length() > 0) {
                        unregisteredTenant.setAddress(addressBuilder.toString());
                    }
                }
                unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);
                unregisteredTenantsRepository.save(unregisteredTenant);
                contract.setUnregisteredTenant(unregisteredTenant);
                contract.setTenant(null); // Đảm bảo clear registered tenant nếu chuyển sang unregistered
                logger.info("Updated unregistered tenant: {}", unregisteredTenant.getFullName());
            }
        }

        // Lưu contract
        logger.info("Saving updated contract");
        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract updated successfully: {}", savedContract.getContractId());
        logger.info("=== END UPDATE CONTRACT FROM DTO ===");
        return savedContract;
    }

    @Override
    @Transactional
    public void deleteContract(Integer contractId) throws Exception {
        logger.info("=== START DELETE CONTRACT ===");
        logger.info("Deleting contract with ID: {}", contractId);

        if (contractId == null || contractId <= 0) {
            logger.error("Invalid contract ID: {}", contractId);
            throw new IllegalArgumentException("ID hợp đồng không hợp lệ!");
        }

        Optional<Contracts> contract = contractRepository.findById(contractId);
        if (!contract.isPresent()) {
            logger.error("Contract not found: {}", contractId);
            throw new Exception("Hợp đồng không tồn tại!");
        }

        logger.info("Deleting contract: {}", contract.get().getContractId());
        contractRepository.delete(contract.get());
        logger.info("Contract deleted successfully: {}", contractId);
        logger.info("=== END DELETE CONTRACT ===");
    }

    @Override
    public Optional<Contracts> findContractById(Integer contractId) {
        logger.info("Finding contract by ID: {}", contractId);
        return contractRepository.findById(contractId);
    }

    @Override
    public List<Contracts> findContractsByRoomId(Integer roomId) {
        logger.info("Finding contracts by room ID: {}", roomId);
        return contractRepository.findByRoomId(roomId);
    }

    @Override
    public List<Contracts> findContractsByTenantUserId(Integer tenantUserId) {
        logger.info("Finding contracts by tenant user ID: {}", tenantUserId);
        return contractRepository.findByTenantUserId(tenantUserId);
    }

    @Override
    public List<Contracts> findContractsByOwnerId(Integer ownerId) {
        logger.info("Finding contracts by owner ID: {}", ownerId);
        return contractRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Contracts> findContractsByStatus(Contracts.Status status) {
        logger.info("Finding contracts by status: {}", status);
        return contractRepository.findByStatus(status);
    }

    @Override
    public List<Contracts> findContractsByTenantName(String name) {
        logger.info("Finding contracts by tenant name: {}", name);
        List<Contracts> foundContracts = contractRepository.findByTenantName(name);
        foundContracts.addAll(contractRepository.findByUnregisteredTenantName(name));
        return foundContracts.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<Contracts> findContractsByTenantPhone(String phone) {
        logger.info("Finding contracts by tenant phone: {}", phone);
        List<Contracts> foundContracts = contractRepository.findByTenantPhone(phone);
        foundContracts.addAll(contractRepository.findByUnregisteredTenantPhone(phone));
        return foundContracts.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<Contracts> findContractsByTenantCccd(String cccd) {
        logger.info("Finding contracts by tenant CCCD: {}", cccd);
        List<Contracts> foundContracts = contractRepository.findByTenantCccd(cccd);
        foundContracts.addAll(contractRepository.findByUnregisteredTenantCccd(cccd));
        return foundContracts.stream().distinct().collect(Collectors.toList());
    }

    @Override
    public List<Contracts> findContractsByDateRange(Date startDate, Date endDate) {
        logger.info("Finding contracts by date range: {} to {}", startDate, endDate);
        return contractRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public List<Contracts> findContractsExpiringWithin30Days() {
        logger.info("Finding contracts expiring within 30 days");
        LocalDate now = LocalDate.now();
        LocalDate threshold = now.plusDays(30);
        Date sqlThreshold = Date.valueOf(threshold);
        return contractRepository.findByEndDateLessThanEqualAndStatus(sqlThreshold, Contracts.Status.ACTIVE);
    }

    @Override
    public Optional<Contracts> findActiveContractByRoomId(Integer roomId) {
        logger.info("Finding active contract by room ID: {}", roomId);
        return contractRepository.findActiveContractByRoomId(roomId, Contracts.Status.ACTIVE);
    }

    @Override
    public Long countContractsByOwnerId(Integer ownerId) {
        logger.info("Counting contracts by owner ID: {}", ownerId);
        return contractRepository.countByOwnerId(ownerId);
    }

    @Override
    public Long countContractsByOwnerIdAndStatus(Integer ownerId, Contracts.Status status) {
        logger.info("Counting contracts by owner ID: {} and status: {}", ownerId, status);
        return contractRepository.countByOwnerIdAndStatus(ownerId, status);
    }

    @Override
    public Float getTotalRevenueByOwnerId(Integer ownerId) {
        logger.info("Calculating total revenue for owner ID: {}", ownerId);
        Float revenue = contractRepository.getTotalRevenueByOwnerId(ownerId, Contracts.Status.ACTIVE);
        logger.info("Revenue calculated: {}", revenue);
        return revenue != null ? revenue : 0.0f;
    }

    @Override
    public List<Contracts> findContractsByOwnerCccd(String cccd) {
        logger.info("=== START FIND CONTRACTS BY OWNER CCCD ===");
        logger.info("Finding contracts for owner with CCCD: {}", cccd);

        if (cccd == null || cccd.trim().isEmpty()) {
            logger.warn("CCCD is null or empty");
            return Collections.emptyList();
        }

        Optional<UserCccd> userCccd = userCccdRepository.findByCccdNumber(cccd);
        if (userCccd.isEmpty()) {
            logger.warn("No UserCccd found with CCCD: {}", cccd);
            return Collections.emptyList();
        }

        Users owner = userCccd.get().getUser();
        if (owner == null || owner.getRole() != Users.Role.OWNER) {
            logger.warn("User with CCCD {} is not an owner or user is null", cccd);
            return Collections.emptyList();
        }

        logger.info("Owner found: {}", owner.getFullname());
        List<Contracts> contracts = contractRepository.findByOwnerId(owner.getUserId());
        logger.info("Found {} contracts for owner", contracts.size());
        contracts.forEach(contract -> {
            if (contract.getOwner() == null) {
                logger.error("Contract ID {} has null owner", contract.getContractId());
            }
        });
        logger.info("=== END FIND CONTRACTS BY OWNER CCCD ===");
        return contracts;
    }

    @Override
    public List<ContractListDto> getAllContractsForList() {
        logger.info("Getting all contracts for list view");
        try {
            List<Contracts> contracts = contractRepository.findAllOrderByContractDateDesc();
            return convertToContractListDto(contracts);
        } catch (Exception e) {
            logger.error("Error getting contracts for list: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi lấy danh sách hợp đồng: " + e.getMessage());
        }
    }

    @Override
    public List<ContractListDto> getContractsListByOwnerId(Integer ownerId) {
        logger.info("Getting contracts list for owner ID: {}", ownerId);
        try {
            List<Contracts> contracts = contractRepository.findByOwnerUserIdOrderByContractDateDesc(ownerId);
            return convertToContractListDto(contracts);
        } catch (Exception e) {
            logger.error("Error getting contracts list for owner ID {}: {}", ownerId, e.getMessage(), e);
            throw new RuntimeException("Lỗi khi lấy danh sách hợp đồng của chủ trọ: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateStatus(Integer contractId, String newStatusString) {
        logger.info("🔄 === SERVICE: UPDATE STATUS ===");
        logger.info("📝 Contract ID: {}", contractId);
        logger.info("📝 New Status String: '{}'", newStatusString);

        try {
            Contracts.Status newStatus;
            try {
                newStatus = Contracts.Status.valueOf(newStatusString.toUpperCase());
                logger.info("✅ Converted to enum: {}", newStatus);
            } catch (IllegalArgumentException e) {
                logger.error("❌ Status không hợp lệ: '{}'", newStatusString);
                logger.error("❌ Các status cho phép: {}", java.util.Arrays.toString(Contracts.Status.values()));
                throw new IllegalArgumentException("Status không hợp lệ: " + newStatusString +
                        ". Các giá trị cho phép: " + java.util.Arrays.toString(Contracts.Status.values()));
            }

            logger.info("🔍 Tìm hợp đồng với ID: {}", contractId);
            Optional<Contracts> contractOpt = contractRepository.findById(contractId);

            if (contractOpt.isEmpty()) {
                logger.error("❌ Không tìm thấy hợp đồng với ID: {}", contractId);
                throw new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId);
            }

            Contracts contract = contractOpt.get();
            logger.info("✅ Tìm thấy hợp đồng: ID={}, Status hiện tại={}",
                    contract.getContractId(), contract.getStatus());

            Contracts.Status oldStatus = contract.getStatus();
            contract.setStatus(newStatus);

            logger.info("🔄 Lưu hợp đồng với status mới...");
            Contracts savedContract = contractRepository.save(contract);

            logger.info("✅ Cập nhật thành công! {} -> {}",
                    oldStatus, savedContract.getStatus());

        } catch (IllegalArgumentException e) {
            logger.error("❌ IllegalArgumentException: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ Unexpected Exception: ", e);
            throw new RuntimeException("Lỗi cập nhật trạng thái hợp đồng: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Contracts getContractById(Integer contractId) {
        logger.info("🔍 Service: Tìm hợp đồng với ID: {}", contractId);
        return contractRepository.findById(contractId)
                .orElseThrow(() -> {
                    logger.warn("⚠️ Không tìm thấy hợp đồng với ID: {}", contractId);
                    return new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId);
                });
    }

    private Contracts.Status parseStatusFromString(String statusString) {
        logger.info("🔄 Parse status string: '{}'", statusString);

        try {
            switch (statusString.toUpperCase()) {
                case "DRAFT":
                case "BAN_NHAP":
                    return Contracts.Status.DRAFT;

                case "ACTIVE":
                case "DANG_THUE":
                case "HOAT_DONG":
                    return Contracts.Status.ACTIVE;

                case "TERMINATED":
                case "DA_HUY":
                case "CANCELLED":
                    return Contracts.Status.TERMINATED;

                case "EXPIRED":
                case "HET_HAN":
                    return Contracts.Status.EXPIRED;

                default:
                    return Contracts.Status.valueOf(statusString.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            logger.error("❌ Trạng thái không hợp lệ: '{}'", statusString);
            throw new RuntimeException("Trạng thái không hợp lệ: " + statusString +
                    ". Các trạng thái hợp lệ: DRAFT, ACTIVE, TERMINATED, EXPIRED");
        }
    }

    public List<String> getAllValidStatuses() {
        return java.util.Arrays.stream(Contracts.Status.values())
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean isValidStatus(String status) {
        try {
            Contracts.Status.valueOf(status.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private boolean canChangeStatus(Contracts.Status currentStatus, Contracts.Status newStatus) {
        logger.info("🔄 Kiểm tra chuyển đổi: {} -> {}", currentStatus, newStatus);

        if (currentStatus == null)
            return true;

        switch (currentStatus) {
            case DRAFT:
                return Arrays.asList(Contracts.Status.ACTIVE, Contracts.Status.TERMINATED)
                        .contains(newStatus);

            case ACTIVE:
                return Arrays.asList(Contracts.Status.TERMINATED, Contracts.Status.EXPIRED)
                        .contains(newStatus);

            case TERMINATED:
                return newStatus == Contracts.Status.DRAFT;

            case EXPIRED:
                return Arrays.asList(Contracts.Status.ACTIVE, Contracts.Status.TERMINATED)
                        .contains(newStatus);

            default:
                return false;
        }
    }

    private String getStatusLabel(Contracts.Status status) {
        if (status == null)
            return "Không xác định";

        switch (status) {
            case DRAFT:
                return "Bản nháp";
            case ACTIVE:
                return "Đang thuê";
            case TERMINATED:
                return "Đã hủy";
            case EXPIRED:
                return "Hết hạn";
            default:
                return status.name();
        }
    }

    private List<ContractListDto> convertToContractListDto(List<Contracts> contracts) {
        return contracts.stream()
                .map(contract -> {
                    ContractListDto dto = new ContractListDto();

                    dto.setContractId(contract.getContractId() != null
                            ? contract.getContractId().longValue()
                            : null);

                    dto.setStartDate(contract.getStartDate() != null
                            ? contract.getStartDate().toLocalDate()
                            : null);

                    dto.setTenantName(Optional.ofNullable(contract.getTenant())
                            .map(Users::getFullname)
                            .orElse(Optional.ofNullable(contract.getUnregisteredTenant())
                                    .map(UnregisteredTenants::getFullName)
                                    .orElse("Chưa xác định")));

                    LocalDate endDate = calculateEndDate(contract);
                    dto.setEndDate(endDate);

                    dto.setTenantPhone(getTenantPhone(contract));

                    dto.setStatus(Optional.ofNullable(contract.getStatus())
                            .map(Enum::toString)
                            .orElse("UNKNOWN"));

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private LocalDate calculateEndDate(Contracts contract) {
        if (contract.getEndDate() != null) {
            return contract.getEndDate().toLocalDate();
        }

        if (contract.getStartDate() != null && contract.getDuration() != null && contract.getDuration() > 0) {
            return contract.getStartDate().toLocalDate()
                    .plusMonths(contract.getDuration().intValue());
        }

        return null;
    }

    private String getTenantPhone(Contracts contract) {
        try {
            if (contract.getTenant() != null && contract.getTenant().getPhone() != null) {
                return contract.getTenant().getPhone();
            }

            if (contract.getUnregisteredTenant() != null && contract.getUnregisteredTenant().getPhone() != null) {
                return contract.getUnregisteredTenant().getPhone();
            }

            return "";

        } catch (Exception e) {
            logger.error("Error getting tenant phone for contract {}: {}",
                    contract.getContractId(), e.getMessage());
            return "";
        }
    }

    public Rooms findRoomByTenantId(Long tenantId) {
        Contracts contract = contractRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hợp đồng cho tenant ID: " + tenantId));

        if (contract.getStatus() != Contracts.Status.ACTIVE) {
            throw new ResourceNotFoundException("Hợp đồng không còn hiệu lực");
        }

        return contract.getRoom();
    }

    @Override
    public List<Contracts> getMyContracts() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        return contractRepository.findByOwnerId(user.getUserId());
    }

    private Users handleRegisteredTenant(ContractDto.Tenant tenantDto) {
        logger.info("=== SERVICE: Handling Registered Tenant ===");
        if (tenantDto.getPhone() == null || tenantDto.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại người thuê không được để trống!");
        }
        Users tenant = userRepository.findByPhone(tenantDto.getPhone())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người thuê với SĐT: " + tenantDto.getPhone()));

        return tenant;
    }

    private UnregisteredTenants handleUnregisteredTenant(
            ContractDto.UnregisteredTenant tenantDto,
            Users owner,
            MultipartFile cccdFrontFile,
            MultipartFile cccdBackFile) {
        logger.info("=== SERVICE: Handling Unregistered Tenant ===");
        if (tenantDto.getPhone() == null || tenantDto.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Số điện thoại người bảo hộ không được để trống!");
        }

        UnregisteredTenants unregisteredTenant = new UnregisteredTenants();
        unregisteredTenant.setUser(owner);
        unregisteredTenant.setFullName(tenantDto.getFullName());
        unregisteredTenant.setPhone(tenantDto.getPhone());
        unregisteredTenant.setCccdNumber(tenantDto.getCccdNumber());
        unregisteredTenant.setIssueDate(tenantDto.getIssueDate());
        unregisteredTenant.setIssuePlace(tenantDto.getIssuePlace());
        unregisteredTenant.setBirthday(tenantDto.getBirthday());

        StringBuilder newAddress = new StringBuilder();
        if (StringUtils.hasText(tenantDto.getStreet())) newAddress.append(tenantDto.getStreet());
        if (StringUtils.hasText(tenantDto.getWard())) newAddress.append(", ").append(tenantDto.getWard());
        if (StringUtils.hasText(tenantDto.getDistrict())) newAddress.append(", ").append(tenantDto.getDistrict());
        if (StringUtils.hasText(tenantDto.getProvince())) newAddress.append(", ").append(tenantDto.getProvince());
        if (!newAddress.toString().isEmpty()) {
            unregisteredTenant.setAddress(newAddress.toString());
        }

        if (cccdFrontFile != null && !cccdFrontFile.isEmpty()) {
            unregisteredTenant.setCccdFrontUrl(saveFile(cccdFrontFile));
        }
        if (cccdBackFile != null && !cccdBackFile.isEmpty()) {
            unregisteredTenant.setCccdBackUrl(saveFile(cccdBackFile));
        }
        unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);

        return unregisteredTenantsRepository.save(unregisteredTenant);
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

@Override
@Transactional
public Contracts createContract(ContractDto contractDto, String ownerCccd, Users tenant,
                                UnregisteredTenants unregisteredTenant) throws Exception {
    logger.info("=== BẮT ĐẦU TẠO HỢP ĐỒNG TỪ DTO (Legacy Method) ===");
    logger.info("Tạo hợp đồng mới từ DTO với loại người thuê: {}", contractDto.getTenantType());

    // 1. Lấy và xác thực Owner (Chủ trọ)
    Users owner = userRepository.findByCccdOrPhone(ownerCccd, null) // Giả định service.findByCccdOrPhone có thể dùng cho ownerCccd
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin chủ trọ với CCCD: " + ownerCccd));
    logger.info("Chủ trọ được tìm thấy: {}", owner.getFullname());

    // 2. Lấy và xác thực Phòng trọ
    Rooms room = roomRepository.findById(contractDto.getRoom().getRoomId())
            .orElseThrow(() -> new ResourceNotFoundException("Phòng trọ không tồn tại!"));
    
    if (room.getStatus() != RoomStatus.unactive) { // Chỉ cho phép tạo hợp đồng cho phòng đang "unactive" (trống)
        throw new IllegalStateException("Phòng này hiện đã được thuê hoặc không khả dụng.");
    }

    // 3. Xử lý tiện ích phòng (nếu có)
    if (contractDto.getRoom().getUtilityIds() != null && !contractDto.getRoom().getUtilityIds().isEmpty()) {
        Set<Utility> utilities = utilityRepository.findByUtilityIdIn(contractDto.getRoom().getUtilityIds());
        room.setUtilities(utilities);
        logger.info("SERVICE: Đã gán {} tiện ích cho phòng ID {}.", utilities.size(), room.getRoomId());
    } else {
        room.getUtilities().clear(); // Xóa tất cả tiện ích cũ nếu không có tiện ích mới được chọn
        logger.info("SERVICE: Đã xóa hết tiện ích cho phòng ID {}.", room.getRoomId());
    }

    // Xác định người thuê cuối cùng và số điện thoại của họ
    String finalTenantPhone = null;
    Users actualTenant = null;
    UnregisteredTenants actualUnregisteredTenant = null;

    if (unregisteredTenant != null && StringUtils.hasText(unregisteredTenant.getFullName())) {
        logger.info("SERVICE: Đang sử dụng thông tin Người bảo hộ/Thuê mới được cung cấp.");
        // Nếu unregisteredTenant đã được tạo/xử lý từ trước (ví dụ ở tầng Controller)
        // thì ta sử dụng đối tượng này.
        // Đảm bảo nó được lưu nếu chưa.
        if (unregisteredTenant.getId() == null) { // Nếu đây là một đối tượng mới chưa được lưu
             unregisteredTenant.setUser(owner); // Đảm bảo người bảo hộ được liên kết với chủ trọ
             if (unregisteredTenant.getStatus() == null) {
                unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE);
            }
            unregisteredTenantsRepository.save(unregisteredTenant);
        }
        actualUnregisteredTenant = unregisteredTenant;
        finalTenantPhone = actualUnregisteredTenant.getPhone();
    } else if (tenant != null) {
        logger.info("SERVICE: Đang sử dụng thông tin Người thuê đã đăng ký được cung cấp.");
        // Nếu tenant đã được tìm thấy/xử lý từ trước (ví dụ ở tầng Controller)
        actualTenant = tenant;
        finalTenantPhone = actualTenant.getPhone();
    } else {
        logger.error("Không có thông tin người thuê (đã đăng ký hoặc chưa đăng ký) hợp lệ.");
        throw new IllegalArgumentException("Phải cung cấp thông tin người thuê hợp lệ!");
    }


    // 4. Tạo và lưu đối tượng Hợp đồng
    Contracts contract = new Contracts();
    contract.setOwner(owner);
    contract.setRoom(room);
    contract.setTenant(actualTenant); // Gán người thuê đã đăng ký (có thể là null)
    contract.setUnregisteredTenant(actualUnregisteredTenant); // Gán người bảo hộ (có thể là null)
    contract.setTenantPhone(finalTenantPhone); // Gán số điện thoại của người thuê chính

    // Điền các thông tin còn lại từ DTO
    contract.setContractDate(Date.valueOf(contractDto.getContractDate()));
    contract.setStartDate(Date.valueOf(contractDto.getTerms().getStartDate()));
    contract.setEndDate(Date.valueOf(contractDto.getTerms().getEndDate()));
    contract.setPrice(contractDto.getTerms().getPrice().floatValue());
    contract.setDeposit(contractDto.getTerms().getDeposit().floatValue());
    contract.setDuration(Float.valueOf(contractDto.getTerms().getDuration()));
    contract.setStatus(Contracts.Status.valueOf(contractDto.getStatus().toUpperCase()));
    contract.setTerms(contractDto.getTerms().getTerms());
    contract.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));

    if (contractDto.getPaymentMethod() != null) {
        contract.setPaymentMethod(Contracts.PaymentMethod.valueOf(contractDto.getPaymentMethod().name()));
    }
    if (contractDto.getResidents() != null && !contractDto.getResidents().isEmpty()) {
        logger.info("SERVICE: Tìm thấy {} người ở. Đang xử lý...", contractDto.getResidents().size());
        for (ContractDto.ResidentDto residentDto : contractDto.getResidents()) {
            Resident resident = new Resident();
            resident.setFullName(residentDto.getFullName());
            resident.setBirthYear(residentDto.getBirthYear());
            resident.setPhone(residentDto.getPhone());
            resident.setCccdNumber(residentDto.getCccdNumber());
            resident.setContract(contract); // **QUAN TRỌNG**: Liên kết người ở với hợp đồng này
            
            contract.getResidents().add(resident); // Thêm vào danh sách của hợp đồng
        }
    }
    
    Contracts savedContract = contractRepository.save(contract);
    
    // 5. Cập nhật trạng thái phòng
    room.setStatus(RoomStatus.active);
    roomRepository.save(room);

    logger.info("SERVICE: Đã tạo hợp đồng ID {} thành công.", savedContract.getContractId());
    return savedContract;
}

  
}