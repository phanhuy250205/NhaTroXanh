document.addEventListener("DOMContentLoaded", function () {
    const provinceHost = document.getElementById("provinceHost");
    const districtHost = document.getElementById("districtHost");
    const wardHost = document.getElementById("wardHost");
    const fullAddressHost = document.getElementById("fullAddressHost");
    const houseNumberHost = document.getElementById("houseNumberHost");
    const streetHost = document.getElementById("streetHost");

    // Khởi tạo VietnamAddressAPI
    window.vietnamAddressAPI = {
        getSelectedAddress: function () {
            return {
                province: {
                    code: provinceHost.value,
                    name: provinceHost.options[provinceHost.selectedIndex].text
                },
                district: {
                    code: districtHost.value,
                    name: districtHost.options[districtHost.selectedIndex].text
                },
                ward: {
                    code: wardHost.value,
                    name: wardHost.options[wardHost.selectedIndex].text
                },
                street: streetHost.value,
                houseNumber: houseNumberHost.value
            };
        }
    };

    // Lấy dữ liệu tỉnh
    fetch("https://provinces.open-api.vn/api/?depth=1")
        .then(res => res.json())
        .then(data => {
            data.forEach(province => {
                const option = document.createElement("option");
                option.value = province.code;
                option.textContent = province.name;
                provinceHost.appendChild(option);
            });
        });

    // Sự kiện thay đổi tỉnh
    provinceHost.addEventListener("change", () => {
        const provinceCode = provinceHost.value;
        districtHost.innerHTML = '<option value="">Chọn quận/huyện</option>';
        wardHost.innerHTML = '<option value="">Chọn phường/xã</option>';
        wardHost.disabled = true;

        fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
            .then(res => res.json())
            .then(data => {
                data.districts.forEach(district => {
                    const option = document.createElement("option");
                    option.value = district.code;
                    option.textContent = district.name;
                    districtHost.appendChild(option);
                });
                districtHost.disabled = false;
            });
    });

    // Sự kiện thay đổi quận
    districtHost.addEventListener("change", () => {
        const districtCode = districtHost.value;
        wardHost.innerHTML = '<option value="">Chọn phường/xã</option>';

        fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
            .then(res => res.json())
            .then(data => {
                data.wards.forEach(ward => {
                    const option = document.createElement("option");
                    option.value = ward.code;
                    option.textContent = ward.name;
                    wardHost.appendChild(option);
                });
                wardHost.disabled = false;
            });
    });

    // Tự động tạo địa chỉ đầy đủ
    [wardHost, houseNumberHost, streetHost].forEach(element => {
        element.addEventListener("change", () => {
            const houseNumber = houseNumberHost.value || "";
            const street = streetHost.value || "";
            const province = provinceHost.options[provinceHost.selectedIndex].text;
            const district = districtHost.options[districtHost.selectedIndex].text;
            const ward = wardHost.options[wardHost.selectedIndex].text;

            fullAddressHost.value = `${houseNumber} ${street}, ${ward}, ${district}, ${province}`.trim();
            // Gán giá trị vào các trường ẩn (nếu cần)
            document.getElementById("provinceCodeHost").value = provinceHost.value;
            document.getElementById("provinceNameHost").value = province;
            document.getElementById("districtCodeHost").value = districtHost.value;
            document.getElementById("districtNameHost").value = district;
            document.getElementById("wardCodeHost").value = wardHost.value;
            document.getElementById("wardNameHost").value = ward;
            document.getElementById("streetHost").value = street;
            document.getElementById("houseNumberHost").value = houseNumber;
        });
    });
});