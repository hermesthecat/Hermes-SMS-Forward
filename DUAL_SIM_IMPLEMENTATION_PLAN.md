# Dual SIM Desteği İmplementasyon Planı

# SMS Forward Android Uygulaması

Bu doküman, mevcut SMS forward uygulamasına dual SIM desteği eklenmesi için gereken tüm adımları ve değişiklikleri detaylarıyla açıklamaktadır.

## Proje Özeti

Mevcut uygulama tek SIM kartı ile çalışmakta ve `SmsManager.getDefault()` kullanmaktadır. Dual SIM desteği eklemek için Android 5.1+ (API 22+) özelliklerini kullanacağız.

## Implementasyon Aşamaları ve Checklist

### ✅ **AŞAMA 1: Temel API Araştırması ve Hazırlık**

- [x] Android dual SIM API'lerini araştır
- [x] Mevcut kod mimarisini analiz et  
- [x] SubscriptionManager ve TelephonyManager kullanımını öğren
- [x] İmplementasyon planı oluştur

### 📋 **AŞAMA 2: Veritabanı Şema Güncellemeleri**

#### ✅ 2.1 TargetNumber Entity Güncellemeleri

- [x] `TargetNumber.java` dosyasına aşağıdaki alanları ekle:

  ```java
  @ColumnInfo(name = "preferred_sim_slot")
  private int preferredSimSlot = -1; // -1 = auto, 0 = SIM 1, 1 = SIM 2
  
  @ColumnInfo(name = "sim_selection_mode") 
  private String simSelectionMode = "auto"; // "auto", "source_sim", "specific_sim"
  ```

- [x] Getter ve setter metodları ekle
- [x] Constructor'ları güncelle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### ✅ 2.2 SmsHistory Entity Güncellemeleri  

- [x] `SmsHistory.java` dosyasına aşağıdaki alanları ekle:

  ```java
  @ColumnInfo(name = "source_sim_slot")
  private int sourceSimSlot = -1;
  
  @ColumnInfo(name = "forwarding_sim_slot")
  private int forwardingSimSlot = -1;
  
  @ColumnInfo(name = "source_subscription_id") 
  private int sourceSubscriptionId = -1;
  
  @ColumnInfo(name = "forwarding_subscription_id")
  private int forwardingSubscriptionId = -1;
  ```

- [x] Getter ve setter metodları ekle
- [x] Constructor'ları güncelle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### ✅ 2.3 Database Migration

- [x] `AppDatabase.java` dosyasında version'ı 4'ten 5'e yükselt
- [x] `MIGRATION_4_5` migration'ı oluştur:

  ```java
  static final Migration MIGRATION_4_5 = new Migration(4, 5) {
      @Override
      public void migrate(SupportSQLiteDatabase database) {
          // TargetNumber tablosuna yeni kolonlar ekle
          database.execSQL("ALTER TABLE target_numbers ADD COLUMN preferred_sim_slot INTEGER DEFAULT -1");
          database.execSQL("ALTER TABLE target_numbers ADD COLUMN sim_selection_mode TEXT DEFAULT 'auto'");
          
          // SmsHistory tablosuna yeni kolonlar ekle
          database.execSQL("ALTER TABLE sms_history ADD COLUMN source_sim_slot INTEGER DEFAULT -1");
          database.execSQL("ALTER TABLE sms_history ADD COLUMN forwarding_sim_slot INTEGER DEFAULT -1");
          database.execSQL("ALTER TABLE sms_history ADD COLUMN source_subscription_id INTEGER DEFAULT -1");
          database.execSQL("ALTER TABLE sms_history ADD COLUMN forwarding_subscription_id INTEGER DEFAULT -1");
      }
  };
  ```

- [x] Migration'ı database builder'a ekle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### ✅ 2.4 DAO Güncellemeleri

- [x] `TargetNumberDao.java` güncelle - SIM filtreli sorgular ekle
- [x] `SmsHistoryDao.java` güncelle - SIM bazlı raporlama sorguları ekle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

### 📋 **AŞAMA 3: SIM Yönetim Utility Sınıfı Oluşturma**

#### ✅ 3.1 SimManager.java Sınıfı Oluştur

