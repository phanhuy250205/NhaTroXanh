// Global variables for pagination
let currentPage = 0;
let currentSearch = '';
let currentStatus = '';
let availableContracts = []; // Store contracts globally for access

// Load specific page
function loadPage(page) {
    currentPage = page;

    // Build URL with parameters
    let url = '/api/payments/paginated?page=' + page + '&size=8';

    if (currentSearch && currentSearch.trim() !== '') {
        url += '&search=' + encodeURIComponent(currentSearch.trim());
    }

    if (currentStatus && currentStatus !== '') {
        url += '&status=' + encodeURIComponent(currentStatus);
    }

    // Make AJAX request
    fetch(url)
        .then(response => response.json())
        .then(data => {
            if (data.payments) {
                updateTable(data.payments);
                updatePagination(data);
            }
        })
        .catch(error => {
            console.error('Error loading page:', error);
            showAlert('error', 'Có lỗi xảy ra khi tải dữ liệu');
        });
}

// Update table with new data
function updateTable(payments) {
    const tbody = document.getElementById('paymentsTableBody');
    tbody.innerHTML = '';

    if (payments.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" class="text-center text-muted">
                    <i class="fas fa-info-circle me-2"></i>
                    Không có dữ liệu thanh toán
                </td>
            </tr>
        `;
        return;
    }

    payments.forEach(payment => {
        let statusClass, statusIcon, statusText;
        if (payment.paymentStatus === 'ĐÃ_THANH_TOÁN') {
            statusClass = 'status-paid';
            statusIcon = 'fas fa-check';
            statusText = 'Đã thanh toán';
        } else if (payment.paymentStatus === 'CHỜ_XÁC_NHẬN_TIỀN_MẶT') {
            statusClass = 'status-pending';
            statusIcon = 'fas fa-hourglass-half';
            statusText = 'Chờ xác nhận';
        } else {
            statusClass = 'status-unpaid';
            statusIcon = 'fas fa-exclamation-triangle';
            statusText = 'Chưa thanh toán';
        }


        const row = `
            <tr>
                <td><strong>${payment.hostelName || ''}</strong></td>
                <td>
                    <span class="badge bg-primary">${payment.roomCode || ''}</span>
                </td>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="bg-primary rounded-circle d-flex align-items-center justify-content-center me-2" style="width: 35px; height: 35px;">
                            <i class="fas fa-user text-white"></i>
                        </div>
                        <strong>${payment.tenantName || ''}</strong>
                    </div>
                </td>
                <td>
                    <i class="fas fa-calendar-alt me-1 text-muted"></i>
                    <span>${payment.month || ''}</span>
                </td>
                <td>
                    <span class="status-badge ${statusClass}">
                        <i class="${statusIcon} me-1"></i>
                        <span>${statusText}</span>
                    </span>
                </td>
                <td>
                    <a href="#" class="action-link" onclick="showInvoiceModalById(${payment.paymentId})">
                        <i class="fas fa-eye"></i>
                        Xem chi tiết
                    </a>
                </td>
            </tr>
        `;
        tbody.innerHTML += row;
    });
}

// Update pagination controls
function updatePagination(data) {
    const paginationSection = document.querySelector('.pagination-section');
    const paginationInfo = document.querySelector('.pagination-info span');

    // Update pagination info
    if (paginationInfo) {
        paginationInfo.innerHTML = `
            Hiển thị <span>${data.payments.length}</span> trong tổng số 
            <span>${data.totalElements}</span> thanh toán
        `;
    }

    // Show/hide pagination section
    if (data.totalPages <= 1) {
        if (paginationSection) {
            paginationSection.style.display = 'none';
        }
        return;
    } else {
        if (paginationSection) {
            paginationSection.style.display = 'block';
        }
    }

    // Update pagination controls
    const pagination = document.querySelector('.pagination-custom');
    if (!pagination) return;

    pagination.innerHTML = '';

    // First page button
    const firstDisabled = data.currentPage === 0 ? 'disabled' : '';
    pagination.innerHTML += `
        <li class="page-item ${firstDisabled}">
            ${data.currentPage === 0 ?
            '<span class="page-link"><i class="fas fa-angle-double-left"></i></span>' :
            '<a class="page-link" href="#" onclick="loadPage(0)"><i class="fas fa-angle-double-left"></i></a>'
        }
        </li>
    `;

    // Previous page button
    pagination.innerHTML += `
        <li class="page-item ${firstDisabled}">
            ${data.currentPage === 0 ?
            '<span class="page-link"><i class="fas fa-angle-left"></i></span>' :
            '<a class="page-link" href="#" onclick="loadPage(' + (data.currentPage - 1) + ')"><i class="fas fa-angle-left"></i></a>'
        }
        </li>
    `;

    // Page numbers
    const startPage = Math.max(0, data.currentPage - 2);
    const endPage = Math.min(data.totalPages - 1, data.currentPage + 2);

    for (let i = startPage; i <= endPage; i++) {
        const activeClass = i === data.currentPage ? 'active' : '';
        pagination.innerHTML += `
            <li class="page-item ${activeClass}">
                ${i === data.currentPage ?
                '<span class="page-link">' + (i + 1) + '</span>' :
                '<a class="page-link" href="#" onclick="loadPage(' + i + ')">' + (i + 1) + '</a>'
            }
            </li>
        `;
    }

    // Next page button
    const lastDisabled = data.currentPage >= data.totalPages - 1 ? 'disabled' : '';
    pagination.innerHTML += `
        <li class="page-item ${lastDisabled}">
            ${data.currentPage >= data.totalPages - 1 ?
            '<span class="page-link"><i class="fas fa-angle-right"></i></span>' :
            '<a class="page-link" href="#" onclick="loadPage(' + (data.currentPage + 1) + ')"><i class="fas fa-angle-right"></i></a>'
        }
        </li>
    `;

    // Last page button
    pagination.innerHTML += `
        <li class="page-item ${lastDisabled}">
            ${data.currentPage >= data.totalPages - 1 ?
            '<span class="page-link"><i class="fas fa-angle-double-right"></i></span>' :
            '<a class="page-link" href="#" onclick="loadPage(' + (data.totalPages - 1) + ')"><i class="fas fa-angle-double-right"></i></a>'
        }
        </li>
    `;
}

// Update search function to work with pagination
function performSearch() {
    currentSearch = document.getElementById('searchInput').value;
    currentStatus = document.getElementById('statusFilter').value;
    currentPage = 0; // Reset to first page
    loadPage(0);
}

// Show alert message using SweetAlert2 with custom color #3E83CC
function showAlert(type, message, options = {}) {
    const { showConfirm = false, onConfirm = null, onCancel = null } = options;

    if (showConfirm) {
        Swal.fire({
            title: '',
            text: message,
            icon: type === 'success' ? 'success' : type === 'error' ? 'error' : type === 'warning' ? 'warning' : 'info',
            background: '#1a1a1a',
            color: '#ffffff',
            showCancelButton: true,
            confirmButtonColor: '#3E83CC',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'OK',
            cancelButtonText: 'Hủy',
            customClass: {
                popup: 'animated fadeInDown'
            },
            buttonsStyling: false,
            reverseButtons: true
        }).then((result) => {
            if (result.isConfirmed && onConfirm) {
                onConfirm();
            } else if (result.dismiss === Swal.DismissReason.cancel && onCancel) {
                onCancel();
            }
        });
    } else {
        Swal.fire({
            title: '',
            text: message,
            icon: type === 'success' ? 'success' : type === 'error' ? 'error' : type === 'warning' ? 'warning' : 'info',
            background: '#1a1a1a',
            color: '#ffffff',
            confirmButtonColor: '#3E83CC',
            confirmButtonText: 'Đóng',
            customClass: {
                popup: 'animated fadeInDown'
            },
            timer: 5000,
            timerProgressBar: true,
            showConfirmButton: true,
            buttonsStyling: false
        });
    }
}

// Show invoice modal by ID
function showInvoiceModalById(paymentId) {
    fetch(`/api/payments/${paymentId}`)
        .then(response => response.json())
        .then(payment => {
            if (payment) {
                populateInvoiceModal(payment);
                const modal = new bootstrap.Modal(document.getElementById('invoiceModal'));
                modal.show();
            }
        })
        .catch(error => {
            console.error('Error loading payment details:', error);
            showAlert('error', 'Có lỗi xảy ra khi tải chi tiết hóa đơn');
        });
}

// Populate invoice modal with payment data
function populateInvoiceModal(payment) {
    document.getElementById('invoiceMonth').textContent = payment.month || '';
    document.getElementById('invoiceRoom').textContent = payment.roomCode || '';
    document.getElementById('invoiceTenant').textContent = payment.tenantName || '';

    // Populate payment details
    if (payment.details && payment.details.length > 0) {
        payment.details.forEach(detail => {
            const itemName = detail.itemName.toLowerCase();
            if (itemName.includes('phòng')) {
                document.getElementById('roomFee').textContent = formatCurrency(detail.amount);
            } else if (itemName.includes('điện')) {
                document.getElementById('electricityFee').textContent = formatCurrency(detail.amount);
                document.getElementById('electricityUsage').textContent = `(${detail.quantity || 0}kWh)`;
            } else if (itemName.includes('nước')) {
                document.getElementById('waterFee').textContent = formatCurrency(detail.amount);
                document.getElementById('waterUsage').textContent = `(${detail.quantity || 0} m³)`;
            } else if (itemName.includes('rác')) {
                document.getElementById('trashFee').textContent = formatCurrency(detail.amount);
            } else if (itemName.includes('wifi')) {
                document.getElementById('wifiFee').textContent = formatCurrency(detail.amount);
            }
        });
    }

    document.getElementById('totalAmount').textContent = formatCurrency(payment.totalAmount);
    document.getElementById('dueDate').textContent = formatDate(payment.dueDate);

    const statusElement = document.getElementById('invoiceStatus');
    if (payment.paymentStatus === 'ĐÃ_THANH_TOÁN') {
        statusElement.innerHTML = '<span class="status-badge status-paid"><i class="fas fa-check me-1"></i>Đã thanh toán</span>';
    } else if (payment.paymentStatus === 'CHỜ_XÁC_NHẬN_TIỀN_MẶT') {
        statusElement.innerHTML = '<span class="status-badge status-pending"><i class="fas fa-hourglass-half me-1"></i>Chờ xác nhận</span>';
    } else {
        statusElement.innerHTML = '<span class="status-badge status-unpaid"><i class="fas fa-exclamation-triangle me-1"></i>Chưa thanh toán</span>';
    }


    // Store payment ID for actions
    document.getElementById('invoiceModal').setAttribute('data-payment-id', payment.paymentId);
}

// Mark payment as paid (for cash payments)
function markAsPaid() {
    const modal = document.getElementById('invoiceModal');
    const paymentId = modal.getAttribute('data-payment-id');

    if (!paymentId) {
        showAlert('error', 'Không tìm thấy ID hóa đơn');
        return;
    }

    // Show confirmation dialog
    showAlert('warning', 'Bạn có chắc chắn muốn đánh dấu hóa đơn này đã thanh toán bằng tiền mặt?', {
        showConfirm: true,
        onConfirm: () => {
            // Call the cash payment confirmation endpoint
            fetch('/landlord/confirm-cash-payment', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `paymentId=${paymentId}`
            })
                .then(response => response.json())
                .then(data => {
                    if (data.success) {
                        showAlert('success', 'Đã xác nhận thanh toán tiền mặt thành công! Thông báo đã được gửi đến người thuê.');
                        // Close modal
                        const bsModal = bootstrap.Modal.getInstance(modal);
                        bsModal.hide();
                        // Reload current page
                        loadPage(currentPage);
                    } else {
                        showAlert('error', data.message || 'Có lỗi xảy ra khi xác nhận thanh toán');
                    }
                })
                .catch(error => {
                    console.error('Error confirming cash payment:', error);
                    showAlert('error', 'Có lỗi xảy ra khi xác nhận thanh toán tiền mặt');
                });
        }
    });
}

// Delete invoice
function deleteInvoice() {
    const modal = document.getElementById('invoiceModal');
    const paymentId = modal.getAttribute('data-payment-id');

    if (!paymentId) {
        showAlert('error', 'Không tìm thấy ID hóa đơn');
        return;
    }

    // Confirm deletion
    showAlert('warning', 'Bạn có chắc chắn muốn xóa hóa đơn này?', {
        showConfirm: true, onConfirm: () => {
            fetch(`/api/payments/${paymentId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                }
            })
                .then(response => {
                    if (response.ok) {
                        return response.json().catch(() => ({})); // Handle empty response
                    } else {
                        throw new Error('Failed to delete payment');
                    }
                })
                .then(data => {
                    showAlert('success', 'Đã xóa hóa đơn thành công');
                    // Close modal
                    const bsModal = bootstrap.Modal.getInstance(modal);
                    bsModal.hide();
                    // Reload current page
                    loadPage(currentPage);
                })
                .catch(error => {
                    console.error('Error deleting payment:', error);
                    showAlert('error', 'Có lỗi xảy ra khi xóa hóa đơn');
                });
        }
    });
}

