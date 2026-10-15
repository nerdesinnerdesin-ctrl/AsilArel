# BUILD_REPORT.md
- Son test: engine kural testleri 31/31 geçti (JDK 21, `engine/run_tests.sh`).
- APK derlemesi: **yapılmadı**.
- Bilinen eksikler: PROGRESS.md.
- Android/libGDX katmanı: yazıldı ama **hiç derlenmedi** (ortamda Gradle ve ağ yok). İlk gerçek derleme GitHub Actions'ta yapılacak; hata çıkması olasıdır.
- Aşama 2 (stage2 arşivi): GitHub Actions debug APK derlemesi başarılı, telefonda açıldı ve oynandı (kullanıcı onayı).
- Premium görünüm (stage3 arşivi): yazıldı, derlenmedi. FreeType native kopyalama ve dokuların performansı ilk derlemede doğrulanmalı.
