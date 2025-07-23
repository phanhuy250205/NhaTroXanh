// Favorite functionality handler
class FavoriteHandler {
    constructor() {
        this.init();
    }

    init() {
        this.bindEvents();
        this.loadFavoriteStatuses();
    }

    bindEvents() {
        // Bind click events to favorite buttons
        document.addEventListener('click', (e) => {
            if (e.target.closest('.btn-favorite') || e.target.closest('.btn-favorite-large' )) {
                e.preventDefault();
                e.stopPropagation();
                this.handleFavoriteClick(e.target.closest('.btn-favorite, .btn-favorite-large'));
            }
        });

        // Bind events for dynamically loaded content
        document.addEventListener('DOMContentLoaded', () => {
            this.loadFavoriteStatuses();
        });
    }

    async handleFavoriteClick(button) {
        try {
            // Get post ID from button or parent element
            const postId = this.getPostIdFromButton(button);
            
            if (!postId) {
                this.showNotification('Không tìm thấy ID bài đăng', 'error');
                return;
            }

            // Show loading state
            this.setButtonLoading(button, true);

            // Call API to toggle favorite
            const response = await fetch(`/api/favorites/toggle/${postId}`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            const data = await response.json();

            if (data.success) {
                // Update button state
                this.updateButtonState(button, data.isFavorited);
                this.showNotification(data.message, 'success');
                
                // Update favorite count if element exists
                this.updateFavoriteCount(postId, data.favoriteCount);
            } else if (data.requireLogin) {
                // Show login modal
                this.showLoginModal();
            } else {
                this.showNotification(data.message || 'Có lỗi xảy ra', 'error');
            }

        } catch (error) {
            console.error('Error toggling favorite:', error);
            this.showNotification('Có lỗi xảy ra. Vui lòng thử lại sau', 'error');
        } finally {
            this.setButtonLoading(button, false);
        }
    }

    getPostIdFromButton(button) {
        // Try to get post ID from various sources
        let postId = button.getAttribute('data-post-id');
        
        if (!postId) {
            // Try to get from parent card
            const card = button.closest('.post-card, .property-card, .card');
            if (card) {
                postId = card.getAttribute('data-post-id');
            }
        }

        if (!postId) {
            // Try to get from URL in nearby link
            const link = button.closest('.post-card, .property-card, .card')?.querySelector('a[href*="/chi-tiet/"]');
            if (link) {
                const href = link.getAttribute('href');
                const match = href.match(/\/chi-tiet\/(\d+)/);
                if (match) {
                    postId = match[1];
                }
            }
        }

        if (!postId) {
            // Try to get from current page URL if on detail page
            const currentUrl = window.location.pathname;
            const match = currentUrl.match(/\/chi-tiet\/(\d+)/);
            if (match) {
                postId = match[1];
            }
        }

        return postId;
    }

    updateButtonState(button, isFavorited) {
        const icon = button.querySelector('i');
        if (icon) {
            if (isFavorited) {
                icon.classList.remove('far');
                icon.classList.add('fas');
                button.classList.add('favorited');
            } else {
                icon.classList.remove('fas');
                icon.classList.add('far');
                button.classList.remove('favorited');
            }
        }
    }

    setButtonLoading(button, isLoading) {
        // No loading effects as requested
        return;
    }

    async loadFavoriteStatuses() {
        try {
            // Get all post IDs on current page
            const postIds = this.getAllPostIds();
            
            if (postIds.length === 0) return;

            // Call API to get favorite statuses
            const response = await fetch('/api/favorites/status/batch', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(postIds)
            });

            const data = await response.json();
            
            if (data.favoriteStatus) {
                // Update all buttons based on status
                this.updateAllButtonStates(data.favoriteStatus);
            }

        } catch (error) {
            console.error('Error loading favorite statuses:', error);
        }
    }

    getAllPostIds() {
        const postIds = new Set();

        // Get from data attributes
        document.querySelectorAll('[data-post-id]').forEach(element => {
            const postId = element.getAttribute('data-post-id');
            if (postId && postId !== 'null') {
                postIds.add(parseInt(postId));
            }
        });

        // Get from links
        document.querySelectorAll('a[href*="/chi-tiet/"]').forEach(link => {
            const href = link.getAttribute('href');
            const match = href.match(/\/chi-tiet\/(\d+)/);
            if (match) {
                postIds.add(parseInt(match[1]));
            }
        });

        // Get from current page URL if on detail page
        const currentUrl = window.location.pathname;
        const match = currentUrl.match(/\/chi-tiet\/(\d+)/);
        if (match) {
            postIds.add(parseInt(match[1]));
        }

        return Array.from(postIds);
    }

    updateAllButtonStates(favoriteStatus) {
        document.querySelectorAll('.btn-favorite, .btn-favorite-large').forEach(button => {
            const postId = this.getPostIdFromButton(button);
            if (postId && favoriteStatus.hasOwnProperty(postId)) {
                this.updateButtonState(button, favoriteStatus[postId]);
            }
        });
    }

    updateFavoriteCount(postId, count) {
        // Update favorite count display if exists
        const countElement = document.querySelector(`[data-favorite-count="${postId}"]`);
        if (countElement) {
            countElement.textContent = count;
        }
    }

    showLoginModal() {
        // Try to show existing login modal
        const loginModal = document.getElementById('loginModal');
        if (loginModal) {
            const modal = new bootstrap.Modal(loginModal);
            modal.show();
        } else {
            // Fallback: redirect to login page
            this.showNotification('Vui lòng đăng nhập để sử dụng tính năng này', 'warning');
            // setTimeout(() => {
            //     window.location.href = '/';
            // }, 2000);
        }
    }

    showNotification(message, type = 'info') {
        // Try to use existing notification system
        if (typeof toastr !== 'undefined') {
            toastr[type](message);
            return;
        }

        if (typeof Swal !== 'undefined') {
            Swal.fire({
                text: message,
                icon: type === 'error' ? 'error' : type === 'success' ? 'success' : 'info',
                timer: 3000,
                showConfirmButton: false,
                toast: true,
                position: 'top-end'
            });
            return;
        }

        // Fallback: simple alert
        alert(message);
    }
}

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
    new FavoriteHandler();
});

// Export for manual initialization if needed
window.FavoriteHandler = FavoriteHandler;
