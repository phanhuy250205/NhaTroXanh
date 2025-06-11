// Login Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    const loginBtn = document.getElementById("loginBtn")
    const loginModalOverlay = document.getElementById("loginModalOverlay")
    const loginModalClose = document.getElementById("loginModalClose")
    const loginForm = document.getElementById("loginForm")
    const passwordToggle = document.querySelector(".password-toggle")
    const passwordInput = document.querySelector('input[type="password"]')

    // Open modal when login button is clicked
    loginBtn.addEventListener("click", (e) => {
        e.preventDefault()
        openModal()
    })

    // Close modal when close button is clicked
    loginModalClose.addEventListener("click", () => {
        closeModal()
    })

    // Close modal when clicking outside
    loginModalOverlay.addEventListener("click", (e) => {
        if (e.target === loginModalOverlay) {
            closeModal()
        }
    })

    // Close modal with Escape key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && loginModalOverlay.classList.contains("show")) {
            closeModal()
        }
    })

    // Password toggle functionality
    passwordToggle.addEventListener("click", function () {
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

    // Form submission
    loginForm.addEventListener("submit", function (e) {
        e.preventDefault()

        const submitBtn = this.querySelector(".btn-login-submit")
        const text = this.querySelector('input[type="text"]').value
        const password = this.querySelector('input[type="password"]').value

        // Basic validation
        if (!text || !password) {
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
            // alert("Đăng nhập thành công!")
            closeModal()

            // Reset form
            loginForm.reset()

            // Set active state for home page before redirect
            const homeLink = document.querySelector('.nav-link[href="/trang-chu"]')
            if (homeLink) {
                // Remove all active classes first
                document.querySelectorAll(".nav-link").forEach((link) => {
                link.classList.remove("active")
                })
                document.querySelectorAll(".dropdown-item").forEach((item) => {
                item.classList.remove("active")
                })

                // Set home page as active
                homeLink.classList.add("active")

                // Update localStorage to reflect home page as active
                const itemInfo = {
                text: homeLink.textContent.trim(),
                href: homeLink.getAttribute("href"),
                isDropdownItem: false,
                }
                localStorage.setItem("activeNavItem", JSON.stringify(itemInfo))
            }
            
            // Redirect to home page after successful login
            setTimeout(() => {
                window.location.href = "/trang-chu"
            }, 500)
        }, 2000)
    })

    // Social login buttons
    document.querySelector(".btn-google").addEventListener("click", () => {
        alert("Đăng nhập với Google - Chức năng đang phát triển")
    })

    document.querySelector(".btn-facebook").addEventListener("click", () => {
        alert("Đăng nhập với Facebook - Chức năng đang phát triển")
    })

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

    // Forgot password link
    document.querySelector(".forgot-password").addEventListener("click", (e) => {
        e.preventDefault()
        alert("Chuyển đến trang quên mật khẩu - Chức năng đang phát triển")
    })

    // Functions to open and close modal
    function openModal() {
        loginModalOverlay.classList.add("show")
        document.body.style.overflow = "hidden" // Prevent background scrolling

        // Focus on first input
        setTimeout(() => {
            const firstInput = loginForm.querySelector('input[type="text"]')
            if (firstInput) {
                firstInput.focus()
            }
        }, 300)
    }

    function closeModal() {
        loginModalOverlay.classList.remove("show")
        document.body.style.overflow = "" // Restore scrolling

        // Reset form and password visibility
        loginForm.reset()
        passwordInput.setAttribute("type", "password")
        const icon = passwordToggle.querySelector("i")
        icon.classList.remove("fa-eye")
        icon.classList.add("fa-eye-slash")
    }

    // Add smooth animations for form elements
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
