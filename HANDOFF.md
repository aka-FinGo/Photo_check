# 📱 PhotoCheck Kids & 1:1 Slidebox Pro — Loyiha Holati va Arxitekturasi (HANDOFF)

Ushbu hujjat loyihaning to'liq arxitekturasi, Rasmlar va Videolar Uchun Mukammal Gorizontal Surish (Swipe), 2-barmoqli Pinch-to-Zoom, Video Player Interactive Timeline, Slidebox Rejimida Tepaga Surish orqali Savatga Tushirish, Ota-ona Sozlamalarida Albomlarni Ikonkali Boshqarish, Gamburger Menyusi, Bento Stats Vidjeti, Interactive Guide & About dialoglari, GitHub Pages Landing Page, Biometrik Chiqish Himoyasi, 1:1 Slidebox Sorter, In-App Updater va Donat tizimini o'z ichiga oladi.

Qo'shimcha rasmiy ro'yxat: [`FEATURES_REGISTRY.md`](file:///e:/Loyihalarim/GitHub/Photo_check/FEATURES_REGISTRY.md)

---

## 1. Loyiha Holati va Umumiy Ma'lumot
* **Loyiha nomi:** PhotoCheck (1:1 Original Slidebox Sorter + Kids Safe Gallery & Parental Control)
* **Paket nomi:** `com.fingo.photocheck`
* **Jonli Havolalar (GitHub Pages):**
  - **Asosiy Landing Page:** [https://aka-fingo.github.io/Photo_check/](https://aka-fingo.github.io/Photo_check/)
  - **Mobil Interaktiv Demo:** [https://aka-fingo.github.io/Photo_check/web-demo/](https://aka-fingo.github.io/Photo_check/web-demo/)

---

## 2. So'nggi Qo'shilgan Imkoniyatlar va Tuzatishlar:
1. 🔄 **Rasmlar va Videolarni Gorizontal Surish (Swipe) va Zoom Konflikti Butunlay Yechildi:**
   - **Android (`KidsSafeGalleryScreen.kt`):** `KidsZoomableImage` da `awaitEachGesture` orqali 1 ta barmoq bilan tegilganda va rasm kattalashtirilmagan bo'lsa (`scale <= 1.05f`), sensor hodisalari `HorizontalPager` ga to'liq va erkin o'tkaziladi. 2 ta barmoq bilan tegilganda esa `calculateZoom` va `calculatePan` bilan 1x dan 4.5x gacha pinch zoom faollashadi.
   - **Web Demo (`web-demo/app.js`):** Sensor va sichqoncha bilan gorizontal surish chegarasi (threshold) 25px ga optimallashtirilib, rasmlar va videolar tezkor suriladi.

2. 🔘 **Albomlarni Katta To'liq Enli Tugmalar Bilan Boshqarish:**
   - **Android (`ParentSettingsScreen.kt`):** `[✓✓ Barchasini Tanlash]` (ko'k) va `[⨂ Bekor Qilish]` (qizil) tugmalari sarlavha ostida to'liq enli qilib joylashtirildi.
   - **Web Demo (`web-demo/index.html` & `style.css`):** `.btn-bulk-action` orqali ikkala tugma butun eni bo'ylab yaqqol ko'rinadigan qilindi.

3. 🎬 **Video Pleyerda Interaktiv Timeline (Progress Bar):**
   - **Android (`KidsSafeGalleryScreen.kt`):** `KidsVideoPlayer` ga vaqt hisoblagichi (`00:15 / 02:45`), interaktiv Slider, 10s oldinga/ortga va avto-yashirinuvchi boshqaruv paneli qo'shildi.
   - **Web Demo (`web-demo/app.js` & `index.html`):** Progress slider va vaqt indikatorlari to'liq ulandi.

4. 🗑️ **Slidebox Rejimida Tepaga Surish (Swipe UP to Trash):**
   - **Android (`PhotoCheckApp.kt`):** `offsetY < -50f` bo'lganda darhol `trash` ro'yxatiga qo'shiladi va Toast `"Savatga tashlandi 🗑️"` chiqadi. Pastda qo'shimcha Savat tugmasi bor.
   - **Web Demo (`web-demo/app.js`):** PointerEvents orqali surish xatosiz ishlaydi.

5. 📦 **APK Hajmi va Build Optimallashuvi:**
   - `build.gradle.kts` da R8 / Minification yoqildi, APK hajmi ~15 MB gacha tushirildi.

---

## 3. GitHub Actions CI/CD va Reliz Tizimi:
1. 🛠️ **Host SDK Yo'li:** `local.properties` tozalandi, CI `$ANDROID_HOME` bilan xatosiz yig'iladi.
2. 📦 **Artifacts & Release:** `upload-artifact@v4` va `action-gh-release@v2` orqali universal va split APK'lar nashr etiladi.
3. 🔐 **Imzolash:** `photocheck.jks` v1 va v2 imzo bilan barcha relizlarni bir xil kalitda himoyalaydi.


