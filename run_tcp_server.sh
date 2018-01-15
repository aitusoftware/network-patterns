#!/bin/bash

RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(pwd)/network-patterns-all.jar"
LISTEN_ADDRESS="192.168.0.7"
LISTEN_PORT="9876"
LOG_DIR="$LABEL"
SERVER_OUTBOUND_CPU=1
SERVER_INBOUND_CPU=2
# onload
CMD_PREFIX=""
mkdir -p $LOG_DIR
cd $LOG_DIR
echo $LIB

$CMD_PREFIX java -cp $LIB -Dnetwork-patterns.affinity.serverOutbound=$SERVER_OUTBOUND_CPU -Dnetwork-patterns.affinity.serverInbound=$SERVER_INBOUND_CPU  -Djava.net.preferIPv4Stack=true -Dnetwork-patterns.bindAddress="$LISTEN_ADDRESS" -Dnetwork-patterns.serverListenPort="$LISTEN_PORT" com.aitusoftware.network.patterns.app.RunAllServers TCP 2>&1 | tee tcp-server-out.log

cd ..
