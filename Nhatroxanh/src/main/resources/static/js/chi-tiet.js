// Property Detail Page JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // Initialize page
    initializePropertyDetail();
});

function initializePropertyDetail() {
    // Initialize thumbnail scrolling
    initializeThumbnailScroll();
    // Initialize favorite button
    initializeFavoriteButton();
    // Initialize contact buttons
    initializeContactButtons();
    // Initialize photo modal
    initializePhotoModal();

    // Initialize tabs
    initializeTabs();
}

// Thumbnail Gallery Functions
function changeMainImage(imageSrc, thumbnailElement) {
    const mainImage = document.getElementById('mainImage');
    
    // Add loading effect
    mainImage.style.opacity = '0.5';
    
    setTimeout(() => {
        mainImage.src = imageSrc;
        mainImage.style.opacity = '1';
    }, 200);
    
    // Update active thumbnail
    document.querySelectorAll('.thumbnail-item').forEach(item => {
        item.classList.remove('active');
    });
    thumbnailElement.classList.add('active');
}

function scrollThumbnails(direction) {
    const container = document.querySelector('.thumbnail-scroll');
    const scrollAmount = 200;
    
    if (direction === 'prev') {
        container.scrollBy({
            left: -scrollAmount,
            behavior: 'smooth'
        });
    } else {
        container.scrollBy({
            left: scrollAmount,
            behavior: 'smooth'
        });
    }
}

function initializeThumbnailScroll() {
    const container = document.querySelector('.thumbnail-scroll');
    const prevBtn = document.querySelector('.thumbnail-nav.prev');
    const nextBtn = document.querySelector('.thumbnail-nav.next');
    
    // Check if scroll buttons are needed
    function checkScrollButtons() {
        const isScrollable = container.scrollWidth > container.clientWidth;
        prevBtn.style.display = isScrollable ? 'flex' : 'none';
        nextBtn.style.display = isScrollable ? 'flex' : 'none';
    }
    
    // Initial check
    checkScrollButtons();
    
    // Check on window resize
    window.addEventListener('resize', checkScrollButtons);
    
    // Update button states on scroll
    container.addEventListener('scroll', function() {
        const isAtStart = container.scrollLeft <= 0;
        const isAtEnd = container.scrollLeft >= container.scrollWidth - container.clientWidth;
        
        prevBtn.style.opacity = isAtStart ? '0.5' : '1';
        nextBtn.style.opacity = isAtEnd ? '0.5' : '1';
    });
}

// Contact Functions
function makeCall() {
    const phoneNumber = '0987654321';
    window.location.href = `tel:${phoneNumber}`;
}

function sendMessage() {
    // Simulate opening messaging app
    showNotification('Đang mở ứng dụng tin nhắn...', 'info');
    // In real app, this would open SMS or messaging app
}

function contactZalo() {
    // Simulate opening Zalo
    showNotification('Đang mở Zalo...', 'info');
    // In real app, this would open Zalo with specific contact
}

// Property Actions
function saveProperty() {
    const btn = event.target.closest('.btn-save');
    const icon = btn.querySelector('i');
    const text = btn.querySelector('span');
    
    if (btn.classList.contains('saved')) {
        // Remove from saved
        btn.classList.remove('saved');
        icon.className = 'fas fa-bookmark';
        text.textContent = 'Lưu tin';
        showNotification('Đã bỏ lưu tin', 'info');
    } else {
        // Add to saved
        btn.classList.add('saved');
        icon.className = 'fas fa-bookmark';
        text.textContent = 'Đã lưu';
        showNotification('Đã lưu tin thành công', 'success');
    }
}

function shareProperty() {
    if (navigator.share) {
        navigator.share({
            title: 'Nhà trọ số 123, đường ABC',
            text: 'Xem nhà trọ này trên Nhà Trọ Xanh',
            url: window.location.href
        });
    } else {
        // Fallback: copy to clipboard
        navigator.clipboard.writeText(window.location.href).then(() => {
            showNotification('Đã sao chép link vào clipboard', 'success');
        });
    }
}

function reportProperty() {
    if (confirm('Bạn có chắc chắn muốn báo cáo tin này?')) {
        showNotification('Cảm ơn bạn đã báo cáo. Chúng tôi sẽ xem xét trong thời gian sớm nhất.', 'info');
    }
}

