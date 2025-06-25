// Dữ liệu mẫu cho modal chi tiết (nhập cứng)
const detailData = {
    1: {
        name: "Nguyễn Văn A",
        birthDate: "17/09/1990",
        gender: "Nam",
        cccd: "001234567890",
        cccdDate: "15/01/2020",
        cccdPlace: "Cục Cảnh sát ĐKQL cư trú và DLQG về dân cư",
        phone: "0987654321",
        email: "nguyenvana@email.com",
        address: "123 Đường ABC, Phường XYZ, Quận 1, TP.HCM",
        job: "Kỹ sư phần mềm",
        workplace: "Công ty TNHH ABC Technology",
         documents: [
            { type: "CCCD mặt trước", url: "/placeholder.svg?height=300&width=400" },
            { type: "CCCD mặt sau", url: "/placeholder.svg?height=300&width=400" },
        ],
    },
    2: {
        name: "Trần Thị B",
        birthDate: "25/03/1995",
        gender: "Nữ",
        cccd: "001234567891",
        cccdDate: "20/05/2021",
        cccdPlace: "Cục Cảnh sát ĐKQL cư trú và DLQG về dân cư",
        phone: "0123456789",
        email: "tranthib@email.com",
        address: "456 Đường DEF, Phường UVW, Quận 2, TP.HCM",
        job: "Nhân viên văn phòng",
        workplace: "Công ty Cổ phần XYZ",
          documents: [
            { type: "CCCD mặt trước", url: "https://bcp.cdnchinhphu.vn/334894974524682240/2024/2/23/duoi-6y-1708685177988380748443.png" },
            { type: "CCCD mặt sau", url: "https://image.plo.vn/w1000/Uploaded/2025/xqeioxdrky/2024_02_11/mau-can-cuoc-tre-tu-6-tuoi-mat-sau-9238.jpg.webp" },
        ],
    },
    3: {
        name: "Lê Minh C",
        birthDate: "12/11/1988",
        gender: "Nam",
        cccd: "001234567892",
        cccdDate: "10/03/2019",
        cccdPlace: "Cục Cảnh sát ĐKQL cư trú và DLQG về dân cư",
        phone: "0369852147",
        email: "leminhc@email.com",
        address: "789 Đường GHI, Phường RST, Quận 3, TP.HCM",
        job: "Giáo viên",
        workplace: "Trường THPT ABC",
       documents: [
            { type: "CCCD mặt trước", url: "/placeholder.svg?height=300&width=400" },
            { type: "CCCD mặt sau", url: "/placeholder.svg?height=300&width=400" },
        ],
    },
}

let isDropdownOpen = false
let currentDetailId = null
let pendingAction = null

// Import Bootstrap
const bootstrap = window.bootstrap

// Initialize page
document.addEventListener("DOMContentLoaded", () => {
    setupEventListeners()
})

function setupEventListeners() {
    // Search on Enter key for landlord tab
    const searchInput = document.getElementById("searchInput")
    if (searchInput) {
        searchInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                performSearch()
            }
        })
    }

    // Search on Enter key for approval tab
    const searchInputApproval = document.getElementById("searchInputApproval")
    if (searchInputApproval) {
        searchInputApproval.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                performApprovalSearch()
            }
        })
    }

    // Close dropdown when clicking outside
    document.addEventListener("click", (event) => {
        const filterDropdown = document.querySelector(".filter-dropdown-staff")
        const dropdown = document.getElementById("filterDropdown")

        if (filterDropdown && !filterDropdown.contains(event.target) && isDropdownOpen) {
            toggleFilterDropdown()
        }
    })

    // Setup detail modal buttons
    const approveDetailBtn = document.getElementById("approveDetailBtn")
    const rejectDetailBtn = document.getElementById("rejectDetailBtn")

    if (approveDetailBtn) {
        approveDetailBtn.addEventListener("click", () => {
            if (currentDetailId) {
                showConfirmationModal(currentDetailId, "approve")
            }
        })
    }

    if (rejectDetailBtn) {
        rejectDetailBtn.addEventListener("click", () => {
            if (currentDetailId) {
                showConfirmationModal(currentDetailId, "reject")
            }
        })
    }
}

// Tab switching functionality
function switchTab(tabIndex, element) {
    // Remove active class from all tabs
    document.querySelectorAll(".custom-tab-staff").forEach((tab) => {
        tab.classList.remove("active")
    })

    // Add active class to clicked tab
    element.classList.add("active")

    // Hide all tab content panels
    document.querySelectorAll(".tab-content-panel-staff").forEach((panel) => {
        panel.classList.add("hidden")
    })

    // Show selected tab content
    document.getElementById(`tab-content-${tabIndex}`).classList.remove("hidden")
}

