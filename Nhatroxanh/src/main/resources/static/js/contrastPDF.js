
console.log('🚀 Contract form loading...');


// ✅ GLOBAL VARIABLES (sửa tên để rõ, tránh duplicate)
let isEditMode = $('#isEditMode').val() === 'true';
let currentRoomId = $('#currentRoomId').val();  // Giữ nguyên, từ hidden input (e.g., '11')
let currentHostelId = $('#currentHostelId').val();
let isLoading = false; // Flag để tránh loop

let globalCurrentRoomId = currentRoomId || null;  // ✅ Đổi tên & set từ currentRoomId (nếu edit mode)

// ✅ Hàm load rooms (filter unactive + current room nếu active)
// ✅ Hàm load rooms (dùng globalCurrentRoomId)
function loadRoomsByHostel(hostelId, callback) {
    if (!hostelId) return;
    $.ajax({
        url: `/api/rooms/by-hostel/${hostelId}`,
        method: 'GET',
        success: function (rooms) {
            console.log('🔍 Raw rooms from API:', rooms);  // Debug
            let roomOptions = '<option value="">-- Chọn phòng trọ --</option>';
            if (rooms && rooms.length > 0) {
                // Filter: Giữ unactive + current room nếu active
                let filteredRooms = rooms.filter(room => room.status === 'unactive');
                if (globalCurrentRoomId) {
                    const currentRoom = rooms.find(room => room.roomId == globalCurrentRoomId);  // == cho number/string
                    if (currentRoom && !filteredRooms.some(r => r.roomId == globalCurrentRoomId)) {
                        filteredRooms.push(currentRoom);  // Add current dù active
                    }
                }
                console.log('🔍 Filtered rooms:', filteredRooms);  // Debug

                if (filteredRooms.length === 0) {
                    roomOptions += '<option value="">Phòng Hiện tại đã hết </option>';
                } else {
                    filteredRooms.forEach(room => {
                        const province = room.province ? (room.province.includes('Đà Nẵng') ? 'Đà Nẵng' : room.province) : 'Đà Nẵng';
                        const fullAddress = [room.street || 'Chưa cập nhật đường', room.ward ? `Phường ${room.ward}` : '', room.district ? `Quận ${room.district}` : '', province].filter(Boolean).join(', ');
                        const price = room.price ? room.price.toLocaleString('vi-VN') : '0';
                        const roomDisplayName = room.roomName || (room.roomNumber ? room.roomNumber : 'không tên');
                        const displayPrefix = roomDisplayName.startsWith('Phòng ') ? '' : 'Phòng ';
                        roomOptions += `
                            <option value="${room.roomId || ''}"
                                data-street="${room.street || ''}"
                                data-ward="${room.ward || ''}"
                                data-district="${room.district || ''}"
                                data-province="${province}"
                                data-area="${room.area || ''}"
                                data-price="${room.price || ''}"
                                data-room-name="${room.roomName || ''}"
                                data-room-number="${room.roomNumber || ''}"
                                data-max-tenants="${room.maxTenants || ''}"
                                data-hostel-id="${hostelId}"
                                data-status="${room.status || ''}"
                            >
                                ${displayPrefix}${roomDisplayName} - ${fullAddress} - ${price}đ
                            </option>
                        `;
                    });
                }
                $('#roomSelect').html(roomOptions).prop('disabled', false);
            } else {
                $('#roomSelect').html('<option value="">Không có phòng</option>').prop('disabled', true);
            }
            if (callback) callback();
        },
        error: function (xhr, status, error) {
            console.error('Lỗi tải phòng:', error);
            $('#roomSelect').html('<option value="">Lỗi tải phòng</option>').prop('disabled', true);
            alert('Không thể tải danh sách phòng. Vui lòng thử lại sau.');
        }
    });
}
// ✅ HÀM GỬI EMAIL (tách riêng)
function sendContractEmail() {
    const contractHtml = captureContractPreview();
    const contractData = {
        contractId: getContractIdFromUrl(),
        recipientEmail: $('#tenant-email').val(),
        recipientName: $('#tenant-name').val(),
        contractHtml: contractHtml,
        subject: `Hợp đồng thuê nhà - ${$('#tenant-name').val()}`,
        tenantName: $('#tenant-name').val(),
        roomNumber: $('#room-number').val(),
        monthlyRent: $('#rent-price').val()
    };

    return $.ajax({
        url: '/api/contracts/send-email-pdf',
        method: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(contractData)
    }).then(response => {
        if (response.success) {
            Swal.fire({
                icon: 'success',
                title: 'Gửi email thành công! 🎉',
                html: `
                    <div class="text-start">
                        <p>✅ Email đã được gửi thành công!</p>
                        <p>📧 <strong>Gửi tới:</strong> ${$('#tenant-email').val()}</p>
                        <p>🕒 <strong>Thời gian:</strong> ${new Date().toLocaleString('vi-VN')}</p>
                    </div>
                `,
                confirmButtonText: 'Về danh sách hợp đồng',
                confirmButtonColor: '#28a745'
            }).then(() => {
                window.location.href = '/chu-tro/DS-hop-dong-host';
            });
        } else {
            throw new Error(response.message);
        }
    }).catch(error => {
        Swal.fire({
            icon: 'error',
            title: 'Lỗi gửi email!',
            text: error.message || 'Có lỗi xảy ra khi gửi email',
            confirmButtonText: 'Thử lại',
            confirmButtonColor: '#e74c3c'
        });
        throw error;
    });
}


$(document).ready(function () {
    // Xử lý tìm người thuê theo số điện thoại
    $('#tenant-phone').on('change', function () {
        const phone = $(this).val();
        $.ajax({
            url: '/api/contracts/get-tenant-by-phone',
            method: 'POST',
            data: { phone: phone },
            success: function (response) {
                if (response.success) {
                    const tenant = response.tenant;
                    $('#tenant-id').val(tenant.cccdNumber); // Gán số CCCD đầy đủ
                    $('#tenant-name').val(tenant.fullName);
                    $('#tenant-dob').val(tenant.birthday);
                    $('#tenant-id-date').val(tenant.issueDate);
                    $('#tenant-id-place').val(tenant.issuePlace);
                    $('#tenant-street').val(tenant.street);
                    $('#tenant-ward').val(tenant.ward);
                    $('#tenant-district').val(tenant.district);
                    $('#tenant-province').val(tenant.province);
                    $('#tenant-email').val(tenant.email);

                    // Lấy ảnh CCCD
                    if (tenant.cccdNumber) {
                        $.ajax({
                            url: '/api/contracts/cccd-images',
                            method: 'POST',
                            data: { cccdNumber: tenant.cccdNumber }, // Sử dụng cccdNumber
                            success: function (imageResponse) {
                                if (imageResponse.success) {
                                    if (imageResponse.cccdFrontUrl) {
                                        $('#cccd-front-preview').html(`<img src="${imageResponse.cccdFrontUrl}" alt="CCCD Front"/>`);
                                    } else {
                                        $('#cccd-front-preview').html('<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt trước</div>');
                                    }
                                    if (imageResponse.cccdBackUrl) {
                                        $('#cccd-back-preview').html(`<img src="${imageResponse.cccdBackUrl}" alt="CCCD Back"/>`);
                                    } else {
                                        $('#cccd-back-preview').html('<i class="fa fa-camera fa-2x"></i><div class="mt-2">Tải ảnh mặt sau</div>');
                                    }
                                } else {
                                    alert('Không tìm thấy ảnh CCCD: ' + imageResponse.message);
                                }
                            },
                            error: function (xhr) {
                                alert('Lỗi khi lấy ảnh CCCD: ' + xhr.responseJSON.message);
                            }
                        });
                    }
                } else {
                    alert('Không tìm thấy người thuê!');
                }
            },
            error: function (xhr) {
                alert('Lỗi: ' + xhr.responseJSON.message);
            }
        });
    });

});

