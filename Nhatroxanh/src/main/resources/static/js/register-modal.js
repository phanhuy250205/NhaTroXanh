document.addEventListener("DOMContentLoaded", () => {
    // --- PHẦN 1: KHAI BÁO CÁC NÚT VÀ FORM CẦN SỬ DỤNG ---
    const registerBtn = document.querySelector(".btn-register");
    const registerModalOverlay = document.getElementById("registerModalOverlay");
    const registerModalClose = document.getElementById("registerModalClose");
    const registerForm = document.getElementById("registerForm");
    const passwordToggles = document.querySelectorAll(".register-modal .password-toggle");

    // --- PHẦN 2: CÁC HÀM VÀ SỰ KIỆN ĐIỀU KHIỂN GIAO DIỆN (UI) ---
     let currentToast = null;

   function showToast(message, type = 'error') {
    let backgroundColor;
   
     if (currentToast) {
            currentToast.hideToast();
        }
    switch (type) {
        case 'success':
            backgroundColor = "#28a745"; // Màu xanh lá cây Bootstrap success
            break;
        case 'info':
             backgroundColor = "#3498DB"; // Màu xanh dương nhạt Bootstrap info
            break;
        case 'error':
        default:
            backgroundColor = "#dc3545"; // Màu đỏ Bootstrap danger
            break;
    }
    currentToast = Toastify({
            text: message,
            duration: 3000,
            close: true,
            gravity: "top",
            position: "right",
            stopOnFocus: true,
            style: {
                background: backgroundColor,
            },
        });
        
        currentToast.showToast();
    
}

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
            e.preventDefault();

            const submitBtn = this.querySelector(".btn-register-submit");

            const fullNameValue = document.getElementById("fullName").value;
            const emailValue = document.getElementById("email").value;
            const phoneNumberValue = document.getElementById("phoneNumber").value;
            const passwordValue = document.getElementById("password").value;
            const confirmPasswordValue = document.getElementById("confirmPassword").value;

            // <<< THÊM VÀO: Validation phía client >>>
            if (!fullNameValue || !emailValue || !phoneNumberValue || !passwordValue || !confirmPasswordValue) {
                showToast("Vui lòng điền đầy đủ thông tin!");
                return;
            }
            const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
            if (!emailRegex.test(emailValue)) {
                showToast("Email bạn nhập không đúng định dạng.");
                return;
            }
            if (passwordValue !== confirmPasswordValue) {
                showToast("Mật khẩu xác nhận không khớp!");
                return;
            }
            // Kiểm tra định dạng mật khẩu bằng Regex
            const passwordRegex = /^(?=.*[A-Z])(?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]).{6,}$/;
            if (!passwordRegex.test(passwordValue)) {
                showToast("Mật khẩu phải từ 6 ký tự, có ít nhất 1 chữ hoa và 1 ký tự đặc biệt.");
                return;
            }

            const userData = {
                fullName: fullNameValue,
                email: emailValue,
                phoneNumber: phoneNumberValue,
                password: passwordValue
            };

            submitBtn.disabled = true;
            submitBtn.textContent = 'ĐANG XỬ LÝ...';

            fetch('/api/users/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(userData),
            })
                .then(async (response) => { // Dùng async để xử lý response.text()
                    if (response.ok) {
                        // THÀNH CÔNG
                        showToast("Mã xác thực đã được gửi đến email của bạn!", 'success');
                        closeModal();
                        // Mở modal xác thực
                        const verificationModal = document.getElementById('verificationModalOverlay');
                        if (verificationModal) {
                            verificationModal.classList.add('show');
                            verificationModal.querySelector('.verification-alert b').textContent = emailValue;
                            verificationModal.dataset.email = emailValue;
                        }
                    } else {
                        // THẤT BẠI: Lấy lỗi từ server
                        const errorText = await response.text();
                        throw new Error(errorText || "Đã xảy ra lỗi không xác định.");
                    }
                })
                .catch(error => {
                    // <<< THAY ĐỔI: Hiển thị lỗi bằng Toast >>>
                    showToast(error.message);
                })
                .finally(() => {
                    submitBtn.disabled = false;
                    submitBtn.textContent = 'ĐĂNG KÝ';
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
