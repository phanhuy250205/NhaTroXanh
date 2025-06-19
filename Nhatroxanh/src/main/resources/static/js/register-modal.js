document.addEventListener("DOMContentLoaded", () => {
    // --- PHẦN 1: KHAI BÁO CÁC NÚT VÀ FORM CẦN SỬ DỤNG (GIỮ NGUYÊN) ---
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

    // Sự kiện: Mở modal khi click nút "Đăng ký" (GIỮ NGUYÊN)
    if (registerBtn) {
        registerBtn.addEventListener("click", (e) => {
            e.preventDefault();
            openModal();
        });
    }

    // Sự kiện: Đóng modal khi click nút close (X) (GIỮ NGUYÊN)
    if (registerModalClose) {
        registerModalClose.addEventListener("click", closeModal);
    }

    // Sự kiện: Đóng modal khi click ra ngoài vùng nội dung (GIỮ NGUYÊN)
    if (registerModalOverlay) {
        registerModalOverlay.addEventListener("click", (e) => {
            if (e.target === registerModalOverlay) {
                closeModal();
            }
        });
    }

    // Sự kiện: Đóng modal bằng phím Escape (GIỮ NGUYÊN)
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && registerModalOverlay && registerModalOverlay.classList.contains("show")) {
            closeModal();
        }
    });

    // --- PHẦN 3: LOGIC ẨN/HIỆN MẬT KHẨU (GIỮ NGUYÊN) ---
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

    // --- PHẦN 4: LOGIC XỬ LÝ SUBMIT FORM ĐĂNG KÝ (PHẦN DUY NHẤT ĐƯỢC SỬA) ---
    if (registerForm) {
        registerForm.addEventListener("submit", function (e) {
            e.preventDefault(); // Ngăn form tự gửi đi

            const submitBtn = this.querySelector(".btn-register-submit");
            const errorMessageDiv = document.getElementById("register-error-message");
            errorMessageDiv.style.display = 'none'; // Ẩn thông báo lỗi cũ

            // Lấy dữ liệu từ các ô input bằng ID
            const fullName = document.getElementById("fullName").value;
            const username = document.getElementById("username").value;
            const email = document.getElementById("email").value;
            const phoneNumber = document.getElementById("phoneNumber").value;
            const password = document.getElementById("password").value;
            const confirmPassword = document.getElementById("confirmPassword").value;

            // Kiểm tra dữ liệu phía client
            if (!fullName || !username || !email || !phoneNumber || !password) {
                 errorMessageDiv.textContent = "Vui lòng điền đầy đủ thông tin!";
                 errorMessageDiv.style.display = "block";
                 return;
            }
            if (password !== confirmPassword) {
                errorMessageDiv.textContent = "Mật khẩu xác nhận không khớp!";
                errorMessageDiv.style.display = "block";
                return;
            }

            const userData = { fullName, username, email, phoneNumber, password };

            // Gọi API thật sự của backend
            submitBtn.textContent = 'ĐANG XỬ LÝ...';
            submitBtn.disabled = true;

            fetch('/api/users/register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(userData),
            })
            .then(response => {
                if (response.ok) {
                    // THÀNH CÔNG: Đóng modal này, mở modal xác thực
                    closeModal();
                    const verificationModal = document.getElementById('verificationModalOverlay');
                    if(verificationModal) {
                        verificationModal.classList.add('show');
                        document.body.style.overflow = "hidden";
                        verificationModal.querySelector('.verification-alert b').textContent = email;
                        verificationModal.dataset.email = email;
                    }
                } else {
                    // THẤT BẠI: Hiển thị lỗi từ server
                    return response.text().then(text => { throw new Error(text) });
                }
            })
            .catch(error => {
                errorMessageDiv.textContent = error.message;
                errorMessageDiv.style.display = "block";
            })
            .finally(() => {
                submitBtn.textContent = 'ĐĂNG KÝ';
                submitBtn.disabled = false;
            });
        });
    }

    // --- PHẦN 5: CÁC CHỨC NĂNG KHÁC (GIỮ NGUYÊN) ---

    // Social register buttons
    const googleBtn = document.querySelector(".register-modal .btn-google")
    const facebookBtn = document.querySelector(".register-modal .btn-facebook")

    if (googleBtn) {
        googleBtn.addEventListener("click", () => {
            alert("Đăng ký với Google - Chức năng đang phát triển")
        })
    }
    if (facebookBtn) {
        facebookBtn.addEventListener("click", () => {
            alert("Đăng ký với Facebook - Chức năng đang phát triển")
        })
    }

    // Login link
    const loginLink = document.querySelector(".register-modal .login-now")
    if (loginLink) {
        loginLink.addEventListener("click", (e) => {
            e.preventDefault()
            closeModal()
            const loginModalOverlay = document.getElementById("loginModalOverlay");
            if (loginModalOverlay) {
                loginModalOverlay.classList.add("show");
            }
        });
    }

    // Add smooth animations for form elements
    const formInputs = document.querySelectorAll(".form-control-register")
    formInputs.forEach((input) => {
        input.addEventListener("focus", function () {
            this.parentElement.style.transform = "scale(1.02)"
        })
        input.addEventListener("blur", function () {
            this.parentElement.style.transform = "scale(1)"
        })
    });
});
