IRQ_CPUS=${OVERRIDE_IRQ_CPUS:-1,4,5}
RESTRICTED_CPUS=${OVERRIDE_RESTRICTED_CPUS:-2,3,6,7}
SYSTEM_CPUS=${OVERRIDE_SYSTEM_CPUS:-0,1,4,5}

for i in $(ls /proc/irq); do echo "$IRQ_CPUS" | sudo tee /proc/irq/$i/smp_affinity_list; done

sudo cset set --cpu_exclusive --cpu $RESTRICTED_CPUS client_set
sudo chown -R $USER:$USER /cpusets/client_set

if [[ ! -d "/cpusets/system" ]]; then
    sudo cset set --cpu_exclusive --cpu $SYSTEM_CPUS system
    sudo cset proc --move --fromset / --toset system --threads -k --force
fi


