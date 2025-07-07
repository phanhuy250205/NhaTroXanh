// Global Variables
let currentStep = 1
let currentSection = "create"
const totalSteps = 4
let voucherData = {}
let voucherList = []
let currentPage = 1
const itemsPerPage = 6

// Mock data for vouchers
const mockVouchers = [
  {
    id: 1,
    name: "Voucher Tết 2024",
    code: "TET2024",
    description: "Giảm giá đặc biệt dịp Tết Nguyên Đán",
    discountValue: 500000,
    maxDiscount: null,
    startDate: "2024-02-01T00:00",
    endDate: "2024-02-29T23:59",
    totalUsageLimit: 100,
    status: "active",
    hostelScope: "all",
    selectedHostels: [],
    createdAt: "2024-01-15T10:00:00",
    usedCount: 25,
  },
  {
    id: 2,
    name: "Voucher Sinh Viên",
    code: "STUDENT50",
    description: "Ưu đãi dành riêng cho sinh viên",
    discountValue: 200000,
    startDate: "2024-01-01T00:00",
    endDate: "2024-12-31T23:59",
    totalUsageLimit: 200,
    status: "active",
    hostelScope: "specific",
    selectedHostels: [1, 2],
    createdAt: "2024-01-10T14:30:00",
    usedCount: 45,
  },
  {
    id: 3,
    name: "Voucher Khách Hàng Mới",
    code: "NEWCUSTOMER",
    description: "Chào mừng khách hàng mới",
    discountValue: 300000,
    maxDiscount: null,
    startDate: "2024-03-01T00:00",
    endDate: "2024-03-31T23:59",
    totalUsageLimit: 50,
    status: "scheduled",
    hostelScope: "all",
    selectedHostels: [],
    createdAt: "2024-01-20T09:15:00",
    usedCount: 0,
  },
  {
    id: 4,
    name: "Voucher Hè 2024",
    code: "SUMMER2024",
    description: "Ưu đãi mùa hè sôi động",
    discountValue: 400000,
    maxDiscount: null,
    startDate: "2023-12-01T00:00",
    endDate: "2023-12-31T23:59",
    totalUsageLimit: 75,
    status: "expired",
    hostelScope: "specific",
    selectedHostels: [1],
    createdAt: "2023-11-15T16:45:00",
    usedCount: 75,
  },
  {
    id: 5,
    name: "Voucher VIP",
    code: "VIP100",
    description: "Dành cho khách hàng VIP",
    discountValue: 1000000,
    startDate: "2024-01-01T00:00",
    endDate: "2024-06-30T23:59",
    totalUsageLimit: 20,
    status: "disabled",
    hostelScope: "all",
    selectedHostels: [],
    createdAt: "2024-01-05T11:20:00",
    usedCount: 8,
  },
]

// Utility functions
function formatCurrency(amount) {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    minimumFractionDigits: 0,
  }).format(amount)
}

function formatDate(date) {
  return new Intl.DateTimeFormat("vi-VN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
  }).format(date)
}

function formatDateTimeLocal(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  const hours = String(date.getHours()).padStart(2, "0")
  const minutes = String(date.getMinutes()).padStart(2, "0")
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// Initialize the application
document.addEventListener("DOMContentLoaded", () => {
  initializeForm()
  setupEventListeners()
  updatePreview()
  setDefaultDates()
  loadVoucherList()
})

// Initialize form with default values
function initializeForm() {
  document.getElementById("voucherStatus").value = "active"
  document.querySelector('input[name="hostelScope"][value="all"]').checked = true
}

// Set default dates
function setDefaultDates() {
  const now = new Date()
  const startDate = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  const endDate = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)

  document.getElementById("startDate").value = formatDateTimeLocal(startDate)
  document.getElementById("endDate").value = formatDateTimeLocal(endDate)
}

// Setup all event listeners
function setupEventListeners() {
  const inputs = document.querySelectorAll("input, select, textarea")
  inputs.forEach((input) => {
    input.addEventListener("input", updatePreview)
    input.addEventListener("change", updatePreview)
  })

  const hostelScopeRadios = document.querySelectorAll('input[name="hostelScope"]')
  hostelScopeRadios.forEach((radio) => {
    radio.addEventListener("change", handleHostelScopeChange)
  })

  document.getElementById("voucherForm").addEventListener("submit", handleFormSubmit)

  const hostelSearch = document.getElementById("hostelSearch")
  if (hostelSearch) {
    hostelSearch.addEventListener("input", filterHostels)
  }

  const voucherSearchInput = document.getElementById("voucherSearchInput")
  if (voucherSearchInput) {
    voucherSearchInput.addEventListener("input", filterVouchers)
  }

  const statusFilter = document.getElementById("statusFilter")
  if (statusFilter) {
    statusFilter.addEventListener("change", filterVouchers)
  }
}

