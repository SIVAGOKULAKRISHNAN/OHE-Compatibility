@echo off
setlocal
cd /d "%~dp0"
echo === Fix 30 clean build ===
call gradlew.bat clean build --no-daemon
if errorlevel 1 (
  echo BUILD FAILED
  exit /b 1
)
echo.
echo === Build output ===
dir /b build\libs\*.jar
echo.
echo === Fix 30 build completed successfully ===
