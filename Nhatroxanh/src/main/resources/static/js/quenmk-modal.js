document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN CHO FORGOT PASSWORD MODAL ---
    const forgotPasswordModalOverlay = document.getElementById("forgotPasswordModalOverlayGuest");
    const forgotPasswordModalClose = document.getElementById("forgotPasswordModalCloseGuest");
    const forgotPasswordForm = document.getElementById("forgotPasswordFormGuest");
    const backToLoginBtn = document.getElementById("backToLoginBtnGuest");

    // --- HÀM VÀ SỰ KIỆN UI CHO FORGOT PASSWORD MODAL ---
    function openForgotPasswordModal() {
        if (forgotPasswordModalOverlay) {
            forgotPasswordModalOverlay.classList.add("show");
            document.body.style.overflow = "hidden";
        }
    }

    function closeForgotPasswordModal() {
        if (forgotPasswordModalOverlay) {
            forgotPasswordModalOverlay.classList.remove("show");
            document.body.style.overflow = "";
        }
    }

    function switchToLoginModal() {
        closeForgotPasswordModal();
        const loginModalOverlay = document.getElementById("loginModalOverlay");
        if (loginModalOverlay) {
            loginModalOverlay.classList.add("show");
            document.body.style.overflow = "hidden";
        }
    }

    // Event listeners cho forgot password modal
    if (forgotPasswordModalClose) {
        forgotPasswordModalClose.addEventListener("click", closeForgotPasswordModal);
    }

    if (backToLoginBtn) {
        backToLoginBtn.addEventListener("click", switchToLoginModal);
    }

    if (forgotPasswordModalOverlay) {
        forgotPasswordModalOverlay.addEventListener("click", (e) => {
            if (e.target === forgotPasswordModalOverlay) {
                closeForgotPasswordModal();
            }
        });
    }

    // Xử lý ESC key cho forgot password modal
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && forgotPasswordModalOverlay && forgotPasswordModalOverlay.classList.contains("show")) {
            closeForgotPasswordModal();
        }
    });

    // --- XỬ LÝ SUBMIT FORM FORGOT PASSWORD ---
    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener("submit", (e) => {
            e.preventDefault();

            const errorMessageDiv = document.getElementById("forgot-password-error-message-guest");
            const successMessageDiv = document.getElementById("forgot-password-success-message-guest");
            const submitBtn = document.querySelector(".btn-forgot-password-submit-guest");

            // Ẩn thông báo cũ
            if (errorMessageDiv) errorMessageDiv.style.display = "none";
            if (successMessageDiv) successMessageDiv.style.display = "none";

            // Lấy dữ liệu từ form
            const contact = document.getElementById("forgotPasswordContactGuest").value.trim();

            // Validation cơ bản
            if (!contact) {
                if (errorMessageDiv) {
                    errorMessageDiv.textContent = "Vui lòng nhập email.";
                    errorMessageDiv.style.display = "block";
                }
                return;
            }

            // Kiểm tra định dạng email
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (!emailRegex.test(contact)) {
                if (errorMessageDiv) {
                    errorMessageDiv.textContent = "Vui lòng nhập email hợp lệ.";
                    errorMessageDiv.style.display = "block";
                }
                return;
            }

            // Thêm loading state
            if (submitBtn) {
                submitBtn.classList.add("loading");
                submitBtn.disabled = true;
            }

            // Gửi yêu cầu đến đúng endpoint với định dạng JSON
            fetch("/api/auth/forgot-password", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: contact }),
            })
                .then((response) => {
                    return response.json().then((data) => {
                        if (!response.ok) {
                            throw new Error(data.message || "Có lỗi xảy ra, vui lòng thử lại.");
                        }
                        return data;
                    });
                })
                .then((data) => {
                    if (successMessageDiv) {
                        successMessageDiv.textContent = data.message || "Yêu cầu khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra email của bạn.";
                        successMessageDiv.style.display = "block";
                    }
                    forgotPasswordForm.reset();
                    // Mở modal xác thực OTP
                    window.openVerificationModalAuth(contact);
                })
                .catch((error) => {
                    if (errorMessageDiv) {
                        errorMessageDiv.textContent = error.message;
                        errorMessageDiv.style.display = "block";
                    }
                })
                .finally(() => {
                    // Xóa loading state
                    if (submitBtn) {
                        submitBtn.classList.remove("loading");
                        submitBtn.disabled = false;
                    }
                });
        });
    }
});