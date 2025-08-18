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
        await this.setInitialAddressFromForm();
    }

    async setInitialAddressFromForm() {
        const fullAddressHost = document.getElementById("fullAddressHost");
        if (!fullAddressHost) return;

        const initialAddress = fullAddressHost.value.trim();
        if (!initialAddress) return;

        const parts = initialAddress.split(",").map(part => part.trim()).filter(part => part.length > 0);
        const normalize = (text) => text.toLowerCase()
            .replace("tỉnh", "")
            .replace("thành phố", "")
            .replace("huyện", "")
            .replace("quận", "")
            .replace("thị xã", "")
            .replace("xã", "")
            .replace("phường", "")
            .replace("thị trấn", "")
            .trim();

        const provinceSelect = document.getElementById("provinceHost");
        const districtSelect = document.getElementById("districtHost");
        const wardSelect = document.getElementById("wardHost");
        const streetInput = document.getElementById("streetHost");

        // Step 1: Tỉnh/Thành phố
        if (parts.length >= 4) {
            const provinceName = parts[3];
            const provinceOption = Array.from(provinceSelect.options).find(opt =>
                normalize(opt.textContent) === normalize(provinceName)
            );
            if (provinceOption) {
                provinceOption.selected = true;
                await this.loadDistricts(provinceOption.value);
            }
        }

        // Step 2: Quận/Huyện
        if (parts.length >= 3) {
            const districtName = parts[2];
            const districtOption = Array.from(districtSelect.options).find(opt =>
                normalize(opt.textContent) === normalize(districtName)
            );
            if (districtOption) {
                districtOption.selected = true;
                await this.loadWards(districtOption.value);
            }
        }

        // Step 3: Phường/Xã
        if (parts.length >= 2) {
            const wardName = parts[1];
            const wardOption = Array.from(wardSelect.options).find(opt =>
                normalize(opt.textContent) === normalize(wardName)
            );
            if (wardOption) {
                wardOption.selected = true;
            }
        }

        // Step 4: Đường/phố
        if (parts.length >= 1) {
            streetInput.value = parts[0].trim();
        }

        this.updateFullAddress();
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
        const streetInput = document.getElementById("streetHost");

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
        streetInput.addEventListener("input", () => this.updateFullAddress());
    }

    updateFullAddress() {
        const provinceSelect = document.getElementById("provinceHost");
        const districtSelect = document.getElementById("districtHost");
        const wardSelect = document.getElementById("wardHost");
        const streetInput = document.getElementById("streetHost");
        const fullAddressInput = document.getElementById("fullAddressHost");

        const clean = (text) => (text || "").replace(/,+/g, "").replace(/\s+/g, " ").trim();

        const street = clean(streetInput.value);
        const ward = wardSelect.value ? clean(wardSelect.options[wardSelect.selectedIndex].text) : "";
        const district = districtSelect.value ? clean(districtSelect.options[districtSelect.selectedIndex].text) : "";
        const province = provinceSelect.value ? clean(provinceSelect.options[provinceSelect.selectedIndex].text) : "";

        const parts = [street, ward, district, province].filter(part => part && part.length > 0);
        fullAddressInput.value = parts.join(", ").replace(/,+(?=,|$)/g, "").trim();
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

    getSelectedAddress() {
        const provinceSelect = document.getElementById("provinceHost");
        const districtSelect = document.getElementById("districtHost");
        const wardSelect = document.getElementById("wardHost");
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
            fullAddress: document.getElementById("fullAddressHost").value,
        };
    }

    validateForm() {
        console.log('API Map - Validating form...');
        let valid = true;

        // Xóa lỗi cũ
        document.querySelectorAll('.invalid-feedback').forEach(el => {
            el.textContent = '';
            el.classList.remove('show');
        });
        document.querySelectorAll('.is-invalid').forEach(el => {
            el.classList.remove('is-invalid');
        });

        const showError = (fieldId, message) => {
            const field = document.getElementById(fieldId);
            const errorElement = document.getElementById('error-' + fieldId);

            if (field && errorElement) {
                field.classList.add('is-invalid');
                errorElement.textContent = message;
                errorElement.classList.add('show');
            }
        };

        // Kiểm tra tên khu trọ
        const name = document.getElementById('hostelNameHost').value.trim();
        if (!name) {
            showError('hostelNameHost', 'Vui lòng nhập tên khu trọ.');
            valid = false;
        }

        // Kiểm tra số phòng
        const roomNumber = document.getElementById('roomCountHost').value.trim();
        if (!roomNumber || isNaN(roomNumber) || roomNumber <= 0) {
            showError('roomCountHost', 'Số phòng phải là số lớn hơn 0.');
            valid = false;
        }

        // Kiểm tra tỉnh/thành phố
        const province = document.getElementById('provinceHost').value;
        if (!province) {
            showError('provinceHost', 'Vui lòng chọn tỉnh/thành phố.');
            valid = false;
        }

        // Kiểm tra quận/huyện
        const district = document.getElementById('districtHost').value;
        if (!district) {
            showError('districtHost', 'Vui lòng chọn quận/huyện.');
            valid = false;
        }

        // Kiểm tra phường/xã
        const ward = document.getElementById('wardHost').value;
        if (!ward) {
            showError('wardHost', 'Vui lòng chọn phường/xã.');
            valid = false;
        }

        // Kiểm tra đường/phố
        const street = document.getElementById('streetHost').value.trim();
        if (!street) {
            showError('streetHost', 'Vui lòng nhập tên đường/phố.');
            valid = false;
        }

        return valid;
    }

    processFormData() {
        let address = this.getSelectedAddress();

        const clean = (text) => (text || "").replace(/,+/g, "").replace(/\s+/g, " ").trim();

        address.street = clean(address.street);
        address.ward.name = clean(address.ward.name);
        address.district.name = clean(address.district.name);
        address.province.name = clean(address.province.name);

        const fullAddress = [address.street, address.ward.name, address.district.name, address.province.name]
            .filter(Boolean)
            .join(", ");

        document.getElementById("streetHost").value = address.street;
        document.getElementById("fullAddressHost").value = fullAddress;
        document.getElementById("provinceCodeHost").value = address.province.code;
        document.getElementById("provinceNameHost").value = address.province.name;
        document.getElementById("districtCodeHost").value = address.district.code;
        document.getElementById("districtNameHost").value = address.district.name;
        document.getElementById("wardCodeHost").value = address.ward.code;
        document.getElementById("wardNameHost").value = address.ward.name;

        console.log("Submitting cleaned address:", fullAddress);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    window.vietnamAddressAPI = new VietnamAddressAPI();

    const form = document.getElementById("addHostelFormHost");
    if (form) {
        form.addEventListener("submit", function (e) {
            e.preventDefault();
            e.stopPropagation();

            const isValid = window.vietnamAddressAPI.validateForm();

            if (isValid) {
                window.vietnamAddressAPI.processFormData();
                setTimeout(() => {
                    this.submit();
                }, 100);
            } else {
                setTimeout(() => {
                    const firstError = document.querySelector('.is-invalid');
                    if (firstError) {
                        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' });
                        firstError.focus();
                    }
                }, 100);
            }
        });
    }
});

