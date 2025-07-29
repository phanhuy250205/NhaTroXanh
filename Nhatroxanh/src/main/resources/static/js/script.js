document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll('.nav-sidebar').forEach(item => {
        item.addEventListener('click', () => {
            document.querySelectorAll('.nav-sidebar').forEach(i => i.classList.remove('active'));
            item.classList.add('active');
        });
    });

    // Load districts and wards if province or district is pre-selected
    const provinceSelect = document.getElementById('provinceSelect');
    if (provinceSelect && provinceSelect.value) {
        loadDistricts();
    }
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

document.addEventListener("DOMContentLoaded", () => {
    loadProvinces();

    const url = new URL(window.location.href);
    const provinceCode = url.searchParams.get("provinceCode");
    const districtCode = url.searchParams.get("districtCode");
    const wardCode = url.searchParams.get("wardCode");

    if (provinceCode) {
        setTimeout(() => {
            document.getElementById("provinceSelect").value = provinceCode;
            loadDistricts(() => {
                if (districtCode) {
                    document.getElementById("districtSelect").value = districtCode;
                    loadWards(() => {
                        if (wardCode) {
                            document.getElementById("wardSelect").value = wardCode;
                        }
                    });
                }
            });
        }, 300);
    }
});

function loadProvinces() {
    const provinceSelect = document.getElementById("provinceSelect");
    provinceSelect.innerHTML = '<option value="">Chọn tỉnh</option>';

    fetch("https://provinces.open-api.vn/api/p")
        .then(res => res.json())
        .then(data => {
            data.forEach(p => {
                const opt = document.createElement("option");
                opt.value = p.code;
                opt.textContent = p.name;
                provinceSelect.appendChild(opt);
            });
        })
        .catch(err => {
            console.error("Lỗi tải danh sách tỉnh:", err);
        });
}

function loadDistricts(callback) {
    const provinceCode = document.getElementById("provinceSelect").value;
    const districtSelect = document.getElementById("districtSelect");
    const wardSelect = document.getElementById("wardSelect");

    districtSelect.innerHTML = '<option value="">Chọn huyện</option>';
    wardSelect.innerHTML = '<option value="">Chọn xã</option>';
    districtSelect.disabled = true;
    wardSelect.disabled = true;

    if (provinceCode) {
        fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
            .then(res => res.json())
            .then(data => {
                data.districts.forEach(d => {
                    const opt = document.createElement("option");
                    opt.value = d.code;
                    opt.textContent = d.name;
                    districtSelect.appendChild(opt);
                });
                districtSelect.disabled = false;
                if (callback) callback();
            })
            .catch(err => {
                console.error("Lỗi tải danh sách huyện:", err);
            });
    }
}

function loadWards(callback) {
    const districtCode = document.getElementById("districtSelect").value;
    const wardSelect = document.getElementById("wardSelect");

    wardSelect.innerHTML = '<option value="">Chọn xã</option>';
    wardSelect.disabled = true;

    if (districtCode) {
        fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
            .then(res => res.json())
            .then(data => {
                data.wards.forEach(w => {
                    const opt = document.createElement("option");
                    opt.value = w.code;
                    opt.textContent = w.name;
                    wardSelect.appendChild(opt);
                });
                wardSelect.disabled = false;
                if (callback) callback();
            })
            .catch(err => {
                console.error("Lỗi tải danh sách xã:", err);
            });
    }
}

// Hàm đặt lại bộ lọc địa điểm
function resetLocationFilter() {
    console.log('Resetting location filter');
    const provinceSelect = document.getElementById('provinceSelect');
    const districtSelect = document.getElementById('districtSelect');
    const wardSelect = document.getElementById('wardSelect');
    const dropdown = document.getElementById('dropdownDiaDiem');

    if (provinceSelect && districtSelect && wardSelect) {
        provinceSelect.value = '';
        districtSelect.innerHTML = '<option value="">Chọn huyện</option>';
        districtSelect.disabled = true;
        wardSelect.innerHTML = '<option value="">Chọn xã</option>';
        wardSelect.disabled = true;
    } else {
        console.error('One or more dropdown elements not found:', {
            provinceSelect: !!provinceSelect,
            districtSelect: !!districtSelect,
            wardSelect: !!wardSelect
        });
    }

    if (dropdown) {
        dropdown.classList.add('d-none');
    } else {
        console.error('Dropdown element not found: dropdownDiaDiem');
    }

    const url = new URL(window.location.href);
    url.searchParams.delete('provinceCode');
    url.searchParams.delete('districtCode');
    url.searchParams.delete('wardCode');
    console.log('Redirecting to:', url.toString());
    window.location.href = url.toString();
}