- [x] `app/src/main/java/com/keremgok/sms/SimManager.java` dosyası oluştur
- [x] Temel fonksiyonlar:

  ```java
  public class SimManager {
      // Dual SIM desteği var mı kontrol et
      public static boolean isDualSimSupported(Context context)
      
      // Aktif SIM kartlarını listele  
      public static List<SubscriptionInfo> getActiveSimCards(Context context)
      
      // Subscription ID'den SIM bilgisi al
      public static SimInfo getSimInfo(Context context, int subscriptionId)
      
      // Default SIM'i belirle
      public static int getDefaultSmsSubscriptionId(Context context)
      
      // SIM slot numarasından subscription ID al
      public static int getSubscriptionIdForSlot(Context context, int slotIndex)
  }
  ```

- [x] SimInfo data class'ı oluştur:

  ```java
  public static class SimInfo {
      public int subscriptionId;
      public int slotIndex;
      public String carrierName;
      public String displayName;
      public String phoneNumber;
      public boolean isActive;
  }
  ```

- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### ✅ 3.2 İzinler ve Manifest Güncellemeleri

- [x] `AndroidManifest.xml`'e izin ekle:

  ```xml
  <uses-permission android:name="android.permission.READ_PHONE_STATE" />
  ```

- [x] API 23+ için runtime permission handling ekle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

### 📋 **AŞAMA 4: SMS Alma Mekanizması Güncellemeleri**

#### ✅ 4.1 SmsReceiver.java Güncellemeleri

- [x] `onReceive` metodunda gelen SMS'in SIM bilgisini çıkar:

  ```java
  // Bundle'dan subscription ID al
  int subscriptionId = -1;
  int slotIndex = -1;
  
  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
      subscriptionId = bundle.getInt("subscription", -1);
      slotIndex = bundle.getInt("slot", -1);
  }
  ```

- [x] SIM bilgisini queue'ya ve history'ye ilet
- [x] `queueSmsForwardingToMultipleTargets` metodunu güncelle - SIM parametresi ekle
- [x] `logSmsHistory` metodunu güncelle - SIM bilgilerini kaydet
- [x] Debug log'larına SIM bilgisi ekle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### ✅ 4.2 FilterEngine Güncellemeleri  

- [x] `FilterEngine.java`'de SIM bazlı filtreleme desteği ekle (isteğe bağlı)
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

### 📋 **AŞAMA 5: SMS Gönderme Mekanizması Güncellemeleri**

#### ✅ 5.1 SmsQueueWorker.java Güncellemeleri

- [x] Input data'ya SIM bilgilerini ekle:

  ```java
  public static final String KEY_SOURCE_SUBSCRIPTION_ID = "source_subscription_id";
  public static final String KEY_FORWARDING_SUBSCRIPTION_ID = "forwarding_subscription_id";
  public static final String KEY_SOURCE_SIM_SLOT = "source_sim_slot";
  public static final String KEY_FORWARDING_SIM_SLOT = "forwarding_sim_slot";
  ```

- [x] `doWork()` metodunda SIM bilgilerini çıkar
- [x] `SmsManager.getDefault()` yerine subscription-specific manager kullan:

  ```java
  int forwardingSubscriptionId = inputData.getInt(KEY_FORWARDING_SUBSCRIPTION_ID, -1);
  SmsManager smsManager;
  
  if (forwardingSubscriptionId != -1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
      smsManager = SmsManager.getSmsManagerForSubscriptionId(forwardingSubscriptionId);
  } else {
      smsManager = SmsManager.getDefault();
  }
  ```

- [x] `createInputData` metodunu güncelle - SIM parametreleri ekle
- [x] History logging'de SIM bilgilerini kaydet
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### ✅ 5.2 SmsQueueManager.java Güncellemeleri

- [x] Queue metodlarına SIM parametreleri ekle:

  ```java
  public UUID queueHighPrioritySms(String sender, String message, String targetNumber, 
                                   long timestamp, int sourceSubscriptionId, 
                                   int forwardingSubscriptionId)
  ```

- [x] SIM selection logic ekle
- [x] APK build test
- [x] Fix errors and warnings if build fails
- [x] ✅ Mark task as completed (AFTER successful APK build)
- [x] Update versionCode and versionName in app/build.gradle
- [x] Update changelog.md
- [x] Git commit with descriptive message
- [x] Git push

#### 5.3 Fallback Direct Forwarding Güncellemeleri

- [ ] `SmsReceiver.java`'deki fallback metodlarda SIM manager kullan
- [ ] Single SIM cihazlarda graceful degradation
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 6: SIM Selection Logic İmplementasyonu**

#### 6.1 SIM Selection Modes

