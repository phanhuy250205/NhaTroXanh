// Verification Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN ---
    const verificationModalOverlay = document.getElementById("verificationModalOverlay");
    const verificationForm = document.getElementById("verificationForm");
    const resendCodeBtn = document.getElementById("resendCodeBtn");
    const verificationInput = document.getElementById("verificationInput");
    const messageDiv = document.getElementById("verification-message");

    // --- XỬ LÝ SUBMIT FORM XÁC THỰC ---
    if (verificationForm) {
        verificationForm.addEventListener("submit", function (e) {
            e.preventDefault();

            const otp = verificationInput.value.trim();
            // Lấy email đã được lưu từ bước đăng ký
            const email = verificationModalOverlay.dataset.email;
            const submitBtn = this.querySelector(".btn-verification-submit");

            if (!otp || !email) {
                messageDiv.className = 'alert alert-danger';
                messageDiv.textContent = 'Có lỗi xảy ra, vui lòng thử lại.';
                messageDiv.style.display = 'block';
                return;
            }
            
            submitBtn.textContent = 'ĐANG KIỂM TRA...';
            submitBtn.disabled = true;

            fetch(`/api/users/verify-otp?email=${email}&otp=${otp}`, {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    // XÁC THỰC THÀNH CÔNG
                    messageDiv.className = 'alert alert-success';
                    messageDiv.textContent = 'Tài khoản đã được kích hoạt! Sẽ chuyển đến đăng nhập...';
                    messageDiv.style.display = 'block';

                    setTimeout(() => {
                        verificationModalOverlay.classList.remove('show');
                        // Tự động mở modal đăng nhập
                        const loginModal = document.getElementById('loginModalOverlay');
                        if (loginModal) {
                            loginModal.classList.add('show');
                        }
                    }, 2500);

                } else {
                    return response.text().then(text => Promise.reject(text));
                }
            })
            .catch(error => {
                messageDiv.className = 'alert alert-danger';
                messageDiv.textContent = error || 'Mã OTP không hợp lệ hoặc đã hết hạn.';
                messageDiv.style.display = 'block';
            })
            .finally(() => {
                 submitBtn.textContent = 'Hoàn tất';
                 submitBtn.disabled = false;
            });
        });
    }

    // --- XỬ LÝ GỬI LẠI MÃ ---
    if (resendCodeBtn) {
        resendCodeBtn.addEventListener("click", function () {
            if (this.disabled) return;

            const email = verificationModalOverlay.dataset.email;
            if (!email) return;

            this.disabled = true;
            this.textContent = 'ĐANG GỬI...';
            
            fetch(`/api/users/resend-otp?email=${email}`, {
                method: 'POST'
            })
            .then(response => {
                if (response.ok) {
                    messageDiv.className = 'alert alert-info';
                    messageDiv.textContent = 'Mã xác thực mới đã được gửi!';
                } else {
                     return response.text().then(text => Promise.reject(text));
                }
            })
            .catch(error => {
                messageDiv.className = 'alert alert-danger';
                messageDiv.textContent = 'Gửi lại mã thất bại. ' + error;
            })
            .finally(() => {
                messageDiv.style.display = 'block';
                this.disabled = false;
                this.textContent = 'Gửi lại mã';
            });
        });
    }
});
