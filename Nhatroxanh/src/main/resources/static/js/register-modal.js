// Register Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    const registerBtn = document.querySelector(".btn-register")
    const registerModalOverlay = document.getElementById("registerModalOverlay")
    const registerModalClose = document.getElementById("registerModalClose")
    const registerForm = document.getElementById("registerForm")
    const passwordToggles = document.querySelectorAll(".password-toggle")

    // Open modal when register button is clicked
    if (registerBtn) {
        registerBtn.addEventListener("click", (e) => {
            e.preventDefault()
            openModal()
        })
    }

    // Close modal when close button is clicked
    if (registerModalClose) {
        registerModalClose.addEventListener("click", () => {
            closeModal()
        })
    }

    // Close modal when clicking outside
    if (registerModalOverlay) {
        registerModalOverlay.addEventListener("click", (e) => {
            if (e.target === registerModalOverlay) {
                closeModal()
            }
        })
    }

    // Close modal with Escape key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && registerModalOverlay && registerModalOverlay.classList.contains("show")) {
            closeModal()
        }
    })

    // Password toggle functionality
    passwordToggles.forEach((toggle) => {
        toggle.addEventListener("click", function () {
            const passwordInput = this.parentElement.querySelector('input[type="password"], input[type="text"]')
            const type = passwordInput.getAttribute("type") === "password" ? "text" : "password"
            passwordInput.setAttribute("type", type)

            const icon = this.querySelector("i")
            if (type === "password") {
                icon.classList.remove("fa-eye")
                icon.classList.add("fa-eye-slash")
            } else {
                icon.classList.remove("fa-eye-slash")
                icon.classList.add("fa-eye")
            }
        })
    })

    // Form submission
    // if (registerForm) {
    //     registerForm.addEventListener("submit", function (e) {
    //         e.preventDefault()

    //         const submitBtn = this.querySelector(".btn-register-submit")
    //         const fullName = this.querySelector('input[placeholder="Nhập Họ và tên"]').value
    //         const email = this.querySelector('input[placeholder="Nhập Email hoặc Số điện thoại"]').value
    //         const password = this.querySelector('input[placeholder="Nhập mật khẩu"]').value
    //         const confirmPassword = this.querySelector('input[placeholder="Nhập mật khẩu"]').value

    //         // Basic validation
    //         if (!fullName || !email || !password || !confirmPassword) {
    //             alert("Vui lòng điền đầy đủ thông tin!")
    //             return
    //         }

    //         if (password !== confirmPassword) {
    //             alert("Mật khẩu xác nhận không khớp!")
    //             return
    //         }

    //         // Email validation
    //         const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    //         const phoneRegex = /^[0-9]{10,11}$/

    //         if (!emailRegex.test(email) && !phoneRegex.test(email)) {
    //             alert("Vui lòng nhập email hoặc số điện thoại hợp lệ!")
    //             return
    //         }

    //         // Add loading state
    //         submitBtn.classList.add("loading")
    //         submitBtn.disabled = true

    //         // Simulate registration process
    //         setTimeout(() => {
    //             submitBtn.classList.remove("loading")
    //             submitBtn.disabled = false

    //             // Show success message
    //             // alert(`Đăng ký thành công!`)
    //             closeModal()

    //             // Reset form
    //             registerForm.reset()

    //             // Reset password visibility
    //             passwordToggles.forEach((toggle) => {
    //                 const passwordInput = toggle.parentElement.querySelector("input")
    //                 passwordInput.setAttribute("type", "password")
    //                 const icon = toggle.querySelector("i")
    //                 icon.classList.remove("fa-eye")
    //                 icon.classList.add("fa-eye-slash")
    //             })
    //         }, 2000)
    //     })
    // }

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
    const loginLink = document.querySelector(".login-now")
    if (loginLink) {
        loginLink.addEventListener("click", (e) => {
            e.preventDefault()
            closeModal()
            // Open login modal if it exists
            const loginBtn = document.getElementById("loginBtn")
            if (loginBtn) {
                loginBtn.click()
            }
        })
    }

    // Functions to open and close modal
    function openModal() {
        if (registerModalOverlay) {
            registerModalOverlay.classList.add("show")
            document.body.style.overflow = "hidden" // Prevent background scrolling

            // Focus on first input
            setTimeout(() => {
                const firstInput = registerForm.querySelector('input[placeholder="Nhập Họ và tên"]')
                if (firstInput) {
                    firstInput.focus()
                }
            }, 300)
        }
    }

    function closeModal() {
        if (registerModalOverlay) {
            registerModalOverlay.classList.remove("show")
            document.body.style.overflow = "" // Restore scrolling

            // Reset form and password visibility
            if (registerForm) {
                registerForm.reset()
                passwordToggles.forEach((toggle) => {
                    const passwordInput = toggle.parentElement.querySelector("input")
                    passwordInput.setAttribute("type", "password")
                    const icon = toggle.querySelector("i")
                    icon.classList.remove("fa-eye")
                    icon.classList.add("fa-eye-slash")
                })
            }
        }
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
    })
})
