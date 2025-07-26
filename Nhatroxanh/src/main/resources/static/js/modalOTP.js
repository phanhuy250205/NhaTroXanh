// Verification Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN ---
    const verificationModalOverlay = document.getElementById("verificationModalOverlayAuth");
    const verificationForm = document.getElementById("verificationFormAuth");
    const resendCodeBtn = document.getElementById("resendCodeBtnAuth");
    const verificationInput = document.getElementById("verificationInputAuth");
    const messageDiv = document.getElementById("verification-message-auth");
    const authModalCloseBtn = document.getElementById("authModalCloseBtnAuth");
    const emailDisplay = document.getElementById("emailDisplayAuth");

    let countdownInterval;
    let currentEmail = "";
    let resendCount = 0;
    const maxResend = 3;

    // --- UTILITY FUNCTIONS ---
    function showMessage(message, type = "danger") {
        if (messageDiv) {
            messageDiv.className = `alert-auth alert-${type}-auth`;
            messageDiv.textContent = message;
            messageDiv.style.display = "block";
        }
    }

    function hideMessage() {
        if (messageDiv) {
            messageDiv.style.display = "none";
        }
    }

    function startCountdown() {
        if (!resendCodeBtn) return;

        const countdownTimer = document.getElementById("countdownTimerAuth");
        const countdownSpan = document.getElementById("countdownAuth");

        if (!countdownTimer || !countdownSpan) return;

        let timeLeft = 300; // 5 phút
        resendCodeBtn.style.display = "none";
        countdownTimer.style.display = "block";

        countdownInterval = setInterval(() => {
            const minutes = Math.floor(timeLeft / 60);
            const seconds = timeLeft % 60;
            countdownSpan.textContent = `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
            timeLeft--;

            if (timeLeft < 0) {
                clearInterval(countdownInterval);
                countdownTimer.style.display = "none";
                resendCodeBtn.style.display = "block";
                resendCodeBtn.disabled = resendCount >= maxResend;
                resendCodeBtn.textContent = resendCount >= maxResend ? "Đã hết lượt gửi lại" : "Gửi lại mã";
            }
        }, 1000);
    }

    function resetResendButton() {
        if (resendCodeBtn) {
            resendCodeBtn.style.display = "block";
            resendCodeBtn.disabled = resendCount >= maxResend;
            resendCodeBtn.textContent = resendCount >= maxResend ? "Đã hết lượt gửi lại" : "Gửi lại mã";
        }

        const countdownTimer = document.getElementById("countdownTimerAuth");
        if (countdownTimer) {
            countdownTimer.style.display = "none";
        }

        if (countdownInterval) {
            clearInterval(countdownInterval);
        }
    }

    // --- PUBLIC FUNCTIONS ---
    window.openVerificationModalAuth = (email) => {
        if (!email) {
            console.error("Email is required to open verification modal");
            return;
        }

        currentEmail = email;

        if (emailDisplay) {
            emailDisplay.textContent = email;
        }

        if (verificationModalOverlay) {
            verificationModalOverlay.dataset.email = email;
            verificationModalOverlay.classList.add("show");
            document.body.style.overflow = "hidden";
        }

        if (verificationForm) {
            verificationForm.reset();
        }
        hideMessage();
        resetResendButton();
        resendCount = 0; // Reset số lần gửi lại
        startCountdown();
    };

    window.closeVerificationModalAuth = () => {
        if (verificationModalOverlay) {
            verificationModalOverlay.classList.remove("show");
            document.body.style.overflow = "";
        }

        if (countdownInterval) {
            clearInterval(countdownInterval);
        }

        if (verificationForm) {
            verificationForm.reset();
        }
        hideMessage();
        resetResendButton();
    };

    // --- EVENT LISTENERS ---

    if (authModalCloseBtn && verificationModalOverlay) {
        authModalCloseBtn.addEventListener("click", () => {
            window.closeVerificationModalAuth();
        });
    }

    if (verificationModalOverlay) {
        verificationModalOverlay.addEventListener("click", (e) => {
            if (e.target === verificationModalOverlay) {
                window.closeVerificationModalAuth();
            }
        });
    }

    if (verificationInput) {
        verificationInput.addEventListener("input", (e) => {
            e.target.value = e.target.value.replace(/[^0-9]/g, "");
            if (e.target.value.length > 6) {
                e.target.value = e.target.value.slice(0, 6);
            }
        });
    }

    if (verificationForm) {
        verificationForm.addEventListener("submit", function (e) {
            e.preventDefault();

            const otp = verificationInput.value.trim();
            const email = verificationModalOverlay.dataset.email || currentEmail;
            const submitBtn = this.querySelector(".btn-verification-submit-auth");

            hideMessage();

            if (!otp) {
                showMessage("Vui lòng nhập mã xác thực.");
                return;
            }

            if (otp.length !== 6) {
                showMessage("Mã xác thực phải có 6 chữ số.");
                return;
            }

            if (!email) {
                showMessage("Có lỗi xảy ra, vui lòng thử lại.");
                return;
            }

            submitBtn.classList.add("loading");
            submitBtn.disabled = true;

            fetch(`/api/auth/verify-otp?email=${email}&otp=${otp}`, {
                method: "POST",
            })
                .then((response) => {
                    if (response.ok) {
                        showMessage("Xác thực thành công! Đang chuyển hướng...", "success");
                        setTimeout(() => {
                            window.closeVerificationModalAuth();
                            // Mở modal đặt lại mật khẩu
                            const resetPasswordModal = document.getElementById("resetPasswordModal");
                            if (resetPasswordModal) {
                                resetPasswordModal.classList.add("show");
                                resetPasswordModal.querySelector("#resetEmail").value = email;
                            }
                        }, 1500);
                    } else {
                        return response.json().then((data) => {
                            throw new Error(data.message || "Mã OTP không hợp lệ hoặc đã hết hạn.");
                        });
                    }
                })
                .catch((error) => {
                    showMessage(error.message);
                })
                .finally(() => {
                    submitBtn.classList.remove("loading");
                    submitBtn.disabled = false;
                });
        });
    }

    if (resendCodeBtn) {
        resendCodeBtn.addEventListener("click", function () {
            if (this.disabled) return;

            const email = verificationModalOverlay.dataset.email || currentEmail;
            if (!email) {
                showMessage("Không tìm thấy email. Vui lòng thử lại.");
                return;
            }

            if (resendCount >= maxResend) {
                showMessage("Đã vượt quá số lần gửi lại mã OTP!", "danger");
                this.disabled = true;
                return;
            }

            this.disabled = true;
            this.textContent = "ĐANG GỬI...";

            fetch("/api/auth/resend-otp", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email })
            })
                .then((response) => {
                    if (response.ok) {
                        showMessage("Mã xác thực mới đã được gửi!", "info");
                        resendCount++;
                        startCountdown();
                    } else {
                        return response.json().then((data) => {
                            throw new Error(data.message || "Gửi lại mã thất bại.");
                        });
                    }
                })
                .catch((error) => {
                    showMessage("Gửi lại mã thất bại: " + error.message);
                    resetResendButton();
                });
        });
    }

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && verificationModalOverlay && verificationModalOverlay.classList.contains("show")) {
            window.closeVerificationModalAuth();
        }
    });
});