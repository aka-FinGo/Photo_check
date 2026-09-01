# 📱 PhotoCheck Kids & Pro — Loyiha Holati va Arxitekturasi (HANDOFF)

Ushbu hujjat loyihaning to'liq arxitekturasi, 1:1 Original Slidebox Sorter, Kids Safe Mode, In-App Updater, Keystore imzolash, dinamik versiyalash va CI/CD yangilanishlarini o'z ichiga oladi.

---

## 1. Loyiha Holati va Umumiy Ma'lumot
* **Loyiha nomi:** PhotoCheck (1:1 Original Slidebox Sorter + Kids Safe Gallery & Parental Control)
* **Paket nomi:** `com.fingo.photocheck`
* **Amalga oshirilgan asosiy tizimlar:**

  1. 🎴 **1:1 Original Slidebox Tajribasi (Klassik Pro Rejim):**
     - Ortiqcha tablar (siqish, dublikatlar, murakkab grafiklar) olib tashlanib, dastur 100% Slidebox kabi toza, chaqqon va qulay qilindi.
     - 👆 **Tepaga surish (Swipe Up to Trash):** Rasmni savatga uchirish animatsiyasi bilan savatga tashlash.
     - 👈 👉 **Chapga / O'ngga surish (Swipe Left / Right):** Keyingi va oldingi rasmga o'tish.
     - ❤️ **Yurakcha (Favorite):** Rasmni bir bosishda sevimli qilish.
     - ↶ **Undo (Ortga qaytarish):** Oxirgi harakatni (savatga tashlangan yoki albomga saralangan) bir zumda bekor qilish.
     - 📁 **Pastki Albomlar Paneli (Quick Album Sorter Tray):** Ekran ostida albomlar ro'yxati. Foydalanuvchi biror albomni bossa, rasm o'sha albomga biriktiriladi va darhol keyingi rasmga avtomatik o'tadi. `+ Yangi Albom` tugmasi mavjud.
     - 🗑️ **Top Bar Savat (Trash Counter):** Yuqoridagi savat hisoblagichi `🗑️ (5)`. Bossangiz, savatdagi barcha fayllar ro'yxati, "Barchasini butunlay o'chirish" va "Tiklash" modal oynasi ochiladi.
     - 🔲 **Galereya ko'rinishi (Grid View):** Barcha rasmlarni 3 qatorli setkada ko'rish va istalganini Slidebox Sorterda ochish.

  2. 👶 **Ota-ona Sozlamalari va Albomlarni Ommaviy Boshqarish:**
     - `ParentSettingsScreen.kt` da **"Barchasini tanlash"** (Select All) va **"Barchasini bekor qilish"** (Deselect All) tugmalari bitta bosishda hamma albomlarni belgilaydi yoki tozalaydi.
     - Ekran vaqti taymeri (15 daq, 30 daq, 45 daq, 1 soat, Cheksiz) va "Qayta Boshlash".
     - Dastur yangilanishi bo'limi (Update info, versiya va tekshirish tugmasi).

  3. 👶 **Bolalar Xavfsiz Galereyasi (Kids Safe Mode):**
     - Dastur ochilganda, ota-ona sozlamalariga kirishda va dasturdan chiqishda (`onBackPressedDispatcher`) tizim biometrikasi (barmoq izi, yuz, telefon PIN/grafik kaliti) talab qilinadi.
     - Bolalar faqat ota-ona ruxsat bergan albomlar (Multfilmlar, Oila, Bolalar) ro'yxatini ko'ra oladi.
     - O'chirish, tahrirlash va boshqalarga yuborish (Share) to'liq taqiqlangan.
     - ⏳ **Ekran vaqti taymeri (Screen Time Limit):** Vaqt tugaganda yoqimli "Uxlash vaqti 🌙" ekrani chiqadi va ota-ona biometrikasisiz ochilmaydi.

  4. 🔄 **In-App Updater Tizimi (`com.fingo.photocheck.update`):**
     - `UpdateInfo.kt`: Versiya taqqoslash (`1.0.02` > `1.0.01`), hajm, yuklab olish URL va changelog modeli.
     - `UpdateManager.kt`: GitHub Releases API orqali eng so'nggi relizni tekshirish, stream va progress kuzatuvi bilan yuklab olish hamda `FileProvider` orqali tizim o'rnatuvchisini (`PackageInstaller`) ishga tushirish. Android 8.0+ ruxsatlarini boshqarish.
     - `UpdateDialog.kt`: Jetpack Compose modal yangilanish dialogi (Changelog, LinearProgressIndicator %, yuklangan MB, "Yangilash" va "O'rnatish" bosqichlari).

  5. 🔑 **Ilova Imzosi va Dinamik Versiyalash (`1.0.XX`):**
     - Release kalit: `android-app/app/keystore/photocheck.jks` (v1 + v2 signing).
     - Dinamik qabul: `-PversionCode=... -PversionName=1.0.XX`.

  6. ⚡ **Tizim Galereyasi Bilan Jonli Sinxronizatsiya (`ContentObserver`):**
     - Kamera yoki tizim galereyasidagi yangi media darhol dasturda aks etadi.

