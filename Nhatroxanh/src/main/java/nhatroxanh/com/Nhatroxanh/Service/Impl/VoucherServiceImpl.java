package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import nhatroxanh.com.Nhatroxanh.Model.Dto.VoucherDTO;
import nhatroxanh.com.Nhatroxanh.Model.entity.Hostel;
import nhatroxanh.com.Nhatroxanh.Model.entity.Notification;
import nhatroxanh.com.Nhatroxanh.Model.entity.Rooms;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Model.entity.Vouchers;
import nhatroxanh.com.Nhatroxanh.Repository.HostelRepository;
import nhatroxanh.com.Nhatroxanh.Repository.RoomsRepository;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import nhatroxanh.com.Nhatroxanh.Repository.VoucherRepository;
import nhatroxanh.com.Nhatroxanh.Repository.NotificationRepository;
import nhatroxanh.com.Nhatroxanh.Security.CustomUserDetails;
import nhatroxanh.com.Nhatroxanh.Service.EmailService;
import nhatroxanh.com.Nhatroxanh.Service.HostelService;
import nhatroxanh.com.Nhatroxanh.Service.VoucherService;
import nhatroxanh.com.Nhatroxanh.Util.VoucherCodeGenerator;

@Service
@Transactional
public class VoucherServiceImpl implements VoucherService {

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HostelService hostelService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public Page<Vouchers> getActiveVouchers(Pageable pageable) {
        return voucherRepository.findByStatusTrue(pageable);
    }

    @Override
    public Page<Vouchers> getVouchersByStatus(Boolean status, Pageable pageable) {
        return voucherRepository.findByStatus(status, pageable);
    }

    @Override
    public Page<Vouchers> getVouchersByUserId(Integer userId, Pageable pageable) {
        return voucherRepository.findByUserId(userId, pageable);
    }

    @Override
    public List<Vouchers> getActiveVouchersByHostelId(Integer hostelId) {
        return voucherRepository.findActiveVouchersByHostelId(hostelId);
    }

    @Override
    public boolean isVoucherCodeExists(String code) {
        return voucherRepository.existsByCode(code);
    }

    @Override
    public String generateUniqueVoucherCode() {
        String code;
        do {
            code = VoucherCodeGenerator.generateVoucherCode();
        } while (voucherRepository.existsByCode(code));
        return code;
    }

