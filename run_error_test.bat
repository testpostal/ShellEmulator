@echo off
chcp 65001 >nul
set "JAVA_BIN=D:\programms\IntelliJ IDEA 2026.2.3\JDK\bin"

echo [Скрипт ОС] Тестирование ошибки параметров командной строки...
echo.

"%JAVA_BIN%\java.exe" -cp out Main
echo.
pause
