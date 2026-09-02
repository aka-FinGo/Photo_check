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

5. 📌 **Screen Pinning / Kiosk Mode va Immersive Fullscreen:**
   - **Android (`MainActivity.kt` & `ParentSettingsScreen.kt`):** `startLockTask()` orqali ilovani ekranga qadash (Home va Recents tugmalarini bloklash). Ota-ona sozlamalarida barmoq izi bilan "Qadashni Bekor Qilish 🔓" tugmasi.
   - **Immersive Sticky Fullscreen (`setImmersiveMode`):** Bolalar rejimida tepa status panelini avtomatik yashirish, bola tortganda 2s ichida qayta yashirinadi.
   - **Web Demo (`web-demo/app.js`):** Kiosk Mode simulyatori integratsiya qilindi.

6. 📦 **APK Hajmi va Build Optimallashuvi:**
   - `build.gradle.kts` da R8 / Minification yoqildi, APK hajmi ~15 MB gacha tushirildi.

7. 🚀 **In-App Updater HTTP 403 Yechimi va Gamburger Drawer Integratsiyasi:**
   - `UpdateManager.kt` da GitHub API 403 xatosiga qarshi avtomatik zero-rate-limit 302 redirect fallback kiritildi.
   - Sozlamalar pastidagi ortiqcha karta olib tashlanib, yangilanishni tekshirish Gamburger menyusidagi (Drawer) "Dastur Yangilanishi" orqali to'liq ishlaydigan qilindi.

8. 🎬 **Kids Rejimida Video Preview (Thumbnail) va Almashuvchi Kiosk Tugmasi:**
   - **Coil `VideoFrameDecoder.Factory()`:** `MainActivity.kt` da global `ImageLoader` ga ulandi.
   - **Grid Thumbnail:** Har bir videoning 1-soniyasidan kadr (`videoFrameMillis(1000L)`) olinadi (qora intro kadrlarni oldini olish uchun).
   - **To'liq ekran:** Video ochilganda sifatli kadr va markazda katta neon Play tugmasi chiqadi, bosilganda video ijro etiladi.
   - **📌 Almashuvchi Kiosk Tugmasi:** Kids paneli tepasida `Qadalgan 📌` (yashil) va `Qadash 🔓` (kulrang) o'zgaruvchan tugmasi qo'shildi. Ikkala holat ham qat'iy ravishda Barmoq izi (biometrika) so'raydi.

9. 🛠️ **CI/CD Kompilyatsiya Xatosi Bartaraf Etildi:**
   - `ParentSettingsScreen.kt` da ortiqcha qavs olib tashlandi va yetishmayotgan `android.widget.Toast` importi qo'shildi. Barcha fayllar sintaksisi 100% toza holatga keltirildi.

10. 🎨 **Kids Rejimi Header Dizayni Tasteskill Darajasiga Ko'tarildi:**
   - **Ixcham Brend Bloki:** Katta matn o'rniga zamonaviy gradient `[🎈]` nishoni va `PhotoCheck` + jonli holat nuqtasi (`BOLALAR REJIMI` / `XAVFSIZ QADALGAN`) o'rnatildi.
   - **Toza Qadash Chip:** Kiosk tugmasi kengligi qisqartirildi, ortiqcha takroriy emojilar olib tashlandi, 32dp neo-glass uslubiga keltirildi (`Qadash` / `Qadalgan`).
   - **Nol Sig'maslik (Zero Overflow):** Barcha boshqaruv elementlari (Brend + Qadash + Taymer + Qalqon) istalgan 360dp+ ekranlarda siqilishsiz yoki qirqilishsiz 100% mutanosib joylashdi.
   - **📌 Ekran Qadalgan Ribon:** Kiosk faol bo'lganda silliq animatsiya bilan pastga tushuvchi yashil xavfsizlik lentasi va barmoq izi bilan tezkor `Yechish 🔓` tugmasi qo'shildi.

11. 🔍 **Double-Tap Zoom In / Zoom Out va Galereya Scroll Holatini Saqlash:**
   - **Double-Tap Zoom Toggle:** `KidsZoomableImage` da 2 marta tez bosilganda agar rasm kattalashgan bo'lsa (`scale > 1.15f`) silliq 250ms animatsiya bilan `scale = 1f` va `offset = Offset.Zero` ga qaytadi (Zoom Out). Agar kattalashmagan bo'lsa silliq `scale = 2.5f` ga kattalashadi (Zoom In).
   - **Silliq 2 Barmoqli Chimdish:** `Modifier.transformable` orqali 2 barmoqli erkin masshtab va surish ta'minlandi; `scale == 1f` paytida gorizontal 1 barmoqli surish `HorizontalPager` orqali fotosuratlar o'rtasida erkin o'tadi.
   - **📍 Galereya Scroll Holati Saqlandi (Zero Reset):** `KidsSafeGalleryScreen` da `rememberLazyGridState()` saqlanadi va to'liq ekrandan "Ortga" (<-) tugmasi yoki Android tizim back tugmasi bosilganda (`BackHandler`) `gridState.scrollToItem(lastViewedIndex)` chaqiriladi. Galereya hech qachon 0-indeksga sakrab ketmaydi, aynan qoldirilgan rasmda turadi.
   - **Web Demo Sinxronizatsiyasi:** `web-demo/` da ham double-tap/double-click zoom toggle, koordinataga qarab kattalashish, va tomoshabin yopilganda oxirgi ko'rilgan kartochkaga silliq fokuslanish (`scrollIntoView`) ulandi.

---

## 3. GitHub Actions CI/CD va Reliz Tizimi:
1. 🛠️ **Host SDK Yo'li:** `local.properties` tozalandi, CI `$ANDROID_HOME` bilan xatosiz yig'iladi.
2. 📦 **Artifacts & Release:** `upload-artifact@v4` va `action-gh-release@v2` orqali universal va split APK'lar nashr etiladi.
3. 🔐 **Imzolash:** `photocheck.jks` v1 va v2 imzo bilan barcha relizlarni bir xil kalitda himoyalaydi.


