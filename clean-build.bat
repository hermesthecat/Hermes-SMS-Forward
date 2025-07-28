@echo off
echo ========================================
echo SMS Forward - Clean & Build Debug APK
echo ========================================
echo.

echo 1. Setting Android SDK paths...
REM Try multiple common Android SDK locations
if exist "C:\Users\%USERNAME%\AppData\Local\Android\Sdk" (
    set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
    set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
    echo ✅ Android SDK found at: C:\Users\%USERNAME%\AppData\Local\Android\Sdk
) else if exist "C:\Android\Sdk" (
    set ANDROID_SDK_ROOT=C:\Android\Sdk
    set ANDROID_HOME=C:\Android\Sdk
    echo ✅ Android SDK found at: C:\Android\Sdk
) else (
    echo ⚠️ Android SDK not found in common locations
    echo Will try to build anyway - Gradle might find SDK automatically
)
echo.

echo 2. Cleaning project...
call gradlew.bat clean

if %errorlevel%==0 (
    echo ✅ Clean completed successfully!
    echo.
    echo 3. Building SMS Forward project...
    call gradlew.bat assembleDebug
    
    echo.
    if %errorlevel%==0 (
        echo.
        echo ✅ Clean build completed successfully!
        echo.
        echo APK location:
        echo app\build\outputs\apk\debug\app-debug.apk
        echo.
        if exist "app\build\outputs\apk\debug\app-debug.apk" (
            echo ✅ APK file verified to exist
        ) else (
            echo ⚠️ APK file not found at expected location
        )
    ) else (
        echo ❌ Build failed with error code: %errorlevel%
    )
) else (
    echo ❌ Clean failed with error code: %errorlevel%
)
echo.
pause 