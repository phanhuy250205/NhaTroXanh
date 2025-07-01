document.addEventListener("DOMContentLoaded", function () {
        const provinceHost = document.getElementById("provinceHost");
        const districtHost = document.getElementById("districtHost");
        const wardHost = document.getElementById("wardHost");
        const fullAddressHost = document.getElementById("fullAddressHost");

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
        [wardHost, document.getElementById("houseNumberHost"), document.getElementById("streetHost")].forEach(element => {
            element.addEventListener("change", () => {
                const houseNumber = document.getElementById("houseNumberHost").value || "";
                const street = document.getElementById("streetHost").value || "";
                const province = provinceHost.options[provinceHost.selectedIndex].text;
                const district = districtHost.options[districtHost.selectedIndex].text;
                const ward = wardHost.options[wardHost.selectedIndex].text;

                fullAddressHost.value = `${houseNumber} ${street}, ${ward}, ${district}, ${province}`.trim();
            });
        });
    });