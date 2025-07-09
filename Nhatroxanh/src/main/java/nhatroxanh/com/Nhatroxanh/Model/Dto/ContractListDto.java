package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.time.LocalDate;

public class ContractListDto {
    private Long contractId;        // Mã số hợp đồng
    private LocalDate startDate;    // Ngày bắt đầu
    private LocalDate endDate;      // Ngày kết thúc
    private String tenantName;  // ← THÊM FIELD NÀY
    private String tenantPhone;     // Số điện thoại
    private String status;          // Trạng thái

    // Constructors
    public ContractListDto() {}

    public ContractListDto(Long contractId, LocalDate startDate, LocalDate endDate,
                           String tenantPhone, String status , String tenantName) {
        this.contractId = contractId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.tenantPhone = tenantPhone;
        this.status = status;
        this.tenantName = tenantName; // ← THÊM TRUYỀN VÀO
    }

    // Getters and Setters


    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getTenantPhone() { return tenantPhone; }
    public void setTenantPhone(String tenantPhone) { this.tenantPhone = tenantPhone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Helper method để format status
    public String getStatusDisplay() {
        switch (status != null ? status.toUpperCase() : "") {
            case "ACTIVE": return "Đang hoạt động";
            case "EXPIRED": return "Hết hạn";
            case "DRAFT": return "Bản nháp";
            case "CANCELLED": return "Đã hủy";
            default: return status;
        }
    }
}