// Photo Modal Functions
function openPhotoModal() {
    const modal = new bootstrap.Modal(document.getElementById('photoModal'));
    loadPhotoGrid();
    modal.show();
}

function loadPhotoGrid() {
    const photoGrid = document.querySelector('.photo-grid');
    const photos = [
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg',
        // 'images/cards/anh1.jpg'
    ];
    
    photoGrid.innerHTML = '';
    
    photos.forEach((photo, index) => {
        const img = document.createElement('img');
        img.src = photo;
        img.alt = `Hình ảnh ${index + 1}`;
        img.onclick = () => openPhotoViewer(photo);
        photoGrid.appendChild(img);
    });
}

function openPhotoViewer(imageSrc) {
    // Create full-screen image viewer
    const viewer = document.createElement('div');
    viewer.className = 'photo-viewer';
    viewer.innerHTML = `
        <div class="photo-viewer-overlay" onclick="closePhotoViewer()">
            <img src="${imageSrc}" alt="Hình ảnh lớn">
            <button class="close-viewer" onclick="closePhotoViewer()">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    document.body.appendChild(viewer);
    
    // Add styles for photo viewer
    const style = document.createElement('style');
    style.textContent = `
        .photo-viewer {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            z-index: 9999;
            background: rgba(0, 0, 0, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        .photo-viewer-overlay {
            position: relative;
            max-width: 90%;
            max-height: 90%;
        }
        
        .photo-viewer img {
            max-width: 100%;
            max-height: 100%;
            object-fit: contain;
        }
        
        .close-viewer {
            position: absolute;
            top: 20px;
            right: 20px;
            background: rgba(255, 255, 255, 0.2);
            border: none;
            border-radius: 50%;
            width: 40px;
            height: 40px;
            color: white;
            font-size: 1.2rem;
            cursor: pointer;
            backdrop-filter: blur(10px);
        }
    `;
    document.head.appendChild(style);
}

function closePhotoViewer() {
    const viewer = document.querySelector('.photo-viewer');
    if (viewer) {
        viewer.remove();
    }
}

// Initialize Functions
function initializeFavoriteButton() {
    const favoriteBtn = document.querySelector('.btn-favorite-large');
    
    favoriteBtn.addEventListener('click', function() {
        const icon = this.querySelector('i');
        
        if (icon.classList.contains('fas')) {
            // Remove from favorites
            icon.className = 'far fa-heart';
            showNotification('Đã bỏ khỏi danh sách yêu thích', 'info');
        } else {
            // Add to favorites
            icon.className = 'fas fa-heart';
            showNotification('Đã thêm vào danh sách yêu thích', 'success');
        }
    });
}

function initializeContactButtons() {
    // Add click effects to contact buttons
    const contactButtons = document.querySelectorAll('.contact-buttons .btn');
    
    contactButtons.forEach(button => {
        button.addEventListener('click', function() {
            // Add ripple effect
            createRippleEffect(this, event);
        });
    });
}

function initializePhotoModal() {
    // Initialize photo modal events
    const photoModal = document.getElementById('photoModal');
    
    photoModal.addEventListener('shown.bs.modal', function() {
        // Focus management for accessibility
        this.querySelector('.btn-close').focus();
    });
}

function initializeTabs() {
    // Add smooth scrolling to tab content
    const tabLinks = document.querySelectorAll('#detailTabs .nav-ct');
    
    tabLinks.forEach(link => {
        link.addEventListener('click', function() {
            // Smooth scroll to tab content
            setTimeout(() => {
                const tabContent = document.querySelector('.tab-content');
                tabContent.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            }, 100);
        });
    });
}

// Utility Functions - FIXED NOTIFICATION SYSTEM
function showNotification(message, type = 'info') {
    // Remove existing notifications first
    const existingNotifications = document.querySelectorAll('.notification');
    existingNotifications.forEach(notification => {
        notification.remove();
    });
    
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.innerHTML = `
        <div class="notification-content">
            <div class="notification-icon">
                <i class="fas fa-${getNotificationIcon(type)}"></i>
            </div>
            <div class="notification-text">
                <span class="notification-message">${message}</span>
            </div>
            <button class="notification-close" onclick="this.parentElement.remove()">
                <i class="fas fa-times"></i>
            </button>
        </div>
    `;
    
    // Add improved styles
    if (!document.querySelector('#notification-styles')) {
        const style = document.createElement('style');
        style.id = 'notification-styles';
        style.textContent = `
            .notification {
                position: fixed;
                top: 90px; /* Adjusted to be below navbar (70px + 20px margin) */
                right: 20px;
                background: white;
                border-radius: 12px;
                box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
                z-index: 10000; /* Higher than navbar z-index */
                animation: slideInRight 0.4s cubic-bezier(0.68, -0.55, 0.265, 1.55);
                max-width: 380px;
                min-width: 300px;
                border: 1px solid rgba(0, 0, 0, 0.08);
                backdrop-filter: blur(10px);
                overflow: hidden;
            }
            
            .notification-content {
                padding: 16px 20px;
                display: flex;
                align-items: flex-start;
                gap: 12px;
                position: relative;
            }
            
            .notification-icon {
                flex-shrink: 0;
                width: 24px;
                height: 24px;
                border-radius: 50%;
                display: flex;
                align-items: center;
                justify-content: center;
                font-size: 12px;
                margin-top: 2px;
            }
            
            .notification-text {
                flex: 1;
                min-width: 0;
            }
            
            .notification-message {
                font-size: 14px;
                font-weight: 500;
                line-height: 1.4;
                color: #2c3e50;
                display: block;
            }
            
            .notification-close {
                flex-shrink: 0;
                background: none;
                border: none;
                color: #95a5a6;
                cursor: pointer;
                padding: 4px;
                border-radius: 4px;
                transition: all 0.2s ease;
                margin-top: -2px;
            }
            
            .notification-close:hover {
                background: rgba(0, 0, 0, 0.05);
                color: #7f8c8d;
            }
            
            .notification-success {
                border-left: 4px solid #27ae60;
            }
            
            .notification-success .notification-icon {
                background: #d5f4e6;
                color: #27ae60;
            }
            
            .notification-info {
                border-left: 4px solid #3498db;
            }
            
            .notification-info .notification-icon {
                background: #dbeafe;
                color: #3498db;
            }
            
            .notification-warning {
                border-left: 4px solid #f39c12;
            }
            
            .notification-warning .notification-icon {
                background: #fef3cd;
                color: #f39c12;
            }
            
            .notification-error {
                border-left: 4px solid #e74c3c;
            }
            
            .notification-error .notification-icon {
                background: #fdeaea;
                color: #e74c3c;
            }
            
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
            
            .notification.removing {
                animation: slideOutRight 0.3s ease-in-out forwards;
            }
            
            /* Mobile responsive */
            @media (max-width: 768px) {
                .notification {
                    top: 80px;
                    right: 15px;
                    left: 15px;
                    max-width: none;
                    min-width: auto;
                }
                
                .notification-content {
                    padding: 14px 16px;
                }
                
                .notification-message {
                    font-size: 13px;
                }
            }
            
            /* Progress bar for auto-dismiss */
            .notification::after {
                content: '';
                position: absolute;
                bottom: 0;
                left: 0;
                height: 3px;
                background: linear-gradient(90deg, 
                    var(--notification-color, #3498db) 0%, 
                    var(--notification-color, #3498db) 100%);
                animation: progressBar 4s linear;
                border-radius: 0 0 12px 12px;
            }
            
            .notification-success::after {
                --notification-color: #27ae60;
            }
            
            .notification-info::after {
                --notification-color: #3498db;
            }
            
            .notification-warning::after {
                --notification-color: #f39c12;
            }
            
            .notification-error::after {
                --notification-color: #e74c3c;
            }
            
            @keyframes progressBar {
                from {
                    width: 100%;
                }
                to {
                    width: 0%;
                }
            }
        `;
        document.head.appendChild(style);
    }
    
    // Add to page
    document.body.appendChild(notification);
    
    // Auto remove after 4 seconds with smooth animation
    setTimeout(() => {
        if (notification.parentNode) {
            notification.classList.add('removing');
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }
    }, 4000);
    
    // Add click to dismiss
    notification.addEventListener('click', function(e) {
        if (e.target.closest('.notification-close')) {
            return; // Let the close button handle it
        }
        this.classList.add('removing');
        setTimeout(() => {
            if (this.parentNode) {
                this.parentNode.removeChild(this);
            }
        }, 300);
    });
}

function getNotificationIcon(type) {
    const icons = {
        success: 'check',
        info: 'info',
        warning: 'exclamation',
        error: 'times'
    };
    return icons[type] || 'info';
}

function createRippleEffect(element, event) {
    const ripple = document.createElement('span');
    const rect = element.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    const x = event.clientX - rect.left - size / 2;
    const y = event.clientY - rect.top - size / 2;
    
    ripple.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        left: ${x}px;
        top: ${y}px;
        background: rgba(255, 255, 255, 0.3);
        border-radius: 50%;
        transform: scale(0);
        animation: ripple 0.6s linear;
        pointer-events: none;
    `;
    
    element.style.position = 'relative';
    element.style.overflow = 'hidden';
    element.appendChild(ripple);
    
    setTimeout(() => {
        ripple.remove();
    }, 600);
}

// Add ripple animation styles
const rippleStyles = document.createElement('style');
rippleStyles.textContent = `
    @keyframes ripple {
        to {
            transform: scale(4);
            opacity: 0;
        }
    }
`;
document.head.appendChild(rippleStyles);

// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Lazy loading for images
function initializeLazyLoading() {
    const images = document.querySelectorAll('img[data-src]');
    
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                img.src = img.dataset.src;
                img.classList.remove('lazy');
                imageObserver.unobserve(img);
            }
        });
    });
    
    images.forEach(img => imageObserver.observe(img));
}

