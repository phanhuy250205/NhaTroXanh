document.addEventListener("DOMContentLoaded", () => {

  // Xử lý active cho sidebar chính
  document.querySelectorAll('.nav-sidebar').forEach(item => {
    item.addEventListener('click', () => {
      document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
      item.classList.add('active');
    });
  });

  // Xử lý dropdown custom
  function toggleDropdown(id) {
    const el = document.getElementById(id);
    const isOpen = !el.classList.contains('d-none');
    document.querySelectorAll('.dropdown-modal').forEach(d => d.classList.add('d-none'));
    if (!isOpen) el.classList.remove('d-none');
  }

  // Ẩn dropdown khi click ra ngoài
  document.addEventListener('click', function (e) {
    if (!e.target.closest('.filter-btn-wrapper')) {
      document.querySelectorAll('.dropdown-modal').forEach(el => el.classList.add('d-none'));
    }
  });

  const overlay = document.getElementById('overlay');

  // Sidebar Tài khoản
  const accountBtn = document.querySelectorAll('.nav-sidebar i.fa-user-circle, .nav-item i.fa-user-circle');
  const accountSidebar = document.getElementById('sidebarAccount');
  const closeAccountBtn = document.getElementById('closeAccountSidebar');

  function showAccountSidebar(btn) {
    hideRentalSidebar(); // 👈 Hide rental if open
    accountSidebar.classList.remove('d-none');
    setTimeout(() => accountSidebar.classList.add('show'), 10);
    overlay.classList.remove('d-none');

    document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
    btn.parentElement.classList.add('active');
  }

  function hideAccountSidebar() {
    accountSidebar.classList.remove('show');
    setTimeout(() => {
      accountSidebar.classList.add('d-none');
      hideOverlayIfNoneOpen();
    }, 300);
  }

  accountBtn.forEach(btn => {
    btn.parentElement.addEventListener('click', () => showAccountSidebar(btn));
  });

  if (closeAccountBtn) closeAccountBtn.addEventListener('click', hideAccountSidebar);

  // Sidebar Thuê trả
  const rentalBtn = document.querySelector('.nav-sidebar i.fa-file-signature')?.parentElement;
  const rentalSidebar = document.getElementById('sidebarRental');
  const closeRentalBtn = document.getElementById('closeRentalSidebar');

  function showRentalSidebar() {
    hideAccountSidebar(); // 👈 Hide account if open
    rentalSidebar.classList.remove('d-none');
    setTimeout(() => rentalSidebar.classList.add('show'), 10);
    overlay.classList.remove('d-none');

    document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
    rentalBtn.classList.add('active');
  }

  function hideRentalSidebar() {
    rentalSidebar.classList.remove('show');
    setTimeout(() => {
      rentalSidebar.classList.add('d-none');
      hideOverlayIfNoneOpen();
    }, 300);
  }

  if (rentalBtn) rentalBtn.addEventListener('click', showRentalSidebar);
  if (closeRentalBtn) closeRentalBtn.addEventListener('click', hideRentalSidebar);

  // Overlay click: đóng cả 2 nếu đang mở
  overlay.addEventListener('click', () => {
    hideAccountSidebar();
    hideRentalSidebar();
  });

  // Helper: Ẩn overlay nếu không có sidebar nào đang mở
  function hideOverlayIfNoneOpen() {
    const isAccountOpen = !accountSidebar.classList.contains('d-none');
    const isRentalOpen = !rentalSidebar.classList.contains('d-none');
    if (!isAccountOpen && !isRentalOpen) {
      overlay.classList.add('d-none');
    }
  }
});
