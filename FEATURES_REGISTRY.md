# 📋 PhotoCheck — Barcha Funksiyalar va Imo-ishoralar Ro'yxati (Features Registry)

Ushbu hujjat loyihadagi har bir funksiya, rejim, imo-ishora (gesture) va tugmalarning to'liq, rasmiy va o'zgarmas ro'yxatidir.

---

## 1. 👶 Bolalar Rejimi (Kids Safe Gallery)
| Funksiya / Element | Qanday Ishlaydi? | Qayerda? |
|---|---|---|
| **Gorizontal Surish (Swipe Left/Right)** | Rasmlar va videolarni navbatdagisiga yoki oldingisiga o'tkazish. 1 ta barmoq bilan gorizontal suriladi. | `KidsSafeGalleryScreen.kt` |
| **2-Barmoqli Zoom (Pinch-to-zoom)** | 2 ta barmoq bilan rasmni 1x dan 4.5x gacha kattalashtirish/kichraytirish va surish. | `KidsZoomableImage` |
| **Double-tap Zoom** | Rasmni 2 marta tez bosganda 2.5x kattalashadi, yana 2 marta bosganda 1x holatga qaytadi. | `KidsZoomableImage` |
| **Video Timeline (Scrubber)** | Video davomiyligi, joriy vaqt (`00:15 / 02:45`), slider bilan istalgan joyga o'tish, 10s oldinga/ortga, Play/Pause. | `KidsVideoPlayer` |
| **Video Preview (Thumbnail)** | Gridda qora kadr bo'lmasligi uchun 1-soniyadagi kadr (`videoFrameMillis(1000)`) dekodlanadi va davomiyligi ko'rsatiladi. | `KidsSafeGalleryScreen.kt` |
| **Video Preview Backdrop & Neon Play** | To'liq ekranda video ochilganda kadr va markazda katta neon Play tugmasi ko'rinadi. | `KidsVideoPlayer` |
| **Minimalist Mi Gallery Uslubi** | Ekranda ortiqcha oldinga/orqaga tugmalari yo'q, faqat toza rasm va video ko'rinadi. | `KidsFullscreenViewer` |
| **Oq Ro'yxat (Whitelisted Albums)** | Faqat ota-ona ruxsat bergan albomlar ko'rinadi. Begona albomlar, savatcha va boshqalar bloklangan. | `KidsPreferencesManager` |
| **Ekran Vaqti Taymeri** | 15, 30, 45, 60 daqiqa. Vaqt tugagach "Uxlash vaqti 🌙" animatsion qulf ekrani chiqadi. | `KidsPreferencesManager` |
| **Biometrik Qulflash** | Chiqish (Back press), Sozlamalarga kirish va taymerni ochishda barmoq izi, yuz yoki PIN kodi so'raladi. | `BiometricAuthManager` |
| **Immersive Sticky Fullscreen** | Tepa bildirishnomalar paneli (status bar) yashiriladi. Bola tepadan tortsa ham panel avto-yashirinadi. | `MainActivity.kt` (`setImmersiveMode`) |
| **Ekran Qadash (Kiosk Rejimi)** | `startLockTask()` orqali ilovani ekranga qadash. Home va Ilovalar ro'yxati tugmalari qulflanadi. | `MainActivity.kt` (`startLockTask`) |
| **Tezkor Ekran Qadash Tugmasi (Almashuvchi)** | Kids paneli tepasida `Qadalgan 📌` / `Qadash 🔓` tugmasi. Qadash va bekor qilish qat'iy barmoq izi bilan ishlaydi. | `KidsSafeHeader` |

---

## 2. 🎴 1:1 Original Slidebox Pro (Saralash Rejimi)
| Funksiya / Element | Qanday Ishlaydi? | Qayerda? |
|---|---|---|
| **Tepaga Surish (Swipe UP to Trash)** | Kartochkani tepaga surish (`offsetY < -50f`) orqali darhol savatga / o'chirish navbatiga o'tkazish. | `SlideboxCardDeck` |
| **Tezkor Savat Tugmasi** | Pastki toolbar panelidagi to'g'ridan-to'g'ri Savat tugmasi. | `SlideboxCardSorterScreen` |
| **Chapga/O'ngga Surish** | Keyingi va oldingi rasm/videoga o'tish. | `SlideboxCardDeck` |
| **Pastki Albomlar Paneli** | Pastdagi istalgan albom ustiga bosilsa, rasm o'sha albomga biriktiriladi. | `SlideboxCardSorterScreen` |
| **Savat Boshqaruvi (Trash Manager)** | Barcha savatdagi fayllarni bir tugma bilan qurilmadan butunlay tozalash (`MediaStore.createDeleteRequest`) yoki tiklash (Restore). | `TrashManagementSheet` |
| **Undo (Ortga Qaytarish)** | Oxirgi qilingan amalni (savatga tashlash, albomga biriktirish, sevimli qilish) orqaga qaytaradi. | `SlideboxAction` |
| **Favorite (Sevimli / Yurakcha)** | Rasmni sevimlilar ro'yxatiga qo'shish / chiqarish. | `SlideboxCardSorterScreen` |
| **Galereya Grid Rejimi** | Barcha rasmlarni 3 ustunli grid holatida ko'rish. | `SlideboxGridScreen` |

---

## 3. 🛡️ Ota-ona Sozlamalari (Parent Settings)
| Funksiya / Element | Qanday Ishlaydi? | Qayerda? |
|---|---|---|
| **Gamburger Menyusi (Drawer)** | Asosiy sozlamalar, Yo'riqnoma, Dastur yangilash, Dastur haqida va Donat dialoglari. | `ParentSettingsScreen.kt` |
| **Bento Analytics Vidjeti** | Ruxsat etilgan albomlar soni, jami fayllar soni va taymer statusini ko'rsatuvchi karta. | `ParentSettingsScreen.kt` |
| **📌 Ekran Qadash Boshqaruvi** | Ota-ona sozlamalarida "Ilovani Ekranga Qadash 📌" va barmoq izi bilan "Qadashni Bekor Qilish 🔓" tugmalari. | `ParentSettingsScreen.kt` |
| **`[✓✓ Barchasini Tanlash]`** | Katta to'liq enli ko'k tugma — barcha albomlarni bitta bosishda ruxsat etilganlar ro'yxatiga oladi. | `ParentSettingsScreen.kt` |
| **`[⨂ Bekor Qilish]`** | Katta to'liq enli qizil tugma — barcha belgilangan albomlarni bitta bosishda tozalaydi. | `ParentSettingsScreen.kt` |
| **In-App Updater** | Gamburger Drawer ichidagi "Dastur Yangilanishi" orqali yangi versiyani tekshirish (HTTP 403 ga qarshi avtomatik 302 fallback bilan). | `UpdateManager.kt` |

---

## 4. 🚀 Build va CI/CD Konfiguratsiyasi
* **R8 / Minification:** Yoqilgan (`isMinifyEnabled = true`, `isShrinkResources = true`).
* **Hajmi:** ~12-18 MB (ortiqcha kutubxona va ikonkalarsiz).
* **Imzo:** Standart `photocheck.jks` v1 va v2 imzolash bilan.
* **Format:** Universal `PhotoCheck.apk` va ABI split `PhotoCheck-arm64-v8a.apk`.
