# PROGRESS.md

## Aşama 1–3: İskelet, motor, kural testleri
- [x] `engine/` saf Java kural motoru: Board, Move, Game (açılış, zar, hapsetme, pul toplama, ilk-tur kuralı, pas, kazanma/mars)
- [x] `engine/run_tests.sh` — 31 test, **gerçekten çalıştırıldı, 31/31 geçti** (JDK 21)
- [ ] Kurallar harici kaynaklarla doğrulanmadı (ağ yok); RULES.md'de işaretli
- [ ] Testler: doubles'ın tüm kenar durumları, rastgele oyun simülasyonları (kilitlenme taraması), bar/ilk-tur çakışma durumu için daha geniş kapsam

## Aşama 4–5 (kısmi): Android iskeleti + yerel oyun ekranı — **DERLENMEDİ, ÇALIŞTIRILMADI**
- [~] Gradle çok modüllü yapı (`engine`, `core`, `android`), libGDX 1.12.1, AGP 8.5.2 — yazıldı, bu ortamda (ağ/Gradle yok) doğrulanamadı
- [~] `core/PlakotoApp.java`: aynı cihazda 2 kişilik oyun (açılış zarı, zar, pul seç/oyna, pul toplama, kazanma). Yer tutucu düz görünüm; premium doku/gölge/3B **yapılmadı**
- [~] `.github/workflows/android.yml`: motor testleri + debug APK + artifact — yazıldı, GitHub'da hiç çalışmadı
- [ ] Gradle wrapper (gradlew) eklenmedi; CI `gradle-version: 8.7` kullanıyor

## Aşama 4 (görünüm) — yazıldı, **GitHub'da derlenip telefonda denenmedi**
- [~] Prosedürel ceviz dokusu (çerçeve + tahta), gölgeli/bevelli/parlak pullar, ışıklı zar yüzleri, zar yuvarlanma animasyonu, pul hareket animasyonu (yay çizerek), Türkçe karakterli yazı tipi (DejaVu Sans Bold + gdx-freetype)
- [ ] Menüler/profil/sonuç ekranı, bilgisayara karşı mod hâlâ yok

## Yapılmadı
Premium görsel tasarım, menüler/profil ekranı, bilgisayara karşı mod yok, çevrim içi oda/eşleştirme, hesap/liderlik, AdMob, imzalama talimatları, BUILD_REPORT'ta gerçek derleme sonucu.
