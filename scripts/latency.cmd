set logscale x
unset xtics
set log y
set xlabel "Percentile"
set ylabel "Latency nanoseconds"
set title "Latency RTT"
plot \
'TCP_DUPLEX_MULTI_THREADED_NON_BLOCKING.txt' using 4:1 with lines ls 2 title "TCP DUPLEX MULTI-THREADED NON-BLOCKING", \
'TCP_DUPLEX_MULTI_THREADED_BLOCKING.txt' using 4:1 with lines ls 1 title "TCP DUPLEX MULTI-THREADED BLOCKING",  'TCP_DUPLEX_SINGLE_THREADED_NON_BLOCKING.txt' using 4:1 with lines ls 3 title "TCP DUPLEX SINGLE-THREADED NON-BLOCKING", \
'TCP_DUPLEX_SINGLE_THREADED_BLOCKING.txt' using 4:1 with lines ls 4 title "TCP DUPLEX SINGLE-THREADED BLOCKING", \
'TCP_SIMPLEX_MULTI_THREADED_NON_BLOCKING.txt' using 4:1 with lines ls 6 title "TCP SIMPLEX MULTI-THREADED NON-BLOCKING", \
'TCP_SIMPLEX_MULTI_THREADED_BLOCKING.txt' using 4:1 with lines ls 5 title "TCP SIMPLEX MULTI-THREADED BLOCKING", \
'TCP_SIMPLEX_SINGLE_THREADED_NON_BLOCKING.txt' using 4:1 with lines ls 7 title "TCP SIMPLEX SINGLE-THREADED NON-BLOCKING", \
'TCP_SIMPLEX_SINGLE_THREADED_BLOCKING.txt' using 4:1 with lines ls 8 title "TCP SIMPLEX SINGLE-THREADED BLOCKING", \
'./xlabels.dat' with labels center offset 0, 1.5 point title ""
pause -1
