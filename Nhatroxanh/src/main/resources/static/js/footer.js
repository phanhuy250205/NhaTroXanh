// Footer JavaScript functionality
document.addEventListener("DOMContentLoaded", () => {
    // Initialize all footer functionality
    initBackToTop()
    initNewsletterForm()
    initSocialTracking()
    initScrollAnimations()
    initParallaxEffect()
    initFloatingElements()
})

// Back to top button functionality
function initBackToTop() {
    const backToTopButton = document.getElementById("backToTop")

    if (backToTopButton) {
        // Show/hide back to top button based on scroll position
        const handleScroll = () => {
            if (window.pageYOffset > 400) {
                backToTopButton.classList.add("show")
            } else {
                backToTopButton.classList.remove("show")
            }
        }

        window.addEventListener("scroll", handleScroll)

        // Smooth scroll to top when button is clicked
        backToTopButton.addEventListener("click", () => {
            window.scrollTo({
                top: 0,
                behavior: "smooth",
            })

            // Add click animation
            backToTopButton.style.transform = "translateY(-4px) scale(0.95)"
            setTimeout(() => {
                backToTopButton.style.transform = ""
            }, 150)
        })
    }
}

// Newsletter form submission
function initNewsletterForm() {
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
                shakeElement(emailInput)
                return
            }

            // Validate privacy checkbox
            if (!privacyCheckbox.checked) {
                showNotification("Vui lòng đồng ý với chính sách bảo mật!", "error")
                shakeElement(privacyCheckbox.closest(".privacy-checkbox"))
                return
            }

            // Show loading state
            const originalHTML = submitButton.innerHTML
            submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i>'
            submitButton.disabled = true
            submitButton.style.background = "linear-gradient(135deg, #6b7280, #9ca3af)"

            // Simulate API call
            setTimeout(() => {
                showNotification("Đăng ký thành công! Cảm ơn bạn đã đăng ký nhận thông báo.", "success")
                emailInput.value = ""
                privacyCheckbox.checked = false

                // Reset button with success animation
                submitButton.innerHTML = '<i class="fas fa-check"></i>'
                submitButton.style.background = "linear-gradient(135deg, #10b981, #34d399)"

                setTimeout(() => {
                    submitButton.innerHTML = originalHTML
                    submitButton.disabled = false
                    submitButton.style.background = ""
                }, 2000)
            }, 2000)
        })
    }
}

// Social link tracking
function initSocialTracking() {
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

            // Track social media click
            console.log(`Social media click: ${platform}`)

            // Add click animation
            this.style.transform = "translateY(-2px) scale(0.95)"
            setTimeout(() => {
                this.style.transform = ""
            }, 150)

            // Analytics tracking (replace with your analytics code)
            if (window.gtag) {
                window.gtag("event", "social_click", {
                    social_platform: platform,
                    event_category: "Social Media",
                    event_label: platform,
                })
            }
        })
    })
}

// Scroll animations
function initScrollAnimations() {
    const observerOptions = {
        threshold: 0.1,
        rootMargin: "0px 0px -50px 0px",
    }

    const observer = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = "1"
                entry.target.style.transform = "translateY(0)"

                // Add stagger animation for child elements
                const children = entry.target.querySelectorAll(".footer-stat-item, .footer-link, .contact-card, .trust-badge")
                children.forEach((child, index) => {
                    setTimeout(() => {
                        child.style.opacity = "1"
                        child.style.transform = "translateY(0)"
                    }, index * 100)
                })
            }
        })
    }, observerOptions)

    // Observe footer sections
    const footerSections = document.querySelectorAll(".footer-section")
    footerSections.forEach((section) => {
        observer.observe(section)
    })
}

// Parallax effect for floating elements
function initParallaxEffect() {
    const floatingElements = document.querySelectorAll(".floating-circle")

    window.addEventListener("scroll", () => {
        const scrolled = window.pageYOffset
        const rate = scrolled * -0.5

        floatingElements.forEach((element, index) => {
            const speed = (index + 1) * 0.3
            element.style.transform = `translateY(${rate * speed}px) rotate(${scrolled * 0.1}deg)`
        })
    })
}

