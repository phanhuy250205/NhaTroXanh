package nhatroxanh.com.Nhatroxanh.Service.Impl;

import nhatroxanh.com.Nhatroxanh.Model.Dto.ContractDto;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
            Contracts.Status status, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant) throws Exception {
        logger.info("Creating new contract for tenant with phone: {}", tenantPhone);

        // Validate inputs
        if (tenantPhone == null || tenantPhone.trim().isEmpty()) {
            logger.error("Tenant phone is null or empty");
            throw new IllegalArgumentException("Số điện thoại khách thuê không được để trống!");
        }
        if (roomId == null || roomId <= 0) {
            logger.error("Invalid room ID: {}", roomId);
            throw new IllegalArgumentException("ID phòng không hợp lệ!");
        }
        if (contractDate == null) {
            logger.error("Contract date is null");
            throw new IllegalArgumentException("Ngày lập hợp đồng không được null!");
        }
        if (startDate == null || endDate == null) {
            logger.error("Start date or end date is null");
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được null!");
        }
        if (price == null || price <= 0) {
            logger.error("Invalid price: {}", price);
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0!");
        }
        if (deposit == null || deposit < 0) {
            logger.error("Invalid deposit: {}", deposit);
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn hoặc bằng 0!");
        }
        if (terms != null && terms.length() > 255) {
            logger.error("Terms too long");
            throw new IllegalArgumentException("Điều khoản không được vượt quá 255 ký tự!");
        }
        if (tenant == null && unregisteredTenant == null) {
            logger.error("No tenant or unregistered tenant provided");
            throw new IllegalArgumentException("Phải cung cấp thông tin người thuê (đã đăng ký hoặc chưa đăng ký)!");
        }

        // Find owner
        Optional<UserCccd> ownerCccdOpt = userCccdRepository.findByCccdNumber(ownerCccd);
        Users owner = ownerCccdOpt.map(UserCccd::getUser)
                .orElseThrow(() -> {
                    logger.error("Owner not found with CCCD: {}", ownerCccd);
                    return new IllegalArgumentException("Chủ trọ không tồn tại!");
                });
        if (owner.getRole() != Users.Role.OWNER) {
            logger.error("User with CCCD {} is not an owner", ownerCccd);
            throw new IllegalArgumentException("Người dùng không phải là chủ trọ!");
        }

        // Find room
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    logger.error("Room not found: {}", roomId);
                    return new IllegalArgumentException("Phòng không tồn tại!");
                });

        // Check for active contract on room
        Optional<Contracts> activeContract = contractRepository.findActiveContractByRoomId(roomId, Contracts.Status.ACTIVE);
        if (activeContract.isPresent()) {
            logger.error("Room {} has an active contract", roomId);
            throw new Exception("Phòng hiện đang có hợp đồng active!");
        }

        // Create contract
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
                .build();

        // Handle tenant
        if (unregisteredTenant != null) {
            if (unregisteredTenant.getStatus() == null) {
                unregisteredTenant.setStatus(UnregisteredTenants.Status.ACTIVE); // Gán trạng thái mặc định
            }
            unregisteredTenantsRepository.save(unregisteredTenant);
            contract.setUnregisteredTenant(unregisteredTenant);
            contract.setTenant(null); // Đảm bảo tenant là null khi dùng unregisteredTenant
        } else if (tenant != null) {
            if (tenant.getRole() != Users.Role.CUSTOMER) {
                logger.error("User with ID {} is not a customer", tenant.getUserId());
                throw new IllegalArgumentException("Người dùng không phải là khách thuê!");
            }
            contract.setTenant(tenant);
        }

        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract created successfully: {}", savedContract.getContractId());
        return savedContract;
    }

    @Override
    @Transactional
    public Contracts createContract(ContractDto contractDto, String ownerCccd, Users tenant, UnregisteredTenants unregisteredTenant) throws Exception {
        logger.info("Creating new contract from DTO with tenant type: {}", contractDto.getTenantType());

        // Validate DTO
        if (contractDto.getTenantType() == null || (!"REGISTERED".equals(contractDto.getTenantType()) && !"UNREGISTERED".equals(contractDto.getTenantType()))) {
            logger.error("Invalid tenant type: {}", contractDto.getTenantType());
            throw new IllegalArgumentException("Loại người thuê không hợp lệ!");
        }
        if (contractDto.getContractDate() == null) {
            logger.error("Contract date is null");
            throw new IllegalArgumentException("Ngày lập hợp đồng không được null!");
        }
        if (contractDto.getTerms().getStartDate() == null || contractDto.getTerms().getEndDate() == null) {
            logger.error("Start date or end date is null");
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được null!");
        }
        if (contractDto.getTerms().getPrice() == null || contractDto.getTerms().getPrice() <= 0) {
            logger.error("Invalid price: {}", contractDto.getTerms().getPrice());
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0!");
        }
        if (contractDto.getTerms().getDeposit() == null || contractDto.getTerms().getDeposit() < 0) {
            logger.error("Invalid deposit: {}", contractDto.getTerms().getDeposit());
            throw new IllegalArgumentException("Tiền đặt cọc phải lớn hơn hoặc bằng 0!");
        }
        if (contractDto.getTerms().getTerms() != null && contractDto.getTerms().getTerms().length() > 255) {
            logger.error("Terms too long");
            throw new IllegalArgumentException("Điều khoản không được vượt quá 255 ký tự!");
        }

        // Find room
        Integer roomId = roomRepository.findByRoomNumber(String.valueOf(contractDto.getRoom().getRoomName()))
                .map(Rooms::getRoomId)
                .orElseThrow(() -> {
                    logger.error("Room not found: {}", contractDto.getRoom().getRoomName());
                    return new IllegalArgumentException("Phòng không tồn tại!");
                });

        String tenantPhone = contractDto.getTenantType().equals("REGISTERED") ? contractDto.getTenant().getPhone() : contractDto.getUnregisteredTenant().getPhone();

        return createContract(
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
                unregisteredTenant);
    }

    @Override
    @Transactional
    public Contracts updateContract(Integer contractId, Contracts updatedContract) throws Exception {
        logger.info("Updating contract with ID: {}", contractId);

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
        logger.debug("Current contract: {}", contract);

        if (updatedContract.getTenantPhone() != null && !updatedContract.getTenantPhone().isEmpty()) {
            contract.setTenantPhone(updatedContract.getTenantPhone());
        }
        if (updatedContract.getContractDate() != null) {
            contract.setContractDate(updatedContract.getContractDate());
        } else {
            logger.error("Contract date is null for contract ID: {}", contractId);
            throw new IllegalArgumentException("Ngày lập hợp đồng không được null!");
        }
        if (updatedContract.getStartDate() != null) {
            contract.setStartDate(updatedContract.getStartDate());
        } else {
            logger.error("Start date is null for contract ID: {}", contractId);
            throw new IllegalArgumentException("Ngày bắt đầu không được null!");
        }
        if (updatedContract.getEndDate() != null) {
            contract.setEndDate(updatedContract.getEndDate());
        } else {
            logger.error("End date is null for contract ID: {}", contractId);
            throw new IllegalArgumentException("Ngày kết thúc không được null!");
        }
        if (updatedContract.getPrice() != null && updatedContract.getPrice() > 0) {
            contract.setPrice(updatedContract.getPrice());
        } else {
            logger.error("Invalid price: {} for contract ID: {}", updatedContract.getPrice(), contractId);
            throw new IllegalArgumentException("Giá thuê phải lớn hơn 0!");
        }
        if (updatedContract.getDeposit() != null && updatedContract.getDeposit() >= 0) {
            contract.setDeposit(updatedContract.getDeposit());
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
        }
        if (updatedContract.getStatus() != null) {
            contract.setStatus(updatedContract.getStatus());
        }

        if (updatedContract.getRoom() != null && updatedContract.getRoom().getRoomId() != null) {
            Rooms room = roomRepository.findById(updatedContract.getRoom().getRoomId())
                    .orElseThrow(() -> {
                        logger.error("Room not found: {}", updatedContract.getRoom().getRoomId());
                        return new IllegalArgumentException("Phòng không tồn tại!");
                    });
            contract.setRoom(room);
        }
        if (updatedContract.getTenant() != null && updatedContract.getTenant().getUserId() != null) {
            Users tenant = userRepository.findById(updatedContract.getTenant().getUserId())
                    .orElseThrow(() -> {
                        logger.error("Tenant not found: {}", updatedContract.getTenant().getUserId());
                        return new Exception("Người thuê không tồn tại!");
                    });
            contract.setTenant(tenant);
            contract.setUnregisteredTenant(null);
        }
        if (updatedContract.getUnregisteredTenant() != null && updatedContract.getUnregisteredTenant().getId() != null) {
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
        }
        if (updatedContract.getOwner() != null && updatedContract.getOwner().getUserId() != null) {
            Users owner = userRepository.findById(updatedContract.getOwner().getUserId())
                    .orElseThrow(() -> {
                        logger.error("Owner not found: {}", updatedContract.getOwner().getUserId());
                        return new Exception("Chủ trọ không tồn tại!");
                    });
            contract.setOwner(owner);
        }

        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract updated successfully: {}", savedContract.getContractId());
        return savedContract;
    }

    @Override
    @Transactional
    public Contracts updateContract(Integer contractId, ContractDto contractDto) throws Exception {
        logger.info("Updating contract with ID: {} from DTO with tenant type: {}", contractId, contractDto.getTenantType());

        if (contractId == null || contractId <= 0) {
            logger.error("Invalid contract ID: {}", contractId);
            throw new IllegalArgumentException("ID hợp đồng không hợp lệ!");
        }
        if (contractDto.getTenantType() == null || (!"REGISTERED".equals(contractDto.getTenantType()) && !"UNREGISTERED".equals(contractDto.getTenantType()))) {
            logger.error("Invalid tenant type: {}", contractDto.getTenantType());
            throw new IllegalArgumentException("Loại người thuê không hợp lệ!");
        }

        Optional<Contracts> existingContract = contractRepository.findById(contractId);
        if (!existingContract.isPresent()) {
            logger.error("Contract not found: {}", contractId);
            throw new Exception("Hợp đồng không tồn tại!");
        }

        Contracts contract = existingContract.get();
        contract.setTenantPhone(contractDto.getTenantType().equals("REGISTERED") ? contractDto.getTenant().getPhone() : contractDto.getUnregisteredTenant().getPhone());
        contract.setContractDate(Date.valueOf(contractDto.getContractDate()));
        contract.setStartDate(Date.valueOf(contractDto.getTerms().getStartDate()));
        contract.setEndDate(Date.valueOf(contractDto.getTerms().getEndDate()));
        contract.setPrice(contractDto.getTerms().getPrice().floatValue());
        contract.setDeposit(contractDto.getTerms().getDeposit().floatValue());
        contract.setTerms(contractDto.getTerms().getTerms());
        contract.setStatus(Contracts.Status.valueOf(contractDto.getStatus().toUpperCase()));

        Optional<UserCccd> ownerCccdOpt = userCccdRepository.findByCccdNumber(contractDto.getOwner().getCccdNumber());
        Users owner = ownerCccdOpt.map(UserCccd::getUser)
                .orElseThrow(() -> {
                    logger.error("Owner not found with CCCD: {}", contractDto.getOwner().getCccdNumber());
                    return new IllegalArgumentException("Chủ trọ không tồn tại!");
                });
        contract.setOwner(owner);

        if ("UNREGISTERED".equals(contractDto.getTenantType())) {
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
            unregisteredTenantUpdate.setStatus(UnregisteredTenants.Status.ACTIVE); // Gán trạng thái mặc định
            Address address = new Address();
            address.setStreet(contractDto.getUnregisteredTenant().getAddress().getStreet());
            unregisteredTenantUpdate.setAddress(address);
            unregisteredTenantsRepository.save(unregisteredTenantUpdate);
            contract.setUnregisteredTenant(unregisteredTenantUpdate);
            contract.setTenant(null);
        } else {
            Optional<Users> tenantOpt = userRepository.findByPhone(contractDto.getTenant().getPhone());
            Users tenantUpdate = tenantOpt.orElseThrow(() -> {
                logger.error("Tenant not found with phone: {}", contractDto.getTenant().getPhone());
                return new IllegalArgumentException("Người thuê không tồn tại!");
            });
            contract.setTenant(tenantUpdate);
            contract.setUnregisteredTenant(null);
        }

        Contracts savedContract = contractRepository.save(contract);
        logger.info("Contract updated successfully: {}", savedContract.getContractId());
        return savedContract;
    }

    @Override
    @Transactional
    public void deleteContract(Integer contractId) throws Exception {
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

        contractRepository.delete(contract.get());
        logger.info("Contract deleted successfully: {}", contractId);
    }

    @Override
    public Optional<Contracts> findContractById(Integer contractId) {
        return contractRepository.findById(contractId);
    }

    @Override
    public List<Contracts> findContractsByRoomId(Integer roomId) {
        return contractRepository.findByRoomId(roomId);
    }

    @Override
    public List<Contracts> findContractsByTenantUserId(Integer tenantUserId) {
        return contractRepository.findByTenantUserId(tenantUserId);
    }

    @Override
    public List<Contracts> findContractsByOwnerId(Integer ownerId) {
        return contractRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Contracts> findContractsByStatus(Contracts.Status status) {
        return contractRepository.findByStatus(status);
    }

    @Override
    public List<Contracts> findContractsByTenantName(String name) {
        return contractRepository.findByTenantName(name);
    }

    @Override
    public List<Contracts> findContractsByTenantPhone(String phone) {
        return contractRepository.findByTenantPhone(phone);
    }

    @Override
    public List<Contracts> findContractsByTenantCccd(String cccd) {
        return contractRepository.findByTenantCccd(cccd);
    }

    @Override
    public List<Contracts> findContractsByDateRange(Date startDate, Date endDate) {
        return contractRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public List<Contracts> findContractsExpiringWithin30Days() {
        LocalDate now = LocalDate.now();
        LocalDate threshold = now.plusDays(30);
        Date sqlThreshold = Date.valueOf(threshold);
        return contractRepository.findByEndDateLessThanEqualAndStatus(sqlThreshold, Contracts.Status.ACTIVE);
    }

    @Override
    public Optional<Contracts> findActiveContractByRoomId(Integer roomId) {
        return contractRepository.findActiveContractByRoomId(roomId, Contracts.Status.ACTIVE);
    }

    @Override
    public Long countContractsByOwnerId(Integer ownerId) {
        return contractRepository.countByOwnerId(ownerId);
    }

    @Override
    public Long countContractsByOwnerIdAndStatus(Integer ownerId, Contracts.Status status) {
        return contractRepository.countByOwnerIdAndStatus(ownerId, status);
    }

    @Override
    public Float getTotalRevenueByOwnerId(Integer ownerId) {
        Float revenue = contractRepository.getTotalRevenueByOwnerId(ownerId, Contracts.Status.ACTIVE);
        return revenue != null ? revenue : 0.0f;
    }

    @Override
    public List<Contracts> findContractsByOwnerCccd(String cccd) {
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

        List<Contracts> contracts = contractRepository.findByOwnerId(owner.getUserId());
        contracts.forEach(contract -> {
            if (contract.getOwner() == null) {
                logger.error("Contract ID {} has null owner", contract.getContractId());
            }
        });

        return contracts;
    }
}