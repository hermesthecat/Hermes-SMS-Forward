@echo off
setlocal enabledelayedexpansion
echo ========================================
echo Quick APK Archive
echo ========================================

REM Extract version from build.gradle
echo Getting version from build.gradle...
for /f "tokens=3 delims= " %%a in ('findstr "versionName" app\build.gradle') do (
    set VERSION=%%a
)
REM Remove quotes
set VERSION=!VERSION:"=!
echo Found version: !VERSION!

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
echo.
echo Checking for release APK...
if exist "app\build\outputs\apk\release\app-release.apk" (
    echo ✅ APK found: app\build\outputs\apk\release\app-release.apk
    echo.
    echo Copying APK as: !APK_NAME!
    copy "app\build\outputs\apk\release\app-release.apk" "apk_archive\!APK_NAME!" > nul
    if %errorlevel%==0 (
        echo ✅ APK successfully archived as: apk_archive\!APK_NAME!
        echo.
        echo Archive contents:
        dir apk_archive\*.apk
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