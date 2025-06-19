document.addEventListener("DOMContentLoaded", () => {
    // Get DOM elements
    const sidebarHost = document.getElementById("sidebarHost")
    const sidebarToggleHost = document.getElementById("sidebarToggleHost")
    const sidebarOverlayHost = document.getElementById("sidebarOverlayHost")
    const navLinksHost = document.querySelectorAll(".sidebar-nav-host .nav-link-host")

    // Storage keys - Sử dụng sessionStorage để phân biệt session
    const ACTIVE_MENU_KEY = "sidebar_active_menu"
    const ACTIVE_SUBMENU_KEY = "sidebar_active_submenu"
    const DROPDOWN_STATE_KEY = "sidebar_dropdown_state"
    const SESSION_INITIALIZED_KEY = "sidebar_session_initialized"

    // Check if we're on mobile
    function isMobile() {
        return window.innerWidth < 992
    }

    // Check if this is a new session (thoát ra vào lại)
    function isNewSession() {
        return !sessionStorage.getItem(SESSION_INITIALIZED_KEY)
    }

    // Initialize session
    function initializeSession() {
        sessionStorage.setItem(SESSION_INITIALIZED_KEY, "true")
    }

    // Save active state to sessionStorage
    function saveActiveState(target, isSubmenu = false, parentTarget = null) {
        if (isSubmenu) {
            sessionStorage.setItem(ACTIVE_SUBMENU_KEY, target)
            sessionStorage.setItem(ACTIVE_MENU_KEY, parentTarget)
            sessionStorage.setItem(DROPDOWN_STATE_KEY, "open")
        } else {
            sessionStorage.setItem(ACTIVE_MENU_KEY, target)
            sessionStorage.removeItem(ACTIVE_SUBMENU_KEY)
            sessionStorage.removeItem(DROPDOWN_STATE_KEY)
        }
        console.log("Saved active state:", { target, isSubmenu, parentTarget })
    }

    // Load active state from sessionStorage
    function loadActiveState() {
        let activeMenu, activeSubmenu, dropdownState

        if (isNewSession()) {
            // Session mới - mặc định là tổng quan
            activeMenu = "overview"
            activeSubmenu = null
            dropdownState = null

            // Lưu trạng thái mặc định và khởi tạo session
            sessionStorage.setItem(ACTIVE_MENU_KEY, activeMenu)
            initializeSession()

            console.log("New session - defaulting to overview")
        } else {
            // Session cũ - lấy từ sessionStorage
            activeMenu = sessionStorage.getItem(ACTIVE_MENU_KEY) || "overview"
            activeSubmenu = sessionStorage.getItem(ACTIVE_SUBMENU_KEY)
            dropdownState = sessionStorage.getItem(DROPDOWN_STATE_KEY)

            console.log("Existing session - loading saved state:", { activeMenu, activeSubmenu, dropdownState })
        }

        // Reset all active states
        document.querySelectorAll(".nav-link-host").forEach((l) => l.classList.remove("active"))
        document.querySelectorAll(".nav-sublink-host").forEach((l) => l.classList.remove("active"))
        document.querySelectorAll(".nav-item-dropdown-host").forEach((item) => item.classList.remove("active"))

        if (activeSubmenu) {
            // Có submenu active
            const submenuLink = document.querySelector(`[data-target="${activeSubmenu}"]`)
            const parentDropdown = document.querySelector(`[data-target="${activeMenu}"]`)?.closest(".nav-item-dropdown-host")

            if (submenuLink && parentDropdown) {
                submenuLink.classList.add("active")
                parentDropdown.classList.add("active")
                parentDropdown.querySelector(".nav-link-dropdown-host").classList.add("active")

                // Position submenu on desktop
                if (!isMobile()) {
                    setTimeout(() => {
                        positionSubmenu(parentDropdown.querySelector(".nav-link-dropdown-host"))
                    }, 100)
                }
            }
        } else {
            // Chỉ có menu chính active
            const menuLink = document.querySelector(`[data-target="${activeMenu}"]`)
            if (menuLink) {
                menuLink.classList.add("active")
            }
        }
    }

    // Position submenu for desktop
    function positionSubmenu(dropdownLink) {
        if (!isMobile()) {
            const parentItem = dropdownLink.closest(".nav-item-dropdown-host")
            const submenu = parentItem.querySelector(".nav-submenu-host")
            const rect = dropdownLink.getBoundingClientRect()

            submenu.style.top = rect.top + "px"
            submenu.style.left = rect.right + 10 + "px"
        }
    }

    // Toggle sidebar function - CHỈ CHO MOBILE
    function toggleSidebarHost() {
        if (!isMobile()) return

        const isOpen = sidebarHost.classList.contains("show")

        if (!isOpen) {
            openSidebarHost()
        } else {
            closeSidebarHost()
        }
    }

    // Open sidebar function - CHỈ CHO MOBILE
    function openSidebarHost() {
        if (!isMobile()) return

        sidebarHost.classList.add("show", "slide-in-host")
        sidebarOverlayHost.classList.add("show")
        sidebarToggleHost.classList.add("active")
        document.body.style.overflow = "hidden"

        // Add stagger animation to nav items
        navLinksHost.forEach((link, index) => {
            link.style.animationDelay = `${index * 0.1}s`
            link.classList.add("fade-in-host")
        })
    }

    // Close sidebar function - CHỈ CHO MOBILE
    function closeSidebarHost() {
        if (!isMobile()) return

        sidebarHost.classList.remove("show", "slide-in-host")
        sidebarOverlayHost.classList.remove("show")
        sidebarToggleHost.classList.remove("active")
        document.body.style.overflow = ""

        // Remove animation classes
        navLinksHost.forEach((link) => {
            link.classList.remove("fade-in-host")
            link.style.animationDelay = ""
        })
    }

    // Event listeners - CHỈ HOẠT ĐỘNG TRÊN MOBILE
    if (sidebarToggleHost) {
        sidebarToggleHost.addEventListener("click", (e) => {
            e.preventDefault()
            e.stopPropagation()
            toggleSidebarHost()
        })
    }

    if (sidebarOverlayHost) {
        sidebarOverlayHost.addEventListener("click", () => {
            if (isMobile()) {
                closeSidebarHost()
            }
        })
    }

    // SUBMENU HANDLING - HOẠT ĐỘNG TRÊN CẢ DESKTOP VÀ MOBILE
    const dropdownItems = document.querySelectorAll(".nav-item-dropdown-host")
    const dropdownLinks = document.querySelectorAll(".nav-link-dropdown-host")
    const subLinks = document.querySelectorAll(".nav-sublink-host")

    // Handle dropdown toggle - HOẠT ĐỘNG TRÊN CẢ DESKTOP VÀ MOBILE
    dropdownLinks.forEach((link) => {
        link.addEventListener("click", function (e) {
            e.preventDefault()
            e.stopPropagation()

            const parentItem = this.closest(".nav-item-dropdown-host")
            const isActive = parentItem.classList.contains("active")
            const target = this.getAttribute("data-target")

            // Close all other dropdowns
            dropdownItems.forEach((item) => {
                if (item !== parentItem) {
                    item.classList.remove("active")
                }
            })

            // Toggle current dropdown
            if (isActive) {
                parentItem.classList.remove("active")
                // Nếu đóng dropdown, set active về menu chính
                saveActiveState(target, false)
            } else {
                parentItem.classList.add("active")
                positionSubmenu(this)
                // Lưu trạng thái dropdown mở
                saveActiveState(target, false)
                sessionStorage.setItem(DROPDOWN_STATE_KEY, "open")
            }

            // Set active cho parent menu
            document.querySelectorAll(".nav-link-host").forEach((l) => l.classList.remove("active"))
            document.querySelectorAll(".nav-sublink-host").forEach((l) => l.classList.remove("active"))
            this.classList.add("active")

            console.log("Dropdown toggled:", isActive ? "closed" : "opened")

            // Trigger navigation event
            const navigationEvent = new CustomEvent("sidebarNavigation", {
                detail: { target: target, element: this, isDropdown: true },
            })
            document.dispatchEvent(navigationEvent)
        })
    })

    // Handle submenu link clicks - ĐÂY LÀ PHẦN ĐƯỢC SỬA
    subLinks.forEach((subLink) => {
        subLink.addEventListener("click", function (e) {
            e.preventDefault()

            const target = this.getAttribute("data-target")
            const parentDropdown = this.closest(".nav-item-dropdown-host")
            const parentTarget = parentDropdown.querySelector(".nav-link-dropdown-host").getAttribute("data-target")

            // Handle navigation to different pages
            if (target === "room-management") {
                window.location.href = "/phong-tro-host"
                return
            } else if (target === "info-management") {
                window.location.href = "/thong-tin-tro-host"
                return
            }

            // Remove active from all nav links and sublinks
            document.querySelectorAll(".nav-link-host").forEach((l) => l.classList.remove("active"))
            document.querySelectorAll(".nav-sublink-host").forEach((l) => l.classList.remove("active"))

            // Add active to clicked sublink and parent
            this.classList.add("active")
            if (parentDropdown) {
                parentDropdown.querySelector(".nav-link-dropdown-host").classList.add("active")
            }

            // Save active state
            saveActiveState(target, true, parentTarget)

            // THÊM LOGIC ĐỂ ĐÓNG DROPDOWN SAU KHI CLICK SUBMENU
            setTimeout(() => {
                parentDropdown.classList.remove("active")
                // Cập nhật sessionStorage để không lưu trạng thái dropdown mở
                sessionStorage.removeItem(DROPDOWN_STATE_KEY)
            }, 300) // Delay 300ms để có hiệu ứng mượt mà

            // Close sidebar on mobile after clicking
            if (isMobile()) {
                setTimeout(() => {
                    closeSidebarHost()
                }, 500)
            }

            console.log("Submenu clicked:", target)

            const navigationEvent = new CustomEvent("sidebarNavigation", {
                detail: { target: target, element: this, isSubmenu: true, parentTarget: parentTarget },
            })
            document.dispatchEvent(navigationEvent)
        })
    })

    // Handle regular navigation link clicks (không phải dropdown)
    const regularNavLinks = document.querySelectorAll(".sidebar-nav-host .nav-link-host:not(.nav-link-dropdown-host)")
    regularNavLinks.forEach((linkHost) => {
        linkHost.addEventListener("click", function (e) {
            e.preventDefault()

            const target = this.getAttribute("data-target")

            // Close all dropdowns when clicking other nav items
            dropdownItems.forEach((item) => {
                item.classList.remove("active")
            })

            // Add loading effect
            this.classList.add("loading-host")

            // Remove active class from all links
            navLinksHost.forEach((l) => {
                l.classList.remove("active")
                l.style.transform = "scale(1)"
            })
            document.querySelectorAll(".nav-sublink-host").forEach((l) => l.classList.remove("active"))

            // Add active class to clicked link with animation
            setTimeout(() => {
                this.classList.add("active")
                this.classList.remove("loading-host")
                this.style.transform = "scale(1.05)"

                setTimeout(() => {
                    this.style.transform = "scale(1)"
                }, 200)
            }, 300)

            // Save active state
            saveActiveState(target, false)

            // Close sidebar on mobile after clicking a link
            if (isMobile()) {
                setTimeout(() => {
                    closeSidebarHost()
                }, 500)
            }

            console.log("Navigation clicked:", target)

            // Trigger custom event for navigation
            const navigationEvent = new CustomEvent("sidebarNavigation", {
                detail: { target: target, element: this },
            })
            document.dispatchEvent(navigationEvent)
        })

        // Add hover effects
        linkHost.addEventListener("mouseenter", function () {
            if (!this.classList.contains("active")) {
                this.style.transform = "translateY(-2px) scale(1.02)"
            }
        })

        linkHost.addEventListener("mouseleave", function () {
            if (!this.classList.contains("active")) {
                this.style.transform = "translateY(0) scale(1)"
            }
        })
    })

    // Close dropdown when clicking outside
    document.addEventListener("click", (e) => {
        if (!e.target.closest(".nav-item-dropdown-host")) {
            dropdownItems.forEach((item) => {
                item.classList.remove("active")
            })
            // Cập nhật sessionStorage khi đóng dropdown
            const activeMenu = sessionStorage.getItem(ACTIVE_MENU_KEY)
            if (activeMenu) {
                sessionStorage.removeItem(DROPDOWN_STATE_KEY)
            }
        }
    })

    // Handle window resize
    let resizeTimeout
    window.addEventListener("resize", () => {
        clearTimeout(resizeTimeout)
        resizeTimeout = setTimeout(() => {
            if (!isMobile()) {
                closeSidebarHost()
                document.body.style.overflow = ""
                // Reposition submenu if open
                const activeDropdown = document.querySelector(".nav-item-dropdown-host.active .nav-link-dropdown-host")
                if (activeDropdown) {
                    positionSubmenu(activeDropdown)
                }
            }
        }, 250)
    })

    // Handle escape key - CHỈ CHO MOBILE
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && isMobile() && sidebarHost.classList.contains("show")) {
            closeSidebarHost()
        }
    })

    // Handle page visibility change để detect khi user thoát ra vào lại
    document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") {
            console.log("Page became visible - checking session state")
        }
    })

    // Handle beforeunload để cleanup nếu cần
    window.addEventListener("beforeunload", () => {
        // Không clear sessionStorage ở đây vì chúng ta muốn giữ state khi reload
        console.log("Page unloading - keeping session state for reload")
    })

    // Initialize with smooth entrance animation - CHỈ CHO DESKTOP
    // if (!isMobile()) {
    //     setTimeout(() => {
    //         navLinksHost.forEach((link, index) => {
    //             link.style.opacity = "0"
    //             link.style.transform = "translateX(-20px)"

    //             setTimeout(() => {
    //                 link.style.transition = "all 0.5s ease"
    //                 link.style.opacity = "1"
    //                 link.style.transform = "translateX(0)"
    //             }, index * 100)
    //         })
    //     }, 500)
    // }

    // Logo image error handling
    const logoImages = document.querySelectorAll(".logo-image-host")
    logoImages.forEach((img) => {
        img.addEventListener("error", function () {
            this.style.display = "none"
            const parent = this.parentElement
            if (!parent.querySelector(".logo-fallback")) {
                const fallback = document.createElement("div")
                fallback.className = "logo-fallback"
                fallback.innerHTML = "NTX"
                fallback.style.cssText = `
                    width: 100%;
                    height: 100%;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: bold;
                    font-size: 12px;
                    color: white;
                    border-radius: 6px;
                `
                parent.appendChild(fallback)
            }
        })
    })

    // Public API
    window.SidebarAPI = {
        open: () => {
            if (isMobile()) openSidebarHost()
        },
        close: () => {
            if (isMobile()) closeSidebarHost()
        },
        toggle: () => {
            if (isMobile()) toggleSidebarHost()
        },
        setActive: (target, isSubmenu = false, parentTarget = null) => {
            saveActiveState(target, isSubmenu, parentTarget)
            loadActiveState()
        },
        resetToOverview: () => {
            sessionStorage.clear()
            loadActiveState()
        },
        getActiveState: () => ({
            activeMenu: sessionStorage.getItem(ACTIVE_MENU_KEY),
            activeSubmenu: sessionStorage.getItem(ACTIVE_SUBMENU_KEY),
            dropdownState: sessionStorage.getItem(DROPDOWN_STATE_KEY),
            isNewSession: isNewSession(),
        }),
        isMobile: isMobile,
    }

    // Load active state when page loads
    setTimeout(() => {
        loadActiveState()
    }, 100)
})

// Example usage:
document.addEventListener("sidebarNavigation", (e) => {
    console.log("User navigated to:", e.detail.target)
})

// Utility functions for external use
window.SidebarUtils = {
    // Set active menu programmatically
    setActiveMenu: (target) => {
        window.SidebarAPI.setActive(target, false)
    },

    // Set active submenu programmatically
    setActiveSubmenu: (submenuTarget, parentTarget) => {
        window.SidebarAPI.setActive(submenuTarget, true, parentTarget)
    },

    // Reset to overview (như khi thoát ra vào lại)
    resetToOverview: () => {
        window.SidebarAPI.resetToOverview()
    },

    // Get current active state
    getCurrentActive: () => window.SidebarAPI.getActiveState(),

    // Check if this is a new session
    isNewSession: () => !sessionStorage.getItem("sidebar_session_initialized"),
}
