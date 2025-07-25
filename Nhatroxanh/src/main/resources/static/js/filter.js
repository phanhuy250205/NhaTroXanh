class LocationFilterGuest {
            constructor() {
                this.apiUrl = 'https://provinces.open-api.vn/api';
                this.provinces = [];
                this.districts = [];
                this.wards = [];
                this.selectedLocation = {
                    province: null,
                    district: null,
                    ward: null
                };
                
                this.init();
            }

            init() {
                this.bindEvents();
                this.loadProvinces();
            }

            bindEvents() {
                const locationDisplay = document.getElementById('locationDisplay-guest');
                const locationDropdown = document.getElementById('locationDropdown-guest');
                const provinceSelect = document.getElementById('provinceSelect-guest');
                const districtSelect = document.getElementById('districtSelect-guest');
                const wardSelect = document.getElementById('wardSelect-guest');
                const searchBtn = document.getElementById('searchBtn-guest');
                const clearLocationBtn = document.getElementById('clearLocationBtn-guest');

                // Toggle location dropdown
                locationDisplay.addEventListener('click', (e) => {
                    e.preventDefault();
                    locationDropdown.classList.toggle('show-guest');
                });

                // Close dropdown when clicking outside
                document.addEventListener('click', (e) => {
                    if (!e.target.closest('.location-wrapper-guest')) {
                        locationDropdown.classList.remove('show-guest');
                    }
                });

                // Clear location
                clearLocationBtn.addEventListener('click', () => {
                    this.clearAllSelections();
                });

                // Province change
                provinceSelect.addEventListener('change', (e) => {
                    const provinceCode = e.target.value;
                    if (provinceCode) {
                        this.selectedLocation.province = this.provinces.find(p => p.code == provinceCode);
                        this.loadDistricts(provinceCode);
                        districtSelect.disabled = false;
                        this.resetSelect(districtSelect);
                        this.resetSelect(wardSelect);
                        wardSelect.disabled = true;
                        this.selectedLocation.district = null;
                        this.selectedLocation.ward = null;
                    } else {
                        this.selectedLocation.province = null;
                        this.resetSelect(districtSelect);
                        this.resetSelect(wardSelect);
                        districtSelect.disabled = true;
                        wardSelect.disabled = true;
                        this.selectedLocation.district = null;
                        this.selectedLocation.ward = null;
                    }
                    this.updateLocationDisplay();
                });

                // District change
                districtSelect.addEventListener('change', (e) => {
                    const districtCode = e.target.value;
                    if (districtCode) {
                        this.selectedLocation.district = this.districts.find(d => d.code == districtCode);
                        this.loadWards(districtCode);
                        wardSelect.disabled = false;
                        this.resetSelect(wardSelect);
                        this.selectedLocation.ward = null;
                    } else {
                        this.selectedLocation.district = null;
                        this.resetSelect(wardSelect);
                        wardSelect.disabled = true;
                        this.selectedLocation.ward = null;
                    }
                    this.updateLocationDisplay();
                });

                // Ward change
                wardSelect.addEventListener('change', (e) => {
                    const wardCode = e.target.value;
                    if (wardCode) {
                        this.selectedLocation.ward = this.wards.find(w => w.code == wardCode);
                    } else {
                        this.selectedLocation.ward = null;
                    }
                    this.updateLocationDisplay();
                });

                // Search button
                searchBtn.addEventListener('click', () => {
                    this.performSearch();
                });

                // Enter key on search input
                document.getElementById('searchInput-guest').addEventListener('keypress', (e) => {
                    if (e.key === 'Enter') {
                        this.performSearch();
                    }
                });

                // Add loading states
                this.addLoadingStates();
            }

            addLoadingStates() {
                const selects = ['provinceSelect-guest', 'districtSelect-guest', 'wardSelect-guest'];
                selects.forEach(selectId => {
                    const select = document.getElementById(selectId);
                    select.addEventListener('focus', () => {
                        if (select.options.length <= 1 && !select.disabled) {
                            select.classList.add('loading-guest');
                        }
                    });
                });
            }

            async loadProvinces() {
                const provinceSelect = document.getElementById('provinceSelect-guest');
                provinceSelect.classList.add('loading-guest');
                
                try {
                    const response = await fetch(`${this.apiUrl}/p/`);
                    this.provinces = await response.json();
                    this.populateSelect('provinceSelect-guest', this.provinces);
                } catch (error) {
                    console.error('Error loading provinces:', error);
                    this.showError('Không thể tải danh sách tỉnh/thành phố');
                } finally {
                    provinceSelect.classList.remove('loading-guest');
                }
            }

            async loadDistricts(provinceCode) {
                const districtSelect = document.getElementById('districtSelect-guest');
                districtSelect.classList.add('loading-guest');
                
                try {
                    const response = await fetch(`${this.apiUrl}/p/${provinceCode}?depth=2`);
                    const data = await response.json();
                    this.districts = data.districts || [];
                    this.populateSelect('districtSelect-guest', this.districts);
                } catch (error) {
                    console.error('Error loading districts:', error);
                    this.showError('Không thể tải danh sách quận/huyện');
                } finally {
                    districtSelect.classList.remove('loading-guest');
                }
            }

            async loadWards(districtCode) {
                const wardSelect = document.getElementById('wardSelect-guest');
                wardSelect.classList.add('loading-guest');
                
                try {
                    const response = await fetch(`${this.apiUrl}/d/${districtCode}?depth=2`);
                    const data = await response.json();
                    this.wards = data.wards || [];
                    this.populateSelect('wardSelect-guest', this.wards);
                } catch (error) {
                    console.error('Error loading wards:', error);
                    this.showError('Không thể tải danh sách phường/xã');
                } finally {
                    wardSelect.classList.remove('loading-guest');
                }
            }

            populateSelect(selectId, data) {
                const select = document.getElementById(selectId);
                const defaultOption = select.querySelector('option[value=""]');
                select.innerHTML = '';
                select.appendChild(defaultOption);
                
                data.forEach(item => {
                    const option = document.createElement('option');
                    option.value = item.code;
                    option.textContent = item.name;
                    select.appendChild(option);
                });
            }

            resetSelect(select) {
                select.selectedIndex = 0;
            }

            clearAllSelections() {
                this.selectedLocation = {
                    province: null,
                    district: null,
                    ward: null
                };
                
                const provinceSelect = document.getElementById('provinceSelect-guest');
                const districtSelect = document.getElementById('districtSelect-guest');
                const wardSelect = document.getElementById('wardSelect-guest');
                
                this.resetSelect(provinceSelect);
                this.resetSelect(districtSelect);
                this.resetSelect(wardSelect);
                
                districtSelect.disabled = true;
                wardSelect.disabled = true;
                
                this.updateLocationDisplay();
            }

            updateLocationDisplay() {
                const locationDisplay = document.getElementById('locationDisplay-guest');
                const selectedLocationInfo = document.getElementById('selectedLocationInfo-guest');
                const selectedLocationText = document.getElementById('selectedLocationText-guest');
                
                let displayText = 'Chọn địa điểm';
                let fullLocationText = '';
                
                if (this.selectedLocation.ward) {
                    displayText = `${this.selectedLocation.ward.name}, ${this.selectedLocation.district.name}, ${this.selectedLocation.province.name}`;
                    fullLocationText = `${this.selectedLocation.ward.name}, ${this.selectedLocation.district.name}, ${this.selectedLocation.province.name}`;
                } else if (this.selectedLocation.district) {
                    displayText = `${this.selectedLocation.district.name}, ${this.selectedLocation.province.name}`;
                    fullLocationText = `${this.selectedLocation.district.name}, ${this.selectedLocation.province.name}`;
                } else if (this.selectedLocation.province) {
                    displayText = this.selectedLocation.province.name;
                    fullLocationText = this.selectedLocation.province.name;
                }
                
                locationDisplay.textContent = displayText;
                
                if (this.selectedLocation.province) {
                    locationDisplay.classList.remove('placeholder-guest');
                    locationDisplay.classList.add('selected-guest');
                    selectedLocationInfo.classList.add('show-guest');
                    selectedLocationText.textContent = fullLocationText;
                } else {
                    locationDisplay.classList.add('placeholder-guest');
                    locationDisplay.classList.remove('selected-guest');
                    selectedLocationInfo.classList.remove('show-guest');
                }
            }

            showError(message) {
                console.error(message);
                // You can implement a toast notification here
            }

            performSearch() {
                const searchInput = document.getElementById('searchInput-guest').value;
                const priceRange = document.getElementById('priceRange-guest').value;
                
                const searchData = {
                    keyword: searchInput,
                    location: this.selectedLocation,
                    priceRange: priceRange,
                    timestamp: new Date().toISOString()
                };
                
                console.log('Search data:', searchData);
                
                // Add search button loading state
                const searchBtn = document.getElementById('searchBtn-guest');
                const originalText = searchBtn.innerHTML;
                searchBtn.innerHTML = '<i class="fas fa-spinner fa-spin me-2"></i>Đang tìm...';
                searchBtn.disabled = true;
                
                // Simulate search delay
                setTimeout(() => {
                    searchBtn.innerHTML = originalText;
                    searchBtn.disabled = false;
                    
                    // Here you can implement your search logic
                    alert('Tìm kiếm với dữ liệu: ' + JSON.stringify(searchData, null, 2));
                }, 1500);
            }
        }

        // Initialize the location filter when DOM is loaded
        document.addEventListener('DOMContentLoaded', () => {
            new LocationFilterGuest();
        });

        // Add ripple effect to buttons
        document.addEventListener('DOMContentLoaded', () => {
            const searchBtn = document.getElementById('searchBtn-guest');
            searchBtn.addEventListener('click', function(e) {
                const ripple = document.createElement('span');
                const rect = this.getBoundingClientRect();
                const size = Math.max(rect.width, rect.height);
                const x = e.clientX - rect.left - size / 2;
                const y = e.clientY - rect.top - size / 2;
                
                ripple.style.width = ripple.style.height = size + 'px';
                ripple.style.left = x + 'px';
                ripple.style.top = y + 'px';
                ripple.classList.add('ripple');
                
                this.appendChild(ripple);
                
                setTimeout(() => {
                    ripple.remove();
                }, 600);
            });
        });