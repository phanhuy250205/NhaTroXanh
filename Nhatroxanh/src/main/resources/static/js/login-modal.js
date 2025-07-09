// Login Modal JavaScript
document.addEventListener("DOMContentLoaded", () => {
    // --- KHAI BÁO BIẾN ---
    const loginBtn = document.getElementById("loginBtn");
    const loginModalOverlay = document.getElementById("loginModalOverlay");
    const loginModalClose = document.getElementById("loginModalClose");
    const loginForm = document.getElementById("loginForm");
    const passwordToggles = document.querySelectorAll(".login-modal .password-toggle");
    const registerLink = document.querySelector(".login-modal .register-now");


    // --- HÀM VÀ SỰ KIỆN UI ---
    function openModal() {
        if (loginModalOverlay) {
            loginModalOverlay.classList.add("show");
            document.body.style.overflow = "hidden";
        }
    }
    function closeModal() {
        if (loginModalOverlay) {
            loginModalOverlay.classList.remove("show");
            document.body.style.overflow = "";
        }
    }
    if (loginBtn) { loginBtn.addEventListener("click", (e) => { e.preventDefault(); openModal(); }); }
    if (loginModalClose) { loginModalClose.addEventListener("click", closeModal); }
    if (loginModalOverlay) { loginModalOverlay.addEventListener("click", (e) => { if (e.target === loginModalOverlay) closeModal(); }); }
    document.addEventListener("keydown", (e) => { if (e.key === "Escape" && loginModalOverlay && loginModalOverlay.classList.contains("show")) closeModal(); });
    if (registerLink) {
        registerLink.addEventListener("click", (e) => {
            e.preventDefault();
            closeModal();
            const registerModalOverlay = document.getElementById("registerModalOverlay");
            if (registerModalOverlay) registerModalOverlay.classList.add("show");
        });
    }
    passwordToggles.forEach(toggle => {
        toggle.addEventListener("click", function () {
            const wrapper = this.closest('.input-wrapper');
            const passwordInput = wrapper.querySelector('input');
            const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
            passwordInput.setAttribute("type", type);
            const icon = this.querySelector("i");
            icon.classList.toggle("fa-eye");
            icon.classList.toggle("fa-eye-slash");
        });
    });


    // --- XỬ LÝ SUBMIT FORM ĐĂNG NHẬP ---
    if (loginForm) {
        loginForm.addEventListener("submit", function (e) {
            e.preventDefault();

            const errorMessageDiv = document.getElementById("login-error-message");
            errorMessageDiv.style.display = 'none'; // Ẩn thông báo lỗi cũ

            // 1. Lấy dữ liệu từ form trong modal
            const username = document.getElementById("loginUsername").value;
            const password = document.getElementById("loginPassword").value;
            const rememberMe = document.getElementById("rememberMe").checked;
            // 2. Chuẩn bị dữ liệu dạng form-urlencoded để gửi cho Spring Security
            const formData = new URLSearchParams();
            formData.append('username', username); // Tên param phải là "username"
            formData.append('password', password);
            if (rememberMe) {
                // Tên parameter phải là 'remember-me' theo mặc định của Spring Security
                formData.append('remember-me', 'on');
            }
            // 3. Gọi đến cổng xử lý đăng nhập chung của Spring Security
            fetch('/login-processing', {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: formData
            })
                .then(response => {
                    if (response.ok) {
                        // 4. THÀNH CÔNG: Tải lại trang để cập nhật thanh điều hướng
                        window.location.reload();
                    } else {
                        // 5. THẤT BẠI: Hiển thị lỗi
                        throw new Error("Tên đăng nhập hoặc mật khẩu không chính xác.");
                    }
                })
                .catch(error => {
                    if (errorMessageDiv) {
                        errorMessageDiv.textContent = error.message;
                        errorMessageDiv.style.display = 'block';
                    } else {
                        alert(error.message);
                    }
                });
        });
    }
});


// Login Modal JavaScript - Updated with Forgot Password Link
// document.addEventListener("DOMContentLoaded", () => {
//     // --- KHAI BÁO BIẾN ---
//     const loginBtn = document.getElementById("loginBtn")
//     const loginModalOverlay = document.getElementById("loginModalOverlay")
//     const loginModalClose = document.getElementById("loginModalClose")
//     const loginForm = document.getElementById("loginForm")
//     const passwordToggles = document.querySelectorAll(".login-modal .password-toggle")
//     const registerLink = document.querySelector(".login-modal .register-now")
//     const forgotPasswordLink = document.querySelector(".login-modal .forgot-password")

