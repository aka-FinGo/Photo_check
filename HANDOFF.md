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

12. 🛠️ **CI/CD Tuzatish va Global Tasteskill UI/UX Mukammallashtirish:**
   - **CI/CD Kompilyatsiya Xatolari Bartaraf Etildi:** `KidsSafeGalleryScreen.kt` da tushib qolgan `import kotlinx.coroutines.launch` va `import androidx.compose.animation.core.*` qo'shildi (`Unresolved reference: launch` va `Unresolved reference: animateFloat` xatolari to'liq hal qilindi). Shuningdek, `PhotoCheckApp.kt` ga ham to'liq `animation.core.*` importi ulandi.
   - **🌙 Tungi Osmon & Uxlash Ekrani (`KidsSleepLockedScreen`):** Radial indigo-midnight gradient, 2.4 soniyali silliq nafas oluvchi (breathing pulse) oy nishoni va zumrad-moviy nurli biometrik ochish tugmasi o'rnatildi.
   - **🎴 Pro Top Bar & Slidebox Ergonomik Klasteri:** 32dp neo-glass tabletkalar, 7 ta tugma o'rniga ergonomik 2 ta guruh (chapda navigatsiya va qaytarish, o'ngda savat, yurak va ulashish), pastki albomlar panelida silliq shisha kartochkalar.
   - **📊 Bento Dashboard Sayqallari:** Ota-ona sozlamalaridagi statistika kartalari piktogrammalar va micro-glow bilan boyitildi.
   - **🌐 Web Demo Ambient Bloom & Neo-Glass Overhaul:** Veb namoyishida telefon romi ortida `.phone-aura` ambient nur yog'dusi (`auraBreath` animatsiyasi), 32px balandlikdagi neo-glass Pro header tabletkalari (`#btn-open-donate-pro`, `#btn-open-trash`, `#btn-lock-to-kids`, `#btn-pro-settings`), taktil Slidebox boshqaruv tugmalari (qizil yoqutli Savat, yurak yoqutli Sevimlilar, Undo), dinamik faol porlovchi shisha albom tabletkalari, yuqori yorug'lik hoshiyali Bento statistika kartalari, silliq neo-glass kalitlar (switch), hamda yulduzli tungi osmon va nafas oluvchi oy nuri bilan jihozlangan Uxlash ekrani to'liq joriy qilindi.

