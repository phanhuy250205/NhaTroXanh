// package nhatroxanh.com.Nhatroxanh.Service;

// import nhatroxanh.com.Nhatroxanh.Model.enity.*;
// import nhatroxanh.com.Nhatroxanh.Repository.*;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class LocationService {
    
//     @Autowired
//     private ProvinceRepository provinceRepository;
    
//     @Autowired
//     private DistrictRepository districtRepository;
    
//     @Autowired
//     private WardRepository wardRepository;
    
//     public List<Province> getAllProvinces() {
//         return provinceRepository.findAll();
//     }
    
//     public List<District> getDistrictsByProvinceId(Integer provinceId) {
//         return districtRepository.findByProvinceId(provinceId);
//     }
    
//     public List<Ward> getWardsByDistrictId(Integer districtId) {
//         return wardRepository.findByDistrictId(districtId);
//     }
// }
