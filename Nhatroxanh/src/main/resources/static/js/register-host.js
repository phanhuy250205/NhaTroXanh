// CCCD Province Codes
const CCCD_PROVINCE_CODES = {
    "001": "Hà Nội", "002": "Hà Giang", "004": "Cao Bằng", "006": "Bắc Kạn", "008": "Tuyên Quang",
    "010": "Lào Cai", "011": "Điện Biên", "012": "Lai Châu", "014": "Sơn La", "015": "Yên Bái",
    "017": "Hoà Bình", "019": "Thái Nguyên", "020": "Lạng Sơn", "022": "Quảng Ninh", "024": "Bắc Giang",
    "025": "Phú Thọ", "026": "Vĩnh Phúc", "027": "Bắc Ninh", "030": "Hải Dương", "031": "Hải Phòng",
    "033": "Hưng Yên", "034": "Thái Bình", "035": "Hà Nam", "036": "Nam Định", "037": "Ninh Bình",
    "038": "Thanh Hóa", "040": "Nghệ An", "042": "Hà Tĩnh", "044": "Quảng Bình", "045": "Quảng Trị",
    "046": "Thừa Thiên Huế", "048": "Đà Nẵng", "049": "Quảng Nam", "051": "Quảng Ngãi", "052": "Bình Định",
    "054": "Phú Yên", "056": "Khánh Hòa", "058": "Ninh Thuận", "060": "Bình Thuận", "062": "Kon Tum",
    "064": "Gia Lai", "066": "Đắk Lắk", "067": "Đắk Nông", "068": "Lâm Đồng", "070": "Bình Phước",
    "072": "Tây Ninh", "074": "Bình Dương", "075": "Đồng Nai", "077": "Bà Rịa - Vũng Tàu", "079": "Hồ Chí Minh",
    "080": "Long An", "082": "Tiền Giang", "083": "Bến Tre", "084": "Trà Vinh", "086": "Vĩnh Long",
    "087": "Đồng Tháp", "089": "An Giang", "091": "Kiên Giang", "092": "Cần Thơ", "093": "Hậu Giang",
    "094": "Sóc Trăng", "095": "Bạc Liêu", "096": "Cà Mau"
};

// Phone Network Codes
const PHONE_NETWORK_CODES = {
    "032": "Viettel", "033": "Viettel", "034": "Viettel", "035": "Viettel", "036": "Viettel",
    "037": "Viettel", "038": "Viettel", "039": "Viettel", "096": "Viettel", "097": "Viettel",
    "098": "Viettel", "081": "Vinaphone", "082": "Vinaphone", "083": "Vinaphone", "084": "Vinaphone",
    "085": "Vinaphone", "091": "Vinaphone", "094": "Vinaphone", "070": "Mobifone", "076": "Mobifone",
    "077": "Mobifone", "078": "Mobifone", "079": "Mobifone", "090": "Mobifone", "093": "Mobifone",
    "056": "Vietnamobile", "058": "Vietnamobile", "092": "Vietnamobile", "059": "Gmobile", "099": "Gmobile"
};

