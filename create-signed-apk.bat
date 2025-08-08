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
    echo 5. Archiving APK...
    
    REM Extract version from build.gradle directly
    echo Getting version from build.gradle...
    REM Find the main versionName line (line 32)
    for /f "skip=31 tokens=2 delims= " %%i in (app\build.gradle) do (
        if "!VERSION!"=="" (
            set VERSION_WITH_QUOTES=%%i
            REM Remove quotes from version string
            set VERSION=!VERSION_WITH_QUOTES:"=!
        )
    )
    if "!VERSION!"=="" (
        echo ❌ Could not extract version from build.gradle
        echo Please check app\build.gradle format
        pause
        exit /b 1
    )
    echo Found version: !VERSION!
    
    REM Create archive directory
    if not exist "apk_archive" mkdir apk_archive
    
    REM Generate APK name with incremental numbering
    REM Get current date in YYYYMMDD format
    for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value ^| findstr "="') do set datetime=%%a
    set CURRENT_DATE=!datetime:~0,8!
    set BASE_NAME=sms-forward-v!VERSION!-signed-!CURRENT_DATE!
    set BUILD_NUM=1
    
    :find_next_number
    set APK_NAME=!BASE_NAME!-!BUILD_NUM!.apk
    if exist "apk_archive\!APK_NAME!" (
        set /a BUILD_NUM+=1
        goto find_next_number
    )
    
    REM Check if APK exists and copy
    echo.
    echo Checking for release APK...
    if exist "app\build\outputs\apk\release\app-release.apk" (
        echo ✅ APK found: app\build\outputs\apk\release\app-release.apk
        echo.
        echo Copying APK as: !APK_NAME!
        copy "app\build\outputs\apk\release\app-release.apk" "apk_archive\!APK_NAME!" > nul
        set COPY_RESULT=!errorlevel!
        if !COPY_RESULT!==0 (
            echo ✅ APK successfully archived as: apk_archive\!APK_NAME!
            echo.
            echo Archive contents:
            dir apk_archive\*.apk
            echo.
            echo ✅ APK creation and archiving completed successfully!
            REM Preserve success status after dir command
            set BUILD_RESULT=0
        ) else (
            echo ❌ Failed to copy APK to archive
        )
    ) else (
        echo ❌ APK not found: app\build\outputs\apk\release\app-release.apk
        echo.
        echo Checking build output directory...
        if exist "app\build\outputs\apk\release\" (
            echo Release directory contents:
            dir app\build\outputs\apk\release\
        ) else (
            echo Release directory does not exist - build may have failed
        )
    )
    echo.
    REM Check final BUILD_RESULT status
    if !BUILD_RESULT!==0 (
        REM Success - exit cleanly
        exit /b 0
    ) else (
        REM Build actually failed
        echo ❌ Build failed with error code: !BUILD_RESULT!
        echo.
        echo Possible reasons:
        echo - Keystore password is incorrect
        echo - Android SDK is not properly configured
        echo - Build dependencies are missing
        echo.
        pause
        exit /b !BUILD_RESULT!
    )
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