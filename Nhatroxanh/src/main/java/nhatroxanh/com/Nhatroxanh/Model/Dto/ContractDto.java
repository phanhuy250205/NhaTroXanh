package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.sql.Date;
import java.time.LocalDate;

public class ContractDto {
    private Long id;
    private LocalDate contractDate;
    private String status;
    private Owner owner;
    private Tenant tenant;
    private Room room;
    private Terms terms;

    public ContractDto() {
        this.owner = new Owner();
        this.tenant = new Tenant();
        this.room = new Room();
        this.terms = new Terms();
        this.contractDate = LocalDate.now(); // Gán mặc định
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
    public Room getRoom() { return room != null ? room : (room = new Room()); }
    public void setRoom(Room room) { this.room = room; }
    public Terms getTerms() { return terms != null ? terms : (terms = new Terms()); }
    public void setTerms(Terms terms) { this.terms = terms; }

    public static class Owner {
        private String fullName;
        private String phone;
        private String cccdNumber; // CCCD number
        private String email;
        private Date birthday;
        private String bankAccount;
        private Date issueDate;
        private String issuePlace;
        private String province;
        private String district;
        private String ward;
        private String street;
        private String cccdFrontUrl; // Thêm trường cho URL hình ảnh CCCD mặt trước
        private String cccdBackUrl;  // Thêm trường cho URL hình ảnh CCCD mặt sau


        public String getCccdNumber() {
            return cccdNumber;
        }

        public void setCccdNumber(String cccdNumber) {
            this.cccdNumber = cccdNumber;
        }

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getId() { return cccdNumber; }
        public void setId(String id) { this.cccdNumber = id; }
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
        public String getCccdFrontUrl() { return cccdFrontUrl; }
        public void setCccdFrontUrl(String cccdFrontUrl) { this.cccdFrontUrl = cccdFrontUrl; }
        public String getCccdBackUrl() { return cccdBackUrl; }
        public void setCccdBackUrl(String cccdBackUrl) { this.cccdBackUrl = cccdBackUrl; }
    }

    public static class Tenant {
        private String fullName;
        private String phone;
        private String cccdNumber; // Đổi từ id thành cccdNumber
        private String email;
        private Date issueDate;
        private String issuePlace;
        private String province;
        private String district;
        private String ward;
        private String street;
        private Date birthday;
        private String cccdFrontUrl; // Thêm trường cho URL hình ảnh CCCD mặt trước
        private String cccdBackUrl;  // Thêm trường cho URL hình ảnh CCCD mặt sau


        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getId() { return cccdNumber; }
        public void setId(String id) { this.cccdNumber = id; }
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

        public String getCccdNumber() {
            return cccdNumber;
        }

        public void setCccdNumber(String cccdNumber) {
            this.cccdNumber = cccdNumber;
        }
    }

    public static class Room {
        private String roomNumber;
        private Double area;
        private String province;
        private String district;
        private String ward;
        private String street;

        // Getters and setters
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public Double getArea() { return area; }
        public void setArea(Double area) { this.area = area; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
    }

    public static class Terms {
        private Double price;
        private Double deposit;
        private LocalDate startDate;
        private LocalDate endDate;
        private String terms;

        // Getters and setters
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public Double getDeposit() { return deposit; }
        public void setDeposit(Double deposit) { this.deposit = deposit; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public String getTerms() { return terms; }
        public void setTerms(String terms) { this.terms = terms; }
    }
}