// Download invoice (placeholder function)
function downloadInvoice() {
    showAlert('info', 'Tính năng tải xuống đang được phát triển');
}

// Send unpaid invoices (placeholder function)
function sendUnpaidInvoices() {
    showAlert('warning', 'Bạn có chắc muốn gửi tất cả hóa đơn chưa thanh toán và quá hạn đến người thuê?', {
        showConfirm: true, onConfirm: () => {
            const button = document.getElementById('send-invoices-btn');
            const spinner = button.querySelector('.spinner-border');
            const buttonText = button.querySelector('.d-none.d-sm-inline');
            const originalText = buttonText.textContent;

            // Disable button and show spinner
            button.disabled = true;
            spinner.classList.remove('d-none');
            buttonText.textContent = 'Đang gửi...';

            fetch('/api/payments/send-unpaid', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    // Add CSRF token if required: 'X-CSRF-TOKEN': getCsrfToken()
                }
            })
                .then(response => response.json())
                .then(data => {
                    // Re-enable button and hide spinner
                    button.disabled = false;
                    spinner.classList.add('d-none');
                    buttonText.textContent = originalText;

                    if (data.success) {
                        if (data.sentCount > 0) {
                            showAlert('success', data.message + ` (${data.sentCount} hóa đơn)`);
                        } else {
                            showAlert('warning', 'Không có hóa đơn nào được gửi. Có thể tất cả hóa đơn đã đạt giới hạn 2 lần gửi thông báo hôm nay.');
                        }
                        // Refresh notifications
                        if (typeof loadNotifications === 'function') {
                            loadNotifications();
                        }
                    } else {
                        showAlert('error', 'Lỗi: ' + data.message);
                    }
                })
                .catch(error => {
                    // Re-enable button and hide spinner
                    button.disabled = false;
                    spinner.classList.add('d-none');
                    buttonText.textContent = originalText;

                    console.error('Error sending unpaid invoices:', error);
                    showAlert('error', 'Đã xảy ra lỗi khi gửi hóa đơn: ' + error.message + '. Vui lòng kiểm tra kết nối hoặc cấu hình email.');
                });
        }
    });
}

