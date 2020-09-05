@echo off

Rem set GRAALVM_HOME=C:\Users\user\Desktop\graalvm-ce-java11-20.2.0

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)
set JAVA_HOME=%GRAALVM_HOME%
set PATH=%GRAALVM_HOME%\bin;%PATH%

call lein with-profiles +clojure-1.10.2-alpha1,+native-image do clean, uberjar
if %errorlevel% neq 0 exit /b %errorlevel%

call %GRAALVM_HOME%\bin\gu.cmd install native-image

Rem the --no-server option is not supported in GraalVM Windows.
call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "target/alc.x-as-tests-0.0.1-SNAPSHOT-standalone.jar" ^
  "-H:Name=alc.xat" ^
  "-H:+ReportExceptionStackTraces" ^
  "-J-Dclojure.spec.skip-macros=true" ^
  "-J-Dclojure.compiler.direct-linking=true" ^
  "-H:ReflectionConfigurationFiles=reflection.json" ^
  "--initialize-at-build-time"  ^
  "-H:Log=registerResource:" ^
  "--no-fallback" ^
  "--verbose" ^
  "-J-Xmx5g"

if %errorlevel% neq 0 exit /b %errorlevel%
