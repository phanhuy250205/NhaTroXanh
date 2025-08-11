document.addEventListener("DOMContentLoaded", () => {
    // Initialize page
    setupEventListeners();
    loadPendingOwners();
});

function setupEventListeners() {
    // Search on Enter key for landlord tab
    const searchInput = document.getElementById("searchInput");
    if (searchInput) {
        searchInput.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                performSearch();
            }
        });
    }

    // Search on Enter key for approval tab
    const searchInputApproval = document.getElementById("searchInputApproval");
    if (searchInputApproval) {
        searchInputApproval.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                performApprovalSearch();
            }
        });
    }

    // Close dropdown when clicking outside
    document.addEventListener("click", (event) => {
        const filterDropdown = document.querySelector(".filter-dropdown-staff");
        const dropdown = document.getElementById("filterDropdown");

        if (filterDropdown && !filterDropdown.contains(event.target) && isDropdownOpen) {
            toggleFilterDropdown();
        }
    });

    // Setup detail modal buttons
    const approveDetailBtn = document.getElementById("approveDetailBtn");
    const rejectDetailBtn = document.getElementById("rejectDetailBtn");

    if (approveDetailBtn) {
        approveDetailBtn.addEventListener("click", () => {
            if (currentDetailId) {
                showConfirmationModal(currentDetailId, "approve");
            }
        });
    }

    if (rejectDetailBtn) {
        rejectDetailBtn.addEventListener("click", () => {
            if (currentDetailId) {
                showConfirmationModal(currentDetailId, "reject");
            }
        });
    }
}

let isDropdownOpen = false;
let currentDetailId = null;
let pendingAction = null;
const bootstrap = window.bootstrap;

function loadPendingOwners() {
    fetch("/nhan-vien/api/pending-owners", {
        method: "GET",
        headers: { "Content-Type": "application/json" }
    })
        .then(response => response.json())
        .then(data => {
            const registrationGrid = document.getElementById("registrationGrid");
            registrationGrid.innerHTML = "";
            data.forEach(user => {
                const card = document.createElement("div");
                card.className = "registration-card-staff";
                card.setAttribute("data-id", user.userId);
                card.innerHTML = `
                <div class="card-header-staff">
                    <div style="display: flex; justify-content: space-between; align-items: center;">
                        <span>Thông tin người dùng</span>
                    </div>
                </div>
                <div class="card-body-staff">
                    <div class="form-group-staff">
                        <label class="form-label-staff">Họ và tên</label>
                        <input type="text" class="form-input-staff" value="${user.fullname || ''}" disabled>
                    </div>
                    <div class="form-group-staff">
                        <label class="form-label-staff">Ngày sinh</label>
                        <input type="text" class="form-input-staff" value="${user.birthday || ''}" disabled>
                    </div>
                    <div class="form-group-staff">
                        <label class="form-label-staff">Số điện thoại</label>
                        <input type="text" class="form-input-staff" value="${user.phone || ''}" disabled>
                    </div>
                    <div class="form-group-staff">
                        <label class="form-label-staff">Giới tính</label>
                        <div class="gender-group-staff">
                            <div class="gender-option-staff">
                                <input type="radio" id="male${user.userId}" name="gender${user.userId}" class="gender-radio-staff" ${user.gender ? 'checked' : ''} disabled>
                                <label for="male${user.userId}" class="gender-label-staff">Nam</label>
                            </div>
                            <div class="gender-option-staff">
                                <input type="radio" id="female${user.userId}" name="gender${user.userId}" class="gender-radio-staff" ${!user.gender ? 'checked' : ''} disabled>
                                <label for="female${user.userId}" class="gender-label-staff">Nữ</label>
                            </div>
                        </div>
                    </div>
                    <div class="card-actions-staff">
                        <button class="view-detail-btn-staff" onclick="showDetailModal(${user.userId})">
                            <i class="fas fa-eye"></i>
                            Xem chi tiết
                        </button>
                    </div>
                </div>
            `;
                registrationGrid.appendChild(card);
            });
        })
        .catch(error => {
            console.error("Error loading pending owners:", error);
            showErrorMessage("Không thể tải danh sách chủ trọ chờ duyệt.");
        });
}

// Tab switching functionality
function switchTab(tabIndex, element) {
    document.querySelectorAll(".custom-tab-staff").forEach((tab) => {
        tab.classList.remove("active");
    });
    element.classList.add("active");
    document.querySelectorAll(".tab-content-panel-staff").forEach((panel) => {
        panel.classList.add("hidden");
    });
    document.getElementById(`tab-content-${tabIndex}`).classList.remove("hidden");
}

