// Remove any default active states on page load
document.querySelectorAll(".nav-link").forEach(link => {
    link.classList.remove("active")
});

document.addEventListener("DOMContentLoaded", () => {
    // Variables for scroll handling
    let lastScrollTop = 0;
    const navbar = document.querySelector(".navbar-custom");

    if (!navbar) {
        console.warn("Navbar not found");
        return;
    }

    const navbarHeight = navbar.offsetHeight;

    // --- MỞ MODAL ĐĂNG NHẬP/ĐĂNG KÝ (LOGIC MỚI) ---
    const loginBtnTrigger = document.getElementById('loginBtnTrigger');
    const registerBtnTrigger = document.getElementById('registerBtnTrigger');
    const loginModal = document.getElementById('loginModalOverlay');
    const registerModal = document.getElementById('registerModalOverlay');

    if (loginBtnTrigger && loginModal) {
        loginBtnTrigger.addEventListener('click', () => {
            loginModal.classList.add('show');
            document.body.style.overflow = 'hidden';
        });
    }

    if (registerBtnTrigger && registerModal) {
        registerBtnTrigger.addEventListener('click', () => {
            registerModal.classList.add('show');
            document.body.style.overflow = 'hidden';
        });
    }


    // --- QUẢN LÝ TRẠNG THÁI ACTIVE MENU (LOGIC GỐC CỦA BẠN) ---
    function removeAllActiveClasses() {
        document.querySelectorAll(".nav-link, .dropdown-item").forEach(el => el.classList.remove("active"));
    }

    function setActiveBasedOnURL() {
        const currentPath = window.location.pathname;
        removeAllActiveClasses();
        let activeSet = false;

        document.querySelectorAll(".nav-link:not(.dropdown-toggle)").forEach((link) => {
            if (link.getAttribute('href') === currentPath) {
                link.classList.add("active");
                activeSet = true;
            }
        });

        if (!activeSet) {
            document.querySelectorAll(".dropdown-item").forEach((item) => {
                if (item.getAttribute('href') === currentPath) {
                    item.classList.add("active");
                    const parentDropdown = item.closest(".dropdown");
                    if (parentDropdown) {
                        parentDropdown.querySelector(".dropdown-toggle")?.classList.add("active");
                    }
                }
            });
        }
    }
    
    // Set active state on initial load and on back/forward navigation
    setActiveBasedOnURL();
    window.addEventListener("popstate", setActiveBasedOnURL);


    // --- XỬ LÝ SỰ KIỆN CLICK (ĐÃ SỬA LỖI ĐĂNG XUẤT) ---
    document.addEventListener("click", (e) => {
        const target = e.target;
        
        // Xử lý hiệu ứng Ripple
        const rippleElement = target.closest(".btn, .nav-link, .dropdown-item");
        if (rippleElement) {
            createRipple(e, rippleElement);
        }

        // Xử lý click vào dropdown item
        const dropdownItem = target.closest(".user-dropdown-menu .dropdown-item");
        if (dropdownItem) {
             // KIỂM TRA QUAN TRỌNG: Nếu là nút đăng xuất, không làm gì cả để form hoạt động
            if (dropdownItem.tagName === 'BUTTON' && dropdownItem.type === 'submit') {
                return; 
            }
             // Đối với các link khác, có thể thêm logic alert ở đây nếu muốn
             // e.preventDefault();
             // alert(`Chức năng "${dropdownItem.textContent.trim()}" đang phát triển.`);
        }
    });


    // --- HIỆU ỨNG CUỘN NAVBAR (LOGIC GỐC CỦA BẠN) ---
    window.addEventListener("scroll", () => {
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        navbar.classList.toggle("scrolled", scrollTop > 10);

        if (scrollTop > navbarHeight) {
            if (scrollTop > lastScrollTop) {
                navbar.classList.add("scrolled-down");
                navbar.classList.remove("scrolled-up");
            } else {
                navbar.classList.remove("scrolled-down");
                navbar.classList.add("scrolled-up");
            }
        } else {
            navbar.classList.remove("scrolled-up", "scrolled-down");
        }
        lastScrollTop = scrollTop <= 0 ? 0 : scrollTop;
    }, { passive: true });


    // --- HIỆU ỨNG RIPPLE (LOGIC GỐC CỦA BẠN) ---
    function createRipple(event, element) {
        const circle = document.createElement("span");
        const diameter = Math.max(element.clientWidth, element.clientHeight);
        const radius = diameter / 2;
        const rect = element.getBoundingClientRect();

        circle.style.width = circle.style.height = `${diameter}px`;
        circle.style.left = `${event.clientX - rect.left - radius}px`;
        circle.style.top = `${event.clientY - rect.top - radius}px`;
        circle.classList.add("ripple");

        element.querySelector(".ripple")?.remove();
        element.appendChild(circle);
    }
});


