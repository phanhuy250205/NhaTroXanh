/* <![CDATA[ */
window.NhaTroContract = {
    currentTab: "tenantInfo",
    zoomLevel: 1,

    async init() {
        // Kiểm tra các phần tử select cần thiết
        const requiredSelects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"];
        const missingSelects = requiredSelects.filter(id => !document.getElementById(id));
        if (missingSelects.length > 0) {
            console.error("Missing select elements in DOM:", missingSelects);
            this.showNotification("Không tìm thấy một số trường tỉnh/thành phố trong giao diện", "error");
        }

        this.setupEventListeners();
        this.setCurrentDate();
        this.updateAllPreview();
        this.setupAmenityModal();
        this.setupCustomerModal();

        try {
            // Đảm bảo provinces được tải trước
            await this.loadProvinces();
            console.log("Provinces loaded");

            const contract = /*[[${contract}]]*/ null;
            if (contract && contract.owner) {
                document.getElementById("owner-name").value = contract.owner.fullName || "";
                document.getElementById("owner-dob").value = contract.owner.birthday || "";
                document.getElementById("owner-id").value = contract.owner.id || "";
                document.getElementById("owner-id-date").value = contract.owner.issueDate || "";
                document.getElementById("owner-id-place").value = contract.owner.issuePlace || "";
                document.getElementById("owner-email").value = contract.owner.email || "";
                document.getElementById("owner-phone").value = contract.owner.phone || "";
                document.getElementById("owner-bank").value = contract.owner.bankAccount || "";
                document.getElementById("owner-street").value = contract.owner.street || "";

                if (contract.owner.province) {
                    const provinceCode = await this.mapProvinceNameToCode(contract.owner.province);
                    console.log("Mapped owner province code:", provinceCode);
                    if (provinceCode) {
                        document.getElementById("owner-province").value = provinceCode;
                        await this.loadDistricts(provinceCode, "owner-district", "owner-ward");
                        if (contract.owner.district) {
                            const districtCode = await this.mapDistrictNameToCode(provinceCode, contract.owner.district);
                            console.log("Mapped owner district code:", districtCode);
                            if (districtCode) {
                                document.getElementById("owner-district").value = districtCode;
                                await this.loadWards(districtCode, "owner-ward", provinceCode);
                                if (contract.owner.ward) {
                                    const wardCode = await this.mapWardNameToCode(districtCode, contract.owner.ward, provinceCode);
                                    console.log("Mapped owner ward code:", wardCode);
                                    if (wardCode) {
                                        document.getElementById("owner-ward").value = wardCode;
                                    } else {
                                        this.showNotification(`Không ánh xạ được phường/xã ${contract.owner.ward}`, "warning");
                                    }
                                }
                            } else {
                                this.showNotification(`Không ánh xạ được quận/huyện ${contract.owner.district}`, "warning");
                            }
                        }
                        this.updateAddress("owner");
                    } else {
                        this.showNotification(`Không ánh xạ được tỉnh/thành phố ${contract.owner.province}`, "warning");
                    }
                }
            }

            const hostelSelect = document.getElementById('hostelId');
            if (hostelSelect && hostelSelect.value) {
                this.filterRooms();
            }
        } catch (error) {
            console.error("Error loading provinces or owner data:", error);
            this.showNotification("Lỗi khi tải dữ liệu tỉnh/thành phố hoặc thông tin chủ trọ: " + error.message, "error");
        }
    },

    safeEncodeURL(value) {
        try {
            return encodeURIComponent(value).replace(/%25/g, '%');
        } catch (e) {
            console.error('Error encoding URL component:', value, e);
            return value;
        }
    },

    normalizeName(name) {
        if (!name) return "";
        // Chỉ normalize ký tự có dấu, giữ nguyên các tiền tố
        return name
            .normalize("NFD") // Phân tách ký tự có dấu
            .replace(/[\u0300-\u036f]/g, "") // Loại bỏ dấu
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
            e.preventDefault();
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

        // Sự kiện nhập số điện thoại
        const tenantPhoneInput = document.getElementById("tenant-phone");
        if (tenantPhoneInput) {
            tenantPhoneInput.addEventListener("input", (e) => {
                const phone = e.target.value.trim();
                if (phone.length >= 10) {
                    this.fetchTenantByPhone(phone);
                } else {
                    this.clearTenantFields();
                }
            });
        }

        // Sự kiện chọn khu trọ
        const hostelSelect = document.getElementById("hostelId");
        if (hostelSelect) {
            hostelSelect.addEventListener("change", () => this.filterRooms());
        }

        // Sự kiện chọn phòng trọ
        const roomSelect = document.getElementById("roomId");
        if (roomSelect) {
            roomSelect.addEventListener("change", () => this.onRoomSelected());
        }

        // Sự kiện chọn loại người thuê
        const tenantTypeSelect = document.getElementById("tenantType");
        if (tenantTypeSelect) {
            tenantTypeSelect.addEventListener("change", () => this.toggleTenantFields());
        }

        // Sự kiện khác
        this.setupPreviewListeners();
        this.setupLocationListeners();
    },

    // ... (filterRooms, onRoomSelected, clearRoomFields, toggleTenantFields, fetchTenantByPhone, fillTenantFields, etc. remain unchanged)

    async mapProvinceNameToCode(provinceName) {
        try {
            const response = await fetch("https://provinces.open-api.vn/api/p/");
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinces = await response.json();
            console.log("Danh sách tỉnh từ API:", provinces.map(p => ({ name: p.name, code: p.code })));
            const normalizedProvinceName = this.normalizeName(provinceName);
            console.log("Normalized province name:", normalizedProvinceName);
            const province = provinces.find(p => {
                const normalizedApiName = this.normalizeName(p.name);
                return normalizedApiName === normalizedProvinceName ||
                    normalizedApiName.includes(normalizedProvinceName) ||
                    normalizedProvinceName.includes(normalizedApiName) ||
                    p.name.toLowerCase().includes(provinceName.toLowerCase());
            });
            if (!province) {
                console.warn(`No match for province: ${provinceName}, checked variants:`, provinces.map(p => this.normalizeName(p.name)));
                return null;
            }
            const provinceCode = String(province.code).padStart(2, '0');
            console.log(`Found province: ${provinceName} -> Code: ${provinceCode}`);
            return provinceCode;
        } catch (error) {
            console.error("Mapping error:", error);
            this.showNotification("Lỗi khi tải danh sách tỉnh/thành phố", "error");
            return null;
        }
    },

    async mapDistrictNameToCode(provinceCode, districtName) {
        try {
            const provinceCodeStr = String(provinceCode).padStart(2, '0');
            if (!/^\d{2}$/.test(provinceCodeStr)) {
                console.warn("Invalid province code:", provinceCode);
                return null;
            }

            const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCodeStr}?depth=2`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const province = await response.json();
            console.log(`Districts fetched for province ${provinceCodeStr}:`, province.districts.map(d => d.name));

            const normalizedDistrictName = this.normalizeName(districtName);
            const districtVariants = [
                normalizedDistrictName,
                `quận ${normalizedDistrictName}`,
                `huyện ${normalizedDistrictName}`,
                `thành phố ${normalizedDistrictName}`,
                `thị xã ${normalizedDistrictName}`,
                normalizedDistrictName.replace(/^quận\s+/i, ''),
                normalizedDistrictName.replace(/^huyện\s+/i, ''),
                normalizedDistrictName.replace(/^thành phố\s+/i, ''),
                normalizedDistrictName.replace(/^thị xã\s+/i, '')
            ];

            const district = province.districts.find(d =>
                districtVariants.some(variant =>
                    this.normalizeName(d.name) === variant ||
                    this.normalizeName(d.name).includes(variant) ||
                    variant.includes(this.normalizeName(d.name)) ||
                    d.name.toLowerCase().includes(districtName.toLowerCase())
                )
            );

            if (!district) {
                console.warn(`District not found: ${districtName}, variants checked: ${districtVariants.join(', ')}`);
                return null;
            }

            console.log(`Mapped district: ${districtName} -> ${district.code}`);
            return district.code;
        } catch (error) {
            console.error("Lỗi khi ánh xạ tên quận:", error);
            this.showNotification("Lỗi khi tải danh sách quận/huyện", "error");
            return null;
        }
    },

    async mapWardNameToCode(districtCode, wardName, provinceCode) {
        try {
            const districtCodeStr = String(districtCode).trim();

            if (!districtCodeStr || districtCodeStr === 'null' || districtCodeStr === 'undefined') {
                console.warn(`Invalid district code: ${districtCode}`);
                return null;
            }

            console.log(`Mapping ward: ${wardName} for district ${districtCodeStr}`);

            const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCodeStr}?depth=2`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const district = await response.json();
            console.log(`Wards fetched for district ${districtCodeStr}:`, district.wards.map(w => w.name));

            const normalizedWardName = this.normalizeName(wardName);

            const wardVariants = new Set([
                normalizedWardName,
                `phường ${normalizedWardName}`,
                `xã ${normalizedWardName}`,
                `thị trấn ${normalizedWardName}`,
                normalizedWardName.replace(/^(phường|xã|thị trấn)\s+/i, '').trim()
            ]);

            const cleanVariants = Array.from(wardVariants).filter(v => v && v.length > 0);
            console.log(`Ward variants for "${wardName}":`, cleanVariants);

            const ward = district.wards.find(w => {
                const normalizedApiName = this.normalizeName(w.name);
                return cleanVariants.some(variant => {
                    if (normalizedApiName === variant) return true;
                    const apiNameWithoutPrefix = normalizedApiName.replace(/^(phường|xã|thị trấn)\s+/i, '').trim();
                    if (apiNameWithoutPrefix === variant) return true;
                    if (normalizedApiName.includes(variant) && variant.length > 3) return true;
                    if (variant.includes(normalizedApiName) && normalizedApiName.length > 3) return true;
                    if (w.name.toLowerCase().includes(wardName.toLowerCase())) return true;
                    return false;
                });
            });

            if (!ward) {
                console.warn(`Ward not found: ${wardName}`);
                console.warn(`Available wards:`, district.wards.map(w => w.name));
                console.warn(`Variants tried:`, cleanVariants);
                return null;
            }

            console.log(`Mapped ward: ${wardName} -> ${ward.name} (${ward.code})`);
            return ward.code;
        } catch (error) {
            console.error(`Error mapping ward ${wardName} for district ${districtCode}:`, error.message);
            return null;
        }
    },

    async loadProvinces() {
        try {
            const response = await fetch("https://provinces.open-api.vn/api/p/");
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinces = await response.json();
            console.log("Loaded provinces:", provinces.map(p => ({ code: p.code, name: p.name })));

            const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"];
            selects.forEach((selectId) => {
                const select = document.getElementById(selectId);
                if (!select) {
                    console.warn(`Select element with ID ${selectId} not found in DOM`);
                    return;
                }
                select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>';
                provinces.forEach((province) => {
                    const option = document.createElement("option");
                    const provinceCode = String(province.code).padStart(2, '0');
                    option.value = provinceCode;
                    option.textContent = province.name;
                    select.appendChild(option);

                    if (selectId === "owner-province") {
                        console.log(`Added province option: ${provinceCode} - ${province.name}`);
                    }
                });
                console.log(`Populated ${selectId} with ${provinces.length} provinces`);
            });

            return provinces;
        } catch (error) {
            console.error("Error loading provinces:", error);
            this.showNotification("Không thể tải danh sách tỉnh/thành phố: " + error.message, "error");
            return [];
        }
    },

    async loadDistricts(provinceCode, districtSelectId, wardSelectId) {
        try {
            const provinceCodeStr = String(provinceCode).padStart(2, '0');
            if (!/^\d{2}$/.test(provinceCodeStr)) {
                throw new Error(`Invalid province code: ${provinceCode}`);
            }

            const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCodeStr}?depth=2`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const province = await response.json();
            console.log(`Loaded districts for province ${provinceCodeStr}:`, province.districts.map(d => ({ code: d.code, name: d.name })));

            const districtSelect = document.getElementById(districtSelectId);
            const wardSelect = document.getElementById(wardSelectId);

            if (districtSelect) {
                districtSelect.innerHTML = '';
                const defaultOption = document.createElement("option");
                defaultOption.value = "";
                defaultOption.textContent = "Chọn Quận/Huyện";
                districtSelect.appendChild(defaultOption);

                province.districts.forEach((district) => {
                    const option = document.createElement("option");
                    option.value = String(district.code);
                    option.textContent = district.name;
                    districtSelect.appendChild(option);

                    if (districtSelectId === "owner-district") {
                        console.log(`Added district option: ${district.code} - ${district.name}`);
                    }
                });

                districtSelect.value = "";
            }

            if (wardSelect) {
                wardSelect.innerHTML = '';
                const defaultOption = document.createElement("option");
                defaultOption.value = "";
                defaultOption.textContent = "Chọn Phường/Xã";
                wardSelect.appendChild(defaultOption);
                wardSelect.value = "";
            }
        } catch (error) {
            console.error(`Error loading districts for province ${provinceCode}:`, error);
            this.showNotification("Không thể tải danh sách quận/huyện: " + error.message, "error");
        }
    },

    async loadWards(districtCode, wardSelectId, provinceCode) {
        try {
            const districtCodeStr = String(districtCode).trim();

            if (!districtCodeStr || districtCodeStr === 'null' || districtCodeStr === 'undefined' || !/^\d+$/.test(districtCodeStr)) {
                throw new Error(`Invalid district code: ${districtCode}`);
            }

            console.log(`Loading wards for district ${districtCodeStr}`);

            const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCodeStr}?depth=2`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const district = await response.json();

            const wardSelect = document.getElementById(wardSelectId);
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                if (district.wards && district.wards.length > 0) {
                    district.wards.forEach((ward) => {
                        const option = document.createElement("option");
                        option.value = ward.code;
                        option.textContent = ward.name;
                        wardSelect.appendChild(option);
                        console.log(`Added ward option: ${ward.code} - ${ward.name}`);
                    });
                    console.log(`Loaded ${district.wards.length} wards for district ${districtCodeStr}`);
                } else {
                    console.warn(`No wards found for district ${districtCodeStr}`);
                    this.showNotification(`Không tìm thấy phường/xã cho quận ${districtCodeStr}`, 'warning');
                }
            }
        } catch (error) {
            console.error(`Error loading wards for district ${districtCode}:`, error.message);
            this.showNotification(`Không thể tải danh sách phường/xã: ${error.message}`, "error");
            const wardSelect = document.getElementById(wardSelectId);
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
            }
        }
    },

    debugDropdownOptions(selectId, expectedValue) {
        const select = document.getElementById(selectId);
        if (!select) {
            console.error(`Select ${selectId} not found`);
            return;
        }

        console.log(`=== DEBUG ${selectId} ===`);
        console.log(`Looking for value: "${expectedValue}"`);
        console.log(`Total options: ${select.options.length}`);

        Array.from(select.options).forEach((option, index) => {
            console.log(`Option ${index}: value="${option.value}", text="${option.textContent}"`);
            if (option.value === expectedValue) {
                console.log(`✅ FOUND MATCH at index ${index}`);
            }
        });

        const foundOption = select.querySelector(`option[value="${expectedValue}"]`);
        console.log(`querySelector result:`, foundOption ? "Found" : "Not found");
        console.log(`=== END DEBUG ===`);
    },

    async safeFetch(url, options = {}) {
        try {
            const response = await fetch(url, options);
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}, URL: ${url}`);
            }
            return await response.json();
        } catch (error) {
            console.error(`Error in safeFetch for ${url}:`, error.message);
            this.showNotification(`Lỗi khi gọi API: ${error.message}`, 'error');
            throw error;
        }
    },

    setupLocationListeners() {
        const prefixes = ["tenant", "owner", "room", "newCustomer"];
        prefixes.forEach((prefix) => {
            const provinceSelect = document.getElementById(`${prefix}-province`);
            const districtSelect = document.getElementById(`${prefix}-district`);
            const wardSelect = document.getElementById(`${prefix}-ward`);

            if (provinceSelect) {
                provinceSelect.addEventListener("change", () => {
                    if (districtSelect) {
                        districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
                        districtSelect.value = "";
                    }
                    if (wardSelect) {
                        wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                        wardSelect.value = "";
                    }

                    if (provinceSelect.value) {
                        this.loadDistricts(provinceSelect.value, `${prefix}-district`, `${prefix}-ward`);
                    }

                    this.updateAddress(prefix);
                });
            }

            if (districtSelect) {
                districtSelect.addEventListener("change", () => {
                    if (wardSelect) {
                        wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                        wardSelect.value = "";
                    }

                    if (districtSelect.value) {
                        this.loadWards(districtSelect.value, `${prefix}-ward`);
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

        window.scrollTo({ top: 0, behavior: "smooth" });
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
        const streetInput = document.getElementById(`${prefix}-street`);
        const provinceSelect = document.getElementById(`${prefix}-province`);
        const districtSelect = document.getElementById(`${prefix}-district`);
        const wardSelect = document.getElementById(`${prefix}-ward`);
        const addressDiv = document.getElementById(`${prefix}-address`);

        if (!addressDiv) return;

        let addressParts = [];

        if (streetInput && streetInput.value.trim()) {
            addressParts.push(streetInput.value.trim());
        }

        if (wardSelect && wardSelect.value && wardSelect.selectedIndex > 0) {
            const selectedWardText = wardSelect.options[wardSelect.selectedIndex].text;
            addressParts.push(selectedWardText);
        }

        if (districtSelect && districtSelect.value && districtSelect.selectedIndex > 0) {
            const selectedDistrictText = districtSelect.options[districtSelect.selectedIndex].text;
            addressParts.push(selectedDistrictText);
        }

        if (provinceSelect && provinceSelect.value && provinceSelect.selectedIndex > 0) {
            const selectedProvinceText = provinceSelect.options[provinceSelect.selectedIndex].text;
            addressParts.push(selectedProvinceText);
        }

        const fullAddress = addressParts.join(", ");
        addressDiv.textContent = fullAddress || "Chưa có địa chỉ";

        console.log(`Updated ${prefix} address:`, fullAddress);
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
        const form = document.querySelector('form');
        const formData = new FormData(form);
        formData.append('tenantType', document.getElementById('tenantType')?.value || 'REGISTERED');

        fetch('/api/contracts', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.showNotification('Hợp đồng đã được lưu thành công!', 'success');
                    window.location.href = '/api/contracts/list';
                } else {
                    this.showNotification(data.message || 'Lỗi khi lưu hợp đồng!', 'error');
                }
            })
            .catch(error => {
                console.error('Error saving contract:', error);
                this.showNotification('Lỗi khi lưu hợp đồng: ' + error.message, 'error');
            });
    },

    setupAmenityModal() {
        const addAmenityBtn = document.getElementById("btn-add-amenity-host");
        const saveAmenityBtn = document.getElementById("saveAmenity-host");
        const amenityForm = document.getElementById("addAmenityForm-host");
        const amenityNameInput = document.getElementById("amenityName-host");
        const amenityModalElement = document.getElementById("addAmenityModal-host");

        if (addAmenityBtn && amenityModalElement) {
            addAmenityBtn.setAttribute("data-bs-toggle", "modal");
            addAmenityBtn.setAttribute("data-bs-target", "#addAmenityModal-host");

            addAmenityBtn.addEventListener("click", () => {
                if (amenityForm) amenityForm.reset();
                if (amenityNameInput) amenityNameInput.focus();
                const modal = new bootstrap.Modal(amenityModalElement);
                modal.show();
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
            (label) => label.textContent.toLowerCase() === amenityName.toLowerCase()
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
        const customerModalElement = document.getElementById("addCustomerModal-host");

        if (addCustomerBtn && customerModalElement) {
            addCustomerBtn.addEventListener("click", () => {
                const modal = new bootstrap.Modal(customerModalElement);
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

    saveNewCustomer() {
        const formData = new FormData();
        formData.append("name", document.getElementById("newCustomer-name").value || "");
        formData.append("dob", document.getElementById("newCustomer-dob").value || "");
        formData.append("id", document.getElementById("newCustomer-id").value || "");
        formData.append("id-date", document.getElementById("newCustomer-id-date").value || "");
        formData.append("id-place", document.getElementById("newCustomer-id-place").value || "");
        formData.append("phone", document.getElementById("newCustomer-phone").value || "");
        formData.append("email", document.getElementById("newCustomer-email").value || "");
        formData.append("street", document.getElementById("newCustomer-street").value || "");
        formData.append("ward", this.getSelectText("newCustomer-ward") || "");
        formData.append("district", this.getSelectText("newCustomer-district") || "");
        formData.append("province", this.getSelectText("newCustomer-province") || "");
        formData.append("cccd-front", document.getElementById("newCustomer-cccd-front").files[0] || null);
        formData.append("cccd-back", document.getElementById("newCustomer-cccd-back").files[0] || null);

        fetch('/api/contracts/add-unregistered-tenant', {
            method: 'POST',
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    document.getElementById("unregisteredTenantFullName").value = data.tenant.fullName || "";
                    document.getElementById("unregisteredTenantPhone").value = data.tenant.phone || "";
                    document.getElementById("unregisteredTenantCccdNumber").value = data.tenant.cccdNumber || "";
                    document.getElementById("unregisteredTenantBirthday").value = data.tenant.birthday || "";
                    document.getElementById("unregisteredTenantIssueDate").value = data.tenant.issueDate || "";
                    document.getElementById("unregisteredTenantIssuePlace").value = data.tenant.issuePlace || "";
                    document.getElementById("unregisteredTenantStreet").value = data.tenant.street || "";
                    document.getElementById("unregisteredTenantProvince").value = data.tenant.province || "";
                    this.loadDistricts(data.tenant.province, "unregisteredTenantDistrict", "unregisteredTenantWard");
                    setTimeout(() => {
                        document.getElementById("unregisteredTenantDistrict").value = data.tenant.district || "";
                        this.loadWards(data.tenant.district, "unregisteredTenantWard");
                        setTimeout(() => {
                            document.getElementById("unregisteredTenantWard").value = data.tenant.ward || "";
                            this.updateAddress("tenant");
                        }, 200);
                    }, 200);

                    const modal = bootstrap.Modal.getInstance(document.getElementById("addCustomerModal-host"));
                    modal.hide();
                    document.getElementById("tenantType").value = "UNREGISTERED";
                    this.toggleTenantFields();
                    this.showNotification("Đã thêm thông tin người thuê thành công!", "success");
                } else {
                    this.showNotification(data.message || "Lỗi khi thêm người thuê!", "error");
                }
            })
            .catch(error => {
                console.error("Error saving unregistered tenant:", error);
                this.showNotification("Lỗi khi thêm người thuê: " + error.message, "error");
            });
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

    setupPreviewListeners() {
        const inputs = document.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('change', () => {
                const previewId = input.dataset.preview;
                if (previewId) {
                    this.updatePreviewField(input.id, previewId);
                }
            });
        });
    }
};

document.addEventListener("DOMContentLoaded", () => {
    window.NhaTroContract.init();
});
/* ]]> */