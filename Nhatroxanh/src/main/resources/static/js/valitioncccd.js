// ✅ CCCD VALIDATOR CLASS - PHIÊN BẢN NGHIÊM NGẶT
class CCCDValidator {
    constructor() {
        // Từ khóa BẮT BUỘC phải có (không thể thiếu)
        // ✅ SỬA LẠI TỪNG KHÓA BẮT BUỘC MẶT SAU
        this.mandatoryKeywords = {
            front: [
                'căn cước công dân',
                'cộng hòa xã hội chủ nghĩa việt nam',
                'họ và tên',
                'ngày sinh'
            ],
            back: [
                // ✅ THÔNG TIN THỰC TẾ MẶT SAU CCCD
                'đặc điểm nhận dạng',    // Tiêu đề chính
                'ngày cấp',              // Ngày cấp CCCD
                'nơi cấp'                // Nơi cấp (thay vì "có giá trị đến")
            ]
        };

        // ✅ THÊM TỪ KHÓA BỔ SUNG MẶT SAU CHÍNH XÁC
        this.supportKeywords = {
            front: [
                'độc lập tự do hạnh phúc',
                'giới tính',
                'quê quán',
                'nơi thường trú',
                'quốc tịch',
                'có giá trị đến'        // Chuyển xuống mặt trước
            ],
            back: [
                // ✅ THÔNG TIN THỰC TẾ CÓ TRÊN MẶT SAU
                'cục trưởng',           // Chức danh
                'cục cảnh sát',         // Tên cơ quan
                'bộ công an',           // Bộ quản lý
                'ngón trỏ trái',        // Vân tay trái
                'ngón trỏ phải',        // Vân tay phải
                'dấu vân tay',          // Khu vực vân tay
                'chữ ký',               // Khu vực chữ ký
                'con dấu',              // Con dấu cơ quan
                'socialist republic'     // Tiếng Anh trên MRZ
            ]
        };

        // Từ khóa CẤM (có thì loại ngay)
        this.blacklistKeywords = [
            // Game keywords
            'gta', 'gtav', 'gtarp', 'roleplay', 'rp', 'game', 'gaming',
            'player', 'online', 'server', 'mod', 'cheat', 'hack',
            'steam', 'epic', 'rockstar', 'launcher',

            // Social media
            'facebook', 'instagram', 'tiktok', 'youtube', 'twitter',
            'snapchat', 'zalo', 'messenger', 'whatsapp',
            'like', 'share', 'comment', 'follow', 'subscribe',

            // Photo/Camera
            'camera', 'photo', 'selfie', 'portrait', 'studio',
            'photographer', 'canon', 'nikon', 'sony', 'iphone',
            'samsung', 'xiaomi', 'oppo', 'vivo',

            // Travel/Tourism
            'travel', 'tour', 'vacation', 'holiday', 'trip',
            'hotel', 'resort', 'beach', 'mountain', 'sea',
            'sapa', 'dalat', 'nhatrang', 'phuquoc', 'halong',

            // Screenshot/Desktop
            'screenshot', 'capture', 'desktop', 'window', 'browser',
            'chrome', 'firefox', 'edge', 'safari', 'opera',
            'windows', 'macos', 'android', 'ios'
        ];

        this.isProcessing = false;
        console.log('🔒 CCCD Validator STRICT MODE initialized');
    }

    async validateCCCDImage(file, side) {
        console.log(`🔍 STRICT validation for ${side} side`);

        if (this.isProcessing) {
            throw new Error('Đang xử lý ảnh khác, vui lòng chờ...');
        }

        this.isProcessing = true;

        try {
            // Bước 1: Kiểm tra file cơ bản
            const basicCheck = this.validateFileBasics(file);
            if (!basicCheck.valid) return basicCheck;

            // Bước 2: Kiểm tra tỷ lệ khung hình NGHIÊM NGẶT
            const aspectCheck = await this.checkCCCDAspectRatio(file);
            if (!aspectCheck.valid) return aspectCheck;

            // Bước 3: OCR và kiểm tra NGHIÊM NGẶT
            const ocrResult = await this.performStrictOCR(file, side);
            return ocrResult;

        } catch (error) {
            console.error('❌ CCCD validation error:', error);
            return {
                valid: false,
                confidence: 0,
                reason: '❌ Lỗi khi xử lý ảnh - Vui lòng thử lại với ảnh CCCD rõ nét',
                detectedText: ''
            };
        } finally {
            this.isProcessing = false;
        }
    }

