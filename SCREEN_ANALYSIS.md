# 📱 HERMES SMS FORWARD - TÜM EKRANLAR DETAYLI ANALİZ RAPORU

**Versiyon**: 2.43.0 (Build 64)
**Tarih**: 2025-10-01
**Hazırlayan**: Claude Code Analysis
**Min SDK**: 21 (Android 5.0)
**Target SDK**: 34 (Android 14)

---

## 📊 GENEL İSTATİSTİKLER

- **Toplam Activity**: 8 (7 normal + 1 debug-only)
- **Toplam Fragment**: 6 (5 onboarding + 1 settings)
- **Toplam Dialog**: 5
- **Toplam Layout Dosyası**: 22
- **Toplam RecyclerView Item Layout**: 4
- **Menü Öğesi**: 6 (5 normal + 1 debug)

---

## 🎯 1. MAIN ACTIVITY (Ana Dashboard Ekranı)

**Dosya**: `MainActivity.java` | **Layout**: `activity_main.xml`
**Launcher**: ✅ Evet (Uygulama açılışta bu ekran açılır)
**Parent**: Yok (Root activity)

### Amaç

Ana kontrol paneli - Uygulama durumu ve izinleri gösterir.

### Özellikler

- ✅ **İzin Durumu Göstergesi**: 3 izin için real-time durum
  - RECEIVE_SMS: SMS alma izni (✓/✗)
  - SEND_SMS: SMS gönderme izni (✓/✗)
  - READ_PHONE_STATE: SIM bilgisi okuma izni (✓/✗)
- ✅ **Onboarding Kontrolü**: İlk açılışta OnboardingActivity'ye yönlendirir
- ✅ **İzin İsteği**: Eksik izinler otomatik olarak istenir
- ✅ **Menü Navigasyonu**: Overflow menüden 6 ekrana erişim
- ✅ **StatisticsManager**: Oturum takibi ve analitik
- ✅ **Dil Desteği**: LanguageManager entegrasyonu

### UI Bileşenleri

```text
┌─────────────────────────────────────┐
│ Hermes SMS Forward      [⋮ Menu]    │
├─────────────────────────────────────┤
│                                     │
│  Uygulama Açıklaması                │
│  (description_text)                 │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ Gerekli İzinler             │   │
│  │                             │   │
│  │  ✓ SMS Alma İzni            │   │
│  │  ✓ SMS Gönderme İzni        │   │
│  │  ✗ Telefon Durumu İzni      │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

### Menü Öğeleri

1. **Hedef Numaralar** → TargetNumbersActivity
2. **Filtre Kuralları** → FilterRulesActivity
3. **Geçmiş** → HistoryActivity
4. **Analitikler** → AnalyticsActivity
5. **Ayarlar** → SettingsActivity
6. **SIM Debug** → SimDebugActivity (Debug only)

### Yaşam Döngüsü

```java
onCreate() →
  Onboarding kontrolü →
    İzin kontrolü →
      UI güncelle →
        onResume() →
          onDestroy() (StatisticsManager session end)
```

**Code Reference**: `MainActivity.java:20-184`

---

## 🎓 2. ONBOARDING ACTIVITY (İlk Kullanım Rehberi)

**Dosya**: `OnboardingActivity.java` | **Layout**: `activity_onboarding.xml`
**Launcher**: ❌ Hayır (MainActivity'den redirect)
**Parent**: Yok (finish sonrası MainActivity açılır)

### Amaç

Yeni kullanıcılar için 5 adımlı onboarding süreci.

### Özellikler

- ✅ **ViewPager2**: Swipe ile sayfa geçişi
- ✅ **5 Fragment**: Her adım ayrı fragment
- ✅ **Progress Indicator**: Adım göstergesi (1/5, 2/5, etc.)
- ✅ **Linear Progress Bar**: Görsel ilerleme çubuğu
- ✅ **Skip Butonu**: Onboarding'i atla
- ✅ **Back/Next Butonları**: Sayfa navigasyonu
- ✅ **SharedPreferences**: `onboarding_completed` flag
- ✅ **Portrait Mode**: Sadece dikey ekran

### Fragment Sırası

#### **Fragment 1: WelcomeFragment**

**Layout**: `fragment_welcome.xml`
**Dosya**: `onboarding/WelcomeFragment.java`

- Uygulama hoş geldin mesajı
- Uygulama tanıtımı
- İkon ve başlık

#### **Fragment 2: PermissionExplanationFragment**

**Layout**: `fragment_permission_explanation.xml`
**Dosya**: `onboarding/PermissionExplanationFragment.java`

- İzinlerin neden gerekli olduğunu açıklar
- RECEIVE_SMS, SEND_SMS, READ_PHONE_STATE açıklamaları
- Güvenlik bilgisi

#### **Fragment 3: TargetNumberSetupFragment**

**Layout**: `fragment_target_number_setup.xml`
**Dosya**: `onboarding/TargetNumberSetupFragment.java` (6391 satır - en kompleks)

- İlk hedef numarası ekleme
- Numarası validation
- Database'e kaydetme

#### **Fragment 4: FilterIntroFragment**

**Layout**: `fragment_filter_intro.xml`
**Dosya**: `onboarding/FilterIntroFragment.java`

- Filtre sisteminin tanıtımı
- Include/Exclude pattern açıklaması
- Örnek kullanım senaryoları

#### **Fragment 5: CompletionFragment**

**Layout**: `fragment_completion.xml`
**Dosya**: `onboarding/CompletionFragment.java`

- Tebrikler mesajı
- Onboarding tamamlandı
- "Başla" butonu → MainActivity

### UI Yapısı

```text
┌─────────────────────────────────────┐
│ ────────────────────── 40%          │ ← Progress Bar
│                                     │
│  [ViewPager2 - Fragment Content]   │
│                                     │
│         ●●●○○                       │ ← Page Dots
│                                     │
│     Adım 2/5                        │ ← Step Indicator
│                                     │
│ [Atla]        [Geri]    [İleri]    │ ← Navigation
└─────────────────────────────────────┘
```

### Veri Akışı

```text
onboarding_completed = false (default)
    ↓
WelcomeFragment → PermissionExplanationFragment →
TargetNumberSetupFragment (kaydet) → FilterIntroFragment →
CompletionFragment →
    ↓
onboarding_completed = true
    ↓
MainActivity
```

**Code Reference**: `OnboardingActivity.java:24-150`

---

## 🎯 3. TARGET NUMBERS ACTIVITY (Hedef Numaralar Yönetimi)

**Dosya**: `TargetNumbersActivity.java` | **Layout**: `activity_target_numbers.xml`
**Parent**: MainActivity

### Amaç

SMS yönlendirilecek hedef telefon numaralarını yönetir.

### Özellikler

- ✅ **RecyclerView**: Hedef numaraları listesi
- ✅ **FAB (Floating Action Button)**: Yeni hedef ekle
- ✅ **Add Dialog**: Hedef ekleme modalı
- ✅ **Phone Validation**: PhoneNumberValidator ile doğrulama
- ✅ **Primary Target**: Bir hedef "ana" olarak işaretlenebilir
- ✅ **Enable/Disable**: Hedefler geçici olarak devre dışı bırakılabilir
- ✅ **Dual SIM Support**: Her hedef için SIM seçimi
- ✅ **Sending Mode Spinner**: Sequential/Parallel gönderim
- ✅ **Empty State**: Liste boşken mesaj göster
- ✅ **Real-time Validation**: Kullanıcı yazarken validasyon
- ✅ **ThreadManager**: Background database işlemleri

### Dialog: Add Target Number

**Layout**: `dialog_add_target_number.xml`

**Alanlar**:

1. **Telefon Numarası** (zorunlu)
   - Input type: phone
   - Real-time validation
   - Format: +90XXXXXXXXXX

2. **Görünen İsim** (opsiyonel)
   - Input type: textPersonName

3. **SIM Seçimi** (dual SIM cihazlarda)
   - RadioGroup:
     - ⚪ Otomatik (varsayılan)
     - ⚪ Kaynak SIM
     - ⚪ Belirli SIM → Spinner görünür

4. **Ana Hedef** (checkbox)
   - Birincil hedef işareti

5. **Etkin** (checkbox, default: checked)
   - Aktif/pasif durumu

### RecyclerView Item Layout

**Layout**: `item_target_number.xml`

```text
┌─────────────────────────────────────┐
│ [ANA] +905551234567      [SIM 1]    │
│ Ahmet'in Telefonu                   │
│ Son Kullanım: 2 saat önce           │
│                   [⋮ Menu]          │
└─────────────────────────────────────┘
```

### Item Menü Seçenekleri

- 📝 **Düzenle**: Hedefi düzenle
- 🗑️ **Sil**: Hedefi sil (onay dialog)
- 🔄 **Ana Yap**: Primary hedef olarak ayarla
- ✓/✗ **Etkinlik**: Aktif/pasif değiştir

### UI Bileşenleri

```text
┌─────────────────────────────────────┐
│ ← Hedef Numaralar                   │
├─────────────────────────────────────┤
│                                     │
│  Gönderim Modu: [Sequential ▼]     │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ [ANA] +905551234567         │   │
│  │ Ahmet                       │   │
│  │ Son: 2 saat önce    [⋮]    │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ +905559876543               │   │
│  │ Mehmet                      │   │
│  │ Son: 1 gün önce     [⋮]    │   │
│  └─────────────────────────────┘   │
│                                     │
│                                     │
│                           [+] FAB   │
└─────────────────────────────────────┘
```

### Database İşlemleri

```java
// Insert
targetNumberDao.insert(targetNumber)

// Update primary
targetNumberDao.setPrimaryTargetNumber(id)

// Query
targetNumberDao.getAllTargetNumbers()

