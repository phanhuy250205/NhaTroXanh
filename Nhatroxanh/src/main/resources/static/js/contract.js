/* <![CDATA[ */
window.NhaTroContract = {
    currentTab: "tenantInfo",
    selectedRoom: null, // Biến để lưu thông tin phòng được chọn
    zoomLevel: 1,

    init() {
        const requiredSelects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"];
        const missingSelects = requiredSelects.filter((id) => !document.getElementById(id));
        if (missingSelects.length > 0) {
            console.error("Missing select elements in DOM:", missingSelects);
            this.showNotification("Không tìm thấy một số trường tỉnh/thành phố trong giao diện", "error");
        }

        this.setupEventListeners();
        this.setCurrentDate();
        this.updateAllPreview();
        this.setupAmenityModal();
        this.setupCustomerModal();
        this.setupResidentModal();

        return this.loadProvinces()
            .then(() => {
                console.log("Provinces loaded");
                const contract = /*[[${contract}]]*/ null;
                if (contract) {
                    // Điền thông tin owner
                    if (contract.owner) {
                        this.fillOwnerFields(contract.owner);
                    }
                    // Điền thông tin tenant
                    if (contract.tenant && contract.tenantType === 'REGISTERED') {
                        console.log("Filling registered tenant data:", contract.tenant);
                        this.fillTenantFields(contract.tenant);
                    } else if (contract.unregisteredTenant && contract.tenantType === 'UNREGISTERED') {
                        console.log("Filling unregistered tenant data:", contract.unregisteredTenant);
                        this.fillUnregisteredTenantFields(contract.unregisteredTenant);
                    }
                    // Điền thông tin phòng
                    if (contract.room && contract.room.hostelId) {
                        const hostelSelect = document.getElementById("hostelId");
                        if (hostelSelect) {
                            hostelSelect.value = contract.room.hostelId;
                            this.filterRooms();
                            setTimeout(() => {
                                const roomSelect = document.getElementById("roomId");
                                if (roomSelect) {
                                    roomSelect.value = contract.room.roomId || "";
                                    this.onRoomSelected();
                                }
                            }, 500);
                        }
                    }
                }
            })
            .catch((error) => {
                console.error("Error loading provinces:", error);
                this.showNotification("Lỗi khi tải danh sách tỉnh/thành phố", "error");
            });
    },
    normalizeName(name) {
        if (!name) return ""
        return name
            .normalize("NFD") // Phân tách ký tự có dấu
            .replace(/[\u0300-\u036f]/g, "") // Loại bỏ dấu
            .replace(/^(Tỉnh|TP\.|Thành phố|Quận|Phường|Huyện|Xã)\s*/i, "")
            .replace(/\s+/g, " ")
            .trim()
            .toLowerCase()
    },

  setupEventListeners() {
    // Sự kiện click cho các tab
    document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
      link.addEventListener("click", (e) => {
        e.preventDefault()
        const tabId = link.getAttribute("data-tab")
        this.showTab(tabId)
      })
    })

    // Sự kiện cho các nút điều hướng
    document.getElementById("btn-next-owner")?.addEventListener("click", () => this.showTab("ownerInfo"))
    document.getElementById("btn-prev-tenant")?.addEventListener("click", () => this.showTab("tenantInfo"))
    document.getElementById("btn-next-room")?.addEventListener("click", () => this.showTab("roomInfo"))
    document.getElementById("btn-prev-owner")?.addEventListener("click", () => this.showTab("ownerInfo"))
    document.getElementById("btn-next-terms")?.addEventListener("click", () => this.showTab("terms"))
    document.getElementById("btn-prev-room")?.addEventListener("click", () => this.showTab("roomInfo"))

    // Sự kiện cho các nút hành động
    document.getElementById("btn-update")?.addEventListener("click", () => this.updateContract())
    document.getElementById("btn-print")?.addEventListener("click", () => this.printContract())
    document.getElementById("btn-save")?.addEventListener("click", (e) => {
      e.preventDefault()
      this.saveContract()
    })

    // Sự kiện cho các nút zoom
    document.getElementById("btn-zoom-in")?.addEventListener("click", () => this.zoomIn())
    document.getElementById("btn-zoom-out")?.addEventListener("click", () => this.zoomOut())
    document.getElementById("btn-reset-zoom")?.addEventListener("click", () => this.resetZoom())

    // Sự kiện tải ảnh
    document.getElementById("cccd-front")?.addEventListener("change", (e) => {
      this.previewImage(e, "cccd-front-preview")
    })
    document.getElementById("cccd-back")?.addEventListener("change", (e) => {
      this.previewImage(e, "cccd-back-preview")
    })

    // Sự kiện nhập số điện thoại
    const tenantPhoneInput = document.getElementById("tenant-phone")
    if (tenantPhoneInput) {
      tenantPhoneInput.addEventListener("input", (e) => {
        const phone = e.target.value.trim()
        if (phone.length >= 10) {
          this.fetchTenantByPhone(phone)
        } else {
          this.clearTenantFields()
        }
      })
    }

    // Sự kiện chọn khu trọ
    const hostelSelect = document.getElementById("hostelId")
    if (hostelSelect) {
      hostelSelect.addEventListener("change", () => this.filterRooms())
    }

    // Sự kiện chọn phòng trọ
    const roomSelect = document.getElementById("roomId")
    if (roomSelect) {
      roomSelect.addEventListener("change", () => this.onRoomSelected())
    }

    // Sự kiện chọn loại người thuê
    const tenantTypeSelect = document.getElementById("tenantType")
    if (tenantTypeSelect) {
      tenantTypeSelect.addEventListener("change", () => this.toggleTenantFields())
    }

    // Sự kiện khác
    this.setupPreviewListeners()
    this.setupLocationListeners()
  },

  // Hàm xử lý khu trọ và phòng trọ
  filterRooms() {
    const hostelId = document.getElementById("hostelId").value
    const roomSelect = document.getElementById("roomId")

    if (!roomSelect) {
      console.error("Room select element not found")
      this.showNotification("Không tìm thấy dropdown phòng trọ!", "error")
      return
    }

    roomSelect.disabled = true
    roomSelect.innerHTML = '<option value="">Đang tải...</option>'
    roomSelect.classList.add("loading-spinner")

    if (!hostelId) {
      roomSelect.classList.remove("loading-spinner")
      roomSelect.innerHTML = '<option value="">-- Chọn phòng trọ --</option>'
      this.clearRoomFields()
      return
    }

    console.log("Fetching rooms for hostelId:", hostelId)

    fetch(`/api/contracts/get-rooms-by-hostel?hostelId=${hostelId}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        "X-Requested-With": "XMLHttpRequest",
        "X-CSRF-TOKEN": document.querySelector('meta[name="_csrf"]')?.content || "",
        Authorization: "Bearer " + localStorage.getItem("authToken") || "",
      },
    })
      .then((response) => {
        roomSelect.classList.remove("loading-spinner")
        if (!response.ok) {
          return response.json().then((data) => {
            throw new Error(data.message || `HTTP error! status: ${response.status}`)
          })
        }
        return response.json()
      })
      .then((data) => {
        console.log("Rooms data received:", data) // Debug
        roomSelect.disabled = false
        roomSelect.innerHTML = '<option value="">-- Chọn phòng trọ --</option>'

        if (data.success && data.rooms && data.rooms.length > 0) {
          data.rooms.forEach((room) => {
            const option = document.createElement("option")
            option.value = room.roomId
            // Xử lý cả roomName và namerooms
            const roomName = room.roomName || room.namerooms || "Phòng không tên"
            const address = room.address || "Không có địa chỉ"
            option.textContent = `${roomName} (${address})`
            option.dataset.price = room.price
            option.dataset.acreage = room.acreage
            option.dataset.maxTenants = room.maxTenants
            roomSelect.appendChild(option)
          })
          this.showNotification(`Đã tải ${data.rooms.length} phòng trọ khả dụng`, "success")
        } else {
          roomSelect.innerHTML =
            '<option value="" disabled>' + (data.message || "Không có phòng trọ khả dụng") + "</option>"
          this.showNotification(data.message || "Không có phòng trọ khả dụng", "warning")
        }
        this.clearRoomFields()
      })
      .catch((error) => {
        console.error("Error loading rooms:", error)
        roomSelect.classList.remove("loading-spinner")
        roomSelect.disabled = false
        roomSelect.innerHTML = '<option value="" disabled>Lỗi khi tải danh sách phòng: ' + error.message + "</option>"
        this.showNotification("Lỗi khi tải danh sách phòng: " + error.message, "error")
        this.clearRoomFields()
      })
  },

  async onRoomSelected() {
    const roomSelect = document.getElementById("roomId")
    if (!roomSelect) {
      this.showNotification("Không tìm thấy dropdown phòng trọ!", "error")
      return
    }

    const selectedOption = roomSelect.options[roomSelect.selectedIndex]
    const roomId = roomSelect.value
    if (!roomId) {
      this.clearRoomFields()
      return
    }

    console.log("Fetching room details for roomId:", roomId)

        try {
            const response = await fetch(`/api/contracts/get-room-details?roomId=${roomId}`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json",
                    "X-Requested-With": "XMLHttpRequest",
                    "X-CSRF-TOKEN": document.querySelector('meta[name="_csrf"]')?.content || "",
                    Authorization: "Bearer " + localStorage.getItem("authToken") || "",
                },
            });
            const data = await response.json();
            console.log("Dữ liệu chi tiết phòng nhận được:", JSON.stringify(data, null, 2));

            if (data.success && data.room) {
                const room = data.room;
                // Cập nhật các trường hiển thị
                const roomNumber = document.getElementById("room-number");
                const roomArea = document.getElementById("room-area");
                const rentPrice = document.getElementById("rent-price");

                if (roomNumber) {
                    roomNumber.value = room.namerooms || selectedOption.text.split(" (")[0] || "";
                } else {
                    console.warn("Phần tử room-number không tồn tại");
                }
                if (roomArea) {
                    roomArea.value = room.acreage || "";
                } else {
                    console.warn("Phần tử room-area không tồn tại");
                }
                if (rentPrice) {
                    rentPrice.value = room.price > 0 ? room.price : "1000000";
                } else {
                    console.warn("Phần tử rent-price không tồn tại");
                }

                // Lưu thông tin phòng vào biến toàn cục
                this.selectedRoom = {
                    roomId: room.roomId,
                    roomName: room.namerooms || selectedOption.text.split(" (")[0] || "",
                    price: room.price > 0 ? room.price : "1000000",
                    status: room.status || "unactive",
                    hostelId: room.hostelId || "",
                    hostelName: room.hostelName || "",
                    acreage: room.acreage || "20",
                    description: room.description || ""
                };
                console.log("Selected room:", JSON.stringify(this.selectedRoom, null, 2));

                // Tính tiền cọc dựa trên giá thuê và số tháng cọc
                const depositMonthsInput = document.getElementById("deposit-months");
                const depositMonths = parseFloat(depositMonthsInput?.value || "2");
                const price = parseFloat(rentPrice?.value || "1000000");
                const termsDeposit = document.getElementById("terms-deposit");
                if (termsDeposit) {
                    termsDeposit.value = (price * depositMonths).toString();
                } else {
                    console.warn("Phần tử terms-deposit không tồn tại");
                }

                let address = room.address;
                if (!address && selectedOption.text.includes("(")) {
                    address = selectedOption.text.split(" (")[1].replace(")", "");
                }
                console.log("Địa chỉ đã xử lý:", address);

        if (address) {
          const addressParts = address.split(", ")
          const street = addressParts.length > 0 ? addressParts[0].trim() : ""
          const ward = addressParts.length > 1 ? addressParts[1].trim() : ""
          const district = addressParts.length > 2 ? addressParts[2].trim() : ""
          const province = addressParts.length > 3 ? addressParts[3].trim() : ""
          console.log("Address parts:", { street, ward, district, province })

                    const roomStreet = document.getElementById("room-street");
                    if (roomStreet) {
                        roomStreet.value = street;
                    } else {
                        console.warn("Phần tử room-street không tồn tại");
                    }

          // Điền vào dropdown Tỉnh/Thành phố
          const provinceSelect = document.getElementById("room-province")
          if (provinceSelect) {
            const provinceCode = await this.mapProvinceNameToCode(province)
            console.log("Mapped province code:", provinceCode)
            if (provinceCode) {
              const provinceOption = provinceSelect.querySelector(`option[value="${provinceCode}"]`)
              console.log("Province option found:", provinceOption)
              if (provinceOption) {
                provinceSelect.value = provinceCode
                console.log("Province set to:", provinceCode)
                await this.loadDistricts(provinceCode, "room-district", "room-ward")
              } else {
                provinceSelect.value = "" // Reset nếu không tìm thấy
                this.showNotification(
                  `Không tìm thấy option cho mã tỉnh/thành phố ${provinceCode} (${province})`,
                  "warning",
                )
              }
            } else {
              provinceSelect.value = province // Fallback với tên
              this.showNotification(`Không ánh xạ được mã cho ${province}`, "warning")
            }
          }

          // Điền vào dropdown Quận/Huyện
          const districtSelect = document.getElementById("room-district")
          if (districtSelect && provinceSelect && provinceSelect.value) {
            const districtCode = await this.mapDistrictNameToCode(provinceSelect.value, district)
            console.log("Mapped district code:", districtCode)
            if (districtCode) {
              const districtOption = districtSelect.querySelector(`option[value="${districtCode}"]`)
              console.log("District option found:", districtOption)
              if (districtOption) {
                districtSelect.value = districtCode
                console.log("District set to:", districtCode)
                await this.loadWards(districtCode, "room-ward", provinceSelect.value) // Truyền provinceCode
              } else {
                districtSelect.value = "" // Reset nếu không tìm thấy
                this.showNotification(
                  `Không tìm thấy option cho mã quận/huyện ${districtCode} (${district})`,
                  "warning",
                )
              }
            } else {
              districtSelect.value = district // Fallback với tên
              this.showNotification(`Không ánh xạ được mã cho ${district}`, "warning")
            }
          }

          // Điền vào dropdown Phường/Xã
          const wardSelect = document.getElementById("room-ward")
          if (wardSelect && districtSelect && districtSelect.value) {
            const wardCode = await this.mapWardNameToCode(districtSelect.value, ward, provinceSelect.value)
            console.log("Mapped ward code:", wardCode)
            if (wardCode) {
              const wardOption = wardSelect.querySelector(`option[value="${wardCode}"]`)
              console.log("Ward option found:", wardOption)
              if (wardOption) {
                wardSelect.value = wardCode
                console.log("Ward set to:", wardCode)
              } else {
                // Fallback: Thêm option mới với tên "Hòa Thuận" nếu không tìm thấy
                const newOption = document.createElement("option")
                newOption.value = ward // Sử dụng tên làm value
                newOption.textContent = ward // Hiển thị "Hòa Thuận"
                wardSelect.appendChild(newOption)
                wardSelect.value = ward
                console.log(`Added fallback ward: ${ward}`)
                this.showNotification(`Không tìm thấy mã cho ${ward}, sử dụng tên trực tiếp`, "warning")
              }
            } else {
              // Fallback: Thêm option mới với tên "Hòa Thuận" nếu không ánh xạ được
              const newOption = document.createElement("option")
              newOption.value = ward // Sử dụng tên làm value
              newOption.textContent = ward // Hiển thị "Hòa Thuận"
              wardSelect.appendChild(newOption)
              wardSelect.value = ward
              console.log(`Added fallback ward: ${ward}`)
              this.showNotification(`Không ánh xạ được mã cho ${ward}, sử dụng tên trực tiếp`, "warning")
            }
          }
        } else {
          this.showNotification("Không tìm thấy địa chỉ để điền!", "warning")
        }

        this.updatePreviewField("room-number", "preview-room-number")
        this.updatePreviewField("room-area", "preview-room-area")
        this.updatePreviewField("rent-price", "preview-rent")
        this.updateAddress("room")
        this.calculateDeposit()

        this.showNotification(
          `Đã chọn ${room.namerooms || selectedOption.text.split(" (")[0]} - Diện tích: ${room.acreage || ""}m² - Giá: ${new Intl.NumberFormat("vi-VN").format(room.price || 0)} VNĐ/tháng`,
          "success",
        )
      } else {
        this.showNotification(data.message || "Không thể lấy thông tin phòng!", "error")
        this.clearRoomFields()
      }
    } catch (error) {
      console.error("Error fetching room details:", error)
      this.showNotification("Lỗi khi lấy thông tin phòng: " + error.message, "error")
      this.clearRoomFields()
    }
  },

  clearRoomFields() {
    document.getElementById("room-number").value = ""
    document.getElementById("room-area").value = ""
    document.getElementById("rent-price").value = ""
    document.getElementById("room-province").value = ""
    document.getElementById("room-district").innerHTML = '<option value="">Quận/Huyện</option>'
    document.getElementById("room-ward").innerHTML = '<option value="">Phường/Xã</option>'
    document.getElementById("room-street").value = ""
    this.updatePreviewField("room-number", "preview-room-number")
    this.updatePreviewField("room-area", "preview-room-area")
    this.updatePreviewField("rent-price", "preview-rent")
    this.updateAddress("room")
    this.calculateDeposit()
  },

  toggleTenantFields() {
    const tenantType = document.getElementById("tenantType").value
    const registeredFields = document.getElementById("registeredTenantFields")
    const unregisteredFields = document.getElementById("unregisteredTenantFields")
    registeredFields.style.display = tenantType === "REGISTERED" ? "block" : "none"
    unregisteredFields.style.display = tenantType === "UNREGISTERED" ? "block" : "none"
  },

  // Hàm lấy thông tin người thuê qua số điện thoại
  async fetchTenantByPhone(phone) {
    try {
      const response = await fetch(`/api/contracts/get-tenant-by-phone?phone=${encodeURIComponent(phone)}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-CSRF-TOKEN": document.querySelector('meta[name="_csrf"]')?.content || "",
        },
      })
      const data = await response.json()
      console.log("API response:", data)
      if (data.success) {
        this.fillTenantFields(data.tenant)
        this.showNotification("Đã tìm thấy thông tin người thuê!", "success")
      } else {
        this.clearTenantFields()
        this.showNotification(data.message || "Không tìm thấy người thuê với số điện thoại này", "warning")
      }
    } catch (error) {
      console.error("Lỗi khi lấy thông tin người thuê:", error)
      this.clearTenantFields()
      this.showNotification("Lỗi khi lấy thông tin người thuê", "error")
    }
  },

    // 4. SỬA HÀM FILL TENANT FIELDS - thêm debug và đảm bảo load provinces trước
    async fillTenantFields(tenant) {
        console.log("Filling tenant fields with data:", tenant);

        await this.loadProvinces();

        this.setInputValue("tenant-name", tenant.fullName || "");
        this.setInputValue("tenant-dob", this.formatDate(tenant.birthday));
        this.setInputValue("tenant-id", tenant.cccdNumber || "");
        this.setInputValue("tenant-id-date", this.formatDate(tenant.issueDate));
        this.setInputValue("tenant-id-place", tenant.issuePlace || "");
        this.setInputValue("tenant-email", tenant.email || "");
        this.setInputValue("tenant-phone", tenant.phone || "");

        // Ưu tiên các trường riêng lẻ từ ContractDto
        let street = tenant.street || "";
        let ward = tenant.ward || "";
        let district = tenant.district || "";
        let province = tenant.province || "";

        // Nếu các trường riêng lẻ rỗng, thử split từ tenant.address
        if (!street && !ward && !district && !province && tenant.address) {
            const addressParts = tenant.address.split(", ").map(part => part.trim());
            if (addressParts.length >= 4) {
                street = addressParts[0];
                ward = addressParts[1];
                district = addressParts[2];
                province = addressParts[3];
            } else if (addressParts.length === 3) {
                ward = addressParts[0];
                district = addressParts[1];
                province = addressParts[2];
            } else if (addressParts.length === 2) {
                district = addressParts[0];
                province = addressParts[1];
            } else if (addressParts.length === 1) {
                province = addressParts[0];
            }
        }

        console.log("Tenant address fields:", { street, ward, district, province });

        this.setInputValue("tenant-street", street);
        if (province) {
            this.debugDropdownOptions("tenant-province", province);
            this.setInputValue("tenant-province", province);
            await this.loadDistricts(province, "tenant-district", "tenant-ward");
            if (district) {
                this.debugDropdownOptions("tenant-district", district);
                this.setInputValue("tenant-district", district);
                await this.loadWards(district, "tenant-ward", province);
                if (ward) {
                    this.debugDropdownOptions("tenant-ward", ward);
                    this.setInputValue("tenant-ward", ward);
                }
            }
        }

        this.updatePreviewFields('tenant', tenant);
        this.updateAddress('tenant');
        this.fillCccdImages('tenant', tenant);
    },

    setInputValue(inputId, value) {
        const input = document.getElementById(inputId);
        if (input) {
            input.value = value || '';
        } else {
            console.warn(`Input not found: ${inputId}`);
        }
    },

    async fillOwnerFields(owner) {
        console.log("Filling owner fields with data:", owner);

        // Đảm bảo provinces đã được load
        await this.loadProvinces();

        // Điền thông tin cơ bản
        this.setInputValue("owner-name", owner.fullName);
        this.setInputValue("owner-dob", this.formatDate(owner.birthday));
        this.setInputValue("owner-id", owner.cccdNumber);
        this.setInputValue("owner-id-date", this.formatDate(owner.issueDate));
        this.setInputValue("owner-id-place", owner.issuePlace);
        this.setInputValue("owner-email", owner.email);
        this.setInputValue("owner-phone", owner.phone);
        this.setInputValue("owner-street", owner.street);

        // Cập nhật preview
        this.updatePreviewFields('owner', owner);

        // Xử lý địa chỉ
        await this.fillAddressFields('owner', owner);

        // Xử lý ảnh CCCD
        this.fillCccdImages('owner', owner);
    },

    setInputValue(inputId, value) {
        const input = document.getElementById(inputId);
        if (input) {
            input.value = value || '';
        } else {
            console.warn(`Input not found: ${inputId}`);
        }
    },

    formatDate(dateString) {
        if (!dateString) return '';
        return new Date(dateString).toISOString().split('T')[0];
    },

    updatePreviewFields(type, data) {
        const previewMapping = {
            tenant: {
                name: 'preview-tenant-name',
                dob: 'preview-tenant-dob',
                id: 'preview-tenant-id',
                idDate: 'preview-tenant-id-date',
                idPlace: 'preview-tenant-id-place',
                phone: 'preview-tenant-phone',
                address: 'preview-tenant-address'
            },
            owner: {
                name: 'preview-owner-name',
                dob: 'preview-owner-dob',
                id: 'preview-owner-id',
                idDate: 'preview-owner-id-date',
                idPlace: 'preview-owner-id-place',
                phone: 'preview-owner-phone',
                address: 'preview-owner-address'
            }
        };

        const mapping = previewMapping[type];
        if (!mapping) return;

        this.updatePreviewField(type + '-name', mapping.name);
        this.updatePreviewField(type + '-dob', mapping.dob);
        this.updatePreviewField(type + '-id', mapping.id);
        this.updatePreviewField(type + '-id-date', mapping.idDate);
        this.updatePreviewField(type + '-id-place', mapping.idPlace);
        this.updatePreviewField(type + '-phone', mapping.phone);

        // Cập nhật địa chỉ
        this.updateAddress(type);
    },

    async fillAddressFields(type, data) {
        const provinceSelect = document.getElementById(`${type}-province`);
        const districtSelect = document.getElementById(`${type}-district`);
        const wardSelect = document.getElementById(`${type}-ward`);

        if (!data.province) return;

        try {
            await this.loadProvinces();
            if (provinceSelect) {
                provinceSelect.value = data.province;
                await this.loadDistricts(data.province, `${type}-district`, `${type}-ward`);
                if (districtSelect && data.district) {
                    districtSelect.value = data.district;
                    await this.loadWards(data.district, `${type}-ward`, data.province);
                    if (wardSelect && data.ward) {
                        wardSelect.value = data.ward;
                    }
                }
            }
            this.updateAddress(type);
        } catch (error) {
            console.error(`Error filling ${type} address fields:`, error);
            this.showNotification(`Lỗi khi điền địa chỉ ${type}`, 'error');
        }
    },

    fillCccdImages(type, data) {
        const frontPreview = document.getElementById(`${type}-cccd-front-preview`);
        const backPreview = document.getElementById(`${type}-cccd-back-preview`);

        if (data.cccdFrontUrl && frontPreview) {
            frontPreview.innerHTML = `<img src="${data.cccdFrontUrl}" alt="CCCD Front" style="max-width: 100%;">`;
        }

        if (data.cccdBackUrl && backPreview) {
            backPreview.innerHTML = `<img src="${data.cccdBackUrl}" alt="CCCD Back" style="max-width: 100%;">`;
        }
    },

