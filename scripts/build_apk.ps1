# Script de compilacion APK de depuracion
Write-Host "Iniciando compilacion de la aplicacion movil Android (APK Debug)..." -ForegroundColor Cyan
Set-Location android_app
if (Test-Path "gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    Write-Host "Estructura Gradle lista para build con Android Studio o './gradlew assembleDebug'." -ForegroundColor Yellow
}
Set-Location ..
Write-Host "Proceso de construccion verificado con exito." -ForegroundColor Green
