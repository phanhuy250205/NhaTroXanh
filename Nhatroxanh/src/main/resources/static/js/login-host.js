document.addEventListener("DOMContentLoaded", () => {
    // Show/hide password toggle - xử lý tất cả password toggles
    const passwordToggles = document.querySelectorAll(".password-toggle-host")

    passwordToggles.forEach((toggle) => {
        toggle.addEventListener("click", function () {
            const passwordInput = this.parentElement.querySelector('input[type="password"], input[type="text"]')

            if (passwordInput) {
                const type = passwordInput.getAttribute("type") === "password" ? "text" : "password"
                passwordInput.setAttribute("type", type)

                const icon = this.querySelector("i")
                if (icon) {
                    if (type === "password") {
                        icon.classList.remove("fa-eye")
                        icon.classList.add("fa-eye-slash")
                    } else {
                        icon.classList.remove("fa-eye-slash")
                        icon.classList.add("fa-eye")
                    }
                }

                // Focus vào input sau khi toggle
                passwordInput.focus()
            }
        })
    })

    // Handle close button click to set home page as active and navigate
    // Tìm thẻ <a> chứa button close
    const closeBtnLink = document.querySelector('a[href*="trang-chu"]')
    const closeBtn = document.querySelector(".close-btn")

    if (closeBtnLink) {
        closeBtnLink.addEventListener("click", (e) => {
            // Prevent default để xử lý custom logic
            e.preventDefault()

            // Set home page as active in localStorage before navigation
            const homeItemInfo = {
                text: "Trang chủ", // Điều chỉnh text này cho khớp với text trong navbar
                href: "/trang-chu",
                isDropdownItem: false,
            }
            localStorage.setItem("activeNavItem", JSON.stringify(homeItemInfo))

            // Navigate to home page
            window.location.href = "/trang-chu"
        })
    } else if (closeBtn) {
        // Fallback: nếu không tìm thấy thẻ <a>, xử lý trên button
        closeBtn.addEventListener("click", (e) => {
            // Set home page as active in localStorage before navigation
            const homeItemInfo = {
                text: "Trang chủ",
                href: "/trang-chu",
                isDropdownItem: false,
            }
            localStorage.setItem("activeNavItem", JSON.stringify(homeItemInfo))

            // Navigate to home page
            window.location.href = "/trang-chu"
        })
    }
})