// Delete
targetNumberDao.deleteTargetNumber(id)
```

**Code Reference**: `TargetNumbersActivity.java:34-650`

---

## 🎛️ 4. FILTER RULES ACTIVITY (Filtre Kuralları Yönetimi)

**Dosya**: `FilterRulesActivity.java` | **Layout**: `activity_filter_rules.xml`
**Parent**: MainActivity

### Amaç

Hangi SMS'lerin yönlendirileceğini belirleyen filtre kurallarını yönetir.

### Özellikler

- ✅ **RecyclerView**: Filtre kuralları listesi
- ✅ **FAB**: Yeni filtre ekle
- ✅ **Add Dialog**: Filtre ekleme modalı
- ✅ **Test Dialog**: Filtreyi test et
- ✅ **Pattern Type**: Include (izin ver) / Exclude (engelle)
- ✅ **Pattern Matching**: Regex-like pattern desteği
- ✅ **Priority**: Filtre öncelik sırası
- ✅ **Enable/Disable**: Filtreleri geçici kapat
- ✅ **Empty State**: Liste boşken mesaj
- ✅ **FilterEngine**: Gerçek zamanlı filtre testi

### Dialog: Add Filter

**Layout**: `dialog_add_filter.xml`

**Alanlar**:

1. **Filtre Adı** (zorunlu)
   - Filtreyi tanımlamak için

2. **Filtre Pattern'i** (zorunlu)
   - Metin deseni
   - Örnek: "BANK", "*kod*", "556*"

3. **Pattern Tipi**
   - RadioGroup:
     - ⚪ Include (Bu pattern'e uyanları yönlendir)
     - ⚪ Exclude (Bu pattern'e uyanları engelle)

4. **Etkin** (checkbox, default: checked)

### Dialog: Test Filter

**Layout**: `dialog_filter_test.xml`

**Özellikler**:

- Test SMS metni gir
- Tüm filtrelere karşı test et
- Sonuç: ✓ Yönlendirilir / ✗ Engellenir
- Hangi filtrenin tetiklendiğini göster

### RecyclerView Item Layout

**Layout**: `item_filter_rule.xml`

```text
┌─────────────────────────────────────┐
│ [ETKİN] BANK SMS Filtresi           │
│ Pattern: "BANK*"                    │
│ Tip: Include | Öncelik: 1          │
│                   [⋮ Menu]          │
└─────────────────────────────────────┘
```

### Item Menü Seçenekleri

- 📝 **Düzenle**: Filtreyi düzenle
- 🗑️ **Sil**: Filtreyi sil
- 🧪 **Test**: Filtre test dialog'u aç
- ✓/✗ **Etkinlik**: Aktif/pasif değiştir
- ⬆️⬇️ **Öncelik**: Sırayı değiştir

### UI Yapısı

```text
┌─────────────────────────────────────┐
│ ← Filtre Kuralları                  │
├─────────────────────────────────────┤
│                                     │
│  ┌─────────────────────────────┐   │
│  │ [ETKİN] BANK SMS            │   │
│  │ Pattern: "BANK*"            │   │
│  │ Tip: Include    [⋮]         │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ [DEVRE DIŞI] SPAM Filter    │   │
│  │ Pattern: "*reklam*"         │   │
│  │ Tip: Exclude    [⋮]         │   │
│  └─────────────────────────────┘   │
│                                     │
│                                     │
│                           [+] FAB   │
└─────────────────────────────────────┘
```

### Filtre Mantığı

```java
FilterEngine.shouldForward(message, filterRules)
    ↓
1. Tüm EXCLUDE filtrelerini kontrol et
   - Eşleşme varsa → ❌ Engelle
    ↓
2. Tüm INCLUDE filtrelerini kontrol et
   - Eşleşme varsa → ✓ Yönlendir
    ↓
3. Hiçbir filtre yok → ✓ Yönlendir (default allow)
```

**Code Reference**: `FilterRulesActivity.java:35-750`

---

## 📜 5. HISTORY ACTIVITY (SMS Geçmişi)

**Dosya**: `HistoryActivity.java` | **Layout**: `activity_history.xml`
**Parent**: MainActivity
**Theme**: `Theme.HermesSmsForward.NoActionBar` (Custom toolbar)

### Amaç

Yönlendirilen tüm SMS'lerin geçmişini gösterir.

### Özellikler

- ✅ **RecyclerView**: SMS geçmiş listesi
- ✅ **Custom Toolbar**: Material toolbar
- ✅ **SwipeRefreshLayout**: Pull-to-refresh
- ✅ **SearchView**: Menüde arama özelliği
- ✅ **Auto Cleanup**: Açılışta eski kayıtları temizle
- ✅ **Empty State**: Kayıt yoksa mesaj
- ✅ **Filtering**: Arama ile filtreleme
- ✅ **ThreadManager**: Background database sorguları
- ✅ **Status Indicators**: Başarılı/Başarısız göstergesi

### RecyclerView Item Layout

**Layout**: `item_sms_history.xml`

```
┌─────────────────────────────────────┐
│ ✓ +905551234567                     │
│ Gönderen: +905559876543             │
│ "BANK: Hesabınıza 500 TL yatırıldı"│
│ 15 Kas 2024, 14:32                  │
│ Durum: Gönderildi | SIM 1           │
└─────────────────────────────────────┘
```

### Toolbar Menü

- 🔍 **Arama**: SearchView açılır
- 🗑️ **Geçmişi Temizle**: Tüm kayıtları sil (onay dialog)
- 📤 **Dışa Aktar**: CSV export

### Search Fonksiyonu

```java
onQueryTextChange(newText)
    ↓
Filter allHistory list
    ↓
filteredHistory.add(item) if:
    - Gönderen numara contains query
    - Alıcı numara contains query
    - SMS içerik contains query
    ↓
Update RecyclerView
```

### UI Yapısı

```text
┌─────────────────────────────────────┐
│ ← SMS Geçmişi          [🔍] [⋮]     │
├─────────────────────────────────────┤
│ ▼ Pull to refresh                   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ✓ +905551234567             │   │
│  │ Gönderen: +90555...         │   │
│  │ "BANK: Hesabınıza..."      │   │
│  │ 15 Kas 2024, 14:32          │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ ✗ +905552223344             │   │
│  │ Gönderen: +90555...         │   │
│  │ "Şifreniz: 123456"          │   │
│  │ 14 Kas 2024, 09:15          │   │
│  │ Hata: Network error         │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

### Database Query

```java
smsHistoryDao.getAllHistory()
// Order by timestamp DESC
// Limit: Last 1000 records
```

**Code Reference**: `HistoryActivity.java:20-200`

---

## 📊 6. ANALYTICS ACTIVITY (İstatistikler Dashboard)

**Dosya**: `AnalyticsActivity.java` | **Layout**: `activity_analytics.xml`
**Parent**: MainActivity

### Amaç

Privacy-first local analytics - Kullanım istatistiklerini gösterir.

### Özellikler

- ✅ **Privacy-First**: Hiçbir veri dışarı gönderilmez
- ✅ **StatisticsManager**: Local analytics engine
- ✅ **CardViews**: İstatistik kartları
- ✅ **Progress Bars**: Başarı oranı göstergeleri
- ✅ **Time Periods**: Bugün/Hafta/Ay istatistikleri
- ✅ **SIM Statistics**: Dual SIM kullanım analizi
- ✅ **Export**: CSV/JSON export
- ✅ **Refresh**: Manuel yenileme butonu
- ✅ **Most Common Error**: En sık hata türü

