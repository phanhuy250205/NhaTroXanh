package nhatroxanh.com.Nhatroxanh.Model.enity;

public enum ContractStatus {
    PENDING("PENDING", "Chờ xử lý"),
    ACTIVE("ACTIVE", "Đang hiệu lực"),
    EXPIRED("EXPIRED", "Hết hạn"),
    TERMINATED("TERMINATED", "Đã chấm dứt"),
    CANCELLED("CANCELLED", "Đã hủy");

    private final String code;
    private final String description;

    ContractStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    // 🔧 CONVERT TỪ STRING
    public static ContractStatus fromString(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status không được để trống");
        }

        String upperStatus = status.trim().toUpperCase();

        for (ContractStatus contractStatus : ContractStatus.values()) {
            if (contractStatus.name().equals(upperStatus) ||
                    contractStatus.getCode().equals(upperStatus)) {
                return contractStatus;
            }
        }

        throw new IllegalArgumentException("Status không hợp lệ: " + status +
                ". Các giá trị cho phép: " + java.util.Arrays.toString(ContractStatus.values()));
    }

    // 🔧 KIỂM TRA STATUS HỢP LỆ
    public static boolean isValid(String status) {
        try {
            fromString(status);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // 🔧 LẤY TẤT CẢ STATUS
    public static java.util.List<String> getAllStatuses() {
        return java.util.Arrays.stream(ContractStatus.values())
                .map(ContractStatus::name)
                .collect(java.util.stream.Collectors.toList());
    }
}