// Filter dropdown functionality for landlord tab
function toggleFilterDropdown() {
    const dropdown = document.getElementById("filterDropdown")
    const chevron = document.getElementById("filterChevron")
    const filterBtn = document.querySelector(".filter-btn-staff")

    if (!dropdown || !chevron || !filterBtn) return

    isDropdownOpen = !isDropdownOpen

    if (isDropdownOpen) {
        dropdown.classList.add("show")
        chevron.style.transform = "rotate(180deg)"
        filterBtn.classList.add("active")
    } else {
        dropdown.classList.remove("show")
        chevron.style.transform = "rotate(0deg)"
        filterBtn.classList.remove("active")
    }
}

function selectFilter(filterValue, element) {
    // Remove selected class from all options
    document.querySelectorAll(".filter-option-staff").forEach((option) => {
        option.classList.remove("selected")
    })

    // Add selected class to clicked option
    element.classList.add("selected")

    // Update filter text
    const filterText = document.getElementById("filterText")
    const filterTexts = {
        all: "Tất cả",
        active: "Hoạt động",
        inactive: "Vô hiệu hóa",
    }

    if (filterText) {
        filterText.textContent = filterTexts[filterValue]
    }

    // Close dropdown
    toggleFilterDropdown()

    // Apply filter to table rows
    const tableRows = document.querySelectorAll("#tableBody tr")
    let visibleCount = 0

    tableRows.forEach((row) => {
        const status = row.getAttribute("data-status")
        if (filterValue === "all" || status === filterValue) {
            row.style.display = ""
            visibleCount++
        } else {
            row.style.display = "none"
        }
    })

    // Show feedback
    showSuccessMessage(`Đã lọc: ${visibleCount} kết quả cho "${filterTexts[filterValue]}"`)
}

// Search functionality for landlord tab
function performSearch() {
    const searchInput = document.getElementById("searchInput")
    if (!searchInput) return

    const searchTerm = searchInput.value.toLowerCase().trim()
    const tableRows = document.querySelectorAll("#tableBody tr")
    let visibleCount = 0

    tableRows.forEach((row) => {
        const cells = row.querySelectorAll("td")
        if (cells.length > 0) {
            const name = cells[1].textContent.toLowerCase()
            const email = cells[2].textContent.toLowerCase()
            const phone = cells[3].textContent.toLowerCase()

            if (searchTerm === "" || name.includes(searchTerm) || email.includes(searchTerm) || phone.includes(searchTerm)) {
                row.style.display = ""
                visibleCount++
            } else {
                row.style.display = "none"
            }
        }
    })

    // Show search feedback
    showSuccessMessage(`Tìm thấy ${visibleCount} kết quả`)
}

// Search functionality for approval tab
function performApprovalSearch() {
    const searchInput = document.getElementById("searchInputApproval")
    if (!searchInput) return

    const searchTerm = searchInput.value.toLowerCase().trim()
    const cards = document.querySelectorAll(".registration-card-staff")

    let visibleCount = 0

    cards.forEach((card) => {
        const nameInput = card.querySelector(".form-input-staff")
        const phoneInput = card.querySelectorAll(".form-input-staff")[2] // Phone is the 3rd input

        if (nameInput && phoneInput) {
            const name = nameInput.value.toLowerCase()
            const phone = phoneInput.value.toLowerCase()

            if (searchTerm === "" || name.includes(searchTerm) || phone.includes(searchTerm)) {
                card.style.display = "block"
                visibleCount++
            } else {
                card.style.display = "none"
            }
        }
    })

    showSuccessMessage(`Tìm thấy ${visibleCount} kết quả`)
}

// Show detail modal
function showDetailModal(id) {
    const registration = detailData[id]
    if (!registration) return

    currentDetailId = id

    // Populate modal with data
    document.getElementById("detailName").textContent = registration.name
    document.getElementById("detailBirthDate").textContent = registration.birthDate
    document.getElementById("detailGender").textContent = registration.gender
    document.getElementById("detailCccd").textContent = registration.cccd
    document.getElementById("detailCccdDate").textContent = registration.cccdDate
    document.getElementById("detailCccdPlace").textContent = registration.cccdPlace
    document.getElementById("detailPhone").textContent = registration.phone
    document.getElementById("detailEmail").textContent = registration.email
    document.getElementById("detailAddress").textContent = registration.address
    document.getElementById("detailJob").textContent = registration.job
    document.getElementById("detailWorkplace").textContent = registration.workplace
    
    // Populate document images
    const documentImages = document.getElementById("documentImages")
    documentImages.innerHTML = registration.documents
        .map(
            (doc) => `
        <div class="document-image-item-staff" onclick="showImageModal('${doc.url}')">
            <img src="${doc.url}" alt="${doc.type}">
            <div class="document-image-overlay-staff">
                <i class="fas fa-search-plus"></i>
            </div>
            <div class="document-image-label-staff">${doc.type}</div>
        </div>
    `,
        )
        .join("")

    // Show modal
    const modal = document.getElementById("detailModal")
    if (modal) {
        new bootstrap.Modal(modal).show()
    }
}

