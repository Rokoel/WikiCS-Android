@echo off
setlocal
cd /d "%~dp0.."
if not exist test-output\classes mkdir test-output\classes
type nul > test-output\sources.txt
for %%F in (app\src\main\java\site\wikics\reader\core\*.java) do echo "%%F">>test-output\sources.txt
echo "tests\CoreTests.java">>test-output\sources.txt
java com.sun.tools.javac.Main -encoding UTF-8 -d test-output\classes @test-output\sources.txt
if errorlevel 1 exit /b %ERRORLEVEL%
java -cp test-output\classes CoreTests