---

## 2. Modullar va Fayllar Xaritasi
```
android-app/
├── app/
│   ├── keystore/
│   │   └── README.md                    # Keystore konfiguratsiyasi va parametrlari
│   ├── src/main/
│   │   ├── AndroidManifest.xml          # INTERNET, INSTALL permissions & FileProvider
│   │   ├── res/xml/file_paths.xml       # FileProvider cache & files paths
│   │   └── java/com/fingo/photocheck/
│   │       ├── auth/BiometricAuthManager.kt     # Biometrika / Tizim PIN / Parol boshqaruvi
│   │       ├── data/KidsPreferencesManager.kt   # Albomlar oq ro'yxati, rejim va taymer
│   │       ├── model/MediaItem.kt               # Media model
│   │       ├── repository/MediaRepository.kt    # MediaStore live loader
│   │       ├── ui/
│   │       │   ├── kids/KidsSafeGalleryScreen.kt   # Bolalar uchun xavfsiz galereya, viewer va timer lock
│   │       │   ├── parent/ParentSettingsScreen.kt  # Ota-ona sozlamalari, Bulk albom tanlash & Update
│   │       │   ├── PhotoCheckApp.kt                # 1:1 Slidebox Sorter, Quick Album Tray, Trash Sheet
│   │       │   └── theme/Theme.kt                  # Material3 mavzusi
│   │       ├── update/
│   │       │   ├── UpdateInfo.kt                   # Update model & version comparator
│   │       │   ├── UpdateManager.kt                # GitHub Releases API, Downloader & Installer
│   │       │   └── UpdateDialog.kt                 # Jetpack Compose Update Dialog & Progress
│   │       └── MainActivity.kt                     # FragmentActivity, Exit lock & Live Sync
│   └── build.gradle.kts                            # Dinamik versiyalash & signingConfigs (v1/v2)
web-demo/
├── index.html                                      # 1:1 Slidebox, Kids Safe & Biometrika prototipi
├── app.js                                          # Slidebox touch gestures, Undo stack & Trash modal
└── style.css                                       # Dark glassmorphism UI uslubi
.github/workflows/
└── build-apk.yml                                    # GitHub Actions: build release, sign, artifact & GitHub Releases
```

---

## 3. GitHub Actions CI/CD va Reliz Jarayoni
1. `main` tarmog'iga push bo'lganda `build-apk.yml` avtomatik ishga tushadi.
2. `github.run_number` asosida `1.0.01`, `1.0.02`, ... versiya raqami va kodi hisoblanadi.
3. Release Keystore (`photocheck.jks`) tekshirilib, release APK yig'iladi va imzolanadi.
4. `PhotoCheck.apk` ham GitHub Artifacts ga, ham avtomatik yaratilgan `v1.0.XX` GitHub Release tegiga biriktiriladi.
