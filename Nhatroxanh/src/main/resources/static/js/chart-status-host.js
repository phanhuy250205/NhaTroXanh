// Chart.js for Room Status Management
class RoomStatusChart {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.data = {
            available: 2,
            occupied: 1,
            maintenance: 1
        };
        this.colors = {
            available: '#10b981',
            occupied: '#ef4444',
            maintenance: '#f59e0b'
        };
        this.labels = {
            available: 'Phòng trống',
            occupied: 'Phòng đã thuê',
            maintenance: 'Phòng bảo trì'
        };
        this.init();
    }

    init() {
        this.canvas.width = 400;
        this.canvas.height = 400;
        this.centerX = this.canvas.width / 2;
        this.centerY = this.canvas.height / 2;
        this.radius = 150;
        this.draw();
    }

    calculateAngles() {
        const total = this.data.available + this.data.occupied + this.data.maintenance;
        const angles = {};
        let currentAngle = -Math.PI / 2; // Start from top

        for (const [key, value] of Object.entries(this.data)) {
            const angle = (value / total) * 2 * Math.PI;
            angles[key] = {
                start: currentAngle,
                end: currentAngle + angle,
                value: value,
                percentage: ((value / total) * 100).toFixed(1)
            };
            currentAngle += angle;
        }
        return angles;
    }

    draw() {
        // Clear canvas
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        const angles = this.calculateAngles();
        
        // Draw pie slices
        for (const [key, angleData] of Object.entries(angles)) {
            this.ctx.beginPath();
            this.ctx.moveTo(this.centerX, this.centerY);
            this.ctx.arc(this.centerX, this.centerY, this.radius, angleData.start, angleData.end);
            this.ctx.closePath();
            this.ctx.fillStyle = this.colors[key];
            this.ctx.fill();
            
            // Add stroke
            this.ctx.strokeStyle = '#ffffff';
            this.ctx.lineWidth = 3;
            this.ctx.stroke();
        }

        // Draw center circle for donut effect
        this.ctx.beginPath();
        this.ctx.arc(this.centerX, this.centerY, 60, 0, 2 * Math.PI);
        this.ctx.fillStyle = '#ffffff';
        this.ctx.fill();
        this.ctx.strokeStyle = '#e5e7eb';
        this.ctx.lineWidth = 2;
        this.ctx.stroke();

        // Draw center text
        this.ctx.fillStyle = '#1f2937';
        this.ctx.font = 'bold 24px Inter, sans-serif';
        this.ctx.textAlign = 'center';
        this.ctx.textBaseline = 'middle';
        const total = this.data.available + this.data.occupied + this.data.maintenance;
        this.ctx.fillText(total, this.centerX, this.centerY - 10);
        
        this.ctx.font = '14px Inter, sans-serif';
        this.ctx.fillStyle = '#6b7280';
        this.ctx.fillText('Tổng phòng', this.centerX, this.centerY + 15);

        // Draw percentage labels on slices
        for (const [key, angleData] of Object.entries(angles)) {
            if (angleData.value > 0) {
                const midAngle = (angleData.start + angleData.end) / 2;
                const labelX = this.centerX + Math.cos(midAngle) * (this.radius * 0.7);
                const labelY = this.centerY + Math.sin(midAngle) * (this.radius * 0.7);
                
                this.ctx.fillStyle = '#ffffff';
                this.ctx.font = 'bold 16px Inter, sans-serif';
                this.ctx.textAlign = 'center';
                this.ctx.textBaseline = 'middle';
                this.ctx.fillText(`${angleData.percentage}%`, labelX, labelY);
            }
        }
    }

    updateData(newData) {
        this.data = { ...this.data, ...newData };
        this.draw();
        this.updateStatCards();
    }

    updateStatCards() {
        // Update stat cards
        document.getElementById('totalRoomsCount').textContent = 
            this.data.available + this.data.occupied + this.data.maintenance;
        document.getElementById('availableRoomsCount').textContent = this.data.available;
        document.getElementById('occupiedRoomsCount').textContent = this.data.occupied;
        document.getElementById('maintenanceRoomsCount').textContent = this.data.maintenance;
    }

    // Animation method
    animateChart() {
        let progress = 0;
        const animate = () => {
            progress += 0.02;
            if (progress <= 1) {
                this.drawAnimated(progress);
                requestAnimationFrame(animate);
            } else {
                this.draw();
            }
        };
        animate();
    }

    drawAnimated(progress) {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        
        const angles = this.calculateAngles();
        
        for (const [key, angleData] of Object.entries(angles)) {
            const animatedEnd = angleData.start + (angleData.end - angleData.start) * progress;
            
            this.ctx.beginPath();
            this.ctx.moveTo(this.centerX, this.centerY);
            this.ctx.arc(this.centerX, this.centerY, this.radius, angleData.start, animatedEnd);
            this.ctx.closePath();
            this.ctx.fillStyle = this.colors[key];
            this.ctx.fill();
            
            this.ctx.strokeStyle = '#ffffff';
            this.ctx.lineWidth = 3;
            this.ctx.stroke();
        }

        // Draw center circle
        this.ctx.beginPath();
        this.ctx.arc(this.centerX, this.centerY, 60, 0, 2 * Math.PI);
        this.ctx.fillStyle = '#ffffff';
        this.ctx.fill();
        this.ctx.strokeStyle = '#e5e7eb';
        this.ctx.lineWidth = 2;
        this.ctx.stroke();
    }
}

