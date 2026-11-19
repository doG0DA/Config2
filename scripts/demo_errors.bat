@echo off
rem Runs the app on every example config with errors
cd /d "%~dp0.."
for %%f in (examples\errors\*.ini nope.ini) do (
    echo ^> %%f
    call "%~dp0..\run.bat" %%f
    echo.
)
