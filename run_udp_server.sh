#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
LISTEN_ADDRESS="192.168.0.8"
LISTEN_PORT="15678"
CLIENT_LISTEN_PORT="15680"
LOG_DIR="$LABEL"
SERVER_OUTBOUND_CPU=1
SERVER_INBOUND_CPU=2
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.affinity.serverOutbound=$SERVER_OUTBOUND_CPU -Dnetwork-patterns.affinity.serverInbound=$SERVER_INBOUND_CPU -Dnetwork-patterns.clientListenPort="$CLIENT_LISTEN_PORT" -Djava.net.preferIPv4Stack=true -Dnetwork-patterns.bindAddress="$LISTEN_ADDRESS" -Dnetwork-patterns.serverListenPort="$LISTEN_PORT" com.aitusoftware.network.patterns.app.RunAllServers UDP 2>&1 | tee udp-server-out.log

cd ..