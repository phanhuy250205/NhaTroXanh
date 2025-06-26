package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.time.LocalDate;

public class ContractDto {
    private Long id;
    private LocalDate contractDate;
    private String status;
    private Owner owner;
    private Tenant tenant;
    private Room room;
    private Terms terms;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Owner getOwner() { return owner; }
    public void setOwner(Owner owner) { this.owner = owner; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Terms getTerms() { return terms; }
    public void setTerms(Terms terms) { this.terms = terms; }

    public static class Owner {
        private String fullName;
        private String phone;
        private String id; // CCCD
        private String email;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Tenant {
        private String fullName;
        private String phone;
        private String id; // CCCD
        private String email;

        // Getters and setters
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class Room {
        private String roomNumber;
        private Double area;

        // Getters and setters
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public Double getArea() { return area; }
        public void setArea(Double area) { this.area = area; }
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