### İstatistik Kartları

#### **Card 1: Genel Özet**

```text
┌─────────────────────────────────────┐
│ GENEL İSTATİSTİKLER                 │
├─────────────────────────────────────┤
│ Toplam Alınan SMS: 1,234            │
│ Toplam Yönlendirilen: 1,150         │
│ Başarı Oranı: ████████░░ 93%       │
│ Toplam Hata: 84                     │
│ Ort. İşlem Süresi: 1.2 sn           │
│ Uygulama Açılışı: 156 kez          │
│ En Sık Hata: Network Timeout        │
└─────────────────────────────────────┘
```

#### **Card 2: Bugün**

```text
┌─────────────────────────────────────┐
│ BUGÜN                               │
├─────────────────────────────────────┤
│ Alınan: 45                          │
│ Yönlendirilen: 42                   │
│ Başarı Oranı: 93%                   │
│ Hata: 3                             │
└─────────────────────────────────────┘
```

#### **Card 3: Bu Hafta**

```text
┌─────────────────────────────────────┐
│ BU HAFTA                            │
├─────────────────────────────────────┤
│ Alınan: 312                         │
│ Yönlendirilen: 289                  │
│ Başarı Oranı: 93%                   │
│ Hata: 23                            │
└─────────────────────────────────────┘
```

#### **Card 4: Bu Ay**

```text
┌─────────────────────────────────────┐
│ BU AY                               │
├─────────────────────────────────────┤
│ Alınan: 1,234                       │
│ Yönlendirilen: 1,150                │
│ Başarı Oranı: 93%                   │
│ Hata: 84                            │
└─────────────────────────────────────┘
```

#### **Card 5: SIM İstatistikleri** (Dual SIM only)

```text
┌─────────────────────────────────────┐
│ SIM KART İSTATİSTİKLERİ             │
├─────────────────────────────────────┤
│ SIM 1 (Turkcell):                   │
│   Alınan: 678                       │
│   Yönlendirilen: 645                │
│   Başarı: ████████░░ 95%           │
│                                     │
│ SIM 2 (Vodafone):                   │
│   Alınan: 556                       │
│   Yönlendirilen: 505                │
│   Başarı: ███████░░░ 91%           │
│                                     │
│ SIM Geçişi: 23 kez                  │
│ En Çok Kullanılan: SIM 1            │
└─────────────────────────────────────┘
```

### Toolbar Menü

- 📤 **Export CSV**: İstatistikleri CSV olarak kaydet
- 📤 **Export JSON**: JSON formatında export
- 📤 **Share**: Analytics dosyasını paylaş
- 🗑️ **Clear Analytics**: Tüm istatistikleri sil

### Butonlar

- 🔄 **Refresh**: İstatistikleri yenile
- 📤 **Export**: Export seçenekleri

### UI Yapısı

```text
┌─────────────────────────────────────┐
│ ← İstatistikler          [⋮]        │
├─────────────────────────────────────┤
│ ▼ Scroll                            │
│                                     │
│  [Genel Özet Card]                  │
│  [Bugün Card]                       │
│  [Bu Hafta Card]                    │
│  [Bu Ay Card]                       │
│  [SIM İstatistikleri Card]          │
│                                     │
│  [🔄 Yenile]  [📤 Dışa Aktar]       │
│                                     │
│  Son Güncelleme: 14:32              │
└─────────────────────────────────────┘
```

### Analytics Events Tracked

```java
- SMS_RECEIVED
- SMS_FORWARDED
- SMS_FAILED
- PERMISSION_REQUESTED
- PERMISSION_GRANTED
- PERMISSION_DENIED
- APP_OPENED
- APP_CLOSED
- SETTINGS_CHANGED
- TARGET_ADDED
- FILTER_ADDED
- SIM_SWITCHED
```

**Code Reference**: `AnalyticsActivity.java:24-450`

---

## ⚙️ 7. SETTINGS ACTIVITY (Ayarlar)

**Dosya**: `SettingsActivity.java` | **Layout**: `activity_settings.xml`
**Parent**: MainActivity
**Fragment**: `SettingsFragment extends PreferenceFragmentCompat`
**XML**: `res/xml/preferences.xml`
**Lines**: 926 satır

### Amaç

Uygulama konfigürasyonları ve gelişmiş ayarlar.

### Özellikler

- ✅ **PreferenceFragmentCompat**: Modern preferences API
- ✅ **PreferenceScreen XML**: Deklaratif ayar tanımları
- ✅ **Kategoriler**: 4 ana kategori
- ✅ **Dependency**: Bazı ayarlar diğerine bağlı
- ✅ **Custom Preferences**: Dialog ile özel ayarlar
- ✅ **Language Switching**: Runtime dil değişimi
- ✅ **Backup/Restore**: BackupManager entegrasyonu
- ✅ **Dual SIM Settings**: SIM seçenekleri
- ✅ **Format Preview**: SMS format önizleme

### Ayar Kategorileri

#### **1. Yönlendirme Ayarları** (`category_forwarding`)

**SwitchPreference**:

- ✅ **SMS Yönlendirmeyi Etkinleştir** (`pref_forwarding_enabled`)
  - Default: true
  - Ana anahtar

**SeekBarPreference**:

- ⏱️ **Yönlendirme Gecikmesi** (`pref_forwarding_delay`)
  - Range: 0-10 saniye
  - Default: 0
  - Gecikmeli gönderim

**ListPreference**:

- 📋 **SMS Format Stili** (`sms_format_type`)
  - Standard (Varsayılan)
  - Compact (Kısa)
  - Detailed (Detaylı)
  - Custom (Özel şablon)

**EditTextPreference**:

- 📝 **Özel Başlık** (`custom_header`)
  - Default: "Hermes SMS Forward"
  - SMS başlığı

