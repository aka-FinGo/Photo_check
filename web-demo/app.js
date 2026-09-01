// PhotoCheck 1:1 Slidebox & Kids Safe Gallery Web Prototype
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
    customAlbums: ['Ta\'til 2026', 'Saralangan'],
    assignedAlbums: {}, // mediaId -> albumName
    favorites: [],
    trash: [],
    historyStack: [], // [{ type: 'trash'|'album'|'fav', item, prevIndex, ... }]
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
        this.renderSlidebox();
        this.renderParentChecklist();
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
            if (state.isKidsMode) {
                this.switchToKidsMode();
            }
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

        // Parent Bulk Album Selection (Barchasi / Bekor qilish)
        document.getElementById('btn-select-all-albums')?.addEventListener('click', () => {
            const allFolders = ['Cartoons', 'Animals', 'Camera', 'WhatsApp', 'Screenshots'];
            state.whitelistedFolders = [...allFolders];
            this.renderParentChecklist();
            this.renderKidsGallery();
            this.showToast('Barcha albomlar tanlandi ✅');
        });

        document.getElementById('btn-clear-all-albums')?.addEventListener('click', () => {
            state.whitelistedFolders = [];
            this.renderParentChecklist();
            this.renderKidsGallery();
            this.showToast('Barcha albomlar bekor qilindi ❌');
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

        // Slidebox Toolbar Buttons
        document.getElementById('btn-undo')?.addEventListener('click', () => this.undoLastAction());
        document.getElementById('btn-prev')?.addEventListener('click', () => this.prevPhoto());
        document.getElementById('btn-next')?.addEventListener('click', () => this.nextPhoto());
        document.getElementById('btn-fav')?.addEventListener('click', () => this.toggleFavorite());
        document.getElementById('btn-share')?.addEventListener('click', () => {
            const active = this.getActiveMediaList();
            if (active.length > 0) {
                const item = active[state.currentIndex];
                if (navigator.share) {
                    navigator.share({ title: item.title, url: window.location.href });
                } else {
                    this.showToast('Ulashish oynasi ochildi 🔗');
                }
            }
        });

        // Reset filter button on empty state
        document.getElementById('btn-reset-filter')?.addEventListener('click', () => {
            state.trash = [];
            state.currentIndex = 0;
            this.renderSlidebox();
            this.showToast('Fayllar qayta tiklandi!');
        });

        // Trash Management Modal
        document.getElementById('btn-open-trash')?.addEventListener('click', () => {
            this.openTrashModal();
        });
        document.getElementById('btn-close-trash')?.addEventListener('click', () => {
            document.getElementById('trash-modal')?.classList.remove('active');
        });
        document.getElementById('btn-restore-all')?.addEventListener('click', () => {
            state.trash = [];
            this.updateTrashCounter();
            this.renderSlidebox();
            document.getElementById('trash-modal')?.classList.remove('active');
            this.showToast('Barcha rasmlar tiklandi! 🔄');
        });
        document.getElementById('btn-delete-all-perm')?.addEventListener('click', () => {
            state.media = state.media.filter(m => !state.trash.includes(m.id));
            state.trash = [];
            this.updateTrashCounter();
            this.renderSlidebox();
            document.getElementById('trash-modal')?.classList.remove('active');
            this.showToast('Savat to\'liq tozalandi 🗑️');
        });

        // New Album Dialog
        document.getElementById('btn-open-add-album')?.addEventListener('click', () => {
            const modal = document.getElementById('new-album-modal');
            const input = document.getElementById('new-album-input');
            if (input) input.value = '';
            if (modal) modal.classList.add('active');
        });

        document.getElementById('btn-cancel-album')?.addEventListener('click', () => {
            document.getElementById('new-album-modal')?.classList.remove('active');
        });

        document.getElementById('btn-save-album')?.addEventListener('click', () => {
            const input = document.getElementById('new-album-input');
            const name = input?.value.trim();
            if (name) {
                if (!state.customAlbums.includes(name)) {
                    state.customAlbums.push(name);
                }
                const active = this.getActiveMediaList();
                if (active.length > 0) {
                    const item = active[state.currentIndex];
                    this.sortToAlbum(item, name);
                }
            }
            document.getElementById('new-album-modal')?.classList.remove('active');
        });

        // Update Check Button in settings
        document.getElementById('btn-check-update')?.addEventListener('click', () => {
            this.showToast('Sizda eng so\'nggi versiya (v1.0.01) o\'rnatilgan ✅');
        });

        // Card Touch Gestures (Swipe Up to Trash, Left/Right navigation)
        this.setupCardGestures();
    }

    setupCardGestures() {
        const card = document.getElementById('slidebox-card');
        const trashBadge = document.getElementById('trash-overlay-badge');
        if (!card) return;

        let startX = 0, startY = 0, currentX = 0, currentY = 0;
        let isDragging = false;

        const onStart = (e) => {
            const active = this.getActiveMediaList();
            if (active.length === 0) return;
            isDragging = true;
            const touch = e.touches ? e.touches[0] : e;
            startX = touch.clientX;
            startY = touch.clientY;
            currentX = startX;
            currentY = startY;
            card.style.transition = 'none';
        };

        const onMove = (e) => {
            if (!isDragging) return;
            const touch = e.touches ? e.touches[0] : e;
            currentX = touch.clientX;
            currentY = touch.clientY;
            const deltaX = currentX - startX;
            const deltaY = currentY - startY;

            if (deltaY < -30) {
                trashBadge?.classList.add('active');
            } else {
                trashBadge?.classList.remove('active');
            }

            const scale = deltaY < 0 ? Math.max(0.75, 1 + deltaY / 1000) : 1;
            card.style.transform = `translate(${deltaX}px, ${deltaY}px) scale(${scale})`;
        };

        const onEnd = () => {
            if (!isDragging) return;
            isDragging = false;
            trashBadge?.classList.remove('active');
            card.style.transition = 'transform 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275)';

            const deltaX = currentX - startX;
            const deltaY = currentY - startY;

            if (deltaY < -70) {
                // 👆 SWIPE UP TO TRASH
                card.style.transform = `translate(${deltaX}px, -800px) scale(0.2)`;
                setTimeout(() => {
                    const active = this.getActiveMediaList();
                    if (active.length > 0) {
                        const item = active[state.currentIndex];
                        this.trashPhoto(item);
                    }
                    card.style.transition = 'none';
                    card.style.transform = 'translate(0, 0) scale(1)';
                }, 200);
            } else if (deltaX > 100) {
                // 👉 SWIPE RIGHT (PREV)
                this.prevPhoto();
                card.style.transform = 'translate(0, 0) scale(1)';
            } else if (deltaX < -100) {
                // 👈 SWIPE LEFT (NEXT)
                this.nextPhoto();
                card.style.transform = 'translate(0, 0) scale(1)';
            } else {
                card.style.transform = 'translate(0, 0) scale(1)';
            }
        };

        card.addEventListener('mousedown', onStart);
        window.addEventListener('mousemove', onMove);
        window.addEventListener('mouseup', onEnd);

        card.addEventListener('touchstart', onStart, { passive: true });
        window.addEventListener('touchmove', onMove, { passive: true });
        window.addEventListener('touchend', onEnd);
    }

    getActiveMediaList() {
        return state.media.filter(m => !state.trash.includes(m.id));
    }

    renderSlidebox() {
        const active = this.getActiveMediaList();
        const mediaContainer = document.getElementById('slidebox-media');
        const emptyState = document.getElementById('slidebox-empty-state');
        const counter = document.getElementById('photo-counter');
        const card = document.getElementById('slidebox-card');
        const assignedBadge = document.getElementById('assigned-album-badge');
        const assignedName = document.getElementById('assigned-album-name');
        const undoBtn = document.getElementById('btn-undo');
        const favBtn = document.getElementById('btn-fav');

        this.updateTrashCounter();

        if (undoBtn) undoBtn.disabled = state.historyStack.length === 0;

        if (active.length === 0) {
            if (mediaContainer) mediaContainer.innerHTML = '';
            if (card) card.classList.add('hidden');
            if (emptyState) emptyState.classList.remove('hidden');
            if (counter) counter.textContent = '0 / 0';
            this.renderAlbumPills(null);
            return;
        }

        if (card) card.classList.remove('hidden');
        if (emptyState) emptyState.classList.add('hidden');

        if (state.currentIndex >= active.length) {
            state.currentIndex = active.length - 1;
        }
        if (state.currentIndex < 0) state.currentIndex = 0;

        const current = active[state.currentIndex];
        if (counter) counter.textContent = `${state.currentIndex + 1} / ${active.length}`;

        if (mediaContainer) {
            if (current.type === 'video') {
                mediaContainer.innerHTML = `<video src="${current.url}" controls autoplay muted style="width:100%; height:100%; object-fit:contain;"></video>`;
            } else {
                mediaContainer.innerHTML = `<img src="${current.url}" alt="${current.title}">`;
            }
        }

        // Assigned album badge
        const assigned = state.assignedAlbums[current.id];
        if (assigned) {
            assignedBadge?.classList.remove('hidden');
            if (assignedName) assignedName.textContent = assigned;
        } else {
            assignedBadge?.classList.add('hidden');
        }

        // Favorite button state
        const isFav = state.favorites.includes(current.id);
        if (favBtn) {
            if (isFav) {
                favBtn.classList.add('active');
                favBtn.innerHTML = '<i class="fas fa-heart"></i>';
            } else {
                favBtn.classList.remove('active');
                favBtn.innerHTML = '<i class="far fa-heart"></i>';
            }
        }

        this.renderAlbumPills(current);
    }

    renderAlbumPills(currentItem) {
        const row = document.getElementById('album-pills-row');
        if (!row) return;

        // Keep "+ Yangi Albom" button
        const addBtn = document.getElementById('btn-open-add-album');
        row.innerHTML = '';
        if (addBtn) row.appendChild(addBtn);

        const allAlbums = ['Camera', 'Cartoons', 'Animals', 'WhatsApp', ...state.customAlbums];
        const unique = [...new Set(allAlbums)];

        unique.forEach(albumName => {
            const isAssigned = currentItem && state.assignedAlbums[currentItem.id] === albumName;
            const btn = document.createElement('button');
            btn.className = `album-pill ${isAssigned ? 'assigned' : ''}`;
            
            const icon = albumName.includes('Camera') ? '📷' :
                         albumName.includes('Cartoon') ? '🎬' :
                         albumName.includes('Animal') ? '🐱' :
                         albumName.includes('WhatsApp') ? '💬' : '📁';

            btn.innerHTML = `${icon} ${albumName}`;
            btn.addEventListener('click', () => {
                if (currentItem) {
                    this.sortToAlbum(currentItem, albumName);
                }
            });
            row.appendChild(btn);
        });
    }

    trashPhoto(item) {
        state.trash.push(item.id);
        state.historyStack.push({ type: 'trash', item, prevIndex: state.currentIndex });
        this.showToast('Savatga tashlandi 🗑️');
        this.renderSlidebox();
    }

    sortToAlbum(item, albumName) {
        state.assignedAlbums[item.id] = albumName;
        state.historyStack.push({ type: 'album', item, albumName, prevIndex: state.currentIndex });
        this.showToast(`"${albumName}" albomiga qo'shildi! 📁`);
        this.nextPhoto();
    }

    toggleFavorite() {
        const active = this.getActiveMediaList();
        if (active.length === 0) return;
        const item = active[state.currentIndex];
        const isFav = state.favorites.includes(item.id);
        if (isFav) {
            state.favorites = state.favorites.filter(id => id !== item.id);
            this.showToast('Sevimlilardan chiqarildi');
        } else {
            state.favorites.push(item.id);
            this.showToast('Sevimlilarga qo\'shildi ❤️');
        }
        state.historyStack.push({ type: 'fav', item, prevFav: isFav });
        this.renderSlidebox();
    }

    undoLastAction() {
        if (state.historyStack.length === 0) return;
        const last = state.historyStack.pop();

        if (last.type === 'trash') {
            state.trash = state.trash.filter(id => id !== last.item.id);
            state.currentIndex = last.prevIndex;
            this.showToast('Savatdan qaytarildi ↶');
        } else if (last.type === 'album') {
            delete state.assignedAlbums[last.item.id];
            state.currentIndex = last.prevIndex;
            this.showToast('Albom saralash bekor qilindi ↶');
        } else if (last.type === 'fav') {
            if (last.prevFav) {
                state.favorites.push(last.item.id);
            } else {
                state.favorites = state.favorites.filter(id => id !== last.item.id);
            }
        }
        this.renderSlidebox();
    }

    nextPhoto() {
        const active = this.getActiveMediaList();
        if (state.currentIndex < active.length - 1) {
            state.currentIndex++;
            this.renderSlidebox();
        }
    }

    prevPhoto() {
        if (state.currentIndex > 0) {
            state.currentIndex--;
            this.renderSlidebox();
        }
    }

    updateTrashCounter() {
        const count = state.trash.length;
        const el = document.getElementById('trash-header-count');
        const btn = document.getElementById('btn-open-trash');
        if (el) el.textContent = count;
        if (btn) {
            if (count > 0) btn.classList.add('has-items');
            else btn.classList.remove('has-items');
        }
    }

    openTrashModal() {
        const modal = document.getElementById('trash-modal');
        const grid = document.getElementById('trash-modal-grid');
        const subtitle = document.getElementById('trash-modal-subtitle');
        if (!modal || !grid) return;

        const trashed = state.media.filter(m => state.trash.includes(m.id));
        if (subtitle) subtitle.textContent = `${trashed.length} ta fayl (${(trashed.length * 3.2).toFixed(1)} MB)`;

        grid.innerHTML = '';
        if (trashed.length === 0) {
            grid.innerHTML = '<p style="grid-column:span 3; color:gray; text-align:center; padding:20px;">Savat bo\'sh!</p>';
        } else {
            trashed.forEach(item => {
                const card = document.createElement('div');
                card.className = 'trash-item';
                card.innerHTML = `<img src="${item.url}" title="Tiklash uchun bosing">`;
                card.addEventListener('click', () => {
                    state.trash = state.trash.filter(id => id !== item.id);
                    this.openTrashModal();
                    this.renderSlidebox();
                    this.showToast('Rasm tiklandi! 🔄');
                });
                grid.appendChild(card);
            });
        }
        modal.classList.add('active');
    }

    renderParentChecklist() {
        const container = document.getElementById('parent-album-checklist');
        if (!container) return;

        const allFolders = [
            { name: 'Cartoons', label: '🎬 Multfilmlar (8 fayl)' },
            { name: 'Animals', label: '🐱 Hayvonlar (5 fayl)' },
            { name: 'Camera', label: '📷 Kamera (12 fayl)' },
            { name: 'WhatsApp', label: '💬 WhatsApp (Yopiq)' },
            { name: 'Screenshots', label: '📸 Screenshots (Yopiq)' }
        ];

        container.innerHTML = '';
        allFolders.forEach(folder => {
            const isChecked = state.whitelistedFolders.includes(folder.name);
            const label = document.createElement('label');
            label.className = 'checkbox-item';
            label.innerHTML = `
                <span>${folder.label}</span>
                <input type="checkbox" value="${folder.name}" ${isChecked ? 'checked' : ''}>
            `;
            const cb = label.querySelector('input');
            cb?.addEventListener('change', () => {
                if (cb.checked) {
                    if (!state.whitelistedFolders.includes(folder.name)) {
                        state.whitelistedFolders.push(folder.name);
                    }
                } else {
                    state.whitelistedFolders = state.whitelistedFolders.filter(f => f !== folder.name);
                }
                this.renderKidsGallery();
            });
            container.appendChild(label);
        });
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
        this.renderKidsGallery();
        this.showScreen('kids-gallery');
    }

    switchToProMode() {
        state.isKidsMode = false;
        document.getElementById('kids-header')?.classList.add('hidden');
        document.getElementById('kids-folder-bar')?.classList.add('hidden');
        document.getElementById('pro-header')?.classList.remove('hidden');
        this.renderSlidebox();
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
