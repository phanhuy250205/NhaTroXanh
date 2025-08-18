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
                            const iconClass = getIconClass(notification);
                            const bgClass = getBgClass(notification);
                            const link = getNotificationLink(notification);
                            const item = document.createElement('li');
                            item.innerHTML = `
                            <a href="${link}" class="dropdown-item notification-item ${notification.isRead ? '' : 'unread'}" data-notification-id="${notification.notificationId}">
                                <div class="notification-icon ${bgClass}">
                                    <i class="${iconClass} text-white"></i>
                                </div>
                                <div class="notification-content">
                                    <div class="notification-title">${notification.title}</div>
                                    <div class="notification-time ">${notification.createAt}</div>
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

    // Cập nhật icon cho từng loại thông báo
    function getIconClass(notification) {
        if (notification.type === 'PAYMENT') {
            if (notification.title.includes('Lịch hẹn thanh toán tiền mặt')) {
                return 'fas fa-calendar-alt'; // Icon lịch cho lịch hẹn thanh toán
            }
            if (notification.title.includes('thành công') || notification.message.includes('thành công')) {
                return 'fas fa-check-circle'; // Icon check cho thanh toán thành công
            }
            return 'fas fa-money-bill-wave'; // Icon ví tiền cho nhắc nhở thanh toán
        }
        switch (notification.type) {
            case 'CONTRACT': return 'fas fa-file-signature'; // Icon hợp đồng
            case 'SYSTEM': return 'fas fa-cog'; // Icon bánh răng cho hệ thống
            case 'REPORT': return 'fas fa-tools'; // Icon cờ cho báo cáo
            case 'ACCOUNT': return 'fas fa-user-cog'; // Icon tài khoản cho thông báo tài khoản
            case 'HOSTEL_ACTIVITY': return 'fas fa-home'; // Icon nhà cho hoạt động nhà trọ
            default: return 'fas fa-gift'; // Icon chuông mặc định
        }
    }

    // Cập nhật màu nền cho từng loại thông báo
    function getBgClass(notification) {
        if (notification.type === 'PAYMENT') {
            if (notification.title.includes('Lịch hẹn thanh toán tiền mặt')) {
                return 'bg-info'; // Màu xanh dương nhạt cho lịch hẹn thanh toán
            }
            if (notification.title.includes('thành công') || notification.message.includes('thành công')) {
                return 'bg-success'; // Màu xanh lá cho thanh toán thành công
            }
            return 'bg-warning'; // Màu vàng cho nhắc nhở thanh toán
        }
        switch (notification.type) {
            case 'CONTRACT': return 'bg-info'; // Màu xanh dương nhạt cho hợp đồng
            case 'SYSTEM': return 'bg-dark'; // Màu xám đậm cho hệ thống
            case 'REPORT': return 'bg-danger'; // Màu đỏ cho báo cáo
            case 'ACCOUNT': return 'bg-primary'; // Màu xanh dương cho thông báo tài khoản
            case 'HOSTEL_ACTIVITY': return 'bg-info'; // Màu xanh dương nhạt cho hoạt động nhà trọ
            default: return 'bg-danger'; // Màu xám mặc định
        }
    }

    // Hàm xác định liên kết cho thông báo
    function getNotificationLink(notification) {
        switch (notification.type) {
            case 'PAYMENT':
                // Check payment status to determine redirect URL
                if (notification.paymentDetails && notification.paymentDetails.status === 'SUCCESS') {
                    // If payment is completed, redirect to payment history
                    return '/khach-thue/lich-su-thanh-toan';
                } else if (notification.invoiceId && notification.roomId && notification.hostelId) {
                    // If payment is pending and has required parameters, redirect to payment page
                    return `/thanh-toan?invoiceId=${notification.invoiceId}&room_id=${notification.roomId}&hostel_id=${notification.hostelId}`;
                } else {
                    // Fallback to management page if parameters are missing
                    return '/khach-thue/quan-ly-thue-tra';
                }
            case 'CONTRACT':
                return '/khach-thue/quan-ly-thue-tra'; // Liên kết đến trang quản lý hợp đồng
            case 'REPORT':
                return '/khach-thue/bao-cao-su-co'; // Liên kết đến trang báo cáo sự cố
            case 'ACCOUNT':
                // Determine profile page based on current URL or user role
                const currentPath = window.location.pathname;
                if (currentPath.includes('/admin/')) {
                    return '/admin/profile';
                } else if (currentPath.includes('/chu-tro/')) {
                    return '/chu-tro/profile-host';
                } else if (currentPath.includes('/nhan-vien/')) {
                    return '/nhan-vien/profile-staff';
                } else {
                    return '/khach-thue/profile-khach-thue'; // Default to guest profile
                }
            case 'HOSTEL_ACTIVITY':
                return '/khach-thue/hoat-dong-nha-tro'; // Liên kết đến trang hoạt động nhà trọ
            default:
                return '#!'; // Không chuyển hướng nếu không xác định
        }
    }

    // Format time for Date object in Vietnam timezone (GMT+7) - Chỉ hiển thị ngày
    function formatTime(date) {
        const notificationDate = new Date(date);
        const now = new Date();

        if (isNaN(notificationDate.getTime())) {
            console.warn(`Invalid date format: ${date}`);
            return "Không xác định";
        }

        const vnOptions = { timeZone: 'Asia/Ho_Chi_Minh' };
        const nowVN = new Date(now.toLocaleString('en-US', vnOptions));
        const notificationVN = new Date(notificationDate.toLocaleString('en-US', vnOptions));

        const diff = (nowVN - notificationVN) / 1000;

        if (diff < 0) {
            console.warn(`Notification date is in the future: ${date}`);
            return "Hôm nay";
        }

        if (diff < 86400) return "Hôm nay";
        if (diff < 172800) return "Hôm qua";
        if (diff < 604800) return `${Math.floor(diff / 86400)} ngày trước`;

        return notificationVN.toLocaleDateString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            timeZone: 'Asia/Ho_Chi_Minh'
        });
    }

    // Show alert (assuming it's defined elsewhere)
    function showAlert(type, message) {
        console.log(`Alert [${type}]: ${message}`);
    }

    // Load notifications on page load
    loadNotifications();

    // Poll for new notifications every 30 seconds
    setInterval(loadNotifications, 30000);
});

document.addEventListener('DOMContentLoaded', function () {
    const notificationDropdown = document.getElementById('notificationDropdown');
    if (!notificationDropdown) {
        console.log("Không tìm thấy chuông thông báo, script thông báo sẽ không chạy.");
        return;
    }

    const notificationBadge = document.querySelector('.notification-badge');
    const notificationHeaderCount = document.querySelector('.notification-header .text-muted');
    const notificationContainer = document.querySelector('.notification-container');
    const notificationLoading = document.querySelector('.notification-loading');

    async function fetchNotifications() {
        if (notificationLoading) notificationLoading.classList.remove('d-none');

        try {
            const response = await fetch('/api/notifications');
            if (!response.ok) {
                throw new Error(`Lỗi server: ${response.status}`);
            }

            const data = await response.json();
            const notifications = data.notifications || [];
            const unreadCount = data.unreadCount || 0;
            if (unreadCount > 0) {
                notificationBadge.textContent = unreadCount;
                notificationBadge.style.display = 'block';
                notificationHeaderCount.textContent = `${unreadCount} mới`;
            } else {
                notificationBadge.style.display = 'none';
                notificationHeaderCount.textContent = `0 mới`;
            }

            notificationContainer.innerHTML = '';
            if (notifications.length === 0) {
                notificationContainer.innerHTML = `
                    <li>
                        <div class="dropdown-item text-center text-muted p-3">
                            <i class="fas fa-check-circle fs-4 mb-2"></i>
                            <p class="mb-0">Không có thông báo mới</p>
                        </div>
                    </li>`;
            } else {
                notifications.forEach(noti => {
                    const isUnreadClass = !noti.isRead ? 'notification-unread' : '';
                    const iconClass = getIconClass(noti);
                    const bgClass = getBgClass(noti);
                    const link = getNotificationLink(noti);

                    const notiElement = document.createElement('li');
                    notiElement.innerHTML = `
                        <a href="${link}" class="dropdown-item notification-item ${isUnreadClass}" data-notification-id="${noti.notificationId}">
                            <div class="notification-icon ${bgClass}">
                                <i class="${iconClass} text-white"></i>
                            </div>
                            <div class="item-content">
                                <p class="mb-1 fw-bold">${noti.title}</p>
                                <small class="text-muted">${formatTimeAgo(noti.createAt)}</small>
                            </div>
                        </a>
                    `;
                    notificationContainer.appendChild(notiElement);
                });
            }
        } catch (error) {
            console.error('Lỗi khi lấy thông báo:', error);
            notificationContainer.innerHTML = `
                <li>
                    <div class="dropdown-item text-center text-danger p-3">
                        <i class="fas fa-exclamation-triangle fs-4 mb-2"></i>
                        <p class="mb-0">Lỗi tải thông báo</p>
                    </div>
                </li>`;
        } finally {
            if (notificationLoading) notificationLoading.classList.add('d-none');
        }
    }

    notificationContainer.addEventListener('click', async function (e) {
        const target = e.target.closest('.notification-item');
        if (!target) return;

        e.preventDefault();
        const notificationId = target.dataset.notificationId;
        const link = target.href;

        if (target.classList.contains('notification-unread')) {
            try {
                const csrfToken = document.querySelector('meta[name="_csrf"]')?.content || "";
                const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";

                let headers = { 'Content-Type': 'application/json' };
                if (csrfToken) {
                    headers[csrfHeader] = csrfToken;
                }

                await fetch(`/api/notifications/${notificationId}/read`, {
                    method: 'PUT',
                    headers: headers
                });
                target.classList.remove('notification-unread');
            } catch (error) {
                console.error('Lỗi khi đánh dấu đã đọc:', error);
            }
        }
        window.location.href = link;
    });

    // Hàm định dạng thời gian - Chỉ hiển thị ngày
    function formatTimeAgo(dateString) {
        const date = new Date(dateString);
        const now = new Date();
        const seconds = Math.floor((now - date) / 1000);

        if (seconds < 0) return "Hôm nay";
        if (seconds < 86400) return "Hôm nay";
        if (seconds < 172800) return "Hôm qua";
        if (seconds < 604800) return `${Math.floor(seconds / 86400)} ngày trước`;

        return date.toLocaleDateString('vi-VN');
    }

    // Cập nhật icon cho từng loại thông báo
    function getIconClass(notification) {
        if (notification.type === 'PAYMENT') {
            if (notification.title.includes('Lịch hẹn thanh toán tiền mặt')) {
                return 'fas fa-calendar-alt'; // Icon lịch cho lịch hẹn thanh toán
            }
            if (notification.title.includes('thành công') || notification.message.includes('thành công')) {
                return 'fas fa-check-circle'; // Icon check cho thanh toán thành công
            }
            return 'fas fa-money-bill-wave'; // Icon ví tiền cho nhắc nhở thanh toán
        }
        switch (notification.type) {
            case 'CONTRACT': return 'fas fa-file-signature'; // Icon hợp đồng
            case 'SYSTEM': return 'fas fa-cog'; // Icon bánh răng cho hệ thống
            case 'REPORT': return 'fas fa-flag'; // Icon cờ cho báo cáo
            case 'ACCOUNT': return 'fas fa-user-cog'; // Icon tài khoản cho thông báo tài khoản
            case 'HOSTEL_ACTIVITY': return 'fas fa-home'; // Icon nhà cho hoạt động nhà trọ
            default: return 'fas fa-percent'; // Icon chuông mặc định
        }
    }

    // Cập nhật màu nền cho từng loại thông báo
    function getBgClass(notification) {
        if (notification.type === 'PAYMENT') {
            if (notification.title.includes('Lịch hẹn thanh toán tiền mặt')) {
                return 'bg-info'; // Màu xanh dương nhạt cho lịch hẹn thanh toán
            }
            if (notification.title.includes('thành công') || notification.message.includes('thành công')) {
                return 'bg-success'; // Màu xanh lá cho thanh toán thành công
            }
            return 'bg-warning'; // Màu vàng cho nhắc nhở thanh toán
        }
        switch (notification.type) {
            case 'CONTRACT': return 'bg-info'; // Màu xanh dương nhạt cho hợp đồng
            case 'SYSTEM': return 'bg-dark'; // Màu xám đậm cho hệ thống
            case 'REPORT': return 'bg-danger'; // Màu đỏ cho báo cáo
            case 'ACCOUNT': return 'bg-primary'; // Màu xanh dương cho thông báo tài khoản
            case 'HOSTEL_ACTIVITY': return 'bg-info'; // Màu xanh dương nhạt cho hoạt động nhà trọ
            default: return 'bg-secondary'; // Màu xám mặc định
        }
    }

    // Hàm xác định liên kết cho thông báo
    function getNotificationLink(notification) {
        switch (notification.type) {
            case 'PAYMENT':
                // Check payment status to determine redirect URL
                if (notification.paymentDetails && notification.paymentDetails.status === 'SUCCESS') {
                    // If payment is completed, redirect to payment history
                    return '/khach-thue/lich-su-thanh-toan';
                } else if (notification.invoiceId && notification.roomId && notification.hostelId) {
                    // If payment is pending and has required parameters, redirect to payment page
                    return `/thanh-toan?invoiceId=${notification.invoiceId}&room_id=${notification.roomId}&hostel_id=${notification.hostelId}`;
                } else {
                    // Fallback to management page if parameters are missing
                    return '/khach-thue/quan-ly-thue-tra';
                }
            case 'CONTRACT':
                return '/khach-thue/quan-ly-thue-tra';
            case 'REPORT':
                return '/khach-thue/bao-cao-su-co';
            case 'ACCOUNT':
                // Determine profile page based on current URL or user role
                const currentPath = window.location.pathname;
                if (currentPath.includes('/admin/')) {
                    return '/admin/profile';
                } else if (currentPath.includes('/chu-tro/')) {
                    return '/chu-tro/profile-host';
                } else if (currentPath.includes('/nhan-vien/')) {
                    return '/nhan-vien/profile-staff';
                } else {
                    return '/khach-thue/profile-khach-thue'; // Default to guest profile
                }
            case 'HOSTEL_ACTIVITY':
                return '/khach-thue/hoat-dong-nha-tro';
            default:
                return '#!';
        }
    }

    notificationDropdown.addEventListener('show.bs.dropdown', fetchNotifications);
    fetchNotifications();
    setInterval(fetchNotifications, 30000);
});
