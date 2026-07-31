// App State
const state = {
    media: [
        {
            id: 'pic1',
            type: 'image',
            title: 'Tog\' etagidagi shafaq',
            folder: 'Camera',
            url: 'assets/pic1.jpg',
            sizeBytes: 3565158,
            size: '3.4 MB',
            date: 'Bugun, 14:20'
        },
        {
            id: 'pic2',
            type: 'image',
            title: 'Kiberpank neon ko\'chalari',
            folder: 'Screenshots',
            url: 'assets/pic2.jpg',
            sizeBytes: 4299161,
            size: '4.1 MB',
            date: 'Bugun, 11:05'
        },
        {
            id: 'vid1',
            type: 'video',
            title: 'Koinot kemasining uchishi',
            folder: 'Camera',
            url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
            sizeBytes: 8598323,
            size: '8.2 MB',
            duration: '00:15',
            date: 'Kecha, 18:45'
        },
        {
            id: 'pic3',
            type: 'image',
            title: 'Mittivoy mushukcha',
            folder: 'WhatsApp',
            url: 'assets/pic3.jpg',
            sizeBytes: 2936012,
            size: '2.8 MB',
            date: 'Kecha, 09:15'
        },
        {
            id: 'pic4',
            type: 'image',
            title: 'Astronavt va tumanlik',
            folder: 'Screenshots',
            url: 'assets/pic4.jpg',
            sizeBytes: 5557452,
            size: '5.3 MB',
            date: '28-iyul, 15:30'
        }
    ],
    favorites: [],
    trash: [],
    currentIndex: 0,
    selectedFolder: 'all',
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
    trashSizeInfo: document.getElementById('trash-size-info'),
    btnEmptyTrash: document.getElementById('btn-empty-trash'),
    deleteConfirmModal: document.getElementById('delete-confirm-modal'),
    btnModalCancel: document.getElementById('btn-modal-cancel'),
    btnModalConfirm: document.getElementById('btn-modal-confirm'),
    toastContainer: document.getElementById('toast-container'),
    navItems: document.querySelectorAll('.bottom-nav .nav-item'),
    screens: document.querySelectorAll('.app-screen'),
    folderChips: document.querySelectorAll('#folder-bar .chip'),
    // Lightbox
    lightboxModal: document.getElementById('lightbox-modal'),
    lightboxClose: document.getElementById('lightbox-close'),
    lightboxContent: document.getElementById('lightbox-content'),
    lightboxCaption: document.getElementById('lightbox-caption'),
    // Overlays
    overlayUp: document.querySelector('.swipe-up-overlay'),
    overlayDown: document.querySelector('.swipe-down-overlay'),
    overlayLeft: document.querySelector('.swipe-left-overlay'),
    overlayRight: document.querySelector('.swipe-right-overlay'),
    // Controls
    btnLeft: document.getElementById('btn-swipe-left'),
    btnRight: document.getElementById('btn-swipe-right'),
    btnUp: document.getElementById('btn-swipe-up'),
    btnDown: document.getElementById('btn-swipe-down'),
    // Stats
    statsTotalSize: document.getElementById('stats-total-size'),
    statsProgress: document.getElementById('stats-progress'),
    statsTrashSaving: document.getElementById('stats-trash-saving'),
    statTotalCount: document.getElementById('stat-total-count'),
    statFavCount: document.getElementById('stat-fav-count'),
    statTrashCount: document.getElementById('stat-trash-count')
};

class MediaSorterApp {
    constructor() {
        this.init();
    }

    init() {
        this.updateTime();
        setInterval(() => this.updateTime(), 60000);

        this.renderStack();
        this.setupNavigation();
        this.setupFolderChips();
        this.setupGestures();
        this.setupControlButtons();
        this.setupTrashEvents();
        this.setupLightboxEvents();
    }

    updateTime() {
        const now = new Date();
        const hrs = String(now.getHours()).padStart(2, '0');
        const mins = String(now.getMinutes()).padStart(2, '0');
        DOM.statusTime.textContent = `${hrs}:${mins}`;
    }

    setupFolderChips() {
        DOM.folderChips.forEach(chip => {
            chip.addEventListener('click', () => {
                DOM.folderChips.forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                state.selectedFolder = chip.getAttribute('data-folder');
                state.currentIndex = 0;
                this.renderStack();
            });
        });
    }

    setupNavigation() {
        DOM.navItems.forEach(item => {
            item.addEventListener('click', () => {
                const screenId = item.getAttribute('data-screen');
                this.showScreen(screenId);
            });
        });
    }

    setupLightboxEvents() {
        DOM.lightboxClose.addEventListener('click', () => {
            DOM.lightboxModal.classList.remove('active');
            DOM.lightboxContent.innerHTML = '';
        });
    }

