# 🐛 BUG CHECKLIST - SMS Forward Android

**Oluşturulma Tarihi**: 2025-10-01
**Son Güncelleme**: 2025-10-01 22:30
**Versiyon**: 2.43.0 (versionCode 64)
**Toplam Bug**: 45
**Durum**: 5/45 Tamamlandı (11.1%)

---

## 📊 İLERLEME ÖZETI

| Kategori | Toplam | Tamamlanan | Kalan | İlerleme |
|----------|---------|------------|-------|----------|
| **Kritik** | 5 | 5 | 0 | 100% ✅ |
| **Yüksek** | 7 | 0 | 7 | 0% |
| **Orta** | 14 | 0 | 14 | 0% |
| **Düşük** | 19 | 0 | 19 | 0% |
| **TOPLAM** | **45** | **5** | **40** | **11.1%** |

---

## 🔴 KRİTİK SEVİYE BUGLAR (ACİL)

### [✅] B001: Deprecated getResources().getColor() Kullanımı
- **Durum**: ✅ **TAMAMLANDI** (2025-10-01)
- **Build**: ✅ Başarılı
- **Önem**: ⛔ KRİTİK
- **Dosyalar**:
  - `MainActivity.java` (satırlar: 89, 92, 97, 100, 105, 108)
  - `TargetNumbersActivity.java` (satırlar: 347, 357, 591)
  - `FilterRulesActivity.java` (satırlar: 444, 453)
- **Sorun**: API 23+ için deprecated olan `getResources().getColor()` kullanılıyor
- **Etki**: Android 6.0+ cihazlarda crash riski
- **Çözüm**:
  ```java
  // ÖNCE:
  getResources().getColor(android.R.color.holo_green_dark)

  // SONRA:
  ContextCompat.getColor(this, android.R.color.holo_green_dark)
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: API 23+ cihazda test et
- **Notlar**: Tüm dosyalardaki tüm kullanımları değiştir

---

### [✅] B002: Database Instance NULL Check Eksikliği
- **Durum**: ✅ **TAMAMLANDI** (2025-10-01)
- **Build**: ✅ Başarılı
- **Önem**: ⛔ KRİTİK
- **Dosyalar**:
  - `MainActivity.java`
  - `TargetNumbersActivity.java`
  - `FilterRulesActivity.java`
  - Tüm database kullanan activities
- **Sorun**: `AppDatabase.getInstance(context)` null dönebilir ama kontrol edilmiyor
- **Etki**: Database init başarısız olursa NullPointerException crash
- **Çözüm**:
  ```java
  AppDatabase database = AppDatabase.getInstance(this);
  if (database == null) {
      Log.e(TAG, "Database initialization failed");
      Toast.makeText(this, R.string.database_error, Toast.LENGTH_LONG).show();
      finish();
      return;
  }
  TargetNumberDao dao = database.targetNumberDao();
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Database init fail senaryosu test et (disk full, permission denied)
- **Notlar**: Tüm getInstance() çağrılarını kontrol et

---

### [✅] B004: PendingIntent FLAG_IMMUTABLE API Uyumsuzluğu
- **Durum**: ✅ **TAMAMLANDI** (2025-10-01)
- **Build**: ✅ Başarılı
- **Önem**: ⛔ KRİTİK
- **Dosyalar**:
  - `SmsQueueWorker.java` (satırlar: 492, 506)
