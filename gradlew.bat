@echo off
setlocal
set APP_HOME=%~dp0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper-main-9.6.0.jar;%APP_HOME%gradle\wrapper\gradle-wrapper-shared-9.6.0.jar
java -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
