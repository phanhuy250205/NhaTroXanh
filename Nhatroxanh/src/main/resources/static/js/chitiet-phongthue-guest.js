let uploadedImages = []

function goBack() {
  window.history.back()
}

function openContractDetailModal(contractId) {
  const modal = document.getElementById("contractDetailModal");
  if (!modal) {
    console.error("Không tìm thấy contractDetailModal");
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể mở chi tiết hợp đồng. Vui lòng thử lại!',
    });
    return;
  }
  if (!contractId) {
    console.error("contractId không hợp lệ:", contractId);
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể tải thông tin hợp đồng. Vui lòng thử lại!',
    });
    return;
  }
  modal.classList.add("active");
  document.body.style.overflow = "hidden";
}

// Hàm đóng modal chi tiết hợp đồng
function closeContractDetailModal() {
  const modal = document.getElementById("contractDetailModal");
  if (!modal) {
    console.error("Không tìm thấy contractDetailModal");
    return;
  }

  modal.classList.remove("active");
  document.body.style.overflow = "auto";
}

function printContract(position) {
  console.log("In hợp đồng từ vị trí:", position);
  const modalContent = document.querySelector(".contract-full-content-guest");
  if (!modalContent) {
    console.error("Không tìm thấy contract-full-content-guest");
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể in hợp đồng. Vui lòng thử lại!',
    });
    return;
  }

  const actionsHeader = modalContent.querySelector(".contract-actions-header-guest");
  if (actionsHeader) actionsHeader.style.display = "none";

  const printWindow = window.open('', '_blank');
  printWindow.document.write(`
    <html>
      <head>
        <title>In Hợp Đồng</title>
        <style>
          @page { size: A4; margin: 10mm; }
          body { 
            font-family: 'Times New Roman', serif; 
            margin: 0; 
            padding: 20mm; 
            font-size: 12px; 
            color: #333; 
            line-height: 1.6;
          }
          .contract-full-content-guest { 
            max-width: 210mm; 
            margin: 0 auto; 
            border: none; 
            background-color: #fff;
          }
          .contract-national-title { text-align: center; }
          .contract-national-title h1 { 
            font-size: 24px; 
            font-weight: bold; 
            color: #d32f2f; 
            margin: 0 0 5mm 0;
          }
          .contract-national-title p { 
            font-size: 18px; 
            font-weight: bold; 
            color: #333; 
            margin: 2mm 0;
          }
          hr { border: 1px solid #d32f2f; margin-bottom: 10mm; }
          .contract-detail-header-guest, .contract-actions-header-guest { display: none; }
          .contract-section { 
            margin-bottom: 10mm; 
            padding: 5mm; 
            border: 1px solid #ddd; 
            border-radius: 0; 
            background-color: #fff; 
            page-break-inside: avoid;
          }
          .contract-section-blue { background-color: #e3f2fd; border-color: #bbdefb; }
          .contract-section-title { 
            font-size: 16px; 
            font-weight: 600; 
            color: #2c3e50; 
            margin-bottom: 5mm;
          }
          .contract-info-row { display: flex; margin-bottom: 3mm; }
          .contract-info-label { font-weight: bold; width: 70mm; color: #555; }
          .contract-info-value { flex: 1; }
          .contract-utilities-list li { margin-bottom: 2mm; }
          .contract-terms-box { 
            white-space: pre-wrap; 
            font-family: 'Times New Roman', serif; 
            padding: 5mm; 
            border: 1px solid #ddd; 
            border-radius: 0; 
            color: #333; 
            line-height: 1.5; 
            font-size: 12px; 
            page-break-inside: avoid;
          }
          .contract-signature-section { margin-top: 10mm; }
          .contract-signature-info { display: flex; justify-content: space-between; gap: 10mm; }
          .contract-signature-party { flex: 1; text-align: center; }
          .contract-signature-title { font-size: 14px; font-weight: bold; color: #2c3e50; margin-bottom: 3mm; }
          .contract-signature-area { margin-top: 5mm; border-top: 1px dashed #ddd; padding-top: 5mm; }
          .contract-footer-note { text-align: center; font-size: 10px; color: #666; margin-top: 10mm; padding: 5mm; border-top: 1px solid #ddd; }
          @media print {
            body { -webkit-print-color-adjust: exact; }
            .contract-full-content-guest { break-inside: avoid; }
          }
        </style>
      </head>
      <body>
        ${modalContent.outerHTML}
      </body>
    </html>
  `);
  printWindow.document.close();
  printWindow.focus();
  printWindow.print();
  printWindow.close();

  if (actionsHeader) actionsHeader.style.display = "flex";
}

