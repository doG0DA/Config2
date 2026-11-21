d2 --version@echo off
rem Prints the D2 text of the graph for every d2 example (stage 5, test repository)
cd /d "%~dp0.."
for %%f in (examples\d2_*.ini) do (
    echo ^> %%f
    call "%~dp0..\run.bat" %%f
    echo.
)
