// App State
const state = {
    media: [
        {
            id: 'pic1',
            type: 'image',
            title: 'Tog\' etagidagi shafaq',
            url: 'assets/pic1.jpg',
            size: '3.4 MB',
            date: 'Bugun, 14:20'
        },
        {
            id: 'pic2',
            type: 'image',
            title: 'Kiberpank neon ko\'chalari',
            url: 'assets/pic2.jpg',
            size: '4.1 MB',
            date: 'Bugun, 11:05'
        },
        {
            id: 'vid1',
            type: 'video',
            title: 'Koinot kemasining uchishi',
            url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
            size: '8.2 MB',
            date: 'Kecha, 18:45'
        },
        {
            id: 'pic3',
            type: 'image',
            title: 'Mittivoy mushukcha',
            url: 'assets/pic3.jpg',
            size: '2.8 MB',
            date: 'Kecha, 09:15'
        },
        {
            id: 'pic4',
            type: 'image',
            title: 'Astronavt va tumanlik',
            url: 'assets/pic4.jpg',
            size: '5.3 MB',
            date: '28-iyul, 15:30'
        }
    ],
    favorites: [], // Saved IDs
    trash: [],     // Deletion queue IDs
    currentIndex: 0,
    activeScreen: 'sorter',
    isDragging: false,
    startX: 0,
    startY: 0,
    currentX: 0,
    currentY: 0
};

// UI Elements
const DOM = {
    cardStack: document.getElementById('card-stack'),
    gestureZone: document.getElementById('gesture-zone'),
    emptyState: document.getElementById('empty-state'),
    mediaCounter: document.getElementById('media-counter'),
    statusTime: document.getElementById('status-time'),
    favBadge: document.getElementById('fav-badge'),
    trashBadge: document.getElementById('trash-badge'),
    trashCounts: document.querySelectorAll('.trash-count'),
    favoritesGrid: document.getElementById('favorites-grid'),
    favoritesEmpty: document.getElementById('favorites-empty'),
    trashGrid: document.getElementById('trash-grid'),
    trashEmpty: document.getElementById('trash-empty'),
    btnEmptyTrash: document.getElementById('btn-empty-trash'),
    deleteConfirmModal: document.getElementById('delete-confirm-modal'),
    btnModalCancel: document.getElementById('btn-modal-cancel'),
    btnModalConfirm: document.getElementById('btn-modal-confirm'),
    toastContainer: document.getElementById('toast-container'),
    tutorialOverlay: document.getElementById('tutorial-overlay'),
    closeTutorial: document.getElementById('close-tutorial'),
    btnShowGuide: document.getElementById('btn-show-guide'),
    navItems: document.querySelectorAll('.bottom-nav .nav-item'),
    screens: document.querySelectorAll('.app-screen'),
    // Overlays
    overlayUp: document.querySelector('.swipe-up-overlay'),
    overlayDown: document.querySelector('.swipe-down-overlay'),
    overlayLeft: document.querySelector('.swipe-left-overlay'),
    overlayRight: document.querySelector('.swipe-right-overlay'),
    // Control Buttons
    btnLeft: document.getElementById('btn-swipe-left'),
    btnRight: document.getElementById('btn-swipe-right'),
    btnUp: document.getElementById('btn-swipe-up'),
    btnDown: document.getElementById('btn-swipe-down')
};

// Core App Controller
class MediaSorterApp {
    constructor() {
        this.init();
    }

    init() {
        this.updateTime();
        setInterval(() => this.updateTime(), 60000);

        this.renderStack();
        this.setupNavigation();
        this.setupGestures();
        this.setupControlButtons();
        this.setupTrashEvents();
        this.setupTutorial();
        
        // Show tutorial if first time
        if (localStorage.getItem('photocheck_tutorial_shown')) {
            DOM.tutorialOverlay.style.display = 'none';
        }
    }

    updateTime() {
        const now = new Date();
        const hrs = String(now.getHours()).padStart(2, '0');
        const mins = String(now.getMinutes()).padStart(2, '0');
        DOM.statusTime.textContent = `${hrs}:${mins}`;
    }

    setupNavigation() {
        DOM.navItems.forEach(item => {
            item.addEventListener('click', () => {
                const screenId = item.getAttribute('data-screen');
                this.showScreen(screenId);
            });
        });
    }

