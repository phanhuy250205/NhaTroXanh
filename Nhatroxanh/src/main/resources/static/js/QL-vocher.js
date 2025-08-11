// --- UI Feedback Message Handlers --- //
function showSuccessMessage(message) {
    const successMsg = document.createElement("div")
    successMsg.className = "success-message-staff"
    successMsg.innerHTML = `<i class="fas fa-check"></i> ${message}`
    document.body.appendChild(successMsg)

    setTimeout(() => {
        successMsg.remove()
    }, 3000)
}

function showErrorMessage(message) {
    const errorMsg = document.createElement("div")
    errorMsg.className = "error-message-staff"
    errorMsg.innerHTML = `<i class="fas fa-times"></i> ${message}`
    document.body.appendChild(errorMsg)

    setTimeout(() => {
        errorMsg.remove()
    }, 3000)
}

// --- Styles for Feedback Messages --- //
const style = document.createElement("style");
style.innerHTML = `
.success-message-staff,
.error-message-staff {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 1055;
  padding: 12px 20px;
  border-radius: 8px;
  color: white;
  font-weight: 500;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  align-items: center;
  gap: 10px;
  animation: slideDown 0.3s ease, fadeOut 0.5s ease 2.5s forwards;
}

.success-message-staff {
  background-color: #28a745;
}

.error-message-staff {
  background-color: #dc3545;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeOut {
  to {
    opacity: 0;
    transform: translateY(-20px);
  }
}
`;
document.head.appendChild(style);
