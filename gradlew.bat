@echo off
setlocal

set ANDROID_HOME=C:\android-sdk
set DIRNAME=%~dp0
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

REM Use java from PATH instead of JAVA_HOME
java -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto :run

REM If not in PATH, try common locations
for %%j in (
    "C:\Program Files\Java\jdk-25.0.2\bin\java.exe"
    "C:\Program Files\Java\jdk-21\bin\java.exe"
    "C:\Program Files\Java\jdk-17\bin\java.exe"
) do (
    if exist %%j (
        set "JAVA_CMD=%%~j"
        goto :run_with_cmd
    )
)

echo ERROR: Java not found.
pause
exit /b 1

:run_with_cmd
"%JAVA_CMD%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
if %ERRORLEVEL% equ 0 goto :end
pause
exit /b %ERRORLEVEL%

:run
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
if %ERRORLEVEL% equ 0 goto :end
pause
exit /b %ERRORLEVEL%

:end
endlocal
