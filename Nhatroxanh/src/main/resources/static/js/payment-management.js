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
        const statusClass = payment.paymentStatus === 'ĐÃ_THANH_TOÁN' ? 'status-paid' : 'status-unpaid';
        const statusIcon = payment.paymentStatus === 'ĐÃ_THANH_TOÁN' ? 'fas fa-check' : 'fas fa-exclamation-triangle';
        const statusText = payment.paymentStatus === 'ĐÃ_THANH_TOÁN' ? 'Đã thanh toán' : 'Chưa thanh toán';
        
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
    } else {
        statusElement.innerHTML = '<span class="status-badge status-unpaid"><i class="fas fa-exclamation-triangle me-1"></i>Chưa thanh toán</span>';
    }
    
    // Store payment ID for actions
    document.getElementById('invoiceModal').setAttribute('data-payment-id', payment.paymentId);
}

// Mark payment as paid
function markAsPaid() {
    const modal = document.getElementById('invoiceModal');
    const paymentId = modal.getAttribute('data-payment-id');
    
    if (!paymentId) {
        showAlert('error', 'Không tìm thấy ID hóa đơn');
        return;
    }
    
    fetch(`/api/payments/${paymentId}/status`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status: 'ĐÃ_THANH_TOÁN' })
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showAlert('success', 'Đã cập nhật trạng thái thanh toán');
            // Close modal
            const bsModal = bootstrap.Modal.getInstance(modal);
            bsModal.hide();
            // Reload current page
            loadPage(currentPage);
        } else {
            showAlert('error', data.message || 'Có lỗi xảy ra');
        }
    })
    .catch(error => {
        console.error('Error updating payment status:', error);
        showAlert('error', 'Có lỗi xảy ra khi cập nhật trạng thái');
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
    showAlert('warning', 'Bạn có chắc chắn muốn xóa hóa đơn này?', { showConfirm: true, onConfirm: () => {
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
    }});
}

// Download invoice (placeholder function)
function downloadInvoice() {
    showAlert('info', 'Tính năng tải xuống đang được phát triển');
}

// Send unpaid invoices (placeholder function)
function sendUnpaidInvoices() {
    showAlert('warning', 'Bạn có chắc muốn gửi tất cả hóa đơn chưa thanh toán và quá hạn đến người thuê?', { showConfirm: true, onConfirm: () => {
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
    }});
}

// Create new invoice
function createNewInvoice() {
    const form = document.getElementById('createInvoiceForm');
    if (form.checkValidity()) {
        form.submit();
    } else {
        showAlert('error', 'Vui lòng điền đầy đủ thông tin');
    }
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
document.addEventListener('DOMContentLoaded', function() {
    // Set up search input event listener
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                performSearch();
            }
        });
    }
    
    // Set up status filter event listener
    const statusFilter = document.getElementById('statusFilter');
    if (statusFilter) {
        statusFilter.addEventListener('change', function() {
            performSearch();
        });
    }
    
    // Load available contracts for create invoice modal
    loadAvailableContracts();
    
    // Setup auto-calculation for utility costs
    setupAutoCalculation();
});

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
            select.addEventListener('change', function() {
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
