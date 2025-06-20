class VietnamAddressAPI {
    constructor() {
        this.baseURL = "https://provinces.open-api.vn/api"
        this.provinces = []
        this.districts = []
        this.wards = []
        this.init()
    }

    async init() {
        await this.loadProvinces()
        this.setupEventListeners()
    }

    async loadProvinces() {
        try {
            const response = await fetch(`${this.baseURL}/p/`)
            const data = await response.json()
            this.provinces = data
            this.populateProvinces()
        } catch (error) {
            console.error("Lỗi khi tải danh sách tỉnh thành:", error)
            this.showError("Không thể tải danh sách tỉnh thành")
        }
    }

    async loadDistricts(provinceCode) {
        try {
            const response = await fetch(`${this.baseURL}/p/${provinceCode}?depth=2`)
            const data = await response.json()
            this.districts = data.districts || []
            this.populateDistricts()
        } catch (error) {
            console.error("Lỗi khi tải danh sách quận/huyện:", error)
            this.showError("Không thể tải danh sách quận/huyện")
        }
    }

    async loadWards(districtCode) {
        try {
            const response = await fetch(`${this.baseURL}/d/${districtCode}?depth=2`)
            const data = await response.json()
            this.wards = data.wards || []
            this.populateWards()
        } catch (error) {
            console.error("Lỗi khi tải danh sách phường/xã:", error)
            this.showError("Không thể tải danh sách phường/xã")
        }
    }

    populateProvinces() {
        const provinceSelect = document.getElementById("provinceHost")
        provinceSelect.innerHTML = '<option value="">Chọn tỉnh/thành phố</option>'

        this.provinces.forEach((province) => {
            const option = document.createElement("option")
            option.value = province.code
            option.textContent = province.name
            provinceSelect.appendChild(option)
        })
    }

    populateDistricts() {
        const districtSelect = document.getElementById("districtHost")
        districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>'
        districtSelect.disabled = false

        this.districts.forEach((district) => {
            const option = document.createElement("option")
            option.value = district.code
            option.textContent = district.name
            districtSelect.appendChild(option)
        })

        // Reset ward select
        this.resetWardSelect()
    }

    populateWards() {
        const wardSelect = document.getElementById("wardHost")
        wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>'
        wardSelect.disabled = false

        this.wards.forEach((ward) => {
            const option = document.createElement("option")
            option.value = ward.code
            option.textContent = ward.name
            wardSelect.appendChild(option)
        })
    }

    resetDistrictSelect() {
        const districtSelect = document.getElementById("districtHost")
        districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>'
        districtSelect.disabled = true
        this.resetWardSelect()
    }

    resetWardSelect() {
        const wardSelect = document.getElementById("wardHost")
        wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>'
        wardSelect.disabled = true
    }

    setupEventListeners() {
        const provinceSelect = document.getElementById("provinceHost")
        const districtSelect = document.getElementById("districtHost")
        const wardSelect = document.getElementById("wardHost")
        const houseNumberInput = document.getElementById("houseNumberHost")
        const streetInput = document.getElementById("streetHost")

        provinceSelect.addEventListener("change", async (e) => {
            const provinceCode = e.target.value
            if (provinceCode) {
                await this.loadDistricts(provinceCode)
            } else {
                this.resetDistrictSelect()
            }
            this.updateFullAddress()
        })

        districtSelect.addEventListener("change", async (e) => {
            const districtCode = e.target.value
            if (districtCode) {
                await this.loadWards(districtCode)
            } else {
                this.resetWardSelect()
            }
            this.updateFullAddress()
        })

        wardSelect.addEventListener("change", () => {
            this.updateFullAddress()
        })

        houseNumberInput.addEventListener("input", () => {
            this.updateFullAddress()
        })

        streetInput.addEventListener("input", () => {
            this.updateFullAddress()
        })
    }

    updateFullAddress() {
        const provinceSelect = document.getElementById("provinceHost")
        const districtSelect = document.getElementById("districtHost")
        const wardSelect = document.getElementById("wardHost")
        const houseNumberInput = document.getElementById("houseNumberHost")
        const streetInput = document.getElementById("streetHost")
        const fullAddressInput = document.getElementById("fullAddressHost")

        const addressParts = []

        if (houseNumberInput.value.trim()) {
            addressParts.push(houseNumberInput.value.trim())
        }

        if (streetInput.value.trim()) {
            addressParts.push(streetInput.value.trim())
        }

        if (wardSelect.value) {
            const selectedWard = wardSelect.options[wardSelect.selectedIndex].text
            addressParts.push(selectedWard)
        }

        if (districtSelect.value) {
            const selectedDistrict = districtSelect.options[districtSelect.selectedIndex].text
            addressParts.push(selectedDistrict)
        }

        if (provinceSelect.value) {
            const selectedProvince = provinceSelect.options[provinceSelect.selectedIndex].text
            addressParts.push(selectedProvince)
        }

        fullAddressInput.value = addressParts.join(", ")
    }

    showError(message) {
        // Tạo thông báo lỗi
        const errorDiv = document.createElement("div")
        errorDiv.className = "alert alert-danger alert-dismissible fade show"
        errorDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `

        // Thêm vào đầu form
        const formContainer = document.querySelector(".form-container-host")
        formContainer.insertBefore(errorDiv, formContainer.firstChild)

        // Tự động ẩn sau 5 giây
        setTimeout(() => {
            if (errorDiv.parentNode) {
                errorDiv.remove()
            }
        }, 5000)
    }

    // Phương thức để lấy thông tin địa chỉ đã chọn
    getSelectedAddress() {
        const provinceSelect = document.getElementById("provinceHost")
        const districtSelect = document.getElementById("districtHost")
        const wardSelect = document.getElementById("wardHost")
        const houseNumberInput = document.getElementById("houseNumberHost")
        const streetInput = document.getElementById("streetHost")

        return {
            province: {
                code: provinceSelect.value,
                name: provinceSelect.value ? provinceSelect.options[provinceSelect.selectedIndex].text : "",
            },
            district: {
                code: districtSelect.value,
                name: districtSelect.value ? districtSelect.options[districtSelect.selectedIndex].text : "",
            },
            ward: {
                code: wardSelect.value,
                name: wardSelect.value ? wardSelect.options[wardSelect.selectedIndex].text : "",
            },
            street: streetInput.value.trim(),
            houseNumber: houseNumberInput.value.trim(),
            fullAddress: document.getElementById("fullAddressHost").value,
        }
    }
}

$(document).ready(function() {
    // Tải danh sách tỉnh/thành
    $.getJSON("https://provinces.open-api.vn/api/p/", function(data) {
        $("#provinceHost").append(data.map(p => `<option value="${p.code}">${p.name}</option>`));
    });

    // Tải quận/huyện khi chọn tỉnh/thành
    $("#provinceHost").change(function() {
        let provinceCode = $(this).val();
        $("#districtHost").prop("disabled", false).empty().append('<option value="">Chọn quận/huyện</option>');
        $("#wardHost").prop("disabled", true).empty().append('<option value="">Chọn phường/xã</option>');
        if (provinceCode) {
            $.getJSON(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`, function(data) {
                $("#districtHost").append(data.districts.map(d => `<option value="${d.code}">${d.name}</option>`));
            });
        }
    });

    // Tải phường/xã khi chọn quận/huyện
    $("#districtHost").change(function() {
        let districtCode = $(this).val();
        $("#wardHost").prop("disabled", false).empty().append('<option value="">Chọn phường/xã</option>');
        if (districtCode) {
            $.getJSON(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`, function(data) {
                $("#wardHost").append(data.wards.map(w => `<option value="${w.code}">${w.name}</option>`));
            });
        }
    });

    // Tự động tạo địa chỉ đầy đủ
    $("#provinceHost, #districtHost, #wardHost, #streetHost, #houseNumberHost").change(function() {
        let province = $("#provinceHost option:selected").text();
        let district = $("#districtHost option:selected").text();
        let ward = $("#wardHost option:selected").text();
        let street = $("#streetHost").val();
        let houseNumber = $("#houseNumberHost").val();
        let fullAddress = `${houseNumber ? houseNumber + ', ' : ''}${street ? street + ', ' : ''}${ward ? ward + ', ' : ''}${district ? district + ', ' : ''}${province}`;
        $("#fullAddressHost").val(fullAddress.trim());
    });
});

// Khởi tạo khi trang web được tải
document.addEventListener("DOMContentLoaded", () => {
    window.vietnamAddressAPI = new VietnamAddressAPI()
})

// Thêm CSS cho loading state
const style = document.createElement("style")
style.textContent = `
    .form-select-host:disabled {
        background-color: #f8f9fa;
        opacity: 0.65;
    }
    
    .loading-option {
        color: #6c757d;
        font-style: italic;
    }
    
    .alert {
        margin-bottom: 1rem;
    }
`
document.head.appendChild(style)
