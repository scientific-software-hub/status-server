@echo off

set TINE_VER=4.5.4
echo TINE_VER=%TINE_VER%

call mvn install:install-file -Dfile=tineJAVA-%TINE_VER%.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=%TINE_VER% -Dpackaging=jar -DgeneratePom=true