// Sử dụng trong quá trình khởi tạo hoặc khi load dữ liệu
    async loadContractData(contractId) {
        try {
            const response = await fetch(`/api/contracts/${contractId}`);
            const contract = await response.json();

            if (contract.tenant) {
                await this.fillTenantFields(contract.tenant);
            }

            if (contract.owner) {
                await this.fillOwnerFields(contract.owner);
            }

            // Điền các thông tin khác của hợp đồng
            this.fillContractDetails(contract);
        } catch (error) {
            console.error('Error loading contract data:', error);
            this.showNotification('Không thể tải dữ liệu hợp đồng', 'error');
        }
    },

    fillContractDetails(contract) {
        // Điền các thông tin chi tiết của hợp đồng
        this.setInputValue('contract-date', this.formatDate(contract.contractDate));
        this.setInputValue('start-date', this.formatDate(contract.startDate));
        this.setInputValue('contract-duration', contract.duration);
        this.setInputValue('rent-price', contract.rentPrice);
        this.setInputValue('deposit-months', contract.depositMonths);
        this.setInputValue('contract-status', contract.status);

        // Điền thông tin phòng
        if (contract.room) {
            this.setInputValue('hostelId', contract.room.hostelId);
            this.setInputValue('roomId', contract.room.roomId);
            this.setInputValue('room-number', contract.room.roomName);
            this.setInputValue('room-area', contract.room.area);
        }

        // Cập nhật các preview
        this.updateAllPreview();
    },

    // Hàm xóa các trường thông tin người thuê
    clearTenantFields() {
        document.getElementById("tenant-name").value = ""
        document.getElementById("tenant-dob").value = ""
        document.getElementById("tenant-id").value = ""
        document.getElementById("tenant-id-date").value = ""
        document.getElementById("tenant-id-place").value = ""
        document.getElementById("tenant-email").value = ""
        document.getElementById("tenant-street").value = ""
        document.getElementById("tenant-province").value = ""
        document.getElementById("tenant-district").innerHTML = '<option value="">Quận/Huyện</option>'
        document.getElementById("tenant-ward").innerHTML = '<option value="">Phường/Xã</option>'


        // Xóa các trường xem trước
        document.getElementById("preview-tenant-name").textContent = "........................";
        document.getElementById("preview-tenant-dob").textContent = "........................";
        document.getElementById("preview-tenant-id").textContent = "........................";
        document.getElementById("preview-tenant-id-date").textContent = "........................";
        document.getElementById("preview-tenant-id-place").textContent = "........................";
        document.getElementById("preview-tenant-phone").textContent = "........................";
        document.getElementById("preview-tenant-address").textContent = "........................";
        this.updateAllPreview()
    },

  async mapProvinceNameToCode(provinceName) {
    try {
      // FIX: Sử dụng API tỉnh thành Việt Nam thực tế
      const response = await fetch("https://provinces.open-api.vn/api/p/")
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
      const provinces = await response.json()
      console.log(
        "Danh sách tỉnh từ API:",
        provinces.map((p) => ({ name: p.name, code: p.code })),
      )
      const normalizedProvinceName = this.normalizeName(provinceName)
      console.log("Normalized province name:", normalizedProvinceName)
      const province = provinces.find((p) => {
        const normalizedApiName = this.normalizeName(p.name)
        return (
          normalizedApiName === normalizedProvinceName ||
          normalizedApiName.includes(normalizedProvinceName) ||
          normalizedProvinceName.includes(normalizedApiName)
        )
      })
      if (!province) {
        console.warn(
          `No match for province: ${provinceName}, checked variants:`,
          provinces.map((p) => this.normalizeName(p.name)),
        )
        return null
      }
      const provinceCode = String(province.code).padStart(2, "0") // Chuyển thành chuỗi và pad
      console.log(`Found province: ${provinceName} -> Code: ${provinceCode}`)
      return provinceCode
    } catch (error) {
      console.error("Mapping error:", error)
      return null
    }
  },

  async mapDistrictNameToCode(provinceCode, districtName) {
    try {
      // Validate provinceCode
      const provinceCodeStr = String(provinceCode).padStart(2, "0")
      if (!/^\d{2}$/.test(provinceCodeStr)) {
        console.warn("Invalid province code:", provinceCode)
        return null
      }

      // FIX: Sử dụng API tỉnh thành Việt Nam thực tế
      const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCodeStr}?depth=2`)
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)

      const province = await response.json()
      console.log(
        `Districts fetched for province ${provinceCodeStr}:`,
        province.districts.map((d) => d.name),
      )

      const normalizedDistrictName = this.normalizeName(districtName)
      const districtVariants = [
        normalizedDistrictName,
        `quận ${normalizedDistrictName}`,
        `huyện ${normalizedDistrictName}`,
        `thành phố ${normalizedDistrictName}`,
        `thị xã ${normalizedDistrictName}`,
        normalizedDistrictName.replace(/^quận\s+/i, ""),
        normalizedDistrictName.replace(/^huyện\s+/i, ""),
        normalizedDistrictName.replace(/^thành phố\s+/i, ""),
        normalizedDistrictName.replace(/^thị xã\s+/i, ""),
      ]

      const district = province.districts.find((d) =>
        districtVariants.some(
          (variant) =>
            this.normalizeName(d.name) === variant ||
            this.normalizeName(d.name).includes(variant) ||
            variant.includes(this.normalizeName(d.name)),
        ),
      )

      if (!district) {
        console.warn(`District not found: ${districtName}, variants checked: ${districtVariants.join(", ")}`)
        return null
      }

      console.log(`Mapped district: ${districtName} -> ${district.code}`)
      return district.code
    } catch (error) {
      console.error("Lỗi khi ánh xạ tên quận:", error)
      return null
    }
  },

  // SỬA LỖI: Thêm tham số provinceCode vào hàm mapWardNameToCode
  async mapWardNameToCode(districtCode, wardName, provinceCode) {
    try {
      // Chuyển districtCode thành string và validate
      const districtCodeStr = String(districtCode).trim()

      if (!districtCodeStr || districtCodeStr === "null" || districtCodeStr === "undefined") {
        console.warn(`Invalid district code: ${districtCode}`)
        return null
      }

      // SỬA LỖI: Sử dụng provinceCode được truyền vào thay vì lấy từ districtCode
      const provinceCodeStr = String(provinceCode).padStart(2, "0")

      if (!provinceCodeStr || !/^\d{2}$/.test(provinceCodeStr)) {
        console.warn(`Invalid province code: ${provinceCode}`)
        return null
      }

      console.log(`Mapping ward: ${wardName} for district ${districtCodeStr} in province ${provinceCodeStr}`)

      // FIX: Sử dụng API tỉnh thành Việt Nam thực tế
      const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCodeStr}?depth=2`)
      if (!response.ok) {
        throw new Error(
          `HTTP error! status: ${response.status}, URL: https://provinces.open-api.vn/api/d/${districtCodeStr}?depth=2`,
        )
      }

      const district = await response.json()

      console.log(
        `Wards fetched for district ${districtCodeStr}:`,
        district.wards.map((w) => w.name),
      )

      // Chuẩn hóa tên phường/xã
      const normalizedWardName = this.normalizeName(wardName)

      // Tạo các biến thể tên phường/xã
      const wardVariants = new Set([
        normalizedWardName,
        `phường ${normalizedWardName}`,
        `xã ${normalizedWardName}`,
        `thị trấn ${normalizedWardName}`,
        normalizedWardName.replace(/^(phường|xã|thị trấn)\s+/i, "").trim(),
      ])

      const cleanVariants = Array.from(wardVariants).filter((v) => v && v.length > 0)
      console.log(`Ward variants for "${wardName}":`, cleanVariants)

      // Tìm phường/xã
      const ward = district.wards.find((w) => {
        const normalizedApiName = this.normalizeName(w.name)
        return cleanVariants.some((variant) => {
          if (normalizedApiName === variant) return true
          const apiNameWithoutPrefix = normalizedApiName.replace(/^(phường|xã|thị trấn)\s+/i, "").trim()
          if (apiNameWithoutPrefix === variant) return true
          if (normalizedApiName.includes(variant) && variant.length > 3) return true
          if (variant.includes(normalizedApiName) && normalizedApiName.length > 3) return true
          return false
        })
      })

      if (!ward) {
        console.warn(`Ward not found: ${wardName}`)
        console.warn(
          `Available wards:`,
          district.wards.map((w) => w.name),
        )
        console.warn(`Variants tried:`, cleanVariants)
        return null
      }

      console.log(`Mapped ward: ${wardName} -> ${ward.name} (${ward.code})`)
      return ward.code
    } catch (error) {
      console.error(`Error mapping ward ${wardName} for district ${districtCode}:`, error.message)
      return null
    }
  },

    async loadProvinces() {
        try {
            const response = await fetch("https://provinces.open-api.vn/api/p/");
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinces = await response.json();
            console.log(
                "Loaded provinces:",
                provinces.map((p) => ({ code: p.code, name: p.name })),
            );

            const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"];
            selects.forEach((selectId) => {
                const select = document.getElementById(selectId);
                if (!select) {
                    console.warn(`Select element with ID ${selectId} not found in DOM`);
                    this.showNotification(`Không tìm thấy dropdown ${selectId}`, "error");
                    return;
                }
                select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>';
                provinces.forEach((province) => {
                    const option = document.createElement("option");
                    option.value = province.name; // Sử dụng tên thay vì code
                    option.textContent = province.name;
                    select.appendChild(option);
                    console.log(`Added province option: ${province.name}`);
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
    async loadDistricts(provinceName, districtSelectId, wardSelectId) {
        try {
            const normalizedProvinceName = this.normalizeName(provinceName);
            const provinceResponse = await fetch("https://provinces.open-api.vn/api/p/");
            if (!provinceResponse.ok) throw new Error(`HTTP error! status: ${provinceResponse.status}`);
            const provinces = await provinceResponse.json();
            const province = provinces.find((p) => this.normalizeName(p.name) === normalizedProvinceName);
            if (!province) throw new Error(`Province not found: ${provinceName}`);

            const provinceCode = String(province.code).padStart(2, "0");
            const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const provinceData = await response.json();
            console.log(
                `Loaded districts for province ${provinceName}:`,
                provinceData.districts.map((d) => ({ code: d.code, name: d.name })),
            );

            const districtSelect = document.getElementById(districtSelectId);
            const wardSelect = document.getElementById(wardSelectId);

            if (districtSelect) {
                districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
                provinceData.districts.forEach((district) => {
                    const option = document.createElement("option");
                    option.value = district.name; // Sử dụng tên thay vì code
                    option.textContent = district.name;
                    districtSelect.appendChild(option);
                    console.log(`Added district option: ${district.name}`);
                });
                districtSelect.value = "";
            }

            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                wardSelect.value = "";
            }
        } catch (error) {
            console.error(`Error loading districts for province ${provinceName}:`, error);
            this.showNotification("Không thể tải danh sách quận/huyện: " + error.message, "error");
        }
    },

  // 3. THÊM HÀM DEBUG KIỂM TRA DROPDOWN
  debugDropdownOptions(selectId, expectedValue) {
    const select = document.getElementById(selectId)
    if (!select) {
      console.error(`Select ${selectId} not found`)
      return
    }

    console.log(`=== DEBUG ${selectId} ===`)
    console.log(`Looking for value: "${expectedValue}"`)
    console.log(`Total options: ${select.options.length}`)

    Array.from(select.options).forEach((option, index) => {
      console.log(`Option ${index}: value="${option.value}", text="${option.textContent}"`)
      if (option.value === expectedValue) {
        console.log(`✅ FOUND MATCH at index ${index}`)
      }
    })

        const foundOption = select.querySelector(`option[value="${expectedValue}"]`)
        console.log(`querySelector result:`, foundOption ? "Found" : "Not found")
        console.log(`=== END DEBUG ===`)
    },
    async safeFetch(url, options = {}) {
        try {
            const response = await fetch(url, options)
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}))
                throw new Error(errorData.message || `HTTP error! status: ${response.status}, URL: ${url}`)
            }
            return await response.json()
        } catch (error) {
            console.error(`Error in safeFetch for ${url}:`, error.message)
            this.showNotification(`Lỗi khi gọi API: ${error.message}`, "error")
            throw error
        }
    },
    async loadWards(districtName, wardSelectId, provinceName) {
        try {
            const normalizedProvinceName = this.normalizeName(provinceName);
            const provinceResponse = await fetch("https://provinces.open-api.vn/api/p/");
            if (!provinceResponse.ok) throw new Error(`HTTP error! status: ${provinceResponse.status}`);
            const provinces = await provinceResponse.json();
            const province = provinces.find((p) => this.normalizeName(p.name) === normalizedProvinceName);
            if (!province) throw new Error(`Province not found: ${provinceName}`);

            const provinceCode = String(province.code).padStart(2, "0");
            const districtResponse = await fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`);
            if (!districtResponse.ok) throw new Error(`HTTP error! status: ${districtResponse.status}`);
            const provinceData = await districtResponse.json();
            const normalizedDistrictName = this.normalizeName(districtName);
            const district = provinceData.districts.find((d) => this.normalizeName(d.name) === normalizedDistrictName);
            if (!district) throw new Error(`District not found: ${districtName}`);

            const districtCode = String(district.code);
            const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`);
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const districtData = await response.json();

            console.log(
                `Loaded wards for district ${districtName}:`,
                districtData.wards.map((w) => ({ code: w.code, name: w.name })),
            );

            const wardSelect = document.getElementById(wardSelectId);
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
                if (districtData.wards && districtData.wards.length > 0) {
                    districtData.wards.forEach((ward) => {
                        const option = document.createElement("option");
                        option.value = ward.name; // Sử dụng tên thay vì code
                        option.textContent = ward.name;
                        wardSelect.appendChild(option);
                        console.log(`Added ward option: ${ward.name}`);
                    });
                    console.log(`Loaded ${districtData.wards.length} wards for district ${districtName}`);
                } else {
                    console.warn(`No wards found for district ${districtName}`);
                    this.showNotification(`Không tìm thấy phường/xã cho quận ${districtName}`, "warning");
                }
            }
        } catch (error) {
            console.error(`Error loading wards for district ${districtName}:`, error.message);
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

                    if (districtSelect.value && provinceSelect.value) {
                        this.loadWards(districtSelect.value, `${prefix}-ward`, provinceSelect.value);
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
    const today = new Date().toISOString().split("T")[0]
    const contractDateInput = document.getElementById("contract-date")
    const startDateInput = document.getElementById("start-date")

    if (contractDateInput) contractDateInput.value = today
    if (startDateInput) startDateInput.value = today

    this.updatePreviewField("contract-date", "preview-sign-date")
    this.updatePreviewField("start-date", "preview-start-date")
    this.calculateEndDate()
  },

  showTab(tabId) {
    document.querySelectorAll(".tab-pane").forEach((pane) => {
      pane.classList.remove("show", "active")
    })

    document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
      link.classList.remove("active")
    })

    const targetTab = document.getElementById(tabId)
    const targetLink = document.querySelector(`[data-tab="${tabId}"]`)

    if (targetTab && targetLink) {
      targetTab.classList.add("show", "active")
      targetLink.classList.add("active")
      this.currentTab = tabId
    }

    window.scrollTo({ top: 0, behavior: "smooth" })
  },

  previewImage(event, previewId) {
    const file = event.target.files[0]
    const preview = document.getElementById(previewId)

    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        preview.innerHTML = `<img src="${e.target.result}" alt="Ảnh CCCD" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
      }
      reader.readAsDataURL(file)
    }
  },

  updateAllPreview() {
    this.updatePreviewField("contract-date", "preview-sign-date")
    this.updatePreviewField("start-date", "preview-start-date")
    this.calculateEndDate()
    this.calculateDeposit()
    this.updatePaymentMethod()
    this.updateAmenities()
    this.updateTermsPreview() // Add this line to update terms preview
  },

  updatePreviewField(inputId, previewId) {
    const input = document.getElementById(inputId)
    const preview = document.getElementById(previewId)

    if (input && preview) {
      let value = input.value

      if (input.type === "date" && value) {
        const date = new Date(value)
        value = date.toLocaleDateString("vi-VN")
      }

      if (inputId === "rent-price" && value) {
        value = new Intl.NumberFormat("vi-VN").format(value)
      }

      if (input.tagName === "TEXTAREA" && value) {
        preview.innerHTML = value.replace(/\n/g, "<br>")
      } else {
        preview.textContent = value || "........................"
      }

      if (value) {
        preview.classList.add("nha-tro-updated")
        setTimeout(() => preview.classList.remove("nha-tro-updated"), 1000)
      }

      if (inputId === "start-date" || inputId === "contract-duration") {
        this.calculateEndDate()
      }
      if (inputId === "rent-price" || inputId === "deposit-months") {
        this.calculateDeposit()
      }
    }
  },

  // SỬA LỖI: Cải thiện hàm updateAddress để lấy text thay vì value
  updateAddress(prefix) {
    const streetInput = document.getElementById(`${prefix}-street`)
    const provinceSelect = document.getElementById(`${prefix}-province`)
    const districtSelect = document.getElementById(`${prefix}-district`)
    const wardSelect = document.getElementById(`${prefix}-ward`)
    const addressDiv = document.getElementById(`${prefix}-address`)

    if (!addressDiv) return

    const addressParts = []

    // Lấy tên đường/số nhà
    if (streetInput && streetInput.value.trim()) {
      addressParts.push(streetInput.value.trim())
    }

    // QUAN TRỌNG: Lấy TEXT (tên hiển thị) thay vì VALUE (mã code)
    if (wardSelect && wardSelect.value && wardSelect.selectedIndex > 0) {
      const selectedWardText = wardSelect.options[wardSelect.selectedIndex].text
      addressParts.push(selectedWardText)
    }

    if (districtSelect && districtSelect.value && districtSelect.selectedIndex > 0) {
      const selectedDistrictText = districtSelect.options[districtSelect.selectedIndex].text
      addressParts.push(selectedDistrictText)
    }

    if (provinceSelect && provinceSelect.value && provinceSelect.selectedIndex > 0) {
      const selectedProvinceText = provinceSelect.options[provinceSelect.selectedIndex].text
      addressParts.push(selectedProvinceText)
    }

    // Tạo địa chỉ đầy đủ
    const fullAddress = addressParts.join(", ")
    addressDiv.textContent = fullAddress || "Chưa có địa chỉ"

    console.log(`Updated ${prefix} address:`, fullAddress)
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
    const startDate = document.getElementById("start-date")?.value
    const duration = document.getElementById("contract-duration")?.value

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
    const rentPrice = document.getElementById("rent-price")?.value
    const depositMonths = document.getElementById("deposit-months")?.value

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

  // Các hàm zoom
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

    // Các hàm hành động


    updateContract() {
        this.showNotification("Hợp đồng đã được cập nhật!", "success")
    },

  printContract() {
    const printContent = document.getElementById("contract-preview").innerHTML
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
                <body>${printContent}</body>
            </html>
        `)
        printWindow.document.close()
        printWindow.print()
    },

    updateContract() {
        // Lấy ID hợp đồng
        const contractId = this.extractContractIdFromUrl();

        if (!contractId) {
            this.showNotification('Không tìm thấy ID hợp đồng', 'error');
            return;
        }

        // Tạo đối tượng dữ liệu chi tiết
        const contractData = this.prepareContractUpdateData(contractId);

        // Log toàn bộ dữ liệu để kiểm tra
        console.log("Contract Update Data:", JSON.stringify(contractData, null, 2));

        // Gửi request update
        this.sendContractUpdateRequest(contractId, contractData);
    },

    extractContractIdFromUrl() {
        // Trích xuất ID từ URL hoặc input ẩn
        const pathParts = window.location.pathname.split('/');
        const urlId = pathParts[pathParts.length - 1];
        const hiddenId = document.getElementById('contract-id')?.value;

        return urlId || hiddenId;
    },

    prepareContractUpdateData(contractId) {
        // Thu thập toàn bộ dữ liệu chi tiết
        return {
            contractId: contractId,

            // Thông tin cơ bản hợp đồng
            contractInfo: {
                contractDate: this.getInputValue('contract-date'),
                startDate: this.getInputValue('start-date'),
                endDate: this.getInputValue('end-date'),
                status: this.getInputValue('contract-status'),
                duration: this.getInputValue('contract-duration')
            },

            // Thông tin người thuê
            tenantInfo: this.prepareTenantData(),

            // Thông tin chủ sở hữu
            ownerInfo: this.prepareOwnerData(),

            // Thông tin phòng
            roomInfo: this.prepareRoomData(),

            // Điều khoản hợp đồng
            termsInfo: {
                rentPrice: this.getInputValue('rent-price'),
                depositMonths: this.getInputValue('deposit-months'),
                depositAmount: this.getInputValue('terms-deposit'),
                paymentMethod: this.getInputValue('payment-method'),
                paymentDate: this.getInputValue('payment-date'),
                additionalTerms: this.getInputValue('terms-conditions')
            },

            // Tiện ích
            utilities: this.collectUtilities(),

            // Người ở
            residents: this.collectResidents()
        };
    },

    prepareTenantData() {
        const tenantType = document.getElementById('tenantType')?.value;

        if (tenantType === 'REGISTERED') {
            return {
                type: 'REGISTERED',
                fullName: this.getInputValue('tenant-name'),
                phone: this.getInputValue('tenant-phone'),
                email: this.getInputValue('tenant-email'),
                dob: this.getInputValue('tenant-dob'),
                cccdNumber: this.getInputValue('tenant-id'),
                cccdIssueDate: this.getInputValue('tenant-id-date'),
                cccdIssuePlace: this.getInputValue('tenant-id-place'),
                address: {
                    street: this.getInputValue('tenant-street'),
                    ward: this.getSelectText('tenant-ward'),
                    district: this.getSelectText('tenant-district'),
                    province: this.getSelectText('tenant-province')
                },
                cccdFiles: this.collectCccdFiles('tenant')
            };
        } else {
            return {
                type: 'UNREGISTERED',
                fullName: this.getInputValue('unregisteredTenantFullName'),
                phone: this.getInputValue('unregisteredTenantPhone'),
                cccdNumber: this.getInputValue('unregisteredTenantCccdNumber'),
                dob: this.getInputValue('unregisteredTenantBirthday'),
                cccdIssueDate: this.getInputValue('unregisteredTenantIssueDate'),
                cccdIssuePlace: this.getInputValue('unregisteredTenantIssuePlace'),
                address: {
                    street: this.getInputValue('unregisteredTenantStreet'),
                    ward: this.getSelectText('unregisteredTenantWard'),
                    district: this.getSelectText('unregisteredTenantDistrict'),
                    province: this.getSelectText('unregisteredTenantProvince')
                },
                cccdFiles: this.collectCccdFiles('unregistered-tenant')
            };
        }
    },

    prepareOwnerData() {
        return {
            fullName: this.getInputValue('owner-name'),
            phone: this.getInputValue('owner-phone'),
            email: this.getInputValue('owner-email'),
            dob: this.getInputValue('owner-dob'),
            cccdNumber: this.getInputValue('owner-id'),
            cccdIssueDate: this.getInputValue('owner-id-date'),
            cccdIssuePlace: this.getInputValue('owner-id-place'),
            address: {
                street: this.getInputValue('owner-street'),
                ward: this.getSelectText('owner-ward'),
                district: this.getSelectText('owner-district'),
                province: this.getSelectText('owner-province')
            },
            cccdFiles: this.collectCccdFiles('owner')
        };
    },

    prepareRoomData() {
        return {
            roomId: this.getInputValue('roomId'),
            roomName: this.getInputValue('room-name'),
            hostelId: this.getInputValue('hostelId'),
            area: this.getInputValue('room-area'),
            price: this.getInputValue('rent-price'),
            address: {
                street: this.getInputValue('room-street'),
                ward: this.getSelectText('room-ward'),
                district: this.getSelectText('room-district'),
                province: this.getSelectText('room-province')
            }
        };
    },

    collectUtilities() {
        const utilities = [];
        document.querySelectorAll('#amenities-list-host input[type="checkbox"]:checked').forEach(checkbox => {
            utilities.push(checkbox.id);
        });
        return utilities;
    },

    collectResidents() {
        const residents = [];
        document.querySelectorAll('#residents-list .nha-tro-resident-item').forEach(residentElement => {
            residents.push({
                name: residentElement.querySelector('h6')?.textContent,
                birthYear: residentElement.querySelector('.text-muted')?.textContent.replace('Năm sinh: ', ''),
                phone: residentElement.querySelector('.text-muted:nth-child(3)')?.textContent.replace('SĐT: ', ''),
                id: residentElement.querySelector('.text-muted:nth-child(4)')?.textContent.replace('CCCD: ', '')
            });
        });
        return residents;
    },

    collectCccdFiles(prefix) {
        const files = {};

        // Xử lý ảnh mặt trước
        const frontInput = document.getElementById(`${prefix}-cccd-front`);
        if (frontInput && frontInput.files.length > 0) {
            files.front = frontInput.files[0];
        }

        // Xử lý ảnh mặt sau
        const backInput = document.getElementById(`${prefix}-cccd-back`);
        if (backInput && backInput.files.length > 0) {
            files.back = backInput.files[0];
        }

        return files;
    },

    sendContractUpdateRequest(contractId, contractData) {
        // Tạo FormData để gửi dữ liệu
        const formData = new FormData();

        // Thêm dữ liệu JSON
        formData.append('contractData', JSON.stringify(contractData));

        // Thêm file CCCD
        this.appendCccdFiles(formData, contractData);

        // Hiển thị loading
        this.showLoadingIndicator();

        // Gửi request
        fetch(`/api/contracts/update/${contractId}`, {
            method: 'PUT',
            headers: {
                'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || '',
                'X-Requested-With': 'XMLHttpRequest'
            },
            body: formData
        })
            .then(response => {
                this.hideLoadingIndicator();

                if (!response.ok) {
                    return response.text().then(errorText => {
                        throw new Error(errorText || 'Lỗi khi cập nhật hợp đồng');
                    });
                }
                return response.json();
            })
            .then(updatedContract => {
                this.showNotification('Cập nhật hợp đồng thành công', 'success');
                window.location.href = `/contracts/details/${contractId}`;
            })
            .catch(error => {
                console.error('Lỗi cập nhật hợp đồng:', error);
                this.showNotification(error.message, 'error');
            });
    },

    appendCccdFiles(formData, contractData) {
        // Thêm file CCCD người thuê
        if (contractData.tenantInfo.cccdFiles.front) {
            formData.append('tenantCccdFront', contractData.tenantInfo.cccdFiles.front);
        }
        if (contractData.tenantInfo.cccdFiles.back) {
            formData.append('tenantCccdBack', contractData.tenantInfo.cccdFiles.back);
        }

        // Thêm file CCCD chủ trọ
        if (contractData.ownerInfo.cccdFiles.front) {
            formData.append('ownerCccdFront', contractData.ownerInfo.cccdFiles.front);
        }
        if (contractData.ownerInfo.cccdFiles.back) {
            formData.append('ownerCccdBack', contractData.ownerInfo.cccdFiles.back);
        }
    },

    getInputValue(id) {
        const element = document.getElementById(id);
        return element ? (element.value || '').trim() : '';
    },

    getSelectText(id) {
        const select = document.getElementById(id);
        return select && select.selectedIndex >= 0
            ? select.options[select.selectedIndex].text
            : '';
    },

    showLoadingIndicator() {
        // Tạo và hiển thị loading spinner
        const loadingIndicator = document.createElement('div');
        loadingIndicator.id = 'contract-update-loading';
        loadingIndicator.innerHTML = `
        <div class="spinner-border text-primary" role="status">
            <span class="visually-hidden">Đang cập nhật...</span>
        </div>
    `;
        loadingIndicator.style.cssText = `
        position: fixed;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        z-index: 9999;
    `;
        document.body.appendChild(loadingIndicator);
    },

    hideLoadingIndicator() {
        const loadingIndicator = document.getElementById('contract-update-loading');
        if (loadingIndicator) {
            loadingIndicator.remove();
        }
    },

    showNotification(message, type = 'info') {
        const notification = document.createElement('div');
        notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`;
        notification.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 400px;';
        notification.innerHTML = `
        ${message}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
        document.body.appendChild(notification);

        // Tự động xóa thông báo sau 5 giây
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
    },