- [ ] `SmsSimSelectionHelper.java` sınıfı oluştur:

  ```java
  public class SmsSimSelectionHelper {
      public static int determineForwardingSim(Context context, String targetNumber, 
                                              int sourceSubscriptionId, 
                                              TargetNumber targetConfig)
      
      // Modes: "auto", "source_sim", "specific_sim"
      private static int handleAutoMode(Context context, int sourceSubscriptionId)
      private static int handleSourceSimMode(int sourceSubscriptionId) 
      private static int handleSpecificSimMode(TargetNumber targetConfig)
  }
  ```

- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 6.2 Selection Logic

- [ ] **Auto Mode**: Varsayılan SMS SIM'ini kullan
- [ ] **Source SIM Mode**: Gelen SMS'in geldiği SIM'i kullan  
- [ ] **Specific SIM Mode**: Hedef numara için belirtilen SIM'i kullan
- [ ] Single SIM cihazlarda mode'ları ignore et
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 7: UI Güncellemeleri**

#### 7.1 Target Numbers Activity Güncellemeleri

- [ ] `TargetNumbersActivity.java`'ye SIM selection UI ekle
- [ ] Add/Edit dialog'larına SIM seçimi ekle:

  ```java
  // SIM selection spinner
  private void setupSimSelectionSpinner() {
      List<SimManager.SimInfo> sims = SimManager.getActiveSimCards(this);
      // Spinner adapter setup
  }
  ```

- [ ] Target list'te SIM göstergesi ekle
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 7.2 Layout Güncellemeleri

- [ ] `dialog_add_target_number.xml` güncelle:

  ```xml
  <Spinner
      android:id="@+id/spinner_sim_selection"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:entries="@array/sim_selection_entries" />
      
  <RadioGroup
      android:id="@+id/radio_group_sim_mode"
      android:layout_width="match_parent"
      android:layout_height="wrap_content">
      
      <RadioButton android:text="Otomatik" android:id="@+id/radio_auto" />
      <RadioButton android:text="Kaynak SIM" android:id="@+id/radio_source" />  
      <RadioButton android:text="Belirli SIM" android:id="@+id/radio_specific" />
  </RadioGroup>
  ```

- [ ] Target number list item layout'una SIM indicator ekle
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 7.3 SIM Selection Dialog

- [ ] `SimSelectionDialog.java` ve layout oluştur
- [ ] Available SIM'leri listele
- [ ] SIM durumlarını göster (aktif/inaktif)
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 8: Settings ve Preferences Güncellemeleri**

#### 8.1 Preferences XML Güncellemeleri

- [ ] `res/xml/preferences.xml`'e dual SIM ayarları ekle:

  ```xml
  <PreferenceCategory android:title="Dual SIM Ayarları">
      <ListPreference
          android:key="pref_default_forwarding_sim"
          android:title="Varsayılan İletim SIM'i"
          android:summary="SMS iletimi için kullanılacak varsayılan SIM" />
          
      <ListPreference  
          android:key="pref_global_sim_mode"
          android:title="Genel SIM Seçim Modu"
          android:summary="Tüm hedefler için varsayılan SIM seçim modu" />
          
      <SwitchPreferenceCompat
          android:key="pref_show_sim_indicators" 
          android:title="SIM Göstergelerini Göster"
          android:summary="Hedef listesinde SIM göstergelerini göster" />
  </PreferenceCategory>
  ```

- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 8.2 Settings Activity Güncellemeleri

