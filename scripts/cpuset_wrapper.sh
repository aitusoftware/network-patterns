#!/bin/bash

chmod +x $1

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"

cset proc --exec $USER_CPUSET $1