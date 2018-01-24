#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
RUN=$(date "+%H%M%S")
LABEL="RUN_$RUN"
LIB="$(readlink -f $SCRIPT_DIR/network-patterns-all.jar)"
CLIENT_OUTBOUND_CPU="1"
CLIENT_INBOUND_CPU="2"
SERVER_OUTBOUND_CPU="3"
SERVER_INBOUND_CPU="4"
CMD_PREFIX=""
LOG_DIR="$LABEL"
WARMUP_MESSAGES="500000"
RUNTIME_MINUTES="2"
SERVER_LISTEN_ADDRESS="127.0.0.1"
CLIENT_LISTEN_ADDRESS="127.0.0.1"
SERVER_TCP_PORT="7786"
CLIENT_TCP_PORT="7788"
SERVER_UDP_PORT="15678"
CLIENT_UDP_PORT="15680"


JAVA_ARGS="-cp $LIB -Dnetwork-patterns.warmupMessages=$WARMUP_MESSAGES \
-Djava.net.preferIPv4Stack=true \
-Dnetwork-patterns.runtimeMinutes=$RUNTIME_MINUTES \
-Dnetwork-patterns.bindAddress=$SERVER_LISTEN_ADDRESS \
-Dnetwork-patterns.clientBindAddress=$CLIENT_LISTEN_ADDRESS -XX:-TieredCompilation"