13. 🔄 **Rasmlar va Videolarni O'ngga/Chapga Surish (Swipe Paging) To'liq Qayta Tiklandi:**
   - **Xato Sababi:** `detectTapGestures` boshlang'ich bosish hodisasini (`down.consume()`) o'zlashtirib olgani sababli, `HorizontalPager` surish imo-ishorasini (drag) qabul qila olmay qolgan edi.
   - **Android (`KidsSafeGalleryScreen.kt`):** `KidsZoomableImage` ga xavfsiz `awaitEachGesture(awaitFirstDown(requireUnconsumed = false))` o'rnatildi: kattalashtirilmagan holatda (`scale <= 1.05f`) 1-barmoqli gorizontal surish hodisasi umuman o'zlashtirilmaydi (`unconsumed`), natijada `HorizontalPager` rasmlar orasida silliq va tabiiy sirpanadi. 2-marta bosganda silliq zoom in/out (2.5x), 2-barmoq bilan chimdiganda esa masshtablash to'liq ishlaydi.
   - **Videolarda Surish (`KidsVideoPlayer`):** `clickable` o'rniga yengil tap detektori o'rnatildi, videodan rasmga va rasmdan videoga bemalol surib o'tish imkoni ochildi.
   - **Qo'shimcha Navigatsiya Tugmalari:** To'liq ekrandagi tomoshabin chap va o'ng tomonlariga zamonaviy suzuvchi `[<]` va `[>]` shisha piktogrammalari o'rnatildi (bitta bosish bilan ham o'tish mumkin).
   - **Web Demo Sinxronizatsiyasi:** `web-demo/` tomoshabiniga ham suzuvchi `#btn-kid-prev` va `#btn-kid-next` tugmalari, hamda sichqoncha va sensor uchun to'liq sinxron swipe boshqaruvi ulandi.

14. 🎴 **Slidebox Rejimi 1:1 Mukammal Standartga Yetkazildi (Complete 1:1 Slidebox Experience):**
   - **Haqiqiy 2 Qavatli 3D Karta Maydonchasi (Physical Card Deck & Tilt):** Oldingi karta ostida keyingi fotosurat 0.93x masshtabda ko'rinib turadi (`peekScale`); ustki karta surilganda pastki karta 1.0x gacha tabiiy kattalashadi. Surish burchagi (`rotationZ`) drag harakatiga mos -16° dan +16° gacha qiyalanadi (physics-based drag tilt).
   - **Jonli Vizual Shtamplar (Dynamic Gesture Stamps):** Tepaga surilganda `SAVATGA TASHLASH 🗑️`, o'ngga surilganda `KEYINGISI 👉`, chapga surilganda `OLDINGI 👈`, pastga surilganda `SEVIMLI ❤️` yorqin neon shtamplari dinamik ravshanlik bilan chiqadi.
   - **Taktil Haptika (Sensory Feedback):** Har bir surish va harakat chegarasida tebranish (`LocalHapticFeedback` / `HapticFeedbackType.LongPress`).
   - **Karta Ichida 2x Zoom:** Kartaning o'zida ikki marta bosganda 2.2x kattalashadi va fotosurat detallarini tekshirish imkonini beradi.
   - **Ketma-ket Kadrlarni Solishtirish (Burst Compare Mode):** 45 soniya oralig'ida olingan o'xshash rasmlar aniqlanganda karta tepasida miniatyura lentasi (`📸 N ta kadr`) va bitta bosish bilan qolgan ortiqcha kadrlarni savatga tashlash tugmasi (`⚡ Boshqalarini savatga`) chiqadi.
   - **Albomlarni Disk Xotirasiga Eksport Qilish (Scoped Storage Export):** Foydalanuvchi saralagan albomlar `KidsPreferencesManager` da doimiy saqlanadi. Saralash tugagach "📁 Albomlarni Qurilma Xotirasiga Saqlash" tugmasi orqali haqiqiy Android MediaStore `Pictures/<Albom>` va `Movies/<Albom>` jildlariga nusxalanadi.
   - **Savat Xotira Hajmi Hisob-kitobi (Real-time MB/GB Stats):** Tepa paneldagi savat tabletkasida va yakuniy Bento ekranida o'chirilishi kutilayotgan fayllarning umumiy hajmi (`14 (52.4 MB)`) real-vaqtda hisoblanadi.
   - **Bento Yakunlash Ekrani (Completion Summary):** Barcha fotosuratlar saralanganda chiroyli g'alaba nishoni, statistika (ko'rilgan, savatdagi hajm, albomlardagi soni), tizim savatchasini tozalash va eksport tugmalari chiqadi.
   - **Web Demo To'liq Mosligi & Klaviatura Boshqaruvi:** Desktop va vebda `ArrowUp` (savat), `ArrowRight` (keyingi), `ArrowLeft` (oldingi), `ArrowDown` (sevimli), `Ctrl+Z` (bekor qilish) klaviatura tugmalari to'liq ulandi.

15. 🛠️ **CI/CD Tuzatish (KidsPreferencesManager Constants):**
   - `KidsPreferencesManager.kt` da tushib qolgan `KEY_CUSTOM_ALBUMS`, `KEY_TRASH_IDS`, `KEY_FAVORITE_IDS`, va `KEY_ALBUM_ASSIGNMENTS` kalitlari `companion object` ga kiritildi (`Unresolved reference` xatosi bartaraf etildi).