// Phương thức thu thập dữ liệu từ form
    collectContractData() {
        // Thu thập dữ liệu cho hợp đồng
        const contractData = {
            // Thông tin chung của hợp đồng
            contractDate: document.getElementById('contract-date')?.value || null,
            startDate: document.getElementById('start-date')?.value || null,
            endDate: document.getElementById('end-date')?.value || null,
            status: document.getElementById('contract-status')?.value || null,

            // Loại người thuê (nếu có radio button)
            tenantType: document.querySelector('input[name="tenantType"]:checked')?.value || null,

            // Thông tin người thuê
            tenant: this.collectTenantData(),

            // Thông tin chủ trọ
            owner: this.collectOwnerData(),

            // Thông tin phòng
            room: {
                roomId: document.getElementById('roomId')?.value || null,
                roomName: document.getElementById('room-name')?.value || null,
                roomNumber: document.getElementById('room-number')?.value || null,
                area: document.getElementById('room-area')?.value || null,
                hostelId: document.getElementById('hostelId')?.value || null,
                province: document.getElementById('room-province')?.value || null,
                district: document.getElementById('room-district')?.value || null,
                ward: document.getElementById('room-ward')?.value || null,
                street: document.getElementById('room-street')?.value || null,
                notes: document.getElementById('room-notes')?.value || null
            },

            // Điều khoản hợp đồng
            terms: {
                price: document.getElementById('rent-price')?.value || null,
                depositMonths: document.getElementById('deposit-months')?.value || null,
                paymentMethod: document.getElementById('payment-method')?.value || null,
                paymentDate: document.getElementById('payment-date')?.value || null,
                duration: document.getElementById('contract-duration')?.value || null,
                startDate: document.getElementById('start-date')?.value || null,
                additionalTerms: document.getElementById('terms-conditions')?.value || null
            },

            // Tiện ích
            utilities: this.collectUtilities(),

            // Người ở
            residents: this.collectResidents(),

            // Ảnh CCCD
            tenantCccdImages: this.collectCccdImages('tenant'),
            ownerCccdImages: this.collectCccdImages('owner')
        };

        return contractData;
    },