    validateFileBasics(file) {
        // Kiểm tra kích thước
        if (file.size > 10 * 1024 * 1024) {
            return {
                valid: false,
                confidence: 0,
                reason: '❌ File ảnh quá lớn (tối đa 10MB)',
                detectedText: ''
            };
        }

        if (file.size < 10 * 1024) { // Nhỏ hơn 10KB
            return {
                valid: false,
                confidence: 0,
                reason: '❌ File ảnh quá nhỏ (tối thiểu 10KB)',
                detectedText: ''
            };
        }

        // Kiểm tra định dạng
        const validTypes = ['image/jpeg', 'image/jpg', 'image/png'];
        if (!validTypes.includes(file.type)) {
            return {
                valid: false,
                confidence: 0,
                reason: '❌ Chỉ chấp nhận file JPG hoặc PNG',
                detectedText: ''
            };
        }

        return { valid: true, confidence: 1, reason: 'File OK', detectedText: '' };
    }

    async checkCCCDAspectRatio(file) {
        return new Promise((resolve) => {
            const img = new Image();
            img.onload = () => {
                const aspectRatio = img.width / img.height;

                // 1. Kiểm tra tỷ lệ CCCD (quan trọng nhất)
                if (aspectRatio < 1.48 || aspectRatio > 1.68) {
                    resolve({
                        valid: false,
                        reason: `❌ Không phải tỷ lệ CCCD (${aspectRatio.toFixed(2)})`
                    });
                    return;
                }

                // 2. Kiểm tra kích thước file (thay vì pixel)
                const fileSizeKB = file.size / 1024;

                // Nếu file quá nhỏ (dưới 50KB) mới reject
                if (fileSizeKB < 50) {
                    resolve({
                        valid: false,
                        reason: `❌ File quá nhỏ (${fileSizeKB.toFixed(1)}KB), có thể chất lượng kém`
                    });
                    return;
                }

                // 3. Chỉ cảnh báo pixel thấp, KHÔNG reject
                let confidence = 1.0;
                let warning = '';

                if (img.width < 300 || img.height < 200) {
                    confidence = 0.7;
                    warning = `⚠️ Độ phân giải thấp (${img.width}x${img.height})`;
                }

                resolve({
                    valid: true,
                    confidence: confidence,
                    reason: warning || `✅ CCCD hợp lệ (${img.width}x${img.height})`,
                    fileSize: `${fileSizeKB.toFixed(1)}KB`
                });
            };

            img.onerror = () => {
                resolve({
                    valid: false,
                    reason: '❌ Không thể đọc file ảnh'
                });
            };

            img.src = URL.createObjectURL(file);
        });
    }


    async performStrictOCR(file, side) {
        try {
            console.log('🤖 Starting STRICT OCR...');

            if (typeof Tesseract === 'undefined') {
                throw new Error('Tesseract.js không khả dụng');
            }

            // OCR với cấu hình tối ưu cho CCCD
            const { data: { text } } = await Tesseract.recognize(
                file,
                'vie+eng',
                {
                    logger: m => {
                        if (m.status === 'recognizing text') {
                            const progress = Math.round(m.progress * 100);
                            console.log(`OCR: ${progress}%`);
                            this.updateProgress(progress);
                        }
                    },
                    tessedit_pageseg_mode: 6, // Uniform block of text
                    tessedit_char_whitelist: 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyzÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯưẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ0123456789/.,:-() '
                }
            );

            console.log('📝 OCR text:', text.substring(0, 200) + '...');

            // Kiểm tra NGHIÊM NGẶT
            return this.strictAnalysis(text, side);

        } catch (error) {
            console.error('❌ OCR failed:', error);
            return {
                valid: false,
                confidence: 0,
                reason: '❌ Không thể đọc text từ ảnh - Vui lòng chụp ảnh CCCD rõ nét hơn',
                detectedText: ''
            };
        }
    }

