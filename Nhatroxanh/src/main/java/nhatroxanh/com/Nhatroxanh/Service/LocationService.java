package nhatroxanh.com.Nhatroxanh.Service;

import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.entity.District;
import nhatroxanh.com.Nhatroxanh.Model.entity.Province;
import nhatroxanh.com.Nhatroxanh.Model.entity.Ward;

@Service
public interface LocationService {
    Province findOrCreateProvince(String provinceName);
    District findOrCreateDistrict(String districtName, Province province);
    Ward findOrCreateWard(String wardName, District district);
}