// Check if payment already exists for the selected contract and month
function checkExistingPayment(contractId, month) {
    return fetch(`/api/payments/check-existing?contractId=${contractId}&month=${month}`)
        .then(response => response.json())
        .then(data => data.exists)
        .catch(error => {
            console.error('Error checking existing payment:', error);
            return false;
        });
}

// Create new invoice with duplicate validation
function createNewInvoice() {
    const form = document.getElementById('createInvoiceForm');

    if (!form.checkValidity()) {
        showAlert('error', 'Vui lòng điền đầy đủ thông tin bắt buộc');
        return;
    }

    const contractId = document.getElementById('invoiceRoomSelect').value;
    const monthInput = document.getElementById('invoiceMonthInput').value;

    if (!contractId || !monthInput) {
        showAlert('error', 'Vui lòng chọn phòng và tháng');
        return;
    }

    // Convert month input (YYYY-MM) to MM/YYYY format for checking
    const [year, month] = monthInput.split('-');
    const formattedMonth = `${month}/${year}`;

    // Show loading state
    const createButton = document.querySelector('button[onclick="createNewInvoice()"]');
    const originalText = createButton.innerHTML;
    createButton.disabled = true;
    createButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang kiểm tra...';

    // Check for existing payment first
    checkExistingPayment(contractId, formattedMonth)
        .then(exists => {
            if (exists) {
                // Get contract info for better error message
                const selectedContract = availableContracts.find(contract => contract.contractId == contractId);
                const roomInfo = selectedContract ? `${selectedContract.hostelName} - ${selectedContract.roomCode}` : 'phòng đã chọn';

                showAlert('warning', `Hóa đơn cho tháng ${formattedMonth} đã tồn tại cho ${roomInfo}. Không thể tạo hóa đơn trùng lặp.`);

                // Reset button
                createButton.disabled = false;
                createButton.innerHTML = originalText;
            } else {
                // Validate form data before submission
                if (validateInvoiceForm()) {
                    // Reset button text but keep it disabled during submission
                    createButton.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang tạo hóa đơn...';

                    // Submit form
                    form.submit();
                } else {
                    // Reset button if validation fails
                    createButton.disabled = false;
                    createButton.innerHTML = originalText;
                }
            }
        })
        .catch(error => {
            console.error('Error during invoice creation:', error);
            showAlert('error', 'Có lỗi xảy ra khi kiểm tra hóa đơn. Vui lòng thử lại.');

            // Reset button
            createButton.disabled = false;
            createButton.innerHTML = originalText;
        });
}

