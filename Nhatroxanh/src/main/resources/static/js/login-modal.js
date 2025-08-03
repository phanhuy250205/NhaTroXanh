document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN ---
    const loginBtn = document.getElementById("loginBtn");
    const loginModalOverlay = document.getElementById("loginModalOverlay");
    const loginModalClose = document.getElementById("loginModalClose");
    const loginForm = document.getElementById("loginForm");
    const passwordToggles = document.querySelectorAll(".login-modal .password-toggle");
    const registerLink = document.querySelector(".login-modal .register-now");
    const forgotPasswordLink = document.querySelector(".login-modal .forgot-password");
    const errorMessageDiv = document.getElementById("login-error-message");

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

    if (loginBtn) {
        loginBtn.addEventListener("click", (e) => {
            e.preventDefault();
            openModal();
        });
    }

    if (loginModalClose) {
        loginModalClose.addEventListener("click", closeModal);
    }

    if (loginModalOverlay) {
        loginModalOverlay.addEventListener("click", (e) => {
            if (e.target === loginModalOverlay) closeModal();
        });
    }

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && loginModalOverlay && loginModalOverlay.classList.contains("show")) closeModal();
    });

    if (registerLink) {
        registerLink.addEventListener("click", (e) => {
            e.preventDefault();
            closeModal();
            const registerModalOverlay = document.getElementById("registerModalOverlay");
            if (registerModalOverlay) registerModalOverlay.classList.add("show");
        });
    }

    if (forgotPasswordLink) {
        forgotPasswordLink.addEventListener("click", (e) => {
            e.preventDefault();
            closeModal();
            const forgotPasswordModalOverlay = document.getElementById("forgotPasswordModalOverlayGuest");
            if (forgotPasswordModalOverlay) {
                forgotPasswordModalOverlay.classList.add("show");
                document.body.style.overflow = "hidden";
            }
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

            if (errorMessageDiv) {
                errorMessageDiv.style.display = 'none'; // Ẩn thông báo lỗi cũ
            }

            const username = document.getElementById("loginUsername").value;
            const password = document.getElementById("loginPassword").value;
            const rememberMe = document.getElementById("rememberMe").checked;

            const formData = new URLSearchParams();
            formData.append('username', username);
            formData.append('password', password);
            if (rememberMe) {
                formData.append('remember-me', 'on');
            }

            fetch('/login-processing', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData
            })
            .then(response => {
                if (response.ok) {
                    window.location.reload();
                } else {
                    throw new Error("Tên đăng nhập hoặc mật khẩu không chính xác.");
                }
            })
            .catch(error => {
                if (errorMessageDiv) {
                    errorMessageDiv.textContent = error.message;
                    errorMessageDiv.style.display = 'block';
                } else {
                    alert(error.message);
                }
            });
        });
    }
});