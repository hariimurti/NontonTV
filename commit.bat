@echo off
setlocal enabledelayedexpansion
set targetFile=NontonTV.json
set untrackedFile=no
for /f "tokens=*" %%i in ('git status %targetFile%') do (
    set temp=%%~ni
    echo.!temp:~0,80!|findstr /C:"Untracked" >nul 2>&1
    if not errorlevel 1 (
       set untrackedFile=yes
    )
)
if "%untrackedFile%" == "no" (
    echo tidak ada perubahan pada file %targetFile%
    timeout 10
    exit
)

for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"

git add %targetFile%
git commit -m "UPDATE: %YYYY%/%MM%/%DD% %HH%:%Min%"

timeout 10