document.addEventListener("DOMContentLoaded", () => {
    // --- PHẦN 1: KHAI BÁO CÁC NÚT VÀ FORM CẦN SỬ DỤNG ---
    const registerBtn = document.querySelector(".btn-register");
    const registerModalOverlay = document.getElementById("registerModalOverlay");
    const registerModalClose = document.getElementById("registerModalClose");
    const registerForm = document.getElementById("registerForm");
    const passwordToggles = document.querySelectorAll(".register-modal .password-toggle");

    // --- PHẦN 2: CÁC HÀM VÀ SỰ KIỆN ĐIỀU KHIỂN GIAO DIỆN (UI) ---

    // Hàm để mở modal
    function openModal() {
        if (registerModalOverlay) {
            registerModalOverlay.classList.add("show");
            document.body.style.overflow = "hidden"; // Chặn cuộn trang nền
        }
    }

    // Hàm để đóng modal
    function closeModal() {
        if (registerModalOverlay) {
            registerModalOverlay.classList.remove("show");
            document.body.style.overflow = ""; // Cho phép cuộn trang nền trở lại
        }
    }

    // Sự kiện: Mở modal khi click nút "Đăng ký"
    if (registerBtn) {
        registerBtn.addEventListener("click", (e) => {
            e.preventDefault();
            openModal();
        });
    }

    // Sự kiện: Đóng modal khi click nút close (X)
    if (registerModalClose) {
        registerModalClose.addEventListener("click", closeModal);
    }

    // Sự kiện: Đóng modal khi click ra ngoài vùng nội dung
    if (registerModalOverlay) {
        registerModalOverlay.addEventListener("click", (e) => {
            if (e.target === registerModalOverlay) {
                closeModal();
            }
        });
    }

    // Sự kiện: Đóng modal bằng phím Escape
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && registerModalOverlay && registerModalOverlay.classList.contains("show")) {
            closeModal();
        }
    });

    // --- PHẦN 3: LOGIC ẨN/HIỆN MẬT KHẨU ---
    passwordToggles.forEach((toggle) => {
        toggle.addEventListener("click", function () {
            const wrapper = this.closest('.input-wrapper');
            const passwordInput = wrapper.querySelector('input');
            const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
            passwordInput.setAttribute("type", type);
            const icon = this.querySelector("i");
            icon.classList.toggle("fa-eye");
            icon.classList.toggle("fa-eye-slash");
        });
    });

    // --- PHẦN 4: LOGIC XỬ LÝ SUBMIT FORM ĐĂNG KÝ ---
    if (registerForm) {
        registerForm.addEventListener("submit", function (e) {
            e.preventDefault(); // Ngăn form tự gửi đi

            const submitBtn = this.querySelector(".btn-register-submit");
            const errorMessageDiv = document.getElementById("register-error-message");
            if(errorMessageDiv) { // Kiểm tra nếu có ô báo lỗi
                 errorMessageDiv.style.display = 'none'; // Ẩn thông báo lỗi cũ
            }
            
            // Lấy dữ liệu từ các ô input bằng ID
            const fullNameValue = document.getElementById("fullName").value;
            // ### THAY ĐỔI 1: Không lấy username từ form nữa ###
            // const usernameValue = document.getElementById("username").value; 
            const emailValue = document.getElementById("email").value;
            const phoneNumberValue = document.getElementById("phoneNumber").value;
            const passwordValue = document.getElementById("password").value;
            const confirmPasswordValue = document.getElementById("confirmPassword").value;

            // ### THAY ĐỔI 2: Cập nhật lại logic kiểm tra ###
            if (!fullNameValue || !emailValue || !phoneNumberValue || !passwordValue) {
                if (errorMessageDiv) {
                    errorMessageDiv.textContent = "Vui lòng điền đầy đủ thông tin!";
                    errorMessageDiv.style.display = "block";
                }
                return;
            }
            if (passwordValue !== confirmPasswordValue) {
                if (errorMessageDiv) {
                    errorMessageDiv.textContent = "Mật khẩu xác nhận không khớp!";
                    errorMessageDiv.style.display = "block";
                }
                return;
            }

            // ### THAY ĐỔI 3: Dùng email làm username ###
            // Tạo đối tượng dữ liệu để gửi đi, gán giá trị của email cho username
            const userData = {
                fullName: fullNameValue,
                username: emailValue, // Sử dụng email làm tên đăng nhập
                email: emailValue,
                phoneNumber: phoneNumberValue,
                password: passwordValue
            };

            // Vô hiệu hóa nút và hiển thị trạng thái đang xử lý
            submitBtn.textContent = 'ĐANG XỬ LÝ...';
            submitBtn.disabled = true;

            // Gọi API của backend để đăng ký
            fetch('/api/users/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(userData),
            })
            .then(response => {
                if (response.ok) {
                    // THÀNH CÔNG: Đóng modal đăng ký, mở modal xác thực
                    closeModal();
                    const verificationModal = document.getElementById('verificationModalOverlay');
                    if (verificationModal) {
                        verificationModal.classList.add('show');
                        document.body.style.overflow = "hidden";
                        // Cập nhật email trong modal xác thực
                        verificationModal.querySelector('.verification-alert b').textContent = emailValue;
                        verificationModal.dataset.email = emailValue; // Lưu email để dùng cho việc gửi lại OTP
                    }
                } else {
                    // THẤT BẠI: Đọc và hiển thị lỗi từ server
                    return response.text().then(text => {
                        // Ném lỗi để khối .catch() có thể bắt được
                        throw new Error(text || "Đã xảy ra lỗi không xác định. Vui lòng thử lại.");
                    });
                }
            })
            .catch(error => {
                // Hiển thị thông báo lỗi cho người dùng
                if (errorMessageDiv) {
                    errorMessageDiv.textContent = error.message;
                    errorMessageDiv.style.display = "block";
                }
            })
            .finally(() => {
                // Kích hoạt lại nút submit dù thành công hay thất bại
                submitBtn.textContent = 'ĐĂNG KÝ';
                submitBtn.disabled = false;
            });
        });
    }

    // --- PHẦN 5: CÁC CHỨC NĂNG KHÁC ---
    const loginLink = document.querySelector(".register-modal .login-now");
    if (loginLink) {
        loginLink.addEventListener("click", (e) => {
            e.preventDefault();
            closeModal(); // Đóng modal đăng ký
            const loginModalOverlay = document.getElementById("loginModalOverlay");
            if (loginModalOverlay) {
                loginModalOverlay.classList.add("show"); // Mở modal đăng nhập
            }
        });
    }
});