// Phương thức thu thập thông tin người thuê
    collectTenantData() {
        return {
            fullName: document.getElementById('tenant-name')?.value || null,
            birthday: document.getElementById('tenant-dob')?.value || null,
            cccdNumber: document.getElementById('tenant-id')?.value || null,
            phone: document.getElementById('tenant-phone')?.value || null,
            email: document.getElementById('tenant-email')?.value || null,
            issueDate: document.getElementById('tenant-id-date')?.value || null,
            issuePlace: document.getElementById('tenant-id-place')?.value || null,
            province: document.getElementById('tenant-province')?.value || null,
            district: document.getElementById('tenant-district')?.value || null,
            ward: document.getElementById('tenant-ward')?.value || null,
            street: document.getElementById('tenant-street')?.value || null
        };
    },

// Phương thức thu thập thông tin chủ trọ
    collectOwnerData() {
        return {
            fullName: document.getElementById('owner-name')?.value || null,
            birthday: document.getElementById('owner-dob')?.value || null,
            cccdNumber: document.getElementById('owner-id')?.value || null,
            phone: document.getElementById('owner-phone')?.value || null,
            email: document.getElementById('owner-email')?.value || null,
            issueDate: document.getElementById('owner-id-date')?.value || null,
            issuePlace: document.getElementById('owner-id-place')?.value || null,
            province: document.getElementById('owner-province')?.value || null,
            district: document.getElementById('owner-district')?.value || null,
            ward: document.getElementById('owner-ward')?.value || null,
            street: document.getElementById('owner-street')?.value || null
        };
    },

