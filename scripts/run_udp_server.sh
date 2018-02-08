#!/bin/bash


SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"

mkdir -p $LOG_DIR
cd $LOG_DIR

JAVA_ARGS="$JAVA_ARGS -Dnetwork-patterns.serverListenPort=$SERVER_UDP_PORT -Dnetwork-patterns.clientListenPort=$CLIENT_UDP_PORT -Dnetwork-patterns.affinity.serverOutbound=$SERVER_OUT_CPU -Dnetwork-patterns.affinity.serverInbound=$SERVER_IN_CPU"

$CMD_PREFIX taskset -c $POOL_CPUS  java $JAVA_ARGS com.aitusoftware.network.patterns.app.RunAllServers UDP 2>&1 | tee udp-server-out.log

cd ..