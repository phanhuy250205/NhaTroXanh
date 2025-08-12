package nhatroxanh.com.Nhatroxanh.Model.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ContractDto {
    private Integer id;
    private LocalDate contractDate;
    private String status;
    private Owner owner;
    private Tenant tenant;
    private UnregisteredTenant unregisteredTenant;
    private String tenantType;
    private Room room;
    private Terms terms;
    // Thêm trường địa chỉ cho chủ trọ và người thuê
    private String ownerAddress;
    private String tenantAddress;
    private PaymentMethod paymentMethod;
    private List<ResidentDto> residents = new ArrayList<>();

    public List<ResidentDto> getResidents() {
        return residents;
    }

    public void setResidents(List<ResidentDto> residents) {
        this.residents = residents;
    }

    public ContractDto() {
        this.owner = new Owner();
        this.tenant = new Tenant();
        this.room = new Room();
        this.terms = new Terms();
        this.contractDate = LocalDate.now();
        this.status = "DRAFT";
        this.tenantType = "REGISTERED";
        this.unregisteredTenant = new UnregisteredTenant();
        this.tenantAddress = ""; // Khởi tạo địa chỉ người thuê
        this.ownerAddress = ""; // Khởi tạo địa chỉ chủ trọ
    }

    public enum PaymentMethod {
        TIEN_MAT,
        BANK
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getOwnerAddress() {
        return this.ownerAddress;
    }

    public void setOwnerAddress(String ownerAddress) {
        this.ownerAddress = ownerAddress;
    }

    public String getTenantAddress() {
        return tenantAddress;
    }

    public void setTenantAddress(String tenantAddress) {
        this.tenantAddress = tenantAddress;
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDate getContractDate() {
        return contractDate;
    }

    public void setContractDate(LocalDate contractDate) {
        this.contractDate = contractDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Owner getOwner() {
        return owner != null ? owner : (owner = new Owner());
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Tenant getTenant() {
        return tenant != null ? tenant : (tenant = new Tenant());
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public UnregisteredTenant getUnregisteredTenant() {
        return unregisteredTenant != null ? unregisteredTenant : (unregisteredTenant = new UnregisteredTenant());
    }

    public void setUnregisteredTenant(UnregisteredTenant unregisteredTenant) {
        this.unregisteredTenant = unregisteredTenant;
    }

    public String getTenantType() {
        return tenantType;
    }

    public void setTenantType(String tenantType) {
        this.tenantType = tenantType;
    }

    public Room getRoom() {
        return room != null ? room : (room = new Room());
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Terms getTerms() {
        return terms != null ? terms : (terms = new Terms());
    }

    public void setTerms(Terms terms) {
        this.terms = terms;
    }

    public static class Owner {
        private Integer userId; // ✅ THÊM FIELD NÀY
        private String fullName;
        private String phone;
        private String cccdNumber;
        private String email;
        private Date birthday;
        private String bankAccount;
        private Date issueDate;
        private String fullCccdNumber; // Thêm trường
        private String issuePlace;
        private String province;
        private String district;
        private String ward;
        private String street;

        // Getters and setters

        public String getFullCccdNumber() {
            return fullCccdNumber;
        }

        public void setFullCccdNumber(String fullCccdNumber) {
            this.fullCccdNumber = fullCccdNumber;
        }

        public Integer getUserId() {
            return userId;
        }

        public void setUserId(Integer userId) {
            this.userId = userId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCccdNumber() {
            return cccdNumber;
        }

        public void setCccdNumber(String cccdNumber) {
            this.cccdNumber = cccdNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String getBankAccount() {
            return bankAccount;
        }

        public void setBankAccount(String bankAccount) {
            this.bankAccount = bankAccount;
        }

        public Date getIssueDate() {
            return issueDate;
        }

        public void setIssueDate(Date issueDate) {
            this.issueDate = issueDate;
        }

        public String getIssuePlace() {
            return issuePlace;
        }

        public void setIssuePlace(String issuePlace) {
            this.issuePlace = issuePlace;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getWard() {
            return ward;
        }

        public void setWard(String ward) {
            this.ward = ward;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public static String getFullAddress(ContractDto contractDto) {
            return contractDto.getOwnerAddress();
        }

    }

    public static class UnregisteredTenant {
        private String fullName;
        private String phone;
        private String cccdNumber;
        private String email;
        private Date issueDate;
        private String issuePlace;
        private String province;
        private String district;
        private String ward;
        private String street;
        private Date birthday;
        private String cccdFrontUrl;
        private String cccdBackUrl;

        // Getters and setters
        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCccdNumber() {
            return cccdNumber;
        }

        public void setCccdNumber(String cccdNumber) {
            this.cccdNumber = cccdNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Date getIssueDate() {
            return issueDate;
        }

        public void setIssueDate(Date issueDate) {
            this.issueDate = issueDate;
        }

        public String getIssuePlace() {
            return issuePlace;
        }

        public void setIssuePlace(String issuePlace) {
            this.issuePlace = issuePlace;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getWard() {
            return ward;
        }

        public void setWard(String ward) {
            this.ward = ward;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String getCccdFrontUrl() {
            return cccdFrontUrl;
        }

        public void setCccdFrontUrl(String cccdFrontUrl) {
            this.cccdFrontUrl = cccdFrontUrl;
        }

        public String getCccdBackUrl() {
            return cccdBackUrl;
        }

        public void setCccdBackUrl(String cccdBackUrl) {
            this.cccdBackUrl = cccdBackUrl;
        }

        public static String getFullAddress(ContractDto contractDto) {
            return contractDto.getTenantAddress();
        }

    }

    public static class Tenant {
        private Long userId; // ✅ THÊM FIELD NÀY
        private String fullName;
        private String phone;
        @NotNull(message = "Số CCCD không được để trống")
        @Pattern(regexp = "\\d{12}", message = "Số CCCD phải có 12 chữ số")
        private String cccdNumber; // Số CCCD đầy đủ
        private String maskedCccdNumber; // Số CCCD bị che để hiển thị
        private String email;
        private Date issueDate;
        private String issuePlace;
        private String province;
        private String district;
        private String ward;
        private String street;
        private Date birthday;
        private String cccdFrontUrl;
        private String cccdBackUrl;
        private String fullCccdNumber;

        public String getMaskedCccdNumber() {
            return maskedCccdNumber;
        }

        public void setMaskedCccdNumber(String maskedCccdNumber) {
            this.maskedCccdNumber = maskedCccdNumber;
        }

        public String getFullCccdNumber() {
            return fullCccdNumber;
        }

        public void setFullCccdNumber(String fullCccdNumber) {
            this.fullCccdNumber = fullCccdNumber;
        }

        // Getters and setters

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCccdNumber() {
            return cccdNumber;
        }

        public void setCccdNumber(String cccdNumber) {
            this.cccdNumber = cccdNumber;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Date getIssueDate() {
            return issueDate;
        }

        public void setIssueDate(Date issueDate) {
            this.issueDate = issueDate;
        }

        public String getIssuePlace() {
            return issuePlace;
        }

        public void setIssuePlace(String issuePlace) {
            this.issuePlace = issuePlace;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getWard() {
            return ward;
        }

        public void setWard(String ward) {
            this.ward = ward;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String getCccdFrontUrl() {
            return cccdFrontUrl;
        }

        public void setCccdFrontUrl(String cccdFrontUrl) {
            this.cccdFrontUrl = cccdFrontUrl;
        }

        public String getCccdBackUrl() {
            return cccdBackUrl;
        }

        public void setCccdBackUrl(String cccdBackUrl) {
            this.cccdBackUrl = cccdBackUrl;
        }

        public static String getFullAddress(ContractDto contractDto) {
            return contractDto.getTenantAddress();
        }
    }

    public static class Room {

        @JsonProperty("roomId")
        private Integer roomId;
        private String roomName;
        private Float area;
        private Integer max_tenants;
        private Float price;
        private String status;
        private Integer hostelId;
        private String hostelName;
        private String address;
        private String street; // Thêm trường street
        private String ward; // Thêm trường ward
        private String district; // Thêm trường district
        private String province; // Thêm trường province
        @JsonProperty("isCurrent") // ✅ THÊM ANNOTATION
        private Boolean isCurrent = false;
        private List<Integer> utilityIds;

        public List<Integer> getUtilityIds() {
            return utilityIds;
        }

        public void setUtilityIds(List<Integer> utilityIds) {
            this.utilityIds = utilityIds;
        }

        public Boolean getIsCurrent() { // ✅ TÊN ĐÚNG
            return isCurrent;
        }

        public void setIsCurrent(Boolean isCurrent) { // ✅ TÊN ĐÚNG
            this.isCurrent = isCurrent;
        }

        @NotNull(message = "ID phòng không được để trống")
        public Integer getRoomId() {
            return roomId != null ? roomId : 0; // Tránh null tạm thời để debug
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getDistrict() {
            return district;
        }

        public void setDistrict(String district) {
            this.district = district;
        }

        public String getWard() {
            return ward;
        }

        public void setWard(String ward) {
            this.ward = ward;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public void setRoomId(Integer roomId) {
            this.roomId = roomId;
        }

        public String getRoomName() {
            return roomName;
        }

        public void setRoomName(String roomName) {
            this.roomName = roomName;
        }

        public Float getArea() {
            return area;
        }

        public void setArea(Float area) {
            this.area = area;
        }

        public Float getPrice() {
            return price;
        }

        public void setPrice(Float price) {
            this.price = price;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Integer getHostelId() {
            return hostelId;
        }

        public void setHostelId(Integer hostelId) {
            this.hostelId = hostelId;
        }

        public String getHostelName() {
            return hostelName;
        }

        public void setHostelName(String hostelName) {
            this.hostelName = hostelName;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getMaxTenants() { return max_tenants; }
        public void setMaxTenants(Integer maxTenants) { this.max_tenants = maxTenants; }

    }

    public static class Terms {
        private Double price;
        private Double deposit;
        private LocalDate startDate;
        private LocalDate endDate;
        private String terms;
        private Integer duration;
        private String paymentDateDescription; // Đổi từ paymentDate thành paymentDateDescription

        // ✅ THÊM: Helper method convert Float -> Double
        public static Double floatToDouble(Float floatValue) {
            if (floatValue == null) {
                return 0.0;
            }
            // Làm tròn để tránh precision errors
            return (double) Math.round(floatValue.doubleValue());
        }

        // ✅ THÊM: Helper method convert Double -> Float cho Entity
        public static Float doubleToFloat(Double doubleValue) {
            if (doubleValue == null) {
                return 0.0f;
            }
            if (doubleValue > Float.MAX_VALUE) {
                return Float.MAX_VALUE;
            }
            return doubleValue.floatValue();
        }

        // ✅ THÊM: Method parse VND amount
        public static Double parseVNDAmount(String amountStr) {
            if (amountStr == null || amountStr.trim().isEmpty()) {
                return 0.0;
            }

            try {
                String cleanAmount = amountStr
                        .replace("VND", "")
                        .replace("₫", "")
                        .replace(" ", "")
                        .trim();

                // ✅ Xử lý format Việt Nam: 2.000.000 -> 2000000
                if (cleanAmount.matches("\\d{1,3}(\\.\\d{3})*")) {
                    cleanAmount = cleanAmount.replace(".", "");
                }

                cleanAmount = cleanAmount.replaceAll("[^0-9]", "");

                if (cleanAmount.isEmpty()) {
                    return 0.0;
                }

                // ✅ Parse thành Long trước để tránh floating point errors
                Long longValue = Long.parseLong(cleanAmount);
                Double result = longValue.doubleValue();

                System.out.println("💰 Parse VND: '" + amountStr + "' -> " + result);
                return result;
            } catch (Exception e) {
                System.err.println("❌ Error parsing VND amount: " + amountStr + " - " + e.getMessage());
                return 0.0;
            }
        }

        // ✅ THÊM: Getter cho formattedPrice - tự động format
        public String getFormattedPrice() {
            return ContractDto.formatVND(this.price);
        }

        // ✅ THÊM: Getter cho formattedDeposit - tự động format
        public String getFormattedDeposit() {
            return ContractDto.formatVND(this.deposit);
        }

        // ✅ THÊM: Setter cho formattedPrice - parse từ string
        public void setFormattedPrice(String formattedPrice) {
            this.price = parseVNDAmount(formattedPrice);
        }

        // ✅ THÊM: Setter cho formattedDeposit - parse từ string
        public void setFormattedDeposit(String formattedDeposit) {
            this.deposit = parseVNDAmount(formattedDeposit);
        }

        // ✅ THÊM: Method để lấy price dưới dạng Float cho Entity
        public Float getPriceAsFloat() {
            return doubleToFloat(this.price);
        }

        // ✅ THÊM: Method để lấy deposit dưới dạng Float cho Entity
        public Float getDepositAsFloat() {
            return doubleToFloat(this.deposit);
        }

        // Getter và Setter
        public String getPaymentDateDescription() {
            return paymentDateDescription;
        }

        public void setPaymentDateDescription(String paymentDateDescription) {
            this.paymentDateDescription = paymentDateDescription;
        }

        public Terms() {
            this.startDate = LocalDate.now();
        }

        @JsonProperty("price")
        @NotNull(message = "Giá thuê không được để trống")
        public Double getPrice() {
            return price;
        }

        // ✅ SỬA: Setter cho price - xử lý cả Float (từ Entity) và String (từ form)
        public void setPrice(Object priceObj) {
            System.out.println("💰 Setting price object: " + priceObj + " (Type: " + (priceObj != null ? priceObj.getClass().getSimpleName() : "null") + ")");

            if (priceObj == null) {
                this.price = 0.0;
            } else if (priceObj instanceof Float) {
                // ✅ QUAN TRỌNG: Từ Entity Float -> DTO Double
                this.price = floatToDouble((Float) priceObj);
            } else if (priceObj instanceof Double) {
                this.price = (Double) priceObj;
            } else if (priceObj instanceof Number) {
                this.price = floatToDouble(((Number) priceObj).floatValue());
            } else if (priceObj instanceof String) {
                // ✅ Từ form input
                this.price = parseVNDAmount((String) priceObj);
            } else {
                this.price = parseVNDAmount(priceObj.toString());
            }

            System.out.println("💰 Final price set: " + this.price);
        }

        // Getters and setters
        @JsonProperty("deposit")
        @NotNull(message = "Tiền cọc không được để trống")
        public Double getDeposit() {
            return deposit;
        }

        // ✅ SỬA: Setter cho deposit - xử lý cả Float (từ Entity) và String (từ form)
        public void setDeposit(Object depositObj) {
            System.out.println("💰 Setting deposit object: " + depositObj + " (Type: " + (depositObj != null ? depositObj.getClass().getSimpleName() : "null") + ")");

            if (depositObj == null) {
                this.deposit = 0.0;
            } else if (depositObj instanceof Float) {
                // ✅ QUAN TRỌNG: Từ Entity Float -> DTO Double
                this.deposit = floatToDouble((Float) depositObj);
            } else if (depositObj instanceof Double) {
                this.deposit = (Double) depositObj;
            } else if (depositObj instanceof Number) {
                this.deposit = floatToDouble(((Number) depositObj).floatValue());
            } else if (depositObj instanceof String) {
                // ✅ Từ form input
                this.deposit = parseVNDAmount((String) depositObj);
            } else {
                this.deposit = parseVNDAmount(depositObj.toString());
            }

            System.out.println("💰 Final deposit set: " + this.deposit);
        }

        @JsonProperty("startDate")
        @NotNull(message = "Ngày bắt đầu không được để trống")
        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            calculateEndDate();
        }

        @JsonProperty("endDate")
        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }

        @JsonProperty("terms")
        public String getTerms() {
            return terms;
        }

        public void setTerms(String terms) {
            this.terms = terms;
        }

        @JsonProperty("duration")
        @NotNull(message = "Thời hạn không được để trống")
        @Min(value = 1, message = "Thời hạn phải lớn hơn 0!")
        public Integer getDuration() {
            return duration;
        }

        public void setDuration(Integer duration) {
            this.duration = duration;
            calculateEndDate();
        }

        // Helper method to calculate end date
        private void calculateEndDate() {
            if (this.startDate != null && this.duration != null && this.duration > 0) {
                this.endDate = this.startDate.plusMonths(this.duration);
            }
        }

    }

    @Data // Tự động tạo getter, setter, toString, equals, hashCode
    @NoArgsConstructor // Tự động tạo constructor rỗng
    @AllArgsConstructor // Tự động tạo constructor với tất cả tham số
    public static class ResidentDto {
        private String fullName;
        private String birthYear;
        private String phone;
        private String cccdNumber;
    }

    // ✅ SỬA: Phương thức format tiền Việt Nam - không hiển thị ".0" cho số nguyên
    public static String formatVND(Double amount) {
        if (amount == null) {
            return "0";
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

        // ✅ Kiểm tra nếu là số nguyên thì không hiển thị decimal
        if (amount == Math.floor(amount)) {
            formatter.setMaximumFractionDigits(0);
            formatter.setMinimumFractionDigits(0);
        } else {
            formatter.setMaximumFractionDigits(2);
            formatter.setMinimumFractionDigits(0);
        }

        return formatter.format(amount);
    }

    // Getter cho price với format VND
    public String getFormattedPrice() {
        return formatVND(this.getTerms() != null ? this.getTerms().getPrice() : null);
    }

    // Getter cho deposit với format VND
    public String getFormattedDeposit() {
        return formatVND(this.getTerms() != null ? this.getTerms().getDeposit() : null);
    }

}
