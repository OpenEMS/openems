@echo off
REM ############################################################################
REM ABL Modbus Simulator Launcher (Windows)
REM
REM Quick launcher script for the ABL EVCC2/3 Modbus simulator on Windows.
REM Supports TCP and Serial (ASCII/RTU) modes.
REM
REM Usage:
REM   run-simulator.bat tcp [ip] [port] [deviceId]
REM   run-simulator.bat serial [port] [baudrate] [deviceId]
REM   run-simulator.bat serial-rtu [port] [baudrate] [deviceId]
REM ############################################################################

setlocal enabledelayedexpansion

echo ABL Modbus Simulator Launcher (Windows)
echo ========================================
echo.

REM Get script directory
set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set TEST_DIR=%PROJECT_ROOT%\test

REM Find j2mod library
echo Looking for j2mod library...
set J2MOD_JAR=

REM Check Maven local repository
if exist "%USERPROFILE%\.m2\repository\com\ghgande\j2mod\" (
    for /r "%USERPROFILE%\.m2\repository\com\ghgande\j2mod" %%f in (j2mod*.jar) do (
        set J2MOD_JAR=%%f
        goto :found_jar
    )
)

REM Check Gradle cache
if exist "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.ghgande\j2mod\" (
    for /r "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.ghgande\j2mod" %%f in (j2mod*.jar) do (
        set J2MOD_JAR=%%f
        goto :found_jar
    )
)

:found_jar
if "%J2MOD_JAR%"=="" (
    echo ERROR: j2mod library not found!
    echo Please install it via Maven:
    echo   mvn dependency:get -Dartifact=com.ghgande:j2mod:3.2.1
    echo Or download from: https://repo1.maven.org/maven2/com/ghgande/j2mod/
    exit /b 1
)

echo Found j2mod: %J2MOD_JAR%

REM Build classpath
set CLASSPATH=%TEST_DIR%;%J2MOD_JAR%

REM Parse command line - default to ASCII mode if no args or COM port specified
set FIRST_ARG=%1

REM Check if first arg is empty or looks like a COM port (default to ASCII)
if "%FIRST_ARG%"=="" (
    set MODE=ascii
    set ADDRESS=COM3
    set PORT=0
    set BAUDRATE=38400
    set DEVICE_ID=1
    set SIMULATOR_MODE=SERIAL_ASCII
    goto :config_done
)

REM Check if first arg looks like a COM port
echo %FIRST_ARG% | findstr /R "^COM[0-9]" >nul
if %errorlevel%==0 (
    set MODE=ascii
    set ADDRESS=%1
    set PORT=0
    if not "%2"=="" (set BAUDRATE=%2) else (set BAUDRATE=38400)
    if not "%3"=="" (set DEVICE_ID=%3) else (set DEVICE_ID=1)
    set SIMULATOR_MODE=SERIAL_ASCII
    goto :config_done
)

REM Explicit mode specified
set MODE=%1

if /i "%MODE%"=="tcp" (
    set SIMULATOR_MODE=TCP
    if not "%2"=="" (set ADDRESS=%2) else (set ADDRESS=127.0.0.1)
    if not "%3"=="" (set PORT=%3) else (set PORT=502)
    set BAUDRATE=0
    if not "%4"=="" (set DEVICE_ID=%4) else (set DEVICE_ID=1)
) else if /i "%MODE%"=="ascii" (
    set SIMULATOR_MODE=SERIAL_ASCII
    if not "%2"=="" (set ADDRESS=%2) else (set ADDRESS=COM3)
    set PORT=0
    if not "%3"=="" (set BAUDRATE=%3) else (set BAUDRATE=38400)
    if not "%4"=="" (set DEVICE_ID=%4) else (set DEVICE_ID=1)
) else if /i "%MODE%"=="rtu" (
    set SIMULATOR_MODE=SERIAL_RTU
    if not "%2"=="" (set ADDRESS=%2) else (set ADDRESS=COM3)
    set PORT=0
    if not "%3"=="" (set BAUDRATE=%3) else (set BAUDRATE=9600)
    if not "%4"=="" (set DEVICE_ID=%4) else (set DEVICE_ID=1)
) else (
    echo ERROR: Unknown mode '%MODE%'
    echo.
    echo Usage:
    echo   %0                              # ASCII mode with defaults (38400 8E1)
    echo   %0 [port]                       # ASCII mode with custom port
    echo   %0 [port] [baudrate] [deviceId] # ASCII mode with custom settings
    echo.
    echo   %0 tcp [ip] [port] [deviceId]
    echo   %0 ascii [port] [baudrate] [deviceId]
    echo   %0 rtu [port] [baudrate] [deviceId]
    echo.
    echo Examples:
    echo   %0                              # ASCII on COM3 @ 38400
    echo   %0 COM4                         # ASCII on COM4 @ 38400
    echo   %0 COM3 19200 1                 # ASCII on COM3 @ 19200
    echo   %0 tcp                          # TCP on localhost:502
    echo   %0 tcp 0.0.0.0 502 1           # TCP on all interfaces
    echo   %0 ascii COM3 38400 1          # Explicit ASCII
    echo   %0 rtu COM3 9600 1             # RTU mode
    exit /b 1
)

:config_done

REM Display configuration
echo.
echo Configuration:
echo   Mode:      %SIMULATOR_MODE%
if "%SIMULATOR_MODE%"=="TCP" (
    echo   Address:   %ADDRESS%:%PORT%
) else (
    echo   Port:      %ADDRESS%
    echo   Baudrate:  %BAUDRATE%
)
echo   Device ID: %DEVICE_ID%
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java not found!
    echo Please install Java 21 or later
    echo Download from: https://adoptium.net/
    exit /b 1
)

REM Check if class files exist
set MAIN_CLASS=io.openems.edge.evse.chargepoint.abl.simulator.AblModbusSimulator
set CLASS_FILE=%TEST_DIR%\io\openems\edge\evse\chargepoint\abl\simulator\AblModbusSimulator.class

if not exist "%CLASS_FILE%" (
    echo WARNING: Class files not found. Please compile first:
    echo   cd %TEST_DIR%
    echo   javac -cp "%CLASSPATH%" io\openems\edge\evse\chargepoint\abl\simulator\*.java
    echo.
    pause
    exit /b 1
)

REM Launch simulator
echo Starting simulator...
echo Press Ctrl+C to stop
echo.

cd /d "%TEST_DIR%"
java -cp "%CLASSPATH%" %MAIN_CLASS% %SIMULATOR_MODE% %ADDRESS% %PORT% %BAUDRATE% %DEVICE_ID%

REM Exit
set EXIT_CODE=%errorlevel%
echo.
if %EXIT_CODE% equ 0 (
    echo Simulator stopped normally
) else (
    echo Simulator exited with error code %EXIT_CODE%
)

endlocal
exit /b %EXIT_CODE%
