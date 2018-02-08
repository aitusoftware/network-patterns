#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"
mkdir -p $LOG_DIR
cd $LOG_DIR

JAVA_ARGS="$JAVA_ARGS -Dnetwork-patterns.affinity.clientOutbound=$CLIENT_OUT_CPU -Dnetwork-patterns.affinity.clientInbound=$CLIENT_IN_CPU -Dnetwork-patterns.serverListenPort=$SERVER_TCP_PORT -Dnetwork-patterns.clientListenPort=$CLIENT_TCP_PORT"

$CMD_PREFIX taskset -c $POOL_CPUS java $JAVA_ARGS com.aitusoftware.network.patterns.app.RunAllClients TCP 2>&1 | tee tcp-client-out.log

cd ..
