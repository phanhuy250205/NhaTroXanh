document.addEventListener("DOMContentLoaded", () => {
  // Active tab
  document.querySelectorAll('.nav-sidebar').forEach(item => {
    item.addEventListener('click', () => {
      document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
      item.classList.add('active');
    });
  });

  // Dropdown toggle (giữ nguyên)
  function toggleDropdown(id) {
    const el = document.getElementById(id);
    const isOpen = !el.classList.contains('d-none');
    document.querySelectorAll('.dropdown-modal').forEach(d => d.classList.add('d-none'));
    if (!isOpen) el.classList.remove('d-none');
  }

  document.addEventListener('click', function (e) {
    if (!e.target.closest('.filter-btn-wrapper')) {
      document.querySelectorAll('.dropdown-modal').forEach(el => el.classList.add('d-none'));
    }
  });

  const overlay = document.getElementById('overlay');
  const accountSidebar = document.getElementById('sidebarAccount');
  const closeAccountBtn = document.getElementById('closeAccountSidebar');

  const accountBtn = document.querySelectorAll('.nav-sidebar i.fa-user-circle');

  function showAccountSidebar(btn) {
    if (typeof hideRentalSidebar === 'function') hideRentalSidebar(); // Không lỗi nếu chưa có
    if (!accountSidebar) return;
    accountSidebar.classList.remove('d-none');
    setTimeout(() => accountSidebar.classList.add('show'), 10);
    overlay.classList.remove('d-none');
    document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
    btn?.parentElement?.classList.add('active');
  }

  function hideAccountSidebar() {
    if (!accountSidebar) return;
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

  // 🔹 Rental sidebar (optional – không lỗi nếu không tồn tại)
  const rentalBtns = document.querySelectorAll('.nav-sidebar i.fa-file-signature');
  const rentalSidebar = document.getElementById('sidebarRental');
  const closeRentalBtn = document.getElementById('closeRentalSidebar');

  function showRentalSidebar(btn) {
    hideAccountSidebar();
    if (!rentalSidebar) return;
    rentalSidebar.classList.remove('d-none');
    setTimeout(() => rentalSidebar.classList.add('show'), 10);
    overlay.classList.remove('d-none');
    document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
    btn?.parentElement?.classList.add('active');
  }

  function hideRentalSidebar() {
    if (!rentalSidebar) return;
    rentalSidebar.classList.remove('show');
    setTimeout(() => {
      rentalSidebar.classList.add('d-none');
      hideOverlayIfNoneOpen();
    }, 300);
  }

  if (rentalBtns.length && rentalSidebar) {
    rentalBtns.forEach(btn => {
      btn.parentElement.addEventListener('click', () => showRentalSidebar(btn));
    });
  }

  if (closeRentalBtn && rentalSidebar) {
    closeRentalBtn.addEventListener('click', hideRentalSidebar);
  }

  // Overlay click: đóng cả hai
  overlay.addEventListener('click', () => {
    hideAccountSidebar();
    hideRentalSidebar?.(); // optional chaining
  });

  function hideOverlayIfNoneOpen() {
    const isAccountOpen = accountSidebar && !accountSidebar.classList.contains('d-none');
    const isRentalOpen = rentalSidebar && !rentalSidebar.classList.contains('d-none');
    if (!isAccountOpen && !isRentalOpen) {
      overlay.classList.add('d-none');
    }
  }
});