    showScreen(screenId) {
        state.activeScreen = screenId;
        
        DOM.navItems.forEach(nav => {
            if (nav.getAttribute('data-screen') === screenId) {
                nav.classList.add('active');
            } else {
                nav.classList.remove('active');
            }
        });

        DOM.screens.forEach(screen => {
            if (screen.id === `screen-${screenId}`) {
                screen.classList.add('active');
            } else {
                screen.classList.remove('active');
            }
        });

        if (screenId === 'favorites') this.renderFavorites();
        if (screenId === 'trash') this.renderTrash();
        
        // Pause any video if we leave sorter
        if (screenId !== 'sorter') {
            const videos = DOM.cardStack.querySelectorAll('video');
            videos.forEach(v => v.pause());
        }
    }

    // Get active queue items (not trash, not done)
    getAvailableMedia() {
        return state.media.filter(item => !state.trash.includes(item.id));
    }

    renderStack() {
        DOM.cardStack.innerHTML = '';
        const items = this.getAvailableMedia();
        
        if (state.currentIndex >= items.length) {
            state.currentIndex = items.length > 0 ? items.length - 1 : 0;
        }

        if (items.length === 0) {
            DOM.cardStack.style.display = 'none';
            DOM.emptyState.style.display = 'flex';
            DOM.mediaCounter.textContent = '0 / 0';
            return;
        }

        DOM.cardStack.style.display = 'block';
        DOM.emptyState.style.display = 'none';
        DOM.mediaCounter.textContent = `${state.currentIndex + 1} / ${items.length}`;

        // Render up to 3 cards for depth styling
        const cardsToShow = items.slice(state.currentIndex, state.currentIndex + 3);
        
        cardsToShow.forEach((item, index) => {
            const card = document.createElement('div');
            card.className = 'media-card';
            card.dataset.id = item.id;
            
            // Mark favorite if already liked
            if (state.favorites.includes(item.id)) {
                card.classList.add('is-favorite');
            }

            let mediaHTML = '';
            if (item.type === 'image') {
                mediaHTML = `<img src="${item.url}" alt="${item.title}">`;
            } else if (item.type === 'video') {
                mediaHTML = `
                    <video loop muted playsinline src="${item.url}"></video>
                    <div class="video-play-btn"><i class="fas fa-play"></i></div>
                `;
            }

            card.innerHTML = `
                <div class="media-container">
                    ${mediaHTML}
                </div>
                <div class="media-info">
                    <div class="media-details">
                        <span class="media-title">${item.title}</span>
                        <div class="media-meta">
                            <span class="media-badge">${item.type}</span>
                            <span>${item.size}</span>
                            <span>•</span>
                            <span>${item.date}</span>
                        </div>
                    </div>
                    <div class="heart-icon"><i class="fas fa-heart"></i></div>
                </div>
            `;

            DOM.cardStack.appendChild(card);

            // Setup autoplay/interaction for top video card
            if (index === 0 && item.type === 'video') {
                const video = card.querySelector('video');
                const playBtn = card.querySelector('.video-play-btn');
                
                // Play on click simulation or auto
                card.addEventListener('click', () => {
                    if (video.paused) {
                        video.play().catch(e => console.log("Auto play prevented", e));
                        playBtn.style.opacity = '0';
                    } else {
                        video.pause();
                        playBtn.style.opacity = '1';
                    }
                });

                // Auto play first video
                setTimeout(() => {
                    video.play().then(() => {
                        playBtn.style.opacity = '0';
                    }).catch(() => {});
                }, 400);
            }
        });

        this.bindCardDrag(DOM.cardStack.firstElementChild);
    }

