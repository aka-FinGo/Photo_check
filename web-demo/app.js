// PhotoCheck Kids & Pro Web Prototype State
const state = {
    isKidsMode: true,
    whitelistedFolders: ['Cartoons', 'Animals', 'Camera'],
    timerMinutes: 30,
    remainingSeconds: 30 * 60,
    isTimerLocked: false,
    selectedFolder: 'all',
    viewingKidIndex: null,
    pendingAuthAction: null,
    activeScreen: 'kids-gallery',
    
    // Sample media database
    media: [
        {
            id: 'k1',
            type: 'image',
            title: 'Qiziqarli Tom va Jerri',
            folder: 'Cartoons',
            url: 'assets/pic1.jpg',
            size: '3.4 MB',
            date: 'Bugun, 14:20'
        },
        {
            id: 'k2',
            type: 'image',
            title: 'Mittivoy Mushukcha',
            folder: 'Animals',
            url: 'assets/pic3.jpg',
            size: '2.8 MB',
            date: 'Kecha, 09:15'
        },
        {
            id: 'k3',
            type: 'video',
            title: 'Koinot kemasining parvozi',
            folder: 'Cartoons',
            url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
            size: '8.2 MB',
            duration: '00:15',
            date: 'Kecha, 18:45'
        },
        {
            id: 'k4',
            type: 'image',
            title: 'Sehrli Tabiat Manzarasi',
            folder: 'Camera',
            url: 'assets/pic2.jpg',
            size: '4.1 MB',
            date: 'Bugun, 11:05'
        },
        {
            id: 'k5',
            type: 'image',
            title: 'Kichik Kosmonavt',
            folder: 'Cartoons',
            url: 'assets/pic4.jpg',
            size: '5.3 MB',
            date: '28-iyul, 15:30'
        },
        {
            id: 'p1',
            type: 'image',
            title: 'Shaxsiy Hujjat',
            folder: 'WhatsApp',
            url: 'assets/pic1.jpg',
            size: '1.2 MB',
            date: '20-iyul, 10:00'
        }
    ],
    favorites: [],
    trash: [],
    currentIndex: 0
};

class PhotoCheckApp {
    constructor() {
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.startTimer();
        this.renderKidsGallery();
        this.renderCardStack();
        this.updateTime();
        setInterval(() => this.updateTime(), 30000);
    }

    updateTime() {
        const now = new Date();
        const timeStr = now.toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' });
        const el = document.getElementById('status-time');
        if (el) el.textContent = timeStr;
    }

    // Live Screen Time Timer
    startTimer() {
        if (this.timerInterval) clearInterval(this.timerInterval);

        this.timerInterval = setInterval(() => {
            if (state.isKidsMode && state.timerMinutes > 0 && !state.isTimerLocked) {
                if (state.remainingSeconds > 0) {
                    state.remainingSeconds--;
                    this.updateTimerDisplay();
                } else {
                    this.triggerSleepLock();
                }
            }
        }, 1000);
    }

    updateTimerDisplay() {
        const mins = Math.floor(state.remainingSeconds / 60);
        const secs = state.remainingSeconds % 60;
        const formatted = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
        const pillText = document.getElementById('kids-timer-text');
        if (pillText) pillText.textContent = formatted;
    }

    triggerSleepLock() {
        state.isTimerLocked = true;
        this.showScreen('kids-sleep');
    }

