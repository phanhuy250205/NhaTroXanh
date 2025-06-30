document.addEventListener("DOMContentLoaded", function () {
        const citySelect = document.getElementById("citySelect");
        const districtSelect = document.getElementById("districtSelect");
        const wardSelect = document.getElementById("wardSelect");
        const fullAddressInput = document.getElementById("fullAddressInput");

        fetch("https://provinces.open-api.vn/api/?depth=1")
            .then(res => res.json())
            .then(data => {
                data.forEach(province => {
                    const option = document.createElement("option");
                    option.value = province.code;
                    option.textContent = province.name;
                    citySelect.appendChild(option);
                });
            });

        citySelect.addEventListener("change", () => {
            const provinceCode = citySelect.value;
            districtSelect.innerHTML = '<option value="">Chọn quận/huyện</option>';
            wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>';
            wardSelect.disabled = true;

            fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
                .then(res => res.json())
                .then(data => {
                    data.districts.forEach(district => {
                        const option = document.createElement("option");
                        option.value = district.code;
                        option.textContent = district.name;
                        districtSelect.appendChild(option);
                    });
                    districtSelect.disabled = false;
                });
        });

        districtSelect.addEventListener("change", () => {
            const districtCode = districtSelect.value;
            wardSelect.innerHTML = '<option value="">Chọn phường/xã</option>';

            fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
                .then(res => res.json())
                .then(data => {
                    data.wards.forEach(ward => {
                        const option = document.createElement("option");
                        option.value = ward.code;
                        option.textContent = ward.name;
                        wardSelect.appendChild(option);
                    });
                    wardSelect.disabled = false;
                });
        });

        // Tự động tạo địa chỉ đầy đủ
        [wardSelect, document.getElementById("houseNumberInput"), document.getElementById("streetInput")].forEach(element => {
            element.addEventListener("change", () => {
                const houseNumber = document.getElementById("houseNumberInput").value || "";
                const street = document.getElementById("streetInput").value || "";
                const province = citySelect.options[citySelect.selectedIndex].text;
                const district = districtSelect.options[districtSelect.selectedIndex].text;
                const ward = wardSelect.options[wardSelect.selectedIndex].text;

                fullAddressInput.value = `${houseNumber} ${street}, ${ward}, ${district}, ${province}`.trim();
            });
        });
    });