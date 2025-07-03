// class VietnamAddressAPI {
//     constructor() {
//         this.baseURL = "https://provinces.open-api.vn/api"
//         this.provinces = []
//         this.districts = []
//         this.wards = []
//         this.init()
//     }

//     async init() {
//         await this.loadProvinces()
//         this.setupEventListeners()
//         this.setInitialAddressFromHiddenFields()
//     }

//     async loadProvinces() {
//         try {
//             const response = await fetch(`${this.baseURL}/p/`)
//             const data = await response.json()
//             this.provinces = data
//             this.populateProvinces()
//         } catch (error) {
//             console.error("Lỗi khi tải tỉnh thành:", error)
//             this.showError("Không thể tải danh sách tỉnh/thành phố")
//         }
//     }

//     async loadDistricts(provinceCode) {
//         try {
//             const response = await fetch(`${this.baseURL}/p/${provinceCode}?depth=2`)
//             const data = await response.json()
//             this.districts = data.districts || []
//             this.populateDistricts()
//         } catch (error) {
//             console.error("Lỗi khi tải quận/huyện:", error)
//             this.showError("Không thể tải danh sách quận/huyện")
//         }
//     }

//     async loadWards(districtCode) {
//         try {
//             const response = await fetch(`${this.baseURL}/d/${districtCode}?depth=2`)
//             const data = await response.json()
//             this.wards = data.wards || []
//             this.populateWards()
//         } catch (error) {
//             console.error("Lỗi khi tải phường/xã:", error)
//             this.showError("Không thể tải danh sách phường/xã")
//         }
//     }

//     populateProvinces() {
//         const provinceSelect = document.getElementById("provinceHost")
//         provinceSelect.innerHTML = '<option value="">Chọn tỉnh/thành phố</option>'
//         this.provinces.forEach((province) => {
//             const option = document.createElement("option")
//             option.value = province.code
//             option.textContent = province.name
//             provinceSelect.appendChild(option)
//         })
//     }

//     populateDistricts() {
//         const districtSelect = document.getElementById("districtHost")
//         districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>'
//         districtSelect.disabled = false

//         this.districts.forEach((district) => {
//             const option = document.createElement("option")
//             option.value = district.code
//             option.textContent = district.name
//             districtSelect.appendChild(option)
//         })

//         this.resetWardSelect()
//     }

//     populateWards() {
//         const wardSelect = document.getElementById("wardHost")
//         wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>'
//         wardSelect.disabled = false

//         this.wards.forEach((ward) => {
//             const option = document.createElement("option")
//             option.value = ward.code
//             option.textContent = ward.name
//             wardSelect.appendChild(option)
//         })
//     }

//     resetDistrictSelect() {
//         const districtSelect = document.getElementById("districtHost")
//         districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>'
//         districtSelect.disabled = true
//         this.resetWardSelect()
//     }

//     resetWardSelect() {
//         const wardSelect = document.getElementById("wardHost")
//         wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>'
//         wardSelect.disabled = true
//     }

//     setupEventListeners() {
//         const provinceSelect = document.getElementById("provinceHost")
//         const districtSelect = document.getElementById("districtHost")
//         const wardSelect = document.getElementById("wardHost")
//         const houseNumberInput = document.getElementById("houseNumberHost")
//         const streetInput = document.getElementById("streetHost")

//         provinceSelect.addEventListener("change", async (e) => {
//             const provinceCode = e.target.value
//             if (provinceCode) {
//                 await this.loadDistricts(provinceCode)
//             } else {
//                 this.resetDistrictSelect()
//             }
//             this.updateFullAddress()
//         })

//         districtSelect.addEventListener("change", async (e) => {
//             const districtCode = e.target.value
//             if (districtCode) {
//                 await this.loadWards(districtCode)
//             } else {
//                 this.resetWardSelect()
//             }
//             this.updateFullAddress()
//         })

