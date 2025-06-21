// Namespace để tránh xung đột với các file JS khác trong Spring Boot
const RoomManagementUI = (function() {
    'use strict';
    
    let isInitialized = false;
    
    // Initialize basic UI functionality
    function initializeUI() {
        if (isInitialized) return;
        
        console.log("Room management UI initialized");
        
        // Kích hoạt chuyển tab khi nhấn nút Chỉnh sửa
        document.addEventListener('click', function(e) {
            if (e.target.closest('.btn-edit-host')) {
                e.preventDefault();
                const addRoomTab = document.getElementById("add-room-tab-host");
                if (addRoomTab) {
                    addRoomTab.click();
                }
            }
        });
        
        isInitialized = true;
    }

    // Initialize tabs functionality
    function initializeTabs() {
        const tabLinks = document.querySelectorAll(".tab-link-host");
        const tabPanes = document.querySelectorAll(".tab-pane");

        tabLinks.forEach((link) => {
            link.addEventListener("click", function (e) {
                e.preventDefault();

                // Remove active class from all tabs and panes
                tabLinks.forEach((l) => l.classList.remove("active"));
                tabPanes.forEach((p) => {
                    p.classList.remove("show", "active");
                });

                // Add active class to clicked tab
                this.classList.add("active");

                // Show corresponding pane
                const targetId = this.getAttribute("data-bs-target");
                const targetPane = document.querySelector(targetId);
                if (targetPane) {
                    targetPane.classList.add("show", "active");

                    // Add animation
                    targetPane.style.opacity = "0";
                    targetPane.style.transform = "translateY(20px)";

                    setTimeout(() => {
                        targetPane.style.transition = "all 0.3s ease";
                        targetPane.style.opacity = "1";
                        targetPane.style.transform = "translateY(0)";
                    }, 50);
                }

                console.log("Tab switched to:", targetId);
            });
        });
    }

    // Basic search functionality (chỉ ẩn/hiện các row có sẵn trong HTML)
    function initializeSearch() {
        const searchInput = document.getElementById("searchRoomInputHost");
        const statusFilter = document.getElementById("statusFilterHost");
        const searchBtn = document.querySelector(".search-btn-host");

        function performSearch() {
            const searchTerm = searchInput ? searchInput.value.toLowerCase().trim() : '';
            const statusFilterValue = statusFilter ? statusFilter.value : '';
            
            const tableRows = document.querySelectorAll("#roomTableBodyHost .table-row-host");
            
            tableRows.forEach(row => {
                const roomName = row.querySelector('.room-name-host')?.textContent.toLowerCase() || '';
                const price = row.querySelector('.price-host')?.textContent || '';
                const statusBadge = row.querySelector('.status-badge-host');
                const roomStatus = statusBadge ? statusBadge.className.includes('status-available') ? 'available' : 
                                                statusBadge.className.includes('status-occupied') ? 'occupied' : 
                                                statusBadge.className.includes('status-maintenance') ? 'maintenance' : '' : '';

                const matchesSearch = !searchTerm || 
                    roomName.includes(searchTerm) || 
                    price.includes(searchTerm);

                const matchesStatus = !statusFilterValue || roomStatus === statusFilterValue;

                if (matchesSearch && matchesStatus) {
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });

            console.log("Search performed:", { searchTerm, statusFilter: statusFilterValue });
        }

        // Event listeners
        if (searchInput) {
            searchInput.addEventListener("input", debounce(performSearch, 300));
            searchInput.addEventListener("keypress", (e) => {
                if (e.key === "Enter") {
                    e.preventDefault();
                    performSearch();
                }
            });
        }

        if (statusFilter) {
            statusFilter.addEventListener("change", performSearch);
        }

        if (searchBtn) {
            searchBtn.addEventListener("click", performSearch);
        }
    }

    // Form validation (không xử lý submit, chỉ validation)
    function initializeFormValidation() {
        const form = document.getElementById("addRoomFormHost");

        if (form) {
            // Real-time validation
            const inputs = form.querySelectorAll("input[required], select[required]");
            inputs.forEach((input) => {
                input.addEventListener("blur", () => {
                    validateField(input);
                });
            });

            // Reset button
            const resetBtn = form.querySelector('button[type="reset"]');
            if (resetBtn) {
                resetBtn.addEventListener("click", () => {
                    setTimeout(() => {
                        showNotification("Form đã được đặt lại", "info");
                        clearAllFieldErrors();
                    }, 100);
                });
            }
        }
    }

    function validateField(field) {
        const value = field.value.trim();
        const fieldName = field.previousElementSibling?.textContent?.replace(" *", "") || "Trường này";

        if (!value) {
            showFieldError(field, `${fieldName} không được để trống`);
            return false;
        }

        // Specific validations
        if (field.id === "roomPriceHost") {
            const price = value.replace(/[^\d]/g, "");
            if (!price || parseInt(price) <= 0) {
                showFieldError(field, "Giá thuê phải là số dương");
                return false;
            }
        }

        if (field.id === "roomAreaHost") {
            const area = value.replace(/[^\d]/g, "");
            if (!area || parseInt(area) <= 0) {
                showFieldError(field, "Diện tích phải là số dương");
                return false;
            }
        }

        if (field.id === "roomCapacityHost") {
            const capacity = value.replace(/[^\d]/g, "");
            if (!capacity || parseInt(capacity) <= 0) {
                showFieldError(field, "Số người tối đa phải là số dương");
                return false;
            }
        }

        clearFieldError(field);
        return true;
    }

    function showFieldError(field, message) {
        clearFieldError(field);

        field.style.borderColor = "#ef4444";
        field.style.boxShadow = "0 0 0 3px rgba(239, 68, 68, 0.1)";

        const errorDiv = document.createElement("div");
        errorDiv.className = "field-error";
        errorDiv.style.cssText = `
            color: #ef4444;
            font-size: 12px;
            margin-top: 4px;
            font-family: 'Inter', 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
        `;
        errorDiv.textContent = message;

        field.parentNode.appendChild(errorDiv);
    }

    function clearFieldError(field) {
        field.style.borderColor = "";
        field.style.boxShadow = "";

        const errorDiv = field.parentNode.querySelector(".field-error");
        if (errorDiv) {
            errorDiv.remove();
        }
    }

    function clearAllFieldErrors() {
        const form = document.getElementById("addRoomFormHost");
        if (form) {
            const errorDivs = form.querySelectorAll(".field-error");
            errorDivs.forEach(div => div.remove());

            const fields = form.querySelectorAll("input, select, textarea");
            fields.forEach(field => {
                field.style.borderColor = "";
                field.style.boxShadow = "";
            });
        }
    }

    // Utility functions
    function switchToTab(tabId) {
        const tab = document.getElementById(tabId);
        if (tab) {
            tab.click();
        }
    }

    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    function showNotification(message, type = "info") {
        const notification = document.createElement("div");
        notification.className = `alert alert-${type === "error" ? "danger" : type} notification-host`;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 9999;
            min-width: 300px;
            opacity: 0;
            transform: translateX(100%);
            transition: all 0.3s ease;
        `;
        notification.innerHTML = `
            <div class="d-flex align-items-center">
                <i class="fas fa-${type === "success" ? "check-circle" : type === "error" ? "exclamation-circle" : type === "warning" ? "exclamation-triangle" : "info-circle"} me-2"></i>
                <span>${message}</span>
                <button type="button" class="btn-close ms-auto" onclick="this.parentElement.parentElement.remove()"></button>
            </div>
        `;

        document.body.appendChild(notification);

        // Animate in
        setTimeout(() => {
            notification.style.opacity = "1";
            notification.style.transform = "translateX(0)";
        }, 100);

        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentElement) {
                notification.style.opacity = "0";
                notification.style.transform = "translateX(100%)";
                setTimeout(() => {
                    if (notification.parentElement) {
                        notification.remove();
                    }
                }, 300);
            }
        }, 5000);
    }

    // Public API
    return {
        init: function() {
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', function() {
                    initializeUI();
                    initializeTabs();
                    initializeSearch();
                    initializeFormValidation();
                });
            } else {
                initializeUI();
                initializeTabs();
                initializeSearch();
                initializeFormValidation();
            }
        },
        
        switchToTab: switchToTab,
        showNotification: showNotification
    };
})();

// Image Upload Functionality - Độc lập và đóng gói
const ImageUploader = (function() {
    'use strict';
    
    function ImageUploaderClass() {
        this.uploadArea = document.getElementById("uploadArea");
        this.fileInput = document.getElementById("imageUpload");
        this.previewContainer = document.getElementById("imagePreviewContainer");
        this.selectedFiles = [];
        this.maxFiles = 10;
        this.maxFileSize = 5 * 1024 * 1024; // 5MB
        this.allowedTypes = ["image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"];

        if (this.uploadArea && this.fileInput && this.previewContainer) {
            this.init();
        }
    }

    ImageUploaderClass.prototype.init = function() {
        const self = this;
        
        // Click to upload
        this.uploadArea.addEventListener("click", function() {
            self.fileInput.click();
        });

        // File input change
        this.fileInput.addEventListener("change", function(e) {
            self.handleFiles(e.target.files);
        });

        // Drag and drop events
        this.uploadArea.addEventListener("dragover", function(e) {
            e.preventDefault();
            self.uploadArea.classList.add("drag-over");
        });

        this.uploadArea.addEventListener("dragleave", function(e) {
            e.preventDefault();
            self.uploadArea.classList.remove("drag-over");
        });

        this.uploadArea.addEventListener("drop", function(e) {
            e.preventDefault();
            self.uploadArea.classList.remove("drag-over");
            self.handleFiles(e.dataTransfer.files);
        });

        // Prevent default drag behaviors
        ["dragenter", "dragover", "dragleave", "drop"].forEach(function(eventName) {
            self.uploadArea.addEventListener(eventName, function(e) {
                e.preventDefault();
                e.stopPropagation();
            });
        });
    };

    ImageUploaderClass.prototype.handleFiles = function(files) {
        const fileArray = Array.from(files);

        if (this.selectedFiles.length + fileArray.length > this.maxFiles) {
            this.showNotification(`Chỉ có thể chọn tối đa ${this.maxFiles} ảnh`, "error");
            return;
        }

        const self = this;
        fileArray.forEach(function(file) {
            if (self.validateFile(file)) {
                self.addFile(file);
            }
        });
    };

    ImageUploaderClass.prototype.validateFile = function(file) {
        if (!this.allowedTypes.includes(file.type)) {
            this.showNotification(`File ${file.name} không đúng định dạng. Chỉ chấp nhận: JPG, PNG, GIF, WEBP`, "error");
            return false;
        }

        if (file.size > this.maxFileSize) {
            this.showNotification(`File ${file.name} quá lớn. Kích thước tối đa: 5MB`, "error");
            return false;
        }

        if (this.selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
            this.showNotification(`File ${file.name} đã được chọn`, "warning");
            return false;
        }

        return true;
    };

    ImageUploaderClass.prototype.addFile = function(file) {
        const fileId = Date.now() + Math.random();
        const fileObj = {
            id: fileId,
            file: file,
            name: file.name,
            size: file.size,
        };

        this.selectedFiles.push(fileObj);
        this.createPreview(fileObj);
        this.updateUploadArea();
    };

    ImageUploaderClass.prototype.createPreview = function(fileObj) {
        const reader = new FileReader();
        const self = this;

        reader.onload = function(e) {
            const previewItem = document.createElement("div");
            previewItem.className = "image-preview-item";
            previewItem.dataset.fileId = fileObj.id;

            previewItem.innerHTML = `
                <img src="${e.target.result}" alt="${fileObj.name}" class="preview-image" title="${fileObj.name}">
                <button type="button" class="remove-image-btn" title="Xóa ảnh" onclick="window.imageUploader.removeFile(${fileObj.id})">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M18 6L6 18M6 6L18 18" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </button>
            `;

            self.previewContainer.appendChild(previewItem);

            setTimeout(function() {
                previewItem.style.opacity = "1";
                previewItem.style.transform = "scale(1)";
            }, 10);
        };

        reader.readAsDataURL(fileObj.file);
    };

    ImageUploaderClass.prototype.removeFile = function(fileId) {
        this.selectedFiles = this.selectedFiles.filter(f => f.id !== fileId);

        const previewItem = document.querySelector(`[data-file-id="${fileId}"]`);
        if (previewItem) {
            previewItem.style.opacity = "0";
            previewItem.style.transform = "scale(0.8)";
            setTimeout(function() {
                previewItem.remove();
            }, 300);
        }

        this.updateUploadArea();
        this.showNotification("Đã xóa ảnh", "success");
    };

    ImageUploaderClass.prototype.updateUploadArea = function() {
        const uploadText = this.uploadArea.querySelector(".upload-text");

        if (this.selectedFiles.length === 0) {
            uploadText.textContent = "Kéo thả ảnh vào đây hoặc click để chọn";
            this.uploadArea.style.opacity = "1";
        } else if (this.selectedFiles.length >= this.maxFiles) {
            uploadText.textContent = `Đã chọn ${this.selectedFiles.length}/${this.maxFiles} ảnh (tối đa)`;
            this.uploadArea.style.opacity = "0.6";
        } else {
            uploadText.textContent = `Đã chọn ${this.selectedFiles.length}/${this.maxFiles} ảnh - Click để chọn thêm`;
            this.uploadArea.style.opacity = "0.8";
        }
    };

    ImageUploaderClass.prototype.getSelectedFiles = function() {
        return this.selectedFiles.map(f => f.file);
    };

    ImageUploaderClass.prototype.clearAll = function() {
        this.selectedFiles = [];
        this.previewContainer.innerHTML = "";
        this.updateUploadArea();
        this.fileInput.value = "";
    };

    ImageUploaderClass.prototype.showNotification = function(message, type) {
        const notification = document.createElement("div");
        notification.className = `notification notification-${type}`;

        const colors = {
            success: "#10b981",
            error: "#ef4444",
            warning: "#f59e0b",
            info: "#3e83cc",
        };

        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: ${colors[type]};
            color: white;
            padding: 12px 20px;
            border-radius: 8px;
            box-shadow: 0 4px 20px rgba(0,0,0,0.15);
            z-index: 10000;
            font-weight: 500;
            font-size: 14px;
            max-width: 300px;
            animation: slideInRight 0.3s ease;
            font-family: 'Inter', 'Segoe UI', 'Roboto', 'Helvetica Neue', Arial, sans-serif;
        `;

        notification.textContent = message;

        if (!document.getElementById("notificationStyles")) {
            const style = document.createElement("style");
            style.id = "notificationStyles";
            style.textContent = `
                @keyframes slideInRight {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
                @keyframes slideOutRight {
                    from { transform: translateX(0); opacity: 1; }
                    to { transform: translateX(100%); opacity: 0; }
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(notification);

        setTimeout(function() {
            notification.style.animation = "slideOutRight 0.3s ease";
            setTimeout(function() {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    };

    return ImageUploaderClass;
})();

// Initialize everything when DOM is ready
(function() {
    'use strict';
    
    function initializeAll() {
        // Initialize UI functionality
        RoomManagementUI.init();
        
        // Initialize image uploader only if elements exist
        if (document.getElementById("uploadArea")) {
            window.imageUploader = new ImageUploader();
        }
        
        // Price formatting for input
        const priceInput = document.getElementById("roomPriceHost");
        if (priceInput) {
            priceInput.addEventListener("input", function(e) {
                let value = e.target.value.replace(/[^\d]/g, "");
                if (value) {
                    value = parseInt(value).toLocaleString("vi-VN");
                    e.target.value = value;
                }
            });
        }
        
        // Handle window resize
        window.addEventListener("resize", debounce(function() {
            const tableContainer = document.querySelector(".table-container-host");
            if (tableContainer && window.innerWidth < 768) {
                tableContainer.style.overflowX = "auto";
            }
        }, 250));
        
        // Initialize tooltips if Bootstrap is available
        if (typeof bootstrap !== "undefined") {
            const tooltipTriggerList = [].slice.call(document.querySelectorAll("[title]"));
            tooltipTriggerList.map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));
        }
        
        console.log("Room management UI initialized successfully");
    }
    
    function debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initializeAll);
    } else {
        initializeAll();
    }
})();

// Expose necessary functions to global scope
window.RoomManagementUI = RoomManagementUI;

document.addEventListener('DOMContentLoaded', function () {
        const searchInput = document.getElementById('searchInputHost');
        const searchButton = document.getElementById('searchButtonHost');
        const tableBody = document.getElementById('tableBodyHost');

        // Xử lý sự kiện khi nhấn nút tìm kiếm
        searchButton.addEventListener('click', function () {
            const searchTerm = searchInput.value.trim();
            searchHostels(searchTerm);
        });

        // Xử lý sự kiện khi nhấn Enter trong ô tìm kiếm
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                const searchTerm = searchInput.value.trim();
                searchHostels(searchTerm);
            }
        });

        // Hàm gửi yêu cầu tìm kiếm
        function searchHostels(name) {
            fetch(`/chu-tro/search-hostels?name=${encodeURIComponent(name)}`, {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
            .then(response => response.json())
            .then(data => {
                updateTable(data);
            })
            .catch(error => {
                console.error('Error:', error);
                tableBody.innerHTML = '<tr><td colspan="4">Đã có lỗi xảy ra. Vui lòng thử lại.</td></tr>';
            });
        }

        function updateTable(hostels) {
            tableBody.innerHTML = ''; 
            if (hostels.length === 0) {
                tableBody.innerHTML = '<tr><td colspan="4">Không tìm thấy khu trọ nào.</td></tr>';
                return;
            }

            hostels.forEach(hostel => {
                const row = document.createElement('tr');
                row.className = 'table-row-host';
                row.innerHTML = `
                    <td class="table-td-host">${hostel.hostelId}</td>
                    <td class="table-td-host">${hostel.name}</td>
                    <td class="table-td-host">
                        <span class="${hostel.status ? 'status-badge-host status-active-host' : 'status-badge-host status-inactive-host'}">
                            ${hostel.status ? 'Đang hoạt động' : 'Ngưng hoạt động'}
                        </span>
                    </td>
                    <td class="table-td-host">
                        <div class="action-btns-host">
                            <a href="/chu-tro/them-khu-tro?id=${hostel.hostelId}">
                                <button class="buttons-edit-host" title="Chỉnh sửa">
                                    <i class="fas fa-edit"></i>
                                </button>
                            </a>
                            <form action="/chu-tro/delete-khu-tro/${hostel.hostelId}" method="post" style="display:inline;">
                                <button type="submit" class="btn-delete-host" title="Xóa" onclick="return confirm('Bạn có chắc muốn xóa khu trọ này?');">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </form>
                        </div>
                    </td>
                `;
                tableBody.appendChild(row);
            });
        }
    });
    searchInput.addEventListener('input', function () {
    const searchTerm = searchInput.value.trim();
    if (searchTerm.length >= 2) {
        searchHostels(searchTerm);
    } else {
        searchHostels(''); 
    }
});