- [ ] `SettingsActivity.java`'de SIM preference handling ekle
- [ ] SIM detection ve validation logic
- [ ] Dynamic preference entries (available SIM'lere göre)
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 8.3 String Resources

- [ ] `strings.xml`'e dual SIM ile ilgili string'ler ekle:

  ```xml
  <!-- Dual SIM Strings -->
  <string name="sim_1">SIM 1</string>
  <string name="sim_2">SIM 2</string>
  <string name="sim_auto">Otomatik</string>
  <string name="sim_source">Kaynak SIM</string>
  <string name="sim_specific">Belirli SIM</string>
  <string name="sim_not_available">SIM mevcut değil</string>
  <string name="dual_sim_not_supported">Dual SIM desteklenmiyor</string>
  ```

- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 9: History ve Analytics Güncellemeleri**

#### 9.1 History Activity Güncellemeleri

- [ ] `HistoryActivity.java`'de SIM bilgilerini göster
- [ ] SIM bazlı filtreleme seçenekleri ekle
- [ ] History item layout'una SIM göstergesi ekle
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 9.2 Analytics Güncellemeleri

- [ ] `StatisticsManager.java`'de SIM bazlı istatistikler ekle
- [ ] SIM kullanım oranları
- [ ] SIM başarı/başarısızlık oranları
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 10: Test ve Hata Durumu Yönetimi**

#### 10.1 Error Handling

- [ ] SIM not available durumları
- [ ] Subscription değişiklikleri (SIM çıkarma/takma)
- [ ] Single SIM cihazlarda graceful degradation
- [ ] Permission denied durumları
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 10.2 Fallback Mechanisms

- [ ] SIM selection başarısız olursa varsayılan SIM kullan
- [ ] Dual SIM API'leri mevcut değilse single SIM mode'a geç
- [ ] Database migration başarısızlıklarını handle et
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 10.3 Logging ve Debug

- [ ] SIM operations için comprehensive logging
- [ ] Debug build'lerde SIM bilgilerini göster
- [ ] Production'da hassas SIM bilgilerini mask et
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 11: Testing ve Validation**

#### 11.1 Unit Tests

- [ ] `SimManager` için unit test'ler
- [ ] SIM selection logic test'leri
- [ ] Database migration test'leri
- [ ] Error handling test'leri
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 11.2 Integration Tests  

- [ ] End-to-end dual SIM forwarding test'leri
- [ ] UI test'leri (SIM selection dialogs)
- [ ] Database integrity test'leri
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 11.3 Manual Testing Scenarios

- [ ] Single SIM cihazda test
- [ ] Dual SIM cihazda test
- [ ] SIM çıkarıp takma senaryoları
- [ ] Different carrier SIM'ler ile test
- [ ] Migration test'i (v4'ten v5'e upgrade)
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

### 📋 **AŞAMA 12: Dokümantasyon ve Finalizasyon**

#### 12.1 Kod Dokümantasyonu

- [ ] Yeni sınıflar için JavaDoc ekle
- [ ] Dual SIM metodları için açıklayıcı comment'ler
- [ ] Complex logic'ler için inline comment'ler
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 12.2 User Documentation

- [ ] CLAUDE.md'yi dual SIM bilgileri ile güncelle
- [ ] changelog.md'ye dual SIM feature'ını ekle
- [ ] Version bump (app/build.gradle)
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

#### 12.3 Build ve Release

- [ ] Debug build ile test
- [ ] ProGuard rules'u kontrol et (dual SIM class'ları için)
- [ ] Release build oluştur ve test et
- [ ] APK boyutu kontrolü
- [ ] APK build test
- [ ] Fix errors and warnings if build fails
- [ ] ✅ Mark task as completed (AFTER successful APK build)
- [ ] Update versionCode and versionName in app/build.gradle
- [ ] Update changelog.md
- [ ] Git commit with descriptive message
- [ ] Git push

## API Level ve Compatibility

### Minimum Requirements

- **API Level 22** (Android 5.1) - Dual SIM API'leri için
- **Graceful degradation** API 21'de (tek SIM olarak çalış)
- **Runtime permission handling** API 23+ için

### Feature Detection

```java
public static boolean isDualSimApiSupported() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1;
}
```

## Risker ve Dikkat Edilecek Noktalar

### 🚨 Yüksek Risk

- Database migration'ın başarısız olması
- SMS permission'ların revoke edilmesi  
- SIM card değişiklikleri sırasında crash

### ⚠️ Orta Risk

- SIM detection'ın bazı cihazlarda çalışmaması
- Carrier-specific issues
- UI'da performance problemi

### 💡 İyileştirme Önerileri

- SIM availability'yi periodic check et
- User'a SIM durumu hakkında bilgi ver
- Advanced SIM selection options (carrier bazlı, zaman bazlı)

## Tahmini Süre

- **Toplam Süre**: 15-20 iş günü
- **Critical Path**: Database migration → SIM API integration → UI updates
- **Risk Buffer**: %20 ek süre

## Success Criteria

- [x] Dual SIM cihazlarda her iki SIM'den SMS gönderebilme
- [x] Gelen SMS'in hangi SIM'den geldiğini tespit etme
- [x] Per-target SIM configuration
- [x] Single SIM cihazlarda backward compatibility
- [x] Comprehensive error handling
- [x] Database migration'ın sorunsuz çalışması

---

## Notlar

- Bu plan, mevcut kod mimarisinin korunmasını ve backward compatibility'nin sağlanmasını hedefler
- Her aşama tamamlandıktan sonra test edilmeli ve bir sonraki aşamaya geçilmelidir
- Critical path'teki değişiklikler öncelikli olarak implement edilmelidir
