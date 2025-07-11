package nhatroxanh.com.Nhatroxanh.Model.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import nhatroxanh.com.Nhatroxanh.Model.enity.UnregisteredTenants;
import java.sql.Date;
import java.time.LocalDate;

public class ContractDto {
    private Long id;
    private LocalDate contractDate;
    private String status;
    private Owner owner;
    private Tenant tenant;
    private UnregisteredTenant unregisteredTenant;
    private String tenantType;
    private Room room;
    private Terms terms;

    public ContractDto() {
        this.owner = new Owner();
        this.tenant = new Tenant();
        this.room = new Room();
        this.terms = new Terms();
        this.contractDate = LocalDate.now();
        this.status = "DRAFT";
        this.tenantType = "REGISTERED";
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Owner getOwner() { return owner != null ? owner : (owner = new Owner()); }
    public void setOwner(Owner owner) { this.owner = owner; }

    public Tenant getTenant() { return tenant != null ? tenant : (tenant = new Tenant()); }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public UnregisteredTenant getUnregisteredTenant() {
        return unregisteredTenant != null ? unregisteredTenant : (unregisteredTenant = new UnregisteredTenant());
    }
    public void setUnregisteredTenant(UnregisteredTenant unregisteredTenant) {
        this.unregisteredTenant = unregisteredTenant;
    }

    public String getTenantType() { return tenantType; }
    public void setTenantType(String tenantType) { this.tenantType = tenantType; }

    public Room getRoom() { return room != null ? room : (room = new Room()); }
    public void setRoom(Room room) { this.room = room; }

    public Terms getTerms() { return terms != null ? terms : (terms = new Terms()); }
    public void setTerms(Terms terms) { this.terms = terms; }

    public static class Owner {
        private String fullName;
        private String phone;
        private String cccdNumber;
        private String email;
        private Date birthday;
        private String bankAccount;
        private Date issueDate;
        private String issuePlace;
        private String province;
        private String district;
        private String ward;
        private String street;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCccdNumber() { return cccdNumber; }
        public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Date getBirthday() { return birthday; }
        public void setBirthday(Date birthday) { this.birthday = birthday; }
        public String getBankAccount() { return bankAccount; }
        public void setBankAccount(String bankAccount) { this.bankAccount = bankAccount; }
        public Date getIssueDate() { return issueDate; }
        public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }
        public String getIssuePlace() { return issuePlace; }
        public void setIssuePlace(String issuePlace) { this.issuePlace = issuePlace; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
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
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCccdNumber() { return cccdNumber; }
        public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Date getIssueDate() { return issueDate; }
        public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }
        public String getIssuePlace() { return issuePlace; }
        public void setIssuePlace(String issuePlace) { this.issuePlace = issuePlace; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public Date getBirthday() { return birthday; }
        public void setBirthday(Date birthday) { this.birthday = birthday; }
        public String getCccdFrontUrl() { return cccdFrontUrl; }
        public void setCccdFrontUrl(String cccdFrontUrl) { this.cccdFrontUrl = cccdFrontUrl; }
        public String getCccdBackUrl() { return cccdBackUrl; }
        public void setCccdBackUrl(String cccdBackUrl) { this.cccdBackUrl = cccdBackUrl; }
    }

    public static class Tenant {
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
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getCccdNumber() { return cccdNumber; }
        public void setCccdNumber(String cccdNumber) { this.cccdNumber = cccdNumber; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Date getIssueDate() { return issueDate; }
        public void setIssueDate(Date issueDate) { this.issueDate = issueDate; }
        public String getIssuePlace() { return issuePlace; }
        public void setIssuePlace(String issuePlace) { this.issuePlace = issuePlace; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public Date getBirthday() { return birthday; }
        public void setBirthday(Date birthday) { this.birthday = birthday; }
        public String getCccdFrontUrl() { return cccdFrontUrl; }
        public void setCccdFrontUrl(String cccdFrontUrl) { this.cccdFrontUrl = cccdFrontUrl; }
        public String getCccdBackUrl() { return cccdBackUrl; }
        public void setCccdBackUrl(String cccdBackUrl) { this.cccdBackUrl = cccdBackUrl; }
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

        // Getters and setters
        // getter setter max_tenants
        public Integer getMaxTenants() { return max_tenants; }
        public void setMaxTenants(Integer maxTenants) { this.max_tenants = maxTenants; }
        public Integer getRoomId() { return roomId; }
        public void setRoomId(Integer roomId) { this.roomId = roomId; }
        public String getRoomName() { return roomName; }
        public void setRoomName(String roomName) { this.roomName = roomName; }
        public Float getArea() { return area; }
        public void setArea(Float area) { this.area = area; }
        public Float getPrice() { return price; }
        public void setPrice(Float price) { this.price = price; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Integer getHostelId() { return hostelId; }
        public void setHostelId(Integer hostelId) { this.hostelId = hostelId; }
        public String getHostelName() { return hostelName; }
        public void setHostelName(String hostelName) { this.hostelName = hostelName; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
    }

    public static class Terms {
        private Double price;
        private Double deposit;
        private LocalDate startDate;
        private LocalDate endDate;
        private String terms;
        private Integer duration;

        public Terms() {
            this.startDate = LocalDate.now();
        }

        // Getters and setters
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }

        public Double getDeposit() { return deposit; }
        public void setDeposit(Double deposit) { this.deposit = deposit; }

        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
            calculateEndDate();
        }

        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

        public String getTerms() { return terms; }
        public void setTerms(String terms) { this.terms = terms; }

        public Integer getDuration() { return duration; }
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
}
