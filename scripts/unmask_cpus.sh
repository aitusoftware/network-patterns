#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"

if [[ -d "/cpusets/$USER_CPUSET" ]]; then
    sudo cset set --destroy $USER_CPUSET
fi

if [[ -d "/cpusets/system" ]]; then
    sudo cset set --destroy system
fi