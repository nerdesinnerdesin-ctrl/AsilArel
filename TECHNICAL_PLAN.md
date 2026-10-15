# TECHNICAL_PLAN.md

## Kararlar
- **Kural motoru: saf Java (`engine/`), Android bağımlılığı yok.** Aynı kod hem istemcide hem sunucuda hamle doğrulamada kullanılabilir. Android Java'yı doğal destekler. Bu ortamda `kotlinc`/Gradle olmadığı için testler yalnızca saf Java ile gerçekten çalıştırılabiliyordu.
- Zarlar motora dışarıdan verilir: sunucu `SecureRandom` ile atar, istemci zar üretmez (hileye kapalı).
- Çok oyunculu: sunucu otoriter. İstemci hamle gönderir, sunucu `Game.play` ile doğrular; hamle sıra numarası (idempotency) ile çift uygulama engellenir. **Altyapı henüz seçilmedi** (ücretsiz limit/maliyet/güvenlik değerlendirmesi yapılacak).
- UI: **libGDX** (onaylandı): zengin ışık/gölge/animasyon ve düşük maliyetli çizim için. Java ile yazılıyor (motorla aynı dil).
- Sırlar kaynağa girmez; imzalama anahtarları GitHub Secrets.

## Devam için
PROGRESS.md'deki sıradaki aşamadan devam et.