    bindCardDrag(card) {
        if (!card) return;

        const handleStart = (e) => {
            state.isDragging = true;
            const clientX = e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
            const clientY = e.type.includes('touch') ? e.touches[0].clientY : e.clientY;
            state.startX = clientX;
            state.startY = clientY;
            
            card.style.transition = 'none';
        };

        const handleMove = (e) => {
            if (!state.isDragging) return;
            const clientX = e.type.includes('touch') ? e.touches[0].clientX : e.clientX;
            const clientY = e.type.includes('touch') ? e.touches[0].clientY : e.clientY;
            
            state.currentX = clientX - state.startX;
            state.currentY = clientY - state.startY;

            // Card Rotation & Position
            const rotate = state.currentX * 0.08;
            card.style.transform = `translate(${state.currentX}px, ${state.currentY}px) rotate(${rotate}deg)`;

            // Overlays fade-in
            this.handleDragOverlays(state.currentX, state.currentY);
        };

        const handleEnd = () => {
            if (!state.isDragging) return;
            state.isDragging = false;

            const threshold = 100;
            const x = state.currentX;
            const y = state.currentY;

            // Hide overlays
            this.resetOverlays();

            // Determine if swipe gesture is complete
            if (Math.abs(y) > Math.abs(x) && Math.abs(y) > threshold) {
                // Vertical Swipe
                if (y < 0) {
                    this.swipeCard(card, 'up'); // Favorite
                } else {
                    this.swipeCard(card, 'down'); // Delete Queue
                }
            } else if (Math.abs(x) > threshold) {
                // Horizontal Swipe
                if (x > 0) {
                    this.swipeCard(card, 'right'); // Previous
                } else {
                    this.swipeCard(card, 'left'); // Next
                }
            } else {
                // Reset card
                card.style.transition = 'transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.25)';
                card.style.transform = 'translate(0px, 0px) rotate(0deg)';
            }

            state.currentX = 0;
            state.currentY = 0;
        };

        // Mouse Events
        card.addEventListener('mousedown', handleStart);
        window.addEventListener('mousemove', handleMove);
        window.addEventListener('mouseup', handleEnd);

        // Touch Events
        card.addEventListener('touchstart', handleStart, { passive: true });
        window.addEventListener('touchmove', handleMove, { passive: false });
        window.addEventListener('touchend', handleEnd);
    }

    handleDragOverlays(x, y) {
        const threshold = 40;
        this.resetOverlays();

        if (Math.abs(y) > Math.abs(x)) {
            // Vertical prioritizing
            if (y < -threshold) {
                const opacity = Math.min(Math.abs(y) / 150, 0.9);
                DOM.overlayUp.style.opacity = opacity;
            } else if (y > threshold) {
                const opacity = Math.min(Math.abs(y) / 150, 0.9);
                DOM.overlayDown.style.opacity = opacity;
            }
        } else {
            // Horizontal prioritizing
            if (x < -threshold) {
                const opacity = Math.min(Math.abs(x) / 150, 0.9);
                DOM.overlayLeft.style.opacity = opacity;
            } else if (x > threshold) {
                const opacity = Math.min(Math.abs(x) / 150, 0.9);
                DOM.overlayRight.style.opacity = opacity;
            }
        }
    }

    resetOverlays() {
        DOM.overlayUp.style.opacity = '0';
        DOM.overlayDown.style.opacity = '0';
        DOM.overlayLeft.style.opacity = '0';
        DOM.overlayRight.style.opacity = '0';
    }

    swipeCard(card, direction) {
        const id = card.dataset.id;
        let transformStr = '';
        
        switch (direction) {
            case 'left':
                transformStr = 'translate(-120%, 0) rotate(-25deg)';
                this.showToast('Keyingi rasmga o\'tildi', 'info');
                break;
            case 'right':
                transformStr = 'translate(120%, 0) rotate(25deg)';
                this.showToast('Oldingi rasmga qaytildi', 'info');
                break;
            case 'up':
                transformStr = 'translate(0, -120%) scale(0.8)';
                this.toggleFavorite(id, true);
                break;
            case 'down':
                transformStr = 'translate(0, 120%) scale(0.8)';
                this.queueDelete(id);
                break;
        }

        card.style.transition = 'transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94), opacity 0.3s';
        card.style.transform = transformStr;
        card.style.opacity = '0';

        // Wait for animation to finish
        setTimeout(() => {
            const items = this.getAvailableMedia();
            
            if (direction === 'left' || direction === 'up' || direction === 'down') {
                if (state.currentIndex < items.length - 1) {
                    state.currentIndex++;
                } else if (items.length > 0) {
                    state.currentIndex = 0; // Wrap around if finished
                }
            } else if (direction === 'right') {
                if (state.currentIndex > 0) {
                    state.currentIndex--;
                } else {
                    state.currentIndex = items.length - 1; // Wrap around
                }
            }

            this.renderStack();
            this.updateBadges();
        }, 300);
    }