// Validate invoice form data
function validateInvoiceForm() {
    const monthInput = document.getElementById('invoiceMonthInput').value;
    const roomFee = parseFloat(document.getElementById('roomFeeInput').value) || 0;
    const wifiFee = parseFloat(document.getElementById('wifiFeeInput').value) || 0;
    const electricityPrev = parseInt(document.getElementById('electricityPrevInput').value) || 0;
    const electricityCurr = parseInt(document.getElementById('electricityCurrInput').value) || 0;
    const electricityUnitPrice = parseFloat(document.getElementById('electricityUnitPriceInput').value) || 0;
    const waterPrev = parseInt(document.getElementById('waterPrevInput').value) || 0;
    const waterCurr = parseInt(document.getElementById('waterCurrInput').value) || 0;
    const waterUnitPrice = parseFloat(document.getElementById('waterUnitPriceInput').value) || 0;
    const trashFee = parseFloat(document.getElementById('trashFeeInput').value) || 0;

    // Validate month range - landlords can only create invoices within ±2 months from current month
    if (monthInput) {
        const [year, month] = monthInput.split('-');
        const requestedMonth = parseInt(month);
        const requestedYear = parseInt(year);
        
        if (!validateInvoiceMonth(requestedMonth, requestedYear)) {
            return false;
        }
    }

    // Validate electricity readings
    if (electricityCurr < electricityPrev) {
        showAlert('error', 'Chỉ số điện mới phải lớn hơn hoặc bằng chỉ số điện cũ');
        return false;
    }

    // Validate water readings
    if (waterCurr < waterPrev) {
        showAlert('error', 'Chỉ số nước mới phải lớn hơn hoặc bằng chỉ số nước cũ');
        return false;
    }

    // Validate unit prices
    if (electricityUnitPrice <= 0) {
        showAlert('error', 'Đơn giá điện phải lớn hơn 0');
        return false;
    }

    if (waterUnitPrice <= 0) {
        showAlert('error', 'Đơn giá nước phải lớn hơn 0');
        return false;
    }

    // Validate fees
    if (roomFee <= 0) {
        showAlert('error', 'Tiền phòng phải lớn hơn 0');
        return false;
    }

    // Calculate total to ensure it's reasonable
    const electricityUsage = electricityCurr - electricityPrev;
    const waterUsage = waterCurr - waterPrev;
    const electricityFee = electricityUsage * electricityUnitPrice;
    const waterFee = waterUsage * waterUnitPrice;
    const totalAmount = roomFee + wifiFee + electricityFee + waterFee + trashFee;

    if (totalAmount <= 0) {
        showAlert('error', 'Tổng số tiền hóa đơn phải lớn hơn 0');
        return false;
    }

    return true;
}