// Hàm áp dụng bộ lọc địa điểm
function applyLocationFilter() {
    console.log('Applying location filter');
    const provinceCode = document.getElementById('provinceSelect').value;
    const districtCode = document.getElementById('districtSelect').value;
    const wardCode = document.getElementById('wardSelect').value;

    const dropdown = document.getElementById('dropdownDiaDiem');
    if (dropdown) {
        dropdown.classList.add('d-none');
    } else {
        console.error('Dropdown element not found: dropdownDiaDiem');
    }

    const url = new URL(window.location.href);
    const currentProvinceCode = url.searchParams.get('provinceCode');
    const currentDistrictCode = url.searchParams.get('districtCode');
    const currentWardCode = url.searchParams.get('wardCode');

    let shouldRedirect = false;
    if (provinceCode && provinceCode !== currentProvinceCode) {
        url.searchParams.set('provinceCode', provinceCode);
        shouldRedirect = true;
    } else if (!provinceCode && currentProvinceCode) {
        url.searchParams.delete('provinceCode');
        shouldRedirect = true;
    }

    if (districtCode && districtCode !== currentDistrictCode) {
        url.searchParams.set('districtCode', districtCode);
        shouldRedirect = true;
    } else if (!districtCode && currentDistrictCode) {
        url.searchParams.delete('districtCode');
        shouldRedirect = true;
    }

    if (wardCode && wardCode !== currentWardCode) {
        url.searchParams.set('wardCode', wardCode);
        shouldRedirect = true;
    } else if (!wardCode && currentWardCode) {
        url.searchParams.delete('wardCode');
        shouldRedirect = true;
    }

    if (shouldRedirect) {
        console.log('Redirecting to:', url.toString());
        window.location.href = url.toString();
    } else {
        console.log('No change in location filter, no redirect needed');
    }
}

// Gắn sự kiện cho nút "Tìm ngay" (địa điểm)
const applyLocationButton = document.querySelector('#dropdownDiaDiem .btn-primary.btn-sm');
if (applyLocationButton) {
    applyLocationButton.addEventListener('click', applyLocationFilter);
} else {
    console.error('Apply location button not found');
}

// Gắn sự kiện cho nút "Đặt lại" (địa điểm)
const resetLocationButton = document.querySelector('#dropdownDiaDiem .btn-outline-secondary.btn-sm');
if (resetLocationButton) {
    resetLocationButton.addEventListener('click', resetLocationFilter);
} else {
    console.error('Reset location button not found');
}

// Gắn sự kiện cho nút toggle dropdown (địa điểm)
const toggleLocationButton = document.querySelector('.filter-btn-wrapper .filter-btn');
if (toggleLocationButton) {
    toggleLocationButton.addEventListener('click', () => toggleDropdown('dropdownDiaDiem'));
} else {
    console.error('Toggle location button not found');
}

// Gắn sự kiện cho nút đóng dropdown (địa điểm)
const closeLocationButton = document.querySelector('#dropdownDiaDiem .btn.btn-sm');
if (closeLocationButton) {
    closeLocationButton.addEventListener('click', () => {
        const dropdown = document.getElementById('dropdownDiaDiem');
        if (dropdown) {
            dropdown.classList.add('d-none');
            console.log('Dropdown closed');
        }
    });
} else {
    console.error('Close location button not found');
}

// Hàm đặt lại bộ lọc mức giá
function resetPriceFilter() {
    console.log('Resetting price filter');
    document.querySelectorAll('input[name="price"]').forEach(radio => radio.checked = false);

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
function applyPriceFilter() {
    console.log('Applying price filter');
    const selectedPrice = document.querySelector('input[name="price"]:checked');
    const priceRange = selectedPrice ? selectedPrice.value : null;

    const dropdown = document.getElementById('dropdownMucGia');
    if (dropdown) {
        dropdown.classList.add('d-none');
    }

    const url = new URL(window.location.href);
    const currentPriceRange = url.searchParams.get('priceRange');

    let newPriceRange = priceRange;
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
    const provinceCode = document.getElementById('provinceSelect').value;
    const districtCode = document.getElementById('districtSelect').value;
    const wardCode = document.getElementById('wardSelect').value;
    const selectedPrice = document.querySelector('input[name="price"]:checked');
    const priceRange = selectedPrice ? selectedPrice.value : null;

    const url = new URL(window.location.href);
    const params = url.searchParams;

    // Gán searchTerm
    if (searchTerm) {
        params.set('searchTerm', searchTerm);
    } else {
        params.delete('searchTerm');
    }

    // Gán provinceCode
    if (provinceCode) {
        params.set('provinceCode', provinceCode);
    } else {
        params.delete('provinceCode');
    }

    // Gán districtCode
    if (districtCode) {
        params.set('districtCode', districtCode);
    } else {
        params.delete('districtCode');
    }

    // Gán wardCode
    if (wardCode) {
        params.set('wardCode', wardCode);
    } else {
        params.delete('wardCode');
    }

    // Gán priceRange
    if (priceRange) {
        params.set('priceRange', priceRange);
    } else {
        params.delete('priceRange');
    }

    // Redirect
    console.log('Redirecting to:', url.toString());
    window.location.href = url.toString();
}


const searchInput = document.getElementById('searchInput');
if (searchInput) {
    searchInput.addEventListener('keypress', function (e) {
        if (e.key === 'Enter') {
            applySearch();
        }
    });
} else {
    console.error('Search input not found: searchInput');
}