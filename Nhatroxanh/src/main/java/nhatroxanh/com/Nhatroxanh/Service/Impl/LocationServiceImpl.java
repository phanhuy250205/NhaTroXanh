package nhatroxanh.com.Nhatroxanh.Service.Impl;



import nhatroxanh.com.Nhatroxanh.Model.entity.District;
import nhatroxanh.com.Nhatroxanh.Model.entity.Province;
import nhatroxanh.com.Nhatroxanh.Model.entity.Ward;
import nhatroxanh.com.Nhatroxanh.Repository.DistrictRepository;
import nhatroxanh.com.Nhatroxanh.Repository.ProvinceRepository;
import nhatroxanh.com.Nhatroxanh.Repository.WardRepository;
import nhatroxanh.com.Nhatroxanh.Service.LocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocationServiceImpl implements LocationService {

    @Autowired
    private ProvinceRepository provinceRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private WardRepository wardRepository;

    @Override
    @Transactional
    public Province findOrCreateProvince(String provinceName) {
        if (provinceName == null || provinceName.trim().isEmpty()) {
            return null;
        }

        return provinceRepository.findByName(provinceName)
                .orElseGet(() -> {
                    Province newProvince = new Province();
                    newProvince.setName(provinceName);
                    return provinceRepository.save(newProvince);
                });
    }

    @Override
    @Transactional
    public District findOrCreateDistrict(String districtName, Province province) {
        if (districtName == null || districtName.trim().isEmpty() || province == null) {
            return null;
        }

        return districtRepository.findByNameAndProvince(districtName, province)
                .orElseGet(() -> {
                    District newDistrict = new District();
                    newDistrict.setName(districtName);
                    newDistrict.setProvince(province);
                    return districtRepository.save(newDistrict);
                });
    }

    @Override
    @Transactional
    public Ward findOrCreateWard(String wardName, District district) {
        if (wardName == null || wardName.trim().isEmpty() || district == null) {
            return null;
        }

        return wardRepository.findByNameAndDistrict(wardName, district)
                .orElseGet(() -> {
                    Ward newWard = new Ward();
                    newWard.setName(wardName);
                    newWard.setDistrict(district);
                    return wardRepository.save(newWard);
                });
    }
}
