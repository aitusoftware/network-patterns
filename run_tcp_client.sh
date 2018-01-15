#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
CONNECT_ADDRESS="192.168.0.7"
SERVER_LISTEN_PORT="9876"
CLIENT_LISTEN_PORT="9878"
CLIENT_OUTBOUND_CPU=1
CLIENT_INBOUND_CPU=2
LOG_DIR="$LABEL"
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.affinity.clientOutbound=$CLIENT_OUTBOUND_CPU -Dnetwork-patterns.affinity.clientInbound=$CLIENT_INBOUND_CPU -Djava.net.preferIPv4Stack=true -Dnetwork-patterns.clientListenPort="$CLIENT_LISTEN_PORT" -Dnetwork-patterns.connectAddress="$CONNECT_ADDRESS" -Dnetwork-patterns.serverListenPort="$SERVER_LISTEN_PORT" com.aitusoftware.network.patterns.app.RunAllClients TCP 2>&1 | tee tcp-client-out.log

cd ..
