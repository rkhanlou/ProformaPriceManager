@echo off
setlocal
cd /d "%~dp0"

set "JAVA23=C:\Program Files\Java\jdk-23\bin\javaw.exe"

if not exist "%JAVA23%" (
    echo Java 23 not found:
    echo %JAVA23%
    echo.
    echo Edit Run-Java23.bat and set JAVA23 to your Java 23 javaw.exe path.
    pause
    exit /b 1
)

if not exist "target\ExportPriceManager.jar" (
    echo target\ExportPriceManager.jar not found.
    echo Run Maven Clean and Build first.
    pause
    exit /b 1
)

start "" "%JAVA23%" -Djdk.tls.client.protocols=TLSv1 -jar "target\ExportPriceManager.jar"
endlocal
