document.addEventListener("DOMContentLoaded", () => {
    // Show/hide password toggle - xử lý tất cả password toggles
    const passwordToggles = document.querySelectorAll(".password-toggle-host");
    const loginForm = document.getElementById("loginFormHost");

    // Xử lý form đăng nhập
    if (loginForm) {
        loginForm.addEventListener("submit", function (event) {
            event.preventDefault(); // Ngăn form tự gửi đi

            const usernameOrEmail = document.getElementById("emailPhone").value;
            const password = document.getElementById("password").value;
            const rememberMe = document.getElementById("rememberMe").checked;
            
            // Dùng URLSearchParams để gửi dữ liệu dạng form, không phải JSON
            const formData = new URLSearchParams();
            formData.append("username", usernameOrEmail); // Tên param phải khớp với SecurityConfig
            formData.append("password", password);
            
            if (rememberMe) {
                // Tên parameter phải là 'remember-me' theo mặc định của Spring Security
                formData.append('remember-me', 'on');
            }
            
            // Gọi đến URL xử lý đăng nhập của Spring Security
            fetch("/login-processing", {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                },
                body: formData,
            })
                .then(response => {
                    if (response.redirected) {
                        // Nếu Spring Security redirect, thì chuyển luôn
                        window.location.href = response.url;
                    } else if (response.ok) {
                        // Nếu backend trả JSON (tùy chỉnh), parse JSON
                        return response.json().then(data => {
                            window.location.href = data.redirectUrl;
                        });
                    } else {
                        throw new Error("Tên đăng nhập hoặc mật khẩu không chính xác.");
                    }
                })
                .catch(error => {
                    console.error("Lỗi đăng nhập:", error);
                    alert(error.message);
                });
        });
    }

    // Xử lý toggle hiển thị/ẩn mật khẩu
    passwordToggles.forEach((toggle) => {
        toggle.addEventListener("click", function () {
            const passwordInput = this.parentElement.querySelector('input[type="password"], input[type="text"]');

            if (passwordInput) {
                const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
                passwordInput.setAttribute("type", type);

                const icon = this.querySelector("i");
                if (icon) {
                    if (type === "password") {
                        icon.classList.remove("fa-eye");
                        icon.classList.add("fa-eye-slash");
                    } else {
                        icon.classList.remove("fa-eye-slash");
                        icon.classList.add("fa-eye");
                    }
                }

                // Focus vào input sau khi toggle
                passwordInput.focus();
            }
        });
    });

    // Xử lý sự kiện click cho nút "Quên mật khẩu?"
    const forgotPasswordLink = document.querySelector(".forgot-password");
    if (forgotPasswordLink) {
        forgotPasswordLink.addEventListener("click", (e) => {
            e.preventDefault(); // Ngăn chặn hành vi mặc định của link

            // Mở modal quên mật khẩu
            const forgotPasswordModalOverlay = document.getElementById("forgotPasswordModalOverlayGuest");
            if (forgotPasswordModalOverlay) {
                forgotPasswordModalOverlay.style.display = "flex";
                forgotPasswordModalOverlay.classList.add("show");
                document.body.style.overflow = "hidden";
            } else {
                console.error("Không tìm thấy modal quên mật khẩu");
            }
        });
    }

    // Xử lý nút đóng trang đăng nhập
    const closeBtn = document.querySelector(".close-btn");
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();

            // Set home page as active in localStorage before navigation
            const homeItemInfo = {
                text: "Trang chủ",
                href: "/trang-chu",
                isDropdownItem: false,
            };
            localStorage.setItem("activeNavItem", JSON.stringify(homeItemInfo));

            // Navigate to home page
            window.location.href = "/trang-chu"
        })
    }
})
document.addEventListener("DOMContentLoaded", function () {
    const modalOverlay = document.getElementById("forgotPasswordModalOverlayGuest");
    const openModalBtn = document.getElementById("forgotPasswordBtnGuest");
    const closeModalBtn = document.getElementById("forgotPasswordModalCloseGuest");
    const backToLoginBtn = document.getElementById("backToLoginBtnGuest");

    if (openModalBtn && modalOverlay) {
        openModalBtn.addEventListener("click", function () {
            modalOverlay.classList.add("show");
        });
    }

    if (closeModalBtn) {
        closeModalBtn.addEventListener("click", function () {
            modalOverlay.style.display = "none";
        });
    }

    if (backToLoginBtn) {
        backToLoginBtn.addEventListener("click", function () {
            modalOverlay.style.display = "none";
        });
    }

    // Đóng modal khi nhấn ra ngoài
    window.addEventListener("click", function (event) {
        if (event.target === modalOverlay) {
            modalOverlay.style.display = "none";
        }
    });
});
