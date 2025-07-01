/* <![CDATA[ */
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

        // Điền thông tin chủ trọ từ backend
        const contract = /*[[${contract}]]*/ null;
        if (contract && contract.owner) {
            document.getElementById("owner-name").value = contract.owner.fullName || "";
            document.getElementById("owner-dob").value = contract.owner.birthday ? new Date(contract.owner.birthday).toISOString().split('T')[0] : "";
            document.getElementById("owner-id").value = contract.owner.cccdNumber || "";
            document.getElementById("owner-phone").value = contract.owner.phone || "";
            document.getElementById("owner-email").value = contract.owner.email || "";
            document.getElementById("owner-street").value = contract.owner.street || "";
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

        // Tải phòng nếu khu trọ được chọn mặc định
        const hostelSelect = document.getElementById('hostelId');
        if (hostelSelect && hostelSelect.value) {
            this.filterRooms();
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
        const roomId = document.getElementById('roomId').value;
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
                document.getElementById('room-number').value = room.roomName || room.namerooms || '';
                document.getElementById('room-area').value = room.area || '';
                document.getElementById('rent-price').value = room.price || '';
                document.getElementById('room-street').value = room.street || '';

                // Điền địa chỉ vào dropdown với fallback
                if (room.province) {
                    const provinceCode = await this.mapProvinceNameToCode(room.province);
                    const provinceSelect = document.getElementById('room-province');
                    if (provinceCode) {
                        provinceSelect.value = provinceCode;
                        await this.loadDistricts(provinceCode, 'room-district', 'room-ward');
                        if (room.district) {
                            const districtCode = await this.mapDistrictNameToCode(provinceCode, room.district);
                            const districtSelect = document.getElementById('room-district');
                            if (districtCode) {
                                districtSelect.value = districtCode;
                                await this.loadWards(districtCode, 'room-ward');
                                if (room.ward) {
                                    const wardCode = await this.mapWardNameToCode(districtCode, room.ward);
                                    const wardSelect = document.getElementById('room-ward');
                                    if (wardCode) {
                                        wardSelect.value = wardCode;
                                    } else {
                                        console.warn('Ward code not found for:', room.ward);
                                        // Fallback: thêm tùy chọn mới với tên phường
                                        const option = document.createElement('option');
                                        option.value = room.ward;
                                        option.textContent = room.ward;
                                        wardSelect.appendChild(option);
                                        wardSelect.value = room.ward;
                                        this.showNotification(`Không tìm thấy mã phường/xã cho ${room.ward}, sử dụng tên trực tiếp`, 'warning');
                                    }
                                }
                            } else {
                                console.warn('District code not found for:', room.district);
                                // Fallback: thêm tùy chọn mới với tên quận
                                const option = document.createElement('option');
                                option.value = room.district;
                                option.textContent = room.district;
                                districtSelect.appendChild(option);
                                districtSelect.value = room.district;
                                this.showNotification(`Không tìm thấy mã quận/huyện cho ${room.district}, sử dụng tên trực tiếp`, 'warning');
                            }
                        }
                    } else {
                        console.warn('Province code not found for:', room.province);
                        // Fallback: thêm tùy chọn mới với tên tỉnh
                        const option = document.createElement('option');
                        option.value = room.province;
                        option.textContent = room.province;
                        provinceSelect.appendChild(option);
                        provinceSelect.value = room.province;
                        this.showNotification(`Không tìm thấy mã tỉnh/thành phố cho ${room.province}, sử dụng tên trực tiếp`, 'warning');
                    }
                }

                this.updatePreviewField('room-number', 'preview-room-number');
                this.updatePreviewField('room-area', 'preview-room-area');
                this.updatePreviewField('rent-price', 'preview-rent');
                this.updateAddress('room');
                this.calculateDeposit();

                this.showNotification(
                    `Đã chọn ${room.roomName || room.namerooms} - Diện tích: ${room.area}m² - Giá: ${new Intl.NumberFormat('vi-VN').format(room.price)} VNĐ/tháng`,
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

    // Hàm điền thông tin người thuê
    async fillTenantFields(tenant) {
        console.log("Filling tenant fields with data:", tenant);
        document.getElementById("tenant-name").value = tenant.fullName || "";
        document.getElementById("tenant-dob").value = tenant.birthday || "";
        document.getElementById("tenant-id").value = tenant.cccdNumber || "";
        document.getElementById("tenant-id-date").value = tenant.issueDate || "";
        document.getElementById("tenant-id-place").value = tenant.issuePlace || "";
        document.getElementById("tenant-email").value = tenant.email || "";
        document.getElementById("tenant-street").value = tenant.street || "";
        const frontPreview = document.getElementById("cccd-front-preview");
        const backPreview = document.getElementById("cccd-back-preview");
        if (tenant.cccdFrontUrl) {
            frontPreview.innerHTML = `<img src="${tenant.cccdFrontUrl}" alt="CCCD Front" style="max-width: 100%;">`;
        }
        if (tenant.cccdBackUrl) {
            backPreview.innerHTML = `<img src="${tenant.cccdBackUrl}" alt="CCCD Back" style="max-width: 100%;">`;
        }

        const provinceSelect = document.getElementById("tenant-province");
        const districtSelect = document.getElementById("tenant-district");
        const wardSelect = document.getElementById("tenant-ward");

        if (tenant.province && provinceSelect) {
            console.log("Attempting to map province:", tenant.province);
            const provinceCode = await this.mapProvinceNameToCode(tenant.province);
            console.log("Province code:", provinceCode);
            if (provinceCode) {
                provinceSelect.value = provinceCode;
                await this.loadDistricts(provinceCode, "tenant-district", "tenant-ward");
                if (tenant.district && districtSelect) {
                    console.log("Attempting to map district:", tenant.district);
                    const districtCode = await this.mapDistrictNameToCode(provinceCode, tenant.district);
                    console.log("District code:", districtCode);
                    if (districtCode) {
                        districtSelect.value = districtCode;
                        await this.loadWards(districtCode, "tenant-ward");
                        if (tenant.ward && wardSelect) {
                            console.log("Attempting to map ward:", tenant.ward);
                            const wardCode = await this.mapWardNameToCode(districtCode, tenant.ward);
                            console.log("Ward code:", wardCode);
                            if (wardCode) {
                                wardSelect.value = wardCode;
                            } else {
                                console.warn("Ward not mapped, using raw value:", tenant.ward);
                                wardSelect.innerHTML = `<option value="${tenant.ward}">${tenant.ward}</option>`;
                                wardSelect.value = tenant.ward;
                            }
                        }
                    } else {
                        console.warn("District not mapped, using raw value:", tenant.district);
                        districtSelect.innerHTML = `<option value="${tenant.district}">${tenant.district}</option>`;
                        districtSelect.value = tenant.district;
                    }
                }
            } else {
                console.warn("Province not mapped, using raw value:", tenant.province);
                provinceSelect.innerHTML = `<option value="${tenant.province}">${tenant.province}</option>`;
                provinceSelect.value = tenant.province;
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
            const response = await fetch("https://provinces.open-api.vn/api/p/");
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinces = await response.json();
            console.log("Danh sách tỉnh từ API:", provinces.map(p => ({ name: p.name, code: p.code })));
            const normalizedProvinceName = this.normalizeName(provinceName);
            console.log("Normalized province name:", normalizedProvinceName);
            const province = provinces.find(p => this.normalizeName(p.name) === normalizedProvinceName);
            if (!province) {
                this.showNotification(`Không tìm thấy tỉnh "${provinceName}"`, "warning");
                return null;
            }
            console.log(`Tìm thấy tỉnh: ${provinceName} -> Code: ${province.code}`);
            return province.code;
        } catch (error) {
            console.error("Lỗi khi ánh xạ tên tỉnh:", error);
            this.showNotification("Không thể tải danh sách tỉnh/thành phố: " + error.message, "error");
            return null;
        }
    },

    async mapDistrictNameToCode(provinceCode, districtName) {
        try {
            const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
            const province = await response.json();
            console.log(`Districts fetched for province ${provinceCode}:`, province.districts.map(d => d.name));
            let normalizedDistrictName = this.normalizeName(districtName);
            const districtVariants = [
                normalizedDistrictName,
                normalizedDistrictName.replace('quận', ''),
                normalizedDistrictName.replace('huyện', '')
            ];
            const district = province.districts.find(d =>
                districtVariants.includes(this.normalizeName(d.name))
            );
            if (!district) {
                console.warn(`District not found: ${districtName}`);
                this.showNotification(`Không tìm thấy quận "${districtName}"`, "warning");
                return null;
            }
            console.log(`Mapped district: ${districtName} -> ${district.code}`);
            return district.code;
        } catch (error) {
            console.error("Lỗi khi ánh xạ tên quận:", error);
            this.showNotification("Không thể tải danh sách quận/huyện", "error");
            return null;
        }
    },

    async mapWardNameToCode(districtCode, wardName) {
        try {
            const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
            const district = await response.json();
            console.log(`Wards fetched for district ${districtCode}:`, district.wards.map(w => w.name));
            let normalizedWardName = this.normalizeName(wardName);
            const wardVariants = [
                normalizedWardName,
                normalizedWardName.replace('phường', ''),
                normalizedWardName.replace('xã', '')
            ];
            const ward = district.wards.find(w =>
                wardVariants.includes(this.normalizeName(w.name))
            );
            if (!ward) {
                console.warn(`Ward not found: ${wardName}`);
                this.showNotification(`Không tìm thấy phường "${wardName}"`, "warning");
                return null;
            }
            console.log(`Mapped ward: ${wardName} -> ${ward.code}`);
            return ward.code;
        } catch (error) {
            console.error("Lỗi khi ánh xạ tên phường:", error);
            this.showNotification("Không thể tải danh sách phường/xã", "error");
            return null;
        }
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
        const prefixes = ["tenant", "owner", "room", "newCustomer"];
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