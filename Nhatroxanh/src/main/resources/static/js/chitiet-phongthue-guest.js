// Sample report data
const reportData = {
  BC001: {
    id: "BC001",
    title: "Điều hòa không hoạt động",
    description:
      "Điều hòa không thể bật, có thể do hỏng remote hoặc máy lạnh. Đã thử thay pin remote nhưng vẫn không hoạt động. Phòng rất nóng và không thoải mái.",
    priority: "high",
    priorityText: "Cao",
    status: "pending",
    statusText: "Chưa xử lý",
    date: "16/12/2024",
    response: null,
  },
  BC002: {
    id: "BC002",
    title: "Vòi nước bị rỉ",
    description: "Vòi nước trong phòng tắm bị rỉ nhỏ giọt liên tục, gây lãng phí nước và tiếng ồn khó chịu.",
    priority: "medium",
    priorityText: "Trung bình",
    status: "resolved",
    statusText: "Đã giải quyết",
    date: "15/12/2024",
    response: "Đã thay thế vòi nước mới. Vấn đề đã được khắc phục hoàn toàn. Cảm ơn bạn đã báo cáo.",
  },
  BC003: {
    id: "BC003",
    title: "Thiếu khăn tắm",
    description: "Phòng chỉ có 1 khăn tắm, cần bổ sung thêm cho 2 khách. Cần thêm khăn mặt và khăn tắm.",
    priority: "low",
    priorityText: "Thấp",
    status: "resolved",
    statusText: "Đã giải quyết",
    date: "15/12/2024",
    response: "Đã bổ sung thêm khăn tắm và các vật dụng cần thiết. Xin lỗi vì sự bất tiện này.",
  },
}

// Store uploaded images
let uploadedImages = []

// Rating system variables
let ratings = {
  overall: 0,
  cleanliness: 0,
  service: 0,
  amenities: 0,
  price: 0,
}

const ratingTexts = {
  1: "Rất không hài lòng",
  2: "Không hài lòng",
  3: "Bình thường",
  4: "Hài lòng",
  5: "Rất hài lòng",
}

// Navigation functions
function goBack() {
  window.history.back()
}

// Contract functions
function viewContractDetail() {
  const modal = document.getElementById("contractDetailModal")
  modal.classList.add("active")
  document.body.style.overflow = "hidden"
}

function closeContractDetailModal() {
  const modal = document.getElementById("contractDetailModal")
  modal.classList.remove("active")
  document.body.style.overflow = "auto"
}

function downloadContract() {
  // Simulate contract download
  console.log("Downloading contract PDF...")
  alert("Đang tải xuống hợp đồng PDF. Vui lòng chờ trong giây lát...")

  // In a real application, this would trigger a file download
  // window.open('/api/contract/download/HD2024001', '_blank')
}

// Image Upload Functions
function handleImageUpload(event) {
  const files = event.target.files

  if (!files || files.length === 0) {
    return
  }

  const container = document.getElementById("imagePreviewContainer")

  for (let i = 0; i < files.length; i++) {
    const file = files[i]

    // Validate file size (5MB max)
    if (file.size > 5 * 1024 * 1024) {
      alert(`Ảnh "${file.name}" quá lớn. Vui lòng chọn ảnh nhỏ hơn 5MB.`)
      continue
    }

    // Validate file type
    if (!file.type.startsWith("image/")) {
      alert(`"${file.name}" không phải là file ảnh hợp lệ.`)
      continue
    }

    const reader = new FileReader()
    reader.onload = (e) => {
      const imageData = {
        file: file,
        url: e.target.result,
        name: file.name,
      }

      uploadedImages.push(imageData)
      addImagePreview(imageData, uploadedImages.length - 1)
    }
    reader.readAsDataURL(file)
  }

  // Clear the input to allow selecting the same files again if needed
  event.target.value = ""
}

function addImagePreview(imageData, index) {
  const container = document.getElementById("imagePreviewContainer")

  const previewItem = document.createElement("div")
  previewItem.className = "image-preview-item-guest"
  previewItem.innerHTML = `
        <img src="${imageData.url}" alt="${imageData.name}" class="image-preview-img-guest">
        <button type="button" class="image-preview-remove-guest" onclick="removeImage(${index})">
            <i class="fas fa-times"></i>
        </button>
    `

  container.appendChild(previewItem)
}

