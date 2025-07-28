@echo off
setlocal enabledelayedexpansion
echo ========================================
echo SMS Forward - Create Signed APK
echo ========================================
echo.

echo 1. Set Android SDK Root
set ANDROID_SDK_ROOT=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk

echo 2. Check Keystore Configuration
if not exist "keystore.properties" (
    echo ⚠️ keystore.properties file not found!
    echo.
    echo Create keystore.properties file with:
    echo storeFile=release-key.keystore
    echo keyAlias=sms-forward-key
    echo storePassword=your_keystore_password
    echo keyPassword=your_key_password
    echo.
    echo Then create keystore with:
    echo keytool -genkey -v -keystore release-key.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward-key
    echo.
    pause
    exit /b 1
)

REM Read keystore file path from properties
for /f "tokens=2 delims==" %%a in ('findstr "storeFile" keystore.properties') do set KEYSTORE_FILE=%%a
REM Check if keystore exists (try with app/ prefix first, then without)
if exist "app\%KEYSTORE_FILE%" (
    echo ✅ Keystore file found: app\%KEYSTORE_FILE%
) else if exist "%KEYSTORE_FILE%" (
    echo ✅ Keystore file found: %KEYSTORE_FILE%
) else (
    echo ⚠️ Keystore file not found: %KEYSTORE_FILE%
    echo.
    echo Create keystore with:
    echo keytool -genkey -v -keystore app\%KEYSTORE_FILE% -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward-key
    echo.
    pause
    exit /b 1
)

echo 3. Check keystore.properties
findstr /C:"your_keystore_password" keystore.properties >nul 2>&1
if %errorlevel%==0 (
    echo ⚠️ keystore.properties passwords need to be updated!
    echo.
    echo Edit keystore.properties and update:
    echo - storePassword=your_keystore_password
    echo - keyPassword=your_key_password
    echo.
    echo Enter the actual keystore passwords.
    echo.
    echo After updating the passwords, press any key...
    pause
)

echo 4. Creating Signed Release APK...
echo.
echo Building signed release APK...
call gradlew.bat clean assembleRelease
set BUILD_RESULT=%errorlevel%

if %BUILD_RESULT%==0 (
    echo.
    echo ✅ Build completed successfully!
    echo.
    echo 5. Running APK archiving...
    call quick-archive.bat
) else (
    echo.
    echo ❌ Build failed with error code: %BUILD_RESULT%
    echo.
    echo Possible reasons:
    echo - Keystore password is incorrect
    echo - Android SDK is not properly configured
    echo - Build dependencies are missing
    echo.
    pause
    exit /b %BUILD_RESULT%
)

:end_script
echo.