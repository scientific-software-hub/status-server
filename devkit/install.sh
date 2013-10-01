#!/bin/bash

MAVEN_HOME=$M2_HOME

export TINE_VER=4.3.9
echo TINE_VER=$TINE_VER

export JTANGO_VER=1.0.2-alpha-all
echo JTANGO_VER=$JTANGO_VER

function _install {
export ARTIFACT=$1
export VERSION=$2
export INSTALL_DIR=/tmp/$ARTIFACT
echo Extracting $ARTIFACT-$VERSION-bundle.zip into $INSTALL_DIR
unzip $ARTIFACT-$VERSION-bundle.zip -d $INSTALL_DIR
$MAVEN_HOME/bin/mvn install:install-file -Dfile=$INSTALL_DIR/$ARTIFACT-$VERSION.jar -DgroupId=hzg.wpn -DartifactId=$ARTIFACT -Dversion=$VERSION -Dpackaging=jar -DpomFile=$INSTALL_DIR/pom.xml -Dsources=$INSTALL_DIR/$ARTIFACT-$VERSION-sources.jar -Djavadoc=$INSTALL_DIR/$ARTIFACT-$VERSION-javadoc.jar
echo Removing $INSTALL_DIR
rm -r $INSTALL_DIR
}

$MAVEN_HOME/bin/mvn install:install-file -Dfile=tineJAVA-$TINE_VER.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=4.3.9 -Dpackaging=jar -DgeneratePom=true

$MAVEN_HOME/bin/mvn install:install-file -Dfile=JTangoServer-$JTANGO_VER.jar -DgroupId=org.tango -DartifactId=JTangoServer -Dversion=1.0.2-alpha-all -Dpackaging=jar -DgeneratePom=true -Dpackaging=jar

_install "utils" "0.0.1"

_install "TangoAPI" "1.0.4"