    setupControlButtons() {
        DOM.btnLeft.addEventListener('click', () => {
            const card = DOM.cardStack.firstElementChild;
            if (card) this.swipeCard(card, 'right');
        });

        DOM.btnRight.addEventListener('click', () => {
            const card = DOM.cardStack.firstElementChild;
            if (card) this.swipeCard(card, 'left');
        });

        DOM.btnUp.addEventListener('click', () => {
            const card = DOM.cardStack.firstElementChild;
            if (card) this.swipeCard(card, 'up');
        });

        DOM.btnDown.addEventListener('click', () => {
            const card = DOM.cardStack.firstElementChild;
            if (card) this.swipeCard(card, 'down');
        });
    }

    toggleFavorite(id, isSwipe = false) {
        const index = state.favorites.indexOf(id);
        if (index === -1) {
            state.favorites.push(id);
            this.showToast('Sevimlilarga qo\'shildi ❤', 'success');
        } else if (!isSwipe) {
            state.favorites.splice(index, 1);
            this.showToast('Sevimlilardan olib tashlandi', 'info');
        }
        this.updateBadges();
    }

    queueDelete(id) {
        if (!state.trash.includes(id)) {
            state.trash.push(id);
            this.showToast('O\'chirish navbatiga qo\'shildi 🗑', 'danger');
            
            // Remove from favorites if it is deleted
            const favIndex = state.favorites.indexOf(id);
            if (favIndex !== -1) {
                state.favorites.splice(favIndex, 1);
            }
        }
        this.updateBadges();
    }

    updateBadges() {
        const favCount = state.favorites.length;
        const trashCount = state.trash.length;

        // Favorite badge
        if (favCount > 0) {
            DOM.favBadge.textContent = favCount;
            DOM.favBadge.style.display = 'block';
        } else {
            DOM.favBadge.style.display = 'none';
        }

        // Trash badge
        if (trashCount > 0) {
            DOM.trashBadge.textContent = trashCount;
            DOM.trashBadge.style.display = 'block';
            DOM.btnEmptyTrash.removeAttribute('disabled');
        } else {
            DOM.trashBadge.style.display = 'none';
            DOM.btnEmptyTrash.setAttribute('disabled', 'true');
        }

        DOM.trashCounts.forEach(el => el.textContent = trashCount);
    }