    openLightbox(item) {
        DOM.lightboxContent.innerHTML = item.type === 'image' 
            ? `<img src="${item.url}" alt="${item.title}">` 
            : `<video src="${item.url}" controls autoplay></video>`;
        
        DOM.lightboxCaption.innerHTML = `<strong>${item.title}</strong> • Papka: ${item.folder} • Haçmi: ${item.size}`;
        DOM.lightboxModal.classList.add('active');
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
        if (screenId === 'analytics') this.renderAnalytics();
    }

    getAvailableMedia() {
        return state.media.filter(item => {
            const notTrash = !state.trash.includes(item.id);
            const matchesFolder = state.selectedFolder === 'all' || item.folder === state.selectedFolder;
            return notTrash && matchesFolder;
        });
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

        const cardsToShow = items.slice(state.currentIndex, state.currentIndex + 3);
        
        cardsToShow.forEach((item, index) => {
            const card = document.createElement('div');
            card.className = 'media-card';
            card.dataset.id = item.id;
            
            if (state.favorites.includes(item.id)) {
                card.classList.add('is-favorite');
            }

            let mediaHTML = item.type === 'image' 
                ? `<img src="${item.url}" alt="${item.title}">` 
                : `<video loop muted playsinline src="${item.url}"></video><div class="video-play-btn"><i class="fas fa-play"></i></div>`;

            card.innerHTML = `
                <div class="media-container">
                    ${mediaHTML}
                </div>
                <div class="media-info">
                    <div class="media-details">
                        <span class="media-title">${item.title}</span>
                        <div class="media-meta">
                            <span class="media-badge">📁 ${item.folder}</span>
                            <span>${item.size}</span>
                            ${item.duration ? `<span>• ${item.duration}</span>` : ''}
                        </div>
                    </div>
                    <div class="heart-icon"><i class="fas fa-heart"></i></div>
                </div>
            `;

            DOM.cardStack.appendChild(card);

            if (index === 0) {
                // Click top card to open Fullscreen Lightbox
                card.addEventListener('click', (e) => {
                    if (Math.abs(state.currentX) < 10 && Math.abs(state.currentY) < 10) {
                        this.openLightbox(item);
                    }
                });
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

            const rotate = state.currentX * 0.08;
            card.style.transform = `translate(${state.currentX}px, ${state.currentY}px) rotate(${rotate}deg)`;
            this.handleDragOverlays(state.currentX, state.currentY);
        };

        const handleEnd = () => {
            if (!state.isDragging) return;
            state.isDragging = false;
            const threshold = 100;
            const x = state.currentX;
            const y = state.currentY;

            this.resetOverlays();

            if (Math.abs(y) > Math.abs(x) && Math.abs(y) > threshold) {
                if (y < 0) this.swipeCard(card, 'up');
                else this.swipeCard(card, 'down');
            } else if (Math.abs(x) > threshold) {
                if (x > 0) this.swipeCard(card, 'right');
                else this.swipeCard(card, 'left');
            } else {
                card.style.transition = 'transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.25)';
                card.style.transform = 'translate(0px, 0px) rotate(0deg)';
            }
        };

        card.addEventListener('mousedown', handleStart);
        window.addEventListener('mousemove', handleMove);
        window.addEventListener('mouseup', handleEnd);
        card.addEventListener('touchstart', handleStart, { passive: true });
        window.addEventListener('touchmove', handleMove, { passive: false });
        window.addEventListener('touchend', handleEnd);
    }

    handleDragOverlays(x, y) {
        const threshold = 40;
        this.resetOverlays();

        if (Math.abs(y) > Math.abs(x)) {
            if (y < -threshold) DOM.overlayUp.style.opacity = Math.min(Math.abs(y) / 150, 0.9);
            else if (y > threshold) DOM.overlayDown.style.opacity = Math.min(Math.abs(y) / 150, 0.9);
        } else {
            if (x < -threshold) DOM.overlayLeft.style.opacity = Math.min(Math.abs(x) / 150, 0.9);
            else if (x > threshold) DOM.overlayRight.style.opacity = Math.min(Math.abs(x) / 150, 0.9);
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

        setTimeout(() => {
            const items = this.getAvailableMedia();
            if (direction === 'left' || direction === 'up' || direction === 'down') {
                if (state.currentIndex < items.length - 1) state.currentIndex++;
                else if (items.length > 0) state.currentIndex = 0;
            } else if (direction === 'right') {
                if (state.currentIndex > 0) state.currentIndex--;
                else items.length > 0 ? state.currentIndex = items.length - 1 : 0;
            }

            state.currentX = 0;
            state.currentY = 0;

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
            this.showToast('Savatga o\'tkazildi 🗑', 'danger');
            const favIndex = state.favorites.indexOf(id);
            if (favIndex !== -1) state.favorites.splice(favIndex, 1);
        }
        this.updateBadges();
    }

    updateBadges() {
        const favCount = state.favorites.length;
        const trashCount = state.trash.length;

        if (favCount > 0) {
            DOM.favBadge.textContent = favCount;
            DOM.favBadge.style.display = 'block';
        } else {
            DOM.favBadge.style.display = 'none';
        }

        if (trashCount > 0) {
            DOM.trashBadge.textContent = trashCount;
            DOM.trashBadge.style.display = 'block';
            DOM.btnEmptyTrash.removeAttribute('disabled');
        } else {
            DOM.trashBadge.style.display = 'none';
            DOM.btnEmptyTrash.setAttribute('disabled', 'true');
        }

        DOM.trashCounts.forEach(el => el.textContent = trashCount);

        const trashBytes = state.media
            .filter(item => state.trash.includes(item.id))
            .reduce((sum, item) => sum + item.sizeBytes, 0);
        const trashMb = (trashBytes / (1024 * 1024)).toFixed(1);
        DOM.trashSizeInfo.textContent = `${trashMb} MB joy bo'shaydi`;
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
            
            let elementHTML = item.type === 'image' 
                ? `<img src="${item.url}">` 
                : `<video src="${item.url}" muted></video><span class="badge"><i class="fas fa-play"></i></span>`;

            div.innerHTML = `
                ${elementHTML}
                <button class="remove-btn" title="Olib tashlash">
                    <i class="fas fa-times"></i>
                </button>
            `;

            div.addEventListener('click', () => this.openLightbox(item));

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
            
            let elementHTML = item.type === 'image' 
                ? `<img src="${item.url}">` 
                : `<video src="${item.url}" muted></video><span class="badge"><i class="fas fa-play"></i></span>`;

            div.innerHTML = `
                ${elementHTML}
                <button class="restore-btn" title="Orqaga qaytarish">
                    <i class="fas fa-rotate-left"></i>
                </button>
            `;

            div.addEventListener('click', () => this.openLightbox(item));

            div.querySelector('.restore-btn').addEventListener('click', (e) => {
                e.stopPropagation();
                state.trash = state.trash.filter(id => id !== item.id);
                this.showToast('Rasm savatdan qaytarildi ↺', 'success');
                this.updateBadges();
                this.renderTrash();
                this.renderStack();
            });

            DOM.trashGrid.appendChild(div);
        });
    }

    renderAnalytics() {
        const totalBytes = state.media.reduce((sum, item) => sum + item.sizeBytes, 0);
        const trashBytes = state.media
            .filter(item => state.trash.includes(item.id))
            .reduce((sum, item) => sum + item.sizeBytes, 0);

        const totalMb = (totalBytes / (1024 * 1024)).toFixed(1);
        const trashMb = (trashBytes / (1024 * 1024)).toFixed(1);
        const percentage = totalBytes > 0 ? (trashBytes / totalBytes) * 100 : 0;

        DOM.statsTotalSize.textContent = `${totalMb} MB`;
        DOM.statsProgress.style.width = `${percentage}%`;
        DOM.statsTrashSaving.textContent = `O'chirish navbatidagi joy: ${trashMb} MB (${percentage.toFixed(0)}%)`;
        
        DOM.statTotalCount.textContent = state.media.length;
        DOM.statFavCount.textContent = state.favorites.length;
        DOM.statTrashCount.textContent = state.trash.length;
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
            const deletedCount = state.trash.length;
            
            state.media = state.media.filter(item => !state.trash.includes(item.id));
            state.trash = [];
            state.favorites = state.favorites.filter(id => state.media.some(m => m.id === id));
            
            this.updateBadges();
            this.renderTrash();
            this.renderStack();
            this.showToast(`${deletedCount} ta fayl butunlay o'chirildi!`, 'success');
        });
    }

    resetDemo() {
        state.trash = [];
        state.favorites = [];
        state.currentIndex = 0;
        this.updateBadges();
        this.renderStack();
        this.showScreen('sorter');
        this.showToast('Loyiha dastlabki holatga qaytarildi', 'info');
    }

    showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        let icon = type === 'success' ? 'fa-check-circle' : type === 'danger' ? 'fa-trash-alt' : 'fa-info-circle';
        toast.innerHTML = `<i class="fas ${icon}"></i><span>${message}</span>`;
        DOM.toastContainer.appendChild(toast);
        setTimeout(() => {
            toast.style.opacity = '0';
            setTimeout(() => toast.remove(), 300);
        }, 2200);
    }
}

let app;
document.addEventListener('DOMContentLoaded', () => {
    app = new MediaSorterApp();
});
window.app = app;
