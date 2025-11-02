@echo off
REM ================================================================
REM CashBee Application Start Script (Windows)
REM ================================================================
REM Purpose: Build and run the CashBee backend application
REM Usage: scripts\start.bat
REM ================================================================

echo ======================================
echo   CashBee Backend - Start Script
echo ======================================
echo.

REM Check if Maven wrapper exists
if not exist "mvnw.cmd" (
    echo Error: Maven wrapper ^(mvnw.cmd^) not found!
    exit /b 1
)

REM Check Java
echo Checking Java version...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java not found! Please install Java 17+
    exit /b 1
)

echo [OK] Java version OK
echo.

REM Build application
echo Building application...
call mvnw.cmd clean install -DskipTests

if %errorlevel% neq 0 (
    echo [ERROR] Build failed
    exit /b 1
)

echo [OK] Build successful
echo.

REM Run application
echo Starting application...
echo Access points:
echo   - API:         http://localhost:8080
echo   - Swagger UI:  http://localhost:8080/swagger-ui.html
echo   - Health:      http://localhost:8080/actuator/health
echo.
echo Press Ctrl+C to stop
echo.

call mvnw.cmd spring-boot:run -pl cashbee-presentation