// Initialize lazy loading if supported
if ('IntersectionObserver' in window) {
    initializeLazyLoading();
}

// Handle window resize for responsive adjustments
window.addEventListener('resize', function() {
    // Recalculate thumbnail scroll buttons
    const thumbnailContainer = document.querySelector('.thumbnail-scroll');
    if (thumbnailContainer) {
        const event = new Event('scroll');
        thumbnailContainer.dispatchEvent(event);
    }
});

// Add keyboard navigation for accessibility
document.addEventListener('keydown', function(e) {
    // Close photo viewer with Escape key
    if (e.key === 'Escape') {
        const photoViewer = document.querySelector('.photo-viewer');
        if (photoViewer) {
            closePhotoViewer();
        }
        
        // Also close notifications with Escape
        const notifications = document.querySelectorAll('.notification');
        notifications.forEach(notification => {
            notification.classList.add('removing');
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        });
    }
    
    // Navigate thumbnails with arrow keys
    if (e.key === 'ArrowLeft' || e.key === 'ArrowRight') {
        const activeThumbnail = document.querySelector('.thumbnail-item.active');
        if (activeThumbnail) {
            const thumbnails = Array.from(document.querySelectorAll('.thumbnail-item'));
            const currentIndex = thumbnails.indexOf(activeThumbnail);
            
            let newIndex;
            if (e.key === 'ArrowLeft') {
                newIndex = currentIndex > 0 ? currentIndex - 1 : thumbnails.length - 1;
            } else {
                newIndex = currentIndex < thumbnails.length - 1 ? currentIndex + 1 : 0;
            }
            
            thumbnails[newIndex].click();
        }
    }
});

// Add notification queue system for multiple notifications
let notificationQueue = [];
let isShowingNotification = false;

function queueNotification(message, type) {
    notificationQueue.push({ message, type });
    processNotificationQueue();
}

function processNotificationQueue() {
    if (isShowingNotification || notificationQueue.length === 0) {
        return;
    }
    
    isShowingNotification = true;
    const { message, type } = notificationQueue.shift();
    showNotification(message, type);
    
    // Reset flag after notification duration
    setTimeout(() => {
        isShowingNotification = false;
        processNotificationQueue();
    }, 4500);
}

// Enhanced notification system with sound (optional)
function playNotificationSound(type) {
    // Only play sound if user has interacted with page (browser policy)
    if (document.hasFocus()) {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();
        
        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);
        
        // Different frequencies for different notification types
        const frequencies = {
            success: 800,
            info: 600,
            warning: 400,
            error: 300
        };
        
        oscillator.frequency.setValueAtTime(frequencies[type] || 600, audioContext.currentTime);
        oscillator.type = 'sine';
        
        gainNode.gain.setValueAtTime(0.1, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1);
        
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.1);
    }
}