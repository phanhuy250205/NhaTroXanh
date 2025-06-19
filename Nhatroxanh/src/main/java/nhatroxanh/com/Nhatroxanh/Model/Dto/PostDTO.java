package nhatroxanh.com.Nhatroxanh.Model.Dto;

import java.sql.Date;
import java.util.List;
import java.util.Set;

public class PostDTO {
    private Integer postId;
    private String title;
    private String description;
    private Float price;
    private Float area;
    private Integer view;
    private Date createdAt;
    private String approvalStatus;
    
    private CategoryDTO category;
    private AddressDTO address;
    private List<ImageDTO> images;
    private Set<UtilityDTO> utilities;
    
    public PostDTO() {}
    
    public PostDTO(Integer postId, String title, String description, Float price, 
                   Float area, Integer view, Date createdAt, String approvalStatus) {
        this.postId = postId;
        this.title = title;
        this.description = description;
        this.price = price;
        this.area = area;
        this.view = view;
        this.createdAt = createdAt;
        this.approvalStatus = approvalStatus;
    }
    
    public Integer getPostId() { return postId; }
    public void setPostId(Integer postId) { this.postId = postId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Float getPrice() { return price; }
    public void setPrice(Float price) { this.price = price; }
    
    public Float getArea() { return area; }
    public void setArea(Float area) { this.area = area; }
    
    public Integer getView() { return view; }
    public void setView(Integer view) { this.view = view; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }
    
    public CategoryDTO getCategory() { return category; }
    public void setCategory(CategoryDTO category) { this.category = category; }
    
    public AddressDTO getAddress() { return address; }
    public void setAddress(AddressDTO address) { this.address = address; }
    
    public List<ImageDTO> getImages() { return images; }
    public void setImages(List<ImageDTO> images) { this.images = images; }
    
    public Set<UtilityDTO> getUtilities() { return utilities; }
    public void setUtilities(Set<UtilityDTO> utilities) { this.utilities = utilities; }
    
    // Nested DTOs
    public static class CategoryDTO {
        private Integer categoryId;
        private String name;
        
        public CategoryDTO() {}
        public CategoryDTO(Integer categoryId, String name) {
            this.categoryId = categoryId;
            this.name = name;
        }
        
        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public static class AddressDTO {
        private String street;
        private WardDTO ward;
        
        public AddressDTO() {}
        public AddressDTO(String street, WardDTO ward) {
            this.street = street;
            this.ward = ward;
        }
        
        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public WardDTO getWard() { return ward; }
        public void setWard(WardDTO ward) { this.ward = ward; }
        
        public static class WardDTO {
            private String name;
            private DistrictDTO district;
            
            public WardDTO() {}
            public WardDTO(String name, DistrictDTO district) {
                this.name = name;
                this.district = district;
            }
            
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public DistrictDTO getDistrict() { return district; }
            public void setDistrict(DistrictDTO district) { this.district = district; }
            
            public static class DistrictDTO {
                private String name;
                private ProvinceDTO province;
                
                public DistrictDTO() {}
                public DistrictDTO(String name, ProvinceDTO province) {
                    this.name = name;
                    this.province = province;
                }
                
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
                public ProvinceDTO getProvince() { return province; }
                public void setProvince(ProvinceDTO province) { this.province = province; }
                
                public static class ProvinceDTO {
                    private String name;
                    
                    public ProvinceDTO() {}
                    public ProvinceDTO(String name) { this.name = name; }
                    
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
            }
        }
    }
    
    public static class ImageDTO {
        private String url;
        
        public ImageDTO() {}
        public ImageDTO(String url) { this.url = url; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
    
    public static class UtilityDTO {
        private Integer utilityId;
        private String name;
        
        public UtilityDTO() {}
        public UtilityDTO(Integer utilityId, String name) {
            this.utilityId = utilityId;
            this.name = name;
        }
        
        public Integer getUtilityId() { return utilityId; }
        public void setUtilityId(Integer utilityId) { this.utilityId = utilityId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
