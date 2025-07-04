    package nhatroxanh.com.Nhatroxanh.Service;

    import nhatroxanh.com.Nhatroxanh.Model.enity.*;
    import nhatroxanh.com.Nhatroxanh.Repository.*;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;

    @Service
    public class AddressService {

        @Autowired
        private ProvinceRepository provinceRepository;

        @Autowired
        private DistrictRepository districtRepository;

        @Autowired
        private WardRepository wardRepository;

        @Autowired
        private AddressRepository addressRepository;

        private String cleanAddressPart(String input) {
            if (input == null) return "";
            return input.replaceAll(",+", "")  // loại dấu phẩy dư thừa
                        .replaceAll("\\s+", " ") // loại khoảng trắng dư
                        .trim();
        }

        @Transactional
        public Province getOrCreateProvince(String code, String name) {
            if (code == null || name == null) {
                throw new IllegalArgumentException("Province code and name cannot be null");
            }
            return provinceRepository.findByCode(code)
                    .orElseGet(() -> {
                        Province province = Province.builder()
                                .code(code)
                                .name(name)
                                .build();
                        return provinceRepository.save(province);
                    });
        }

        @Transactional
        public District getOrCreateDistrict(String code, String name, Province province) {
            if (code == null || name == null || province == null) {
                throw new IllegalArgumentException("District code, name, and province cannot be null");
            }
            return districtRepository.findByCode(code)
                    .orElseGet(() -> {
                        District district = District.builder()
                                .code(code)
                                .name(name)
                                .province(province)
                                .build();
                        return districtRepository.save(district);
                    });
        }

        @Transactional
        public Ward getOrCreateWard(String code, String name, District district) {
            if (code == null || name == null || district == null) {
                throw new IllegalArgumentException("Ward code, name, and district cannot be null");
            }
            return wardRepository.findByCode(code)
                    .orElseGet(() -> {
                        Ward ward = Ward.builder()
                                .code(code)
                                .name(name)
                                .district(district)
                                .build();
                        return wardRepository.save(ward);
                    });
        }

        @Transactional
        public Address createAddress(String street, Ward ward) {
            if (street == null || ward == null) {
                throw new IllegalArgumentException("Street and ward cannot be null");
            }
            Address address = Address.builder()
                    .street(street)
                    .ward(ward)
                    .build();
            return addressRepository.save(address);
        }

        @Transactional
    public Address processAddressFromApi(String provinceCode, String districtCode, String wardCode,
                                        String provinceName, String districtName, String wardName,
                                        String street) {
        if (provinceCode == null || districtCode == null || wardCode == null ||
            provinceName == null || districtName == null || wardName == null || street == null) {
            throw new IllegalArgumentException("All address fields are required");
        }

    

        // Làm sạch tên các phần tử
        String cleanStreet = cleanAddressPart(street);
        String cleanProvinceName = cleanAddressPart(provinceName);
        String cleanDistrictName = cleanAddressPart(districtName);
        String cleanWardName = cleanAddressPart(wardName);

        Province province = getOrCreateProvince(provinceCode, cleanProvinceName);
        District district = getOrCreateDistrict(districtCode, cleanDistrictName, province);
        Ward ward = getOrCreateWard(wardCode, cleanWardName, district);

        return createAddress(cleanStreet, ward);
    }
    }