// Voucher Management System with Unique Naming
document.addEventListener("DOMContentLoaded", () => {
  setVoucherDefaultDates()
})

// Section navigation
function switchVoucherSection(sectionName) {
  // Hide all sections
  document.querySelectorAll(".voucher-content-panel").forEach((section) => {
    section.classList.remove("voucher-panel-active")
  })

  // Show selected section
  document.getElementById("voucher" + capitalizeFirst(sectionName) + "Panel").classList.add("voucher-panel-active")

  // Update nav tabs
  document.querySelectorAll(".voucher-tab-button").forEach((tab) => {
    tab.classList.remove("voucher-tab-active")
  })
  document.querySelector(`[data-voucher-section="${sectionName}"]`).classList.add("voucher-tab-active")
}

function capitalizeFirst(str) {
  return str.charAt(0).toUpperCase() + str.slice(1)
}

// Generate voucher code
function generateVoucherCode() {
  const prefix = "VC"
  const timestamp = Date.now().toString().slice(-6)
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  const code = `${prefix}${timestamp}${random}`

  document.getElementById("voucherCodeInput").value = code
  showVoucherToast("success", "Mã voucher đã được tạo tự động!")
}

// Set default dates
function setVoucherDefaultDates() {
  const now = new Date()
  const startDate = new Date(now.getTime() + 24 * 60 * 60 * 1000)
  const endDate = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)

  document.getElementById("voucherStartDateInput").value = formatVoucherDateTimeLocal(startDate)
  document.getElementById("voucherEndDateInput").value = formatVoucherDateTimeLocal(endDate)
}

