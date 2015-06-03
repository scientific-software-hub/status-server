#!/bin/bash

#export TANGO_HOST="hzgharwi3:10000"
XENV_ROOT=..
echo "Using XENV_ROOT=$XENV_ROOT"
echo "Using TANGO_HOST=$TANGO_HOST"
echo "Using JAVA_HOME=$JAVA_HOME"

export INSTANCE_NAME="development"
echo "Using INSTANCE_NAME=$INSTANCE_NAME"

export TINE_HOME=/home/p07user/tine/database
echo "Using TINE_HOME=$TINE_HOME"

#-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005
JAVA_OPTS="-server -Xmx1G -Xshare:off -XX:+UseG1GC"
echo "Using JAVA_OPTS=$JAVA_OPTS"

$JAVA_HOME/bin/java $JAVA_OPTS -cp "$XENV_ROOT/lib/share/*:$XENV_ROOT/lib/StatusServer/*" wpn.hdri.ss.Launcher --instance $INSTANCE_NAME --config $XENV_ROOT/etc/StatusServer/conf