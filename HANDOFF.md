# 📱 PhotoCheck Kids & 1:1 Slidebox Pro — Loyiha Holati va Arxitekturasi (HANDOFF)

Ushbu hujjat loyihaning to'liq arxitekturasi, Rasmlar va Videolar Uchun Mukammal Gorizontal Surish (Swipe), Zoom (Pinch/Double-tap), Hamburger Boshqaruv Menyusi, Bento Stats Vidjeti, Interactive Guide & About dialoglari, GitHub Pages Landing Page, Biometrik Chiqish Himoyasi, 1:1 Slidebox Sorter, In-App Updater va Donat tizimini o'z ichiga oladi.

---

## 1. Loyiha Holati va Umumiy Ma'lumot
* **Loyiha nomi:** PhotoCheck (1:1 Original Slidebox Sorter + Kids Safe Gallery & Parental Control)
* **Paket nomi:** `com.fingo.photocheck`
* **Jonli Havolalar (GitHub Pages):**
  - **Asosiy Landing Page:** [https://aka-fingo.github.io/Photo_check/](https://aka-fingo.github.io/Photo_check/)
  - **Mobil Interaktiv Demo:** [https://aka-fingo.github.io/Photo_check/web-demo/](https://aka-fingo.github.io/Photo_check/web-demo/)

---

## 2. Yangi Tuzatish va Imkoniyatlar:
1. 🔄 **Rasmlar va Videolarni Gorizontal Surish (Swipe) Muammosi To'liq Bartaraf Etildi:**
   - **Android (`KidsSafeGalleryScreen.kt`):** `KidsZoomableImage` oddiy holatda (`scale <= 1.05f`) gorizontal surishni ushlab qolmasligi ta'minlandi. Natijada `HorizontalPager` orqali rasmlar ham, videolar ham birdek silliq va qulay o'tadi. Faqat rasm zoom qilingandagina pan faollashadi.
   - **Web Demo (`web-demo/app.js`):** Brauzerning standart rasm tortish (ghost image drag) xulqi bekor qilinib (`draggable = false`, `ondragstart = preventDefault`), rasmlar ham sensor/sichqoncha orqali keyingisiga bir zumda o'tadigan qilindi.
2. 🍔 **Gamburger Navigatsiya Menyusi (Drawer):**
   - Asosiy xavfsizlik, Yo'riqnoma, Dastur haqida, Yangilanish markazi va Donat.
3. 📊 **Bento Grid Stats Vidjeti:**
   - Albomlar soni, umumiy fayllar va taymer hisoblagichi.

---

## 3. GitHub Actions CI/CD va Reliz Tizimi Tuzatishlari:
1. 🛠️ **Hardcoded SDK Path Muammosi:** `android-app/local.properties` faylidagi `/home/kali/android-sdk` yo'li olib tashlandi va `.gitignore` ga qo'shildi. Workflow ichida `rm -f local.properties` bosqichi orqali CI-da runnerning standart `$ANDROID_HOME` dan foydalanishi kafolatlandi.
2. 📦 **Artifacts & Release Yuklash:** `actions/upload-artifact@v4` da `path: dist/*` o'rniga to'g'ridan-to'g'ri `path: dist` belgilandi va `if-no-files-found: error` bilan mustahkamlandi.
3. 🚀 **Universal va Split APK Relizlari:** `softprops/action-gh-release@v2` da `files: dist/*.apk` orqali universal APK (`PhotoCheck.apk`, `PhotoCheck-v1.0.XX.apk`) hamda arxitektura bo'yicha split APK'lar (`arm64-v8a`, `armeabi-v7a`, `x86_64`) avtomatik tarzda GitHub Releases-ga yuklanadi. `make_latest: true` va token parametrlari sozlandi.
4. 🔐 **Keystore Imzolash:** `android-app/app/build.gradle.kts` da keystore faylini qidirish zanjiri (`sequenceOf`) kuchaytirildi, v1 va v2 imzolash faollashtirildi.
5. ⚡ **In-App Updater API Muvofiqligi va 3-Bosqichli APK Tanlash:** `UpdateManager.checkForUpdate` yordamchi funksiyasi qo'shildi va `UpdateManager` da Universal APK (`PhotoCheck.apk`), versiyali (`PhotoCheck-v*.apk`) hamda arxitektura split fallback qoidalari kiritildi.
6. 🛡️ **ProGuard / R8 Qoidalari:** `android-app/app/proguard-rules.pro` yaratildi (Compose, Coroutines, Coil va model qoidalari).


