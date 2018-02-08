#!/bin/bash
set -euxo pipefail

sudo apt install cpuset openjdk-8-jdk-headless git hwloc



cd

lstopo-no-graphics -of xml -v topo.xml

git clone https://github.com/aitusoftware/network-patterns.git

cd network-patterns

bash ./package.sh

cd

mkdir dist
cd dist
unzip ../dist.zip

export OVERRIDE_IRQ_CPUS=1,4,5
export OVERRIDE_RESTRICTED_CPUS=2,3,6,7
export OVERRIDE_SYSTEM_CPUS=0,1,4,5
CLIENT_IN_CPU=2
CLIENT_OUT_CPU=3
SERVER_IN_CPU=6
SERVER_OUT_CPU=7


bash ./mask_cpus.sh