- **Sorun**: FLAG_IMMUTABLE API 23+ ama minSdk 21
- **Etki**: Android 5.0-5.1 (API 21-22) cihazlarda crash
- **Çözüm**:
  ```java
  int flags = PendingIntent.FLAG_UPDATE_CURRENT;
  if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
  }
  PendingIntent pendingIntent = PendingIntent.getBroadcast(
      context, requestCode, intent, flags
  );
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: API 21-22 emulator veya cihazda test et
- **Notlar**: Tüm PendingIntent oluşturma yerlerini kontrol et

---

### [✅] B005: SecurityException Sessiz Hata Yönetimi
- **Durum**: ✅ **TAMAMLANDI** (2025-10-01)
- **Build**: ✅ Başarılı
- **Önem**: ⛔ KRİTİK
- **Dosyalar**:
  - `SimManager.java` (satırlar: 106-110, 163-166)
- **Sorun**: SecurityException yakalanıyor ama kullanıcıya bildirilmiyor
- **Etki**: Kullanıcı dual SIM özelliklerin neden çalışmadığını anlamıyor
- **Çözüm**:
  ```java
  } catch (SecurityException e) {
      Log.e(TAG, "Permission denied for dual SIM access", e);
      // Kullanıcıya bildir
      notifyUserAboutPermissionIssue();
      return new ArrayList<>();
  }

  private void notifyUserAboutPermissionIssue() {
      // Toast veya notification göster
  }
  ```
- **Tahmini Süre**: 45 dakika
- **Test**: READ_PHONE_STATE izni olmadan test et
- **Notlar**: User-friendly error message string resource'u ekle

---

### [ ] B003: StatisticsManager Singleton Race Condition (FALSE POSITIVE)
- **Önem**: ⚠️ Kontrol Edildi - Sorun YOK
- **Dosyalar**: `StatisticsManager.java`
- **Durum**: INSTANCE zaten volatile olarak tanımlı (line 27)
- **Notlar**: Bu bug false positive, kod zaten doğru

---

## 🟠 YÜKSEK SEVİYE BUGLAR

### [ ] B006: Handler Memory Leak in SmsReceiver
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `SmsReceiver.java` (satır: 377)
- **Sorun**: Handler, Looper.getMainLooper() ile oluşturulmuş ve BroadcastReceiver context'ini leak ediyor
- **Etki**: Zaman içinde memory leak, app yavaşlaması
- **Çözüm**:
  ```java
  // Seçenek 1: WeakReference kullan
  private static class WeakHandler extends Handler {
      private final WeakReference<SmsReceiver> mReceiver;

      WeakHandler(SmsReceiver receiver) {
          super(Looper.getMainLooper());
          mReceiver = new WeakReference<>(receiver);
      }

      @Override
      public void handleMessage(Message msg) {
          SmsReceiver receiver = mReceiver.get();
          if (receiver != null) {
              // Process message
          }
      }
  }

  // Seçenek 2: WorkManager kullan (tercih edilen)
  // Handler'ı tamamen kaldır, WorkManager scheduling kullan
  ```
- **Tahmini Süre**: 2 saat
- **Test**: LeakCanary ile memory leak test et
- **Notlar**: WorkManager'a geçiş daha iyi bir çözüm olabilir

---

### [ ] B007: Database Cursor Leak in Migrations
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `AppDatabase.java` (satırlar: 153-165, 185-207)
- **Sorun**: Database cursor açılıyor ama tüm code path'lerde kapatılmıyor
- **Etki**: Cursor leak, eventual crash
- **Çözüm**:
  ```java
  // Migration 1->2 (satır 153-165)
  try (Cursor cursor = database.query("PRAGMA table_info(target_numbers)")) {
      boolean columnExists = false;
      while (cursor.moveToNext()) {
          String columnName = cursor.getString(cursor.getColumnIndex("name"));
          if ("enabled".equals(columnName)) {
              columnExists = true;
              break;
          }
      }
      if (!columnExists) {
          database.execSQL("ALTER TABLE target_numbers ADD COLUMN enabled INTEGER NOT NULL DEFAULT 1");
      }
  }

  // Diğer migration'lar için de aynı pattern
  ```
- **Tahmini Süre**: 1.5 saat
- **Test**: Migration test senaryolarında cursor leak kontrolü
- **Notlar**: Tüm migration'lardaki cursor kullanımlarını kontrol et

---

### [ ] B008: Thread.sleep() in WorkManager Worker
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `SmsQueueWorker.java` (satırlar: 169, 172)
- **Sorun**: Thread.sleep() WorkManager worker thread'ini blokluyor
- **Etki**: WorkManager verimliliğini düşürür, ANR riski
- **Çözüm**:
  ```java
  // Thread.sleep(2000) yerine:

  // OneTimeWorkRequest oluştururken:
  OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(SmsQueueWorker.class)
      .setInitialDelay(2, TimeUnit.SECONDS)  // Delay'i burada belirt
      .setInputData(inputData)
      .setConstraints(constraints)
      .build();

  // Worker içinde sleep kullanma
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Yüksek SMS yükü ile test et
- **Notlar**: Tüm Thread.sleep() kullanımlarını kaldır

---

### [ ] B009: SimManager Infinite Recursion Risk
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `SimManager.java` (satır: 96)
- **Sorun**: `getActiveSimCards()` içinde `isDualSimSupported()` çağrılıyor, circular dependency riski
- **Etki**: Stack overflow crash (edge cases)
- **Çözüm**:
  ```java
  // Cache mekanizmasını düzenle
  private static Boolean cachedDualSimSupport = null;

  public static boolean isDualSimSupported(Context context) {
      if (cachedDualSimSupport != null) {
          return cachedDualSimSupport;
      }

      // Direct check without calling getActiveSimCards()
      try {
          SubscriptionManager subscriptionManager =
              (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
          if (subscriptionManager == null) {
              cachedDualSimSupport = false;
              return false;
          }

          int activeSubscriptionInfoCountMax = subscriptionManager.getActiveSubscriptionInfoCountMax();
          cachedDualSimSupport = activeSubscriptionInfoCountMax > 1;
          return cachedDualSimSupport;
      } catch (Exception e) {
          cachedDualSimSupport = false;
          return false;
      }
  }
  ```
- **Tahmini Süre**: 2 saat
- **Test**: Dual SIM ve single SIM cihazlarda test et
- **Notlar**: Cache invalidation stratejisi ekle

---

