@echo off
title "flyingsocks Server"

if "%1%" == "-install" (
    md C:\ProgramData\flyingsocks-server
    md C:\ProgramData\flyingsocks-server\log
    exit
)

echo "Run flyingsocks-server..."
if "%1%" == "-daemon" (
    start /b java -server -Xbootclasspath/a:../conf;../ -jar ../lib/flyingsocks-server-1.1.jar
) else (
    java -server -Xbootclasspath/a:../conf;../ -jar ../lib/flyingsocks-server-1.1.jar
)
echo "Complete."
pause