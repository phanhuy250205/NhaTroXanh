document.addEventListener("DOMContentLoaded", () => {
    // Show/hide password toggle - xử lý tất cả password toggles
    const passwordToggles = document.querySelectorAll(".password-toggle-host")
      const registerForm = document.getElementById("registerForm");

   if (registerForm) {
        registerForm.addEventListener("submit", function (event) {
            event.preventDefault();
            if (!validateForm()) return;
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

    // Xử lý form validation
    if (registerForm) {
        // Thêm event listener cho từng input để xóa lỗi khi user nhập
        const inputs = registerForm.querySelectorAll("input")
        inputs.forEach((input) => {
            input.addEventListener("input", () => {
                clearFieldError(input)
            })

            input.addEventListener("change", () => {
                clearFieldError(input)
            })
        })

        registerForm.addEventListener("submit", (e) => {
            e.preventDefault()

            // Validate form
            if (validateForm()) {
                console.log("Form hợp lệ, đang xử lý đăng ký...")
                // Thực hiện logic đăng ký ở đây
            }
        })
    }

    // Function để clear error cho một field
    function clearFieldError(input) {
        // Loại bỏ tất cả các class validation của Bootstrap
        input.classList.remove("is-invalid", "is-valid")

        const errorMessage = input.parentElement.querySelector(".error-message")
        if (errorMessage) {
            errorMessage.classList.remove("show")
        }

        // Xử lý riêng cho checkbox terms
        if (input.id === "agreeTerms") {
            const checkmark = input.parentElement.querySelector(".checkmark")
            if (checkmark) {
                checkmark.classList.remove("is-invalid")
            }
            const termsError = document.querySelector(".terms-error")
            if (termsError) {
                termsError.classList.remove("show")
            }
        }
    }

    // Function để hiển thị error cho một field
    function showFieldError(input, message = null) {
        // Chỉ thêm class tùy chỉnh, không dùng Bootstrap
        input.classList.add("is-invalid")
        input.classList.remove("is-valid") // Đảm bảo không có class valid

        const errorMessage = input.parentElement.querySelector(".error-message")
        if (errorMessage) {
            if (message) {
                errorMessage.textContent = message
            }
            errorMessage.classList.add("show")
        }

        // Xử lý riêng cho checkbox terms
        if (input.id === "agreeTerms") {
            const checkmark = input.parentElement.querySelector(".checkmark")
            if (checkmark) {
                checkmark.classList.add("is-invalid")
            }
            const termsError = document.querySelector(".terms-error")
            if (termsError) {
                termsError.classList.add("show")
            }
        }
    }

    // Function để validate form
    function validateForm() {
        let isValid = true
        const form = document.getElementById("registerForm")

        // Clear all previous errors
        const allInputs = form.querySelectorAll("input")
        allInputs.forEach((input) => clearFieldError(input))

        // Validate họ tên
        const fullName = document.getElementById("fullName")
        if (!fullName.value.trim()) {
            showFieldError(fullName, "Vui lòng nhập họ và tên.")
            isValid = false
        } else if (fullName.value.trim().length < 2) {
            showFieldError(fullName, "Họ tên phải có ít nhất 2 ký tự.")
            isValid = false
        }

        // Validate ngày sinh
        const birthDate = document.getElementById("birthDate")
        if (!birthDate.value) {
            showFieldError(birthDate, "Vui lòng nhập ngày sinh.")
            isValid = false
        } else {
            const today = new Date()
            const birth = new Date(birthDate.value)
            const age = today.getFullYear() - birth.getFullYear()
            if (age < 18) {
                showFieldError(birthDate, "Bạn phải đủ 18 tuổi để đăng ký.")
                isValid = false
            }
        }

        // Validate email
        const email = document.getElementById("email")
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!email.value.trim()) {
            showFieldError(email, "Vui lòng nhập email.")
            isValid = false
        } else if (!emailRegex.test(email.value)) {
            showFieldError(email, "Vui lòng nhập email hợp lệ.")
            isValid = false
        }

        // Validate số điện thoại
        const phone = document.getElementById("phoneNumber")
        const phoneRegex = /^[0-9]{10,11}$/
        if (!phone.value.trim()) {
            showFieldError(phone, "Vui lòng nhập số điện thoại.")
            isValid = false
        } else if (!phoneRegex.test(phone.value.replace(/\s/g, ""))) {
            showFieldError(phone, "Số điện thoại phải có 10-11 chữ số.")
            isValid = false
        }

        // Validate mật khẩu
        const password = document.getElementById("password")
        if (!password.value) {
            showFieldError(password, "Vui lòng nhập mật khẩu.")
            isValid = false
        } else if (password.value.length < 6) {
            showFieldError(password, "Mật khẩu phải có ít nhất 6 ký tự.")
            isValid = false
        }

        // Validate xác nhận mật khẩu
        const confirmPassword = document.getElementById("confirmPassword")
        if (!confirmPassword.value) {
            showFieldError(confirmPassword, "Vui lòng xác nhận mật khẩu.")
            isValid = false
        } else if (password.value !== confirmPassword.value) {
            showFieldError(confirmPassword, "Mật khẩu xác nhận không khớp.")
            isValid = false
        }

        // Validate điều khoản
        const agreeTerms = document.getElementById("agreeTerms")
        if (!agreeTerms.checked) {
            showFieldError(agreeTerms)
            isValid = false
        }

        return isValid
    }
})
