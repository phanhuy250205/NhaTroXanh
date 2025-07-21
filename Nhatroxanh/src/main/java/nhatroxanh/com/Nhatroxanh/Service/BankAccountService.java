package nhatroxanh.com.Nhatroxanh.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nhatroxanh.com.Nhatroxanh.Model.entity.Users;
import nhatroxanh.com.Nhatroxanh.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final UserRepository userRepository;

    /**
     * Lấy thông tin tài khoản ngân hàng của nhân viên đang hoạt động để tạo VietQR
     */
    public BankAccountInfo getStaffBankAccount() {
        try {
            log.info("Searching for active staff with complete bank account information for QR generation");
            
            // Tìm nhân viên đang hoạt động có thông tin ngân hàng đầy đủ
            Optional<Users> staffOpt = userRepository.findFirstActiveStaffWithCompleteBankInfo(
                Users.Role.STAFF, true);
            
            if (staffOpt.isPresent()) {
                Users staff = staffOpt.get();
                
                log.info("Found active staff with bank account: {} - Bank: {} - Account: {}", 
                    staff.getFullname(), staff.getBankName(), 
                    staff.getBankAccount() != null ? staff.getBankAccount().substring(0, 4) + "****" : "null");
                
                // Convert bank ID to numeric format if needed
                String numericBankId = convertToNumericBankId(staff.getBankId());
                
                return new BankAccountInfo(
                    numericBankId,
                    staff.getBankAccount(),
                    staff.getAccountHolderName(),
                    staff.getBankName()
                );
            }
            
            // Fallback: thử tìm bằng phương pháp cũ
            log.warn("No active staff with complete bank account found, trying fallback method");
            Optional<Users> fallbackStaffOpt = userRepository.findByRoleAndEnabledAndBankAccountIsNotNull(
                Users.Role.STAFF, true);
            
            if (fallbackStaffOpt.isPresent()) {
                Users staff = fallbackStaffOpt.get();
                
                // Kiểm tra thông tin ngân hàng đầy đủ
                if (staff.getBankId() != null && !staff.getBankId().trim().isEmpty() &&
                    staff.getBankAccount() != null && !staff.getBankAccount().trim().isEmpty() &&
                    staff.getAccountHolderName() != null && !staff.getAccountHolderName().trim().isEmpty()) {
                    
                    log.info("Using fallback staff with bank account: {} - Bank: {}", 
                        staff.getFullname(), staff.getBankName());
                    
                    // Convert bank ID to numeric format if needed
                    String numericBankId = convertToNumericBankId(staff.getBankId());
                    
                    return new BankAccountInfo(
                        numericBankId,
                        staff.getBankAccount(),
                        staff.getAccountHolderName(),
                        staff.getBankName()
                    );
                }
            }
            
            // Cuối cùng: sử dụng thông tin mặc định nếu không tìm thấy staff nào
            log.warn("No staff with complete bank account info found, using default configuration");
            return getDefaultBankAccount();
            
        } catch (Exception e) {
            log.error("Error getting staff bank account: {}", e.getMessage(), e);
            log.warn("Falling back to default bank account due to error");
            return getDefaultBankAccount();
        }
    }

    /**
     * Lấy danh sách tất cả nhân viên đang hoạt động có thông tin ngân hàng đầy đủ
     */
    public List<BankAccountInfo> getAllActiveStaffBankAccounts() {
        try {
            log.info("Getting all active staff bank accounts");
            
            List<Users> activeStaff = userRepository.findActiveStaffWithCompleteBankInfo(
                Users.Role.STAFF, true);
            
            List<BankAccountInfo> bankAccounts = activeStaff.stream()
                .map(staff -> new BankAccountInfo(
                    staff.getBankId(),
                    staff.getBankAccount(),
                    staff.getAccountHolderName(),
                    staff.getBankName()
                ))
                .toList();
            
            log.info("Found {} active staff with complete bank account information", bankAccounts.size());
            return bankAccounts;
            
        } catch (Exception e) {
            log.error("Error getting all active staff bank accounts: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Thông tin tài khoản ngân hàng mặc định - sử dụng cấu hình từ application.properties
     */
    private BankAccountInfo getDefaultBankAccount() {
        log.warn("Using default bank account configuration - please configure active staff bank accounts");
        
        // Sử dụng cấu hình từ application.properties nếu có
        try {
            // Lấy cấu hình từ environment hoặc sử dụng giá trị mặc định
            String defaultBankId = System.getProperty("vietqr.bankId", "970422"); // MB Bank
            String defaultAccountNo = System.getProperty("vietqr.accountNo", "0123456789");
            String defaultAccountName = System.getProperty("vietqr.accountName", "CONG TY TNHH NHA TRO XANH");
            String defaultBankName = getBankNameById(defaultBankId);
            
            log.info("Using default bank account: {} - {} - {}", defaultBankName, defaultBankId, 
                defaultAccountNo.substring(0, Math.min(4, defaultAccountNo.length())) + "****");
            
            return new BankAccountInfo(
                defaultBankId,
                defaultAccountNo,
                defaultAccountName,
                defaultBankName
            );
        } catch (Exception e) {
            log.error("Error getting default bank account configuration: {}", e.getMessage());
            // Fallback to hardcoded values
            return new BankAccountInfo(
                "970422", // MB Bank - commonly supported
                "0123456789",
                "CONG TY TNHH NHA TRO XANH",
                "MB Bank"
            );
        }
    }

    /**
     * Validate bank ID for VietQR compatibility
     */
    public boolean isValidBankId(String bankId) {
        if (bankId == null || bankId.trim().isEmpty()) {
            return false;
        }
        
        // List of valid Vietnamese bank IDs for VietQR
        String[] validBankIds = {
            "970405", // Agribank
            "970406", // DongA Bank
            "970407", // Techcombank
            "970408", // GPBank
            "970409", // BacA Bank
            "970412", // PVcomBank
            "970414", // OceanBank
            "970415", // Vietinbank
            "970416", // ACB
            "970418", // BIDV
            "970422", // MB Bank
            "970423", // TPBank
            "970424", // Shinhan Bank
            "970425", // ABBank
            "970426", // MSB
            "970427", // VietABank
            "970428", // Nam A Bank
            "970429", // SCB
            "970430", // PGBank
            "970431", // Eximbank
            "970432", // VPBank
            "970433", // VietBank
            "970436", // Vietcombank
            "970437", // HDBank
            "970438", // BVBank
            "970439", // LienVietPostBank
            "970440", // SeABank
            "970441", // VIB
            "970442", // HDB
            "970443", // SHB
            "970448", // OCB
            "970449", // UOB
            "970454", // VietCapitalBank
            "970455", // IBK - HCMC
            "970456", // CAKE by VPBank
            "970458", // UOB Vietnam
            "970463", // HSBC Vietnam
            "970466", // KienLongBank
            "970467", // COOPBANK
            "970468", // BaoVietBank
            "970469", // NCB
        };
        
        String trimmedBankId = bankId.trim();
        for (String validId : validBankIds) {
            if (validId.equals(trimmedBankId)) {
                return true;
            }
        }
        
        log.warn("Invalid bank ID: {}. Please use a valid Vietnamese bank ID.", bankId);
        return false;
    }

    /**
     * Convert bank ID from short code to numeric format for VietQR
     */
    private String convertToNumericBankId(String bankId) {
        if (bankId == null || bankId.trim().isEmpty()) {
            log.warn("Bank ID is null or empty, using default");
            return "970436"; // Default to Vietcombank
        }
        
        String trimmedBankId = bankId.trim().toUpperCase();
        
        // If already numeric format, return as is
        if (trimmedBankId.matches("^\\d{6}$")) {
            log.info("Bank ID {} is already in numeric format", trimmedBankId);
            return trimmedBankId;
        }
        
        // Convert short codes to numeric format
        switch (trimmedBankId) {
            case "MB": return "970422"; // MB Bank
            case "VCB": case "VIETCOMBANK": return "970436"; // Vietcombank
            case "BIDV": return "970418"; // BIDV
            case "VTB": case "VIETINBANK": return "970415"; // Vietinbank
            case "ACB": return "970416"; // ACB
            case "TCB": case "TECHCOMBANK": return "970407"; // Techcombank
            case "VPB": case "VPBANK": return "970432"; // VPBank
            case "TPB": case "TPBANK": return "970423"; // TPBank
            case "STB": case "SACOMBANK": return "970403"; // Sacombank
            case "HDB": case "HDBANK": return "970437"; // HDBank
            case "VIB": return "970441"; // VIB
            case "SHB": return "970443"; // SHB
            case "EIB": case "EXIMBANK": return "970431"; // Eximbank
            case "MSB": return "970426"; // MSB
            case "SEAB": case "SEABANK": return "970440"; // SeABank
            case "OCB": return "970448"; // OCB
            case "LPB": case "LIENVIETPOSTBANK": return "970439"; // LienVietPostBank
            case "ABB": case "ABBANK": return "970425"; // ABBank
            case "VAB": case "VIETNAMABANK": return "970427"; // VietABank
            case "NAB": case "NAMABANK": return "970428"; // Nam A Bank
            case "PGB": case "PGBANK": return "970430"; // PGBank
            case "AGRI": case "AGRIBANK": return "970405"; // Agribank
            case "GPB": case "GPBANK": return "970408"; // GPBank
            case "BACA": case "BACABANK": return "970409"; // BacA Bank
            case "PVC": case "PVCOMBANK": return "970412"; // PVcomBank
            case "OJB": case "OCEANBANK": return "970414"; // OceanBank
            case "SHBVN": case "SHINHAN": return "970424"; // Shinhan Bank
            case "SCB": return "970429"; // SCB
            case "VIETBANK": return "970433"; // VietBank
            case "BVB": case "BAOVIETBANK": return "970468"; // BaoVietBank
            case "COOPBANK": return "970467"; // COOPBANK
            case "KLB": case "KIENLONGBANK": return "970466"; // KienLongBank
            case "NCB": return "970469"; // NCB
            case "UOB": return "970458"; // UOB Vietnam
            case "HSBC": return "970463"; // HSBC Vietnam
            case "IBK": return "970455"; // IBK - HCMC
            case "CAKE": return "970456"; // CAKE by VPBank
            case "VIETCAP": case "VIETCAPITALBANK": return "970454"; // VietCapitalBank
            default:
                log.warn("Unknown bank ID: {}, using default Vietcombank", bankId);
                return "970436"; // Default to Vietcombank
        }
    }

    /**
     * Get bank name by bank ID
     */
    public String getBankNameById(String bankId) {
        if (bankId == null) return "Unknown Bank";
        
        switch (bankId.trim()) {
            case "970405": return "Agribank";
            case "970406": return "DongA Bank";
            case "970407": return "Techcombank";
            case "970408": return "GPBank";
            case "970409": return "BacA Bank";
            case "970412": return "PVcomBank";
            case "970414": return "OceanBank";
            case "970415": return "Vietinbank";
            case "970416": return "ACB";
            case "970418": return "BIDV";
            case "970422": return "MB Bank";
            case "970423": return "TPBank";
            case "970424": return "Shinhan Bank";
            case "970425": return "ABBank";
            case "970426": return "MSB";
            case "970427": return "VietABank";
            case "970428": return "Nam A Bank";
            case "970429": return "SCB";
            case "970430": return "PGBank";
            case "970431": return "Eximbank";
            case "970432": return "VPBank";
            case "970433": return "VietBank";
            case "970436": return "Vietcombank";
            case "970437": return "HDBank";
            case "970438": return "BVBank";
            case "970439": return "LienVietPostBank";
            case "970440": return "SeABank";
            case "970441": return "VIB";
            case "970442": return "HDB";
            case "970443": return "SHB";
            case "970448": return "OCB";
            case "970449": return "UOB";
            case "970454": return "VietCapitalBank";
            case "970455": return "IBK - HCMC";
            case "970456": return "CAKE by VPBank";
            case "970458": return "UOB Vietnam";
            case "970463": return "HSBC Vietnam";
            case "970466": return "KienLongBank";
            case "970467": return "COOPBANK";
            case "970468": return "BaoVietBank";
            case "970469": return "NCB";
            default: return "Unknown Bank";
        }
    }

    /**
     * Cập nhật thông tin ngân hàng cho nhân viên
     */
    public boolean updateStaffBankAccount(Integer staffId, String bankId, String bankAccount, 
                                        String accountHolderName, String bankName) {
        try {
            Optional<Users> staffOpt = userRepository.findById(staffId);
            if (staffOpt.isPresent()) {
                Users staff = staffOpt.get();
                
                // Kiểm tra quyền STAFF
                if (staff.getRole() != Users.Role.STAFF) {
                    log.warn("User {} is not a staff member", staffId);
                    return false;
                }
                
                // Cập nhật thông tin ngân hàng
                staff.setBankId(bankId);
                staff.setBankAccount(bankAccount);
                staff.setAccountHolderName(accountHolderName);
                staff.setBankName(bankName);
                
                userRepository.save(staff);
                
                log.info("Updated bank account info for staff {}", staffId);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            log.error("Error updating staff bank account: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Class chứa thông tin tài khoản ngân hàng
     */
    public static class BankAccountInfo {
        private final String bankId;
        private final String accountNo;
        private final String accountHolderName;
        private final String bankName;

        public BankAccountInfo(String bankId, String accountNo, String accountHolderName, String bankName) {
            this.bankId = bankId;
            this.accountNo = accountNo;
            this.accountHolderName = accountHolderName;
            this.bankName = bankName;
        }

        public String getBankId() { return bankId; }
        public String getAccountNo() { return accountNo; }
        public String getAccountHolderName() { return accountHolderName; }
        public String getBankName() { return bankName; }
    }
}
