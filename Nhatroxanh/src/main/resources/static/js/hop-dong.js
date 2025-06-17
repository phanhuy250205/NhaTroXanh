  document.addEventListener('DOMContentLoaded', function () {
    function updatePreview(inputId, previewId) {
      const input = document.getElementById(inputId);
      const preview = document.getElementById(previewId);
      if (input && preview) {
        input.addEventListener('input', function () {
          preview.textContent = input.value || '......................';
        });
      }
    }
    updatePreview('tenant-name', 'preview-name');
    updatePreview('tenant-dob', 'preview-dob');
    updatePreview('tenant-id', 'preview-id');
  });

  function nextTab(id) {
    const triggerEl = document.querySelector(`a[href="${id}"]`);
    if (triggerEl) new bootstrap.Tab(triggerEl).show();
  }

  function prevTab(id) {
    const triggerEl = document.querySelector(`a[href="${id}"]`);
    if (triggerEl) new bootstrap.Tab(triggerEl).show();
  }
   function previewImage(event, previewId) {
    const input = event.target;
    const preview = document.getElementById(previewId);
    if (input.files && input.files[0]) {
      const reader = new FileReader();
      reader.onload = function (e) {
        if (preview.tagName === 'DIV') {
        preview.innerHTML = `<img src="${e.target.result}" class='img-fluid h-100 object-fit-contain' alt='CCCD Image'>`;
      } else {
        preview.src = e.target.result;
      }
      }
      reader.readAsDataURL(input.files[0]);
    }
  }