// Filter dropdown functionality for landlord tab
function toggleFilterDropdown() {
    const dropdown = document.getElementById("filterDropdown");
    const chevron = document.getElementById("filterChevron");
    const filterBtn = document.querySelector(".filter-btn-staff");

    if (!dropdown || !chevron || !filterBtn) return;

    isDropdownOpen = !isDropdownOpen;

    if (isDropdownOpen) {
        dropdown.classList.add("show");
        chevron.style.transform = "rotate(180deg)";
        filterBtn.classList.add("active");
    } else {
        dropdown.classList.remove("show");
        chevron.style.transform = "rotate(0deg)";
        filterBtn.classList.remove("active");
    }
}

function selectFilter(filterValue, element) {
    document.querySelectorAll(".filter-option-staff").forEach((option) => {
        option.classList.remove("selected");
    });
    element.classList.add("selected");
    const filterText = document.getElementById("filterText");
    const filterTexts = {
        all: "Tất cả",
        active: "Hoạt động",
        inactive: "Vô hiệu hóa",
    };
    if (filterText) {
        filterText.textContent = filterTexts[filterValue];
    }
    toggleFilterDropdown();
    // Apply filter via server-side (reload page with filter param)
    window.location.href = `/nhan-vien/chu-tro?statusFilter=${filterValue}`;
}

// Search functionality for landlord tab
function performSearch() {
    const searchInput = document.getElementById("searchInput");
    if (!searchInput) return;
    const searchTerm = searchInput.value.trim();
    // Redirect to server-side search
    window.location.href = `/nhan-vien/chu-tro?search=${encodeURIComponent(searchTerm)}`;
}

// Search functionality for approval tab
function performApprovalSearch() {
    const searchInput = document.getElementById("searchInputApproval");
    if (!searchInput) return;

    const searchTerm = searchInput.value.toLowerCase().trim();
    const cards = document.querySelectorAll(".registration-card-staff");

    let visibleCount = 0;

    cards.forEach((card) => {
        const nameInput = card.querySelector(".form-input-staff");
        const phoneInput = card.querySelectorAll(".form-input-staff")[2];

        if (nameInput && phoneInput) {
            const name = nameInput.value.toLowerCase();
            const phone = phoneInput.value.toLowerCase();

            if (searchTerm === "" || name.includes(searchTerm) || phone.includes(searchTerm)) {
                card.style.display = "block";
                visibleCount++;
            } else {
                card.style.display = "none";
            }
        }
    });

    showSuccessMessage(`Tìm thấy ${visibleCount} kết quả`);
}

// Show detail modal
function showDetailModal(id) {
    fetch(`/nhan-vien/api/owner-detail/${id}`, {
        method: "GET",
        headers: { "Content-Type": "application/json" }
    })
        .then(response => response.json())
        .then(data => {
            currentDetailId = id;

            // Populate modal with data
            document.getElementById("detailName").textContent = data.user.fullname || "-";
            document.getElementById("detailBirthDate").textContent = data.user.birthday || "-";
            document.getElementById("detailGender").textContent = data.user.gender ? "Nam" : "Nữ";
            document.getElementById("detailCccd").textContent = data.cccd?.cccdNumber || "-";
            document.getElementById("detailCccdDate").textContent = data.cccd?.issueDate || "-";
            document.getElementById("detailCccdPlace").textContent = data.cccd?.issuePlace || "-";
            document.getElementById("detailPhone").textContent = data.user.phone || "-";
            document.getElementById("detailEmail").textContent = data.user.email || "-";
            document.getElementById("detailAddress").textContent = data.user.address || "-";

            // Populate document images
            const documentImages = document.getElementById("documentImages");
            documentImages.innerHTML = "";
            if (data.cccd?.frontImageUrl) {
                documentImages.innerHTML += `
                <div class="document-image-item-staff" onclick="showImageModal('${data.cccd.frontImageUrl}')">
                    <img src="${data.cccd.frontImageUrl}" alt="CCCD mặt trước">
                    <div class="document-image-overlay-staff">
                        <i class="fas fa-search-plus"></i>
                    </div>
                    <div class="document-image-label-staff">CCCD mặt trước</div>
                </div>
            `;
            }
            if (data.cccd?.backImageUrl) {
                documentImages.innerHTML += `
                <div class="document-image-item-staff" onclick="showImageModal('${data.cccd.backImageUrl}')">
                    <img src="${data.cccd.backImageUrl}" alt="CCCD mặt sau">
                    <div class="document-image-overlay-staff">
                        <i class="fas fa-search-plus"></i>
                    </div>
                    <div class="document-image-label-staff">CCCD mặt sau</div>
                </div>
            `;
            }

            // Show modal
            const modal = document.getElementById("detailModal");
            if (modal) {
                new bootstrap.Modal(modal).show();
            }
        })
        .catch(error => {
            console.error("Error loading owner details:", error);
            showErrorMessage("Không thể tải chi tiết chủ trọ.");
        });
}