// ✅ HÀM CAPTURE HTML PREVIEW
function captureContractPreview() {
    // Lấy dữ liệu từ form
    const today = new Date();
    const formData = {
        tenantName: $('#tenant-name').val() || 'Khách hàng',
        tenantBirth: $('#tenant-birth').val() || '',
        tenantId: $('#tenant-id').val() || '',
        tenantIdDate: $('#tenant-id-date').val() || '',
        tenantIdPlace: $('#tenant-id-place').val() || '',
        tenantAddress: $('#tenant-address').val() || '',
        tenantPhone: $('#tenant-phone').val() || '',

        landlordName: $('#owner-name').val() || 'Chủ trọ',
        landlordBirth: $('#owner-birth').val() || '',
        landlordId: $('#owner-id').val() || '',
        landlordIdDate: $('#owner-id-date').val() || '',
        landlordIdPlace: $('#owner-id-place').val() || '',
        landlordAddress: $('#owner-address').val() || '',
        landlordPhone: $('#owner-phone').val() || '',

        roomAddress: $('#room-address').val() || '',
        roomArea: $('#room-area').val() || '',
        roomPrice: $('#room-price').val() || '',
        deposit: $('#deposit').val() || '',
        startDate: $('#start-date').val() || '',
        contractDuration: $('#contract-duration').val() || '',
        electricPrice: $('#electric-price').val() || '',
        waterPrice: $('#water-price').val() || '',
        wifiPrice: $('#wifi-price').val() || '',
        cleaningPrice: $('#cleaning-price').val() || '',
        parkingPrice: $('#parking-price').val() || '',
        otherFees: $('#other-fees').val() || '',
        paymentDate: $('#payment-date').val() || '',
        contractLocation: $('#contract-location').val() || 'Đà Nẵng'
    };

    // ✅ THÊM CÁC BIẾN THIẾU
    const roomNumber = $('#room-number').val() || '';
    const residents = $('#residents-list .resident-item').map(function () {
        const name = $(this).find('.resident-name').text().trim();
        return name ? name : null;
    }).get().filter(name => name).join(', ') || '';

    const amenities = $('.amenities input:checked').map(function () {
        return $(this).next('label').text().trim();
    }).get().join(', ') || 'Không có tiện ích đặc biệt';

    const depositMonths = $('#deposit-months').val() || '2';
    const paymentMethod = $('#payment-method option:selected').text() || 'Tiền mặt';

// Tính ngày kết thúc
    let endDate = '';
    if (formData.startDate && formData.contractDuration) {
        const start = new Date(formData.startDate);
        start.setMonth(start.getMonth() + parseInt(formData.contractDuration));
        endDate = formatDate(start.toISOString().split('T')[0]);
    }

    const contractSignDate = formatDate(today.toISOString().split('T')[0]);


    // Format số tiền
    function formatMoney(amount) {
        if (!amount) return '0';
        return parseInt(amount).toLocaleString('vi-VN') + ' VNĐ';
    }

    // Format ngày
    function formatDate(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }

    // Chuyển số tiền thành chữ
    function numberToWords(number) {
        if (!number) return 'Không đồng';
        const units = ['', 'nghìn', 'triệu', 'tỷ'];
        const numbers = ['không', 'một', 'hai', 'ba', 'bốn', 'năm', 'sáu', 'bảy', 'tám', 'chín'];
        let result = '';
        let num = parseInt(number);
        let unitIndex = 0;

        if (num === 0) return 'Không đồng';

        while (num > 0) {
            let chunk = num % 1000;
            let chunkStr = '';
            if (chunk > 0) {
                let hundreds = Math.floor(chunk / 100);
                let tens = Math.floor((chunk % 100) / 10);
                let ones = chunk % 10;

                if (hundreds > 0) {
                    chunkStr += numbers[hundreds] + ' trăm';
                    if (tens > 0 || ones > 0) chunkStr += ' ';
                }
                if (tens > 1) {
                    chunkStr += numbers[tens] + ' mươi';
                    if (ones > 0) chunkStr += ' ' + numbers[ones];
                } else if (tens === 1) {
                    chunkStr += 'mười';
                    if (ones > 0) chunkStr += ' ' + numbers[ones];
                } else if (ones > 0) {
                    chunkStr += numbers[ones];
                }
                if (unitIndex > 0) {
                    chunkStr += ' ' + units[unitIndex];
                }
                result = chunkStr + (result ? ' ' + result : '');
            }
            num = Math.floor(num / 1000);
            unitIndex++;
        }
        return result.charAt(0).toUpperCase() + result.slice(1) + ' đồng';
    }

    // Tạo HTML hợp đồng
    const contractHtml = `<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hợp đồng thuê nhà</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: "Liberation Serif", "DejaVu Serif", "Times New Roman", "Noto Serif", serif;
            font-size: 16px;
            line-height: 1.6;
            color: #1a1a1a;
            background: #ffffff;
            padding: 50px;
            /* max-width: 900px; */
            margin: 0 auto;
            min-height: 100vh;
            background-image:
                linear-gradient(to right, #f8f9fa 0%, #ffffff 2%, #ffffff 98%, #f8f9fa 100%);
        }

        .contract-wrapper {
            background: #ffffff;
            /* border: 2px solid #2c5aa0; */
            border-radius: 8px;
            padding: 40px;
            box-shadow: 0 8px 25px rgba(44, 90, 160, 0.1);
            position: relative;
            overflow: hidden;
        }

        /* .contract-wrapper::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 6px;
            background: linear-gradient(90deg, #2c5aa0 0%, #3d7bd4 50%, #2c5aa0 100%);
        } */

        .header {
            text-align: center;
            margin-bottom: 40px;
            position: relative;
            padding: 20px 0;
        }

        .header .country {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 14px;
            font-weight: 600;
            text-transform: uppercase;
            line-height: 1.4;
            margin-bottom: 25px;
            color: #2c5aa0;
            letter-spacing: 0.5px;
        }

        .header .motto {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 14px;
            font-weight: 600;
            margin-bottom: 15px;
            color: #2c5aa0;
        }

        .header .separator {
            height: 2px;
            width: 120px;
            background: #2c5aa0;
            border: none;
            margin: 10px auto;
            border-radius: 1px;
        }

        .header .title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 24px;
            font-weight: 700;
            text-transform: uppercase;
            margin: 25px 0;
            letter-spacing: 1.5px;
            color: #1a365d;
            position: relative;
            padding: 15px 30px;
        }

        .header .contract-number {
            font-style: italic;
            font-size: 13px;
            margin-bottom: 0;
            color: #2c5aa0;
            font-weight: 600;
        }

        .content-wrapper {
            text-align: justify;
            line-height: 1.7;
            font-size: 16px;
        }

        .date-location {
            text-align: right;
            margin-bottom: 30px;
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-style: italic;
            font-size: 17px;
            font-weight: 600;
            color: #2c5aa0;
            padding: 15px;
            background: #f8fafc;

        }

        .intro-section {
            margin-bottom: 35px;
            text-indent: 40px;
            font-size: 17px;
            padding: 20px;
            background: #f8fafc;
            border-left: 4px solid #2c5aa0;
            border-radius: 0 6px 6px 0;
            box-shadow: 0 2px 4px rgba(44, 90, 160, 0.05);
        }

        .section {
            margin-bottom: 35px;
            padding: 25px;
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
            position: relative;
        }

        .section::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: linear-gradient(90deg, #2c5aa0 0%, #4299e1 100%);
            border-radius: 8px 8px 0 0;
        }

        .section-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 16px;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 18px;
            color: #2c5aa0;
            padding-bottom: 8px;
            border-bottom: 2px solid #e2e8f0;
            letter-spacing: 0.3px;
        }

        .party-info {
            margin-left: 20px;
            line-height: 1.8;
            font-size: 16px;
            color: #2d3748;
        }

        .party-info div {
            margin-bottom: 8px;
        }

        .clause {
            margin-bottom: 25px;
            padding: 20px;
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
            position: relative;
            page-break-inside: avoid;
        }

        .clause::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 4px;
            background: linear-gradient(180deg, #2c5aa0 0%, #4299e1 100%);
            border-radius: 8px 0 0 8px;
        }

        .clause-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-weight: 700;
            color: #2c5aa0;
            font-size: 17px;
            display: block;
            margin-bottom: 10px;
            padding-bottom: 8px;
            border-bottom: 1px solid #e2e8f0;
            text-transform: uppercase;
        }

        .clause-content {
            margin-left: 0;
            text-align: justify;
            line-height: 1.7;
        }

        .clause-content div {
            margin-bottom: 12px;
        }

        .sub-clause {
            margin: 12px 0 12px 30px;
            line-height: 1.7;
            padding: 8px 15px;
            background: #f7fafc;
            border-radius: 4px;
            border-left: 3px solid #cbd5e0;
            font-size: 16px;
            position: relative;
        }

        .signature-section {
            margin-top: 50px;
            page-break-inside: avoid;
            padding: 30px;
            background: #f8fafc;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
        }

        .signature-table {
            width: 100%;
            border-collapse: separate;
            border-spacing: 24px 0;
        }

        .signature-cell {
            width: 50%;
            text-align: center;
            vertical-align: top;
            padding: 20px;
            background: #ffffff;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
        }

        .signature-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 15px;
            font-size: 17px;
            color: #2c5aa0;
            padding: 10px;
            background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
            border-radius: 4px;
            letter-spacing: 0.5px;
        }

        .signature-note {
            font-size: 12px;
            margin-bottom: 80px;
            font-style: italic;
            color: #4a5568;
        }

        .signature-name {
            font-weight: 600;
            font-size: 16px;
            color: #1a365d;
            border-top: 2px solid #2c5aa0;
            padding-top: 10px;
            margin-top: 20px;
            display: inline-block;
            min-width: 200px;
        }

        .footer {
            text-align: center;
            font-size: 12px;
            color: #64748b;
            margin-top: 40px;
            border-top: 2px solid #e2e8f0;
            padding: 24px;
            background: #f8fafc;
            border-radius: 0 0 8px 8px;
        }

        .footer div:first-child {
            font-weight: 600;
            margin-bottom: 4px;
        }

        .highlight {
            font-weight: 700;
            color: #1a365d;
            background: rgba(59, 130, 246, 0.1);
            padding: 2px 6px;
            border-radius: 4px;
        }

        /* Decorative elements */
        .decorative-line {
            height: 2px;
            background: linear-gradient(90deg, transparent 0%, #2c5aa0 20%, #4299e1 50%, #2c5aa0 80%, transparent 100%);
            margin: 25px 0;
            border-radius: 1px;
        }

        /* Enhanced typography */
        strong {
            font-weight: 600;
            color: #1a365d;
        }

        em {
            color: #2c5aa0;
            font-style: italic;
        }

        @media (max-width: 768px) {
            body {
                padding: 20px;
                font-size: 14px;
            }

            .contract-wrapper {
                padding: 25px;
                margin: 0;
            }

            .header .title {
                font-size: 20px;
                padding: 12px 20px;
            }

            .signature-table {
                border-spacing: 16px 0;
            }

            .signature-cell {
                padding: 24px 12px;
            }

            .party-info {
                padding: 20px;
            }

            .clause-content {
                padding: 20px;
            }
        }

        @media print {
            body {
                padding: 15mm;
                background: white;
                box-shadow: none;
                font-size: 13px;
                -webkit-print-color-adjust: exact;
            }

            .contract-wrapper {
                box-shadow: none;
                /* border: 1px solid #2c5aa0; */
            }

            .signature-section {
                page-break-inside: avoid;
            }

            .clause {
                page-break-inside: avoid;
                margin-bottom: 15px;
            }

            * {
                -webkit-print-color-adjust: exact !important;
                color-adjust: exact !important;
            }

            .no-print { display: none; }
            .page-break { page-break-before: always; }
        }

        /* Custom scrollbar for better aesthetics */
        ::-webkit-scrollbar {
            width: 8px;
        }

        ::-webkit-scrollbar-track {
            background: #f1f1f1;
        }

        ::-webkit-scrollbar-thumb {
            background: #2c5aa0;
            border-radius: 4px;
        }

        ::-webkit-scrollbar-thumb:hover {
            background: #1e3a8a;
        }
    </style>
</head>
<body>
    <div class="contract-wrapper">
        <!-- HEADER -->
        <div class="header">
            <div class="country">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</div>
            <div class="motto">Độc lập - Tự do - Hạnh phúc</div>
            <div class="separator"></div>
            <div class="title">HỢP ĐỒNG THUÊ NHÀ</div>
            <div class="contract-number">Số: ${Date.now()}/HDTN-${new Date().getFullYear()}</div>
        </div>

        <div class="content-wrapper">
            <!-- NGÀY VÀ ĐỊA ĐIỂM -->
            <div class="date-location">
                <em>Đà Nẵng, ngày ${today.getDate()} tháng ${today.getMonth() + 1} năm ${today.getFullYear()}</em>
            </div>

            <!-- MỞ ĐẦU -->
            <div class="intro-section">
                Căn cứ Bộ luật Dân sự năm 2015; Luật Nhà ở năm 2014 và các văn bản pháp luật có liên quan, hôm nay
                tại <strong>${formData.propertyAddress || 'Đà Nẵng'}</strong>, chúng tôi gồm có:
            </div>

            <div class="decorative-line"></div>

            <!-- BÊN CHO THUÊ -->
            <div class="section">
                <div class="section-title">BÊN CHO THUÊ (Bên A):</div>
                <div class="party-info">
                    <div>Ông/Bà: <span class="highlight">${formData.landlordName}</span></div>
                    <div>Sinh ngày: ${formData.landlordBirth}</div>
                    <div>Số CMND/CCCD: <span class="highlight">${formData.landlordId}</span>, cấp ngày ${formData.landlordIdDate}</div>
                    <div>Nơi cấp: ${formData.landlordIdPlace}</div>
                    <div>Địa chỉ thường trú: ${formData.landlordAddress}</div>
                    <div>Số điện thoại: <span class="highlight">${formData.landlordPhone}</span></div>
                </div>
            </div>

            <!-- BÊN THUÊ -->
            <div class="section">
                <div class="section-title">BÊN THUÊ (Bên B):</div>
                <div class="party-info">
                    <div>Ông/Bà: <span class="highlight">${formData.tenantName}</span></div>
                    <div>Sinh ngày: ${formData.tenantBirth}</div>
                    <div>Số CMND/CCCD: <span class="highlight">${formData.tenantId}</span>, cấp ngày ${formData.tenantIdDate}</div>
                    <div>Nơi cấp: ${formData.tenantIdPlace}</div>
                    <div>Địa chỉ thường trú: ${formData.tenantAddress}</div>
                    <div>Số điện thoại: <span class="highlight">${formData.tenantPhone}</span></div>
                </div>
            </div>

            // ${residents ? `
            // <div class="section">
            //     <div class="section-title">Danh sách người cùng ở:</div>
            //     <div class="party-info"></div>
            // </div>
            // ` : ''}

            <div class="decorative-line"></div>

            <div style="text-align: center; margin: 30px 0; font-size: 15px; font-style: italic; color: #2c5aa0; font-weight: 600;">
                Sau khi bàn bạc trên tinh thần dân chủ, bình đẳng, tự nguyện, hai bên cùng thống nhất
                ký kết hợp đồng thuê nhà với những điều khoản sau đây:
            </div>

            <!-- ĐIỀU 1 -->
            <div class="clause">
                <div class="clause-title">Điều 1: Đối tượng của hợp đồng</div>
                <div class="clause-content">
                    <div>Bên A đồng ý cho Bên B thuê căn phòng trọ số <span class="highlight">${roomNumber}</span>,
                    tọa lạc tại địa chỉ: <span class="highlight">${formData.roomAddress}</span>.</div>
                    <div>Diện tích sử dụng: <span class="highlight">${formData.roomArea} m²</span></div>
                    <div>Tình trạng nhà: Nhà được bàn giao trong tình trạng tốt, đầy đủ các tiện ích cơ bản.</div>
                </div>
            </div>

            <!-- ĐIỀU 2 -->
            <div class="clause">
                <div class="clause-title">Điều 2: Thời hạn thuê</div>
                <div class="clause-content">
                    <div>Thời hạn thuê nhà: <span class="highlight">${formData.contractDuration} tháng</span></div>
                    <div>Từ ngày: <span class="highlight">${formData.startDate}</span></div>
                    <div>Đến ngày: <span class="highlight">${endDate}</span></div>
                    <div>Hợp đồng có hiệu lực kể từ ngày ký và bàn giao nhà.</div>
                </div>
            </div>

            <!-- ĐIỀU 3 -->
            <div class="clause">
                <div class="clause-title">Điều 3: Giá thuê và phương thức thanh toán</div>
                <div class="clause-content">
                    <div>Giá thuê nhà: <span class="highlight">${formatMoney(formData.roomPrice)} VNĐ/tháng</span></div>
                    <div>Tiền đặt cọc: <span class="highlight">${formatMoney(formData.deposit)}VNĐ</span> (tương đương ${depositMonths} tháng tiền nhà)</div>
                    <div>Thời hạn thanh toán: ${formData.paymentDate} hàng tháng</div>
                    <div>Phương thức thanh toán: ${paymentMethod}</div>
                    <div>Các chi phí khác (điện, nước, internet, vệ sinh) do Bên B thanh toán theo thực tế sử dụng.</div>
                </div>
            </div>

            <!-- ĐIỀU 4 -->
            <div class="clause">
                <div class="clause-title">Điều 4: Tiện ích và trang thiết bị</div>
                <div class="clause-content">
                    <div>Nhà được trang bị các tiện ích: <span class="highlight">${amenities}</span></div>
                    <div>Bên A cam kết bàn giao nhà đúng hiện trạng như đã thỏa thuận.</div>
                </div>
            </div>

            <!-- ĐIỀU 5 -->
            <div class="clause">
                <div class="clause-title">Điều 5: Quyền và nghĩa vụ của bên cho thuê</div>
                <div class="clause-content">
                    <div><strong>Quyền của Bên A:</strong></div>
                    <div class="sub-clause">Được nhận tiền thuê nhà đúng thời hạn theo thỏa thuận;</div>
                    <div class="sub-clause">Được kiểm tra, giám sát việc sử dụng nhà của Bên B;</div>
                    <div class="sub-clause">Được đơn phương chấm dứt hợp đồng nếu Bên B vi phạm nghiêm trọng.</div>

                    <div style="margin-top: 16px;"><strong>Nghĩa vụ của Bên A:</strong></div>
                    <div class="sub-clause">Bàn giao nhà cho Bên B đúng thời hạn, đúng hiện trạng đã thỏa thuận;</div>
                    <div class="sub-clause">Đảm bảo Bên B sử dụng nhà ổn định trong thời hạn hợp đồng;</div>
                    <div class="sub-clause">Sửa chữa những hư hỏng do lỗi kỹ thuật của nhà;</div>
                    <div class="sub-clause">Không được tăng giá thuê trong thời hạn hợp đồng.</div>
                </div>
            </div>

            <!-- ĐIỀU 6 -->
            <div class="clause">
                <div class="clause-title">Điều 6: Quyền và nghĩa vụ của bên thuê</div>
                <div class="clause-content">
                    <div><strong>Quyền của Bên B:</strong></div>
                    <div class="sub-clause">Được sử dụng nhà theo đúng mục đích đã thỏa thuận;</div>
                    <div class="sub-clause">Được yêu cầu Bên A sửa chữa những hư hỏng không do lỗi của mình;</div>
                    <div class="sub-clause">Được gia hạn hợp đồng nếu hai bên thỏa thuận.</div>

                    <div style="margin-top: 16px;"><strong>Nghĩa vụ của Bên B:</strong></div>
                    <div class="sub-clause">Thanh toán tiền thuê nhà đúng hạn theo thỏa thuận;</div>
                    <div class="sub-clause">Sử dụng nhà đúng mục đích, giữ gìn và bảo quản tài sản;</div>
                    <div class="sub-clause">Không được chuyển nhượng hợp đồng cho người thứ ba;</div>
                    <div class="sub-clause">Trả nhà trong tình trạng ban đầu khi hết hạn hợp đồng;</div>
                    <div class="sub-clause">Chấp hành đầy đủ các quy định của pháp luật và nội quy khu vực.</div>
                </div>
            </div>

            <!-- ĐIỀU 7 -->
            <div class="clause">
                <div class="clause-title">Điều 7: Chấm dứt hợp đồng</div>
                <div class="clause-content">
                    <div>Hợp đồng chấm dứt trong các trường hợp sau:</div>
                    <div class="sub-clause">Hết thời hạn hợp đồng;</div>
                    <div class="sub-clause">Hai bên thỏa thuận chấm dứt trước hạn;</div>
                    <div class="sub-clause">Bên thuê vi phạm nghiêm trọng nghĩa vụ trong hợp đồng;</div>
                    <div class="sub-clause">Các trường hợp khác theo quy định của pháp luật.</div>
                </div>
            </div>

            <!-- ĐIỀU 8 -->
            <div class="clause">
                <div class="clause-title">Điều 8: Giải quyết tranh chấp</div>
                <div class="clause-content">
                    <div>Trong quá trình thực hiện hợp đồng, nếu có tranh chấp phát sinh, hai bên cùng
                    bàn bạc giải quyết trên tinh thần thiện chí, hợp tác.</div>
                    <div>Trường hợp không thể thỏa thuận được, tranh chấp sẽ được giải quyết tại
                    Tòa án nhân dân có thẩm quyền theo quy định của pháp luật.</div>
                </div>
            </div>

            <!-- ĐIỀU 9 -->
            <div class="clause">
                <div class="clause-title">Điều 9: Điều khoản chung</div>
                <div class="clause-content">
                    <div>Hợp đồng này được lập thành 02 (hai) bản có giá trị pháp lý như nhau,
                    mỗi bên giữ 01 bản.</div>
                    <div>Mọi sửa đổi, bổ sung hợp đồng phải được lập thành văn bản và có
                    sự thỏa thuận của cả hai bên.</div>
                    <div>Hợp đồng có hiệu lực kể từ ngày ký.</div>
                </div>
            </div>

            <div class="decorative-line"></div>

            <!-- CHỮ KÝ -->
            <div class="signature-section">
                <table class="signature-table">
                    <tr>
                        <td class="signature-cell">
                            <div class="signature-title">BÊN CHO THUÊ</div>
                            <div class="signature-note">(Ký, ghi rõ họ tên)</div>
                            
                        </td>
                        <td class="signature-cell">
                            <div class="signature-title">BÊN THUÊ</div>
                            <div class="signature-note">(Ký, ghi rõ họ tên)</div>
                            
                        </td>
                    </tr>
                </table>
            </div>

            <!-- FOOTER -->
            <div class="footer">
                <div>Hợp đồng được lập bởi hệ thống Nhà Trọ Xanh</div>
                <div>Ngày tạo: ${contractSignDate} | Mã hợp đồng: ${Date.now()}</div>
            </div>
        </div>
    </div>
</body>
</html>`;

    return contractHtml;
}





function captureContractPreviewForPdf() {
    const $previewContainer = $('#preview-container');

    if ($previewContainer.length === 0 || $previewContainer.html().trim() === '') {
        throw new Error('Vui lòng xem trước hợp đồng trước khi tạo PDF!');
    }

    // LẤY DỮ LIỆU TỪ FORM
    const landlordName = $('#owner-name').val() || '................................';
    const landlordDob = $('#owner-dob').val() ? formatDate($('#owner-dob').val()) : '................................';
    const landlordId = $('#owner-id').val() || '................................';
    const landlordIdDate = $('#owner-id-date').val() ? formatDate($('#owner-id-date').val()) : '................................';
    const landlordIdPlace = $('#owner-id-place').val() || '................................';
    const landlordPhone = $('#owner-phone').val() || '................................';

    // XÂY DỰNG ĐỊA CHỈ CHỦ TRỌ
    const ownerStreet = $('#owner-street').val() || '';
    const ownerWard = $('#owner-ward option:selected').text() || '';
    const ownerDistrict = $('#owner-district option:selected').text() || '';
    const ownerProvince = $('#owner-province option:selected').text() || '';
    const landlordAddress = [ownerStreet, ownerWard, ownerDistrict, ownerProvince]
        .filter(item => item && item !== 'Chọn...' && item.trim() !== '')
        .join(', ') || '................................';

    // THÔNG TIN NGƯỜI THUÊ
    const tenantName = $('#tenant-name').val() || '................................';
    const tenantDob = $('#tenant-dob').val() ? formatDate($('#tenant-dob').val()) : '................................';
    const tenantId = $('#tenant-id').val() || '................................';
    const tenantIdDate = $('#tenant-id-date').val() ? formatDate($('#tenant-id-date').val()) : '................................';
    const tenantIdPlace = $('#tenant-id-place').val() || '................................';
    const tenantPhone = $('#tenant-phone').val() || '................................';

    // XÂY DỰNG ĐỊA CHỈ NGƯỜI THUÊ
    const tenantStreet = $('#tenant-street').val() || '';
    const tenantWard = $('#tenant-ward option:selected').text() || '';
    const tenantDistrict = $('#tenant-district option:selected').text() || '';
    const tenantProvince = $('#tenant-province option:selected').text() || '';
    const tenantAddress = [tenantStreet, tenantWard, tenantDistrict, tenantProvince]
        .filter(item => item && item !== 'Chọn...' && item.trim() !== '')
        .join(', ') || '................................';

    // DANH SÁCH NGƯỜI Ở
    const residents = $('#residents-list .resident-item').map(function () {
        const name = $(this).find('.resident-name').text().trim();
        return name ? name : null;
    }).get().filter(name => name).join(', ') || '';

    // TIỆN ÍCH
    const amenities = $('.nha-tro-amenities input:checked').map(function () {
        return $(this).next('label').text().trim();
    }).get().join(', ') || 'Không có tiện ích đặc biệt';

    // THÔNG TIN PHÒNG TRỌ
    const roomNumber = $('#room-number').val() || '................................';
    const roomArea = $('#room-area').val() || '................................';

    // XÂY DỰNG ĐỊA CHỈ PHÒNG
    const roomStreet = $('#room-street').val() || '';
    const roomWard = $('#room-ward option:selected').text() || '';
    const roomDistrict = $('#room-district option:selected').text() || '';
    const roomProvince = $('#room-province option:selected').text() || '';
    const propertyAddress = [roomStreet, roomWard, roomDistrict, roomProvince]
        .filter(item => item && item !== 'Chọn...' && item.trim() !== '')
        .join(', ') || '................................';

    // THÔNG TIN HỢP ĐỒNG
    const contractDuration = $('#contract-duration').val() || '................................';
    const startDate = $('#start-date').val() ? formatDate($('#start-date').val()) : '................................';

    // TÍNH NGÀY KẾT THÚC
    let endDate = '................................';
    if ($('#start-date').val() && contractDuration) {
        const start = new Date($('#start-date').val());
        start.setMonth(start.getMonth() + parseInt(contractDuration));
        endDate = formatDate(start.toISOString().split('T')[0]);
    }

    const monthlyRent = $('#rent-price').val() ?
        $('#rent-price').val().replace(' VND', '') : '................................';

    // TÍNH TIỀN CỌC
    const depositMonths = $('#deposit-months').val() || '2';
    const rentPrice = parseInt($('#rent-price-hidden').val()) || 0;
    const depositAmount = rentPrice * parseFloat(depositMonths);
    const deposit = depositAmount > 0 ?
        new Intl.NumberFormat('vi-VN').format(depositAmount) : '................................';

    const paymentDate = $('#payment-date').val() || '................................';
    const paymentMethod = $('#payment-method option:selected').text() || 'Tiền mặt';

    // Ngày ký hợp đồng
    const today = new Date();
    const contractSignDate = formatDate(today.toISOString().split('T')[0]);

    // HTML CHUẨN PHÁP LÝ VIỆT NAM
    const contractHtml = `<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Hợp đồng thuê nhà trọ - ${tenantName}</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: "Liberation Serif", "DejaVu Serif", "Times New Roman", "Noto Serif", serif;
            font-size: 16px;
            line-height: 1.6;
            color: #1a1a1a;
            background: #ffffff;
            padding: 50px;
            max-width: 900px;
            margin: 0 auto;
            min-height: 100vh;
            background-image:
                linear-gradient(to right, #f8f9fa 0%, #ffffff 2%, #ffffff 98%, #f8f9fa 100%);
        }

        .contract-wrapper {
            background: #ffffff;
            border-radius: 8px;
            padding: 40px;
            box-shadow: 0 8px 25px rgba(44, 90, 160, 0.1);
            position: relative;
            overflow: hidden;
        }

        .contract-header {
            text-align: center;
            /* margin-bottom: 40px; */
            position: relative;
            padding: 20px 0;
        }

        .country-info {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 14px;
            font-weight: 600;
            text-transform: uppercase;
            line-height: 1.4;
            margin-bottom: 25px;
            color: #2c5aa0;
            letter-spacing: 0.5px;
        }

        .country-info hr {
            width: 120px;
            height: 2px;
            background: #2c5aa0;
            border: none;
            margin: 10px auto;
        }

        .contract-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 24px;
            font-weight: 700;
            text-transform: uppercase;
            margin: 25px 0;
            letter-spacing: 1.5px;
            color: #1a365d;
            position: relative;
            padding: 15px 30px;
            /* background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
            border: 1px solid #cbd5e0;
            border-radius: 6px;
            box-shadow: 0 2px 8px rgba(26, 54, 93, 0.1); */
        }

        .contract-content {
            text-align: justify;
            line-height: 1.7;
            font-size: 16px;
        }

        .contract-intro {
            margin-bottom: 35px;
            text-indent: 40px;
            font-size: 17px;
            padding: 20px;
            background: #f8fafc;
            border-left: 4px solid #2c5aa0;
            border-radius: 0 6px 6px 0;
            box-shadow: 0 2px 4px rgba(44, 90, 160, 0.05);
        }

        .party-section {
            margin-bottom: 35px;
            padding: 25px;
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.03);
            position: relative;
        }

        .party-section::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            height: 3px;
            background: linear-gradient(90deg, #2c5aa0 0%, #4299e1 100%);
            border-radius: 8px 8px 0 0;
        }

        .party-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 16px;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 18px;
            color: #2c5aa0;
            padding-bottom: 8px;
            border-bottom: 2px solid #e2e8f0;
            letter-spacing: 0.3px;
        }

        .party-info {
            margin-left: 20px;
            line-height: 1.8;
            font-size: 16px;
            color: #2d3748;
        }

        .party-info strong {
            color: #1a365d;
            font-weight: 600;
        }

        .terms-section {
            margin: 40px 0;
        }

        .terms-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-size: 20px;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 20px;
            text-align: center;
            color: #1a365d;
            padding: 15px 0;
            background: linear-gradient(135deg, #2c5aa0 0%, #3d7bd4 100%);
            color: white;
            border-radius: 6px;
            box-shadow: 0 4px 12px rgba(44, 90, 160, 0.2);
            letter-spacing: 1px;
        }

        .term-item {
            margin-bottom: 25px;
            padding: 20px;
            background: #ffffff;
            border: 1px solid #e2e8f0;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.02);
            position: relative;
        }

        .term-item::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 4px;
            background: linear-gradient(180deg, #2c5aa0 0%, #4299e1 100%);
            border-radius: 8px 0 0 8px;
        }

        .term-number {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-weight: 700;
            color: #2c5aa0;
            font-size: 17px;
            display: block;
            margin-bottom: 10px;
            padding-bottom: 8px;
            border-bottom: 1px solid #e2e8f0;
        }

        .sub-term {
            margin: 12px 0 12px 30px;
            line-height: 1.7;
            padding: 8px 15px;
            background: #f7fafc;
            border-radius: 4px;
            border-left: 3px solid #cbd5e0;
            font-size: 16px;
        }

        .signature-section {
            margin-top: 50px;
            page-break-inside: avoid;
            padding: 30px;
            background: #f8fafc;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
        }

        .signature-date {
            text-align: right;
            margin-bottom: 50px;
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-style: italic;
            font-size: 17px;
            font-weight: 600;
            color: #2c5aa0;
            padding: 15px;
            background: #ffffff;
            border-radius: 6px;
            box-shadow: 0 2px 4px rgba(44, 90, 160, 0.1);
        }

        .signature-parties {
            display: flex;
            justify-content: space-between;
            margin-top: 40px;
            gap: 40px;
        }

        .signature-party {
            width: 45%;
            text-align: center;
            padding: 20px;
            background: #ffffff;
            border-radius: 8px;
            border: 1px solid #e2e8f0;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
        }

        .signature-title {
            font-family: "Liberation Sans", "DejaVu Sans", "Arial", "Segoe UI", sans-serif;
            font-weight: 700;
            text-transform: uppercase;
            margin-bottom: 15px;
            font-size: 17px;
            color: #2c5aa0;
            padding: 10px;
            background: linear-gradient(135deg, #f7fafc 0%, #edf2f7 100%);
            border-radius: 4px;
            letter-spacing: 0.5px;
        }

        .signature-subtitle {
            font-size: 12px;
            margin-bottom: 80px;
            font-style: italic;
            color: #4a5568;
        }

        .signature-name {
            font-weight: 600;
            font-size: 16px;
            color: #1a365d;
            border-top: 2px solid #2c5aa0;
            padding-top: 10px;
            margin-top: 20px;
        }

        /* Decorative elements */
        .decorative-line {
            height: 2px;
            background: linear-gradient(90deg, transparent 0%, #2c5aa0 20%, #4299e1 50%, #2c5aa0 80%, transparent 100%);
            margin: 25px 0;
            border-radius: 1px;
        }

        /* Enhanced typography */
        strong {
            font-weight: 600;
            color: #1a365d;
        }

        em {
            color: #2c5aa0;
            font-style: italic;
        }

        /* Improved spacing for better readability */
        br + br {
            line-height: 2;
        }

        @media (max-width: 768px) {
            body {
                padding: 20px;
                font-size: 14px;
            }

            .contract-wrapper {
                padding: 25px;
                margin: 0;
            }

            .contract-title {
                font-size: 20px;
                padding: 12px 20px;
            }

            .signature-parties {
                flex-direction: row;
                gap: 20px;
            }

            .signature-party {
                width: 100%;
                margin-bottom: 30px;
            }
        }

        @media print {
            body {
                padding: 15mm;
                background: white;
                box-shadow: none;
            }

            .contract-wrapper {
                box-shadow: none;
            }

            .signature-section {
                page-break-inside: avoid;
            }

            .term-item {
                page-break-inside: avoid;
                margin-bottom: 15px;
            }

            * {
                -webkit-print-color-adjust: exact !important;
                color-adjust: exact !important;
            }
        }

        /* Custom scrollbar for better aesthetics */
        ::-webkit-scrollbar {
            width: 8px;
        }

        ::-webkit-scrollbar-track {
            background: #f1f1f1;
        }

        ::-webkit-scrollbar-thumb {
            background: #2c5aa0;
            border-radius: 4px;
        }

        ::-webkit-scrollbar-thumb:hover {
            background: #1e3a8a;
        }
    </style>
</head>
<body>
    <div class="contract-wrapper">
        <!-- HEADER HỢP ĐỒNG -->
        <div class="contract-header">
            <div class="country-info">
                CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM<br>
                Độc lập - Tự do - Hạnh phúc
                <hr>
            </div>
            <div class="contract-title">
                HỢP ĐỒNG THUÊ NHÀ TRỌ
            </div>
        </div>

        <!-- NỘI DUNG HỢP ĐỒNG -->
        <div class="contract-content">
            <div class="contract-intro">
                Căn cứ vào Bộ luật Dân sự số 91/2015/QH13 ngày 24/11/2015; Luật Nhà ở số 65/2014/QH13 ngày 25/11/2014; Nghị định số 99/2015/NĐ-CP ngày 20/10/2015 của Chính phủ quy định chi tiết và hướng dẫn thi hành một số điều của Luật Nhà ở; và các quy định pháp luật có liên quan khác. Hôm nay, ngày ${new Date().toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' })}, tại ${formData.contractLocation}, chúng tôi gồm có:
            </div>

            <div class="decorative-line"></div>

            <!-- BÊN CHO THUÊ -->
            <div class="party-section">
                <div class="party-title">Bên cho thuê nhà trọ (Bên A):</div>
                <div class="party-info">
                    <strong>Họ và tên:</strong> ${formData.landlordName}<br>
                    ${formData.landlordBirth ? `<strong>Ngày sinh:</strong> ${formatDate(formData.landlordBirth)}<br>` : ''}
                    ${formData.landlordId ? `<strong>Số CMND/CCCD:</strong> ${formData.landlordId}` : ''}${formData.landlordIdDate ? `, cấp ngày ${formatDate(formData.landlordIdDate)}` : ''}${formData.landlordIdPlace ? `, tại ${formData.landlordIdPlace}` : ''}<br>
                    ${formData.landlordAddress ? `<strong>Địa chỉ thường trú:</strong> ${formData.landlordAddress}<br>` : ''}
                    ${formData.landlordPhone ? `<strong>Số điện thoại:</strong> ${formData.landlordPhone}` : ''}
                </div>
            </div>

            <!-- BÊN THUÊ -->
            <div class="party-section">
                <div class="party-title">Bên thuê nhà trọ (Bên B):</div>
                <div class="party-info">
                    <strong>Họ và tên:</strong> ${formData.tenantName}<br>
                    ${formData.tenantBirth ? `<strong>Ngày sinh:</strong> ${formatDate(formData.tenantBirth)}<br>` : ''}
                    ${formData.tenantId ? `<strong>Số CMND/CCCD:</strong> ${formData.tenantId}` : ''}${formData.tenantIdDate ? `, cấp ngày ${formatDate(formData.tenantIdDate)}` : ''}${formData.tenantIdPlace ? `, tại ${formData.tenantIdPlace}` : ''}<br>
                    ${formData.tenantAddress ? `<strong>Địa chỉ thường trú:</strong> ${formData.tenantAddress}<br>` : ''}
                    ${formData.tenantPhone ? `<strong>Số điện thoại:</strong> ${formData.tenantPhone}` : ''}
                </div>
            </div>

            <div class="decorative-line"></div>

            <div style="text-align: center; margin: 30px 0; font-size: 15px; font-style: italic; color: #2c5aa0; font-weight: 600;">
                Hai bên cùng thỏa thuận ký kết hợp đồng thuê nhà trọ với các điều khoản sau:
            </div>

            <!-- ĐIỀU KHOẢN HỢP ĐỒNG -->
            <div class="terms-section">
                <div class="terms-title">CÁC ĐIỀU KHOẢN HỢP ĐỒNG</div>
                <div class="term-item">
                    <span class="term-number">Điều 1: Đối tượng và mục đích thuê</span>
                    Bên A đồng ý cho Bên B thuê căn phòng trọ tại địa chỉ: <strong>${formData.roomAddress || '[Địa chỉ phòng trọ]'}</strong>, với diện tích sử dụng <strong>${formData.roomArea || '[Diện tích]'} m²</strong>. Phòng trọ được trang bị đầy đủ các tiện nghi cơ bản phục vụ cho mục đích sinh hoạt của Bên B.
                    <div class="sub-term">1.1. Mục đích thuê: Sử dụng để ở. Bên B cam kết không sử dụng phòng trọ vào mục đích kinh doanh, buôn bán hoặc bất kỳ hoạt động nào trái với quy định pháp luật.</div>
                    <div class="sub-term">1.2. Bên B chịu trách nhiệm giữ gìn tài sản, sử dụng phòng trọ đúng mục đích và bảo quản các trang thiết bị như tài sản của chính mình.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 2: Thời hạn thuê</span>
                    Thời hạn thuê là <strong>${formData.contractDuration || '12'} tháng</strong>, từ ngày <strong>${formatDate(formData.startDate) || '[Ngày bắt đầu]'}</strong> đến ngày <strong>${(() => {
        if (formData.startDate && formData.contractDuration) {
            const start = new Date(formData.startDate);
            const end = new Date(start.setMonth(start.getMonth() + parseInt(formData.contractDuration)));
            return formatDate(end);
        }
        return '[Ngày kết thúc]';
    })()}</strong>.
                    <div class="sub-term">2.1. Khi hết thời hạn thuê, nếu hai bên có nhu cầu tiếp tục, hợp đồng mới sẽ được ký kết theo thỏa thuận.</div>
                    <div class="sub-term">2.2. Bên B phải thông báo bằng văn bản cho Bên A ít nhất 30 ngày trước khi hết hạn hợp đồng nếu muốn gia hạn.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 3: Giá thuê và phương thức thanh toán</span>
                    Giá thuê phòng trọ: <strong>${formatMoney(formData.roomPrice)}</strong> (Bằng chữ: <strong>${numberToWords(formData.roomPrice)}</strong>).
                    <div class="sub-term">3.1. Tiền đặt cọc: <strong>${formatMoney(formData.deposit)}</strong> (Bằng chữ: <strong>${numberToWords(formData.deposit)}</strong>), được hoàn trả khi kết thúc hợp đồng nếu không có vi phạm hoặc thiệt hại.</div>
                    <div class="sub-term">3.2. Bên B thanh toán tiền thuê vào ngày <strong>${formData.paymentDate || '1'}</strong> hàng tháng bằng hình thức chuyển khoản hoặc tiền mặt.</div>
                    <div class="sub-term">3.3. Trường hợp chậm thanh toán quá 7 ngày, Bên B chịu phí phạt <strong>0,5% giá trị tiền thuê/ngày</strong>.</div>
                    <div class="sub-term">3.4. Tiền đặt cọc không được sử dụng để thanh toán tiền thuê trong thời gian hợp đồng còn hiệu lực.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 4: Các khoản phí phát sinh</span>
                    Ngoài tiền thuê phòng, Bên B chịu trách nhiệm thanh toán các khoản phí sau:
                    ${formData.electricPrice ? `<div class="sub-term">4.1. Tiền điện: <strong>${formatMoney(formData.electricPrice)}/kWh</strong> (theo chỉ số công tơ điện).</div>` : ''}
                    ${formData.waterPrice ? `<div class="sub-term">4.2. Tiền nước: <strong>${formatMoney(formData.waterPrice)}/m³</strong> (theo chỉ số đồng hồ nước).</div>` : ''}
                    ${formData.wifiPrice ? `<div class="sub-term">4.3. Phí Internet/Wifi: <strong>${formatMoney(formData.wifiPrice)}/tháng</strong>.</div>` : ''}
                    ${formData.cleaningPrice ? `<div class="sub-term">4.4. Phí vệ sinh chung: <strong>${formatMoney(formData.cleaningPrice)}/tháng</strong>.</div>` : ''}
                    ${formData.parkingPrice ? `<div class="sub-term">4.5. Phí gửi xe: <strong>${formatMoney(formData.parkingPrice)}/tháng</strong>.</div>` : ''}
                    ${formData.otherFees ? `<div class="sub-term">4.6. Các phí khác: ${formData.otherFees}.</div>` : ''}
                    <div class="sub-term">4.7. Các khoản phí này được thanh toán cùng thời điểm với tiền thuê phòng hàng tháng.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 5: Quyền và nghĩa vụ của Bên A</span>
                    <strong>Quyền của Bên A:</strong>
                    <div class="sub-term">5.1. Nhận tiền thuê và các khoản phí đúng hạn theo thỏa thuận.</div>
                    <div class="sub-term">5.2. Yêu cầu Bên B sử dụng phòng trọ đúng mục đích, bảo quản tài sản và tuân thủ nội quy.</div>
                    <div class="sub-term">5.3. Kiểm tra định kỳ tình trạng phòng trọ (thông báo trước ít nhất 24 giờ).</div>
                    <div class="sub-term">5.4. Đơn phương chấm dứt hợp đồng nếu Bên B vi phạm nghiêm trọng các điều khoản.</div>
                    <strong>Nghĩa vụ của Bên A:</strong>
                    <div class="sub-term">5.5. Giao phòng trọ đúng thời hạn, đảm bảo tình trạng sử dụng tốt.</div>
                    <div class="sub-term">5.6. Đảm bảo quyền sử dụng ổn định của Bên B trong thời gian hợp đồng.</div>
                    <div class="sub-term">5.7. Sửa chữa các hư hỏng do hao mòn tự nhiên hoặc lỗi kỹ thuật không do Bên B gây ra.</div>
                    <div class="sub-term">5.8. Cung cấp hóa đơn, biên lai minh bạch cho các khoản phí điện, nước và dịch vụ.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 6: Quyền và nghĩa vụ của Bên B</span>
                    <strong>Quyền của Bên B:</strong>
                    <div class="sub-term">6.1. Sử dụng phòng trọ theo đúng mục đích đã thỏa thuận.</div>
                    <div class="sub-term">6.2. Yêu cầu Bên A sửa chữa kịp thời các hư hỏng không do lỗi của Bên B.</div>
                    <div class="sub-term">6.3. Nhận lại tiền đặt cọc khi kết thúc hợp đồng nếu không vi phạm các điều khoản.</div>
                    <strong>Nghĩa vụ của Bên B:</strong>
                    <div class="sub-term">6.4. Thanh toán đầy đủ và đúng hạn các khoản tiền thuê, phí dịch vụ.</div>
                    <div class="sub-term">6.5. Giữ gìn, bảo quản tài sản và sử dụng phòng trọ đúng mục đích.</div>
                    <div class="sub-term">6.6. Tuân thủ nội quy khu nhà trọ và các quy định pháp luật.</div>
                    <div class="sub-term">6.7. Bồi thường thiệt hại nếu làm hư hỏng tài sản do lỗi của mình.</div>
                    <div class="sub-term">6.8. Thông báo ngay cho Bên A khi phát hiện hư hỏng hoặc sự cố.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 7: Các hành vi bị cấm</span>
                    Bên B không được phép thực hiện các hành vi sau:
                    <div class="sub-term">7.1. Sử dụng phòng trọ để kinh doanh, buôn bán hoặc các hoạt động trái pháp luật.</div>
                    <div class="sub-term">7.2. Chuyển nhượng, cho thuê lại phòng trọ dưới bất kỳ hình thức nào.</div>
                    <div class="sub-term">7.3. Gây rối, làm mất trật tự hoặc ảnh hưởng đến cư dân xung quanh.</div>
                    <div class="sub-term">7.4. Tự ý sửa chữa, cải tạo kết cấu phòng trọ mà không có sự đồng ý của Bên A.</div>
                    <div class="sub-term">7.5. Nuôi thú cưng hoặc tổ chức các hoạt động trái pháp luật (ma túy, cờ bạc, mại dâm).</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 8: Chấm dứt hợp đồng</span>
                    Hợp đồng chấm dứt trong các trường hợp sau:
                    <div class="sub-term">8.1. Hết thời hạn hợp đồng và không có thỏa thuận gia hạn.</div>
                    <div class="sub-term">8.2. Hai bên thỏa thuận chấm dứt hợp đồng trước thời hạn bằng văn bản.</div>
                    <div class="sub-term">8.3. Bên B vi phạm nghiêm trọng các điều khoản hợp đồng (bao gồm chậm thanh toán quá 15 ngày).</div>
                    <div class="sub-term">8.4. Khi chấm dứt hợp đồng, Bên B phải bàn giao phòng trọ trong tình trạng ban đầu (trừ hao mòn tự nhiên).</div>
                    <div class="sub-term">8.5. Bên A hoàn trả tiền đặt cọc trong vòng 7 ngày sau khi kiểm tra và xác nhận không có thiệt hại.</div>
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 9: Giải quyết tranh chấp</span>
                    Mọi tranh chấp phát sinh từ hợp đồng này sẽ được giải quyết thông qua thương lượng, hòa giải. Nếu không đạt được thỏa thuận, tranh chấp sẽ được đưa ra Tòa án nhân dân có thẩm quyền tại ${formData.contractLocation} để giải quyết theo quy định của pháp luật Việt Nam.
                </div>

                <div class="term-item">
                    <span class="term-number">Điều 10: Hiệu lực hợp đồng</span>
                    Hợp đồng này có hiệu lực từ ngày ký và được lập thành 02 (hai) bản, mỗi bên giữ 01 bản, có giá trị pháp lý như nhau. Mọi sửa đổi, bổ sung hợp đồng phải được lập thành văn bản và có sự đồng ý của cả hai bên.
                </div>
            </div>

            <div class="decorative-line"></div>

            <!-- CHỮ KÝ -->
            <div class="signature-section">
                <div class="signature-date">
                    <strong><em>${formData.contractLocation}, ngày ${new Date().getDate()} tháng ${new Date().getMonth() + 1} năm ${new Date().getFullYear()}</em></strong>
                </div>

                <div class="signature-parties">
                    <div class="signature-party">
                        <div class="signature-title">BÊN CHO THUÊ</div>
                        <div class="signature-subtitle">(Ký và ghi rõ họ tên)</div>
                        <div class="signature-name">${formData.landlordName}</div>
                    </div>

                    <div class="signature-party">
                        <div class="signature-title">BÊN THUÊ</div>
                        <div class="signature-subtitle">(Ký và ghi rõ họ tên)</div>
                        <div class="signature-name">${formData.tenantName}</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>`;

    return contractHtml;
}



// Hàm format ngày theo chuẩn Việt Nam
function formatDate(dateString) {
    if (!dateString) return 'Chưa nhập';
    const date = new Date(dateString);
    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    return `${day}/${month}/${year}`;
}

// Hàm xây dựng địa chỉ đầy đủ
function buildFullAddress(street, ward, district, province) {
    const parts = [];
    if (street && street.trim()) parts.push(street.trim());
    if (ward && ward.trim() && !ward.includes('Chọn')) parts.push(ward.trim());
    if (district && district.trim() && !district.includes('Chọn')) parts.push(district.trim());
    if (province && province.trim() && !province.includes('Chọn')) parts.push(province.trim());

    return parts.length > 0 ? parts.join(', ') : 'Chưa nhập địa chỉ';
}





// ✅ CẬP NHẬT NÚT DOWNLOAD PDF
$('#btn-download-pdf').on('click', function () {
    console.log('📄 Downloading PDF...');

    if (!validateContractForm()) {
        Swal.fire({
            icon: 'warning',
            title: 'Thiếu thông tin!',
            text: 'Vui lòng điền đầy đủ thông tin hợp đồng trước khi tải PDF!',
            confirmButtonText: 'OK',
            confirmButtonColor: '#f39c12'
        });
        return;
    }

    // Hiển thị loading
    Swal.fire({
        title: 'Đang tạo PDF...',
        html: 'Vui lòng chờ trong giây lát',
        allowOutsideClick: false,
        didOpen: () => {
            Swal.showLoading();
        }
    });

    try {
        const contractHtml = captureContractPreviewForPdf();

        $.ajax({
            url: '/api/contracts/generate-pdf',
            method: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({
                contractHtml: contractHtml,
                fileName: `HopDong_${$('#tenant-name').val().replace(/\s+/g, '_')}_${Date.now()}`
            }),
            xhrFields: {
                responseType: 'blob'
            },
            success: function (data, status, xhr) {
                // Tạo download
                const blob = new Blob([data], { type: 'application/pdf' });
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `HopDong_${$('#tenant-name').val().replace(/\s+/g, '_')}.pdf`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);

                // Thông báo thành công
                Swal.fire({
                    icon: 'success',
                    title: 'Tải PDF thành công! 📄',
                    html: `
                        <div class="text-start">
                            <p>✅ File PDF đã được tạo và tải xuống!</p>
                            <p>📁 <strong>Tên file:</strong> HopDong_${$('#tenant-name').val().replace(/\s+/g, '_')}.pdf</p>
                            <p>👤 <strong>Người thuê:</strong> ${$('#tenant-name').val()}</p>
                        </div>
                    `,
                    confirmButtonText: 'OK',
                    confirmButtonColor: '#28a745'
                });
            },
            error: function (xhr, status, error) {
                console.error('❌ Error:', error);
                Swal.fire({
                    icon: 'error',
                    title: 'Lỗi tạo PDF!',
                    text: 'Có lỗi xảy ra khi tạo file PDF. Vui lòng thử lại!',
                    confirmButtonText: 'Thử lại',
                    confirmButtonColor: '#e74c3c'
                });
            }
        });

    } catch (error) {
        Swal.fire({
            icon: 'error',
            title: 'Lỗi!',
            text: error.message,
            confirmButtonText: 'OK',
            confirmButtonColor: '#e74c3c'
        });
    }
});


$('#btn-send-email').on('click', function () {
    const $thisButton = $(this);
    if ($thisButton.prop('disabled')) {
        return;
    }
    $thisButton.prop('disabled', true);
    if (!validateContractForm()) {
        Swal.fire({
            icon: 'warning',
            title: 'Thiếu thông tin!',
            text: 'Vui lòng điền đầy đủ thông tin hợp đồng trước khi gửi email!',
        });
        $thisButton.prop('disabled', false);
        return;
    }

    const tenantEmail = $('#tenant-email').val();
    if (!tenantEmail || !tenantEmail.includes('@')) {
        Swal.fire({
            icon: 'error',
            title: 'Email không hợp lệ!',
            text: 'Vui lòng nhập email hợp lệ cho người thuê!',
        }).then(() => {
            $('#tenant-email').focus();
        });
        $thisButton.prop('disabled', false);
        return;
    }
    Swal.fire({
        title: 'Xác nhận gửi email',
        html: `Bạn có chắc chắn muốn gửi hợp đồng tới email: <strong>${tenantEmail}</strong>?`,
        icon: 'question',
        showCancelButton: true,
        confirmButtonColor: '#3085d6',
        cancelButtonColor: '#d33',
        confirmButtonText: 'Có, gửi ngay!',
        cancelButtonText: 'Hủy',
        showLoaderOnConfirm: true,
        preConfirm: () => {
            return sendContractEmail(); // Gọi hàm AJAX để gửi
        },
        allowOutsideClick: () => !Swal.isLoading()
    }).then((result) => {
        // Kích hoạt lại nút bấm sau khi hộp thoại Swal đóng lại
        $thisButton.prop('disabled', false);
    });
});
function validateContractForm() {
    const requiredFields = [
        '#tenant-name', '#tenant-phone', '#tenant-id',
        '#owner-name', '#owner-phone', '#owner-id',
        '#hostelSelect', '#roomSelect', '#rent-price',
        '#tenant-email', '#room-number', '#start-date'
    ];
    for (let field of requiredFields) {
        if (!$(field).val() || $(field).val().trim() === '') {
            $(field).focus();
            alert(`❌ Vui lòng điền đầy đủ thông tin tại: ${$(field).attr('id')}`);
            return false;
        }
    }
    const $preview = $('#preview-container');
    if ($preview.length === 0 || $preview.html().trim() === '') {
        alert('❌ Vui lòng nhấn "Xem trước hợp đồng" trước!');
        return false;
    }
    return true;
}


function getContractIdFromUrl() {
    const pathParts = window.location.pathname.split('/');
    return pathParts[pathParts.length - 1] || `CT_${Date.now()}`;
}





function collectContractData() {
    const $roomOption = $('#roomSelect option:selected');
    const $hostelOption = $('#hostelSelect option:selected');
    let roomAddress = '';
    let roomNumber = '';

    if ($roomOption.val() && $hostelOption.val()) {
        roomNumber = $roomOption.data('room-name') || $('#room-number').val() || '';
        const hostelName = $hostelOption.text() || `Ký túc xá ${$hostelOption.val() || 'chưa xác định'}`;
        roomAddress = roomNumber ? `${hostelName}, ${roomNumber}` : '';
    } else {
        roomNumber = $('#room-number').val() || '';
        roomAddress = roomNumber ? `Ký túc xá chưa xác định, ${roomNumber}` : '';
    }

    // Lấy ngày ký hợp đồng, mặc định là ngày hiện tại
    const contractDateInput = $('#contract-date').val();
    const contractDate = contractDateInput || new Date().toISOString().split('T')[0];

    // Đặt giá trị mặc định cho paymentDate và paymentMethod
    const paymentDate = $('#payment-date').val() || '5';
    const paymentMethod = $('#payment-method').val() || 'BANK';

    console.log('🔍 Hostel Name:', $hostelOption.text());
    console.log('🔍 Room Address:', roomAddress);
    console.log('🔍 Room Number:', roomNumber);
    console.log('🔍 Contract Date:', contractDate);
    console.log('🔍 Payment Date:', paymentDate);
    console.log('🔍 Payment Method:', paymentMethod);

    return {
        tenantName: $('#tenant-name').val(),
        tenantPhone: $('#tenant-phone').val(),
        tenantEmail: $('#tenant-email').val(),
        tenantIdCard: $('#tenant-id').val(),
        tenantBirthday: $('#tenant-dob').val(),
        tenantIdDate: $('#tenant-id-date').val(),
        tenantIdPlace: $('#tenant-id-place').val(),
        tenantAddress: buildAddress('tenant'),
        ownerName: $('#owner-name').val(),
        ownerPhone: $('#owner-phone').val(),
        ownerEmail: $('#owner-email').val(),
        ownerIdCard: $('#owner-id').val(),
        ownerBirthday: $('#owner-dob').val(),
        ownerIdDate: $('#owner-id-date').val(),
        ownerIdPlace: $('#owner-id-place').val(),
        ownerAddress: buildAddress('owner'),
        roomNumber: roomNumber,
        roomAddress: roomAddress,
        roomArea: parseFloat($roomOption.data('area')) || $('#room-area').val() || '',
        contractDate: contractDate,
        contractStartDate: $('#start-date').val(),
        contractDuration: $('#contract-duration').val(),
        monthlyRent: parseInt($('#rent-price-hidden').val(), 10) || 0,
        deposit: parseInt($('#deposit-amount').val(), 10) || 0,
        depositMonths: $('#deposit-months').val(),
        paymentMethod: paymentMethod,
        paymentDate: paymentDate,
        terms: $('#terms-conditions').val(),
        recipientEmail: $('#tenant-email').val(),
        recipientName: $('#tenant-name').val()
    };
}

// ✅ HÀM XÂY DỰNG ĐỊA CHỈ
function buildAddress(type) {
    const street = $(`#${type}-street`).val() || '';
    const ward = $(`#${type}-ward option:selected`).text() || '';
    const district = $(`#${type}-district option:selected`).text() || '';
    const province = $(`#${type}-province option:selected`).text() || '';

    return [street, ward, district, province]
        .filter(item => item && item !== 'Chọn...' && item.trim() !== '')
        .join(', ');
}

// Hàm tính tiền đặt cọc
function calculateDeposit() {
    const monthlyRent = parseFloat($('#rent-price-hidden').val()) || 0; // Lấy từ hidden input
    const depositMonths = parseFloat($('#deposit-months').val()) || 0;
    const depositAmount = monthlyRent * depositMonths;

    // Cập nhật trường ẩn để gửi về server
    $('#deposit-amount').val(depositAmount);

    // Cập nhật hiển thị số tiền đặt cọc
    $('#deposit-amount-display').text(
        depositAmount > 0
            ? new Intl.NumberFormat('vi-VN').format(depositAmount) + ' VNĐ'
            : '0 VNĐ'
    );

    // Cập nhật preview hợp đồng
    $('#preview-deposit').text(
        depositAmount > 0
            ? new Intl.NumberFormat('vi-VN').format(depositAmount)
            : '........................'
    );
    $('#preview-deposit-months').text(
        depositMonths > 0
            ? depositMonths
            : '........................'
    );

    return depositAmount;
}

// Cập nhật tiền đặt cọc khi thay đổi giá thuê hoặc số tháng
$(document).ready(function () {
    $('#rent-price, #deposit-months').on('input change', function () {
        calculateDeposit();
    });

    // Khởi tạo giá trị ban đầu khi tải trang
    calculateDeposit();
});

function updateRoomDetails() {
    const $select = $('#roomSelect');
    const $selectedOption = $select.find('option:selected');
    if (!$selectedOption.val()) return;

    // Ưu tiên data-room-name (room.roomName từ backend)
    let roomNumber = $selectedOption.data('room-name') || '';

    // Nếu không, parse từ text
    if (!roomNumber) {
        const selectedText = $selectedOption.text();
        const roomNumberMatch = selectedText.match(/Phòng\s*([a-zA-Z0-9_\u00C0-\u1EF9\s]+)/i); // Hỗ trợ unicode và space
        roomNumber = roomNumberMatch ? roomNumberMatch[1].trim() : 'Chưa đặt tên';
    }

    // Fallback nếu là fallback backend
    if (roomNumber === 'không tên') {
        roomNumber = 'Phòng ' + $selectedOption.val(); // Sử dụng roomId
    }

    console.log('🔍 Parsed Room Number:', roomNumber);

    $('#room-number').val(roomNumber);

    // ✅ THÊM DEBUG ĐÂY: Check raw data-area từ option (in console để xem backend gửi gì)
    console.log('🔍 Data area raw from option:', $selectedOption.data('area'));

    // Cập nhật các trường khác
    const roomData = {
        street: $selectedOption.data('street') || '',
        ward: $selectedOption.data('ward') || '',
        district: $selectedOption.data('district') || '',
        province: $selectedOption.data('province') || 'Đà Nẵng',
        area: parseFloat($selectedOption.data('area')) || 0,  // ✅ THÊM parseFloat để chuyển string '45.0' thành 45 (number), fallback 0 nếu null
        price: $selectedOption.data('price') || '0',
        roomName: $selectedOption.data('room-name') || ''
    };

    console.log('🔍 Updating Room Details:', roomData);

    $('#room-street').val(roomData.street);
    $('#room-area').val(roomData.area);  // Bây giờ val() sẽ set number đúng (không '0')
    $('#rent-price').val(roomData.price);

    updateAddressDropdowns(roomData);
}

// ✅ Hàm cập nhật dropdown địa chỉ
function updateAddressDropdowns(addressData) {
    const updateDropdown = (selectId, value) => {
        if (!value) return;
        const $select = $(`#${selectId}`);
        const $matchedOption = $select.find('option').filter(function () { return $(this).text().toLowerCase().includes(value.toLowerCase()); });
        if ($matchedOption.length) {
            $select.val($matchedOption.first().val());
        } else {
            $select.append(`<option value="${value}">${value}</option>`);
            $select.val(value);
        }
    };
    updateDropdown('room-province', addressData.province);
    updateDropdown('room-district', addressData.district);
    updateDropdown('room-ward', addressData.ward);
}

// ✅ Hàm auto load ở edit mode (sử dụng flag để tránh loop)
function autoLoadRoomAndHostel() {
    if (isLoading) return; // Tránh gọi lặp
    isLoading = true;
    console.log('🔄 Auto-loading for edit mode');
    $('#hostelSelect').val(currentHostelId).trigger('change');
    setTimeout(() => {
        $('#roomSelect').val(currentRoomId).trigger('change');
        isLoading = false;
    }, 500); // Tăng timeout để đảm bảo rooms load xong
}

// ✅ MAIN READY FUNCTION (hợp nhất tất cả)
$(document).ready(function () {
    console.log('🚀 Contract form initialized');

    // HOSTEL CHANGE EVENT
    $('#hostelSelect').on('change', function () {
        const hostelId = $(this).val();
        console.log('🏢 Hostel changed to:', hostelId);
        if (hostelId) {
            loadRoomsByHostel(hostelId);
        } else {
            $('#roomSelect').html('<option value="">-- Chọn phòng trọ --</option>').prop('disabled', true);
        }
    });

    // ROOM CHANGE EVENT
    $('#roomSelect').on('change', function () {
        console.log('🏠 Room changed to:', $(this).val());
        updateRoomDetails();
    });

    // AUTO LOAD FOR EDIT MODE (gọi ở ngoài, không trong callback)
    if (isEditMode && currentHostelId && currentRoomId) {
        autoLoadRoomAndHostel();
    }

    // FORM VALIDATION (giữ nguyên)
    $('#contractForm').on('submit', function (e) {
        const hostelId = $('#hostelSelect').val();
        const roomId = $('#roomSelect').val();
        if (!hostelId || !roomId) {
            e.preventDefault();
            alert('Vui lòng chọn khu trọ và phòng trọ');
        }
    });
});