    strictAnalysis(text, side) {
        const normalizedText = text.toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '') // Bỏ dấu
            .replace(/[^\w\s]/g, ' ')       // Bỏ ký tự đặc biệt
            .replace(/\s+/g, ' ');          // Normalize spaces

        console.log('🔍 Normalized text:', normalizedText.substring(0, 200));

        // BƯỚC 1: Kiểm tra BLACKLIST (có từ cấm = loại ngay)
        const blacklistCheck = this.checkBlacklist(normalizedText);
        if (blacklistCheck.found) {
            return {
                valid: false,
                confidence: 0,
                reason: `❌ Không phải ảnh CCCD - Phát hiện: "${blacklistCheck.words.join('", "')}"`,
                detectedText: text.substring(0, 300),
                blacklisted: true
            };
        }

        // BƯỚC 2: Kiểm tra từ khóa BẮT BUỘC
        const mandatoryCheck = this.checkMandatoryKeywords(normalizedText, side);
        if (!mandatoryCheck.valid) {
            return {
                valid: false,
                confidence: mandatoryCheck.confidence,
                reason: `❌ Thiếu từ khóa bắt buộc của CCCD: "${mandatoryCheck.missing.join('", "')}"`,
                detectedText: text.substring(0, 300),
                foundMandatory: mandatoryCheck.found,
                missingMandatory: mandatoryCheck.missing
            };
        }

        // BƯỚC 3: Kiểm tra từ khóa bổ sung
        const supportCheck = this.checkSupportKeywords(normalizedText, side);

        // Tính confidence tổng thể
        const totalConfidence = (mandatoryCheck.confidence * 0.8) + (supportCheck.confidence * 0.2);