    renderFavorites() {
        DOM.favoritesGrid.innerHTML = '';
        const items = state.media.filter(item => state.favorites.includes(item.id));

        if (items.length === 0) {
            DOM.favoritesEmpty.style.display = 'flex';
            DOM.favoritesGrid.style.display = 'none';
            return;
        }

        DOM.favoritesEmpty.style.display = 'none';
        DOM.favoritesGrid.style.display = 'grid';

        items.forEach(item => {
            const div = document.createElement('div');
            div.className = 'grid-item';
            
            let elementHTML = '';
            if (item.type === 'image') {
                elementHTML = `<img src="${item.url}">`;
            } else {
                elementHTML = `<video src="${item.url}" muted></video>
                               <span class="badge"><i class="fas fa-play"></i></span>`;
            }

            div.innerHTML = `
                ${elementHTML}
                <button class="remove-btn" data-id="${item.id}" title="Olib tashlash">
                    <i class="fas fa-heart-broken"></i>
                </button>
            `;

            div.querySelector('.remove-btn').addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleFavorite(item.id);
                this.renderFavorites();
            });

            DOM.favoritesGrid.appendChild(div);
        });
    }

    renderTrash() {
        DOM.trashGrid.innerHTML = '';
        const items = state.media.filter(item => state.trash.includes(item.id));

        if (items.length === 0) {
            DOM.trashEmpty.style.display = 'flex';
            DOM.trashGrid.style.display = 'none';
            return;
        }

        DOM.trashEmpty.style.display = 'none';
        DOM.trashGrid.style.display = 'grid';

        items.forEach(item => {
            const div = document.createElement('div');
            div.className = 'grid-item';
            
            let elementHTML = '';
            if (item.type === 'image') {
                elementHTML = `<img src="${item.url}">`;
            } else {
                elementHTML = `<video src="${item.url}" muted></video>
                               <span class="badge"><i class="fas fa-play"></i></span>`;
            }

            div.innerHTML = `
                ${elementHTML}
                <button class="remove-btn" style="background: var(--accent-color)" data-id="${item.id}" title="Qaytarish">
                    <i class="fas fa-rotate-left"></i>
                </button>
            `;

            div.querySelector('.remove-btn').addEventListener('click', (e) => {
                e.stopPropagation();
                // Restore item
                state.trash = state.trash.filter(id => id !== item.id);
                this.showToast('Rasm qaytarildi', 'success');
                this.updateBadges();
                this.renderTrash();
            });

            DOM.trashGrid.appendChild(div);
        });
    }

    setupTrashEvents() {
        DOM.btnEmptyTrash.addEventListener('click', () => {
            DOM.deleteConfirmModal.classList.add('active');
        });

        DOM.btnModalCancel.addEventListener('click', () => {
            DOM.deleteConfirmModal.classList.remove('active');
        });

        DOM.btnModalConfirm.addEventListener('click', () => {
            DOM.deleteConfirmModal.classList.remove('active');
            
            // Perform simulated deletion
            const deletedCount = state.trash.length;
            
            // Filter deleted media out of local state
            state.media = state.media.filter(item => !state.trash.includes(item.id));
            state.trash = [];
            state.favorites = state.favorites.filter(id => state.media.some(m => m.id === id));
            
            this.updateBadges();
            this.renderTrash();
            this.renderStack();
            
            this.showToast(`${deletedCount} ta fayl butunlay o'chirildi!`, 'success');
        });
    }

    setupTutorial() {
        DOM.closeTutorial.addEventListener('click', () => {
            DOM.tutorialOverlay.style.animation = 'fadeIn 0.3s reverse';
            setTimeout(() => {
                DOM.tutorialOverlay.style.display = 'none';
                localStorage.setItem('photocheck_tutorial_shown', 'true');
            }, 300);
        });

        DOM.btnShowGuide.addEventListener('click', () => {
            this.showToast('Git-ga yuklash qo\'llanmasi README faylida!', 'info');
        });
    }

    resetDemo() {
        state.media = [
            {
                id: 'pic1',
                type: 'image',
                title: 'Tog\' etagidagi shafaq',
                url: 'assets/pic1.jpg',
                size: '3.4 MB',
                date: 'Bugun, 14:20'
            },
            {
                id: 'pic2',
                type: 'image',
                title: 'Kiberpank neon ko\'chalari',
                url: 'assets/pic2.jpg',
                size: '4.1 MB',
                date: 'Bugun, 11:05'
            },
            {
                id: 'vid1',
                type: 'video',
                title: 'Koinot kemasining uchishi',
                url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
                size: '8.2 MB',
                date: 'Kecha, 18:45'
            },
            {
                id: 'pic3',
                type: 'image',
                title: 'Mittivoy mushukcha',
                url: 'assets/pic3.jpg',
                size: '2.8 MB',
                date: 'Kecha, 09:15'
            },
            {
                id: 'pic4',
                type: 'image',
                title: 'Astronavt va tumanlik',
                url: 'assets/pic4.jpg',
                size: '5.3 MB',
                date: '28-iyul, 15:30'
            }
        ];
        state.favorites = [];
        state.trash = [];
        state.currentIndex = 0;
        
        this.updateBadges();
        this.renderStack();
        this.showScreen('sorter');
        this.showToast('Loyiha dastlabki holatga qaytarildi', 'info');
    }

    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        
        let icon = 'fa-info-circle';
        if (type === 'success') icon = 'fa-check-circle';
        if (type === 'danger') icon = 'fa-trash-alt';
        
        toast.innerHTML = `
            <i class="fas ${icon}"></i>
            <span>${message}</span>
        `;
        
        DOM.toastContainer.appendChild(toast);

        // Remove toast after animation
        setTimeout(() => {
            toast.style.animation = 'slideUpFade 0.3s reverse';
            setTimeout(() => toast.remove(), 300);
        }, 2200);
    }
}

// Start Application
let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new MediaSorterApp();
});
window.app = app; // Expose to global scope for HTML inline calls
