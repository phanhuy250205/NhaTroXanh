document.addEventListener("DOMContentLoaded", () => {
    // Show/hide password toggle - xử lý tất cả password toggles
    const passwordToggles = document.querySelectorAll(".password-toggle-host")
      const registerForm = document.getElementById("registerForm");

   if (registerForm) {
        registerForm.addEventListener("submit", function (event) {
            event.preventDefault();

            // Lấy dữ liệu từ các input
            const fullName = document.getElementById("fullName").value;
            const email = document.getElementById("email").value;
            const phoneNumber = document.getElementById("phoneNumber").value;
            const password = document.getElementById("password").value;
            const confirmPassword = document.getElementById("confirmPassword").value;
            const agreeTerms = document.getElementById("agreeTerms").checked;
            const birthDate = document.getElementById("birthDate") ? document.getElementById("birthDate").value : null;


            if (password !== confirmPassword) {
                alert("Mật khẩu và xác nhận mật khẩu không khớp!");
                return;
            }
            if (!agreeTerms) {
                alert("Bạn phải đồng ý với Điều khoản và Chính sách bảo mật.");
                return;
            }

            // Chuẩn bị dữ liệu để gửi đi
            const userRequest = {
                fullName: fullName,
                email: email,
                phoneNumber: phoneNumber,
                password: password,
                birthDate: birthDate
            };

            fetch("/api/users/register-owner", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(userRequest),
            })
            .then(response => {
                if (response.ok) {
                    // ---- PHẦN THAY ĐỔI ----
                    // 1. Thay đổi thông báo
                    alert("Đăng ký thành công! Đang chuyển đến trang đăng nhập.");
                    // 2. Chuyển hướng thẳng đến trang đăng nhập của chủ trọ
                    window.location.href = '/dang-nhap-chu-tro'; 
                    // -----------------------
                } else {
                    return response.text().then(text => { throw new Error(text || "Lỗi không xác định.") });
                }
            })
            .catch(error => {
                console.error("Lỗi đăng ký:", error);
                alert("Đăng ký thất bại: " + error.message);
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