16. ⏱️❤️ **Bolalar Taymeri (5-30m + Qo'lda Sozlash), Reaktiv Sevimlilar & Savat Tiklash Mukammallashtirildi:**
   - **Ekran Vaqti Taymeri (5-30m va Qo'lda Sozlash):**
     * `ParentSettingsScreen.kt` da taymer variantlari 2 qatorga ajratildi: `5 m`, `10 m`, `15 m`, `20 m` va `25 m`, `30 m`, `∞ (Cheksiz)`.
     * Agar foydalanuvchi nostandart vaqt o'rnatgan bo'lsa, avtomatik ravishda `⭐ X m` faol chipi paydo bo'ladi.
     * `[✏️ Vaqtni Qo'lda Sozlash...]` tugmasi qo'shildi: bosilganda chiroyli `AlertDialog` ochilib, unda raqamli klaviatura (`KeyboardType.Number`) orqali istalgan daqiqa kiritish va saqlash mumkin.
   - **Sevimlilar To'liq Ko'rish va Boshqarish:**
     * Pro rejim Top Barida Savat yoniga alohida `❤️ ${favoriteItems.size}` yorqin yoqut-pushti tabletkasi o'rnatildi (bosilganda to'g'ridan-to'g'ri Sevimlilar filtrini ochadi).
     * Albomlar filtri ro'yxatiga `"❤️ SEVIMLILAR"` birinchi darajali toifa sifatida qo'shildi.
     * Agar Sevimlilar filtri tanlangan bo'lsa va unda fotosurat bo'lmasa, Bento uslubida maxsus yoqimli bo'sh holat ekrani chiqadi.
   - **Savat va Sevimlilar Reaktivligi (Instant State Reactivity):**
     * Jetpack Compose da `SnapshotStateList` ob'ekt havolasi o'zgarmasligi sababli `remember(mediaList, trash)` sekin yoki yangilanmaslik muammosi aniqlanib, `trashSnapshot = trash.toList()` va `favoritesSnapshot = favorites.toList()` orqali Compose holat kuzatuvi 100% reaktiv holga keltirildi.
     * `onTrash`, `onToggleFavorite` va `onUndo` amallari bajarilishi bilan holatlar `kidsPrefs` ga zudlik bilan yoziladi va ekranda tezkor Toast xabari aks etadi.
   - **Savatdan Chiqarish (Quick Restore) Sayqallandi:**
     * `TrashManagementSheet` da har bir miniatyura pastiga `[Tiklash ↶]` tezkor banneri o'rnatildi. Foydalanuvchi bitta tegish bilan fotosuratni savatdan chiqarishi mumkin va savat hisoblagichi darhol kamayadi.
   - **Surish Shtamplarining Bir-biriga Qorishib Ketishi (Anti-Overlap) Tuzatildi:**
     * Foydalanuvchi skrinshotida ko'ringan diagonali surish paytida ham Savat, ham Keyingisi yoki Sevimli shtamplari bir vaqtda chiqib qolish xatosi o'q dominantligi (`isVerticalDominant = absY > absX * 0.75f`) orqali hal qilindi. Endi istalgan paytda aniq va qat'iy 1 ta shtamp aks etadi.
   - **Web Demo Sinxronizatsiyasi:**
     * `#btn-open-favs` tugmasi, `#favs-header-count` hisoblagichi, 5-30m taymer chiplari va `#btn-custom-timer-web` qo'lda vaqt kiritish muloqot oynasi ulandi.
     * Savat miniatyuralariga `[Tiklash ↶]` qatlami va `restoreSingleTrash(id)` funksiyasi qo'shildi.

---

## 3. GitHub Actions CI/CD va Reliz Tizimi:
1. 🛠️ **Host SDK Yo'li:** `local.properties` tozalandi, CI `$ANDROID_HOME` bilan xatosiz yig'iladi.
2. 📦 **Artifacts & Release:** `upload-artifact@v4` va `action-gh-release@v2` orqali universal va split APK'lar nashr etiladi.
3. 🔐 **Imzolash:** `photocheck.jks` v1 va v2 imzo bilan barcha relizlarni bir xil kalitda himoyalaydi.

