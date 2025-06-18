// Login Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    const loginBtn = document.getElementById("loginBtn")
    const loginModalOverlay = document.getElementById("loginModalOverlay")
    const loginModalClose = document.getElementById("loginModalClose")
    const loginForm = document.getElementById("loginForm")

    // Lấy password toggle button trong login modal (dựa theo register modal)
    const passwordToggle = document.querySelector("#loginModalOverlay .password-toggle")

    // Open modal when login button is clicked
    if (loginBtn) {
        loginBtn.addEventListener("click", (e) => {
            e.preventDefault()
            openModal()
        })
    }

    // Close modal when close button is clicked
    if (loginModalClose) {
        loginModalClose.addEventListener("click", () => {
            closeModal()
        })
    }

    // Close modal when clicking outside
    if (loginModalOverlay) {
        loginModalOverlay.addEventListener("click", (e) => {
            if (e.target === loginModalOverlay) {
                closeModal()
            }
        })
    }

    // Close modal with Escape key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && loginModalOverlay && loginModalOverlay.classList.contains("show")) {
            closeModal()
        }
    })

    // Password toggle functionality - COPY TỪNG DÒNG TỪ REGISTER MODAL
    // if (passwordToggle) {
    //     passwordToggle.addEventListener("click", function () {
    //         const passwordInput = this.parentElement.querySelector('input[type="password"], input[type="text"]')
    //         const type = passwordInput.getAttribute("type") === "password" ? "text" : "password"
    //         passwordInput.setAttribute("type", type)

    //         const icon = this.querySelector("i")
    //         if (type === "password") {
    //             icon.classList.remove("fa-eye")
    //             icon.classList.add("fa-eye-slash")
    //         } else {
    //             icon.classList.remove("fa-eye-slash")
    //             icon.classList.add("fa-eye")
    //         }
    //     })
    // }

    // Form submission
    if (loginForm) {
        loginForm.addEventListener("submit", function (e) {
            e.preventDefault()

            const submitBtn = this.querySelector(".btn-login-submit")
            const username = this.querySelector('input[placeholder="Nhập SĐT hoặc email"]').value
            const password = this.querySelector(".input-password").value

            // Basic validation
            if (!username || !password) {
                alert("Vui lòng điền đầy đủ thông tin!")
                return
            }

            // Add loading state
            submitBtn.classList.add("loading")
            submitBtn.disabled = true

            // Simulate login process
            setTimeout(() => {
                submitBtn.classList.remove("loading")
                submitBtn.disabled = false

                // Show success message
                alert(`Đăng nhập thành công!`)
                closeModal()

                // Reset form
                loginForm.reset()

                // Reset password visibility - DÙNG LOGIC TỪ REGISTER MODAL
                if (passwordToggle) {
                    const passwordInput = passwordToggle.parentElement.querySelector("input")
                    passwordInput.setAttribute("type", "password")
                    const icon = passwordToggle.querySelector("i")
                    icon.classList.remove("fa-eye")
                    icon.classList.add("fa-eye-slash")
                }
            }, 2000)
        })
    }

    // Social login buttons
    const googleBtn = document.querySelector(".login-modal .btn-google")
    const facebookBtn = document.querySelector(".login-modal .btn-facebook")

    if (googleBtn) {
        googleBtn.addEventListener("click", () => {
            alert("Đăng nhập với Google - Chức năng đang phát triển")
        })
    }

    if (facebookBtn) {
        facebookBtn.addEventListener("click", () => {
            alert("Đăng nhập với Facebook - Chức năng đang phát triển")
        })
    }

    // Register link
    const registerLink = document.querySelector(".register-now")
    if (registerLink) {
        registerLink.addEventListener("click", (e) => {
            e.preventDefault()
            closeModal()
            // Open login modal if it exists
            const registerBtn = document.getElementById("registerBtn")
            if (registerBtn) {
                registerBtn.click()
            }
        })
    }

    // Functions to open and close modal
    function openModal() {
        if (loginModalOverlay) {
            loginModalOverlay.classList.add("show")
            document.body.style.overflow = "hidden" // Prevent background scrolling

            // Focus on first input
            setTimeout(() => {
                const firstInput = loginForm.querySelector('input[placeholder="Nhập SĐT hoặc email"]')
                if (firstInput) {
                    firstInput.focus()
                }
            }, 300)
        }
    }

    function closeModal() {
        if (loginModalOverlay) {
            loginModalOverlay.classList.remove("show")
            document.body.style.overflow = "" // Restore scrolling

            // Reset form and password visibility
            if (loginForm) {
                loginForm.reset()
                // Reset password visibility - DÙNG LOGIC TỪ REGISTER MODAL
                if (passwordToggle) {
                    const passwordInput = passwordToggle.parentElement.querySelector("input")
                    passwordInput.setAttribute("type", "password")
                    const icon = passwordToggle.querySelector("i")
                    icon.classList.remove("fa-eye")
                    icon.classList.add("fa-eye-slash")
                }
            }
        }
    }

    // Add smooth animations for form elements - COPY TỪ REGISTER MODAL
    const formInputs = document.querySelectorAll(".form-control-login")
    formInputs.forEach((input) => {
        input.addEventListener("focus", function () {
            this.parentElement.style.transform = "scale(1.02)"
        })

        input.addEventListener("blur", function () {
            this.parentElement.style.transform = "scale(1)"
        })
    })
})