//         wardSelect.addEventListener("change", () => this.updateFullAddress())
//         houseNumberInput.addEventListener("input", () => this.updateFullAddress())
//         streetInput.addEventListener("input", () => this.updateFullAddress())
//     }

//     updateFullAddress() {
//         const provinceSelect = document.getElementById("provinceHost")
//         const districtSelect = document.getElementById("districtHost")
//         const wardSelect = document.getElementById("wardHost")
//         const houseNumberInput = document.getElementById("houseNumberHost")
//         const streetInput = document.getElementById("streetHost")
//         const fullAddressInput = document.getElementById("fullAddressHost")

//         const parts = []
//         if (houseNumberInput.value.trim()) parts.push(houseNumberInput.value.trim())
//         if (streetInput.value.trim()) parts.push(streetInput.value.trim())
//         if (wardSelect.value) parts.push(wardSelect.options[wardSelect.selectedIndex].text)
//         if (districtSelect.value) parts.push(districtSelect.options[districtSelect.selectedIndex].text)
//         if (provinceSelect.value) parts.push(provinceSelect.options[provinceSelect.selectedIndex].text)

//         fullAddressInput.value = parts.join(", ")
//     }

//     showError(message) {
//         const errorDiv = document.createElement("div")
//         errorDiv.className = "alert alert-danger alert-dismissible fade show"
//         errorDiv.innerHTML = `
//             ${message}
//             <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
//         `
//         const formContainer = document.querySelector(".form-container-host")
//         formContainer.insertBefore(errorDiv, formContainer.firstChild)

//         setTimeout(() => {
//             if (errorDiv.parentNode) errorDiv.remove()
//         }, 5000)
//     }

//     async setInitialAddress(provinceCode, districtCode, wardCode) {
//         if (provinceCode) {
//             document.getElementById("provinceHost").value = provinceCode
//             await this.loadDistricts(provinceCode)
//         }
//         if (districtCode) {
//             document.getElementById("districtHost").value = districtCode
//             await this.loadWards(districtCode)
//         }
//         if (wardCode) {
//             document.getElementById("wardHost").value = wardCode
//         }
//         this.updateFullAddress()
//     }

//     setInitialAddressFromHiddenFields() {
//         const province = document.getElementById("hiddenProvince")?.value
//         const district = document.getElementById("hiddenDistrict")?.value
//         const ward = document.getElementById("hiddenWard")?.value

//         if (province && district && ward) {
//             this.setInitialAddress(province, district, ward)
//         }
//     }

//     getSelectedAddress() {
//         const provinceSelect = document.getElementById("provinceHost")
//         const districtSelect = document.getElementById("districtHost")
//         const wardSelect = document.getElementById("wardHost")
//         const houseNumberInput = document.getElementById("houseNumberHost")
//         const streetInput = document.getElementById("streetHost")

//         return {
//             province: {
//                 code: provinceSelect.value,
//                 name: provinceSelect.options[provinceSelect.selectedIndex]?.text || "",
//             },
//             district: {
//                 code: districtSelect.value,
//                 name: districtSelect.options[districtSelect.selectedIndex]?.text || "",
//             },
//             ward: {
//                 code: wardSelect.value,
//                 name: wardSelect.options[wardSelect.selectedIndex]?.text || "",
//             },
//             street: streetInput.value.trim(),
//             houseNumber: houseNumberInput.value.trim(),
//             fullAddress: document.getElementById("fullAddressHost").value,
//         }
//     }
// }

// document.addEventListener("DOMContentLoaded", () => {
//     window.vietnamAddressAPI = new VietnamAddressAPI()
// })

// const style = document.createElement("style")
// style.textContent = `
//     .form-select-host:disabled {
//         background-color: #f8f9fa;
//         opacity: 0.65;
//     }
    
//     .alert {
//         margin-bottom: 1rem;
//     }
// `
// document.head.appendChild(style)
