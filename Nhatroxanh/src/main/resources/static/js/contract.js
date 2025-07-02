// // Namespace để tránh xung đột
// window.NhaTroContract = {
//     // Properties
//     currentTab: "tenantInfo",
//     zoomLevel: 1,
//     residents: [],
//     bootstrap: null, // Will be initialized after DOM loads
//
//     // Initialize application
//     init() {
//         // Initialize bootstrap reference
//         this.bootstrap = window.bootstrap || window.bootstrap // Declared variable here
//
//         this.setupEventListeners()
//         this.setCurrentDate()
//         this.updateAllPreview()
//         this.setupAmenityModal()
//         this.setupCustomerModal()
//         this.setupResidentModal()
//         this.loadProvinces()
//     },
//
//     // Setup all event listeners
//     setupEventListeners() {
//         // Tab navigation
//         this.setupTabListeners()
//
//         // Button events
//         this.setupButtonListeners()
//
//         // Image upload events
//         this.setupImageListeners()
//
//         // Form input events for live preview
//         this.setupPreviewListeners()
//
//         // Location listeners
//         this.setupLocationListeners()
//     },
//
//     setupTabListeners() {
//         document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
//             link.addEventListener("click", (e) => {
//                 e.preventDefault()
//                 const tabId = link.getAttribute("data-tab")
//                 this.showTab(tabId)
//             })
//         })
//     },
//
//     setupButtonListeners() {
//         // Navigation buttons
//         const buttonMappings = [
//             { id: "btn-next-owner", action: () => this.showTab("ownerInfo") },
//             { id: "btn-prev-tenant", action: () => this.showTab("tenantInfo") },
//             { id: "btn-next-room", action: () => this.showTab("roomInfo") },
//             { id: "btn-prev-owner", action: () => this.showTab("ownerInfo") },
//             { id: "btn-next-terms", action: () => this.showTab("terms") },
//             { id: "btn-prev-room", action: () => this.showTab("roomInfo") },
//
//             // Action buttons
//             { id: "btn-update", action: () => this.updateContract() },
//             { id: "btn-print", action: () => this.printContract() },
//             { id: "btn-save", action: () => this.saveContract() },
//
//             // Zoom buttons
//             { id: "btn-zoom-in", action: () => this.zoomIn() },
//             { id: "btn-zoom-out", action: () => this.zoomOut() },
//             { id: "btn-reset-zoom", action: () => this.resetZoom() },
//         ]
//
//         buttonMappings.forEach(({ id, action }) => {
//             const element = document.getElementById(id)
//             if (element) {
//                 element.addEventListener("click", action)
//             }
//         })
//     },
//
//     setupImageListeners() {
//         const imageInputs = [
//             { id: "cccd-front", preview: "cccd-front-preview" },
//             { id: "cccd-back", preview: "cccd-back-preview" },
//         ]
//
//         imageInputs.forEach(({ id, preview }) => {
//             const element = document.getElementById(id)
//             if (element) {
//                 element.addEventListener("change", (e) => {
//                     this.previewImage(e, preview)
//                 })
//             }
//         })
//     },
//
//     setupPreviewListeners() {
//         const inputs = [
//             { id: "tenant-name", preview: "preview-tenant-name" },
//             { id: "tenant-dob", preview: "preview-tenant-dob" },
//             { id: "tenant-id", preview: "preview-tenant-id" },
//             { id: "tenant-id-date", preview: "preview-tenant-id-date" },
//             { id: "tenant-id-place", preview: "preview-tenant-id-place" },
//             { id: "tenant-phone", preview: "preview-tenant-phone" },
//             { id: "owner-name", preview: "preview-owner-name" },
//             { id: "owner-dob", preview: "preview-owner-dob" },
//             { id: "owner-id", preview: "preview-owner-id" },
//             { id: "owner-id-date", preview: "preview-owner-id-date" },
//             { id: "owner-id-place", preview: "preview-owner-id-place" },
//             { id: "owner-phone", preview: "preview-owner-phone" },
//             { id: "room-number", preview: "preview-room-number" },
//             { id: "room-area", preview: "preview-room-area" },
//             { id: "rent-price", preview: "preview-rent" },
//             { id: "contract-duration", preview: "preview-duration" },
//             { id: "start-date", preview: "preview-start-date" },
//             { id: "contract-date", preview: "preview-sign-date" },
//             { id: "payment-date", preview: "preview-payment-date" },
//             { id: "deposit-months", preview: "preview-deposit-months" },
//             { id: "terms-conditions", preview: "preview-terms" },
//         ]
//
//         inputs.forEach((input) => {
//             const element = document.getElementById(input.id)
//             if (element) {
//                 element.addEventListener("input", () => {
//                     this.updatePreviewField(input.id, input.preview)
//                 })
//                 element.addEventListener("change", () => {
//                     this.updatePreviewField(input.id, input.preview)
//                 })
//             }
//         })
//
//         // Special handlers
//         const paymentMethod = document.getElementById("payment-method")
//         if (paymentMethod) {
//             paymentMethod.addEventListener("change", () => {
//                 this.updatePaymentMethod()
//             })
//         }
//
//         // Amenities checkboxes
//         document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]').forEach((checkbox) => {
//             checkbox.addEventListener("change", () => {
//                 this.updateAmenities()
//             })
//         })
//
//         // Address fields
//         const addressFields = ["tenant", "owner", "room"]
//         addressFields.forEach((prefix) => {
//             ;["province", "district", "ward", "street"].forEach((field) => {
//                 const element = document.getElementById(`${prefix}-${field}`)
//                 if (element) {
//                     element.addEventListener("change", () => {
//                         this.updateAddress(prefix)
//                     })
//                 }
//             })
//         })
//     },
//
//     // Location API functions
//     async loadProvinces() {
//         try {
//             const response = await fetch("https://provinces.open-api.vn/api/p/")
//             const provinces = await response.json()
//
//             const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"]
//             selects.forEach((selectId) => {
//                 const select = document.getElementById(selectId)
//                 if (select) {
//                     select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>'
//                     provinces.forEach((province) => {
//                         const option = document.createElement("option")
//                         option.value = province.code
//                         option.textContent = province.name
//                         select.appendChild(option)
//                     })
//                 }
//             })
//         } catch (error) {
//             console.error("Error loading provinces:", error)
//             this.showNotification("Không thể tải danh sách tỉnh/thành phố", "warning")
//         }
//     },
//
//     async loadDistricts(provinceCode, districtSelectId, wardSelectId) {
//         try {
//             const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
//             const province = await response.json()
//
//             const districtSelect = document.getElementById(districtSelectId)
//             const wardSelect = document.getElementById(wardSelectId)
//
//             if (districtSelect) {
//                 districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
//                 province.districts.forEach((district) => {
//                     const option = document.createElement("option")
//                     option.value = district.code
//                     option.textContent = district.name
//                     districtSelect.appendChild(option)
//                 })
//             }
//
//             if (wardSelect) {
//                 wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
//             }
//         } catch (error) {
//             console.error("Error loading districts:", error)
//             this.showNotification("Không thể tải danh sách quận/huyện", "warning")
//         }
//     },
//
//     async loadWards(districtCode, wardSelectId) {
//         try {
//             const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
//             const district = await response.json()
//
//             const wardSelect = document.getElementById(wardSelectId)
//             if (wardSelect) {
//                 wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
//                 district.wards.forEach((ward) => {
//                     const option = document.createElement("option")
//                     option.value = ward.code
//                     option.textContent = ward.name
//                     wardSelect.appendChild(option)
//                 })
//             }
//         } catch (error) {
//             console.error("Error loading wards:", error)
//             this.showNotification("Không thể tải danh sách phường/xã", "warning")
//         }
//     },
//
//     setupLocationListeners() {
//         const prefixes = ["tenant", "owner", "room"]
//
//         prefixes.forEach((prefix) => {
//             const provinceSelect = document.getElementById(`${prefix}-province`)
//             const districtSelect = document.getElementById(`${prefix}-district`)
//             const wardSelect = document.getElementById(`${prefix}-ward`)
//
//             if (provinceSelect) {
//                 provinceSelect.addEventListener("change", () => {
//                     if (provinceSelect.value) {
//                         this.loadDistricts(provinceSelect.value, `${prefix}-district`, `${prefix}-ward`)
//                     } else {
//                         if (districtSelect) districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
//                         if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
//                     }
//                     this.updateAddress(prefix)
//                 })
//             }
//
//             if (districtSelect) {
//                 districtSelect.addEventListener("change", () => {
//                     if (districtSelect.value) {
//                         this.loadWards(districtSelect.value, `${prefix}-ward`)
//                     } else {
//                         if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
//                     }
//                     this.updateAddress(prefix)
//                 })
//             }
//
//             if (wardSelect) {
//                 wardSelect.addEventListener("change", () => {
//                     this.updateAddress(prefix)
//                 })
//             }
//         })
//     },
//
//     // Resident management
//     setupResidentModal() {
//         const addResidentBtn = document.getElementById("btn-add-resident")
//         const saveResidentBtn = document.getElementById("btn-save-resident")
//         const searchBtn = document.getElementById("btn-search-customer")
//
//         if (addResidentBtn) {
//             addResidentBtn.addEventListener("click", () => {
//                 const modal = new window.bootstrap.Modal(document.getElementById("addResidentModal"))
//                 modal.show()
//                 this.clearResidentForm()
//             })
//         }
//
//         if (saveResidentBtn) {
//             saveResidentBtn.addEventListener("click", () => {
//                 this.saveResident()
//             })
//         }
//
//         if (searchBtn) {
//             searchBtn.addEventListener("click", () => {
//                 this.searchCustomer()
//             })
//         }
//
//         // Enter key to search
//         const searchInput = document.getElementById("search-customer-phone")
//         if (searchInput) {
//             searchInput.addEventListener("keypress", (e) => {
//                 if (e.key === "Enter") {
//                     e.preventDefault()
//                     this.searchCustomer()
//                 }
//             })
//         }
//     },
//
//     async searchCustomer() {
//         const phone = document.getElementById("search-customer-phone").value.trim()
//         const resultsDiv = document.getElementById("search-results")
//
//         if (!phone) {
//             this.showNotification("Vui lòng nhập số điện thoại", "warning")
//             return
//         }
//
//         // Show loading
//         resultsDiv.innerHTML = `
//             <div class="text-center py-3">
//                 <div class="spinner-border spinner-border-sm me-2" role="status"></div>
//                 Đang tìm kiếm...
//             </div>
//         `
//
//         try {
//             // Simulate API call - replace with actual API
//             await new Promise((resolve) => setTimeout(resolve, 1000))
//
//             // Mock data - replace with actual API response
//             const mockCustomers = [
//                 {
//                     id: 1,
//                     name: "Nguyễn Văn A",
//                     phone: "0123456789",
//                     birthYear: 1990,
//                     idNumber: "123456789",
//                     isRegistered: true,
//                 },
//                 {
//                     id: 2,
//                     name: "Trần Thị B",
//                     phone: "0987654321",
//                     birthYear: 1995,
//                     idNumber: "987654321",
//                     isRegistered: true,
//                 },
//             ]
//
//             const foundCustomers = mockCustomers.filter((customer) => customer.phone.includes(phone))
//
//             if (foundCustomers.length > 0) {
//                 resultsDiv.innerHTML = `
//                     <div class="border rounded p-3 bg-light">
//                         <h6 class="mb-3">Tìm thấy ${foundCustomers.length} khách hàng:</h6>
//                         ${foundCustomers
//                         .map(
//                             (customer) => `
//                             <div class="card mb-2">
//                                 <div class="card-body p-3">
//                                     <div class="d-flex justify-content-between align-items-center">
//                                         <div>
//                                             <h6 class="mb-1">${customer.name}</h6>
//                                             <small class="text-muted">
//                                                 SĐT: ${customer.phone} | Năm sinh: ${customer.birthYear}
//                                                 <span class="badge bg-success ms-2">Đã đăng ký</span>
//                                             </small>
//                                         </div>
//                                         <button class="btn btn-sm btn-primary" onclick="window.NhaTroContract.selectExistingCustomer(${customer.id}, '${customer.name}', ${customer.birthYear}, '${customer.phone}', '${customer.idNumber}', ${customer.isRegistered})">
//                                             <i class="fa fa-plus"></i> Chọn
//                                         </button>
//                                     </div>
//                                 </div>
//                             </div>
//                         `,
//                         )
//                         .join("")}
//                     </div>
//                 `
//             } else {
//                 resultsDiv.innerHTML = `
//                     <div class="alert alert-info">
//                         <i class="fa fa-info-circle me-2"></i>
//                         Không tìm thấy khách hàng với số điện thoại này. Bạn có thể thêm người mới bên dưới.
//                     </div>
//                 `
//             }
//         } catch (error) {
//             resultsDiv.innerHTML = `
//                 <div class="alert alert-danger">
//                     <i class="fa fa-exclamation-triangle me-2"></i>
//                     Có lỗi xảy ra khi tìm kiếm. Vui lòng thử lại.
//                 </div>
//             `
//         }
//     },
//
//     selectExistingCustomer(id, name, birthYear, phone, idNumber, isRegistered) {
//         const resident = {
//             id: Date.now(),
//             customerId: id,
//             name: name,
//             birthYear: birthYear,
//             phone: phone,
//             idNumber: idNumber,
//             isRegistered: isRegistered,
//             notes: "",
//         }
//
//         this.residents.push(resident)
//         this.updateResidentsList()
//         this.updateResidentsPreview()
//
//         // Close modal
//         const modal = window.bootstrap.Modal.getInstance(document.getElementById("addResidentModal"))
//         if (modal) modal.hide()
//
//         this.showNotification(`Đã thêm ${name} vào danh sách người ở`, "success")
//     },
//
//     saveResident() {
//         const name = document.getElementById("resident-name").value.trim()
//         const birthYear = document.getElementById("resident-birth-year").value
//         const phone = document.getElementById("resident-phone").value.trim()
//         const idNumber = document.getElementById("resident-id").value.trim()
//         const notes = document.getElementById("resident-notes").value.trim()
//
//         if (!name || !birthYear) {
//             this.showNotification("Vui lòng nhập họ tên và năm sinh", "warning")
//             return
//         }
//
//         const resident = {
//             id: Date.now(),
//             customerId: null,
//             name: name,
//             birthYear: Number.parseInt(birthYear),
//             phone: phone,
//             idNumber: idNumber,
//             isRegistered: false,
//             notes: notes,
//         }
//
//         this.residents.push(resident)
//         this.updateResidentsList()
//         this.updateResidentsPreview()
//
//         // Close modal
//         const modal = window.bootstrap.Modal.getInstance(document.getElementById("addResidentModal"))
//         if (modal) modal.hide()
//
//         this.showNotification(`Đã thêm ${name} vào danh sách người ở`, "success")
//     },
//
//     updateResidentsList() {
//         const container = document.getElementById("residents-list")
//         const countBadge = document.getElementById("residents-count")
//         const noMessage = document.getElementById("no-residents-message")
//
//         if (countBadge) countBadge.textContent = this.residents.length
//
//         if (this.residents.length === 0) {
//             if (noMessage) noMessage.style.display = "block"
//             return
//         }
//
//         if (noMessage) noMessage.style.display = "none"
//
//         if (container) {
//             container.innerHTML = this.residents
//                 .map(
//                     (resident) => `
//                 <div class="nha-tro-resident-card" data-resident-id="${resident.id}">
//                     <div class="card mb-3">
//                         <div class="card-body">
//                             <div class="d-flex justify-content-between align-items-start">
//                                 <div class="flex-grow-1">
//                                     <div class="d-flex align-items-center mb-2">
//                                         <h6 class="mb-0 me-2">${resident.name}</h6>
//                                         <span class="badge ${resident.isRegistered ? "bg-success" : "bg-warning text-dark"}">
//                                             ${resident.isRegistered ? "Đã đăng ký" : "Khách mới"}
//                                         </span>
//                                     </div>
//                                     <div class="row g-2 text-sm">
//                                         <div class="col-md-6">
//                                             <small class="text-muted">
//                                                 <i class="fa fa-calendar me-1"></i>Năm sinh: ${resident.birthYear}
//                                             </small>
//                                         </div>
//                                         ${resident.phone
//                             ? `
//                                             <div class="col-md-6">
//                                                 <small class="text-muted">
//                                                     <i class="fa fa-phone me-1"></i>${resident.phone}
//                                                 </small>
//                                             </div>
//                                         `
//                             : ""
//                         }
//                                         ${resident.idNumber
//                             ? `
//                                             <div class="col-md-6">
//                                                 <small class="text-muted">
//                                                     <i class="fa fa-id-card me-1"></i>${resident.idNumber}
//                                                 </small>
//                                             </div>
//                                         `
//                             : ""
//                         }
//                                         ${resident.notes
//                             ? `
//                                             <div class="col-12">
//                                                 <small class="text-muted">
//                                                     <i class="fa fa-note-sticky me-1"></i>${resident.notes}
//                                                 </small>
//                                             </div>
//                                         `
//                             : ""
//                         }
//                                     </div>
//                                 </div>
//                                 <div class="ms-3">
//                                     <button class="btn btn-sm btn-outline-danger" onclick="window.NhaTroContract.removeResident(${resident.id})" title="Xóa">
//                                         <i class="fa fa-trash"></i>
//                                     </button>
//                                 </div>
//                             </div>
//                         </div>
//                     </div>
//                 </div>
//             `,
//                 )
//                 .join("")
//         }
//     },
//
//     removeResident(residentId) {
//         const resident = this.residents.find((r) => r.id === residentId)
//         if (resident && confirm(`Bạn có chắc chắn muốn xóa ${resident.name} khỏi danh sách?`)) {
//             this.residents = this.residents.filter((r) => r.id !== residentId)
//             this.updateResidentsList()
//             this.updateResidentsPreview()
//             this.showNotification(`Đã xóa ${resident.name} khỏi danh sách`, "info")
//         }
//     },
//
//     updateResidentsPreview() {
//         const previewSection = document.getElementById("preview-residents-section")
//         const previewText = document.getElementById("preview-residents")
//
//         if (this.residents.length === 0) {
//             if (previewSection) previewSection.style.display = "none"
//             return
//         }
//
//         if (previewSection) previewSection.style.display = "block"
//         const residentsText = this.residents.map((resident) => `${resident.name} (${resident.birthYear})`).join(", ")
//
//         if (previewText) previewText.textContent = residentsText
//     },
//
//     clearResidentForm() {
//         const form = document.getElementById("addResidentForm")
//         if (form) form.reset()
//
//         const searchPhone = document.getElementById("search-customer-phone")
//         if (searchPhone) searchPhone.value = ""
//
//         const searchResults = document.getElementById("search-results")
//         if (searchResults) searchResults.innerHTML = ""
//     },
//
//     // Amenity management
//     setupAmenityModal() {
//         const addAmenityBtn = document.getElementById("btn-add-amenity-host")
//         const saveAmenityBtn = document.getElementById("saveAmenity-host")
//         const amenityForm = document.getElementById("addAmenityForm-host")
//         const amenityNameInput = document.getElementById("amenityName-host")
//
//         if (addAmenityBtn) {
//             addAmenityBtn.addEventListener("click", () => {
//                 const modal = new window.bootstrap.Modal(document.getElementById("addAmenityModal-host"))
//                 modal.show()
//                 if (amenityForm) amenityForm.reset()
//             })
//         }
//
//         if (saveAmenityBtn) {
//             saveAmenityBtn.addEventListener("click", () => {
//                 this.saveNewAmenity()
//             })
//         }
//
//         if (amenityNameInput) {
//             amenityNameInput.addEventListener("keypress", (e) => {
//                 if (e.key === "Enter") {
//                     e.preventDefault()
//                     this.saveNewAmenity()
//                 }
//             })
//         }
//     },
//
//     saveNewAmenity() {
//         const amenityNameInput = document.getElementById("amenityName-host")
//         const amenityName = amenityNameInput.value.trim()
//
//         if (!amenityName) {
//             this.showNotification("Vui lòng nhập tên tiện ích", "warning")
//             if (amenityNameInput) amenityNameInput.focus()
//             return
//         }
//
//         // Check if amenity already exists
//         const existingAmenities = document.querySelectorAll("#amenities-list-host .form-check-label")
//         const exists = Array.from(existingAmenities).some(
//             (label) => label.textContent.toLowerCase() === amenityName.toLowerCase(),
//         )
//
//         if (exists) {
//             this.showNotification("Tiện ích này đã tồn tại!", "warning")
//             return
//         }
//
//         // Create new amenity
//         this.addAmenityToList(amenityName)
//
//         // Close modal
//         const modal = window.bootstrap.Modal.getInstance(document.getElementById("addAmenityModal-host"))
//         if (modal) modal.hide()
//
//         this.showNotification(`Đã thêm tiện ích "${amenityName}" thành công!`, "success")
//         this.updateAmenities()
//     },
//
//     addAmenityToList(amenityName) {
//         const amenitiesList = document.getElementById("amenities-list-host")
//         const amenityId = "amenity-" + Date.now() + "-host"
//
//         const amenityDiv = document.createElement("div")
//         amenityDiv.className = "form-check nha-tro-host-custom-amenity"
//         amenityDiv.innerHTML = `
//             <input class="form-check-input" type="checkbox" id="${amenityId}">
//             <label class="form-check-label" for="${amenityId}">${amenityName}</label>
//             <button type="button" class="btn btn-sm btn-outline-danger nha-tro-host-remove-amenity" onclick="window.NhaTroContract.removeAmenity('${amenityId}')" title="Xóa tiện ích">
//                 <i class="fa fa-times"></i>
//             </button>
//         `
//
//         if (amenitiesList) amenitiesList.appendChild(amenityDiv)
//
//         // Add event listener for the new checkbox
//         const newCheckbox = document.getElementById(amenityId)
//         if (newCheckbox) {
//             newCheckbox.addEventListener("change", () => {
//                 this.updateAmenities()
//             })
//         }
//     },
//
//     removeAmenity(amenityId) {
//         const amenityElement = document.getElementById(amenityId)
//         if (!amenityElement) return
//
//         const amenityContainer = amenityElement.closest(".nha-tro-host-custom-amenity")
//         const amenityLabel = amenityContainer.querySelector("label")
//         const amenityName = amenityLabel ? amenityLabel.textContent : "tiện ích này"
//
//         if (confirm(`Bạn có chắc chắn muốn xóa tiện ích "${amenityName}"?`)) {
//             amenityContainer.remove()
//             this.updateAmenities()
//             this.showNotification(`Đã xóa tiện ích "${amenityName}"`, "info")
//         }
//     },
//
//     // Customer management
//     setupCustomerModal() {
//         const addCustomerBtn = document.getElementById("btn-add-customer-host")
//         const saveCustomerBtn = document.getElementById("saveCustomer-host")
//         const customerForm = document.getElementById("addCustomerForm-host")
//
//         if (addCustomerBtn) {
//             addCustomerBtn.addEventListener("click", () => {
//                 const modal = new window.bootstrap.Modal(document.getElementById("addCustomerModal-host"))
//                 modal.show()
//                 if (customerForm) customerForm.reset()
//                 this.clearCustomerFormImages()
//                 this.setupCustomerLocationListeners()
//             })
//         }
//
//         if (saveCustomerBtn) {
//             saveCustomerBtn.addEventListener("click", () => {
//                 this.saveNewCustomer()
//             })
//         }
//
//         // Image upload events for new customer
//         const frontInput = document.getElementById("newCustomer-cccd-front")
//         const backInput = document.getElementById("newCustomer-cccd-back")
//
//         if (frontInput) {
//             frontInput.addEventListener("change", (e) => {
//                 this.previewCustomerImage(e, "newCustomer-cccd-front-preview")
//             })
//         }
//
//         if (backInput) {
//             backInput.addEventListener("change", (e) => {
//                 this.previewCustomerImage(e, "newCustomer-cccd-back-preview")
//             })
//         }
//     },
//
//     setupCustomerLocationListeners() {
//         const provinceSelect = document.getElementById("newCustomer-province")
//         const districtSelect = document.getElementById("newCustomer-district")
//         const wardSelect = document.getElementById("newCustomer-ward")
//
//         if (provinceSelect) {
//             provinceSelect.addEventListener("change", () => {
//                 if (provinceSelect.value) {
//                     this.loadDistricts(provinceSelect.value, "newCustomer-district", "newCustomer-ward")
//                 } else {
//                     if (districtSelect) districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
//                     if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
//                 }
//             })
//         }
//
//         if (districtSelect) {
//             districtSelect.addEventListener("change", () => {
//                 if (districtSelect.value) {
//                     this.loadWards(districtSelect.value, "newCustomer-ward")
//                 } else {
//                     if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
//                 }
//             })
//         }
//     },
//
//     previewCustomerImage(event, previewId) {
//         const file = event.target.files[0]
//         const preview = document.getElementById(previewId)
//         const uploadContainer = preview ? preview.closest(".nha-tro-image-upload") : null
//
//         if (file && preview) {
//             const reader = new FileReader()
//             reader.onload = (e) => {
//                 preview.innerHTML = `<img src="${e.target.result}" alt="CCCD Preview" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
//                 if (uploadContainer) uploadContainer.classList.add("has-image")
//             }
//             reader.readAsDataURL(file)
//         }
//     },
//
//     clearCustomerFormImages() {
//         const frontPreview = document.getElementById("newCustomer-cccd-front-preview")
//         const backPreview = document.getElementById("newCustomer-cccd-back-preview")
//
//         if (frontPreview) {
//             frontPreview.innerHTML = `
//                 <i class="fa fa-camera fa-2x"></i>
//                 <div class="mt-2">Tải ảnh mặt trước</div>
//                 <small class="text-muted">Nhấn để chọn ảnh</small>
//             `
//         }
//
//         if (backPreview) {
//             backPreview.innerHTML = `
//                 <i class="fa fa-camera fa-2x"></i>
//                 <div class="mt-2">Tải ảnh mặt sau</div>
//                 <small class="text-muted">Nhấn để chọn ảnh</small>
//             `
//         }
//
//         // Remove has-image class
//         document.querySelectorAll("#addCustomerModal-host .nha-tro-image-upload").forEach((container) => {
//             container.classList.remove("has-image")
//         })
//     },
//
//     saveNewCustomer() {
//         // Collect customer data
//         const customerData = {
//             name: this.getInputValue("newCustomer-name"),
//             dob: this.getInputValue("newCustomer-dob"),
//             id: this.getInputValue("newCustomer-id"),
//             idDate: this.getInputValue("newCustomer-id-date"),
//             idPlace: this.getInputValue("newCustomer-id-place"),
//             phone: this.getInputValue("newCustomer-phone"),
//             email: this.getInputValue("newCustomer-email"),
//             province: this.getInputValue("newCustomer-province"),
//             district: this.getInputValue("newCustomer-district"),
//             ward: this.getInputValue("newCustomer-ward"),
//             street: this.getInputValue("newCustomer-street"),
//             notes: this.getInputValue("newCustomer-notes"),
//             cccdFront: this.getFileInput("newCustomer-cccd-front"),
//             cccdBack: this.getFileInput("newCustomer-cccd-back"),
//         }
//
//         // Update preview directly instead of filling main form
//         this.updatePreviewWithCustomerData(customerData)
//
//         // Close modal
//         const modal = window.bootstrap.Modal.getInstance(document.getElementById("addCustomerModal-host"))
//         if (modal) modal.hide()
//
//         this.showNotification("Đã thêm thông tin khách thuê vào xem trước hợp đồng!", "success")
//     },
//
//     async updatePreviewWithCustomerData(customerData) {
//         // Update preview fields directly
//         const preview = document.getElementById("preview-tenant-name")
//         if (preview) preview.textContent = customerData.name || "........................"
//
//         const previewDob = document.getElementById("preview-tenant-dob")
//         if (previewDob && customerData.dob) {
//             const date = new Date(customerData.dob)
//             previewDob.textContent = date.toLocaleDateString("vi-VN")
//         }
//
//         const previewId = document.getElementById("preview-tenant-id")
//         if (previewId) previewId.textContent = customerData.id || "........................"
//
//         const previewIdDate = document.getElementById("preview-tenant-id-date")
//         if (previewIdDate && customerData.idDate) {
//             const date = new Date(customerData.idDate)
//             previewIdDate.textContent = date.toLocaleDateString("vi-VN")
//         }
//
//         const previewIdPlace = document.getElementById("preview-tenant-id-place")
//         if (previewIdPlace) previewIdPlace.textContent = customerData.idPlace || "........................"
//
//         const previewPhone = document.getElementById("preview-tenant-phone")
//         if (previewPhone) previewPhone.textContent = customerData.phone || "........................"
//
//         // Build address from location data
//         const addressParts = []
//         if (customerData.street) addressParts.push(customerData.street)
//
//         // Get text values for location selects
//         if (customerData.ward) {
//             const wardSelect = document.getElementById("newCustomer-ward")
//             const wardText = wardSelect && wardSelect.options[wardSelect.selectedIndex] ?
//                             wardSelect.options[wardSelect.selectedIndex].text : ""
//             if (wardText && !wardText.includes("Chọn")) addressParts.push(wardText)
//         }
//
//         if (customerData.district) {
//             const districtSelect = document.getElementById("newCustomer-district")
//             const districtText = districtSelect && districtSelect.options[districtSelect.selectedIndex] ?
//                             districtSelect.options[districtSelect.selectedIndex].text : ""
//             if (districtText && !districtText.includes("Chọn")) addressParts.push(districtText)
//         }
//
//         if (customerData.province) {
//             const provinceSelect = document.getElementById("newCustomer-province")
//             const provinceText = provinceSelect && provinceSelect.options[provinceSelect.selectedIndex] ?
//                             provinceSelect.options[provinceSelect.selectedIndex].text : ""
//             if (provinceText && !provinceText.includes("Chọn")) addressParts.push(provinceText)
//         }
//
//         const fullAddress = addressParts.join(", ")
//         const previewAddress = document.getElementById("preview-tenant-address")
//         if (previewAddress) {
//             previewAddress.textContent = fullAddress || "........................"
//         }
//
//         // Add highlight effect to updated fields
//         const updatedFields = [
//             "preview-tenant-name", "preview-tenant-dob", "preview-tenant-id",
//             "preview-tenant-id-date", "preview-tenant-id-place", "preview-tenant-phone", "preview-tenant-address"
//         ]
//
//         updatedFields.forEach(fieldId => {
//             const field = document.getElementById(fieldId)
//             if (field && field.textContent !== "........................") {
//                 field.classList.add("nha-tro-updated")
//                 setTimeout(() => field.classList.remove("nha-tro-updated"), 1000)
//             }
//         })
//     },
//
//     // Utility functions
//     getInputValue(id) {
//         const element = document.getElementById(id)
//         return element ? element.value || "" : ""
//     },
//
//     setInputValue(id, value) {
//         const element = document.getElementById(id)
//         if (element) element.value = value
//     },
//
//     getFileInput(id) {
//         const element = document.getElementById(id)
//         return element && element.files.length > 0 ? element.files[0] : null
//     },
//
//     // Date and initialization functions
//     setCurrentDate() {
//         const today = new Date().toISOString().split("T")[0]
//         this.setInputValue("contract-date", today)
//         this.setInputValue("start-date", today)
//
//         this.updatePreviewField("contract-date", "preview-sign-date")
//         this.updatePreviewField("start-date", "preview-start-date")
//         this.calculateEndDate()
//     },
//
//     // Tab management
//     showTab(tabId) {
//         // Hide all tabs
//         document.querySelectorAll(".tab-pane").forEach((pane) => {
//             pane.classList.remove("show", "active")
//         })
//
//         // Remove active class from all nav links
//         document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
//             link.classList.remove("active")
//         })
//
//         // Show selected tab
//         const targetTab = document.getElementById(tabId)
//         const targetLink = document.querySelector(`[data-tab="${tabId}"]`)
//
//         if (targetTab && targetLink) {
//             targetTab.classList.add("show", "active")
//             targetLink.classList.add("active")
//             this.currentTab = tabId
//         }
//
//         // Scroll to top
//         window.scrollTo({ top: 0, behavior: "smooth" })
//     },
//
//     // Image preview
//     previewImage(event, previewId) {
//         const file = event.target.files[0]
//         const preview = document.getElementById(previewId)
//
//         if (file && preview) {
//             const reader = new FileReader()
//             reader.onload = (e) => {
//                 preview.innerHTML = `<img src="${e.target.result}" alt="CCCD Preview" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
//             }
//             reader.readAsDataURL(file)
//         }
//     },
//
//     // Preview updates
//     updateAllPreview() {
//         this.updatePreviewField("contract-date", "preview-sign-date")
//         this.updatePreviewField("start-date", "preview-start-date")
//         this.calculateEndDate()
//         this.calculateDeposit()
//         this.updatePaymentMethod()
//         this.updateAmenities()
//         this.updateResidentsPreview()
//     },
//
//     updatePreviewField(inputId, previewId) {
//         const input = document.getElementById(inputId)
//         const preview = document.getElementById(previewId)
//
//         if (input && preview) {
//             let value = input.value
//
//             // Format date if needed
//             if (input.type === "date" && value) {
//                 const date = new Date(value)
//                 value = date.toLocaleDateString("vi-VN")
//             }
//
//             // Format number if it's rent price
//             if (inputId === "rent-price" && value) {
//                 value = new Intl.NumberFormat("vi-VN").format(value)
//             }
//
//             // Special handling for textarea
//             if (input.tagName === "TEXTAREA" && value) {
//                 preview.innerHTML = value.replace(/\n/g, "<br>")
//             } else {
//                 preview.textContent = value || "........................"
//             }
//
//             // Add highlight effect
//             if (value) {
//                 preview.classList.add("nha-tro-updated")
//                 setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000)
//             }
//
//             // Update related calculations
//             if (inputId === "start-date" || inputId === "contract-duration") {
//                 this.calculateEndDate()
//             }
//             if (inputId === "rent-price" || inputId === "deposit-months") {
//                 this.calculateDeposit()
//             }
//         }
//     },
//
//     updateAddress(prefix) {
//         const street = this.getInputValue(`${prefix}-street`)
//         const ward = this.getSelectText(`${prefix}-ward`)
//         const district = this.getSelectText(`${prefix}-district`)
//         const province = this.getSelectText(`${prefix}-province`)
//
//         const parts = [street, ward, district, province].filter((part) => part && !part.includes("Chọn") && part !== "")
//         const fullAddress = parts.join(", ")
//
//         const previewId = `preview-${prefix === "room" ? "room" : prefix}-address`
//         const preview = document.getElementById(previewId)
//         if (preview) {
//             preview.textContent = fullAddress || "........................"
//             if (fullAddress) {
//                 preview.classList.add("nha-tro-updated")
//                 setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000)
//             }
//         }
//     },
//
//     updatePaymentMethod() {
//         const paymentMethod = document.getElementById("payment-method")
//         const preview = document.getElementById("preview-payment-method")
//
//         if (paymentMethod && preview) {
//             const selectedText = paymentMethod.options[paymentMethod.selectedIndex]?.text || "........................"
//             preview.textContent = selectedText
//         }
//     },
//
//     updateAmenities() {
//         const checkboxes = document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]:checked')
//         const amenities = Array.from(checkboxes).map((cb) => {
//             const label = document.querySelector(`label[for="${cb.id}"]`)
//             return label ? label.textContent : cb.id
//         })
//
//         const preview = document.getElementById("preview-amenities")
//         if (preview) {
//             preview.textContent = amenities.length > 0 ? amenities.join(", ") : "........................"
//         }
//     },
//
//     calculateEndDate() {
//         const startDate = this.getInputValue("start-date")
//         const duration = this.getInputValue("contract-duration")
//
//         if (startDate && duration) {
//             const start = new Date(startDate)
//             const end = new Date(start)
//             end.setMonth(end.getMonth() + Number.parseInt(duration))
//
//             const preview = document.getElementById("preview-end-date")
//             if (preview) {
//                 preview.textContent = end.toLocaleDateString("vi-VN")
//             }
//         }
//     },
//
//     calculateDeposit() {
//         const rentPrice = this.getInputValue("rent-price")
//         const depositMonths = this.getInputValue("deposit-months")
//
//         if (rentPrice && depositMonths) {
//             const deposit = Number.parseInt(rentPrice) * Number.parseInt(depositMonths)
//             const preview = document.getElementById("preview-deposit")
//             if (preview) {
//                 preview.textContent = new Intl.NumberFormat("vi-VN").format(deposit)
//             }
//         }
//     },
//
//     getSelectText(id) {
//         const element = document.getElementById(id)
//         if (element && element.selectedIndex >= 0) {
//             return element.options[element.selectedIndex].text
//         }
//         return ""
//     },
//
//     // Zoom functions
//     zoomIn() {
//         this.zoomLevel = Math.min(this.zoomLevel + 0.1, 2)
//         this.applyZoom()
//     },
//
//     zoomOut() {
//         this.zoomLevel = Math.max(this.zoomLevel - 0.1, 0.5)
//         this.applyZoom()
//     },
//
//     resetZoom() {
//         this.zoomLevel = 1
//         this.applyZoom()
//     },
//
//     applyZoom() {
//         const container = document.getElementById("preview-container")
//         if (container) {
//             container.style.transform = `scale(${this.zoomLevel})`
//         }
//     },
//
//     // Action functions
//     updateContract() {
//         this.showNotification("Hợp đồng đã được cập nhật!", "success")
//     },
//
//     printContract() {
//         const printContent = document.getElementById("contract-preview")
//         if (!printContent) return
//
//         const printWindow = window.open("", "_blank")
//         printWindow.document.write(`
//             <html>
//                 <head>
//                     <title>Hợp đồng thuê nhà</title>
//                     <style>
//                         body { font-family: 'Times New Roman', serif; line-height: 1.8; }
//                         .nha-tro-highlight { color: #000; font-weight: normal; }
//                         @media print { body { margin: 0; } }
//                     </style>
//                 </head>
//                 <body>${printContent.innerHTML}</body>
//             </html>
//         `)
//         printWindow.document.close()
//         printWindow.print()
//     },
//
//     saveContract() {
//         const contractData = this.collectFormData()
//         console.log("Saving contract:", contractData)
//         this.showNotification("Hợp đồng đã được lưu thành công!", "success")
//     },
//
//     collectFormData() {
//         return {
//             tenant: {
//                 name: this.getInputValue("tenant-name"),
//                 dob: this.getInputValue("tenant-dob"),
//                 id: this.getInputValue("tenant-id"),
//                 phone: this.getInputValue("tenant-phone"),
//                 email: this.getInputValue("tenant-email"),
//             },
//             owner: {
//                 name: this.getInputValue("owner-name"),
//                 dob: this.getInputValue("owner-dob"),
//                 id: this.getInputValue("owner-id"),
//                 phone: this.getInputValue("owner-phone"),
//             },
//             room: {
//                 address: this.getFullAddress("room"),
//                 number: this.getInputValue("room-number"),
//                 area: this.getInputValue("room-area"),
//             },
//             terms: {
//                 rentPrice: this.getInputValue("rent-price"),
//                 duration: this.getInputValue("contract-duration"),
//                 startDate: this.getInputValue("start-date"),
//                 paymentMethod: this.getInputValue("payment-method"),
//             },
//             residents: this.residents,
//         }
//     },
//
//     getFullAddress(prefix) {
//         const street = this.getInputValue(`${prefix}-street`)
//         const ward = this.getSelectText(`${prefix}-ward`)
//         const district = this.getSelectText(`${prefix}-district`)
//         const province = this.getSelectText(`${prefix}-province`)
//
//         return [street, ward, district, province].filter((part) => part && !part.includes("Chọn")).join(", ")
//     },
//
//     showNotification(message, type = "info") {
//         const notification = document.createElement("div")
//         notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`
//         notification.style.cssText = "top: 20px; right: 20px; z-index: 9999; min-width: 400px;"
//         notification.innerHTML = `
//             ${message}
//             <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
//         `
//
//         document.body.appendChild(notification)
//
//         setTimeout(() => {
//             if (notification.parentNode) {
//                 notification.remove()
//             }
//         }, 5000)
//     },
// }
//
// // Initialize when DOM is loaded
// document.addEventListener("DOMContentLoaded", () => {
//     window.NhaTroContract.init()
// })



    /*
    <
    ![CDATA[*/
    window.NhaTroContract = {
    currentTab: "tenantInfo",
    zoomLevel: 1,

    init() {
    this.setupEventListeners();
    this.setCurrentDate();
    this.updateAllPreview();
    this.setupAmenityModal();
    this.setupCustomerModal();
    this.loadProvinces();

    // Điền trước thông tin chủ trọ từ backend
    const contract = /*[[${contract}]]*/ null;
    if (contract && contract.owner) {
    document.getElementById("owner-name").value = contract.owner.fullName || "";
    document.getElementById("owner-dob").value = contract.owner.birthday ? new Date(contract.owner.birthday).toISOString().split('T')[0] : "";
    document.getElementById("owner-id").value = contract.owner.id || "";
    document.getElementById("owner-phone").value = contract.owner.phone || "";
    document.getElementById("owner-email").value = contract.owner.email || "";
    // Các trường khác để trống hoặc xử lý thủ công
}
},

    // Hàm chuẩn hóa tên để so sánh
    normalizeName(name) {
    if (!name) return "";
    // Loại bỏ tiền tố "Tỉnh", "TP.", "Thành phố", "Quận", "Phường" và khoảng trắng thừa
    return name.replace(/^(Tỉnh|TP\.|Thành phố|Quận|Phường)\s*/i, "")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
},

    setupEventListeners() {
    // Sự kiện click cho các tab
    document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
    link.addEventListener("click", (e) => {
    e.preventDefault();
    const tabId = link.getAttribute("data-tab");
    this.showTab(tabId);
});
});

    // Sự kiện cho các nút điều hướng
    document.getElementById("btn-next-owner")?.addEventListener("click", () => this.showTab("ownerInfo"));
    document.getElementById("btn-prev-tenant")?.addEventListener("click", () => this.showTab("tenantInfo"));
    document.getElementById("btn-next-room")?.addEventListener("click", () => this.showTab("roomInfo"));
    document.getElementById("btn-prev-owner")?.addEventListener("click", () => this.showTab("ownerInfo"));
    document.getElementById("btn-next-terms")?.addEventListener("click", () => this.showTab("terms"));
    document.getElementById("btn-prev-room")?.addEventListener("click", () => this.showTab("roomInfo"));

    // Sự kiện cho các nút hành động
    document.getElementById("btn-update")?.addEventListener("click", () => this.updateContract());
    document.getElementById("btn-print")?.addEventListener("click", () => this.printContract());
    document.getElementById("btn-save")?.addEventListener("click", (e) => {
    e.preventDefault(); // Ngăn submit mặc định
    this.saveContract();
});

    // Sự kiện cho các nút zoom
    document.getElementById("btn-zoom-in")?.addEventListener("click", () => this.zoomIn());
    document.getElementById("btn-zoom-out")?.addEventListener("click", () => this.zoomOut());
    document.getElementById("btn-reset-zoom")?.addEventListener("click", () => this.resetZoom());

    // Sự kiện tải ảnh
    document.getElementById("cccd-front")?.addEventListener("change", (e) => {
    this.previewImage(e, "cccd-front-preview");
});
    document.getElementById("cccd-back")?.addEventListener("change", (e) => {
    this.previewImage(e, "cccd-back-preview");
});

    // Sự kiện nhập số điện thoại để lấy thông tin người thuê
    const tenantPhoneInput = document.getElementById("tenant-phone");
    if (tenantPhoneInput) {
    tenantPhoneInput.addEventListener("input", (e) => {
    const phone = e.target.value.trim();
    if (phone.length >= 10) { // Giả sử số điện thoại Việt Nam cần ít nhất 10 số
    this.fetchTenantByPhone(phone);
} else {
    this.clearTenantFields(); // Xóa các trường nếu số điện thoại không hợp lệ
}
});
}

    // Các sự kiện khác cho bản xem trước
    this.setupPreviewListeners();
    this.setupLocationListeners();
},

    // Hàm ánh xạ tên tỉnh sang mã số
    async mapProvinceNameToCode(provinceName) {
    try {
    const response = await fetch("https://provinces.open-api.vn/api/p/");
    const provinces = await response.json();
    console.log("Danh sách tỉnh từ API:", provinces.map(p => p.name)); // Log để debug

    const normalizedProvinceName = this.normalizeName(provinceName);
    const province = provinces.find(p => this.normalizeName(p.name) === normalizedProvinceName);
    if (!province) {
    this.showNotification(`Không tìm thấy tỉnh "${provinceName}"`, "warning");
    return null;
}
    console.log(`Tìm thấy tỉnh: ${provinceName} -> Code: ${province.code}`);
    return province.code;
} catch (error) {
    console.error("Lỗi khi ánh xạ tên tỉnh:", error);
    this.showNotification("Không thể tải danh sách tỉnh/thành phố", "error");
    return null;
}
},

    // Hàm ánh xạ tên quận sang mã số
    async mapDistrictNameToCode(provinceCode, districtName) {
    try {
    const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
    const province = await response.json();
    console.log(`Danh sách quận cho tỉnh ${provinceCode}:`, province.districts.map(d => d.name)); // Log để debug

    const normalizedDistrictName = this.normalizeName(districtName);
    const district = province.districts.find(d => this.normalizeName(d.name) === normalizedDistrictName);
    if (!district) {
    this.showNotification(`Không tìm thấy quận "${districtName}"`, "warning");
    return null;
}
    console.log(`Tìm thấy quận: ${districtName} -> Code: ${district.code}`);
    return district.code;
} catch (error) {
    console.error("Lỗi khi ánh xạ tên quận:", error);
    this.showNotification("Không thể tải danh sách quận/huyện", "error");
    return null;
}
},

    // Hàm ánh xạ tên phường sang mã số
    async mapWardNameToCode(districtCode, wardName) {
    try {
    const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
    const district = await response.json();
    console.log(`Danh sách phường cho quận ${districtCode}:`, district.wards.map(w => w.name)); // Log để debug

    const normalizedWardName = this.normalizeName(wardName);
    const ward = district.wards.find(w => this.normalizeName(w.name) === normalizedWardName);
    if (!ward) {
    this.showNotification(`Không tìm thấy phường "${wardName}"`, "warning");
    return null;
}
    console.log(`Tìm thấy phường: ${wardName} -> Code: ${ward.code}`);
    return ward.code;
} catch (error) {
    console.error("Lỗi khi ánh xạ tên phường:", error);
    this.showNotification("Không thể tải danh sách phường/xã", "error");
    return null;
}
},
    // Hàm lấy thông tin người thuê qua số điện thoại
    async fetchTenantByPhone(phone) {
    try {
    const response = await fetch(`/api/contracts/get-tenant-by-phone?phone=${encodeURIComponent(phone)}`, {
    method: "POST",
    headers: {
    "Content-Type": "application/json",
    // Nếu cần CSRF token
    "X-CSRF-TOKEN": document.querySelector('meta[name="_csrf"]')?.content || ""
}
});
    const data = await response.json();

    if (data.success) {
    this.fillTenantFields(data.tenant);
    this.showNotification("Đã tìm thấy thông tin người thuê!", "success");
} else {
    this.clearTenantFields();
    this.showNotification(data.message || "Không tìm thấy người thuê với số điện thoại này", "warning");
}
} catch (error) {
    console.error("Lỗi khi lấy thông tin người thuê:", error);
    this.clearTenantFields();
    this.showNotification("Lỗi khi lấy thông tin người thuê", "error");
}
},

    // Hàm điền thông tin người thuê
    async fillTenantFields(tenant) {
    // Điền các trường cơ bản
    document.getElementById("tenant-name").value = tenant.fullName || "";
    document.getElementById("tenant-dob").value = tenant.birthday || "";
    document.getElementById("tenant-id").value = tenant.id || "";
    document.getElementById("tenant-id-date").value = tenant.issueDate || "";
    document.getElementById("tenant-id-place").value = tenant.issuePlace || "";
    document.getElementById("tenant-email").value = tenant.email || "";
    document.getElementById("tenant-street").value = tenant.street || "";

    // Xử lý địa chỉ (tỉnh, quận, phường)
    const provinceSelect = document.getElementById("tenant-province");
    const districtSelect = document.getElementById("tenant-district");
    const wardSelect = document.getElementById("tenant-ward");

    if (tenant.province && provinceSelect) {
    const provinceCode = await this.mapProvinceNameToCode(tenant.province);
    if (provinceCode) {
    provinceSelect.value = provinceCode;
    await this.loadDistricts(provinceCode, "tenant-district", "tenant-ward");

    if (tenant.district && districtSelect) {
    const districtCode = await this.mapDistrictNameToCode(provinceCode, tenant.district);
    if (districtCode) {
    districtSelect.value = districtCode;
    await this.loadWards(districtCode, "tenant-ward");

    if (tenant.ward && wardSelect) {
    const wardCode = await this.mapWardNameToCode(districtCode, tenant.ward);
    if (wardCode) {
    wardSelect.value = wardCode;
}
}
}
}
}
    // Cập nhật địa chỉ trong bản xem trước
    this.updateAddress("tenant");
}

    // Cập nhật toàn bộ bản xem trước
    this.updateAllPreview();
},

    // Hàm xóa các trường thông tin người thuê
    clearTenantFields() {
    document.getElementById("tenant-name").value = "";
    document.getElementById("tenant-dob").value = "";
    document.getElementById("tenant-id").value = "";
    document.getElementById("tenant-id-date").value = "";
    document.getElementById("tenant-id-place").value = "";
    document.getElementById("tenant-email").value = "";
    document.getElementById("tenant-street").value = "";
    document.getElementById("tenant-province").value = "";
    document.getElementById("tenant-district").innerHTML = '<option value="">Quận/Huyện</option>';
    document.getElementById("tenant-ward").innerHTML = '<option value="">Phường/Xã</option>';

    // Cập nhật bản xem trước
    this.updateAllPreview();
},

    setupPreviewListeners() {
    const inputs = [
{id: "tenant-name", preview: "preview-tenant-name"},
{id: "tenant-dob", preview: "preview-tenant-dob"},
{id: "tenant-id", preview: "preview-tenant-id"},
{id: "tenant-id-date", preview: "preview-tenant-id-date"},
{id: "tenant-id-place", preview: "preview-tenant-id-place"},
{id: "tenant-phone", preview: "preview-tenant-phone"},
{id: "owner-name", preview: "preview-owner-name"},
{id: "owner-dob", preview: "preview-owner-dob"},
{id: "owner-id", preview: "preview-owner-id"},
{id: "owner-id-date", preview: "preview-owner-id-date"},
{id: "owner-id-place", preview: "preview-owner-id-place"},
{id: "owner-phone", preview: "preview-owner-phone"},
{id: "room-number", preview: "preview-room-number"},
{id: "room-area", preview: "preview-room-area"},
{id: "rent-price", preview: "preview-rent"},
{id: "contract-duration", preview: "preview-duration"},
{id: "start-date", preview: "preview-start-date"},
{id: "contract-date", preview: "preview-sign-date"},
{id: "payment-date", preview: "preview-payment-date"},
{id: "deposit-months", preview: "preview-deposit-months"},
{id: "terms-conditions", preview: "preview-terms"},
    ];

    inputs.forEach((input) => {
    const element = document.getElementById(input.id);
    if (element) {
    element.addEventListener("input", () => {
    this.updatePreviewField(input.id, input.preview);
});
    element.addEventListener("change", () => {
    this.updatePreviewField(input.id, input.preview);
});
}
});

    // Xử lý đặc biệt
    document.getElementById("payment-method")?.addEventListener("change", () => {
    this.updatePaymentMethod();
});

    // Checkbox tiện ích
    document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]').forEach((checkbox) => {
    checkbox.addEventListener("change", () => {
    this.updateAmenities();
});
});

    // Các trường địa chỉ
    const addressFields = ["tenant", "owner", "room"];
    addressFields.forEach((prefix) => {
    ["province", "district", "ward", "street"].forEach((field) => {
    const element = document.getElementById(`${prefix}-${field}`);
    if (element) {
    element.addEventListener("change", () => {
    this.updateAddress(prefix);
});
}
});
});
},

    // Các hàm API cho địa chỉ
    async loadProvinces() {
    try {
    const response = await fetch("https://provinces.open-api.vn/api/p/");
    const provinces = await response.json();

    const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"];
    selects.forEach((selectId) => {
    const select = document.getElementById(selectId);
    if (select) {
    select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>';
    provinces.forEach((province) => {
    const option = document.createElement("option");
    option.value = province.code;
    option.textContent = province.name;
    select.appendChild(option);
});
}
});
} catch (error) {
    console.error("Lỗi khi tải danh sách tỉnh/thành phố:", error);
    this.showNotification("Không thể tải danh sách tỉnh/thành phố", "warning");
}
},

    async loadDistricts(provinceCode, districtSelectId, wardSelectId) {
    try {
    const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
    const province = await response.json();

    const districtSelect = document.getElementById(districtSelectId);
    const wardSelect = document.getElementById(wardSelectId);

    if (districtSelect) {
    districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
    province.districts.forEach((district) => {
    const option = document.createElement("option");
    option.value = district.code;
    option.textContent = district.name;
    districtSelect.appendChild(option);
});
}

    if (wardSelect) {
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
}
} catch (error) {
    console.error("Lỗi khi tải danh sách quận/huyện:", error);
    this.showNotification("Không thể tải danh sách quận/huyện", "warning");
}
},

    async loadWards(districtCode, wardSelectId) {
    try {
    const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
    const district = await response.json();

    const wardSelect = document.getElementById(wardSelectId);
    if (wardSelect) {
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
    district.wards.forEach((ward) => {
    const option = document.createElement("option");
    option.value = ward.code;
    option.textContent = ward.name;
    wardSelect.appendChild(option);
});
}
} catch (error) {
    console.error("Lỗi khi tải danh sách phường/xã:", error);
    this.showNotification("Không thể tải danh sách phường/xã", "warning");
}
},

    setupLocationListeners() {
    const prefixes = ["tenant", "owner", "room"];

    prefixes.forEach((prefix) => {
    const provinceSelect = document.getElementById(`${prefix}-province`);
    const districtSelect = document.getElementById(`${prefix}-district`);
    const wardSelect = document.getElementById(`${prefix}-ward`);

    if (provinceSelect) {
    provinceSelect.addEventListener("change", () => {
    if (provinceSelect.value) {
    this.loadDistricts(provinceSelect.value, `${prefix}-district`, `${prefix}-ward`);
} else {
    districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
}
    this.updateAddress(prefix);
});
}

    if (districtSelect) {
    districtSelect.addEventListener("change", () => {
    if (districtSelect.value) {
    this.loadWards(districtSelect.value, `${prefix}-ward`);
} else {
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
}
    this.updateAddress(prefix);
});
}

    if (wardSelect) {
    wardSelect.addEventListener("change", () => {
    this.updateAddress(prefix);
});
}
});
},

    setCurrentDate() {
    const today = new Date().toISOString().split("T")[0];
    const contractDateInput = document.getElementById("contract-date");
    const startDateInput = document.getElementById("start-date");

    if (contractDateInput) contractDateInput.value = today;
    if (startDateInput) startDateInput.value = today;

    this.updatePreviewField("contract-date", "preview-sign-date");
    this.updatePreviewField("start-date", "preview-start-date");
    this.calculateEndDate();
},

    showTab(tabId) {
    document.querySelectorAll(".tab-pane").forEach((pane) => {
    pane.classList.remove("show", "active");
});

    document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
    link.classList.remove("active");
});

    const targetTab = document.getElementById(tabId);
    const targetLink = document.querySelector(`[data-tab="${tabId}"]`);

    if (targetTab && targetLink) {
    targetTab.classList.add("show", "active");
    targetLink.classList.add("active");
    this.currentTab = tabId;
}

    window.scrollTo({top: 0, behavior: "smooth"});
},

    previewImage(event, previewId) {
    const file = event.target.files[0];
    const preview = document.getElementById(previewId);

    if (file) {
    const reader = new FileReader();
    reader.onload = (e) => {
    preview.innerHTML = `<img src="${e.target.result}" alt="Ảnh CCCD" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`;
};
    reader.readAsDataURL(file);
}
},

    updateAllPreview() {
    this.updatePreviewField("contract-date", "preview-sign-date");
    this.updatePreviewField("start-date", "preview-start-date");
    this.calculateEndDate();
    this.calculateDeposit();
    this.updatePaymentMethod();
    this.updateAmenities();
},

    updatePreviewField(inputId, previewId) {
    const input = document.getElementById(inputId);
    const preview = document.getElementById(previewId);

    if (input && preview) {
    let value = input.value;

    if (input.type === "date" && value) {
    const date = new Date(value);
    value = date.toLocaleDateString("vi-VN");
}

    if (inputId === "rent-price" && value) {
    value = new Intl.NumberFormat("vi-VN").format(value);
}

    if (input.tagName === "TEXTAREA" && value) {
    preview.innerHTML = value.replace(/\n/g, "<br>");
} else {
    preview.textContent = value || "........................";
}

    if (value) {
    preview.classList.add("nha-tro-updated");
    setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000);
}

    if (inputId === "start-date" || inputId === "contract-duration") {
    this.calculateEndDate();
}
    if (inputId === "rent-price" || inputId === "deposit-months") {
    this.calculateDeposit();
}
}
},

    updateAddress(prefix) {
    const street = document.getElementById(`${prefix}-street`)?.value || "";
    const ward = this.getSelectText(`${prefix}-ward`);
    const district = this.getSelectText(`${prefix}-district`);
    const province = this.getSelectText(`${prefix}-province`);

    const parts = [street, ward, district, province].filter((part) => part && !part.includes("Chọn") && part !== "");
    const fullAddress = parts.join(", ");

    const previewId = `preview-${prefix === "room" ? "room" : prefix}-address`;
    const preview = document.getElementById(previewId);
    if (preview) {
    preview.textContent = fullAddress || "........................";
    if (fullAddress) {
    preview.classList.add("nha-tro-updated");
    setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000);
}
}
},

    updatePaymentMethod() {
    const paymentMethod = document.getElementById("payment-method");
    const preview = document.getElementById("preview-payment-method");

    if (paymentMethod && preview) {
    const selectedText = paymentMethod.options[paymentMethod.selectedIndex]?.text || "........................";
    preview.textContent = selectedText;
}
},

    updateAmenities() {
    const checkboxes = document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]:checked');
    const amenities = Array.from(checkboxes).map((cb) => {
    const label = document.querySelector(`label[for="${cb.id}"]`);
    return label ? label.textContent : cb.id;
});

    const preview = document.getElementById("preview-amenities");
    if (preview) {
    preview.textContent = amenities.length > 0 ? amenities.join(", ") : "........................";
}
},

    calculateEndDate() {
    const startDate = document.getElementById("start-date")?.value;
    const duration = document.getElementById("contract-duration")?.value;

    if (startDate && duration) {
    const start = new Date(startDate);
    const end = new Date(start);
    end.setMonth(end.getMonth() + Number.parseInt(duration));

    const preview = document.getElementById("preview-end-date");
    if (preview) {
    preview.textContent = end.toLocaleDateString("vi-VN");
}
}
},

    calculateDeposit() {
    const rentPrice = document.getElementById("rent-price")?.value;
    const depositMonths = document.getElementById("deposit-months")?.value;

    if (rentPrice && depositMonths) {
    const deposit = Number.parseInt(rentPrice) * Number.parseInt(depositMonths);
    const preview = document.getElementById("preview-deposit");
    if (preview) {
    preview.textContent = new Intl.NumberFormat("vi-VN").format(deposit);
}
}
},

    getSelectText(id) {
    const element = document.getElementById(id);
    if (element && element.selectedIndex >= 0) {
    return element.options[element.selectedIndex].text;
}
    return "";
},

    // Các hàm zoom
    zoomIn() {
    this.zoomLevel = Math.min(this.zoomLevel + 0.1, 2);
    this.applyZoom();
},

    zoomOut() {
    this.zoomLevel = Math.max(this.zoomLevel - 0.1, 0.5);
    this.applyZoom();
},

    resetZoom() {
    this.zoomLevel = 1;
    this.applyZoom();
},

    applyZoom() {
    const container = document.getElementById("preview-container");
    if (container) {
    container.style.transform = `scale(${this.zoomLevel})`;
}
},

    // Các hàm hành động
    updateContract() {
    this.showNotification("Hợp đồng đã được cập nhật!", "success");
},

    printContract() {
    const printContent = document.getElementById("contract-preview").innerHTML;
    const printWindow = window.open("", "_blank");
    printWindow.document.write(`
                <html>
                    <head>
                        <title>Hợp đồng thuê nhà</title>
                        <style>
                            body { font-family: 'Times New Roman', serif; line-height: 1.8; }
                            .nha-tro-highlight { color: #000; font-weight: normal; }
                            @media print { body { margin: 0; } }
                        </style>
                    </head>
                    <body>${printContent}</body>
                </html>
            `);
    printWindow.document.close();
    printWindow.print();
},

    saveContract() {
    const contractData = this.collectFormData();
    console.log("Lưu hợp đồng:", contractData);
    // Gửi dữ liệu qua AJAX nếu cần
    fetch('/api/contracts', {
    method: 'POST',
    headers: {'Content-Type': 'application/json'},
    body: JSON.stringify(contractData)
}).then(response => response.json())
    .then(data => this.showNotification("Hợp đồng đã được lưu thành công!", "success"))
    .catch(error => this.showNotification("Lỗi khi lưu hợp đồng!", "error"));
},

    collectFormData() {
    return {
    tenant: {
    name: document.getElementById("tenant-name")?.value || "",
    dob: document.getElementById("tenant-dob")?.value || "",
    id: document.getElementById("tenant-id")?.value || "",
    phone: document.getElementById("tenant-phone")?.value || "",
    email: document.getElementById("tenant-email")?.value || "",
},
    owner: {
    name: document.getElementById("owner-name")?.value || "",
    dob: document.getElementById("owner-dob")?.value || "",
    id: document.getElementById("owner-id")?.value || "",
    phone: document.getElementById("owner-phone")?.value || "",
},
    room: {
    address: this.getFullAddress("room"),
    number: document.getElementById("room-number")?.value || "",
    area: document.getElementById("room-area")?.value || "",
},
    terms: {
    rentPrice: document.getElementById("rent-price")?.value || "",
    duration: document.getElementById("contract-duration")?.value || "",
    startDate: document.getElementById("start-date")?.value || "",
    paymentMethod: document.getElementById("payment-method")?.value || "",
},
};
},

    getFullAddress(prefix) {
    const street = document.getElementById(`${prefix}-street`)?.value || "";
    const ward = this.getSelectText(`${prefix}-ward`);
    const district = this.getSelectText(`${prefix}-district`);
    const province = this.getSelectText(`${prefix}-province`);

    return [street, ward, district, province].filter((part) => part && !part.includes("Chọn")).join(", ");
},

    showNotification(message, type = "info") {
    const notification = document.createElement("div");
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = "top: 20px; right: 20px; z-index: 9999; min-width: 400px;";
    notification.innerHTML = `
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            `;

    document.body.appendChild(notification);

    setTimeout(() => {
    if (notification.parentNode) {
    notification.remove();
}
}, 10000);
},

    setupAmenityModal() {
    const addAmenityBtn = document.getElementById("btn-add-amenity-host");
    const saveAmenityBtn = document.getElementById("saveAmenity-host");
    const amenityForm = document.getElementById("addAmenityForm-host");
    const amenityNameInput = document.getElementById("amenityName-host");

    if (addAmenityBtn) {
    addAmenityBtn.addEventListener("click", () => {
    const modal = new bootstrap.Modal(document.getElementById("addAmenityModal-host"));
    modal.show();
    amenityForm.reset();
});
}

    if (saveAmenityBtn) {
    saveAmenityBtn.addEventListener("click", () => {
    this.saveNewAmenity();
});
}

    if (amenityNameInput) {
    amenityNameInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") {
    e.preventDefault();
    this.saveNewAmenity();
}
});
}
},

    saveNewAmenity() {
    const amenityNameInput = document.getElementById("amenityName-host");
    const amenityName = amenityNameInput.value.trim();

    if (!amenityName) {
    this.showNotification("Vui lòng nhập tên tiện ích", "warning");
    amenityNameInput.focus();
    return;
}

    const existingAmenities = document.querySelectorAll("#amenities-list-host .form-check-label");
    const exists = Array.from(existingAmenities).some(
    (label) => label.textContent.toLowerCase() === amenityName.toLowerCase(),
    );

    if (exists) {
    this.showNotification("Tiện ích này đã tồn tại!", "warning");
    return;
}

    this.addAmenityToList(amenityName);

    const modal = bootstrap.Modal.getInstance(document.getElementById("addAmenityModal-host"));
    modal.hide();

    this.showNotification(`Đã thêm tiện ích "${amenityName}" thành công!`, "success");
    this.updateAmenities();
},

    addAmenityToList(amenityName) {
    const amenitiesList = document.getElementById("amenities-list-host");
    const amenityId = "amenity-" + Date.now() + "-host";

    const amenityDiv = document.createElement("div");
    amenityDiv.className = "form-check nha-tro-host-custom-amenity";
    amenityDiv.innerHTML = `
                <input class="form-check-input" type="checkbox" id="${amenityId}">
                <label class="form-check-label" for="${amenityId}">${amenityName}</label>
                <button type="button" class="btn btn-sm btn-outline-danger nha-tro-host-remove-amenity" onclick="NhaTroContract.removeAmenity('${amenityId}')" title="Xóa tiện ích">
                    <i class="fa fa-times"></i>
                </button>
            `;

    amenitiesList.appendChild(amenityDiv);

    const newCheckbox = document.getElementById(amenityId);
    newCheckbox.addEventListener("change", () => {
    this.updateAmenities();
});
},

    removeAmenity(amenityId) {
    const amenityElement = document.getElementById(amenityId).closest(".nha-tro-host-custom-amenity");
    const amenityName = amenityElement.querySelector("label").textContent;

    if (confirm(`Bạn có chắc chắn muốn xóa tiện ích "${amenityName}"?`)) {
    amenityElement.remove();
    this.updateAmenities();
    this.showNotification(`Đã xóa tiện ích "${amenityName}"`, "info");
}
},

    setupCustomerModal() {
    const addCustomerBtn = document.getElementById("btn-add-customer-host");
    const saveCustomerBtn = document.getElementById("saveCustomer-host");
    const customerForm = document.getElementById("addCustomerForm-host");

    if (addCustomerBtn) {
    addCustomerBtn.addEventListener("click", () => {
    const modal = new bootstrap.Modal(document.getElementById("addCustomerModal-host"));
    modal.show();
    customerForm.reset();
    this.clearCustomerFormImages();
    this.setupCustomerLocationListeners();
});
}

    if (saveCustomerBtn) {
    saveCustomerBtn.addEventListener("click", () => {
    this.saveNewCustomer();
});
}

    document.getElementById("newCustomer-cccd-front")?.addEventListener("change", (e) => {
    this.previewCustomerImage(e, "newCustomer-cccd-front-preview");
});

    document.getElementById("newCustomer-cccd-back")?.addEventListener("change", (e) => {
    this.previewCustomerImage(e, "newCustomer-cccd-back-preview");
});
},

    setupCustomerLocationListeners() {
    const provinceSelect = document.getElementById("newCustomer-province");
    const districtSelect = document.getElementById("newCustomer-district");
    const wardSelect = document.getElementById("newCustomer-ward");

    if (provinceSelect) {
    provinceSelect.addEventListener("change", () => {
    if (provinceSelect.value) {
    this.loadDistricts(provinceSelect.value, "newCustomer-district", "newCustomer-ward");
} else {
    districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
}
});
}

    if (districtSelect) {
    districtSelect.addEventListener("change", () => {
    if (districtSelect.value) {
    this.loadWards(districtSelect.value, "newCustomer-ward");
} else {
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
}
});
}
},

    previewCustomerImage(event, previewId) {
    const file = event.target.files[0];
    const preview = document.getElementById(previewId);
    const uploadContainer = preview.closest(".nha-tro-image-upload");

    if (file) {
    const reader = new FileReader();
    reader.onload = (e) => {
    preview.innerHTML = `<img src="${e.target.result}" alt="Ảnh CCCD" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`;
    uploadContainer.classList.add("has-image");
};
    reader.readAsDataURL(file);
}
},

    clearCustomerFormImages() {
    document.getElementById("newCustomer-cccd-front-preview").innerHTML = `
                <i class="fa fa-camera fa-2x"></i>
                <div class="mt-2">Tải ảnh mặt trước</div>
                <small class="text-muted">Nhấn để chọn ảnh</small>
            `;
    document.getElementById("newCustomer-cccd-back-preview").innerHTML = `
                <i class="fa fa-camera fa-2x"></i>
                <div class="mt-2">Tải ảnh mặt sau</div>
                <small class="text-muted">Nhấn để chọn ảnh</small>
            `;

    document.querySelectorAll("#addCustomerModal-host .nha-tro-image-upload").forEach((container) => {
    container.classList.remove("has-image");
});
},

    saveNewCustomer() {
    const customerData = {
    name: document.getElementById("newCustomer-name").value || "",
    dob: document.getElementById("newCustomer-dob").value || "",
    id: document.getElementById("newCustomer-id").value || "",
    idDate: document.getElementById("newCustomer-id-date").value || "",
    idPlace: document.getElementById("newCustomer-id-place").value || "",
    phone: document.getElementById("newCustomer-phone").value || "",
    email: document.getElementById("newCustomer-email").value || "",
    province: document.getElementById("newCustomer-province").value || "",
    district: document.getElementById("newCustomer-district").value || "",
    ward: document.getElementById("newCustomer-ward").value || "",
    street: document.getElementById("newCustomer-street").value || "",
    notes: document.getElementById("newCustomer-notes").value || "",
    cccdFront: document.getElementById("newCustomer-cccd-front").files[0] || null,
    cccdBack: document.getElementById("newCustomer-cccd-back").files[0] || null,
};

    this.fillMainFormWithCustomerData(customerData);

    const modal = bootstrap.Modal.getInstance(document.getElementById("addCustomerModal-host"));
    modal.hide();

    this.showNotification(`Đã thêm thông tin khách thuê thành công!`, "success");
},

    fillMainFormWithCustomerData(customerData) {
    document.getElementById("tenant-name").value = customerData.name;
    document.getElementById("tenant-dob").value = customerData.dob;
    document.getElementById("tenant-id").value = customerData.id;
    document.getElementById("tenant-id-date").value = customerData.idDate;
    document.getElementById("tenant-id-place").value = customerData.idPlace;
    document.getElementById("tenant-phone").value = customerData.phone;
    document.getElementById("tenant-email").value = customerData.email;
    document.getElementById("tenant-street").value = customerData.street;

    const mainProvinceSelect = document.getElementById("tenant-province");
    const mainDistrictSelect = document.getElementById("tenant-district");
    const mainWardSelect = document.getElementById("tenant-ward");

    if (mainProvinceSelect && customerData.province) {
    mainProvinceSelect.value = customerData.province;
    if (customerData.district) {
    this.loadDistricts(customerData.province, "tenant-district", "tenant-ward");
    setTimeout(() => {
    mainDistrictSelect.value = customerData.district;
    if (customerData.ward) {
    this.loadWards(customerData.district, "tenant-ward");
    setTimeout(() => {
    mainWardSelect.value = customerData.ward;
    this.updateAddress("tenant");
}, 200);
}
}, 200);
}
}

    if (customerData.cccdFront) {
    this.transferImageToMainForm(customerData.cccdFront, "cccd-front", "cccd-front-preview");
}
    if (customerData.cccdBack) {
    this.transferImageToMainForm(customerData.cccdBack, "cccd-back", "cccd-back-preview");
}

    setTimeout(() => {
    this.updateAllPreview();
}, 500);
},

    transferImageToMainForm(file, inputId, previewId) {
    const mainInput = document.getElementById(inputId);
    const mainPreview = document.getElementById(previewId);

    if (mainInput && mainPreview) {
    const dt = new DataTransfer();
    dt.items.add(file);
    mainInput.files = dt.files;

    const reader = new FileReader();
    reader.onload = (e) => {
    mainPreview.innerHTML = `<img src="${e.target.result}" alt="Ảnh CCCD" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`;
};
    reader.readAsDataURL(file);
}
},
};

    // Khởi tạo khi DOM được tải
    document.addEventListener("DOMContentLoaded", () => {
    window.NhaTroContract.init();
});
    /*]]>*/


    // <script th:src="@{/bootstrap/js/bootstrap.bundle.min.js}"></script>



        // Housing area and room selection functionality
        document.addEventListener('DOMContentLoaded', function() {
        const housingAreaSelect = document.getElementById('housing-area');
        const roomSelectionSelect = document.getElementById('room-selection');

        // Sample room data for each housing area
        const roomData = {
        'khu-a': [
    {value: 'a101', text: 'Phòng A101 - 25m² - 3.5tr/tháng', number: 'A101', area: 25, price: 3500000},
    {value: 'a102', text: 'Phòng A102 - 30m² - 4tr/tháng', number: 'A102', area: 30, price: 4000000},
    {value: 'a103', text: 'Phòng A103 - 28m² - 3.8tr/tháng', number: 'A103', area: 28, price: 3800000},
    {value: 'a201', text: 'Phòng A201 - 25m² - 3.5tr/tháng', number: 'A201', area: 25, price: 3500000},
    {value: 'a202', text: 'Phòng A202 - 30m² - 4tr/tháng', number: 'A202', area: 30, price: 4000000}
        ],
        'khu-b': [
    {value: 'b101', text: 'Phòng B101 - 22m² - 3.2tr/tháng', number: 'B101', area: 22, price: 3200000},
    {value: 'b102', text: 'Phòng B102 - 27m² - 3.7tr/tháng', number: 'B102', area: 27, price: 3700000},
    {value: 'b103', text: 'Phòng B103 - 25m² - 3.5tr/tháng', number: 'B103', area: 25, price: 3500000},
    {value: 'b201', text: 'Phòng B201 - 22m² - 3.2tr/tháng', number: 'B201', area: 22, price: 3200000},
    {value: 'b202', text: 'Phòng B202 - 27m² - 3.7tr/tháng', number: 'B202', area: 27, price: 3700000}
        ],
        'khu-c': [
    {value: 'c101', text: 'Phòng C101 - 35m² - 4.5tr/tháng', number: 'C101', area: 35, price: 4500000},
    {value: 'c102', text: 'Phòng C102 - 32m² - 4.2tr/tháng', number: 'C102', area: 32, price: 4200000},
    {value: 'c103', text: 'Phòng C103 - 30m² - 4tr/tháng', number: 'C103', area: 30, price: 4000000},
    {value: 'c201', text: 'Phòng C201 - 35m² - 4.5tr/tháng', number: 'C201', area: 35, price: 4500000}
        ],
        'khu-d': [
    {value: 'd101', text: 'Phòng D101 - 20m² - 2.8tr/tháng', number: 'D101', area: 20, price: 2800000},
    {value: 'd102', text: 'Phòng D102 - 24m² - 3.2tr/tháng', number: 'D102', area: 24, price: 3200000},
    {value: 'd103', text: 'Phòng D103 - 26m² - 3.4tr/tháng', number: 'D103', area: 26, price: 3400000},
    {value: 'd201', text: 'Phòng D201 - 20m² - 2.8tr/tháng', number: 'D201', area: 20, price: 2800000},
    {value: 'd202', text: 'Phòng D202 - 24m² - 3.2tr/tháng', number: 'D202', area: 24, price: 3200000}
        ]
    };

        // Handle housing area selection change
        housingAreaSelect.addEventListener('change', function() {
        const selectedArea = this.value;

        // Clear room selection
        roomSelectionSelect.innerHTML = '<option value="">-- Chọn phòng trọ --</option>';

        if (selectedArea && roomData[selectedArea]) {
        // Enable room selection
        roomSelectionSelect.disabled = false;

        // Populate room options
        roomData[selectedArea].forEach(room => {
        const option = document.createElement('option');
        option.value = room.value;
        option.textContent = room.text;
        roomSelectionSelect.appendChild(option);
    });
    } else {
        // Disable room selection if no area selected
        roomSelectionSelect.disabled = true;
        // Clear form fields
        document.getElementById('room-number').value = '';
        document.getElementById('room-area').value = '';
        document.getElementById('rent-price').value = '';

        // Update preview
        if (window.NhaTroContract) {
        window.NhaTroContract.updatePreviewField('room-number', 'preview-room-number');
        window.NhaTroContract.updatePreviewField('room-area', 'preview-room-area');
        window.NhaTroContract.updatePreviewField('rent-price', 'preview-rent');
    }
    }
    });

        // Handle room selection change
        roomSelectionSelect.addEventListener('change', function() {
        const selectedArea = housingAreaSelect.value;
        const selectedRoomValue = this.value;

        if (selectedArea && selectedRoomValue && roomData[selectedArea]) {
        // Find the selected room data
        const selectedRoom = roomData[selectedArea].find(room => room.value === selectedRoomValue);

        if (selectedRoom) {
        // Auto-fill room information
        document.getElementById('room-number').value = selectedRoom.number;
        document.getElementById('room-area').value = selectedRoom.area;
        document.getElementById('rent-price').value = selectedRoom.price;

        // Update contract preview using the existing NhaTroContract methods
        if (window.NhaTroContract) {
        // Update preview fields with highlight effect
        window.NhaTroContract.updatePreviewField('room-number', 'preview-room-number');
        window.NhaTroContract.updatePreviewField('room-area', 'preview-room-area');
        window.NhaTroContract.updatePreviewField('rent-price', 'preview-rent');

        // Recalculate deposit based on new rent price
        window.NhaTroContract.calculateDeposit();

        // Show notification
        window.NhaTroContract.showNotification(
        `Đã chọn ${selectedRoom.number} - Diện tích: ${selectedRoom.area}m² - Giá: ${new Intl.NumberFormat('vi-VN').format(selectedRoom.price)} VNĐ/tháng`,
        'success'
        );
    }
    }
    } else {
        // Clear fields if no room selected
        document.getElementById('room-number').value = '';
        document.getElementById('room-area').value = '';
        document.getElementById('rent-price').value = '';

        // Update preview
        if (window.NhaTroContract) {
        window.NhaTroContract.updatePreviewField('room-number', 'preview-room-number');
        window.NhaTroContract.updatePreviewField('room-area', 'preview-room-area');
        window.NhaTroContract.updatePreviewField('rent-price', 'preview-rent');
        window.NhaTroContract.calculateDeposit();
    }
    }
    });
    });

