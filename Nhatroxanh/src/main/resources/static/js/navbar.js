// Remove any default active states on page load
const navLinks = document.querySelectorAll(".nav-link")
navLinks.forEach((link) => {
  link.classList.remove("active")
})

document.addEventListener("DOMContentLoaded", () => {
  // Variables for scroll handling
  let lastScrollTop = 0
  const navbar = document.querySelector(".navbar-custom")

  // Kiểm tra navbar có tồn tại không
  if (!navbar) {
    console.warn("Navbar not found")
    return
  }

  const navbarHeight = navbar.offsetHeight

  // Function to remove active class from all navigation items
  function removeAllActiveClasses() {
    // Remove active from all nav-links
    document.querySelectorAll(".nav-link").forEach((link) => {
      link.classList.remove("active")
    })

    // Remove active from all dropdown-items
    document.querySelectorAll(".dropdown-item").forEach((item) => {
      item.classList.remove("active")
    })
  }

  // Function to set active class for clicked item
  function setActiveClass(clickedElement) {
    // Remove all active classes first
    removeAllActiveClasses()

    // Add active class to clicked element
    clickedElement.classList.add("active")

    // If it's a dropdown item, also set its parent dropdown as active
    if (clickedElement.classList.contains("dropdown-item")) {
      const parentDropdown = clickedElement.closest(".dropdown")
      if (parentDropdown) {
        const dropdownToggle = parentDropdown.querySelector(".dropdown-toggle")
        if (dropdownToggle) {
          dropdownToggle.classList.add("active")
        }
      }
    }

    // Store the active item info for page reload
    const itemInfo = {
      text: clickedElement.textContent.trim(),
      href: clickedElement.getAttribute("href"),
      isDropdownItem: clickedElement.classList.contains("dropdown-item"),
    }
    localStorage.setItem("activeNavItem", JSON.stringify(itemInfo))
  }

  // Function to restore active state on page load
  function restoreActiveState() {
    const storedItem = localStorage.getItem("activeNavItem")
    if (storedItem) {
      try {
        const itemInfo = JSON.parse(storedItem)

        // Find the element by text content and href
        let targetElement = null

        if (itemInfo.isDropdownItem) {
          // Look for dropdown item
          document.querySelectorAll(".dropdown-item").forEach((item) => {
            if (item.textContent.trim() === itemInfo.text) {
              targetElement = item
            }
          })
        } else {
          // Look for nav link
          document.querySelectorAll(".nav-link").forEach((link) => {
            if (link.textContent.trim() === itemInfo.text || link.getAttribute("href") === itemInfo.href) {
              targetElement = link
            }
          })
        }

        if (targetElement) {
          removeAllActiveClasses()
          targetElement.classList.add("active")

          // If it's a dropdown item, also set parent as active
          if (targetElement.classList.contains("dropdown-item")) {
            const parentDropdown = targetElement.closest(".dropdown")
            if (parentDropdown) {
              const dropdownToggle = parentDropdown.querySelector(".dropdown-toggle")
              if (dropdownToggle) {
                dropdownToggle.classList.add("active")
              }
            }
          }
        }
      } catch (e) {
        console.log("Error restoring active state:", e)
      }
    }
  }

  // Handle clicks on ALL navigation links (existing and future ones)
  document.addEventListener("click", (e) => {
    // Check if clicked element is a nav-link (but not dropdown-toggle)
    if (e.target.closest(".nav-link") && !e.target.closest(".nav-link").classList.contains("dropdown-toggle")) {
      const navLink = e.target.closest(".nav-link")

      // Add ripple effect
      createRipple(e, navLink)

      // Set active class
      setActiveClass(navLink)

      // Let the default navigation happen (href will work automatically)
      // No need to prevent default or handle navigation manually
    }

    // Check if clicked element is a dropdown-item
    if (e.target.closest(".dropdown-item")) {
      const dropdownItem = e.target.closest(".dropdown-item")

      // Add ripple effect
      createRipple(e, dropdownItem)

      // Set active class
      setActiveClass(dropdownItem)

      // Let the default navigation happen (href will work automatically)
      // No need to prevent default or handle navigation manually
    }
  })

  // Restore active state on page load
  restoreActiveState()

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

  document.addEventListener("click", (e) => {
    if (e.target.closest(".nav-link") && !e.target.closest(".nav-link").classList.contains("dropdown-toggle")) {
      // Only auto-close if we're on mobile
      if (window.innerWidth < 992 && navbarCollapse && navbarCollapse.classList.contains("show")) {
        const bsCollapse = new window.bootstrap.Collapse(navbarCollapse, {
          toggle: false,
        })
        bsCollapse.hide()

        // Reset toggler icon
        if (navbarToggler) {
          navbarToggler.setAttribute("aria-expanded", "false")
        }
      }
    }
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
          : this.classList.contains("btn-notification")
            ? "thông báo"
            : "đăng tin cho chủ trọ"

      // Delay alert to allow ripple effect to show
      setTimeout(() => {
        if (this.classList.contains("btn-notification")) {
          alert(`Bạn có 3 thông báo mới!`)
        } else if (this.classList.contains("btn-login")) {
          // Don't show alert for login button, let the modal handle it
          return
        } else if (this.classList.contains("btn-register")) {
          return
        } else {
          alert(`Chức năng ${buttonType} đã được kích hoạt`)
        }
      }, 300)
    })
  })

  // User dropdown menu click handlers
  document.addEventListener("click", (e) => {
    if (e.target.closest(".user-dropdown-menu .dropdown-item")) {
      const dropdownItem = e.target.closest(".dropdown-item")
      const itemText = dropdownItem.textContent.trim()

      // Prevent default for demo purposes
      e.preventDefault()

      // Show feedback based on menu item
      setTimeout(() => {
        if (itemText.includes("Đăng xuất")) {
          // alert("Đăng xuất thành công!")
          // Set active state back to home page after logout
          removeAllActiveClasses()
          const homeLink = document.querySelector('.nav-link[href="/trang-chu"]')
          if (homeLink) {
            homeLink.classList.add("active")
            // Update localStorage to reflect home page as active
            const itemInfo = {
              text: homeLink.textContent.trim(),
              href: homeLink.getAttribute("href"),
              isDropdownItem: false,
            }
            localStorage.setItem("activeNavItem", JSON.stringify(itemInfo))
            // Redirect to home page after a short delay
            setTimeout(() => {
              window.location.href = "/trang-chu"
            }, 100)
          }
        } else if (itemText.includes("Hồ sơ")) {
          alert("Chuyển đến trang hồ sơ cá nhân")
        } else if (itemText.includes("Trọ đã lưu")) {
          alert("Hiển thị danh sách trọ đã lưu")
        } else if (itemText.includes("Cài đặt")) {
          alert("Mở trang cài đặt")
        }
      }, 100)
    }
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
})