### [ ] B010: Unchecked ParseException in Statistics
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `StatisticsManager.java` (satır: 331)
- **Sorun**: `dateFormat.parse(date)` ParseException fırlatabilir ama kontrol edilmiyor
- **Etki**: Invalid date ile crash
- **Çözüm**:
  ```java
  private void generateDailySummary(String date) {
      try {
          Date summaryDate = dateFormat.parse(date);
          if (summaryDate == null) {
              Log.e(TAG, "Failed to parse date: " + date);
              return;
          }
          // Rest of the code
      } catch (ParseException e) {
          Log.e(TAG, "ParseException for date: " + date, e);
          return;
      }
  }
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Invalid date formatları ile test et
- **Notlar**: Tüm date parsing yerlerini kontrol et

---

### [ ] B011: Missing Bundle Validation in SmsReceiver
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `SmsReceiver.java` (satır: 114)
- **Sorun**: Intent'ten Bundle alınıyor ama null kontrolü yapılmıyor
- **Etki**: Malformed SMS intent ile NullPointerException
- **Çözüm**:
  ```java
  @Override
  public void onReceive(Context context, Intent intent) {
      if (intent == null) {
          Log.e(TAG, "Received null intent");
          return;
      }

      String action = intent.getAction();
      if (action == null) {
          Log.e(TAG, "Intent action is null");
          return;
      }

      Bundle bundle = intent.getExtras();
      if (bundle == null) {
          Log.e(TAG, "Intent extras bundle is null");
          return;
      }

      // Continue processing
  }
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Malformed SMS intent ile test et
- **Notlar**: Tüm Intent handling yerlerinde validation ekle

---

### [ ] B012: SIM Selection Validity Not Checked
- **Önem**: 🔴 YÜKSEK
- **Dosyalar**:
  - `SmsSimSelectionHelper.java` (satırlar: 138-139)
  - SIM selection sonucunu kullanan tüm yerler
- **Sorun**: `SimSelectionResult.isValid()` kontrol edilmeden subscription ID kullanılıyor
- **Etki**: Invalid SIM ID ile SMS gönderme hatası
- **Çözüm**:
  ```java
  // SmsQueueWorker.java içinde
  SimSelectionResult selectionResult = SmsSimSelectionHelper.selectSimForSending(
      context, targetNumber, incomingSimSlot
  );

  if (!selectionResult.isValid()) {
      Log.e(TAG, "Invalid SIM selection: " + selectionResult.getErrorMessage());
      // Fallback: Default SIM kullan veya hata döndür
      return Result.failure(createErrorData("Invalid SIM selection"));
  }

  int subscriptionId = selectionResult.getSubscriptionId();
  // Continue with SMS sending
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Invalid SIM configuration ile test et
- **Notlar**: Tüm SIM selection kullanımlarını kontrol et

---

## 🟡 ORTA SEVİYE BUGLAR

### [ ] B013: Integer Overflow in RequestCode Generation
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `SmsQueueWorker.java` (satırlar: 489, 503)
- **Sorun**: RequestCode hesaplama integer overflow yapabilir
- **Etki**: PendingIntent collision, yanlış intent routing
- **Çözüm**:
  ```java
  private int generateUniqueRequestCode(TargetNumber targetNumber, boolean isSentIntent) {
      long timestamp = System.currentTimeMillis();
      int typeOffset = isSentIntent ? 0 : 10000;

      // Proper hash combining
      int hash = 17;
      hash = 31 * hash + (int)(timestamp ^ (timestamp >>> 32));
      hash = 31 * hash + targetNumber.getPhoneNumber().hashCode();
      hash = 31 * hash + targetNumber.getId();
      hash = 31 * hash + typeOffset;

      // Ensure positive
      return Math.abs(hash);
  }
  ```
- **Tahmini Süre**: 45 dakika
- **Test**: Çok sayıda SMS ile collision test et
- **Notlar**: UUID kullanımı da düşünülebilir

---

### [ ] B014: FileProvider Authority Hardcoding
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `AndroidManifest.xml` (satır: 134)
- **Sorun**: FileProvider authority `${applicationId}.fileprovider` kullanıyor, fork'larda conflict riski
- **Etki**: App fork edildiğinde FileProvider conflict
- **Çözüm**:
  ```xml
  <!-- AndroidManifest.xml -->
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="com.keremgok.sms.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/file_paths" />
  </provider>
  ```
- **Tahmini Süre**: 15 dakika
- **Test**: FileProvider işlevselliğini test et
- **Notlar**: Düşük risk, ama best practice

---

### [ ] B015: Html.fromHtml() API Inconsistency
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `FilterRulesActivity.java` (satırlar: 697-701)
- **Sorun**: API < 24 için farklı davranış olabilir
- **Etki**: HTML rendering farklılıkları
- **Çözüm**: Zaten doğru implement edilmiş, sadece test edilmeli
- **Tahmini Süre**: 30 dakika (test)
- **Test**: API 21-23 ve 24+ cihazlarda HTML rendering test et
- **Notlar**: Mevcut kod doğru, sadece verification gerekli

---

### [ ] B016: Dialog EditText Memory Leak
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `TargetNumbersActivity.java` (member variables: etNewPhoneNumber, etc.)
  - `FilterRulesActivity.java` (member variables: etFilterName, etc.)
- **Sorun**: Dialog view referansları member variable olarak tutuluyor, dialog dismiss'ten sonra temizlenmiyor
- **Etki**: Dialog view hierarchy leak
- **Çözüm**:
  ```java
  private void showAddTargetDialog() {
      View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_target_number, null);

      // Local variables instead of member variables
      EditText etNewPhoneNumber = dialogView.findViewById(R.id.etNewPhoneNumber);
      EditText etNewDisplayName = dialogView.findViewById(R.id.etNewDisplayName);
      // ... other views

      AlertDialog dialog = new AlertDialog.Builder(this)
          .setView(dialogView)
          .setPositiveButton(R.string.add_target_number, (d, which) -> {
              // Use local variables
              String phoneNumber = etNewPhoneNumber.getText().toString();
              // ...
          })
          .setOnDismissListener(d -> {
              // Cleanup if needed
          })
          .create();

      dialog.show();
  }
  ```
- **Tahmini Süre**: 1.5 saat
- **Test**: LeakCanary ile dialog açıp kapama memory leak test
- **Notlar**: Tüm dialog'ları kontrol et

---

### [ ] B017: Phone Number Length Validation Missing
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `PhoneNumberValidator.java` (maskPhoneNumber metodu)
- **Sorun**: `maskPhoneNumber()` minimum 8 karakter varsayıyor ama validate etmiyor
- **Etki**: Kısa telefon numaralarında StringIndexOutOfBoundsException
- **Çözüm**:
  ```java
  public static String maskPhoneNumber(String phoneNumber) {
      if (phoneNumber == null || phoneNumber.length() < 8) {
          return phoneNumber; // Return as-is if too short to mask
      }

      int length = phoneNumber.length();
      String start = phoneNumber.substring(0, 4);
      String end = phoneNumber.substring(length - 4);
      String masked = start + "****" + end;
      return masked;
  }
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Kısa ve uzun telefon numaraları ile test et
- **Notlar**: PhoneNumberValidator.java dosyasını oku ve kontrol et

