#!/bin/bash
set -euxo pipefail

# not needed on a system that already has cpusets
export OVERRIDE_IRQ_CPUS=1,2
export OVERRIDE_RESTRICTED_CPUS=2,3,4,5,6,7
export OVERRIDE_SYSTEM_CPUS=0,1

# name of cpuset available to this user (e.g /cpusets/user_cpuset)
export OVERRIDE_USER_CPUSET="user_cpuset"
# 4 isolated CPUs that can be used by the application
export OVERRIDE_CLIENT_IN_CPU=4
export OVERRIDE_CLIENT_OUT_CPU=5
export OVERRIDE_SERVER_IN_CPU=6
export OVERRIDE_SERVER_OUT_CPU=7
# pool CPUs that will be used for running taskset on the application
export OVERRIDE_POOL_CPUS=2,3

# the address of the machine designated as the server
export OVERRIDE_SERVER_LISTEN_ADDRESS="127.0.0.1"
# the address of the machine designated as the client
export OVERRIDE_CLIENT_LISTEN_ADDRESS="127.0.0.1"


# uncomment below depending on this machine's role
# TCP test
#bash ./cpuset_wrapper.sh ./run_tcp_server.sh
#bash ./cpuset_wrapper.sh ./run_tcp_client.sh

# UDP test
#bash ./cpuset_wrapper.sh ./run_udp_server.sh
#bash ./cpuset_wrapper.sh ./run_udp_client.sh