// Show image modal
function showImageModal(imageUrl) {
    const modalImage = document.getElementById("modalImage")
    modalImage.src = imageUrl

    const modal = document.getElementById("imageModal")
    if (modal) {
        new bootstrap.Modal(modal).show()
    }
}

// Show confirmation modal
function showConfirmationModal(id, action) {
    const registration = detailData[id]
    if (!registration) return

    const modal = document.getElementById("confirmationModal")
    if (modal) {
        const modalInstance = new bootstrap.Modal(modal)
        const modalTitle = document.getElementById("modalTitle")
        const modalMessage = document.getElementById("modalMessage")
        const confirmBtn = document.getElementById("confirmBtn")

        pendingAction = { id, action }

        if (action === "approve") {
            modalTitle.textContent = "Xác nhận phê duyệt"
            modalMessage.textContent = `Bạn có chắc chắn muốn phê duyệt đăng ký của "${registration.name}"?`
            confirmBtn.textContent = "Phê duyệt"
            confirmBtn.className = "btn btn-success"
            confirmBtn.onclick = confirmApproval
        } else {
            modalTitle.textContent = "Xác nhận từ chối"
            modalMessage.textContent = `Bạn có chắc chắn muốn từ chối đăng ký của "${registration.name}"?`
            confirmBtn.textContent = "Từ chối"
            confirmBtn.className = "btn btn-danger"
            confirmBtn.onclick = confirmRejection
        }

        modalInstance.show()
    }
}

// Confirm approval
function confirmApproval() {
    if (!pendingAction || pendingAction.action !== "approve") return

    const id = pendingAction.id
    const registration = detailData[id]

    if (registration) {
        // Show success message
        showSuccessMessage(`Đã phê duyệt đăng ký của ${registration.name}`)

        // Hide the card
        const card = document.querySelector(`[data-id="${id}"]`)
        if (card) {
            card.style.display = "none"
        }

        // Close detail modal
        const detailModal = document.getElementById("detailModal")
        if (detailModal) {
            bootstrap.Modal.getInstance(detailModal).hide()
        }
    }

    // Close confirmation modal
    const modal = document.getElementById("confirmationModal")
    if (modal) {
        bootstrap.Modal.getInstance(modal).hide()
    }

    pendingAction = null
    currentDetailId = null
}

// Confirm rejection
function confirmRejection() {
    if (!pendingAction || pendingAction.action !== "reject") return

    const id = pendingAction.id
    const registration = detailData[id]

    if (registration) {
        // Show error message
        showErrorMessage(`Đã từ chối đăng ký của ${registration.name}`)

        // Hide the card
        const card = document.querySelector(`[data-id="${id}"]`)
        if (card) {
            card.style.display = "none"
        }

        // Close detail modal
        const detailModal = document.getElementById("detailModal")
        if (detailModal) {
            bootstrap.Modal.getInstance(detailModal).hide()
        }
    }

    // Close confirmation modal
    const modal = document.getElementById("confirmationModal")
    if (modal) {
        bootstrap.Modal.getInstance(modal).hide()
    }

    pendingAction = null
    currentDetailId = null
}

// View details functionality for landlord tab
function viewDetails(id) {
    showSuccessMessage(`Xem chi tiết chủ trọ ID: ${id}`)
    // Here you would typically navigate to a detail page
}

// Show success message
function showSuccessMessage(message) {
    const successMsg = document.createElement("div")
    successMsg.className = "success-message-staff"
    successMsg.innerHTML = `<i class="fas fa-check"></i> ${message}`
    document.body.appendChild(successMsg)

    setTimeout(() => {
        successMsg.remove()
    }, 3000)
}

// Show error message
function showErrorMessage(message) {
    const errorMsg = document.createElement("div")
    errorMsg.className = "error-message-staff"
    errorMsg.innerHTML = `<i class="fas fa-times"></i> ${message}`
    document.body.appendChild(errorMsg)

    setTimeout(() => {
        errorMsg.remove()
    }, 3000)
}