---

### [ ] B018: WorkManager REPLACE Policy SMS Loss Risk
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `SmsQueueManager.java` (satırlar: 377-378)
- **Sorun**: Yüksek öncelik REPLACE policy kullanıyor, var olan yüksek öncelikli SMS'i iptal edebilir
- **Etki**: SMS kaybı riski
- **Çözüm**:
  ```java
  // Yüksek öncelik için APPEND_OR_REPLACE kullan
  private void enqueueHighPrioritySms(String messageId, SmsQueueItem queueItem) {
      // ... existing code ...

      workManager.enqueueUniqueWork(
          WORK_NAME_HIGH_PRIORITY,
          ExistingWorkPolicy.APPEND_OR_REPLACE,  // REPLACE yerine
          workRequest
      );
  }
  ```
- **Tahmini Süre**: 45 dakika
- **Test**: Ardışık yüksek öncelikli SMS'lerle test et
- **Notlar**: Tüm priority level'ları için policy'leri gözden geçir

---

### [ ] B019: SimpleDateFormat Locale Issue (ALREADY FIXED)
- **Önem**: ✅ FIXED
- **Dosyalar**: `StatisticsManager.java`
- **Durum**: Zaten `Locale.US` kullanılıyor (satır 330)
- **Notlar**: Bu bug zaten düzeltilmiş

---

### [ ] B020: Hardcoded Error Messages
- **Önem**: 🟡 ORTA
- **Dosyalar**: Multiple files
- **Sorun**: Error message'lar hardcoded English string, localization yok
- **Etki**: Çok dilli destek eksik, sadece İngilizce error
- **Çözüm**:
  ```java
  // Önce strings.xml'e ekle:
  // <string name="error_database_init">Database initialization failed</string>
  // <string name="error_permission_denied">Permission denied</string>
  // <string name="error_sim_unavailable">SIM card not available</string>

  // Kod içinde:
  Log.e(TAG, getString(R.string.error_database_init));
  Toast.makeText(this, R.string.error_permission_denied, Toast.LENGTH_LONG).show();
  ```
- **Tahmini Süre**: 2 saat
- **Test**: Farklı dillerde hata mesajlarını kontrol et
- **Notlar**: Tüm hardcoded string'leri bul ve resource'a taşı

---

