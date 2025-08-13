const CCCD_PROVINCE_CODES = {
    "001": "Hà Nội",
    "002": "Hà Giang",
    "004": "Cao Bằng",
    "006": "Bắc Kạn",
    "008": "Tuyên Quang",
    "010": "Lào Cai",
    "011": "Điện Biên",
    "012": "Lai Châu",
    "014": "Sơn La",
    "015": "Yên Bái",
    "017": "Hoà Bình",
    "019": "Thái Nguyên",
    "020": "Lạng Sơn",
    "022": "Quảng Ninh",
    "024": "Bắc Giang",
    "025": "Phú Thọ",
    "026": "Vĩnh Phúc",
    "027": "Bắc Ninh",
    "030": "Hải Dương",
    "031": "Hải Phòng",
    "033": "Hưng Yên",
    "034": "Thái Bình",
    "035": "Hà Nam",
    "036": "Nam Định",
    "037": "Ninh Bình",
    "038": "Thanh Hóa",
    "040": "Nghệ An",
    "042": "Hà Tĩnh",
    "044": "Quảng Bình",
    "045": "Quảng Trị",
    "046": "Thừa Thiên Huế",
    "048": "Đà Nẵng",
    "049": "Quảng Nam",
    "051": "Quảng Ngãi",
    "052": "Bình Định",
    "054": "Phú Yên",
    "056": "Khánh Hòa",
    "058": "Ninh Thuận",
    "060": "Bình Thuận",
    "062": "Kon Tum",
    "064": "Gia Lai",
    "066": "Đắk Lắk",
    "067": "Đắk Nông",
    "068": "Lâm Đồng",
    "070": "Bình Phước",
    "072": "Tây Ninh",
    "074": "Bình Dương",
    "075": "Đồng Nai",
    "077": "Bà Rịa - Vũng Tàu",
    "079": "Hồ Chí Minh",
    "080": "Long An",
    "082": "Tiền Giang",
    "083": "Bến Tre",
    "084": "Trà Vinh",
    "086": "Vĩnh Long",
    "087": "Đồng Tháp",
    "089": "An Giang",
    "091": "Kiên Giang",
    "092": "Cần Thơ",
    "093": "Hậu Giang",
    "094": "Sóc Trăng",
    "095": "Bạc Liêu",
    "096": "Cà Mau",
};
// Phone Network Codes
const PHONE_NETWORK_CODES = {
    "032": "Viettel",
    "033": "Viettel",
    "034": "Viettel",
    "035": "Viettel",
    "036": "Viettel",
    "037": "Viettel",
    "038": "Viettel",
    "039": "Viettel",
    "096": "Viettel",
    "097": "Viettel",
    "098": "Viettel",
    "081": "Vinaphone",
    "082": "Vinaphone",
    "083": "Vinaphone",
    "084": "Vinaphone",
    "085": "Vinaphone",
    "091": "Vinaphone",
    "094": "Vinaphone",
    "070": "Mobifone",
    "076": "Mobifone",
    "077": "Mobifone",
    "078": "Mobifone",
    "079": "Mobifone",
    "090": "Mobifone",
    "093": "Mobifone",
    "056": "Vietnamobile",
    "058": "Vietnamobile",
    "092": "Vietnamobile",
    "059": "Gmobile",
    "099": "Gmobile",
};
// Application State
let currentStep = 1;
const totalSteps = 3;
let formData = {};
let isFrontIdValid = false;
let isBackIdValid = false;
// Vietnam Address API
const API_BASE = "https://provinces.open-api.vn/api";
document.addEventListener("DOMContentLoaded", () => {
    initializeApp();
});
// Initialize Application
function initializeApp() {
    loadProvinces();
    setupEventListeners();
    updateProgressBar();
    setupFormValidation();
}
// Setup Event Listeners
function setupEventListeners() {
    const form = document.getElementById("registrationForm");
    // Province change handler
    document.getElementById("provinceSelect").addEventListener("change", handleProvinceChange);
    // District change handler
    document.getElementById("districtSelect").addEventListener("change", handleDistrictChange);
    // File upload handlers
    document.getElementById("frontId").addEventListener("change", (e) => handleImageUpload(e, "front"));
    document.getElementById("backId").addEventListener("change", (e) => handleImageUpload(e, "back"));
    // Drag and drop handlers
    setupDragAndDrop();
    // Form submission
    form.addEventListener("submit", handleFormSubmit);
    // Real-time validation
    setupRealTimeValidation();
}
// Load Provinces
async function loadProvinces() {
    try {
        showElementLoading("provinceSelect");
        const response = await fetch(`${API_BASE}/p/`);
        const provinces = await response.json();
        const provinceSelect = document.getElementById("provinceSelect");
        provinceSelect.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>';
        provinces.forEach((province) => {
            const option = document.createElement("option");
            option.value = province.code;
            option.textContent = province.name;
            provinceSelect.appendChild(option);
        });
        hideElementLoading("provinceSelect");
    } catch (error) {
        console.error("Error loading provinces:", error);
        showNotification("Không thể tải danh sách tỉnh/thành phố", "error");
        hideElementLoading("provinceSelect");
    }
}
// Handle Province Change
async function handleProvinceChange() {
    const provinceCode = this.value;
    const districtSelect = document.getElementById("districtSelect");
    const wardSelect = document.getElementById("wardSelect");
    // Reset districts and wards
    districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
    wardSelect.disabled = true;
    if (provinceCode) {
        try {
            showElementLoading("districtSelect");
            const response = await fetch(`${API_BASE}/p/${provinceCode}?depth=2`);
            const data = await response.json();
            data.districts.forEach((district) => {
                const option = document.createElement("option");
                option.value = district.code;
                option.textContent = district.name;
                districtSelect.appendChild(option);
            });
            districtSelect.disabled = false;
            hideElementLoading("districtSelect");
        } catch (error) {
            console.error("Error loading districts:", error);
            showNotification("Không thể tải danh sách quận/huyện", "error");
            hideElementLoading("districtSelect");
        }
    } else {
        districtSelect.disabled = true;
    }
}
// Handle District Change
async function handleDistrictChange() {
    const districtCode = this.value;
    const wardSelect = document.getElementById("wardSelect");
    // Reset wards
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
    if (districtCode) {
        try {
            showElementLoading("wardSelect");
            const response = await fetch(`${API_BASE}/d/${districtCode}?depth=2`);
            const data = await response.json();
            data.wards.forEach((ward) => {
                const option = document.createElement("option");
                option.value = ward.code;
                option.textContent = ward.name;
                wardSelect.appendChild(option);
            });
            wardSelect.disabled = false;
            hideElementLoading("wardSelect");
        } catch (error) {
            console.error("Error loading wards:", error);
            showNotification("Không thể tải danh sách phường/xã", "error");
            hideElementLoading("wardSelect");
        }
    } else {
        wardSelect.disabled = true;
    }
}
// Handle Image Upload
async function handleImageUpload(event, type) {
    const file = event.target.files[0];
    if (!file) return;

    // Validate file
    if (!validateImageFile(file)) {
        event.target.value = "";
        return;
    }

    // Show upload loading overlay
    const loadingOverlay = document.getElementById(`${type}IdLoading`);
    if (loadingOverlay) {
        loadingOverlay.classList.add("active");
    }

    try {
        // Show preview and update validation status
        const reader = new FileReader();
        reader.onload = (e) => {
            showImagePreview(e.target.result, type);
            updateImageValidationStatus(type, true);
            showNotification(`Ảnh ${type === "front" ? "mặt trước" : "mặt sau"} CCCD đã được tải lên`, "success");
        };
        reader.readAsDataURL(file);

        // Update global validation status
        if (type === "front") isFrontIdValid = true;
        if (type === "back") isBackIdValid = true;
    } catch (error) {
        console.error("Image upload error:", error);
        showNotification("Lỗi khi xử lý ảnh", "error");
        event.target.value = "";
        removeImage(type);
        updateImageValidationStatus(type, false);
    } finally {
        // Hide upload loading overlay
        if (loadingOverlay) {
            loadingOverlay.classList.remove("active");
        }
    }
}
// Validate Image File
function validateImageFile(file) {
    // Check file type
    if (!file.type.startsWith("image/")) {
        showNotification("Vui lòng chọn file ảnh hợp lệ (JPG, PNG)", "error");
        return false;
    }
    // Check file size (5MB limit)
    if (file.size > 5 * 1024 * 1024) {
        showNotification("File ảnh quá lớn. Vui lòng chọn file nhỏ hơn 5MB", "error");
        return false;
    }
    return true;
}
// Update image validation status
function updateImageValidationStatus(type, isValid) {
    const uploadItem = document.getElementById(`${type}Id`).parentElement;
    uploadItem.classList.remove("valid", "invalid");
    uploadItem.classList.add(isValid ? "valid" : "invalid");
    if (type === "front") isFrontIdValid = isValid;
    if (type === "back") isBackIdValid = isValid;
}
// Show Image Preview
function showImagePreview(imageSrc, type) {
    const imageElement = document.getElementById(`${type}Image`);
    const previewElement = document.getElementById(`${type}Preview`);
    if (imageElement && previewElement) {
        imageElement.src = imageSrc;
        previewElement.style.display = "block";
        // Add animation
        previewElement.style.opacity = "0";
        previewElement.style.transform = "scale(0.9)";
        setTimeout(() => {
            previewElement.style.transition = "all 0.3s ease";
            previewElement.style.opacity = "1";
            previewElement.style.transform = "scale(1)";
        }, 10);
    }
}
// Remove Image
function removeImage(type) {
    const inputElement = document.getElementById(`${type}Id`);
    const previewElement = document.getElementById(`${type}Preview`);
    const uploadItem = inputElement.parentElement;
    if (inputElement) inputElement.value = "";
    if (previewElement) {
        previewElement.style.transition = "all 0.3s ease";
        previewElement.style.opacity = "0";
        previewElement.style.transform = "scale(0.9)";
        setTimeout(() => {
            previewElement.style.display = "none";
        }, 300);
    }
    // Reset validation status
    updateImageValidationStatus(type, false);
}
// Setup Drag and Drop
function setupDragAndDrop() {
    const uploadZones = document.querySelectorAll(".upload-zone");
    uploadZones.forEach((zone) => {
        zone.addEventListener("dragover", handleDragOver);
        zone.addEventListener("dragleave", handleDragLeave);
        zone.addEventListener("drop", handleDrop);
    });
}
function handleDragOver(e) {
    e.preventDefault();
    this.style.backgroundColor = "#c8f0ff";
    this.style.borderColor = "#0085be";
    this.style.transform = "scale(1.02)";
}
function handleDragLeave(e) {
    e.preventDefault();
    this.style.backgroundColor = "#d5f8ff";
    this.style.borderColor = "#1196f5";
    this.style.transform = "scale(1)";
}
function handleDrop(e) {
    e.preventDefault();
    this.style.backgroundColor = "#d5f8ff";
    this.style.borderColor = "#1196f5";
    this.style.transform = "scale(1)";
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        const input = this.parentElement.querySelector('input[type="file"]');
        if (input) {
            input.files = files;
            const event = new Event("change");
            input.dispatchEvent(event);
        }
    }
}
// Toggle Password Visibility
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector(".password-toggle");
    const icon = button.querySelector("i");
    if (input.type === "password") {
        input.type = "text";
        icon.classList.remove("fa-eye-slash");
        icon.classList.add("fa-eye");
    } else {
        input.type = "password";
        icon.classList.remove("fa-eye");
        icon.classList.add("fa-eye-slash");
    }
    // Add animation effect
    button.style.transform = "scale(0.9)";
    setTimeout(() => {
        button.style.transform = "scale(1)";
    }, 150);
}
// Step Navigation
function nextStep() {
    if (validateCurrentStep()) {
        if (currentStep < totalSteps) {
            currentStep++;
            showStep(currentStep);
            updateProgressBar();
            scrollToTop();
        }
    }
}
function prevStep() {
    if (currentStep > 1) {
        currentStep--;
        showStep(currentStep);
        updateProgressBar();
        scrollToTop();
    }
}
function showStep(step) {
    document.querySelectorAll(".form-step").forEach((stepEl) => {
        stepEl.classList.remove("active");
    });
    const currentStepEl = document.getElementById(`step${step}`);
    if (currentStepEl) {
        currentStepEl.classList.add("active");
    }
    document.querySelectorAll(".progress-step").forEach((stepEl, index) => {
        if (index + 1 <= step) {
            stepEl.classList.add("active");
        } else {
            stepEl.classList.remove("active");
        }
    });
}
function updateProgressBar() {
    const progressFill = document.getElementById("progressFill");
    const percentage = (currentStep / totalSteps) * 100;
    if (progressFill) {
        progressFill.style.width = `${percentage}%`;
    }
}
function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: "smooth",
    });
}
// Form Validation
function validateCurrentStep() {
    let isValid = true;
    const currentStepEl = document.getElementById(`step${currentStep}`);
    if (!currentStepEl) return false;
    const inputs = currentStepEl.querySelectorAll("input[required], select[required]");
    inputs.forEach((input) => {
        if (!validateField(input)) {
            isValid = false;
        }
    });
    // Special validation for step 3
    if (currentStep === 3) {
        const frontId = document.getElementById("frontId");
        const backId = document.getElementById("backId");
        const agreeTerms = document.getElementById("agreeTerms");
        if (!frontId.files || frontId.files.length === 0 || !isFrontIdValid) {
            showFieldError(frontId, "Vui lòng chọn ảnh CCCD mặt trước hợp lệ");
            isValid = false;
        }
        if (!backId.files || backId.files.length === 0 || !isBackIdValid) {
            showFieldError(backId, "Vui lòng chọn ảnh CCCD mặt sau hợp lệ");
            isValid = false;
        }
        if (!agreeTerms.checked) {
            showFieldError(agreeTerms, "Vui lòng đồng ý với điều khoản và chính sách bảo mật");
            isValid = false;
        }
    }
    return isValid;
}
function validateField(field) {
    const value = field.value.trim();
    let isValid = true;
    let errorMessage = "";
    // Clear previous errors
    clearFieldError(field);
    // Required field validation
    if (field.hasAttribute("required") && !value) {
        isValid = false;
        errorMessage = `Vui lòng ${field.type === "select-one" ? "chọn" : "nhập"} ${getFieldLabel(field)}`;
    }
    // Specific field validations
    if (value && isValid) {
        switch (field.id) {
            case "fullName":
                if (value.length < 2) {
                    isValid = false;
                    errorMessage = "Họ tên phải có ít nhất 2 ký tự";
                }
                break;
            case "idNumber":
                if (!/^\d{9,12}$/.test(value)) {
                    isValid = false;
                    errorMessage = "Số CCCD/CMND không hợp lệ";
                } else {
                    if (value.length === 12) {
                        const provinceCode = value.substring(0, 3);
                        if (!CCCD_PROVINCE_CODES[provinceCode]) {
                            isValid = false;
                            errorMessage = `Mã tỉnh trong CCCD không hợp lệ (3 số đầu: ${provinceCode})`;
                        }
                    }
                }
                break;
            case "phoneNumber":
                const phoneValue = value.replace(/[\s\-()]/g, "");
                if (!phoneValue.startsWith("0")) {
                    isValid = false;
                    errorMessage = "Số điện thoại phải bắt đầu bằng số 0";
                } else if (!/^0[0-9]{9}$/.test(phoneValue)) {
                    isValid = false;
                    errorMessage = "Số điện thoại phải có 10 chữ số";
                } else {
                    const networkCode = phoneValue.substring(0, 3);
                    if (!PHONE_NETWORK_CODES[networkCode]) {
                        isValid = false;
                        errorMessage = "Số điện thoại không thuộc nhà mạng hợp lệ tại Việt Nam";
                    }
                }
                break;
            case "email":
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                    isValid = false;
                    errorMessage = "Vui lòng nhập email hợp lệ";
                }
                break;
            case "birthDate":
                const today = new Date();
                const birth = new Date(value);
                const age = today.getFullYear() - birth.getFullYear();
                if (age < 18) {
                    isValid = false;
                    errorMessage = "Bạn phải đủ 18 tuổi để đăng ký";
                }
                break;
            case "password":
                const passwordRegex = /^(?=.*[A-Z])(?=.*[!@#$%^&*])[A-Za-z\d!@#$%^&*]{6,}$/;
                if (!passwordRegex.test(value)) {
                    isValid = false;
                    errorMessage = "Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa và ký tự đặc biệt";
                }
                break;
            case "confirmPassword":
                const password = document.getElementById("password").value;
                if (value !== password) {
                    isValid = false;
                    errorMessage = "Mật khẩu xác nhận không khớp";
                }
                break;
        }
    }
    if (!isValid) {
        showFieldError(field, errorMessage);
    }
    return isValid;
}
function getFieldLabel(field) {
    const label = field.parentElement.parentElement.querySelector(".form-label");
    return label ? label.textContent.replace(" *", "").toLowerCase() : "thông tin này";
}
function showFieldError(field, message) {
    field.classList.add("is-invalid");
    let feedback = field.parentElement.parentElement.querySelector(".invalid-feedback");
    if (!feedback) {
        feedback = field.parentElement.querySelector(".invalid-feedback");
    }
    if (feedback) {
        feedback.textContent = message;
        feedback.style.display = "block";
    }
    if (field.type === "checkbox") {
        const termsError = document.querySelector(".terms-error");
        if (termsError) {
            termsError.style.display = "block";
            termsError.textContent = message;
        }
    }
}
function clearFieldError(field) {
    field.classList.remove("is-invalid");
    let feedback = field.parentElement.parentElement.querySelector(".invalid-feedback");
    if (!feedback) {
        feedback = field.parentElement.querySelector(".invalid-feedback");
    }
    if (feedback) {
        feedback.style.display = "none";
    }
    if (field.type === "checkbox") {
        const termsError = document.querySelector(".terms-error");
        if (termsError) {
            termsError.style.display = "none";
        }
    }
}
// Setup Real-time Validation
function setupRealTimeValidation() {
    const inputs = document.querySelectorAll("input, select");
    inputs.forEach((input) => {
        input.addEventListener("blur", () => validateField(input));
        input.addEventListener("input", () => {
            if (input.classList.contains("is-invalid")) {
                validateField(input);
            }
        });
    });
}
// Setup Form Validation
function setupFormValidation() {
    console.log("Form validation setup completed");
}
// Form Submission
function handleFormSubmit(e) {
    e.preventDefault();
    if (validateCurrentStep()) {
        collectFormData();
        showLoadingOverlay();
        const provinceText =
            document.getElementById("provinceSelect").options[document.getElementById("provinceSelect").selectedIndex]?.text || "";
        const districtText =
            document.getElementById("districtSelect").options[document.getElementById("districtSelect").selectedIndex]?.text || "";
        const wardText =
            document.getElementById("wardSelect").options[document.getElementById("wardSelect").selectedIndex]?.text || "";
        const addressDetail = document.getElementById("addressDetail").value;
        const fullAddress = `${addressDetail}, ${wardText}, ${districtText}, ${provinceText}`.trim();
        const submitFormData = new FormData();
        submitFormData.append("fullName", document.getElementById("fullName").value);
        submitFormData.append("email", document.getElementById("email").value);
        submitFormData.append("phoneNumber", document.getElementById("phoneNumber").value);
        submitFormData.append("password", document.getElementById("password").value);
        submitFormData.append("birthDate", document.getElementById("birthDate").value);
        submitFormData.append("gender", document.getElementById("gender").value);
        submitFormData.append("cccdNumber", document.getElementById("idNumber").value);
        submitFormData.append("issuePlace", document.getElementById("idIssuePlace").value);
        submitFormData.append("issueDate", document.getElementById("idIssueDate").value);
        submitFormData.append("frontImage", document.getElementById("frontId").files[0]);
        submitFormData.append("backImage", document.getElementById("backId").files[0]);
        submitFormData.append("address", fullAddress);
        fetch("/api/users/register-owner", {
            method: "POST",
            body: submitFormData,
        })
            .then(async (response) => {
                const contentType = response.headers.get("content-type");
                if (response.ok) {
                    if (contentType && contentType.includes("application/json")) {
                        return response.json();
                    } else {
                        const text = await response.text();
                        console.log("Response text:", text);
                        return {
                            success: true,
                            message: text,
                            userId: extractUserIdFromResponse(text),
                        };
                    }
                } else {
                    const text = await response.text();
                    throw new Error(text || "Lỗi không xác định.");
                }
            })
            .then((result) => {
                hideLoadingOverlay();
                if (result.userId) {
                    showSuccessModal();
                    setTimeout(() => {
                        window.location.href = `/dang-ky-chi-tiet?userId=${result.userId}`;
                    }, 2000);
                } else {
                    showSuccessModal();
                    setTimeout(() => {
                        window.location.href = "/dang-nhap-chu-tro";
                    }, 2000);
                }
            })
            .catch((error) => {
                console.error("Lỗi đăng ký:", error);
                hideLoadingOverlay();
                showNotification("Đăng ký thất bại: " + error.message, "error");
            });
    }
}
// Helper function to extract userId from response
function extractUserIdFromResponse(text) {
    try {
        const userIdMatch = text.match(/userId[:\s]*(\d+)/i);
        if (userIdMatch) return userIdMatch[1];
        const idMatch = text.match(/id[:\s]*(\d+)/i);
        if (idMatch) return idMatch[1];
        return null;
    } catch (e) {
        console.log("Không thể extract userId từ response");
        return null;
    }
}
function collectFormData() {
    formData = {
        fullName: document.getElementById("fullName").value,
        gender: document.getElementById("gender").value,
        birthDate: document.getElementById("birthDate").value,
        idNumber: document.getElementById("idNumber").value,
        idIssueDate: document.getElementById("idIssueDate").value,
        idIssuePlace: document.getElementById("idIssuePlace").value,
        phoneNumber: document.getElementById("phoneNumber").value,
        email: document.getElementById("email").value,
        province: document.getElementById("provinceSelect").value,
        district: document.getElementById("districtSelect").value,
        ward: document.getElementById("wardSelect").value,
        addressDetail: document.getElementById("addressDetail").value,
        password: document.getElementById("password").value,
        frontIdFile: document.getElementById("frontId").files[0],
        backIdFile: document.getElementById("backId").files[0],
        agreeTerms: document.getElementById("agreeTerms").checked,
    };
    console.log("Form data collected:", formData);
}
// UI Helper Functions
function showLoadingOverlay() {
    const overlay = document.getElementById("loadingOverlay");
    if (overlay) {
        overlay.classList.add("active");
    }
}
function hideLoadingOverlay() {
    const overlay = document.getElementById("loadingOverlay");
    if (overlay) {
        overlay.classList.remove("active");
    }
}
function showSuccessModal() {
    Swal.fire({
        icon: 'success',
        title: 'Đăng ký thành công, tài khoản của bạn sẽ được nhân viên duyệt sớm nhất có thể!',
        text: 'Bạn sẽ được chuyển hướng trong giây lát.',
        showConfirmButton: false,
        timer: 5000
    });
}
function showElementLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.disabled = true;
        element.style.opacity = "0.6";
    }
}
function hideElementLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.disabled = false;
        element.style.opacity = "1";
    }
}
function showNotification(message, type = "info") {
    const notification = document.createElement("div");
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${type === "error" ? "exclamation-circle" : type === "success" ? "check-circle" : "info-circle"}"></i>
            <span>${message}</span>
        </div>
    `;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === "error" ? "#ef4444" : type === "success" ? "#22c55e" : "#1196f5"};
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 10000;
        transform: translateX(100%);
        transition: transform 0.3s ease;
    `;
    document.body.appendChild(notification);
    setTimeout(() => {
        notification.style.transform = "translateX(0)";
    }, 10);
    setTimeout(() => {
        notification.style.transform = "translateX(100%)";
        setTimeout(() => {
            if (document.body.contains(notification)) {
                document.body.removeChild(notification);
            }
        }, 300);
    }, 3000);
}
// Global functions for HTML onclick handlers
window.nextStep = nextStep;
window.prevStep = prevStep;
window.togglePassword = togglePassword;
window.removeImage = removeImage;