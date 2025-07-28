@echo off
echo ========================================
echo Quick APK Archive
echo ========================================

REM Use hardcoded version for now
set VERSION=2.11.0

REM Create archive directory
if not exist "apk_archive" mkdir apk_archive

REM Generate APK name with incremental numbering
REM Get current date in YYYYMMDD format
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value ^| findstr "="') do set datetime=%%a
set CURRENT_DATE=%datetime:~0,8%
set BASE_NAME=sms-forward-v%VERSION%-signed-%CURRENT_DATE%
set BUILD_NUM=1

:find_next_number
set APK_NAME=%BASE_NAME%-%BUILD_NUM%.apk
if exist "apk_archive\%APK_NAME%" (
    set /a BUILD_NUM+=1
    goto find_next_number
)

REM Check if APK exists and copy
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo Copying APK: %APK_NAME%
    copy "app\build\outputs\apk\release\app-release.apk" "apk_archive\%APK_NAME%"
    echo ✅ APK archived as: apk_archive\%APK_NAME%
    dir apk_archive\*.apk
) else (
    echo ❌ APK not found: app\build\outputs\apk\release\app-release.apk
)

REM pause