### [ ] B021: Database Transaction Missing
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `TargetNumbersActivity.java` (satırlar: 442-455)
- **Sorun**: Multiple DB operations transaction olmadan yapılıyor
- **Etki**: Yarım kalan operation'lar, data inconsistency
- **Çözüm**:
  ```java
  private boolean addTargetNumber() {
      // ... validation code ...

      ThreadManager.getInstance().executeDatabase(() -> {
          AppDatabase database = AppDatabase.getInstance(this);
          database.runInTransaction(() -> {
              // If setting as primary, unset other primary targets
              if (isPrimary) {
                  targetNumberDao.setPrimaryTargetNumber(-1);
              }

              long id = targetNumberDao.insert(targetNumber);

              // Additional operations
              List<TargetNumber> allTargets = targetNumberDao.getAllTargetNumbers();
              if (allTargets.size() == 1 || isPrimary) {
                  targetNumberDao.setPrimaryTargetNumber((int) id);
              }
          });

          runOnUiThread(() -> {
              Toast.makeText(this, R.string.target_add_success, Toast.LENGTH_SHORT).show();
              loadTargetNumbers();
          });
      });

      return true;
  }
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Concurrent database operations test et
- **Notlar**: Tüm multi-step DB operations için transaction ekle

---

### [ ] B022: onDestroy() Cleanup Not Guaranteed
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `MainActivity.java` (satırlar: 180-183)
- **Sorun**: `StatisticsManager.endSession()` sadece onDestroy()'da, process death'te çağrılmayabilir
- **Etki**: Session verileri düzgün kapanmayabilir
- **Çözüm**:
  ```java
  @Override
  protected void onStop() {
      super.onStop();
      // onStop daha güvenilir, app background'a gittiğinde çağrılır
      StatisticsManager.getInstance(this).pauseSession();
  }

  @Override
  protected void onStart() {
      super.onStart();
      StatisticsManager.getInstance(this).resumeSession();
  }

  @Override
  protected void onDestroy() {
      super.onDestroy();
      if (isFinishing()) {
          StatisticsManager.getInstance(this).endSession();
      }
  }
  ```
- **Tahmini Süre**: 1 saat
- **Test**: App lifecycle senaryoları (background, kill, restart)
- **Notlar**: Lifecycle-aware components kullanımı düşünülebilir

---

### [ ] B023: SIM Information Lost in Callbacks
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `SmsCallbackReceiver.java` (satırlar: 275-278)
- **Sorun**: SmsHistory kaydında SIM bilgisi hardcoded -1, dual SIM tracking kaybolmuş
- **Etki**: İstatistiklerde hangi SIM'den gönderildiği belli değil
- **Çözüm**:
  ```java
  // SmsQueueWorker'da SMS gönderirken:
  Intent sentIntent = new Intent(ACTION_SMS_SENT);
  sentIntent.putExtra("target_number", targetPhoneNumber);
  sentIntent.putExtra("message_id", queueItem.getMessageId());
  sentIntent.putExtra("subscription_id", subscriptionId);  // SIM bilgisi ekle
  sentIntent.putExtra("sim_slot", simSlot);

  // SmsCallbackReceiver'da:
  @Override
  public void onReceive(Context context, Intent intent) {
      // ... existing code ...

      int subscriptionId = intent.getIntExtra("subscription_id", -1);
      int simSlot = intent.getIntExtra("sim_slot", -1);

      SmsHistory history = new SmsHistory(
          originalMessageId,
          originalSender,
          originalMessage,
          targetNumber,
          currentTime,
          statusText,
          subscriptionId,  // Gerçek değer
          simSlot          // Gerçek değer
      );
  }
  ```
- **Tahmini Süre**: 1.5 saat
- **Test**: Dual SIM cihazda her iki SIM'den gönderim test et
- **Notlar**: İstatistik raporlarını kontrol et

---

### [ ] B024: ViewPager2 Lifecycle Issues
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `OnboardingActivity.java` (dosya okunamadı, var olduğu biliniyor)
- **Sorun**: ViewPager2 fragment lifecycle sorunları olabilir
- **Etki**: Fragment state loss, memory leak
- **Çözüm**: Dosyayı oku ve analiz et
- **Tahmini Süre**: 2 saat
- **Test**: Onboarding flow'u test et, screen rotation test et
- **Notlar**: OnboardingActivity.java'yı detaylı incele

---

### [ ] B025: Filter Priority Management Missing
- **Önem**: 🟡 ORTA
- **Dosyalar**:
  - `FilterRulesActivity.java` (satır: 488)
- **Sorun**: Filter priority her zaman 0, kullanıcı düzenleyemiyor
- **Etki**: Filter sıralaması belirsiz, expected behavior olmayabilir
- **Çözüm**:
  ```java
  // Dialog'a priority spinner ekle
  // dialog_add_filter.xml:
  <Spinner
      android:id="@+id/spinner_filter_priority"
      android:layout_width="match_parent"
      android:layout_height="wrap_content"
      android:entries="@array/filter_priorities" />

  // strings.xml:
  <string-array name="filter_priorities">
      <item>Highest Priority (1)</item>
      <item>High Priority (2)</item>
      <item>Normal Priority (3)</item>
      <item>Low Priority (4)</item>
      <item>Lowest Priority (5)</item>
  </string-array>

  // FilterRulesActivity.java:
  Spinner spinnerPriority = dialogView.findViewById(R.id.spinner_filter_priority);
  int priority = spinnerPriority.getSelectedItemPosition() + 1; // 1-5

  SmsFilter newFilter = new SmsFilter(
      filterName,
      filterPattern,
      filterType,
      isEnabled,
      priority  // Use actual priority
  );
  ```
- **Tahmini Süre**: 2 saat
- **Test**: Farklı önceliklerle filter'lar oluştur ve sıralamayı test et
- **Notlar**: FilterEngine'de priority'ye göre sıralama kontrolü

---

### [ ] B026: SharedPreferences Race Condition
- **Önem**: 🟡 ORTA
- **Dosyalar**: Multiple files
- **Sorun**: SharedPreferences multiple thread'den access ediliyor, synchronization yok
- **Etki**: Preference okuma/yazma race condition
- **Çözüm**:
  ```java
  // Option 1: Always use apply() instead of commit()
  prefs.edit()
      .putString(KEY, value)
      .apply();  // Asynchronous, thread-safe

  // Option 2: Create a preference manager wrapper
  public class PreferenceManager {
      private static final Object LOCK = new Object();
      private final SharedPreferences prefs;

      public void putString(String key, String value) {
          synchronized (LOCK) {
              prefs.edit().putString(key, value).apply();
          }
      }

      public String getString(String key, String defaultValue) {
          synchronized (LOCK) {
              return prefs.getString(key, defaultValue);
          }
      }
  }
  ```
- **Tahmini Süre**: 2 saat
- **Test**: Concurrent preference access test
- **Notlar**: Tüm commit() kullanımlarını apply() yap

---

## 🟢 DÜŞÜK SEVİYE BUGLAR (Code Quality)

### [ ] B027: Unused Import Statements
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**: Multiple files
- **Sorun**: Kullanılmayan import'lar
- **Etki**: APK boyutu minimal artış, code cleanliness
- **Çözüm**: IDE'de "Optimize Imports" çalıştır
- **Tahmini Süre**: 30 dakika
- **Test**: Build yap, import errors olmadığını kontrol et
- **Notlar**: Otomatik tool kullanılabilir

---

### [ ] B028: Magic Numbers in Code
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `SmsReceiver.java` (satırlar: 307, 450)
- **Sorun**: 2000 (delay), 160 (SMS length) gibi magic number'lar
- **Etki**: Code maintainability
- **Çözüm**:
  ```java
  // Constants class veya activity içinde:
  private static final int SMS_FORWARD_DELAY_MS = 2000;
  private static final int SMS_MAX_LENGTH = 160;
  private static final int SMS_EXTENDED_LENGTH = 1600;

  // Kullanım:
  handler.postDelayed(runnable, SMS_FORWARD_DELAY_MS);
  if (messageLength > SMS_MAX_LENGTH) {
      // handle multipart
  }
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Functionality aynı kaldığını doğrula
- **Notlar**: Tüm magic number'ları bul ve constant'a çevir