function removeImage(index) {
  uploadedImages.splice(index, 1)
  refreshImagePreviews()
}

function refreshImagePreviews() {
  const container = document.getElementById("imagePreviewContainer")
  container.innerHTML = ""

  uploadedImages.forEach((imageData, index) => {
    addImagePreview(imageData, index)
  })
}

// Initialize upload area event listeners
function initializeImageUpload() {
  const uploadArea = document.getElementById("imageUploadArea")
  const fileInput = document.getElementById("imageInput")

  if (!uploadArea || !fileInput) return

  // Handle click on upload area
  uploadArea.addEventListener("click", () => {
    fileInput.click()
  })

  // Handle file input change
  fileInput.addEventListener("change", handleImageUpload)

  // Drag and drop functionality
  uploadArea.addEventListener("dragover", function (e) {
    e.preventDefault()
    this.classList.add("dragover")
  })

  uploadArea.addEventListener("dragleave", function (e) {
    e.preventDefault()
    this.classList.remove("dragover")
  })

  uploadArea.addEventListener("drop", function (e) {
    e.preventDefault()
    this.classList.remove("dragover")

    const files = e.dataTransfer.files
    if (files.length > 0) {
      // Manually trigger the handleImageUpload function with the dropped files
      handleImageUpload({ target: { files: files, value: "" } })
    }
  })
}

// Report Detail Modal Functions
function viewReportDetail(reportId) {
  const report = reportData[reportId]
  if (!report) return

  const modal = document.getElementById("reportDetailModal")
  const title = document.getElementById("reportDetailTitle")
  const content = document.getElementById("reportDetailContent")
  const footer = document.getElementById("reportDetailFooter")

  title.innerHTML = `
        <i class="fas fa-info-circle"></i>
        Chi tiết báo cáo ${reportId}
    `

  const priorityClass =
    report.priority === "high"
      ? "priority-high-guest"
      : report.priority === "medium"
        ? "priority-medium-guest"
        : "priority-low-guest"

  const statusClass =
    report.status === "pending"
      ? "status-pending-guest"
      : report.status === "processing"
        ? "status-processing-guest"
        : "status-resolved-guest"

  content.innerHTML = `
        <div class="report-detail-section-guest">
            <div class="report-detail-title-guest">${report.title}</div>
            <div class="report-detail-meta-guest">
                <span class="priority-badge-guest ${priorityClass}">
                    <i class="fas fa-flag"></i>
                    ${report.priorityText}
                </span>
                <span class="status-badge-guest ${statusClass}">
                    <i class="fas fa-${report.status === "pending" ? "clock" : report.status === "processing" ? "cog" : "check-circle"}"></i>
                    ${report.statusText}
                </span>
                <span class="status-badge-guest" style="background: #6b7280;">
                    <i class="fas fa-calendar"></i>
                    ${report.date}
                </span>
            </div>
            <div class="report-detail-description-guest">
                ${report.description}
            </div>
        </div>
        ${
          report.response
            ? `
            <div class="report-response-section-guest">
                <div class="report-response-title-guest">
                    <i class="fas fa-reply"></i>
                    Phản hồi từ quản lý:
                </div>
                <div class="report-response-text-guest">
                    ${report.response}
                </div>
            </div>
        `
            : ""
        }
    `

  // Show edit/delete buttons only for pending reports
  if (report.status === "pending") {
    footer.innerHTML = `
            <button class="modal-btn-guest modal-btn-secondary-guest" onclick="closeReportDetailModal()">
                <i class="fas fa-times"></i>
                Đóng
            </button>
            <button class="modal-btn-guest modal-btn-danger-guest" onclick="deleteReport('${reportId}')">
                <i class="fas fa-trash"></i>
                Xóa
            </button>
            <button class="modal-btn-guest modal-btn-primary-guest" onclick="editReport('${reportId}')">
                <i class="fas fa-edit"></i>
                Chỉnh sửa
            </button>
        `
  } else {
    footer.innerHTML = `
            <button class="modal-btn-guest modal-btn-secondary-guest" onclick="closeReportDetailModal()">
                <i class="fas fa-times"></i>
                Đóng
            </button>
        `
  }

  modal.classList.add("active")
  document.body.style.overflow = "hidden"
}

function closeReportDetailModal() {
  const modal = document.getElementById("reportDetailModal")
  modal.classList.remove("active")
  document.body.style.overflow = "auto"
}

function editReport(reportId) {
  const report = reportData[reportId]
  if (!report || report.status !== "pending") return

  closeReportDetailModal()

  // Populate edit form
  document.getElementById("reportId").value = reportId
  document.getElementById("reportTitle").value = report.title
  document.getElementById("reportPriority").value = report.priority
  document.getElementById("reportDescription").value = report.description

  // Update modal title and button text
  document.getElementById("reportModalTitle").innerHTML = `
        <i class="fas fa-edit"></i>
        Chỉnh Sửa Báo Cáo
    `
  document.getElementById("reportSubmitText").textContent = "Cập nhật báo cáo"

  openReportModal()
}

function deleteReport(reportId) {
  if (confirm("Bạn có chắc chắn muốn xóa báo cáo này không?")) {
    console.log("Xóa báo cáo:", reportId)

    // Remove from data (in real app, this would be an API call)
    delete reportData[reportId]

    alert("Báo cáo đã được xóa thành công!")
    closeReportDetailModal()

    // Refresh the page or update the table
    location.reload()
  }
}

// Report Modal Functions
function openReportModal() {
  const modal = document.getElementById("reportModal")
  modal.classList.add("active")
  document.body.style.overflow = "hidden"
}

function closeReportModal() {
  const modal = document.getElementById("reportModal")
  modal.classList.remove("active")
  document.body.style.overflow = "auto"

  // Reset form
  document.getElementById("reportForm").reset()
  document.getElementById("reportId").value = ""

  // Clear uploaded images
  uploadedImages = []
  document.getElementById("imagePreviewContainer").innerHTML = ""

  // Reset modal title and button text
  document.getElementById("reportModalTitle").innerHTML = `
        <i class="fas fa-exclamation-triangle"></i>
        Báo Cáo Sự Cố
    `
  document.getElementById("reportSubmitText").textContent = "Gửi báo cáo"
}

function submitReport() {
  const reportId = document.getElementById("reportId").value
  const title = document.getElementById("reportTitle").value
  const priority = document.getElementById("reportPriority").value
  const description = document.getElementById("reportDescription").value

  if (!title || !priority || !description) {
    alert("Vui lòng điền đầy đủ thông tin!")
    return
  }

  const isEdit = reportId !== ""

  // Simulate API call
  console.log(isEdit ? "Cập nhật báo cáo:" : "Gửi báo cáo:", {
    id: reportId || "BC" + String(Date.now()).slice(-3),
    room: "A101 - Deluxe",
    title: title,
    priority: priority,
    description: description,
    images: uploadedImages.map((img) => img.name),
    date: new Date().toLocaleDateString("vi-VN"),
  })

  // Show success message
  alert(
    isEdit
      ? "Báo cáo đã được cập nhật thành công!"
      : "Báo cáo sự cố đã được gửi thành công! Chúng tôi sẽ xử lý trong thời gian sớm nhất.",
  )

  // Close modal
  closeReportModal()

  // Optionally refresh the reports table
  // location.reload();
}

// Extend Modal Functions
function openExtendModal() {
  const modal = document.getElementById("extendModal")
  const today = new Date()
  const currentExpiry = new Date("2024-12-20")
  const minDate = new Date(Math.max(today, currentExpiry))
  minDate.setDate(minDate.getDate() + 1)

  // Set minimum date for custom date input
  document.getElementById("customExtendDate").min = minDate.toISOString().split("T")[0]

  modal.classList.add("active")
  document.body.style.overflow = "hidden"
}

function closeExtendModal() {
  const modal = document.getElementById("extendModal")
  modal.classList.remove("active")
  document.body.style.overflow = "auto"

  // Reset form
  document.getElementById("extendForm").reset()
  document.getElementById("customDateContainer").style.display = "none"
  document.getElementById("newExpiryDisplay").style.display = "none"
}

function handleExtendPeriodChange() {
  const select = document.getElementById("extendPeriod")
  const customContainer = document.getElementById("customDateContainer")
  const newExpiryDisplay = document.getElementById("newExpiryDisplay")

  if (select.value === "custom") {
    customContainer.style.display = "block"
    newExpiryDisplay.style.display = "none"
  } else if (select.value !== "") {
    customContainer.style.display = "none"
    calculateNewExpiryDate(Number.parseInt(select.value))
    newExpiryDisplay.style.display = "block"
  } else {
    customContainer.style.display = "none"
    newExpiryDisplay.style.display = "none"
  }
}

function calculateNewExpiryDate(months) {
  const currentExpiry = new Date("2024-12-20")
  const newExpiry = new Date(currentExpiry)
  newExpiry.setMonth(newExpiry.getMonth() + months)

  const formattedDate = newExpiry.toLocaleDateString("vi-VN")
  document.getElementById("newExpiryDate").textContent = formattedDate
}

function updateNewExpiryDate() {
  const customDate = document.getElementById("customExtendDate").value
  const newExpiryDisplay = document.getElementById("newExpiryDisplay")

  if (customDate) {
    const date = new Date(customDate)
    const formattedDate = date.toLocaleDateString("vi-VN")
    document.getElementById("newExpiryDate").textContent = formattedDate
    newExpiryDisplay.style.display = "block"
  } else {
    newExpiryDisplay.style.display = "none"
  }
}

function submitExtend() {
  const extendPeriod = document.getElementById("extendPeriod").value
  const customDate = document.getElementById("customExtendDate").value
  const message = document.getElementById("extendMessage").value

  if (!extendPeriod) {
    alert("Vui lòng chọn thời gian gia hạn!")
    return
  }

  if (extendPeriod === "custom" && !customDate) {
    alert("Vui lòng chọn ngày hết hạn mới!")
    return
  }

  let newExpiryDate
  if (extendPeriod === "custom") {
    newExpiryDate = customDate
  } else {
    const currentExpiry = new Date("2024-12-20")
    const newExpiry = new Date(currentExpiry)
    newExpiry.setMonth(newExpiry.getMonth() + Number.parseInt(extendPeriod))
    newExpiryDate = newExpiry.toISOString().split("T")[0]
  }

  // Simulate API call
  console.log("Gửi yêu cầu gia hạn:", {
    room: "A101 - Deluxe",
    extendPeriod: extendPeriod === "custom" ? "Tùy chỉnh" : `${extendPeriod} tháng`,
    newExpiryDate: newExpiryDate,
    message: message,
    currentExpiry: "2024-12-20",
  })

  // Show success message
  const displayDate = new Date(newExpiryDate).toLocaleDateString("vi-VN")
  alert(
    `Yêu cầu gia hạn đã được gửi thành công!\n\nThông tin yêu cầu:\n- Gia hạn đến: ${displayDate}\n- Chủ nhà sẽ nhận được thông báo và phản hồi sớm nhất có thể.`,
  )

  // Close modal
  closeExtendModal()
}

// Return Modal Functions
function openReturnModal() {
  const modal = document.getElementById("returnModal")
  const today = new Date().toISOString().split("T")[0]
  document.getElementById("returnDate").value = today
  document.getElementById("returnDate").min = today

  modal.classList.add("active")
  document.body.style.overflow = "hidden"
}

function closeReturnModal() {
  const modal = document.getElementById("returnModal")
  modal.classList.remove("active")
  document.body.style.overflow = "auto"

  // Reset form
  document.getElementById("returnForm").reset()
}

function submitReturn() {
  const returnDate = document.getElementById("returnDate").value
  const returnReason = document.getElementById("returnReason").value

  if (!returnDate) {
    alert("Vui lòng chọn ngày trả phòng!")
    return
  }

  // Hiển thị thông báo xác nhận
  const confirmMessage = `
        Xác nhận trả phòng A101 - Deluxe
        
        Ngày trả: ${new Date(returnDate).toLocaleDateString("vi-VN")}
        ${returnReason ? `Lý do: ${returnReason}` : ""}
        
        Bạn có chắc chắn muốn thực hiện?
    `

  if (confirm(confirmMessage)) {
    // Simulate API call
    console.log("Trả phòng:", {
      room: "A101 - Deluxe",
      returnDate: returnDate,
      reason: returnReason,
    })

    // Show success message
    alert("Trả phòng thành công! Phòng đã được cập nhật trạng thái.")

    // Close return modal
    closeReturnModal()

    // Open rating modal after successful return
    setTimeout(() => {
      openRatingModal()
    }, 500)
  }
}

