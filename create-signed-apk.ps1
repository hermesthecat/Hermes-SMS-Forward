Write-Host "========================================" -ForegroundColor Green
Write-Host "SMS Forward - Create Signed APK (PowerShell)" -ForegroundColor Green  
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# 1. Set Android SDK Root
Write-Host "1. Set Android SDK Root"
$env:ANDROID_SDK_ROOT = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"
$env:ANDROID_HOME = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk"

# 2. Check Keystore Configuration
Write-Host "2. Check Keystore Configuration"
if (!(Test-Path "keystore.properties")) {
    Write-Host "⚠️ keystore.properties file not found!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Create keystore.properties file with:"
    Write-Host "storeFile=release-key.keystore"
    Write-Host "keyAlias=sms-forward-key"
    Write-Host "storePassword=your_keystore_password"
    Write-Host "keyPassword=your_key_password"
    Write-Host ""
    Write-Host "Then create keystore with:"
    Write-Host "keytool -genkey -v -keystore release-key.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward-key"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# Read keystore file path from properties
$keystoreFile = (Get-Content "keystore.properties" | Where-Object { $_ -match "storeFile=" }) -replace "storeFile=", ""

# Check if keystore exists
if (Test-Path "app\$keystoreFile") {
    Write-Host "✅ Keystore file found: app\$keystoreFile" -ForegroundColor Green
} elseif (Test-Path $keystoreFile) {
    Write-Host "✅ Keystore file found: $keystoreFile" -ForegroundColor Green
} else {
    Write-Host "⚠️ Keystore file not found: $keystoreFile" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Create keystore with:"
    Write-Host "keytool -genkey -v -keystore app\$keystoreFile -keyalg RSA -keysize 2048 -validity 10000 -alias sms-forward-key"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

# 3. Check keystore.properties for placeholder passwords
Write-Host "3. Check keystore.properties"
$content = Get-Content "keystore.properties" -Raw
if ($content -match "your_keystore_password") {
    Write-Host "⚠️ keystore.properties passwords need to be updated!" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Edit keystore.properties and update:"
    Write-Host "- storePassword=your_keystore_password"
    Write-Host "- keyPassword=your_key_password"
    Write-Host ""
    Write-Host "Enter the actual keystore passwords."
    Write-Host ""
    Read-Host "After updating the passwords, press Enter to continue"
}

# 4. Creating Signed Release APK
Write-Host "4. Creating Signed Release APK..."
Write-Host ""
Write-Host "DEBUG: About to call gradlew.bat..."

$process = Start-Process -FilePath ".\gradlew.bat" -ArgumentList "clean", "assembleRelease" -Wait -PassThru -NoNewWindow
$buildResult = $process.ExitCode

Write-Host "DEBUG: gradlew.bat returned, exit code: $buildResult"
Write-Host "Build completed with result: $buildResult"

if ($buildResult -eq 0) {
    Write-Host ""
    Write-Host "✅ Signed APK created successfully!" -ForegroundColor Green
    Write-Host "Proceeding to APK archiving..."
    Write-Host ""
    
    # 5. Creating APK archive directory
    Write-Host "5. Creating APK archive directory..."
    if (!(Test-Path "apk_archive")) {
        New-Item -ItemType Directory -Path "apk_archive" | Out-Null
        Write-Host "Created apk_archive directory"
    }
    
    # 6. Getting version from app\build.gradle
    Write-Host "6. Getting version from app\build.gradle..."
    $buildGradleContent = Get-Content "app\build.gradle"
    $versionLine = $buildGradleContent | Where-Object { $_ -match "versionName" }
    $version = ($versionLine -split '"')[1]
    Write-Host "Found version: $version"
    
    # 7. Moving signed APK to archive with incremental numbering
    Write-Host "7. Moving signed APK to archive..."
    $dateString = Get-Date -Format "yyyyMMdd"
    $baseApkName = "sms-forward-v$version-signed-$dateString"
    
    # Find next available number for today
    $buildNumber = 1
    do {
        $apkName = "$baseApkName-$buildNumber.apk"
        $buildNumber++
    } while (Test-Path "apk_archive\$apkName")
    
    Write-Host "Target APK name: $apkName"
    
    # Check for APK file
    Write-Host "Checking for APK file..."
    $apkSource = ""
    
    if (Test-Path "app\build\outputs\apk\release\app-release.apk") {
        $apkSource = "app\build\outputs\apk\release\app-release.apk"
        Write-Host "Found release APK: app-release.apk"
    } else {
        Write-Host "Standard APK not found, listing directory contents..."
        if (Test-Path "app\build\outputs\apk\release\") {
            Write-Host "Release directory contents:"
            Get-ChildItem "app\build\outputs\apk\release\*.apk"
        } else {
            Write-Host "Release directory does not exist"
        }
    }
    
    if ($apkSource -ne "") {
        Write-Host "✅ APK file found: $apkSource" -ForegroundColor Green
        Write-Host "Copying to archive..."
        Copy-Item $apkSource "apk_archive\$apkName"
        
        if ($?) {
            Write-Host "✅ APK successfully archived as: apk_archive\$apkName" -ForegroundColor Green
            Write-Host ""
            Write-Host "Listing archive contents:"
            Get-ChildItem "apk_archive\*.apk"
            Write-Host ""
            Write-Host "✅ APK archiving completed successfully!" -ForegroundColor Green
        } else {
            Write-Host "❌ Failed to copy APK to archive" -ForegroundColor Red
            Write-Host "Check permissions and disk space"
        }
    } else {
        Write-Host "❌ No APK file found in release directory" -ForegroundColor Red
        Write-Host "Checking build directory contents..."
        if (Test-Path "app\build\outputs\apk\release\") {
            Write-Host "Release directory exists, listing contents:"
            Get-ChildItem "app\build\outputs\apk\release\"
        } else {
            Write-Host "Release directory does not exist - build may have failed"
        }
    }
} else {
    Write-Host ""
    Write-Host "❌ APK creation failed!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Possible reasons:"
    Write-Host "- Keystore password is incorrect"
    Write-Host "- Android SDK is not installed"
    Write-Host "- Java is not installed"
    Write-Host ""
}

Write-Host ""
Write-Host "Script completed. Press Enter to exit."
Read-Host