// Application State
let currentStep = 1;
const totalSteps = 3;
let formData = {};

// Vietnam Address API
const API_BASE = 'https://provinces.open-api.vn/api';

// DOM Content Loaded
document.addEventListener('DOMContentLoaded', function() {
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
    const form = document.getElementById('registrationForm');
    
    // Province change handler
    document.getElementById('provinceSelect').addEventListener('change', handleProvinceChange);
    
    // District change handler
    document.getElementById('districtSelect').addEventListener('change', handleDistrictChange);
    
    // File upload handlers
    document.getElementById('frontId').addEventListener('change', (e) => handleImageUpload(e, 'front'));
    document.getElementById('backId').addEventListener('change', (e) => handleImageUpload(e, 'back'));
    
    // Drag and drop handlers
    setupDragAndDrop();
    
    // Form submission
    form.addEventListener('submit', handleFormSubmit);
    
    // Real-time validation
    setupRealTimeValidation();
}

// Load Provinces
async function loadProvinces() {
    try {
        showElementLoading('provinceSelect');
        const response = await fetch(`${API_BASE}/p/`);
        const provinces = await response.json();
        
        const provinceSelect = document.getElementById('provinceSelect');
        provinceSelect.innerHTML = '<option value="">Chọn Tỉnh/Thành phố</option>';
        
        provinces.forEach(province => {
            const option = document.createElement('option');
            option.value = province.code;
            option.textContent = province.name;
            provinceSelect.appendChild(option);
        });
        
        hideElementLoading('provinceSelect');
    } catch (error) {
        console.error('Error loading provinces:', error);
        showNotification('Không thể tải danh sách tỉnh/thành phố', 'error');
        hideElementLoading('provinceSelect');
    }
}

// Handle Province Change
async function handleProvinceChange() {
    const provinceCode = this.value;
    const districtSelect = document.getElementById('districtSelect');
    const wardSelect = document.getElementById('wardSelect');
    
    // Reset districts and wards
    districtSelect.innerHTML = '<option value="">Chọn Quận/Huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
    wardSelect.disabled = true;
    
    if (provinceCode) {
        try {
            showElementLoading('districtSelect');
            const response = await fetch(`${API_BASE}/p/${provinceCode}?depth=2`);
            const data = await response.json();
            
            data.districts.forEach(district => {
                const option = document.createElement('option');
                option.value = district.code;
                option.textContent = district.name;
                districtSelect.appendChild(option);
            });
            
            districtSelect.disabled = false;
            hideElementLoading('districtSelect');
        } catch (error) {
            console.error('Error loading districts:', error);
            showNotification('Không thể tải danh sách quận/huyện', 'error');
            hideElementLoading('districtSelect');
        }
    } else {
        districtSelect.disabled = true;
    }
}

// Handle District Change
async function handleDistrictChange() {
    const districtCode = this.value;
    const wardSelect = document.getElementById('wardSelect');
    
    // Reset wards
    wardSelect.innerHTML = '<option value="">Chọn Phường/Xã</option>';
    
    if (districtCode) {
        try {
            showElementLoading('wardSelect');
            const response = await fetch(`${API_BASE}/d/${districtCode}?depth=2`);
            const data = await response.json();
            
            data.wards.forEach(ward => {
                const option = document.createElement('option');
                option.value = ward.code;
                option.textContent = ward.name;
                wardSelect.appendChild(option);
            });
            
            wardSelect.disabled = false;
            hideElementLoading('wardSelect');
        } catch (error) {
            console.error('Error loading wards:', error);
            showNotification('Không thể tải danh sách phường/xã', 'error');
            hideElementLoading('wardSelect');
        }
    } else {
        wardSelect.disabled = true;
    }
}

// Handle Image Upload
function handleImageUpload(event, type) {
    const file = event.target.files[0];
    if (!file) return;
    
    // Validate file
    if (!validateImageFile(file)) return;
    
    // Show preview
    const reader = new FileReader();
    reader.onload = function(e) {
        showImagePreview(e.target.result, type);
    };
    reader.readAsDataURL(file);
}

// Validate Image File
function validateImageFile(file) {
    // Check file type
    if (!file.type.startsWith('image/')) {
        showNotification('Vui lòng chọn file ảnh hợp lệ', 'error');
        return false;
    }
    
    // Check file size (5MB limit)
    if (file.size > 5 * 1024 * 1024) {
        showNotification('File ảnh quá lớn. Vui lòng chọn file nhỏ hơn 5MB', 'error');
        return false;
    }
    
    return true;
}

// Show Image Preview
function showImagePreview(imageSrc, type) {
    const imageElement = document.getElementById(`${type}Image`);
    const previewElement = document.getElementById(`${type}Preview`);
    
    if (imageElement && previewElement) {
        imageElement.src = imageSrc;
        previewElement.style.display = 'block';
        
        // Add animation
        previewElement.style.opacity = '0';
        previewElement.style.transform = 'scale(0.9)';
        
        setTimeout(() => {
            previewElement.style.transition = 'all 0.3s ease';
            previewElement.style.opacity = '1';
            previewElement.style.transform = 'scale(1)';
        }, 10);
    }
}

// Remove Image
function removeImage(type) {
    const inputElement = document.getElementById(`${type}Id`);
    const previewElement = document.getElementById(`${type}Preview`);
    
    if (inputElement) inputElement.value = '';
    if (previewElement) {
        previewElement.style.transition = 'all 0.3s ease';
        previewElement.style.opacity = '0';
        previewElement.style.transform = 'scale(0.9)';
        
        setTimeout(() => {
            previewElement.style.display = 'none';
        }, 300);
    }
}

// Setup Drag and Drop
function setupDragAndDrop() {
    const uploadZones = document.querySelectorAll('.upload-zone');
    
    uploadZones.forEach(zone => {
        zone.addEventListener('dragover', handleDragOver);
        zone.addEventListener('dragleave', handleDragLeave);
        zone.addEventListener('drop', handleDrop);
    });
}

function handleDragOver(e) {
    e.preventDefault();
    this.style.backgroundColor = '#c8f0ff';
    this.style.borderColor = '#0085be';
    this.style.transform = 'scale(1.02)';
}

function handleDragLeave(e) {
    e.preventDefault();
    this.style.backgroundColor = '#d5f8ff';
    this.style.borderColor = '#1196f5';
    this.style.transform = 'scale(1)';
}

function handleDrop(e) {
    e.preventDefault();
    this.style.backgroundColor = '#d5f8ff';
    this.style.borderColor = '#1196f5';
    this.style.transform = 'scale(1)';
    
    const files = e.dataTransfer.files;
    if (files.length > 0) {
        const input = this.parentElement.querySelector('input[type="file"]');
        if (input) {
            input.files = files;
            const event = new Event('change');
            input.dispatchEvent(event);
        }
    }
}

// Toggle Password Visibility
function togglePassword(inputId) {
    const input = document.getElementById(inputId);
    const button = input.parentElement.querySelector('.password-toggle');
    const icon = button.querySelector('i');
    
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.remove('fa-eye-slash');
        icon.classList.add('fa-eye');
    } else {
        input.type = 'password';
        icon.classList.remove('fa-eye');
        icon.classList.add('fa-eye-slash');
    }
    
    // Add animation effect
    button.style.transform = 'scale(0.9)';
    setTimeout(() => {
        button.style.transform = 'scale(1)';
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
    // Hide all steps
    document.querySelectorAll('.form-step').forEach(stepEl => {
        stepEl.classList.remove('active');
    });
    
    // Show current step
    const currentStepEl = document.getElementById(`step${step}`);
    if (currentStepEl) {
        currentStepEl.classList.add('active');
    }
    
    // Update progress steps
    document.querySelectorAll('.progress-step').forEach((stepEl, index) => {
        if (index + 1 <= step) {
            stepEl.classList.add('active');
        } else {
            stepEl.classList.remove('active');
        }
    });
}

function updateProgressBar() {
    const progressFill = document.getElementById('progressFill');
    const percentage = (currentStep / totalSteps) * 100;
    
    if (progressFill) {
        progressFill.style.width = `${percentage}%`;
    }
}

function scrollToTop() {
    window.scrollTo({
        top: 0,
        behavior: 'smooth'
    });
}

// Form Validation
function validateCurrentStep() {
    let isValid = true;
    const currentStepEl = document.getElementById(`step${currentStep}`);
    
    if (!currentStepEl) return false;
    
    const inputs = currentStepEl.querySelectorAll('input[required], select[required]');
    
    inputs.forEach(input => {
        if (!validateField(input)) {
            isValid = false;
        }
    });
    
    // Special validation for step 3
    if (currentStep === 3) {
        // Validate file uploads
        const frontId = document.getElementById('frontId');
        const backId = document.getElementById('backId');
        
        if (!frontId.files || frontId.files.length === 0) {
            showFieldError(frontId, 'Vui lòng chọn ảnh CCCD mặt trước');
            isValid = false;
        }
        
        if (!backId.files || backId.files.length === 0) {
            showFieldError(backId, 'Vui lòng chọn ảnh CCCD mặt sau');
            isValid = false;
        }
        
        // Validate terms agreement
        const agreeTerms = document.getElementById('agreeTerms');
        if (!agreeTerms.checked) {
            showFieldError(agreeTerms, 'Vui lòng đồng ý với điều khoản và chính sách bảo mật');
            isValid = false;
        }
    }
    
    return isValid;
}

function validateField(field) {
    const value = field.value.trim();
    let isValid = true;
    let errorMessage = '';
    
    // Clear previous errors
    clearFieldError(field);
    
    // Required field validation
    if (field.hasAttribute('required') && !value) {
        isValid = false;
        errorMessage = `Vui lòng ${field.type === 'select-one' ? 'chọn' : 'nhập'} ${getFieldLabel(field)}`;
    }
    
    // Specific field validations
    if (value && isValid) {
        switch (field.id) {
            case 'fullName':
                if (value.length < 2) {
                    isValid = false;
                    errorMessage = 'Họ tên phải có ít nhất 2 ký tự';
                }
                break;
                
            case 'idNumber':
                if (!/^\d{9,12}$/.test(value)) {
                    isValid = false;
                    errorMessage = 'Số CCCD/CMND không hợp lệ';
                }
                break;
                
            case 'phoneNumber':
                if (!/^[0-9]{10,11}$/.test(value.replace(/\s/g, ''))) {
                    isValid = false;
                    errorMessage = 'Số điện thoại phải có 10-11 chữ số';
                }
                break;
                
            case 'email':
                if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
                    isValid = false;
                    errorMessage = 'Vui lòng nhập email hợp lệ';
                }
                break;
                
            case 'birthDate':
                const today = new Date();
                const birth = new Date(value);
                const age = today.getFullYear() - birth.getFullYear();
                if (age < 18) {
                    isValid = false;
                    errorMessage = 'Bạn phải đủ 18 tuổi để đăng ký';
                }
                break;
                
            case 'password':
                if (value.length < 6) {
                    isValid = false;
                    errorMessage = 'Mật khẩu phải có ít nhất 6 ký tự';
                }
                break;
                
            case 'confirmPassword':
                const password = document.getElementById('password').value;
                if (value !== password) {
                    isValid = false;
                    errorMessage = 'Mật khẩu xác nhận không khớp';
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
    const label = field.parentElement.parentElement.querySelector('.form-label');
    return label ? label.textContent.replace(' *', '').toLowerCase() : 'thông tin này';
}

function showFieldError(field, message) {
    field.classList.add('is-invalid');
    
    let feedback = field.parentElement.parentElement.querySelector('.invalid-feedback');
    if (!feedback) {
        feedback = field.parentElement.querySelector('.invalid-feedback');
    }
    
    if (feedback) {
        feedback.textContent = message;
        feedback.style.display = 'block';
    }
    
    // Special handling for checkbox
    if (field.type === 'checkbox') {
        const termsError = document.querySelector('.terms-error');
        if (termsError) {
            termsError.style.display = 'block';
            termsError.textContent = message;
        }
    }
}

function clearFieldError(field) {
    field.classList.remove('is-invalid');
    
    let feedback = field.parentElement.parentElement.querySelector('.invalid-feedback');
    if (!feedback) {
        feedback = field.parentElement.querySelector('.invalid-feedback');
    }
    
    if (feedback) {
        feedback.style.display = 'none';
    }
    
    // Special handling for checkbox
    if (field.type === 'checkbox') {
        const termsError = document.querySelector('.terms-error');
        if (termsError) {
            termsError.style.display = 'none';
        }
    }
}

// Setup Real-time Validation
function setupRealTimeValidation() {
    const inputs = document.querySelectorAll('input, select');
    
    inputs.forEach(input => {
        input.addEventListener('blur', () => validateField(input));
        input.addEventListener('input', () => {
            if (input.classList.contains('is-invalid')) {
                validateField(input);
            }
        });
    });
}

// Setup Form Validation
function setupFormValidation() {
    // This function sets up initial form validation
    // Currently handled by setupRealTimeValidation() and other validation functions
    console.log('Form validation setup completed');
}

// Form Submission
function handleFormSubmit(e) {
    e.preventDefault();
    
    if (validateCurrentStep()) {
        collectFormData();
        showLoadingOverlay();
        
        // Simulate API call
        setTimeout(() => {
            hideLoadingOverlay();
            showSuccessModal();
        }, 2000);
    }
}

function collectFormData() {
    formData = {
        fullName: document.getElementById('fullName').value,
        gender: document.getElementById('gender').value,
        birthDate: document.getElementById('birthDate').value,
        idNumber: document.getElementById('idNumber').value,
        idIssueDate: document.getElementById('idIssueDate').value,
        idIssuePlace: document.getElementById('idIssuePlace').value,
        phoneNumber: document.getElementById('phoneNumber').value,
        email: document.getElementById('email').value,
        province: document.getElementById('provinceSelect').value,
        district: document.getElementById('districtSelect').value,
        ward: document.getElementById('wardSelect').value,
        addressDetail: document.getElementById('addressDetail').value,
        password: document.getElementById('password').value,
        frontIdFile: document.getElementById('frontId').files[0],
        backIdFile: document.getElementById('backId').files[0],
        agreeTerms: document.getElementById('agreeTerms').checked
    };
    
    console.log('Form data collected:', formData);
}

// UI Helper Functions
function showLoadingOverlay() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.classList.add('active');
    }
}

function hideLoadingOverlay() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        overlay.classList.remove('active');
    }
}

function showSuccessModal() {
    const modal = new bootstrap.Modal(document.getElementById('successModal'));
    modal.show();
}

function showElementLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.disabled = true;
        element.style.opacity = '0.6';
    }
}

function hideElementLoading(elementId) {
    const element = document.getElementById(elementId);
    if (element) {
        element.disabled = false;
        element.style.opacity = '1';
    }
}

function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
            <span>${message}</span>
        </div>
    `;
    
    // Add styles
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === 'error' ? '#ef4444' : '#1196f5'};
        color: white;
        padding: 1rem 1.5rem;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        z-index: 10000;
        transform: translateX(100%);
        transition: transform 0.3s ease;
    `;
    
    document.body.appendChild(notification);
    
    // Show notification
    setTimeout(() => {
        notification.style.transform = 'translateX(0)';
    }, 10);
    
    // Hide notification
    setTimeout(() => {
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// Global functions for HTML onclick handlers
window.nextStep = nextStep;
window.prevStep = prevStep;
window.togglePassword = togglePassword;
window.removeImage = removeImage;