@echo off
chcp 65001 >nul
if not exist out mkdir out

:: Путь к Java из IntelliJ IDEA
set "JAVA_BIN=D:\programms\IntelliJ IDEA 2026.2.3\JDK\bin"

echo Компиляция эмулятора...
"%JAVA_BIN%\javac.exe" src/Main.java -d out
if %errorlevel% neq 0 ( pause & exit /b )

echo.
echo === Список тестируемых команд из tests.ini ===
type tests\tests.ini
echo ==========================================

echo.
echo === Автоматический тест команд ===
"%JAVA_BIN%\java.exe" -cp out Main < tests\tests.ini

echo.
echo === Запуск эмулятора ===
"%JAVA_BIN%\java.exe" -cp out Main
pause