// Floating elements animation
function initFloatingElements() {
    const circles = document.querySelectorAll(".floating-circle")

    circles.forEach((circle, index) => {
        // Random initial position
        const randomX = Math.random() * 100
        const randomY = Math.random() * 100

        circle.style.left = randomX + "%"
        circle.style.top = randomY + "%"

        // Continuous floating animation
        setInterval(
            () => {
                const newX = Math.random() * 100
                const newY = Math.random() * 100

                circle.style.transition = "all 10s ease-in-out"
                circle.style.left = newX + "%"
                circle.style.top = newY + "%"
            },
            (index + 1) * 8000,
        )
    })
}

// Helper functions
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
}

function shakeElement(element) {
    element.style.animation = "shake 0.5s ease-in-out"
    setTimeout(() => {
        element.style.animation = ""
    }, 500)
}

function showNotification(message, type = "info") {
    // Remove existing notifications
    const existingNotifications = document.querySelectorAll(".notification")
    existingNotifications.forEach((notification) => notification.remove())

    // Create notification element
    const notification = document.createElement("div")
    notification.className = `notification notification-${type}`
    notification.innerHTML = `
    <div class="notification-content">
      <div class="notification-icon">
        <i class="fas fa-${type === "success" ? "check-circle" : type === "error" ? "exclamation-circle" : "info-circle"}"></i>
      </div>
      <div class="notification-text">
        <span class="notification-title">${type === "success" ? "Thành công!" : type === "error" ? "Lỗi!" : "Thông báo"}</span>
        <span class="notification-message">${message}</span>
      </div>
    </div>
    <button class="notification-close">
      <i class="fas fa-times"></i>
    </button>
  `

    // Add styles
    notification.style.cssText = `
    position: fixed;
    top: 24px;
    right: 24px;
    background: ${type === "success" ? "linear-gradient(135deg, #10b981, #34d399)" : type === "error" ? "linear-gradient(135deg, #ef4444, #f87171)" : "linear-gradient(135deg, #3b82f6, #60a5fa)"};
    color: white;
    padding: 20px 24px;
    border-radius: 16px;
    box-shadow: 0 20px 40px rgba(0,0,0,0.15);
    z-index: 10000;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    max-width: 420px;
    min-width: 320px;
    animation: slideInRight 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.2);
  `

    // Add to page
    document.body.appendChild(notification)

    // Close button functionality
    const closeBtn = notification.querySelector(".notification-close")
    closeBtn.addEventListener("click", () => {
        notification.style.animation = "slideOutRight 0.3s ease-in-out"
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification)
            }
        }, 300)
    })

    // Auto remove after 5 seconds
    setTimeout(() => {
        if (notification.parentNode) {
            notification.style.animation = "slideOutRight 0.3s ease-in-out"
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification)
                }
            }, 300)
        }
    }, 5000)
}

// Add enhanced notification styles
if (!document.querySelector("#enhanced-notification-styles")) {
    const style = document.createElement("style")
    style.id = "enhanced-notification-styles"
    style.textContent = `
    @keyframes slideInRight {
      from {
        transform: translateX(100%) scale(0.8);
        opacity: 0;
      }
      to {
        transform: translateX(0) scale(1);
        opacity: 1;
      }
    }
    
    @keyframes slideOutRight {
      from {
        transform: translateX(0) scale(1);
        opacity: 1;
      }
      to {
        transform: translateX(100%) scale(0.8);
        opacity: 0;
      }
    }
    
    @keyframes shake {
      0%, 100% { transform: translateX(0); }
      25% { transform: translateX(-5px); }
      75% { transform: translateX(5px); }
    }
    
    .notification-content {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
    }
    
    .notification-icon {
      width: 40px;
      height: 40px;
      background: rgba(255, 255, 255, 0.2);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
    }
    
    .notification-text {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    
    .notification-title {
      font-weight: 700;
      font-size: 14px;
    }
    
    .notification-message {
      font-size: 13px;
      opacity: 0.9;
      line-height: 1.4;
    }
    
    .notification-close {
      background: rgba(255, 255, 255, 0.2);
      border: none;
      color: white;
      cursor: pointer;
      padding: 8px;
      border-radius: 50%;
      transition: background 0.3s ease;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    
    .notification-close:hover {
      background: rgba(255, 255, 255, 0.3);
    }
  `
    document.head.appendChild(style)
}
