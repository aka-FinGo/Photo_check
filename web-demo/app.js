// PhotoCheck Kids & 1:1 Slidebox Pro Web App
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
    
    // Media database
    media: [
        {
            id: 'm1',
            type: 'image',
            title: 'Qiziqarli Tom va Jerri',
            folder: 'Cartoons',
            url: 'assets/pic1.jpg',
            size: '3.4 MB',
            date: 'Bugun, 14:20',
            isFavorite: false
        },
        {
            id: 'm2',
            type: 'image',
            title: 'Kiberpank Neon Shahri',
            folder: 'Camera',
            url: 'assets/pic2.jpg',
            size: '4.1 MB',
            date: 'Bugun, 11:05',
            isFavorite: false
        },
        {
            id: 'm3',
            type: 'video',
            title: 'Koinot kemasining parvozi',
            folder: 'Cartoons',
            url: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4',
            size: '8.2 MB',
            duration: '00:15',
            date: 'Kecha, 18:45',
            isFavorite: false
        },
        {
            id: 'm4',
            type: 'image',
            title: 'Mittivoy Mushukcha',
            folder: 'Animals',
            url: 'assets/pic3.jpg',
            size: '2.8 MB',
            date: 'Kecha, 09:15',
            isFavorite: true
        },
        {
            id: 'm5',
            type: 'image',
            title: 'Kichik Kosmonavt',
            folder: 'Cartoons',
            url: 'assets/pic4.jpg',
            size: '5.3 MB',
            date: '28-iyul, 15:30',
            isFavorite: false
        },
        {
            id: 'm6',
            type: 'image',
            title: 'Oila bilan sayohat',
            folder: 'Camera',
            url: 'assets/pic2.jpg',
            size: '3.9 MB',
            date: '20-iyul, 10:00',
            isFavorite: false
        }
    ],

    // User created custom albums for quick sorting
    userAlbums: ["Ta'til", "Oila", "Do'stlar", "Tabiat"],
    
    // Trash and undo action history
    trash: [],
    undoStack: [],
    currentIndex: 0,
    selectedProAlbumFilter: 'BARCHA FAYLLAR'
};

class PhotoCheckApp {
    constructor() {
        this.init();
    }

