package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.util.List;

public class RoomCreateDTO {
    private Integer roomId;
    private String namerooms;
    private Float price;
    private Float acreage;
    private String status; 
    private Integer maxTenants;
    private Integer hostelId;
    private String description;
    private List<String> amenities;

    //getter and setter methods
    public String getNamerooms() {
        return namerooms;        
    }
    public Integer getRoomId() {
        return roomId;
    }
    public void setRoomId(Integer roomId) {
        this.roomId = roomId;
    }
    public void setNamerooms(String namerooms) {
        this.namerooms = namerooms;
    }
    public Float getPrice() {
        return price;
    }
    public void setPrice(Float price) {
        this.price = price;
    }
    public Float getAcreage() {
        return acreage;
    }
    public void setAcreage(Float acreage) {
        this.acreage = acreage;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public Integer getMaxTenants() {
        return maxTenants;
    }
    public void setMaxTenants(Integer maxTenants) {
        this.maxTenants = maxTenants;
    }
    public Integer getHostelId() {
        return hostelId;
    }
    public void setHostelId(Integer hostelId) {
        this.hostelId = hostelId;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public List<String> getAmenities() {
        return amenities;
    }
    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }
}
