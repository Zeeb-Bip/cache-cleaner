# Cache Cleaner for Poco F4 (HyperOS / Android 14)

Aplikasi pembersih cache native Android tanpa root menggunakan Shizuku UserService & AIDL.

## Fitur
- Eksekusi aman `pm trim-caches 999G` melalui Shizuku UserService berprivilese (UID 2000).
- AIDL terisolasi dengan transaction code 16777114 untuk `destroy()`.
- Jetpack Compose + Material 3 dengan dark theme dan aksen Teal.
- Kompatibel dengan Poco F4 (munch), HyperOS 1.0+, Android 14.

## Cara Import ke Android Studio:
1. Buka Android Studio (Hedgehog / Iguana / Jellyfish / Koala).
2. Pilih **File > Open**, lalu arahkan ke folder hasil ekstrak zip ini.
3. Tunggu proses Gradle Sync selesai.
4. Jalankan perintah build: `./gradlew assembleDebug` atau klik **Run App** (Shift+F10).