    init() {
        this.setupEventListeners();
        this.startTimer();
        this.renderKidsGallery();
        this.renderAlbumChecklist();
        this.renderSlideboxTray();
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

    // Live Screen Time Countdown
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
        // Parent Shield click
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

        // Kids Mode toggle in settings
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

        // Open Donate buttons
        document.getElementById('btn-open-donate-kids')?.addEventListener('click', () => this.openDonateScreen());
        document.getElementById('btn-open-donate-pro')?.addEventListener('click', () => this.openDonateScreen());

        // Drawer Hamburger
        document.getElementById('btn-open-drawer')?.addEventListener('click', () => this.openDrawer());
        document.getElementById('drawer-overlay')?.addEventListener('click', (e) => {
            if (e.target.id === 'drawer-overlay') this.closeDrawer();
        });

        // Check update hub simulator
        document.getElementById('btn-check-update-hub')?.addEventListener('click', () => {
            const statusEl = document.getElementById('update-hub-status');
            if (statusEl) statusEl.textContent = 'Tekshirilmoqda... ⏳';
            setTimeout(() => {
                if (statusEl) statusEl.textContent = 'v1.0.03 • Eng so\'nggi versiya o\'rnatilgan ✅';
                this.showToast('Ilova eng so\'nggi versiyada! ✅');
            }, 1000);
        });

        // Screen Pinning (Kiosk Mode) Simulator
        document.getElementById('btn-toggle-kiosk')?.addEventListener('click', () => {
            this.requestBiometric('Ilovani Ekranga Qadash / Bekor Qilish', () => {
                state.isKioskPinned = !state.isKioskPinned;
                this.updateKioskUI();
                if (state.isKioskPinned) {
                    this.showToast('Ilova ekranga qadandi (Kiosk Rejimi) 📌');
                } else {
                    this.showToast('Ekranni qadash bekor qilindi 🔓');
                }
            });
        });

        // Kids Header Kiosk Toggle Button (Alternating Pin / Unpin with Biometrics)
        document.getElementById('btn-kids-kiosk-toggle')?.addEventListener('click', () => {
            this.requestBiometric('Ilovani Ekranga Qadash / Bekor Qilish', () => {
                state.isKioskPinned = !state.isKioskPinned;
                this.updateKioskUI();
                if (state.isKioskPinned) {
                    this.showToast('Ilova ekranga qadandi (Kiosk Rejimi) 📌');
                } else {
                    this.showToast('Ekranni qadash bekor qilindi 🔓');
                }
            });
        });

        // Ribbon Quick Unpin button
        document.getElementById('btn-ribbon-unpin')?.addEventListener('click', () => {
            this.requestBiometric('Qadashni Bekor Qilish', () => {
                state.isKioskPinned = false;
                this.updateKioskUI();
                this.showToast('Ekranni qadash bekor qilindi 🔓');
            });
        });

        // Bulk Album Selection (Select All / Deselect All)
        document.getElementById('btn-select-all-albums')?.addEventListener('click', () => {
            const allFolders = Array.from(new Set(state.media.map(m => m.folder)));
            state.whitelistedFolders = allFolders;
            this.renderAlbumChecklist();
            this.renderKidsGallery();
            this.updateBentoStats();
            this.showToast('Barcha albomlar belgilandi ✅');
        });

        document.getElementById('btn-clear-all-albums')?.addEventListener('click', () => {
            state.whitelistedFolders = [];
            this.renderAlbumChecklist();
            this.renderKidsGallery();
            this.updateBentoStats();
            this.showToast('Barcha albomlar bekor qilindi ❌');
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
            this.closeKidsFullscreen();
        });

        // Close when clicking modal backdrop
        document.getElementById('kids-viewer-modal')?.addEventListener('click', (e) => {
            if (e.target === document.getElementById('kids-viewer-modal')) {
                this.closeKidsFullscreen();
            }
        });

        // Keyboard navigation (Escape to close, Arrow keys to navigate)
        document.addEventListener('keydown', (e) => {
            const modal = document.getElementById('kids-viewer-modal');
            if (!modal || !modal.classList.contains('active')) return;

            if (e.key === 'Escape') {
                this.closeKidsFullscreen();
            } else if (e.key === 'ArrowRight') {
                const list = this.getFilteredKidsMedia();
                if (state.viewingKidIndex < list.length - 1) {
                    state.viewingKidIndex++;
                    this.showKidsFullscreen(list[state.viewingKidIndex]);
                }
            } else if (e.key === 'ArrowLeft') {
                const list = this.getFilteredKidsMedia();
                if (state.viewingKidIndex > 0) {
                    state.viewingKidIndex--;
                    this.showKidsFullscreen(list[state.viewingKidIndex]);
                }
            }
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

        // Slidebox Undo, Trash & Favorite buttons
        document.getElementById('btn-slide-undo')?.addEventListener('click', () => this.performUndo());
        document.getElementById('btn-slide-trash')?.addEventListener('click', () => this.trashCurrentCard());
        document.getElementById('btn-slide-fav')?.addEventListener('click', () => this.toggleCurrentFavorite());

        // Top Bar Trash button
        document.getElementById('btn-open-trash')?.addEventListener('click', () => this.openTrashModal());
        document.getElementById('btn-close-trash-modal')?.addEventListener('click', () => this.closeTrashModal());
        document.getElementById('btn-empty-all-trash')?.addEventListener('click', () => this.emptyTrash());
        document.getElementById('btn-restore-all-trash')?.addEventListener('click', () => this.restoreAllTrash());

        // Album filter dropdown in Pro mode
        document.getElementById('btn-album-filter')?.addEventListener('click', () => this.openAlbumFilterModal());
        document.getElementById('btn-close-filter-modal')?.addEventListener('click', () => this.closeAlbumFilterModal());

        // Create Album Modal
        document.getElementById('btn-cancel-create-album')?.addEventListener('click', () => {
            document.getElementById('create-album-modal')?.classList.remove('active');
        });

        document.getElementById('btn-confirm-create-album')?.addEventListener('click', () => {
            const input = document.getElementById('new-album-name-input');
            const name = input ? input.value.trim() : '';
            if (name) {
                if (!state.userAlbums.includes(name)) {
                    state.userAlbums.push(name);
                    this.renderSlideboxTray();
                    this.showToast(`"${name}" albomi yaratildi! 📁`);
                }
                if (input) input.value = '';
                document.getElementById('create-album-modal')?.classList.remove('active');
            }
        });
    }

    // Filtered Kids Media
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
            card.dataset.index = index;
            card.setAttribute('data-index', index);
            
            let mediaContent = '';
            if (item.type === 'video') {
                mediaContent = `
                    <video src="${item.url}#t=1.0" preload="metadata" muted playsinline style="width: 100%; height: 100%; object-fit: cover;"></video>
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
        const counter = document.getElementById('kids-viewer-counter');
        const list = this.getFilteredKidsMedia();
        if (!modal || !content) return;

        if (counter) {
            counter.textContent = `${state.viewingKidIndex + 1} / ${list.length}`;
        }

        if (item.type === 'video') {
            content.innerHTML = `
                <div class="video-player-container">
                    <video id="kids-active-video" src="${item.url}" playsinline autoplay loop></video>
                    <div class="video-controls-overlay">
                        <div class="video-timeline-row">
                            <span class="video-time-label" id="vid-curr-time">00:00</span>
                            <input type="range" class="video-timeline-slider" id="vid-timeline" min="0" max="100" value="0">
                            <span class="video-time-label" id="vid-total-time">00:00</span>
                        </div>
                        <div class="video-btns-row">
                            <button class="btn-video-ctrl" id="btn-vid-rewind" title="10s ortga"><i class="fas fa-backward"></i></button>
                            <button class="btn-video-ctrl btn-play-toggle" id="btn-vid-play" title="Play/Pause"><i class="fas fa-pause"></i></button>
                            <button class="btn-video-ctrl" id="btn-vid-forward" title="10s oldinga"><i class="fas fa-forward"></i></button>
                        </div>
                    </div>
                </div>
            `;

            const video = document.getElementById('kids-active-video');
            const timeline = document.getElementById('vid-timeline');
            const currTimeLabel = document.getElementById('vid-curr-time');
            const totalTimeLabel = document.getElementById('vid-total-time');
            const playBtn = document.getElementById('btn-vid-play');
            const rewindBtn = document.getElementById('btn-vid-rewind');
            const forwardBtn = document.getElementById('btn-vid-forward');

            const formatSec = (sec) => {
                if (isNaN(sec) || sec < 0) return "00:00";
                const m = Math.floor(sec / 60);
                const s = Math.floor(sec % 60);
                return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
            };

            if (video && timeline) {
                video.addEventListener('loadedmetadata', () => {
                    timeline.max = Math.floor(video.duration);
                    if (totalTimeLabel) totalTimeLabel.textContent = formatSec(video.duration);
                });

                video.addEventListener('timeupdate', () => {
                    timeline.value = Math.floor(video.currentTime);
                    if (currTimeLabel) currTimeLabel.textContent = formatSec(video.currentTime);
                    if (totalTimeLabel && video.duration) totalTimeLabel.textContent = formatSec(video.duration);
                });

                timeline.addEventListener('input', () => {
                    video.currentTime = Number(timeline.value);
                });

                playBtn?.addEventListener('click', () => {
                    if (video.paused) {
                        video.play();
                        playBtn.innerHTML = '<i class="fas fa-pause"></i>';
                    } else {
                        video.pause();
                        playBtn.innerHTML = '<i class="fas fa-play"></i>';
                    }
                });

                rewindBtn?.addEventListener('click', () => {
                    video.currentTime = Math.max(0, video.currentTime - 10);
                });

                forwardBtn?.addEventListener('click', () => {
                    video.currentTime = Math.min(video.duration || 100, video.currentTime + 10);
                });
            }
        } else {
            content.classList.remove('is-zoomed');
            content.innerHTML = `<img src="${item.url}" alt="${item.title}">`;
        }

        // Double tap, Double click & Pinch Zoom for Images
        let currentScale = 1;
        let startScale = 1;
        let initialPinchDist = 0;
        let lastTapTime = 0;
        let lastTapX = 0;
        let lastTapY = 0;
        let lastTouchZoomTime = 0;
        let startX = 0;
        let startY = 0;
        let isDragging = false;
        let isSwiping = false;

        const imgEl = content.querySelector('img');

        // Smooth Zoom Toggle Function
        const toggleZoom = (clientX, clientY) => {
            if (!imgEl) return;

            // Enforce smooth CSS transition on transform
            imgEl.style.transition = 'transform 0.25s cubic-bezier(0.2, 0, 0, 1)';

            if (currentScale > 1.2) {
                // If image is already zoomed (currentScale > 1.2), smoothly zoom out to scale 1 (centered)
                currentScale = 1;
                imgEl.style.transform = 'scale(1)';
                content.classList.remove('is-zoomed');
            } else {
                // If image is not zoomed (currentScale <= 1.2), smoothly zoom in to scale 2.5 centered on click position
                const rect = imgEl.getBoundingClientRect();
                let originX = 50;
                let originY = 50;
                if (rect.width > 0 && rect.height > 0) {
                    originX = Math.max(0, Math.min(100, ((clientX - rect.left) / rect.width) * 100));
                    originY = Math.max(0, Math.min(100, ((clientY - rect.top) / rect.height) * 100));
                }
                imgEl.style.transformOrigin = `${originX}% ${originY}%`;
                currentScale = 2.5;
                imgEl.style.transform = 'scale(2.5)';
                content.classList.add('is-zoomed');
            }
        };

        if (imgEl) {
            imgEl.draggable = false;
            imgEl.ondragstart = (e) => e.preventDefault();
            imgEl.style.transition = 'transform 0.25s cubic-bezier(0.2, 0, 0, 1)';
            imgEl.style.transformOrigin = 'center center';
            imgEl.style.userSelect = 'none';
            imgEl.style.webkitUserDrag = 'none';

            // Reset origin to center when zoom-out transition completes
            imgEl.addEventListener('transitionend', (e) => {
                if (e.propertyName === 'transform' && currentScale === 1) {
                    imgEl.style.transformOrigin = 'center center';
                }
            });

            // Desktop double-click zoom toggle
            content.ondblclick = (e) => {
                // Prevent duplicate trigger if double-tap just fired on touch devices
                if (Date.now() - lastTouchZoomTime < 600) return;
                toggleZoom(e.clientX, e.clientY);
            };

            // Desktop mouse wheel zoom
            content.onwheel = (e) => {
                e.preventDefault();
                imgEl.style.transition = 'none';
                currentScale += e.deltaY * -0.002;
                currentScale = Math.min(Math.max(1, currentScale), 4.5);
                imgEl.style.transform = `scale(${currentScale})`;
                if (currentScale > 1.2) {
                    content.classList.add('is-zoomed');
                } else {
                    content.classList.remove('is-zoomed');
                    if (currentScale === 1) imgEl.style.transformOrigin = 'center center';
                }
            };
        }

        // Touch Gestures (Pinch-zoom, Double-tap, Horizontal swipe, Background tap)
        const handleTouchStart = (e) => {
            if (e.touches && e.touches.length === 2 && imgEl) {
                // 2-finger pinch start
                isSwiping = false;
                isDragging = false;
                initialPinchDist = Math.hypot(
                    e.touches[0].clientX - e.touches[1].clientX,
                    e.touches[0].clientY - e.touches[1].clientY
                );
                startScale = currentScale;
                return;
            }

            if (e.touches && e.touches.length > 1) return;

            const touch = e.touches ? e.touches[0] : e;
            const now = Date.now();

            if (e.touches && imgEl) {
                const timesince = now - lastTapTime;
                const distFromLast = Math.hypot(touch.clientX - lastTapX, touch.clientY - lastTapY);

                if (timesince < 300 && timesince > 40 && distFromLast < 50) {
                    // Double tap zoom toggle
                    lastTapTime = 0;
                    lastTouchZoomTime = now;
                    toggleZoom(touch.clientX, touch.clientY);
                    if (e.cancelable && e.preventDefault) e.preventDefault();
                    return;
                }
                lastTapTime = now;
                lastTapX = touch.clientX;
                lastTapY = touch.clientY;
            }

            startX = touch.clientX;
            startY = touch.clientY;
            isDragging = false;
            isSwiping = true;
        };

        const handleTouchMove = (e) => {
            if (e.touches && e.touches.length === 2 && imgEl && initialPinchDist > 0) {
                // 2-finger pinch move
                if (e.cancelable) e.preventDefault();
                const dist = Math.hypot(
                    e.touches[0].clientX - e.touches[1].clientX,
                    e.touches[0].clientY - e.touches[1].clientY
                );
                currentScale = Math.min(Math.max(1, startScale * (dist / initialPinchDist)), 4.5);
                imgEl.style.transition = 'none';
                imgEl.style.transform = `scale(${currentScale})`;
                if (currentScale > 1.2) {
                    content.classList.add('is-zoomed');
                } else {
                    content.classList.remove('is-zoomed');
                }
                return;
            }

            const touch = e.touches ? e.touches[0] : e;
            const diffX = touch.clientX - startX;
            const diffY = touch.clientY - startY;

            if (Math.abs(diffX) > 8 || Math.abs(diffY) > 8) {
                isDragging = true;
            }
        };

        const handleTouchEnd = (e) => {
            if (e.touches && e.touches.length < 2) {
                initialPinchDist = 0;
                if (imgEl) {
                    imgEl.style.transition = 'transform 0.25s cubic-bezier(0.2, 0, 0, 1)';
                    if (currentScale < 1.05) {
                        currentScale = 1;
                        imgEl.style.transform = 'scale(1)';
                        imgEl.style.transformOrigin = 'center center';
                        content.classList.remove('is-zoomed');
                    }
                }
            }

            if (!isSwiping) return;
            isSwiping = false;

            // Background click / tap to close (only if not dragged and clicked on background)
            if (!isDragging && (e.target === content || e.target === modal)) {
                this.closeKidsFullscreen();
                return;
            }

            if (currentScale > 1.1) return; // don't swipe between items while zoomed in

            const touch = e.changedTouches ? e.changedTouches[0] : e;
            const diffX = touch.clientX - startX;
            const diffY = touch.clientY - startY;

            // Horizontal Swipe detected
            if (Math.abs(diffX) > 25 && Math.abs(diffX) > Math.abs(diffY)) {
                if (diffX < 0) {
                    // Swipe Left -> Next Item
                    if (state.viewingKidIndex < list.length - 1) {
                        state.viewingKidIndex++;
                        this.showKidsFullscreen(list[state.viewingKidIndex]);
                    }
                } else {
                    // Swipe Right -> Previous Item
                    if (state.viewingKidIndex > 0) {
                        state.viewingKidIndex--;
                        this.showKidsFullscreen(list[state.viewingKidIndex]);
                    }
                }
            }
        };

        content.ontouchstart = handleTouchStart;
        content.ontouchmove = handleTouchMove;
        content.ontouchend = handleTouchEnd;

        // Desktop mouse drag for swipe and background click to close
        content.onmousedown = (e) => {
            startX = e.clientX;
            startY = e.clientY;
            isDragging = false;
            isSwiping = true;
        };

        content.onmousemove = (e) => {
            if (!isSwiping) return;
            const diffX = e.clientX - startX;
            const diffY = e.clientY - startY;
            if (Math.abs(diffX) > 8 || Math.abs(diffY) > 8) {
                isDragging = true;
            }
        };

        content.onmouseup = (e) => {
            if (!isSwiping) return;
            isSwiping = false;

            // Background click to close on desktop
            if (!isDragging && (e.target === content || e.target === modal)) {
                this.closeKidsFullscreen();
                return;
            }

            if (currentScale > 1.1) return;

            const diffX = e.clientX - startX;
            const diffY = e.clientY - startY;

            if (Math.abs(diffX) > 40 && Math.abs(diffX) > Math.abs(diffY)) {
                if (diffX < 0) {
                    if (state.viewingKidIndex < list.length - 1) {
                        state.viewingKidIndex++;
                        this.showKidsFullscreen(list[state.viewingKidIndex]);
                    }
                } else {
                    if (state.viewingKidIndex > 0) {
                        state.viewingKidIndex--;
                        this.showKidsFullscreen(list[state.viewingKidIndex]);
                    }
                }
            }
        };

        modal.classList.add('active');
    }

    // Close Kids Fullscreen Viewer with Gallery Scroll Preservation
    closeKidsFullscreen() {
        const modal = document.getElementById('kids-viewer-modal');
        const content = document.getElementById('kids-viewer-content');
        if (!modal || !modal.classList.contains('active')) return;

        modal.classList.remove('active');
        if (content) content.classList.remove('is-zoomed');

        // Stop video if active
        const video = document.getElementById('kids-active-video');
        if (video) {
            video.pause();
        }

        // Gallery scroll position preservation:
        // Automatically scroll the gallery grid to the exact last-viewed card so the user never loses their position
        const targetCard = document.querySelector(`.kid-card[data-index="${state.viewingKidIndex}"]`);
        if (targetCard) {
            targetCard.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
            targetCard.classList.add('kid-card-returned');
            setTimeout(() => targetCard.classList.remove('kid-card-returned'), 1200);
        }
    }

    // Parent Settings Album Checkboxes
    renderAlbumChecklist() {
        const container = document.getElementById('album-checklist-container');
        if (!container) return;
        container.innerHTML = '';

        const allFolders = Array.from(new Set(state.media.map(m => m.folder)));
        allFolders.forEach(folder => {
            const count = state.media.filter(m => m.folder === folder).length;
            const isChecked = state.whitelistedFolders.includes(folder);

            const label = document.createElement('label');
            label.className = 'checkbox-item';
            label.innerHTML = `
                <input type="checkbox" value="${folder}" ${isChecked ? 'checked' : ''}>
                <span>📁 ${folder} (${count} ta fayl)</span>
            `;

            label.querySelector('input').addEventListener('change', (e) => {
                if (e.target.checked) {
                    if (!state.whitelistedFolders.includes(folder)) state.whitelistedFolders.push(folder);
                } else {
                    state.whitelistedFolders = state.whitelistedFolders.filter(f => f !== folder);
                }
                this.renderKidsGallery();
            });

            container.appendChild(label);
        });
    }

    // 1:1 Slidebox Active Sorter List
    getProActiveMedia() {
        return state.media.filter(item => {
            const notInTrash = !state.trash.includes(item.id);
            const matchesFilter = state.selectedProAlbumFilter === 'BARCHA FAYLLAR' || item.folder.toLowerCase() === state.selectedProAlbumFilter.toLowerCase();
            return notInTrash && matchesFilter;
        });
    }

    getCurrentProItem() {
        const list = this.getProActiveMedia();
        if (list.length === 0) return null;
        const safeIndex = Math.min(Math.max(0, state.currentIndex), list.length - 1);
        return list[safeIndex];
    }

    renderCardStack() {
        const stack = document.getElementById('card-stack');
        const emptyState = document.getElementById('empty-state');
        if (!stack) return;

        const currentItem = this.getCurrentProItem();
        this.updateTrashCounter();

        if (!currentItem) {
            stack.innerHTML = '';
            if (emptyState) emptyState.classList.add('active');
            return;
        }

        if (emptyState) emptyState.classList.remove('active');
        stack.innerHTML = '';

        const card = document.createElement('div');
        card.className = 'card-item';
        card.id = 'current-swipe-card';
        card.innerHTML = currentItem.type === 'video'
            ? `<video src="${currentItem.url}" controls autoplay muted style="width: 100%; height: 100%; object-fit: cover;"></video>`
            : `<img src="${currentItem.url}" alt="${currentItem.title}" style="width: 100%; height: 100%; object-fit: cover;">`;

        stack.appendChild(card);
        this.attachCardGestures(card, currentItem);
        this.updateFavButtonState(currentItem);
    }

    attachCardGestures(card, item) {
        let isDragging = false;
        let startY = 0, startX = 0;
        let currentY = 0, currentX = 0;

        const overlayUp = document.querySelector('.swipe-up-overlay');
        const overlayLeft = document.querySelector('.swipe-left-overlay');
        const overlayRight = document.querySelector('.swipe-right-overlay');

        const onTouchStart = (e) => {
            isDragging = true;
            const touch = e.touches ? e.touches[0] : e;
            startX = touch.clientX;
            startY = touch.clientY;
            currentX = 0;
            currentY = 0;
            card.style.transition = 'none';
        };

        const onTouchMove = (e) => {
            if (!isDragging) return;
            const touch = e.touches ? e.touches[0] : e;
            currentX = touch.clientX - startX;
            currentY = touch.clientY - startY;

            const rotate = currentX * 0.05;
            card.style.transform = `translate(${currentX}px, ${currentY}px) rotate(${rotate}deg)`;

            // Swipe Up to Trash visual feedback
            if (currentY < -30 && overlayUp) {
                overlayUp.style.opacity = Math.min(1, Math.abs(currentY) / 80);
            } else if (overlayUp) {
                overlayUp.style.opacity = '0';
            }

            // Left/Right feedback
            if (currentX < -30 && overlayLeft) {
                overlayLeft.style.opacity = Math.min(1, Math.abs(currentX) / 80);
            } else if (overlayLeft) {
                overlayLeft.style.opacity = '0';
            }

            if (currentX > 30 && overlayRight) {
                overlayRight.style.opacity = Math.min(1, Math.abs(currentX) / 80);
            } else if (overlayRight) {
                overlayRight.style.opacity = '0';
            }
        };

        const onTouchEnd = () => {
            if (!isDragging) return;
            isDragging = false;
            card.style.transition = 'transform 0.25s cubic-bezier(0.2, 0.8, 0.2, 1)';

            if (overlayUp) overlayUp.style.opacity = '0';
            if (overlayLeft) overlayLeft.style.opacity = '0';
            if (overlayRight) overlayRight.style.opacity = '0';

            // Swipe UP -> Move to Trash (Threshold: currentY < -50)
            if (currentY < -50 && Math.abs(currentY) > Math.abs(currentX) * 0.6) {
                card.style.transform = 'translate(0px, -600px) scale(0.3)';
                setTimeout(() => {
                    this.moveToTrash(item);
                }, 180);
            }
            // Swipe LEFT -> Next
            else if (currentX < -70) {
                card.style.transform = 'translate(-600px, 0px)';
                setTimeout(() => {
                    this.nextCard();
                }, 180);
            }
            // Swipe RIGHT -> Previous
            else if (currentX > 70) {
                card.style.transform = 'translate(600px, 0px)';
                setTimeout(() => {
                    this.prevCard();
                }, 180);
            }
            // Snap back
            else {
                card.style.transform = 'translate(0px, 0px) rotate(0deg)';
            }
        };

        card.onpointerdown = (e) => {
            try { card.setPointerCapture(e.pointerId); } catch (_) {}
            onTouchStart(e);
        };
        card.onpointermove = onTouchMove;
        card.onpointerup = (e) => {
            try { card.releasePointerCapture(e.pointerId); } catch (_) {}
            onTouchEnd(e);
        };
        card.onpointercancel = (e) => {
            try { card.releasePointerCapture(e.pointerId); } catch (_) {}
            onTouchEnd(e);
        };
    }

    // 1:1 Slidebox Actions
    trashCurrentCard() {
        const list = this.getProActiveMedia();
        if (list.length > 0 && state.currentIndex < list.length) {
            const item = list[state.currentIndex];
            this.moveToTrash(item);
        }
    }

    moveToTrash(item) {
        if (!state.trash.includes(item.id)) {
            state.trash.push(item.id);
            state.undoStack.push({ type: 'trash', itemId: item.id });
            this.showToast('Savatga tashlandi 🗑️');
            this.renderCardStack();
        }
    }

    nextCard() {
        const list = this.getProActiveMedia();
        if (state.currentIndex < list.length - 1) {
            state.currentIndex++;
            this.renderCardStack();
        } else {
            this.showToast('Oxirgi rasm');
        }
    }

    prevCard() {
        if (state.currentIndex > 0) {
            state.currentIndex--;
            this.renderCardStack();
        } else {
            this.showToast('Birinchi rasm');
        }
    }

    toggleCurrentFavorite() {
        const item = this.getCurrentProItem();
        if (!item) return;
        item.isFavorite = !item.isFavorite;
        this.updateFavButtonState(item);
        this.showToast(item.isFavorite ? 'Sevimlilarga qo\'shildi ❤️' : 'Sevimlilardan olib tashlandi');
    }

    updateFavButtonState(item) {
        const btn = document.getElementById('btn-slide-fav');
        const icon = document.getElementById('slide-fav-icon');
        if (!btn || !icon) return;

        if (item && item.isFavorite) {
            btn.classList.add('active');
            icon.className = 'fas fa-heart';
        } else {
            btn.classList.remove('active');
            icon.className = 'far fa-heart';
        }
    }

    performUndo() {
        if (state.undoStack.length === 0) {
            this.showToast('Bekor qilish uchun harakatlar yo\'q');
            return;
        }

        const lastAction = state.undoStack.pop();
        if (lastAction.type === 'trash') {
            state.trash = state.trash.filter(id => id !== lastAction.itemId);
            this.showToast('Savatdan qaytarildi ↶');
            this.renderCardStack();
        } else if (lastAction.type === 'album') {
            const item = state.media.find(m => m.id === lastAction.itemId);
            if (item) item.folder = lastAction.oldFolder;
            this.showToast('Albom o\'zgarishi bekor qilindi ↶');
            this.renderCardStack();
        }
    }

    // Slidebox Bottom Quick Album Sorter Tray
    renderSlideboxTray() {
        const tray = document.getElementById('album-tray-list');
        if (!tray) return;
        tray.innerHTML = '';

        state.userAlbums.forEach(albumName => {
            const btn = document.createElement('button');
            btn.className = 'btn-tray-album';
            btn.innerHTML = `📁 ${albumName}`;
            btn.addEventListener('click', () => {
                const item = this.getCurrentProItem();
                if (item) {
                    const oldFolder = item.folder;
                    item.folder = albumName;
                    state.undoStack.push({ type: 'album', itemId: item.id, oldFolder });
                    this.showToast(`"${albumName}" albomiga saralandi! ✨`);
                    this.nextCard();
                }
            });
            tray.appendChild(btn);
        });

        // Add new album button
        const addBtn = document.createElement('button');
        addBtn.className = 'btn-tray-album add-album';
        addBtn.innerHTML = `➕ Yangi Albom`;
        addBtn.addEventListener('click', () => {
            document.getElementById('create-album-modal')?.classList.add('active');
        });
        tray.appendChild(addBtn);
    }

    // Trash Manager
    updateTrashCounter() {
        const count = state.trash.length;
        const headerCount = document.getElementById('trash-header-count');
        if (headerCount) headerCount.textContent = count;

        document.querySelectorAll('.trash-count').forEach(el => el.textContent = count);
    }

    openTrashModal() {
        this.updateTrashCounter();
        const modal = document.getElementById('trash-modal');
        const grid = document.getElementById('trash-items-grid');
        const emptyNotice = document.getElementById('trash-empty-notice');
        if (!modal || !grid) return;

        grid.innerHTML = '';
        const trashedItems = state.media.filter(m => state.trash.includes(m.id));

        if (trashedItems.length === 0) {
            if (emptyNotice) emptyNotice.style.display = 'block';
        } else {
            if (emptyNotice) emptyNotice.style.display = 'none';
            trashedItems.forEach(item => {
                const thumb = document.createElement('div');
                thumb.className = 'trash-grid-thumb';
                thumb.innerHTML = item.type === 'video'
                    ? `<video src="${item.url}"></video>`
                    : `<img src="${item.url}">`;
                grid.appendChild(thumb);
            });
        }

        modal.classList.add('active');
    }

    closeTrashModal() {
        document.getElementById('trash-modal')?.classList.remove('active');
    }

    emptyTrash() {
        if (state.trash.length === 0) return;
        state.media = state.media.filter(m => !state.trash.includes(m.id));
        state.trash = [];
        this.showToast('Savat to\'liq tozalandi! 🗑️');
        this.closeTrashModal();
        this.renderCardStack();
        this.renderKidsGallery();
    }

    restoreAllTrash() {
        if (state.trash.length === 0) return;
        state.trash = [];
        this.showToast('Barcha fayllar savatdan qaytarildi! ↶');
        this.closeTrashModal();
        this.renderCardStack();
    }

    // Pro Album Filter Dropdown Modal
    openAlbumFilterModal() {
        const modal = document.getElementById('album-filter-modal');
        const list = document.getElementById('album-filter-list');
        if (!modal || !list) return;

        list.innerHTML = '';
        const allOptions = ['BARCHA FAYLLAR', ...new Set(state.media.map(m => m.folder)), ...state.userAlbums];
        const unique = Array.from(new Set(allOptions));

        unique.forEach(opt => {
            const item = document.createElement('div');
            item.className = `album-filter-item ${state.selectedProAlbumFilter === opt ? 'active' : ''}`;
            item.textContent = opt === 'BARCHA FAYLLAR' ? '📁 BARCHA FAYLLAR' : `📁 ${opt}`;
            item.addEventListener('click', () => {
                state.selectedProAlbumFilter = opt;
                const txt = document.getElementById('current-album-filter-text');
                if (txt) txt.textContent = opt;
                state.currentIndex = 0;
                this.closeAlbumFilterModal();
                this.renderCardStack();
            });
            list.appendChild(item);
        });

        modal.classList.add('active');
    }

    closeAlbumFilterModal() {
        document.getElementById('album-filter-modal')?.classList.remove('active');
    }

    // Biometrics Modal
    requestBiometricAuth(title, onSuccess) {
        state.pendingAuthAction = onSuccess;
        const modal = document.getElementById('biometric-modal');
        if (modal) modal.classList.add('active');
    }

    closeBiometricModal() {
        document.getElementById('biometric-modal')?.classList.remove('active');
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

    // Mode Switches
    switchToKidsMode() {
        state.isKidsMode = true;
        document.getElementById('kids-header')?.classList.remove('hidden');
        document.getElementById('kids-folder-bar')?.classList.remove('hidden');
        document.getElementById('pro-header')?.classList.add('hidden');
        this.renderKidsGallery();
        this.showScreen('kids-gallery');
    }

    switchToProMode() {
        state.isKidsMode = false;
        document.getElementById('kids-header')?.classList.add('hidden');
        document.getElementById('kids-folder-bar')?.classList.add('hidden');
        document.getElementById('pro-header')?.classList.remove('hidden');
        this.renderCardStack();
        this.showScreen('sorter');
    }

    // Donate Screen
    openDonateScreen() {
        this.showScreen('donate');
    }

    closeDonateScreen() {
        if (state.isKidsMode) {
            this.showScreen('kids-gallery');
        } else {
            this.showScreen('sorter');
        }
    }

    copyText(text, label) {
        navigator.clipboard.writeText(text).then(() => {
            this.showToast(`${label} nusxalandi! ✅`);
        }).catch(() => {
            this.showToast(`${label} nusxalandi! ✅`);
        });
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

    resetDemo() {
        state.trash = [];
        state.currentIndex = 0;
        this.showToast('Qayta boshlandi 🔄');
        this.renderCardStack();
    }

    // Hamburger Drawer & Modal Methods
    openDrawer() {
        document.getElementById('drawer-overlay')?.classList.add('active');
    }

    closeDrawer() {
        document.getElementById('drawer-overlay')?.classList.remove('active');
    }

    openGuideModal() {
        this.closeDrawer();
        document.getElementById('guide-modal')?.classList.add('active');
    }

    openAboutModal() {
        this.closeDrawer();
        document.getElementById('about-modal')?.classList.add('active');
    }

    checkUpdateFromDrawer() {
        this.closeDrawer();
        this.showToast('Yangilanishlar tekshirilmoqda... ⏳');
        setTimeout(() => {
            this.showToast('Sizda eng so\'nggi versiya o\'rnatilgan (v1.0.03) ✅');
        }, 1000);
    }

    updateBentoStats() {
        const countEl = document.getElementById('bento-whitelisted-count');
        const timerEl = document.getElementById('bento-timer-value');
        if (countEl) {
            countEl.textContent = `${state.whitelistedFolders.length} ta albom`;
        }
        if (timerEl) {
            timerEl.textContent = state.timerMinutes > 0 ? `${state.timerMinutes} daqiqa` : 'Cheksiz';
        }
    }

    updateKioskUI() {
        const chip = document.getElementById('btn-kids-kiosk-toggle');
        const icon = document.getElementById('kids-kiosk-icon');
        const text = document.getElementById('kids-kiosk-text');
        const ribbon = document.getElementById('kids-pinned-ribbon');
        const statusIndicator = document.getElementById('kids-status-indicator');

        if (state.isKioskPinned) {
            chip?.classList.add('pinned');
            if (icon) icon.className = 'fas fa-lock';
            if (text) text.textContent = 'Qadalgan';
            if (ribbon) ribbon.classList.remove('hidden');
            if (statusIndicator) {
                statusIndicator.classList.add('pinned');
                statusIndicator.innerHTML = '<span class="status-dot green"></span> XAVFSIZ QADALGAN';
            }
        } else {
            chip?.classList.remove('pinned');
            if (icon) icon.className = 'fas fa-lock-open';
            if (text) text.textContent = 'Qadash';
            if (ribbon) ribbon.classList.add('hidden');
            if (statusIndicator) {
                statusIndicator.classList.remove('pinned');
                statusIndicator.innerHTML = '<span class="status-dot"></span> BOLALAR REJIMI';
            }
        }

        const badge = document.getElementById('kiosk-status-badge');
        const btnText = document.getElementById('kiosk-btn-text');
        const btn = document.getElementById('btn-toggle-kiosk');
        if (state.isKioskPinned) {
            if (badge) badge.style.display = 'inline-block';
            if (btnText) btnText.textContent = 'Qadashni Bekor Qilish 🔓';
            if (btn) btn.className = 'btn btn-danger';
        } else {
            if (badge) badge.style.display = 'none';
            if (btnText) btnText.textContent = 'Ilovani Ekranga Qadash 📌';
            if (btn) btn.className = 'btn btn-outline';
        }
    }

    showToast(msg) {
        const container = document.getElementById('toast-container');
        if (!container) return;
        const toast = document.createElement('div');
        toast.style.cssText = 'background: rgba(22, 28, 44, 0.95); color: #fff; padding: 10px 18px; border-radius: 14px; margin-top: 8px; font-size: 13px; font-weight: 700; border: 1px solid rgba(255,255,255,0.12); box-shadow: 0 4px 16px rgba(0,0,0,0.6);';
        toast.textContent = msg;
        container.appendChild(toast);
        setTimeout(() => toast.remove(), 2500);
    }
}

// Initialize Application
const app = new PhotoCheckApp();