// Section Management
function showSection(sectionName) {
  currentSection = sectionName

  // Update nav tabs
  document.querySelectorAll(".nav-tab").forEach((tab) => tab.classList.remove("active"))
  document.querySelector(`[data-section="${sectionName}"]`).classList.add("active")

  // Update content sections
  document.querySelectorAll(".content-section").forEach((section) => section.classList.remove("active"))
  document.getElementById(sectionName + "Section").classList.add("active")

  if (sectionName === "list") {
    loadVoucherList()
  }
}

// Generate random voucher code
function generateVoucherCode() {
  const prefix = "VC"
  const timestamp = Date.now().toString().slice(-6)
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  const code = `${prefix}${timestamp}${random}`

  document.getElementById("voucherCode").value = code
  updatePreview()

  showToast("success", "Mã voucher đã được tạo tự động!")
}

// Handle hostel scope change
function handleHostelScopeChange() {
  const selectedScope = document.querySelector('input[name="hostelScope"]:checked').value
  const specificSection = document.getElementById("specificHostelsSection")

  if (specificSection) {
    specificSection.style.display = selectedScope === "specific" ? "block" : "none"
  }

  updatePreview()
}

// Filter hostels based on search
function filterHostels() {
  const searchTerm = document.getElementById("hostelSearch").value.toLowerCase()
  const hostelItems = document.querySelectorAll(".hostel-item")

  hostelItems.forEach((item) => {
    const hostelName = item.querySelector("h5").textContent.toLowerCase()
    const hostelAddress = item.querySelector("p").textContent.toLowerCase()

    if (hostelName.includes(searchTerm) || hostelAddress.includes(searchTerm)) {
      item.style.display = "block"
    } else {
      item.style.display = "none"
    }
  })
}

// Update voucher preview
function updatePreview() {
  if (currentSection !== "create") return

  const name = document.getElementById("voucherName").value || "Tên voucher"
  const code = document.getElementById("voucherCode").value || "VOUCHER_CODE"
  const description = document.getElementById("voucherDescription").value || "Mô tả voucher sẽ hiển thị ở đây"
  const status = document.getElementById("voucherStatus").value
  const discountValue = document.getElementById("discountValue").value || "0"
  const startDate = document.getElementById("startDate").value
  const endDate = document.getElementById("endDate").value
  const totalUsageLimit = document.getElementById("totalUsageLimit").value
  const hostelScope = document.querySelector('input[name="hostelScope"]:checked')?.value || "all"

  // Update preview elements
  document.getElementById("previewName").textContent = name
  document.getElementById("previewCode").textContent = code
  document.getElementById("previewDescription").textContent = description

  // Update discount display
  const discountAmount = Number.parseFloat(discountValue) || 0
  document.getElementById("previewDiscount").textContent = discountAmount.toLocaleString("vi-VN")

  // Update valid date
  let validDateText = "Chưa thiết lập thời gian"
  if (startDate && endDate) {
    const start = new Date(startDate)
    const end = new Date(endDate)
    validDateText = `${formatDate(start)} - ${formatDate(end)}`
  }
  document.getElementById("previewValidDate").textContent = validDateText

  // Update target audience
  let targetText = "Tất cả khu trọ"
  if (hostelScope === "specific") {
    const selectedHostels = document.querySelectorAll('#specificHostelsSection input[type="checkbox"]:checked')
    targetText = `${selectedHostels.length} khu trọ được chọn`
  }
  document.getElementById("previewTarget").textContent = targetText

  // Update usage limit
  document.getElementById("previewUsage").textContent = totalUsageLimit ? `${totalUsageLimit} lượt` : "Không giới hạn"

  // Update status badge
  const statusBadge = document.getElementById("previewStatusBadge")
  statusBadge.className = `status-badge ${status}`
  statusBadge.textContent = getStatusText(status)

  // Update quick stats
  updateQuickStats()
}

