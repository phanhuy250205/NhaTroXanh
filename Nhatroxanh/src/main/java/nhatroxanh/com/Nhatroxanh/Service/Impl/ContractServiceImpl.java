package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractListDto;
import nhatroxanh.com.Nhatroxanh.Model.enity.Address;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.UnregisteredTenants;
import nhatroxanh.com.Nhatroxanh.Repository.ContractRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserCccdRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UnregisteredTenantsRepository;
import nhatroxanh.com.Nhatroxanh.Service.ContractService;
import org.apache.hc.core5.annotation.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContractServiceImpl implements ContractService {

    private static final Logger logger = LoggerFactory.getLogger(ContractServiceImpl.class);

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
            Contracts.Status status, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant, Integer duration) throws Exception {
        logger.info("=== START CREATE CONTRACT ===");
        logger.info("Creating new contract for tenant phone: {}", tenantPhone);
        logger.info("Input parameters - roomId: {}, contractDate: {}, startDate: {}, endDate: {}, price: {}, deposit: {}, duration: {}, status: {}",
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
        logger.info("Tenant/unregisteredTenant provided: tenant={}, unregisteredTenant={}", tenant != null, unregisteredTenant != null);

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
        Optional<Contracts> activeContract = contractRepository.findActiveContractByRoomId(roomId, Contracts.Status.ACTIVE);
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
            contract.setTenant(null);
            logger.info("Unregistered tenant saved and set to contract");
        } else if (tenant != null) {
            logger.info("Handling registered tenant: {}", tenant.getFullname());
            if (tenant.getRole() != Users.Role.CUSTOMER) {
                logger.error("User with ID {} is not a customer", tenant.getUserId());
                throw new IllegalArgumentException("Người dùng không phải là khách thuê!");
            }
            contract.setTenant(tenant);
            logger.info("Registered tenant set to contract");
        }

        logger.info("Saving contract to repository");
        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract created successfully: {}", savedContract.getContractId());
        logger.info("=== END CREATE CONTRACT ===");
        return savedContract;
    }

    @Override
    @Transactional
    public Contracts createContract(ContractDto contractDto, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant) throws Exception {
        logger.info("=== START CREATE CONTRACT FROM DTO ===");
        logger.info("Creating new contract from DTO with tenant type: {}", contractDto.getTenantType());
        logger.info("Contract DTO received: {}", contractDto);

        // Validate DTO
        if (contractDto.getTenantType() == null || (!"REGISTERED".equals(contractDto.getTenantType()) && !"UNREGISTERED".equals(contractDto.getTenantType()))) {
            logger.error("Invalid tenant type: {}", contractDto.getTenantType());
            throw new IllegalArgumentException("Loại người thuê không hợp lệ!");
        }
        logger.info("Tenant type validated: {}", contractDto.getTenantType());

        if (contractDto.getContractDate() == null) {
            logger.error("Contract date is null");
            throw new IllegalArgumentException("Ngày lập hợp đồng không được null!");
        }
        logger.info("Contract date validated: {}", contractDto.getContractDate());

        if (contractDto.getTerms().getStartDate() == null || contractDto.getTerms().getEndDate() == null) {
            logger.error("Start date or end date is null, startDate: {}, endDate: {}", contractDto.getTerms().getStartDate(), contractDto.getTerms().getEndDate());
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được null!");
        }
        logger.info("Start and end dates validated: {}, {}", contractDto.getTerms().getStartDate(), contractDto.getTerms().getEndDate());

        if (contractDto.getTerms().getPrice() == null || contractDto.getTerms().getPrice() <= 0) {
            logger.error("Invalid price: {}", contractDto.getTerms().getPrice());
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0!");
        }
        logger.info("Price validated: {}", contractDto.getTerms().getPrice());

        if (contractDto.getTerms().getDeposit() == null || contractDto.getTerms().getDeposit() < 0) {
            logger.error("Invalid deposit: {}", contractDto.getTerms().getDeposit());
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn hoặc bằng 0!");
        }
        logger.info("Deposit validated: {}", contractDto.getTerms().getDeposit());

        if (contractDto.getTerms().getTerms() != null && contractDto.getTerms().getTerms().length() > 255) {
            logger.error("Terms too long: {}", contractDto.getTerms().getTerms().length());
            throw new IllegalArgumentException("Điều khoản không được vượt quá 255 ký tự!");
        }
        logger.info("Terms validated: {}", contractDto.getTerms().getTerms());

        if (contractDto.getTerms().getDuration() != null && contractDto.getTerms().getDuration() <= 0) {
            logger.error("Invalid duration: {}", contractDto.getTerms().getDuration());
            throw new IllegalArgumentException("Thời hạn hợp đồng phải lớn hơn 0!");
        }
        logger.info("Duration validated: {}", contractDto.getTerms().getDuration());

        // Find room
        logger.info("Searching for room with name: {}", contractDto.getRoom().getRoomName());
        Integer roomId = roomRepository.findByRoomNumber(String.valueOf(contractDto.getRoom().getRoomName()))
                .map(Rooms::getRoomId)
                .orElseThrow(() -> {
                    logger.error("Room not found: {}", contractDto.getRoom().getRoomName());
                    return new IllegalArgumentException("Phòng không tồn tại!");
                });
        logger.info("Room ID found: {}", roomId);

        String tenantPhone = contractDto.getTenantType().equals("REGISTERED") ? contractDto.getTenant().getPhone() : contractDto.getUnregisteredTenant().getPhone();
        logger.info("Tenant phone extracted: {}", tenantPhone);

        Contracts contract = createContract(
                tenantPhone,
                roomId,
                Date.valueOf(contractDto.getContractDate()),
                Date.valueOf(contractDto.getTerms().getStartDate()),
                Date.valueOf(contractDto.getTerms().getEndDate()),
                contractDto.getTerms().getPrice().floatValue(),
                contractDto.getTerms().getDeposit().floatValue(),
                contractDto.getTerms().getTerms(),
                Contracts.Status.valueOf(contractDto.getStatus().toUpperCase()),
                ownerCccd,
                tenant,
                unregisteredTenant,
                contractDto.getTerms().getDuration());
        logger.info("=== END CREATE CONTRACT FROM DTO ===");
        return contract;
    }

    @Override
    @Transactional
    public Contracts updateContract(Integer contractId, Contracts updatedContract) throws Exception {
        logger.info("=== START UPDATE CONTRACT ===");
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
            contract.setUnregisteredTenant(null);
            logger.info("Tenant updated: {}", tenant.getFullname());
        }
        if (updatedContract.getUnregisteredTenant() != null && updatedContract.getUnregisteredTenant().getId() != null) {
            logger.info("Updating unregistered tenant with ID: {}", updatedContract.getUnregisteredTenant().getId());
            UnregisteredTenants unregisteredTenantUpdate = unregisteredTenantsRepository.findById(updatedContract.getUnregisteredTenant().getId())
                    .orElseThrow(() -> {
                        logger.error("Unregistered tenant not found: {}", updatedContract.getUnregisteredTenant().getId());
                        return new Exception("Người thuê chưa đăng ký không tồn tại!");
                    });
            if (unregisteredTenantUpdate.getStatus() == null) {
                unregisteredTenantUpdate.setStatus(UnregisteredTenants.Status.ACTIVE);
                unregisteredTenantsRepository.save(unregisteredTenantUpdate);
            }
            contract.setUnregisteredTenant(unregisteredTenantUpdate);
            contract.setTenant(null);
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
        logger.info("Updating contract with ID: {} from DTO with tenant type: {}", contractId, contractDto.getTenantType());
        logger.info("Contract DTO received: {}", contractDto);

        if (contractId == null || contractId <= 0) {
            logger.error("Invalid contract ID: {}", contractId);
            throw new IllegalArgumentException("ID hợp đồng không hợp lệ!");
        }

        if (contractDto.getTenantType() == null || (!"REGISTERED".equals(contractDto.getTenantType()) && !"UNREGISTERED".equals(contractDto.getTenantType()))) {
            logger.error("Invalid tenant type: {}", contractDto.getTenantType());
            throw new IllegalArgumentException("Loại người thuê không hợp lệ!");
        }
        logger.info("Tenant type validated: {}", contractDto.getTenantType());

        Optional<Contracts> existingContract = contractRepository.findById(contractId);
        if (!existingContract.isPresent()) {
            logger.error("Contract not found: {}", contractId);
            throw new Exception("Hợp đồng không tồn tại!");
        }

        Contracts contract = existingContract.get();
        logger.info("Current contract: {}", contract);

        contract.setTenantPhone(contractDto.getTenantType().equals("REGISTERED") ? contractDto.getTenant().getPhone() : contractDto.getUnregisteredTenant().getPhone());
        logger.info("Updated tenant phone: {}", contract.getTenantPhone());

        contract.setContractDate(Date.valueOf(contractDto.getContractDate()));
        logger.info("Updated contract date: {}", contract.getContractDate());

        contract.setStartDate(Date.valueOf(contractDto.getTerms().getStartDate()));
        logger.info("Updated start date: {}", contract.getStartDate());

        contract.setEndDate(Date.valueOf(contractDto.getTerms().getEndDate()));
        logger.info("Updated end date: {}", contract.getEndDate());

        contract.setPrice(contractDto.getTerms().getPrice().floatValue());
        logger.info("Updated price: {}", contract.getPrice());

        contract.setDeposit(contractDto.getTerms().getDeposit().floatValue());
        logger.info("Updated deposit: {}", contract.getDeposit());

        contract.setTerms(contractDto.getTerms().getTerms());
        logger.info("Updated terms: {}", contract.getTerms());

        contract.setStatus(Contracts.Status.valueOf(contractDto.getStatus().toUpperCase()));
        logger.info("Updated status: {}", contract.getStatus());

        contract.setDuration(Float.valueOf(contractDto.getTerms().getDuration()));
        logger.info("Updated duration: {}", contract.getDuration());

        logger.info("Searching for owner with CCCD: {}", contractDto.getOwner().getCccdNumber());
        Optional<UserCccd> ownerCccdOpt = userCccdRepository.findByCccdNumber(contractDto.getOwner().getCccdNumber());
        Users owner = ownerCccdOpt.map(UserCccd::getUser)
                .orElseThrow(() -> {
                    logger.error("Owner not found with CCCD: {}", contractDto.getOwner().getCccdNumber());
                    return new IllegalArgumentException("Chủ trọ không tồn tại!");
                });
        contract.setOwner(owner);
        logger.info("Owner updated: {}", owner.getFullname());

        if ("UNREGISTERED".equals(contractDto.getTenantType())) {
            logger.info("Creating new unregistered tenant from DTO");
            UnregisteredTenants unregisteredTenantUpdate = new UnregisteredTenants();
            unregisteredTenantUpdate.setUser(owner);
            unregisteredTenantUpdate.setFullName(contractDto.getUnregisteredTenant().getFullName());
            unregisteredTenantUpdate.setPhone(contractDto.getUnregisteredTenant().getPhone());
            unregisteredTenantUpdate.setCccdNumber(contractDto.getUnregisteredTenant().getCccdNumber());
            unregisteredTenantUpdate.setIssueDate(contractDto.getUnregisteredTenant().getIssueDate());
            unregisteredTenantUpdate.setIssuePlace(contractDto.getUnregisteredTenant().getIssuePlace());
            unregisteredTenantUpdate.setBirthday(contractDto.getUnregisteredTenant().getBirthday());
            unregisteredTenantUpdate.setCccdFrontUrl(contractDto.getUnregisteredTenant().getCccdFrontUrl());
            unregisteredTenantUpdate.setCccdBackUrl(contractDto.getUnregisteredTenant().getCccdBackUrl());
            unregisteredTenantUpdate.setStatus(UnregisteredTenants.Status.ACTIVE);
            Address address = new Address();
            address.setStreet(contractDto.getUnregisteredTenant().getStreet());
            unregisteredTenantUpdate.setAddress(address);
            unregisteredTenantsRepository.save(unregisteredTenantUpdate);
            contract.setUnregisteredTenant(unregisteredTenantUpdate);
            contract.setTenant(null);
            logger.info("Unregistered tenant created and set: {}", unregisteredTenantUpdate.getFullName());
        } else {
            logger.info("Finding tenant with phone: {}", contractDto.getTenant().getPhone());
            Optional<Users> tenantOpt = userRepository.findByPhone(contractDto.getTenant().getPhone());
            Users tenantUpdate = tenantOpt.orElseThrow(() -> {
                logger.error("Tenant not found with phone: {}", contractDto.getTenant().getPhone());
                return new IllegalArgumentException("Người thuê không tồn tại!");
            });
            contract.setTenant(tenantUpdate);
            contract.setUnregisteredTenant(null);
            logger.info("Tenant updated: {}", tenantUpdate.getFullname());
        }

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
        return contractRepository.findByTenantName(name);
    }

    @Override
    public List<Contracts> findContractsByTenantPhone(String phone) {
        logger.info("Finding contracts by tenant phone: {}", phone);
        return contractRepository.findByTenantPhone(phone);
    }

    @Override
    public List<Contracts> findContractsByTenantCccd(String cccd) {
        logger.info("Finding contracts by tenant CCCD: {}", cccd);
        return contractRepository.findByTenantCccd(cccd);
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
    public void updateStatus(Long contractId, String newStatusString) {
        logger.info("🔄 === SERVICE: UPDATE STATUS ===");
        logger.info("📝 Contract ID: {}", contractId);
        logger.info("📝 New Status String: '{}'", newStatusString);

        try {
            // 🔄 CONVERT STRING TO ENUM
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

            // 🔍 TÌM HỢP ĐỒNG
            logger.info("🔍 Tìm hợp đồng với ID: {}", contractId);
            Optional<Contracts> contractOpt = contractRepository.findById(Math.toIntExact(contractId));

            if (contractOpt.isEmpty()) {
                logger.error("❌ Không tìm thấy hợp đồng với ID: {}", contractId);
                throw new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId);
            }

            Contracts contract = contractOpt.get();
            logger.info("✅ Tìm thấy hợp đồng: ID={}, Status hiện tại={}",
                    contract.getContractId(), contract.getStatus());

            // 🔄 CẬP NHẬT STATUS
            Contracts.Status oldStatus = contract.getStatus();
            contract.setStatus(newStatus);

            logger.info("🔄 Lưu hợp đồng với status mới...");
            Contracts savedContract = contractRepository.save(contract);

            logger.info("✅ Cập nhật thành công! {} -> {}",
                    oldStatus, savedContract.getStatus());

        } catch (IllegalArgumentException e) {
            logger.error("❌ IllegalArgumentException: {}", e.getMessage());
            throw e; // Re-throw để Controller xử lý

        } catch (Exception e) {
            logger.error("❌ Lỗi trong service updateStatus: ", e);
            throw new RuntimeException("Lỗi cập nhật trạng thái hợp đồng: " + e.getMessage(), e);
        }
    }
    // 🔍 TÌM HỢP ĐỒNG THEO ID
    @Override
    @Transactional(readOnly = true)
    public Contracts getContractById(Long contractId) {
        logger.info("🔍 Service: Tìm hợp đồng với ID: {}", contractId);
        return contractRepository.findById(Math.toIntExact(contractId))
                .orElseThrow(() -> {
                    logger.warn("⚠️ Không tìm thấy hợp đồng với ID: {}", contractId);
                    return new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId);
                });
    }

    // 🔄 CHUYỂN ĐỔI STRING THÀNH ENUM STATUS
    private Contracts.Status parseStatusFromString(String statusString) {
        logger.info("🔄 Parse status string: '{}'", statusString);

        try {
            // Chuyển đổi các giá trị phổ biến
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
                    // Thử parse trực tiếp từ enum
                    return Contracts.Status.valueOf(statusString.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            logger.error("❌ Trạng thái không hợp lệ: '{}'", statusString);
            throw new RuntimeException("Trạng thái không hợp lệ: " + statusString +
                    ". Các trạng thái hợp lệ: DRAFT, ACTIVE, TERMINATED, EXPIRED");
        }
    }

    // 🔧 METHOD BỔ SUNG
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



    // 🔄 KIỂM TRA LOGIC CHUYỂN ĐỔI TRẠNG THÁI
    private boolean canChangeStatus(Contracts.Status currentStatus, Contracts.Status newStatus) {
        logger.info("🔄 Kiểm tra chuyển đổi: {} -> {}", currentStatus, newStatus);

        if (currentStatus == null) return true;

        // Logic chuyển đổi trạng thái theo enum
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
    // 🏷️ LẤY NHÃN TRẠNG THÁI TIẾNG VIỆT
    private String getStatusLabel(Contracts.Status status) {
        if (status == null) return "Không xác định";

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

                    // ID hợp đồng
                    dto.setContractId(contract.getContractId() != null
                            ? contract.getContractId().longValue()
                            : null);

                    // Ngày bắt đầu
                    dto.setStartDate(contract.getStartDate() != null
                            ? contract.getStartDate().toLocalDate()
                            : null);

                    // Tên khách thuê
                    dto.setTenantName(Optional.ofNullable(contract.getTenant())
                            .map(Users::getFullname)
                            .orElse(Optional.ofNullable(contract.getUnregisteredTenant())
                                    .map(UnregisteredTenants::getFullName)
                                    .orElse("Chưa xác định")));

                    // Ngày kết thúc
                    LocalDate endDate = calculateEndDate(contract);
                    dto.setEndDate(endDate);

                    // Số điện thoại
                    dto.setTenantPhone(getTenantPhone(contract));

                    // Trạng thái
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
            // Kiểm tra tenant đã đăng ký
            if (contract.getTenant() != null && contract.getTenant().getPhone() != null) {
                return contract.getTenant().getPhone();
            }

            // Kiểm tra unregistered tenant
            if (contract.getUnregisteredTenant() != null && contract.getUnregisteredTenant().getPhone() != null) {
                return contract.getUnregisteredTenant().getPhone();
            }

            return ""; // Trả về chuỗi rỗng nếu không có số điện thoại

        } catch (Exception e) {
            logger.error("Error getting tenant phone for contract {}: {}",
                    contract.getContractId(), e.getMessage());
            return "";
        }
    }


}