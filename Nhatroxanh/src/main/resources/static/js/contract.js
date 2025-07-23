/* <![CDATA[ */
window.NhaTroContract = {
    currentTab: "tenantInfo",
    zoomLevel: 1,
    residents: [],
    contractTerms: [], // New array to store individual terms


    init() {
        console.log("🚀 Contract form loading...");
        this.setupEventListeners();
        this.setupTermsManagement();
        this.setCurrentDate();
        this.setupAmenityModal();
        this.setupCustomerModal();
        this.setupResidentModal();
        this.initializePreviewUpdates();

        return this.loadProvinces() // Load provinces ngay từ đầu
            .then(() => {
                console.log("Provinces loaded successfully");
                const contractId = window.location.pathname.split('/').pop();
                console.log("Contract ID from URL:", contractId);

                if (contractId) {
                    return fetch(`/api/contracts/edit-data/${contractId}`, {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json',
                            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ""
                        }
                    })
                        .then(response => {
                            console.log("Response status:", response.status);
                            if (!response.ok) {
                                return response.text().then(text => {
                                    console.error("Response text:", text);
                                    throw new Error(`HTTP error! status: ${response.status}, response: ${text}`);
                                });
                            }
                            return response.json();
                        })
                        .then(contract => {
                            console.log("Contract data:", JSON.stringify(contract, null, 2));
                            window.contractData = contract; // Lưu dữ liệu toàn cục
                            console.log("🚀 Contract data saved, waiting for tab activation");
                            console.log("🚀 Contract form initialized");
                        })
                        .catch(error => {
                            console.error("Error fetching contract data:", error);
                            this.showNotification("Lỗi khi tải dữ liệu hợp đồng: " + error.message, "error");
                        });
                }
            })
            .catch(error => {
                console.error("Error loading provinces:", error);
                this.showNotification("Lỗi khi tải danh sách tỉnh/thành phố: " + error.message, "error");
            });
    },

    // New method to setup terms management
    setupTermsManagement() {
        const addTermBtn = document.getElementById("btn-add-term")
        const newTermInput = document.getElementById("new-term-input")

        if (addTermBtn) {
            addTermBtn.addEventListener("click", () => {
                this.addNewTerm()
            })
        }

        if (newTermInput) {
            // Allow adding term with Enter key (Ctrl+Enter for new line)
            newTermInput.addEventListener("keydown", (e) => {
                if (e.key === "Enter" && !e.ctrlKey && !e.shiftKey) {
                    e.preventDefault()
                    this.addNewTerm()
                }
            })
        }
    },

    // New method to add a term
    addNewTerm() {
        const newTermInput = document.getElementById("new-term-input")
        const termText = newTermInput.value.trim()

        if (!termText) {
            this.showNotification("Vui lòng nhập nội dung điều khoản", "warning")
            newTermInput.focus()
            return
        }

        // Add term to array
        const newTerm = {
            id: Date.now(),
            text: termText,
            order: this.contractTerms.length + 1,
        }

        this.contractTerms.push(newTerm)

        // Clear input
        newTermInput.value = ""

        // Update UI
        this.updateTermsList()
        this.updateTermsPreview()
        this.updateTermsCount()

        // Show success message
        this.showNotification(`Đã thêm điều khoản thứ ${newTerm.order}`, "success")

        // Focus back to input for easy adding more terms
        newTermInput.focus()
    },

    // New method to update terms list in UI
    updateTermsList() {
        const termsList = document.getElementById("terms-list")
        const emptyMessage = document.getElementById("empty-terms-message")

        if (this.contractTerms.length === 0) {
            emptyMessage.style.display = "block"
            return
        }

        emptyMessage.style.display = "none"

        // Clear existing terms (except empty message)
        const existingTerms = termsList.querySelectorAll(".term-item")
        existingTerms.forEach((term) => term.remove())

        // Add all terms
        this.contractTerms.forEach((term, index) => {
            const termElement = this.createTermElement(term, index + 1)
            termsList.appendChild(termElement)
        })
    },

    // New method to create term element
    createTermElement(term, displayOrder) {
        const termDiv = document.createElement("div")
        termDiv.className = "term-item"
        termDiv.dataset.termId = term.id

        termDiv.innerHTML = `
      <div class="term-content">
        <span class="term-number">${displayOrder}.</span>
        <span class="term-text">${this.escapeHtml(term.text)}</span>
      </div>
      <div class="term-actions">
        <button type="button" class="btn btn-outline-primary btn-term-action" 
                onclick="NhaTroContract.editTerm(${term.id})" title="Sửa điều khoản">
          <i class="fa fa-edit"></i>
        </button>
        <button type="button" class="btn btn-outline-danger btn-term-action" 
                onclick="NhaTroContract.removeTerm(${term.id})" title="Xóa điều khoản">
          <i class="fa fa-trash"></i>
        </button>
      </div>
    `

        return termDiv
    },

    // New method to edit term
    editTerm(termId) {
        const term = this.contractTerms.find((t) => t.id === termId)
        if (!term) return

        const newText = prompt("Sửa điều khoản:", term.text)
        if (newText !== null && newText.trim() !== "") {
            term.text = newText.trim()
            this.updateTermsList()
            this.updateTermsPreview()
            this.showNotification("Đã cập nhật điều khoản", "success")
        }
    },

    // New method to remove term
    removeTerm(termId) {
        const term = this.contractTerms.find((t) => t.id === termId)
        if (!term) return

        if (
            confirm(
                `Bạn có chắc chắn muốn xóa điều khoản: "${term.text.substring(0, 50)}${term.text.length > 50 ? "..." : ""}"?`,
            )
        ) {
            this.contractTerms = this.contractTerms.filter((t) => t.id !== termId)

            // Reorder remaining terms
            this.contractTerms.forEach((t, index) => {
                t.order = index + 1
            })

            this.updateTermsList()
            this.updateTermsPreview()
            this.updateTermsCount()
            this.showNotification("Đã xóa điều khoản", "info")
        }
    },

    // New method to update terms count
    updateTermsCount() {
        const termsCount = document.getElementById("terms-count")
        if (termsCount) {
            termsCount.textContent = this.contractTerms.length
        }
    },

    // New method to update terms preview
    updateTermsPreview() {
        const previewTermsList = document.getElementById("preview-terms-list")

        if (!previewTermsList) return

        if (this.contractTerms.length === 0) {
            previewTermsList.innerHTML = '<p class="text-muted fst-italic">Chưa có điều khoản nào được thêm</p>'
            return
        }

        let termsHtml = ""
        this.contractTerms.forEach((term, index) => {
            termsHtml += `<p><strong>6.${index + 1}.</strong> ${this.escapeHtml(term.text)}</p>`
        })

        previewTermsList.innerHTML = termsHtml
    },

    // Helper method to escape HTML
    escapeHtml(text) {
        const div = document.createElement("div")
        div.textContent = text
        return div.innerHTML
    },

    // Add preview updates initialization
    initializePreviewUpdates() {
        // Update preview when payment method changes
        document.getElementById("payment-method")?.addEventListener("change", (e) => {
            this.updatePaymentMethodPreview(e.target.value)
        })

        // Add form field listeners for real-time preview updates
        this.addFormFieldListeners()
    },

    // Add resident to preview functionality
    addResidentToPreview() {
        const residentName = document.getElementById("resident-name")?.value
        const residentBirthYear = document.getElementById("resident-birth-year")?.value
        const residentPhone = document.getElementById("resident-phone")?.value
        const residentId = document.getElementById("resident-id")?.value

        if (residentName && residentBirthYear) {
            // Add to residents array
            const resident = {
                name: residentName,
                birthYear: residentBirthYear,
                phone: residentPhone || "",
                id: residentId || "",
            }

            this.residents.push(resident)

            // Update residents count
            const residentsCount = document.getElementById("residents-count")
            if (residentsCount) {
                residentsCount.textContent = this.residents.length
            }

            // Update preview
            this.updateResidentsPreview()
        }
    },

    // Update residents in preview
    updateResidentsPreview() {
        const previewSection = document.getElementById("preview-residents-section")
        const previewResidents = document.getElementById("preview-residents")

        if (this.residents.length > 0) {
            if (previewSection) previewSection.style.display = "block"

            const residentsText = this.residents
                .map((resident) => {
                    let info = resident.name
                    if (resident.birthYear) info += ` (${resident.birthYear})`
                    if (resident.phone) info += ` - SĐT: ${resident.phone}`
                    if (resident.id) info += ` - CCCD: ${resident.id}`
                    return info
                })
                .join("; ")

            if (previewResidents) {
                previewResidents.textContent = residentsText
            }
        } else {
            if (previewSection) previewSection.style.display = "none"
        }
    },

    // Update payment method in preview
    updatePaymentMethodPreview(paymentMethod) {
        const previewElement = document.getElementById("preview-payment-method")
        if (previewElement) {
            let methodText = ""
            switch (paymentMethod) {
                case "cash":
                    methodText = "tiền mặt"
                    break
                case "transfer":
                    methodText = "chuyển khoản"
                    break
                default:
                    methodText = "........................"
            }
            previewElement.textContent = methodText
        }
    },

    loadCccdImages() {
        const cccdNumber = document.getElementById('cccd-number').value;
        if (!cccdNumber) {
            this.showNotification("Số CCCD không được để trống!", "error");
            return;
        }

        fetch(`/api/contracts/cccd-images?cccdNumber=${encodeURIComponent(cccdNumber)}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const frontPreview = document.getElementById('cccd-front-preview');
                    if (data.cccdFrontUrl) {
                        frontPreview.innerHTML = `<img src="${data.cccdFrontUrl}" alt="Ảnh CCCD mặt trước">`;
                    } else {
                        frontPreview.innerHTML = `
                    <i class="fa fa-camera fa-2x"></i>
                    <div class="mt-2">Tải ảnh mặt trước</div>
                `;
                    }

                    const backPreview = document.getElementById('cccd-back-preview');
                    if (data.cccdBackUrl) {
                        backPreview.innerHTML = `<img src="${data.cccdBackUrl}" alt="Ảnh CCCD mặt sau">`;
                    } else {
                        backPreview.innerHTML = `
                    <i class="fa fa-camera fa-2x"></i>
                    <div class="mt-2">Tải ảnh mặt sau</div>
                `;
                    }

                    this.showNotification("Lấy ảnh CCCD thành công!", "success");
                } else {
                    this.showNotification(data.message || "Lỗi khi lấy ảnh CCCD!", "error");
                }
            })
            .catch(error => {
                console.error("Lỗi khi lấy ảnh CCCD:", error);
                this.showNotification("Lỗi khi lấy ảnh CCCD: " + error.message, "error");
            });
    },


    saveCccdImages() {
        const cccdNumber = document.getElementById('cccd-number').value;
        if (!cccdNumber) {
            this.showNotification("Số CCCD không được để trống!", "error");
            return;
        }

        const formData = new FormData();
        const cccdFront = document.getElementById('cccd-front').files[0];
        const cccdBack = document.getElementById('cccd-back').files[0];
        if (cccdFront) formData.append("cccdFront", cccdFront);
        if (cccdBack) formData.append("cccdBack", cccdBack);
        formData.append("cccdNumber", cccdNumber);

        fetch("/api/contracts/upload-cccd", {
            method: "POST",
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    const frontPreview = document.getElementById('cccd-front-preview');
                    const backPreview = document.getElementById('cccd-back-preview');
                    if (data.cccdFrontUrl) {
                        frontPreview.innerHTML = `<img src="${data.cccdFrontUrl}" alt="Ảnh CCCD mặt trước">`;
                    }
                    if (data.cccdBackUrl) {
                        backPreview.innerHTML = `<img src="${data.cccdBackUrl}" alt="Ảnh CCCD mặt sau">`;
                    }
                    this.showNotification("Lưu ảnh CCCD mới thành công!", "success");
                } else {
                    this.showNotification(data.message || "Lỗi khi lưu ảnh CCCD!", "error");
                }
            })
            .catch(error => {
                console.error("Lỗi khi lưu ảnh CCCD:", error);
                this.showNotification("Lỗi khi lưu ảnh CCCD: " + error.message, "error");
            });
    },




    // Add comprehensive form field listeners
    addFormFieldListeners() {
        // Update tenant info in preview
        document.getElementById("tenant-name")?.addEventListener("input", (e) => {
            const previewName = document.getElementById("preview-tenant-name")
            const previewSignature = document.getElementById("preview-tenant-signature")
            const value = e.target.value || "........................"
            if (previewName) previewName.textContent = value
            if (previewSignature) previewSignature.textContent = value
        })

        document.getElementById("tenant-phone")?.addEventListener("input", (e) => {
            const preview = document.getElementById("preview-tenant-phone")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        document.getElementById("tenant-dob")?.addEventListener("change", (e) => {
            const preview = document.getElementById("preview-tenant-dob")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        document.getElementById("tenant-id")?.addEventListener("input", (e) => {
            const preview = document.getElementById("preview-tenant-id")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        document.getElementById("tenant-id-date")?.addEventListener("change", (e) => {
            const preview = document.getElementById("preview-tenant-id-date")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        document.getElementById("tenant-id-place")?.addEventListener("input", (e) => {
            const preview = document.getElementById("preview-tenant-id-place")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        // Update address preview
        const updateTenantAddress = () => {
            const street = document.getElementById("tenant-street")?.value || ""
            const wardSelect = document.getElementById("tenant-ward")
            const districtSelect = document.getElementById("tenant-district")
            const provinceSelect = document.getElementById("tenant-province")

            const ward = wardSelect?.selectedOptions[0]?.text || ""
            const district = districtSelect?.selectedOptions[0]?.text || ""
            const province = provinceSelect?.selectedOptions[0]?.text || ""

            const addressParts = [street, ward, district, province].filter(
                (item) => item && item !== "Chọn Tỉnh/Thành phố" && item !== "Chọn Quận/Huyện" && item !== "Chọn Phường/Xã",
            )

            const address = addressParts.join(", ")
            const preview = document.getElementById("preview-tenant-address")
            if (preview) preview.textContent = address || "........................"
        }

        document.getElementById("tenant-street")?.addEventListener("input", updateTenantAddress)
        document.getElementById("tenant-ward")?.addEventListener("change", updateTenantAddress)
        document.getElementById("tenant-district")?.addEventListener("change", updateTenantAddress)
        document.getElementById("tenant-province")?.addEventListener("change", updateTenantAddress)

        // Update contract terms in preview
        document.getElementById("rent-price")?.addEventListener("input", (e) => {
            const price = e.target.value ? new Intl.NumberFormat("vi-VN").format(e.target.value) : "........................"
            const preview = document.getElementById("preview-rent")
            if (preview) preview.textContent = price
        })

        document.getElementById("payment-date")?.addEventListener("input", (e) => {
            const preview = document.getElementById("preview-payment-date")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        document.getElementById("contract-duration")?.addEventListener("input", (e) => {
            const preview = document.getElementById("preview-duration")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        document.getElementById("start-date")?.addEventListener("change", (e) => {
            const preview = document.getElementById("preview-start-date")
            if (preview) preview.textContent = e.target.value || "........................"

            // Calculate end date
            if (e.target.value) {
                const startDate = new Date(e.target.value)
                const duration = Number.parseInt(document.getElementById("contract-duration")?.value || 0)
                if (duration > 0) {
                    const endDate = new Date(startDate)
                    endDate.setMonth(endDate.getMonth() + duration)
                    const endPreview = document.getElementById("preview-end-date")
                    if (endPreview) endPreview.textContent = endDate.toISOString().split("T")[0]
                }
            }
        })

        document.getElementById("deposit-months")?.addEventListener("input", (e) => {
            const months = e.target.value || 0
            const rentPrice = document.getElementById("rent-price")?.value || 0
            const deposit = months * rentPrice

            const monthsPreview = document.getElementById("preview-deposit-months")
            const depositPreview = document.getElementById("preview-deposit")

            if (monthsPreview) monthsPreview.textContent = months
            if (depositPreview) {
                depositPreview.textContent = deposit
                    ? new Intl.NumberFormat("vi-VN").format(deposit)
                    : "........................"
            }
        })

        document.getElementById("contract-date")?.addEventListener("change", (e) => {
            const preview = document.getElementById("preview-sign-date")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        // Update room info in preview
        document.getElementById("roomId")?.addEventListener("change", (e) => {
            const selectedOption = e.target.selectedOptions[0]
            if (selectedOption) {
                const roomText = selectedOption.text
                // Extract room info from the option text
                const roomMatch = roomText.match(/^(.+?)\s*$$(.+)$$$/)
                if (roomMatch) {
                    const roomNumberPreview = document.getElementById("preview-room-number")
                    const roomAddressPreview = document.getElementById("preview-room-address")
                    if (roomNumberPreview) roomNumberPreview.textContent = roomMatch[1]
                    if (roomAddressPreview) roomAddressPreview.textContent = roomMatch[2]
                }
            }
        })

        document.getElementById("room-area")?.addEventListener("input", (e) => {
            const preview = document.getElementById("preview-room-area")
            if (preview) preview.textContent = e.target.value || "........................"
        })

        // Update amenities preview
        const updateAmenitiesPreview = () => {
            const amenities = []
            document.querySelectorAll('#amenities-list-host input[type="checkbox"]:checked').forEach((checkbox) => {
                const label = document.querySelector(`label[for="${checkbox.id}"]`)
                if (label) {
                    amenities.push(label.textContent)
                }
            })
            const preview = document.getElementById("preview-amenities")
            if (preview) preview.textContent = amenities.join(", ") || "........................"
        }

        document.querySelectorAll('#amenities-list-host input[type="checkbox"]').forEach((checkbox) => {
            checkbox.addEventListener("change", updateAmenitiesPreview)
        })
    },

    safeEncodeURL(value) {
        try {
            return encodeURIComponent(value).replace(/%25/g, "%")
        } catch (e) {
            console.error("Error encoding URL component:", value, e)
            return value
        }
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
        document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
            link.addEventListener("click", (e) => {
                e.preventDefault();
                const tabId = link.getAttribute("data-tab");
                console.log(`Switching to tab: ${tabId}`);
                this.showTab(tabId);

                // Kiểm tra tab đã active trước khi điền dữ liệu
                const targetTab = document.getElementById(tabId);
                if (targetTab && targetTab.classList.contains("active")) {
                    setTimeout(() => {
                        if (window.contractData) {
                            if (tabId === "tenantInfo") {
                                if (window.contractData.tenantType === "REGISTERED" && window.contractData.tenant) {
                                    console.log("Filling tenant fields...");
                                    this.fillTenantFields(window.contractData.tenant);
                                } else if (window.contractData.tenantType === "UNREGISTERED" && window.contractData.unregisteredTenant) {
                                    console.log("Filling unregistered tenant fields...");
                                    this.fillTenantFields(window.contractData.unregisteredTenant);
                                }
                            } else if (tabId === "ownerInfo" && window.contractData.owner) {
                                console.log("Filling owner fields...");
                                this.fillOwnerFields(window.contractData.owner);
                            } else if (tabId === "roomInfo" && window.contractData.room) {
                                console.log("Filling room fields...");
                                this.fillRoomFields(window.contractData.room);
                            }
                        } else {
                            console.warn("No contract data available for tab:", tabId);
                        }
                    }, 300); // Tăng lên 300ms
                } else {
                    console.warn(`Tab ${tabId} not active yet, skipping fill`);
                }
            });
        });

        const updateBtn = document.getElementById("btn-update");
        if (updateBtn) {
            updateBtn.addEventListener("click", (e) => {
                e.preventDefault();
                const contractId = updateBtn.dataset.contractId || window.location.pathname.split('/').pop();
                console.log("Raw contractId:", contractId);
                if (!contractId) {
                    this.showNotification("Không tìm thấy ID hợp đồng để cập nhật!", "error");
                    return;
                }
                const parsedId = parseInt(contractId, 10);
                console.log("Parsed contractId:", parsedId);
                console.log("Is NaN:", isNaN(parsedId));
                if (isNaN(parsedId)) {
                    this.showNotification("ID hợp đồng không hợp lệ!", "error");
                    return;
                }
                console.log("Updating contract with ID:", parsedId);
                this.editContract(parsedId);
            });
        }

        document.getElementById("btn-next-owner")?.addEventListener("click", () => {
            console.log("Next to ownerInfo tab...");
            this.showTab("ownerInfo");
            setTimeout(() => {
                if (window.contractData && window.contractData.owner) {
                    this.fillOwnerFields(window.contractData.owner);
                }
            }, 300);
        });
        document.getElementById("btn-prev-tenant")?.addEventListener("click", () => {
            console.log("Prev to tenantInfo tab...");
            this.showTab("tenantInfo");
            setTimeout(() => {
                if (window.contractData) {
                    if (window.contractData.tenantType === "REGISTERED" && window.contractData.tenant) {
                        this.fillTenantFields(window.contractData.tenant);
                    } else if (window.contractData.tenantType === "UNREGISTERED" && window.contractData.unregisteredTenant) {
                        this.fillTenantFields(window.contractData.unregisteredTenant);
                    }
                }
            }, 300);
        });
        document.getElementById("btn-next-room")?.addEventListener("click", () => {
            console.log("Next to roomInfo tab...");
            this.showTab("roomInfo");
            setTimeout(() => {
                if (window.contractData && window.contractData.room) {
                    this.fillRoomFields(window.contractData.room);
                }
            }, 300);
        });
        document.getElementById("btn-prev-owner")?.addEventListener("click", () => {
            console.log("Prev to ownerInfo tab...");
            this.showTab("ownerInfo");
            setTimeout(() => {
                if (window.contractData && window.contractData.owner) {
                    this.fillOwnerFields(window.contractData.owner);
                }
            }, 300);
        });
        document.getElementById("btn-next-terms")?.addEventListener("click", () => {
            console.log("Next to terms tab...");
            this.showTab("terms");
        });
        document.getElementById("btn-prev-room")?.addEventListener("click", () => {
            console.log("Prev to roomInfo tab...");
            this.showTab("roomInfo");
            setTimeout(() => {
                if (window.contractData && window.contractData.room) {
                    this.fillRoomFields(window.contractData.room);
                }
            }, 300);
        });

        document.getElementById("btn-print")?.addEventListener("click", () => this.printContract());
        document.getElementById("btn-save")?.addEventListener("click", (e) => {
            e.preventDefault();
            this.saveContract();
        });

        document.getElementById("btn-zoom-in")?.addEventListener("click", () => this.zoomIn());
        document.getElementById("btn-zoom-out")?.addEventListener("click", () => this.zoomOut());
        document.getElementById("btn-reset-zoom")?.addEventListener("click", () => this.resetZoom());

        document.getElementById("cccd-front")?.addEventListener("change", (e) => {
            this.previewImage(e, "cccd-front-preview");
        });
        document.getElementById("cccd-back")?.addEventListener("change", (e) => {
            this.previewImage(e, "cccd-back-preview");
        });

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

        const hostelSelect = document.getElementById("hostelSelect");
        if (hostelSelect) {
            hostelSelect.addEventListener("change", () => this.filterRooms());
        }

        const roomSelect = document.getElementById("roomSelect");
        if (roomSelect) {
            roomSelect.addEventListener("change", () => this.onRoomSelected());
        }

        const tenantTypeSelect = document.getElementById("tenantType");
        if (tenantTypeSelect) {
            tenantTypeSelect.addEventListener("change", () => this.toggleTenantFields());
        }

        this.setupPreviewListeners();
        this.setupLocationListeners();
    },

    editContract(contractId) {
        console.log("=== EDIT CONTRACT ===");
        console.log("ID:", contractId);

        const parsedId = parseInt(contractId, 10);
        if (isNaN(parsedId) || parsedId <= 0) {
            this.showNotification("ID hợp đồng không hợp lệ!", "error");
            return;
        }

        const roomSelect = document.getElementById('roomSelect');
        const roomIdValue = roomSelect?.value;
        const roomIdNumber = parseInt(roomIdValue, 10);

        // Chỉ validate room nếu user chọn thay đổi
        if (roomIdValue && (isNaN(roomIdNumber) || roomIdNumber <= 0)) {
            this.showNotification("Vui lòng chọn phòng trọ hợp lệ!", "error");
            return;
        }

        // Gọi uploadCccd để cập nhật ảnh trước
        this.uploadCccd(parsedId).then(() => {
            const contractData = this.buildContractData(roomIdNumber, roomSelect);
            contractData.id = parsedId;

            console.log("Data gửi:", JSON.stringify(contractData, null, 2));

            fetch(`/api/contracts/update/${parsedId}`, {
                method: "PUT",
                headers: {
                    'Content-Type': 'application/json',
                    'Accept': 'application/json',
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ""
                },
                body: JSON.stringify(contractData)
            })
                .then(response => {
                    console.log("Response:", response.status);
                    return response.json();
                })
                .then(data => {
                    if (data.success) {
                        this.showNotification("Cập nhật thành công!", "success");
                        setTimeout(() => window.location.href = "/api/contracts/list", 1500);
                    } else {
                        this.showNotification(data.message || "Lỗi cập nhật!", "error");
                    }
                })
                .catch(error => {
                    console.error("Error:", error);
                    this.showNotification("Lỗi kết nối: " + error.message, "error");
                });
        }).catch(error => {
            console.error("Error uploading CCCD:", error);
            this.showNotification("Lỗi khi cập nhật ảnh: " + error.message, "error");
        });
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
        const roomSelect = document.getElementById("roomSelect") // Đổi từ roomId thành roomSelect
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
            })
            const data = await response.json()
            console.log("Room details received:", data)

            if (data.success && data.room) {
                const room = data.room

                // Cập nhật các field
                document.getElementById("room-number").value = room.namerooms || selectedOption.text.split(" (")[0] || ""
                document.getElementById("room-area").value = room.acreage || ""
                document.getElementById("rent-price").value = room.price || ""

                // ✅ QUAN TRỌNG: Cập nhật địa chỉ vào preview ngay lập tức
                let roomAddress = room.address
                if (!roomAddress && selectedOption.text.includes("(")) {
                    roomAddress = selectedOption.text.split(" (")[1].replace(")", "")
                }

                console.log("Room address for preview:", roomAddress)

                // ✅ CẬP NHẬT PREVIEW NGAY
                this.updatePreviewField("room-number", "preview-room-number")
                this.updatePreviewField("room-area", "preview-room-area")
                this.updatePreviewField("rent-price", "preview-rent")

                // ✅ CẬP NHẬT ĐỊA CHỈ PHÒNG VÀO PREVIEW
                const previewRoomAddress = document.getElementById("preview-room-address")
                if (previewRoomAddress && roomAddress) {
                    previewRoomAddress.textContent = roomAddress
                    console.log("✅ Updated preview-room-address:", roomAddress)
                }

                // Xử lý địa chỉ cho form (nếu cần)
                if (roomAddress) {
                    const addressParts = roomAddress.split(", ")
                    const street = addressParts.length > 0 ? addressParts[0].trim() : ""
                    const ward = addressParts.length > 1 ? addressParts[1].trim() : ""
                    const district = addressParts.length > 2 ? addressParts[2].trim() : ""
                    const province = addressParts.length > 3 ? addressParts[3].trim() : ""

                    document.getElementById("room-street").value = street

                    // Load địa chỉ vào dropdown (nếu cần)
                    const provinceSelect = document.getElementById("room-province")
                    if (provinceSelect && province) {
                        const provinceCode = await this.mapProvinceNameToCode(province)
                        if (provinceCode) {
                            const provinceOption = provinceSelect.querySelector(`option[value="${provinceCode}"]`)
                            if (provinceOption) {
                                provinceSelect.value = provinceCode
                                await this.loadDistricts(provinceCode, "room-district", "room-ward")

                                // Load district
                                const districtSelect = document.getElementById("room-district")
                                if (districtSelect && district) {
                                    const districtCode = await this.mapDistrictNameToCode(provinceCode, district)
                                    if (districtCode) {
                                        const districtOption = districtSelect.querySelector(`option[value="${districtCode}"]`)
                                        if (districtOption) {
                                            districtSelect.value = districtCode
                                            await this.loadWards(districtCode, "room-ward")

                                            // Load ward
                                            const wardSelect = document.getElementById("room-ward")
                                            if (wardSelect && ward) {
                                                const wardCode = await this.mapWardNameToCode(districtCode, ward, provinceCode)
                                                if (wardCode) {
                                                    const wardOption = wardSelect.querySelector(`option[value="${wardCode}"]`)
                                                    if (wardOption) {
                                                        wardSelect.value = wardCode
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

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
    async uploadCccd(contractId) {
        const cccdFront = document.getElementById('cccd-front')?.files[0];
        const cccdBack = document.getElementById('cccd-back')?.files[0];
        const cccdNumber = document.getElementById('tenant-id')?.value;

        // Kiểm tra điều kiện bắt buộc
        if (!cccdNumber || !cccdNumber.match(/^\d{12}$/)) {
            throw new Error("Số CCCD phải là 12 chữ số hợp lệ!");
        }

        // Kiểm tra định dạng file
        const validTypes = ['image/jpeg', 'image/png'];
        if (cccdFront && !validTypes.includes(cccdFront.type)) {
            throw new Error("Ảnh mặt trước phải là file .jpg hoặc .png!");
        }
        if (cccdBack && !validTypes.includes(cccdBack.type)) {
            throw new Error("Ảnh mặt sau phải là file .jpg hoặc .png!");
        }

        // Nếu không có ảnh mới, bỏ qua upload
        if (!cccdFront && !cccdBack) {
            console.log("Không có ảnh mới được chọn, bỏ qua upload.");
            return; // Resolve promise để tiếp tục editContract
        }

        const formData = new FormData();
        formData.append('cccdNumber', cccdNumber);
        if (cccdFront) formData.append('cccdFront', cccdFront);
        if (cccdBack) formData.append('cccdBack', cccdBack);

        try {
            const response = await fetch('/api/contracts/update-cccd-image', { // Sửa đường dẫn
                method: 'POST',
                body: formData,
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ""
                }
            });

            if (!response.ok) {
                const errorText = await response.text();
                console.error("Server error response:", errorText);
                if (response.status === 405) {
                    throw new Error(`Phương thức không được hỗ trợ. Status: ${response.status} - ${errorText}`);
                }
                throw new Error(`Lỗi server: ${response.status} - ${errorText}`);
            }

            const data = await response.json();
            if (!data.success) {
                throw new Error(data.message || "Lỗi khi cập nhật ảnh CCCD");
            }

            console.log("Upload CCCD thành công:", data);
            // Cập nhật preview ảnh trong UI
            const frontPreview = document.getElementById('cccd-front-preview');
            const backPreview = document.getElementById('cccd-back-preview');
            if (frontPreview && data.cccdFrontUrl) {
                frontPreview.innerHTML = `<img src="${data.cccdFrontUrl}" alt="CCCD Front" style="max-width: 100%; max-height: 200px;">`;
            }
            if (backPreview && data.cccdBackUrl) {
                backPreview.innerHTML = `<img src="${data.cccdBackUrl}" alt="CCCD Back" style="max-width: 100%; max-height: 200px;">`;
            }

            this.showNotification("Cập nhật ảnh CCCD thành công!", "success");
        } catch (error) {
            console.error("Lỗi khi upload CCCD:", error);
            throw error; // Ném lỗi để xử lý trong catch của editContract
        }
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
        console.log("Filling tenant fields with data:", JSON.stringify(tenant, null, 2));

        // Đảm bảo provinces đã được load (kiểm tra số lượng option thực tế)
        const provinceSelect = document.getElementById('tenant-province');
        if (!provinceSelect || provinceSelect.options.length <= 1) {
            console.log("Province dropdown not initialized or empty, reloading provinces...");
            await this.loadProvinces().catch(error => {
                console.error("Error reloading provinces:", error);
                this.showNotification("Lỗi khi tải danh sách tỉnh/thành phố: " + error.message, "error");
            });
        } else {
            console.log("Province dropdown already loaded with", provinceSelect.options.length, "options");
        }

        // Điền các trường thông tin tenant
        const tenantName = document.getElementById("tenant-name");
        const tenantDob = document.getElementById("tenant-dob");
        const tenantId = document.getElementById("tenant-id");
        const tenantIdDate = document.getElementById("tenant-id-date");
        const tenantIdPlace = document.getElementById("tenant-id-place");
        const tenantEmail = document.getElementById("tenant-email");
        const tenantStreet = document.getElementById("tenant-street");

        if (!tenantName || !tenantDob || !tenantId || !tenantIdDate || !tenantIdPlace || !tenantEmail || !tenantStreet) {
            console.error("Missing tenant input elements in DOM");
            this.showNotification("Không tìm thấy các trường thông tin tenant!", "error");
            return;
        }

        tenantName.value = tenant.fullName || "";
        tenantDob.value = this.formatDate(tenant.birthday) || "";
        tenantId.value = tenant.cccdNumber || "";
        tenantIdDate.value = this.formatDate(tenant.issueDate) || "";
        tenantIdPlace.value = tenant.issuePlace || "";
        tenantEmail.value = tenant.email || "";
        tenantStreet.value = tenant.street || "";

        // Xử lý ảnh CCCD
        const frontPreview = document.getElementById("cccd-front-preview");
        const backPreview = document.getElementById("cccd-back-preview");

        if (!frontPreview || !backPreview) {
            console.error("Missing preview elements: cccd-front-preview or cccd-back-preview");
            this.showNotification("Không tìm thấy phần tử hiển thị ảnh CCCD!", "error");
            return;
        }

        const baseUrl = window.location.origin;
        const cccdFrontUrl = tenant.cccdFrontUrl ? `${baseUrl}${tenant.cccdFrontUrl.replace(/ /g, '%20')}` : null;
        const cccdBackUrl = tenant.cccdBackUrl ? `${baseUrl}${tenant.cccdBackUrl.replace(/ /g, '%20')}` : null;

        console.log("CCCD URLs:", {frontUrl: cccdFrontUrl, backUrl: cccdBackUrl});

        frontPreview.innerHTML = cccdFrontUrl
            ? `<img src="${cccdFrontUrl}" alt="CCCD Front" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;" onerror="this.onerror=null; this.parentElement.innerHTML='<i class=\"fa fa-camera fa-2x\"></i><div class=\"mt-2\">Tải ảnh mặt trước</div>';">`
            : `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt trước</div>`;
        backPreview.innerHTML = cccdBackUrl
            ? `<img src="${cccdBackUrl}" alt="CCCD Back" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;" onerror="this.onerror=null; this.parentElement.innerHTML='<i class=\"fa fa-camera fa-2x\"></i><div class=\"mt-2\">Tải ảnh mặt sau</div>';">`
            : `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt sau</div>`;

        // Nếu không có URL ảnh, gọi API để lấy
        if (!cccdFrontUrl || !cccdBackUrl) {
            if (tenant.cccdNumber) {
                console.log("No CCCD URLs in tenant data, fetching from API for cccdNumber:", tenant.cccdNumber);
                try {
                    const response = await fetch(`/api/contracts/cccd-images?cccdNumber=${encodeURIComponent(tenant.cccdNumber)}`, {
                        method: 'GET', // Sửa từ POST thành GET vì endpoint dùng @GetMapping
                        headers: {
                            'Accept': 'application/json',
                            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ""
                        }
                    });
                    const data = await response.json();
                    console.log("CCCD images response:", data);
                    if (data.success) {
                        if (data.cccdFrontUrl) {
                            frontPreview.innerHTML = `<img src="${baseUrl}${data.cccdFrontUrl.replace(/ /g, '%20')}" alt="CCCD Front" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;" onerror="this.onerror=null; this.parentElement.innerHTML='<i class=\"fa fa-camera fa-2x\"></i><div class=\"mt-2\">Tải ảnh mặt trước</div>';">`;
                        }
                        if (data.cccdBackUrl) {
                            backPreview.innerHTML = `<img src="${baseUrl}${data.cccdBackUrl.replace(/ /g, '%20')}" alt="CCCD Back" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;" onerror="this.onerror=null; this.parentElement.innerHTML='<i class=\"fa fa-camera fa-2x\"></i><div class=\"mt-2\">Tải ảnh mặt sau</div>';">`;
                        }
                        this.showNotification("Lấy ảnh CCCD thành công!", "success");
                    } else {
                        console.warn("Failed to fetch CCCD images:", data.message);
                        this.showNotification(data.message || "Không tìm thấy ảnh CCCD!", "warning");
                    }
                } catch (error) {
                    console.error("Error fetching CCCD images:", error);
                    this.showNotification("Lỗi khi lấy ảnh CCCD: " + error.message, "error");
                }
            } else {
                console.warn("No cccdNumber provided, cannot fetch CCCD images");
                this.showNotification("Không có số CCCD để lấy ảnh!", "warning");
            }
        }

        // Xử lý địa chỉ
        // const provinceSelect = document.getElementById("tenant-province");
        const districtSelect = document.getElementById("tenant-district");
        const wardSelect = document.getElementById("tenant-ward");

        if (!provinceSelect) {
            console.error("tenant-province element not found in DOM");
            this.showNotification("Không tìm thấy dropdown tỉnh/thành phố!", "error");
            return;
        }

        if (tenant.province && provinceSelect) {
            console.log("Attempting to map province:", tenant.province);
            let provinceCode = await this.mapProvinceNameToCode(tenant.province);
            console.log("Province code:", provinceCode);

            this.debugDropdownOptions("tenant-province", provinceCode);

            if (provinceCode) {
                const provinceOption = provinceSelect.querySelector(`option[value="${provinceCode}"]`);
                if (provinceOption) {
                    provinceSelect.value = provinceCode;
                    console.log("Province set to code:", provinceCode);
                    await this.loadDistricts(provinceCode, "tenant-district", "tenant-ward");
                } else {
                    console.warn("Province code not found, adding as new option:", tenant.province);
                    const newOption = document.createElement("option");
                    newOption.value = provinceCode || tenant.province;
                    newOption.textContent = tenant.province;
                    provinceSelect.appendChild(newOption);
                    provinceSelect.value = provinceCode || tenant.province;
                    this.showNotification(`Không tìm thấy mã tỉnh/thành phố cho ${tenant.province}, thêm tùy chọn mới`, "warning");
                }
            } else {
                console.warn("No province code mapped, using raw value:", tenant.province);
                provinceSelect.value = tenant.province; // Fallback với tên
                this.showNotification(`Không ánh xạ được mã tỉnh/thành phố cho ${tenant.province}, sử dụng tên trực tiếp`, "warning");
            }

            let districtCode = null;
            if (tenant.district && districtSelect) {
                console.log("Attempting to map district:", tenant.district);
                districtCode = await this.mapDistrictNameToCode(provinceCode || tenant.province, tenant.district);
                console.log("District code:", districtCode);

                this.debugDropdownOptions("tenant-district", districtCode);

                if (districtCode) {
                    const districtOption = districtSelect.querySelector(`option[value="${districtCode}"]`);
                    if (districtOption) {
                        districtSelect.value = districtCode;
                        console.log("District set to code:", districtCode);
                        await this.loadWards(districtCode, "tenant-ward");
                    } else {
                        console.warn("District code not found, adding as new option:", tenant.district);
                        const newOption = document.createElement("option");
                        newOption.value = districtCode || tenant.district;
                        newOption.textContent = tenant.district;
                        districtSelect.appendChild(newOption);
                        districtSelect.value = districtCode || tenant.district;
                        this.showNotification(`Không tìm thấy mã quận/huyện cho ${tenant.district}, thêm tùy chọn mới`, "warning");
                    }
                } else {
                    console.warn("No district code mapped, using raw value:", tenant.district);
                    districtSelect.value = tenant.district;
                    this.showNotification(`Không ánh xạ được mã quận/huyện cho ${tenant.district}, sử dụng tên trực tiếp`, "warning");
                }

                if (tenant.ward && wardSelect) {
                    console.log("Attempting to map ward:", tenant.ward);
                    const wardCode = await this.mapWardNameToCode(districtCode, tenant.ward, provinceCode);
                    console.log("Ward code:", wardCode);

                    if (wardCode) {
                        const wardOption = wardSelect.querySelector(`option[value="${wardCode}"]`);
                        if (wardOption) {
                            wardSelect.value = wardCode;
                            console.log("Ward set to code:", wardCode);
                        } else {
                            console.warn("Ward code not found, adding as new option:", tenant.ward);
                            const newOption = document.createElement("option");
                            newOption.value = wardCode || tenant.ward;
                            newOption.textContent = tenant.ward;
                            wardSelect.appendChild(newOption);
                            wardSelect.value = wardCode || tenant.ward;
                            this.showNotification(`Không tìm thấy mã phường/xã cho ${tenant.ward}, thêm tùy chọn mới`, "warning");
                        }
                    } else {
                        console.warn("No ward code mapped, using raw value:", tenant.ward);
                        wardSelect.value = tenant.ward;
                        this.showNotification(`Không ánh xạ được mã phường/xã cho ${tenant.ward}, sử dụng tên trực tiếp`, "warning");
                    }
                }
            }

            this.updateAddress("tenant");
            this.updateAllPreview();
        }
    },

    async fillOwnerFields(owner) {
        console.log("Filling owner fields with data:", JSON.stringify(owner, null, 2));

        // Hàm kiểm tra DOM đơn giản và hiệu quả
        const waitForOwnerDOM = async () => {
            const maxAttempts = 10;
            let attempt = 0;

            while (attempt < maxAttempts) {
                const elements = {
                    provinceSelect: document.getElementById('owner-province'),
                    ownerName: document.getElementById("owner-name"),
                    ownerPhone: document.getElementById("owner-phone"),
                    ownerId: document.getElementById("owner-id"),
                    ownerStreet: document.getElementById("owner-street"),
                    districtSelect: document.getElementById("owner-district"),
                    wardSelect: document.getElementById("owner-ward")
                };

                console.log(`Checking owner DOM (Attempt ${attempt + 1}/${maxAttempts}):`, {
                    provinceSelect: !!elements.provinceSelect,
                    ownerName: !!elements.ownerName,
                    ownerPhone: !!elements.ownerPhone,
                    ownerId: !!elements.ownerId,
                    ownerStreet: !!elements.ownerStreet,
                    districtSelect: !!elements.districtSelect,
                    wardSelect: !!elements.wardSelect
                });

                // Kiểm tra các element quan trọng
                if (elements.provinceSelect && elements.ownerName && elements.ownerPhone &&
                    elements.ownerId && elements.ownerStreet && elements.districtSelect && elements.wardSelect) {
                    console.log("✅ All owner DOM elements found!");
                    return elements;
                }

                attempt++;
                await new Promise(resolve => setTimeout(resolve, 100)); // Đợi 100ms
            }

            console.error("❌ Owner DOM elements not found after maximum attempts");
            throw new Error("Không tìm thấy các trường thông tin chủ trọ sau " + maxAttempts + " lần thử");
        };

        try {
            // Đợi DOM sẵn sàng
            const elements = await waitForOwnerDOM();

            // Đảm bảo provinces đã được load
            if (!elements.provinceSelect || elements.provinceSelect.options.length <= 1) {
                console.log("Owner province dropdown not initialized, reloading provinces...");
                await this.loadProvinces();

                // Kiểm tra lại sau khi load provinces
                if (elements.provinceSelect.options.length <= 1) {
                    throw new Error("Không thể tải danh sách tỉnh/thành phố");
                }
            } else {
                console.log("Owner province dropdown already loaded with", elements.provinceSelect.options.length, "options");
            }

            // Điền dữ liệu cơ bản
            elements.ownerName.value = owner.fullName || "";
            elements.ownerPhone.value = owner.phone || "";
            elements.ownerId.value = owner.cccdNumber || "";
            elements.ownerStreet.value = owner.street || "";

            // Xử lý địa chỉ nếu có
            if (owner.province && elements.provinceSelect) {
                await this.fillOwnerAddress(owner, elements);
            }

            this.updateAddress("owner");
            this.updateAllPreview();

            console.log("✅ Owner fields filled successfully");

        } catch (error) {
            console.error("❌ Error filling owner fields:", error);
            this.showNotification("Lỗi khi điền thông tin chủ trọ: " + error.message, "error");
        }
    },

// Tách riêng logic xử lý địa chỉ để code sạch hơn
    async fillOwnerAddress(owner, elements) {
        try {
            // Kiểm tra format tỉnh hợp lệ
            if (!/^(Tỉnh|Thành phố)/.test(owner.province)) {
                console.warn("Invalid owner province format:", owner.province);
                this.showNotification("Dữ liệu tỉnh không hợp lệ cho chủ trọ!", "warning");
                return;
            }

            console.log("Attempting to map owner province:", owner.province);
            let provinceCode = await this.mapProvinceNameToCode(owner.province);
            console.log("Owner province code:", provinceCode);

            if (provinceCode) {
                // Tìm option theo code
                const provinceOption = elements.provinceSelect.querySelector(`option[value="${provinceCode}"]`);
                if (provinceOption) {
                    elements.provinceSelect.value = provinceCode;
                    console.log("✅ Owner province set to code:", provinceCode);

                    // Load districts
                    await this.loadDistricts(provinceCode, "owner-district", "owner-ward");

                    // Xử lý district
                    if (owner.district && elements.districtSelect) {
                        await this.fillOwnerDistrict(owner, elements, provinceCode);
                    }
                } else {
                    // Thêm option mới nếu không tìm thấy
                    console.warn("Owner province code not found, adding as new option:", owner.province);
                    const newOption = document.createElement("option");
                    newOption.value = provinceCode;
                    newOption.textContent = owner.province;
                    elements.provinceSelect.appendChild(newOption);
                    elements.provinceSelect.value = provinceCode;
                    this.showNotification(`Đã thêm tỉnh/thành phố mới: ${owner.province}`, "info");
                }
            } else {
                // Fallback: tìm theo tên
                const existingOption = Array.from(elements.provinceSelect.options).find(opt =>
                    this.normalizeName(opt.textContent) === this.normalizeName(owner.province)
                );

                if (existingOption) {
                    elements.provinceSelect.value = existingOption.value;
                    console.log("✅ Using existing owner province option:", existingOption.value);
                } else {
                    console.warn("No owner province mapping found:", owner.province);
                    this.showNotification(`Không tìm thấy tỉnh/thành phố: ${owner.province}`, "warning");
                }
            }
        } catch (error) {
            console.error("Error filling owner address:", error);
            this.showNotification("Lỗi khi xử lý địa chỉ chủ trọ: " + error.message, "error");
        }
    },

// Tách riêng logic xử lý district
    async fillOwnerDistrict(owner, elements, provinceCode) {
        try {
            const districtCode = await this.mapDistrictNameToCode(provinceCode, owner.district);
            console.log("Owner district code:", districtCode);

            if (districtCode) {
                const districtOption = elements.districtSelect.querySelector(`option[value="${districtCode}"]`);
                if (districtOption) {
                    elements.districtSelect.value = districtCode;
                    console.log("✅ Owner district set to code:", districtCode);

                    // Load wards
                    await this.loadWards(districtCode, "owner-ward");

                    // Xử lý ward
                    if (owner.ward && elements.wardSelect) {
                        await this.fillOwnerWard(owner, elements, districtCode, provinceCode);
                    }
                } else {
                    // Thêm district mới
                    const newOption = document.createElement("option");
                    newOption.value = districtCode;
                    newOption.textContent = owner.district;
                    elements.districtSelect.appendChild(newOption);
                    elements.districtSelect.value = districtCode;
                    this.showNotification(`Đã thêm quận/huyện mới: ${owner.district}`, "info");
                }
            } else {
                console.warn("No district code mapped for:", owner.district);
                this.showNotification(`Không tìm thấy quận/huyện: ${owner.district}`, "warning");
            }
        } catch (error) {
            console.error("Error filling owner district:", error);
        }
    },

// Tách riêng logic xử lý ward
    async fillOwnerWard(owner, elements, districtCode, provinceCode) {
        try {
            const wardCode = await this.mapWardNameToCode(districtCode, owner.ward, provinceCode);

            if (wardCode) {
                const wardOption = elements.wardSelect.querySelector(`option[value="${wardCode}"]`);
                if (wardOption) {
                    elements.wardSelect.value = wardCode;
                    console.log("✅ Owner ward set to code:", wardCode);
                } else {
                    // Thêm ward mới
                    const newOption = document.createElement("option");
                    newOption.value = wardCode;
                    newOption.textContent = owner.ward;
                    elements.wardSelect.appendChild(newOption);
                    elements.wardSelect.value = wardCode;
                    this.showNotification(`Đã thêm phường/xã mới: ${owner.ward}`, "info");
                }
            } else {
                console.warn("No ward code mapped for:", owner.ward);
                this.showNotification(`Không tìm thấy phường/xã: ${owner.ward}`, "warning");
            }
        } catch (error) {
            console.error("Error filling owner ward:", error);
        }
    },





    async fillRoomFields(room) {
        console.log("Filling room fields with data:", JSON.stringify(room, null, 2));

        const checkDOMReady = () => {
            return new Promise(resolve => {
                const observer = new MutationObserver((mutations, obs) => {
                    const provinceSelect = document.getElementById('room-province');
                    const districtSelect = document.getElementById('room-district');
                    const wardSelect = document.getElementById('room-ward');
                    const roomStreet = document.getElementById('room-street');
                    const roomNumber = document.getElementById('room-number');
                    const roomArea = document.getElementById('room-area');

                    console.log("Checking room DOM:", {
                        provinceSelect: !!provinceSelect,
                        districtSelect: !!districtSelect,
                        wardSelect: !!wardSelect,
                        roomStreet: !!roomStreet,
                        roomNumber: !!roomNumber,
                        roomArea: !!roomArea
                    });

                    if (provinceSelect && districtSelect && wardSelect && roomStreet && roomNumber && roomArea) {
                        obs.disconnect();
                        resolve();
                    } else if (mutations.length > 10) { // Timeout sau 10 mutations
                        console.error("DOM not ready after multiple mutations, forcing resolve");
                        obs.disconnect();
                        resolve();
                    }
                });
                observer.observe(document.body, { childList: true, subtree: true });
            });
        };

        await checkDOMReady();

        const provinceSelect = document.getElementById('room-province');
        if (!provinceSelect || provinceSelect.options.length <= 1) {
            console.log("Room province dropdown not initialized, reloading provinces...");
            await this.loadProvinces().catch(error => {
                console.error("Error reloading provinces for room:", error);
                this.showNotification("Lỗi khi tải danh sách tỉnh/thành phố cho phòng trọ: " + error.message, "error");
            });
        } else {
            console.log("Room province dropdown already loaded with", provinceSelect.options.length, "options");
        }

        console.log("DOM elements found:", {
            provinceSelect: !!provinceSelect,
            districtSelect: !!districtSelect,
            wardSelect: !!wardSelect,
            roomStreet: !!roomStreet,
            roomNumber: !!roomNumber,
            roomArea: !!roomArea
        });

        if (!provinceSelect || !districtSelect || !wardSelect || !roomStreet || !roomNumber || !roomArea) {
            console.error("Missing room input elements in DOM:", { provinceSelect, districtSelect, wardSelect, roomStreet, roomNumber, roomArea });
            this.showNotification("Không tìm thấy các trường thông tin phòng trọ!", "error");
            return;
        }

        roomNumber.value = room.roomName || "";
        roomArea.value = room.area || 0;

        // Fallback: Parse province from address if room.province is invalid
        let provinceToUse = room.province;
        let districtToUse = room.district;
        let wardToUse = room.ward;
        let streetToUse = room.street;

        if (!/^(Tỉnh|Thành phố)/.test(room.province) && room.address) {
            console.warn("Room province invalid, parsing from address:", room.address);
            const addressParts = room.address.split(",");
            streetToUse = addressParts[0]?.trim() || "";
            wardToUse = addressParts[1]?.trim() || "";
            districtToUse = addressParts[2]?.trim() || "";
            provinceToUse = addressParts[3]?.trim() || "";
            console.log("Parsed address parts:", { streetToUse, wardToUse, districtToUse, provinceToUse });
        }

        if (provinceToUse && provinceSelect) {
            if (/^(Tỉnh|Thành phố)/.test(provinceToUse)) {
                console.log("Attempting to map room province:", provinceToUse);
                let provinceCode = await this.mapProvinceNameToCode(provinceToUse);
                console.log("Room province code:", provinceCode);

                this.debugDropdownOptions("room-province", provinceCode);

                if (provinceCode) {
                    const provinceOption = provinceSelect.querySelector(`option[value="${provinceCode}"]`);
                    if (provinceOption) {
                        provinceSelect.value = provinceCode;
                        console.log("Room province set to code:", provinceCode);
                        await this.loadDistricts(provinceCode, "room-district", "room-ward").catch(error => {
                            console.error("Error loading districts for room:", error);
                            this.showNotification("Lỗi khi tải quận/huyện cho phòng trọ: " + error.message, "error");
                        });
                    } else {
                        console.warn("Room province code not found, adding as new option:", provinceToUse);
                        const newOption = document.createElement("option");
                        newOption.value = provinceCode;
                        newOption.textContent = provinceToUse;
                        provinceSelect.appendChild(newOption);
                        provinceSelect.value = provinceCode;
                        this.showNotification(`Không tìm thấy mã tỉnh/thành phố cho ${provinceToUse}, thêm tùy chọn mới`, "warning");
                    }
                } else {
                    const existingOption = Array.from(provinceSelect.options).find(opt => this.normalizeName(opt.textContent) === this.normalizeName(provinceToUse));
                    if (existingOption) {
                        provinceSelect.value = existingOption.value;
                        console.log("Using existing room province option:", existingOption.value);
                    } else {
                        console.warn("No room province code mapped, using raw value:", provinceToUse);
                        provinceSelect.value = provinceToUse;
                        this.showNotification(`Không ánh xạ được mã tỉnh/thành phố cho ${provinceToUse}, sử dụng tên trực tiếp`, "warning");
                    }
                }
            } else {
                console.warn("Invalid room province value, skipping:", provinceToUse);
                this.showNotification("Dữ liệu tỉnh không hợp lệ cho phòng trọ!", "warning");
            }

            let districtCode = null;
            if (districtToUse && districtSelect) {
                districtCode = await this.mapDistrictNameToCode(provinceSelect.value || provinceToUse, districtToUse);
                console.log("Room district code:", districtCode);
                this.debugDropdownOptions("room-district", districtCode);
                if (districtCode) {
                    const districtOption = districtSelect.querySelector(`option[value="${districtCode}"]`);
                    if (districtOption) {
                        districtSelect.value = districtCode;
                        console.log("Room district set to code:", districtCode);
                        await this.loadWards(districtCode, "room-ward").catch(error => {
                            console.error("Error loading wards for room:", error);
                            this.showNotification("Lỗi khi tải phường/xã cho phòng trọ: " + error.message, "error");
                        });
                    } else {
                        const newOption = document.createElement("option");
                        newOption.value = districtCode;
                        newOption.textContent = districtToUse;
                        districtSelect.appendChild(newOption);
                        districtSelect.value = districtCode;
                        this.showNotification(`Không tìm thấy mã quận/huyện cho ${districtToUse}, thêm tùy chọn mới`, "warning");
                    }
                } else {
                    districtSelect.value = districtToUse;
                    this.showNotification(`Không ánh xạ được mã quận/huyện cho ${districtToUse}, sử dụng tên trực tiếp`, "warning");
                }

                if (wardToUse && wardSelect) {
                    const wardCode = await this.mapWardNameToCode(districtCode, wardToUse, provinceSelect.value);
                    if (wardCode) {
                        const wardOption = wardSelect.querySelector(`option[value="${wardCode}"]`);
                        if (wardOption) {
                            wardSelect.value = wardCode;
                        } else {
                            const newOption = document.createElement("option");
                            newOption.value = wardCode;
                            newOption.textContent = wardToUse;
                            wardSelect.appendChild(newOption);
                            wardSelect.value = wardCode;
                            this.showNotification(`Không tìm thấy mã phường/xã cho ${wardToUse}, thêm tùy chọn mới`, "warning");
                        }
                    } else {
                        wardSelect.value = wardToUse;
                        this.showNotification(`Không ánh xạ được mã phường/xã cho ${wardToUse}, sử dụng tên trực tiếp`, "warning");
                    }
                }
            }

            roomStreet.value = streetToUse || "";
            this.updateAddress("room");
            this.updateAllPreview();
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
        console.log(`Current selected value: "${select.value}"`);
        console.log(`=== END DEBUG ===`);
    },

// Hàm chuyển đổi định dạng ngày
    formatDate(dateStr) {
        if (!dateStr) return "";
        // Nếu định dạng là dd/MM/yy
        if (dateStr.match(/^\d{1,2}\/\d{1,2}\/\d{2}$/)) {
            const [day, month, year] = dateStr.split("/");
            return `20${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
        }
        // Nếu đã ở định dạng yyyy-MM-dd
        if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
            return dateStr;
        }
        console.warn("Invalid date format:", dateStr);
        return "";
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
        this.updateAllPreview()
    },




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
                console.log(`Comparing: "${normalizedProvinceName}" with "${normalizedApiName}"`);
                return normalizedApiName === normalizedProvinceName ||
                    normalizedApiName.includes(normalizedProvinceName) ||
                    normalizedProvinceName.includes(normalizedApiName);
            });

            if (!province) {
                console.warn(`No match for province: ${provinceName}, checked variants:`,
                    provinces.map(p => this.normalizeName(p.name)));
                return null;
            }
            const provinceCode = String(province.code).padStart(2, "0");
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
            // FIX: Sử dụng API tỉnh thành Việt Nam thực tế
            const response = await fetch("https://provinces.open-api.vn/api/p/")
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
            const provinces = await response.json()
            console.log(
                "Loaded provinces:",
                provinces.map((p) => ({ code: p.code, name: p.name })),
            )

            const selects = ["tenant-province", "owner-province", "room-province", "newCustomer-province"]
            selects.forEach((selectId) => {
                const select = document.getElementById(selectId)
                if (!select) {
                    console.warn(`Select element with ID ${selectId} not found in DOM`)
                    this.showNotification(`Không tìm thấy dropdown ${selectId}`, "error")
                    return
                }
                // Xóa nội dung hiện tại và thêm tùy chọn mặc định
                select.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>'
                // Thêm các tỉnh từ API
                provinces.forEach((province) => {
                    const option = document.createElement("option")
                    // SỬA: Đảm bảo format code giống nhau
                    const provinceCode = String(province.code).padStart(2, "0")
                    option.value = provinceCode
                    option.textContent = province.name
                    select.appendChild(option)

                    // DEBUG: Log để kiểm tra
                    if (selectId === "tenant-province") {
                        console.log(`Added province option: ${provinceCode} - ${province.name}`)
                    }
                })
                console.log(`Populated ${selectId} with ${provinces.length} provinces`)
            })

            return provinces
        } catch (error) {
            console.error("Error loading provinces:", error)
            this.showNotification("Không thể tải danh sách tỉnh/thành phố: " + error.message, "error")
            return []
        }
    },

    // SỬA LỖI: Cải thiện hàm loadDistricts để đảm bảo reset hoàn toàn
    async loadDistricts(provinceCode, districtSelectId, wardSelectId) {
        try {
            const provinceCodeStr = String(provinceCode).padStart(2, "0")
            if (!/^\d{2}$/.test(provinceCodeStr)) {
                throw new Error(`Invalid province code: ${provinceCode}`)
            }

            // FIX: Sử dụng API tỉnh thành Việt Nam thực tế
            const response = await fetch(`https://provinces.open-api.vn/api/p/${provinceCodeStr}?depth=2`)
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
            const province = await response.json()
            console.log(
                `Loaded districts for province ${provinceCodeStr}:`,
                province.districts.map((d) => ({ code: d.code, name: d.name })),
            )

            const districtSelect = document.getElementById(districtSelectId)
            const wardSelect = document.getElementById(wardSelectId)

            if (districtSelect) {
                // QUAN TRỌNG: Xóa hoàn toàn và reset
                districtSelect.innerHTML = ""
                const defaultOption = document.createElement("option")
                defaultOption.value = ""
                defaultOption.textContent = "Chọn Quận/Huyện"
                districtSelect.appendChild(defaultOption)

                // Thêm các quận/huyện mới
                province.districts.forEach((district) => {
                    const option = document.createElement("option")
                    option.value = String(district.code)
                    option.textContent = district.name
                    districtSelect.appendChild(option)

                    if (districtSelectId === "tenant-district") {
                        console.log(`Added district option: ${district.code} - ${district.name}`)
                    }
                })

                // Đảm bảo giá trị được reset
                districtSelect.value = ""
            }

            if (wardSelect) {
                // QUAN TRỌNG: Xóa hoàn toàn và reset wards
                wardSelect.innerHTML = ""
                const defaultOption = document.createElement("option")
                defaultOption.value = ""
                defaultOption.textContent = "Chọn Phường/Xã"
                wardSelect.appendChild(defaultOption)
                wardSelect.value = ""
            }
        } catch (error) {
            console.error(`Error loading districts for province ${provinceCode}:`, error)
            this.showNotification("Không thể tải danh sách quận/huyện", "error")
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
    async loadWards(districtCode, wardSelectId, provinceCode) {
        try {
            // Chuyển districtCode và provinceCode thành string và validate
            const districtCodeStr = String(districtCode).trim()

            if (
                !districtCodeStr ||
                districtCodeStr === "null" ||
                districtCodeStr === "undefined" ||
                !/^\d+$/.test(districtCodeStr)
            ) {
                throw new Error(`Invalid district code: ${districtCode}`)
            }

            console.log(`Loading wards for district ${districtCodeStr}`)

            // FIX: Sử dụng API tỉnh thành Việt Nam thực tế
            const response = await fetch(`https://provinces.open-api.vn/api/d/${districtCodeStr}?depth=2`)
            if (!response.ok) {
                throw new Error(
                    `HTTP error! status: ${response.status}, URL: https://provinces.open-api.vn/api/d/${districtCodeStr}?depth=2`,
                )
            }

            const district = await response.json()

            // Cập nhật dropdown phường/xã
            const wardSelect = document.getElementById(wardSelectId)
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                if (district.wards && district.wards.length > 0) {
                    district.wards.forEach((ward) => {
                        const option = document.createElement("option")
                        option.value = ward.code
                        option.textContent = ward.name
                        wardSelect.appendChild(option)
                        console.log(`Added ward option: ${ward.code} - ${ward.name}`)
                    })
                    console.log(`Loaded ${district.wards.length} wards for district ${districtCodeStr}`)
                } else {
                    console.warn(`No wards found for district ${districtCodeStr}`)
                    this.showNotification(`Không tìm thấy phường/xã cho quận ${districtCodeStr}`, "warning")
                }
            }
        } catch (error) {
            console.error(`Error loading wards for district ${districtCode}:`, error.message)
            this.showNotification(`Không thể tải danh sách phường/xã: ${error.message}`, "error")
            const wardSelect = document.getElementById(wardSelectId)
            if (wardSelect) {
                wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
            }
        }
    },

    // SỬA LỖI: Thêm hàm reset districts và wards khi chọn tỉnh mới
    setupLocationListeners() {
        const prefixes = ["tenant", "owner", "room", "newCustomer"]
        prefixes.forEach((prefix) => {
            const provinceSelect = document.getElementById(`${prefix}-province`)
            const districtSelect = document.getElementById(`${prefix}-district`)
            const wardSelect = document.getElementById(`${prefix}-ward`)

            if (provinceSelect) {
                provinceSelect.addEventListener("change", () => {
                    // QUAN TRỌNG: Reset districts và wards trước khi load mới
                    if (districtSelect) {
                        districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>'
                        districtSelect.value = "" // Reset giá trị
                    }
                    if (wardSelect) {
                        wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                        wardSelect.value = "" // Reset giá trị
                    }

                    if (provinceSelect.value) {
                        this.loadDistricts(provinceSelect.value, `${prefix}-district`, `${prefix}-ward`)
                    }

                    // Cập nhật địa chỉ sau khi reset
                    this.updateAddress(prefix)
                })
            }

            if (districtSelect) {
                districtSelect.addEventListener("change", () => {
                    // QUAN TRỌNG: Reset wards trước khi load mới
                    if (wardSelect) {
                        wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>'
                        wardSelect.value = "" // Reset giá trị
                    }

                    if (districtSelect.value) {
                        this.loadWards(districtSelect.value, `${prefix}-ward`)
                    }

                    // Cập nhật địa chỉ sau khi reset
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


// Hàm chuyển đổi định dạng ngày
formatDate(dateStr) {
    if (!dateStr) return "";
    // Nếu định dạng là dd/MM/yy
    if (dateStr.match(/^\d{1,2}\/\d{1,2}\/\d{2}$/)) {
        const [day, month, year] = dateStr.split("/");
        return `20${year}-${month.padStart(2, "0")}-${day.padStart(2, "0")}`;
    }
    // Nếu đã ở định dạng yyyy-MM-dd
    if (dateStr.match(/^\d{4}-\d{2}-\d{2}$/)) {
        return dateStr;
    }
    console.warn("Invalid date format:", dateStr);
    return "";
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

    saveContract() {
        const roomSelect = document.getElementById('roomSelect');
        const roomIdValue = roomSelect?.value;

        console.log("=== ROOM DEBUG ===");
        console.log("Room select element:", roomSelect);
        console.log("Room ID value:", roomIdValue);
        console.log("Room ID type:", typeof roomIdValue);

        if (!roomIdValue || roomIdValue.trim() === "" || roomIdValue === "null" || roomIdValue === "undefined") {
            this.showNotification("Vui lòng chọn phòng trọ!", "error");
            return;
        }

        const roomIdNumber = parseInt(roomIdValue, 10);
        console.log("Room ID as number:", roomIdNumber);

        if (isNaN(roomIdNumber) || roomIdNumber <= 0) {
            this.showNotification("ID phòng không hợp lệ!", "error");
            return;
        }

        const contractData = this.buildContractData(roomIdNumber, roomSelect);

        const tenantPhone = contractData.tenantType === "UNREGISTERED" ?
            contractData.unregisteredTenant?.phone : contractData.tenant?.phone;
        if (!tenantPhone) {
            this.showNotification("Số điện thoại người thuê không được để trống!", "error");
            return;
        }

        const cccdNumber = contractData.tenantType === "UNREGISTERED" ?
            contractData.unregisteredTenant?.cccdNumber : contractData.tenant?.cccdNumber;
        if (!cccdNumber) {
            this.showNotification("Số CCCD không được để trống!", "error");
            return;
        }

        const formData = new FormData();
        const cccdFront = document.getElementById("cccd-front").files[0];
        const cccdBack = document.getElementById("cccd-back").files[0];
        if (cccdFront) formData.append("cccdFront", cccdFront);
        if (cccdBack) formData.append("cccdBack", cccdBack);
        formData.append("cccdNumber", cccdNumber);

        console.log("=== SENDING CCCD FORM DATA ===");
        for (let [key, value] of formData.entries()) {
            console.log(`${key}: ${value instanceof File ? value.name : value}`);
        }

        fetch("/api/contracts/upload-cccd", {
            method: "POST",
            body: formData
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    if (contractData.tenantType === "UNREGISTERED") {
                        if (!contractData.unregisteredTenant) contractData.unregisteredTenant = {};
                        contractData.unregisteredTenant.cccdFrontUrl = data.cccdFrontUrl;
                        contractData.unregisteredTenant.cccdBackUrl = data.cccdBackUrl;
                    } else {
                        if (!contractData.tenant) contractData.tenant = {};
                        contractData.tenant.cccdFrontUrl = data.cccdFrontUrl;
                        contractData.tenant.cccdBackUrl = data.cccdBackUrl;
                    }

                    console.log("=== SENDING CONTRACT DATA ===");
                    console.log(JSON.stringify(contractData, null, 2));

                    return fetch("/api/contracts", {
                        method: "POST",
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify(contractData)
                    });
                } else {
                    throw new Error(data.message || "Lỗi khi upload CCCD!");
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.showNotification("Hợp đồng đã được tạo thành công!", "success");
                    setTimeout(() => window.location.href = "/api/contracts/list", 1500);
                } else {
                    this.showNotification(data.message || "Lỗi khi tạo hợp đồng!", "error");
                }
            })
            .catch(error => {
                console.error("Lỗi khi tạo hợp đồng:", error);
                this.showNotification("Lỗi khi tạo hợp đồng: " + error.message, "error");
            });
    },



    buildContractData(roomIdNumber, roomSelect) {
        const contractData = {};

        // Room (chỉ gửi nếu người dùng chọn phòng mới)
        if (roomIdNumber && roomSelect && roomSelect.selectedIndex >= 0) {
            const selectedOption = roomSelect.options[roomSelect.selectedIndex];
            contractData.room = {
                roomId: roomIdNumber,
                roomName: selectedOption.dataset.roomName || `Phòng ${roomIdNumber}`,
                price: parseFloat(selectedOption.dataset.price) || undefined,
                area: parseFloat(selectedOption.dataset.area) || undefined,
                status: selectedOption.dataset.status || undefined
            };
        }

        // Contract basic info
        const contractDate = document.getElementById("contract-date")?.value?.trim();
        if (contractDate) contractData.contractDate = contractDate;

        const contractStatus = document.getElementById("contract-status")?.value?.trim();
        if (contractStatus) contractData.status = contractStatus;

        // Terms
        const terms = {};
        const startDate = document.getElementById("start-date")?.value?.trim();
        if (startDate) terms.startDate = startDate;

        const duration = parseInt(document.getElementById("contract-duration")?.value);
        if (!isNaN(duration) && duration > 0) terms.duration = duration;

        const rentPrice = parseFloat(document.getElementById("rent-price")?.value);
        if (!isNaN(rentPrice) && rentPrice > 0) terms.price = rentPrice;

        const depositMonths = parseFloat(document.getElementById("deposit-months")?.value);
        if (!isNaN(depositMonths) && !isNaN(rentPrice) && depositMonths >= 0) {
            terms.deposit = depositMonths * rentPrice;
        }

        const termsText = this.getContractTermsText ? this.getContractTermsText() : undefined;
        if (termsText) terms.terms = termsText;

        if (Object.keys(terms).length > 0) contractData.terms = terms;

        // Tenant
        const tenantType = document.getElementById("tenantType")?.value?.trim() || "REGISTERED";
        contractData.tenantType = tenantType;

        if (tenantType === "REGISTERED") {
            const tenant = {};
            const tenantPhone = document.getElementById("tenant-phone")?.value?.trim();
            if (tenantPhone) tenant.phone = tenantPhone;

            const tenantFullName = document.getElementById("tenant-name")?.value?.trim();
            if (tenantFullName) tenant.fullName = tenantFullName;

            const tenantCccd = document.getElementById("tenant-id")?.value?.trim();
            if (tenantCccd) tenant.cccdNumber = tenantCccd;

            const tenantBirthday = document.getElementById("tenant-dob")?.value?.trim();
            if (tenantBirthday) tenant.birthday = tenantBirthday;

            const tenantEmail = document.getElementById("tenant-email")?.value?.trim();
            if (tenantEmail) tenant.email = tenantEmail;

            const tenantStreet = document.getElementById("tenant-street")?.value?.trim();
            if (tenantStreet) tenant.street = tenantStreet;

            const tenantWard = document.getElementById("tenant-ward")?.options[document.getElementById("tenant-ward")?.selectedIndex]?.text?.trim();
            if (tenantWard) tenant.ward = tenantWard;

            const tenantDistrict = document.getElementById("tenant-district")?.options[document.getElementById("tenant-district")?.selectedIndex]?.text?.trim();
            if (tenantDistrict) tenant.district = tenantDistrict;

            const tenantProvince = document.getElementById("tenant-province")?.options[document.getElementById("tenant-province")?.selectedIndex]?.text?.trim();
            if (tenantProvince) tenant.province = tenantProvince;


            // Thêm ảnh từ fillTenantFields
            const frontPreview = document.getElementById('cccd-front-preview');
            const backPreview = document.getElementById('cccd-back-preview');
            if (frontPreview && frontPreview.querySelector('img')) tenant.cccdFrontUrl = frontPreview.querySelector('img').src;
            if (backPreview && backPreview.querySelector('img')) tenant.cccdBackUrl = backPreview.querySelector('img').src;

            if (Object.keys(tenant).length > 0) contractData.tenant = tenant;


        } else if (tenantType === "UNREGISTERED") {
            const unregisteredTenant = {};
            const tenantPhone = document.getElementById("unregisteredTenantPhone")?.value?.trim();
            if (tenantPhone) unregisteredTenant.phone = tenantPhone;

            const tenantFullName = document.getElementById("unregisteredTenantFullName")?.value?.trim();
            if (tenantFullName) unregisteredTenant.fullName = tenantFullName;

            const tenantCccd = document.getElementById("unregisteredTenantCccdNumber")?.value?.trim();
            if (tenantCccd) unregisteredTenant.cccdNumber = tenantCccd;

            const tenantBirthday = document.getElementById("unregisteredTenantBirthday")?.value?.trim();
            if (tenantBirthday) unregisteredTenant.birthday = tenantBirthday;

            const tenantIssueDate = document.getElementById("unregisteredTenantIssueDate")?.value?.trim();
            if (tenantIssueDate) unregisteredTenant.issueDate = tenantIssueDate;

            const tenantIssuePlace = document.getElementById("unregisteredTenantIssuePlace")?.value?.trim();
            if (tenantIssuePlace) unregisteredTenant.issuePlace = tenantIssuePlace;

            if (Object.keys(unregisteredTenant).length > 0) contractData.unregisteredTenant = unregisteredTenant;
        }

        // Owner
        const owner = {};
        const ownerFullName = document.getElementById("owner-name")?.value?.trim();
        if (ownerFullName) owner.fullName = ownerFullName;

        const ownerPhone = document.getElementById("owner-phone")?.value?.trim();
        if (ownerPhone) owner.phone = ownerPhone;

        const ownerCccd = document.getElementById("owner-id")?.value?.trim();
        if (ownerCccd) owner.cccdNumber = ownerCccd;

        if (Object.keys(owner).length > 0) contractData.owner = owner;

        console.log("=== FINAL CONTRACT DATA ===");
        console.log(JSON.stringify(contractData, null, 2));
        return contractData;
    },


    getContractTermsText() {
        if (this.contractTerms && this.contractTerms.length > 0) {
            return this.contractTerms.map((term, index) =>
                `${index + 1}. ${term.text}`
            ).join("\n")
        }
        return "Các điều khoản hợp đồng sẽ được bổ sung sau."
    },


// ✅ Method gửi FormData (backup - đã sửa)
    saveContractWithFormData() {
        // Lấy roomId từ form
        const roomSelect = document.getElementById('roomSelect')
        const roomIdValue = roomSelect?.value
        const roomIdNumber = parseInt(roomIdValue, 10)

        if (isNaN(roomIdNumber) || roomIdNumber <= 0) {
            this.showNotification("ID phòng không hợp lệ!", "error")
            return
        }

        const formData = new FormData()
        formData.append("roomId", roomIdNumber.toString())
        formData.append("tenantType", document.getElementById("tenantType")?.value || "REGISTERED")

        // Thêm contract terms nếu có
        if (this.contractTerms && this.contractTerms.length > 0) {
            const termsText = this.contractTerms.map((term, index) =>
                `${index + 1}. ${term.text}`
            ).join("\n")
            formData.append("contractTerms", termsText)
        }

        console.log("=== SENDING FORM DATA ===")
        for (let [key, value] of formData.entries()) {
            console.log(`${key}: ${value}`)
        }

        this.sendFormData(formData)
    },

// ✅ Method gửi FormData (đã sửa)
    sendFormData(formData) {
        fetch("/api/contracts", {
            method: "POST",
            body: formData // Không set Content-Type cho FormData
        })
            .then(this.handleResponse.bind(this))
            .catch(this.handleError.bind(this))
    },

// ✅ Handle response (đã sửa)
    async handleResponse(response) {
        console.log("=== SERVER RESPONSE ===")
        console.log("Status:", response.status)
        console.log("Headers:", Object.fromEntries(response.headers.entries()))

        const responseText = await response.text()
        console.log("Response Body:", responseText)

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${responseText}`)
        }

        try {
            // Thử parse JSON
            const data = JSON.parse(responseText)
            console.log("Parsed JSON:", data)

            if (data.success !== false) { // Coi như success nếu không có field success hoặc success = true
                this.showNotification("Hợp đồng đã được lưu thành công!", "success")
                setTimeout(() => {
                    window.location.href = "/api/contracts/list" // hoặc window.location.reload()
                }, 1500)
            } else {
                this.showNotification(data.message || "Lỗi khi lưu hợp đồng!", "error")
            }
        } catch (e) {
            // Nếu không parse được JSON, coi như success nếu status 200
            if (response.status >= 200 && response.status < 300) {
                this.showNotification("Hợp đồng đã được lưu thành công!", "success")
                setTimeout(() => {
                    window.location.href = "/api/contracts/list"
                }, 1500)
            } else {
                throw new Error('Server không trả về JSON hợp lệ: ' + responseText)
            }
        }
    },

// ✅ Handle error (giữ nguyên)
    handleError(error) {
        console.error("Error saving contract:", error)
        this.showNotification("Lỗi khi lưu hợp đồng: " + error.message, "error")
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
        const formData = new FormData()
        formData.append("name", document.getElementById("newCustomer-name").value || "")
        formData.append("dob", document.getElementById("newCustomer-dob").value || "")
        formData.append("id", document.getElementById("newCustomer-id").value || "")
        formData.append("id-date", document.getElementById("newCustomer-id-date").value || "")
        formData.append("id-place", document.getElementById("newCustomer-id-place").value || "")
        formData.append("phone", document.getElementById("newCustomer-phone").value || "")
        formData.append("email", document.getElementById("newCustomer-email").value || "")
        formData.append("street", document.getElementById("newCustomer-street").value || "")
        formData.append("ward", this.getSelectText("newCustomer-ward") || "")
        formData.append("district", this.getSelectText("newCustomer-district") || "")
        formData.append("province", this.getSelectText("newCustomer-province") || "")
        formData.append("cccd-front", document.getElementById("newCustomer-cccd-front").files[0] || null)
        formData.append("cccd-back", document.getElementById("newCustomer-cccd-back").files[0] || null)

        fetch("/api/contracts/add-unregistered-tenant", {
            method: "POST",
            body: formData,
        })
            .then((response) => response.json())
            .then((data) => {
                if (data.success) {
                    document.getElementById("unregisteredTenantFullName").value = data.tenant.fullName || ""
                    document.getElementById("unregisteredTenantPhone").value = data.tenant.phone || ""
                    document.getElementById("unregisteredTenantCccdNumber").value = data.tenant.cccdNumber || ""
                    document.getElementById("unregisteredTenantBirthday").value = data.tenant.birthday || ""
                    document.getElementById("unregisteredTenantIssueDate").value = data.tenant.issueDate || ""
                    document.getElementById("unregisteredTenantIssuePlace").value = data.tenant.issuePlace || ""
                    document.getElementById("unregisteredTenantStreet").value = data.tenant.street || ""
                    document.getElementById("unregisteredTenantProvince").value = data.tenant.province || ""
                    this.loadDistricts(data.tenant.province, "unregisteredTenantDistrict", "unregisteredTenantWard")
                    setTimeout(() => {
                        document.getElementById("unregisteredTenantDistrict").value = data.tenant.district || ""
                        this.loadWards(data.tenant.district, "unregisteredTenantWard")
                        setTimeout(() => {
                            document.getElementById("unregisteredTenantWard").value = data.tenant.ward || ""
                            this.updateAddress("tenant")
                        }, 200)
                    }, 200)

                    const modalElement = document.getElementById("addCustomerModal-host")
                    const modal = bootstrap.Modal.getInstance(modalElement)
                    if (modal) {
                        modal.hide()
                    }
                    // Đảm bảo cleanup sau khi đóng
                    setTimeout(() => {
                        this.cleanupModalBackdrop()
                    }, 300)

                    document.getElementById("tenantType").value = "UNREGISTERED"
                    this.toggleTenantFields()
                    this.showNotification("Đã thêm thông tin người thuê thành công!", "success")
                } else {
                    this.showNotification(data.message || "Lỗi khi thêm người thuê!", "error")
                }
            })
            .catch((error) => {
                console.error("Error saving unregistered tenant:", error)
                this.showNotification("Lỗi khi thêm người thuê: " + error.message, "error")
            })
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

class ContractPreview {
    constructor() {
        this.initializePreview();
        this.bindEvents();
    }

    initializePreview() {
        this.updatePreview();
    }

    bindEvents() {
        const formInputs = document.querySelectorAll('#contract-form input, #contract-form select, #contract-form textarea');

        formInputs.forEach(input => {
            input.addEventListener('input', () => this.updatePreview());
            input.addEventListener('change', () => this.updatePreview());
        });

        this.initializeZoomControls();
    }

    updatePreview() {
        try {
            this.updateOwnerInfo();
            this.updateTenantInfo();
            this.updateRoomInfo();
            this.updateContractInfo();
            this.updateAmenities();
            this.updateTerms();
        } catch (error) {
            console.error('Error updating preview:', error);
        }
    }

    updateOwnerInfo() {
        // Tên chủ nhà
        const ownerName = document.getElementById('owner-name')?.value || '........................';
        this.setPreviewText('preview-owner-name', ownerName);
        this.setPreviewText('preview-owner-signature', ownerName);

        // Ngày sinh chủ nhà
        const ownerDob = document.getElementById('owner-dob')?.value;
        this.setPreviewText('preview-owner-dob', this.formatDate(ownerDob));

        // CCCD chủ nhà - SỬA: từ 'owner-cccd' thành 'owner-id'
        const ownerCccd = document.getElementById('owner-id')?.value;
        this.setPreviewText('preview-owner-id', ownerCccd || '........................');

        // Ngày cấp CCCD
        const ownerIdDate = document.getElementById('owner-id-date')?.value;
        this.setPreviewText('preview-owner-id-date', this.formatDate(ownerIdDate));

        // Nơi cấp CCCD
        const ownerIdPlace = document.getElementById('owner-id-place')?.value;
        this.setPreviewText('preview-owner-id-place', ownerIdPlace || '........................');

        // Địa chỉ chủ nhà
        const ownerAddress = this.getFullAddress('owner');
        this.setPreviewText('preview-owner-address', ownerAddress);

        // Số điện thoại chủ nhà
        const ownerPhone = document.getElementById('owner-phone')?.value;
        this.setPreviewText('preview-owner-phone', ownerPhone || '........................');
    }

    updateTenantInfo() {
        // Tên người thuê
        const tenantName = document.getElementById('tenant-name')?.value;
        this.setPreviewText('preview-tenant-name', tenantName || '........................');
        this.setPreviewText('preview-tenant-signature', tenantName || '........................');

        // Ngày sinh người thuê
        const tenantDob = document.getElementById('tenant-dob')?.value;
        this.setPreviewText('preview-tenant-dob', this.formatDate(tenantDob));

        // CCCD người thuê - SỬA: từ 'tenant-cccd' thành 'tenant-id'
        const tenantCccd = document.getElementById('tenant-id')?.value;
        this.setPreviewText('preview-tenant-id', tenantCccd || '........................');

        // Ngày cấp CCCD người thuê
        const tenantIdDate = document.getElementById('tenant-id-date')?.value;
        this.setPreviewText('preview-tenant-id-date', this.formatDate(tenantIdDate));

        // Nơi cấp CCCD người thuê
        const tenantIdPlace = document.getElementById('tenant-id-place')?.value;
        this.setPreviewText('preview-tenant-id-place', tenantIdPlace || '........................');

        // Địa chỉ người thuê
        const tenantAddress = this.getFullAddress('tenant');
        this.setPreviewText('preview-tenant-address', tenantAddress);

        // Số điện thoại người thuê
        const tenantPhone = document.getElementById('tenant-phone')?.value;
        this.setPreviewText('preview-tenant-phone', tenantPhone || '........................');

        // Danh sách người ở
        this.updateResidents();
    }


    updateResidents() {
        const residentsContainer = document.getElementById('preview-residents-section');
        const residentsSpan = document.getElementById('preview-residents');

        const residentsTable = document.getElementById('residents-table-body');
        if (residentsTable && residentsTable.children.length > 0) {
            const residents = [];
            for (let row of residentsTable.children) {
                const name = row.children[0]?.textContent?.trim();
                const cccd = row.children[1]?.textContent?.trim();
                if (name && cccd && name !== 'Không có dữ liệu') {
                    residents.push(`${name} (CCCD: ${cccd})`);
                }
            }

            if (residents.length > 0) {
                residentsSpan.textContent = residents.join(', ');
                residentsContainer.style.display = 'block';
            } else {
                residentsContainer.style.display = 'none';
            }
        } else {
            residentsContainer.style.display = 'none';
        }
    }

    updateRoomInfo() {
        console.log('🚀 updateRoomInfo() BẮT ĐẦU!');

        const roomSelect = document.getElementById('roomSelect');

        if (roomSelect && roomSelect.selectedOptions[0] && roomSelect.value) {
            const selectedOption = roomSelect.selectedOptions[0];
            const selectedText = selectedOption.text;

            console.log('📝 Selected room text:', selectedText);

            // Parse từ text: "Phòng 000 - Trương hòa, Phường Quận liên chiểu, Quận Hòa minh, Tỉnh quảng nam - 20.000đ"
            let roomNumber = '';
            let roomAddress = '';

            if (selectedText.includes(' - ')) {
                const parts = selectedText.split(' - ');
                roomNumber = parts[0].trim(); // "Phòng 000"

                // Loại bỏ phần giá tiền cuối cùng
                let addressParts = parts.slice(1);
                let fullAddress = addressParts.join(' - ').trim();

                // Tìm và loại bỏ phần giá (có đuôi đ)
                const pricePattern = /\s*-\s*[\d.,]+đ\s*$/;
                roomAddress = fullAddress.replace(pricePattern, '').trim();

                console.log('✅ Parsed roomNumber:', roomNumber);
                console.log('✅ Parsed roomAddress:', roomAddress);
            } else {
                roomNumber = selectedText;
                roomAddress = 'Địa chỉ chưa được cập nhật';
            }

            // ✅ CẬP NHẬT PREVIEW - TRỰC TIẾP
            const previewRoomNumber = document.getElementById('preview-room-number');
            const previewRoomAddress = document.getElementById('preview-room-address');
            const previewRoomArea = document.getElementById('preview-room-area');

            console.log('🔍 Elements found:', {
                previewRoomNumber: !!previewRoomNumber,
                previewRoomAddress: !!previewRoomAddress,
                previewRoomArea: !!previewRoomArea
            });

            if (previewRoomNumber) {
                previewRoomNumber.textContent = roomNumber;
                console.log('✅ Updated preview-room-number:', roomNumber);
            } else {
                console.error('❌ preview-room-number element not found!');
            }

            if (previewRoomAddress) {
                previewRoomAddress.textContent = roomAddress;
                console.log('✅ Updated preview-room-address:', roomAddress);
            } else {
                console.error('❌ preview-room-address element not found!');
            }

            // ✅ CẬP NHẬT DIỆN TÍCH
            const roomAreaInput = document.getElementById('room-area')?.value;
            if (roomAreaInput && previewRoomArea) {
                previewRoomArea.textContent = roomAreaInput + ' m²';
                console.log('✅ Updated preview-room-area:', roomAreaInput + ' m²');
            } else if (previewRoomArea) {
                // Lấy từ data attribute nếu có
                const areaFromData = selectedOption.getAttribute('data-area');
                if (areaFromData) {
                    previewRoomArea.textContent = areaFromData + ' m²';
                    console.log('✅ Updated preview-room-area from data:', areaFromData + ' m²');
                }
            }

            console.log('✅ Updated room info:', { roomNumber, roomAddress });

        } else {
            console.warn('⚠️ Không có phòng nào được chọn');
        }

        console.log('✅ updateRoomInfo() KẾT THÚC!');
    }




// 🔥 THÊM METHOD setPreviewText nếu chưa có:
    setPreviewText(elementId, text) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = text || '........................';
            console.log(`✅ Updated ${elementId}:`, text);
        } else {
            console.warn(`❌ Element not found: ${elementId}`);
        }
    }


    updateContractInfo() {
        // Ngày ký hợp đồng
        const signDate = document.getElementById('sign-date')?.value;
        this.setPreviewText('preview-sign-date', this.formatDate(signDate));

        // Thời hạn thuê
        const duration = document.getElementById('duration')?.value;
        this.setPreviewText('preview-duration', duration || '........................');

        // Ngày bắt đầu
        const startDate = document.getElementById('start-date')?.value;
        this.setPreviewText('preview-start-date', this.formatDate(startDate));

        // Ngày kết thúc
        const endDate = document.getElementById('end-date')?.value;
        this.setPreviewText('preview-end-date', this.formatDate(endDate));

        // Giá thuê
        const rent = document.getElementById('rent')?.value;
        this.setPreviewText('preview-rent', this.formatCurrency(rent));

        // Ngày thanh toán - FIX: Lấy đúng giá trị
        const paymentDate = document.getElementById('payment-date')?.value;
        const paymentDateText = paymentDate ? `vào ngày ${paymentDate} hàng tháng` : '........................';
        this.setPreviewText('preview-payment-date', paymentDateText);

        // Phương thức thanh toán - FIX: Lấy text thay vì value
        const paymentMethodSelect = document.getElementById('payment-method');
        const paymentMethod = paymentMethodSelect?.selectedOptions[0]?.text;
        const paymentMethodText = (paymentMethod && paymentMethod !== 'Chọn phương thức') ?
            paymentMethod.toLowerCase() : '........................';
        this.setPreviewText('preview-payment-method', paymentMethodText);

        // Tiền đặt cọc
        const deposit = document.getElementById('deposit')?.value;
        this.setPreviewText('preview-deposit', this.formatCurrency(deposit));

        // Số tháng đặt cọc - FIX: Tính toán đúng
        this.updateDepositMonths();
    }

    updateDepositMonths() {
        const deposit = document.getElementById('deposit')?.value;
        const rent = document.getElementById('rent')?.value;

        if (deposit && rent && rent > 0) {
            const depositAmount = parseFloat(deposit.toString().replace(/[^\d]/g, ''));
            const rentAmount = parseFloat(rent.toString().replace(/[^\d]/g, ''));

            if (depositAmount > 0 && rentAmount > 0) {
                const months = (depositAmount / rentAmount).toFixed(1);
                this.setPreviewText('preview-deposit-months', months);
            } else {
                this.setPreviewText('preview-deposit-months', '........................');
            }
        } else {
            this.setPreviewText('preview-deposit-months', '........................');
        }
    }

    updateAmenities() {
        const amenitiesContainer = document.getElementById('preview-amenities');

        // Lấy tiện ích từ checkboxes
        const amenityCheckboxes = document.querySelectorAll('input[name="amenities"]:checked');
        const selectedAmenities = [];

        amenityCheckboxes.forEach(checkbox => {
            // Lấy text từ label
            const label = checkbox.closest('label')?.textContent?.trim() ||
                checkbox.nextElementSibling?.textContent?.trim() ||
                checkbox.value;
            if (label) {
                selectedAmenities.push(label);
            }
        });

        const amenitiesText = selectedAmenities.length > 0 ?
            selectedAmenities.join(', ') : '........................';
        this.setPreviewText('preview-amenities', amenitiesText);
    }

    updateTerms() {
        const terms = document.getElementById('terms')?.value;
        this.setPreviewText('preview-terms', terms || '........................');
    }

    getFullAddress(type) {
        const street = document.getElementById(`${type}-street`)?.value || '';
        const wardSelect = document.getElementById(`${type}-ward`);
        const districtSelect = document.getElementById(`${type}-district`);
        const provinceSelect = document.getElementById(`${type}-province`);

        // Lấy text thay vì value, và loại bỏ "Chọn..."
        const ward = wardSelect?.selectedOptions[0]?.text || '';
        const district = districtSelect?.selectedOptions[0]?.text || '';
        const province = provinceSelect?.selectedOptions[0]?.text || '';

        const addressParts = [street, ward, district, province]
            .filter(part => part &&
                part !== 'Chọn...' &&
                part !== 'Chọn Tỉnh/Thành phố' &&
                part !== 'Chọn Quận/Huyện' &&
                part !== 'Chọn Phường/Xã');

        return addressParts.length > 0 ? addressParts.join(', ') : '........................';
    }

    formatDate(dateString) {
        if (!dateString) {
            return '........................';
        }

        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) {
                return '........................';
            }
            return date.toLocaleDateString('vi-VN');
        } catch (error) {
            return '........................';
        }
    }


    getRoomNumber() {
        // Thử lấy từ input trước
        const roomNumberInput = document.getElementById('room-number')?.value;
        if (roomNumberInput && roomNumberInput.trim()) {
            return roomNumberInput.trim();
        }

        // Lấy từ selected room option
        const roomSelect = document.getElementById('roomSelect');
        if (roomSelect && roomSelect.selectedOptions[0]) {
            const selectedOption = roomSelect.selectedOptions[0];
            const roomText = selectedOption.textContent || selectedOption.text || '';

            // Extract room number từ text (ví dụ: "Phòng 101 - 25m²" -> "101")
            const roomMatch = roomText.match(/Phòng\s*(\d+)/i);
            if (roomMatch) {
                return roomMatch[1];
            }

            // Hoặc lấy toàn bộ text nếu không match pattern
            if (roomText && roomText !== '-- Chọn phòng trọ --') {
                return roomText;
            }
        }

        return '........................';
    }
    getRoomAddress() {
        console.log('🔍 getRoomAddress() được gọi');

        // Lấy từ room được chọn
        const roomSelect = document.getElementById('roomSelect');
        if (roomSelect && roomSelect.selectedOptions[0] && roomSelect.value) {
            const selectedText = roomSelect.selectedOptions[0].text;
            console.log('📝 Selected room text:', selectedText);

            if (selectedText && selectedText !== '-- Chọn phòng trọ --') {
                // Parse format: "Phòng 000 - Địa chỉ - Giá"
                const parts = selectedText.split(' - ');
                console.log('🔍 Split parts:', parts);

                if (parts.length >= 3) {
                    // Lấy phần giữa (index 1) là địa chỉ
                    const address = parts[1].trim();
                    console.log('✅ Extracted address:', address);
                    return address;
                } else if (parts.length === 2) {
                    // Nếu chỉ có 2 phần, lấy phần sau
                    const address = parts[1].trim();
                    console.log('✅ Extracted address (fallback):', address);
                    return address;
                }
            }
        }

        console.log('❌ No address found, using fallback');
        // Fallback: lấy từ form inputs
        return this.getFullAddress('room');
    }

    getRoomArea() {
        // Thử lấy từ input trước
        const roomAreaInput = document.getElementById('room-area')?.value;
        if (roomAreaInput && roomAreaInput.trim()) {
            return roomAreaInput.trim() + ' m²';
        }

        // Lấy từ selected room option
        const roomSelect = document.getElementById('roomSelect');
        if (roomSelect && roomSelect.selectedOptions[0]) {
            const selectedOption = roomSelect.selectedOptions[0];
            const roomText = selectedOption.textContent || selectedOption.text || '';

            // Extract area từ text (ví dụ: "Phòng 101 - 25m²" -> "25")
            const areaMatch = roomText.match(/(\d+)\s*m²/i);
            if (areaMatch) {
                return areaMatch[1] + ' m²';
            }
        }

        // Lấy từ data attribute nếu có
        const roomSelect2 = document.getElementById('roomSelect');
        if (roomSelect2 && roomSelect2.selectedOptions[0]) {
            const area = roomSelect2.selectedOptions[0].getAttribute('data-area');
            if (area) {
                return area + ' m²';
            }
        }

        return '........................';
    }

    getHostelName() {
        const hostelSelect = document.getElementById('hostelSelect');
        if (hostelSelect && hostelSelect.selectedOptions[0]) {
            const selectedOption = hostelSelect.selectedOptions[0];
            const hostelName = selectedOption.textContent || selectedOption.text || '';

            if (hostelName && hostelName !== '-- Chọn khu trọ --') {
                return hostelName;
            }
        }

        return '........................';
    }

    formatCurrency(amount) {
        if (!amount) {
            return '........................';
        }

        try {
            const number = parseFloat(amount.toString().replace(/[^\d]/g, ''));
            if (isNaN(number) || number === 0) {
                return '........................';
            }
            return number.toLocaleString('vi-VN');
        } catch (error) {
            return '........................';
        }
    }

    setPreviewText(elementId, text) {
        const element = document.getElementById(elementId);
        if (element) {
            element.textContent = text || '........................';
        }
    }

    initializeZoomControls() {
        const previewContainer = document.getElementById('preview-container');
        if (!previewContainer) return;

        let currentZoom = 1;

        const zoomIn = document.getElementById('btn-zoom-in');
        const zoomOut = document.getElementById('btn-zoom-out');
        const resetZoom = document.getElementById('btn-reset-zoom');

        if (zoomIn) {
            zoomIn.addEventListener('click', () => {
                currentZoom = Math.min(currentZoom + 0.1, 2);
                previewContainer.style.transform = `scale(${currentZoom})`;
            });
        }

        if (zoomOut) {
            zoomOut.addEventListener('click', () => {
                currentZoom = Math.max(currentZoom - 0.1, 0.5);
                previewContainer.style.transform = `scale(${currentZoom})`;
            });
        }

        if (resetZoom) {
            resetZoom.addEventListener('click', () => {
                currentZoom = 1;
                previewContainer.style.transform = `scale(${currentZoom})`;
            });
        }
    }
}

// Khởi tạo preview
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(() => {
        window.contractPreview = new ContractPreview();
        console.log('✅ Contract Preview initialized');
    }, 500);
});

// Trigger update khi switch tab
function switchToTab(tabName) {
    // ... existing tab switching code ...

    // Trigger preview update
    if (window.contractPreview) {
        setTimeout(() => {
            window.contractPreview.updatePreview();
            console.log('🔄 Preview updated after tab switch');
        }, 200);
    }
}


document.addEventListener("DOMContentLoaded", () => {
    window.NhaTroContract.init()
})
/* ]]> */