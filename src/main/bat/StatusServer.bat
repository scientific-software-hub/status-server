@echo off

rem set TANGO_HOST=hzgharwi3:10000
echo Using TANGO_HOST=%TANGO_HOST%
echo Using JAVA_HOME=%JAVA_HOME%

set INSTANCE_NAME="development"
echo Using INSTANCE_NAME=%INSTANCE_NAME%

set SS_HOME=%cd%
echo Using SS_HOME=%SS_HOME%

set CONFIGURATION=\conf
echo CONFIG_ROOT=%SS_HOME%\..%CONFIGURATION%

%JAVA_HOME%\bin\java -server -Xmx1G -cp "%SS_HOME%\..\lib\*" -Dss.home="%SS_HOME%\.." wpn.hdri.ss.Launcher --instance %INSTANCE_NAME% --config %SS_HOME%\..%CONFIGURATION%
