# 📱 PhotoCheck Kids — Loyiha Holati va Arxitekturasi (HANDOFF)

Ushbu hujjat loyihaning to'liq arxitekturasi, joriy holati va yangilanishlarini o'z ichiga oladi.

---

## 1. Loyiha Holati va Umumiy Ma'lumot
* **Loyiha nomi:** PhotoCheck Kids (Smart Photo & Video Organizer + Kids Safe Gallery & Parental Control)
* **Amalga oshirilgan asosiy o'zgarishlar:**
  1. 👶 **Bolalar Xavfsiz Galereyasi (Kids Safe Mode):**
     - Dastur ochilganda, ota-ona sozlamalariga kirishda va dasturdan chiqishda (`onBackPressedDispatcher`) tizim biometrikasi (barmoq izi, yuz, telefon PIN/grafik kaliti) talab qilinadi.
     - Bolalar faqat ota-ona ruxsat bergan albomlar (Multfilmlar, Oila, Bolalar) ro'yxatini ko'ra oladi.
     - O'chirish, tahrirlash va boshqalarga yuborish (Share) to'liq taqiqlangan.
     - ⏳ **Ekran vaqti taymeri (Screen Time Limit):** Vaqt tugaganda yoqimli "Uxlash vaqti 🌙" ekrani chiqadi va ota-ona biometrikasisiz ochilmaydi.
  2. 🛡️ **Ota-ona Boshqaruvi va PhotoCheck Pro (Parent Dashboard & Classic Mode):**
     - `ParentSettingsScreen` orqali ruxsat berilgan albomlar (Whitelisted Folders) nazorati.
     - Taymer vaqti (15 daq, 30 daq, 45 daq, 1 soat, Cheksiz) va uni qayta boshlash.
     - Bolalar rejimini o'chirish orqali PhotoCheck'ning barcha klassik funksiyalariga (Slidebox, Galereya, Dublikatlar, Siqish, Tahlil) o'tish va tepadan bitta tugma bilan yana qulflash.
  3. ⚡ **Tizim Galereyasi Bilan Jonli Sinxronizatsiya (`ContentObserver`):**
     - Kamera yoki tizim galereyasidagi yangi media darhol dasturda aks etadi.

---

## 2. Modullar va Fayllar Xaritasi
```
android-app/
├── app/src/main/java/com/fingo/photocheck/
│   ├── auth/BiometricAuthManager.kt     # Biometrika / Tizim PIN / Parol boshqaruvi
│   ├── data/KidsPreferencesManager.kt   # Albomlar oq ro'yxati, rejim va taymer
│   ├── ui/kids/KidsSafeGalleryScreen.kt # Bolalar uchun xavfsiz galereya, viewer va timer lock
│   ├── ui/parent/ParentSettingsScreen.kt # Ota-ona sozlamalari dashboardi
│   ├── ui/PhotoCheckApp.kt              # Kids Safe va Pro rejimlarining birlashuvi
│   ├── repository/MediaRepository.kt    # MediaStore live loader
│   └── MainActivity.kt                  # FragmentActivity, Exit lock & Live Sync
web-demo/
├── index.html, style.css, app.js        # Kids Safe Mode, Taymer va Biometrika prototipi
.github/workflows/
└── build-apk.yml                        # GitHub Actions avtomatik APK build
```

---

## 3. GitHub Actions APK Build
Loyiha `main` tarmog'iga push qilinishi bilan GitHub Actions bulutda (Ubuntu muhitida JDK 17 bilan) avtomatik ravishda `PhotoCheck.apk` faylini yig'adi va GitHub Artifacts bo'limiga joylaydi.
