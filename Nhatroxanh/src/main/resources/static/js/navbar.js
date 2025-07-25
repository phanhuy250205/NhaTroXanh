document.querySelectorAll(".nav-link").forEach(link => {
    link.classList.remove("active");
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

    // --- MỞ MODAL ĐĂNG NHẬP/ĐĂNG KÝ ---
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

    // --- QUẢN LÝ TRẠNG THÁI ACTIVE MENU ---
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

    setActiveBasedOnURL();
    window.addEventListener("popstate", setActiveBasedOnURL);

    // --- XỬ LÝ SỰ KIỆN CLICK ---
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
            if (dropdownItem.tagName === 'BUTTON' && dropdownItem.type === 'submit') {
                return;
            }
        }

        // Xử lý click vào notification item
        const notificationItem = target.closest(".notification-item");
        if (notificationItem && notificationItem.classList.contains('unread')) {
            const notificationId = notificationItem.getAttribute('data-notification-id');
            markNotificationAsRead(notificationId, notificationItem);
        }
    });

    // --- HIỆU ỨNG CUỘN NAVBAR ---
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

    // --- HIỆU ỨNG RIPPLE ---
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

    // --- QUẢN LÝ THÔNG BÁO ---
    function loadNotifications() {
        const dropdownMenus = document.querySelectorAll('.notification-dropdown-menu');
        const isAuthenticated = !!document.querySelector('[sec\\:authorize="isAuthenticated()"]');

        dropdownMenus.forEach(dropdownMenu => {
            const dropdownId = dropdownMenu.getAttribute('aria-labelledby');
            const badge = dropdownMenu.closest('.notification-wrapper').querySelector('.notification-badge');
            const headerCount = dropdownMenu.querySelector('.notification-header small');
            const loadingIndicator = dropdownMenu.querySelector('.notification-loading');

            // Log DOM structure for debugging
            console.debug(`Processing dropdown: ${dropdownId}`);
            console.debug(`Found divider: ${!!dropdownMenu.querySelector('.dropdown-divider')}`);

            // Skip for anonymous users
            if (!isAuthenticated && dropdownId === 'anonymousNotificationDropdown') {
                console.log('Skipping notification fetch for anonymous user');
                const existingItems = dropdownMenu.querySelectorAll('.notification-item');
                existingItems.forEach(item => item.remove());
                const emptyItem = document.createElement('li');
                emptyItem.innerHTML = '<div class="dropdown-item text-center">Vui lòng đăng nhập để xem thông báo</div>';
                const divider = dropdownMenu.querySelector('.dropdown-divider');
                if (divider && divider.parentNode === dropdownMenu) {
                    dropdownMenu.insertBefore(emptyItem, divider);
                } else {
                    dropdownMenu.appendChild(emptyItem);
                }
                badge.textContent = '0';
                badge.style.display = 'none';
                headerCount.textContent = '0 mới';
                return;
            }

            // Show loading indicator
            loadingIndicator.classList.remove('d-none');

            fetch('/api/notifications', {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json'
                }
            })
            .then(response => {
                // Log raw response for debugging
                return response.text().then(text => {
                    try {
                        const data = JSON.parse(text);
                        return { response, data };
                    } catch (e) {
                        console.error('Invalid JSON response:', text);
                        throw new Error(`Invalid JSON: ${e.message}`);
                    }
                });
            })
            .then(({ response, data }) => {
                // Hide loading indicator
                loadingIndicator.classList.add('d-none');

                if (data.error) {
                    console.error('Backend error:', data.error);
                    showAlert('danger', 'Không thể tải thông báo: ' + data.error);
                    return;
                }

                // Update badge and header
                badge.textContent = data.unreadCount || 0;
                badge.style.display = data.unreadCount > 0 ? 'inline' : 'none';
                headerCount.textContent = `${data.unreadCount || 0} mới`;

                // Clear existing notification items
                const existingItems = dropdownMenu.querySelectorAll('.notification-item');
                existingItems.forEach(item => item.remove());

                // Find the first divider
                const divider = dropdownMenu.querySelector('.dropdown-divider');
                if (!divider || divider.parentNode !== dropdownMenu) {
                    console.warn(`Divider not found in dropdown ${dropdownId}, appending to end`);
                }

                // Add new notifications
                if (data.notifications && data.notifications.length > 0) {
                    data.notifications.forEach(notification => {
                        const iconClass = getIconClass(notification.type);
                        const bgClass = getBgClass(notification.type);
                        const href = getNotificationHref(notification);
                        const item = document.createElement('li');
                        item.innerHTML = `
                            <a class="dropdown-item notification-item ${notification.isRead ? '' : 'unread'}" href="${href}" data-notification-id="${notification.notificationId}">
                                <div class="notification-icon ${bgClass}">
                                    <i class="${iconClass} text-white"></i>
                                </div>
                                <div class="notification-content">
                                    <div class="notification-title">${notification.title}</div>
                                    <div class="notification-text">${notification.message}</div>
                                    <div class="notification-time">${formatTime(notification.createAt)}</div>
                                </div>
                                ${notification.isRead ? '' : '<div class="notification-dot"></div>'}
                            </a>
                        `;
                        if (divider && divider.parentNode === dropdownMenu) {
                            dropdownMenu.insertBefore(item, divider);
                        } else {
                            dropdownMenu.appendChild(item);
                        }
                    });
                } else {
                    // Show empty state
                    const emptyItem = document.createElement('li');
                    emptyItem.innerHTML = '<div class="dropdown-item text-center">Không có thông báo</div>';
                    if (divider && divider.parentNode === dropdownMenu) {
                        dropdownMenu.insertBefore(emptyItem, divider);
                    } else {
                        dropdownMenu.appendChild(emptyItem);
                    }
                }
            })
            .catch(error => {
                console.error('Error loading notifications:', error);
                loadingIndicator.classList.add('d-none');
                showAlert('danger', 'Không thể tải thông báo: ' + error.message);
            });
        });
    }

    // Mark notification as read
    function markNotificationAsRead(notificationId, element) {
        fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            }
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! Status: ${response.status}`);
            }
            element.classList.remove('unread');
            element.querySelector('.notification-dot')?.remove();
            document.querySelectorAll('.notification-badge').forEach(badge => {
                const currentCount = parseInt(badge.textContent) || 0;
                if (currentCount > 0) {
                    badge.textContent = currentCount - 1;
                    badge.style.display = badge.textContent > 0 ? 'inline' : 'none';
                }
            });
            document.querySelectorAll('.notification-header small').forEach(headerCount => {
                headerCount.textContent = `${document.querySelector('.notification-badge').textContent} mới`;
            });
        })
        .catch(error => console.error('Error marking notification as read:', error));
    }

    // Map notification type to icon
    function getIconClass(type) {
        switch (type) {
            case 'PAYMENT': return 'fas fa-exclamation';
            case 'CONTRACT': return 'fas fa-file-contract';
            case 'SYSTEM': return 'fas fa-bell';
            case 'REPORT': return 'fas fa-flag';
            default: return 'fas fa-bell';
        }
    }

    // Map notification type to background class
    function getBgClass(type) {
        switch (type) {
            case 'PAYMENT': return 'bg-warning';
            case 'CONTRACT': return 'bg-primary';
            case 'SYSTEM': return 'bg-secondary';
            case 'REPORT': return 'bg-danger';
            default: return 'bg-info';
        }
    }

    // Generate href based on notification type
    function getNotificationHref(notification) {
        if (notification.type === 'PAYMENT' && notification.paymentId) {
            return `/tenant/payments?paymentId=${notification.paymentId}`;
        }
        
        return '#';
    }

    // Format time for Date object in Vietnam timezone (GMT+7)
    function formatTime(date) {
        // Chuyển đổi đầu vào thành đối tượng Date
        const notificationDate = new Date(date);
        const now = new Date();

        // Kiểm tra xem notificationDate có hợp lệ không
        if (isNaN(notificationDate.getTime())) {
            console.warn(`Invalid date format: ${date}`);
            return "Không xác định";
        }

        // Lấy thời gian theo múi giờ Việt Nam (Asia/Ho_Chi_Minh)
        const vnOptions = { timeZone: 'Asia/Ho_Chi_Minh' };
        const nowVN = new Date(now.toLocaleString('en-US', vnOptions));
        const notificationVN = new Date(notificationDate.toLocaleString('en-US', vnOptions));

        // Tính chênh lệch thời gian (theo giây)
        const diff = (nowVN - notificationVN) / 1000;

        // Xử lý trường hợp thời gian âm (tương lai)
        if (diff < 0) {
            console.warn(`Notification date is in the future: ${date}`);
            return "Vừa xong";
        }

        // Các ngưỡng thời gian
        if (diff < 60) return `${Math.floor(diff)} giây trước`;
        if (diff < 3600) return `${Math.floor(diff / 60)} phút trước`;
        if (diff < 86400) return `${Math.floor(diff / 3600)} giờ trước`;
        if (diff < 604800) return `${Math.floor(diff / 86400)} ngày trước`; // Dưới 7 ngày

        // Nếu trên 7 ngày, hiển thị ngày tháng theo định dạng Việt Nam
        return notificationVN.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            timeZone: 'Asia/Ho_Chi_Minh'
        });
    }

    // Show alert (assuming it's defined elsewhere)
    function showAlert(type, message) {
        console.log(`Alert [${type}]: ${message}`);
        // Implement your alert display logic here
    }

    // Load notifications on page load
    loadNotifications();

    // Poll for new notifications every 30 seconds
    setInterval(loadNotifications, 30000);
});
