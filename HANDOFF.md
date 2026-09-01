# 📱 PhotoCheck Kids & Pro — Loyiha Holati va Arxitekturasi (HANDOFF)

Ushbu hujjat loyihaning to'liq arxitekturasi, 1:1 Original Slidebox Sorter, Kids Safe Mode, In-App Updater, Keystore imzolash, dinamik versiyalash va CI/CD yangilanishlarini o'z ichiga oladi.

---

## 1. Loyiha Holati va Umumiy Ma'lumot
* **Loyiha nomi:** PhotoCheck (1:1 Original Slidebox Sorter + Kids Safe Gallery & Parental Control)
* **Paket nomi:** `com.fingo.photocheck`
* **Joriy holat:**
  - Build loglaridagi `compileReleaseKotlin` xatosi (`dp`, `sp`, `FontWeight` importlari) to'liq tuzatildi.
  - Release imzo kaliti (`android-app/app/keystore/photocheck.jks`) repozitoriyaga mustahkam saqlandi.
  - GitHub Actions CI/CD to'liq tuzatilgan holatda `main` tarmog'ida ishga tushirildi.

---

## 2. Asosiy Modullar
1. **1:1 Original Slidebox Sorter:**
   - Swipe Up to Trash (tepaga uchirish animatsiyasi).
   - Pastki albomlar paneli orqali bir bosishda saralash va avtomatik keyingi rasmga o'tish.
   - Yurakcha (Favorite) va Undo (↶).
   - Top Bar Savat `🗑️ (5)` va tozalash/tiklash boshqaruvi.
2. **In-App Updater (`com.fingo.photocheck.update`):**
   - GitHub Releases API orqali eng so'nggi relizni tekshirish, progressli yuklab olish va `FileProvider` orqali Android tizim o'rnatuvchisini chaqirish.
3. **Bolalar Xavfsiz Galereyasi & Biometrik Himoya:**
   - Barmoq izi / tizim PIN kodi bilan ochilish va chiqish.
   - Oq ro'yxatdagi albomlar (Whitelist), o'chirish/ulashishning yo'qligi va ekran vaqti taymeri.
4. **Ota-ona Boshqaruvi:**
   - Albomlarni ommaviy "Barchasini tanlash" va "Barchasini bekor qilish" boshqaruvi.
