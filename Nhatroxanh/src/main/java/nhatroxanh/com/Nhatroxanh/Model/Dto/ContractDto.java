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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    private String paymentDate;
    public List<ResidentDto> getResidents() {
        return residents;
    }

    public void setResidents(List<ResidentDto> residents) {
        this.residents = residents;
    }
    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
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

        @JsonProperty("roomId") // Đảm bảo tên trường khớp
        private Integer roomId;
        private String roomName;
        private Float area;
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
        private Set<Integer> utilityIds;
        // ✅ SỬA GETTER/SETTER

        public Set<Integer> getUtilityIds() {
            return utilityIds;
        }

        public void setUtilityIds(Set<Integer> utilityIds) {
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

        // Getters and setters

        // public Boolean getCurrent() {
        // return isCurrent;
        // }
        //
        // public void setCurrent(Boolean current) {
        // isCurrent = current;
        // }

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

    }

    public static class Terms {
        private Double price;
        private Double deposit;
        private LocalDate startDate;
        private LocalDate endDate;
        private String terms;
        private Integer duration;
        private String paymentDate;
        public Terms() {
            this.startDate = LocalDate.now();
        }

        @JsonProperty("price")
        @NotNull(message = "Giá thuê không được để trống")
        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        // Getters and setters
        @JsonProperty("deposit")
        @NotNull(message = "Tiền cọc không được để trống")
        public Double getDeposit() {
            return deposit;
        }

        public void setDeposit(Double deposit) {
            this.deposit = deposit;
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
         public String getPaymentDate() { // <--- GETTER CẦN THIẾT
            return paymentDate;
        }

        public void setPaymentDate(String paymentDate) { // <--- SETTER CẦN THIẾT
            this.paymentDate = paymentDate;
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


}
