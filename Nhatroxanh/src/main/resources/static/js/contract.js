// Namespace để tránh xung đột
window.NhaTroContract = {
    // Properties
    currentTab: "tenantInfo",
    zoomLevel: 1,
    residents: [],
    bootstrap: null, // Will be initialized after DOM loads

    // Initialize application
    init() {
        // Initialize bootstrap reference
        this.bootstrap = window.bootstrap || window.bootstrap // Declared variable here

        this.setupEventListeners()
        this.setCurrentDate()
        this.updateAllPreview()
        this.setupAmenityModal()
        this.setupCustomerModal()
        this.setupResidentModal()
        this.loadProvinces()
    },

    // Setup all event listeners
    setupEventListeners() {
        // Tab navigation
        this.setupTabListeners()

        // Button events
        this.setupButtonListeners()

        // Image upload events
        this.setupImageListeners()

        // Form input events for live preview
        this.setupPreviewListeners()

        // Location listeners
        this.setupLocationListeners()
    },

    setupTabListeners() {
        document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
            link.addEventListener("click", (e) => {
                e.preventDefault()
                const tabId = link.getAttribute("data-tab")
                this.showTab(tabId)
            })
        })
    },

    setupButtonListeners() {
        // Navigation buttons
        const buttonMappings = [
            { id: "btn-next-owner", action: () => this.showTab("ownerInfo") },
            { id: "btn-prev-tenant", action: () => this.showTab("tenantInfo") },
            { id: "btn-next-room", action: () => this.showTab("roomInfo") },
            { id: "btn-prev-owner", action: () => this.showTab("ownerInfo") },
            { id: "btn-next-terms", action: () => this.showTab("terms") },
            { id: "btn-prev-room", action: () => this.showTab("roomInfo") },

            // Action buttons
            { id: "btn-update", action: () => this.updateContract() },
            { id: "btn-print", action: () => this.printContract() },
            { id: "btn-save", action: () => this.saveContract() },

            // Zoom buttons
            { id: "btn-zoom-in", action: () => this.zoomIn() },
            { id: "btn-zoom-out", action: () => this.zoomOut() },
            { id: "btn-reset-zoom", action: () => this.resetZoom() },
        ]

        buttonMappings.forEach(({ id, action }) => {
            const element = document.getElementById(id)
            if (element) {
                element.addEventListener("click", action)
            }
        })
    },

    setupImageListeners() {
        const imageInputs = [
            { id: "cccd-front", preview: "cccd-front-preview" },
            { id: "cccd-back", preview: "cccd-back-preview" },
        ]

        imageInputs.forEach(({ id, preview }) => {
            const element = document.getElementById(id)
            if (element) {
                element.addEventListener("change", (e) => {
                    this.previewImage(e, preview)
                })
            }
        })
    },

    setupPreviewListeners() {
        const inputs = [
            { id: "tenant-name", preview: "preview-tenant-name" },
            { id: "tenant-dob", preview: "preview-tenant-dob" },
            { id: "tenant-id", preview: "preview-tenant-id" },
            { id: "tenant-id-date", preview: "preview-tenant-id-date" },
            { id: "tenant-id-place", preview: "preview-tenant-id-place" },
            { id: "tenant-phone", preview: "preview-tenant-phone" },
            { id: "owner-name", preview: "preview-owner-name" },
            { id: "owner-dob", preview: "preview-owner-dob" },
            { id: "owner-id", preview: "preview-owner-id" },
            { id: "owner-id-date", preview: "preview-owner-id-date" },
            { id: "owner-id-place", preview: "preview-owner-id-place" },
            { id: "owner-phone", preview: "preview-owner-phone" },
            { id: "room-number", preview: "preview-room-number" },
            { id: "room-area", preview: "preview-room-area" },
            { id: "rent-price", preview: "preview-rent" },
            { id: "contract-duration", preview: "preview-duration" },
            { id: "start-date", preview: "preview-start-date" },
            { id: "contract-date", preview: "preview-sign-date" },
            { id: "payment-date", preview: "preview-payment-date" },
            { id: "deposit-months", preview: "preview-deposit-months" },
            { id: "terms-conditions", preview: "preview-terms" },
        ]

        inputs.forEach((input) => {
            const element = document.getElementById(input.id)
            if (element) {
                element.addEventListener("input", () => {
                    this.updatePreviewField(input.id, input.preview)
                })
                element.addEventListener("change", () => {
                    this.updatePreviewField(input.id, input.preview)
                })
            }
        })

        // Special handlers
        const paymentMethod = document.getElementById("payment-method")
        if (paymentMethod) {
            paymentMethod.addEventListener("change", () => {
                this.updatePaymentMethod()
            })
        }

        // Amenities checkboxes
        document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]').forEach((checkbox) => {
            checkbox.addEventListener("change", () => {
                this.updateAmenities()
            })
        })

        // Address fields
        const addressFields = ["tenant", "owner", "room"]
        addressFields.forEach((prefix) => {
            ;["province", "district", "ward", "street"].forEach((field) => {
                const element = document.getElementById(`${prefix}-${field}`)
                if (element) {
                    element.addEventListener("change", () => {
                        this.updateAddress(prefix)
                    })
                }
            })
        })
    },

    // Location API functions
    async loadProvinces() {
        try {
            const response = await fetch("https://provinces.open-api.vn/api/p/")
            const provinces = await response.json()

            const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"]
            selects.forEach((selectId) => {
                const select = document.getElementById(selectId)
                if (select) {
                    select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>'
                    provinces.forEach((province) => {
                        const option = document.createElement("option")
                        option.value = province.code
                        option.textContent = province.name
                        select.appendChild(option)
                    })
                }
            })
        } catch (error) {
            console.error("Error loading provinces:", error)
            this.showNotification("Không thể tải danh sách tỉnh/thành phố", "warning")
        }
    },

    async loadDistricts(provinceCode, districtSelectId, wardSelectId) {
        try {
            const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
            const province = await response.json()

            const districtSelect = document.getElementById(districtSelectId)
            const wardSelect = document.getElementById(wardSelectId)

            if (districtSelect) {
                districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
                province.districts.forEach((district) => {
                    const option = document.createElement("option")
                    option.value = district.code
                    option.textContent = district.name
                    districtSelect.appendChild(option)
                })
            }

            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
            }
        } catch (error) {
            console.error("Error loading districts:", error)
            this.showNotification("Không thể tải danh sách quận/huyện", "warning")
        }
    },

    async loadWards(districtCode, wardSelectId) {
        try {
            const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
            const district = await response.json()

            const wardSelect = document.getElementById(wardSelectId)
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                district.wards.forEach((ward) => {
                    const option = document.createElement("option")
                    option.value = ward.code
                    option.textContent = ward.name
                    wardSelect.appendChild(option)
                })
            }
        } catch (error) {
            console.error("Error loading wards:", error)
            this.showNotification("Không thể tải danh sách phường/xã", "warning")
        }
    },

    setupLocationListeners() {
        const prefixes = ["tenant", "owner", "room"]

        prefixes.forEach((prefix) => {
            const provinceSelect = document.getElementById(`${prefix}-province`)
            const districtSelect = document.getElementById(`${prefix}-district`)
            const wardSelect = document.getElementById(`${prefix}-ward`)

            if (provinceSelect) {
                provinceSelect.addEventListener("change", () => {
                    if (provinceSelect.value) {
                        this.loadDistricts(provinceSelect.value, `${prefix}-district`, `${prefix}-ward`)
                    } else {
                        if (districtSelect) districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
                        if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                    }
                    this.updateAddress(prefix)
                })
            }

            if (districtSelect) {
                districtSelect.addEventListener("change", () => {
                    if (districtSelect.value) {
                        this.loadWards(districtSelect.value, `${prefix}-ward`)
                    } else {
                        if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                    }
                    this.updateAddress(prefix)
                })
            }

            if (wardSelect) {
                wardSelect.addEventListener("change", () => {
                    this.updateAddress(prefix)
                })
            }
        })
    },

    // Resident management
    setupResidentModal() {
        const addResidentBtn = document.getElementById("btn-add-resident")
        const saveResidentBtn = document.getElementById("btn-save-resident")
        const searchBtn = document.getElementById("btn-search-customer")

        if (addResidentBtn) {
            addResidentBtn.addEventListener("click", () => {
                const modal = new window.bootstrap.Modal(document.getElementById("addResidentModal"))
                modal.show()
                this.clearResidentForm()
            })
        }

        if (saveResidentBtn) {
            saveResidentBtn.addEventListener("click", () => {
                this.saveResident()
            })
        }

        if (searchBtn) {
            searchBtn.addEventListener("click", () => {
                this.searchCustomer()
            })
        }

        // Enter key to search
        const searchInput = document.getElementById("search-customer-phone")
        if (searchInput) {
            searchInput.addEventListener("keypress", (e) => {
                if (e.key === "Enter") {
                    e.preventDefault()
                    this.searchCustomer()
                }
            })
        }
    },

    async searchCustomer() {
        const phone = document.getElementById("search-customer-phone").value.trim()
        const resultsDiv = document.getElementById("search-results")

        if (!phone) {
            this.showNotification("Vui lòng nhập số điện thoại", "warning")
            return
        }

        // Show loading
        resultsDiv.innerHTML = `
            <div class="text-center py-3">
                <div class="spinner-border spinner-border-sm me-2" role="status"></div>
                Đang tìm kiếm...
            </div>
        `

        try {
            // Simulate API call - replace with actual API
            await new Promise((resolve) => setTimeout(resolve, 1000))

            // Mock data - replace with actual API response
            const mockCustomers = [
                {
                    id: 1,
                    name: "Nguyễn Văn A",
                    phone: "0123456789",
                    birthYear: 1990,
                    idNumber: "123456789",
                    isRegistered: true,
                },
                {
                    id: 2,
                    name: "Trần Thị B",
                    phone: "0987654321",
                    birthYear: 1995,
                    idNumber: "987654321",
                    isRegistered: true,
                },
            ]

            const foundCustomers = mockCustomers.filter((customer) => customer.phone.includes(phone))

            if (foundCustomers.length > 0) {
                resultsDiv.innerHTML = `
                    <div class="border rounded p-3 bg-light">
                        <h6 class="mb-3">Tìm thấy ${foundCustomers.length} khách hàng:</h6>
                        ${foundCustomers
                        .map(
                            (customer) => `
                            <div class="card mb-2">
                                <div class="card-body p-3">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <h6 class="mb-1">${customer.name}</h6>
                                            <small class="text-muted">
                                                SĐT: ${customer.phone} | Năm sinh: ${customer.birthYear}
                                                <span class="badge bg-success ms-2">Đã đăng ký</span>
                                            </small>
                                        </div>
                                        <button class="btn btn-sm btn-primary" onclick="window.NhaTroContract.selectExistingCustomer(${customer.id}, '${customer.name}', ${customer.birthYear}, '${customer.phone}', '${customer.idNumber}', ${customer.isRegistered})">
                                            <i class="fa fa-plus"></i> Chọn
                                        </button>
                                    </div>
                                </div>
                            </div>
                        `,
                        )
                        .join("")}
                    </div>
                `
            } else {
                resultsDiv.innerHTML = `
                    <div class="alert alert-info">
                        <i class="fa fa-info-circle me-2"></i>
                        Không tìm thấy khách hàng với số điện thoại này. Bạn có thể thêm người mới bên dưới.
                    </div>
                `
            }
        } catch (error) {
            resultsDiv.innerHTML = `
                <div class="alert alert-danger">
                    <i class="fa fa-exclamation-triangle me-2"></i>
                    Có lỗi xảy ra khi tìm kiếm. Vui lòng thử lại.
                </div>
            `
        }
    },

    selectExistingCustomer(id, name, birthYear, phone, idNumber, isRegistered) {
        const resident = {
            id: Date.now(),
            customerId: id,
            name: name,
            birthYear: birthYear,
            phone: phone,
            idNumber: idNumber,
            isRegistered: isRegistered,
            notes: "",
        }

        this.residents.push(resident)
        this.updateResidentsList()
        this.updateResidentsPreview()

        // Close modal
        const modal = window.bootstrap.Modal.getInstance(document.getElementById("addResidentModal"))
        if (modal) modal.hide()

        this.showNotification(`Đã thêm ${name} vào danh sách người ở`, "success")
    },

    saveResident() {
        const name = document.getElementById("resident-name").value.trim()
        const birthYear = document.getElementById("resident-birth-year").value
        const phone = document.getElementById("resident-phone").value.trim()
        const idNumber = document.getElementById("resident-id").value.trim()
        const notes = document.getElementById("resident-notes").value.trim()

        if (!name || !birthYear) {
            this.showNotification("Vui lòng nhập họ tên và năm sinh", "warning")
            return
        }

        const resident = {
            id: Date.now(),
            customerId: null,
            name: name,
            birthYear: Number.parseInt(birthYear),
            phone: phone,
            idNumber: idNumber,
            isRegistered: false,
            notes: notes,
        }

        this.residents.push(resident)
        this.updateResidentsList()
        this.updateResidentsPreview()

        // Close modal
        const modal = window.bootstrap.Modal.getInstance(document.getElementById("addResidentModal"))
        if (modal) modal.hide()

        this.showNotification(`Đã thêm ${name} vào danh sách người ở`, "success")
    },

    updateResidentsList() {
        const container = document.getElementById("residents-list")
        const countBadge = document.getElementById("residents-count")
        const noMessage = document.getElementById("no-residents-message")

        if (countBadge) countBadge.textContent = this.residents.length

        if (this.residents.length === 0) {
            if (noMessage) noMessage.style.display = "block"
            return
        }

        if (noMessage) noMessage.style.display = "none"

        if (container) {
            container.innerHTML = this.residents
                .map(
                    (resident) => `
                <div class="nha-tro-resident-card" data-resident-id="${resident.id}">
                    <div class="card mb-3">
                        <div class="card-body">
                            <div class="d-flex justify-content-between align-items-start">
                                <div class="flex-grow-1">
                                    <div class="d-flex align-items-center mb-2">
                                        <h6 class="mb-0 me-2">${resident.name}</h6>
                                        <span class="badge ${resident.isRegistered ? "bg-success" : "bg-warning text-dark"}">
                                            ${resident.isRegistered ? "Đã đăng ký" : "Khách mới"}
                                        </span>
                                    </div>
                                    <div class="row g-2 text-sm">
                                        <div class="col-md-6">
                                            <small class="text-muted">
                                                <i class="fa fa-calendar me-1"></i>Năm sinh: ${resident.birthYear}
                                            </small>
                                        </div>
                                        ${resident.phone
                            ? `
                                            <div class="col-md-6">
                                                <small class="text-muted">
                                                    <i class="fa fa-phone me-1"></i>${resident.phone}
                                                </small>
                                            </div>
                                        `
                            : ""
                        }
                                        ${resident.idNumber
                            ? `
                                            <div class="col-md-6">
                                                <small class="text-muted">
                                                    <i class="fa fa-id-card me-1"></i>${resident.idNumber}
                                                </small>
                                            </div>
                                        `
                            : ""
                        }
                                        ${resident.notes
                            ? `
                                            <div class="col-12">
                                                <small class="text-muted">
                                                    <i class="fa fa-note-sticky me-1"></i>${resident.notes}
                                                </small>
                                            </div>
                                        `
                            : ""
                        }
                                    </div>
                                </div>
                                <div class="ms-3">
                                    <button class="btn btn-sm btn-outline-danger" onclick="window.NhaTroContract.removeResident(${resident.id})" title="Xóa">
                                        <i class="fa fa-trash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `,
                )
                .join("")
        }
    },

    removeResident(residentId) {
        const resident = this.residents.find((r) => r.id === residentId)
        if (resident && confirm(`Bạn có chắc chắn muốn xóa ${resident.name} khỏi danh sách?`)) {
            this.residents = this.residents.filter((r) => r.id !== residentId)
            this.updateResidentsList()
            this.updateResidentsPreview()
            this.showNotification(`Đã xóa ${resident.name} khỏi danh sách`, "info")
        }
    },

    updateResidentsPreview() {
        const previewSection = document.getElementById("preview-residents-section")
        const previewText = document.getElementById("preview-residents")

        if (this.residents.length === 0) {
            if (previewSection) previewSection.style.display = "none"
            return
        }

        if (previewSection) previewSection.style.display = "block"
        const residentsText = this.residents.map((resident) => `${resident.name} (${resident.birthYear})`).join(", ")

        if (previewText) previewText.textContent = residentsText
    },

    clearResidentForm() {
        const form = document.getElementById("addResidentForm")
        if (form) form.reset()

        const searchPhone = document.getElementById("search-customer-phone")
        if (searchPhone) searchPhone.value = ""

        const searchResults = document.getElementById("search-results")
        if (searchResults) searchResults.innerHTML = ""
    },

    // Amenity management
    setupAmenityModal() {
        const addAmenityBtn = document.getElementById("btn-add-amenity-host")
        const saveAmenityBtn = document.getElementById("saveAmenity-host")
        const amenityForm = document.getElementById("addAmenityForm-host")
        const amenityNameInput = document.getElementById("amenityName-host")

        if (addAmenityBtn) {
            addAmenityBtn.addEventListener("click", () => {
                const modal = new window.bootstrap.Modal(document.getElementById("addAmenityModal-host"))
                modal.show()
                if (amenityForm) amenityForm.reset()
            })
        }

        if (saveAmenityBtn) {
            saveAmenityBtn.addEventListener("click", () => {
                this.saveNewAmenity()
            })
        }

        if (amenityNameInput) {
            amenityNameInput.addEventListener("keypress", (e) => {
                if (e.key === "Enter") {
                    e.preventDefault()
                    this.saveNewAmenity()
                }
            })
        }
    },

    saveNewAmenity() {
        const amenityNameInput = document.getElementById("amenityName-host")
        const amenityName = amenityNameInput.value.trim()

        if (!amenityName) {
            this.showNotification("Vui lòng nhập tên tiện ích", "warning")
            if (amenityNameInput) amenityNameInput.focus()
            return
        }

        // Check if amenity already exists
        const existingAmenities = document.querySelectorAll("#amenities-list-host .form-check-label")
        const exists = Array.from(existingAmenities).some(
            (label) => label.textContent.toLowerCase() === amenityName.toLowerCase(),
        )

        if (exists) {
            this.showNotification("Tiện ích này đã tồn tại!", "warning")
            return
        }

        // Create new amenity
        this.addAmenityToList(amenityName)

        // Close modal
        const modal = window.bootstrap.Modal.getInstance(document.getElementById("addAmenityModal-host"))
        if (modal) modal.hide()

        this.showNotification(`Đã thêm tiện ích "${amenityName}" thành công!`, "success")
        this.updateAmenities()
    },

    addAmenityToList(amenityName) {
        const amenitiesList = document.getElementById("amenities-list-host")
        const amenityId = "amenity-" + Date.now() + "-host"

        const amenityDiv = document.createElement("div")
        amenityDiv.className = "form-check nha-tro-host-custom-amenity"
        amenityDiv.innerHTML = `
            <input class="form-check-input" type="checkbox" id="${amenityId}">
            <label class="form-check-label" for="${amenityId}">${amenityName}</label>
            <button type="button" class="btn btn-sm btn-outline-danger nha-tro-host-remove-amenity" onclick="window.NhaTroContract.removeAmenity('${amenityId}')" title="Xóa tiện ích">
                <i class="fa fa-times"></i>
            </button>
        `

        if (amenitiesList) amenitiesList.appendChild(amenityDiv)

        // Add event listener for the new checkbox
        const newCheckbox = document.getElementById(amenityId)
        if (newCheckbox) {
            newCheckbox.addEventListener("change", () => {
                this.updateAmenities()
            })
        }
    },

    removeAmenity(amenityId) {
        const amenityElement = document.getElementById(amenityId)
        if (!amenityElement) return

        const amenityContainer = amenityElement.closest(".nha-tro-host-custom-amenity")
        const amenityLabel = amenityContainer.querySelector("label")
        const amenityName = amenityLabel ? amenityLabel.textContent : "tiện ích này"

        if (confirm(`Bạn có chắc chắn muốn xóa tiện ích "${amenityName}"?`)) {
            amenityContainer.remove()
            this.updateAmenities()
            this.showNotification(`Đã xóa tiện ích "${amenityName}"`, "info")
        }
    },

    // Customer management
    setupCustomerModal() {
        const addCustomerBtn = document.getElementById("btn-add-customer-host")
        const saveCustomerBtn = document.getElementById("saveCustomer-host")
        const customerForm = document.getElementById("addCustomerForm-host")

        if (addCustomerBtn) {
            addCustomerBtn.addEventListener("click", () => {
                const modal = new window.bootstrap.Modal(document.getElementById("addCustomerModal-host"))
                modal.show()
                if (customerForm) customerForm.reset()
                this.clearCustomerFormImages()
                this.setupCustomerLocationListeners()
            })
        }

        if (saveCustomerBtn) {
            saveCustomerBtn.addEventListener("click", () => {
                this.saveNewCustomer()
            })
        }

        // Image upload events for new customer
        const frontInput = document.getElementById("newCustomer-cccd-front")
        const backInput = document.getElementById("newCustomer-cccd-back")

        if (frontInput) {
            frontInput.addEventListener("change", (e) => {
                this.previewCustomerImage(e, "newCustomer-cccd-front-preview")
            })
        }

        if (backInput) {
            backInput.addEventListener("change", (e) => {
                this.previewCustomerImage(e, "newCustomer-cccd-back-preview")
            })
        }
    },

    setupCustomerLocationListeners() {
        const provinceSelect = document.getElementById("newCustomer-province")
        const districtSelect = document.getElementById("newCustomer-district")
        const wardSelect = document.getElementById("newCustomer-ward")

        if (provinceSelect) {
            provinceSelect.addEventListener("change", () => {
                if (provinceSelect.value) {
                    this.loadDistricts(provinceSelect.value, "newCustomer-district", "newCustomer-ward")
                } else {
                    if (districtSelect) districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
                    if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                }
            })
        }

        if (districtSelect) {
            districtSelect.addEventListener("change", () => {
                if (districtSelect.value) {
                    this.loadWards(districtSelect.value, "newCustomer-ward")
                } else {
                    if (wardSelect) wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                }
            })
        }
    },

    previewCustomerImage(event, previewId) {
        const file = event.target.files[0]
        const preview = document.getElementById(previewId)
        const uploadContainer = preview ? preview.closest(".nha-tro-image-upload") : null

        if (file && preview) {
            const reader = new FileReader()
            reader.onload = (e) => {
                preview.innerHTML = `<img src="${e.target.result}" alt="CCCD Preview" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
                if (uploadContainer) uploadContainer.classList.add("has-image")
            }
            reader.readAsDataURL(file)
        }
    },

    clearCustomerFormImages() {
        const frontPreview = document.getElementById("newCustomer-cccd-front-preview")
        const backPreview = document.getElementById("newCustomer-cccd-back-preview")

        if (frontPreview) {
            frontPreview.innerHTML = `
                <i class="fa fa-camera fa-2x"></i>
                <div class="mt-2">Tải ảnh mặt trước</div>
                <small class="text-muted">Nhấn để chọn ảnh</small>
            `
        }

        if (backPreview) {
            backPreview.innerHTML = `
                <i class="fa fa-camera fa-2x"></i>
                <div class="mt-2">Tải ảnh mặt sau</div>
                <small class="text-muted">Nhấn để chọn ảnh</small>
            `
        }

        // Remove has-image class
        document.querySelectorAll("#addCustomerModal-host .nha-tro-image-upload").forEach((container) => {
            container.classList.remove("has-image")
        })
    },

    saveNewCustomer() {
        // Collect customer data
        const customerData = {
            name: this.getInputValue("newCustomer-name"),
            dob: this.getInputValue("newCustomer-dob"),
            id: this.getInputValue("newCustomer-id"),
            idDate: this.getInputValue("newCustomer-id-date"),
            idPlace: this.getInputValue("newCustomer-id-place"),
            phone: this.getInputValue("newCustomer-phone"),
            email: this.getInputValue("newCustomer-email"),
            province: this.getInputValue("newCustomer-province"),
            district: this.getInputValue("newCustomer-district"),
            ward: this.getInputValue("newCustomer-ward"),
            street: this.getInputValue("newCustomer-street"),
            notes: this.getInputValue("newCustomer-notes"),
            cccdFront: this.getFileInput("newCustomer-cccd-front"),
            cccdBack: this.getFileInput("newCustomer-cccd-back"),
        }

        // Update preview directly instead of filling main form
        this.updatePreviewWithCustomerData(customerData)

        // Close modal
        const modal = window.bootstrap.Modal.getInstance(document.getElementById("addCustomerModal-host"))
        if (modal) modal.hide()

        this.showNotification("Đã thêm thông tin khách thuê vào xem trước hợp đồng!", "success")
    },

    async updatePreviewWithCustomerData(customerData) {
        // Update preview fields directly
        const preview = document.getElementById("preview-tenant-name")
        if (preview) preview.textContent = customerData.name || "........................"

        const previewDob = document.getElementById("preview-tenant-dob")
        if (previewDob && customerData.dob) {
            const date = new Date(customerData.dob)
            previewDob.textContent = date.toLocaleDateString("vi-VN")
        }

        const previewId = document.getElementById("preview-tenant-id")
        if (previewId) previewId.textContent = customerData.id || "........................"

        const previewIdDate = document.getElementById("preview-tenant-id-date")
        if (previewIdDate && customerData.idDate) {
            const date = new Date(customerData.idDate)
            previewIdDate.textContent = date.toLocaleDateString("vi-VN")
        }

        const previewIdPlace = document.getElementById("preview-tenant-id-place")
        if (previewIdPlace) previewIdPlace.textContent = customerData.idPlace || "........................"

        const previewPhone = document.getElementById("preview-tenant-phone")
        if (previewPhone) previewPhone.textContent = customerData.phone || "........................"

        // Build address from location data
        const addressParts = []
        if (customerData.street) addressParts.push(customerData.street)
        
        // Get text values for location selects
        if (customerData.ward) {
            const wardSelect = document.getElementById("newCustomer-ward")
            const wardText = wardSelect && wardSelect.options[wardSelect.selectedIndex] ? 
                            wardSelect.options[wardSelect.selectedIndex].text : ""
            if (wardText && !wardText.includes("Chọn")) addressParts.push(wardText)
        }
        
        if (customerData.district) {
            const districtSelect = document.getElementById("newCustomer-district")
            const districtText = districtSelect && districtSelect.options[districtSelect.selectedIndex] ? 
                            districtSelect.options[districtSelect.selectedIndex].text : ""
            if (districtText && !districtText.includes("Chọn")) addressParts.push(districtText)
        }
        
        if (customerData.province) {
            const provinceSelect = document.getElementById("newCustomer-province")
            const provinceText = provinceSelect && provinceSelect.options[provinceSelect.selectedIndex] ? 
                            provinceSelect.options[provinceSelect.selectedIndex].text : ""
            if (provinceText && !provinceText.includes("Chọn")) addressParts.push(provinceText)
        }

        const fullAddress = addressParts.join(", ")
        const previewAddress = document.getElementById("preview-tenant-address")
        if (previewAddress) {
            previewAddress.textContent = fullAddress || "........................"
        }

        // Add highlight effect to updated fields
        const updatedFields = [
            "preview-tenant-name", "preview-tenant-dob", "preview-tenant-id",
            "preview-tenant-id-date", "preview-tenant-id-place", "preview-tenant-phone", "preview-tenant-address"
        ]

        updatedFields.forEach(fieldId => {
            const field = document.getElementById(fieldId)
            if (field && field.textContent !== "........................") {
                field.classList.add("nha-tro-updated")
                setTimeout(() => field.classList.remove("nha-tro-updated"), 1000)
            }
        })
    },

    // Utility functions
    getInputValue(id) {
        const element = document.getElementById(id)
        return element ? element.value || "" : ""
    },

    setInputValue(id, value) {
        const element = document.getElementById(id)
        if (element) element.value = value
    },

    getFileInput(id) {
        const element = document.getElementById(id)
        return element && element.files.length > 0 ? element.files[0] : null
    },

    // Date and initialization functions
    setCurrentDate() {
        const today = new Date().toISOString().split("T")[0]
        this.setInputValue("contract-date", today)
        this.setInputValue("start-date", today)

        this.updatePreviewField("contract-date", "preview-sign-date")
        this.updatePreviewField("start-date", "preview-start-date")
        this.calculateEndDate()
    },

    // Tab management
    showTab(tabId) {
        // Hide all tabs
        document.querySelectorAll(".tab-pane").forEach((pane) => {
            pane.classList.remove("show", "active")
        })

        // Remove active class from all nav links
        document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
            link.classList.remove("active")
        })

        // Show selected tab
        const targetTab = document.getElementById(tabId)
        const targetLink = document.querySelector(`[data-tab="${tabId}"]`)

        if (targetTab && targetLink) {
            targetTab.classList.add("show", "active")
            targetLink.classList.add("active")
            this.currentTab = tabId
        }

        // Scroll to top
        window.scrollTo({ top: 0, behavior: "smooth" })
    },

    // Image preview
    previewImage(event, previewId) {
        const file = event.target.files[0]
        const preview = document.getElementById(previewId)

        if (file && preview) {
            const reader = new FileReader()
            reader.onload = (e) => {
                preview.innerHTML = `<img src="${e.target.result}" alt="CCCD Preview" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
            }
            reader.readAsDataURL(file)
        }
    },

    // Preview updates
    updateAllPreview() {
        this.updatePreviewField("contract-date", "preview-sign-date")
        this.updatePreviewField("start-date", "preview-start-date")
        this.calculateEndDate()
        this.calculateDeposit()
        this.updatePaymentMethod()
        this.updateAmenities()
        this.updateResidentsPreview()
    },

    updatePreviewField(inputId, previewId) {
        const input = document.getElementById(inputId)
        const preview = document.getElementById(previewId)

        if (input && preview) {
            let value = input.value

            // Format date if needed
            if (input.type === "date" && value) {
                const date = new Date(value)
                value = date.toLocaleDateString("vi-VN")
            }

            // Format number if it's rent price
            if (inputId === "rent-price" && value) {
                value = new Intl.NumberFormat("vi-VN").format(value)
            }

            // Special handling for textarea
            if (input.tagName === "TEXTAREA" && value) {
                preview.innerHTML = value.replace(/\n/g, "<br>")
            } else {
                preview.textContent = value || "........................"
            }

            // Add highlight effect
            if (value) {
                preview.classList.add("nha-tro-updated")
                setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000)
            }

            // Update related calculations
            if (inputId === "start-date" || inputId === "contract-duration") {
                this.calculateEndDate()
            }
            if (inputId === "rent-price" || inputId === "deposit-months") {
                this.calculateDeposit()
            }
        }
    },

    updateAddress(prefix) {
        const street = this.getInputValue(`${prefix}-street`)
        const ward = this.getSelectText(`${prefix}-ward`)
        const district = this.getSelectText(`${prefix}-district`)
        const province = this.getSelectText(`${prefix}-province`)

        const parts = [street, ward, district, province].filter((part) => part && !part.includes("Chọn") && part !== "")
        const fullAddress = parts.join(", ")

        const previewId = `preview-${prefix === "room" ? "room" : prefix}-address`
        const preview = document.getElementById(previewId)
        if (preview) {
            preview.textContent = fullAddress || "........................"
            if (fullAddress) {
                preview.classList.add("nha-tro-updated")
                setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000)
            }
        }
    },

    updatePaymentMethod() {
        const paymentMethod = document.getElementById("payment-method")
        const preview = document.getElementById("preview-payment-method")

        if (paymentMethod && preview) {
            const selectedText = paymentMethod.options[paymentMethod.selectedIndex]?.text || "........................"
            preview.textContent = selectedText
        }
    },

    updateAmenities() {
        const checkboxes = document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]:checked')
        const amenities = Array.from(checkboxes).map((cb) => {
            const label = document.querySelector(`label[for="${cb.id}"]`)
            return label ? label.textContent : cb.id
        })

        const preview = document.getElementById("preview-amenities")
        if (preview) {
            preview.textContent = amenities.length > 0 ? amenities.join(", ") : "........................"
        }
    },

    calculateEndDate() {
        const startDate = this.getInputValue("start-date")
        const duration = this.getInputValue("contract-duration")

        if (startDate && duration) {
            const start = new Date(startDate)
            const end = new Date(start)
            end.setMonth(end.getMonth() + Number.parseInt(duration))

            const preview = document.getElementById("preview-end-date")
            if (preview) {
                preview.textContent = end.toLocaleDateString("vi-VN")
            }
        }
    },

    calculateDeposit() {
        const rentPrice = this.getInputValue("rent-price")
        const depositMonths = this.getInputValue("deposit-months")

        if (rentPrice && depositMonths) {
            const deposit = Number.parseInt(rentPrice) * Number.parseInt(depositMonths)
            const preview = document.getElementById("preview-deposit")
            if (preview) {
                preview.textContent = new Intl.NumberFormat("vi-VN").format(deposit)
            }
        }
    },

    getSelectText(id) {
        const element = document.getElementById(id)
        if (element && element.selectedIndex >= 0) {
            return element.options[element.selectedIndex].text
        }
        return ""
    },

    // Zoom functions
    zoomIn() {
        this.zoomLevel = Math.min(this.zoomLevel + 0.1, 2)
        this.applyZoom()
    },

    zoomOut() {
        this.zoomLevel = Math.max(this.zoomLevel - 0.1, 0.5)
        this.applyZoom()
    },

    resetZoom() {
        this.zoomLevel = 1
        this.applyZoom()
    },

    applyZoom() {
        const container = document.getElementById("preview-container")
        if (container) {
            container.style.transform = `scale(${this.zoomLevel})`
        }
    },

    // Action functions
    updateContract() {
        this.showNotification("Hợp đồng đã được cập nhật!", "success")
    },

    printContract() {
        const printContent = document.getElementById("contract-preview")
        if (!printContent) return

        const printWindow = window.open("", "_blank")
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
                <body>${printContent.innerHTML}</body>
            </html>
        `)
        printWindow.document.close()
        printWindow.print()
    },

    saveContract() {
        const contractData = this.collectFormData()
        console.log("Saving contract:", contractData)
        this.showNotification("Hợp đồng đã được lưu thành công!", "success")
    },

    collectFormData() {
        return {
            tenant: {
                name: this.getInputValue("tenant-name"),
                dob: this.getInputValue("tenant-dob"),
                id: this.getInputValue("tenant-id"),
                phone: this.getInputValue("tenant-phone"),
                email: this.getInputValue("tenant-email"),
            },
            owner: {
                name: this.getInputValue("owner-name"),
                dob: this.getInputValue("owner-dob"),
                id: this.getInputValue("owner-id"),
                phone: this.getInputValue("owner-phone"),
            },
            room: {
                address: this.getFullAddress("room"),
                number: this.getInputValue("room-number"),
                area: this.getInputValue("room-area"),
            },
            terms: {
                rentPrice: this.getInputValue("rent-price"),
                duration: this.getInputValue("contract-duration"),
                startDate: this.getInputValue("start-date"),
                paymentMethod: this.getInputValue("payment-method"),
            },
            residents: this.residents,
        }
    },

    getFullAddress(prefix) {
        const street = this.getInputValue(`${prefix}-street`)
        const ward = this.getSelectText(`${prefix}-ward`)
        const district = this.getSelectText(`${prefix}-district`)
        const province = this.getSelectText(`${prefix}-province`)

        return [street, ward, district, province].filter((part) => part && !part.includes("Chọn")).join(", ")
    },

    showNotification(message, type = "info") {
        const notification = document.createElement("div")
        notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`
        notification.style.cssText = "top: 20px; right: 20px; z-index: 9999; min-width: 400px;"
        notification.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `

        document.body.appendChild(notification)

        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove()
            }
        }, 5000)
    },
}

// Initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", () => {
    window.NhaTroContract.init()
})