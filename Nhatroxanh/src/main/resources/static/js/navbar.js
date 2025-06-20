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

  // Function to set active class based on current URL
  function setActiveBasedOnURL() {
    const currentPath = window.location.pathname
    const currentPage = window.location.pathname.split('/').pop() || 'index'
    
    removeAllActiveClasses()
    
    let activeSet = false

    // Check nav-links first
    document.querySelectorAll(".nav-link").forEach((link) => {
      const href = link.getAttribute("href")
      const dataPage = link.getAttribute("data-page")
      
      if (!href || link.classList.contains("dropdown-toggle")) return
      
      // Exact match with href
      if (href === currentPath) {
        link.classList.add("active")
        activeSet = true
        return
      }
      
      // Match with data-page attribute
      if (dataPage && (currentPath.includes(dataPage) || currentPage === dataPage)) {
        link.classList.add("active")
        activeSet = true
        return
      }
      
      // Handle root path
      if ((currentPath === "/" || currentPath === "/trang-chu" || currentPath === "") && 
          (href === "/" || href.includes("trang-chu") || dataPage === "trang-chu")) {
        link.classList.add("active")
        activeSet = true
        return
      }
    })

    // Check dropdown items if no nav-link was activated
    if (!activeSet) {
      document.querySelectorAll(".dropdown-item").forEach((item) => {
        const href = item.getAttribute("href")
        
        if (!href) return
        
        // Exact match with href
        if (href === currentPath) {
          item.classList.add("active")
          
          // Also activate parent dropdown
          const parentDropdown = item.closest(".dropdown")
          if (parentDropdown) {
            const dropdownToggle = parentDropdown.querySelector(".dropdown-toggle")
            if (dropdownToggle) {
              dropdownToggle.classList.add("active")
            }
          }
          activeSet = true
          return
        }
        
        // Partial match for dynamic routes
        if (currentPath.includes(href.replace(/^\//, ''))) {
          item.classList.add("active")
          
          // Also activate parent dropdown
          const parentDropdown = item.closest(".dropdown")
          if (parentDropdown) {
            const dropdownToggle = parentDropdown.querySelector(".dropdown-toggle")
            if (dropdownToggle) {
              dropdownToggle.classList.add("active")
            }
          }
          activeSet = true
          return
        }
      })
    }
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

    // Store the active item info for consistency
    const itemInfo = {
      text: clickedElement.textContent.trim(),
      href: clickedElement.getAttribute("href"),
      isDropdownItem: clickedElement.classList.contains("dropdown-item"),
    }
    localStorage.setItem("activeNavItem", JSON.stringify(itemInfo))
  }

  // Set active state based on current URL on page load
  setActiveBasedOnURL()

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
      // The active state will be set correctly on the next page load
    }

    // Check if clicked element is a dropdown-item
    if (e.target.closest(".dropdown-item")) {
      const dropdownItem = e.target.closest(".dropdown-item")

      // Add ripple effect
      createRipple(e, dropdownItem)

      // Set active class
      setActiveClass(dropdownItem)

      // Let the default navigation happen (href will work automatically)
    }
  })

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

  // Xử lý click cho menu dropdown của người dùng
  document.addEventListener("click", (e) => {
      if (e.target.closest(".user-dropdown-menu .dropdown-item")) {
          const dropdownItem = e.target.closest(".dropdown-item");
          const itemText = dropdownItem.textContent.trim();

          // Ngăn hành vi mặc định cho mục demo
          e.preventDefault();

          // Hiển thị phản hồi dựa trên mục menu
          setTimeout(() => {
              if (itemText.includes("Đăng xuất")) {
                  // Xóa tất cả trạng thái active
                  removeAllActiveClasses();
                  // Xóa localStorage
                  localStorage.removeItem("activeNavItem");
                  // Chuyển hướng đến trang chủ
                  setTimeout(() => {
                      window.location.href = "/trang-chu";
                  }, 100);
              } else if (itemText.includes("Thông tin tài khoản")) {
                  alert("Chuyển đến trang thông tin tài khoản");
              } else if (itemText.includes("Đổi mật khẩu")) {
                  alert("Chuyển đến trang đổi mật khẩu");
              } else if (itemText.includes("Trọ đã lưu")) {
                  alert("Hiển thị danh sách trọ đã lưu");
              } else if (itemText.includes("Quản lý đánh giá")) {
                  alert("Mở trang quản lý đánh giá");
              }
          }, 100);
      }
  });

  // Listen for browser back/forward navigation
  window.addEventListener("popstate", () => {
    setActiveBasedOnURL()
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

    /* Active states styling */
    .nav-link.active {
      color: #3498DB !important;
      font-weight: 600;
    }
    
    .nav-link.active .nav-icon {
      color: #3498DB;
    }
    
    .dropdown-item.active {
      background-color: #E3F2FD;
      color: #3498DB !important;
    }
    
    .dropdown-toggle.active {
      color: #3498DB !important;
      font-weight: 600;
    }
    
    .dropdown-toggle.active .nav-icon {
      color: #3498DB;
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