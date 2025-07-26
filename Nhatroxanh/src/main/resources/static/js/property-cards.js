// Property Cards JavaScript
document.addEventListener("DOMContentLoaded", () => {
    // Initialize AOS (Animate On Scroll)
    AOS.init({
        duration: 800,
        easing: "ease-out-cubic",
        once: true,
        offset: 100,
    })

    // Favorite button functionality
    const favoriteButtons = document.querySelectorAll(".btn-favorite")
    favoriteButtons.forEach((button) => {
        button.addEventListener("click", function (e) {
            e.preventDefault()
            e.stopPropagation()

            const icon = this.querySelector("i")
            const isActive = this.classList.contains("active")

            if (isActive) {
                // Remove from favorites
                this.classList.remove("active")
                icon.className = "far fa-heart"
                this.style.animation = "heartBeat 0.3s ease"
            } else {
                // Add to favorites
                this.classList.add("active")
                icon.className = "fas fa-heart"
                this.style.animation = "heartBeat 0.6s ease"
            }

            // Reset animation
            setTimeout(() => {
                this.style.animation = ""
            }, 600)

            // Add ripple effect
            createRipple(e, this)
        })
    })

    // Contact button functionality
    const contactButtons = document.querySelectorAll(".btn-contact");

    contactButtons.forEach((button) => {
        button.addEventListener("click", function (e) {
            e.preventDefault();
            createRipple(e, this); // hiệu ứng ripple

            const phone = this.getAttribute("data-phone");
            const card = this.closest(".property-card");
            const title = card.querySelector(".property-title").textContent;

            setTimeout(() => {
                if (phone && phone.trim() !== '') {
                    window.location.href = `tel:${phone}`; // nếu muốn gọi trực tiếp
                } else {
                    alert(`Không có số điện thoại cho phòng "${title}"`);
                }
            }, 200);
        });
    });


    // View detail button functionality
    // const viewButtons = document.querySelectorAll(".btn-view")
    // viewButtons.forEach((button) => {
    //     button.addEventListener("click", function (e) {
    //         e.preventDefault()
    //         createRipple(e, this)

    //         // Get property title
    //         const card = this.closest(".property-card")
    //         const title = card.querySelector(".property-title").textContent

    //         setTimeout(() => {
    //             alert(`Xem chi tiết: ${title}\nChức năng đang được phát triển...`)
    //         }, 200)
    //     })
    // })

    // View more button functionality
    // const viewMoreButton = document.querySelector(".btn-view-more")
    // if (viewMoreButton) {
    //     viewMoreButton.addEventListener("click", function (e) {
    //         e.preventDefault()
    //         createRipple(e, this)

    //         setTimeout(() => {
    //             alert("Đang tải thêm nhà trọ...\nChức năng đang được phát triển")
    //         }, 200)
    //     })
    // }

    // Card hover effects
    const propertyCards = document.querySelectorAll(".property-card")
    propertyCards.forEach((card) => {
        card.addEventListener("mouseenter", function () {
            // Add subtle animation to card elements
            const image = this.querySelector(".card-image")
            const overlay = this.querySelector(".card-overlay")

            if (image) {
                image.style.transform = "scale(1.1)"
            }

            if (overlay) {
                overlay.style.opacity = "1"
            }
        })

        card.addEventListener("mouseleave", function () {
            const image = this.querySelector(".card-image")
            const overlay = this.querySelector(".card-overlay")

            if (image) {
                image.style.transform = "scale(1)"
            }

            if (overlay) {
                overlay.style.opacity = "0"
            }
        })
    })

    // Ripple effect function
    function createRipple(event, element) {
        const circle = document.createElement("span")
        const diameter = Math.max(element.clientWidth, element.clientHeight)
        const radius = diameter / 2

        const rect = element.getBoundingClientRect()
        circle.style.width = circle.style.height = `${diameter}px`
        circle.style.left = `${event.clientX - rect.left - radius}px`
        circle.style.top = `${event.clientY - rect.top - radius}px`
        circle.classList.add("ripple")

        const ripple = element.querySelector(".ripple")
        if (ripple) {
            ripple.remove()
        }

        element.appendChild(circle)

        setTimeout(() => {
            if (circle) {
                circle.remove()
            }
        }, 600)
    }

    // Intersection Observer for performance
    const cardObserver = new IntersectionObserver(
        (entries) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    entry.target.classList.add("visible")
                }
            })
        },
        {
            threshold: 0.1,
            rootMargin: "0px 0px -50px 0px",
        },
    )

    propertyCards.forEach((card) => {
        cardObserver.observe(card)
    })

    // Add heart beat animation
    const style = document.createElement("style")
    style.textContent = `
    @keyframes heartBeat {
        0% { transform: scale(1); }
        25% { transform: scale(1.2); }
        50% { transform: scale(1); }
        75% { transform: scale(1.1); }
        100% { transform: scale(1); }
    }
    
    .ripple {
        position: absolute;
        background-color: rgba(255, 255, 255, 0.6);
        border-radius: 50%;
        transform: scale(0);
        animation: ripple 0.6s linear;
        pointer-events: none;
    }
    
    @keyframes ripple {
        to {
        transform: scale(4);
        opacity: 0;
        }
    }
`
    document.head.appendChild(style)
})
