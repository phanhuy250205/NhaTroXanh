document.addEventListener("DOMContentLoaded", function () {
  const provinceSelect = document.getElementById("province");
  const districtSelect = document.getElementById("district");
  const wardSelect = document.getElementById("ward");

  if (!provinceSelect || !districtSelect || !wardSelect) {
    console.warn("Không tìm thấy các phần tử select!");
    return;
  }

  // Load tỉnh
  fetch("https://provinces.open-api.vn/api/p/")
    .then(res => res.json())
    .then(data => {
      data.forEach(province => {
        const option = document.createElement("option");
        option.value = province.code;
        option.textContent = province.name;
        provinceSelect.appendChild(option);
      });
    });

  // Load quận
  provinceSelect.addEventListener("change", () => {
    const provinceCode = provinceSelect.value;
    districtSelect.innerHTML = '<option selected>Quận/Huyện</option>';
    wardSelect.innerHTML = '<option selected>Phường/Xã</option>';

    fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
      .then(res => res.json())
      .then(data => {
        data.districts.forEach(district => {
          const option = document.createElement("option");
          option.value = district.code;
          option.textContent = district.name;
          districtSelect.appendChild(option);
        });
      });
  });

  // Load phường
  districtSelect.addEventListener("change", () => {
    const districtCode = districtSelect.value;
    wardSelect.innerHTML = '<option selected>Phường/Xã</option>';

    fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
      .then(res => res.json())
      .then(data => {
        data.wards.forEach(ward => {
          const option = document.createElement("option");
          option.value = ward.code;
          option.textContent = ward.name;
          wardSelect.appendChild(option);
        });
      });
  });
});
