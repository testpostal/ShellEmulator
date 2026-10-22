@echo off
chcp 65001 >nul
if not exist out mkdir out

set "JAVA_BIN=D:\programms\IntelliJ IDEA 2026.2.3\JDK\bin"

echo Компиляция эмулятора...
"%JAVA_BIN%\javac.exe" src/Main.java -d out
if %errorlevel% neq 0 ( pause & exit /b )

echo.
echo ========================================================
echo                       МЕНЮ ЗАПУСКА
echo ========================================================
echo [1] Обычный запуск
echo [2] Запуск стартового скрипта автотестов файла tests.ini
echo ========================================================
echo.

set /p choice="Выберите вариант запуска (1 или 2) и нажмите Enter: "

if "%choice%"=="1" (
    echo.
    echo [Система] Запуск в интерактивном режиме...

    if not exist tests\empty.ini echo # Пустой скрипт > tests\empty.ini
    "%JAVA_BIN%\java.exe" -cp out Main virtual_fs.zip tests\empty.ini
) else if "%choice%"=="2" (
    echo.
    echo [Система] Запуск прогона стартового скрипта...
    "%JAVA_BIN%\java.exe" -cp out Main virtual_fs.zip tests\tests.ini
) else (
    echo [ОШИБКА] Неверный выбор! Завершение работы.
)

pause

