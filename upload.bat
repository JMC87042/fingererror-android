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
set "GIT=C:\Program Files (x86)\Git\cmd\git.exe"
if exist "%GIT%" goto gitok
rem GitHub Desktop 에 들어 있는 git
for /d %%D in ("%LOCALAPPDATA%\GitHubDesktop\app-*") do if exist "%%D\resources\app\git\cmd\git.exe" set "GIT=%%D\resources\app\git\cmd\git.exe"
if exist "%GIT%" goto gitok

rem --- 없으면 자동 설치 (윈도우 기본 winget) ---
echo Git 이 없어서 자동으로 설치할게요. 1~2분 걸려요...
where winget >nul 2>nul
if errorlevel 1 goto nogit
winget install --id Git.Git -e --silent --accept-package-agreements --accept-source-agreements
set "GIT=C:\Program Files\Git\cmd\git.exe"
if exist "%GIT%" goto gitok
set "GIT=%LOCALAPPDATA%\Programs\Git\cmd\git.exe"
if exist "%GIT%" goto gitok

:nogit
echo [!] Git 을 찾지 못했어요.
echo     https://git-scm.com/download/win 에서 설치(전부 '다음')한 뒤 이 파일을 다시 실행하세요.
start "" "https://git-scm.com/download/win"
pause
exit /b 1

:gitok
rem USB·외장 드라이브(exFAT 등)는 소유자 기록이 없어 git 이 거부함 → 허용
"%GIT%" config --global --get-all safe.directory 2>nul | findstr /x /c:"*" >nul
if errorlevel 1 "%GIT%" config --global --add safe.directory "*"

if exist ".git\HEAD" goto hasrepo
echo 처음 한 번만 설정해요.
if exist ".git" rmdir /s /q ".git"
"%GIT%" init -b main >nul

:hasrepo
"%GIT%" config user.name "JMC"
"%GIT%" config user.email "mixbyjmc@gmail.com"
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