// Get status text in Vietnamese
function getStatusText(status) {
  const statusMap = {
    active: "Kích hoạt",
    scheduled: "Lên lịch",
    disabled: "Vô hiệu hóa",
    expired: "Hết hạn",
  }
  return statusMap[status] || "Kích hoạt"
}

// Update quick stats
function updateQuickStats() {
  const hostelScope = document.querySelector('input[name="hostelScope"]:checked')?.value || "all"
  let estimatedHostels = 0

  if (hostelScope === "all") {
    estimatedHostels = 5 // Mock data
  } else if (hostelScope === "specific") {
    estimatedHostels = document.querySelectorAll('#specificHostelsSection input[type="checkbox"]:checked').length
  }

  document.getElementById("estimatedHostels").textContent = estimatedHostels

  const totalUsageLimit = document.getElementById("totalUsageLimit").value
  document.getElementById("maxUsage").textContent = totalUsageLimit || "∞"

  const startDate = document.getElementById("startDate").value
  const endDate = document.getElementById("endDate").value
  let validDays = 0

  if (startDate && endDate) {
    const start = new Date(startDate)
    const end = new Date(endDate)
    validDays = Math.ceil((end - start) / (1000 * 60 * 60 * 24))
  }

  document.getElementById("validDays").textContent = Math.max(0, validDays) + " ngày"
}

// Change step function
function changeStep(direction) {
  if (direction === 1) {
    if (!validateCurrentStep()) {
      return
    }

    if (currentStep < totalSteps) {
      currentStep++
    }
  } else if (direction === -1) {
    if (currentStep > 1) {
      currentStep--
    }
  }

  updateStepDisplay()
  updateNavigationButtons()
  updateProgressSteps()
}

// Validate current step
function validateCurrentStep() {
  const currentStepElement = document.getElementById(`step${currentStep}`)
  const requiredFields = currentStepElement.querySelectorAll("[required]")
  let isValid = true

  requiredFields.forEach((field) => {
    if (!field.value.trim()) {
      field.classList.add("is-invalid")
      isValid = false
    } else {
      field.classList.remove("is-invalid")
    }
  })

  // Additional validation for specific steps
  if (currentStep === 2) {
    const discountValue = Number.parseFloat(document.getElementById("discountValue").value)

    if (discountValue < 1000) {
      document.getElementById("discountValue").classList.add("is-invalid")
      showToast("error", "Số tiền giảm tối thiểu là 1,000 VNĐ")
      isValid = false
    }
  }

  if (currentStep === 3) {
    const startDate = new Date(document.getElementById("startDate").value)
    const endDate = new Date(document.getElementById("endDate").value)
    const now = new Date()

    if (startDate <= now) {
      document.getElementById("startDate").classList.add("is-invalid")
      showToast("error", "Ngày bắt đầu phải sau thời điểm hiện tại")
      isValid = false
    }

    if (endDate <= startDate) {
      document.getElementById("endDate").classList.add("is-invalid")
      showToast("error", "Ngày kết thúc phải sau ngày bắt đầu")
      isValid = false
    }
  }

  if (!isValid) {
    showToast("error", "Vui lòng điền đầy đủ thông tin bắt buộc")
  }

  return isValid
}

// Update step display
function updateStepDisplay() {
  document.querySelectorAll(".form-step").forEach((step) => {
    step.classList.remove("active")
  })
  document.getElementById(`step${currentStep}`).classList.add("active")
}

// Update navigation buttons
function updateNavigationButtons() {
  const prevBtn = document.getElementById("prevBtn")
  const nextBtn = document.getElementById("nextBtn")
  const submitBtn = document.getElementById("submitBtn")

  if (currentStep === 1) {
    prevBtn.style.display = "none"
  } else {
    prevBtn.style.display = "inline-flex"
  }

  if (currentStep === totalSteps) {
    nextBtn.style.display = "none"
    submitBtn.style.display = "inline-flex"
  } else {
    nextBtn.style.display = "inline-flex"
    submitBtn.style.display = "none"
  }
}

// Update progress steps
function updateProgressSteps() {
  document.querySelectorAll(".progress-step").forEach((step, index) => {
    const stepNumber = index + 1

    step.classList.remove("active", "completed")

    if (stepNumber < currentStep) {
      step.classList.add("completed")
    } else if (stepNumber === currentStep) {
      step.classList.add("active")
    }
  })
}

