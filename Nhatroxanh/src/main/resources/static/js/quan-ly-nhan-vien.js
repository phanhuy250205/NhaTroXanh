// Initialize page
document.addEventListener('DOMContentLoaded', function () {
    initializeEventListeners();
});

// Initialize event listeners
function initializeEventListeners() {
    // Search functionality
    const searchInput = document.querySelector('.search-box input');
    searchInput.addEventListener('input', function (e) {
        filterEmployees(e.target.value);
    });

    // Filter functionality
    const filterOptions = document.querySelectorAll('.dropdown-item');
    filterOptions.forEach(option => {
        option.addEventListener('click', function (e) {
            e.preventDefault();
            const status = this.textContent.trim();
            filterByStatus(status);
        });
    });

    // Action buttons
    document.addEventListener('click', function (e) {
        if (e.target.closest('.action-btn')) {
            handleActionClick(e);
        }
    });

    // Form validation
    const form = document.getElementById('createEmployeeForm');
    form.addEventListener('submit', function (e) {
        e.preventDefault();
        createEmployee();
    });
}

// Filter employees by status
function filterByStatus(status) {
    const rows = document.querySelectorAll('.custom-table tbody tr');
    const filterText = document.querySelector('.filter-text');

    // Update button text if filterText exists
    if (filterText) {
        filterText.textContent = status === 'Tất cả' ? 'Lọc' : `Lọc: ${status}`;
    }

    rows.forEach(row => {
        const statusBadge = row.querySelector('.status-badge');
        const currentStatus = statusBadge.textContent.trim();

        if (status === 'Tất cả' || currentStatus === status) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

// Filter employees based on search
function filterEmployees(searchTerm) {
    const rows = document.querySelectorAll('.custom-table tbody tr');
    searchTerm = searchTerm.toLowerCase();

    rows.forEach(row => {
        const employeeName = row.querySelector('.employee-details h6').textContent.toLowerCase();
        const employeeEmail = row.querySelector('.employee-details span').textContent.toLowerCase();

        if (employeeName.includes(searchTerm) ||
            employeeEmail.includes(searchTerm)) {
            row.style.display = '';
        } else {
            row.style.display = 'none';
        }
    });
}

// Handle action button clicks
function handleActionClick(e) {
    const button = e.target.closest('.action-btn');
    const row = button.closest('tr');
    const employeeName = row.querySelector('.employee-details h6').textContent;

    if (button.querySelector('.fa-edit')) {
        editEmployee(row);
    } else if (button.querySelector('.fa-eye')) {
        viewEmployee(row);
    } else if (button.querySelector('.fa-trash')) {
        deleteEmployee(row, employeeName);
    }
}

// Edit employee
function editEmployee(row) {
    const employeeData = extractEmployeeData(row);
    populateForm(employeeData);
    document.getElementById('createEmployeeModalLabel').innerHTML = '<i class="fas fa-user-edit me-2"></i>Chỉnh sửa thông tin nhân viên';
    document.querySelector('.modal-footer .btn-primary-custom').innerHTML = '<i class="fas fa-save me-1"></i>Cập nhật';
    const modal = new bootstrap.Modal(document.getElementById('createEmployeeModal'));
    modal.show();
}

// View employee details
function viewEmployee(row) {
    const data = extractEmployeeData(row);
    document.getElementById('viewName').textContent = data.name;
    document.getElementById('viewEmail').textContent = data.email;
    document.getElementById('viewSalary').textContent = data.salary;
    
    // Set phone number if element exists
    const phoneElement = document.getElementById('viewPhone');
    if (phoneElement) {
        phoneElement.textContent = '0987654321'; // Default phone since we don't store it in table
    }
    
    // Set ID number if element exists  
    const idElement = document.getElementById('viewId');
    if (idElement) {
        idElement.textContent = '0123456789'; // Default ID since we don't store it in table
    }
    
    // Set address if element exists
    const addressElement = document.getElementById('viewAddress');
    if (addressElement) {
        addressElement.textContent = '123 Nguyễn Văn Linh, Quận 1, TP.HCM'; // Default address
    }
    
    const statusBadge = document.getElementById('viewStatus');
    statusBadge.textContent = data.status;
    statusBadge.className = `badge rounded-pill ${data.status === 'Hoạt động' ? 'bg-success' :
        data.status === 'Chờ duyệt' ? 'bg-warning' : 'bg-danger'}`;
    document.getElementById('viewAvatar').src = row.querySelector('.employee-avatar').src;
    const modal = new bootstrap.Modal(document.getElementById('viewEmployeeModal'));
    modal.show();
}

// Delete employee
function deleteEmployee(row, employeeName) {
    if (confirm(`Bạn có chắc chắn muốn xóa nhân viên "${employeeName}"?\n\nHành động này không thể hoàn tác.`)) {
        row.style.transition = 'all 0.3s ease';
        row.style.opacity = '0';
        row.style.transform = 'translateX(100px)';
        setTimeout(() => {
            row.remove();
            updateStats();
            showNotification('Đã xóa nhân viên thành công!', 'success');
        }, 300);
    }
}

// Extract employee data from table row
function extractEmployeeData(row) {
    return {
        name: row.querySelector('.employee-details h6').textContent,
        email: row.querySelector('.employee-details span').textContent,
        status: row.children[1].querySelector('.status-badge').textContent,
        salary: row.children[2].textContent
    };
}

// Populate form with employee data
function populateForm(data) {
    const [firstName, ...lastNameParts] = data.name.split(' ');
    document.getElementById('firstName').value = firstName;
    document.getElementById('lastName').value = lastNameParts.join(' ');
    document.getElementById('email').value = data.email;
}

// Create new employee
function createEmployee() {
    const formData = getFormData();
    if (validateFormData(formData)) {
        setTimeout(() => {
            addEmployeeToTable(formData);
            updateStats();
            resetForm();
            hideModal();
            showNotification('Tạo tài khoản nhân viên thành công!', 'success');
        }, 1000);
        showLoadingState();
    }
}

// Get form data
function getFormData() {
    return {
        firstName: document.getElementById('firstName').value,
        lastName: document.getElementById('lastName').value,
        email: document.getElementById('email').value,
        phone: document.getElementById('phone').value,
        salary: document.getElementById('salary').value,
        idNumber: document.getElementById('idNumber').value,
        status: document.getElementById('status').value
    };
}

// Validate form data
function validateFormData(data) {
    if (!data.firstName || !data.lastName || !data.email || !data.phone || !data.salary) {
        showNotification('Vui lòng điền đầy đủ thông tin bắt buộc!', 'error');
        return false;
    }
    if (!isValidEmail(data.email)) {
        showNotification('Email không hợp lệ!', 'error');
        return false;
    }
    if (!isValidPhone(data.phone)) {
        showNotification('Số điện thoại không hợp lệ!', 'error');
        return false;
    }
    return true;
}

// Email validation
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Phone validation
function isValidPhone(phone) {
    const phoneRegex = /^[0-9]{10,11}$/;
    return phoneRegex.test(phone.replace(/\s+/g, ''));
}

// Add employee to table
function addEmployeeToTable(data) {
    const tbody = document.querySelector('.custom-table tbody');
    const newRow = createEmployeeRow(data);
    newRow.style.opacity = '0';
    newRow.style.transform = 'translateY(-20px)';
    tbody.insertBefore(newRow, tbody.firstChild);
    setTimeout(() => {
        newRow.style.transition = 'all 0.3s ease';
        newRow.style.opacity = '1';
        newRow.style.transform = 'translateY(0)';
    }, 100);
}

// Create employee table row
function createEmployeeRow(data) {
    const row = document.createElement('tr');
    const fullName = `${data.firstName} ${data.lastName}`;
    const statusClass = getStatusClass(data.status);
    const statusText = getStatusText(data.status);
    const salary = formatCurrency(data.salary);

    row.innerHTML = `
        <td>
            <div class="employee-info">
                <img src="https://storage.googleapis.com/workspace-0f70711f-8b4e-4d94-86f1-2a93ccde5887/image/75dbf6df-63ca-4494-9a63-2eb0f76bed85.png" alt="Professional headshot of ${fullName}, new employee with friendly demeanor wearing business attire" class="employee-avatar">
                <div class="employee-details">
                    <h6>${fullName}</h6>
                    <span>${data.email}</span>
                </div>
            </div>
        </td>
        <td><span class="status-badge ${statusClass}">${statusText}</span></td>
        <td>${salary}</td>
        <td>
            <button class="action-btn" title="Chỉnh sửa">
                <i class="fas fa-edit"></i>
            </button>
            <button class="action-btn" title="Xem chi tiết">
                <i class="fas fa-eye"></i>
            </button>
            <button class="action-btn danger" title="Xóa">
                <i class="fas fa-trash"></i>
            </button>
        </td>
    `;
    return row;
}

// Helper functions for formatting
function getStatusClass(status) {
    const statusMap = {
        'active': 'status-active',
        'pending': 'status-pending',
        'inactive': 'status-inactive'
    };
    return statusMap[status] || 'status-pending';
}

function getStatusText(status) {
    const statusMap = {
        'active': 'Hoạt động',
        'pending': 'Chờ duyệt',
        'inactive': 'Ngưng hoạt động'
    };
    return statusMap[status] || 'Chờ duyệt';
}

function formatCurrency(amount) {
    return new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND'
    }).format(amount);
}

// Update statistics
function updateStats() {
    const rows = document.querySelectorAll('.custom-table tbody tr');
    const stats = {
        total: rows.length,
        active: 0,
        pending: 0,
        inactive: 0
    };

    rows.forEach(row => {
        const statusBadge = row.querySelector('.status-badge');
        if (statusBadge.classList.contains('status-active')) {
            stats.active++;
        } else if (statusBadge.classList.contains('status-pending')) {
            stats.pending++;
        } else if (statusBadge.classList.contains('status-inactive')) {
            stats.inactive++;
        }
    });

    updateStatCard(0, stats.total);
    updateStatCard(1, stats.active);
    updateStatCard(2, stats.pending);
    updateStatCard(3, stats.inactive);
}

function updateStatCard(index, newValue) {
    const statCards = document.querySelectorAll('.stat-card');
    const numberElement = statCards[index].querySelector('.stat-number');
    const currentValue = parseInt(numberElement.textContent);

    if (currentValue !== newValue) {
        animateNumber(numberElement, currentValue, newValue, 500);
    }
}

function animateNumber(element, start, end, duration) {
    const startTime = performance.now();
    const change = end - start;

    function update(currentTime) {
        const elapsedTime = currentTime - startTime;
        const progress = Math.min(elapsedTime / duration, 1);
        const easeOut = 1 - Math.pow(1 - progress, 3);
        element.textContent = Math.round(start + change * easeOut);
        if (progress < 1) {
            requestAnimationFrame(update);
        }
    }

    requestAnimationFrame(update);
}

// UI State Management
function showLoadingState() {
    const button = document.querySelector('.modal-footer .btn-primary-custom');
    const originalText = button.innerHTML;
    button.innerHTML = '<i class="fas fa-spinner fa-spin me-1"></i>Đang xử lý...';
    button.disabled = true;
    setTimeout(() => {
        button.innerHTML = originalText;
        button.disabled = false;
    }, 1000);
}

function resetForm() {
    document.getElementById('createEmployeeForm').reset();
    document.getElementById('createEmployeeModalLabel').innerHTML = '<i class="fas fa-user-plus me-2"></i>Tạo tài khoản nhân viên mới';
    document.querySelector('.modal-footer .btn-primary-custom').innerHTML = '<i class="fas fa-save me-1"></i>Tạo tài khoản';
}

function hideModal() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('createEmployeeModal'));
    modal.hide();
}

function showNotification(message, type = 'info') {
    const notification = document.createElement('div');
    notification.className = `alert alert-${type === 'error' ? 'danger' : type} alert-dismissible fade show position-fixed`;
    notification.style.cssText = `
        top: 20px;
        right: 20px;
        z-index: 9999;
        min-width: 300px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.1);
    `;
    const icon = type === 'success' ? 'fas fa-check-circle' :
        type === 'error' ? 'fas fa-exclamation-circle' :
            'fas fa-info-circle';
    notification.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="${icon} me-2"></i>
            <span>${message}</span>
            <button type="button" class="btn-close ms-auto" data-bs-dismiss="alert"></button>
        </div>
    `;
    document.body.appendChild(notification);
    setTimeout(() => {
        if (notification.parentNode) {
            notification.remove();
        }
    }, 5000);
}