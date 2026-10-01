# 📱 PhotoCheck Kids & 1:1 Slidebox Pro — Loyiha Holati va Arxitekturasi (HANDOFF)

> **Last Updated**: 2026-10-01 20:34 UTC  
> **Status**: GitHub `origin/main` to'liq sinxronlashtirildi (v1.0.77 gacha). Kids Rejimidagi Taymer tugaganda Uxlash Ekrani chiqmaslik muammosi aniqlandi.

---

## 1. Loyiha Holati va Umumiy Ma'lumot
* **Loyiha nomi:** PhotoCheck (1:1 Original Slidebox Sorter + Kids Safe Gallery & Parental Control)
* **Paket nomi:** `com.fingo.photocheck`
* **Jonli Havolalar (GitHub Pages):**
  - **Asosiy Landing Page:** [https://aka-fingo.github.io/Photo_check/](https://aka-fingo.github.io/Photo_check/)
  - **Mobil Interaktiv Demo:** [https://aka-fingo.github.io/Photo_check/web-demo/](https://aka-fingo.github.io/Photo_check/web-demo/)

---

## 2. Aniqlangan Muammo: Kids Taymeri Nolga Kelganda Uxlash Ekrani Chiqmayotgani Sababi
* **Fayl:** `android-app/app/src/main/java/com/fingo/photocheck/ui/PhotoCheckApp.kt` (qator 109-119).
* **Tub sababi:**
  ```kotlin
  LaunchedEffect(remainingSeconds, isKidsMode, isClassicModeActive) {
      if (isKidsMode && !isClassicModeActive && remainingSeconds > 0) {
          delay(1000L)
          remainingSeconds--
          if (remainingSeconds <= 0L) {
              isTimerExpired = true
          }
      } else {
          isTimerExpired = false
      }
  }
  ```
  1. `remainingSeconds` 1 dan 0 ga o'tganda `isTimerExpired = true` bo'ladi.
  2. Ammo `remainingSeconds` qiymati o'zgargani uchun Compose `LaunchedEffect(remainingSeconds, ...)` ni darhol bekor qilib, yangitdan 0 qiymati bilan chaqiradi.
  3. Yangi chaqiruvda `remainingSeconds > 0` sharti yolg'on bo'lib, `else` bloki ishga tushadi: `isTimerExpired = false`.
  4. Natijada `KidsSleepLockedScreen` (🌙 "Uxlash va dam olish vaqti!") 1 millisekundda yopilib ketadi va bola galereyadan cheksiz foydalanishda davom etadi.
  5. Shuningdek `timerLimitMinutes == 0` (Cheksiz) bo'lganda headerda `00:00` qizil chiqib qolish xatosi mavjud.

---

## 3. Tuzatish Rejasi (Action Plan):
- [ ] `PhotoCheckApp.kt` dagi taymerni `while(remainingSeconds > 0)` sikliga o'tkazish va `LaunchedEffect` kalitidan `remainingSeconds` ni olib tashlash.
- [ ] Vaqt 0 ga yetganda `isTimerExpired = true` holatini qat'iy saqlab qolish va faqat ota-ona biometrika bilan ochgandagina tiklash.
- [ ] `KidsSafeHeader` da `timerLimitMinutes == 0` holati uchun `∞ Cheksiz` nishonini chiqarish.
- [ ] Web Demo (`web-demo/app.js`) da ham taymer va uxlash ekranini to'liq sinxron tekshirish.
- [ ] GitHub Actions orqali yangi reliz APK sini build qilish.

---

## 4. Resumption Command
```bash
cd /home/kali/Photo_check
```
