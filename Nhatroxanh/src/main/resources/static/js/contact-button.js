// Contact Widget JavaScript
document.addEventListener('DOMContentLoaded', function() {
    const contactBtn = document.getElementById('contactBtn');
    const contactMenu = document.getElementById('contactMenu');
    const contactIcon = document.getElementById('contactIcon');
    
    let isMenuOpen = false;
    
    // Toggle menu khi nhấn vào nút contact
    contactBtn.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    });
    
    // Đóng menu khi click ra ngoài
    document.addEventListener('click', function(e) {
        if (isMenuOpen && !contactBtn.contains(e.target) && !contactMenu.contains(e.target)) {
            closeMenu();
        }
    });
    
    // Đóng menu khi nhấn ESC
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape' && isMenuOpen) {
            closeMenu();
        }
    });
    
    function openMenu() {
        isMenuOpen = true;
        contactMenu.classList.remove('closing');
        contactMenu.classList.add('show');
        contactBtn.classList.add('active');
        contactBtn.classList.remove('closing');
        contactIcon.className = 'fas fa-times';
    }
    
    function closeMenu() {
        isMenuOpen = false;
        
        // Add closing animation
        contactMenu.classList.add('closing');
        contactBtn.classList.add('closing');
        contactBtn.classList.remove('active');
        
        // Apply closing animation to items
        const items = contactMenu.querySelectorAll('.contact-item');
        items.forEach((item, index) => {
            item.style.animation = `slideOutBounce 0.4s cubic-bezier(0.55, 0.085, 0.68, 0.53) ${index * 0.1}s both`;
        });
        
        // Remove classes after animation completes
        setTimeout(() => {
            contactMenu.classList.remove('show', 'closing');
            contactBtn.classList.remove('closing');
            contactIcon.className = 'fas fa-comments';
            
            // Reset item animations
            items.forEach(item => {
                item.style.animation = '';
            });
        }, 500);
    }
    
    // Thêm hiệu ứng hover cho contact links
    const contactLinks = document.querySelectorAll('.contact-link');
    contactLinks.forEach(link => {
        link.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-5px) scale(1.05) rotateZ(-2deg)';
        });
        
        link.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
        
        // Xử lý click cho các link
        link.addEventListener('click', function(e) {
            // Đóng menu sau khi click vào link
            setTimeout(() => {
                closeMenu();
            }, 300);
            
            // Thêm hiệu ứng click
            this.style.transform = 'scale(0.9) rotateZ(5deg)';
            setTimeout(() => {
                this.style.transform = 'translateY(-5px) scale(1.05) rotateZ(-2deg)';
            }, 150);
        });
    });
    
    // Thêm hiệu ứng cho nút chính
    contactBtn.addEventListener('mouseenter', function() {
        if (!isMenuOpen) {
            this.style.transform = 'scale(1.15) rotate(15deg)';
            this.style.boxShadow = '0 8px 30px rgba(46, 134, 193, 0.8)';
        }
    });
    
    contactBtn.addEventListener('mouseleave', function() {
        if (!isMenuOpen) {
            this.style.transform = 'scale(1) rotate(0deg)';
            this.style.boxShadow = '0 4px 20px rgba(46, 134, 193, 0.4)';
        }
    });
    
    // Smooth scroll behavior cho phone link
    const phoneLink = document.querySelector('.contact-link.phone');
    if (phoneLink) {
        phoneLink.addEventListener('click', function(e) {
            // Tạo vibration effect nếu hỗ trợ
            if (navigator.vibrate) {
                navigator.vibrate(200);
            }
        });
    }
    
    // Lazy loading animation khi scroll
    function checkScroll() {
        const widget = document.querySelector('.contact-widget');
        const scrollTop = window.pageYOffset || document.documentElement.scrollTop;
        
        if (scrollTop > 100) {
            widget.style.opacity = '1';
            widget.style.transform = 'translateY(0)';
        } else {
            widget.style.opacity = '0.8';
            widget.style.transform = 'translateY(10px)';
        }
    }
    
    // Initial setup
    const widget = document.querySelector('.contact-widget');
    widget.style.transition = 'all 0.3s ease';
    
    window.addEventListener('scroll', checkScroll);
    checkScroll(); // Check initial state
});

// Utility functions cho Spring Boot integration
window.ContactWidget = {
    // Mở menu programmatically
    openMenu: function() {
        const event = new Event('click');
        document.getElementById('contactBtn').dispatchEvent(event);
    },
    
    // Đóng menu programmatically
    closeMenu: function() {
        const contactMenu = document.getElementById('contactMenu');
        if (contactMenu.classList.contains('show')) {
            const event = new Event('click');
            document.getElementById('contactBtn').dispatchEvent(event);
        }
    },
    
    // Cập nhật thông tin liên hệ
    updateContactInfo: function(config) {
        if (config.zalo) {
            const zaloLink = document.querySelector('.contact-link.zalo');
            if (zaloLink) {
                zaloLink.href = `https://zalo.me/${config.zalo}`;
            }
        }
        
        if (config.facebook) {
            const facebookLink = document.querySelector('.contact-link.facebook');
            if (facebookLink) {
                facebookLink.href = config.facebook;
            }
        }
        
        if (config.phone) {
            const phoneLink = document.querySelector('.contact-link.phone');
            if (phoneLink) {
                phoneLink.href = `tel:${config.phone}`;
                const phoneText = phoneLink.querySelector('.contact-text');
                if (phoneText) {
                    phoneText.textContent = config.phone;
                }
            }
        }
    },
    
    // Ẩn/hiện widget
    toggle: function(show = true) {
        const widget = document.querySelector('.contact-widget');
        if (widget) {
            widget.style.display = show ? 'block' : 'none';
        }
    }
};