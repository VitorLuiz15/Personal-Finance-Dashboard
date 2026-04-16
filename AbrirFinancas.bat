@echo off
chcp 65001 >nul
title Finance Manager - Build ^& Run

:: ============================================================
::  Finance Manager - Compile & Run Script
::  Place this .bat file in the SAME folder as your .java files
:: ============================================================

echo.
echo  ================================================
echo   Finance Manager - Build ^& Launch
echo  ================================================
echo.

:: ── Step 1: Move to the folder where this .bat file lives ────────────────────
cd /d "%~dp0"
echo  [1/4] Working directory set to:
echo        %CD%
echo.

:: ── Step 2: Verify all required source files exist ───────────────────────────
echo  [2/4] Checking source files...

set MISSING=0
for %%F in (TransactionEntry.java FinanceFileHandler.java FinanceWindow.java) do (
    if not exist "%%F" (
        echo        [MISSING] %%F
        set MISSING=1
    ) else (
        echo        [  OK   ] %%F
    )
)

if "%MISSING%"=="1" (
    echo.
    echo  ERROR: One or more source files are missing.
    echo  Make sure all 3 .java files are in the same folder as this .bat:
    echo    - TransactionEntry.java
    echo    - FinanceFileHandler.java
    echo    - FinanceWindow.java
    echo.
    pause
    exit /b 1
)
echo.

:: ── Step 3: Detect Java compiler ─────────────────────────────────────────────
echo  [3/4] Locating Java compiler (javac)...

where javac >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo.
    echo  ERROR: javac not found on PATH.
    echo.
    echo  Please install the Java JDK (version 17 or newer^) from:
    echo    https://adoptium.net   or   https://www.oracle.com/java/
    echo.
    echo  After installing, re-open this window so PATH is updated.
    echo.
    pause
    exit /b 1
)

for /f "tokens=*" %%V in ('javac -version 2^>^&1') do (
    echo        Found: %%V
)
echo.

:: ── Step 4: Compile ───────────────────────────────────────────────────────────
echo  [4/4] Compiling...
echo.

javac -encoding UTF-8 TransactionEntry.java FinanceFileHandler.java FinanceWindow.java

if %ERRORLEVEL% neq 0 (
    echo.
    echo  ================================================
    echo   COMPILE FAILED - see errors above
    echo  ================================================
    echo.
    echo  Common causes:
    echo    1. You have an OLD version of one of the .java files.
    echo       Re-download all 3 files from the chat and replace them here.
    echo    2. A file is open and locked by another program.
    echo       Close any text editor that has the files open.
    echo.
    pause
    exit /b 1
)

echo.
echo  ================================================
echo   Compile successful! Launching application...
echo  ================================================
echo.

:: ── Launch (keep console open to see any runtime errors) ─────────────────────
java -Dfile.encoding=UTF-8 FinanceWindow

if %ERRORLEVEL% neq 0 (
    echo.
    echo  Application exited with an error (code %ERRORLEVEL%^).
    echo  Check the output above for details.
    echo.
    pause
)