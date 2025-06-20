// Login Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN ---
    const loginBtn = document.getElementById("loginBtn");
    const loginModalOverlay = document.getElementById("loginModalOverlay");
    const loginModalClose = document.getElementById("loginModalClose");
    const loginForm = document.getElementById("loginForm");
    const passwordToggles = document.querySelectorAll(".login-modal .password-toggle");
    const registerLink = document.querySelector(".login-modal .register-now");


    // --- HÀM VÀ SỰ KIỆN UI ---
    function openModal() {
        if (loginModalOverlay) {
            loginModalOverlay.classList.add("show");
            document.body.style.overflow = "hidden";
        }
    }
    function closeModal() {
        if (loginModalOverlay) {
            loginModalOverlay.classList.remove("show");
            document.body.style.overflow = "";
        }
    }
    if (loginBtn) { loginBtn.addEventListener("click", (e) => { e.preventDefault(); openModal(); }); }
    if (loginModalClose) { loginModalClose.addEventListener("click", closeModal); }
    if (loginModalOverlay) { loginModalOverlay.addEventListener("click", (e) => { if (e.target === loginModalOverlay) closeModal(); }); }
    document.addEventListener("keydown", (e) => { if (e.key === "Escape" && loginModalOverlay && loginModalOverlay.classList.contains("show")) closeModal(); });
    if (registerLink) {
        registerLink.addEventListener("click", (e) => {
            e.preventDefault();
            closeModal();
            const registerModalOverlay = document.getElementById("registerModalOverlay");
            if (registerModalOverlay) registerModalOverlay.classList.add("show");
        });
    }
    passwordToggles.forEach(toggle => {
        toggle.addEventListener("click", function() {
            const wrapper = this.closest('.input-wrapper');
            const passwordInput = wrapper.querySelector('input');
            const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
            passwordInput.setAttribute("type", type);
            const icon = this.querySelector("i");
            icon.classList.toggle("fa-eye");
            icon.classList.toggle("fa-eye-slash");
        });
    });


    // --- XỬ LÝ SUBMIT FORM ĐĂNG NHẬP ---
    if (loginForm) {
        loginForm.addEventListener("submit", function (e) {
            e.preventDefault();

            const submitBtn = this.querySelector(".btn-login-submit");
            const errorMessageDiv = document.getElementById("login-error-message");
            errorMessageDiv.style.display = 'none';

            // 1. Lấy dữ liệu
            const username = document.getElementById("loginUsername").value;
            const password = document.getElementById("loginPassword").value;
            const rememberMe = document.getElementById("rememberMe").checked;

            // 2. Dữ liệu phải gửi dạng form-urlencoded cho Spring Security
            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('password', password);
            if (rememberMe) {
                formData.append('remember-me', 'on');
            }
            
            submitBtn.textContent = "ĐANG ĐĂNG NHẬP...";
            submitBtn.disabled = true;

            // 3. Gọi API /perform_login
            fetch('/perform_login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            })
            .then(response => {
                if (response.ok) {
                    // 4. THÀNH CÔNG: Tải lại trang để server cập nhật trạng thái đã đăng nhập
                    window.location.reload();
                } else {
                    // 5. THẤT BẠI: Lấy lỗi từ server (đã cấu hình ở SecurityConfig)
                    return response.text().then(text => Promise.reject(text));
                }
            })
            .catch(error => {
                errorMessageDiv.textContent = error || "Đăng nhập thất bại. Vui lòng thử lại.";
                errorMessageDiv.style.display = 'block';
            })
            .finally(() => {
                submitBtn.textContent = "ĐĂNG NHẬP";
                submitBtn.disabled = false;
            });
        });
    }
});