// Initialize chart when DOM is loaded
document.addEventListener('DOMContentLoaded', function() {
    let roomChart = null;

    // Initialize chart when status management tab is shown
    const statusTab = document.getElementById('status-management-tab-host');
    const chartCanvas = document.getElementById('roomStatusChart');
    
    if (statusTab && chartCanvas) {
        statusTab.addEventListener('shown.bs.tab', function() {
            if (!roomChart) {
                roomChart = new RoomStatusChart('roomStatusChart');
                roomChart.animateChart();
            }
        });

        // If the tab is already active on page load
        if (statusTab.classList.contains('active')) {
            roomChart = new RoomStatusChart('roomStatusChart');
            roomChart.animateChart();
        }
    }

    // Update status functionality
    const updateStatusBtn = document.getElementById('updateStatusBtn');
    const quickRoomSelect = document.getElementById('quickRoomSelect');
    const quickStatusSelect = document.getElementById('quickStatusSelect');

    if (updateStatusBtn) {
        updateStatusBtn.addEventListener('click', function() {
            const selectedRoom = quickRoomSelect.value;
            const newStatus = quickStatusSelect.value;

            if (!selectedRoom || !newStatus) {
                alert('Vui lòng chọn phòng và trạng thái mới!');
                return;
            }

            // Update room status in table
            updateRoomStatusInTable(selectedRoom, newStatus);
            
            // Update chart data
            if (roomChart) {
                const updatedData = calculateRoomStatusData();
                roomChart.updateData(updatedData);
            }

            // Reset form
            quickRoomSelect.value = '';
            quickStatusSelect.value = '';

            // Show success message
            showNotification('Cập nhật trạng thái phòng thành công!', 'success');
        });
    }

    // Helper functions
    function updateRoomStatusInTable(roomName, newStatus) {
        const tableRows = document.querySelectorAll('#roomTableBodyHost .table-row-host');
        
        tableRows.forEach(row => {
            const roomNameCell = row.querySelector('.room-name-host');
            if (roomNameCell && roomNameCell.textContent.includes(roomName)) {
                const statusCell = row.querySelector('.status-badge-host');
                if (statusCell) {
                    // Remove old status classes
                    statusCell.classList.remove('status-available-host', 'status-occupied-host', 'status-maintenance-host');
                    
                    // Add new status class and text
                    switch (newStatus) {
                        case 'available':
                            statusCell.classList.add('status-available-host');
                            statusCell.textContent = 'Trống';
                            break;
                        case 'occupied':
                            statusCell.classList.add('status-occupied-host');
                            statusCell.textContent = 'Đã thuê';
                            break;
                        case 'maintenance':
                            statusCell.classList.add('status-maintenance-host');
                            statusCell.textContent = 'Bảo trì';
                            break;
                    }
                }
            }
        });
    }

    function calculateRoomStatusData() {
        const statusBadges = document.querySelectorAll('#roomTableBodyHost .status-badge-host');
        const data = { available: 0, occupied: 0, maintenance: 0 };

        statusBadges.forEach(badge => {
            if (badge.classList.contains('status-available-host')) {
                data.available++;
            } else if (badge.classList.contains('status-occupied-host')) {
                data.occupied++;
            } else if (badge.classList.contains('status-maintenance-host')) {
                data.maintenance++;
            }
        });

        return data;
    }

    function showNotification(message, type = 'success') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `alert alert-${type === 'success' ? 'success' : 'danger'} alert-dismissible fade show position-fixed`;
        notification.style.cssText = `
            top: 20px;
            right: 20px;
            z-index: 9999;
            min-width: 300px;
            animation: slideInRight 0.3s ease;
        `;
        
        notification.innerHTML = `
            <div class="d-flex align-items-center">
                <i class="fas fa-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
                <span>${message}</span>
                <button type="button" class="btn-close ms-auto" data-bs-dismiss="alert"></button>
            </div>
        `;

        document.body.appendChild(notification);

        // Auto remove after 3 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.style.animation = 'slideOutRight 0.3s ease';
                setTimeout(() => {
                    if (notification.parentNode) {
                        notification.parentNode.removeChild(notification);
                    }
                }, 300);
            }
        }, 3000);
    }

    // Stat card click handlers
    document.querySelectorAll('.stat-action-modern').forEach(action => {
        action.addEventListener('click', function() {
            // Switch to room list tab
            const roomListTab = document.getElementById('room-list-tab-host');
            if (roomListTab) {
                roomListTab.click();
            }
        });
    });

    // Real-time data simulation (optional)
    function simulateRealTimeUpdates() {
        setInterval(() => {
            if (roomChart && Math.random() > 0.95) { // 5% chance every interval
                const currentData = calculateRoomStatusData();
                roomChart.updateData(currentData);
            }
        }, 5000); // Check every 5 seconds
    }

    // Uncomment to enable real-time simulation
    // simulateRealTimeUpdates();
});

// Export for global access if needed
window.RoomStatusChart = RoomStatusChart;