function formatVoucherDateTimeLocal(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  const hours = String(date.getHours()).padStart(2, "0")
  const minutes = String(date.getMinutes()).padStart(2, "0")
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

// Toggle hostel selection section
function toggleVoucherHostelSection() {
  const selectedScope = document.querySelector('input[name="hostelScope"]:checked').value
  const specificSection = document.getElementById("voucherHostelSelectionPanel")

  if (specificSection) {
    specificSection.style.display = selectedScope === "specific" ? "block" : "none"
  }
}

// Reset form
function resetVoucherForm() {
  document.getElementById("voucherCreationForm").reset()

  // Remove validation classes
  document.querySelectorAll(".voucher-input-control").forEach((field) => {
    field.classList.remove("voucher-input-invalid")
  })

  // Reset defaults
  document.getElementById("voucherStatusSelect").value = "active"
  document.querySelector('input[name="hostelScope"][value="all"]').checked = true
  document.getElementById("voucherHostelSelectionPanel").style.display = "none"

  setVoucherDefaultDates()
  showVoucherToast("success", "Form đã được làm mới!")
}

// Form validation
function validateVoucherForm() {
  const requiredFields = document.querySelectorAll("[required]")
  let isValid = true

  requiredFields.forEach((field) => {
    if (!field.value.trim()) {
      field.classList.add("voucher-input-invalid")
      isValid = false
    } else {
      field.classList.remove("voucher-input-invalid")
    }
  })

  // Validate discount value
  const discountValue = Number.parseFloat(document.getElementById("voucherDiscountInput").value)
  if (discountValue < 1000) {
    document.getElementById("voucherDiscountInput").classList.add("voucher-input-invalid")
    showVoucherToast("error", "Số tiền giảm tối thiểu là 1,000 VNĐ")
    isValid = false
  }

  // Validate dates
  const startDate = new Date(document.getElementById("voucherStartDateInput").value)
  const endDate = new Date(document.getElementById("voucherEndDateInput").value)
  const now = new Date()

  if (startDate <= now) {
    document.getElementById("voucherStartDateInput").classList.add("voucher-input-invalid")
    showVoucherToast("error", "Ngày bắt đầu phải sau thời điểm hiện tại")
    isValid = false
  }

  if (endDate <= startDate) {
    document.getElementById("voucherEndDateInput").classList.add("voucher-input-invalid")
    showVoucherToast("error", "Ngày kết thúc phải sau ngày bắt đầu")
    isValid = false
  }

  if (!isValid) {
    showVoucherToast("error", "Vui lòng điền đầy đủ thông tin bắt buộc")
  }

  return isValid
}

// Voucher management functions
function editVoucherItem(voucherId) {
  // Sample data - Backend sẽ cung cấp data thực
  const sampleVoucherData = {
    1: {
      name: "Voucher Giảm Giá Tháng 12",
      code: "VC123456",
      description: "Voucher giảm giá đặc biệt cho tháng 12",
      discountValue: 50000,
      maxDiscount: 100000,
      startDate: "2024-12-01T00:00",
      endDate: "2024-12-31T23:59",
      totalUsageLimit: 100,
      status: "active",
    },
    2: {
      name: "Voucher Khách Hàng Mới",
      code: "VC789012",
      description: "Voucher dành cho khách hàng mới",
      discountValue: 100000,
      maxDiscount: 200000,
      startDate: "2024-12-15T00:00",
      endDate: "2025-01-15T23:59",
      totalUsageLimit: 50,
      status: "scheduled",
    },
    3: {
      name: "Voucher Sinh Nhật",
      code: "VC345678",
      description: "Voucher sinh nhật đặc biệt",
      discountValue: 75000,
      maxDiscount: 150000,
      startDate: "2024-11-01T00:00",
      endDate: "2024-11-30T23:59",
      totalUsageLimit: 100,
      status: "disabled",
    },
  }

  const data = sampleVoucherData[voucherId]
  if (data) {
    // Populate edit form
    document.getElementById("editVoucherIdInput").value = voucherId
    document.getElementById("editVoucherNameInput").value = data.name
    document.getElementById("editVoucherCodeInput").value = data.code
    document.getElementById("editVoucherDescriptionInput").value = data.description
    document.getElementById("editVoucherDiscountInput").value = data.discountValue
    document.getElementById("editVoucherMaxDiscountInput").value = data.maxDiscount
    document.getElementById("editVoucherStartDateInput").value = data.startDate
    document.getElementById("editVoucherEndDateInput").value = data.endDate
    document.getElementById("editVoucherUsageLimitInput").value = data.totalUsageLimit
    document.getElementById("editVoucherStatusSelect").value = data.status

    showVoucherModal()
  }
}

function deleteVoucherItem(voucherId) {
  if (confirm("Bạn có chắc chắn muốn xóa voucher này?")) {
    // Backend sẽ xử lý việc xóa
    showVoucherToast("success", "Voucher đã được xóa thành công!")
    // Reload danh sách voucher
  }
}

function toggleVoucherItem(voucherId) {
  // Backend sẽ xử lý việc bật/tắt voucher
  showVoucherToast("success", "Trạng thái voucher đã được cập nhật!")
  // Reload danh sách voucher
}

// Modal functions
function showVoucherModal() {
  const modal = document.getElementById("voucherEditModal")
  modal.classList.add("voucher-modal-show")
  modal.style.display = "block"
  modal.setAttribute("aria-hidden", "false")
  document.body.classList.add("voucher-modal-open")
}

function closeVoucherModal() {
  const modal = document.getElementById("voucherEditModal")
  modal.classList.remove("voucher-modal-show")
  modal.style.display = "none"
  modal.setAttribute("aria-hidden", "true")
  document.body.classList.remove("voucher-modal-open")
}

// Toast notifications
function showVoucherToast(type, message) {
  const toastId = type === "success" ? "voucherSuccessToast" : "voucherErrorToast"
  const messageId = type === "success" ? "voucherSuccessMessage" : "voucherErrorMessage"

  const toast = document.getElementById(toastId)
  const messageElement = document.getElementById(messageId)

  if (!toast || !messageElement) return

  messageElement.textContent = message
  toast.classList.add("voucher-toast-show")

  setTimeout(() => {
    hideVoucherToast(toastId)
  }, 5000)
}

function hideVoucherToast(toastId) {
  const toast = document.getElementById(toastId)
  if (toast) {
    toast.classList.remove("voucher-toast-show")
  }
}

// Form submissions
document.getElementById("voucherCreationForm").addEventListener("submit", (e) => {
  if (!validateVoucherForm()) {
    e.preventDefault()
    return false
  }

  // Show loading state
  const submitBtn = e.target.querySelector('button[type="submit"]')
  const originalText = submitBtn.innerHTML
  submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tạo voucher...'
  submitBtn.disabled = true

  // Form will be submitted to backend
})

document.getElementById("voucherEditForm").addEventListener("submit", (e) => {
  const submitBtn = e.target.querySelector('button[type="submit"]')
  const originalText = submitBtn.innerHTML

  submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...'
  submitBtn.disabled = true

  // Form will be submitted to backend at /vouchers/update
})