// Phương thức thu thập tiện ích
    collectUtilities() {
        const utilities = [];
        document.querySelectorAll('#amenities-list-host input[type="checkbox"]:checked').forEach(checkbox => {
            utilities.push(checkbox.id);
        });
        return utilities;
    },

// Phương thức thu thập người ở
    collectResidents() {
        const residents = [];
        const residentsList = document.getElementById('residents-list');

        // Kiểm tra nếu có người ở được thêm
        if (residentsList) {
            residentsList.querySelectorAll('.resident-item').forEach(residentElement => {
                residents.push({
                    name: residentElement.querySelector('.resident-name')?.textContent || null,
                    birthYear: residentElement.querySelector('.resident-birth-year')?.textContent || null,
                    phone: residentElement.querySelector('.resident-phone')?.textContent || null,
                    idNumber: residentElement.querySelector('.resident-id')?.textContent || null,
                    notes: residentElement.querySelector('.resident-notes')?.textContent || null
                });
            });
        }

        return residents;
    },

// Phương thức thu thập ảnh CCCD
    collectCccdImages(type) {
        const frontImage = document.getElementById(`${type}-cccd-front`);
        const backImage = document.getElementById(`${type}-cccd-back`);

        return {
            front: frontImage?.files[0] || null,
            back: backImage?.files[0] || null
        };
    },


