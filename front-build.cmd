@echo off
setlocal

set "ROOT=%~dp0"
set "GUIDE=%ROOT%src\main\resources\guide"

if not exist "%GUIDE%" (
  echo Guide directory not found: %GUIDE%
  exit /b 1
)

pushd "%GUIDE%"

if not exist "node_modules" (
  echo Installing dependencies...
  call npm install
  if errorlevel 1 goto :error
)

echo Building frontend...
call npm run build
if errorlevel 1 goto :error

popd
echo Done. Output: %ROOT%docs\index.html
pause
exit /b 0

:error
popd
echo Build failed.
pause
exit /b 1
