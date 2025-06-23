document.addEventListener("DOMContentLoaded", () => {
  const overlay = document.getElementById('overlay');
  const accountSidebar = document.getElementById('sidebarAccount');
  const closeAccountBtn = document.getElementById('closeAccountSidebar');
  const rentalSidebar = document.getElementById('sidebarRental');
  const closeRentalBtn = document.getElementById('closeRentalSidebar');
  const navTro = document.getElementById('nav-tro');
  const submenu = navTro?.querySelector('.nav-submenu-host');
  const troSidebar = document.getElementById('sidebarTro');
  const closeTroBtn = document.getElementById('closeTroSidebar');

  function hideSubmenu() {
    submenu?.classList.remove('show');
    setTimeout(() => submenu?.classList.add('d-none'), 300);
  }

  function hideOverlayIfNoneOpen() {
    const isAccountOpen = accountSidebar?.classList.contains('show');
    const isRentalOpen = rentalSidebar?.classList.contains('show');
    const isSubmenuOpen = submenu?.classList.contains('show');
    if (!isAccountOpen && !isRentalOpen && !isSubmenuOpen) {
      overlay.classList.add('d-none');
    }
  }

  function hideAll() {
    hideAccountSidebar();
    hideRentalSidebar();
    hideTroSidebar();
    hideSubmenu();
  }

  function showOverlay() {
    overlay.classList.remove('d-none');
  }

  function setActiveSidebar(el) {
    document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
    el?.classList.add('active');

    // Sync sidebar and bottom-nav via data-id
    const dataId = el?.getAttribute('data-id');
    document.querySelectorAll(`.nav-sidebar[data-id="${dataId}"]`).forEach(item => item.classList.add('active'));

    localStorage.setItem('activeSidebarId', dataId || '');
  }

  function setActiveSubmenu(href) {
    document.querySelectorAll('.nav-sublink-host').forEach(l => l.classList.remove('active'));
    const link = document.querySelector(`.nav-sublink-host[href="${href}"]`);
    link?.classList.add('active');
    localStorage.setItem('activeSubmenuHref', href);
  }

  function showAccountSidebar(el) {
    hideSubmenu();
    hideRentalSidebar();
    hideTroSidebar(); 
    if (!accountSidebar) return;
    accountSidebar.classList.remove('d-none');
    setTimeout(() => accountSidebar.classList.add('show'), 10);
    showOverlay();
    setActiveSidebar(el);
  }

  function showTroSidebar(el) {
    hideAccountSidebar();
    hideRentalSidebar();
    hideSubmenu();
    if (!troSidebar) return;
    troSidebar.classList.remove('d-none');
    setTimeout(() => troSidebar.classList.add('show'), 10);
    showOverlay();
    setActiveSidebar(el);
  }

  function hideTroSidebar() {
    troSidebar?.classList.remove('show');
    setTimeout(() => {
      troSidebar?.classList.add('d-none');
      hideOverlayIfNoneOpen();
    }, 300);
  }

  function hideAccountSidebar() {
    accountSidebar?.classList.remove('show');
    setTimeout(() => {
      accountSidebar?.classList.add('d-none');
      hideOverlayIfNoneOpen();
    }, 300);
  }

  function showRentalSidebar(el) {
    hideSubmenu();
    hideTroSidebar(); 
    hideAccountSidebar();
    if (!rentalSidebar) return;
    rentalSidebar.classList.remove('d-none');
    setTimeout(() => rentalSidebar.classList.add('show'), 10);
    showOverlay();
    setActiveSidebar(el);
  }

  function hideRentalSidebar() {
    rentalSidebar?.classList.remove('show');
    setTimeout(() => {
      rentalSidebar?.classList.add('d-none');
      hideOverlayIfNoneOpen();
    }, 300);
  }

  function showTroSubmenu(el) {
    hideAccountSidebar();
    hideRentalSidebar();
    if (!submenu) return;
    submenu.classList.remove('d-none');
    setTimeout(() => submenu.classList.add('show'), 10);
    showOverlay();
    setActiveSidebar(el);
  }

  function initializeActiveState() {
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav-sidebar').forEach(item => item.classList.remove('active'));
    document.querySelectorAll('.nav-sublink-host').forEach(link => link.classList.remove('active'));

    // Cập nhật navMap với các đường dẫn từ controller
    const navMap = {
      '/chu-tro/tong-quan': { sidebar: '.nav-sidebar:has(.fa-chart-pie)' },
      '/chu-tro/bai-dang': { sidebar: '.nav-sidebar:has(.fa-file-alt)' },
      '/chu-tro/dang-tin': { sidebar: '.nav-sidebar:has(.fa-plus-circle)' },
      '/chu-tro/khach-thue': { sidebar: '.nav-sidebar:has(.fa-address-card)' },
      '/chu-tro/quan-ly-tro': { sidebar: '#nav-tro' },
      '/chu-tro/thong-tin-tro': { sidebar: '#nav-tro' },
      '/chu-tro/DS-hop-dong-host': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/lich-su-thue': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/thanh-toan': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/gia-hang-tra-phong': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/danh-gia': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/profile-host': { sidebar: '.nav-sidebar:has(.fa-user-circle)' },
      '/chu-tro/hop-dong': { sidebar: '.nav-sidebar:has(.fa-chart-pie)' },
      '/chu-tro/chi-tiet-bai-dang': { sidebar: '.nav-sidebar:has(.fa-file-alt)' },
      '/chu-tro/sua-bai-dang': { sidebar: '.nav-sidebar:has(.fa-file-alt)' },

       '/admin/employee-management': { sidebar: '.nav-sidebar:has(.fa-users)' },
      '/admin/statistics-reports': { sidebar: '.nav-sidebar:has(.fa-chart-bar)' },
      '/admin/profile-host': { sidebar: '.nav-sidebar:has(.fa-user-circle)' },
      '/nhan-vien/bai-dang': { sidebar: '.nav-sidebar:has(.fa-file-alt)' },
      '/nhan-vien/khuyen-mai': { sidebar: '.nav-sidebar:has(.fa-gift)' },
      '/nhan-vien/khieu-nai': { sidebar: '.nav-sidebar:has(.fa-comment-dots)' },
      '/nhan-vien/chu-tro': { sidebar: '.nav-sidebar:has(.fa-user-tie)' },
      '/nhan-vien/khach-thue': { sidebar: '.nav-sidebar:has(.fa-user)' },
      '/nhan-vien/thanh-toan': { sidebar: '.nav-sidebar:has(.fa-credit-card)' },
      '/nhan-vien/thong-tin-tro': { sidebar: '.nav-sidebar:has(.fa-house-user)' },
      '/nhan-vien/profile': { sidebar: '.nav-sidebar:has(.fa-user-circle)' }
    };

    const matchedNav = Object.entries(navMap).find(([path]) => currentPath.includes(path));
    if (matchedNav) {
      const { sidebar, submenu: submenuHref } = matchedNav[1];
      const sidebarEl = document.querySelector(sidebar);
      if (sidebarEl) {
        setActiveSidebar(sidebarEl);
      }
      if (submenuHref) {
        setActiveSubmenu(submenuHref);
      }
    }

    // Restore from localStorage
    const savedSidebarId = localStorage.getItem('activeSidebarId');
    if (savedSidebarId) {
      document.querySelectorAll(`.nav-sidebar[data-id="${savedSidebarId}"]`).forEach(el => el.classList.add('active'));
    }
    const savedSubmenuHref = localStorage.getItem('activeSubmenuHref');
    if (savedSubmenuHref) {
      setActiveSubmenu(savedSubmenuHref);
    }
  }

  // Xử lý click cho nav-sidebar
  document.querySelectorAll('.nav-sidebar').forEach(item => {
    item.addEventListener('click', e => {
      e.stopPropagation();
      const icon = item.querySelector('i');
      const isAccount = icon?.classList.contains('fa-user-circle');
      const isRental = icon?.classList.contains('fa-file-signature');
      const isTro = item.id === 'nav-tro';
      const hasSubmenu = item.classList.contains('has-submenu');

      // Nếu có data-link và không có submenu, điều hướng trực tiếp
      const url = item.getAttribute('data-link');
      if (url && !hasSubmenu) {
        window.location.href = url;
        return;
      }

      // Xử lý các sidebar đặc biệt
      if (isAccount) {
        showAccountSidebar(item);
      } else if (isRental) {
        showRentalSidebar(item);
      } else if (isTro) {
        if (window.innerWidth <= 768) {
          showTroSidebar(item); // mobile
        } else {
          showTroSubmenu(item); // desktop
        }
      }
    });
  });

  // Xử lý click cho submenu links
  document.querySelectorAll('.nav-sublink-host').forEach(link => {
    link.addEventListener('click', e => {
      e.stopPropagation();
      setActiveSubmenu(link.getAttribute('href'));
      setActiveSidebar(navTro);
      if (window.innerWidth <= 768) {
        hideSubmenu();
        hideOverlayIfNoneOpen();
      }
    });
  });

  // Đóng submenu khi click bên ngoài
  document.addEventListener('click', e => {
    if (!e.target.closest('#nav-tro') && !e.target.closest('.nav-submenu-host')) {
      hideSubmenu();
      hideOverlayIfNoneOpen();
    }
  });

  // Event listeners cho các nút đóng
  overlay?.addEventListener('click', hideAll);
  closeAccountBtn?.addEventListener('click', hideAccountSidebar);
  closeRentalBtn?.addEventListener('click', hideRentalSidebar);
  closeTroBtn?.addEventListener('click', hideTroSidebar);
  document.querySelector('#closeSubmenu')?.addEventListener('click', hideSubmenu);

  // Khởi tạo trạng thái active
  initializeActiveState();
});