**SwitchPreference**:

- 🕐 **Zaman Damgası Ekle** (`include_timestamp`)
  - Default: true
  - SMS'e tarih/saat ekle

- 📱 **SIM Bilgisi Ekle** (`include_sim_info`)
  - Default: true
  - Hangi SIM'den geldiğini göster

**ListPreference**:

- 📅 **Tarih Formatı** (`date_format`)
  - dd/MM/yyyy HH:mm:ss (Default)
  - MM/dd/yyyy HH:mm:ss
  - yyyy-MM-dd HH:mm:ss
  - dd.MM.yyyy HH:mm:ss

**Preference** (Custom Dialog):

- 🎨 **Özel SMS Şablonu** (`custom_sms_template`)
  - Dialog açar
  - Layout: `dialog_custom_template.xml`
  - Placeholder'lar: {sender}, {message}, {time}, {sim}

- 👁️ **Format Önizleme** (`format_preview`)
  - Seçili formatın örneğini gösterir

#### **2. Bildirim Ayarları** (`category_notifications`)

**SwitchPreference**:

- 🔔 **Bildirimleri Göster** (`pref_show_notifications`)
  - Default: true
  - Master notification switch

- 🔊 **Bildirim Sesi** (`pref_notification_sound`)
  - Default: false
  - Dependency: `pref_show_notifications`

- 📳 **Bildirim Titreşimi** (`pref_notification_vibration`)
  - Default: false
  - Dependency: `pref_show_notifications`

- 📞 **Cevapsız Arama Bildirimleri** (`missed_call_notifications_enabled`)
  - Default: false
  - MissedCallReceiver entegrasyonu

#### **3. Gelişmiş Ayarlar** (`category_advanced`)

**ListPreference**:

- 🌍 **Dil Seçimi** (`pref_app_language`)
  - Auto (Sistem)
  - Türkçe (tr)
  - English (en)
  - Deutsch (de)
  - Italiano (it)
  - Français (fr)
  - Español (es)
  - Restart required

- 📝 **Log Seviyesi** (`pref_log_level`)
  - Error (Default)
  - Warning
  - Info
  - Debug
  - Verbose

**Preference** (Custom Action):

- 💾 **Ayarları Yedekle** (`pref_backup_settings`)
  - BackupManager.backup()
  - JSON export
  - File picker

- 📥 **Ayarları Geri Yükle** (`pref_restore_settings`)
  - BackupManager.restore()
  - JSON import
  - File picker

#### **4. Dual SIM Ayarları** (`category_dual_sim`)

**Visibility**: Sadece dual SIM cihazlarda görünür

**ListPreference**:

- 📡 **Varsayılan Yönlendirme SIM'i** (`pref_default_forwarding_sim`)
  - Auto (Sistem varsayılanı)
  - SIM 1
  - SIM 2
  - Hedef bazlı ayarları override etmez

**SwitchPreference**:

- 👁️ **SIM Göstergelerini Göster** (`pref_show_sim_indicators`)
  - Default: true
  - UI'da SIM badge'leri göster

**Preference** (Custom Dialog):

- ℹ️ **SIM Bilgileri** (`pref_sim_information`)
  - Dialog: SIM detayları
  - Layout: `dialog_sim_selection.xml`
  - Carrier, Slot, Subscription ID

#### **5. Hakkında** (`category_about`)

**Preference** (Non-clickable):

- ℹ️ **Uygulama Sürümü** (`pref_app_version`)
  - Selectable: false
  - Summary: "2.43.0 (Build 64)"

**Preference**:

- 👤 **Geliştirici Bilgileri** (`pref_developer_info`)
  - Dialog: Developer info
  - GitHub, Email, Website

### UI Yapısı

```text
┌─────────────────────────────────────┐
│ ← Ayarlar                           │
├─────────────────────────────────────┤
│ ▼ Scroll                            │
│                                     │
│ ▼ YÖNLENDİRME AYARLARI              │
│   ☑ SMS Yönlendirmeyi Etkinleştir  │
│   ⎯⎯⎯⎯⎯●⎯⎯⎯⎯⎯ Gecikme: 0 sn         │
│   SMS Format: Standard ▼            │
│   Özel Başlık: "Hermes..."          │
│   ☑ Zaman Damgası Ekle              │
│   ☑ SIM Bilgisi Ekle                │
│   Tarih Formatı: dd/MM/yyyy ▼       │
│   > Özel SMS Şablonu                │
│   > Format Önizleme                 │
│                                     │
│ ▼ BİLDİRİM AYARLARI                 │
│   ☑ Bildirimleri Göster             │
│   ☐ Bildirim Sesi                   │
│   ☐ Bildirim Titreşimi              │
│   ☐ Cevapsız Arama Bildirimleri     │
│                                     │
│ ▼ GELİŞMİŞ AYARLAR                  │
│   Dil: Türkçe ▼                     │
│   Log Seviyesi: Error ▼             │
│   > Ayarları Yedekle                │
│   > Ayarları Geri Yükle             │
│                                     │
│ ▼ DUAL SIM AYARLARI                 │
│   Varsayılan SIM: Auto ▼            │
│   ☑ SIM Göstergelerini Göster       │
│   > SIM Bilgileri                   │
│                                     │
│ ▼ HAKKINDA                          │
│   Sürüm: 2.43.0 (Build 64)          │
│   > Geliştirici Bilgileri           │
│                                     │
└─────────────────────────────────────┘
```

### Custom Dialogs

#### **Custom Template Dialog**

**Layout**: `dialog_custom_template.xml`

