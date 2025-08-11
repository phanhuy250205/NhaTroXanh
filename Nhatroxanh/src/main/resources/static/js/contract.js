/* <![CDATA[ */
window.NhaTroContract = {
    currentTab: "tenantInfo",
    zoomLevel: 1,
    residents: [],
    contractTerms: [], // New array to store individual terms
    unregisteredTenantData: null, // Biến mới để lưu thông tin người bảo hộ tạm thời
    unregisteredTenantCccdFrontFile: null, // File ảnh tạm thời
    unregisteredTenantCccdBackFile: null,

    init() {
        console.log("🚀 Contract form loading...");
        this.setupEventListeners();
        this.setupTermsManagement();
        this.setCurrentDate();
        this.setupAmenityModal();
        this.setupCustomerModal();
        this.setupResidentModal();
        this.initializePreviewUpdates();

        return this.loadProvinces()
            .then(() => {
                console.log("Provinces loaded successfully");

                const pathParts = window.location.pathname.split('/');
                let contractId = null;
                for (let i = pathParts.length - 1; i >= 0; i--) {
                    const part = pathParts[i];
                    if (part && !isNaN(part) && !['form', 'edit', 'create', 'new'].includes(part.toLowerCase())) {
                        contractId = parseInt(part);
                        break;
                    }
                }

                console.log("Contract ID from URL:", contractId);

                if (contractId && contractId > 0) {
                    return fetch(`/api/contracts/edit-data/${contractId}`, {
                        method: 'GET',
                        headers: {
                            'Content-Type': 'application/json',
                            'Accept': 'application/json',
                            'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ""
                        }
                    })
                        .then(response => {
                            if (!response.ok) {
                                return response.text().then(text => { throw new Error(`HTTP error! status: ${response.status}, response: ${text}`); });
                            }
                            return response.json();
                        })
                        .then(contract => {
                            console.log("Contract data:", JSON.stringify(contract, null, 2));
                            window.contractData = contract; // Lưu dữ liệu toàn cục

                            // Xử lý tenant
                            if (contract.tenantType === "UNREGISTERED" && contract.unregisteredTenant) {
                                console.log("Initial load: Filling unregistered tenant fields...");
                                this.fillTenantFields(contract.unregisteredTenant);
                            } else if (contract.tenantType === "REGISTERED" && contract.tenant) {
                                console.log("Initial load: Filling registered tenant fields...");
                                this.fillTenantFields(contract.tenant);
                            }

                            // 🔥 THÊM XỬ LÝ RESIDENTS 🔥
                            if (contract.residents && Array.isArray(contract.residents) && contract.residents.length > 0) {
                                console.log("🔍 Loading residents:", contract.residents);
                                // Gán residents vào mảng toàn cục
                                this.residents = contract.residents.map(res => ({
                                    name: res.fullName,
                                    birthYear: res.birthYear,
                                    phone: res.phone || "",
                                    id: res.cccdNumber || "",
                                    notes: "" // Ghi chú không có trong DTO, để trống
                                }));

                                // Hiển thị residents trong UI
                                this.residents.forEach(resident => {
                                    this.addResidentToList(resident);
                                });

                                // Cập nhật số lượng và preview
                                this.updateResidentsCount();
                                this.updateResidentsPreview();
                                console.log("✅ Loaded", this.residents.length, "residents into UI");
                            } else {
                                console.log("🔍 No residents found in contract data");
                                this.residents = [];
                                this.updateResidentsCount();
                                this.updateResidentsPreview();
                            }

                            console.log("🚀 Contract data saved, and initial form filled.");
                        });
                } else {
                    console.log("No valid contract ID found, skipping data load (create mode)");
                    return Promise.resolve();
                }
            })
            .catch(error => {
                console.error("Error during initialization:", error);
                this.showNotification("Lỗi khi khởi tạo form: " + error.message, "error");
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
// Thêm vào setupEventListeners()
        document.getElementById("btn-verify-cccd")?.addEventListener("click", () => {
            this.verifyCCCDNumber();
        });

        // 🔥 THÊM EVENT LISTENER CHO CCCD VERIFICATION
        document.getElementById("cccd-front")?.addEventListener("change", (e) => {
            this.previewImage(e, "cccd-front-preview");
            // Tự động verify khi upload ảnh mới
            this.autoVerifyCCCD();
        });

        document.getElementById("cccd-back")?.addEventListener("change", (e) => {
            this.previewImage(e, "cccd-back-preview");
            // Tự động verify khi upload ảnh mới
            this.autoVerifyCCCD();
        });


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

    // 🔥 THÊM HÀM Tự ĐỘNG VERIFY CCCD
    async autoVerifyCCCD() {
        // Đợi một chút để ảnh được preview xong
        setTimeout(async () => {
            await this.verifyCCCDNumber();
        }, 500);
    },

// 🔥 HÀM CHÍNH: VERIFY SỐ CCCD VỚI ẢNH
    async verifyCCCDNumber() {
        console.log("🔍 Starting CCCD verification...");

        try {
            // 1. Lấy số CCCD từ form
            const cccdNumber = this.getCCCDFromForm();
            if (!cccdNumber) {
                this.showCCCDVerificationResult({
                    success: false,
                    error: "Không tìm thấy số CCCD trong form"
                });
                return;
            }

            // 2. Lấy file ảnh CCCD
            const imageFile = this.getCCCDImageFile();
            if (!imageFile) {
                this.showCCCDVerificationResult({
                    success: false,
                    error: "Vui lòng chọn ảnh CCCD để xác thực"
                });
                return;
            }

            // 3. Hiển thị loading
            this.showCCCDVerificationLoading();

            // 4. Extract số từ ảnh bằng OCR
            const extractResult = await this.extractCCCDFromImage(imageFile);
            if (!extractResult.success) {
                this.showCCCDVerificationResult({
                    success: false,
                    error: "Không thể đọc số CCCD từ ảnh: " + extractResult.error
                });
                return;
            }

            // 5. So sánh số form vs số từ ảnh
            const compareResult = this.compareCCCDNumbers(cccdNumber, extractResult.numbers);

            // 6. Hiển thị kết quả
            this.showCCCDVerificationResult({
                success: true,
                formNumber: cccdNumber,
                imageNumbers: extractResult.numbers,
                isMatch: compareResult.isMatch,
                confidence: compareResult.confidence,
                matchType: compareResult.matchType,
                similarity: compareResult.similarity
            });

        } catch (error) {
            console.error("❌ CCCD verification error:", error);
            this.showCCCDVerificationResult({
                success: false,
                error: "Lỗi khi xác thực CCCD: " + error.message
            });
        }
    },

// 🔍 LẤY SỐ CCCD TỪ FORM
    getCCCDFromForm() {
        // Ưu tiên lấy từ unregisteredTenantData (người bảo hộ)
        if (this.unregisteredTenantData && this.unregisteredTenantData.cccdNumber) {
            console.log("📋 CCCD from unregistered tenant:", this.unregisteredTenantData.cccdNumber);
            return this.unregisteredTenantData.cccdNumber.replace(/\D/g, '');
        }

        // Lấy từ input tenant-id (người thuê thường)
        const tenantIdInput = document.getElementById("tenant-id");
        if (tenantIdInput && tenantIdInput.value.trim()) {
            const cleanNumber = tenantIdInput.value.replace(/\D/g, '');
            console.log("📋 CCCD from tenant input:", cleanNumber);
            return cleanNumber;
        }

        console.log("❌ No CCCD found in form");
        return null;
    },

// 📷 LẤY FILE ẢNH CCCD
    getCCCDImageFile() {
        // Ưu tiên ảnh mặt trước
        const frontFile = document.getElementById("cccd-front")?.files[0];
        if (frontFile) {
            console.log("📷 Using front CCCD image:", frontFile.name);
            return frontFile;
        }

        // Fallback: ảnh mặt sau
        const backFile = document.getElementById("cccd-back")?.files[0];
        if (backFile) {
            console.log("📷 Using back CCCD image:", backFile.name);
            return backFile;
        }

        console.log("❌ No CCCD image file found");
        return null;
    },

// 🔍 EXTRACT SỐ CCCD TỪ ẢNH (sử dụng Tesseract)
    async extractCCCDFromImage(file) {
        try {
            console.log("🔍 Extracting CCCD number from image...");

            const { data: { text } } = await Tesseract.recognize(
                file,
                'eng', // Chỉ dùng tiếng Anh cho số
                {
                    logger: m => {
                        if (m.status === 'recognizing text') {
                            const progress = Math.round(m.progress * 100);
                            console.log(`📖 OCR Progress: ${progress}%`);
                        }
                    },
                    tessedit_pageseg_mode: 6,
                    tessedit_char_whitelist: '0123456789 \n', // Chỉ số và space
                    tessedit_ocr_engine_mode: 2
                }
            );

            console.log("📝 Raw OCR text:", text);

            // Tìm số CCCD (12 chữ số)
            const numbers = this.findCCCDNumbersInText(text);

            return {
                success: true,
                numbers: numbers,
                rawText: text
            };

        } catch (error) {
            console.error("❌ OCR failed:", error);
            return {
                success: false,
                numbers: [],
                error: error.message
            };
        }
    },

// 🔍 TÌM SỐ CCCD TRONG TEXT
    findCCCDNumbersInText(text) {
        const numbers = [];

        // Làm sạch text - chỉ giữ số và space
        const cleanText = text.replace(/[^\d\s]/g, ' ').replace(/\s+/g, ' ');
        console.log("🧹 Clean OCR text:", cleanText);

        // Pattern tìm 12 chữ số
        const cccdPattern = /\b\d{12}\b/g;
        const matches = cleanText.match(cccdPattern);

        if (matches) {
            const uniqueNumbers = [...new Set(matches)];

            for (const num of uniqueNumbers) {
                if (this.isValidCCCDFormat(num)) {
                    numbers.push(num);
                }
            }
        }

        console.log("🔢 Valid CCCD numbers found:", numbers);
        return numbers;
    },

// ✅ VALIDATE FORMAT SỐ CCCD
    isValidCCCDFormat(number) {
        if (!/^\d{12}$/.test(number)) return false;
        if (/^0{12}$/.test(number) || /^1{12}$/.test(number)) return false;
        if (/(\d)\1{7,}/.test(number)) return false; // Không quá 7 số giống liên tiếp
        return true;
    },

// 📊 SO SÁNH SỐ FORM VỚI SỐ ẢNH
    compareCCCDNumbers(formNumber, imageNumbers) {
        if (imageNumbers.length === 0) {
            return {
                isMatch: false,
                confidence: 0,
                matchType: 'no_image_number',
                similarity: 0
            };
        }

        // Kiểm tra khớp chính xác
        if (imageNumbers.includes(formNumber)) {
            return {
                isMatch: true,
                confidence: 1.0,
                matchType: 'exact',
                similarity: 1.0
            };
        }

        // Kiểm tra khớp gần đúng (cho phép OCR sai)
        let bestSimilarity = 0;
        for (const imageNumber of imageNumbers) {
            const similarity = this.calculateSimilarity(formNumber, imageNumber);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
            }
        }

        const threshold = 0.83; // Chấp nhận nếu giống 83%+ (10/12 số đúng)

        return {
            isMatch: bestSimilarity >= threshold,
            confidence: bestSimilarity,
            matchType: bestSimilarity >= threshold ? 'similar' : 'different',
            similarity: bestSimilarity
        };
    },

// 📏 TÍNH ĐỘ TƯƠNG TỰ
    calculateSimilarity(str1, str2) {
        if (str1.length !== str2.length) return 0;

        let matches = 0;
        for (let i = 0; i < str1.length; i++) {
            if (str1[i] === str2[i]) matches++;
        }

        return matches / str1.length;
    },

// 🔄 HIỂN THỊ LOADING
    showCCCDVerificationLoading() {
        const container = this.getCCCDVerificationContainer();
        container.innerHTML = `
        <div class="alert alert-info">
            <i class="fas fa-spinner fa-spin me-2"></i>
            <strong>Đang xác thực số CCCD...</strong>
            <div class="progress mt-2" style="height: 4px;">
                <div class="progress-bar progress-bar-striped progress-bar-animated" style="width: 100%"></div>
            </div>
        </div>
    `;
    },

// 🎨 HIỂN THỊ KẾT QUẢ VERIFICATION
    showCCCDVerificationResult(result) {
        const container = this.getCCCDVerificationContainer();

        if (!result.success) {
            container.innerHTML = `
            <div class="alert alert-danger">
                <i class="fas fa-times-circle me-2"></i>
                <strong>Lỗi xác thực:</strong> ${result.error}
            </div>
        `;
            return;
        }

        const isValid = result.isMatch;
        const confidencePercent = Math.round(result.confidence * 100);

        let statusIcon, statusText, alertClass;

        if (result.matchType === 'exact') {
            statusIcon = 'fas fa-check-circle';
            statusText = '✅ Số CCCD khớp hoàn toàn';
            alertClass = 'alert-success';
        } else if (result.matchType === 'similar') {
            statusIcon = 'fas fa-check-circle';
            statusText = `✅ Số CCCD khớp (${confidencePercent}%)`;
            alertClass = 'alert-success';
        } else if (result.matchType === 'different') {
            statusIcon = 'fas fa-exclamation-triangle';
            statusText = `❌ Số CCCD không khớp (${confidencePercent}%)`;
            alertClass = 'alert-danger';
        } else {
            statusIcon = 'fas fa-question-circle';
            statusText = '❌ Không tìm thấy số CCCD trong ảnh';
            alertClass = 'alert-warning';
        }

        container.innerHTML = `
        <div class="alert ${alertClass}">
            <div class="d-flex justify-content-between align-items-start">
                <div style="flex: 1;">
                    <div class="d-flex align-items-center mb-2">
                        <i class="${statusIcon} me-2"></i>
                        <strong>${statusText}</strong>
                    </div>
                    
                    <div class="verification-details">
                        <div class="row">
                            <div class="col-md-6">
                                <small class="text-muted">Số trong form:</small><br>
                                <code class="fs-6">${result.formNumber || 'Không có'}</code>
                            </div>
                            <div class="col-md-6">
                                <small class="text-muted">Số từ ảnh:</small><br>
                                ${result.imageNumbers.length > 0 ?
            result.imageNumbers.map(num => `<code class="fs-6">${num}</code>`).join('<br>') :
            '<em class="text-muted">Không tìm thấy</em>'
        }
                            </div>
                        </div>
                    </div>

                    ${confidencePercent > 0 ? `
                        <div class="confidence-bar mt-2">
                            <div class="progress" style="height: 8px;">
                                <div class="progress-bar ${isValid ? 'bg-success' : 'bg-danger'}" 
                                     style="width: ${confidencePercent}%"></div>
                            </div>
                            <small class="text-muted">Độ tin cậy: ${confidencePercent}%</small>
                        </div>
                    ` : ''}
                </div>
                <button type="button" class="btn-close ms-2" aria-label="Close"></button>
            </div>
        </div>
    `;

        // Add close functionality
        container.querySelector('.btn-close')?.addEventListener('click', () => {
            container.innerHTML = '';
        });

        // Highlight form input
        const tenantIdInput = document.getElementById("tenant-id");
        if (tenantIdInput) {
            if (isValid) {
                tenantIdInput.classList.remove('is-invalid');
                tenantIdInput.classList.add('is-valid');
            } else {
                tenantIdInput.classList.remove('is-valid');
                tenantIdInput.classList.add('is-invalid');
            }
        }
    },

// 📍 LẤY CONTAINER HIỂN THỊ KẾT QUẢ
    getCCCDVerificationContainer() {
        let container = document.getElementById('cccd-verification-result');

        if (!container) {
            // Tạo container mới nếu chưa có
            container = document.createElement('div');
            container.id = 'cccd-verification-result';
            container.className = 'mt-3';

            // Chèn sau phần upload ảnh CCCD
            const cccdSection = document.querySelector('.cccd-upload-section') ||
                document.getElementById('cccd-back-preview')?.parentElement?.parentElement;

            if (cccdSection) {
                cccdSection.insertAdjacentElement('afterend', container);
            } else {
                // Fallback: chèn vào cuối form tenant
                const tenantForm = document.querySelector('#tenantInfo .card-body');
                if (tenantForm) {
                    tenantForm.appendChild(container);
                }
            }
        }

        return container;
    },

    async onRoomSelected() {
        const roomSelect = document.getElementById("roomSelect");
        if (!roomSelect) {
            this.showNotification("Không tìm thấy dropdown phòng trọ!", "error");
            return;
        }

        const roomId = roomSelect.value;
        if (!roomId) {
            this.clearRoomFields();
            return;
        }

        console.log("Fetching details for room ID:", roomId);

        try {
            // 🔥 BƯỚC 1: Lấy thông tin chi tiết phòng
            const roomDetailsResponse = await fetch(`/api/contracts/get-room-details?roomId=${roomId}`);

            // ✅ KIỂM TRA CONTENT-TYPE TRƯỚC KHI PARSE
            const contentType = roomDetailsResponse.headers.get("content-type");
            console.log("Room details response content-type:", contentType);

            if (!roomDetailsResponse.ok) {
                const errorText = await roomDetailsResponse.text();
                console.error("Room details error response:", errorText);
                throw new Error(`Lỗi khi lấy chi tiết phòng: ${roomDetailsResponse.status}`);
            }

            if (!contentType || !contentType.includes("application/json")) {
                const responseText = await roomDetailsResponse.text();
                console.error("Expected JSON but got:", responseText.substring(0, 200));
                throw new Error("Server không trả về JSON cho thông tin phòng");
            }

            const roomData = await roomDetailsResponse.json();
            console.log("Room data received:", roomData);

            if (roomData.success && roomData.room) {
                const room = roomData.room;
                document.getElementById("room-number").value = room.roomName || "";
                document.getElementById("room-area").value = room.acreage || "";
                document.getElementById("rent-price").value = room.price || "";

                // Cập nhật preview
                this.updatePreviewField("room-number", "preview-room-number");
                this.updatePreviewField("room-area", "preview-room-area");
                this.updatePreviewField("rent-price", "preview-rent");

                const previewRoomAddress = document.getElementById("preview-room-address");
                if (previewRoomAddress) {
                    previewRoomAddress.textContent = room.address || "Chưa có địa chỉ";
                }

                // 🔥 BƯỚC 2: Lấy tiện ích phòng với error handling
                try {
                    const utilityResponse = await fetch(`/api/contracts/rooms/${roomId}/utilities`);

                    // ✅ KIỂM TRA CONTENT-TYPE CHO UTILITIES
                    const utilityContentType = utilityResponse.headers.get("content-type");
                    console.log("Utilities response content-type:", utilityContentType);
                    console.log("Utilities response status:", utilityResponse.status);

                    if (!utilityResponse.ok) {
                        const errorText = await utilityResponse.text();
                        console.error("Utilities error response:", errorText);

                        // ❌ NẾU API KHÔNG TỒN TẠI (404), BỎ QUA PHẦN UTILITIES
                        if (utilityResponse.status === 404) {
                            console.warn("Utilities API not found, skipping utilities loading");
                            this.showNotification("API tiện ích không khả dụng, bỏ qua tải tiện ích", "warning");
                            return; // Vẫn tiếp tục, chỉ không load utilities
                        }

                        throw new Error(`Lỗi khi lấy tiện ích: ${utilityResponse.status}`);
                    }

                    if (!utilityContentType || !utilityContentType.includes("application/json")) {
                        const responseText = await utilityResponse.text();
                        console.error("Expected JSON for utilities but got:", responseText.substring(0, 200));
                        console.warn("Utilities response is not JSON, skipping utilities loading");
                        return; // Bỏ qua utilities nếu không phải JSON
                    }

                    const utilities = await utilityResponse.json();
                    console.log("Utilities received:", utilities);

                    // Bỏ check tất cả checkbox
                    document.querySelectorAll('#amenities-list-host input[name="contract.room.utilityIds"]').forEach(checkbox => {
                        checkbox.checked = false;
                    });

                    // Tick vào những checkbox tương ứng với tiện ích của phòng
                    if (utilities && Array.isArray(utilities) && utilities.length > 0) {
                        console.log(`Phòng có ${utilities.length} tiện ích.`);
                        const utilityIds = utilities.map(util => util.utilityId || util.id);
                        utilityIds.forEach(id => {
                            const checkbox = document.getElementById(`utility-${id}`);
                            if (checkbox) {
                                checkbox.checked = true;
                                console.log(`Đã tick vào tiện ích ID: ${id}`);
                            } else {
                                console.warn(`Không tìm thấy checkbox cho utility ID: ${id}`);
                            }
                        });

                        this.showNotification(`Đã tải ${utilities.length} tiện ích cho phòng`, "success");
                    } else {
                        console.log("Phòng này không có tiện ích nào.");
                    }

                    // Cập nhật preview tiện ích
                    this.updateAmenities();

                } catch (utilityError) {
                    console.error("Error loading utilities:", utilityError);
                    // ❌ KHÔNG THROW ERROR - chỉ log và thông báo
                    this.showNotification("Không thể tải tiện ích phòng: " + utilityError.message, "warning");
                }

                this.showNotification(`Đã tải thông tin phòng ${room.roomName}`, "success");

            } else {
                this.showNotification(roomData.message || "Không thể lấy thông tin phòng!", "error");
                this.clearRoomFields();
            }

        } catch (error) {
            console.error("Error in onRoomSelected:", error);
            this.showNotification("Lỗi khi tải dữ liệu phòng: " + error.message, "error");
            this.clearRoomFields();
        }
    },


    // Thêm vào object NhaTroContract
    async safeFetchJSON(url, options = {}) {
        try {
            console.log(`🌐 Fetching: ${url}`);
            const response = await fetch(url, options);

            // Kiểm tra content-type
            const contentType = response.headers.get("content-type");
            console.log(`📄 Content-Type: ${contentType}`);
            console.log(`📊 Status: ${response.status}`);

            if (!response.ok) {
                const errorText = await response.text();
                console.error(`❌ Error Response (${response.status}):`, errorText.substring(0, 500));
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            // Kiểm tra xem có phải JSON không
            if (!contentType || !contentType.includes("application/json")) {
                const responseText = await response.text();
                console.error("❌ Expected JSON but received:", responseText.substring(0, 200));
                throw new Error(`Server trả về ${contentType || 'unknown'} thay vì JSON`);
            }

            const data = await response.json();
            console.log("✅ JSON parsed successfully:", data);
            return data;

        } catch (error) {
            console.error(`❌ safeFetchJSON error for ${url}:`, error);
            throw error;
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

        try {
            // ✅ BƯỚC 1: Đảm bảo provinces được load và đợi DOM update
            await this.ensureProvincesLoaded();

            // ✅ BƯỚC 2: Điền dữ liệu cơ bản trước
            this.fillBasicTenantInfo(tenant);

            // ✅ BƯỚC 3: Xử lý địa chỉ với proper error handling
            if (tenant.province) {
                await this.fillTenantAddress(tenant);
            }

            // ✅ BƯỚC 4: Xử lý ảnh CCCD
            this.fillTenantImages(tenant);

            // ✅ BƯỚC 5: Xử lý trạng thái tenant type
            this.handleTenantTypeStatus(tenant);

            // ✅ BƯỚC 6 (ĐÃ SỬA): Gọi hàm cập nhật preview đầy đủ
            // Thay thế hàm updateAllPreview() bằng hàm updateContractPreview()
            if (typeof updateContractPreview === 'function') {
                console.log("Forcing preview update after filling tenant fields...");
                updateContractPreview();
            }

        } catch (error) {
            console.error("Error filling tenant fields:", error);
            this.showNotification("Lỗi khi điền thông tin người thuê: " + error.message, "error");
        }
    },


    // ✅ HÀM PHỤ: Xử lý trạng thái tenant type với validation
    handleTenantTypeStatus(tenant) {
        console.log("🏷️ Handling tenant type status...");

        try {
            const guardianDisplayContainer = document.getElementById("guardian-display-container");
            const guardianDisplayName = document.getElementById("guardian-display-name");
            const btnAddCustomerHost = document.getElementById("btn-add-customer-host");
            const tenantTypeSelect = document.getElementById("tenantType");
            const btnEditGuardian = document.getElementById("btn-edit-guardian");
            const btnDeleteGuardian = document.getElementById("btn-delete-guardian");

            if (!guardianDisplayContainer || !btnAddCustomerHost || !tenantTypeSelect) {
                console.warn("❌ Missing critical elements for tenant type status handling.");
                return;
            }
            if (!('userId' in tenant)) {
                // Unregistered tenant
                this.unregisteredTenantData = { ...tenant };
                console.log("✅ Set as UNREGISTERED tenant");

                if (guardianDisplayContainer) {
                    guardianDisplayName.textContent = tenant.fullName;
                    $(guardianDisplayContainer).removeClass('d-none').show();
                }

                $(btnAddCustomerHost).hide();
                tenantTypeSelect.value = "UNREGISTERED";
                this.toggleTenantInputFields(false); // Vô hiệu hóa các trường input

                if (btnEditGuardian) {
                    $(btnEditGuardian).off('click').on('click', () => {
                        this.openEditCustomerModal(this.unregisteredTenantData);
                    });
                }

                if (btnDeleteGuardian) {
                    $(btnDeleteGuardian).off('click').on('click', () => {
                        this.clearUnregisteredTenantData();
                    });
                }

            } else {
                // Registered tenant (có userId)
                this.unregisteredTenantData = null;
                console.log("✅ Set as REGISTERED tenant");

                $(guardianDisplayContainer).hide();
                $(btnAddCustomerHost).show();
                tenantTypeSelect.value = "REGISTERED";
                this.toggleTenantInputFields(true); // Kích hoạt lại các trường input
            }

        } catch (error) {
            console.error("❌ Error handling tenant type status:", error);
            this.showNotification("Lỗi khi xử lý trạng thái tenant: " + error.message, "error");
        }
    },

    // ✅ HÀM PHỤ: Xử lý ảnh CCCD với validation
    fillTenantImages(tenant) {
        console.log("🖼️ Filling tenant images...");

        const frontPreview = document.getElementById("cccd-front-preview");
        const backPreview = document.getElementById("cccd-back-preview");

        // Kiểm tra phần tử DOM
        if (!frontPreview) {
            console.warn("❌ cccd-front-preview element not found");
            return;
        }
        if (!backPreview) {
            console.warn("❌ cccd-back-preview element not found");
            return;
        }

        try {
            // Xóa nội dung cũ
            frontPreview.innerHTML = '';
            backPreview.innerHTML = '';

            // Tạo base URL
            const baseUrl = window.location.origin;
            const cccdFrontUrl = tenant.cccdFrontUrl ?
                `${baseUrl}${tenant.cccdFrontUrl.replace('/uploads/', '/uploads/cccd/')}` : null;
            const cccdBackUrl = tenant.cccdBackUrl ?
                `${baseUrl}${tenant.cccdBackUrl.replace('/uploads/', '/uploads/cccd/')}` : null;

            // Hàm tạo thẻ img với xử lý lỗi
            const createImageElement = (url, alt) => {
                const img = document.createElement('img');
                img.src = url;
                img.alt = alt;
                img.style.maxWidth = '100%';
                img.style.maxHeight = '200px';
                img.style.height = 'auto';
                img.style.objectFit = 'contain';
                img.style.borderRadius = '8px';
                img.onerror = () => {
                    console.error(`❌ Failed to load image: ${url}`);
                    img.parentElement.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Không tải được ảnh</div>`;
                    if (typeof this.showNotification === 'function') {
                        this.showNotification(`Không tải được ảnh ${alt}`, 'error');
                    }
                };
                return img;
            };

            // Hiển thị ảnh mặt trước
            if (cccdFrontUrl) {
                frontPreview.appendChild(createImageElement(cccdFrontUrl, 'CCCD Front'));
                console.log("✅ CCCD front image set");
            } else {
                frontPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt trước</div>`;
            }

            // Hiển thị ảnh mặt sau
            if (cccdBackUrl) {
                backPreview.appendChild(createImageElement(cccdBackUrl, 'CCCD Back'));
                console.log("✅ CCCD back image set");
            } else {
                backPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt sau</div>`;
            }

        } catch (error) {
            console.error("❌ Error filling tenant images:", error);
            if (typeof this.showNotification === 'function') {
                this.showNotification("Lỗi khi hiển thị ảnh CCCD: " + error.message, "error");
            }
        }
    },

    // ✅ HÀM PHỤ: Đảm bảo provinces được load
    async ensureProvincesLoaded() {
        const tenantProvinceSelect = document.getElementById("tenant-province");

        if (!tenantProvinceSelect || tenantProvinceSelect.options.length <= 1) {
            console.log("Loading provinces...");
            await this.loadProvinces();

            // ✅ QUAN TRỌNG: Đợi DOM update
            await new Promise(resolve => setTimeout(resolve, 200));

            // Kiểm tra lại
            if (tenantProvinceSelect.options.length <= 1) {
                throw new Error("Không thể tải danh sách tỉnh/thành phố");
            }
        }

        console.log("✅ Provinces ready with", tenantProvinceSelect.options.length, "options");
    },

    // ✅ HÀM PHỤ: Điền thông tin cơ bản
    // ✅ HÀM PHỤ: Điền thông tin cơ bản với validation
    fillBasicTenantInfo(tenant) {
        console.log("🔍 Filling basic tenant info...");

        // ✅ DANH SÁCH TẤT CẢ ELEMENTS CẦN KIỂM TRA
        const elementMappings = [

            // Input elements
            { id: "tenant-name", value: tenant.fullName, type: "input" },
            { id: "tenant-phone", value: tenant.phone, type: "input" },
            { id: "tenant-id", value: tenant.cccdNumber, type: "input" },
            { id: "tenant-dob", value: tenant.birthday, type: "input" },
            { id: "tenant-id-date", value: tenant.issueDate, type: "input" },
            { id: "tenant-id-place", value: tenant.issuePlace, type: "input" },
            { id: "tenant-email", value: tenant.email, type: "input" },
            { id: "tenant-street", value: tenant.street, type: "input" }
        ];

        // ✅ KIỂM TRA VÀ SET GIÁ TRỊ CHO TỪNG ELEMENT
        const missingElements = [];

        elementMappings.forEach(mapping => {
            const element = document.getElementById(mapping.id);

            if (!element) {
                missingElements.push(mapping.id);
                console.warn(`❌ Element not found: ${mapping.id}`);
                return;
            }

            try {
                if (mapping.type === "display") {
                    element.textContent = mapping.value || "........................";
                    console.log(`✅ Set display ${mapping.id}:`, mapping.value);
                } else if (mapping.type === "input") {
                    element.value = mapping.value || "";
                    console.log(`✅ Set input ${mapping.id}:`, mapping.value);
                }
            } catch (error) {
                console.error(`❌ Error setting ${mapping.id}:`, error);
            }
        });

        // ✅ BÁO CÁO CÁC ELEMENTS THIẾU
        if (missingElements.length > 0) {
            console.error("❌ Missing elements in DOM:", missingElements);
            this.showNotification(
                `Một số trường hiển thị không tồn tại: ${missingElements.join(', ')}. Vui lòng kiểm tra HTML.`,
                "warning"
            );
        }

        console.log("✅ Basic tenant info filled successfully");
    },

    // ✅ HÀM PHỤ: Xử lý địa chỉ với proper validation
    async fillTenantAddress(tenant) {
        const tenantProvinceSelect = document.getElementById("tenant-province");

        if (!tenantProvinceSelect) {
            throw new Error("Không tìm thấy dropdown tỉnh/thành phố");
        }

        try {
            // ✅ XỬ LÝ PROVINCE
            const provinceCode = await this.mapProvinceNameToCode(tenant.province);
            console.log("Province code mapped:", provinceCode);

            if (provinceCode) {
                // ✅ KIỂM TRA OPTION TỒN TẠI TRƯỚC KHI SET
                const provinceOption = tenantProvinceSelect.querySelector(`option[value="${provinceCode}"]`);
                if (!provinceOption) {
                    console.warn("Province option not found:", provinceCode);
                    this.showNotification(`Không tìm thấy tỉnh: ${tenant.province}`, "warning");
                    return;
                }

                tenantProvinceSelect.value = provinceCode;
                console.log("✅ Province set successfully");

                // ✅ XỬ LÝ DISTRICT
                if (tenant.district) {
                    await this.loadDistricts(provinceCode, "tenant-district", "tenant-ward");
                    await this.fillTenantDistrict(tenant, provinceCode);
                }
            } else {
                console.warn("Could not map province name to code:", tenant.province);
                this.showNotification(`Không thể ánh xạ tỉnh: ${tenant.province}`, "warning");
            }

        } catch (error) {
            console.error("Error filling tenant address:", error);
            throw new Error("Lỗi khi xử lý địa chỉ: " + error.message);
        }
    },

    // ✅ HÀM PHỤ: Xử lý district và ward
    async fillTenantDistrict(tenant, provinceCode) {
        const tenantDistrictSelect = document.getElementById("tenant-district");

        if (!tenantDistrictSelect || !tenant.district) return;

        try {
            const districtCode = await this.mapDistrictNameToCode(provinceCode, tenant.district);
            console.log("District code mapped:", districtCode);

            if (districtCode) {
                // ✅ ĐỢI DISTRICTS LOAD XONG
                await new Promise(resolve => setTimeout(resolve, 100));

                const districtOption = tenantDistrictSelect.querySelector(`option[value="${districtCode}"]`);
                if (districtOption) {
                    tenantDistrictSelect.value = districtCode;
                    console.log("✅ District set successfully");

                    // ✅ XỬ LÝ WARD
                    if (tenant.ward) {
                        await this.loadWards(districtCode, "tenant-ward");
                        await this.fillTenantWard(tenant, districtCode, provinceCode);
                    }
                } else {
                    console.warn("District option not found:", districtCode);
                }
            }
        } catch (error) {
            console.error("Error filling district:", error);
        }
    },

    // ✅ HÀM PHỤ: Xử lý ward
    async fillTenantWard(tenant, districtCode, provinceCode) {
        const tenantWardSelect = document.getElementById("tenant-ward");

        if (!tenantWardSelect || !tenant.ward) return;

        try {
            const wardCode = await this.mapWardNameToCode(districtCode, tenant.ward, provinceCode);
            console.log("Ward code mapped:", wardCode);

            if (wardCode) {
                // ✅ ĐỢI WARDS LOAD XONG
                await new Promise(resolve => setTimeout(resolve, 100));

                const wardOption = tenantWardSelect.querySelector(`option[value="${wardCode}"]`);
                if (wardOption) {
                    tenantWardSelect.value = wardCode;
                    console.log("✅ Ward set successfully");
                } else {
                    console.warn("Ward option not found:", wardCode);
                }
            }
        } catch (error) {
            console.error("Error filling ward:", error);
        }
    },




    openEditCustomerModal(data) {
        const modalElement = document.getElementById("addCustomerModal-host");
        const modal = new bootstrap.Modal(modalElement);
        if (modal) {
            modal.show();
        }

        // Điền dữ liệu vào form modal
        document.getElementById("newCustomer-name").value = data.fullName || "";
        document.getElementById("newCustomer-dob").value = data.birthday || "";
        document.getElementById("newCustomer-id").value = data.cccdNumber || "";
        document.getElementById("newCustomer-id-date").value = data.issueDate || "";
        document.getElementById("newCustomer-id-place").value = data.issuePlace || "";
        document.getElementById("newCustomer-phone").value = data.phone || "";
        document.getElementById("newCustomer-email").value = data.email || "";
        document.getElementById("newCustomer-street").value = data.street || "";

        // Tải lại các dropdown địa chỉ và set giá trị
        this.loadProvinces().then(() => {
            const newCustomerProvinceSelect = document.getElementById("newCustomer-province");
            if (newCustomerProvinceSelect && data.province) {
                this.mapProvinceNameToCode(data.province).then(pCode => {
                    if (pCode) {
                        newCustomerProvinceSelect.value = pCode;
                        this.loadDistricts(pCode, "newCustomer-district", "newCustomer-ward").then(() => {
                            const newCustomerDistrictSelect = document.getElementById("newCustomer-district");
                            if (newCustomerDistrictSelect && data.district) {
                                this.mapDistrictNameToCode(pCode, data.district).then(dCode => {
                                    if (dCode) {
                                        newCustomerDistrictSelect.value = dCode;
                                        this.loadWards(dCode, "newCustomer-ward").then(() => {
                                            const newCustomerWardSelect = document.getElementById("newCustomer-ward");
                                            if (newCustomerWardSelect && data.ward) {
                                                this.mapWardNameToCode(dCode, data.ward, pCode).then(wCode => {
                                                    if (wCode) {
                                                        newCustomerWardSelect.value = wCode;
                                                    }
                                                });
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });

        // Hiển thị ảnh CCCD nếu có URL
        const newCustomerCccdFrontPreview = document.getElementById("newCustomer-cccd-front-preview");
        const newCustomerCccdBackPreview = document.getElementById("newCustomer-cccd-back-preview");

        if (data.cccdFrontUrl) {
            newCustomerCccdFrontPreview.innerHTML = `<img src="${window.location.origin}${data.cccdFrontUrl.replace(/ /g, '%20')}" alt="CCCD Front" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`;
        } else {
            newCustomerCccdFrontPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt trước</div>`;
        }

        if (data.cccdBackUrl) {
            newCustomerCccdBackPreview.innerHTML = `<img src="${window.location.origin}${data.cccdBackUrl.replace(/ /g, '%20')}" alt="CCCD Back" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`;
        } else {
            newCustomerCccdBackPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt sau</div>`;
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
    buildAddressString(street, ward, district, province) {
        console.log("🏠 Building address string...");

        const addressParts = [street, ward, district, province]
            .filter(part => part && part.trim() !== "")
            .map(part => part.trim());

        const fullAddress = addressParts.join(", ");
        console.log("✅ Built address:", fullAddress);

        return fullAddress || "Chưa có thông tin địa chỉ";
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
        console.log("💾 Starting contract save process...");

        // ✅ BƯỚC 1: Validate cơ bản
        if (!this.validateBasicContractInfo()) {
            return;
        }

        // ✅ BƯỚC 2: Lấy và validate room
        const { roomIdNumber, roomSelect } = this.getRoomInfo();
        if (!roomIdNumber) {
            return;
        }

        // ✅ BƯỚC 3: Build contract data
        const contractData = this.buildContractData(roomIdNumber, roomSelect);

        // ✅ BƯỚC 4: Validate tenant data
        if (!this.validateTenantData(contractData)) {
            return;
        }

        // ✅ BƯỚC 5: Prepare form data
        const formData = this.prepareFormData(contractData);

        // ✅ BƯỚC 6: Show loading state
        this.setLoadingState(true);

        // ✅ BƯỚC 7: Send request
        this.sendSaveRequest(formData);
    },

    // ✅ HÀM PHỤ: Validate thông tin cơ bản
    validateBasicContractInfo() {
        const requiredFields = [
            { id: 'tenant-name', name: 'Tên người thuê' },
            { id: 'tenant-phone', name: 'Số điện thoại người thuê' },
            { id: 'owner-name', name: 'Tên chủ trọ' },
            { id: 'owner-phone', name: 'Số điện thoại chủ trọ' },
            { id: 'rent-price', name: 'Giá thuê' },
            { id: 'start-date', name: 'Ngày bắt đầu hợp đồng' }
        ];

        for (const field of requiredFields) {
            const element = document.getElementById(field.id);
            if (!element || !element.value || element.value.trim() === '') {
                this.showNotification(`❌ Vui lòng điền: ${field.name}`, "error");
                element?.focus();
                return false;
            }
        }

        return true;
    },

    // ✅ HÀM PHỤ: Lấy thông tin phòng
    getRoomInfo() {
        const roomSelect = document.getElementById('roomSelect');
        const roomIdValue = roomSelect?.value;

        if (!roomIdValue || roomIdValue.trim() === "" || roomIdValue === "null" || roomIdValue === "undefined") {
            this.showNotification("❌ Vui lòng chọn phòng trọ!", "error");
            roomSelect?.focus();
            return { roomIdNumber: null, roomSelect: null };
        }

        const roomIdNumber = parseInt(roomIdValue, 10);
        if (isNaN(roomIdNumber) || roomIdNumber <= 0) {
            this.showNotification("❌ ID phòng không hợp lệ!", "error");
            return { roomIdNumber: null, roomSelect: null };
        }

        console.log("✅ Room validated:", roomIdNumber);
        return { roomIdNumber, roomSelect };
    },

    // ✅ HÀM PHỤ: Validate dữ liệu người thuê
    validateTenantData(contractData) {
        const tenantPhone = contractData.tenantType === "UNREGISTERED" ?
            this.unregisteredTenantData?.phone : contractData.tenant?.phone;

        if (!tenantPhone) {
            this.showNotification("❌ Số điện thoại người thuê không được để trống!", "error");
            document.getElementById('tenant-phone')?.focus();
            return false;
        }

        const cccdNumber = contractData.tenantType === "UNREGISTERED" ?
            this.unregisteredTenantData?.cccdNumber : contractData.tenant?.cccdNumber;

        if (!cccdNumber) {
            this.showNotification("❌ Số CCCD không được để trống!", "error");
            document.getElementById('tenant-id')?.focus();
            return false;
        }

        // Validate CCCD format (12 digits)
        if (!/^\d{12}$/.test(cccdNumber)) {
            this.showNotification("❌ Số CCCD phải có đúng 12 chữ số!", "error");
            document.getElementById('tenant-id')?.focus();
            return false;
        }

        console.log("✅ Tenant data validated");
        return true;
    },

    // ✅ HÀM PHỤ: Chuẩn bị form data
    prepareFormData(contractData) {
        const formData = new FormData();
        formData.append("contract", JSON.stringify(contractData));

        // Thêm file CCCD
        if (contractData.tenantType === "UNREGISTERED") {
            // Người bảo hộ - lấy file từ biến tạm
            if (this.unregisteredTenantCccdFrontFile) {
                formData.append("cccdFrontFile", this.unregisteredTenantCccdFrontFile);
                console.log("📎 Added unregistered tenant front CCCD file");
            }
            if (this.unregisteredTenantCccdBackFile) {
                formData.append("cccdBackFile", this.unregisteredTenantCccdBackFile);
                console.log("📎 Added unregistered tenant back CCCD file");
            }
        } else {
            // Người thuê đã đăng ký - lấy file từ input
            const cccdFront = document.getElementById("cccd-front")?.files[0];
            const cccdBack = document.getElementById("cccd-back")?.files[0];

            if (cccdFront) {
                formData.append("cccdFrontFile", cccdFront);
                console.log("📎 Added registered tenant front CCCD file:", cccdFront.name);
            }
            if (cccdBack) {
                formData.append("cccdBackFile", cccdBack);
                console.log("📎 Added registered tenant back CCCD file:", cccdBack.name);
            }
        }

        // Debug form data
        console.log("=== FORM DATA PREPARED ===");
        for (let [key, value] of formData.entries()) {
            console.log(`${key}: ${value instanceof File ? `${value.name} (${value.size} bytes)` : value}`);
        }

        return formData;
    },

    // ✅ HÀM PHỤ: Set loading state
    setLoadingState(isLoading) {
        const saveButton = document.getElementById('btn-save');
        const sendEmailButton = document.getElementById('btn-send-email');

        if (saveButton) {
            saveButton.disabled = isLoading;
            saveButton.innerHTML = isLoading
                ? '<i class="fa fa-spinner fa-spin me-1"></i> Đang lưu...'
                : '<i class="fa fa-floppy-disk me-1"></i> Lưu hợp đồng';
        }

        // Disable send email button khi đang save
        if (sendEmailButton) {
            sendEmailButton.disabled = isLoading;
        }

        console.log(isLoading ? "🔄 Loading state ON" : "✅ Loading state OFF");
    },

    // ✅ HÀM PHỤ: Gửi request save
    async sendSaveRequest(formData) {
        try {
            console.log("📤 Sending save request to /api/contracts...");

            const response = await fetch("/api/contracts", {
                method: "POST",
                body: formData,
                headers: {
                    'X-CSRF-TOKEN': document.querySelector('meta[name="_csrf"]')?.content || ""
                }
            });

            console.log("📥 Response status:", response.status);

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: 'Unknown error' }));
                throw new Error(errorData.message || `HTTP error! status: ${response.status}`);
            }

            const data = await response.json();
            console.log("📥 Response data:", data);

            await this.handleSaveSuccess(data);

        } catch (error) {
            console.error("❌ Save error:", error);
            this.handleSaveError(error);
        } finally {
            this.setLoadingState(false);
        }
    },

    // ✅ HÀM PHỤ: Xử lý khi save thành công
    async handleSaveSuccess(data) {
        if (data.success) {
            console.log("✅ Contract saved successfully!");

            // Hiển thị thông báo thành công
            this.showNotification("✅ Hợp đồng đã được lưu thành công!", "success");

            // Cập nhật URL nếu có contractId (tạo mới)
            if (data.contractId) {
                const newUrl = `/contracts/edit/${data.contractId}`;
                window.history.replaceState({ contractId: data.contractId }, '', newUrl);
                console.log("🔗 URL updated to:", newUrl);

                // Lưu contractId để sử dụng cho các chức năng khác
                this.currentContractId = data.contractId;
            }

            // Cập nhật preview và enable nút gửi email
            try {
                this.updateAllPreview();

                // Enable send email button nếu có email
                const tenantEmail = document.getElementById('tenant-email')?.value?.trim();
                const sendEmailButton = document.getElementById('btn-send-email');
                if (sendEmailButton && tenantEmail) {
                    sendEmailButton.disabled = false;
                    sendEmailButton.classList.remove('btn-secondary');
                    sendEmailButton.classList.add('btn-success');
                    console.log("📧 Send email button enabled");
                }

            } catch (previewError) {
                console.warn("⚠️ Preview update failed:", previewError);
            }

            // ✅ QUAN TRỌNG: KHÔNG CHUYỂN TRANG - chỉ thông báo thành công
            console.log("🏠 Staying on current page as requested");

        } else {
            throw new Error(data.message || "Phản hồi không hợp lệ từ server");
        }
    },

    // ✅ HÀM PHỤ: Xử lý khi save lỗi
    handleSaveError(error) {
        console.error("❌ Contract save failed:", error);

        let errorMessage = "Lỗi khi lưu hợp đồng: ";

        if (error.message.includes('duplicate')) {
            errorMessage += "Hợp đồng đã tồn tại hoặc trùng lặp dữ liệu!";
        } else if (error.message.includes('validation')) {
            errorMessage += "Dữ liệu không hợp lệ, vui lòng kiểm tra lại!";
        } else if (error.message.includes('network')) {
            errorMessage += "Lỗi kết nối mạng, vui lòng thử lại!";
        } else {
            errorMessage += error.message;
        }

        this.showNotification(errorMessage, "error");

        // Focus vào trường đầu tiên có lỗi (nếu có)
        const firstErrorField = document.querySelector('.is-invalid, .error');
        if (firstErrorField) {
            firstErrorField.focus();
        }
    },




    buildContractData(roomIdNumber, roomSelect) {
        const contractData = {};

        // Room (giữ nguyên)
        if (roomIdNumber && roomSelect && roomSelect.selectedIndex >= 0) {
            const selectedOption = roomSelect.options[roomSelect.selectedIndex];
            contractData.room = {
                roomId: roomIdNumber,
                roomName: selectedOption.dataset.roomName || `Phòng ${roomIdNumber}`,
                price: parseFloat(selectedOption.dataset.price) || undefined,
                area: parseFloat(selectedOption.dataset.area) || undefined,
                status: selectedOption.dataset.status || undefined
            };

            // Thu thập tiện ích
            const selectedUtilityIds = [];
            document.querySelectorAll('#amenities-list-host input[name="contract.room.utilityIds"]:checked').forEach(checkbox => {
                selectedUtilityIds.push(parseInt(checkbox.value));
            });

            if (selectedUtilityIds.length > 0) {
                contractData.room.utilityIds = selectedUtilityIds;
                console.log("🛠️ Tiện ích đã chọn (IDs):", selectedUtilityIds);
            }
        }

        // ✅ SỬA: Contract Date - Lấy từ hidden input
        const contractDateValue = document.getElementById("contract-date-value")?.value?.trim();
        if (contractDateValue) {
            contractData.contractDate = contractDateValue;
            console.log("📅 Contract date from hidden input:", contractDateValue);
        } else {
            // Fallback
            contractData.contractDate = new Date().toISOString().split('T')[0];
            console.log("📅 Using current date as fallback:", contractData.contractDate);
        }

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

        // Tính endDate
        if (terms.startDate && terms.duration) {
            const start = new Date(terms.startDate);
            start.setMonth(start.getMonth() + terms.duration);
            terms.endDate = start.toISOString().split("T")[0];
        }

        const termsText = this.getContractTermsText ? this.getContractTermsText() : undefined;
        if (termsText) terms.terms = termsText;

        // Payment method và date
        const paymentMethod = document.getElementById("payment-method")?.value?.trim();
        if (paymentMethod) contractData.paymentMethod = paymentMethod;

        const paymentDate = document.getElementById("payment-date")?.value?.trim();
        if (paymentDate) {
            terms.paymentDateDescription = paymentDate;
            console.log("💳 Payment date description:", paymentDate);
        }

        if (Object.keys(terms).length > 0) {
            contractData.terms = terms;
        }

        // Tenant (giữ nguyên code của bạn)
        if (this.unregisteredTenantData) {
            contractData.tenantType = "UNREGISTERED";
            contractData.unregisteredTenant = { ...this.unregisteredTenantData };
        } else {
            contractData.tenantType = "REGISTERED";
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
            if (tenantWard && tenantWard !== "Chọn Phường/Xã") tenant.ward = tenantWard;

            const tenantDistrict = document.getElementById("tenant-district")?.options[document.getElementById("tenant-district")?.selectedIndex]?.text?.trim();
            if (tenantDistrict && tenantDistrict !== "Chọn Quận/Huyện") tenant.district = tenantDistrict;

            const tenantProvince = document.getElementById("tenant-province")?.options[document.getElementById("tenant-province")?.selectedIndex]?.text?.trim();
            if (tenantProvince && tenantProvince !== "Chọn Tỉnh/Thành phố") tenant.province = tenantProvince;

            const tenantIssueDate = document.getElementById("tenant-id-date")?.value?.trim();
            if (tenantIssueDate) tenant.issueDate = tenantIssueDate;

            const tenantIssuePlace = document.getElementById("tenant-id-place")?.value?.trim();
            if (tenantIssuePlace) tenant.issuePlace = tenantIssuePlace;

            // CCCD images
            const frontPreviewElement = document.getElementById('cccd-front-preview');
            const frontImg = frontPreviewElement ? frontPreviewElement.querySelector('img') : null;
            if (frontImg && frontImg.src) {
                const relativePath = frontImg.src.replace(window.location.origin, '');
                if (relativePath && !relativePath.includes('data:')) {
                    tenant.cccdFrontUrl = relativePath;
                }
            }

            const backPreviewElement = document.getElementById('cccd-back-preview');
            const backImg = backPreviewElement ? backPreviewElement.querySelector('img') : null;
            if (backImg && backImg.src) {
                const relativePath = backImg.src.replace(window.location.origin, '');
                if (relativePath && !relativePath.includes('data:')) {
                    tenant.cccdBackUrl = relativePath;
                }
            }

            if (Object.keys(tenant).length > 0) contractData.tenant = tenant;
        }

        // Owner (giữ nguyên code của bạn)
        const owner = {};
        const ownerFullName = document.getElementById("owner-name")?.value?.trim();
        if (ownerFullName) owner.fullName = ownerFullName;

        const ownerPhone = document.getElementById("owner-phone")?.value?.trim();
        if (ownerPhone) owner.phone = ownerPhone;

        const ownerCccd = document.getElementById("owner-id")?.value?.trim();
        if (ownerCccd) owner.cccdNumber = ownerCccd;

        const ownerBirthday = document.getElementById("owner-dob")?.value?.trim();
        if (ownerBirthday) owner.birthday = ownerBirthday;

        const ownerEmail = document.getElementById("owner-email")?.value?.trim();
        if (ownerEmail) owner.email = ownerEmail;

        const ownerStreet = document.getElementById("owner-street")?.value?.trim();
        if (ownerStreet) owner.street = ownerStreet;

        const ownerWard = document.getElementById("owner-ward")?.options[document.getElementById("owner-ward")?.selectedIndex]?.text?.trim();
        if (ownerWard && ownerWard !== "Chọn Phường/Xã") owner.ward = ownerWard;

        const ownerDistrict = document.getElementById("owner-district")?.options[document.getElementById("owner-district")?.selectedIndex]?.text?.trim();
        if (ownerDistrict && ownerDistrict !== "Chọn Quận/Huyện") owner.district = ownerDistrict;

        const ownerProvince = document.getElementById("owner-province")?.options[document.getElementById("owner-province")?.selectedIndex]?.text?.trim();
        if (ownerProvince && ownerProvince !== "Chọn Tỉnh/Thành phố") owner.province = ownerProvince;

        const ownerIssueDate = document.getElementById("owner-id-date")?.value?.trim();
        if (ownerIssueDate) owner.issueDate = ownerIssueDate;

        const ownerIssuePlace = document.getElementById("owner-id-place")?.value?.trim();
        if (ownerIssuePlace) owner.issuePlace = ownerIssuePlace;

        const ownerBankAccount = document.getElementById("owner-bankAccount")?.value?.trim();
        if (ownerBankAccount) owner.bankAccount = ownerBankAccount;

        if (Object.keys(owner).length > 0) contractData.owner = owner;

        // Residents
        if (this.residents && this.residents.length > 0) {
            contractData.residents = this.residents.map(res => ({
                fullName: res.name,
                birthYear: res.birthYear,
                phone: res.phone,
                cccdNumber: res.id
            }));
            console.log("👥 Residents data:", contractData.residents);
        }

        console.log("=== FINAL CONTRACT DATA ===");
        console.log("📅 Contract Date:", contractData.contractDate);
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
        const residentsList = document.getElementById("residents-list");
        const noResidentsMessage = document.getElementById("no-residents-message");

        if (noResidentsMessage) {
            noResidentsMessage.style.display = "none";
        }

        const residentId = "resident-" + Date.now();
        const residentDiv = document.createElement("div");
        residentDiv.className = "nha-tro-resident-item card mb-2";
        residentDiv.id = residentId;
        residentDiv.dataset.residentId = residentId; // Lưu ID để chỉnh sửa
        residentDiv.innerHTML = `
        <div class="card-body p-3">
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <h6 class="mb-1">${this.escapeHtml(resident.name)}</h6>
                    <small class="text-muted">Năm sinh: ${this.escapeHtml(resident.birthYear)}</small>
                    ${resident.phone ? `<br><small class="text-muted">SĐT: ${this.escapeHtml(resident.phone)}</small>` : ""}
                    ${resident.id ? `<br><small class="text-muted">CCCD: ${this.escapeHtml(resident.id)}</small>` : ""}
                    ${resident.notes ? `<br><small class="text-muted">Ghi chú: ${this.escapeHtml(resident.notes)}</small>` : ""}
                </div>
                <div>
                    <button type="button" class="btn btn-sm btn-outline-primary me-1" onclick="NhaTroContract.editResident('${residentId}')" title="Sửa người ở">
                        <i class="fa fa-edit"></i>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="NhaTroContract.removeResident('${residentId}')" title="Xóa người ở">
                        <i class="fa fa-times"></i>
                    </button>
                </div>
            </div>
        </div>
    `;
        residentsList.appendChild(residentDiv);
    },

    editResident(residentId) {
        const residentElement = document.getElementById(residentId);
        if (!residentElement) return;

        const residentName = residentElement.querySelector("h6").textContent;
        const resident = this.residents.find(r => r.name === residentName);
        if (!resident) return;

        // Mở modal và điền dữ liệu
        const modalElement = document.getElementById("addResidentModal");
        const modal = new bootstrap.Modal(modalElement);
        modal.show();

        document.getElementById("resident-name").value = resident.name;
        document.getElementById("resident-birth-year").value = resident.birthYear;
        document.getElementById("resident-phone").value = resident.phone || "";
        document.getElementById("resident-id").value = resident.id || "";
        document.getElementById("resident-notes").value = resident.notes || "";

        // Cập nhật nút lưu để chỉnh sửa
        const saveButton = document.getElementById("btn-save-resident");
        saveButton.textContent = "Cập nhật người ở";
        const originalOnClick = saveButton.onclick;
        saveButton.onclick = () => {
            const updatedResident = {
                name: document.getElementById("resident-name").value.trim(),
                birthYear: document.getElementById("resident-birth-year").value,
                phone: document.getElementById("resident-phone").value.trim(),
                id: document.getElementById("resident-id").value.trim(),
                notes: document.getElementById("resident-notes").value.trim()
            };

            if (!updatedResident.name || !updatedResident.birthYear) {
                this.showNotification("Vui lòng nhập đầy đủ họ tên và năm sinh", "warning");
                return;
            }

            // Cập nhật resident trong mảng
            const index = this.residents.findIndex(r => r.name === resident.name);
            if (index !== -1) {
                this.residents[index] = updatedResident;
            }

            // Cập nhật UI
            residentElement.querySelector(".card-body").innerHTML = `
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <h6 class="mb-1">${this.escapeHtml(updatedResident.name)}</h6>
                    <small class="text-muted">Năm sinh: ${this.escapeHtml(updatedResident.birthYear)}</small>
                    ${updatedResident.phone ? `<br><small class="text-muted">SĐT: ${this.escapeHtml(updatedResident.phone)}</small>` : ""}
                    ${updatedResident.id ? `<br><small class="text-muted">CCCD: ${this.escapeHtml(updatedResident.id)}</small>` : ""}
                    ${updatedResident.notes ? `<br><small class="text-muted">Ghi chú: ${this.escapeHtml(updatedResident.notes)}</small>` : ""}
                </div>
                <div>
                    <button type="button" class="btn btn-sm btn-outline-primary me-1" onclick="NhaTroContract.editResident('${residentId}')" title="Sửa người ở">
                        <i class="fa fa-edit"></i>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-danger" onclick="NhaTroContract.removeResident('${residentId}')" title="Xóa người ở">
                        <i class="fa fa-times"></i>
                    </button>
                </div>
            </div>
        `;

            this.updateResidentsPreview();
            this.updateResidentsCount();
            this.showNotification(`Đã cập nhật người ở "${updatedResident.name}"`, "success");
            modal.hide();
            saveButton.textContent = "Thêm người ở";
            saveButton.onclick = originalOnClick; // Khôi phục hàm thêm mới
        };
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

    saveNewCustomer: async function () {
        const fullName = document.getElementById("newCustomer-name").value.trim();
        const dob = document.getElementById("newCustomer-dob").value || null;
        const cccdNumber = document.getElementById("newCustomer-id").value.trim();
        const issueDate = document.getElementById("newCustomer-id-date").value || null;
        const issuePlace = document.getElementById("newCustomer-id-place").value || null;
        const phone = document.getElementById("newCustomer-phone").value.trim();
        const email = document.getElementById("newCustomer-email").value || null;
        const street = document.getElementById("newCustomer-street").value || null;

        // 🔥 SỬA LỖI: Lấy mã số (value) thay vì lấy tên (text)
        const provinceCode = document.getElementById("newCustomer-province").value;
        const districtCode = document.getElementById("newCustomer-district").value;
        const wardCode = document.getElementById("newCustomer-ward").value;

        const relationship = document.getElementById("newCustomer-relationship")?.value || null;
        const relationshipNote = document.getElementById("newCustomer-relationship-note")?.value || null;
        const notes = document.getElementById("newCustomer-notes")?.value || null;

        if (!fullName || !phone || !cccdNumber) {
            this.showNotification("Vui lòng nhập đầy đủ Họ và tên, Số điện thoại và Số CMND/CCCD cho người bảo hộ!", "warning");
            return;
        }

        this.unregisteredTenantData = {
            fullName: fullName,
            phone: phone,
            cccdNumber: cccdNumber,
            birthday: dob,
            issueDate: issueDate,
            issuePlace: issuePlace,
            email: email,
            street: street,
            // Lưu cả tên để hiển thị
            ward: this.getSelectText("newCustomer-ward"),
            district: this.getSelectText("newCustomer-district"),
            province: this.getSelectText("newCustomer-province"),
            relationship: relationship,
            relationshipNote: relationshipNote,
            notes: notes
        };
        this.unregisteredTenantCccdFrontFile = document.getElementById("newCustomer-cccd-front").files[0] || null;
        this.unregisteredTenantCccdBackFile = document.getElementById("newCustomer-cccd-back").files[0] || null;

        const guardianDisplayContainer = document.getElementById("guardian-display-container");
        const guardianDisplayName = document.getElementById("guardian-display-name");
        const btnAddCustomerHost = document.getElementById("btn-add-customer-host");

        if (guardianDisplayContainer && guardianDisplayName && btnAddCustomerHost) {
            guardianDisplayName.textContent = fullName;
            guardianDisplayContainer.classList.remove('d-none');
            guardianDisplayContainer.style.display = 'flex';
            btnAddCustomerHost.style.display = 'none';
        }

        $('#btn-edit-guardian').off('click').on('click', () => {
            this.openEditCustomerModal(this.unregisteredTenantData);
        });
        $('#btn-delete-guardian').off('click').on('click', () => {
            this.clearUnregisteredTenantData();
        });

        // Điền các thông tin cơ bản
        document.getElementById("tenant-name").value = fullName;
        document.getElementById("tenant-phone").value = phone;
        document.getElementById("tenant-id").value = cccdNumber;
        document.getElementById("tenant-dob").value = dob;
        document.getElementById("tenant-id-date").value = issueDate;
        document.getElementById("tenant-id-place").value = issuePlace;
        document.getElementById("tenant-email").value = email;
        document.getElementById("tenant-street").value = street;
        document.getElementById("tenant-email").value = email;
        // 🔥 BẮT ĐẦU KHỐI CODE SỬA LỖI ĐỊA CHỈ 🔥
        const tenantProvinceSelect = document.getElementById("tenant-province");
        const tenantDistrictSelect = document.getElementById("tenant-district");
        const tenantWardSelect = document.getElementById("tenant-ward");

        if (tenantProvinceSelect && provinceCode) {
            tenantProvinceSelect.value = provinceCode;
            // Tải danh sách huyện tương ứng và chờ cho nó hoàn thành
            await this.loadDistricts(provinceCode, 'tenant-district', 'tenant-ward');

            if (tenantDistrictSelect && districtCode) {
                tenantDistrictSelect.value = districtCode;
                // Tải danh sách xã tương ứng và chờ cho nó hoàn thành
                await this.loadWards(districtCode, 'tenant-ward');

                if (tenantWardSelect && wardCode) {
                    tenantWardSelect.value = wardCode;
                }
            }
        }
        // 🔥 KẾT THÚC KHỐI CODE SỬA LỖI ĐỊA CHỈ 🔥

        this.toggleTenantInputFields(false);
        document.getElementById("tenantType").value = "UNREGISTERED";

        const frontPreview = document.getElementById("cccd-front-preview");
        const backPreview = document.getElementById("cccd-back-preview");
        frontPreview.innerHTML = '';
        backPreview.innerHTML = '';

        if (this.unregisteredTenantCccdFrontFile) {
            const readerFront = new FileReader();
            readerFront.onload = (e) => { frontPreview.innerHTML = `<img src="${e.target.result}" alt="CCCD Front" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;">`; };
            readerFront.readAsDataURL(this.unregisteredTenantCccdFrontFile);
        } else if (this.unregisteredTenantData.cccdFrontUrl) {
            frontPreview.innerHTML = `<img src="${window.location.origin}${this.unregisteredTenantData.cccdFrontUrl.startsWith('/') ? '' : '/'}${this.unregisteredTenantData.cccdFrontUrl.replace(/ /g, '%20')}" alt="CCCD Front" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;">`;
        } else {
            frontPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt trước</div>`;
        }

        if (this.unregisteredTenantCccdBackFile) {
            const readerBack = new FileReader();
            readerBack.onload = (e) => { backPreview.innerHTML = `<img src="${e.target.result}" alt="CCCD Back" style="max-width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`; };
            readerBack.readAsDataURL(this.unregisteredTenantCccdBackFile);
        } else if (this.unregisteredTenantData.cccdBackUrl) {
            backPreview.innerHTML = `<img src="${window.location.origin}${this.unregisteredTenantData.cccdBackUrl.startsWith('/') ? '' : '/'}${this.unregisteredTenantData.cccdBackUrl.replace(/ /g, '%20')}" alt="CCCD Back" style="max-width: 100%; max-height: 200px; height: auto; object-fit: contain; border-radius: 8px;">`;
        } else {
            backPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt sau</div>`;
        }

        $('#addCustomerModal-host').modal('hide');

        this.showNotification(`Đã thêm thông tin người bảo hộ "${fullName}" vào form tạm thời!`, "success");
        this.updateAllPreview();
    },
    toggleTenantInputFields: function (enable) {
        const fieldsToControl = [
            "tenant-name", "tenant-phone", "tenant-id", "tenant-dob",
            "tenant-id-date", "tenant-id-place", "tenant-email",
            "tenant-street", "tenant-province", "tenant-district", "tenant-ward"
        ];
        const cccdFileInputs = ["cccd-front", "cccd-back"]; // IDs of the file inputs

        fieldsToControl.forEach(id => {
            const element = document.getElementById(id);
            if (element) {
                element.disabled = !enable; // Set disabled = true nếu enable là false
                if (!enable) {
                    element.classList.add('nha-tro-input-disabled'); // Thêm class để đổi style
                } else {
                    element.classList.remove('nha-tro-input-disabled'); // Bỏ class để kích hoạt
                }
            }
        });

        // Xử lý các input type="file" (ảnh CCCD) và div click của chúng
        cccdFileInputs.forEach(id => {
            const fileInput = document.getElementById(id); // Input type="file"
            const uploadDiv = fileInput?.closest('.nha-tro-image-upload'); // Div bao ngoài có onclick

            if (fileInput) fileInput.disabled = !enable; // Vô hiệu hóa input file
            if (uploadDiv) {
                if (!enable) {
                    uploadDiv.classList.add('is-disabled'); // Thêm class mới cho div upload
                } else {
                    uploadDiv.classList.remove('is-disabled');
                }
            }
        });
    },
    clearUnregisteredTenantData: function () {
        this.unregisteredTenantData = null;
        this.unregisteredTenantCccdFrontFile = null;
        this.unregisteredTenantCccdBackFile = null;

        // Ẩn khung hiển thị người bảo hộ và hiện lại nút "Thêm người bảo hộ"
        $('#guardian-display-container').hide();
        $('#btn-add-customer-host').show();

        // Reset các trường input của người thuê trên form chính
        document.getElementById("tenant-name").value = "";
        document.getElementById("tenant-phone").value = "";
        document.getElementById("tenant-id").value = "";
        document.getElementById("tenant-dob").value = "";
        document.getElementById("tenant-id-date").value = "";
        document.getElementById("tenant-id-place").value = "";
        document.getElementById("tenant-email").value = "";
        document.getElementById("tenant-street").value = "";
        document.getElementById("tenant-ward").value = "";
        document.getElementById("tenant-district").value = "";
        document.getElementById("tenant-province").value = "";

        // Kích hoạt lại các trường input cho người thuê
        this.toggleTenantInputFields(true);

        // Reset preview ảnh CCCD trên form chính
        const frontPreview = document.getElementById("cccd-front-preview");
        const backPreview = document.getElementById("cccd-back-preview");
        if (frontPreview) {
            frontPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt trước</div>`;
        }
        if (backPreview) {
            backPreview.innerHTML = `<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt sau</div>`;
        }

        // Cập nhật loại người thuê về REGISTERED (mặc định ban đầu)
        const tenantTypeSelect = document.getElementById("tenantType");
        if (tenantTypeSelect) {
            tenantTypeSelect.value = "REGISTERED";
        }

        this.showNotification("Đã xóa thông tin người bảo hộ khỏi form.", "info");
        this.updateAllPreview();
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

    // ✅ PHIÊN BẢN TOAST ĐẸP NHẤT
    showNotification(message, type = "info") {
        const Toast = Swal.mixin({
            toast: true,
            position: 'top-end',
            showConfirmButton: false,
            timer: type === 'error' ? 5000 : 3000,
            timerProgressBar: true,
            didOpen: (toast) => {
                toast.addEventListener('mouseenter', Swal.stopTimer);
                toast.addEventListener('mouseleave', Swal.resumeTimer);
            },
            customClass: {
                popup: 'colored-toast'
            }
        });

        const config = {
            title: message
        };

        switch (type) {
            case 'success':
                config.icon = 'success';
                break;
            case 'error':
                config.icon = 'error';
                break;
            case 'warning':
                config.icon = 'warning';
                break;
            case 'info':
            default:
                config.icon = 'info';
                break;
        }

        Toast.fire(config);
    },
}
function updateContractPreview() {
    // Lấy dữ liệu từ form
    const contractData = collectContractData();

    // Định dạng ngày ký hợp đồng
    const signDate = contractData.contractDate
        ? new Date(contractData.contractDate).toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        })
        : '........................';

    // Cập nhật các trường trong preview
    $('#preview-sign-date').text(signDate);
    $('#preview-owner-name').text(contractData.ownerName || '........................');
    $('#preview-owner-dob').text(contractData.ownerBirthday || '........................');
    $('#preview-owner-id').text(contractData.ownerIdCard || '........................');
    $('#preview-owner-id-date').text(contractData.ownerIdDate || '........................');
    $('#preview-owner-id-place').text(contractData.ownerIdPlace || '........................');
    $('#preview-owner-address').text(contractData.ownerAddress || '........................');
    $('#preview-owner-phone').text(contractData.ownerPhone || '........................');
    $('#preview-owner-signature').text(contractData.ownerName || '........................');

    $('#preview-tenant-name').text(contractData.tenantName || '........................');
    $('#preview-tenant-dob').text(contractData.tenantBirthday || '........................');
    $('#preview-tenant-id').text(contractData.tenantIdCard || '........................');
    $('#preview-tenant-id-date').text(contractData.tenantIdDate || '........................');
    $('#preview-tenant-id-place').text(contractData.tenantIdPlace || '........................');
    $('#preview-tenant-address').text(contractData.tenantAddress || '........................');
    $('#preview-tenant-phone').text(contractData.tenantPhone || '........................');
    $('#preview-tenant-signature').text(contractData.tenantName || '........................');

    $('#preview-room-address').text(contractData.roomAddress || 'Ký túc xá chưa xác định');
    $('#preview-room-number').text(contractData.roomNumber || '........................');
    $('#preview-room-area').text(contractData.roomArea ? `${contractData.roomArea} m²` : '........................');

    $('#preview-duration').text(contractData.contractDuration || '........................');
    $('#preview-start-date').text(contractData.contractStartDate || '........................');

    // Tính ngày kết thúc
    let endDate = '........................';
    if (contractData.contractStartDate && contractData.contractDuration) {
        const start = new Date(contractData.contractStartDate);
        start.setMonth(start.getMonth() + parseInt(contractData.contractDuration));
        endDate = start.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
    $('#preview-end-date').text(endDate);

    // Cập nhật giá thuê
    const monthlyRent = contractData.monthlyRent;
    $('#preview-rent').text(!isNaN(monthlyRent) && monthlyRent > 0
        ? new Intl.NumberFormat('vi-VN').format(monthlyRent) + ' VNĐ/tháng'
        : '........................');

    // Cập nhật thông tin thanh toán
    const paymentMethodText = contractData.paymentMethod === 'TIEN_MAT' ? 'Tiền mặt' : contractData.paymentMethod === 'BANK' ? 'Chuyển khoản' : '';
    const paymentInfo = contractData.paymentDate && paymentMethodText
        ? `vào ngày ${contractData.paymentDate} hàng tháng bằng ${paymentMethodText}`
        : '........................';
    $('#preview-payment-date').text(paymentInfo);

    $('#preview-deposit').text(!isNaN(contractData.deposit) && contractData.deposit > 0
        ? new Intl.NumberFormat('vi-VN').format(contractData.deposit) + ' VNĐ'
        : '........................');
    $('#preview-deposit-months').text(contractData.depositMonths || '........................');

    // Cập nhật danh sách người ở
    const residents = $('#residents-list').children().not('#no-residents-message').map(function () {
        return $(this).find('.resident-name').text();
    }).get().join(', ') || '........................';
    $('#preview-residents').text(residents);
    $('#preview-residents-section').css('display', residents !== '........................' ? 'block' : 'none');

    // Cập nhật tiện ích
    const amenities = $('.nha-tro-amenities input:checked').map(function () {
        return $(this).siblings('label').text();
    }).get().join(', ') || '........................';
    $('#preview-amenities').text(amenities);

    // Cập nhật điều khoản chung
    $('#preview-terms').text(contractData.terms || 'Không có điều khoản bổ sung.');

    // Debug
    console.log('🔍 Preview Sign Date:', signDate);
    console.log('🔍 Preview Rent:', contractData.monthlyRent);
    console.log('🔍 Preview Payment Info:', paymentInfo);
    console.log('🔍 Preview Room Address:', contractData.roomAddress);
}

// Hàm debounce để tối ưu hiệu suất
function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}



// Thêm sự kiện input để cập nhật preview
$(document).ready(function () {
    const inputs = [
        '#tenant-name', '#tenant-phone', '#tenant-email', '#tenant-id', '#tenant-dob', '#tenant-id-date', '#tenant-id-place', '#tenant-street', '#tenant-ward', '#tenant-district', '#tenant-province',
        '#owner-name', '#owner-phone', '#owner-email', '#owner-id', '#owner-dob', '#owner-id-date', '#owner-id-place', '#owner-street', '#owner-ward', '#owner-district', '#owner-province',
        '#room-number', '#room-area', '#room-street', '#room-ward', '#room-district', '#room-province',
        '#contract-date', '#start-date', '#contract-duration', '#rent-price', '#payment-date', '#payment-method', '#deposit-months', '#terms-conditions'
    ];

    const debouncedUpdatePreview = debounce(updateContractPreview, 300);
    inputs.forEach(selector => {
        $(selector).on('input change', debouncedUpdatePreview);
    });

    // Cập nhật preview khi thêm/xóa người ở hoặc tiện ích
    $('#btn-save-resident, .nha-tro-amenities input').on('click', debouncedUpdatePreview);

    // Cập nhật preview ban đầu
    updateContractPreview();
});

// Hàm debounce để tối ưu hiệu suất
function debounce(func, wait) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

// Thêm sự kiện input để cập nhật preview
$(document).ready(function () {
    const inputs = [
        '#tenant-name', '#tenant-phone', '#tenant-email', '#tenant-id', '#tenant-dob', '#tenant-id-date', '#tenant-id-place', '#tenant-street', '#tenant-ward', '#tenant-district', '#tenant-province',
        '#owner-name', '#owner-phone', '#owner-email', '#owner-id', '#owner-dob', '#owner-id-date', '#owner-id-place', '#owner-street', '#owner-ward', '#owner-district', '#owner-province',
        '#room-number', '#room-area', '#room-street', '#room-ward', '#room-district', '#room-province',
        '#contract-date', '#start-date', '#contract-duration', '#rent-price', '#payment-date', '#payment-method', '#deposit-months', '#terms-conditions'
    ];

    const debouncedUpdatePreview = debounce(updateContractPreview, 300);
    inputs.forEach(selector => {
        $(selector).on('input change', debouncedUpdatePreview);
    });

    // Cập nhật preview khi thêm/xóa người ở hoặc tiện ích
    $('#btn-save-resident, .nha-tro-amenities input').on('click', debouncedUpdatePreview);

    // Cập nhật preview ban đầu
    updateContractPreview();
});
function updateContractStatus() {
    fetch('/api/contracts/auto-update-status', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + localStorage.getItem('token'),
            'Content-Type': 'application/json'
        }
    })
        .then(response => response.json())
        .then(data => {
            if (data.success) {
                alert(`✅ Đã cập nhật ${data.updatedCount} hợp đồng!`);
                location.reload(); // Reload trang để thấy thay đổi
            } else {
                alert('❌ Lỗi: ' + data.message);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            alert('❌ Có lỗi xảy ra!');
        });
}




document.addEventListener("DOMContentLoaded", () => {
    window.NhaTroContract.init()
})
/* ]]> */