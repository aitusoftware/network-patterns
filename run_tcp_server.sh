#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
LISTEN_ADDRESS="0.0.0.0"
LISTEN_PORT="9876"
LOG_DIR="$LABEL"
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR
echo $LIB

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.bindAddress="$LISTEN_ADDRESS" com.aitusoftware.network.patterns.app.RunAllServers TCP 2>&1 | tee tcp-server-out.log

cd ..