```text
┌─────────────────────────────────────┐
│ Özel SMS Şablonu              [X]   │
├─────────────────────────────────────┤
│                                     │
│ Şablonunuzu girin:                  │
│ ┌─────────────────────────────────┐ │
│ │ Gönderen: {sender}              │ │
│ │ Mesaj: {message}                │ │
│ │ Zaman: {time}                   │ │
│ │ SIM: {sim}                      │ │
│ └─────────────────────────────────┘ │
│                                     │
│ Kullanılabilir Placeholder'lar:     │
│ • {sender} - Gönderen numara        │
│ • {message} - SMS içerik            │
│ • {time} - Zaman damgası            │
│ • {sim} - SIM bilgisi               │
│ • {header} - Özel başlık            │
│                                     │
│           [İptal]  [Kaydet]         │
└─────────────────────────────────────┘
```

**Code Reference**: `SettingsActivity.java:19-926`

---

## 🐛 8. SIM DEBUG ACTIVITY (SIM Hata Ayıklama)

**Dosya**: `SimDebugActivity.java` | **Layout**: `activity_sim_debug.xml`
**Parent**: MainActivity
**Visibility**: Debug builds only (`@bool/is_debug_build`)
**Theme**: `Theme.HermesSmsForward.NoActionBar`

### Amaç

Debug build'lerde SIM kartları ile ilgili detaylı bilgi gösterir.

### Özellikler

- ✅ **Debug Only**: Release'de devre dışı
- ✅ **System Info**: Android sürümü, API level, cihaz
- ✅ **SIM Status**: Dual SIM durumu
- ✅ **ScrollView**: Uzun log metinleri için
- ✅ **Test Buttons**: SIM seçim testleri
- ✅ **Refresh**: Bilgileri yenileme

### Gösterilen Bilgiler

```text
=== SIM DEBUG INFORMATION ===

SYSTEM INFO:
Android Version: 13
API Level: 33
Device: Samsung Galaxy S21
Build Type: Debug

DUAL SIM STATUS:
SIM Debug Activity loaded successfully!
Use the buttons below to test functionality.

=== END DEBUG INFO ===
```

### Butonlar

- 🔄 **Refresh**: Bilgileri yenile
- 🧪 **Test SIM Selection**: Test butonu

### UI Yapısı

```text
┌─────────────────────────────────────┐
│ ← SIM Debug Info                    │
├─────────────────────────────────────┤
│ ▼ ScrollView                        │
│                                     │
│  === SIM DEBUG INFORMATION ===     │
│                                     │
│  SYSTEM INFO:                       │
│  Android Version: 13                │
│  API Level: 33                      │
│  Device: Samsung Galaxy S21         │
│  Build Type: Debug                  │
│                                     │
│  DUAL SIM STATUS:                   │
│  API Support: Yes (API 22+)         │
│  Dual SIM Detected: Yes             │
│  Active SIMs: 2                     │
│                                     │
│  SIM 1:                             │
│    Carrier: Turkcell                │
│    Slot: 0                          │
│    Subscription ID: 1               │
│    Display Name: İş Hattı           │
│                                     │
│  === END DEBUG INFO ===             │
│                                     │
│  [🔄 Refresh]  [🧪 Test Selection]  │
│                                     │
└─────────────────────────────────────┘
```

### Launch Conditions

```java
// Only in debug builds
android:enabled="@bool/is_debug_build"

// values/bools.xml:
<bool name="is_debug_build">false</bool>

// values-debug/bools.xml:
<bool name="is_debug_build">true</bool>
```

**Code Reference**: `SimDebugActivity.java:14-94`

---

## 🎨 DIALOG'LAR (Özel Modal'lar)

### 1. **Add Target Number Dialog**

