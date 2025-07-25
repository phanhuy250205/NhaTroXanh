
window.NhaTroContract = {
  // Tab management
  currentTab: "tenantInfo",

  init() {
    this.setupEventListeners()
    this.setCurrentDate()
    this.updatePreview()
  },

  setupEventListeners() {
    // Tab click events
    document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
      link.addEventListener("click", (e) => {
        e.preventDefault()
        const tabId = link.getAttribute("data-tab")
        this.showTab(tabId)
      })
    })

    // Button events
    document.getElementById("btn-next-owner")?.addEventListener("click", () => this.showTab("ownerInfo"))
    document.getElementById("btn-prev-tenant")?.addEventListener("click", () => this.showTab("tenantInfo"))
    document.getElementById("btn-next-room")?.addEventListener("click", () => this.showTab("roomInfo"))
    document.getElementById("btn-prev-owner")?.addEventListener("click", () => this.showTab("ownerInfo"))
    document.getElementById("btn-next-terms")?.addEventListener("click", () => this.showTab("terms"))
    document.getElementById("btn-prev-room")?.addEventListener("click", () => this.showTab("roomInfo"))

    // Image upload events
    document.getElementById("cccd-front")?.addEventListener("change", (e) => {
      this.previewImage(e, "cccd-front-preview")
    })

    document.getElementById("cccd-back")?.addEventListener("change", (e) => {
      this.previewImage(e, "cccd-back-preview")
    })

    // Form input events for live preview
    this.setupPreviewListeners()
  },

  setupPreviewListeners() {
    const inputs = [
      { id: "tenant-name", preview: "preview-tenant-name" },
      { id: "tenant-dob", preview: "preview-tenant-dob" },
      { id: "tenant-id", preview: "preview-tenant-id" },
      { id: "tenant-id-date", preview: "preview-tenant-id-date" },
      { id: "tenant-id-place", preview: "preview-tenant-id-place" },
      { id: "tenant-address", preview: "preview-tenant-address" },
      { id: "owner-name", preview: "preview-owner-name" },
      { id: "owner-dob", preview: "preview-owner-dob" },
      { id: "owner-id", preview: "preview-owner-id" },
      { id: "owner-id-date", preview: "preview-owner-id-date" },
      { id: "owner-id-place", preview: "preview-owner-id-place" },
      { id: "owner-address", preview: "preview-owner-address" },
      { id: "room-address", preview: "preview-room-address" },
      { id: "rent-price", preview: "preview-rent" },
      { id: "contract-duration", preview: "preview-duration" },
      { id: "start-date", preview: "preview-start-date" },
      { id: "contract-date", preview: "preview-sign-date" },
    ]

    inputs.forEach((input) => {
      const element = document.getElementById(input.id)
      if (element) {
        element.addEventListener("input", () => {
          this.updatePreviewField(input.id, input.preview)
        })
      }
    })
  },

  setCurrentDate() {
    const today = new Date().toISOString().split("T")[0]
    const contractDateInput = document.getElementById("contract-date")
    const startDateInput = document.getElementById("start-date")

    if (contractDateInput) contractDateInput.value = today
    if (startDateInput) startDateInput.value = today
  },

  showTab(tabId) {
    // Hide all tabs
    document.querySelectorAll(".tab-pane").forEach((pane) => {
      pane.classList.remove("show", "active")
    })

    // Remove active class from all nav links
    document.querySelectorAll(".nha-tro-tabs .nav-link").forEach((link) => {
      link.classList.remove("active")
    })

    // Show selected tab
    const targetTab = document.getElementById(tabId)
    const targetLink = document.querySelector(`[data-tab="${tabId}"]`)

    if (targetTab && targetLink) {
      targetTab.classList.add("show", "active")
      targetLink.classList.add("active")
      this.currentTab = tabId
    }

    // Scroll to top
    window.scrollTo({ top: 0, behavior: "smooth" })
  },

  previewImage(event, previewId) {
    const file = event.target.files[0]
    const preview = document.getElementById(previewId)

    if (file) {
      const reader = new FileReader()
      reader.onload = (e) => {
        preview.innerHTML = `<img src="${e.target.result}" alt="CCCD Preview" style="width: 100%; height: 100%; object-fit: cover; border-radius: 8px;">`
      }
      reader.readAsDataURL(file)
    }
  },

  updatePreview() {
    // Update all preview fields on page load
    this.updatePreviewField("contract-date", "preview-sign-date")
    this.updatePreviewField("start-date", "preview-start-date")
  },

  updatePreviewField(inputId, previewId) {
    const input = document.getElementById(inputId)
    const preview = document.getElementById(previewId)

    if (input && preview) {
      let value = input.value

      // Format date if needed
      if (input.type === "date" && value) {
        const date = new Date(value)
        value = date.toLocaleDateString("vi-VN")
      }

      // Format number if it's rent price
      if (inputId === "rent-price" && value) {
        value = new Intl.NumberFormat("vi-VN").format(value)
      }

      preview.textContent = value || "........................"
    }
  },

  showNotification(message, type = "info") {
    // Simple notification system
    const notification = document.createElement("div")
    notification.className = `alert alert-${type} alert-dismissible fade show position-fixed`
    notification.style.cssText = "top: 20px; right: 20px; z-index: 9999; min-width: 300px;"
    notification.innerHTML = `
                    ${message}
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                `

    document.body.appendChild(notification)

    // Auto remove after 5 seconds
    setTimeout(() => {
      if (notification.parentNode) {
        notification.remove()
      }
    }, 5000)
  }
}

// Initialize when DOM is loaded
document.addEventListener("DOMContentLoaded", () => {
  window.NhaTroContract.init()
})