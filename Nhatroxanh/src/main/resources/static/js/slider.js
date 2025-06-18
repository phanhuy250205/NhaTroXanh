// Initialize Swiper when DOM is loaded
document.addEventListener("DOMContentLoaded", () => {
    // Initialize Hero Swiper
    const heroSwiper = new Swiper(".heroSwiper", {
        // Basic settings
        loop: true,
        autoplay: {
            delay: 5000, // Giảm thời gian xuống 4 giây để chuyển nhanh hơn
            disableOnInteraction: false, // Tiếp tục autoplay sau khi user tương tác
            pauseOnMouseEnter: false, // Thay đổi thành false để không dừng khi hover
            reverseDirection: false, // Chuyển theo chiều thuận
        },
        speed: 1000, // Tăng tốc độ chuyển slide
        effect: "fade",
        slidesPerView: 1,
        spaceBetween: 0,
        centeredSlides: true,

        // Navigation arrows
        navigation: {
            nextEl: ".swiper-button-next",
            prevEl: ".swiper-button-prev",
        },

        // Pagination dots
        pagination: {
            el: ".swiper-pagination",
            clickable: true,
            dynamicBullets: true,
        },

        // Responsive breakpoints
        breakpoints: {
            320: {
                autoplay: {
                    delay: 3500, // Mobile chuyển nhanh hơn
                },
            },
            768: {
                autoplay: {
                    delay: 4000,
                },
            },
            1024: {
                autoplay: {
                    delay: 4500,
                },
            },
        },

        // Event callbacks
        on: {
            init: function () {
                console.log("Hero Swiper initialized - Auto sliding enabled")
                // Add entrance animation to first slide
                const firstSlide = this.slides[this.activeIndex]
                if (firstSlide) {
                    const slideText = firstSlide.querySelector(".slide-text")
                    if (slideText) {
                        slideText.style.animation = "slideUp 1s ease-out"
                    }
                }
            },

            slideChange: function () {
                console.log(`Slide changed to: ${this.activeIndex}`)
                // Reset and trigger animations for active slide
                const activeSlide = this.slides[this.activeIndex]
                if (activeSlide) {
                    const slideText = activeSlide.querySelector(".slide-text")
                    const slideTitle = activeSlide.querySelector(".slide-title")
                    const slideDescription = activeSlide.querySelector(".slide-description")
                    const slideBtn = activeSlide.querySelector(".slide-btn")

                    // Reset animations
                    if (slideText) {
                        slideText.style.animation = "none"
                        slideText.offsetHeight // Trigger reflow
                        slideText.style.animation = "slideUp 1s ease-out"
                    }

                    // Stagger animations for text elements
                    if (slideTitle) {
                        slideTitle.style.animation = "none"
                        slideTitle.offsetHeight
                        slideTitle.style.animation = "fadeInUp 1s ease-out 0.3s both"
                    }

                    if (slideDescription) {
                        slideDescription.style.animation = "none"
                        slideDescription.offsetHeight
                        slideDescription.style.animation = "fadeInUp 1s ease-out 0.6s both"
                    }

                    if (slideBtn) {
                        slideBtn.style.animation = "none"
                        slideBtn.offsetHeight
                        slideBtn.style.animation = "fadeInUp 1s ease-out 0.9s both"
                    }
                }
            },

            autoplayTimeLeft: (s, time, progress) => {
                // Hiển thị thời gian còn lại (optional)
                // console.log(`Autoplay time left: ${Math.ceil(time / 1000)}s`);
            },

            autoplayStart: () => {
                console.log("Autoplay started")
            },

            autoplayStop: () => {
                console.log("Autoplay stopped")
            },
        },
    })

    // Đảm bảo autoplay luôn chạy
    heroSwiper.autoplay.start()

    // Loại bỏ phần pause autoplay khi hover để slide luôn tự chuyển
    // Comment out hoặc xóa phần này:
    /*
    const sliderContainer = document.querySelector(".hero-slider")
    if (sliderContainer) {
      sliderContainer.addEventListener("mouseenter", () => {
        heroSwiper.autoplay.stop()
      })
  
      sliderContainer.addEventListener("mouseleave", () => {
        heroSwiper.autoplay.start()
      })
    }
    */

    // Thay vào đó, chỉ đảm bảo autoplay luôn chạy
    const sliderContainer = document.querySelector(".hero-slider")
    if (sliderContainer) {
        // Đảm bảo autoplay không bị dừng
        setInterval(() => {
            if (!heroSwiper.autoplay.running) {
                heroSwiper.autoplay.start()
            }
        }, 1000)
    }

    // Handle slide button clicks
    const slideButtons = document.querySelectorAll(".slide-btn")
    slideButtons.forEach((button, index) => {
        button.addEventListener("click", function (e) {
            e.preventDefault()

            // Add ripple effect
            const ripple = document.createElement("span")
            const rect = this.getBoundingClientRect()
            const size = Math.max(rect.width, rect.height)
            const x = e.clientX - rect.left - size / 2
            const y = e.clientY - rect.top - size / 2

            ripple.style.width = ripple.style.height = size + "px"
            ripple.style.left = x + "px"
            ripple.style.top = y + "px"
            ripple.classList.add("ripple")

            this.appendChild(ripple)

            setTimeout(() => {
                ripple.remove()
            }, 600)

            // Handle button action based on slide
            const slideIndex = heroSwiper.activeIndex
            switch (slideIndex) {
                case 0:
                    alert("Chuyển đến trang tìm kiếm nhà trọ")
                    break
                case 1:
                    alert("Chuyển đến trang căn hộ cao cấp")
                    break
                case 2:
                    alert("Chuyển đến trang nhà nguyên căn")
                    break
                default:
                    alert("Chức năng đang được phát triển")
            }
        })
    })

    // Keyboard navigation
    document.addEventListener("keydown", (e) => {
        if (e.key === "ArrowLeft") {
            heroSwiper.slidePrev()
        } else if (e.key === "ArrowRight") {
            heroSwiper.slideNext()
        }
    })

    // Touch/swipe gestures for mobile (already handled by Swiper, but can be customized)
    let touchStartX = 0
    let touchEndX = 0

    sliderContainer?.addEventListener("touchstart", (e) => {
        touchStartX = e.changedTouches[0].screenX
    })

    sliderContainer?.addEventListener("touchend", (e) => {
        touchEndX = e.changedTouches[0].screenX
        handleSwipe()
    })

    function handleSwipe() {
        const swipeThreshold = 50
        const diff = touchStartX - touchEndX

        if (Math.abs(diff) > swipeThreshold) {
            if (diff > 0) {
                // Swiped left - next slide
                heroSwiper.slideNext()
            } else {
                // Swiped right - previous slide
                heroSwiper.slidePrev()
            }
        }
    }

    // Intersection Observer for performance optimization
    const observerOptions = {
        root: null,
        rootMargin: "0px",
        threshold: 0.1,
    }

    const sliderObserver = new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                // Slider is visible, ensure autoplay is running
                if (!heroSwiper.autoplay.running) {
                    heroSwiper.autoplay.start()
                }
            } else {
                // Slider is not visible, pause autoplay for performance
                heroSwiper.autoplay.stop()
            }
        })
    }, observerOptions)

    if (sliderContainer) {
        sliderObserver.observe(sliderContainer)
    }
})
