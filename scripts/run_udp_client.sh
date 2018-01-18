#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"

mkdir -p $LOG_DIR
cd $LOG_DIR

JAVA_ARGS="$JAVA_ARGS -Dnetwork-patterns.serverListenPort=$SERVER_UDP_PORT -Dnetwork-patterns.clientListenPort=$CLIENT_UDP_PORT"

$CMD_PREFIX java $JAVA_ARGS com.aitusoftware.network.patterns.app.RunAllClients UDP 2>&1 | tee udp-client-out.log

cd ..
