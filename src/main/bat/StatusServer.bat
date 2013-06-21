@echo off

set TANGO_HOST=hzgharwi3:10000
echo Using TANGO_HOST=%TANGO_HOST%

set SS_HOME=%cd%
echo Using SS_HOME=%SS_HOME%

set CONFIGURATION=\conf\StatusServer.configuration.xml
echo Using configuration file %CONFIGURATION%

java -server -Xmx1G -cp "%SS_HOME%\..\lib\*" -Dss.home="%SS_HOME%\.." wpn.hdri.ss.Launcher --config %SS_HOME%\..%CONFIGURATION%
