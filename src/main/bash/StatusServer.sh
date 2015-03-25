#!/bin/bash

#export TANGO_HOST="hzgharwi3:10000"
echo "Using TANGO_HOST=$TANGO_HOST"
echo "Using JAVA_HOME=$JAVA_HOME"

export INSTANCE_NAME="development"
echo "Using INSTANCE_NAME=$INSTANCE_NAME"

export SS_HOME=./..
echo "Using SS_HOME=$SS_HOME"

export TINE_HOME=/home/p07user/tine/database
echo "Using TINE_HOME=$TINE_HOME"

$JAVA_HOME/bin/java -server -Xmx1G -cp "$SS_HOME/lib/*" -Dss.home=$SS_HOME wpn.hdri.ss.Launcher --instance $INSTANCE_NAME --config $SS_HOME/conf