// Validate that the invoice month is within the allowed range (current month ±2 months)
function validateInvoiceMonth(month, year) {
    const currentDate = new Date();
    const currentMonth = currentDate.getMonth() + 1; // JavaScript months are 0-indexed
    const currentYear = currentDate.getFullYear();
    
    // Create date objects for comparison (using first day of month)
    const currentMonthDate = new Date(currentYear, currentMonth - 1, 1);
    const requestedMonthDate = new Date(year, month - 1, 1);
    
    // Calculate difference in months
    const monthsDifference = (requestedMonthDate.getFullYear() - currentMonthDate.getFullYear()) * 12 + 
                            (requestedMonthDate.getMonth() - currentMonthDate.getMonth());
    
    // Allow creation for current month ±2 months
    if (Math.abs(monthsDifference) > 2) {
        const currentMonthYear = String(currentMonth).padStart(2, '0') + '/' + currentYear;
        const requestedMonthYear = String(month).padStart(2, '0') + '/' + year;
        
        if (monthsDifference > 2) {
            showAlert('error', 
                `Không thể tạo hóa đơn cho tháng ${requestedMonthYear}. Chỉ có thể tạo hóa đơn tối đa 2 tháng trong tương lai từ tháng hiện tại (${currentMonthYear}).`);
        } else {
            showAlert('error', 
                `Không thể tạo hóa đơn cho tháng ${requestedMonthYear}. Chỉ có thể tạo hóa đơn tối đa 2 tháng trong quá khứ từ tháng hiện tại (${currentMonthYear}).`);
        }
        return false;
    }
    
    return true;
}

