# RULES.md — Plakoto kuralları (motor: `engine/`)

> Durum: kurallar **eğitim bilgisinden** derlendi; güvenilir kaynaklarla çapraz doğrulama için ağ erişimi gerekiyor ve henüz yapılmadı (bkz. PROGRESS.md). Bölgesel farklar varsayılmadı; belirsiz noktalar aşağıda işaretli.

## Tahta ve yön
- 24 nokta, 15'er pul. Beyaz: tüm pullar nokta 1'de, 1→24 yönünde ilerler, ev bölgesi 19–24.
- Siyah: tüm pullar nokta 24'te, 24→1 yönünde ilerler, ev bölgesi 1–6.

## Hapsetme
- Tek (hapsedilmemiş) rakip pulunun olduğu noktaya inen pul onu hapseder (başlangıç noktaları dahil).
- Hapsedilmiş pul, üstündeki son rakip pul ayrılana kadar oynayamaz; ayrılınca serbest kalır.
- 2+ rakip pulu olan nokta veya hapis içeren nokta rakibe kapalıdır.
- Hapsedilmiş pul, "tüm pullar evde" kontrolünde sayılır (içeri alınmadan pul toplanamaz).

## Tur akışı
- Açılış zarları ayrıdır (her oyuncu 1 zar), oynanmaz; beraberlikte tekrar. Başlayan iki yeni zar atar.
- Çift zar 4 hamledir. Mümkün olan en fazla zar oynanır; yalnız biri oynanabiliyorsa mümkünse büyük olan.
- Yasal hamle yoksa tur pas geçilir (oyun kilitlenmez).
- **Özel kural:** her oyuncunun kendi ilk normal turunda, yasal hamleler elverdiği sürece en az iki farklı pul oynanır. Tek pulu toplam kadar ilerletmek bunu karşılamaz. Mümkün değilse mümkün olan en fazla yasal hamle uygulanır. Sonraki turlarda uygulanmaz.
  - Uygulama notu (**yorum**): çok-pullu bir sıra varsa, onlar arasında en uzun olan seçilir; bu, tek pulla daha çok zar oynamaktan önceliklidir. Prompt bu çakışmayı belirtmiyor.

## Pul toplama ve kazanma
- Tüm 15 pul evdeyken toplanabilir. Tam zarla çıkılır; daha büyük zarla yalnız geride (ev dışı/daha uzak) pul yoksa.
- 15 pulunu ilk toplayan kazanır. Puan: rakip hiç pul toplamadıysa **mars = 2**, aksi halde 1. (**Belirsiz/bölgesel:** puanlama varyantları değişebilir; mars tanımı doğrulanmalı.)
