# PhotoCheck Release Keystore Sozlamalari

Ushbu papka PhotoCheck ilovasi relizlarini imzolash uchun `photocheck.jks` kalitini saqlaydi.

### Standart Kalit Parametrlari:
- **Fayl nomi**: `photocheck.jks`
- **Alias**: `photocheck`
- **Store Password**: `photocheck123`
- **Key Password**: `photocheck123`
- **CN**: `PhotoCheck`
- **O**: `FinGo`
- **C**: `UZ`

### Kalitni qo'lda yaratish (agar kerak bo'lsa):
```bash
keytool -genkeypair -v \
  -keystore photocheck.jks \
  -alias photocheck \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass photocheck123 \
  -keypass photocheck123 \
  -dname "CN=PhotoCheck, OU=FinGo, O=FinGo, L=Tashkent, ST=Tashkent, C=UZ"
```

GitHub Actions CI/CD jarayonida ushbu kalit avtomatik ravishda tekshiriladi va kerak bo'lsa generatsiya qilinadi.