// Remove modal backdrop (utility function)
function removeModalBackdrop() {
    const backdrop = document.querySelector('.modal-backdrop');
    if (backdrop) {
        backdrop.remove();
    }
}

// Format currency
function formatCurrency(amount) {
    if (!amount) return '0 VNĐ';
    return new Intl.NumberFormat('vi-VN').format(amount) + ' VNĐ';
}

// Format date
function formatDate(dateString) {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('vi-VN');
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function () {
    // Set up search input event listener
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function (e) {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
    }

    // Set up status filter event listener
    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) {
        statusFilter.addEventListener('change', function () {
            performSearch();
        });
    }

    // Load available contracts for create invoice modal
    loadAvailableContracts();

    // Setup auto-calculation for utility costs
    setupAutoCalculation();

    // Setup month input validation
    setupMonthInputValidation();
});

// Setup month input validation to restrict date selection
function setupMonthInputValidation() {
    const monthInput = document.getElementById('invoiceMonthInput');
    if (monthInput) {
        // Set min and max dates for the month input (±2 months from current)
        const currentDate = new Date();
        const currentYear = currentDate.getFullYear();
        const currentMonth = currentDate.getMonth() + 1; // JavaScript months are 0-indexed
        
        // Calculate min date (2 months ago)
        let minYear = currentYear;
        let minMonth = currentMonth - 2;
        if (minMonth <= 0) {
            minMonth += 12;
            minYear -= 1;
        }
        
        // Calculate max date (2 months ahead)
        let maxYear = currentYear;
        let maxMonth = currentMonth + 2;
        if (maxMonth > 12) {
            maxMonth -= 12;
            maxYear += 1;
        }
        
        // Format dates for input (YYYY-MM)
        const minDate = `${minYear}-${String(minMonth).padStart(2, '0')}`;
        const maxDate = `${maxYear}-${String(maxMonth).padStart(2, '0')}`;
        
        monthInput.setAttribute('min', minDate);
        monthInput.setAttribute('max', maxDate);
        
        // Add event listener for real-time validation
        monthInput.addEventListener('change', function() {
            const selectedValue = this.value;
            if (selectedValue) {
                const [year, month] = selectedValue.split('-');
                const selectedMonth = parseInt(month);
                const selectedYear = parseInt(year);
                
                if (!validateInvoiceMonth(selectedMonth, selectedYear)) {
                    // Clear the invalid selection
                    this.value = '';
                }
            }
        });
        
        // Set default to current month
        const defaultDate = `${currentYear}-${String(currentMonth).padStart(2, '0')}`;
        monthInput.value = defaultDate;
    }
}