---

### [ ] B029: Inconsistent Logging TAG Names
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**: Multiple
- **Sorun**: Bazı TAG'ler "Hermes" prefix'li, bazıları değil
- **Etki**: Log filtering zorluğu
- **Çözüm**:
  ```java
  // Standardize all TAGs:
  private static final String TAG = "Hermes.MainActivity";
  private static final String TAG = "Hermes.SmsReceiver";
  private static final String TAG = "Hermes.FilterEngine";

  // Or without prefix:
  private static final String TAG = "MainActivity";
  private static final String TAG = "SmsReceiver";
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Logcat'te filter test et
- **Notlar**: Projewide TAG naming convention belirle

---

### [ ] B030: Missing @Override Annotations
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**: Adapter classes
- **Sorun**: Interface implementation'larında @Override eksik
- **Etki**: Code clarity, compile-time checking
- **Çözüm**: Tüm override methodlara @Override ekle
- **Tahmini Süre**: 30 dakika
- **Test**: Build yap
- **Notlar**: IDE inspection tool kullan

---

### [ ] B031: Deprecated API Usage Without Suppression
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `SmsReceiver.java` (satır: 190)
- **Sorun**: Deprecated `SmsMessage.createFromPdu()` için @SuppressWarnings yok
- **Etki**: Compiler warnings
- **Çözüm**:
  ```java
  @SuppressWarnings("deprecation")
  private SmsMessage createSmsMessage(byte[] pdu, String format) {
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
          return SmsMessage.createFromPdu(pdu, format);
      } else {
          return SmsMessage.createFromPdu(pdu);
      }
  }
  ```
- **Tahmini Süre**: 15 dakika
- **Test**: Build warnings kontrol et
- **Notlar**: Sadece gerçekten unavoidable deprecated usage için suppress et

---

### [ ] B032: Empty Catch Block with Only Interrupt Restore
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `ThreadManager.java` (satırlar: 198-201)
- **Sorun**: Empty catch block, sadece interrupt status restore
- **Etki**: Error context kaybolması (ama interrupt properly handled)
- **Çözüm**:
  ```java
  } catch (InterruptedException e) {
      Log.w(TAG, "Thread interrupted during shutdown", e);
      Thread.currentThread().interrupt();
  }
  ```
- **Tahmini Süre**: 15 dakika
- **Test**: Thread interrupt senaryolarını test et
- **Notlar**: Interrupt handling zaten doğru, sadece logging ekle

---

### [ ] B033: String.format() Without Locale
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `StatisticsManager.java` (multiple locations)
- **Sorun**: `String.format()` explicit Locale olmadan
- **Etki**: Locale-dependent formatting
- **Çözüm**:
  ```java
  // Instead of:
  String.format("Value: %d", count)

  // Use:
  String.format(Locale.US, "Value: %d", count)
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Farklı locale'lerde test et
- **Notlar**: Sadece log message'lar için, UI string'ler zaten resource'tan

---