// Kiểm tra realtime số phòng
const roomNumberField = document.getElementById("roomCountHost");
const errorField = document.getElementById("error-roomCountHost");

if (roomNumberField) {
    roomNumberField.addEventListener("input", function () {
        const value = parseInt(roomNumberField.value, 10);

        if (isNaN(value) || value < 1) {
            errorField.innerText = "Số phòng phải từ 1 trở lên";
            roomNumberField.classList.add("is-invalid");
        } else if (value > 20) {
            errorField.innerText = "Số phòng tối đa là 20";
            roomNumberField.classList.add("is-invalid");
        } else {
            errorField.innerText = "";
            roomNumberField.classList.remove("is-invalid");
        }
    });
}

// Kiểm tra tên khu trọ bắt buộc bắt đầu bằng "Khu"
document.getElementById("addHostelFormHost").addEventListener("submit", function (e) {
    const hostelNameInput = document.getElementById("hostelNameHost");
    const errorMsg = document.getElementById("error-hostelNameHost");

    const value = hostelNameInput.value.trim();

    if (!/^Khu/i.test(value)) {
        e.preventDefault();
        errorMsg.textContent = "Tên khu trọ phải bắt đầu bằng từ 'Khu'.";
        hostelNameInput.classList.add("is-invalid");
    } else {
        errorMsg.textContent = "";
        hostelNameInput.classList.remove("is-invalid");
    }
});
