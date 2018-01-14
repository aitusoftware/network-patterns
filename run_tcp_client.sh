#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
CONNECT_ADDRESS="127.0.0.1"
LISTEN_PORT="9876"
LOG_DIR="$LABEL"
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.bindAddress="$LISTEN_ADDRESS" com.aitusoftware.network.patterns.app.RunAllClients TCP 2>&1 | tee tcp-client-out.log

cd ..