### [ ] B034: Debug Flag Not Using BuildConfig
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**: Multiple
- **Sorun**: DEBUG flag'ler hardcoded false, BuildConfig.DEBUG kullanmalı
- **Etki**: Debug build'lerde debug log'lar aktif olamıyor
- **Çözüm**:
  ```java
  // Instead of:
  private static final boolean DEBUG = false;

  // Use:
  private static final boolean DEBUG = BuildConfig.DEBUG;
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Debug ve release build'lerde log seviyelerini kontrol et
- **Notlar**: Tüm DEBUG flag'leri güncelle

---

### [ ] B035: Missing Null Safety Annotations
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**: All Java files
- **Sorun**: Method parameters @Nullable/@NonNull annotate edilmemiş
- **Etki**: Static analysis eksik, null safety kontrol edilemiyor
- **Çözüm**:
  ```java
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;

  public void processMessage(@NonNull Context context,
                            @Nullable String message,
                            @NonNull MessageCallback callback) {
      // Method body
  }
  ```
- **Tahmini Süre**: 4 saat
- **Test**: Android Lint null analysis çalıştır
- **Notlar**: Büyük refactoring, öncelik düşük

---

### [ ] B036: Error Codes as Strings Instead of Enum
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `StatisticsManager.java` (satırlar: 75-81)
- **Sorun**: Error code'lar String constant, enum olmalı
- **Etki**: Type safety eksik
- **Çözüm**:
  ```java
  public enum ErrorCode {
      NO_PERMISSION("no_permission"),
      SMS_SEND_FAILED("sms_send_failed"),
      INVALID_NUMBER("invalid_number"),
      NETWORK_ERROR("network_error"),
      UNKNOWN_ERROR("unknown_error");

      private final String code;

      ErrorCode(String code) {
          this.code = code;
      }

      public String getCode() {
          return code;
      }
  }
  ```
- **Tahmini Süre**: 1.5 saat
- **Test**: Tüm error code kullanımlarını test et
- **Notlar**: Better type safety

---

### [ ] B037: Missing @RequiresApi Annotations
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `SimManager.java` (satır: 191)
- **Sorun**: API 29+ methodlar @RequiresApi annotate edilmemiş
- **Etki**: Lint warnings
- **Çözüm**:
  ```java
  @RequiresApi(api = Build.VERSION_CODES.Q)
  private static List<SimInfo> getSimInfoAndroidQ(Context context) {
      // Android Q+ specific code
  }
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Lint check çalıştır
- **Notlar**: Code maintainability için iyi practice

---

### [ ] B038: String Concatenation in Loops
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `FilterEngine.java` (logging loops)
- **Sorun**: Loop içinde String concatenation
- **Etki**: Minor performance, mostly logging
- **Çözüm**:
  ```java
  // Instead of:
  for (Filter filter : filters) {
      Log.d(TAG, "Processing filter: " + filter.getName());
  }

  // Use StringBuilder if building complex strings:
  StringBuilder sb = new StringBuilder();
  for (Filter filter : filters) {
      sb.append(filter.getName()).append(", ");
  }
  Log.d(TAG, "Filters: " + sb.toString());
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: Performance test
- **Notlar**: Sadece performance-critical loop'lar için gerekli

---

### [ ] B039: Missing equals() and hashCode() in Data Classes
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `SimManager.SimInfo`
  - `SmsQueueManager.QueueStatus`
- **Sorun**: Data class'lar equals/hashCode implement etmemiş
- **Etki**: Instance comparison hatalı olabilir
- **Çözüm**:
  ```java
  public static class SimInfo {
      // ... fields ...

      @Override
      public boolean equals(Object o) {
          if (this == o) return true;
          if (!(o instanceof SimInfo)) return false;
          SimInfo simInfo = (SimInfo) o;
          return slotIndex == simInfo.slotIndex &&
                 subscriptionId == simInfo.subscriptionId &&
                 Objects.equals(carrierName, simInfo.carrierName) &&
                 Objects.equals(displayName, simInfo.displayName);
      }

      @Override
      public int hashCode() {
          return Objects.hash(slotIndex, subscriptionId, carrierName, displayName);
      }
  }
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Collection operations (Set, Map) ile test et
- **Notlar**: @AutoValue kullanımı düşünülebilir

---

### [ ] B040: Layout XML Analysis Not Performed
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**: All XML layouts
- **Sorun**: Layout dosyaları analiz edilmedi, resource ID conflict olabilir
- **Etki**: Unknown
- **Çözüm**: Tüm layout XML'lerini oku ve analiz et
- **Tahmini Süre**: 3 saat
- **Test**: Build ve runtime test
- **Notlar**: Ayrı bir analiz task'ı

---

### [ ] B041: ProGuard Rules Validation
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `proguard-rules.pro`
- **Sorun**: ProGuard rules doğrulanmadı, Room/WorkManager reflection break olabilir
- **Etki**: Release build'de runtime crash riski
- **Çözüm**: proguard-rules.pro dosyasını oku ve validate et
- **Tahmini Süre**: 1 saat
- **Test**: Release build ile full test suite çalıştır
- **Notlar**: Özellikle Room, WorkManager, Gson rules kontrol et

---

