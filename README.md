# 📱 PhotoCheck - Android uchun Rasm va Videolarni Saralash Dasturi

**PhotoCheck** — Play Marketdagi *Slidebox* dasturining muqobili bo'lib, telefoningizdagi rasm va videolarni qulay va tezkor saralash, keraksizlarini o'chirish va yoqqanlarini yurakcha (sevimlilar) ro'yxatiga qo'shish uchun yaratilgan Android loyihadir.

---

## ⚡ Asosiy Imkoniyatlar va Boshqaruv (Gestures)

* 👈 **Chapga surish (Swipe Left):** Keyingi rasm yoki videoga o'tish.
* 👉 **O'ngga surish (Swipe Right):** Oldingi rasm yoki videoga qaytish.
* 👆 **Tepaga surish (Swipe Up):** Rasmni **yurakcha (Sevimlilar)** ro'yxatiga saqlash.
* 👇 **Pastga surish (Swipe Down):** Rasmni **o'chirish navbatiga (Savat)** o'tkazish.
* 🗑 **Savat boshqaruvi (Trash Manager):** Pastga surilgan barcha fayllarni bir tugma bilan qurilmadan butunlay o'chirish yoki xatoni qaytarish (Restore).

---

## 📁 Loyiha Tarkibi

1. **`android-app/`** — **Jetpack Compose** va **Kotlin** asosida yaratilgan to'liq Android Studio loyihasi.
2. **`web-demo/`** — Brauzerda va telefonda dasturni darhol ishlatib va sinab ko'rish uchun mo'ljallangan interaktiv mobil prototip.
3. **`.github/workflows/build-apk.yml`** — **GitHub Actions** orqali avtomatik ravishda tayyor `.apk` faylini yig'ish (build qilish) ssenariysi.

---

## 🚀 GitHub Actions Orqali APK Tayyorlash va Yuklab Olish

Ushbu repozitoriyada **GitHub Actions** sozlangan. Siz Android Studio o'rnatmasdan turib, to'g'ridan-to'g'ri GitHub saytining o'zida APK yaratishingiz va telefoningizga yuklab olishingiz mumkin:

### 1-qadam: GitHub Actions bo'limiga kiring
1. Repozitoriyangizning yuqori menyusidagi **Actions** tugmasini bosing.
2. Chap tomondagi ro'yxatdan **Build Android APK (PhotoCheck)** bo'limini tanlang.

### 2-qadam: APK turini tanlang va Build qilishni boshlang
1. O'ng tomondagi **Run workflow** tugmasini bosing.
2. **"Chiqariladigan APK turini tanlang"** bo'limida o'zingizga kerakli variantni tanlang:
   - **`arm64-v8a`** — *Mening telefonim uchun:* Zamonaviy Android telefonlar uchun maxsus optimallashgan, ixcham hajmdagi APK.
   - **`universal`** — *Universal APK:* Istalgan turdagi Android telefoniga tushadigan universal versiya.
   - **`both`** — Ikkala versiyani ham bir vaqtda chiqarish.
3. **Run workflow** (Yashil tugma)ni bosing.

### 3-qadam: APK faylini yuklab oling
1. 2-3 daqiqadan so'ng jarayon (Checkmark ✅) yakunlanadi.
2. Bajarilgan ish ustiga bosing va eng pastdagi **Artifacts** bo'limidan tayyor `.apk` faylini yuklab oling va telefoningizga o'rnating!

---

## 🛠 GitHub-ga Push Qilish Ko'rsatmasi

Ushbu loyihani o'zingizning GitHub repozitoriyangizga yuklash uchun terminalda quyidagi buyruqlarni ketma-ket bajaring:

```bash
git add .
git commit -m "Add PhotoCheck Android app, web demo, and GitHub Actions APK build workflow"
git branch -M main
git push -u origin main
```

---

## 💻 Muallif va Ruxsatlar
* **Dasturchi:** aka-FinGo & Google Antigravity AI
* **Litsenziya:** MIT License
