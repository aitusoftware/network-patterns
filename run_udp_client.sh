#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
CONNECT_ADDRESS="192.168.0.8"
SERVER_LISTEN_PORT="15678"
CLIENT_LISTEN_PORT="15680"
CLIENT_LISTEN_ADDRESS="0.0.0.0"
LOG_DIR="$LABEL"
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.bindAddress="$CLIENT_LISTEN_ADDRESS" -Djava.net.preferIPv4Stack=true -Dnetwork-patterns.clientListenPort="$CLIENT_LISTEN_PORT" -Dnetwork-patterns.connectAddress="$CONNECT_ADDRESS" -Dnetwork-patterns.serverListenPort="$SERVER_LISTEN_PORT" com.aitusoftware.network.patterns.app.RunAllClients UDP 2>&1 | tee udp-client-out.log

cd ..
