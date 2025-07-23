document.addEventListener("DOMContentLoaded", () => {
    // Show/hide password toggle - xử lý tất cả password toggles
    const passwordToggles = document.querySelectorAll(".password-toggle-host")
    const loginForm = document.getElementById("loginFormHost");

    if (loginForm) {
        // Lắng nghe sự kiện submit của form
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
                .then(async response => {
                    if (response.ok) {
                        const data = await response.json(); // Lấy redirectUrl từ backend
                        window.location.href = data.redirectUrl; // Chuyển trang
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

    passwordToggles.forEach((toggle) => {
        toggle.addEventListener("click", function () {
            const passwordInput = this.parentElement.querySelector('input[type="password"], input[type="text"]')

            if (passwordInput) {
                const type = passwordInput.getAttribute("type") === "password" ? "text" : "password"
                passwordInput.setAttribute("type", type)

                const icon = this.querySelector("i")
                if (icon) {
                    if (type === "password") {
                        icon.classList.remove("fa-eye")
                        icon.classList.add("fa-eye-slash")
                    } else {
                        icon.classList.remove("fa-eye-slash")
                        icon.classList.add("fa-eye")
                    }
                }

                // Focus vào input sau khi toggle
                passwordInput.focus()
            }
        })
    })

    // Handle close button click to set home page as active and navigate
    // Tìm thẻ <a> chứa button close
    // const closeBtnLink = document.querySelector('a[href*="trang-chu"]')
    const closeBtn = document.querySelector(".close-btn")

    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault()

            // Set home page as active in localStorage before navigation
            const homeItemInfo = {
                text: "Trang chủ",
                href: "/trang-chu",
                isDropdownItem: false,
            }
            localStorage.setItem("activeNavItem", JSON.stringify(homeItemInfo))

            // Navigate to home page
            window.location.href = "/trang-chu"
        })
    }
})