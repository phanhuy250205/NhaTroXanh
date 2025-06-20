
// document.addEventListener("DOMContentLoaded", function () {
//   const provinceSelect = document.getElementById("provinceHost");
//   const districtSelect = document.getElementById("districtHost");
//   const wardSelect = document.getElementById("wardHost");
//   const fullAddressInput = document.getElementById("fullAddressHost");

//   fetch("https://provinces.open-api.vn/api/?depth=1")
//     .then(res => res.json())
//     .then(data => {
//       data.forEach(province => {
//         const option = document.createElement("option");
//         option.value = province.code;
//         option.textContent = province.name;
//         provinceSelect.appendChild(option);
//       });
//     });

//   provinceSelect.addEventListener("change", () => {
//     const provinceCode = provinceSelect.value;
//     districtSelect.innerHTML = '<option>Chọn quận/huyện</option>';
//     wardSelect.innerHTML = '<option>Chọn phường/xã</option>';
//     wardSelect.disabled = true;

//     fetch(`https://provinces.open-api.vn/api/p/${provinceCode}?depth=2`)
//       .then(res => res.json())
//       .then(data => {
//         data.districts.forEach(district => {
//           const option = document.createElement("option");
//           option.value = district.code;
//           option.textContent = district.name;
//           districtSelect.appendChild(option);
//         });
//         districtSelect.disabled = false;
//       });
//   });

//   districtSelect.addEventListener("change", () => {
//     const districtCode = districtSelect.value;
//     wardSelect.innerHTML = '<option>Chọn phường/xã</option>';

//     fetch(`https://provinces.open-api.vn/api/d/${districtCode}?depth=2`)
//       .then(res => res.json())
//       .then(data => {
//         data.wards.forEach(ward => {
//           const option = document.createElement("option");
//           option.value = ward.code;
//           option.textContent = ward.name;
//           wardSelect.appendChild(option);
//         });
//         wardSelect.disabled = false;
//       });
//   });

//   // Tự động tạo địa chỉ đầy đủ
//   wardSelect.addEventListener("change", () => {
//     const street = document.getElementById("streetHost").value || "";
//     const houseNumber = document.getElementById("houseNumberHost").value || "";

//     const province = provinceSelect.options[provinceSelect.selectedIndex].text;
//     const district = districtSelect.options[districtSelect.selectedIndex].text;
//     const ward = wardSelect.options[wardSelect.selectedIndex].text;

//     fullAddressInput.value = `${houseNumber} ${street}, ${ward}, ${district}, ${province}`;
//   });
// });

