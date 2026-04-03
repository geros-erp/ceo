@echo off
echo ========================================
echo Deteniendo Backend...
echo ========================================
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :8443') do (
    taskkill /F /PID %%a 2>nul
)
timeout /t 2 /nobreak >nul

echo.
echo ========================================
echo Iniciando Backend...
echo ========================================
cd backend
call mvnw.cmd spring-boot:run