document.addEventListener("DOMContentLoaded", () => {
    // Show/hide password toggle
    const passwordToggles = document.querySelectorAll(".password-toggle-host");
    const registerForm = document.getElementById("registerForm");

    // Image preview handling
    const frontImageInput = document.getElementById("frontImage");
    const backImageInput = document.getElementById("backImage");
    const frontImagePreview = document.getElementById("frontImagePreview");
    const backImagePreview = document.getElementById("backImagePreview");

    if (frontImageInput) {
        frontImageInput.addEventListener("change", function () {
            previewImage(this, frontImagePreview);
        });
    }

    if (backImageInput) {
        backImageInput.addEventListener("change", function () {
            previewImage(this, backImagePreview);
        });
    }

    function previewImage(input, previewElement) {
        const file = input.files[0];
        if (file) {
            if (!["image/jpeg", "image/png"].includes(file.type)) {
                showFieldError(input, "Chỉ được chọn file ảnh JPG hoặc PNG.");
                return;
            }
            if (file.size > 5 * 1024 * 1024) {
                showFieldError(input, "Kích thước ảnh không được vượt quá 5MB.");
                return;
            }
            const reader = new FileReader();
            reader.onload = function (e) {
                previewElement.src = e.target.result;
                previewElement.style.display = "block";
            };
            reader.readAsDataURL(file);
        } else {
            previewElement.src = "#";
            previewElement.style.display = "none";
        }
    }

    // Address API handling
    const provinceSelect = document.getElementById("province");
    const districtSelect = document.getElementById("district");
    const wardSelect = document.getElementById("ward");

    // Fetch provinces
    fetch("https://provinces.open-api.vn/api/p/")
        .then(response => {
            // if (!response.ok) throw new Error("Không thể tải danh sách tỉnh/thành phố.");
            return response.json();
        })
        .then(provinces => {
            provinces.forEach(province => {
                const option = document.createElement("option");
                option.value = province.code;
                option.text = province.name;
                provinceSelect.appendChild(option);
            });
        })
        // .catch(error => {
        //     console.error("Error fetching provinces:", error);
        //     Swal.fire({
        //         icon: "error",
        //         title: "Lỗi",
        //         text: "Lỗi tải danh sách tỉnh/thành phố. Vui lòng thử lại sau.",
        //         confirmButtonText: "OK"
        //     });
        //     provinceSelect.disabled = true;
        // });

    // Fetch districts when a province is selected
    provinceSelect.addEventListener("change", function () {
        districtSelect.innerHTML = '<option value="" disabled selected>Chọn quận/huyện</option>';
        wardSelect.innerHTML = '<option value="" disabled selected>Chọn phường/xã</option>';
        districtSelect.disabled = true;
        wardSelect.disabled = true;

        if (this.value) {
            fetch(`https://provinces.open-api.vn/api/p/${this.value}?depth=2`)
                .then(response => {
                    if (!response.ok) throw new Error("Không thể tải danh sách quận/huyện.");
                    return response.json();
                })
                .then(data => {
                    data.districts.forEach(district => {
                        const option = document.createElement("option");
                        option.value = district.code;
                        option.text = district.name;
                        districtSelect.appendChild(option);
                    });
                    districtSelect.disabled = false;
                })
                .catch(error => {
                    console.error("Error fetching districts:", error);
                    Swal.fire({
                        icon: "error",
                        title: "Lỗi",
                        text: "Lỗi tải danh sách quận/huyện. Vui lòng thử lại sau.",
                        confirmButtonText: "OK"
                    });
                    districtSelect.disabled = true;
                });
        }
    });

    // Fetch wards when a district is selected
    districtSelect.addEventListener("change", function () {
        wardSelect.innerHTML = '<option value="" disabled selected>Chọn phường/xã</option>';
        wardSelect.disabled = true;

        if (this.value) {
            fetch(`https://provinces.open-api.vn/api/d/${this.value}?depth=2`)
                .then(response => {
                    if (!response.ok) throw new Error("Không thể tải danh sách phường/xã.");
                    return response.json();
                })
                .then(data => {
                    data.wards.forEach(ward => {
                        const option = document.createElement("option");
                        option.value = ward.code;
                        option.text = ward.name;
                        wardSelect.appendChild(option);
                    });
                    wardSelect.disabled = false;
                })
                .catch(error => {
                    console.error("Error fetching wards:", error);
                    Swal.fire({
                        icon: "error",
                        title: "Lỗi",
                        text: "Lỗi tải danh sách phường/xã. Vui lòng thử lại sau.",
                        confirmButtonText: "OK"
                    });
                    wardSelect.disabled = true;
                });
        }
    });

    // Form submission
    if (registerForm) {
        registerForm.addEventListener("submit", function (event) {
            event.preventDefault();
            if (!validateForm()) return;

            const provinceText = provinceSelect.options[provinceSelect.selectedIndex]?.text || "";
            const districtText = districtSelect.options[districtSelect.selectedIndex]?.text || "";
            const wardText = wardSelect.options[wardSelect.selectedIndex]?.text || "";
            const addressDetail = document.getElementById("addressDetail").value;
            const fullAddress = `${addressDetail}, ${wardText}, ${districtText}, ${provinceText}`.trim();

            const formData = new FormData();
            formData.append("fullName", document.getElementById("fullName").value);
            formData.append("email", document.getElementById("email").value);
            formData.append("phoneNumber", document.getElementById("phoneNumber").value);
            formData.append("password", document.getElementById("password").value);
            formData.append("birthDate", document.getElementById("birthDate").value);
            formData.append("gender", document.getElementById("gender").value);
            formData.append("cccdNumber", document.getElementById("cccdNumber").value);
            formData.append("issuePlace", document.getElementById("issuePlace").value);
            formData.append("issueDate", document.getElementById("issueDate").value);
            formData.append("frontImage", document.getElementById("frontImage").files[0]);
            formData.append("backImage", document.getElementById("backImage").files[0]);
            formData.append("address", fullAddress);

            if (document.getElementById("password").value !== document.getElementById("confirmPassword").value) {
                Swal.fire({
                    icon: "error",
                    title: "Lỗi",
                    text: "Mật khẩu xác nhận không khớp.",
                    confirmButtonText: "OK"
                });
                return;
            }
            if (!document.getElementById("agreeTerms").checked) {
                Swal.fire({
                    icon: "error",
                    title: "Lỗi",
                    text: "Bạn phải đồng ý với Điều khoản và Chính sách bảo mật.",
                    confirmButtonText: "OK"
                });
                return;
            }

            fetch("/api/users/register-owner", {
                method: "POST",
                body: formData,
            })
            .then(response => {
                if (response.ok) {
                    Swal.fire({
                        icon: "success",
                        title: "Thành công",
                        text: "Đăng ký tài khoản thành công bạn sẽ được hệ thống hỗ trợ duyệt sớm!",
                        confirmButtonText: "OK"
                    }).then((result) => {
                        if (result.isConfirmed) {
                            window.location.href = '/dang-nhap-chu-tro';
                        }
                    });
                } else {
                    return response.text().then(text => { throw new Error(text || "Lỗi không xác định.") });
                }
            })
            .catch(error => {
                console.error("Lỗi đăng ký:", error);
                Swal.fire({
                    icon: "error",
                    title: "Lỗi",
                    text: "Đăng ký thất bại: " + error.message,
                    confirmButtonText: "OK"
                });
            });
        });
    }

    // Password toggle
    passwordToggles.forEach((toggle) => {
        toggle.addEventListener("click", function () {
            const passwordInput = this.parentElement.querySelector('input');
            if (passwordInput) {
                const type = passwordInput.type === "password" ? "text" : "password";
                passwordInput.type = type;
                const icon = this.querySelector("i");
                if (icon) {
                    icon.classList.toggle("fa-eye", type === "text");
                    icon.classList.toggle("fa-eye-slash", type === "password");
                }
                passwordInput.focus();
            }
        });
    });

    // Handle close button
    const closeBtn = document.querySelector(".close-btn");
    if (closeBtn) {
        closeBtn.addEventListener("click", (e) => {
            e.preventDefault();
            const homeItemInfo = {
                text: "Trang chủ",
                href: "/trang-chu",
                isDropdownItem: false,
            };
            localStorage.setItem("activeNavItem", JSON.stringify(homeItemInfo));
            window.location.href = "/trang-chu";
        });
    }

    // Form validation and real-time feedback
    if (registerForm) {
        const inputs = registerForm.querySelectorAll("input, select");
        inputs.forEach((input) => {
            input.addEventListener("input", () => {
                clearFieldError(input);
                if (input.id === "phoneNumber") validatePhoneNumber(input);
                if (input.id === "cccdNumber") validateCCCDNumber(input);
            });
            input.addEventListener("change", () => {
                clearFieldError(input);
                if (input.id === "phoneNumber") validatePhoneNumber(input);
                if (input.id === "cccdNumber") validateCCCDNumber(input);
            });
        });
    }

    function clearFieldError(input) {
        input.classList.remove("is-invalid", "is-valid");
        const errorMessage = input.parentElement.querySelector(".error-message");
        if (errorMessage) {
            errorMessage.classList.remove("show");
        }
        if (input.id === "agreeTerms") {
            const checkmark = input.parentElement.querySelector(".checkmark");
            if (checkmark) {
                checkmark.classList.remove("is-invalid");
            }
            const termsError = document.querySelector(".terms-error");
            if (termsError) {
                termsError.classList.remove("show");
            }
        }
        // Clear info messages
        const infoElement = document.getElementById(input.id + "Info");
        if (infoElement) {
            infoElement.style.display = "none";
            infoElement.textContent = "";
        }
    }

    function showFieldError(input, message = null) {
        input.classList.add("is-invalid");
        input.classList.remove("is-valid");
        const errorMessage = input.parentElement.querySelector(".error-message");
        if (errorMessage) {
            if (message) {
                errorMessage.textContent = message;
            }
            errorMessage.classList.add("show");
        }
        if (input.id === "agreeTerms") {
            const checkmark = input.parentElement.querySelector(".checkmark");
            if (checkmark) {
                checkmark.classList.add("is-invalid");
            }
            const termsError = document.querySelector(".terms-error");
            if (termsError) {
                termsError.classList.add("show");
            }
        }
    }

    function showFieldInfo(input, message) {
        input.classList.add("is-valid");
        input.classList.remove("is-invalid");
        const infoElement = document.getElementById(input.id + "Info");
        if (infoElement) {
            infoElement.textContent = message;
            infoElement.style.display = "block";
        }
    }

    function validatePhoneNumber(input) {
        const value = input.value.replace(/[\s\-()]/g, "");
        if (value) {
            if (!value.startsWith("0")) {
                showFieldError(input, "Số điện thoại phải bắt đầu bằng số 0.");
                return false;
            }
            if (!/^0[0-9]{9}$/.test(value)) {
                showFieldError(input, "Số điện thoại phải có 10 chữ số.");
                return false;
            }
            const networkCode = value.substring(0, 3);
            if (!PHONE_NETWORK_CODES[networkCode]) {
                showFieldError(input, "Số điện thoại không thuộc nhà mạng hợp lệ tại Việt Nam.");
                return false;
            }
            showFieldInfo(input, `Nhà mạng: ${PHONE_NETWORK_CODES[networkCode]}`);
            return true;
        }
        return false;
    }

    function validateCCCDNumber(input) {
        const value = input.value.replace(/\s/g, "");
        if (value) {
            if (!/^[0-9]{12}$/.test(value)) {
                showFieldError(input, "Số CCCD phải có 12 chữ số.");
                return false;
            }
            const provinceCode = value.substring(0, 3);
            if (!CCCD_PROVINCE_CODES[provinceCode]) {
                showFieldError(input, `Mã tỉnh trong CCCD không hợp lệ (3 số đầu: ${provinceCode}).`);
                return false;
            }
            showFieldInfo(input, `Tỉnh/Thành phố: ${CCCD_PROVINCE_CODES[provinceCode]}`);
            return true;
        }
        return false;
    }

    function validateForm() {
        let isValid = true;
        const form = document.getElementById("registerForm");
        const allInputs = form.querySelectorAll("input, select");
        allInputs.forEach((input) => clearFieldError(input));

        // Validate họ tên
        const fullName = document.getElementById("fullName");
        if (!fullName.value.trim()) {
            showFieldError(fullName, "Vui lòng nhập họ và tên.");
            isValid = false;
        } else if (fullName.value.trim().length < 2) {
            showFieldError(fullName, "Họ tên phải có ít nhất 2 ký tự.");
            isValid = false;
        } else if (fullName.value.trim().length > 100) {
            showFieldError(fullName, "Họ tên không được vượt quá 100 ký tự.");
            isValid = false;
        }

        // Validate ngày sinh
        const birthDate = document.getElementById("birthDate");
        if (!birthDate.value) {
            showFieldError(birthDate, "Vui lòng nhập ngày sinh.");
            isValid = false;
        } else {
            const today = new Date();
            const birth = new Date(birthDate.value);
            const age = today.getFullYear() - birth.getFullYear();
            const monthDiff = today.getMonth() - birth.getMonth();
            if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
                age--;
            }
            if (age < 18) {
                showFieldError(birthDate, "Bạn phải đủ 18 tuổi để đăng ký.");
                isValid = false;
            }
        }

        // Validate email
        const email = document.getElementById("email");
        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
        if (!email.value.trim()) {
            showFieldError(email, "Vui lòng nhập email.");
            isValid = false;
        } else if (!emailRegex.test(email.value)) {
            showFieldError(email, "Vui lòng nhập email hợp lệ.");
            isValid = false;
        }

        // Validate số điện thoại
        const phone = document.getElementById("phoneNumber");
        if (!validatePhoneNumber(phone)) {
            isValid = false;
        }

        // Validate mật khẩu
        const password = document.getElementById("password");
        if (!password.value) {
            showFieldError(password, "Vui lòng nhập mật khẩu.");
            isValid = false;
        } else if (password.value.length < 6) {
            showFieldError(password, "Mật khẩu phải có ít nhất 6 ký tự.");
            isValid = false;
        } else if (!/[A-Z]/.test(password.value)) {
            showFieldError(password, "Mật khẩu phải chứa ít nhất 1 chữ cái in hoa.");
            isValid = false;
        } else if (!/[!@#$%^&*(),.?\":{}|<>]/.test(password.value)) {
            showFieldError(password, "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt.");
            isValid = false;
        }

        // Validate xác nhận mật khẩu
        const confirmPassword = document.getElementById("confirmPassword");
        if (!confirmPassword.value) {
            showFieldError(confirmPassword, "Vui lòng xác nhận mật khẩu.");
            isValid = false;
        } else if (password.value !== confirmPassword.value) {
            showFieldError(confirmPassword, "Mật khẩu xác nhận không khớp.");
            isValid = false;
        }

        // Validate giới tính
        const gender = document.getElementById("gender");
        if (!gender.value) {
            showFieldError(gender, "Vui lòng chọn giới tính.");
            isValid = false;
        }

        // Validate số CCCD
        const cccdNumber = document.getElementById("cccdNumber");
        if (!validateCCCDNumber(cccdNumber)) {
            isValid = false;
        }

        // Validate nơi cấp
        const issuePlace = document.getElementById("issuePlace");
        if (!issuePlace.value.trim()) {
            showFieldError(issuePlace, "Vui lòng nhập nơi cấp CCCD.");
            isValid = false;
        } else if (issuePlace.value.trim().length < 2) {
            showFieldError(issuePlace, "Nơi cấp phải có ít nhất 2 ký tự.");
            isValid = false;
        }

        // Validate ngày cấp
        const issueDate = document.getElementById("issueDate");
        if (!issueDate.value) {
            showFieldError(issueDate, "Vui lòng nhập ngày cấp CCCD.");
            isValid = false;
        } else {
            const issue = new Date(issueDate.value);
            const birth = new Date(birthDate.value);
            const today = new Date();
            if (issue <= birth) {
                showFieldError(issueDate, "Ngày cấp CCCD phải sau ngày sinh.");
                isValid = false;
            } else if (issue > today) {
                showFieldError(issueDate, "Ngày cấp CCCD không được là tương lai.");
                isValid = false;
            }
        }

        // Validate ảnh CCCD mặt trước
        const frontImage = document.getElementById("frontImage");
        if (!frontImage.files[0]) {
            showFieldError(frontImage, "Vui lòng chọn ảnh CCCD mặt trước.");
            isValid = false;
        }

        // Validate ảnh CCCD mặt sau
        const backImage = document.getElementById("backImage");
        if (!backImage.files[0]) {
            showFieldError(backImage, "Vui lòng chọn ảnh CCCD mặt sau.");
            isValid = false;
        }

        // Validate tỉnh/thành phố
        const province = document.getElementById("province");
        if (!province.value) {
            showFieldError(province, "Vui lòng chọn tỉnh/thành phố.");
            isValid = false;
        }

        // Validate quận/huyện
        const district = document.getElementById("district");
        if (!district.value) {
            showFieldError(district, "Vui lòng chọn quận/huyện.");
            isValid = false;
        }

        // Validate phường/xã
        const ward = document.getElementById("ward");
        if (!ward.value) {
            showFieldError(ward, "Vui lòng chọn phường/xã.");
            isValid = false;
        }

        // Validate địa chỉ chi tiết
        const addressDetail = document.getElementById("addressDetail");
        if (!addressDetail.value.trim()) {
            showFieldError(addressDetail, "Vui lòng nhập địa chỉ chi tiết.");
            isValid = false;
        } else if (addressDetail.value.trim().length < 5) {
            showFieldError(addressDetail, "Địa chỉ chi tiết phải có ít nhất 5 ký tự.");
            isValid = false;
        }

        // Validate điều khoản
        const agreeTerms = document.getElementById("agreeTerms");
        if (!agreeTerms.checked) {
            showFieldError(agreeTerms);
            isValid = false;
        }

        return isValid;
    }
});