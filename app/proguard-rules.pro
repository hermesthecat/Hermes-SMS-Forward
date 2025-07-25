# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep SMS-related classes and methods
-keep class com.keremgok.sms.SmsReceiver {
    public *;
}

-keep class com.keremgok.sms.MainActivity {
    public *;
}

-keep class com.keremgok.sms.PhoneNumberValidator {
    public *;
}

# Keep BroadcastReceiver classes
-keep public class * extends android.content.BroadcastReceiver {
    public *;
}

# Keep SMS and telephony related classes
-keep class android.telephony.SmsMessage {
    public *;
}

-keep class android.telephony.SmsManager {
    public *;
}

# Keep Activity classes
-keep public class * extends android.app.Activity {
    public protected *;
}

# Keep Service classes
-keep public class * extends android.app.Service {
    public *;
}

# Keep Application class
-keep public class * extends android.app.Application {
    public *;
}

# Keep Context classes
-keep class android.content.Context {
    public *;
}

# Keep SharedPreferences related classes
-keep class android.content.SharedPreferences {
    public *;
}

-keep class android.content.SharedPreferences$Editor {
    public *;
}

# Keep permission related classes
-keep class android.content.pm.PackageManager {
    public static final int PERMISSION_GRANTED;
    public static final int PERMISSION_DENIED;
}

# Keep TextWatcher classes for input validation
-keep interface android.text.TextWatcher {
    public *;
}

# Keep Bundle and Intent classes
-keep class android.os.Bundle {
    public *;
}

-keep class android.content.Intent {
    public *;
}

# Keep Log class for debugging
-keep class android.util.Log {
    public static *** d(...);
    public static *** e(...);
    public static *** i(...);
    public static *** w(...);
    public static *** v(...);
}

# Keep BuildConfig class
-keep class com.keremgok.sms.BuildConfig {
    public static final boolean DEBUG;
}

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Serializable classes
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Remove logging in release builds (except errors)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
}

# Keep all public methods in MainActivity for proper functionality
-keepclassmembers class com.keremgok.sms.MainActivity {
    public void onRequestPermissionsResult(int, java.lang.String[], int[]);
    public void saveTargetNumber(android.view.View);
}

# Keep annotation classes
-keepattributes *Annotation*

# Optimization settings
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify