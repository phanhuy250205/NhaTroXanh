document.addEventListener('DOMContentLoaded', function () {
    // --- KHAI BÁO BIẾN ---
    const forgotPasswordModal = document.getElementById('forgotPasswordModalOverlayGuest');
    const verificationModal = document.getElementById('verificationModalOverlayAuth');
    const resetPasswordModal = document.getElementById('resetPasswordModalOverlayGuest');
    const forgotPasswordForm = document.getElementById('forgotPasswordFormGuest');
    const verificationForm = document.getElementById('verificationFormAuth');
    const resetPasswordForm = document.getElementById('resetPasswordFormGuest');
    const closeForgotPasswordBtn = document.getElementById('forgotPasswordModalCloseGuest');
    const closeVerificationBtn = document.getElementById('authModalCloseBtnAuth');
    const closeResetPasswordBtn = document.getElementById('resetPasswordModalCloseGuest');
    const backToLoginBtn = document.getElementById('backToLoginBtnGuest');
    const backToLoginFromResetBtn = document.getElementById('backToLoginFromResetBtnGuest');
    const resendCodeBtn = document.getElementById('resendCodeBtnAuth');
    const emailDisplay = document.getElementById('emailDisplayAuth');
    const countdownTimer = document.getElementById('countdownTimerAuth');
    const countdownDisplay = document.getElementById('countdownAuth');
    const verificationInput = document.getElementById('verificationInputAuth');
    const newPasswordInput = document.getElementById('newPasswordInputGuest');
    const confirmPasswordInput = document.getElementById('confirmPasswordInputGuest');

    let countdownInterval;
    let currentEmail = '';
    let resendCount = 0;
    const maxResend = 3;

    // --- HÀM GỠ LỖI ---
    function logDebug(message) {
        console.log(`[DEBUG] ${message}`);
    }

    // --- HÀM HỖ TRỢ ---
    function showMessage(elementId, message, type = 'danger') {
        const messageElement = document.getElementById(elementId);
        if (messageElement) {
            messageElement.className = `alert-guest alert-${type}-guest`;
            messageElement.textContent = message;
            messageElement.style.display = 'block';
            setTimeout(() => {
                messageElement.style.display = 'none';
            }, 5000);
        } else {
            logDebug(`Không tìm thấy phần tử thông báo ${elementId}`);
        }
    }

    function hideMessage(elementId) {
        const messageElement = document.getElementById(elementId);
        if (messageElement) {
            messageElement.style.display = 'none';
        }
    }

    function startCountdown() {
        if (!resendCodeBtn || !countdownTimer || !countdownDisplay) {
            logDebug('Thiếu nút gửi lại, bộ đếm thời gian, hoặc hiển thị thời gian');
            return;
        }

        let timeLeft = 300; // 5 phút
        countdownTimer.style.display = 'block';
        resendCodeBtn.style.display = 'none';
        countdownDisplay.textContent = `${Math.floor(timeLeft / 60)}:${(timeLeft % 60).toString().padStart(2, '0')}`;

        countdownInterval = setInterval(() => {
            timeLeft--;
            countdownDisplay.textContent = `${Math.floor(timeLeft / 60)}:${(timeLeft % 60).toString().padStart(2, '0')}`;
            if (timeLeft < 0) {
                clearInterval(countdownInterval);
                countdownTimer.style.display = 'none';
                resendCodeBtn.style.display = 'block';
                resendCodeBtn.disabled = resendCount >= maxResend;
                resendCodeBtn.textContent = resendCount >= maxResend ? 'Đã hết lượt gửi lại' : 'Gửi lại mã';
            }
        }, 1000);
    }

    function resetResendButton() {
        if (resendCodeBtn) {
            resendCodeBtn.style.display = 'block';
            resendCodeBtn.disabled = resendCount >= maxResend;
            resendCodeBtn.textContent = resendCount >= maxResend ? 'Đã hết lượt gửi lại' : 'Gửi lại mã';
        }
        if (countdownTimer) {
            countdownTimer.style.display = 'none';
        }
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }
    }

    // --- HÀM ĐIỀU KHIỂN MODAL ---
    function showForgotPasswordModal() {
        if (!forgotPasswordModal) {
            logDebug('Không tìm thấy modal quên mật khẩu');
            return;
        }
        forgotPasswordModal.style.display = 'flex';
        forgotPasswordModal.classList.add('show');
        if (verificationModal) {
            verificationModal.style.display = 'none';
            verificationModal.classList.remove('show');
        }
        if (resetPasswordModal) {
            resetPasswordModal.style.display = 'none';
            resetPasswordModal.classList.remove('show');
        }
        document.body.style.overflow = 'hidden';
        hideMessage('forgot-password-error-message-guest');
        hideMessage('forgot-password-success-message-guest');
        if (forgotPasswordForm) {
            forgotPasswordForm.reset();
        }
        logDebug('Hiển thị modal quên mật khẩu');
    }

    function showVerificationModal(email) {
        if (!verificationModal) {
            logDebug('Không tìm thấy modal xác thực OTP');
            return;
        }
        if (forgotPasswordModal) {
            forgotPasswordModal.style.display = 'none';
            forgotPasswordModal.classList.remove('show');
        }
        if (resetPasswordModal) {
            resetPasswordModal.style.display = 'none';
            resetPasswordModal.classList.remove('show');
        }
        verificationModal.style.display = 'flex';
        verificationModal.classList.add('show');
        verificationModal.dataset.email = email;
        document.body.style.overflow = 'hidden';
        if (emailDisplay) {
            emailDisplay.textContent = email;
        } else {
            logDebug('Không tìm thấy phần tử hiển thị email');
        }
        if (verificationForm) {
            verificationForm.reset();
        }
        currentEmail = email;
        resendCount = 0;
        hideMessage('verification-message-auth');
        resetResendButton();
        startCountdown();
        logDebug(`Hiển thị modal xác thực OTP cho email: ${email}`);
        setTimeout(() => {
            logDebug(`Trạng thái hiển thị modal OTP: ${verificationModal.style.display}`);
            logDebug(`Lớp modal OTP: ${verificationModal.className}`);
            logDebug(`Opacity modal OTP: ${window.getComputedStyle(verificationModal).opacity}`);
            logDebug(`Visibility modal OTP: ${window.getComputedStyle(verificationModal).visibility}`);
            logDebug(`Transform modal nội dung: ${window.getComputedStyle(document.querySelector('.verification-modal-auth')).transform}`);
        }, 100);
    }

    function showResetPasswordModal(email) {
        if (!resetPasswordModal) {
            logDebug('Không tìm thấy modal đặt lại mật khẩu');
            return;
        }
        if (forgotPasswordModal) {
            forgotPasswordModal.style.display = 'none';
            forgotPasswordModal.classList.remove('show');
        }
        if (verificationModal) {
            verificationModal.style.display = 'none';
            verificationModal.classList.remove('show');
        }
        resetPasswordModal.style.display = 'flex';
        resetPasswordModal.classList.add('show');
        resetPasswordModal.dataset.email = email;
        document.body.style.overflow = 'hidden';
        if (resetPasswordForm) {
            resetPasswordForm.reset();
        }
        hideMessage('reset-password-error-message-guest');
        hideMessage('reset-password-success-message-guest');
        logDebug(`Hiển thị modal đặt lại mật khẩu cho email: ${email}`);
        setTimeout(() => {
            logDebug(`Trạng thái hiển thị modal đặt lại mật khẩu: ${resetPasswordModal.style.display}`);
            logDebug(`Lớp modal đặt lại mật khẩu: ${resetPasswordModal.className}`);
        }, 100);
    }

    function closeModals() {
        if (forgotPasswordModal) {
            forgotPasswordModal.style.display = 'none';
            forgotPasswordModal.classList.remove('show');
        }
        if (verificationModal) {
            verificationModal.style.display = 'none';
            verificationModal.classList.remove('show');
        }
        if (resetPasswordModal) {
            resetPasswordModal.style.display = 'none';
            resetPasswordModal.classList.remove('show');
        }
        document.body.style.overflow = '';
        if (countdownInterval) {
            clearInterval(countdownInterval);
        }
        hideMessage('forgot-password-error-message-guest');
        hideMessage('forgot-password-success-message-guest');
        hideMessage('verification-message-auth');
        hideMessage('reset-password-error-message-guest');
        hideMessage('reset-password-success-message-guest');
        if (forgotPasswordForm) {
            forgotPasswordForm.reset();
        }
        if (verificationForm) {
            verificationForm.reset();
        }
        if (resetPasswordForm) {
            resetPasswordForm.reset();
        }
        logDebug('Đóng tất cả modal');
    }

    // --- TRÌNH NGHE SỰ KIỆN ---

    // Mở modal quên mật khẩu
    window.openForgotPasswordModal = () => {
        showForgotPasswordModal();
    };

    // Xử lý submit form quên mật khẩu
    if (forgotPasswordForm) {
        forgotPasswordForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            const emailInput = document.getElementById('forgotPasswordContactGuest');
            if (!emailInput) {
                logDebug('Không tìm thấy trường nhập email');
                showMessage('forgot-password-error-message-guest', 'Không tìm thấy trường email.');
                return;
            }
            const email = emailInput.value.trim();

            if (!email) {
                showMessage('forgot-password-error-message-guest', 'Vui lòng nhập email.');
                return;
            }

            logDebug(`Gửi yêu cầu quên mật khẩu cho email: ${email}`);

            try {
                const response = await fetch('/api/auth/forgot-password', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ email }),
                });

                const data = await response.json();
                if (response.ok) {
                    showMessage('forgot-password-success-message-guest', data.message, 'success');
                    logDebug('Yêu cầu quên mật khẩu thành công, chuyển sang modal xác thực');
                    setTimeout(() => {
                        showVerificationModal(email);
                    }, 1000);
                } else {
                    showMessage('forgot-password-error-message-guest', data.message);
                    logDebug(`Yêu cầu quên mật khẩu thất bại: ${data.message}`);
                }
            } catch (error) {
                showMessage('forgot-password-error-message-guest', 'Có lỗi xảy ra, vui lòng thử lại.');
                logDebug(`Lỗi khi gửi yêu cầu quên mật khẩu: ${error.message}`);
            }
        });
    } else {
        logDebug('Không tìm thấy form quên mật khẩu');
    }

    // Xử lý submit form xác thực OTP
    if (verificationForm) {
        verificationForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            const otp = verificationInput.value.trim();
            const email = verificationModal.dataset.email || currentEmail;
            const submitBtn = this.querySelector('.btn-verification-submit-auth');

            hideMessage('verification-message-auth');

            if (!otp) {
                showMessage('verification-message-auth', 'Vui lòng nhập mã xác thực.');
                return;
            }

            if (otp.length !== 6) {
                showMessage('verification-message-auth', 'Mã xác thực phải có 6 chữ số.');
                return;
            }

            if (!email) {
                showMessage('verification-message-auth', 'Có lỗi xảy ra, vui lòng thử lại.');
                logDebug('Không tìm thấy email cho xác thực OTP');
                return;
            }

            submitBtn.classList.add('loading');
            submitBtn.disabled = true;

            logDebug(`Gửi xác thực OTP cho email: ${email}, OTP: ${otp}`);

            try {
                const response = await fetch(`/api/auth/verify-otp?email=${encodeURIComponent(email)}&otp=${encodeURIComponent(otp)}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                });

                const data = await response.json();
                if (response.ok) {
                    showMessage('verification-message-auth', data.message, 'success');
                    logDebug('Xác thực OTP thành công, chuyển sang modal đặt lại mật khẩu');
                    setTimeout(() => {
                        showResetPasswordModal(email);
                    }, 2000);
                } else {
                    showMessage('verification-message-auth', data.message);
                    logDebug(`Xác thực OTP thất bại: ${data.message}`);
                }
            } catch (error) {
                showMessage('verification-message-auth', 'Có lỗi xảy ra, vui lòng thử lại.');
                logDebug(`Lỗi khi xác thực OTP: ${error.message}`);
            } finally {
                submitBtn.classList.remove('loading');
                submitBtn.disabled = false;
            }
        });
    } else {
        logDebug('Không tìm thấy form xác thực OTP');
    }

    // Xử lý submit form đặt lại mật khẩu
    if (resetPasswordForm) {
        resetPasswordForm.addEventListener('submit', async function (e) {
            e.preventDefault();
            const email = resetPasswordModal.dataset.email || currentEmail;
            const newPassword = newPasswordInput.value.trim();
            const confirmPassword = confirmPasswordInput.value.trim();
            const submitBtn = this.querySelector('.btn-reset-password-submit-guest');

            hideMessage('reset-password-error-message-guest');
            hideMessage('reset-password-success-message-guest');

            if (!newPassword || !confirmPassword) {
                showMessage('reset-password-error-message-guest', 'Vui lòng nhập mật khẩu mới và xác nhận mật khẩu.');
                return;
            }

            if (newPassword !== confirmPassword) {
                showMessage('reset-password-error-message-guest', 'Mật khẩu mới và xác nhận mật khẩu không khớp.');
                return;
            }

            // ✅ Kiểm tra độ mạnh của mật khẩu
            const passwordRegex = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+{}\[\]:;<>,.?~\\/-]).{6,}$/;
            if (!passwordRegex.test(newPassword)) {
                showMessage(
                    'reset-password-error-message-guest',
                    'Mật khẩu phải có ít nhất 6 ký tự, bao gồm chữ hoa, số và ký tự đặc biệt.'
                );
                return;
            }

            if (!email) {
                showMessage('reset-password-error-message-guest', 'Có lỗi xảy ra, vui lòng thử lại.');
                logDebug('Không tìm thấy email cho đặt lại mật khẩu');
                return;
            }

            submitBtn.classList.add('loading');
            submitBtn.disabled = true;

            logDebug(`Gửi yêu cầu đặt lại mật khẩu cho email: ${email}`);

            try {
                const response = await fetch('/api/auth/reset-password', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ email, newPassword, confirmPassword }),
                });

                const data = await response.json();
                if (response.ok) {
                    showMessage('reset-password-success-message-guest', data.message, 'success');
                    logDebug('Đặt lại mật khẩu thành công');
                    setTimeout(() => {
                        closeModals();
                    }, 2000);
                } else {
                    showMessage('reset-password-error-message-guest', data.message);
                    logDebug(`Đặt lại mật khẩu thất bại: ${data.message}`);
                }
            } catch (error) {
                showMessage('reset-password-error-message-guest', 'Có lỗi xảy ra, vui lòng thử lại.');
                logDebug(`Lỗi khi đặt lại mật khẩu: ${error.message}`);
            } finally {
                submitBtn.classList.remove('loading');
                submitBtn.disabled = false;
            }
        });
    } else {
        logDebug('Không tìm thấy form đặt lại mật khẩu');
    }


    // Xử lý gửi lại OTP
    if (resendCodeBtn) {
        resendCodeBtn.addEventListener('click', async function () {
            if (this.disabled) return;

            const email = verificationModal.dataset.email || currentEmail;
            if (!email) {
                showMessage('verification-message-auth', 'Không tìm thấy email. Vui lòng thử lại.');
                logDebug('Không tìm thấy email cho gửi lại OTP');
                return;
            }

            if (resendCount >= maxResend) {
                showMessage('verification-message-auth', 'Đã vượt quá số lần gửi lại mã OTP!');
                this.disabled = true;
                logDebug('Đã đạt giới hạn số lần gửi lại');
                return;
            }

            this.disabled = true;
            this.textContent = 'ĐANG GỬI...';
            logDebug(`Gửi lại OTP cho email: ${email}`);

            try {
                const response = await fetch('/api/auth/resend-otp', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({ email }),
                });

                const data = await response.json();
                if (response.ok) {
                    showMessage('verification-message-auth', data.message, 'info');
                    resendCount++;
                    startCountdown();
                    logDebug('Gửi lại OTP thành công');
                } else {
                    showMessage('verification-message-auth', data.message);
                    resetResendButton();
                    logDebug(`Gửi lại OTP thất bại: ${data.message}`);
                }
            } catch (error) {
                showMessage('verification-message-auth', 'Gửi lại mã thất bại: ' + error.message);
                resetResendButton();
                logDebug(`Lỗi khi gửi lại OTP: ${error.message}`);
            }
        });
    } else {
        logDebug('Không tìm thấy nút gửi lại mã OTP');
    }

    // Giới hạn trường nhập OTP chỉ nhận 6 chữ số
    if (verificationInput) {
        verificationInput.addEventListener('input', (e) => {
            e.target.value = e.target.value.replace(/[^0-9]/g, '');
            if (e.target.value.length > 6) {
                e.target.value = e.target.value.slice(0, 6);
            }
        });
    } else {
        logDebug('Không tìm thấy trường nhập OTP');
    }

    // Đóng modal khi nhấn nút đóng
    if (closeForgotPasswordBtn) {
        closeForgotPasswordBtn.addEventListener('click', closeModals);
    }
    if (closeVerificationBtn) {
        closeVerificationBtn.addEventListener('click', closeModals);
    }
    if (closeResetPasswordBtn) {
        closeResetPasswordBtn.addEventListener('click', closeModals);
    }
    if (backToLoginBtn) {
        backToLoginBtn.addEventListener('click', closeModals);
    }
    if (backToLoginFromResetBtn) {
        backToLoginFromResetBtn.addEventListener('click', closeModals);
    }

    // Đóng modal khi nhấn vào nền (overlay)
    if (forgotPasswordModal) {
        forgotPasswordModal.addEventListener('click', (e) => {
            if (e.target === forgotPasswordModal) {
                closeModals();
            }
        });
    }
    if (verificationModal) {
        verificationModal.addEventListener('click', (e) => {
            if (e.target === verificationModal) {
                closeModals();
            }
        });
    }
    if (resetPasswordModal) {
        resetPasswordModal.addEventListener('click', (e) => {
            if (e.target === resetPasswordModal) {
                closeModals();
            }
        });
    }

    // Đóng modal khi nhấn phím Escape
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && (forgotPasswordModal?.style.display === 'flex' ||
            verificationModal?.style.display === 'flex' ||
            resetPasswordModal?.style.display === 'flex')) {
            closeModals();
        }
    });

    // Kiểm tra ban đầu xem các phần tử modal có tồn tại không
    if (!forgotPasswordModal || !verificationModal || !resetPasswordModal ||
        !forgotPasswordForm || !verificationForm || !resetPasswordForm) {
        logDebug('Thiếu một hoặc nhiều phần tử modal cần thiết');
    }
});