// Rating Modal Functions
function openRatingModal() {
  const modal = document.getElementById("ratingModal")
  modal.classList.add("active")
  document.body.style.overflow = "hidden"

  // Initialize rating system
  initializeRatingSystem()
}

function closeRatingModal() {
  const modal = document.getElementById("ratingModal")
  modal.classList.remove("active")
  document.body.style.overflow = "auto"

  // Reset ratings
  resetRatings()
}

function initializeRatingSystem() {
  // Overall rating
  const overallStars = document.querySelectorAll("#overallRating .star")
  overallStars.forEach((star, index) => {
    star.addEventListener("click", () => setOverallRating(index + 1))
    star.addEventListener("mouseover", () => highlightStars(overallStars, index + 1))
    star.addEventListener("mouseout", () => highlightStars(overallStars, ratings.overall))
  })

  // Category ratings
  const categories = ["cleanliness", "service", "amenities", "price"]
  categories.forEach((category) => {
    const categoryStars = document.querySelectorAll(`[data-category="${category}"] .star`)
    categoryStars.forEach((star, index) => {
      star.addEventListener("click", () => setCategoryRating(category, index + 1))
      star.addEventListener("mouseover", () => highlightStars(categoryStars, index + 1))
      star.addEventListener("mouseout", () => highlightStars(categoryStars, ratings[category]))
    })
  })
}

function setOverallRating(rating) {
  ratings.overall = rating
  const overallStars = document.querySelectorAll("#overallRating .star")
  highlightStars(overallStars, rating)
  document.getElementById("overallRatingText").textContent = ratingTexts[rating]
}

function setCategoryRating(category, rating) {
  ratings[category] = rating
  const categoryStars = document.querySelectorAll(`[data-category="${category}"] .star`)
  highlightStars(categoryStars, rating)
}

function highlightStars(stars, rating) {
  stars.forEach((star, index) => {
    if (index < rating) {
      star.classList.add("active")
    } else {
      star.classList.remove("active")
    }
  })
}

function resetRatings() {
  ratings = {
    overall: 0,
    cleanliness: 0,
    service: 0,
    amenities: 0,
    price: 0,
  }

  // Reset all stars
  document.querySelectorAll(".star").forEach((star) => {
    star.classList.remove("active")
  })

  // Reset overall rating text
  document.getElementById("overallRatingText").textContent = "Chọn số sao để đánh giá"

  // Reset comment
  document.getElementById("ratingComment").value = ""
}

function submitRating() {
  if (ratings.overall === 0) {
    alert("Vui lòng chọn đánh giá tổng thể!")
    return
  }

  const comment = document.getElementById("ratingComment").value

  // Simulate API call
  console.log("Gửi đánh giá:", {
    room: "A101 - Deluxe",
    ratings: ratings,
    comment: comment,
  })

  alert("Cảm ơn bạn đã đánh giá! Phản hồi của bạn rất quan trọng với chúng tôi.")
  closeRatingModal()
}

function skipRating() {
  if (confirm("Bạn có chắc chắn muốn bỏ qua đánh giá không?")) {
    closeRatingModal()
  }
}

// Event Listeners and Initialization
function initializeEventListeners() {
  // Close modals when clicking outside
  document.querySelectorAll(".modal-overlay-guest").forEach((modal) => {
    modal.addEventListener("click", function (e) {
      if (e.target === this) {
        this.classList.remove("active")
        document.body.style.overflow = "auto"
      }
    })
  })

  // Close modals with Escape key
  document.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
      document.querySelectorAll(".modal-overlay-guest.active").forEach((modal) => {
        modal.classList.remove("active")
        document.body.style.overflow = "auto"
      })
    }
  })
}

// Initialize page
document.addEventListener("DOMContentLoaded", () => {
  console.log("Room detail page loaded")
  // Initialize image upload functionality
  initializeImageUpload()
  // Initialize event listeners
  initializeEventListeners()
})