// Show image modal
function showImageModal(imageUrl) {
    const modalImage = document.getElementById("modalImage");
    modalImage.src = imageUrl;

    const modal = document.getElementById("imageModal");
    if (modal) {
        new bootstrap.Modal(modal).show();
    }
}

// Show confirmation modal
function showConfirmationModal(id, action) {
    fetch(`/nhan-vien/chi-tiet-chu-tro/${id}`)
        .then(response => response.json())
        .then(data => {
            const modal = document.getElementById("confirmationModal");
            if (modal) {
                const modalInstance = new bootstrap.Modal(modal);
                const modalTitle = document.getElementById("modalTitle");
                const modalMessage = document.getElementById("modalMessage");
                const confirmBtn = document.getElementById("confirmBtn");

                pendingAction = { id, action };

                if (action === "approve") {
                    modalTitle.textContent = "Xác nhận phê duyệt";
                    modalMessage.textContent = `Bạn có chắc chắn muốn phê duyệt đăng ký của "${data.user.fullname}"?`;
                    confirmBtn.textContent = "Phê duyệt";
                    confirmBtn.className = "btn btn-success";
                    confirmBtn.onclick = confirmApproval;
                } else {
                    modalTitle.textContent = "Xác nhận từ chối";
                    modalMessage.textContent = `Bạn có chắc chắn muốn từ chối đăng ký của "${data.user.fullname}"?`;
                    confirmBtn.textContent = "Từ chối";
                    confirmBtn.className = "btn btn-danger";
                    confirmBtn.onclick = confirmRejection;
                }

                modalInstance.show();
            }
        })
        .catch(error => {
            console.error("Error loading owner name for confirmation:", error);
            showErrorMessage("Không thể tải thông tin để xác nhận.");
        });
}

// Confirm approval
function confirmApproval() {
    if (!pendingAction || pendingAction.action !== "approve") return;

    const id = pendingAction.id;

    fetch(`/nhan-vien/api/approve-owner/${id}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" }
    })
        .then(response => {
            if (response.ok) {
                return response.text();
            }
            return response.text().then(text => { throw new Error(text || "Lỗi không xác định."); });
        })
        .then(message => {
            showSuccessMessage(message);
            const card = document.querySelector(`[data-id="${id}"]`);
            if (card) {
                card.style.display = "none";
            }
            const detailModal = document.getElementById("detailModal");
            if (detailModal) {
                bootstrap.Modal.getInstance(detailModal).hide();
            }
            const modal = document.getElementById("confirmationModal");
            if (modal) {
                bootstrap.Modal.getInstance(modal).hide();
            }
        })
        .catch(error => {
            console.error("Error approving owner:", error);
            showErrorMessage(error.message);
        })
        .finally(() => {
            pendingAction = null;
            currentDetailId = null;
        });
}

// Confirm rejection
function confirmRejection() {
    if (!pendingAction || pendingAction.action !== "reject") return;

    const id = pendingAction.id;

    fetch(`/nhan-vien/api/reject-owner/${id}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" }
    })
        .then(response => {
            if (response.ok) {
                return response.text();
            }
            return response.text().then(text => { throw new Error(text || "Lỗi không xác định."); });
        })
        .then(message => {
            showErrorMessage(message);
            const card = document.querySelector(`[data-id="${id}"]`);
            if (card) {
                card.style.display = "none";
            }
            const detailModal = document.getElementById("detailModal");
            if (detailModal) {
                bootstrap.Modal.getInstance(detailModal).hide();
            }
            const modal = document.getElementById("confirmationModal");
            if (modal) {
                bootstrap.Modal.getInstance(modal).hide();
            }
        })
        .catch(error => {
            console.error("Error rejecting owner:", error);
            showErrorMessage(error.message);
        })
        .finally(() => {
            pendingAction = null;
            currentDetailId = null;
        });
}

// Show success message
function showSuccessMessage(message) {
    const successMsg = document.createElement("div");
    successMsg.className = "success-message-staff";
    successMsg.innerHTML = `<i class="fas fa-check"></i> ${message}`;
    document.body.appendChild(successMsg);

    setTimeout(() => {
        successMsg.remove();
    }, 3000);
}

// Show error message
function showErrorMessage(message) {
    const errorMsg = document.createElement("div");
    errorMsg.className = "error-message-staff";
    errorMsg.innerHTML = `<i class="fas fa-times"></i> ${message}`;
    document.body.appendChild(errorMsg);

    setTimeout(() => {
        errorMsg.remove();
    }, 3000);
}