        return {
            valid: true,
            confidence: totalConfidence,
            reason: `✅ Ảnh CCCD hợp lệ (${mandatoryCheck.found.length}/${mandatoryCheck.total} bắt buộc + ${supportCheck.found.length}/${supportCheck.total} bổ sung)`,
            detectedText: text.substring(0, 500),
            foundMandatory: mandatoryCheck.found,
            foundSupport: supportCheck.found,
            analysis: {
                mandatoryScore: mandatoryCheck.confidence,
                supportScore: supportCheck.confidence,
                totalScore: totalConfidence
            }
        };
    }

    checkBlacklist(text) {
        const foundWords = [];

        for (const word of this.blacklistKeywords) {
            if (text.includes(word.toLowerCase())) {
                foundWords.push(word);
            }
        }

        return {
            found: foundWords.length > 0,
            words: foundWords
        };
    }

    checkMandatoryKeywords(text, side) {
        // ✅ THÊM BIẾN THỂ OCR CÓ THỂ ĐỌC SAI
        const keywordVariants = {
            front: [
                ['căn cước công dân', 'can cuoc cong dan', 'cccd', 'identity card'],
                ['họ và tên', 'ho va ten', 'ho ten', 'full name'],
                ['ngày sinh', 'ngay sinh', 'date of birth', 'sinh']
            ],
            back: [
                // ✅ BIẾN THỂ CHO MẶT SAU
                ['đặc điểm nhận dạng', 'dac diem nhan dang', 'nhan dang', 'identifying', 'features'],
                ['ngày cấp', 'ngay cap', 'date of issue', 'cap ngay'],
                ['nơi cấp', 'noi cap', 'place of issue', 'issued by', 'cuc canh sat']
            ]
        };

        const variants = keywordVariants[side];
        const found = [];
        const missing = [];

        for (let i = 0; i < variants.length; i++) {
            const variantGroup = variants[i];
            let foundInGroup = false;

            // Kiểm tra từng biến thể trong nhóm
            for (const variant of variantGroup) {
                const normalizedVariant = variant.toLowerCase()
                    .normalize('NFD')
                    .replace(/[\u0300-\u036f]/g, '');

                if (text.includes(normalizedVariant)) {
                    found.push(variantGroup[0]); // Lấy tên gốc
                    foundInGroup = true;
                    break;
                }
            }

            if (!foundInGroup) {
                missing.push(variantGroup[0]);
            }
        }

        const confidence = found.length / variants.length;
        // ✅ GIẢM NGƯỠNG: Chỉ cần 60% thay vì 75%
        const isValid = found.length >= Math.ceil(variants.length * 0.6);

        console.log(`🔒 Mandatory check: ${found.length}/${variants.length} (${(confidence*100).toFixed(1)}%)`);

        return {
            valid: isValid,
            confidence: confidence,
            found: found,
            missing: missing,
            total: variants.length
        };
    }


    checkSupportKeywords(text, side) {
        const keywords = this.supportKeywords[side];
        const found = [];

        for (const keyword of keywords) {
            const normalizedKeyword = keyword.toLowerCase()
                .normalize('NFD')
                .replace(/[\u0300-\u036f]/g, '');

            if (text.includes(normalizedKeyword)) {
                found.push(keyword);
            }
        }

        const confidence = found.length / keywords.length;
        console.log(`➕ Support check: ${found.length}/${keywords.length} (${(confidence*100).toFixed(1)}%)`);

        return {
            confidence: confidence,
            found: found,
            total: keywords.length
        };
    }

    updateProgress(percent) {
        const progressBars = document.querySelectorAll('.cccd-progress-bar');
        progressBars.forEach(bar => {
            bar.style.width = percent + '%';
            bar.textContent = percent + '%';
        });
    }

    showValidationResult(result, inputElement, side) {
        const container = inputElement.closest('.cccd-validation-container');
        const previewDiv = container.querySelector('.nha-tro-image-upload');
        const existingAlert = container.querySelector('.validation-alert');

        if (existingAlert) existingAlert.remove();

        previewDiv.classList.remove('validation-success', 'validation-error');
        previewDiv.classList.add(result.valid ? 'validation-success' : 'validation-error');

        const alertDiv = document.createElement('div');
        alertDiv.className = `alert validation-alert ${result.valid ? 'alert-success' : 'alert-danger'}`;

        const icon = result.valid ? '✅' : '❌';
        const confidencePercent = Math.round(result.confidence * 100);

        let detailsHtml = '';
        if (result.foundMandatory) {
            detailsHtml += `
                <div class="validation-details">
                    <small><strong>Từ khóa bắt buộc tìm thấy:</strong></small><br>
                    ${result.foundMandatory.map(kw => `<span class="keyword-tag mandatory">${kw}</span>`).join('')}
                </div>
            `;
        }
        if (result.foundSupport && result.foundSupport.length > 0) {
            detailsHtml += `
                <div class="validation-details">
                    <small><strong>Từ khóa bổ sung:</strong></small><br>
                    ${result.foundSupport.map(kw => `<span class="keyword-tag support">${kw}</span>`).join('')}
                </div>
            `;
        }
        if (result.blacklisted) {
            detailsHtml += `<div class="text-danger"><small><em>Ảnh chứa nội dung không phải CCCD</em></small></div>`;
        }

        alertDiv.innerHTML = `
            <div class="d-flex justify-content-between align-items-start">
                <div style="flex: 1;">
                    <strong>${icon} ${result.reason}</strong>
                    <div class="validation-info">
                        <small>Độ tin cậy: ${confidencePercent}%</small>
                        <div class="confidence-bar">
                            <div class="confidence-fill ${result.valid ? 'success' : 'danger'}" style="width: ${confidencePercent}%"></div>
                        </div>
                    </div>
                    ${detailsHtml}
                </div>
                <button type="button" class="btn-close ms-2" aria-label="Close"></button>
            </div>
        `;

        container.appendChild(alertDiv);
        alertDiv.querySelector('.btn-close').onclick = () => alertDiv.remove();

        setTimeout(() => {
            if (alertDiv.parentNode) alertDiv.remove();
        }, 30000);

        return result.valid;
    }
}

