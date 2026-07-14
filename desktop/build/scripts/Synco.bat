@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Synco startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables, and ensure extensions are enabled
setlocal EnableExtensions

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and SYNCO_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

"%COMSPEC%" /c exit 1

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\desktop-1.0.0.jar;%APP_HOME%\lib\okhttp-4.10.0.jar;%APP_HOME%\lib\kotlinx-serialization-core-jvm-1.6.3.jar;%APP_HOME%\lib\kotlinx-serialization-json-jvm-1.6.3.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.10.2.jar;%APP_HOME%\lib\javalin-5.6.3.jar;%APP_HOME%\lib\okio-jvm-3.0.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.8.21.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.8.21.jar;%APP_HOME%\lib\kotlin-stdlib-2.2.10.jar;%APP_HOME%\lib\bcprov-jdk18on-1.78.1.jar;%APP_HOME%\lib\Java-WebSocket-1.5.4.jar;%APP_HOME%\lib\slf4j-simple-2.0.7.jar;%APP_HOME%\lib\annotations-24.0.1.jar;%APP_HOME%\lib\websocket-jetty-server-11.0.17.jar;%APP_HOME%\lib\jetty-webapp-11.0.17.jar;%APP_HOME%\lib\websocket-servlet-11.0.17.jar;%APP_HOME%\lib\jetty-servlet-11.0.17.jar;%APP_HOME%\lib\jetty-security-11.0.17.jar;%APP_HOME%\lib\websocket-core-server-11.0.17.jar;%APP_HOME%\lib\jetty-server-11.0.17.jar;%APP_HOME%\lib\websocket-jetty-common-11.0.17.jar;%APP_HOME%\lib\websocket-core-common-11.0.17.jar;%APP_HOME%\lib\jetty-http-11.0.17.jar;%APP_HOME%\lib\jetty-io-11.0.17.jar;%APP_HOME%\lib\jetty-xml-11.0.17.jar;%APP_HOME%\lib\jetty-util-11.0.17.jar;%APP_HOME%\lib\slf4j-api-2.0.7.jar;%APP_HOME%\lib\websocket-jetty-api-11.0.17.jar;%APP_HOME%\lib\jetty-jakarta-servlet-api-5.0.2.jar


@rem Execute Synco
@rem endlocal doesn't take effect until after the line is parsed and variables are expanded
@rem which allows us to clear the local environment before executing the java command
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %SYNCO_OPTS%  -classpath "%CLASSPATH%" com.remoteaudiosync.desktop.MainKt %* & call :exitWithErrorLevel

:exitWithErrorLevel
@rem Use "%COMSPEC%" /c exit to allow operators to work properly in scripts
"%COMSPEC%" /c exit %ERRORLEVEL%