    setupEventListeners() {
        // Parent Shield click (triggers Biometrics)
        document.getElementById('btn-parent-shield')?.addEventListener('click', () => {
            this.requestBiometricAuth('Ota-ona Sozlamalari', () => {
                this.showScreen('parent-settings');
            });
        });

        // Sleep screen unlock
        document.getElementById('btn-sleep-unlock')?.addEventListener('click', () => {
            this.requestBiometricAuth('Taymerni Ochish', () => {
                state.isTimerLocked = false;
                state.remainingSeconds = state.timerMinutes * 60;
                this.showScreen('kids-gallery');
                this.showToast('Taymer qayta ishga tushirildi! ✨');
            });
        });

        // Biometric Modal buttons
        document.getElementById('btn-bio-cancel')?.addEventListener('click', () => {
            this.closeBiometricModal();
        });

        document.getElementById('btn-bio-confirm')?.addEventListener('click', () => {
            this.confirmBiometricModal();
        });

        // Close Parent Settings
        document.getElementById('btn-close-parent-settings')?.addEventListener('click', () => {
            if (state.isKidsMode) {
                this.showScreen('kids-gallery');
            } else {
                this.showScreen('sorter');
            }
        });

        // Kids Mode switch in settings
        document.getElementById('toggle-kids-mode')?.addEventListener('change', (e) => {
            state.isKidsMode = e.target.checked;
            this.showToast(state.isKidsMode ? 'Bolalar rejimi yoqildi 👶' : 'Bolalar rejimi o\'chirildi 🛡️');
        });

        // Launch Pro Mode button
        document.getElementById('btn-launch-pro')?.addEventListener('click', () => {
            state.isKidsMode = false;
            const toggle = document.getElementById('toggle-kids-mode');
            if (toggle) toggle.checked = false;
            this.switchToProMode();
        });

        // Lock back to Kids Mode
        document.getElementById('btn-lock-to-kids')?.addEventListener('click', () => {
            state.isKidsMode = true;
            this.switchToKidsMode();
            this.showToast('Ilova Bolalar rejimiga qulflandi! 🎈');
        });

        // Pro settings gear
        document.getElementById('btn-pro-settings')?.addEventListener('click', () => {
            this.showScreen('parent-settings');
        });

        // Timer chips
        document.querySelectorAll('.timer-chip').forEach(chip => {
            chip.addEventListener('click', () => {
                document.querySelectorAll('.timer-chip').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                const mins = parseInt(chip.dataset.min, 10);
                state.timerMinutes = mins;
                state.remainingSeconds = mins * 60;
                state.isTimerLocked = false;
                this.updateTimerDisplay();
                this.showToast(`Taymer ${mins > 0 ? mins + ' daqiqaga' : 'cheksiz'} o'rnatildi`);
            });
        });

        // Reset timer button
        document.getElementById('btn-reset-timer')?.addEventListener('click', () => {
            state.remainingSeconds = state.timerMinutes * 60;
            state.isTimerLocked = false;
            this.updateTimerDisplay();
            this.showToast('Taymer qayta boshlandi');
        });

        // Whitelisted album checkboxes
        document.querySelectorAll('.album-checklist input[type="checkbox"]').forEach(cb => {
            cb.addEventListener('change', () => {
                const checked = Array.from(document.querySelectorAll('.album-checklist input[type="checkbox"]:checked')).map(el => el.value);
                state.whitelistedFolders = checked;
                this.renderKidsGallery();
            });
        });

        // Kids Folder filter chips
        document.querySelectorAll('#kids-folder-bar .chip').forEach(chip => {
            chip.addEventListener('click', () => {
                document.querySelectorAll('#kids-folder-bar .chip').forEach(c => c.classList.remove('active'));
                chip.classList.add('active');
                state.selectedFolder = chip.dataset.folder;
                this.renderKidsGallery();
            });
        });

        // Kids Fullscreen Viewer Navigation
        document.getElementById('kids-viewer-close')?.addEventListener('click', () => {
            document.getElementById('kids-viewer-modal')?.classList.remove('active');
        });

        document.getElementById('btn-kid-prev')?.addEventListener('click', () => {
            const list = this.getFilteredKidsMedia();
            if (state.viewingKidIndex > 0) {
                state.viewingKidIndex--;
                this.showKidsFullscreen(list[state.viewingKidIndex]);
            }
        });

        document.getElementById('btn-kid-next')?.addEventListener('click', () => {
            const list = this.getFilteredKidsMedia();
            if (state.viewingKidIndex < list.length - 1) {
                state.viewingKidIndex++;
                this.showKidsFullscreen(list[state.viewingKidIndex]);
            }
        });

        // Pro Bottom Nav items
        document.querySelectorAll('#pro-bottom-nav .nav-item').forEach(btn => {
            btn.addEventListener('click', () => {
                document.querySelectorAll('#pro-bottom-nav .nav-item').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                this.showScreen(btn.dataset.screen);
            });
        });

        // Pro Floating swipe buttons
        document.getElementById('btn-swipe-up')?.addEventListener('click', () => this.swipe('up'));
        document.getElementById('btn-swipe-down')?.addEventListener('click', () => this.swipe('down'));
        document.getElementById('btn-swipe-left')?.addEventListener('click', () => this.swipe('left'));
        document.getElementById('btn-swipe-right')?.addEventListener('click', () => this.swipe('right'));
    }

    getFilteredKidsMedia() {
        return state.media.filter(item => {
            const allowedInWhitelist = state.whitelistedFolders.includes(item.folder);
            const matchesFilter = state.selectedFolder === 'all' || item.folder.toLowerCase() === state.selectedFolder.toLowerCase();
            return allowedInWhitelist && matchesFilter;
        });
    }

