@echo off
rem Runs the app on every test-repository example (stage 3)
cd /d "%~dp0.."
for %%f in (examples\test_*.ini) do (
    echo ^> %%f
    call "%~dp0..\run.bat" %%f
    echo.
)
