// Đặt trong file api-map.js
class VietnamAddressAPI {
    constructor() {
        this.baseURL = "https://provinces.open-api.vn/api";
        this.provinces = [];
        this.districts = [];
        this.wards = [];
        this.init();
    }

    async init() {
        await this.loadProvinces();
        this.setupEventListeners();
        this.setInitialAddressFromForm();
    }

    async loadProvinces() {
        try {
            const response = await fetch(`${this.baseURL}/`);
            const data = await response.json();
            this.provinces = data;
            this.populateProvinces();
        } catch (error) {
            console.error("Lỗi khi tải tỉnh thành:", error);
            this.showError("Không thể tải danh sách tỉnh/thành phố");
        }
    }

    async loadDistricts(provinceCode) {
        try {
            const response = await fetch(`${this.baseURL}/p/${provinceCode}?depth=2`);
            const data = await response.json();
            this.districts = data.districts || [];
            this.populateDistricts();
        } catch (error) {
            console.error("Lỗi khi tải quận/huyện:", error);
            this.showError("Không thể tải danh sách quận/huyện");
        }
    }

    async loadWards(districtCode) {
        try {
            const response = await fetch(`${this.baseURL}/d/${districtCode}?depth=2`);
            const data = await response.json();
            this.wards = data.wards || [];
            this.populateWards();
        } catch (error) {
            console.error("Lỗi khi tải phường/xã:", error);
            this.showError("Không thể tải danh sách phường/xã");
        }
    }

    populateProvinces() {
        const provinceSelect = document.getElementById("provinceHost");
        provinceSelect.innerHTML = '<option value="">Chọn tỉnh/thành phố</option>';
        this.provinces.forEach((province) => {
            const option = document.createElement("option");
            option.value = province.code;
            option.textContent = province.name;
            provinceSelect.appendChild(option);
        });
        // Đổ dữ liệu province khi sửa
        const fullAddressHost = document.getElementById("fullAddressHost");
        const initialAddress = fullAddressHost ? fullAddressHost.value.trim() : "";
        if (initialAddress) {
            const parts = initialAddress.split(",").map(part => part.trim()).filter(part => part.length > 0);
            if (parts.length >= 4) {
                const [, , , province] = parts;
                const provinceOption = Array.from(provinceSelect.options).find(opt => opt.textContent === province);
                if (provinceOption) {
                    provinceOption.selected = true;
                    provinceSelect.dispatchEvent(new Event("change"));
                }
            }
        }
    }

    populateDistricts() {
        const districtSelect = document.getElementById("districtHost");
        districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
        districtSelect.disabled = false;
        this.districts.forEach((district) => {
            const option = document.createElement("option");
            option.value = district.code;
            option.textContent = district.name;
            districtSelect.appendChild(option);
        });
        // Đổ dữ liệu district khi sửa
        const fullAddressHost = document.getElementById("fullAddressHost");
        const initialAddress = fullAddressHost ? fullAddressHost.value.trim() : "";
        if (initialAddress) {
            const parts = initialAddress.split(",").map(part => part.trim()).filter(part => part.length > 0);
            if (parts.length >= 3) {
                const [, , district] = parts;
                const districtOption = Array.from(districtSelect.options).find(opt => opt.textContent === district);
                if (districtOption) {
                    districtOption.selected = true;
                    districtSelect.dispatchEvent(new Event("change"));
                }
            }
        }
    }

    populateWards() {
        const wardSelect = document.getElementById("wardHost");
        wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>';
        wardSelect.disabled = false;
        this.wards.forEach((ward) => {
            const option = document.createElement("option");
            option.value = ward.code;
            option.textContent = ward.name;
            wardSelect.appendChild(option);
        });
        // Đổ dữ liệu ward khi sửa
        const fullAddressHost = document.getElementById("fullAddressHost");
        const initialAddress = fullAddressHost ? fullAddressHost.value.trim() : "";
        if (initialAddress) {
            const parts = initialAddress.split(",").map(part => part.trim()).filter(part => part.length > 0);
            if (parts.length >= 2) {
                const [, ward] = parts;
                const wardOption = Array.from(wardSelect.options).find(opt => opt.textContent === ward);
                if (wardOption) {
                    wardOption.selected = true;
                }
            }
        }
    }

