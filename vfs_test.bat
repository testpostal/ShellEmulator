@echo off
chcp 65001 >nul
if not exist out mkdir out

set "JAVA_BIN=D:\programms\IntelliJ IDEA 2026.2.3\JDK\bin"

echo Компиляция эмулятора...
"%JAVA_BIN%\javac.exe" src/Main.java -d out
if %errorlevel% neq 0 ( pause & exit /b )

echo.
echo ========================================================
echo       ТЕСТИРОВАНИЕ РАЗЛИЧНЫХ ВАРИАНТОВ VFS
echo ========================================================

echo.
echo [ТЕСТ 1] Вариант VFS: Минимальный (vfs_minimal.zip)...
"%JAVA_BIN%\java.exe" -cp out Main vfs_minimal.zip tests\tests.ini
echo --------------------------------------------------------

echo.
echo [ТЕСТ 2] Вариант VFS: Несколько файлов (vfs_normal.zip)...
"%JAVA_BIN%\java.exe" -cp out Main vfs_normal.zip tests\tests.ini
echo --------------------------------------------------------

echo.
echo [ТЕСТ 3] Вариант VFS: Не менее 3 уровней вложенности (vfs_large.zip)...
"%JAVA_BIN%\java.exe" -cp out Main vfs_large.zip tests\tests.ini
echo --------------------------------------------------------

echo Все тесты вариантов VFS выполнены.
pause
