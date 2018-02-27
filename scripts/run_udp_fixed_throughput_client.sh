#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"

mkdir -p $LOG_DIR
cd $LOG_DIR

JAVA_ARGS="$JAVA_ARGS -Dnetwork-patterns.fixedThroughput=true -Dnetwork-patterns.serverListenPort=$SERVER_UDP_PORT -Dnetwork-patterns.affinity.clientOutbound=$CLIENT_OUT_CPU -Dnetwork-patterns.affinity.clientInbound=$CLIENT_IN_CPU -Dnetwork-patterns.clientListenPort=$CLIENT_UDP_PORT"

$CMD_PREFIX taskset -c $POOL_CPUS  java $JAVA_ARGS com.aitusoftware.network.patterns.app.RunAllClients UDP 2>&1 | tee udp-client-out.log

cd ..