### [ ] B042: Dependency Version Updates
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `app/build.gradle`
- **Sorun**: WorkManager 2.8.1, güncel versiyon kontrol edilmeli
- **Etki**: Bug fixes ve yeni features kaçırılıyor
- **Çözüm**:
  ```bash
  ./gradlew dependencyUpdates

  # Check for updates:
  # - androidx.work:work-runtime
  # - androidx.room:room-runtime
  # - com.google.android.material:material
  ```
- **Tahmini Süre**: 1 saat
- **Test**: Full regression test after updates
- **Notlar**: Changelog'ları oku, breaking changes kontrol et

---

### [ ] B043: Database Migration Documentation
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `AppDatabase.java`
- **Sorun**: 7 version, migration tracking zor
- **Etki**: Developer confusion
- **Çözüm**: Migration documentation ekle
  ```java
  /**
   * Database Version History:
   *
   * Version 1 (Initial):
   * - target_numbers table created
   * - sms_history table created
   *
   * Version 2:
   * - Added 'enabled' column to target_numbers
   *
   * Version 3:
   * - Added 'display_name' column to target_numbers
   *
   * ... (continue for all versions)
   */
  ```
- **Tahmini Süre**: 30 dakika
- **Test**: N/A (documentation)
- **Notlar**: README veya separate MIGRATIONS.md file

---

### [ ] B044: ImageButton Missing ContentDescription
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - `FilterRulesActivity.java` layout (satır: 102)
- **Sorun**: ImageButton muhtemelen contentDescription eksik
- **Etki**: Accessibility for screen readers
- **Çözüm**:
  ```xml
  <ImageButton
      android:id="@+id/btn_help"
      android:layout_width="wrap_content"
      android:layout_height="wrap_content"
      android:src="@drawable/ic_help"
      android:contentDescription="@string/help_button_description"
      android:background="?attr/selectableItemBackgroundBorderless" />
  ```
- **Tahmini Süre**: 1 saat (tüm layout'ları kontrol et)
- **Test**: TalkBack ile test et
- **Notlar**: Tüm ImageButton, ImageView için contentDescription ekle

---

### [ ] B045: Adapter Context Reference Leak
- **Önem**: 🟢 DÜŞÜK
- **Dosyalar**:
  - Adapter classes (TargetNumberAdapter, FilterAdapter, etc.)
- **Sorun**: Adapter'lar Context reference tutuyor, lifecycle-aware değil
- **Etki**: Minor memory leak if Activity destroyed
- **Çözüm**:
  ```java
  // Option 1: WeakReference
  private final WeakReference<Context> contextRef;

  public MyAdapter(Context context) {
      this.contextRef = new WeakReference<>(context);
  }

  // Option 2: Application context
  public MyAdapter(Context context) {
      this.context = context.getApplicationContext();
  }

  // Option 3: Pass context per method call
  public void bindData(Context context, Data data) {
      // Use context here
  }
  ```
- **Tahmini Süre**: 2 saat
- **Test**: LeakCanary ile adapter lifecycle test
- **Notlar**: Application context non-UI operations için ok, UI için WeakReference tercih et

---

## 📋 HIZLI EYLEM PLANI

### Hafta 1 (Kritik Buglar):
- [ ] B001: getColor() → ContextCompat.getColor()
- [ ] B002: Database NULL checks
- [ ] B004: PendingIntent API fix
- [ ] B005: SecurityException handling

### Hafta 2 (Yüksek Öncelikli):
- [ ] B006: Handler memory leak
- [ ] B007: Cursor leaks
- [ ] B008: Thread.sleep removal
- [ ] B011: Bundle validation
- [ ] B012: SIM selection validity

### Hafta 3-4 (Orta Öncelikli Seçilmiş):
- [ ] B016: Dialog memory leaks
- [ ] B018: WorkManager policy
- [ ] B021: Database transactions
- [ ] B023: SIM tracking in callbacks
- [ ] B025: Filter priority UI

### Sürekli İyileştirme (Düşük Öncelikli):
- [ ] Code quality issues (B027-B045)
- [ ] Documentation improvements
- [ ] Test coverage increase

---

## 🧪 TEST ARAÇLARI

### Memory Leak Detection:
```gradle
// app/build.gradle
dependencies {
    debugImplementation 'com.squareup.leakcanary:leakcanary-android:2.12'
}
```

### Thread Safety Testing:
```bash
# Android Studio Profiler kullan
# Thread monitoring ve race condition detection
```

### Lint Analysis:
```bash
./gradlew lint --continue
# Report: app/build/reports/lint-results.html
```

---

## 📝 NOTLAR

- Her bug düzeltmesinden sonra related test'leri çalıştır
- Critical buglar için regression test suite oluştur
- Build başarılı olana kadar sonraki bug'a geçme
- Git commit message formatı: `fix(B001): Replace deprecated getColor() with ContextCompat`
- Her commit'te changelog.md güncelle

---

**Son Güncelleme**: 2025-10-01
**Durum**: İlk analiz tamamlandı, düzeltme bekliyor
**Sonraki İnceleme**: Tüm kritik buglar düzeltildikten sonra
