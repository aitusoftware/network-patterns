#!/bin/bash
set -euxo pipefail
sudo apt update
sudo apt install -y cpuset openjdk-8-jdk-headless git hwloc unzip

cd

if [[ -d "./network-patterns" ]]; then
    rm -rf ./network-patterns
fi



lstopo-no-graphics --of xml -v topo.xml

git clone https://github.com/aitusoftware/network-patterns.git

cd network-patterns

bash ./package.sh

cd

mkdir -p dist
cd dist
unzip -o ../network-patterns/dist.zip

export OVERRIDE_IRQ_CPUS=0,1,4,5
export OVERRIDE_RESTRICTED_CPUS=1,2,3,5,6,7
export OVERRIDE_SYSTEM_CPUS=0,4
export OVERRIDE_CLIENT_IN_CPU=2
export OVERRIDE_CLIENT_OUT_CPU=3
export OVERRIDE_SERVER_IN_CPU=6
export OVERRIDE_SERVER_OUT_CPU=7
export OVERRIDE_POOL_CPUS=1,5
export OVERRIDE_SERVER_LISTEN_ADDRESS=""
export OVERRIDE_CLIENT_LISTEN_ADDRESS=""

bash ./unmask_cpus.sh
bash ./mask_cpus.sh

#bash ./cpuset_wrapper.sh ./run_tcp_server.sh
#bash ./cpuset_wrapper.sh ./run_tcp_client.sh
#bash ./cpuset_wrapper.sh ./run_udp_server.sh
#bash ./cpuset_wrapper.sh ./run_udp_client.sh
