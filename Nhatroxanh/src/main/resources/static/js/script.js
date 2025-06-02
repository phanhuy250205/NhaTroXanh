 document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('.nav-sidebar').forEach(item => {
      item.addEventListener('click', () => {
        document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
        item.classList.add('active');
      });
    });
  });

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