// Hàm tải xuống hợp đồng dưới dạng PDF
function downloadContractAsPDF() {
  const modal = document.getElementById("contractDetailModal");
  if (!modal) {
    console.error("Không tìm thấy contractDetailModal");
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể tải hợp đồng. Vui lòng thử lại!',
    });
    return;
  }

  // Mở modal để đảm bảo nội dung được render
  modal.classList.add("active");
  document.body.style.overflow = "hidden";
  window.scrollTo(0, 0); // Đảm bảo nội dung ở đầu trang

  const modalContent = document.querySelector(".contract-full-content-guest");
  if (!modalContent) {
    console.error("Không tìm thấy contract-full-content-guest");
    closeContractDetailModal();
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể tải hợp đồng. Vui lòng thử lại!',
    });
    return;
  }

  // Ẩn các nút hành động trước khi tạo PDF
  const actionsHeader = modalContent.querySelector(".contract-actions-header-guest");
  if (actionsHeader) actionsHeader.style.display = "none";

  // Log nội dung để debug
  console.log("Nội dung trước khi tạo PDF:", modalContent.innerHTML);

  const opt = {
    margin: [5, 5, 5, 5], // Giảm lề để chứa nhiều nội dung hơn
    filename: `HopDong_CT${modalContent.querySelector(".contract-id-badge-guest span").textContent.replace("Mã hợp đồng: #CT", "")}.pdf`,
    image: { type: 'jpeg', quality: 0.95 },
    html2canvas: { scale: 1.5, useCORS: true, windowWidth: document.documentElement.scrollWidth },
    jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
    pagebreak: { mode: ['css', 'legacy'], avoid: ['.contract-section-guest', '.contract-terms-box'] }
  };

  html2pdf().from(modalContent).set(opt).toPdf().get('pdf').then((pdf) => {
    const totalPages = pdf.internal.getNumberOfPages();
    for (let i = 1; i <= totalPages; i++) {
      pdf.setPage(i);
      pdf.setFontSize(10);
      pdf.text(`Trang ${i} / ${totalPages}`, 190, 285); // Thêm số trang
    }
    pdf.save();
  }).then(() => {
    if (actionsHeader) actionsHeader.style.display = "flex";
    closeContractDetailModal();
    Swal.fire({
      icon: 'success',
      title: 'Thành công',
      text: 'Hợp đồng đã được tải xuống dưới dạng PDF!',
    });
  }).catch((error) => {
    if (actionsHeader) actionsHeader.style.display = "flex";
    closeContractDetailModal();
    console.error("Lỗi khi tạo PDF:", error);
    Swal.fire({
      icon: 'error',
      title: 'Lỗi',
      text: 'Không thể tạo PDF. Vui lòng thử lại!',
    });
  });
}

function handleImageUpload(event) {
  const files = event.target.files
  if (!files || files.length === 0) return

  for (let i = 0; i < files.length; i++) {
    const file = files[i]

    if (file.size > 5 * 1024 * 1024) {
      alert(`Ảnh "${file.name}" quá lớn. Vui lòng chọn ảnh nhỏ hơn 5MB.`)
      continue
    }

    if (!file.type.startsWith("image/")) {
      alert(`"${file.name}" không phải là file ảnh hợp lệ.`)
      continue
    }

    uploadedImages.push(file)
  }

  refreshImagePreviews()
  event.target.value = "" // Cho phép chọn lại cùng 1 ảnh sau khi xóa
}

function refreshImagePreviews() {
  const container = document.getElementById("imagePreviewContainer")
  container.innerHTML = ""

  uploadedImages.forEach((file, index) => {
    const url = URL.createObjectURL(file)

    const previewItem = document.createElement("div")
    previewItem.className = "image-preview-item-guest"
    previewItem.innerHTML = `
            <img src="${url}" alt="${file.name}" class="image-preview-img-guest">
            <button type="button" class="image-preview-remove-guest" onclick="removeImage(${index})">
                <i class="fas fa-times"></i>
            </button>
        `
    container.appendChild(previewItem)
  })
}

function removeImage(index) {
  uploadedImages.splice(index, 1)
  refreshImagePreviews()
}
function deleteOldImage(imageId) {
  fetch('/khach-thue/xoa-anh', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'X-Requested-With': 'XMLHttpRequest'
    },
    body: new URLSearchParams({ imageId })
  })
    .then(response => {
      if (!response.ok) return response.text().then(err => { throw new Error(err) })
      return response.text()
    })
    .then(msg => {
      const imgDiv = document.getElementById('img-' + imageId)
      if (imgDiv) imgDiv.remove()
    })
    .catch(err => {
      alert("Lỗi khi xóa ảnh: " + err.message)
    })
}


let uploadAreaInitialized = false;

function initializeImageUpload() {
  if (uploadAreaInitialized) return; // Ngăn gắn sự kiện nhiều lần
  uploadAreaInitialized = true;

  const uploadArea = document.getElementById("imageUploadArea");
  const fileInput = document.getElementById("imageInput");

  if (!uploadArea || !fileInput) return;

  uploadArea.addEventListener("click", () => {
    fileInput.click();
  });

  fileInput.addEventListener("change", handleImageUpload);

  uploadArea.addEventListener("dragover", function (e) {
    e.preventDefault();
    this.classList.add("dragover");
  });

  uploadArea.addEventListener("dragleave", function (e) {
    e.preventDefault();
    this.classList.remove("dragover");
  });

  uploadArea.addEventListener("drop", function (e) {
    e.preventDefault();
    this.classList.remove("dragover");
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handleImageUpload({ target: { files: files, value: "" } });
    }
  });
}


// ✅ Gắn lại file vào input trước khi submit form
document.getElementById("reportForm").addEventListener("submit", function () {
  const fileInput = document.getElementById("imageInput")

  if (uploadedImages.length > 0) {
    const dataTransfer = new DataTransfer()
    uploadedImages.forEach(file => dataTransfer.items.add(file))
    fileInput.files = dataTransfer.files
  }
})

document.addEventListener("DOMContentLoaded", initializeImageUpload)

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
        ${report.response
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
  // location.reload();
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
