document.addEventListener('DOMContentLoaded', function () {
    const rentPriceInput = document.getElementById('rent-price');
    const rentPriceHidden = document.getElementById('rent-price-hidden');

    // Kiểm tra và xử lý giá trị từ backend
    if (rentPriceInput.value) {
        // Loại bỏ '.', 'VND' và khoảng trắng
        const cleanedValue = rentPriceInput.value
            .replace(/\./g, '')     // Loại bỏ dấu chấm
            .replace(/VND/i, '')    // Loại bỏ 'VND'
            .replace(/\s+/g, '')    // Loại bỏ khoảng trắng
            .trim();

        console.log('Cleaned value:', cleanedValue);

        // Chuyển đổi thành số
        const numericValue = parseInt(cleanedValue, 10);

        if (!isNaN(numericValue)) {
            // Cập nhật hidden input
            rentPriceHidden.value = numericValue;

            // Format lại input
            rentPriceInput.value = new Intl.NumberFormat('vi-VN', {
                style: 'decimal',
                minimumFractionDigits: 0,
                maximumFractionDigits: 0
            }).format(numericValue) + ' VND';
        }
    }

    // Đặt lại isInitialLoad sau khi form tải
    setTimeout(() => {
        isInitialLoad = false;
    }, 0);
});

function formatPrice(input) {
    // Không chạy khi form vừa tải
    if (isInitialLoad) {
        console.log('formatPrice skipped on initial load');
        return;
    }

    console.log('formatPrice called with value:', input.value);

    // Loại bỏ mọi ký tự không phải số
    let value = input.value.replace(/[^0-9]/g, '');
    const hiddenInput = document.getElementById('rent-price-hidden');

    if (value) {
        const numberValue = parseInt(value, 10);
        const formatter = new Intl.NumberFormat('vi-VN', {
            style: 'decimal',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        });

        // Format và hiển thị
        input.value = formatter.format(numberValue) + ' VND';

        // Lưu giá trị số nguyên vào hidden input
        hiddenInput.value = numberValue;
    } else {
        input.value = '';
        hiddenInput.value = 0;
    }
}

function validatePrice(input) {
    // Không chạy khi form vừa tải
    if (isInitialLoad) {
        console.log('validatePrice skipped on initial load');
        return;
    }

    console.log('validatePrice called with value:', input.value);

    // Loại bỏ mọi ký tự không phải số
    const value = input.value.replace(/[^0-9]/g, '');
    const errorDiv = document.getElementById('price-error');
    const hiddenInput = document.getElementById('rent-price-hidden');

    if (!value || isNaN(parseInt(value, 10)) || parseInt(value, 10) < 1) {
        errorDiv.style.display = 'block';
        input.classList.add('is-invalid');
        input.value = '';
        hiddenInput.value = 0;
    } else {
        errorDiv.style.display = 'none';
        input.classList.remove('is-invalid');
        const numberValue = parseInt(value, 10);

        // Format lại giá trị
        const formatter = new Intl.NumberFormat('vi-VN', {
            style: 'decimal',
            minimumFractionDigits: 0,
            maximumFractionDigits: 0
        });

        input.value = formatter.format(numberValue) + ' VND';
        hiddenInput.value = numberValue;
    }
}

// Thêm event listener cho input
document.addEventListener('DOMContentLoaded', function () {
    const rentPriceInput = document.getElementById('rent-price');
    if (rentPriceInput) {
        rentPriceInput.addEventListener('input', function () {
            formatPrice(this);
        });

        rentPriceInput.addEventListener('blur', function () {
            validatePrice(this);
        });
    }
});

// ✅ CẬP NHẬT NÚT CẬP NHẬT HỢP ĐỒNG
$('#btn-update').on('click', function () {
    const contractId = $(this).data('contract-id');

    if (!contractId) {
        Swal.fire({
            icon: 'error',
            title: 'Không tìm thấy ID hợp đồng!',
            text: 'Vui lòng tải lại trang và thử lại.',
            confirmButtonText: 'OK',
            confirmButtonColor: '#e74c3c'
        });
        return;
    }

    if (!validateContractForm()) {
        Swal.fire({
            icon: 'warning',
            title: 'Thiếu thông tin!',
            text: 'Vui lòng điền đầy đủ thông tin trước khi cập nhật!',
            confirmButtonText: 'OK',
            confirmButtonColor: '#f39c12'
        });
        return;
    }

    Swal.fire({
        title: 'Xác nhận cập nhật',
        html: `
            <div class="text-start">
                <p>Bạn có chắc chắn muốn cập nhật hợp đồng này?</p>
                <p><strong>ID:</strong> ${contractId}</p>
                <p><strong>Người thuê:</strong> ${$('#tenant-name').val()}</p>
                <p><strong>Phòng:</strong> ${$('#room-number').val()}</p>
            </div>
        `,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#17a2b8',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Có, cập nhật!',
        cancelButtonText: 'Hủy'
    }).then((result) => {
        if (result.isConfirmed) {
            // Hiển thị loading
            Swal.fire({
                title: 'Đang cập nhật...',
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Simulate update process
            setTimeout(() => {
                Swal.fire({
                    icon: 'success',
                    title: 'Cập nhật thành công! ✏️',
                    text: 'Hợp đồng đã được cập nhật trong hệ thống!',
                    confirmButtonText: 'OK',
                    confirmButtonColor: '#17a2b8'
                });
            }, 2000);
        }
    });
});


// ✅ THÔNG BÁO CHO CÁC NÚT KHÁC
$('#btn-add-customer-host').on('click', function () {
    Toast.fire({
        icon: 'info',
        title: 'Mở form thêm Khách thuê khác'
    });
});

$('#btn-add-resident').on('click', function () {
    Toast.fire({
        icon: 'info',
        title: 'Mở form thêm người ở'
    });
});

// ✅ THÔNG BÁO KHI CHUYỂN TAB
$('.nav-link').on('click', function () {
    const tabName = $(this).text().trim();
    Toast.fire({
        icon: 'info',
        title: `Chuyển sang tab: ${tabName}`
    });
});

// ✅ THÔNG BÁO KHI CHỌN KHU TRỌ/PHÒNG
$('#hostelSelect').on('change', function () {
    const hostelName = $(this).find('option:selected').text();
    if (hostelName && hostelName !== '-- Chọn khu trọ --') {
        Toast.fire({
            icon: 'success',
            title: `Đã chọn: ${hostelName}`
        });
    }
});

$('#roomSelect').on('change', function () {
    const roomName = $(this).find('option:selected').text();
    if (roomName && roomName !== '-- Chọn phòng trọ --') {
        Toast.fire({
            icon: 'success',
            title: `Đã chọn phòng: ${roomName.split(' - ')[0]}`
        });
    }
});

// ✅ THÔNG BÁO KHI ZOOM PREVIEW
$('#btn-zoom-in, #btn-zoom-out, #btn-reset-zoom').on('click', function () {
    const action = $(this).attr('title');
    Toast.fire({
        icon: 'info',
        title: action
    });
});

// ✅ SWEETALERT2 CONFIGURATION
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 3000,
    timerProgressBar: true,
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer)
        toast.addEventListener('mouseleave', Swal.resumeTimer)
    }
});