// Thu thập thông tin người thuê đã đăng ký
    collectTenantData() {
        // Chỉ thu thập nếu là tenant registered
        if (document.getElementById('tenant-type-registered').checked) {
            return {
                cccd: document.getElementById('tenant-cccd').value,
                fullName: document.getElementById('tenant-name').value,
                phone: document.getElementById('tenant-phone').value,
                email: document.getElementById('tenant-email').value,
                // Các thông tin khác
            };
        }
        return null;
    },

// Thu thập thông tin người thuê chưa đăng ký
    collectUnregisteredTenantData() {
        // Chỉ thu thập nếu là tenant unregistered
        if (document.getElementById('tenant-type-unregistered').checked) {
            return {
                fullName: document.getElementById('unregistered-tenant-name').value,
                phone: document.getElementById('unregistered-tenant-phone').value,
                cccd: document.getElementById('unregistered-tenant-cccd').value,
                // Các thông tin khác
            };
        }
        return null;
    },


    saveContract() {
        try {
            const form = document.querySelector("form#contractForm");
            const roomSelect = document.getElementById("roomId");
            const hostelSelect = document.getElementById("hostelId");
            const tenantTypeSelect = document.getElementById("tenantType");
            const cccdFront = document.getElementById("cccd-front");
            const cccdBack = document.getElementById("cccd-back");
            const rentPrice = document.getElementById("rent-price");
            const depositMonths = document.getElementById("deposit-months");

            // Kiểm tra form và các phần tử chính
            if (!form || !roomSelect || !hostelSelect) {
                this.showNotification("Lỗi khởi tạo form! Vui lòng làm mới trang.", "error");
                return;
            }

            // Kiểm tra roomSelect và giá trị
            if (!roomSelect || !roomSelect.value) {
                this.showNotification("Vui lòng chọn phòng trước khi lưu hợp đồng!", "error");
                roomSelect?.focus();
                return;
            }

            // Kiểm tra hostelSelect
            if (!hostelSelect || !hostelSelect.value) {
                this.showNotification("Vui lòng chọn khu trọ trước khi lưu hợp đồng!", "error");
                hostelSelect?.focus();
                return;
            }

            // Kiểm tra tenantType
            const tenantType = tenantTypeSelect ? tenantTypeSelect.value : "REGISTERED";
            if (!tenantType || !["REGISTERED", "UNREGISTERED"].includes(tenantType)) {
                this.showNotification("Loại người thuê không hợp lệ!", "error");
                tenantTypeSelect?.focus();
                return;
            }

            // Kiểm tra trạng thái phòng
            const selectedOption = roomSelect.options[roomSelect.selectedIndex];
            if (selectedOption.dataset.status !== "unactive") {
                this.showNotification("Phòng này không khả dụng!", "error");
                roomSelect.value = "";
                this.clearRoomFields();
                roomSelect?.focus();
                return;
            }

            // Kiểm tra các trường bắt buộc
            const requiredFields = {
                "tenant-name": "Tên người thuê",
                "tenant-id": "CCCD người thuê",
                "tenant-phone": "Số điện thoại người thuê",
                "tenant-id-date": "Ngày cấp CCCD người thuê",
                "owner-name": "Tên chủ trọ",
                "owner-id": "CCCD chủ trọ",
                "owner-phone": "Số điện thoại chủ trọ",
                "owner-id-date": "Ngày cấp CCCD chủ trọ",
                "contract-date": "Ngày hợp đồng",
                "start-date": "Ngày bắt đầu",
                "contract-duration": "Thời hạn hợp đồng",
                "owner-province": "Tỉnh/Thành phố của chủ trọ",
                "rent-price": "Giá thuê",
                "deposit-months": "Số tháng đặt cọc"
            };

            for (const [fieldId, fieldName] of Object.entries(requiredFields)) {
                const field = document.getElementById(fieldId);
                if (!field || !field.value.trim() || (fieldId === "owner-province" && field.value === "Chọn Tỉnh/Thành phố")) {
                    this.showNotification(`${fieldName} không được để trống!`, "error");
                    field?.focus();
                    return;
                }
            }

            // Kiểm tra giá trị hợp lệ
            const priceValue = parseFloat(rentPrice.value);
            const depositMonthsValue = parseFloat(depositMonths.value);
            if (isNaN(priceValue) || priceValue <= 0) {
                this.showNotification("Giá thuê phải lớn hơn 0!", "error");
                rentPrice.focus();
                return;
            }
            if (isNaN(depositMonthsValue) || depositMonthsValue < 0) {
                this.showNotification("Số tháng đặt cọc phải lớn hơn hoặc bằng 0!", "error");
                depositMonths.focus();
                return;
            }

            // Tính tiền cọc
            const depositValue = priceValue * depositMonthsValue;

            // Tính ngày kết thúc
            const endDate = this.calculateEndDate();
            if (!endDate) {
                this.showNotification("Không thể tính ngày kết thúc hợp đồng!", "error");
                return;
            }

            // Tạo FormData để gửi dữ liệu
            const formData = new FormData(form);

            // Thêm các trường bổ sung từ this.selectedRoom
            formData.set("room.roomId", roomSelect.value);
            formData.set("tenantType", tenantType);
            formData.set("terms.endDate", endDate);
            if (this.selectedRoom) {
                formData.set("room.roomName", this.selectedRoom.roomName);
                formData.set("room.area", document.getElementById("room-area")?.value || "20");
                formData.set("room.price", this.selectedRoom.price);
                formData.set("room.status", this.selectedRoom.status);
                formData.set("room.hostelId", this.selectedRoom.hostelId);
                formData.set("room.hostelName", this.selectedRoom.hostelName);
                formData.set("room.address", document.getElementById("room-street")?.value || "");
            } else {
                this.showNotification("Không có thông tin phòng được chọn!", "error");
                return;
            }
            formData.set("terms.deposit", depositValue.toString());

            // Thêm file CCCD nếu có
            if (cccdFront && cccdFront.files[0]) {
                formData.set("cccdFront", cccdFront.files[0]);
            }
            if (cccdBack && cccdBack.files[0]) {
                formData.set("cccdBack", cccdBack.files[0]);
            }

            // Log dữ liệu FormData để kiểm tra
            console.log("Dữ liệu FormData:");
            for (const [key, value] of formData.entries()) {
                console.log(`${key}: ${value instanceof File ? value.name : value}`);
            }

    fetch("/api/contracts", {
      method: "POST",
      body: formData,
    })
      .then((response) => response.json())
      .then((data) => {
        if (data.success) {
          this.showNotification("Hợp đồng đã được lưu thành công!", "success")
          window.location.href = "/api/contracts/list"
        } else {
          this.showNotification(data.message || "Lỗi khi lưu hợp đồng!", "error")
        }
      })
      .catch((error) => {
        console.error("Error saving contract:", error)
        this.showNotification("Lỗi khi lưu hợp đồng: " + error.message, "error")
      })
  },

  // FIX: Thêm setup cho resident modal
  setupResidentModal() {
    const addResidentBtn = document.getElementById("btn-add-resident")
    const saveResidentBtn = document.getElementById("btn-save-resident")
    const residentForm = document.getElementById("addResidentForm")
    const modalElement = document.getElementById("addResidentModal")

    if (addResidentBtn && modalElement) {
      addResidentBtn.addEventListener("click", () => {
        console.log("Add resident button clicked")
        // Đảm bảo xóa backdrop cũ trước khi mở modal mới
        this.cleanupModalBackdrop()
        const modal = new bootstrap.Modal(modalElement)
        modal.show()
        if (residentForm) residentForm.reset()
      })
    }

    if (saveResidentBtn) {
      saveResidentBtn.addEventListener("click", () => {
        this.saveNewResident()
      })
    }

    // Thêm event listener để cleanup khi modal đóng
    if (modalElement) {
      modalElement.addEventListener("hidden.bs.modal", () => {
        this.cleanupModalBackdrop()
      })
    }
  },

  // FIX: Thêm hàm saveNewResident
  saveNewResident() {
    const residentName = document.getElementById("resident-name").value.trim()
    const residentBirthYear = document.getElementById("resident-birth-year").value
    const residentPhone = document.getElementById("resident-phone").value.trim()
    const residentId = document.getElementById("resident-id").value.trim()
    const residentNotes = document.getElementById("resident-notes").value.trim()

    if (!residentName || !residentBirthYear) {
      this.showNotification("Vui lòng nhập đầy đủ họ tên và năm sinh", "warning")
      return
    }

    // Add to residents array for preview
    this.addResidentToPreview()

    // Thêm resident vào danh sách UI
    this.addResidentToList({
      name: residentName,
      birthYear: residentBirthYear,
      phone: residentPhone,
      id: residentId,
      notes: residentNotes,
    })

    const modalElement = document.getElementById("addResidentModal")
    const modal = bootstrap.Modal.getInstance(modalElement)
    if (modal) {
      modal.hide()
    }
    // Đảm bảo cleanup sau khi đóng
    setTimeout(() => {
      this.cleanupModalBackdrop()
    }, 300)
    this.showNotification(`Đã thêm người ở "${residentName}" thành công!`, "success")
    this.updateResidentsCount()
  },

  // FIX: Thêm hàm addResidentToList
  addResidentToList(resident) {
    const residentsList = document.getElementById("residents-list")
    const noResidentsMessage = document.getElementById("no-residents-message")

    if (noResidentsMessage) {
      noResidentsMessage.style.display = "none"
    }

    const residentId = "resident-" + Date.now()
    const residentDiv = document.createElement("div")
    residentDiv.className = "nha-tro-resident-item card mb-2"
    residentDiv.innerHTML = `
            <div class="card-body p-3">
                <div class="d-flex justify-content-between align-items-start">
                    <div>
                        <h6 class="mb-1">${resident.name}</h6>
                        <small class="text-muted">Năm sinh: ${resident.birthYear}</small>
                        ${resident.phone ? `<br><small class="text-muted">SĐT: ${resident.phone}</small>` : ""}
                        ${resident.id ? `<br><small class="text-muted">CCCD: ${resident.id}</small>` : ""}
                        ${resident.notes ? `<br><small class="text-muted">Ghi chú: ${resident.notes}</small>` : ""}
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="NhaTroContract.removeResident('${residentId}')" title="Xóa người ở">
                        <i class="fa fa-times"></i>
                    </button>
                </div>
            </div>
        `
    residentDiv.id = residentId

    residentsList.appendChild(residentDiv)
  },

  // FIX: Thêm hàm removeResident
  removeResident(residentId) {
    const residentElement = document.getElementById(residentId)
    const residentName = residentElement.querySelector("h6").textContent

    if (confirm(`Bạn có chắc chắn muốn xóa người ở "${residentName}"?`)) {
      // Remove from residents array
      const residentIndex = this.residents.findIndex((r) => r.name === residentName)
      if (residentIndex > -1) {
        this.residents.splice(residentIndex, 1)
        this.updateResidentsPreview()
      }

      residentElement.remove()
      this.updateResidentsCount()
      this.showNotification(`Đã xóa người ở "${residentName}"`, "info")

      // Hiển thị lại thông báo nếu không còn resident nào
      const residentsList = document.getElementById("residents-list")
      const noResidentsMessage = document.getElementById("no-residents-message")
      if (residentsList.children.length === 1 && noResidentsMessage) {
        // 1 vì còn no-residents-message
        noResidentsMessage.style.display = "block"
      }
    }
  },

  // FIX: Thêm hàm updateResidentsCount
  updateResidentsCount() {
    const residentsList = document.getElementById("residents-list")
    const residentsCount = document.getElementById("residents-count")
    const noResidentsMessage = document.getElementById("no-residents-message")

    const count = residentsList.querySelectorAll(".nha-tro-resident-item").length
    if (residentsCount) {
      residentsCount.textContent = count
    }
  },

  setupAmenityModal() {
    const addAmenityBtn = document.getElementById("btn-add-amenity-host")
    const saveAmenityBtn = document.getElementById("saveAmenity-host")
    const amenityForm = document.getElementById("addAmenityForm-host")
    const amenityNameInput = document.getElementById("amenityName-host")
    const modalElement = document.getElementById("addAmenityModal-host")

    if (addAmenityBtn && modalElement) {
      addAmenityBtn.addEventListener("click", () => {
        console.log("Add amenity button clicked")
        // Đảm bảo xóa backdrop cũ trước khi mở modal mới
        this.cleanupModalBackdrop()
        const modal = new bootstrap.Modal(modalElement)
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

    // Thêm event listener để cleanup khi modal đóng
    if (modalElement) {
      modalElement.addEventListener("hidden.bs.modal", () => {
        this.cleanupModalBackdrop()
      })
    }
  },

  saveNewAmenity() {
    const amenityNameInput = document.getElementById("amenityName-host")
    const amenityName = amenityNameInput.value.trim()

    if (!amenityName) {
      this.showNotification("Vui lòng nhập tên tiện ích", "warning")
      amenityNameInput.focus()
      return
    }

    const existingAmenities = document.querySelectorAll("#amenities-list-host .form-check-label")
    const exists = Array.from(existingAmenities).some(
      (label) => label.textContent.toLowerCase() === amenityName.toLowerCase(),
    )

    if (exists) {
      this.showNotification("Tiện ích này đã tồn tại!", "warning")
      return
    }

    this.addAmenityToList(amenityName)
    const modalElement = document.getElementById("addAmenityModal-host")
    const modal = bootstrap.Modal.getInstance(modalElement)
    if (modal) {
      modal.hide()
    }
    // Đảm bảo cleanup sau khi đóng
    setTimeout(() => {
      this.cleanupModalBackdrop()
    }, 300)
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
            <button type="button" class="btn btn-sm btn-outline-danger nha-tro-host-remove-amenity" onclick="NhaTroContract.removeAmenity('${amenityId}')" title="Xóa tiện ích">
                <i class="fa fa-times"></i>
            </button>
        `

    amenitiesList.appendChild(amenityDiv)

    const newCheckbox = document.getElementById(amenityId)
    newCheckbox.addEventListener("change", () => {
      this.updateAmenities()
    })
  },

  removeAmenity(amenityId) {
    const amenityElement = document.getElementById(amenityId).closest(".nha-tro-host-custom-amenity")
    const amenityName = amenityElement.querySelector("label").textContent

    if (confirm(`Bạn có chắc chắn muốn xóa tiện ích "${amenityName}"?`)) {
      amenityElement.remove()
      this.updateAmenities()
      this.showNotification(`Đã xóa tiện ích "${amenityName}"`, "info")
    }
  },

  setupCustomerModal() {
    const addCustomerBtn = document.getElementById("btn-add-customer-host")
    const saveCustomerBtn = document.getElementById("saveCustomer-host")
    const customerForm = document.getElementById("addCustomerForm-host")
    const modalElement = document.getElementById("addCustomerModal-host")

    if (addCustomerBtn && modalElement) {
      addCustomerBtn.addEventListener("click", () => {
        console.log("Add customer button clicked")
        // Đảm bảo xóa backdrop cũ trước khi mở modal mới
        this.cleanupModalBackdrop()
        const modal = new bootstrap.Modal(modalElement)
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

    document.getElementById("newCustomer-cccd-front")?.addEventListener("change", (e) => {
      this.previewCustomerImage(e, "newCustomer-cccd-front-preview")
    })

    document.getElementById("newCustomer-cccd-back")?.addEventListener("change", (e) => {
      this.previewCustomerImage(e, "newCustomer-cccd-back-preview")
    })

    // Thêm event listener để cleanup khi modal đóng
    if (modalElement) {
      modalElement.addEventListener("hidden.bs.modal", () => {
        this.cleanupModalBackdrop()
      })
    }
  },

  cleanupModalBackdrop() {
    // Xóa tất cả backdrop còn sót lại
    const backdrops = document.querySelectorAll(".modal-backdrop")
    backdrops.forEach((backdrop) => backdrop.remove())

    // Khôi phục scroll cho body
    document.body.classList.remove("modal-open")
    document.body.style.overflow = ""
    document.body.style.paddingRight = ""

    // Xóa style inline có thể còn sót lại
    const modals = document.querySelectorAll(".modal")
    modals.forEach((modal) => {
      modal.style.display = ""
      modal.classList.remove("show")
      modal.setAttribute("aria-hidden", "true")
      modal.removeAttribute("aria-modal")
    })
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

        fetch("/api/contracts/add-unregistered-tenant", {
            method: "POST",
            body: formData,
        })
            .then((response) => response.json())
            .then((data) => {
                if (data.success) {
                    document.getElementById("tenant-name").value = data.tenant.fullName || "";
                    document.getElementById("tenant-phone").value = data.tenant.phone || "";
                    document.getElementById("tenant-id").value = data.tenant.cccdNumber || "";
                    document.getElementById("tenant-dob").value = data.tenant.birthday || "";
                    document.getElementById("tenant-id-date").value = data.tenant.issueDate || "";
                    document.getElementById("tenant-id-place").value = data.tenant.issuePlace || "";
                    document.getElementById("tenant-street").value = data.tenant.street || "";
                    document.getElementById("tenant-province").value = data.tenant.province || "";
                    this.loadDistricts(data.tenant.province, "tenant-district", "tenant-ward");
                    setTimeout(() => {
                        document.getElementById("tenant-district").value = data.tenant.district || "";
                        this.loadWards(data.tenant.district, "tenant-ward", data.tenant.province);
                        setTimeout(() => {
                            document.getElementById("tenant-ward").value = data.tenant.ward || "";
                            this.updateAddress("tenant");
                        }, 200);
                    }, 200);

                    const modalElement = document.getElementById("addCustomerModal-host");
                    const modal = bootstrap.Modal.getInstance(modalElement);
                    if (modal) {
                        modal.hide();
                    }
                    setTimeout(() => {
                        this.cleanupModalBackdrop();
                    }, 300);

                    document.getElementById("tenantType").value = "UNREGISTERED";
                    this.toggleTenantFields();
                    this.showNotification("Đã thêm thông tin người thuê thành công!", "success");
                } else {
                    this.showNotification(data.message || "Lỗi khi thêm người thuê!", "error");
                }
            })
            .catch((error) => {
                console.error("Error saving unregistered tenant:", error);
                this.showNotification("Lỗi khi thêm người thuê: " + error.message, "error");
            });
    },

  previewCustomerImage(event, previewId) {
    const file = event.target.files[0]
    const preview = document.getElementById(previewId)
    const uploadContainer = preview.closest(".nha-tro-image-upload")

    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        preview.innerHTML = `<img src="${e.target.result}" alt="Ảnh CCCD" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
        uploadContainer.classList.add("has-image")
      }
      reader.readAsDataURL(file)
    }
  },

  clearCustomerFormImages() {
    document.getElementById("newCustomer-cccd-front-preview").innerHTML = `
            <i class="fa fa-camera fa-2x"></i>
            <div class="mt-2">Tải ảnh mặt trước</div>
            <small class="text-muted">Nhấn để chọn ảnh</small>
        `
    document.getElementById("newCustomer-cccd-back-preview").innerHTML = `
            <i class="fa fa-camera fa-2x"></i>
            <div class="mt-2">Tải ảnh mặt sau</div>
            <small class="text-muted">Nhấn để chọn ảnh</small>
        `
    document.querySelectorAll("#addCustomerModal-host .nha-tro-image-upload").forEach((container) => {
      container.classList.remove("has-image")
    })
  },

  // FIX: Thêm hàm setupPreviewListeners
  setupPreviewListeners() {
    // Setup listeners for preview updates
    const previewFields = [
      { input: "tenant-name", preview: "preview-tenant-name" },
      { input: "tenant-dob", preview: "preview-tenant-dob" },
      { input: "tenant-id", preview: "preview-tenant-id" },
      { input: "tenant-id-date", preview: "preview-tenant-id-date" },
      { input: "tenant-id-place", preview: "preview-tenant-id-place" },
      { input: "tenant-phone", preview: "preview-tenant-phone" },
      { input: "rent-price", preview: "preview-rent" },
      { input: "payment-date", preview: "preview-payment-date" },
      { input: "contract-duration", preview: "preview-duration" },
      { input: "deposit-months", preview: "preview-deposit-months" },
    ]

    previewFields.forEach((field) => {
      const input = document.getElementById(field.input)
      if (input) {
        input.addEventListener("input", () => {
          this.updatePreviewField(field.input, field.preview)
        })
      }
    })

    // Setup amenity checkboxes
    document.querySelectorAll('.nha-tro-amenities input[type="checkbox"]').forEach((checkbox) => {
      checkbox.addEventListener("change", () => {
        this.updateAmenities()
      })
    })
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
    }, 10000)
  },
}

document.addEventListener("DOMContentLoaded", () => {
  window.NhaTroContract.init()
})
/* ]]> */
