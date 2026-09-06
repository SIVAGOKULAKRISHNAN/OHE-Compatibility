@echo off
setlocal
cd /d "%~dp0"

set "GRADLE_VERSION=8.13"
set "GRADLE_HOME=%USERPROFILE%\.gradle\ohecompat\gradle-%GRADLE_VERSION%"
set "GRADLE_ZIP=%USERPROFILE%\.gradle\ohecompat\gradle-%GRADLE_VERSION%-bin.zip"
set "GRADLE_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if exist "%GRADLE_HOME%\bin\gradle.bat" goto run

echo.
echo [OHE] Gradle %GRADLE_VERSION% is not installed for this project.
echo [OHE] Downloading the official Gradle distribution...
echo.

if not exist "%USERPROFILE%\.gradle\ohecompat" mkdir "%USERPROFILE%\.gradle\ohecompat"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%GRADLE_URL%' -OutFile '%GRADLE_ZIP%'"
if errorlevel 1 (
  echo.
  echo [OHE] ERROR: Could not download Gradle.
  echo [OHE] Check your internet connection and try again.
  exit /b 1
)

echo [OHE] Extracting Gradle...
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "Expand-Archive -LiteralPath '%GRADLE_ZIP%' -DestinationPath '%USERPROFILE%\.gradle\ohecompat' -Force"
if errorlevel 1 (
  echo.
  echo [OHE] ERROR: Could not extract Gradle.
  exit /b 1
)

if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  echo.
  echo [OHE] ERROR: Gradle installation was not found after extraction.
  exit /b 1
)

:run
call "%GRADLE_HOME%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