//     // --- HÀM VÀ SỰ KIỆN UI ---
//     function openModal() {
//         if (loginModalOverlay) {
//             loginModalOverlay.classList.add("show")
//             document.body.style.overflow = "hidden"
//         }
//     }
//     function closeModal() {
//         if (loginModalOverlay) {
//             loginModalOverlay.classList.remove("show")
//             document.body.style.overflow = ""
//         }
//     }
//     if (loginBtn) {
//         loginBtn.addEventListener("click", (e) => {
//             e.preventDefault()
//             openModal()
//         })
//     }
//     if (loginModalClose) {
//         loginModalClose.addEventListener("click", closeModal)
//     }
//     if (loginModalOverlay) {
//         loginModalOverlay.addEventListener("click", (e) => {
//             if (e.target === loginModalOverlay) closeModal()
//         })
//     }
//     document.addEventListener("keydown", (e) => {
//         if (e.key === "Escape" && loginModalOverlay && loginModalOverlay.classList.contains("show")) closeModal()
//     })

//     if (registerLink) {
//         registerLink.addEventListener("click", (e) => {
//             e.preventDefault()
//             closeModal()
//             const registerModalOverlay = document.getElementById("registerModalOverlay")
//             if (registerModalOverlay) registerModalOverlay.classList.add("show")
//         })
//     }

//     // --- THÊM SỰ KIỆN CHO FORGOT PASSWORD LINK ---
//     if (forgotPasswordLink) {
//         forgotPasswordLink.addEventListener("click", (e) => {
//             e.preventDefault()
//             closeModal()
//             const forgotPasswordModalOverlay = document.getElementById("forgotPasswordModalOverlayGuest")
//             if (forgotPasswordModalOverlay) {
//                 forgotPasswordModalOverlay.classList.add("show")
//                 document.body.style.overflow = "hidden"
//             }
//         })
//     }

//     passwordToggles.forEach((toggle) => {
//         toggle.addEventListener("click", function () {
//             const wrapper = this.closest(".input-wrapper")
//             const passwordInput = wrapper.querySelector("input")
//             const type = passwordInput.getAttribute("type") === "password" ? "text" : "password"
//             passwordInput.setAttribute("type", type)
//             const icon = this.querySelector("i")
//             icon.classList.toggle("fa-eye")
//             icon.classList.toggle("fa-eye-slash")
//         })
//     })

//     // --- XỬ LÝ SUBMIT FORM ĐĂNG NHẬP ---
//     if (loginForm) {
//         loginForm.addEventListener("submit", (e) => {
//             e.preventDefault()

//             const errorMessageDiv = document.getElementById("login-error-message")
//             errorMessageDiv.style.display = "none" // Ẩn thông báo lỗi cũ

//             // 1. Lấy dữ liệu từ form trong modal
//             const username = document.getElementById("loginUsername").value
//             const password = document.getElementById("loginPassword").value
//             const rememberMe = document.getElementById("rememberMe").checked
//             // 2. Chuẩn bị dữ liệu dạng form-urlencoded để gửi cho Spring Security
//             const formData = new URLSearchParams()
//             formData.append("username", username) // Tên param phải là "username"
//             formData.append("password", password)
//             if (rememberMe) {
//                 // Tên parameter phải là 'remember-me' theo mặc định của Spring Security
//                 formData.append("remember-me", "on")
//             }
//             // 3. Gọi đến cổng xử lý đăng nhập chung của Spring Security
//             fetch("/login-processing", {
//                 method: "POST",
//                 headers: { "Content-Type": "application/x-www-form-urlencoded" },
//                 body: formData,
//             })
//                 .then((response) => {
//                     if (response.ok) {
//                         // 4. THÀNH CÔNG: Tải lại trang để cập nhật thanh điều hướng
//                         window.location.reload()
//                     } else {
//                         // 5. THẤT BẠI: Hiển thị lỗi
//                         throw new Error("Tên đăng nhập hoặc mật khẩu không chính xác.")
//                     }
//                 })
//                 .catch((error) => {
//                     if (errorMessageDiv) {
//                         errorMessageDiv.textContent = error.message
//                         errorMessageDiv.style.display = "block"
//                     } else {
//                         alert(error.message)
//                     }
//                 })
//         })
//     }
// })
