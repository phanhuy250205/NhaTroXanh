package nhatroxanh.com.Nhatroxanh.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.District;
import nhatroxanh.com.Nhatroxanh.Model.enity.Province;
import nhatroxanh.com.Nhatroxanh.Model.enity.Ward;
import org.springframework.stereotype.Service;

@Service
public interface LocationService {
    Province findOrCreateProvince(String provinceName);
    District findOrCreateDistrict(String districtName, Province province);
    Ward findOrCreateWard(String wardName, District district);
}
