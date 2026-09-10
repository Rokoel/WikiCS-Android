@echo off
setlocal
cd /d "%~dp0"
if defined JAVA_HOME (
  set "WIKICS_JAVA=%JAVA_HOME%\bin\java.exe"
) else (
  set "WIKICS_JAVA=java"
)
if exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
  "%WIKICS_JAVA%" -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
) else (
  "%WIKICS_JAVA%" "%~dp0tools\GradleBootstrap.java" %*
)
exit /b %ERRORLEVEL%