    setInitialAddressFromForm() {
        const fullAddressHost = document.getElementById("fullAddressHost");
        const initialAddress = fullAddressHost ? fullAddressHost.value.trim() : "";
        if (initialAddress) {
            const parts = initialAddress.split(",").map(part => part.trim()).filter(part => part.length > 0);
            if (parts.length >= 4) {
                const [, wardName, districtName, provinceName] = parts; // Lấy theo thứ tự từ cuối
                const provinceSelect = document.getElementById("provinceHost");
                const districtSelect = document.getElementById("districtHost");
                const wardSelect = document.getElementById("wardHost");
                const houseNumberInput = document.getElementById("houseNumberHost");
                const streetInput = document.getElementById("streetHost");

                // Chọn province
                const provinceOption = Array.from(provinceSelect.options).find(opt => opt.textContent === provinceName);
                if (provinceOption) {
                    provinceOption.selected = true;
                    provinceSelect.dispatchEvent(new Event("change")); // Tải districts
                }

                // Chờ districts tải xong
                setTimeout(() => {
                    const districtOption = Array.from(districtSelect.options).find(opt => opt.textContent === districtName);
                    if (districtOption) {
                        districtOption.selected = true;
                        districtSelect.dispatchEvent(new Event("change")); // Tải wards
                    }

                    // Chờ wards tải xong
                    setTimeout(() => {
                        const wardOption = Array.from(wardSelect.options).find(opt => opt.textContent === wardName);
                        if (wardOption) {
                            wardOption.selected = true;
                        }
                    }, 500);
                }, 500);

                // Tách houseNumber và street
                const addressPart = parts[0].trim();
                const firstSpaceIndex = addressPart.indexOf(" ");
                houseNumberInput.value = firstSpaceIndex > 0 ? addressPart.substring(0, firstSpaceIndex) : addressPart;
                streetInput.value = firstSpaceIndex > 0 ? addressPart.substring(firstSpaceIndex + 1) : "";
            }
        }
    }
    
    resetDistrictSelect() {
        const districtSelect = document.getElementById("districtHost");
        districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
        districtSelect.disabled = true;
        this.resetWardSelect();
    }

    resetWardSelect() {
        const wardSelect = document.getElementById("wardHost");
        wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>';
        wardSelect.disabled = true;
    }

    setupEventListeners() {
        const provinceSelect = document.getElementById("provinceHost");
        const districtSelect = document.getElementById("districtHost");
        const wardSelect = document.getElementById("wardHost");
        const houseNumberInput = document.getElementById("houseNumberHost");
        const streetInput = document.getElementById("streetHost");
        const fullAddressInput = document.getElementById("fullAddressHost");

        provinceSelect.addEventListener("change", async (e) => {
            const provinceCode = e.target.value;
            if (provinceCode) {
                await this.loadDistricts(provinceCode);
            } else {
                this.resetDistrictSelect();
            }
            this.updateFullAddress();
        });

        districtSelect.addEventListener("change", async (e) => {
            const districtCode = e.target.value;
            if (districtCode) {
                await this.loadWards(districtCode);
            } else {
                this.resetWardSelect();
            }
            this.updateFullAddress();
        });

        wardSelect.addEventListener("change", () => this.updateFullAddress());
        houseNumberInput.addEventListener("input", () => this.updateFullAddress());
        streetInput.addEventListener("input", () => this.updateFullAddress());
    }

