#!/bin/bash

echo M2_HOME=$M2_HOME

TINE_VER=4.5.4
echo TINE_VER=$TINE_VER

$M2_HOME/bin/mvn install:install-file -Dfile=tineJAVA-$TINE_VER.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=$TINE_VER -Dpackaging=jar -DgeneratePom=true
