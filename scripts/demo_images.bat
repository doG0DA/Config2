@echo off
rem Draws and opens a picture for every image example (stage 5, needs d2 in PATH)
cd /d "%~dp0.."
for %%f in (examples\image_*.ini) do (
    echo ^> %%f
    call "%~dp0..\run.bat" %%f
    echo.
)