// Handle form submission
function handleFormSubmit(event) {
  event.preventDefault()

  if (!validateCurrentStep()) {
    return
  }

  collectFormData()

  const submitBtn = document.getElementById("submitBtn")
  const originalText = submitBtn.innerHTML
  submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tạo voucher...'
  submitBtn.disabled = true

  setTimeout(() => {
    const newVoucher = {
      id: mockVouchers.length + 1,
      ...voucherData,
      createdAt: new Date().toISOString(),
      usedCount: 0,
    }
    mockVouchers.unshift(newVoucher)

    submitBtn.innerHTML = originalText
    submitBtn.disabled = false

    showToast("success", "Voucher đã được tạo thành công!")

    resetForm()
    showSection("list")
  }, 2000)
}

// Collect all form data
function collectFormData() {
  voucherData = {
    name: document.getElementById("voucherName").value,
    code: document.getElementById("voucherCode").value,
    description: document.getElementById("voucherDescription").value,
    status: document.getElementById("voucherStatus").value,
    discountValue: Number.parseFloat(document.getElementById("discountValue").value),
    maxDiscount: Number.parseFloat(document.getElementById("maxDiscount").value) || null,
    startDate: document.getElementById("startDate").value,
    endDate: document.getElementById("endDate").value,
    totalUsageLimit: Number.parseInt(document.getElementById("totalUsageLimit").value) || null,
    hostelScope: document.querySelector('input[name="hostelScope"]:checked').value,
    selectedHostels: getSelectedHostels(),
  }
}

// Get selected hostels
function getSelectedHostels() {
  const checkboxes = document.querySelectorAll('#specificHostelsSection input[type="checkbox"]:checked')
  return Array.from(checkboxes).map((cb) => Number.parseInt(cb.value))
}

// Load voucher list
function loadVoucherList() {
  voucherList = [...mockVouchers]
  filterVouchers()
}

// Filter vouchers
function filterVouchers() {
  const searchInput = document.getElementById("voucherSearchInput")
  const statusFilter = document.getElementById("statusFilter")

  if (!searchInput || !statusFilter) return

  const searchTerm = searchInput.value.toLowerCase()
  const statusFilterValue = statusFilter.value

  const filteredVouchers = mockVouchers.filter((voucher) => {
    const matchesSearch =
      voucher.name.toLowerCase().includes(searchTerm) ||
      voucher.code.toLowerCase().includes(searchTerm) ||
      voucher.description.toLowerCase().includes(searchTerm)

    const matchesStatus = !statusFilterValue || getVoucherStatus(voucher) === statusFilterValue

    return matchesSearch && matchesStatus
  })

  voucherList = filteredVouchers
  currentPage = 1
  renderVoucherList()
  renderPagination()
}

// Get voucher status
function getVoucherStatus(voucher) {
  const now = new Date()
  const endDate = new Date(voucher.endDate)

  if (voucher.status === "disabled") return "disabled"
  if (endDate < now) return "expired"
  if (voucher.totalUsageLimit && voucher.usedCount >= voucher.totalUsageLimit) return "expired"

  return voucher.status
}