// Khởi tạo validator NGHIÊM NGẶT
window.cccdValidator = new CCCDValidator();

// Thêm CSS cho styling
const style = document.createElement('style');
style.textContent = `
    .keyword-tag {
        display: inline-block;
        background: #e3f2fd;
        color: #1976d2;
        padding: 2px 6px;
        margin: 1px;
        border-radius: 3px;
        font-size: 11px;
    }
    .keyword-tag.mandatory {
        background: #c8e6c9;
        color: #388e3c;
        font-weight: bold;
    }
    .keyword-tag.support {
        background: #fff3e0;
        color: #f57c00;
    }
    .confidence-fill.success {
        background: linear-gradient(90deg, #4caf50, #8bc34a);
    }
    .confidence-fill.danger {
        background: linear-gradient(90deg, #f44336, #ff9800);
    }
`;
document.head.appendChild(style);


// ✅ CCCD VALIDATION EVENT HANDLERS
$(document).ready(function() {
    console.log('🚀 CCCD Validator initialized');

    // Validation cho ảnh CCCD mặt trước
    $('#cccd-front').on('change', async function(e) {
        await handleCCCDValidation(e, 'front', 'cccd-front-preview');
    });

    // Validation cho ảnh CCCD mặt sau
    $('#cccd-back').on('change', async function(e) {
        await handleCCCDValidation(e, 'back', 'cccd-back-preview');
    });

    // Validation cho người bảo hộ
    $('#newCustomer-cccd-front').on('change', async function(e) {
        await handleCCCDValidation(e, 'front', 'newCustomer-cccd-front-preview');
    });

    $('#newCustomer-cccd-back').on('change', async function(e) {
        await handleCCCDValidation(e, 'back', 'newCustomer-cccd-back-preview');
    });

    // Hàm xử lý validation chung
    async function handleCCCDValidation(event, side, previewId) {
        const file = event.target.files[0];
        const input = event.target;

        if (!file) return;

        console.log(`📸 Processing ${side} CCCD image:`, file.name);

        // Hiển thị preview trước
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById(previewId);
            if (preview) {
                preview.innerHTML = `<img src="${e.target.result}" alt="CCCD ${side}" style="max-width: 100%; max-height: 200px; object-fit: contain; border-radius: 8px;">`;
            }
        };
        reader.readAsDataURL(file);

        // Hiển thị loading state
        const container = input.closest('.cccd-validation-container');
        const progressContainer = container.querySelector('.cccd-progress-container');
        const uploadDiv = container.querySelector('.nha-tro-image-upload');

        // ✅ RESET KHUNG VỀ TRẠNG THÁI ĐANG KIỂM TRA
        uploadDiv.classList.remove('validation-success', 'validation-error');
        uploadDiv.classList.add('cccd-validating');

        progressContainer.style.display = 'block';

        // Reset progress
        const progressBar = container.querySelector('.cccd-progress-bar');
        progressBar.style.width = '0%';
        progressBar.textContent = '0%';

        try {
            // Thực hiện validation
            const result = await window.cccdValidator.validateCCCDImage(file, side);

            // ✅ THAY ĐỔI MÀU KHUNG DỰA TRÊN KẾT QUẢ
            uploadDiv.classList.remove('cccd-validating');

            if (result.valid) {
                // 🟢 KHUNG XANH KHI ĐÚNG
                uploadDiv.classList.add('validation-success');
                uploadDiv.classList.remove('validation-error');
            } else {
                // 🔴 KHUNG ĐỎ KHI SAI
                uploadDiv.classList.add('validation-error');
                uploadDiv.classList.remove('validation-success');
            }

            // Hiển thị thông báo toast
            if (result.valid) {
                Toast.fire({
                    icon: 'success',
                    title: `✅ CCCD ${side === 'front' ? 'mặt trước' : 'mặt sau'} hợp lệ!`,
                    html: `
                    <div class="text-start">
                        <small>Độ tin cậy: ${Math.round(result.confidence * 100)}%</small><br>
                    </div>
                `,
                    timer: 4000
                });
            } else {
                Toast.fire({
                    icon: 'warning',
                    title: `⚠️ CCCD ${side === 'front' ? 'mặt trước' : 'mặt sau'} cần kiểm tra`,
                    text: result.reason,
                    timer: 5000
                });
            }

        } catch (error) {
            console.error('❌ Validation error:', error);

            // 🔴 KHUNG ĐỎ KHI LỖI
            uploadDiv.classList.remove('cccd-validating', 'validation-success');
            uploadDiv.classList.add('validation-error');

            Toast.fire({
                icon: 'error',
                title: 'Lỗi kiểm tra ảnh',
                text: error.message,
                timer: 4000
            });
        } finally {
            // Ẩn loading state
            setTimeout(() => {
                progressContainer.style.display = 'none';
                // ✅ GIỮ NGUYÊN MÀU KHUNG, KHÔNG XÓA
            }, 1000);
        }
    }


    const style = document.createElement('style');
    style.textContent = `
    /* CSS cũ giữ nguyên... */
    .keyword-tag {
        display: inline-block;
        background: #e3f2fd;
        color: #1976d2;
        padding: 2px 6px;
        margin: 1px;
        border-radius: 3px;
        font-size: 11px;
    }
    .keyword-tag.mandatory {
        background: #c8e6c9;
        color: #388e3c;
        font-weight: bold;
    }
    .keyword-tag.support {
        background: #fff3e0;
        color: #f57c00;
    }
    .confidence-fill.success {
        background: linear-gradient(90deg, #4caf50, #8bc34a);
    }
    .confidence-fill.danger {
        background: linear-gradient(90deg, #f44336, #ff9800);
    }

    /* ✅ THÊM CSS CHO KHUNG MÀU */
    .nha-tro-image-upload {
        transition: all 0.3s ease;
        border: 2px solid #ddd;
        border-radius: 8px;
        padding: 10px;
    }

    /* 🟢 KHUNG XANH KHI ĐÚNG */
    .nha-tro-image-upload.validation-success {
        border-color: #28a745 !important;
        background-color: rgba(40, 167, 69, 0.1);
        box-shadow: 0 0 10px rgba(40, 167, 69, 0.3);
    }

    /* 🔴 KHUNG ĐỎ KHI SAI */
    .nha-tro-image-upload.validation-error {
        border-color: #dc3545 !important;
        background-color: rgba(220, 53, 69, 0.1);
        box-shadow: 0 0 10px rgba(220, 53, 69, 0.3);
    }

    /* 🟡 KHUNG VÀNG KHI ĐANG KIỂM TRA */
    .nha-tro-image-upload.cccd-validating {
        border-color: #ffc107 !important;
        background-color: rgba(255, 193, 7, 0.1);
        box-shadow: 0 0 10px rgba(255, 193, 7, 0.3);
    }

    /* Animation cho khung */
    .nha-tro-image-upload.validation-success {
        animation: successPulse 0.6s ease-in-out;
    }

    .nha-tro-image-upload.validation-error {
        animation: errorShake 0.6s ease-in-out;
    }

    @keyframes successPulse {
        0% { transform: scale(1); }
        50% { transform: scale(1.02); }
        100% { transform: scale(1); }
    }

    @keyframes errorShake {
        0%, 100% { transform: translateX(0); }
        25% { transform: translateX(-5px); }
        75% { transform: translateX(5px); }
    }
`;


    // Hàm preview ảnh (giữ lại cho compatibility)
    window.NhaTroContract = {
        previewImage: function(event, previewId) {
            const file = event.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = function(e) {
                    const preview = document.getElementById(previewId);
                    if (preview) {
                        preview.innerHTML = `<img src="${e.target.result}" alt="Preview" style="max-width: 100%; max-height: 200px; object-fit: contain; border-radius: 8px;">`;
                    }
                };
                reader.readAsDataURL(file);
            }
        }
    };
});