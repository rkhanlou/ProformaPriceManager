@echo off
setlocal
cd /d "%~dp0"

set "JAVA23=C:\Program Files\Java\jdk-23\bin\java.exe"

if not exist "%JAVA23%" (
    echo Java 23 not found: %JAVA23%
    pause
    exit /b 1
)

"%JAVA23%" -Djdk.tls.client.protocols=TLSv1 -jar "target\ExportPriceManager.jar"
pause
endlocal
