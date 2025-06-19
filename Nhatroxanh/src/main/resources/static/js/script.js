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

    const navMap = {
      '/chu-tro/overview': { sidebar: '.nav-sidebar:has(.fa-chart-pie)' },
      '/chu-tro/posts': { sidebar: '.nav-sidebar:has(.fa-file-alt)' },
      '/chu-tro/post-create': { sidebar: '.nav-sidebar:has(.fa-plus-circle)' },
      '/chu-tro/tenants': { sidebar: '.nav-sidebar:has(.fa-address-card)' },
      '/chu-tro/room-management': { sidebar: '#nav-tro', submenu: '#room-management' },
      '/chu-tro/info-management': { sidebar: '#nav-tro', submenu: '#info-management' },
      '/chu-tro/DS-hop-dong-host': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/lich-su-thue': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/thanh-toan': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/gia-hang-tra-phong': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/danh-gia': { sidebar: '.nav-sidebar:has(.fa-file-signature)' },
      '/chu-tro/profile-host': { sidebar: '.nav-sidebar:has(.fa-user-circle)' },
      '/admin/employee-management': { sidebar: '.nav-sidebar:has(.fa-users)' },
      '/admin/statistics-reports': { sidebar: '.nav-sidebar:has(.fa-chart-bar)' },
      '/admin/profile-host': { sidebar: '.nav-sidebar:has(.fa-user-circle)' },
      '/nhan-vien/posts': { sidebar: '.nav-sidebar:has(.fa-file-alt)' },
      '/nhan-vien/promotions': { sidebar: '.nav-sidebar:has(.fa-gift)' },
      '/nhan-vien/complaints': { sidebar: '.nav-sidebar:has(.fa-comment-dots)' },
      '/nhan-vien/hosts': { sidebar: '.nav-sidebar:has(.fa-user-tie)' },
      '/nhan-vien/tenants': { sidebar: '.nav-sidebar:has(.fa-user)' },
      '/nhan-vien/payments': { sidebar: '.nav-sidebar:has(.fa-credit-card)' },
      '/nhan-vien/rental-info': { sidebar: '.nav-sidebar:has(.fa-house-user)' },
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

  document.querySelectorAll('.nav-sidebar').forEach(item => {
    item.addEventListener('click', e => {
      e.stopPropagation();
      const icon = item.querySelector('i');
      const isAccount = icon?.classList.contains('fa-user-circle');
      const isRental = icon?.classList.contains('fa-file-signature');
      const isTro = item.id === 'nav-tro';

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

  document.addEventListener('click', e => {
    if (!e.target.closest('#nav-tro') && !e.target.closest('.nav-submenu-host')) {
      hideSubmenu();
      hideOverlayIfNoneOpen();
    }
  });
  // Điều hướng bằng data-link nếu có
  document.querySelectorAll('.nav-sidebar').forEach(item => {
    item.addEventListener('click', () => {
      const url = item.getAttribute('data-link');
      if (url && !item.classList.contains('has-submenu')) {
        window.location.href = url;
      }
    });
  });

  overlay?.addEventListener('click', hideAll);
  closeAccountBtn?.addEventListener('click', hideAccountSidebar);
  closeRentalBtn?.addEventListener('click', hideRentalSidebar);
  closeTroBtn?.addEventListener('click', hideTroSidebar); // thêm dòng này
  document.querySelector('#closeSubmenu')?.addEventListener('click', hideSubmenu);


  initializeActiveState();
});
