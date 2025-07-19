document.addEventListener('DOMContentLoaded', () => {
    // Tab switching functionality
    const tabLinks = document.querySelectorAll('.tab-link-notifications');
    const tabPanes = document.querySelectorAll('.panel-section-notifications');

    tabLinks.forEach(link => {
        link.addEventListener('click', function () {
            const targetId = this.getAttribute('data-target');
            tabLinks.forEach(tab => tab.classList.remove('active-tab-notifications'));
            tabPanes.forEach(pane => pane.classList.remove('show-panel-notifications'));
            this.classList.add('active-tab-notifications');
            const targetPane = document.getElementById(targetId);
            if (targetPane) {
                targetPane.classList.add('show-panel-notifications');
            }
        });
    });

    // Back button functionality
    const backButton = document.querySelector('.return-button-notifications');
    if (backButton) {
        backButton.addEventListener('click', function (e) {
            e.preventDefault();
            if (window.history.length > 1) {
                window.history.back();
            } else {
                window.location.href = '/';
            }
        });
    }

    // Add smooth scrolling
    document.documentElement.style.scrollBehavior = 'smooth';

    // Touch optimization for mobile
    const interactiveElements = document.querySelectorAll('.notification-card-header, .tab-link-notifications, .return-button-notifications, .payment-button-notifications');
    interactiveElements.forEach(element => {
        element.addEventListener('touchstart', function () {
            this.style.opacity = '0.7';
        });
        element.addEventListener('touchend', function () {
            this.style.opacity = '1';
        });
        element.addEventListener('touchcancel', function () {
            this.style.opacity = '1';
        });
    });

    // Check if server-side data is available
    function loadNotifications() {
        // Try to use server-side data first
        if (window.serverNotifications && Array.isArray(window.serverNotifications) && window.serverNotifications.length > 0) {
            console.log('Using server-side notifications data');
            renderNotifications(window.serverNotifications);
            return;
        }

        // Show server error if available
        if (window.serverError) {
            showAlert('danger', window.serverError);
            return;
        }

        // Fallback to AJAX loading
        console.log('Falling back to AJAX loading');
        fetch('/api/notifications', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                return response.text().then(text => {
                    try {
                        const data = JSON.parse(text);
                        return data;
                    } catch (e) {
                        console.error('Invalid JSON response:', text);
                        throw new Error(`Invalid JSON: ${e.message}`);
                    }
                });
            })
            .then(data => {
                if (data.error) {
                    console.error('Backend error:', data.error);
                    showAlert('danger', 'Không thể tải thông báo: ' + data.error);
                    return;
                }

                if (!data.notifications || !Array.isArray(data.notifications)) {
                    console.error('No notifications data:', data);
                    showAlert('danger', 'Không có dữ liệu thông báo.');
                    return;
                }

                renderNotifications(data.notifications);
            })
            .catch(error => {
                console.error('Error loading notifications:', error);
                showAlert('danger', 'Không thể tải thông báo: ' + error.message);
            });
    }

    // Render notifications from data
    function renderNotifications(notificationsData) {
        // Clear existing notifications
        const containers = {
            all: document.getElementById('all-notifications-list'),
            lodging: document.getElementById('lodging-notifications-list'),
            finance: document.getElementById('finance-notifications-list'),
            deals: document.getElementById('deals-notifications-list'),
            profile: document.getElementById('profile-notifications-list')
        };

        Object.values(containers).forEach(container => {
            if (container) container.innerHTML = '';
        });

        // Map NotificationType to tabs
        const typeToTab = {
            'PAYMENT': 'finance-notifications-list',
            'CONTRACT': 'lodging-notifications-list',
            'SYSTEM': 'profile-notifications-list',
            'REPORT': 'deals-notifications-list'
        };

        // Process notifications data (handle both server-side and AJAX formats)
        notificationsData.forEach(notificationData => {
            if (!notificationData) {
                console.warn('Invalid notification:', notificationData);
                return;
            }

            // Handle server-side format (wrapped in notification object)
            const notification = notificationData.notification || notificationData;
            const room = notificationData.room || notification.room;
            const paymentDetails = notificationData.paymentDetails;

            if (!notification) {
                console.warn('No notification object found:', notificationData);
                return;
            }

            const tabId = typeToTab[notification.type] || 'all-notifications-list';
            const container = document.getElementById(tabId);
            const allContainer = document.getElementById('all-notifications-list');

            if (!container || !allContainer) {
                console.error('Tab container not found for tabId:', tabId);
                return;
            }

            // Create and add card to both specific tab and all tab
            const specificCard = createNotificationCard(notification, room, paymentDetails);
            const allCard = createNotificationCard(notification, room, paymentDetails);
            
            if (specificCard && container !== allContainer) {
                container.appendChild(specificCard);
            }
            if (allCard) {
                allContainer.appendChild(allCard);
            }

            // Mark as read on click
            [container, allContainer].forEach(cont => {
                const cardHeader = cont.querySelector(`[data-notification-id="${notification.notificationId}"]`);
                if (cardHeader && !notification.isRead) {
                    cardHeader.addEventListener('click', () => {
                        markNotificationAsRead(notification.notificationId, cardHeader);
                    }, { once: true });
                }
            });
        });

        // Add empty state if no notifications
        Object.entries(containers).forEach(([key, container]) => {
            if (container && container.children.length === 0) {
                const emptyItem = document.createElement('div');
                emptyItem.className = 'notification-card-item';
                emptyItem.innerHTML = '<div class="notification-card-body text-center">Không có thông báo</div>';
                container.appendChild(emptyItem);
            }
        });
    }

    // Create notification card
    function createNotificationCard(notification, room, paymentDetails) {
        if (!notification) {
            console.error('Invalid notification data:', notification);
            return null;
        }

        const card = document.createElement('div');
        card.className = 'notification-card-item';
        const cardId = `notif-card-${notification.notificationId}`;
        const toggleId = `toggle-card-${notification.notificationId}`;

        // Parse payment details if not provided and it's a PAYMENT notification
        if (!paymentDetails && notification.type === 'PAYMENT' && notification.message) {
            paymentDetails = parsePaymentNotification(notification);
        }

        card.innerHTML = `
            <div class="notification-card-header" data-notification-id="${notification.notificationId}" onclick="toggleNotificationCard('${cardId}')">
                <div class="notification-card-info">
                    <h3 class="notification-card-title">
                        ${notification.title || 'Không có tiêu đề'}
                        ${notification.type === 'PAYMENT' && paymentDetails && paymentDetails.status === 'PENDING' ?
                '<span class="unpaid-status-badge">Chưa thanh toán</span>' : ''}
                    </h3>
                    <div class="notification-card-meta">
                        <span><i class="fas fa-clock"></i> ${formatDate(notification.createAt) || 'Không có ngày'}</span>
                        <span><i class="fas fa-tag"></i> ${getTagName(notification.type) || 'Không xác định'}</span>
                        <span><i class="fas fa-user"></i> ${getSender(notification.type) || 'Không xác định'}</span>
                    </div>
                </div>
                <div class="notification-card-toggle" id="${toggleId}">
                    <i class="fas fa-chevron-down"></i>
                </div>
            </div>
            <div class="notification-card-content" id="${cardId}">
                <div class="notification-card-body">
                    ${notification.type === 'PAYMENT' && paymentDetails ? renderPaymentNotification(paymentDetails, room) :
                `<div class="message-body-notifications">
                            <h4><i class="fas fa-info-circle" style="color: #3e83cc;"></i> Thông tin chi tiết</h4>
                            <p>${notification.message || 'Không có nội dung'}</p>
                            ${room ? `
                                <div class="data-block-notifications">
                                    <div class="data-title-notifications">
                                        <i class="fas fa-home"></i>
                                        Thông tin phòng
                                    </div>
                                    <div class="data-content-notifications">
                                        <strong>Phòng:</strong> ${room.namerooms || 'Không xác định'}<br>
                                        <strong>Loại phòng:</strong> ${room.category?.name || 'Không xác định'}<br>
                                        <strong>Diện tích:</strong> ${room.acreage || 'Không xác định'}m²<br>
                                        <strong>Giá phòng:</strong> ${room.price || '0'} VNĐ
                                    </div>
                                </div>
                            ` : '<p>Không có thông tin phòng liên quan.</p>'}
                        </div>`}
                </div>
            </div>
        `;

        return card;
    }

    // Parse PAYMENT notification message
    function parsePaymentNotification(notification) {
        try {
            const message = notification.message;
            
            // Check if this is a success notification
            if (message.includes('thanh toán thành công') || message.includes('Bạn đã thanh toán thành công')) {
                // Parse success notification
                const successMatch = message.match(/Bạn đã thanh toán thành công hóa đơn #(\d+) cho phòng ([^\s]+) tại ([^.]+)\. Tháng: ([^.]+)\. Số tiền: ([^.]+)\. Phương thức: ([^.]+)\./);
                
                if (successMatch) {
                    return {
                        invoiceId: successMatch[1],
                        month: successMatch[4],
                        total: successMatch[5],
                        roomName: successMatch[2],
                        hostelName: successMatch[3],
                        paymentMethod: successMatch[6],
                        status: 'SUCCESS',
                        details: []
                    };
                }
                
                // Fallback for success messages that don't match the pattern
                const fallbackMatch = message.match(/hóa đơn #(\d+)/);
                if (fallbackMatch) {
                    return {
                        invoiceId: fallbackMatch[1],
                        status: 'SUCCESS',
                        details: []
                    };
                }
            } else {
                // Parse payment reminder notification
                const match = message.match(/Hóa đơn #(\d+) cho tháng ([\d/]+) \(Tổng: ([^\)]+)\)\.? Hạn thanh toán: ([^\.]+)\.?/i);
                if (match) {
                    return {
                        invoiceId: match[1],
                        month: match[2],
                        total: match[3],
                        dueDate: match[4],
                        status: 'PENDING',
                        details: []
                    };
                }
            }
            
            console.warn('No match for payment notification:', message);
            return null;
        } catch (e) {
            console.error('Error parsing PAYMENT notification:', e, notification.message);
            return null;
        }
    }

    // Render PAYMENT notification content
    function renderPaymentNotification(paymentDetails, room) {
        console.log('Rendering payment notification with:', { paymentDetails, room });
        return `
            <div class="message-body-notifications">
                <h4><i class="fas fa-exclamation-triangle" style="color: #f59e0b;"></i> Thông báo thanh toán</h4>
                <p>Khoản thanh toán tiền phòng tháng ${paymentDetails.month || 'Không xác định'} của bạn ${paymentDetails.status === 'PENDING' ? 'đã đến hạn' : 'đã được xử lý thành công'}. Vui lòng ${paymentDetails.status === 'PENDING' ? 'thanh toán để tránh bị gián đoạn dịch vụ' : 'kiểm tra chi tiết dưới đây'}.</p>
                <div class="data-layout-notifications">
                    <div class="data-block-notifications">
                        <div class="data-title-notifications">
                            <i class="fas fa-home"></i>
                            Thông tin phòng
                        </div>
                        <div class="data-content-notifications">
                            <strong>Phòng:</strong> ${room?.namerooms || 'Không có thông tin phòng'}<br>
                            <strong>Loại phòng:</strong> ${room?.category?.name || 'Không xác định'}<br>
                            <strong>Diện tích:</strong> ${room?.acreage || 'Không xác định'}m²<br>
                            <strong>Giá phòng:</strong> ${room?.price || '0'} VNĐ
                        </div>
                    </div>
                    <div class="data-block-notifications">
                        <div class="data-title-notifications">
                            <i class="fas fa-dollar-sign"></i>
                            Chi phí ${paymentDetails.status === 'PENDING' ? 'cần thanh toán' : ''}
                        </div>
                        <div class="data-content-notifications">
                            
                            <strong style="color: ${paymentDetails.status === 'PENDING' ? '#dc2626' : '#FF8000'};">Tổng cộng:</strong> ${paymentDetails.total || '0 VNĐ'}<br>
                            ${paymentDetails.status === 'PENDING' ? `<strong style="color: #dc2626;">Hạn thanh toán:</strong> ${paymentDetails.dueDate || 'Không xác định'}` : ''}
                        </div>
                    </div>
                </div>
            </div>
            <div class="payment-record-notifications">
                <div class="payment-summary-notifications">
                    <div class="payment-label-notifications">Tiền phòng tháng ${paymentDetails.month || 'Không xác định'}</div>
                    <div class="payment-value-notifications" style="color: ${paymentDetails.status === 'PENDING' ? '#dc2626' : '#10b981'};">${paymentDetails.status === 'PENDING' ? '' : '-'} ${paymentDetails.total || '0 VNĐ'}</div>
                </div>
                <div class="payment-info-notifications">
                    <div><strong>Mã hóa đơn:</strong> ${paymentDetails.invoiceId || 'Không có'}</div>
                    <div><strong>Ngày phát hành:</strong> ${paymentDetails.dueDate ? paymentDetails.dueDate.split('-').reverse().join('/') : 'Không xác định'}</div>
                    <div><strong>Trạng thái:</strong> <span style="color: ${paymentDetails.status === 'PENDING' ? '#dc2626' : '#10b981'};">${paymentDetails.status === 'PENDING' ? 'Chưa thanh toán' : 'Thành công'}</span></div>
                    ${paymentDetails.status === 'PENDING' ? `<div><strong>Hạn thanh toán:</strong> <span style="color: #dc2626;">${paymentDetails.dueDate || 'Không xác định'}</span></div>` : ''}
                </div>
                ${paymentDetails.status === 'PENDING' ? `
                    <div class="payment-button-container">
                        <button class="payment-button-notifications" onclick="handlePayment('${paymentDetails.invoiceId || ''}', '${(paymentDetails.total || '0').replace(/[^0-9]/g, '')}', '${(room?.roomId || '').toString()}')">
                            <i class="fas fa-credit-card"></i>
                            Thanh toán ngay
                        </button>
                    </div>
                ` : ''}
            </div>
        `;
    }

    // Mark notification as read
    function markNotificationAsRead(notificationId, element) {
        fetch(`/api/notifications/${notificationId}/read`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${localStorage.getItem('token') || ''}` // Thêm token nếu cần
            }
        })
            .then(response => {
                if (!response.ok) {
                    throw new Error(`HTTP error! Status: ${response.status}`);
                }
                const card = element.closest('.notification-card-item');
                card.querySelector('.unpaid-status-badge')?.remove();
                card.querySelector('.notification-card-header').style.opacity = '0.7';
                showAlert('success', 'Thông báo đã được đánh dấu là đã đọc');
            })
            .catch(error => {
                console.error('Error marking notification as read:', error);
                showAlert('danger', 'Không thể đánh dấu thông báo là đã đọc: ' + error.message);
            });
    }

    // Format date
    function formatDate(date) {
        if (!date) return 'Không có ngày';
        const d = new Date(date);
        return `${d.getDate().toString().padStart(2, '0')}/${(d.getMonth() + 1).toString().padStart(2, '0')}/${d.getFullYear()} - ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
    }

    // Map NotificationType to tag name
    function getTagName(type) {
        switch (type) {
            case 'PAYMENT': return 'Thanh toán';
            case 'CONTRACT': return 'Hoạt động trọ';
            case 'SYSTEM': return 'Tài khoản';
            case 'REPORT': return 'Khuyến mãi';
            default: return 'Hệ thống';
        }
    }

    // Map NotificationType to sender
    function getSender(type) {
        switch (type) {
            case 'PAYMENT': return 'Hệ thống';
            case 'CONTRACT': return 'Kỹ thuật';
            case 'SYSTEM': return 'Hệ thống';
            case 'REPORT': return 'Marketing';
            default: return 'Hệ thống';
        }
    }

   

    // Show Bootstrap alert
    function showAlert(type, message) {
        const alertContainer = document.createElement('div');
        alertContainer.className = `alert alert-${type} alert-dismissible fade show`;
        alertContainer.role = 'alert';
        alertContainer.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        document.body.prepend(alertContainer);
        setTimeout(() => alertContainer.remove(), 5000);
    }

    // Load notifications on page load
    loadNotifications();
});