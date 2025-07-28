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
echo DEBUG: About to call gradlew.bat...
call gradlew.bat clean assembleRelease
set BUILD_RESULT=%errorlevel%
echo DEBUG: gradlew.bat returned, errorlevel: %BUILD_RESULT%
echo Build completed with result: %BUILD_RESULT%

if %BUILD_RESULT%==0 (
    echo.
    echo ✅ Signed APK created successfully!
    echo Proceeding to APK archiving...
    echo.
    
    echo 5. Creating APK archive directory...
    if not exist "apk_archive" (
        mkdir apk_archive
        echo Created apk_archive directory
    )
    
    echo 6. Getting version from app\build.gradle...
    REM Extract version name from build.gradle
    for /f "tokens=3 delims= " %%a in ('findstr "versionName" app\build.gradle') do (
        set VERSION=%%a
    )
    REM Remove quotes if present
    set VERSION=!VERSION:"=!
    echo Found version: !VERSION!
    
    echo 7. Moving signed APK to archive...
    set APK_NAME=sms-forward-v!VERSION!-signed-%date:~6,4%%date:~3,2%%date:~0,2%.apk
    echo Target APK name: !APK_NAME!
    
    echo Checking for APK file...
    REM Use known APK name pattern for SMS Forward project
    set APK_SOURCE=
    echo DEBUG: Checking for release APK...
    if exist "app\build\outputs\apk\release\app-release.apk" (
        set APK_SOURCE=app\build\outputs\apk\release\app-release.apk
        echo Found release APK: app-release.apk
    ) else (
        echo Standard APK not found, listing directory contents...
        if exist "app\build\outputs\apk\release\" (
            echo Release directory contents:
            dir app\build\outputs\apk\release\*.apk
        ) else (
            echo Release directory does not exist
        )
    
    :apk_found
    if not "!APK_SOURCE!"=="" (
        echo ✅ APK file found: !APK_SOURCE!
        echo Copying to archive...
        copy "!APK_SOURCE!" "apk_archive\!APK_NAME!"
        set COPY_RESULT=!errorlevel!
        if !COPY_RESULT!==0 (
            echo ✅ APK successfully archived as: apk_archive\!APK_NAME!
            echo.
            echo Listing archive contents:
            dir apk_archive\*.apk
            echo.
            echo.
            echo ✅ APK archiving completed successfully!
            goto :end_script
        ) else (
            echo ❌ Failed to copy APK to archive (Error code: !COPY_RESULT!)
            echo Check permissions and disk space
        )
    ) else (
        echo ❌ No APK file found in release directory
        echo Checking build directory contents...
        if exist "app\build\outputs\apk\release\" (
            echo Release directory exists, listing contents:
            dir app\build\outputs\apk\release\
        ) else (
            echo Release directory does not exist - build may have failed
        )
    )
) else (
    echo.
    echo ❌ APK creation failed!
    echo.
    echo Possible reasons:
    echo - Keystore password is incorrect
    echo - Android SDK is not installed
    echo - Java is not installed
    echo.
)

:end_script
echo.