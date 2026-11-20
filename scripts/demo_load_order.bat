@echo off
rem Runs the app on every load-order example (stage 4, test repository)
cd /d "%~dp0.."
for %%f in (examples\order_*.ini) do (
    echo ^> %%f
    call "%~dp0..\run.bat" %%f
    echo.
)
