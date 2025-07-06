// Forgot Password Modal JavaScript - Updated with Guest suffix
document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN CHO FORGOT PASSWORD MODAL ---
    const forgotPasswordModalOverlay = document.getElementById("forgotPasswordModalOverlayGuest")
    const forgotPasswordModalClose = document.getElementById("forgotPasswordModalCloseGuest")
    const forgotPasswordForm = document.getElementById("forgotPasswordFormGuest")
    const backToLoginBtn = document.getElementById("backToLoginBtnGuest")

    // --- HÀM VÀ SỰ KIỆN UI CHO FORGOT PASSWORD MODAL ---
    function openForgotPasswordModal() {
        if (forgotPasswordModalOverlay) {
            forgotPasswordModalOverlay.classList.add("show")
            document.body.style.overflow = "hidden"
        }
    }

    function closeForgotPasswordModal() {
        if (forgotPasswordModalOverlay) {
            forgotPasswordModalOverlay.classList.remove("show")
            document.body.style.overflow = ""
        }
    }

    function switchToLoginModal() {
        closeForgotPasswordModal()
        const loginModalOverlay = document.getElementById("loginModalOverlay")
        if (loginModalOverlay) {
            loginModalOverlay.classList.add("show")
            document.body.style.overflow = "hidden"
        }
    }

    // Event listeners cho forgot password modal
    if (forgotPasswordModalClose) {
        forgotPasswordModalClose.addEventListener("click", closeForgotPasswordModal)
    }

    if (backToLoginBtn) {
        backToLoginBtn.addEventListener("click", switchToLoginModal)
    }

    if (forgotPasswordModalOverlay) {
        forgotPasswordModalOverlay.addEventListener("click", (e) => {
            if (e.target === forgotPasswordModalOverlay) {
                closeForgotPasswordModal()
            }
        })
    }

    // Xử lý ESC key cho forgot password modal
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && forgotPasswordModalOverlay && forgotPasswordModalOverlay.classList.contains("show")) {
            closeForgotPasswordModal()
        }
    })

    // --- XỬ LÝ SUBMIT FORM FORGOT PASSWORD ---
    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener("submit", (e) => {
            e.preventDefault()

            const errorMessageDiv = document.getElementById("forgot-password-error-message-guest")
            const successMessageDiv = document.getElementById("forgot-password-success-message-guest")
            const submitBtn = document.querySelector(".btn-forgot-password-submit-guest")

            // Ẩn thông báo cũ
            errorMessageDiv.style.display = "none"
            successMessageDiv.style.display = "none"

            // Lấy dữ liệu từ form
            const contact = document.getElementById("forgotPasswordContactGuest").value.trim()

            // Validation cơ bản
            if (!contact) {
                errorMessageDiv.textContent = "Vui lòng nhập email hoặc số điện thoại."
                errorMessageDiv.style.display = "block"
                return
            }

            // Thêm loading state
            submitBtn.classList.add("loading")
            submitBtn.disabled = true

            // Chuẩn bị dữ liệu để gửi
            const formData = new URLSearchParams()
            formData.append("contact", contact)

            // Gọi API forgot password (thay đổi URL theo backend của bạn)
            fetch("/forgot-password", {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: formData,
            })
                .then((response) => {
                    if (response.ok) {
                        // Thành công
                        successMessageDiv.textContent =
                            "Yêu cầu khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra email hoặc tin nhắn của bạn."
                        successMessageDiv.style.display = "block"
                        // Reset form
                        forgotPasswordForm.reset()
                    } else {
                        throw new Error("Không thể gửi yêu cầu. Vui lòng thử lại sau.")
                    }
                })
                .catch((error) => {
                    errorMessageDiv.textContent = error.message
                    errorMessageDiv.style.display = "block"
                })
                .finally(() => {
                    // Xóa loading state
                    submitBtn.classList.remove("loading")
                    submitBtn.disabled = false
                })
        })
    }
})