**Layout**: `dialog_add_target_number.xml`
**Kullanım**: TargetNumbersActivity
**Özellikler**: [Bölüm 3'te detaylı anlatıldı]

### 2. **Add Filter Dialog**

**Layout**: `dialog_add_filter.xml`
**Kullanım**: FilterRulesActivity
**Özellikler**: [Bölüm 4'te detaylı anlatıldı]

### 3. **Filter Test Dialog**

**Layout**: `dialog_filter_test.xml`
**Kullanım**: FilterRulesActivity

**UI**:

```text
┌─────────────────────────────────────┐
│ Filtreyi Test Et              [X]   │
├─────────────────────────────────────┤
│                                     │
│ Test SMS Metni:                     │
│ ┌─────────────────────────────────┐ │
│ │ BANK: Hesabınıza 500 TL...     │ │
│ └─────────────────────────────────┘ │
│                                     │
│                [Test Et]            │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ Sonuç:                          │ │
│ │ ✓ Bu SMS yönlendirilecek        │ │
│ │                                 │ │
│ │ Tetiklenen Filtre:              │ │
│ │ • "BANK SMS Filtresi" (Include) │ │
│ └─────────────────────────────────┘ │
│                                     │
│              [Kapat]                │
└─────────────────────────────────────┘
```

### 4. **SIM Selection Dialog**

**Layout**: `dialog_sim_selection.xml`
**Kullanım**: SettingsActivity (SIM Info preference)

**UI**:

```text
┌─────────────────────────────────────┐
│ SIM Kartı Seçimi              [X]   │
├─────────────────────────────────────┤
│                                     │
│ Kullanmak istediğiniz SIM kartını   │
│ seçin:                              │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ ⚪ SIM 1 (Turkcell)              │ │
│ │    Slot: 0 | SubID: 1           │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ ⚪ SIM 2 (Vodafone)              │ │
│ │    Slot: 1 | SubID: 2           │ │
│ └─────────────────────────────────┘ │
│                                     │
│           [İptal]  [Seç]            │
└─────────────────────────────────────┘
```

**RecyclerView Item**: `item_sim_selection.xml`

### 5. **Custom Template Dialog**

**Layout**: `dialog_custom_template.xml`
**Kullanım**: SettingsActivity
**Özellikler**: [Bölüm 7'de detaylı anlatıldı]

---

## 📋 RECYCLERVIEW ITEM LAYOUT'LARI

### 1. **item_target_number.xml**

**Kullanım**: TargetNumbersActivity
**Özellikler**: [Bölüm 3'te detaylı anlatıldı]

### 2. **item_filter_rule.xml**

**Kullanım**: FilterRulesActivity
**Özellikler**: [Bölüm 4'te detaylı anlatıldı]

### 3. **item_sms_history.xml**

**Kullanım**: HistoryActivity
**Özellikler**: [Bölüm 5'te detaylı anlatıldı]

### 4. **item_sim_selection.xml**

**Kullanım**: SIM Selection Dialog
**Özellikler**: [Dialog 4'te detaylı anlatıldı]

---

## 🗺️ EKRAN AKIŞ DİYAGRAMI

```text
┌──────────────────────────────────────────────────────────────────┐
│                        APP LAUNCH                                │
└──────────────────────────────────────────────────────────────────┘
                              ↓
                  ┌───────────────────────┐
                  │ Onboarding Completed? │
                  └───────────────────────┘
                     YES ↓           ↓ NO
                         ↓           ↓
         ┌───────────────┘           └──────────────────┐
         ↓                                              ↓
┌─────────────────┐                        ┌──────────────────────┐
│  MainActivity   │                        │ OnboardingActivity   │
│   (Dashboard)   │                        │   (5 Fragments)      │
└─────────────────┘                        └──────────────────────┘
         │                                              │
         │  [Menu Navigation]                           │
         │                                              ↓
         ├──→ Target Numbers ──→ TargetNumbersActivity │
         │         ↓                                    │
         │    [FAB Click]                              ↓
         │         ↓                          [Finish Onboarding]
         │   Add Target Dialog                          │
         │         ↓                                     │
         │   [Save to DB]                                │
         │                                              ↓
         ├──→ Filter Rules ──→ FilterRulesActivity    Back to
         │         ↓                                  MainActivity
         │    [FAB Click]
         │         ↓
         │   Add Filter Dialog
         │         ↓
         │   [Save to DB]
         │
         ├──→ History ──→ HistoryActivity
         │         ↓
         │   [SearchView]
         │         ↓
         │   [Filter List]
         │
         ├──→ Analytics ──→ AnalyticsActivity
         │         ↓
         │   [Export Button]
         │         ↓
         │   [Share Analytics]
         │
         ├──→ Settings ──→ SettingsActivity
         │         ↓           ↓
         │    [Preferences]   ↓
         │         ↓           ↓
         │   [Custom Dialog] [Backup/Restore]
         │
         └──→ SIM Debug ──→ SimDebugActivity (Debug only)
                  ↓
            [Refresh Button]
                  ↓
            [Test Buttons]
```

---

## 📊 EKRAN KARŞILAŞTIRMA TABLOSU

| Ekran | Activity/Fragment | Layout Count | Dialog Count | RecyclerView | Database | Permissions | Special Features |
|-------|-------------------|--------------|--------------|--------------|----------|-------------|------------------|
| **Main** | Activity | 1 | 0 | ❌ | ❌ | ✅ Check | Menu, Onboarding redirect |
| **Onboarding** | Activity + 5 Fragments | 6 | 0 | ❌ | ✅ Insert | ❌ | ViewPager2, Progress |
| **Target Numbers** | Activity | 1 | 1 | ✅ | ✅ CRUD | ❌ | FAB, Validation, Dual SIM |
| **Filter Rules** | Activity | 1 | 2 | ✅ | ✅ CRUD | ❌ | FAB, Test, Priority |
| **History** | Activity | 1 | 0 | ✅ | ✅ Read | ❌ | Search, Swipe Refresh |
| **Analytics** | Activity | 1 | 0 | ❌ | ✅ Read | ❌ | Cards, Export, Stats |
| **Settings** | Activity + Fragment | 1 | 3 | ❌ | ❌ | ❌ | PreferenceScreen, Backup |
| **SIM Debug** | Activity | 1 | 0 | ❌ | ❌ | ❌ | Debug only, System info |

---

## 🎯 ÖZET

### **Toplam Ekran Sayısı**: **8 Ana Ekran**

1. ✅ MainActivity (Dashboard)
2. ✅ OnboardingActivity (5 Fragment)
3. ✅ TargetNumbersActivity
4. ✅ FilterRulesActivity
5. ✅ HistoryActivity
6. ✅ AnalyticsActivity
7. ✅ SettingsActivity (+ SettingsFragment)
8. ✅ SimDebugActivity (Debug only)

### **Fragment Sayısı**: **6**

- 5 Onboarding Fragment
- 1 Settings Fragment

### **Dialog Sayısı**: **5**

- Add Target Number
- Add Filter
- Filter Test
- SIM Selection
- Custom Template

### **RecyclerView Kullanımı**: **4 Ekran**

- TargetNumbersActivity
- FilterRulesActivity
- HistoryActivity
- SIM Selection Dialog

### **Database Kullanımı**: **6 Ekran**

- OnboardingActivity (Insert)
- TargetNumbersActivity (CRUD)
- FilterRulesActivity (CRUD)
- HistoryActivity (Read)
- AnalyticsActivity (Read)
- MainActivity (indirect - permission logging)

### **Özel Özellikler**

- ✅ **ViewPager2**: Onboarding
- ✅ **PreferenceScreen**: Settings
- ✅ **SwipeRefreshLayout**: History
- ✅ **SearchView**: History
- ✅ **CardView**: Analytics
- ✅ **FAB**: Target Numbers, Filter Rules
- ✅ **Material Design 3**: Tüm ekranlar
- ✅ **Runtime Permissions**: Main
- ✅ **Dual SIM Support**: Target, Settings, Analytics
- ✅ **Privacy-First Analytics**: Analytics
- ✅ **Backup/Restore**: Settings
- ✅ **Multi-Language**: Tüm ekranlar (6 dil)

---

## 📁 DOSYA YAPISI

```bash
app/src/main/
├── java/com/keremgok/sms/
│   ├── MainActivity.java (184 lines)
│   ├── OnboardingActivity.java (150 lines)
│   ├── TargetNumbersActivity.java (650 lines)
│   ├── FilterRulesActivity.java (750 lines)
│   ├── HistoryActivity.java (200 lines)
│   ├── AnalyticsActivity.java (450 lines)
│   ├── SettingsActivity.java (926 lines)
│   ├── SimDebugActivity.java (94 lines)
│   └── onboarding/
│       ├── WelcomeFragment.java
│       ├── PermissionExplanationFragment.java
│       ├── TargetNumberSetupFragment.java (6391 lines)
│       ├── FilterIntroFragment.java
│       └── CompletionFragment.java
│
└── res/
    ├── layout/
    │   ├── activity_main.xml
    │   ├── activity_onboarding.xml
    │   ├── activity_target_numbers.xml
    │   ├── activity_filter_rules.xml
    │   ├── activity_history.xml
    │   ├── activity_analytics.xml
    │   ├── activity_settings.xml
    │   ├── activity_sim_debug.xml
    │   ├── fragment_welcome.xml
    │   ├── fragment_permission_explanation.xml
    │   ├── fragment_target_number_setup.xml
    │   ├── fragment_filter_intro.xml
    │   ├── fragment_completion.xml
    │   ├── dialog_add_target_number.xml
    │   ├── dialog_add_filter.xml
    │   ├── dialog_filter_test.xml
    │   ├── dialog_sim_selection.xml
    │   ├── dialog_custom_template.xml
    │   ├── item_target_number.xml
    │   ├── item_filter_rule.xml
    │   ├── item_sms_history.xml
    │   └── item_sim_selection.xml
    │
    ├── menu/
    │   └── main_menu.xml
    │
    └── xml/
        └── preferences.xml
```

---

## 🔄 NAVIGATION FLOW

```bash
MainActivity (Root)
    ↓ [Overflow Menu]
    ├── Target Numbers Activity
    │   └── [FAB] → Add Target Dialog
    │       └── [Save] → Database Insert
    │
    ├── Filter Rules Activity
    │   ├── [FAB] → Add Filter Dialog
    │   │   └── [Save] → Database Insert
    │   └── [Test] → Filter Test Dialog
    │
    ├── History Activity
    │   └── [Search Icon] → SearchView
    │       └── Filter RecyclerView
    │
    ├── Analytics Activity
    │   └── [Export] → Share Analytics File
    │
    ├── Settings Activity
    │   ├── [Preference Item] → Edit/Toggle
    │   ├── [Custom Template] → Custom Template Dialog
    │   ├── [SIM Information] → SIM Selection Dialog
    │   └── [Backup/Restore] → File Picker
    │
    └── SIM Debug Activity (Debug Only)
        └── [Test Buttons] → SIM Tests

Onboarding Activity (First Launch Only)
    ↓ [ViewPager2]
    ├── Welcome Fragment
    ├── Permission Explanation Fragment
    ├── Target Number Setup Fragment
    ├── Filter Intro Fragment
    └── Completion Fragment
        └── [Finish] → MainActivity
```

---

## 🎨 UI/UX PATTERNS

### **Material Design 3 Components Used**

- ✅ MaterialButton
- ✅ TextInputLayout/TextInputEditText
- ✅ CardView
- ✅ FloatingActionButton (FAB)
- ✅ RecyclerView
- ✅ ViewPager2
- ✅ SwitchPreferenceCompat
- ✅ ListPreference
- ✅ SeekBarPreference
- ✅ LinearProgressIndicator
- ✅ SwipeRefreshLayout
- ✅ SearchView
- ✅ AlertDialog
- ✅ Toolbar

### **Color Scheme**

- Primary: `@color/hermes_primary`
- Accent: `@color/hermes_accent`
- Secondary: `@color/hermes_secondary`
- Success: `android.R.color.holo_green_dark`
- Error: `android.R.color.holo_red_dark`

### **Typography**

- Titles: 20sp, bold
- Body: 16sp, regular
- Captions: 12sp, regular
- Buttons: 14sp, medium

---

## 🔐 PERMISSIONS PER SCREEN

| Screen | RECEIVE_SMS | SEND_SMS | READ_PHONE_STATE | READ_CALL_LOG |
|--------|-------------|----------|------------------|---------------|
| MainActivity | Check | Check | Check | Check |
| OnboardingActivity | - | - | - | - |
| TargetNumbersActivity | - | - | (Dual SIM) | - |
| FilterRulesActivity | - | - | - | - |
| HistoryActivity | - | - | - | - |
| AnalyticsActivity | - | - | - | - |
| SettingsActivity | - | - | (Dual SIM) | - |
| SimDebugActivity | - | - | Required | - |

---

## 📝 NOTES

- **Bu rapor** uygulamanın 2.43.0 (versionCode 64) sürümüne göre hazırlanmıştır.
- **Tüm ekranlar** Material Design 3 guidelines'ına uygun olarak tasarlanmıştır.
- **Android 5.0+ (API 21+)** cihazlarla uyumludur.
- **Dual SIM desteği** API 22+ (Android 5.1+) gerektirir.
- **Debug ekranları** sadece debug build'lerde görünür.
- **Privacy-first**: Hiçbir kullanıcı verisi dışarı gönderilmez.
- **6 dil desteği**: Türkçe, İngilizce, Almanca, İtalyanca, Fransızca, İspanyolca.

---

**Son Güncelleme**: 2025-10-01
**Hazırlayan**: Claude Code Analysis System
**Repository**: sms-forward-android