    updateFullAddress() {
        const provinceSelect = document.getElementById("provinceHost");
        const districtSelect = document.getElementById("districtHost");
        const wardSelect = document.getElementById("wardHost");
        const houseNumberInput = document.getElementById("houseNumberHost");
        const streetInput = document.getElementById("streetHost");
        const fullAddressInput = document.getElementById("fullAddressHost");

        // Hàm làm sạch triệt để
        const clean = (text) => (text || "").replace(/,+/g, "").replace(/\s+/g, " ").trim();

        const houseNumber = clean(houseNumberInput.value);
        const street = clean(streetInput.value);
        const ward = wardSelect.value ? clean(wardSelect.options[wardSelect.selectedIndex].text) : "";
        const district = districtSelect.value ? clean(districtSelect.options[districtSelect.selectedIndex].text) : "";
        const province = provinceSelect.value ? clean(provinceSelect.options[provinceSelect.selectedIndex].text) : "";

        // Gộp houseNumber và street thành một phần duy nhất nếu có street
        let addressPart = houseNumber;
        if (street) {
            addressPart += (houseNumber ? " " : "") + street;
        }

        const parts = [addressPart, ward, district, province].filter(part => part && part.length > 0);
        fullAddressInput.value = parts.join(", ").replace(/,+(?=,|$)/g, "").trim();
        console.log("Updated Full Address:", fullAddressInput.value);
    }

    showError(message) {
        const errorDiv = document.createElement("div");
        errorDiv.className = "alert alert-danger alert-dismissible fade show";
        errorDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        const formContainer = document.querySelector(".form-container-host");
        formContainer.insertBefore(errorDiv, formContainer.firstChild);

        setTimeout(() => {
            if (errorDiv.parentNode) errorDiv.remove();
        }, 5000);
    }

    setInitialAddressFromForm() {
        // Logic này đã được xử lý trong populateProvinces, populateDistricts, populateWards
    }

    getSelectedAddress() {
        const provinceSelect = document.getElementById("provinceHost");
        const districtSelect = document.getElementById("districtHost");
        const wardSelect = document.getElementById("wardHost");
        const houseNumberInput = document.getElementById("houseNumberHost");
        const streetInput = document.getElementById("streetHost");

        return {
            province: {
                code: provinceSelect.value,
                name: provinceSelect.options[provinceSelect.selectedIndex]?.text || "",
            },
            district: {
                code: districtSelect.value,
                name: districtSelect.options[districtSelect.selectedIndex]?.text || "",
            },
            ward: {
                code: wardSelect.value,
                name: wardSelect.options[wardSelect.selectedIndex]?.text || "",
            },
            street: streetInput.value.trim(),
            houseNumber: houseNumberInput.value.trim(),
            fullAddress: document.getElementById("fullAddressHost").value,
        };
    }
}

document.addEventListener("DOMContentLoaded", () => {
    window.vietnamAddressAPI = new VietnamAddressAPI();

    // Xử lý submit form
    const form = document.getElementById("addHostelFormHost");
        if (form) {
            form.addEventListener("submit", function (e) {
            e.preventDefault();

            let address = window.vietnamAddressAPI.getSelectedAddress();

            // Hàm làm sạch triệt để
            const clean = (text) => (text || "").replace(/,+/g, "").replace(/\s+/g, " ").trim();

            address.houseNumber = clean(address.houseNumber);
            address.street = clean(address.street);
            address.ward.name = clean(address.ward.name);
            address.district.name = clean(address.district.name);
            address.province.name = clean(address.province.name);

            // Gộp phần đầu địa chỉ: houseNumber + street
            const addressPart = [address.houseNumber, address.street]
                .filter(Boolean)
                .join(" ");

            // Gộp địa chỉ đầy đủ
            const fullAddress = [addressPart, address.ward.name, address.district.name, address.province.name]
                .filter(Boolean)
                .join(", ");

            // Gán lại giá trị input
            document.getElementById("houseNumberHost").value = address.houseNumber;
            document.getElementById("streetHost").value = address.street;
            document.getElementById("fullAddressHost").value = fullAddress;
            document.getElementById("provinceCodeHost").value = address.province.code;
            document.getElementById("provinceNameHost").value = address.province.name;
            document.getElementById("districtCodeHost").value = address.district.code;
            document.getElementById("districtNameHost").value = address.district.name;
            document.getElementById("wardCodeHost").value = address.ward.code;
            document.getElementById("wardNameHost").value = address.ward.name;

            console.log("Submitting cleaned address:", fullAddress);

            // Gửi form
            this.submit();
        });
    }
});