    renderKidsGallery() {
        const grid = document.getElementById('kids-grid');
        if (!grid) return;

        const items = this.getFilteredKidsMedia();
        grid.innerHTML = '';

        if (items.length === 0) {
            grid.innerHTML = `
                <div style="grid-column: span 2; text-align: center; padding: 40px 10px; color: #94a3b8;">
                    <div style="font-size: 48px; margin-bottom: 10px;">🧸</div>
                    <h3 style="color: #fff; margin-bottom: 6px;">Rasm yoki videolar yo'q</h3>
                    <p style="font-size: 13px;">Ota-onangiz ruxsat bergan albomlar shu yerda ko'rinadi.</p>
                </div>
            `;
            return;
        }

        items.forEach((item, index) => {
            const card = document.createElement('div');
            card.className = 'kid-card';
            
            let mediaContent = '';
            if (item.type === 'video') {
                mediaContent = `
                    <video src="${item.url}" muted></video>
                    <div class="video-badge"><i class="fas fa-play"></i> ${item.duration || 'Video'}</div>
                `;
            } else {
                mediaContent = `<img src="${item.url}" alt="${item.title}" loading="lazy">`;
            }

            card.innerHTML = mediaContent;
            card.addEventListener('click', () => {
                state.viewingKidIndex = index;
                this.showKidsFullscreen(item);
            });

            grid.appendChild(card);
        });
    }

    showKidsFullscreen(item) {
        const modal = document.getElementById('kids-viewer-modal');
        const content = document.getElementById('kids-viewer-content');
        if (!modal || !content) return;

        if (item.type === 'video') {
            content.innerHTML = `<video src="${item.url}" controls autoplay style="width: 100%; max-height: 80vh;"></video>`;
        } else {
            content.innerHTML = `<img src="${item.url}" alt="${item.title}" style="max-width: 100%; max-height: 80vh; object-fit: contain;">`;
        }

        modal.classList.add('active');
    }

    requestBiometricAuth(actionTitle, onSuccess) {
        state.pendingAuthAction = onSuccess;
        const modal = document.getElementById('biometric-modal');
        if (modal) modal.classList.add('active');
    }

    closeBiometricModal() {
        const modal = document.getElementById('biometric-modal');
        if (modal) modal.classList.remove('active');
        state.pendingAuthAction = null;
    }

    confirmBiometricModal() {
        const dots = document.querySelectorAll('.pin-dot');
        dots.forEach(d => d.classList.add('filled'));

        setTimeout(() => {
            this.closeBiometricModal();
            dots.forEach(d => d.classList.remove('filled'));
            if (state.pendingAuthAction) {
                state.pendingAuthAction();
                state.pendingAuthAction = null;
            }
        }, 300);
    }

    switchToKidsMode() {
        state.isKidsMode = true;
        document.getElementById('kids-header')?.classList.remove('hidden');
        document.getElementById('kids-folder-bar')?.classList.remove('hidden');
        document.getElementById('pro-header')?.classList.add('hidden');
        document.getElementById('pro-bottom-nav')?.classList.add('hidden');
        this.renderKidsGallery();
        this.showScreen('kids-gallery');
    }

    switchToProMode() {
        state.isKidsMode = false;
        document.getElementById('kids-header')?.classList.add('hidden');
        document.getElementById('kids-folder-bar')?.classList.add('hidden');
        document.getElementById('pro-header')?.classList.remove('hidden');
        document.getElementById('pro-bottom-nav')?.classList.remove('hidden');
        this.showScreen('sorter');
    }

    showScreen(screenName) {
        document.querySelectorAll('.app-screen').forEach(s => s.classList.add('hidden'));
        document.querySelectorAll('.app-screen').forEach(s => s.classList.remove('active'));

        const target = document.getElementById(`screen-${screenName}`);
        if (target) {
            target.classList.remove('hidden');
            target.classList.add('active');
            state.activeScreen = screenName;
        }
    }

    renderCardStack() {
        const stack = document.getElementById('card-stack');
        if (!stack) return;
        stack.innerHTML = '';

        const activeMedia = state.media.filter(m => !state.trash.includes(m.id));
        if (activeMedia.length === 0) {
            document.getElementById('empty-state')?.classList.add('active');
            return;
        }

        const current = activeMedia[state.currentIndex % activeMedia.length];
        const card = document.createElement('div');
        card.className = 'card-item';
        card.innerHTML = current.type === 'video' ? `<video src="${current.url}" controls></video>` : `<img src="${current.url}">`;
        stack.appendChild(card);
    }

    swipe(direction) {
        if (direction === 'up') {
            this.showToast('Sevimlilarga qo\'shildi ❤️');
        } else if (direction === 'down') {
            this.showToast('Savatga o\'tkazildi 🗑️');
        }
        state.currentIndex++;
        this.renderCardStack();
    }

    showToast(msg) {
        const container = document.getElementById('toast-container');
        if (!container) return;
        const toast = document.createElement('div');
        toast.style.cssText = 'background: rgba(30, 41, 59, 0.95); color: #fff; padding: 10px 18px; border-radius: 12px; margin-top: 8px; font-size: 13px; font-weight: 700; border: 1px solid rgba(255,255,255,0.1); box-shadow: 0 4px 12px rgba(0,0,0,0.5);';
        toast.textContent = msg;
        container.appendChild(toast);
        setTimeout(() => toast.remove(), 2500);
    }
}

// Instantiate
const app = new PhotoCheckApp();
