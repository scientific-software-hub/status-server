@echo off

call mvn install:install-file -Dfile=tineJAVA-4.3.9.jar -DgroupId=de.desy.tine -DartifactId=tineJAVA -Dversion=4.3.9 -Dpackaging=jar -DgeneratePom=true

call mvn install:install-file -Dfile=JTangoServer-1.0.2-alpha-all.jar -DgroupId=org.tango -DartifactId=JTangoServer -Dversion=1.0.2-alpha-all -Dpackaging=jar -DgeneratePom=true -Dpackaging=jar

call mvn install:install-file -Dfile=utils-0.0.1.jar -DgroupId=hzg.wpn -DartifactId=utils -Dversion=0.0.1 -Dpackaging=jar -DpomFile=utils-0.0.1.pom -Dsources=utils-0.0.1-sources.jar -Djavadoc=utils-0.0.1-javadoc.jar

call mvn install:install-file -Dfile=TangoAPI-1.0.2.jar -DgroupId=hzg.wpn -DartifactId=TangoAPI -Dversion=1.0.2 -Dpackaging=jar -DpomFile=TangoAPI-1.0.2.pom -Dsources=TangoAPI-1.0.2-sources.jar -Djavadoc=TangoAPI-1.0.2-javadoc.jar