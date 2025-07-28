# PowerShell script to extract version from build.gradle
$content = Get-Content "app\build.gradle"
$versionLine = $content | Where-Object { $_ -match "versionName" -and $_ -notmatch "Suffix" }
if ($versionLine -match '"([^"]+)"') {
    $version = $Matches[1]
    Write-Output $version
} else {
    Write-Error "Could not extract version from build.gradle"
    exit 1
}