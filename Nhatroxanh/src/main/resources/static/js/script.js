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

        // Hàm đặt lại bộ lọc tỉnh
    function resetProvinceFilter() {
        console.log('Resetting province filter');
        const checkboxes = document.querySelectorAll('.province-checkbox');
        checkboxes.forEach(checkbox => checkbox.checked = false);

        const dropdown = document.getElementById('dropdownDiaDiem');
        if (dropdown) {
            dropdown.classList.add('d-none');
        } else {
            console.error('Dropdown element not found: dropdownDiaDiem');
        }

        const url = new URL(window.location.href);
        url.searchParams.delete('province');
        console.log('Redirecting to:', url.toString());
        window.location.href = url.toString();
    }

    // Hàm áp dụng bộ lọc tỉnh
    function applyProvinceFilter() {
        console.log('Applying province filter');
        const selectedProvince = document.querySelector('input[name="province"]:checked');
        const provinceId = selectedProvince ? selectedProvince.value : null;
        console.log('Selected province ID:', provinceId);

        const dropdown = document.getElementById('dropdownDiaDiem');
        if (dropdown) {
            dropdown.classList.add('d-none');
        } else {
            console.error('Dropdown element not found: dropdownDiaDiem');
        }

        const url = new URL(window.location.href);
        const currentProvince = url.searchParams.get('province');

        // Chỉ chuyển hướng nếu provinceId khác với tham số hiện tại
        if (provinceId && provinceId !== currentProvince) {
            url.searchParams.set('province', provinceId);
            console.log('Redirecting to:', url.toString());
            window.location.href = url.toString();
        } else if (!provinceId && currentProvince) {
            url.searchParams.delete('province');
            console.log('Redirecting to:', url.toString());
            window.location.href = url.toString();
        } else {
            console.log('No change in province filter, no redirect needed');
        }
    }

    // Gắn sự kiện cho nút "Tìm ngay"
    const applyButton = document.getElementById('apply-province-filter');
    if (applyButton) {
        applyButton.addEventListener('click', applyProvinceFilter);
    } else {
        console.error('Apply button not found: apply-province-filter');
    }

    // Gắn sự kiện cho nút "Đặt lại"
    const resetButton = document.querySelector('.btn-outline-secondary.btn-sm');
    if (resetButton) {
        resetButton.addEventListener('click', resetProvinceFilter);
    } else {
        console.error('Reset button not found');
    }

    // Gắn sự kiện cho nút toggle dropdown
    const toggleButton = document.querySelector('.filter-btn');
    if (toggleButton) {
        toggleButton.addEventListener('click', () => toggleDropdown('dropdownDiaDiem'));
    } else {
        console.error('Toggle button not found: .filter-btn');
    }

    // Gắn sự kiện cho nút đóng dropdown
    const closeButton = document.querySelector('#dropdownDiaDiem .btn.btn-sm');
    if (closeButton) {
        closeButton.addEventListener('click', () => {
            const dropdown = document.getElementById('dropdownDiaDiem');
            if (dropdown) {
                dropdown.classList.add('d-none');
                console.log('Dropdown closed');
            }
        });
    } else {
        console.error('Close button not found: #dropdownDiaDiem .btn.btn-sm');
    }

    // Hàm đặt lại bộ lọc mức giá
    function resetPriceFilter() {
        console.log('Resetting price filter');
        document.querySelectorAll('input[name="price"]').forEach(radio => radio.checked = false);
        document.getElementById('minPrice').value = '';
        document.getElementById('maxPrice').value = '';
        document.getElementById('priceRangeSlider').value = 0;

        const dropdown = document.getElementById('dropdownMucGia');
        if (dropdown) {
            dropdown.classList.add('d-none');
        }

        const url = new URL(window.location.href);
        url.searchParams.delete('priceRange');
        console.log('Redirecting to:', url.toString());
        window.location.href = url.toString();
    }

    // Hàm áp dụng bộ lọc mức giá
    // Hàm áp dụng bộ lọc mức giá
    function applyPriceFilter() {
        console.log('Applying price filter');
        const selectedPrice = document.querySelector('input[name="price"]:checked');
        const priceRange = selectedPrice ? selectedPrice.value : null;
        const minPrice = document.getElementById('minPrice').value ? parseFloat(document.getElementById('minPrice').value) * 1_000_000 : null;
        const maxPrice = document.getElementById('maxPrice').value ? parseFloat(document.getElementById('maxPrice').value) * 1_000_000 : null;

        const dropdown = document.getElementById('dropdownMucGia');
        if (dropdown) {
            dropdown.classList.add('d-none');
        }

        const url = new URL(window.location.href);
        const currentPriceRange = url.searchParams.get('priceRange');

        let newPriceRange = priceRange;
        if (minPrice || maxPrice) {
            newPriceRange = `custom_${minPrice || 0}_${maxPrice || ''}`;
        }

        if (newPriceRange && newPriceRange !== currentPriceRange) {
            url.searchParams.set('priceRange', newPriceRange);
            console.log('Redirecting to:', url.toString());
            window.location.href = url.toString();
        } else if (!newPriceRange && currentPriceRange) {
            url.searchParams.delete('priceRange');
            console.log('Redirecting to:', url.toString());
            window.location.href = url.toString();
        } else {
            console.log('No change in price filter, no redirect needed');
        }
    }
    // Gắn sự kiện cho nút "Áp dụng" (mức giá)
    const applyPriceButton = document.getElementById('apply-price-filter');
    if (applyPriceButton) {
        applyPriceButton.addEventListener('click', applyPriceFilter);
    } else {
        console.error('Apply price button not found: apply-price-filter');
    }

    // Gắn sự kiện cho nút "Đặt lại" (mức giá)
    const resetPriceButton = document.getElementById('reset-price-filter');
    if (resetPriceButton) {
        resetPriceButton.addEventListener('click', resetPriceFilter);
    } else {
        console.error('Reset price button not found: reset-price-filter');
    }

    // Gắn sự kiện cho nút toggle dropdown (mức giá)
    const togglePriceButton = document.querySelector('#dropdownMucGia + .filter-btn');
    if (togglePriceButton) {
        togglePriceButton.addEventListener('click', () => toggleDropdown('dropdownMucGia'));
    } else {
        console.error('Toggle price button not found');
    }

    // Gắn sự kiện cho nút đóng dropdown (mức giá)
    const closePriceButton = document.getElementById('close-muc-gia');
    if (closePriceButton) {
        closePriceButton.addEventListener('click', () => {
            const dropdown = document.getElementById('dropdownMucGia');
            if (dropdown) {
                dropdown.classList.add('d-none');
                console.log('Price dropdown closed');
            }
        });
    } else {
        console.error('Close price button not found');
    }

    // Hàm áp dụng tìm kiếm theo địa chỉ đường
    function applySearch() {
        console.log('Applying search');
        const searchTerm = document.getElementById('searchInput').value.trim();
        console.log('Search term:', searchTerm);

        const url = new URL(window.location.href);
        const currentSearchTerm = url.searchParams.get('searchTerm');

        if (searchTerm && searchTerm !== currentSearchTerm) {
            url.searchParams.set('searchTerm', searchTerm);
            console.log('Redirecting to:', url.toString());
            window.location.href = url.toString();
        } else if (!searchTerm && currentSearchTerm) {
            url.searchParams.delete('searchTerm');
            console.log('Redirecting to:', url.toString());
            window.location.href = url.toString();
        } else {
            console.log('No change in search term, no redirect needed');
        }
    }
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                applySearch();
            }
        });
    } else {
        console.error('Search input not found: searchInput');
    }

    // Xử lý thanh trượt mức giá
    const priceSlider = document.getElementById('priceRangeSlider');
    const minPriceInput = document.getElementById('minPrice');
    const maxPriceInput = document.getElementById('maxPrice');
    const priceValue = document.getElementById('priceRangeValue');
    const priceRadios = document.querySelectorAll('input[name="price"]');

    if (priceSlider && minPriceInput && maxPriceInput && priceValue) {
        // Khởi tạo giá trị mặc định
        let minVal = 0;
        let maxVal = 10000000;

        // Cập nhật giá trị khi kéo thanh trượt
        priceSlider.addEventListener('input', function() {
            const value = parseInt(this.value);
            const range = 10000000 - 0; // Tổng phạm vi
            minVal = 0 + (value / 100) * range * 0.4; // 40% đầu cho min
            maxVal = 0 + (value / 100) * range * 1.4; // 140% cuối cho max, giới hạn max là 10 triệu

            minVal = Math.min(Math.max(0, minVal), maxVal); // Giới hạn min không vượt max
            maxVal = Math.min(10000000, maxVal); // Giới hạn max không vượt 10 triệu

            minPriceInput.value = (minVal / 1_000_000).toFixed(1);
            maxPriceInput.value = (maxVal / 1_000_000).toFixed(1);
            priceValue.textContent = `${(minVal / 1_000_000).toFixed(1)} - ${(maxVal / 1_000_000).toFixed(1)} triệu`;

            // Đồng bộ với radio button
            let selectedRadio = null;
            priceRadios.forEach(radio => {
                const min = parseFloat(radio.getAttribute('data-min') || 0);
                const max = parseFloat(radio.getAttribute('data-max') || 10000000);
                if (minVal >= min && maxVal <= max) {
                    radio.checked = true;
                    selectedRadio = radio;
                }
            });
            if (!selectedRadio) {
                document.getElementById('price-all').checked = true;
            }
        });

        // Áp dụng bộ lọc khi thả thanh trượt
        priceSlider.addEventListener('change', function() {
            applyPriceFilter();
        });

        // Đồng bộ input với thanh trượt
        minPriceInput.addEventListener('input', function() {
            const value = parseFloat(this.value) * 1_000_000 || 0;
            if (value > maxVal) maxVal = value;
            priceSlider.value = Math.round((value / 10000000) * 100);
            priceValue.textContent = `${(value / 1_000_000).toFixed(1)} - ${(maxVal / 1_000_000).toFixed(1)} triệu`;
            applyPriceFilter();
        });

        maxPriceInput.addEventListener('input', function() {
            const value = parseFloat(this.value) * 1_000_000 || 10000000;
            if (value < minVal) minVal = value;
            priceSlider.value = Math.round((value / 10000000) * 100);
            priceValue.textContent = `${(minVal / 1_000_000).toFixed(1)} - ${(value / 1_000_000).toFixed(1)} triệu`;
            applyPriceFilter();
        });
    } else {
        console.error('Price slider or inputs not found');
    }