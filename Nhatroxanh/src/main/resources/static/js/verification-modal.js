// Verification Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    const verificationModalOverlay = document.getElementById("verificationModalOverlay")
    const verificationForm = document.getElementById("verificationForm")
    const resendCodeBtn = document.getElementById("resendCodeBtn")
    const verificationInput = document.getElementById("verificationInput")
    const verificationSuccess = document.getElementById("verificationSuccess")

    // Show verification modal after registration
    // This would typically be called after successful registration
    function showVerificationModal() {
        if (verificationModalOverlay) {
            verificationModalOverlay.classList.add("show")
            document.body.style.overflow = "hidden" // Prevent background scrolling

            // Focus on verification input
            setTimeout(() => {
                if (verificationInput) {
                    verificationInput.focus()
                }
            }, 300)

            // Start countdown for resend button
            startResendCountdown()
        }
    }

    // Close verification modal
    function closeVerificationModal() {
        if (verificationModalOverlay) {
            verificationModalOverlay.classList.remove("show")
            document.body.style.overflow = "" // Restore scrolling

            // Reset form
            if (verificationForm) {
                verificationForm.reset()
            }
        }
    }

    // Handle form submission
    if (verificationForm) {
        verificationForm.addEventListener("submit", function (e) {
            e.preventDefault()

            const code = verificationInput.value.trim()
            const submitBtn = this.querySelector(".btn-verification-submit")

            // Basic validation
            if (!code) {
                alert("Vui lòng nhập mã xác thực!")
                return
            }

            // Add loading state
            submitBtn.classList.add("loading")
            submitBtn.disabled = true

            // Simulate verification process
            setTimeout(() => {
                submitBtn.classList.remove("loading")
                submitBtn.disabled = false

                // Close verification modal
                closeVerificationModal()

                // Show success message
                showSuccessMessage()

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

                // Redirect to home page after a delay
                setTimeout(() => {
                    window.location.href = "/trang-chu"
                }, 500)
            }, 2000)
        })
    }

    // Resend code functionality
    if (resendCodeBtn) {
        resendCodeBtn.addEventListener("click", function () {
            if (this.disabled) return

            // Simulate resending code
            alert("Mã xác thực mới đã được gửi!")

            // Restart countdown
            startResendCountdown()
        })
    }

    // Start countdown for resend button
    function startResendCountdown() {
        if (!resendCodeBtn) return

        let seconds = 50
        resendCodeBtn.disabled = true
        resendCodeBtn.textContent = `Gửi lại mã (${seconds}s)`

        const countdownInterval = setInterval(() => {
            seconds--
            resendCodeBtn.textContent = `Gửi lại mã (${seconds}s)`

            if (seconds <= 0) {
                clearInterval(countdownInterval)
                resendCodeBtn.disabled = false
                resendCodeBtn.textContent = "Gửi lại mã"
            }
        }, 1000)
    }

    // Show success message
    function showSuccessMessage() {
        if (verificationSuccess) {
            verificationSuccess.classList.add("show")

            // Hide success message after a delay
            setTimeout(() => {
                verificationSuccess.classList.remove("show")
            }, 3000)
        }
    }

    // Connect register form submission to show verification modal
    const registerForm = document.getElementById("registerForm")
    if (registerForm) {
        const originalSubmitHandler = registerForm.onsubmit

        registerForm.onsubmit = function (e) {
            e.preventDefault()

            const submitBtn = this.querySelector(".btn-register-submit")
            const fullName = this.querySelector('input[placeholder="Nhập Họ và tên"]').value
            const email = this.querySelector('input[placeholder="Nhập Email hoặc Số điện thoại"]').value
            const password = this.querySelector('input[placeholder="Nhập mật khẩu"]').value
            const confirmPassword = this.querySelector('input[placeholder="Xác nhận mật khẩu"]').value

            // Basic validation
            if (!fullName || !email || !password || !confirmPassword) {
                alert("Vui lòng điền đầy đủ thông tin!")
                return
            }

            if (password !== confirmPassword) {
                alert("Mật khẩu xác nhận không khớp!")
                return
            }

            // Add loading state
            submitBtn.classList.add("loading")
            submitBtn.disabled = true

            // Simulate registration process
            setTimeout(() => {
                submitBtn.classList.remove("loading")
                submitBtn.disabled = false

                // Close register modal
                const registerModalOverlay = document.getElementById("registerModalOverlay")
                if (registerModalOverlay) {
                    registerModalOverlay.classList.remove("show")
                }

                // Show verification modal
                showVerificationModal()

                // Reset register form
                registerForm.reset()
            }, 2000)
        }
    }

    // Expose function to global scope for testing
    window.showVerificationModal = showVerificationModal
})
