#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
LISTEN_ADDRESS="192.168.0.8"
LISTEN_PORT="15678"
CLIENT_LISTEN_PORT="15680"
LOG_DIR="$LABEL"
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.clientListenPort="$CLIENT_LISTEN_PORT" -Djava.net.preferIPv4Stack=true -Dnetwork-patterns.bindAddress="$LISTEN_ADDRESS" -Dnetwork-patterns.serverListenPort="$LISTEN_PORT" com.aitusoftware.network.patterns.app.RunAllServers UDP 2>&1 | tee udp-server-out.log

cd ..