// Load available contracts for create invoice modal
function loadAvailableContracts() {
    fetch('/api/payments/available-contracts')
        .then(response => response.json())
        .then(contracts => {
            availableContracts = contracts; // Store contracts globally
            const select = document.getElementById('invoiceRoomSelect');
            if (select && contracts) {
                select.innerHTML = '<option value="">Chọn phòng</option>';
                contracts.forEach(contract => {
                    const option = document.createElement('option');
                    option.value = contract.contractId;
                    option.textContent = `${contract.hostelName} - ${contract.roomCode} (${contract.tenantName})`;
                    select.appendChild(option);
                });
            }
            // Add event listener to update room fee when a room is selected
            select.addEventListener('change', function () {
                const selectedContractId = select.value;
                const roomFeeInput = document.getElementById('roomFeeInput');
                if (roomFeeInput && selectedContractId) {
                    const selectedContract = availableContracts.find(contract => contract.contractId == selectedContractId);
                    if (selectedContract && selectedContract.roomPrice) {
                        roomFeeInput.value = selectedContract.roomPrice;
                    } else {
                        roomFeeInput.value = ''; // Clear if no price is found
                    }
                } else {
                    roomFeeInput.value = ''; // Clear if no room is selected
                }
            });
        })
        .catch(error => {
            console.error('Error loading contracts:', error);
            showAlert('error', 'Có lỗi khi tải danh sách hợp đồng');
        });
}

// Calculate utility costs with unit price from frontend
function calculateUtilityCost(previousReading, currentReading, utilityType, unitPrice) {
    const requestData = {
        previousReading: previousReading,
        currentReading: currentReading,
        utilityType: utilityType,
        unitPrice: unitPrice
    };

    return fetch('/api/payments/calculate-utility', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(requestData)
    })
        .then(response => response.json())
        .then(data => {
            return data;
        })
        .catch(error => {
            console.error('Error calculating utility cost:', error);
            return null;
        });
}

// Auto-calculate electricity cost when values change
function setupAutoCalculation() {
    const electricityPrevInput = document.getElementById('electricityPrevInput');
    const electricityCurrInput = document.getElementById('electricityCurrInput');
    const electricityUnitPriceInput = document.getElementById('electricityUnitPriceInput');
    const waterPrevInput = document.getElementById('waterPrevInput');
    const waterCurrInput = document.getElementById('waterCurrInput');
    const waterUnitPriceInput = document.getElementById('waterUnitPriceInput');

    // Function to calculate and display electricity cost
    function calculateElectricityCost() {
        const prev = parseInt(electricityPrevInput.value) || 0;
        const curr = parseInt(electricityCurrInput.value) || 0;
        const unitPrice = parseFloat(electricityUnitPriceInput.value) || 0;

        if (curr > prev && unitPrice > 0) {
            calculateUtilityCost(prev, curr, 'electricity', unitPrice)
                .then(result => {
                    if (result && result.totalCost) {
                        // You can display the calculated cost somewhere if needed
                        console.log('Electricity cost calculated:', result.totalCost);
                    }
                });
        }
    }

    // Function to calculate and display water cost
    function calculateWaterCost() {
        const prev = parseInt(waterPrevInput.value) || 0;
        const curr = parseInt(waterCurrInput.value) || 0;
        const unitPrice = parseFloat(waterUnitPriceInput.value) || 0;

        if (curr > prev && unitPrice > 0) {
            calculateUtilityCost(prev, curr, 'water', unitPrice)
                .then(result => {
                    if (result && result.totalCost) {
                        // You can display the calculated cost somewhere if needed
                        console.log('Water cost calculated:', result.totalCost);
                    }
                });
        }
    }

    // Add event listeners
    if (electricityPrevInput && electricityCurrInput && electricityUnitPriceInput) {
        electricityPrevInput.addEventListener('input', calculateElectricityCost);
        electricityCurrInput.addEventListener('input', calculateElectricityCost);
        electricityUnitPriceInput.addEventListener('input', calculateElectricityCost);
    }

    if (waterPrevInput && waterCurrInput && waterUnitPriceInput) {
        waterPrevInput.addEventListener('input', calculateWaterCost);
        waterCurrInput.addEventListener('input', calculateWaterCost);
        waterUnitPriceInput.addEventListener('input', calculateWaterCost);
    }
}
