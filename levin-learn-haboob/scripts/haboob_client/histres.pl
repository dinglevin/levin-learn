#!/usr/bin/perl

# histres.pl - Extract connect, response, and combined response time
# histograms from a benchmark log file. Input is a gexec log with lines
# of the form
#   N CT C ms CC count
#   N RT R ms RC count
#   N CRT CR ms CRC count
# where N is the node number, C is the connect time, and CC is the count
# of measurements in the histogram bucket for that connect time.
# (R and CR are the response time and combined response time measurements; 
# RC and CRC are the corresponding bucket counts.)
#
# Matt Welsh, mdw@cs.berkeley.edu

while (<>) {

  if (/(\d+)\s+CT\s+(\S+)\s+ms\s+(\S+)\s+count/) {
    $conntime[$2] += $3;
    $totalconncount += $3;
  }
  if (/(\d+)\s+RT\s+(\S+)\s+ms\s+(\S+)\s+count/) {
    $resptime[$2] += $3;
    if ($2 > $RESMAX) { $RESMAX = $2 };
    $totalrespcount += $3;
  }
  if (/(\d+)\s+CRT\s+(\S+)\s+ms\s+(\S+)\s+count/) {
    $combresptime[$2] += $3;
    if ($2 > $COMBRESMAX) { $COMBRESMAX = $2 };
    $totalcombrespcount += $3;
  }
}

for ($i = 0; $i <= $#conntime; $i++) {
  if ($conntime[$i] != 0) {
    $pct = ($conntime[$i] / $totalconncount) * 100.0;
    printf "conntime %d ms %d count %.4f %%\n", $i, $conntime[$i], $pct;
  }
}
print "\n";

for ($i = 0; $i <= $#resptime; $i++) {
  if ($resptime[$i] != 0) {
    $pct = ($resptime[$i] / $totalrespcount) * 100.0;
    printf "resptime %d ms %d count %.4f %%\n", $i, $resptime[$i], $pct;
  }
}
print "\n";

for ($i = 0; $i <= $#combresptime; $i++) {
  if ($combresptime[$i] != 0) {
    $pct = ($combresptime[$i] / $totalcombrespcount) * 100.0;
    printf "combresptime %d ms %d count %.4f %%\n", $i, $combresptime[$i], $pct;
  }
}
print "\n";
