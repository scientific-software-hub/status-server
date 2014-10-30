@echo off

set TINE_VER=4.5.0
echo TINE_VER=%TINE_VER%

call mvn install:install-file -Dfile=tineJAVA-%TINE_VER%.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=%TINE_VER% -Dpackaging=jar -DgeneratePom=true

call :install "utils" "0.0.1"

GOTO:EOF

:install
set ARTIFACT=%~1
set VERSION=%~2
set INSTALL_DIR=%TMP%\%ARTIFACT%
echo Extracting %ARTIFACT%-%VERSION%-bundle.zip into %INSTALL_DIR%
call unzip %ARTIFACT%-%VERSION%-bundle.zip -d %INSTALL_DIR%
call mvn install:install-file -Dfile=%INSTALL_DIR%\%ARTIFACT%-%VERSION%.jar -DgroupId=hzg.wpn -DartifactId=%ARTIFACT% -Dversion=%VERSION% -Dpackaging=jar -DpomFile=%INSTALL_DIR%\pom.xml -Dsources=%INSTALL_DIR%\%ARTIFACT%-%VERSION%-sources.jar -Djavadoc=%INSTALL_DIR%\%ARTIFACT%-%VERSION%-javadoc.jar
echo Removing %INSTALL_DIR%
call rm -r %INSTALL_DIR%
GOTO:EOF
