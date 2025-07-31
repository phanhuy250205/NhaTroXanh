// Footer JavaScript functionality
document.addEventListener("DOMContentLoaded", () => {
    // Back to top button functionality
    const backToTopButton = document.getElementById("backToTop")

    if (backToTopButton) {
        // Show/hide back to top button based on scroll position
        window.addEventListener("scroll", () => {
            if (window.pageYOffset > 300) {
                backToTopButton.classList.add("show")
            } else {
                backToTopButton.classList.remove("show")
            }
        })

        // Smooth scroll to top when button is clicked
        backToTopButton.addEventListener("click", () => {
            window.scrollTo({
                top: 0,
                behavior: "smooth",
            })
        })
    }

    // Newsletter form submission
    const newsletterForm = document.querySelector(".newsletter-form")
    if (newsletterForm) {
        newsletterForm.addEventListener("submit", function (e) {
            e.preventDefault()

            const emailInput = this.querySelector('input[name="email"]')
            const privacyCheckbox = this.querySelector('input[name="privacy"]')
            const submitButton = this.querySelector(".newsletter-btn")

            // Validate email
            if (!emailInput.value || !isValidEmail(emailInput.value)) {
                showNotification("Vui lòng nhập email hợp lệ!", "error")
                return
            }

            // Validate privacy checkbox
            if (!privacyCheckbox.checked) {
                showNotification("Vui lòng đồng ý với chính sách bảo mật!", "error")
                return
            }

            // Show loading state
            const originalText = submitButton.innerHTML
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xử lý...'
            submitButton.disabled = true

            // Simulate API call (replace with actual implementation)
            setTimeout(() => {
                showNotification("Đăng ký thành công! Cảm ơn bạn đã đăng ký nhận thông báo.", "success")
                emailInput.value = ""
                privacyCheckbox.checked = false

                // Reset button
                submitButton.innerHTML = originalText
                submitButton.disabled = false
            }, 2000)
        })
    }

    // Social link tracking (for analytics)
    const socialLinks = document.querySelectorAll(".social-link")
    socialLinks.forEach((link) => {
        link.addEventListener("click", function (e) {
            const platform = this.classList.contains("facebook")
                ? "Facebook"
                : this.classList.contains("zalo")
                    ? "Zalo"
                    : this.classList.contains("youtube")
                        ? "YouTube"
                        : this.classList.contains("instagram")
                            ? "Instagram"
                            : this.classList.contains("tiktok")
                                ? "TikTok"
                                : "Unknown"

            // Track social media click (replace with your analytics code)
            console.log(`Social media click: ${platform}`)

            // You can add Google Analytics or other tracking here
            // gtag('event', 'social_click', {
            //     'social_platform': platform,
            //     'event_category': 'Social Media',
            //     'event_label': platform
            // });
        })
    })

    // Animate footer sections on scroll
    const observerOptions = {
        threshold: 0.1,
        rootMargin: "0px 0px -50px 0px",
    }

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = "1"
                entry.target.style.transform = "translateY(0)"
            }
        })
    }, observerOptions)

    // Observe footer sections
    const footerSections = document.querySelectorAll(".footer-section")
    footerSections.forEach((section) => {
        section.style.opacity = "0"
        section.style.transform = "translateY(30px)"
        section.style.transition = "opacity 0.6s ease, transform 0.6s ease"
        observer.observe(section)
    })
})

// Helper functions
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
}

function showNotification(message, type = "info") {
    // Create notification element
    const notification = document.createElement("div")
    notification.className = `notification notification-${type}`
    notification.innerHTML = `
        <div class="notification-content">
            <i class="fas fa-${type === "success" ? "check-circle" : type === "error" ? "exclamation-circle" : "info-circle"}"></i>
            <span>${message}</span>
        </div>
        <button class="notification-close">
            <i class="fas fa-times"></i>
        </button>
    `

    // Add styles
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: ${type === "success" ? "#4caf50" : type === "error" ? "#f44336" : "#2196f3"};
        color: white;
        padding: 15px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 15px rgba(0,0,0,0.2);
        z-index: 10000;
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 15px;
        max-width: 400px;
        animation: slideInRight 0.3s ease;
    `

    // Add to page
    document.body.appendChild(notification)

    // Close button functionality
    const closeBtn = notification.querySelector(".notification-close")
    closeBtn.addEventListener("click", () => {
        notification.style.animation = "slideOutRight 0.3s ease"
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification)
            }
        }, 300)
    })

    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.style.animation = "slideOutRight 0.3s ease"
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification)
                }
            }, 300)
        }
    }, 5000)
}

// Add notification animations to head
if (!document.querySelector("#notification-styles")) {
    const style = document.createElement("style")
    style.id = "notification-styles"
    style.textContent = `
        @keyframes slideInRight {
            from {
                transform: translateX(100%);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes slideOutRight {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(100%);
                opacity: 0;
            }
        }
        
        .notification-content {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        
        .notification-close {
            background: none;
            border: none;
            color: white;
            cursor: pointer;
            padding: 5px;
            border-radius: 50%;
            transition: background 0.3s ease;
        }
        
        .notification-close:hover {
            background: rgba(255,255,255,0.2);
        }
    `
    document.head.appendChild(style)
}