// Render voucher list
function renderVoucherList() {
  const voucherGrid = document.getElementById("voucherGrid")
  if (!voucherGrid) return

  const startIndex = (currentPage - 1) * itemsPerPage
  const endIndex = startIndex + itemsPerPage
  const pageVouchers = voucherList.slice(startIndex, endIndex)

  if (pageVouchers.length === 0) {
    voucherGrid.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
        <i class="fas fa-ticket-alt" style="font-size: 2.5rem; color: #6b7280; margin-bottom: 1rem;"></i>
        <h5 style="color: #6b7280; margin-bottom: 0.5rem; font-size: 1rem;">Không tìm thấy voucher nào</h5>
        <p style="color: #6b7280; font-size: 0.875rem;">Thử thay đổi bộ lọc hoặc tạo voucher mới</p>
      </div>
    `
    return
  }

  voucherGrid.innerHTML = pageVouchers
    .map((voucher) => {
      const status = getVoucherStatus(voucher)
      const discountDisplay = formatCurrency(voucher.discountValue)

      return `
        <div class="voucher-item">
          <div class="voucher-header">
            <div class="voucher-title">
              <h6>${voucher.name}</h6>
              <small>${voucher.code}</small>
            </div>
            <div class="voucher-actions">
              <button class="btn btn-sm btn-outline-primary" onclick="editVoucher(${voucher.id})" title="Chỉnh sửa">
                <i class="fas fa-edit"></i>
              </button>
              <button class="btn btn-sm ${status === "disabled" ? "btn-success" : "btn-outline-danger"}" 
                      onclick="toggleVoucherStatus(${voucher.id})" 
                      title="${status === "disabled" ? "Kích hoạt" : "Vô hiệu hóa"}">
                <i class="fas fa-${status === "disabled" ? "play" : "ban"}"></i>
              </button>
            </div>
          </div>
          
          <div class="voucher-body">
            <div class="voucher-discount">
              <span>${discountDisplay.replace("₫", "")}</span>
              <span class="currency">VNĐ</span>
            </div>
            <div class="voucher-description">${voucher.description}</div>
            
            <div class="voucher-info">
              <div class="info-item">
                <i class="fas fa-calendar-alt"></i>
                <span>${formatDate(new Date(voucher.startDate))} - ${formatDate(new Date(voucher.endDate))}</span>
              </div>
              <div class="info-item">
                <i class="fas fa-users"></i>
                <span>Đã sử dụng: ${voucher.usedCount}/${voucher.totalUsageLimit || "∞"}</span>
              </div>
              <div class="info-item">
                <i class="fas fa-building"></i>
                <span>${voucher.hostelScope === "all" ? "Tất cả khu trọ" : `${voucher.selectedHostels.length} khu trọ`}</span>
              </div>
              <div class="info-item">
                <i class="fas fa-clock"></i>
                <span>Tạo: ${formatDate(new Date(voucher.createdAt))}</span>
              </div>
            </div>
          </div>
          
          <div class="voucher-footer">
            <span class="status-badge ${status}">${getStatusText(status)}</span>
            <small>ID: #${voucher.id}</small>
          </div>
        </div>
      `
    })
    .join("")
}

// Render pagination
function renderPagination() {
  const totalPages = Math.ceil(voucherList.length / itemsPerPage)
  const pagination = document.getElementById("voucherPagination")

  if (!pagination || totalPages <= 1) {
    if (pagination) pagination.innerHTML = ""
    return
  }

  let paginationHTML = ""

  // Previous button
  paginationHTML += `
    <li class="page-item ${currentPage === 1 ? "disabled" : ""}">
      <a class="page-link" href="#" onclick="changePage(${currentPage - 1}); return false;">
        <i class="fas fa-chevron-left"></i>
      </a>
    </li>
  `

  // Page numbers
  for (let i = 1; i <= totalPages; i++) {
    if (i === 1 || i === totalPages || (i >= currentPage - 1 && i <= currentPage + 1)) {
      paginationHTML += `
        <li class="page-item ${i === currentPage ? "active" : ""}">
          <a class="page-link" href="#" onclick="changePage(${i}); return false;">${i}</a>
        </li>
      `
    } else if (i === currentPage - 2 || i === currentPage + 2) {
      paginationHTML += `<li class="page-item disabled"><span class="page-link">...</span></li>`
    }
  }

  // Next button
  paginationHTML += `
    <li class="page-item ${currentPage === totalPages ? "disabled" : ""}">
      <a class="page-link" href="#" onclick="changePage(${currentPage + 1}); return false;">
        <i class="fas fa-chevron-right"></i>
      </a>
    </li>
  `

  pagination.innerHTML = paginationHTML
}

// Change page
function changePage(page) {
  const totalPages = Math.ceil(voucherList.length / itemsPerPage)
  if (page < 1 || page > totalPages) return

  currentPage = page
  renderVoucherList()
  renderPagination()
}

// Edit voucher - Fixed modal
function editVoucher(voucherId) {
  const voucher = mockVouchers.find((v) => v.id === voucherId)
  if (!voucher) return

  // Populate edit form
  document.getElementById("editVoucherId").value = voucher.id
  document.getElementById("editVoucherName").value = voucher.name
  document.getElementById("editVoucherCode").value = voucher.code
  document.getElementById("editVoucherDescription").value = voucher.description
  document.getElementById("editDiscountValue").value = voucher.discountValue
  document.getElementById("editStartDate").value = voucher.startDate
  document.getElementById("editEndDate").value = voucher.endDate
  document.getElementById("editVoucherStatus").value = voucher.status

  // Show modal using standard Bootstrap data attributes
  const modalElement = document.getElementById("editVoucherModal")
  modalElement.classList.add("show")
  modalElement.style.display = "block"
  modalElement.setAttribute("aria-hidden", "false")
  document.body.classList.add("modal-open")
  const backdrop = document.createElement("div")
  backdrop.className = "modal-backdrop fade show"
  document.body.appendChild(backdrop)
}

// Đóng modal chỉnh sửa voucher
function closeEditModal() {
    const modalElement = document.getElementById("editVoucherModal");
    modalElement.classList.remove("show");
    modalElement.style.display = "none";
    modalElement.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    const backdrop = document.querySelector(".modal-backdrop");
    if (backdrop) {
        backdrop.remove();
    }
}

// Save voucher edit
function saveVoucherEdit() {
    const voucherId = Number.parseInt(document.getElementById("editVoucherId").value)
    const voucherIndex = mockVouchers.findIndex((v) => v.id === voucherId)

    if (voucherIndex === -1) return

    // Update voucher data
    mockVouchers[voucherIndex] = {
        ...mockVouchers[voucherIndex],
        name: document.getElementById("editVoucherName").value,
        code: document.getElementById("editVoucherCode").value,
        description: document.getElementById("editVoucherDescription").value,
        discountValue: Number.parseFloat(document.getElementById("editDiscountValue").value),
        startDate: document.getElementById("editStartDate").value,
        endDate: document.getElementById("editEndDate").value,
        status: document.getElementById("editVoucherStatus").value,
    }

    // Close modal manually
    const modalElement = document.getElementById("editVoucherModal");
    modalElement.classList.remove("show");
    modalElement.style.display = "none";
    modalElement.setAttribute("aria-hidden", "true");
    document.body.classList.remove("modal-open");
    const backdrop = document.querySelector(".modal-backdrop");
    if (backdrop) {
        backdrop.remove();
    }

    // Refresh list
    filterVouchers()

    showToast("success", "Voucher đã được cập nhật thành công!")
}

// Toggle voucher status
function toggleVoucherStatus(voucherId) {
  const voucherIndex = mockVouchers.findIndex((v) => v.id === voucherId)
  if (voucherIndex === -1) return

  const voucher = mockVouchers[voucherIndex]
  const currentStatus = getVoucherStatus(voucher)

  if (currentStatus === "disabled") {
    voucher.status = "active"
    showToast("success", "Voucher đã được kích hoạt!")
  } else {
    voucher.status = "disabled"
    showToast("success", "Voucher đã được vô hiệu hóa!")
  }

  filterVouchers()
}

// Reset form to initial state
function resetForm() {
  document.getElementById("voucherForm").reset()

  currentStep = 1
  updateStepDisplay()
  updateNavigationButtons()
  updateProgressSteps()

  document.querySelectorAll(".form-control").forEach((field) => {
    field.classList.remove("is-invalid")
  })

  const specificSection = document.getElementById("specificHostelsSection")
  if (specificSection) {
    specificSection.style.display = "none"
  }

  initializeForm()
  setDefaultDates()
  updatePreview()

  showToast("success", "Form đã được làm mới!")
}

// Export all vouchers
function exportAllVouchers() {
  const dataStr = JSON.stringify(mockVouchers, null, 2)
  const dataBlob = new Blob([dataStr], { type: "application/json" })

  const link = document.createElement("a")
  link.href = URL.createObjectURL(dataBlob)
  link.download = `vouchers_${new Date().toISOString().split("T")[0]}.json`
  link.click()

  showToast("success", "Dữ liệu voucher đã được xuất thành công!")
}

// Show toast notification
function showToast(type, message) {
  const toastElement = document.getElementById(type + "Toast")
  const messageElement = document.getElementById(type + "Message")

  if (!toastElement || !messageElement) return

  messageElement.textContent = message
  toastElement.classList.add("show")

  setTimeout(() => {
    hideToast(type + "Toast")
  }, 5000)
}

// Hide toast notification
function hideToast(toastId) {
  const toastElement = document.getElementById(toastId)
  if (toastElement) {
    toastElement.classList.remove("show")
  }
}

// Initialize keyboard shortcuts
document.addEventListener("keydown", (event) => {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
    if (currentSection === "create") {
      if (currentStep === totalSteps) {
        document.getElementById("submitBtn").click()
      } else {
        document.getElementById("nextBtn").click()
      }
    }
  }

  if (event.key === "Escape" && currentStep > 1 && currentSection === "create") {
    document.getElementById("prevBtn").click()
  }
})