document.addEventListener("DOMContentLoaded", () => {
    // Variables for scroll handling
    let lastScrollTop = 0
    const navbar = document.querySelector(".navbar-custom")
    const navbarHeight = navbar.offsetHeight

    // Remove any default active states on page load
    const navLinks = document.querySelectorAll(".nav-link")
    navLinks.forEach((link) => {
        link.classList.remove("active")
    })

    // Enhanced navigation link interactions with animations and page navigation
    navLinks.forEach((link) => {
        // Skip dropdown toggles as they are handled by Bootstrap
        if (!link.classList.contains("dropdown-toggle")) {
            link.addEventListener("click", function (e) {
                e.preventDefault()

                // Add ripple effect
                createRipple(e, this)

                // Get the page data attribute
                const page = this.getAttribute("data-page")

                // Handle navigation based on page type
                if (page) {
                    // Remove active class from all links
                    navLinks.forEach((l) => l.classList.remove("active"))

                    // Add active class to clicked link
                    this.classList.add("active")

                    // Store active state in localStorage
                    localStorage.setItem("activePage", page)

                    // Navigate to appropriate page after a short delay for animation
                    setTimeout(() => {
                        switch (page) {
                            case "trang-chu":
                                window.location.href = "trang-chu"
                                break
                            case "video-review":
                                // Create or navigate to video review page
                                window.location.href = "video-review"
                                break
                            case "blog":
                                // Create or navigate to blog page
                                window.location.href = "blog.html"
                                break
                            default:
                                // For any other pages, you can add more cases here
                                console.log(`Navigation to ${page} not implemented yet`)
                                break
                        }
                    }, 300) // Small delay to show the ripple effect
                }
            })
        }
    })

    // Handle dropdown item clicks
    const dropdownItems = document.querySelectorAll(".dropdown-item")
    dropdownItems.forEach((item) => {
        item.addEventListener("click", function (e) {
            e.preventDefault()

            // Add ripple effect
            createRipple(e, this)

            // You can add specific functionality for each dropdown item here
            console.log("Clicked:", this.textContent.trim())
        })
    })

    // Restore active state if explicitly set
    const activePage = localStorage.getItem("activePage")
    if (activePage) {
        const activeLink = document.querySelector(`[data-page="${activePage}"]`)
        if (activeLink) {
            activeLink.classList.add("active")
        }
    }

    // Enhanced scroll effect for navbar with hide/show on scroll
    window.addEventListener("scroll", () => {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop

        // Add scrolled class for subtle design changes
        if (scrollTop > 10) {
            navbar.classList.add("scrolled")
        } else {
            navbar.classList.remove("scrolled")
        }

        // Hide/show navbar on scroll
        if (scrollTop > navbarHeight) {
            if (scrollTop > lastScrollTop) {
                // Scrolling down
                navbar.classList.add("scrolled-down")
                navbar.classList.remove("scrolled-up")
            } else {
                // Scrolling up
                navbar.classList.remove("scrolled-down")
                navbar.classList.add("scrolled-up")
            }
        } else {
            navbar.classList.remove("scrolled-up")
        }

        lastScrollTop = scrollTop
    })

    // Auto-close mobile menu when clicking on a link (except dropdown toggles)
    const navbarCollapse = document.getElementById("navbarNav")
    const navbarToggler = document.querySelector(".navbar-toggler")

    navLinks.forEach((link) => {
        link.addEventListener("click", () => {
            // Only auto-close if it's not a dropdown toggle and we're on mobile
            if (
                window.innerWidth < 992 &&
                navbarCollapse.classList.contains("show") &&
                !link.classList.contains("dropdown-toggle")
            ) {
                const bsCollapse = new bootstrap.Collapse(navbarCollapse, {
                    toggle: false,
                })
                bsCollapse.hide()

                // Reset toggler icon
                navbarToggler.setAttribute("aria-expanded", "false")
            }
        })
    })

    // Button click handlers with enhanced feedback
    const buttons = document.querySelectorAll(".auth-buttons .btn")
    buttons.forEach((button) => {
        button.addEventListener("click", function (e) {
            createRipple(e, this)

            // Show feedback based on button type
            const buttonType = this.classList.contains("btn-login")
                ? "đăng nhập"
                : this.classList.contains("btn-register")
                    ? "đăng ký"
                    : "đăng tin cho chủ trọ"

            // Delay alert to allow ripple effect to show
            setTimeout(() => {
                alert(`Chức năng ${buttonType} đã được kích hoạt`)
            }, 300)
        })
    })

    // Ripple effect function for buttons and links
    function createRipple(event, element) {
        const circle = document.createElement("span")
        const diameter = Math.max(element.clientWidth, element.clientHeight)
        const radius = diameter / 2

        // Position the ripple
        const rect = element.getBoundingClientRect()

        circle.style.width = circle.style.height = `${diameter}px`
        circle.style.left = `${event.clientX - rect.left - radius}px`
        circle.style.top = `${event.clientY - rect.top - radius}px`
        circle.classList.add("ripple")

        // Remove existing ripples
        const ripple = element.querySelector(".ripple")
        if (ripple) {
            ripple.remove()
        }

        // Add new ripple
        element.appendChild(circle)

        // Remove ripple after animation
        setTimeout(() => {
            if (circle) {
                circle.remove()
            }
        }, 600)
    }

    // Add ripple style
    const style = document.createElement("style")
    style.textContent = `
        .ripple {
            position: absolute;
            background-color: rgba(255, 255, 255, 0.4);
            border-radius: 50%;
            transform: scale(0);
            animation: ripple 0.6s linear;
            pointer-events: none;
        }
        
        @keyframes ripple {
            to {
                transform: scale(4);
                opacity: 0;
            }
        }
        
        .btn, .nav-link, .dropdown-item {
            position: relative;
            overflow: hidden;
        }
    `
    document.head.appendChild(style)

    // Preload hover states for smoother interactions
    function preloadHoverStates() {
        const hoverStyle = document.createElement("div")
        hoverStyle.style.position = "absolute"
        hoverStyle.style.width = "0"
        hoverStyle.style.height = "0"
        hoverStyle.style.opacity = "0"
        hoverStyle.style.pointerEvents = "none"
        document.body.appendChild(hoverStyle)

        // Preload button hover states
        hoverStyle.className = "btn-login hover"
        hoverStyle.className = "btn-register hover"
        hoverStyle.className = "btn-post hover"

        // Remove after preloading
        setTimeout(() => {
            document.body.removeChild(hoverStyle)
        }, 500)
    }

    // Call preload function
    preloadHoverStates()

    // Handle dropdown item navigation
    dropdownItems.forEach((item) => {
        item.addEventListener("click", function (e) {
            e.preventDefault()

            // Add ripple effect
            createRipple(e, this)

            // Get the text content to determine navigation
            const itemText = this.textContent.trim()

            // Navigate based on dropdown item after animation
            setTimeout(() => {
                switch (itemText) {
                    case "Phòng trọ":
                        window.location.href = "phong-tro"
                        break
                    case "Nhà nguyên căn":
                        window.location.href = "nha-nguyen-can.html"
                        break
                    case "Căn hộ":
                        window.location.href = "can-ho.html"
                        break
                    default:
                        console.log(`Navigation to ${itemText} not implemented yet`)
                        break
                }
            }, 300)
        })
    })
})
