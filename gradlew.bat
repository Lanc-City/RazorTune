@rem Copyright 2015-2022 the original author or authors.
@if "%DEBUG%"=="" @echo off
set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
java -jar "%DIRNAME%\gradle\wrapper\gradle-wrapper.jar" %*
