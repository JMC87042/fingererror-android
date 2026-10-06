@echo off
chcp 65001 >nul
cd /d "%~dp0"
setlocal

rem --- git 찾기 (PATH 에 없으면 기본 설치 위치에서) ---
set "GIT=git"
where git >nul 2>nul
if not errorlevel 1 goto gitok
set "GIT=C:\Program Files\Git\cmd\git.exe"
if exist "%GIT%" goto gitok
set "GIT=%LOCALAPPDATA%\Programs\Git\cmd\git.exe"
if exist "%GIT%" goto gitok
echo [!] Git 을 찾지 못했어요. 설치 후 PC 를 재시작하거나 cmd 창을 새로 열어 다시 실행하세요.
pause
exit /b 1

:gitok
if exist ".git" goto hasrepo
echo 처음 한 번만 설정해요.
"%GIT%" init -b main >nul
"%GIT%" config user.name "JMC"
"%GIT%" config user.email "mixbyjmc@gmail.com"

:hasrepo
"%GIT%" remote get-url origin >nul 2>nul
if not errorlevel 1 goto hasremote
"%GIT%" remote add origin https://github.com/JMC87042/fingererror-android.git

:hasremote
"%GIT%" add -A
"%GIT%" commit -m "update %date% %time%" >nul 2>nul

echo.
echo GitHub 로 올리는 중... (처음엔 로그인 창이 뜰 수 있어요)
"%GIT%" push -u -f origin main
if errorlevel 1 goto fail

echo.
echo 완료! 빌드 진행 상황 페이지를 열어요.
start "" "https://github.com/JMC87042/fingererror-android/actions"
pause
exit /b 0

:fail
echo.
echo [!] 올리기 실패. 위 빨간 메시지를 캡처해서 보내주세요.
pause
exit /b 1
