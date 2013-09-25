#!/bin/bash

mvn install:install-file -Dfile=tineJAVA-4.3.9.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=4.3.9 -Dpackaging=jar -DgeneratePom=true

mvn install:install-file -Dfile=JTangoServer-1.0.1-alpha.jar -DgroupId=org.tango -DartifactId=JTangoServer -Dversion=1.0.1-alpha -Dpackaging=jar -DpomFile=JTangoServer-1.0.1-alpha.pom -Dsources=JTangoServer-1.0.1-alpha-sources.jar -Djavadoc=JTangoServer-1.0.1-alpha-javadoc.jar

mvn install:install-file -Dfile=JTangoClientLang-1.0.3-SNAPSHOT.jar -DgroupId=org.tango -DartifactId=JTangoClientLang -Dversion=1.0.3-SNAPSHOT -Dpackaging=jar -DpomFile=JTangoClientLang-1.0.3-SNAPSHOT.pom

mvn install:install-file -Dfile=JTangoCommons-1.0.0.jar -DgroupId=org.tine -DartifactId=JTangoCommons -Dversion=1.0.0 -Dpackaging=jar -DpomFile=JTangoCommons-1.0.0.pom

mvn install:install-file -Dfile=TangORB-8.1.3.jar -DgroupId=org.tango -DartifactId=TangORB -Dversion=8.1.3 -Dpackaging=jar -DpomFile=TangORB-8.1.3.pom -Dsources=TangORB-8.1.3-sources.jar

mvn install:install-file -Dfile=TangORB-javacommon-8.1.3.jar -DgroupId=org.tango -DartifactId=TangORB-javacommon -Dversion=8.1.3 -Dpackaging=jar -DpomFile=TangORB-javacommon-8.1.3.pom -Dsources=TangORB-javacommon-8.1.3-sources.jar

mvn install:install-file -Dfile=JavaTangoIDL-4.0.1.jar -DgroupId=org.tango -DartifactId=JavaTangoIDL -Dversion=4.0.1 -Dpackaging=jar -DpomFile=JavaTangoIDL-4.0.1.pom -Dsources=JavaTangoIDL-4.0.1-sources.jar

mvn install:install-file -Dfile=JavaAPI-8.1.3.pom -DgroupId=org.tango -DartifactId=JavaAPI -Dversion=8.1.2 -Dpackaging=pom -DpomFile=JavaAPI-8.1.3.pom

mvn install:install-file -Dfile=utils-0.0.1.jar -DgroupId=hzg.wpn -DartifactId=utils -Dversion=0.0.1 -Dpackaging=jar -DpomFile=utils-0.0.1.pom -Dsources=utils-0.0.1-sources.jar -Djavadoc=utils-0.0.1-javadoc.jar

mvn install:install-file -Dfile=TangoAPI-1.0.2.jar -DgroupId=hzg.wpn -DartifactId=TangoAPI -Dversion=1.0.2 -Dpackaging=jar -DpomFile=TangoAPI-1.0.2.pom -Dsources=TangoAPI-1.0.2-sources.jar -Djavadoc=TangoAPI-1.0.2-javadoc.jar
