package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import nhatroxanh.com.Nhatroxanh.Model.enity.Utility;
import lombok.RequiredArgsConstructor;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantDetailDTO;
import nhatroxanh.com.Nhatroxanh.Model.Dto.TenantInfoDTO;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts;
import nhatroxanh.com.Nhatroxanh.Model.enity.Contracts.Status;
import nhatroxanh.com.Nhatroxanh.Model.enity.ExtensionRequests;
import nhatroxanh.com.Nhatroxanh.Model.enity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.enity.Image;
import nhatroxanh.com.Nhatroxanh.Model.enity.IncidentReports;
import nhatroxanh.com.Nhatroxanh.Model.enity.Review;
import nhatroxanh.com.Nhatroxanh.Model.enity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.enity.UserCccd;
import nhatroxanh.com.Nhatroxanh.Model.enity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.ContractRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ContractsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.IncidentReportsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ReviewRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ImageRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ExtensionRequestRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.TenantService;
import nhatroxanh.com.Nhatroxanh.Service.FileUploadService;
import java.util.Map;
import java.util.Optional;
import java.io.IOException;
import java.sql.Date;
import nhatroxanh.com.Nhatroxanh.Model.enity.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {
    private final ContractsRepository contractsRepository;
    private final HostelRepository hostelRepository;

    @Autowired
    private ContractRepository contractRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomsRepository roomRepository;

    @Autowired
    private IncidentReportsRepository incidentReportsRepository;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ExtensionRequestRepository extensionRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<TenantInfoDTO> getTenantsForOwner(Integer ownerId, String keyword, Integer hostelId, Status status,
            Pageable pageable) {
        Page<Contracts> contractsPage = contractsRepository.findTenantsByOwnerWithFilters(ownerId, keyword, hostelId,
                status, pageable);
        return contractsPage.map(this::convertToTenantInfoDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hostel> getHostelsForOwner(Integer ownerId) {
        return hostelRepository.findByOwnerUserId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantDetailDTO getTenantDetailByContractId(Integer contractId) {
        Contracts contract = contractsRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));

        Users tenant = contract.getTenant();
        Rooms room = contract.getRoom();
        Hostel hostel = room.getHostel();
        UserCccd userCccd = tenant.getUserCccd();
        String cccdNumber = (userCccd != null) ? userCccd.getCccdNumber() : "Chưa có";
        String issuePlace = (userCccd != null) ? userCccd.getIssuePlace() : "Chưa có";

        return TenantDetailDTO.builder()
                .contractId(contract.getContractId())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .terms(contract.getTerms())
                .contractStatus(contract.getStatus().name())
                .roomName(room.getNamerooms())
                .hostelName(hostel.getName())
                .userFullName(tenant.getFullname())
                .userGender(tenant.getGender())
                .userPhone(tenant.getPhone())
                .userBirthday(tenant.getBirthday())
                .userCccdNumber(cccdNumber)
                .userCccdMasked(maskCccd(cccdNumber))
                .userIssuePlace(issuePlace)
                .build();
    }

    @Override
    @Transactional
    public void updateContractStatus(Integer contractId, Boolean newStatus) {
        Contracts contract = contractsRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng với ID: " + contractId));
        if (!newStatus) {
            contract.setEndDate(new java.sql.Date(System.currentTimeMillis()));
        }
        contract.setStatus(newStatus ? Contracts.Status.ACTIVE : Contracts.Status.EXPIRED);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantInfoDTO> findAllForTesting(Pageable pageable) {
        Page<Contracts> contractsPage = contractsRepository.findAll(pageable);
        return contractsPage.map(this::convertToTenantInfoDTO);
    }

    private TenantInfoDTO convertToTenantInfoDTO(Contracts contract) {
        Users tenant = contract.getTenant();
        Rooms room = contract.getRoom();
        Hostel hostel = room.getHostel();

        return new TenantInfoDTO(
                contract.getContractId(),
                tenant.getUserId(),
                tenant.getFullname(),
                tenant.getPhone(),
                hostel.getName(),
                room.getNamerooms(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getStatus() == Contracts.Status.ACTIVE);
    }

    private String maskCccd(String cccd) {
        if (cccd == null || cccd.length() < 7 || "Chưa có".equals(cccd)) {
            return cccd;
        }
        return cccd.substring(0, 3) + "******" + cccd.substring(cccd.length() - 3);
    }

    private Users getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<Contracts> getActiveContracts() {
        Users tenant = getCurrentUser();
        return contractRepository.findByTenantAndStatus(tenant, Contracts.Status.ACTIVE);
    }

    @Override
    public Page<Contracts> getContractHistory(Pageable pageable) {
        Users tenant = getCurrentUser();
        return contractRepository.findByTenant(tenant, pageable);
    }

    public Contracts getContractById(Integer contractId) {
        Users tenant = getCurrentUser();
        Optional<Contracts> contract = contractRepository.findById(contractId);
        if (contract.isPresent() && contract.get().getTenant().getUserId().equals(tenant.getUserId())) {
            return contract.get();
        }
        throw new RuntimeException("Contract not found or unauthorized access");
    }

    @Override
    public Map<String, Object> getQuickStats(Pageable pageable) {
        Users tenant = getCurrentUser();
        List<Contracts> activeContracts = getActiveContracts();
        Page<Contracts> pagedContracts = getContractHistory(pageable); // phân trang
        List<Contracts> allContracts = pagedContracts.getContent(); // chỉ lấy contracts ở page hiện tại

        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        double totalCostThisMonth = allContracts.stream()
                .filter(c -> c.getStartDate() != null && c.getEndDate() != null)
                .filter(c -> {
                    LocalDate start = c.getStartDate().toLocalDate();
                    LocalDate end = c.getEndDate().toLocalDate();
                    return (start.getYear() <= currentYear && end.getYear() >= currentYear)
                            && (start.getMonthValue() <= currentMonth && end.getMonthValue() >= currentMonth);
                })
                .mapToDouble(c -> {
                    LocalDate start = c.getStartDate().toLocalDate();
                    LocalDate end = c.getEndDate().toLocalDate();
                    LocalDate monthStart = LocalDate.of(currentYear, currentMonth, 1);
                    LocalDate monthEnd = monthStart.withDayOfMonth(monthStart.lengthOfMonth());

                    LocalDate effectiveStart = start.isBefore(monthStart) ? monthStart : start;
                    LocalDate effectiveEnd = end.isAfter(monthEnd) ? monthEnd : end;

                    long daysInMonth = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd.plusDays(1));
                    long totalDaysInMonth = monthStart.lengthOfMonth();
                    return c.getPrice() * ((double) daysInMonth / totalDaysInMonth);
                })
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeRooms", activeContracts.size());
        stats.put("totalRentals", pagedContracts.getTotalElements() - activeContracts.size());
        stats.put("totalCostThisMonth", Math.round(totalCostThisMonth));

        return stats;
    }

    @Transactional
    public void returnRoom(Integer contractId, Date returnDate, String reason) {
        Contracts contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        Users tenant = getCurrentUser();
        if (!contract.getTenant().getUserId().equals(tenant.getUserId())) {
            throw new RuntimeException("Unauthorized action");
        }

        contract.setEndDate(returnDate);
        contract.setStatus(Contracts.Status.TERMINATED);
        contract.setReturnReason(reason);

        if (contract.getRoom() != null) {
            contract.getRoom().setStatus(RoomStatus.unactive);
        }

        contractRepository.save(contract);
    }

    @Transactional
    public void submitReview(Integer roomId, Double rating, String comment) {
        Users tenant = getCurrentUser();
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        Review review = Review.builder()
                .user(tenant)
                .room(room)
                .rating(rating)
                .comment(comment)
                .build();

        reviewRepository.save(review);
    }

    @Override
    public List<IncidentReports> getIncidentReportsByRoom(Integer roomId) {
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        return incidentReportsRepository.findByRoom(room);
    }

    @Override
    @Transactional
    public void submitIncidentReport(Integer roomId, String incidentType, String description,
            IncidentReports.IncidentLevel level) {
        Users tenant = getCurrentUser();
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        IncidentReports report = IncidentReports.builder()
                .room(room)
                .user(tenant)
                .incidentType(incidentType)
                .description(description)
                .level(level)
                .status(IncidentReports.IncidentStatus.CHUA_XU_LY)
                .reportedAt(new Date(System.currentTimeMillis()))
                .build();

        incidentReportsRepository.save(report);
    }

    @Override
    @Transactional
    public void extendContract(Integer contractId, Date newEndDate, String message) {
        Contracts contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        Users tenant = getCurrentUser();
        if (!contract.getTenant().getUserId().equals(tenant.getUserId())) {
            throw new RuntimeException("Unauthorized action");
        }

        contract.setEndDate(newEndDate);
        contract.setTerms(contract.getTerms() + "\nGia hạn: " + message);
        contractRepository.save(contract);
    }

    public Set<Utility> getUtilitiesByRoomId(Integer roomId) {
        return roomRepository.findUtilitiesByRoomId(roomId);
    }

    @Transactional
    public void createIncidentReport(Integer roomId, String incidentType, String description, String level,
            List<MultipartFile> images, CustomUserDetails userDetails) throws Exception {

        Users tenant = userDetails.getUser();
        Rooms room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));

        IncidentReports.IncidentLevel incidentLevel = IncidentReports.IncidentLevel.valueOf(level.toUpperCase());

        IncidentReports report = IncidentReports.builder()
                .room(room)
                .user(tenant)
                .incidentType(incidentType)
                .description(description)
                .level(incidentLevel)
                .status(IncidentReports.IncidentStatus.CHUA_XU_LY)
                .reportedAt(new Date(System.currentTimeMillis()))
                .images(new ArrayList<>()) // khởi tạo trước để thêm
                .build();

        report = incidentReportsRepository.save(report);

        if (images != null && !images.isEmpty()) {
            List<Image> uploadedImages = uploadReportImages(images, report);
            report.setImages(uploadedImages);
        }

        incidentReportsRepository.save(report);
    }

    public void updateIncidentReport(Integer reportId, Integer roomId, String incidentType,
            String description, String level, List<MultipartFile> images,
            List<Integer> imageIdsToDelete,
            CustomUserDetails userDetails) throws IOException {

        IncidentReports report = incidentReportsRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy báo cáo."));

        if (!report.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new SecurityException("Bạn không có quyền chỉnh sửa báo cáo này.");
        }

        report.setIncidentType(incidentType);
        report.setDescription(description);
        report.setLevel(IncidentReports.IncidentLevel.valueOf(level.toUpperCase()));
        report.setReportedAt(new java.sql.Date(System.currentTimeMillis()));

        // Xóa ảnh cũ nếu có chọn xoá
        if (imageIdsToDelete != null && !imageIdsToDelete.isEmpty()) {
            List<Image> imagesToRemove = report.getImages().stream()
                    .filter(img -> imageIdsToDelete.contains(img.getId()))
                    .collect(Collectors.toList());

            for (Image img : imagesToRemove) {
                // Xoá bản ghi và có thể xoá file vật lý (nếu cần)
                imageRepository.delete(img);
            }

            // Loại ảnh đã xoá khỏi danh sách trong entity
            report.getImages().removeAll(imagesToRemove);
        }

        // Thêm ảnh mới nếu có
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    String imageUrl = fileUploadService.uploadFile(image, "");
                    Image img = Image.builder()
                            .url(imageUrl)
                            .report(report)
                            .build();
                    report.getImages().add(img);
                }
            }
        }

        incidentReportsRepository.save(report);
    }

    private List<Image> uploadReportImages(List<MultipartFile> images, IncidentReports report) throws Exception {
        List<Image> imageList = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (images.size() > 8) {
            throw new IllegalArgumentException("Không được tải lên quá 8 ảnh");
        }

        for (int i = 0; i < images.size(); i++) {
            MultipartFile imageFile = images.get(i);
            if (!imageFile.isEmpty()) {
                try {
                    if (imageFile.getSize() > 10 * 1024 * 1024) {
                        errors.add("Ảnh " + (i + 1) + " quá lớn (tối đa 10MB)");
                        continue;
                    }

                    String imageUrl = fileUploadService.uploadFile(imageFile, "");
                    Image image = Image.builder()
                            .url(imageUrl)
                            .report(report)
                            .build();
                    imageList.add(imageRepository.save(image));
                } catch (Exception e) {
                    errors.add("Lỗi khi upload ảnh " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        if (!errors.isEmpty() && imageList.isEmpty()) {
            throw new Exception("Không thể upload ảnh nào: " + String.join(", ", errors));
        }

        if (!errors.isEmpty()) {
            System.out.println("Một số ảnh không thể upload: " + String.join(", ", errors));
        }

        return imageList;
    }

    @Override
    @Transactional
    public void createExtensionRequest(Integer contractId, LocalDate requestedExtendDate, String message, Users tenant) {
        Contracts contract = getContractById(contractId);

        if (contract.getTenant() == null || !contract.getTenant().getUserId().equals(tenant.getUserId())) {
            throw new RuntimeException("Bạn không có quyền thực hiện yêu cầu này.");
        }

        // Kiểm tra trùng yêu cầu đang chờ
        boolean alreadyRequested = extensionRequestRepository
                .existsByContract_ContractIdAndStatus(contractId, ExtensionRequests.RequestStatus.PENDING);
        if (alreadyRequested) {
            throw new RuntimeException("Bạn đã gửi yêu cầu gia hạn trước đó và đang chờ xử lý.");
        }

        ExtensionRequests request = ExtensionRequests.builder()
                .contract(contract)
                .requestedExtendDate(Date.valueOf(requestedExtendDate))
                .message(message)
                .status(ExtensionRequests.RequestStatus.PENDING)
                .createdAt(Date.valueOf(LocalDate.now()))
                .build();

        extensionRequestRepository.save(request);
    }

}
