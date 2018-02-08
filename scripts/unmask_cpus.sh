#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"

sudo cset set --destroy $USER_CPUSET
sudo cset set --destroy system