    @Override
    public Vouchers createVoucher(VoucherDTO dto, CustomUserDetails userDetails) {
        if (voucherRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại!");
        }

        Users user = userRepository.findById(userDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại!"));

        Vouchers voucher = Vouchers.builder()
                .code(dto.getCode())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .discountValue(dto.getDiscountValue().floatValue())
                .startDate(Date.valueOf(dto.getStartDate()))
                .endDate(Date.valueOf(dto.getEndDate()))
                .quantity(dto.getQuantity())
                .minAmount(dto.getMinAmount() != null ? dto.getMinAmount().floatValue() : null)
                .user(user)
                .status(true)
                .createdAt(Date.valueOf(LocalDate.now()))
                .build();

        return voucherRepository.save(voucher);
    }

    @Override
    public Vouchers updateVoucher(Integer id, VoucherDTO dto, CustomUserDetails userDetails) {
        Vouchers existing = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (!existing.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa!");
        }

        if (!existing.getCode().equals(dto.getCode()) &&
                voucherRepository.existsByCode(dto.getCode())) {
            throw new RuntimeException("Mã voucher đã tồn tại!");
        }

        existing.setCode(dto.getCode());
        existing.setTitle(dto.getTitle());
        existing.setDescription(dto.getDescription());
        existing.setDiscountValue(dto.getDiscountValue().floatValue());
        existing.setStartDate(Date.valueOf(dto.getStartDate()));
        existing.setEndDate(Date.valueOf(dto.getEndDate()));
        existing.setQuantity(dto.getQuantity());
        existing.setMinAmount(dto.getMinAmount() != null ? dto.getMinAmount().floatValue() : null);

        Date today = new java.sql.Date(System.currentTimeMillis());
        boolean isExpired = existing.getEndDate().before(today);
        boolean isOut = existing.getQuantity() == 0;
        existing.setStatus(!isExpired && !isOut);

        return voucherRepository.save(existing);
    }

    @Override
    public void deleteVoucher(Integer id, CustomUserDetails userDetails) {
        Vouchers voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Voucher không tồn tại!"));

        if (!voucher.getUser().getUserId().equals(userDetails.getUserId())) {
            throw new RuntimeException("Bạn không có quyền xóa voucher này!");
        }

        voucherRepository.delete(voucher);
    }

    @Override
    public Optional<Vouchers> getVoucherById(Integer id) {
        return voucherRepository.findById(id);
    }

    @Override
    public Page<Vouchers> searchVouchers(Users user, String keyword, Pageable pageable) {
        return voucherRepository.searchVouchers(user, keyword, pageable);
    }

    @Override
    public void createVoucherHost(Vouchers voucher, Integer ownerId) {
        Hostel hostel = voucher.getHostel();

        if (hostel == null) {
            throw new IllegalArgumentException("Khu trọ là bắt buộc.");
        }
        if (!hostel.getOwner().getUserId().equals(ownerId)) {
            throw new IllegalArgumentException("Khu trọ không thuộc quyền quản lý của bạn.");
        }

        if (voucherRepository.existsByCode(voucher.getCode())) {
            throw new IllegalArgumentException("Mã voucher đã tồn tại.");
        }

        if (voucher.getDiscountValue() <= 0 || voucher.getQuantity() <= 0) {
            throw new IllegalArgumentException("Giá trị và số lượng phải lớn hơn 0.");
        }

        if (voucher.getStartDate().after(voucher.getEndDate())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }

        voucherRepository.save(voucher);
    }

    @Override
    public List<Vouchers> getVouchersByOwnerId(Integer ownerId) {
        return voucherRepository.findByUserUserId(ownerId);
    }

    @Override
    public Page<Vouchers> getVouchersByOwnerIdWithFilters(Integer ownerId, String searchQuery, String statusFilter,
            Pageable pageable) {
        String normalizedSearchQuery = (searchQuery != null && !searchQuery.isEmpty())
                ? "%" + searchQuery.toLowerCase() + "%"
                : null;
        Boolean status = null;
        if ("active".equalsIgnoreCase(statusFilter)) {
            status = true;
        } else if ("expired".equalsIgnoreCase(statusFilter)) {
            status = false;
        }
        return voucherRepository.findByUserUserIdWithFilters(ownerId, normalizedSearchQuery, status, pageable);
    }

    @Override
    public void deleteVoucherByIdHost(Integer voucherId, Integer ownerId) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại."));

        if (!voucher.getUser().getUserId().equals(ownerId)) {
            throw new SecurityException("Bạn không có quyền xóa voucher này.");
        }

        voucherRepository.deleteById(voucherId);
    }

