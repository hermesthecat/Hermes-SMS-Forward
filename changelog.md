# Changelog - Hermes SMS Forward

## [1.3.0] - 2025-07-25
- ✅ Task 4: SMS Receiver Implementation tamamlandı
  - SmsReceiver.java BroadcastReceiver sınıfı oluşturuldu
  - Gelen SMS'leri yakalama ve parsing implementasyonu
  - SharedPreferences'dan hedef numara alma entegrasyonu
  - SMS yönlendirme fonksiyonu implementasyonu
  - Uzun SMS'ler için multipart destek eklendi
  - Orijinal gönderen, mesaj ve zaman bilgilerini içeren format
  - Kapsamlı error handling ve logging implementasyonu
  - APK build başarılı olarak tamamlandı

## [1.2.0] - 2025-07-25
- ✅ Task 3: Ana Aktivite (MainActivity) geliştirildi
  - Kullanıcı dostu arayüz tasarımı oluşturuldu
  - Hedef telefon numarası girme alanı eklendi
  - Kaydet ve Başlat butonu eklendi
  - SharedPreferences ile ayar saklama implementasyonu
  - Runtime permission handling (RECEIVE_SMS, SEND_SMS)
  - Uygulama ve izin durumu göstergesi eklendi
  - Toast mesajları ile kullanıcı geri bildirimi
  - APK build başarılı olarak tamamlandı

## [1.1.0] - 2025-07-25
- ✅ Task 2: Manifest ve SMS izinleri eklendi
  - RECEIVE_SMS izni eklendi
  - SEND_SMS izni eklendi  
  - SmsReceiver BroadcastReceiver kaydı eklendi
  - SMS_RECEIVED intent filter yapılandırıldı
  - APK build başarılı olarak tamamlandı

## [1.0.0] - 2025-07-25
- ✅ Task 1: Temel proje yapısı oluşturuldu
  - Android Gradle projesi yapılandırıldı
  - Gradle wrapper dosyaları oluşturuldu
  - Minimal AndroidManifest.xml ve MainActivity eklendi
  - Temel layout ve strings kaynakları oluşturuldu
  - İlk APK build başarılı olarak tamamlandı