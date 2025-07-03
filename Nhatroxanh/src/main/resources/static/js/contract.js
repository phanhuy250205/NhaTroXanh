/* <![CDATA[ */
window.NhaTroContract = {
    currentTab: "tenantInfo",
    zoomLevel: 1,

    init() {
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
        return this.loadProvinces().then(() => {
            console.log("Provinces loaded");
            const contract = /*[[${contract}]]*/ null;
            if (contract && contract.owner) {
                document.getElementById("owner-name").value = contract.owner.fullName || "";
                if (contract.owner.province) {
                    this.loadDistricts(contract.owner.province, "owner-district", "owner-ward");
                    document.getElementById("owner-province").value = contract.owner.province;
                    setTimeout(() => {
                        if (contract.owner.district) {
                            document.getElementById("owner-district").value = contract.owner.district;
                            this.loadWards(contract.owner.district, "owner-ward");
                            setTimeout(() => {
                                document.getElementById("owner-ward").value = contract.owner.ward || "";
                                this.updateAddress("owner");
                            }, 200);
                        }
                    }, 200);
                }
            }
            const hostelSelect = document.getElementById('hostelId');
            if (hostelSelect && hostelSelect.value) {
                this.filterRooms();
            }
        }).catch(error => {
            console.error("Error loading provinces:", error);
            this.showNotification("Lỗi khi tải danh sách tỉnh/thành phố", "error");
        });
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
        return name
            .normalize("NFD") // Phân tách ký tự có dấu
            .replace(/[\u0300-\u036f]/g, "") // Loại bỏ dấu
            .replace(/^(Tỉnh|TP\.|Thành phố|Quận|Phường|Huyện|Xã)\s*/i, "")
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

    // Hàm xử lý khu trọ và phòng trọ
    filterRooms() {
        const hostelId = document.getElementById('hostelId').value;
        const roomSelect = document.getElementById('roomId');

        if (!roomSelect) {
            console.error('Room select element not found');
            this.showNotification('Không tìm thấy dropdown phòng trọ!', 'error');
            return;
        }

        roomSelect.disabled = true;
        roomSelect.innerHTML = '<option value="">Đang tải...</option>';
        roomSelect.classList.add('loading-spinner');

        if (!hostelId) {
            roomSelect.classList.remove('loading-spinner');
            roomSelect.innerHTML = '<option value="">-- Chọn phòng trọ --</option>';
            this.clearRoomFields();
            return;
        }

        console.log('Fetching rooms for hostelId:', hostelId);

        fetch(`/api/contracts/get-rooms-by-hostel?hostelId=${hostelId}`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest',
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || "",
                'Authorization': 'Bearer ' + localStorage.getItem('authToken') || ""
            }
        })
            .then(response => {
                roomSelect.classList.remove('loading-spinner');
                if (!response.ok) {
                    return response.json().then(data => {
                        throw new Error(data.message || `HTTP error! status: ${response.status}`);
                    });
                }
                return response.json();
            })
            .then(data => {
                console.log('Rooms data received:', data); // Debug
                roomSelect.disabled = false;
                roomSelect.innerHTML = '<option value="">-- Chọn phòng trọ --</option>';

                if (data.success && data.rooms && data.rooms.length > 0) {
                    data.rooms.forEach(room => {
                        const option = document.createElement('option');
                        option.value = room.roomId;
                        // Xử lý cả roomName và namerooms
                        const roomName = room.roomName || room.namerooms || 'Phòng không tên';
                        const address = room.address || 'Không có địa chỉ';
                        option.textContent = `${roomName} (${address})`;
                        option.dataset.price = room.price;
                        option.dataset.acreage = room.acreage;
                        option.dataset.maxTenants = room.maxTenants;
                        roomSelect.appendChild(option);
                    });
                    this.showNotification(`Đã tải ${data.rooms.length} phòng trọ khả dụng`, 'success');
                } else {
                    roomSelect.innerHTML = '<option value="" disabled>' + (data.message || 'Không có phòng trọ khả dụng') + '</option>';
                    this.showNotification(data.message || 'Không có phòng trọ khả dụng', 'warning');
                }
                this.clearRoomFields();
            })
            .catch(error => {
                console.error('Error loading rooms:', error);
                roomSelect.classList.remove('loading-spinner');
                roomSelect.disabled = false;
                roomSelect.innerHTML = '<option value="" disabled>Lỗi khi tải danh sách phòng: ' + error.message + '</option>';
                this.showNotification('Lỗi khi tải danh sách phòng: ' + error.message, 'error');
                this.clearRoomFields();
            });
    },

    async onRoomSelected() {
        const roomSelect = document.getElementById('roomId');
        if (!roomSelect) {
            this.showNotification('Không tìm thấy dropdown phòng trọ!', 'error');
            return;
        }

        const selectedOption = roomSelect.options[roomSelect.selectedIndex];
        const roomId = roomSelect.value;
        if (!roomId) {
            this.clearRoomFields();
            return;
        }

        console.log('Fetching room details for roomId:', roomId);

        try {
            const response = await fetch(`/api/contracts/get-room-details?roomId=${roomId}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || "",
                    'Authorization': 'Bearer ' + localStorage.getItem('authToken') || ""
                }
            });
            const data = await response.json();
            console.log('Room details received:', data);

            if (data.success && data.room) {
                const room = data.room;
                document.getElementById('room-number').value = room.namerooms || selectedOption.text.split(' (')[0] || '';
                document.getElementById('room-area').value = room.acreage || '';
                document.getElementById('rent-price').value = room.price || '';

                // Tách địa chỉ từ text của option nếu API không cung cấp
                let address = room.address;
                if (!address && selectedOption.text.includes('(')) {
                    address = selectedOption.text.split(' (')[1].replace(')', '');
                }
                console.log('Processed address:', address);

                if (address) {
                    const addressParts = address.split(', ');
                    const street = addressParts.length > 0 ? addressParts[0].trim() : '';
                    const ward = addressParts.length > 1 ? addressParts[1].trim() : '';
                    const district = addressParts.length > 2 ? addressParts[2].trim() : '';
                    const province = addressParts.length > 3 ? addressParts[3].trim() : '';
                    console.log('Address parts:', { street, ward, district, province });

                    // Điền vào ô Địa chỉ (street)
                    document.getElementById('room-street').value = street;

                    // Điền vào dropdown Tỉnh/Thành phố
                    const provinceSelect = document.getElementById('room-province');
                    if (provinceSelect) {
                        const provinceCode = await this.mapProvinceNameToCode(province);
                        console.log('Mapped province code:', provinceCode);
                        if (provinceCode) {
                            const provinceOption = provinceSelect.querySelector(`option[value="${provinceCode}"]`);
                            console.log('Province option found:', provinceOption);
                            if (provinceOption) {
                                provinceSelect.value = provinceCode;
                                console.log('Province set to:', provinceCode);
                                await this.loadDistricts(provinceCode, 'room-district', 'room-ward');
                            } else {
                                provinceSelect.value = ''; // Reset nếu không tìm thấy
                                this.showNotification(`Không tìm thấy option cho mã tỉnh/thành phố ${provinceCode} (${province})`, 'warning');
                            }
                        } else {
                            provinceSelect.value = province; // Fallback với tên
                            this.showNotification(`Không ánh xạ được mã cho ${province}`, 'warning');
                        }
                    }

                    // Điền vào dropdown Quận/Huyện
                    const districtSelect = document.getElementById('room-district');
                    if (districtSelect && provinceSelect && provinceSelect.value) {
                        const districtCode = await this.mapDistrictNameToCode(provinceSelect.value, district);
                        console.log('Mapped district code:', districtCode);
                        if (districtCode) {
                            const districtOption = districtSelect.querySelector(`option[value="${districtCode}"]`);
                            console.log('District option found:', districtOption);
                            if (districtOption) {
                                districtSelect.value = districtCode;
                                console.log('District set to:', districtCode);
                                await this.loadWards(districtCode, 'room-ward', provinceSelect.value); // Truyền provinceCode
                            } else {
                                districtSelect.value = ''; // Reset nếu không tìm thấy
                                this.showNotification(`Không tìm thấy option cho mã quận/huyện ${districtCode} (${district})`, 'warning');
                            }
                        } else {
                            districtSelect.value = district; // Fallback với tên
                            this.showNotification(`Không ánh xạ được mã cho ${district}`, 'warning');
                        }
                    }

                    // Điền vào dropdown Phường/Xã
                    const wardSelect = document.getElementById('room-ward');
                    if (wardSelect && districtSelect && districtSelect.value) {
                        const wardCode = await this.mapWardNameToCode(districtSelect.value, ward, provinceSelect.value);
                        console.log('Mapped ward code:', wardCode);
                        if (wardCode) {
                            const wardOption = wardSelect.querySelector(`option[value="${wardCode}"]`);
                            console.log('Ward option found:', wardOption);
                            if (wardOption) {
                                wardSelect.value = wardCode;
                                console.log('Ward set to:', wardCode);
                            } else {
                                // Fallback: Thêm option mới với tên "Hòa Thuận" nếu không tìm thấy
                                const newOption = document.createElement("option");
                                newOption.value = ward; // Sử dụng tên làm value
                                newOption.textContent = ward; // Hiển thị "Hòa Thuận"
                                wardSelect.appendChild(newOption);
                                wardSelect.value = ward;
                                console.log(`Added fallback ward: ${ward}`);
                                this.showNotification(`Không tìm thấy mã cho ${ward}, sử dụng tên trực tiếp`, 'warning');
                            }
                        } else {
                            // Fallback: Thêm option mới với tên "Hòa Thuận" nếu không ánh xạ được
                            const newOption = document.createElement("option");
                            newOption.value = ward; // Sử dụng tên làm value
                            newOption.textContent = ward; // Hiển thị "Hòa Thuận"
                            wardSelect.appendChild(newOption);
                            wardSelect.value = ward;
                            console.log(`Added fallback ward: ${ward}`);
                            this.showNotification(`Không ánh xạ được mã cho ${ward}, sử dụng tên trực tiếp`, 'warning');
                        }
                    }
                } else {
                    this.showNotification('Không tìm thấy địa chỉ để điền!', 'warning');
                }

                this.updatePreviewField('room-number', 'preview-room-number');
                this.updatePreviewField('room-area', 'preview-room-area');
                this.updatePreviewField('rent-price', 'preview-rent');
                this.updateAddress('room');
                this.calculateDeposit();

                this.showNotification(
                    `Đã chọn ${room.namerooms || selectedOption.text.split(' (')[0]} - Diện tích: ${room.acreage || ''}m² - Giá: ${new Intl.NumberFormat('vi-VN').format(room.price || 0)} VNĐ/tháng`,
                    'success'
                );
            } else {
                this.showNotification(data.message || 'Không thể lấy thông tin phòng!', 'error');
                this.clearRoomFields();
            }
        } catch (error) {
            console.error('Error fetching room details:', error);
            this.showNotification('Lỗi khi lấy thông tin phòng: ' + error.message, 'error');
            this.clearRoomFields();
        }
    },

    clearRoomFields() {
        document.getElementById('room-number').value = '';
        document.getElementById('room-area').value = '';
        document.getElementById('rent-price').value = '';
        document.getElementById('room-province').value = '';
        document.getElementById('room-district').innerHTML = '<option value="">Quận/Huyện</option>';
        document.getElementById('room-ward').innerHTML = '<option value="">Phường/Xã</option>';
        document.getElementById('room-street').value = '';
        this.updatePreviewField('room-number', 'preview-room-number');
        this.updatePreviewField('room-area', 'preview-room-area');
        this.updatePreviewField('rent-price', 'preview-rent');
        this.updateAddress('room');
        this.calculateDeposit();
    },

    toggleTenantFields() {
        const tenantType = document.getElementById('tenantType').value;
        const registeredFields = document.getElementById('registeredTenantFields');
        const unregisteredFields = document.getElementById('unregisteredTenantFields');
        registeredFields.style.display = tenantType === 'REGISTERED' ? 'block' : 'none';
        unregisteredFields.style.display = tenantType === 'UNREGISTERED' ? 'block' : 'none';
    },

    // Hàm lấy thông tin người thuê qua số điện thoại
    async fetchTenantByPhone(phone) {
        try {
            const response = await fetch(`/api/contracts/get-tenant-by-phone?phone=${encodeURIComponent(phone)}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-CSRF-TOKEN": document.querySelector('meta[name="_csrf"]')?.content || ""
                }
            });
            const data = await response.json();
            console.log("API response:", data);
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

    // 4. SỬA HÀM FILL TENANT FIELDS - thêm debug và đảm bảo load provinces trước
    async fillTenantFields(tenant) {
        console.log("Filling tenant fields with data:", tenant);

        // THÊM: Đảm bảo provinces đã được load
        await this.loadProvinces();

        document.getElementById("tenant-name").value = tenant.fullName || "";
        document.getElementById("tenant-dob").value = tenant.birthday || "";
        document.getElementById("tenant-id").value = tenant.cccdNumber || "";
        document.getElementById("tenant-id-date").value = tenant.issueDate || "";
        document.getElementById("tenant-id-place").value = tenant.issuePlace || "";
        document.getElementById("tenant-email").value = tenant.email || "";
        document.getElementById("tenant-street").value = tenant.street || "";

        const frontPreview = document.getElementById("cccd-front-preview");
        const backPreview = document.getElementById("cccd-back-preview");
        if (tenant.cccdFrontUrl) frontPreview.innerHTML = `<img src="${tenant.cccdFrontUrl}" alt="CCCD Front" style="max-width: 100%;">`;
        if (tenant.cccdBackUrl) backPreview.innerHTML = `<img src="${tenant.cccdBackUrl}" alt="CCCD Back" style="max-width: 100%;">`;

        const provinceSelect = document.getElementById("tenant-province");
        const districtSelect = document.getElementById("tenant-district");
        const wardSelect = document.getElementById("tenant-ward");

        let provinceCode = null;

        if (tenant.province && provinceSelect) {
            console.log("Attempting to map province:", tenant.province);
            provinceCode = await this.mapProvinceNameToCode(tenant.province);
            console.log("Province code:", provinceCode);

            // THÊM DEBUG
            this.debugDropdownOptions("tenant-province", provinceCode);

            if (provinceCode && provinceSelect.querySelector(`option[value="${provinceCode}"]`)) {
                provinceSelect.value = provinceCode;
                await this.loadDistricts(provinceCode, "tenant-district", "tenant-ward");
            } else {
                console.warn("Province code not found in options, using raw value:", tenant.province);
                // THÊM: Tạo option mới với code thay vì name
                if (!provinceSelect.querySelector(`option[value="${provinceCode}"]`)) {
                    const option = document.createElement("option");
                    option.value = provinceCode || tenant.province;
                    option.textContent = tenant.province;
                    provinceSelect.appendChild(option);
                }
                provinceSelect.value = provinceCode || tenant.province;
                this.showNotification(`Không tìm thấy mã tỉnh/thành phố cho ${tenant.province}, sử dụng tên trực tiếp`, "warning");
            }

            let districtCode = null;

            if (tenant.district && districtSelect) {
                console.log("Attempting to map district:", tenant.district);
                districtCode = await this.mapDistrictNameToCode(provinceCode || tenant.province, tenant.district);
                console.log("District code:", districtCode);

                // THÊM DEBUG
                this.debugDropdownOptions("tenant-district", districtCode);

                if (districtCode && districtSelect.querySelector(`option[value="${districtCode}"]`)) {
                    districtSelect.value = districtCode;
                    await this.loadWards(districtCode, "tenant-ward");
                } else {
                    console.warn("District not mapped, using raw value:", tenant.district);
                    if (!districtSelect.querySelector(`option[value="${districtCode}"]`)) {
                        const option = document.createElement("option");
                        option.value = districtCode || tenant.district;
                        option.textContent = tenant.district;
                        districtSelect.appendChild(option);
                    }
                    districtSelect.value = districtCode || tenant.district;
                    this.showNotification(`Không tìm thấy mã quận/huyện cho ${tenant.district}, sử dụng tên trực tiếp`, "warning");
                }

                if (tenant.ward && wardSelect && districtCode && provinceCode) {
                    console.log("Attempting to map ward:", tenant.ward);
                    const wardCode = await this.mapWardNameToCode(districtCode, tenant.ward, provinceCode);
                    console.log("Ward code:", wardCode);

                    if (wardCode && wardSelect.querySelector(`option[value="${wardCode}"]`)) {
                        wardSelect.value = wardCode;
                    } else {
                        console.warn("Ward not mapped, using raw value:", tenant.ward);
                        if (!wardSelect.querySelector(`option[value="${tenant.ward}"]`)) {
                            const option = document.createElement("option");
                            option.value = tenant.ward;
                            option.textContent = tenant.ward;
                            wardSelect.appendChild(option);
                        }
                        wardSelect.value = tenant.ward;
                        this.showNotification(`Không tìm thấy mã phường/xã cho ${tenant.ward}, sử dụng tên trực tiếp`, "warning");
                    }
                } else if (tenant.ward && wardSelect) {
                    console.warn("Ward not mapped due to missing district or province code, using raw value:", tenant.ward);
                    if (!wardSelect.querySelector(`option[value="${tenant.ward}"]`)) {
                        const option = document.createElement("option");
                        option.value = tenant.ward;
                        option.textContent = tenant.ward;
                        wardSelect.appendChild(option);
                    }
                    wardSelect.value = tenant.ward;
                    this.showNotification(`Không thể tải danh sách phường/xã do mã quận hoặc mã tỉnh không hợp lệ`, "warning");
                }
            }
        }
        this.updateAddress("tenant");
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
        this.updateAllPreview();
    },

    async mapProvinceNameToCode(provinceName) {
        try {
            const response = await fetch("/api/proxy/provinces"); // Sử dụng proxy
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinces = await response.json();
            console.log("Danh sách tỉnh từ API:", provinces.map(p => ({ name: p.name, code: p.code })));
            const normalizedProvinceName = this.normalizeName(provinceName);
            console.log("Normalized province name:", normalizedProvinceName);
            const province = provinces.find(p => {
                const normalizedApiName = this.normalizeName(p.name);
                return normalizedApiName === normalizedProvinceName ||
                    normalizedApiName.includes(normalizedProvinceName) ||
                    normalizedProvinceName.includes(normalizedApiName);
            });
            if (!province) {
                console.warn(`No match for province: ${provinceName}, checked variants:`, provinces.map(p => this.normalizeName(p.name)));
                return null;
            }
            const provinceCode = String(province.code).padStart(2, '0'); // Chuyển thành chuỗi và pad
            console.log(`Found province: ${provinceName} -> Code: ${provinceCode}`);
            return provinceCode;
        } catch (error) {
            console.error("Mapping error:", error);
            return null;
        }
    },

    async mapDistrictNameToCode(provinceCode, districtName) {
        try {
            // Validate provinceCode
            const provinceCodeStr = String(provinceCode).padStart(2, '0');
            if (!/^\d{2}$/.test(provinceCodeStr)) {
                console.warn("Invalid province code:", provinceCode);
                return null;
            }

            const response = await fetch(`/api/proxy/provinces/${provinceCodeStr}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

            const province = await response.json();
            console.log(`Districts fetched for province ${provinceCodeStr}:`, province.districts.map(d => d.name));

            let normalizedDistrictName = this.normalizeName(districtName);
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
                    variant.includes(this.normalizeName(d.name))
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
            return null;
        }
    },

    // SỬA LỖI: Thêm tham số provinceCode vào hàm mapWardNameToCode
    async mapWardNameToCode(districtCode, wardName, provinceCode) {
        try {
            // Chuyển districtCode thành string và validate
            const districtCodeStr = String(districtCode).trim();

            if (!districtCodeStr || districtCodeStr === 'null' || districtCodeStr === 'undefined') {
                console.warn(`Invalid district code: ${districtCode}`);
                return null;
            }

            // SỬA LỖI: Sử dụng provinceCode được truyền vào thay vì lấy từ districtCode
            const provinceCodeStr = String(provinceCode).padStart(2, '0');

            if (!provinceCodeStr || !/^\d{2}$/.test(provinceCodeStr)) {
                console.warn(`Invalid province code: ${provinceCode}`);
                return null;
            }

            console.log(`Mapping ward: ${wardName} for district ${districtCodeStr} in province ${provinceCodeStr}`);

            // Gọi API để lấy thông tin tỉnh
            const response = await fetch(`/api/proxy/provinces/${this.safeEncodeURL(provinceCodeStr)}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}, URL: /api/proxy/provinces/${provinceCodeStr}`);
            }

            const province = await response.json();

            // Tìm quận/huyện theo code
            const district = province.districts.find(d => String(d.code) === districtCodeStr);

            if (!district) {
                console.warn(`District with code ${districtCodeStr} not found in province ${provinceCodeStr}`);
                return null;
            }

            console.log(`Wards fetched for district ${districtCodeStr}:`, district.wards.map(w => w.name));

            // Chuẩn hóa tên phường/xã
            const normalizedWardName = this.normalizeName(wardName);

            // Tạo các biến thể tên phường/xã
            const wardVariants = new Set([
                normalizedWardName,
                `phường ${normalizedWardName}`,
                `xã ${normalizedWardName}`,
                `thị trấn ${normalizedWardName}`,
                normalizedWardName.replace(/^(phường|xã|thị trấn)\s+/i, '').trim()
            ]);

            const cleanVariants = Array.from(wardVariants).filter(v => v && v.length > 0);
            console.log(`Ward variants for "${wardName}":`, cleanVariants);

            // Tìm phường/xã
            const ward = district.wards.find(w => {
                const normalizedApiName = this.normalizeName(w.name);
                return cleanVariants.some(variant => {
                    if (normalizedApiName === variant) return true;
                    const apiNameWithoutPrefix = normalizedApiName.replace(/^(phường|xã|thị trấn)\s+/i, '').trim();
                    if (apiNameWithoutPrefix === variant) return true;
                    if (normalizedApiName.includes(variant) && variant.length > 3) return true;
                    if (variant.includes(normalizedApiName) && normalizedApiName.length > 3) return true;
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
            const response = await fetch("/api/proxy/provinces");
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinces = await response.json();
            console.log("Loaded provinces:", provinces.map(p => ({ code: p.code, name: p.name })));

            const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"];
            selects.forEach((selectId) => {
                const select = document.getElementById(selectId);
                if (!select) {
                    console.warn(`Select element with ID ${selectId} not found in DOM`);
                    this.showNotification(`Không tìm thấy dropdown ${selectId}`, "error");
                    return;
                }
                // Xóa nội dung hiện tại và thêm tùy chọn mặc định
                select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>';
                // Thêm các tỉnh từ API
                provinces.forEach((province) => {
                    const option = document.createElement("option");
                    // SỬA: Đảm bảo format code giống nhau
                    const provinceCode = String(province.code).padStart(2, '0');
                    option.value = provinceCode;
                    option.textContent = province.name;
                    select.appendChild(option);

                    // DEBUG: Log để kiểm tra
                    if (selectId === "tenant-province") {
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

    // SỬA LỖI: Cải thiện hàm loadDistricts để đảm bảo reset hoàn toàn
    async loadDistricts(provinceCode, districtSelectId, wardSelectId) {
        try {
            const provinceCodeStr = String(provinceCode).padStart(2, '0');
            if (!/^\d{2}$/.test(provinceCodeStr)) {
                throw new Error(`Invalid province code: ${provinceCode}`);
            }

            const response = await fetch(`/api/proxy/provinces/${this.safeEncodeURL(provinceCodeStr)}`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const province = await response.json();
            console.log(`Loaded districts for province ${provinceCodeStr}:`, province.districts.map(d => ({ code: d.code, name: d.name })));

            const districtSelect = document.getElementById(districtSelectId);
            const wardSelect = document.getElementById(wardSelectId);

            if (districtSelect) {
                // QUAN TRỌNG: Xóa hoàn toàn và reset
                districtSelect.innerHTML = '';
                const defaultOption = document.createElement("option");
                defaultOption.value = "";
                defaultOption.textContent = "Chọn Quận/Huyện";
                districtSelect.appendChild(defaultOption);

                // Thêm các quận/huyện mới
                province.districts.forEach((district) => {
                    const option = document.createElement("option");
                    option.value = String(district.code);
                    option.textContent = district.name;
                    districtSelect.appendChild(option);

                    if (districtSelectId === "tenant-district") {
                        console.log(`Added district option: ${district.code} - ${district.name}`);
                    }
                });

                // Đảm bảo giá trị được reset
                districtSelect.value = "";
            }

            if (wardSelect) {
                // QUAN TRỌNG: Xóa hoàn toàn và reset wards
                wardSelect.innerHTML = '';
                const defaultOption = document.createElement("option");
                defaultOption.value = "";
                defaultOption.textContent = "Chọn Phường/Xã";
                wardSelect.appendChild(defaultOption);
                wardSelect.value = "";
            }
        } catch (error) {
            console.error(`Error loading districts for province ${provinceCode}:`, error);
            this.showNotification("Không thể tải danh sách quận/huyện", "error");
        }
    },



    // 3. THÊM HÀM DEBUG KIỂM TRA DROPDOWN
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
    async loadWards(districtCode, wardSelectId, provinceCode) {
        try {
            // Chuyển districtCode và provinceCode thành string và validate
            const districtCodeStr = String(districtCode).trim();
            const provinceCodeStr = String(provinceCode).padStart(2, '0');

            if (!districtCodeStr || districtCodeStr === 'null' || districtCodeStr === 'undefined' || !/^\d+$/.test(districtCodeStr)) {
                throw new Error(`Invalid district code: ${districtCode}`);
            }
            if (!provinceCodeStr || !/^\d{2}$/.test(provinceCodeStr)) {
                throw new Error(`Invalid province code: ${provinceCode}`);
            }

            console.log(`Loading wards for district ${districtCodeStr} in province ${provinceCodeStr}`);

            // Gọi API để lấy thông tin tỉnh
            const response = await fetch(`/api/proxy/provinces/${this.safeEncodeURL(provinceCodeStr)}`);
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}, URL: /api/proxy/provinces/${provinceCodeStr}`);
            }

            const province = await response.json();

            // Tìm quận/huyện theo code
            const district = province.districts.find(d => String(d.code) === districtCodeStr);

            if (!district) {
                throw new Error(`District with code ${districtCodeStr} not found in province ${provinceCodeStr}`);
            }

            // Cập nhật dropdown phường/xã
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

    // SỬA LỖI: Thêm hàm reset districts và wards khi chọn tỉnh mới
    setupLocationListeners() {
        const prefixes = ["tenant", "owner", "room", "newCustomer"];
        prefixes.forEach((prefix) => {
            const provinceSelect = document.getElementById(`${prefix}-province`);
            const districtSelect = document.getElementById(`${prefix}-district`);
            const wardSelect = document.getElementById(`${prefix}-ward`);

            if (provinceSelect) {
                provinceSelect.addEventListener("change", () => {
                    // QUAN TRỌNG: Reset districts và wards trước khi load mới
                    if (districtSelect) {
                        districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
                        districtSelect.value = ""; // Reset giá trị
                    }
                    if (wardSelect) {
                        wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                        wardSelect.value = ""; // Reset giá trị
                    }

                    if (provinceSelect.value) {
                        this.loadDistricts(provinceSelect.value, `${prefix}-district`, `${prefix}-ward`);
                    }

                    // Cập nhật địa chỉ sau khi reset
                    this.updateAddress(prefix);
                });
            }

            if (districtSelect) {
                districtSelect.addEventListener("change", () => {
                    // QUAN TRỌNG: Reset wards trước khi load mới
                    if (wardSelect) {
                        wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                        wardSelect.value = ""; // Reset giá trị
                    }

                    if (districtSelect.value) {
                        this.loadWards(districtSelect.value, `${prefix}-ward`);
                    }

                    // Cập nhật địa chỉ sau khi reset
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

    // SỬA LỖI: Cải thiện hàm updateAddress để lấy text thay vì value
    updateAddress(prefix) {
        const streetInput = document.getElementById(`${prefix}-street`);
        const provinceSelect = document.getElementById(`${prefix}-province`);
        const districtSelect = document.getElementById(`${prefix}-district`);
        const wardSelect = document.getElementById(`${prefix}-ward`);
        const addressDiv = document.getElementById(`${prefix}-address`);

        if (!addressDiv) return;

        let addressParts = [];

        // Lấy tên đường/số nhà
        if (streetInput && streetInput.value.trim()) {
            addressParts.push(streetInput.value.trim());
        }

        // QUAN TRỌNG: Lấy TEXT (tên hiển thị) thay vì VALUE (mã code)
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

        // Tạo địa chỉ đầy đủ
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
    }
};

// Khởi tạo khi DOM được tải
document.addEventListener("DOMContentLoaded", () => {
    window.NhaTroContract.init();
});
/* ]]> */