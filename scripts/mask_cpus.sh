#!/bin/bash

SCRIPT_DIR=$(dirname "$0")
source "$SCRIPT_DIR/config.sh"


IRQ_CPUS=${OVERRIDE_IRQ_CPUS:-1,2}
RESTRICTED_CPUS=${OVERRIDE_RESTRICTED_CPUS:-2,3,4,5,6,7}
SYSTEM_CPUS=${OVERRIDE_SYSTEM_CPUS:-0,1}

for i in $(ls /proc/irq); do echo "$IRQ_CPUS" | sudo tee /proc/irq/$i/smp_affinity_list; done

sudo cset set --cpu_exclusive --cpu $RESTRICTED_CPUS $USER_CPUSET
sudo chown -R $USER:$USER /cpusets/$USER_CPUSET

if [[ ! -d "/cpusets/system" ]]; then
    sudo cset set --cpu_exclusive --cpu $SYSTEM_CPUS system
    sudo cset proc --move --fromset / --toset system --threads -k --force
fi


