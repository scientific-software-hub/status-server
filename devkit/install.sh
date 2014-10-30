#!/bin/bash

echo M2_HOME=$M2_HOME

TINE_VER=4.5.0
echo TINE_VER=$TINE_VER

function _install {
export ARTIFACT=$1
export VERSION=$2
export INSTALL_DIR=/tmp/$ARTIFACT
echo Extracting $ARTIFACT-$VERSION-bundle.zip into $INSTALL_DIR
unzip $ARTIFACT-$VERSION-bundle.zip -d $INSTALL_DIR
$M2_HOME/bin/mvn install:install-file -Dfile=$INSTALL_DIR/$ARTIFACT-$VERSION.jar -DgroupId=hzg.wpn -DartifactId=$ARTIFACT -Dversion=$VERSION -Dpackaging=jar -DpomFile=$INSTALL_DIR/pom.xml -Dsources=$INSTALL_DIR/$ARTIFACT-$VERSION-sources.jar -Djavadoc=$INSTALL_DIR/$ARTIFACT-$VERSION-javadoc.jar
echo Removing $INSTALL_DIR
rm -r $INSTALL_DIR
}

$M2_HOME/bin/mvn install:install-file -Dfile=tineJAVA-$TINE_VER.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=$TINE_VER -Dpackaging=jar -DgeneratePom=true

_install "utils" "0.0.1"