    @Override
    public Vouchers getVoucherByIdAndHost(Integer voucherId, Integer hostId) {
        Vouchers voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new IllegalArgumentException("Voucher không tồn tại"));
        if (!voucher.getUser().getUserId().equals(hostId)) {
            throw new SecurityException("Không có quyền truy cập voucher này.");
        }
        return voucher;
    }

    @Override
    public void updateVoucherHost(Integer voucherId, Integer hostId, String title, String code,
            Integer hostelId, Float discountValue, Integer quantity, Float minAmount,
            Date startDate, Date endDate, String description, Boolean status) {

        Vouchers voucher = getVoucherByIdAndHost(voucherId, hostId);

        // Kiểm tra mã voucher mới nếu khác với mã cũ
        String oldCode = voucher.getCode() != null ? voucher.getCode().trim().toLowerCase() : "";
        String newCode = code != null ? code.trim().toLowerCase() : "";
        if (!oldCode.equals(newCode)) {
            if (voucherRepository.existsByCodeAndNotId(newCode, voucherId)) {
                throw new IllegalArgumentException("Mã voucher đã tồn tại. Vui lòng chọn mã khác.");
            }
            voucher.setCode(code.trim());
        }

        // Kiểm tra khu trọ
        Hostel hostel = hostelService.getHostelById(hostelId)
                .orElseThrow(() -> new IllegalArgumentException("Khu trọ không tồn tại."));
        if (!hostel.getOwner().getUserId().equals(hostId)) {
            throw new IllegalArgumentException("Khu trọ không thuộc quyền quản lý của bạn.");
        }
        voucher.setHostel(hostel);

        // Cập nhật các trường khác
        voucher.setTitle(title != null ? title.trim() : voucher.getTitle());
        voucher.setDiscountValue(discountValue != null ? discountValue : voucher.getDiscountValue());
        voucher.setQuantity(quantity != null ? quantity : voucher.getQuantity());
        voucher.setMinAmount(minAmount != null ? minAmount : voucher.getMinAmount());
        voucher.setStartDate(startDate != null ? startDate : voucher.getStartDate());
        voucher.setEndDate(endDate != null ? endDate : voucher.getEndDate());
        voucher.setDescription(description != null ? description.trim() : voucher.getDescription());
        voucher.setStatus(status != null ? status : voucher.getStatus());

        voucherRepository.save(voucher);
    }

    @Override
    public boolean existsByCodeAndNotId(String code, Integer id) {
        return voucherRepository.existsByCodeAndNotId(code, id);
    }

    @Override
    public boolean existsByCode(String code) {
        return voucherRepository.existsByCode(code);
    }

    @Override
    public void checkAndDeactivateVouchersIfNeeded() {
        List<Vouchers> activeVouchers = voucherRepository.findByStatus(true);
        Date today = new Date(System.currentTimeMillis());
        for (Vouchers voucher : activeVouchers) {
            boolean isExpired = voucher.getEndDate().before(today);
            boolean isDepleted = voucher.getQuantity() != null && voucher.getQuantity() <= 0;
            if (isExpired || isDepleted) {
                voucher.setStatus(false);
                voucherRepository.save(voucher);
                String reason = isExpired && isDepleted ? "Voucher đã hết hạn và số lượng đã về 0."
                        : isExpired ? "Voucher đã hết hạn." : "Số lượng voucher đã về 0.";
                Users owner = voucher.getHostel() != null ? voucher.getHostel().getOwner() : voucher.getUser();
                if (owner != null && owner.getEmail() != null) {
                    emailService.sendVoucherDeactivatedEmail(
                            owner.getEmail(),
                            owner.getFullname(),
                            voucher.getTitle(),
                            reason);
                }
            }
        }
    }

    @Override
    public Page<Vouchers> searchVouchersByStatus(Users user, String keyword, Boolean status, Pageable pageable) {
        return voucherRepository.searchVouchersByStatus(user, keyword, status, pageable);
    }

    @Override
    public void sendVoucherNotification(Vouchers voucher, CustomUserDetails userDetails) {
        List<Users> recipients;
        if (voucher.getHostel() != null) {
            recipients = userRepository.findByHostelIdAndRole(voucher.getHostel().getHostelId(), Users.Role.CUSTOMER);
        } else {
            recipients = userRepository.findByRole(Users.Role.CUSTOMER);
        }

        for (Users recipient : recipients) {
            Notification notification = new Notification();
            notification.setUser(recipient);
            notification.setTitle("Khuyến mãi mới: " + voucher.getTitle());
            notification.setMessage("Sử dụng mã voucher " + voucher.getCode() + " để được giảm "
                    + voucher.getDiscountValue() + " VNĐ cho đơn tối thiểu " + voucher.getMinAmount()
                    + " VNĐ. Hạn sử dụng đến " + voucher.getEndDate() + ".");
            notification.setType(Notification.NotificationType.PROMOTION);
            notification.setIsRead(false);
            notification.setCreateAt(new java.sql.Timestamp(System.currentTimeMillis()));
            notificationRepository.save(notification);

            if (recipient.getEmail() != null && !recipient.getEmail().isEmpty()) {
                try {
                    String emailContent = buildVoucherEmailContent(recipient, voucher);
                    emailService.sendHtmlEmail(
                            recipient.getEmail(),
                            "🎁 " + voucher.getTitle() + " - Mã khuyến mãi độc quyền từ Nhà Trọ Xanh",
                            emailContent);
                } catch (Exception e) {

                }
            }
        }
    }

    private String buildVoucherEmailContent(Users recipient, Vouchers voucher) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String endDate = dateFormat.format(voucher.getEndDate());

        // Định dạng tiền tệ với dấu phân cách hàng nghìn
        DecimalFormat currencyFormat = new DecimalFormat("###,###,###");
        String formattedDiscount = currencyFormat.format(voucher.getDiscountValue()) + " VNĐ";
        String formattedMinAmount = currencyFormat.format(voucher.getMinAmount()) + " VNĐ";

        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <style>" +
                "        body { font-family: Arial, sans-serif; line-height: 1.6; margin: 0; padding: 0; }" +
                "        .ticket { max-width: 600px; margin: 20px auto; border-radius: 10px; overflow: hidden; box-shadow: 0 0 20px rgba(0,0,0,0.1); }"
                +
                "        .header { background-color: #3e83cc; color: white; padding: 20px; text-align: center; }" +
                "        .header h1 { margin: 0; font-size: 24px; }" +
                "        .content { padding: 30px; background-color: #f9f9f9; }" +
                "        .voucher-code { background-color: white; border: 2px dashed #3e83cc; padding: 15px; text-align: center; margin: 20px 0; font-size: 24px; font-weight: bold; color: #3e83cc; border-radius: 5px; }"
                +
                "        .details { background-color: white; border-radius: 5px; padding: 20px; margin-bottom: 20px; }"
                +
                "        .detail-row { display: flex; margin-bottom: 10px; }" +
                "        .detail-label { font-weight: bold; color: #555; width: 150px; }" +
                "        .footer { background-color: #3e83cc; color: white; padding: 15px; text-align: center; font-size: 12px; }"
                +
                "        .discount-value { font-size: 28px; color: #3e83cc; font-weight: bold; text-align: center; margin: 15px 0; }"
                +
                "    </style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"ticket\">" +
                "        <div class=\"header\">" +
                "            <h1>ƯU ĐÃI ĐẶC BIỆT</h1>" +
                "            <p>Dành riêng cho khách hàng thân thiết</p>" +
                "        </div>" +
                "        <div class=\"content\">" +
                "            <p>Xin chào <strong>" + recipient.getFullname() + "</strong>,</p>" +
                "            <p>Nhà Trọ Xanh gửi tặng bạn mã giảm giá đặc biệt:</p>" +
                "            " +
                "            <div class=\"discount-value\">GIẢM " + formattedDiscount + "</div>" +
                "            " +
                "            <div class=\"voucher-code\">" + voucher.getCode() + "</div>" +
                "            " +
                "            <div class=\"details\">" +
                "                <div class=\"detail-row\">" +
                "                    <span class=\"detail-label\">Tiêu đề:</span>" +
                "                    <span>" + voucher.getTitle() + "</span>" +
                "                </div>" +
                "                <div class=\"detail-row\">" +
                "                    <span class=\"detail-label\">Đơn tối thiểu:</span>" +
                "                    <span>" + formattedMinAmount + "</span>" +
                "                </div>" +
                "                <div class=\"detail-row\">" +
                "                    <span class=\"detail-label\">Hạn sử dụng:</span>" +
                "                    <span>" + endDate + "</span>" +
                "                </div>" +
                "            </div>" +
                "            " +
                "            <p>Hãy sử dụng mã này khi thanh toán để nhận ưu đãi đặc biệt này!</p>" +
                "            <p>Trân trọng,<br>Đội ngũ Nhà Trọ Xanh</p>" +
                "        </div>" +
                "        <div class=\"footer\">" +
                "            Đây là email tự động, vui lòng không trả lời. Mọi thắc mắc xin liên hệ hỗ trợ khách hàng."
                +
                "        </div>" +
                "    </div>" +
                "</body>" +
                "</html>";
    }

    @Override
    public Vouchers getVoucherByCode(String code) {
        return voucherRepository.findByCode(code); // Giả sử có method findByCode trong VoucherRepository
    }

    @Override
    public void updateVoucherQuantity(Vouchers voucher) {
        if (voucher.getQuantity() <= 0) {
            voucher.setStatus(false);
        }
        voucherRepository.save(voucher);
    }
}